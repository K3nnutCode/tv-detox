# Hybrid TV Preview v0.4.3 (isolated test app)

## v0.4.3 — Stremio last-seen playback card survives pressing Back

The TCL Android TV 11 test confirmed that pressing Home from Stremio kept
its media session and showed artwork/progress, but pressing Back returned to
Stremio's menu and often **removed the active media session**. Hybrid TV had
only queried `MediaSessionManager.getActiveSessions()` when its tile was
focused, so there was no independent Continue Watching record.

This version adds **best-effort, local, explicitly opt-in last-seen snapshots**:
- `HybridSessionListener` listens for active media sessions **only when the
  owner has granted Android's notification-listener access**. It filters
  sessions strictly to `com.stremio.one`. It never reads notification
  messages, contents, titles, other apps' metadata, or contacts.
- It registers `MediaController.Callback` for Stremio and samples session
  position every 15 seconds while connected. Only a valid
  PLAYING/PAUSED/BUFFERING session with title, duration and position is
  eligible for capture. This is best effort: Android firmware can kill
  background services or Stremio can stop reporting media sessions.
- Captures title, artist, duration, last observed progress and a locally
  downscaled JPG of provided artwork in preview-app PRIVATE storage.
  No server sync, Internet calls, account access or extra permissions.
- When the selected Stremio card has no valid live session, the launcher
  reads the previous snapshot. A saved card clearly displays
  **"LAST SEEN · STREMIO · SAVED"**, an original observed time and saved
  progress, rather than claiming real-time playing information.
- A stored card expires after **14 days**. Disabling the optional listener
  causes the private saved data to be purged (on system callback or the
  next attempt to view Stremio). Deleting this isolated preview also
  deletes its cached information.
- The launcher still opens Stremio normally. This is **not** a complete
  Stremio watch-history API, and it does NOT guarantee the exact resume
  location after Stremio discards its own history.
- Android TV 11 emulator tests cover app start and D-pad focus, but only
  a real Stremio session on the TCL can verify the Back/Home distinction.

Everything already approved in v0.4.2 stays intact: undistorted family
slideshow and crossfade, native TV app banners, single blue focus outline,
1310×536 Stremio hero framing, Brunei prayer times and transitions, stable
Open-Meteo weather retries, and the permanent Home Photos fallback.
The package remains `com.ansub.tvhome.hybrid.preview`, and its manifest
still has NO HOME intent.

**Install note:** the CI test signing certificate is disposable. Return to
permanent Home Photos, revoke the existing preview listener, uninstall
ONLY the old Hybrid TV Preview, install v0.4.3, grant the same photo-read
permission if required, then explicitly re-enable the optional listener
before testing Stremio. Do not disable or remove Stremio/Home Photos.

## Earlier v0.4.2 wallpaper fix

## v0.4.2 — non-stretched, aspect-ratio-correct family wallpaper

A real TCL screenshot after the successful v0.4.1 stability tests showed
the family photos appearing unnaturally stretched. Code inspection found a
likely reason: the old slideshow built a `TransitionDrawable` combining two
different size/orientation `BitmapDrawable` images. Those layers share
bounds, which can distort one of the photo layers despite the outer
ImageView having `CENTER_CROP`.

The preview now uses a `TvWallpaperView` with TWO SEPARATE wallpaper frames.
Each frame owns its own background and foreground ImageView so image scaling
never mixes photo sizes; only the complete frame alpha is crossfaded for 850ms.

- Normal wide landscape photos: **CENTER_CROP**, filling a 16:9 TV screen
  without squashing or stretching; small edge crop is possible.
- Portrait and narrower-than-1.55:1 photographs (including 4:3): show the
  original FULL picture using **FIT_CENTER** over a **blurred, center-cropped
  copy of the same photo** that fills the TV behind it. No black pillarboxes.
- Blurred textures are generated off the UI thread at ~213x120 pixels, using
  a two-pass software box blur supported on TCL Android TV 11 (API 30).
  They do not require new network access, RenderEffect, or third-party SDKs.
- Keep the 60-second slideshow cadence and 850ms crossfade. Decode sampling
  now limits both the photo's width AND height, reducing portrait-memory use;
  finished crossfades release references to the old frame's bitmap.
- No photo modification: all images remain read-only under
  `/sdcard/Pictures/TVWallpapers/`. No changes to photo selection/order.

The current prayer times/highlighting, verified Brunei timetable,
weather timeout/retry improvement, permanent Home Photos launcher, Stremio
cinematic art, native dock banners and focus ring are **unchanged**.

Safety: This is still ONLY `com.ansub.tvhome.hybrid.preview`, with no HOME
intent, no apps disabled, and no new permissions. Since the ephemeral
GitHub Actions key changes each run, return to permanent Home Photos,
revoke only the preview's optional notification access, uninstall only
the *previous hybrid preview*, and install this newly signed test APK.
Restore photo read permission to the new installation.

**Validation:** CI Android TV navigation test checks the app opens and
responds to the remote. The final photo aspect-ratio/blur judgment must
be made from TCL screenshots showing landscape and portrait slides.

## Previous v0.4.1 weather fix

## v0.4.1 — weather reliability after TCL HTTPS handshake timeout

An actual TCL Android TV 11 `adb logcat` trace from 21 October 2026
reported `java.net.SocketTimeoutException: Read timed out` while the
Open-Meteo HTTPS TLS handshake was still underway. This was **not** a
prayer-timetable or UI-rendering failure. The seven-second TLS read timeout
and 30-minute retry-on-failure delay from the shared Home Photos component
could leave the fresh v0.4 preview displaying "--°C" for a long time.

v0.4.1 uses the new `HybridWeatherWidget.java` **only in the isolated
Hybrid TV Preview app**. `WeatherWidget.java` for permanent Home Photos
is unchanged.

- Weather source unchanged: original Open-Meteo HTTPS forecast request
  using fixed Bandar Seri Begawan coordinates (no device location).
- Network connection timeout 7s -> **15s**; HTTPS read/TLS timeout 7s -> **20s**.
- Failed requests retry after **1 minute**, **3 minutes**, **10 minutes**,
  then every **30 minutes** while preview remains active.
- Successful responses return to the original **30-minute refresh interval**.
- With no valid cached weather, status says **Weather reconnecting…**
  rather than making up temperatures. With cached data it shows the
  previous temperature marked **saved**.
- Logging uses distinct `HybridWeather` tag to make diagnosing further
  TLS/network errors easy.
- No new internet destinations, permissions, location access, or other
  app connectivity. The 2026 Brunei-Muara offline prayer timetable,
  dynamic Stremio artwork, TV dock/focus border, family slideshow,
  Brunei greeting and default Home Photos are unchanged.

**Note:** Longer timeouts/retries make the widget resilient to *transient*
handshake delays; they cannot override TV firmware TLS errors, DNS outages
or routing blocks. The real TCL must be tested after installation. If
still offline, run `adb logcat -d -s HybridWeather:W '*:S'` and inspect
the root exception before changing endpoints or certificates.

v0.4.1 remains a non-HOME test package
`com.ansub.tvhome.hybrid.preview`. Since the preview APK receives a new
throwaway signing key each run, return to permanent Home Photos,
revoke optional notification-listener access and uninstall **only**
the previous hybrid preview before installing and granting photo-read
permission again.

## Previous v0.4.0 prayer widget

## v0.4.0 — Brunei-Muara current / next prayer widget

This preview now shows a real, locally embedded prayer timetable rather than
the static "awaiting verification" notice, with precise **Brunei time**.
The initial verified scope is **1 October–31 December 2026** only (92 days);
2027 and earlier 2026 dates must never be extrapolated. For unsupported
dates the widget explicitly reports "Timetable unavailable" with `--:--`
instead of making up prayer times.

The embedded source is the published **TAQWIM 2026 ©PDI2025** printed
tables from Pusat Da'wah Islamiah, Ministry of Religious Affairs, and the
Jabatan Ukur official 2026 timetable listing. Times for **Brunei-Muara**
require no district offset. See:
- https://www.mora.gov.bn/SitePages/WaktuSembahyang.aspx
- https://www.survey.gov.bn/jadual-waktu-sembahyang/
- Publicly readable 2026 PDI Taqwim text:
  https://id.scribd.com/document/970886922/TAQWIM-2026-PDI2025

The table provides **Subuh, Syuruk, Zuhur, Asar, Maghrib and Isyak**.
Syuruk is used only as the end of the Subuh period, **not counted as a
sixth obligatory prayer**. The widget displays the five obligatory
prayer times, highlights a period only when that prayer is currently
active, and gives the next prayer's name, scheduled time and countdown.
Between sunrise and Zuhur it says "After Syuruk · before Zuhur"; after
midnight and before Subuh it says "Night · awaiting Subuh" and does
**not** incorrectly highlight yesterday's Asar. After Isyak, it looks
up **tomorrow's verified** Subuh and rolls over the countdown across midnight.

The screen refreshes countdown and active prayer automatically every
30 seconds and refreshes immediately when the launcher returns to the
foreground. No new Internet access or permissions are needed; this is an
**offline** timetable. The weather remains the only data fetched by HTTPS.

Verification in CI checks all 92 daily entries for nonmissing values,
ascending prayer times, the month boundary, 2026-10-21, first/last dates
of each month, and makes sure the program declines 2027 and
out-of-range dates. Oct 21 is **04:47 / 06:05 / 12:06 / 15:24 /
18:05 / 19:15** (Subuh/Syuruk/Zuhur/Asar/Maghrib/Isyak).

**Preserved without visual changes:** v0.3.8's single smooth blue focus
outline, original native TV banners, dock dimensions, D-pad navigation,
family photo slideshow, cinematic Stremio artwork and progress, clock,
weather and Home/Back behavior. Permanent `com.ansub.tvhome.photos.stable`
is unchanged, and test `com.ansub.tvhome.hybrid.preview` has no HOME
intent. For a test-key rotation, return to Home Photos, revoke the old
preview's optional notification-listener access, uninstall *only the
hybrid preview*, and install the new test-signed APK. Re-grant read-only
photo access afterward.

**Follow-up:** Import and independently verify January–September 2026
and the official 2027 Taqwim when available; never silently repeat
2026 dates in a different year. This build covers the user's current
October date and the remaining 2026 dates.

## Earlier v0.3.8 dock focus refinements

## v0.3.8 — fix doubled outline introduced in v0.3.7

The TCL screenshot from v0.3.7 showed a **double, offset blue focus
rectangle**, with uneven edges around Stremio. We traced this to v0.3.7
drawing two selection strokes: one in the tile's `dockSurface()`
background and another in its over-banner foreground.

**This release draws only ONE blue rounded focus outline.** The selected
background has **no stroke** at all, and the foreground has one 3px blue
stroke, inset 3px within the rounded clipping region. Unselected tiles
retain their subtle 1px neutral outline.

Only this selected-focus stroke changes. Original YouTube/Stremio/Play
Store/Quick Panel banners, tile sizing, banner crop-to-fill, six-tile dock,
D-pad navigation, Continue Watching hero, slideshow, weather, clock,
and unverified prayer placeholder are not changed.

This remains a test-only app (`com.ansub.tvhome.hybrid.preview`) without
a HOME intent. Preserve the permanent Home Photos launcher. Because CI
uses a disposable signing key, return to Home Photos, revoke the old
preview's optional broad notification permission, uninstall **only** the
hybrid preview and install v0.3.8. No new permissions needed.

## Previous v0.3.7 focus border experiment

## v0.3.7 — fix missing left/right focus outline over original TV banners

The marked TCL v0.3.6 screenshot showed Stremio's pale blue focus
border was visible at its top and bottom but missing/cut off along its
left/right edges. Root cause: the tile's focus stroke lived in its
*background*, but the `ImageView` containing the original TV banner
draws **over** that background stroke.

The focused dock tile now draws a separate, **4px inset rounded foreground
outline** *after* the banner. The inset keeps the entire border inside the
tile's rounded clipping region, making all four sides and corners visible.
No border is added when the tile isn't selected. The underlying blue
selection emphasis and small focus-scale animation are preserved.

**No other UI changes:** original app banners remain exactly as published by
their apps; banner crop-to-fill, dock dimensions, icon/text content and layout,
the large cinematic Stremio panel, existing TV remote navigation, slideshow,
clock, weather and prayer-verification placeholder are unchanged.
No new permissions and no changes to permanent Home Photos.

This remains the isolated test package `com.ansub.tvhome.hybrid.preview`.
To update from v0.3.6, first return to Home Photos, revoke the hybrid
preview's optional notification-listener access and uninstall **only** the
hybrid preview (the disposable CI signing certificate changes every build).
Then install the new v0.3.7 APK, grant existing photo read access and reopen.
The real TCL screenshot must confirm the complete blue border.

## Earlier v0.3.6 original-banner fit

## v0.3.6 — restore ORIGINAL Android TV banners fitted to their dock tiles

After trying the custom vector banners in v0.3.5, the user explicitly asked
to **revert to each installed app's own original Android TV banner**. This
release removes the hand-drawn `TvDockArt` replacements entirely.

- YouTube, Stremio, Google Play Store and Quick Panel use **their original
  Android TV activity banner**, or the application banner if that is all the
  installed APK provides.
- Display uses `ImageView.ScaleType.CENTER_CROP` with just a 2px inset
  around the landscape tile. This **fills the box** while preserving banner
  aspect ratio and avoiding the inset-letterboxing of `FIT_CENTER`.
  Depending on asset aspect ratio, a small amount of the **top/bottom may be
  cropped**. Built-in white backgrounds within an original app banner are
  part of the banner and intentionally not erased.
- If an app provides no banner, a landscape row with a small native icon and
  wordmark inside the tile remains the last resort.
- Inputs and Settings remain their clean system TV glyphs.
- The existing six-tile dock geometry and remote focus animation **do not
  change**. There is **no app-name caption beneath** the banner.
- Unchanged: the large cinematic Stremio featured panel (1310 × 536 reference),
  right-side artwork, video progress, photo slideshow, time/weather, existing
  privacy boundary, optional media-session access and unverified prayer
  timetable placeholder.

This APK remains only the separate `com.ansub.tvhome.hybrid.preview` test
application and does not register as system Home. For safe upgrades, first
return to permanent Home Photos, revoke the optional preview notification
listener, uninstall **only the old preview** (GitHub uses a new ephemeral
signing key on every run), install this APK and re-grant read-only photo
access. Do not uninstall permanent Home Photos or Stremio.

## Earlier v0.3.5 custom artwork experiment

## v0.3.5 — custom, edge-to-edge TV app cards matching the approved mockup

The TV screenshot for v0.3.4 showed that the stock banners of YouTube,
Google Play Store and Quick Panel contain fixed inner white/blank padding,
causing them to float inside the landscape cards.

For **YouTube, Stremio, Google Play Store and Quick Panel** the dock now
renders self-contained **wide, fitted vector television artwork** using
`TvDockArt.java`. The artwork is locally drawn: no image downloading,
stock mobile icon squares, embedded white-margin TV banners, new network
permission or user data collection. The treatment follows the approved mockup:
- YouTube: dark red/black branded landscape tile, large centered logo and wordmark
- Stremio: cinematic violet/navy, centered diamond-play mark and wordmark
- Google Play Store: dark widescreen branded tile with multicolour Play mark
- Quick Panel: blue gradient and large stacked-blue glyph, without a caption
- Inputs and Settings: their existing TV controls/glyphs remain
- Other installed TV apps: use their native Leanback banner (cropped to fill)
  when available; otherwise a landscape label and small icon fallback

No app-name captions are added below the tiles. The icon/wordmark built inside
each branded banner is intentional. The normal blue focus outline and
remote-navigation behavior remain unchanged.

**Preserved:** 1310 × 536 cinematic featured panel, Stremio title/episode and
progress metadata, 850px right-side artwork viewport, family photo slideshow,
clock, Open-Meteo weather, safe app launch, and unverified prayer-time
placeholder. This is a separate package `com.ansub.tvhome.hybrid.preview`
with no HOME intent. Permanent Home Photos and other apps are unaffected.

When installing a new preview build, use the GitHub Actions artifact from the
latest successful v0.3.5 run. Return to permanent Home Photos, revoke the
previous hybrid preview's broad notification access, uninstall **only**
`com.ansub.tvhome.hybrid.preview`, install the new test APK, and re-grant
READ_EXTERNAL_STORAGE for the photo slideshow. Notification access is optional
and should be granted only while testing Stremio metadata.

## Earlier v0.3.4 banner preference

## v0.3.4 — Smart TV banners and wide branded app tiles

- The bottom dock follows the approved Smart TV **landscape banner** mockup, not the mobile app-icon layout. Each installed app uses its real Android TV/Leanback activity banner first, then its application banner if supplied.
- **No extra app-name captions beneath the tiles.** A native TV banner may include its own wordmark, which is the intended design.
- If the app has no TV banner, a purpose-built horizontal brand tile is rendered: a compact emblem plus the app name inside the tile, not a giant square app icon. YouTube and Stremio get simple recognizable emblems and widescreen wordmarks. Other apps use a *small* installed icon plus wordmark only as the last resort.
- System Inputs and Settings retain their clean icon-only control tiles. Dark branded gradients and blue focused outline remain.
- No change to the 1310×536 large featured show panel, right-side Stremio art, status widgets, clock, weather, offline family slideshow, Stremio media-session access or remote navigation.
- Prayer times are still intentionally hidden until official Brunei source validation. YouTube metadata remains a fallback.
- This remains an isolated TV test package, **not** a HOME replacement. The CI test certificate changes on each build, so uninstall **only** `com.ansub.tvhome.hybrid.preview` first; never uninstall the permanent Home Photos launcher or Stremio.

## Earlier v0.3.3 icon-only dock experiment

## v0.3.3 — icon-only Smart TV dock, plus v0.3.2 artwork improvements

- Dock tiles now show **only one large centered native app logo/icon**. There is **no app-name caption underneath** any app, Inputs or Settings tile.
- App names are still used internally and as the tile's accessibility description; keyboard/D-pad accessibility and explicit OK-to-open functionality are retained.
- Uses `PackageManager.getApplicationIcon()` rather than TV banners by default, since banners often contain built-in text/wordmarks. Some third-party app icons may still have embedded letters within the artwork, which the launcher cannot independently remove.
- Tiles use subtle brand-adjacent neutral/navy/purple gradient surfaces, with the existing blue focus outline and safe remote navigation.
- **Large cinematic content hero stays at 1310 × 536** with its improved right-side key-art framing and smooth left-to-right text gradient. Do not shrink the featured area.
- Family slideshow, Brunei clock, weather, Stremio media-session artwork/progress, YouTube fallback and prayer-verification placeholder remain unchanged.
- Still only the separate `com.ansub.tvhome.hybrid.preview` test package, not a Home launcher. A disposable key is generated for each GitHub build; uninstall *only* the old preview, then install this update.

## Earlier v0.3.2 artwork refinements

## v0.3.2 — right-side art framing and smooth cinematic fade

The large selected-app featured panel **stays 1310 × 536 at x=68,y=274**.
This is a targeted visual fix requested after the real TCL v0.3.1 screenshot:
- Stremio's 16:9 media-session artwork now occupies an **850px-wide right-aligned viewport** inside the unchanged hero, rather than being enlarged to the full hero's 2.44:1 shape.
- The existing `CENTER_CROP` within this narrower viewport retains the image height and most character/body details (less severe cropping than the old 1310×536 crop). The artwork is intentionally positioned on the right; the text remains on the left.
- A five-stop left-to-right overlay protects the show title and progress bar and fades gently to nearly transparent over the right-side artwork.
- No change to the fullscreen cinematic backdrop, actual media-session information, playback progress, TV remote controls, slideshow, clock, weather, prayer placeholder, or permanent Home Photos.
- YouTube and other apps still use the **same large hero size** but honestly show placeholder information until metadata capabilities are verified.
- Test package stays `com.ansub.tvhome.hybrid.preview`; uninstall only the old preview because GitHub generates an ephemeral test certificate for every APK.
- Real TV review is needed after the Android TV 11 CI build; screen appearance depends on the original Stremio-provided image.

## Earlier v0.3.1 large-panel update

## v0.3.1 — large cinematic featured panel matching design reference

The selected **Stremio / YouTube featured-content panel** is intentionally much larger, matching the area highlighted on the user's concept screenshot. At 1920x1080 the hero is approximately **1310px wide × 536px tall**, starting at **x=68, y=274** and ending **above the dock**. This is not the app tile's blue focus border.

- When Stremio publishes artwork, it **fills the large featured panel** behind a dark left-to-right text gradient; the cinematic full-screen backdrop still works.
- The series name, episode, real progress bar, elapsed/remaining time and percentage appear **on the left** in larger typography, with the right side reserved for artwork.
- **YouTube and other apps** use the same large feature-panel size, but show honest fallback text without claiming inaccessible viewing data.
- A visual hint says **Press OK to open Stremio**, rather than promising direct episode resume (which has not been verified).
- The working TV dock focus behavior, family slideshow, weather, clock and safe Home behavior are unchanged. Prayer times are still pending official verification; v0.4 is reserved for that work.
- Stremio's published artwork may be only 455x256 pixels, so the enlarged card will look softer than high-resolution studio concepts. No artwork is fabricated or downloaded.

Reinstall this **new** preview APK as described below. Since CI rotates the disposable test signing key, return to permanent Home Photos, revoke the hybrid listener, **uninstall only `com.ansub.tvhome.hybrid.preview`**, then install the latest test APK. Do not uninstall Stremio or permanent Home Photos.

## Earlier v0.3 visual refinements

## v0.3 — visual refinements for the TCL TV
- Clock/date and greeting now use Brunei local time (`Asia/Brunei`). Greeting switches automatically: **Good Night** (22:00–04:59), **Good Morning** (05:00–11:59), **Good Afternoon** (12:00–17:59), **Good Evening** (18:00–21:59).
- Stremio Continue Watching now uses a **compact glass panel** instead of a huge black box; episode artwork and cinematic background continue to be shown when the owner opts into media-session access.
- Playback has a real horizontal progress indicator, precise watched percentage (e.g., 0.8%), elapsed and time remaining **when duration is supplied by Stremio**.
- The selected app stays dark, with a thin blue glow/focus border instead of a bright white tile.
- Inputs and Settings have native HDMI/controls glyphs rather than blank squares.
- Existing shared photo slideshow and Open-Meteo weather stay unchanged.
- **No new prayer times yet**: the Brunei-Muara prayer panel still explicitly reads "awaiting verification" until official source rows have been validated.

Download the latest `hybrid-tv-preview-apk` from the successful GitHub Actions run associated with this branch. The APK is signed using an ephemeral key on each build. Before installing this new build, **revoke notification-listener access and uninstall only `com.ansub.tvhome.hybrid.preview`** (the old preview app). This does not remove the permanent Home Photos launcher, your photos, Stremio, or any viewing history.

## Earlier v0.2 remote fix

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
