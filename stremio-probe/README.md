# Stremio session compatibility test (Android TV 11)

This is a **temporary, read-only diagnostic**, not a new home screen.
Version **1.1** adds TCL/Android TV remote navigation: a visible yellow focus border,
LEFT/UP selects **Refresh**, RIGHT/DOWN selects **Notification access settings**,
and OK/Enter activates the selected button. HOME and BACK remain normal.
The first test APK used an ephemeral signing key, so **uninstall only
`com.ansub.stremioprobe` before installing the new test**; do not touch
either Home Photos or Stremio.
Package: `com.ansub.stremioprobe`. Its separate signing key is disposable.

The app displays only `com.stremio.one` media-session information:
title, subtitle, episode/artist, playback state, position, duration,
progress (when duration exists), and artwork bitmap presence. It can
render an artwork bitmap already present in the metadata; it does not fetch
external artwork, use internet, log video titles, save files, enumerate
other apps' media metadata, or change any TV/app setting.

## Privacy caution

Android **Notification access is system-wide**, not scoped only to Stremio.
Enabling this listener potentially grants access to notifications from other apps.
The listener's callbacks are deliberately empty and the test reads only
Stremio's media-session controller, but grant this access only temporarily.
Turn it off immediately after testing, then uninstall the test app.

## Install and run without changing Home

1. Download the **stremio-session-test-apk** artifact from the successful
   GitHub Actions run, extract `stremio-session-test.apk` into Downloads.
2. In Git Bash on your Windows computer:

```sh
adb connect 192.168.68.55:5555
adb shell am start -n com.ansub.tvhome.photos.stable/com.ansub.tvhome.MainActivity
# Remove ONLY the old standalone diagnostic. It was signed with a throwaway key.
adb uninstall com.ansub.stremioprobe
adb install "$HOME/Downloads/stremio-session-test.apk"
adb shell am start -n com.ansub.stremioprobe/.MainActivity
```

If installation fails or the diagnostic doesn't open, stop and paste the terminal
output. The original TV Home app, permanent Home Photos app, CleanYourTV and
Stremio are unaffected.

Use the remote's RIGHT button to highlight notification-access settings;
the highlighted button has a yellow border and a text focus indicator.
Press OK to open it. LEFT goes back to Refresh.

3. Choose **Notification access settings** using your TV remote and
   enable **Stremio Session Test access**. This is **opt-in** and cannot
   happen silently through the APK.
4. If the TV has no usable notification access settings, discuss the broad
   access scope before explicitly choosing the ADB alternative:

```sh
adb shell cmd notification allow_listener com.ansub.stremioprobe/.SessionAccessService
```

5. Return to the app and choose **Refresh Stremio status**. If no session
   is found, launch Stremio, play and pause a video, then return to the
   diagnostic app via the explicit `am start` command.
6. Photograph the results or transcribe only the safe fields. Hide any
   private video titles if preferred.

## Revoke access and remove test

```sh
adb shell cmd notification disallow_listener com.ansub.stremioprobe/.SessionAccessService
adb uninstall com.ansub.stremioprobe
adb shell input keyevent KEYCODE_HOME
```

The TV's permanent `com.ansub.tvhome.photos.stable` launcher remains default.
Neither these commands nor the test APK modify the default Home, Stremio's
viewing history, other TV apps, or the saved family photos.

**Note:** The artifact uses an ephemeral test-only signing key, so future
rebuilds may require uninstalling this diagnostic app before installing a
new test APK. Never use the permanent Home Photos signing key for this test.
