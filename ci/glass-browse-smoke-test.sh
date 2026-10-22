#!/bin/sh
# Emulator-only Glass navigation check. No HOME-role or listener grants.
set -eu
SERIAL=${1:-emulator-5554}
RESULT_DIR=${2:-glass-browse-results}
mkdir -p "$RESULT_DIR"

key() { adb -s "$SERIAL" shell input keyevent "$1"; }
dump_ui() {
    adb -s "$SERIAL" shell uiautomator dump /sdcard/glass-browse.xml >/dev/null
    adb -s "$SERIAL" shell cat /sdcard/glass-browse.xml > "$RESULT_DIR/ui.xml"
}
expect_browse() {
    dump_ui
    grep -q 'content-desc="Glass Browse"' "$RESULT_DIR/ui.xml"
    grep -q 'content-desc="Browse featured:' "$RESULT_DIR/ui.xml"
}
expect_photo_home() {
    dump_ui
    if grep -q 'content-desc="Glass Browse"' "$RESULT_DIR/ui.xml"; then
        echo '::error::Browse stayed open instead of returning to Photo Home'
        exit 1
    fi
    grep -q 'content-desc="Hybrid dock:' "$RESULT_DIR/ui.xml"
}
expect_focus() {
    if ! grep -q "content-desc=\"$1[^\"]*\"[^>]*focused=\"true\"" "$RESULT_DIR/ui.xml"; then
        echo "::error::Expected focus on $1"
        grep -o 'content-desc="[^"]*"[^>]*focused="true"' "$RESULT_DIR/ui.xml" || true
        exit 1
    fi
}

adb -s "$SERIAL" install out-hybrid-stable/hybrid-tv-glass-v1.1-preview.apk
adb -s "$SERIAL" shell am start -W -n \
    com.ansub.tvhome.hybrid.glass.preview/com.ansub.tvhome.HybridActivity
expect_photo_home
# Preserve Photo Home navigation: DOWN enters dock, UP returns to idle.
key KEYCODE_DPAD_DOWN
key KEYCODE_DPAD_UP
expect_photo_home
# A second UP opens Browse from idle, without changing Android's HOME role.
key KEYCODE_DPAD_UP
expect_browse
adb -s "$SERIAL" exec-out screencap -p > "$RESULT_DIR/browse.png"
# The app shelf must be reachable and return focus to a featured card.
key KEYCODE_DPAD_DOWN
dump_ui
expect_focus 'Browse app: '
key KEYCODE_DPAD_UP
dump_ui
expect_focus 'Browse featured: '
# Native card -> header focus, then Inputs. Works without physical TV inputs.
key KEYCODE_DPAD_UP
key KEYCODE_DPAD_RIGHT
dump_ui
expect_focus 'Browse: Inputs'
key KEYCODE_DPAD_CENTER
dump_ui
grep -q 'TV inputs' "$RESULT_DIR/ui.xml"
key KEYCODE_BACK
expect_browse
expect_focus 'Browse: Inputs'
key KEYCODE_BACK
expect_photo_home
# Reopening and the explicit Photo Home button must also work.
key KEYCODE_DPAD_UP
expect_browse
key KEYCODE_DPAD_UP
key KEYCODE_DPAD_CENTER
expect_photo_home
echo 'PASS: Photo Home, Browse, native focus, Inputs/Back, reopen and Photo Home button'
