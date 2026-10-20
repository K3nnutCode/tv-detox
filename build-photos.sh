#!/bin/sh
# Builds the separate Home Photos edition without replacing out/tvhome.apk.
# Requires Java 17, Android SDK platform 34 and build-tools 34.0.0.
# Keep home-photos.jks safe: future updates must use the SAME key.
set -eu
cd "$(dirname "$0")"

[ -n "${JAVA_HOME:-}" ] || for j in /opt/homebrew/opt/openjdk@17 /usr/local/opt/openjdk@17 /usr/lib/jvm/java-17-openjdk-amd64; do
  [ -d "$j" ] && JAVA_HOME=$j && break
done
[ -z "${JAVA_HOME:-}" ] || PATH="$JAVA_HOME/bin:$PATH"
export PATH

SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
[ -n "$SDK" ] || for d in /opt/homebrew/share/android-commandlinetools "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
  [ -d "$d" ] && SDK=$d && break
done
BT=$SDK/build-tools/34.0.0
JAR=$SDK/platforms/android-34/android.jar
[ -f "$JAR" ] && [ -d "$BT" ] || { echo "Android SDK missing. Set ANDROID_HOME and install platforms;android-34 and build-tools;34.0.0." >&2; exit 1; }
command -v keytool >/dev/null 2>&1 || { echo "Java keytool missing: install Java 17 and configure JAVA_HOME." >&2; exit 1; }

OUT=out-photos
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/gen/com/ansub/tvhome"

{
  echo "package com.ansub.tvhome;"
  echo "final class Apps { static final String[] LIST = {"
  grep -v '^[[:space:]]*#' "${APPS:-apps.txt}" | tr -d ' \t\r' | grep -v '^$' | sed 's/.*/    "&",/'
  echo "}; }"
} > "$OUT/gen/com/ansub/tvhome/Apps.java"

"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" --manifest AndroidManifest-photos.xml --min-sdk-version 21 --target-sdk-version 34
# shellcheck disable=SC2046
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" \
  -d "$OUT/classes" $(find src "$OUT/gen" -name "*.java")
# shellcheck disable=SC2046
"$BT/d8" --min-api 21 --release --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

KS=${HOME_PHOTOS_KEYSTORE:-home-photos.jks}
PASS=${HOME_PHOTOS_KEYSTORE_PASS:-tvhomephotos123}
if [ ! -f "$KS" ]; then
  keytool -genkeypair -keystore "$KS" -storepass "$PASS" -keypass "$PASS" -alias homephotos \
    -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Home Photos" >/dev/null 2>&1
  echo "NEW signing key: $KS"
  echo "Back it up securely. Losing it would prevent future in-place updates!"
fi
"$BT/apksigner" sign --ks "$KS" --ks-pass "pass:$PASS" --out "$OUT/home-photos.apk" "$OUT/aligned.apk"
echo "Built $OUT/home-photos.apk (package com.ansub.tvhome.photos)"
echo "Your existing com.ansub.tvhome APK and settings were not changed."
