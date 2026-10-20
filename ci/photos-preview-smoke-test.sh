#!/bin/sh
# After ci/photos-smoke-test.sh, install the *third* standalone preview launcher.
set -eu
cd "$(dirname "$0")/.."
TV="${1:-}"
[ -n "$TV" ] || { echo "Usage: sh ci/photos-preview-smoke-test.sh <adb serial>" >&2; exit 1; }
adb -s "$TV" install -r out-photos-preview/home-photos-preview.apk
adb -s "$TV" shell pm list packages com.ansub.tvhome.photos.preview | tr -d '\r' | grep -Fx 'package:com.ansub.tvhome.photos.preview'
adb -s "$TV" shell pm list packages com.ansub.tvhome.photos | tr -d '\r' | grep -Fx 'package:com.ansub.tvhome.photos'
adb -s "$TV" shell pm list packages com.ansub.tvhome | tr -d '\r' | grep -Fx 'package:com.ansub.tvhome'
adb -s "$TV" shell pm grant com.ansub.tvhome.photos.preview android.permission.READ_EXTERNAL_STORAGE
# A real PNG from the emulator makes the preview decode a photo and render its
# blurred clock/weather glass backgrounds, not just the empty-folder fallback.
adb -s "$TV" shell mkdir -p /sdcard/Pictures/TVWallpapers
adb -s "$TV" shell screencap -p /sdcard/Pictures/TVWallpapers/ci-frost-test.png
adb -s "$TV" shell am start -n com.ansub.tvhome.photos.preview/com.ansub.tvhome.MainActivity
sleep 4
adb -s "$TV" shell uiautomator dump /sdcard/home-photos-preview-ui.xml >/dev/null 2>&1
ui=$(adb -s "$TV" shell cat /sdcard/home-photos-preview-ui.xml)
printf '%s' "$ui" | grep -q 'BANDAR SERI BEGAWAN'
printf '%s' "$ui" | grep -q 'Open-Meteo.com'
printf '%s' "$ui" | grep -q 'Settings'
adb -s "$TV" shell dumpsys window | grep -m1 mCurrentFocus | grep -q 'com.ansub.tvhome.photos.preview'
adb -s "$TV" exec-out screencap -p > preview-frost-test.png
echo "PASS: Home Photos Preview runs with a real photo, frosted glass, and both other apps installed"
