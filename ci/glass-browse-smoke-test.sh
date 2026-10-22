#!/bin/sh
# Emulator-only Glass navigation check. No HOME-role or listener grants.
set -eu
SERIAL=${1:-emulator-5554}
RESULT_DIR=${2:-glass-browse-results}
case "$SERIAL" in emulator-*) ;; *) echo 'Glass CI runs on emulators only' >&2; exit 1;; esac
adb -s "$SERIAL" shell getprop ro.kernel.qemu | tr -d '\r' | grep -qx '1'
python3 ci/check-glass-source.py
sh ci/check-glass-focus-motion.sh
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
# Synthetic fixtures only: exercise the actual photo path without owner data.
python3 - "$RESULT_DIR" <<'PHOTO_PY'
import pathlib, struct, sys, zlib
def chunk(kind, data):
    return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data))
for name, width, height, color in [('landscape', 1920, 1080, (24, 72, 112)),
                                    ('portrait', 960, 1440, (74, 102, 64))]:
    rows = (b'\x00' + bytes(color) * width) * height
    png = b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 2, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(rows)) + chunk(b'IEND', b'')
    pathlib.Path(sys.argv[1], 'glass-ci-' + name + '.png').write_bytes(png)
PHOTO_PY
adb -s "$SERIAL" shell mkdir -p /sdcard/Pictures/TVWallpapers
adb -s "$SERIAL" push "$RESULT_DIR/glass-ci-landscape.png" /sdcard/Pictures/TVWallpapers/ >/dev/null
adb -s "$SERIAL" push "$RESULT_DIR/glass-ci-portrait.png" /sdcard/Pictures/TVWallpapers/ >/dev/null
adb -s "$SERIAL" shell pm grant com.ansub.tvhome.hybrid.glass.preview android.permission.READ_EXTERNAL_STORAGE
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
# The runner disables Android animations. Real fades must still schedule frames.
adb -s "$SERIAL" shell settings get global animator_duration_scale | tr -d '\r' | grep -Eq '^0(\.0)?$'
GLASS_PID=$(adb -s "$SERIAL" shell pidof com.ansub.tvhome.hybrid.glass.preview | tr -d '\r')
adb -s "$SERIAL" logcat --pid="$GLASS_PID" -d -v epoch -s HybridWallpaperFade:I '*:S' > "$RESULT_DIR/fades-before.txt"
adb -s "$SERIAL" shell dumpsys meminfo com.ansub.tvhome.hybrid.glass.preview > "$RESULT_DIR/idle-memory-before.txt"
adb -s "$SERIAL" shell dumpsys gfxinfo com.ansub.tvhome.hybrid.glass.preview reset > "$RESULT_DIR/idle-reset.txt"
SEGMENT=0
while [ "$SEGMENT" -lt 9 ]; do
    sleep 15
    SEGMENT=$((SEGMENT + 1))
    echo "Glass synthetic slideshow sample: $((SEGMENT * 15))/135 seconds"
done
adb -s "$SERIAL" shell dumpsys gfxinfo com.ansub.tvhome.hybrid.glass.preview framestats > "$RESULT_DIR/idle-frames.txt"
adb -s "$SERIAL" shell dumpsys meminfo com.ansub.tvhome.hybrid.glass.preview > "$RESULT_DIR/idle-memory-after.txt"
adb -s "$SERIAL" logcat --pid="$GLASS_PID" -d -v epoch -s HybridWallpaperFade:I '*:S' > "$RESULT_DIR/fades-after.txt"
python3 - "$RESULT_DIR" <<'FADE_PY'
import pathlib, re, sys
root = pathlib.Path(sys.argv[1])
before = root.joinpath('fades-before.txt').read_text()
after = root.joinpath('fades-after.txt').read_text()
times = re.findall(r'^\s*(\d+\.\d+).*HybridWallpaperFade:', before, re.M)
boundary = float(times[-1]) if times else 0
fades = [(int(ms), int(frames)) for timestamp, ms, frames in re.findall(
    r'^\s*(\d+\.\d+).*Photo crossfade complete: (\d+)ms, (\d+) frames', after, re.M)
    if float(timestamp) > boundary]
assert len(fades) >= 2, 'Expected at least two actual completed photo fades'
assert all(ms >= 1050 and frames > 1 for ms, frames in fades), 'Fade timing/callback regression'
print('PASS: actual fades at animator scale zero; elapsed ms / callback counts:', fades)
print('Emulator counters are diagnostic artifacts, not TCL performance evidence.')
FADE_PY
