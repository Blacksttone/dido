package app.dido.freechat;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Dido is the web app in this repository, running in a full-screen WebView.
 *
 * The app's files are packed in the APK (assets/www) and served from https://appassets.androidplatform.net/,
 * a secure address that never goes to the network, so storage, WebAssembly and WebGPU work as on a website.
 * Only the AI model itself is downloaded (from Hugging Face) the first time you chat.
 */
public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final String HOME = "https://" + HOST + "/index.html";

    private FrameLayout root;
    private WebView web;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        styleSystemBars();
        root = new FrameLayout(this);
        setContentView(root);
        openWebView(HOME);
    }

    private boolean isNight() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    /** Status and navigation bars in the app's background colour, with dark icons in light mode. */
    private void styleSystemBars() {
        boolean night = isNight();
        int bg = night ? 0xFF17181C : 0xFFF7F7F8;
        Window w = getWindow();
        w.setStatusBarColor(bg);
        int flags = w.getDecorView().getSystemUiVisibility();
        if (night) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        else flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        if (night || Build.VERSION.SDK_INT >= 26) {
            w.setNavigationBarColor(bg);
            int lightNav = 0x00000010; // View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR (Android 8+)
            if (night) flags &= ~lightNav;
            else flags |= lightNav;
        }
        w.getDecorView().setSystemUiVisibility(flags);
    }

    private void openWebView(String url) {
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT); // show the theme colour while the page loads
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSupportMultipleWindows(false);
        web.addJavascriptInterface(new Bridge(), "DidoNative");
        web.setWebChromeClient(new WebChromeClient()); // enables alert() and confirm()
        web.setWebViewClient(new Client());
        root.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.loadUrl(url);
    }

    private static String mimeType(String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js") || p.endsWith(".mjs")) return "text/javascript";
        if (p.endsWith(".wasm")) return "application/wasm";
        if (p.endsWith(".json")) return "application/json";
        if (p.endsWith(".webmanifest")) return "application/manifest+json";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".md") || p.endsWith(".txt")) return "text/plain";
        return "application/octet-stream";
    }

    private class Client extends WebViewClient {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri uri = request.getUrl();
            if (!HOST.equals(uri.getHost())) return null; // everything else (the model download) uses the network
            String path = uri.getPath();
            if (path == null || path.length() <= 1) path = "/index.html";
            Map<String, String> headers = new HashMap<String, String>();
            // Cross-origin isolation lets the AI use several CPU cores (SharedArrayBuffer).
            headers.put("Cross-Origin-Opener-Policy", "same-origin");
            headers.put("Cross-Origin-Embedder-Policy", "require-corp");
            headers.put("Cache-Control", "no-cache");
            if (path.contains("..")) {
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", headers, null);
            }
            try {
                InputStream in = getAssets().open("www" + path);
                return new WebResourceResponse(mimeType(path), "UTF-8", 200, "OK", headers, in);
            } catch (IOException e) {
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", headers, null);
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (HOST.equals(uri.getHost())) return false;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri)); // links open in the phone's browser
            } catch (Exception ignored) {
            }
            return true;
        }

        // Android 8+: if the page is killed for using too much memory, start it again instead of closing the app.
        public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
            if (view == web) {
                root.removeView(view);
                view.destroy();
                web = null;
                openWebView(HOME + "?restarted=1");
            }
            return true;
        }
    }

    /** Helpers the page can call as window.DidoNative.*  */
    private class Bridge {
        @JavascriptInterface
        public void copy(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Dido", text));
                }
            });
        }

        /** Keeps the screen on while a model downloads or a reply is being written. */
        @JavascriptInterface
        public void keepAwake(final boolean on) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (web == null) {
            moveTaskToBack(true);
            return;
        }
        // Back closes the menu first; otherwise Dido goes to the background (the loaded AI stays ready).
        web.evaluateJavascript("window.didoBack ? window.didoBack() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String handled) {
                if (!"true".equals(handled)) moveTaskToBack(true);
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        styleSystemBars();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override
    protected void onPause() {
        if (web != null) web.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            root.removeView(web);
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }
}
