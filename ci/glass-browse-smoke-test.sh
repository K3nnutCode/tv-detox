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
    grep -q 'content-desc="Browse app:' "$RESULT_DIR/ui.xml"
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
# Grid focus must move horizontally, return to the same item, and reach Home.
dump_ui
grep -o 'content-desc="Browse app: [^"]*"[^>]*focused="true"' "$RESULT_DIR/ui.xml" > "$RESULT_DIR/first-focus.txt"
test -s "$RESULT_DIR/first-focus.txt"
key KEYCODE_DPAD_RIGHT
dump_ui
expect_focus 'Browse app: '
grep -o 'content-desc="Browse app: [^"]*"[^>]*focused="true"' "$RESULT_DIR/ui.xml" > "$RESULT_DIR/right-focus.txt"
if cmp -s "$RESULT_DIR/first-focus.txt" "$RESULT_DIR/right-focus.txt"; then
    echo '::error::Grid Right did not move focus'
    exit 1
fi
key KEYCODE_DPAD_LEFT
key KEYCODE_DPAD_UP
dump_ui
expect_focus 'Browse: Photo Home'
key KEYCODE_DPAD_DOWN
dump_ui
expect_focus 'Browse app: '
# Locate the real utility cell; fewer installed apps must not require fake slots.
INPUT_POSITION=$(python3 - "$RESULT_DIR/ui.xml" <<'XML_PY'
import sys, xml.etree.ElementTree as E
nodes=[n.attrib.get('content-desc','') for n in E.parse(sys.argv[1]).iter('node')]
cells=[d for d in nodes if d.startswith('Browse app: ') or d in ('Browse: Inputs','Browse: Settings')]
print(cells.index('Browse: Inputs'))
XML_PY
)
GRID_ROW=$((INPUT_POSITION / 4))
GRID_COLUMN=$((INPUT_POSITION % 4))
# Exercise vertical movement when the installed app set spans two rows.
if [ "$GRID_ROW" -gt 0 ]; then
    key KEYCODE_DPAD_DOWN
    key KEYCODE_DPAD_UP
    dump_ui
    grep -o 'content-desc="Browse app: [^"]*"[^>]*focused="true"' "$RESULT_DIR/ui.xml" > "$RESULT_DIR/returned-focus.txt"
    cmp "$RESULT_DIR/first-focus.txt" "$RESULT_DIR/returned-focus.txt"
fi
while [ "$GRID_COLUMN" -gt 0 ]; do
    key KEYCODE_DPAD_RIGHT
    GRID_COLUMN=$((GRID_COLUMN - 1))
done
while [ "$GRID_ROW" -gt 0 ]; do
    key KEYCODE_DPAD_DOWN
    GRID_ROW=$((GRID_ROW - 1))
done
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
echo 'PASS: Photo Home, Browse grid D-pad navigation, Inputs/Back, reopen and Photo Home button'
