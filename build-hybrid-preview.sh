#!/bin/sh
# Build an isolated Hybrid TV Preview APK. NO release signing secrets.
# It cannot register as TV Home, so the working Home Photos stays default.
set -eu
cd "$(dirname "$0")"
SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
[ -n "$SDK" ] || { echo "Set ANDROID_HOME" >&2; exit 1; }
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
[ -f "$JAR" ] && [ -x "$BT/aapt2" ] || { echo "Install Android platform & build-tools 34" >&2; exit 1; }
OUT=out-hybrid
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/gen/com/ansub/tvhome"
{
  echo "package com.ansub.tvhome;"
  echo "final class Apps { static final String[] LIST = {"
  grep -v '^[[:space:]]*#' apps-photos.txt | tr -d ' \t\r' | grep -v '^$' | sed 's/.*/    "&",/'
  echo "}; }"
} > "$OUT/gen/com/ansub/tvhome/Apps.java"

"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" \
    --manifest AndroidManifest-hybrid-preview.xml --min-sdk-version 26 --target-sdk-version 30
# shellcheck disable=SC2046
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 \
    -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" \
    -d "$OUT/classes" $(find src "$OUT/gen" -name '*.java')
# shellcheck disable=SC2046
"$BT/d8" --min-api 26 --release --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"
keytool -genkeypair -keystore "$OUT/hybrid-test-key.jks" -storepass hybrid-only-test \
    -keypass hybrid-only-test -alias hybrid -keyalg RSA -keysize 2048 -validity 365 \
    -dname "CN=Temporary Hybrid Preview" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$OUT/hybrid-test-key.jks" --ks-pass pass:hybrid-only-test \
    --out "$OUT/hybrid-tv-preview.apk" "$OUT/aligned.apk"
echo "Built $OUT/hybrid-tv-preview.apk (isolated preview, NOT Home)"
