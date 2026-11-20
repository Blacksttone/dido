# Dido — Free AI Chat

A ChatGPT-style AI assistant that is **100% free**: no subscription, no login, no API key.
The AI runs on your own phone or computer, so your chats never leave your device.

## Install on Android

1. Download **[Dido.apk](Dido.apk)** on your phone. On GitHub, open the file and tap **Download** (or "View raw").
2. Open the downloaded file. When Android asks, allow your browser or Files app to **install unknown apps**.
3. Open Dido and send a message. The first message downloads the AI model once
   (about 1.1 GB, so use Wi-Fi). After that, Dido works without internet.

Needs Android 7.0 or newer. A phone with 6 GB of RAM or more is best for the default model;
on phones with less memory, choose **Qwen3 0.6B** in the menu (☰).

## What Dido can do

- **Chat** like ChatGPT, in English, Tamil or Tanglish, with its own friendly "Dido" personality
- **Remembers you:** say "my name is …" (or fill in *What Dido should remember about you* in the menu)
- **Looks things up:** factual questions are checked on Wikipedia (free, no key) and the answer shows the source link
- **Exact tools, offline:** calculator (`18% of 2450`, `sqrt(144)+2^3`, `5 into 6`), unit conversions
  (`5 km to miles`, `100 f to c`, `2 kg in pounds`) and date/time (`what time is it`, `inniku enna date`)
- **Voice:** tap 🔊 Listen to hear any answer (uses the phone's own text-to-speech)

## Features

- Several chats with history, saved only on your device
- Replies appear word by word, with formatting and code blocks; **Stop** button; **Copy** button
- Choose the AI model; uses the phone's GPU when possible, otherwise the CPU
- Works offline once a model is downloaded
- Light and dark mode; "Free up space" deletes downloaded models

## Models

| Model | Download | Notes |
|---|---|---|
| Qwen3 1.7B (default) | 1.1 GB | Good answers; understands Tamil and many other languages |
| Qwen3 0.6B | 0.4 GB | Fastest, for older phones; simpler answers |
| Qwen 2.5 1.5B | 1.1 GB | Older alternative, mostly English |

Models are GGUF files downloaded from Hugging Face the first time you use them.

## How it works

- The whole app is [`index.html`](index.html): a web page with no server behind it.
- The AI is [llama.cpp](https://github.com/ggml-org/llama.cpp) compiled to WebAssembly by
  [wllama](https://github.com/ngxson/wllama). It uses WebGPU when the device supports it and the CPU otherwise.
  An older, slower build is bundled for old browsers/WebViews.
- The Android app ([`android/`](android)) shows that page in a full-screen WebView. The page's files are packed
  inside the APK, so only the model is downloaded.
- All libraries are in [`vendor/`](vendor) (no CDN). Licences: [`vendor/THIRD_PARTY_LICENSES.md`](vendor/THIRD_PARTY_LICENSES.md).

## Build the APK

On Ubuntu or Debian:

```bash
sudo apt install android-sdk-platform-23 aapt dalvik-exchange zipalign apksigner openjdk-21-jdk-headless
android/build.sh          # writes Dido.apk
```

No Android Studio or Gradle needed. The APK is signed with the key in `~/.dido/release.jks`, which the script
creates on first use and which is never committed. Android installs a new version over an old one only
when both are signed with the same key; otherwise uninstall the old app first (this deletes its chats).

## Run it in a browser

```bash
python3 -m http.server 8080    # then open http://localhost:8080
```

For multi-core speed, serve it with the headers `Cross-Origin-Opener-Policy: same-origin` and
`Cross-Origin-Embedder-Policy: require-corp`.
