#!/bin/sh
# Build a disposable, offline read-only media-session diagnostic.
# Separate package, no HOME intent, no permanent/release signing secrets.
set -eu
cd "$(dirname "$0")"

SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
[ -n "$SDK" ] || { echo "Set ANDROID_HOME to Android SDK location" >&2; exit 1; }
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
[ -f "$JAR" ] && [ -x "$BT/aapt2" ] && [ -x "$BT/d8" ] || {
  echo "Install Android SDK platform 34 and build-tools 34.0.0" >&2
  exit 1
}
for tool in javac keytool zip; do
  command -v "$tool" >/dev/null 2>&1 || { echo "Missing $tool" >&2; exit 1; }
done

OUT=out-stremio-probe
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex"

"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" \
  --manifest stremio-probe/AndroidManifest.xml --min-sdk-version 26 --target-sdk-version 30

# shellcheck disable=SC2046
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 \
  -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" \
  -d "$OUT/classes" $(find stremio-probe/src -name '*.java')
# shellcheck disable=SC2046
"$BT/d8" --min-api 26 --release --lib "$JAR" --output "$OUT/dex" $(find "$OUT/classes" -name '*.class')

cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

# Fixed TEST-ONLY signing credentials; no personal release key is used.
keytool -genkeypair -keystore "$OUT/test-only.jks" -storepass probe-only-123 \
  -keypass probe-only-123 -alias probe -keyalg RSA -keysize 2048 -validity 365 \
  -dname "CN=Temporary Stremio Session Test" >/dev/null 2>&1
"$BT/apksigner" sign --ks "$OUT/test-only.jks" --ks-pass pass:probe-only-123 \
  --out "$OUT/stremio-session-test.apk" "$OUT/aligned.apk"
echo "Built $OUT/stremio-session-test.apk (test only; not signed with Home Photos key)"
