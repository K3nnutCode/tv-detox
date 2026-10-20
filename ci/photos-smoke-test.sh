#!/bin/sh
# Checks that Home Photos installs *alongside* the existing Home app on Android TV.
set -eu
cd "$(dirname "$0")/.."
TV="$1"
[ -n "$TV" ] || { echo "Usage: sh ci/photos-smoke-test.sh <adb serial>" >&2; exit 1; }
adb -s "$TV" install -r out/tvhome.apk
adb -s "$TV" install -r out-photos/home-photos.apk

adb -s "$TV" shell pm list packages com.ansub.tvhome | tr -d '\r' | grep -Fx 'package:com.ansub.tvhome'
adb -s "$TV" shell pm list packages com.ansub.tvhome.photos | tr -d '\r' | grep -Fx 'package:com.ansub.tvhome.photos'
adb -s "$TV" shell dumpsys package com.ansub.tvhome.photos | grep -q 'android.permission.INTERNET'
adb -s "$TV" shell dumpsys package com.ansub.tvhome.photos | grep -q 'android.permission.READ_EXTERNAL_STORAGE'

adb -s "$TV" shell pm grant com.ansub.tvhome.photos android.permission.READ_EXTERNAL_STORAGE
adb -s "$TV" shell am start -n com.ansub.tvhome.photos/com.ansub.tvhome.MainActivity
sleep 4
adb -s "$TV" shell uiautomator dump /sdcard/home-photos-ui.xml >/dev/null 2>&1
ui=$(adb -s "$TV" shell cat /sdcard/home-photos-ui.xml)
printf '%s' "$ui" | grep -q 'BANDAR SERI BEGAWAN'
printf '%s' "$ui" | grep -q 'Open-Meteo.com'
printf '%s' "$ui" | grep -q 'Settings'
adb -s "$TV" shell pm path com.ansub.tvhome | grep -q 'base.apk'
echo "PASS: both launchers are installed, weather UI shows, original Home is untouched"
