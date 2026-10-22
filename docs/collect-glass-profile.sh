#!/usr/bin/env bash
# Owner-side diagnostics. Only package-scoped reads, profiling-counter resets,
# filtered logcat, and the explicitly allowed Glass am start -W are used.
# No force-stop, install, input injection, configuration or permission changes.
set -euo pipefail
TV_SERIAL="${1:?Usage: bash collect-glass-profile.sh <adb-serial> [expected-version-code]}"
EXPECTED_VERSION="${2:-111}"
case "$EXPECTED_VERSION" in ''|*[!0-9]*) printf 'Expected version must be numeric.\n' >&2; exit 2;; esac
PKG=com.ansub.tvhome.hybrid.glass.preview
ADB=(adb -s "$TV_SERIAL")
CAPTURE_DIR="$(mktemp -d "$HOME/Downloads/glass-profile.XXXXXX")"
DETAILS_FILE="$CAPTURE_DIR.txt"
capture() {
    local label="$1"; shift
    date -u '+%Y-%m-%dT%H:%M:%SZ' > "$CAPTURE_DIR/$label.time.txt"
    "${ADB[@]}" "$@" > "$CAPTURE_DIR/$label.txt"
}
prompt() { read -r -p "$1 Press Enter here when ready. " REPLY; }
memory() { capture "$1-memory" shell dumpsys meminfo "$PKG"; }
reset_frames() { capture "$1-reset" shell dumpsys gfxinfo "$PKG" reset; }
frames() { capture "$1-frames" shell dumpsys gfxinfo "$PKG" framestats; }
logs() { capture "$1-fades" logcat --pid="$GLASS_PID" -d -v epoch -s HybridWallpaperFade:I '*:S'; }
idle_sample() {
    local label="$1" duration="$2" elapsed=0
    memory "$label-before"
    logs "$label-before"
    reset_frames "$label"
    while (( elapsed < duration )); do
        sleep 15
        elapsed=$((elapsed + 15))
        printf '%s: %s/%s seconds\n' "$label" "$elapsed" "$duration"
    done
    frames "$label"
    memory "$label-after"
    logs "$label-after"
}
bundle() {
    for file in "$CAPTURE_DIR"/*.txt; do
        printf '\nFILE: %s\n' "${file##*/}"
        cat "$file"
    done > "$DETAILS_FILE"
    printf '\nUpload this ONE private file:\n%s\n' "$DETAILS_FILE"
    if command -v cygpath >/dev/null 2>&1; then cygpath -w "$DETAILS_FILE"; fi
}
# Also preserve partial results on a failed command or interrupted local run.
trap bundle EXIT
capture package shell dumpsys package "$PKG"
if ! grep -Eq "versionCode=$EXPECTED_VERSION([[:space:]]|$)" "$CAPTURE_DIR/package.txt"; then
    printf 'Expected Glass versionCode %s is not installed. Capture stopped before launching.\n' "$EXPECTED_VERSION" >&2
    exit 1
fi
memory before-start
capture start shell am start -W -n "$PKG/com.ansub.tvhome.HybridActivity"
memory after-start
GLASS_PID="$(awk '/MEMINFO in pid/ {print $5; exit}' "$CAPTURE_DIR/after-start-memory.txt")"
[[ "$GLASS_PID" =~ ^[0-9]+$ ]] || { printf 'No Glass PID confirmed.\n' >&2; exit 1; }
printf 'Expected versionCode: %s\n' "$EXPECTED_VERSION" > "$CAPTURE_DIR/context.txt"
prompt 'Keep Glass on idle Photo Home with at least two photos. Close Browse if open; leave the TV untouched for the next six minutes.'
idle_sample idle 135
idle_sample later-idle 225
prompt 'Use Down to enter the Photo Home dock and verify that an app has the visible focus outline.'
memory navigation-before
logs navigation-before
reset_frames navigation
prompt 'Perform exactly 15 visible Right/Left pairs (30 moves), about one move each second, without opening an app.'
frames navigation
memory navigation-after
logs navigation-after
prompt 'Highlight YouTube without opening it.'
memory cycle-before
reset_frames cycle
prompt 'Open YouTube using OK on the remote, wait until it is visible, and leave it there for the next 30 seconds.'
sleep 30
memory cycle-away
prompt 'Press physical Home to return to the existing default Home.'
capture return-to-glass shell am start -W -n "$PKG/com.ansub.tvhome.HybridActivity"
frames cycle
memory cycle-return
sleep 10
memory cycle-settled
printf '\nCapture finished. Cold/hot classification is recorded as reported; no forced cold start.\n'
