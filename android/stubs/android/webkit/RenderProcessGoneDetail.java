package android.webkit;

/**
 * Compile-time stand-in for the Android 8 class of the same name (the build uses the Android 6 SDK).
 * It is only on the compiler's classpath and is not packaged: on the phone the real class is used.
 */
public abstract class RenderProcessGoneDetail {
    public abstract boolean didCrash();

    public abstract int rendererPriorityAtExit();
}
