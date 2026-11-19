#!/usr/bin/env bash
# Builds Dido.apk from this repository with Ubuntu/Debian's Android tools (no Android Studio or Gradle needed):
#
#   sudo apt install android-sdk-platform-23 aapt dalvik-exchange zipalign apksigner openjdk-21-jdk-headless
#   android/build.sh
#
# The APK is signed with the key in $DIDO_KEYSTORE (default ~/.dido/release.jks); a new key is made if there is none.
# Keep that key: Android only installs a new Dido over an old one when both are signed with the same key.
set -euo pipefail

VERSION_CODE=1
VERSION_NAME=1.0
MIN_SDK=24      # Android 7.0
TARGET_SDK=34   # Android 14

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(dirname "$HERE")"
ANDROID_JAR="${ANDROID_JAR:-/usr/lib/android-sdk/platforms/android-23/android.jar}"
KEYSTORE="${DIDO_KEYSTORE:-$HOME/.dido/release.jks}"
PASS_FILE="${DIDO_KEYSTORE_PASS_FILE:-$KEYSTORE.pass}"
OUT="$HERE/build"
APK="$REPO/Dido.apk"

rm -rf "$OUT"
mkdir -p "$OUT/assets/www" "$OUT/gen" "$OUT/classes" "$OUT/stubs"

echo "1/5 Copying the web app into the APK"
cp "$REPO"/{index.html,manifest.webmanifest,icon.svg,icon-192.png,icon-512.png,icon-maskable-512.png,apple-touch-icon.png} \
   "$OUT/assets/www/"
cp -r "$REPO/vendor" "$OUT/assets/www/vendor"

echo "2/5 Packing resources"
aapt package -f -m -J "$OUT/gen" -M "$HERE/AndroidManifest.xml" -S "$HERE/res" -A "$OUT/assets" -I "$ANDROID_JAR" \
  --min-sdk-version "$MIN_SDK" --target-sdk-version "$TARGET_SDK" \
  --version-code "$VERSION_CODE" --version-name "$VERSION_NAME" \
  -F "$OUT/dido-unsigned.apk"

echo "3/5 Compiling the Java code"
javac --release 8 -Xlint:-options -nowarn -d "$OUT/stubs" "$HERE/stubs/android/webkit/RenderProcessGoneDetail.java"
javac --release 8 -Xlint:-options -d "$OUT/classes" -cp "$ANDROID_JAR:$OUT/stubs" \
  "$HERE/src/app/dido/freechat/MainActivity.java" "$OUT/gen/app/dido/freechat/R.java"
dalvik-exchange --dex --min-sdk-version="$MIN_SDK" --output="$OUT/classes.dex" "$OUT/classes"
(cd "$OUT" && aapt add dido-unsigned.apk classes.dex >/dev/null)

echo "4/5 Aligning"
zipalign -f -p 4 "$OUT/dido-unsigned.apk" "$OUT/dido-aligned.apk"

echo "5/5 Signing"
if [ ! -f "$KEYSTORE" ]; then
  mkdir -p "$(dirname "$KEYSTORE")"
  (umask 077 && openssl rand -hex 24 > "$PASS_FILE")
  keytool -genkeypair -keystore "$KEYSTORE" -storetype PKCS12 -alias dido -keyalg RSA -keysize 3072 \
    -validity 10000 -dname "CN=Dido" -storepass:file "$PASS_FILE" -keypass:file "$PASS_FILE" >/dev/null 2>&1
  echo "    Made a new signing key: $KEYSTORE (password in $PASS_FILE). Keep both safe and out of git."
fi
apksigner sign --ks "$KEYSTORE" --ks-pass "file:$PASS_FILE" --min-sdk-version "$MIN_SDK" --v4-signing-enabled false --out "$APK" "$OUT/dido-aligned.apk"
apksigner verify --min-sdk-version "$MIN_SDK" "$APK"
echo "Done: $APK ($(du -h "$APK" | cut -f1))"
