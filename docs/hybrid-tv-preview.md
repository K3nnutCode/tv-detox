# Hybrid TV Preview v0.3.6 (isolated test app)

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
