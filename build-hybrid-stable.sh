#!/bin/sh
# Hybrid TV Stable v1.0.0.
# MODE=smoke: disposable signing for emulator only, NEVER a user release.
# MODE=release: REQUIRE the same persistent signing key for all future updates.
set -eu
umask 077
cd "$(dirname "$0")"
MODE="${MODE:-smoke}"
case "$MODE" in smoke|release|preview|glass) ;; *) echo "Unsupported MODE" >&2; exit 2;; esac
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[ -n "$SDK" ] || { echo "Set ANDROID_HOME" >&2; exit 1; }
BT="$SDK/build-tools/34.0.0"
JAR="$SDK/platforms/android-34/android.jar"
[ -f "$JAR" ] && [ -x "$BT/aapt2" ] || { echo "Install Android SDK 34/build tools" >&2; exit 1; }
OUT=out-hybrid-stable
rm -rf "$OUT"
mkdir -p "$OUT/classes" "$OUT/dex" "$OUT/gen/com/ansub/tvhome"
{
  echo 'package com.ansub.tvhome;'
  echo 'final class Apps { static final String[] LIST = {'
  grep -v '^[[:space:]]*#' apps-photos.txt | tr -d ' \t\r' | grep -v '^$' | sed 's/.*/    "&",/'
  echo '}; }'
} > "$OUT/gen/com/ansub/tvhome/Apps.java"

MANIFEST=AndroidManifest-hybrid-stable.xml
if [ "$MODE" = preview ]; then MANIFEST=AndroidManifest-hybrid-youtube-preview.xml; fi
if [ "$MODE" = glass ]; then MANIFEST=AndroidManifest-hybrid-glass-preview.xml; fi
"$BT/aapt2" link -o "$OUT/base.apk" -I "$JAR" \
  --manifest "$MANIFEST" --min-sdk-version 26 --target-sdk-version 30
# shellcheck disable=SC2046
javac -nowarn -Xlint:-options -encoding UTF-8 -source 1.8 -target 1.8 \
  -bootclasspath "$JAR:$BT/core-lambda-stubs.jar" -d "$OUT/classes" \
  $(find src "$OUT/gen" -name '*.java')
# shellcheck disable=SC2046
"$BT/d8" --min-api 26 --release --lib "$JAR" --output "$OUT/dex" \
  $(find "$OUT/classes" -name '*.class')
cp "$OUT/base.apk" "$OUT/unsigned.apk"
(cd "$OUT/dex" && zip -q ../unsigned.apk classes.dex)
"$BT/zipalign" -f 4 "$OUT/unsigned.apk" "$OUT/aligned.apk"

# Neither signing keys nor passwords are ever committed or stored as artifacts.
KEY_TMP=$(mktemp "${RUNNER_TEMP:-/tmp}/hybrid-signing-XXXXXX")
trap 'rm -f "$KEY_TMP"' EXIT HUP INT TERM
if [ "$MODE" = release ]; then
  : "${HYBRID_RELEASE_KEYSTORE_B64:?Add persistent keystore as HYBRID_RELEASE_KEYSTORE_B64 secret}"
  : "${HYBRID_RELEASE_KEYSTORE_PASS:?Add HYBRID_RELEASE_KEYSTORE_PASS secret}"
  : "${HYBRID_RELEASE_KEY_ALIAS:?Add HYBRID_RELEASE_KEY_ALIAS secret}"
  # Decode the exact same keystore on every release, never generate a new one.
  printf '%s' "$HYBRID_RELEASE_KEYSTORE_B64" | tr -d '\r\n\t ' | base64 -d > "$KEY_TMP"
  if [ -n "${HYBRID_RELEASE_KEY_PASS:-}" ]; then
    export HYBRID_SIGN_KEY_PASS="$HYBRID_RELEASE_KEY_PASS"
  else
    export HYBRID_SIGN_KEY_PASS="$HYBRID_RELEASE_KEYSTORE_PASS"
  fi
  "$BT/apksigner" sign --ks "$KEY_TMP" \
    --ks-key-alias "$HYBRID_RELEASE_KEY_ALIAS" \
    --ks-pass env:HYBRID_RELEASE_KEYSTORE_PASS \
    --key-pass env:HYBRID_SIGN_KEY_PASS \
    --out "$OUT/hybrid-tv-v1.0.0.apk" "$OUT/aligned.apk"
  "$BT/apksigner" verify "$OUT/hybrid-tv-v1.0.0.apk"
  echo "Built production-signed Hybrid TV Stable v1.0.0"
else
  # keytool creates the keystore itself; mktemp leaves an empty file.
  rm -f "$KEY_TMP"
  keytool -genkeypair -keystore "$KEY_TMP" -storetype PKCS12 \
    -storepass smoke-test-only -keypass smoke-test-only \
    -alias ci-smoke -keyalg RSA -keysize 2048 -validity 2 \
    -dname 'CN=CI smoke test ONLY' >/dev/null 2>&1
  TEST_APK="$OUT/hybrid-tv-v1-smoke-NOT-RELEASE.apk"
  if [ "$MODE" = preview ]; then TEST_APK="$OUT/hybrid-tv-v1.0.1-youtube-preview.apk"; fi
  if [ "$MODE" = glass ]; then TEST_APK="$OUT/hybrid-tv-glass-v1.1-preview.apk"; fi
  "$BT/apksigner" sign --ks "$KEY_TMP" --ks-key-alias ci-smoke \
    --ks-pass pass:smoke-test-only --key-pass pass:smoke-test-only \
    --out "$TEST_APK" "$OUT/aligned.apk"
  "$BT/apksigner" verify "$TEST_APK"
  echo "Built test-only $MODE APK — NOT a stable release"
fi
