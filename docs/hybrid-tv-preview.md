# Hybrid TV Preview v0.1 (isolated test app)

This is **stage 1** of the combined CleanYourTV-inspired TV home and Home Photos. It is a separate application, **not** a Home launcher.

## v0.2 TV remote navigation fix

The initial v0.1 layout let the background take focus on some TCL TV remotes.
v0.2 handles D-pad keys explicitly:

- DOWN or RIGHT from the unselected photo view enters the app dock
- LEFT/RIGHT moves between dock entries with visible focus
- UP returns to the unobstructed family photo slideshow
- OK/Enter opens the focused tile
- BACK and HOME retain system behavior

The preview APK is signed with a **different disposable key per GitHub build**.
Before installing v0.2, return to the permanent Home Photos screen, **uninstall
only** `com.ansub.tvhome.hybrid.preview`, then install the newest downloaded
`hybrid-tv-preview.apk`. Do not uninstall Home Photos or Stremio. The permanent `com.ansub.tvhome.photos.stable` remains unchanged/default, as do Stremio and other apps.

## Included

- Smart TV bottom dock: real installed apps (prefer YouTube, Stremio), extra apps and TV input/Settings shortcuts; clear D-pad focus and remote OK actions
- Full-screen family photo slideshow read-only from `/sdcard/Pictures/TVWallpapers`, with an in-memory fade and one-minute rotation
- Clock/date and cached HTTPS Open-Meteo weather for Bandar Seri Begawan, reusing the existing Home Photos weather implementation
- **When highlighting Stremio:** a media-session-driven cinematic background and card with title, episode, watched/total time and artwork **if Android exposes them**; opt-in, local-only notification listener required. The app does not save, export or sync viewing data.
- YouTube and other apps show an honest generic preview until separate compatibility testing confirms available metadata.

**Not included yet:** accurate prayer schedule. The top prayer panel reads "Official prayer timetable: awaiting verification" and deliberately shows no incorrect or made-up prayer times. Source for the later import: [Brunei Ministry of Religious Affairs 2026 Taqwim](https://www.mora.gov.bn/Muat%20Turun%20Library/PDI/TAQWIM%202026%20%C2%A9PDI2025.pdf), for Brunei-Muara, with timezone Asia/Brunei. Once parsed, compare several random days against the official document before making it active. The source has different offsets for Tutong and Belait.

This preview is a fresh Android application implementing our layout; it is **CleanYourTV-inspired**, not an exact fork of CleanYourTV's underlying code, themes or animations. We can port its more intricate focus effects later.

## Installation on TCL TV (Android 11)

1. Download latest `hybrid-tv-preview-apk` GitHub Actions artifact and extract `hybrid-tv-preview.apk` into `C:\Users\rahim\Downloads`.
2. Open Git Bash and run:

```sh
adb connect 192.168.68.55:5555
adb shell pm list packages com.ansub.tvhome.hybrid.preview
# If installed, first return to working Home Photos and uninstall ONLY the preview.
adb shell am start -n com.ansub.tvhome.photos.stable/com.ansub.tvhome.MainActivity
adb shell cmd notification disallow_listener com.ansub.tvhome.hybrid.preview/com.ansub.tvhome.HybridSessionListener
adb uninstall com.ansub.tvhome.hybrid.preview
adb install "$HOME/Downloads/hybrid-tv-preview.apk"
adb shell pm grant com.ansub.tvhome.hybrid.preview android.permission.READ_EXTERNAL_STORAGE
adb shell am start -n com.ansub.tvhome.hybrid.preview/com.ansub.tvhome.HybridActivity
```

3. Press TV remote arrows to navigate. HOME still returns to permanent Home Photos. If navigation fails, collect a TV screenshot using `adb exec-out screencap -p > "$HOME/Downloads/hybrid-preview.png"`.
4. When finished, return to permanent Home Photos:

```sh
adb shell am start -n com.ansub.tvhome.photos.stable/com.ansub.tvhome.MainActivity
```

## Stremio preview access is OPT-IN and broad

The system notification-listener permission potentially exposes notifications from other apps. This app's listener methods are empty; the user interface reads only Stremio's active Android media session, and **no notification content** is read or stored. Do **not** enable the listener just to test the dock/photos/weather; that part works without it.

If you deliberately choose to test Stremio's Continue Watching panel, grant permission in TV settings or, **only with informed consent**, via ADB:

```sh
adb shell cmd notification allow_listener com.ansub.tvhome.hybrid.preview/com.ansub.tvhome.HybridSessionListener
```

Then pause a video in Stremio, return to Hybrid TV Preview and highlight Stremio.

Revoke immediately when finished:

```sh
adb shell cmd notification disallow_listener com.ansub.tvhome.hybrid.preview/com.ansub.tvhome.HybridSessionListener
```

## Removal, signing and safety

`adb uninstall com.ansub.tvhome.hybrid.preview` deletes **only the preview package**. This does not delete Stremio history or shared photos. This test APK uses a disposable CI-generated signing key which changes for each build; to install a *new* build, first uninstall only the preview app. Unlike the permanent Home Photos workflow, **no release signing key or GitHub secret is used**.

Do not run `install.sh`, `undo.sh`, CleanYourTV debloat tools, or `set-home-activity` for this test. Those are unrelated and can change the TV's home behavior.
