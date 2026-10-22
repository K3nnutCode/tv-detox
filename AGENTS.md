# Codex project instructions — Hybrid TV Glass (experimental branch)

Read `docs/CODEX_HANDOFF.md` before planning changes. For UI requirements and regression gates also read `docs/hybrid-tv-glass-design.md` and `docs/hybrid-tv-glass-validation.md`.

## Non-negotiable safety
- This repo contains a user's **working TCL Android TV Home launcher**. Keep `com.ansub.tvhome.hybrid.stable` unchanged and default.
- Do **not** merge PR #10 or PR #9, modify main, alter signing keys, change the Android HOME role, disable OEM launchers/system apps, uninstall other variants, or run ADB against the user's TV without their **explicit approval**.
- Work only on `feature/hybrid-tv-glass-v1.1-preview` or a separate review branch from it. It depends on `feature/hybrid-youtube-preview-v1.0.1` (PR #9); PR #10 is a draft.
- Glass is its own installable package `com.ansub.tvhome.hybrid.glass.preview`, with **no HOME intent** and disposable signing for test builds. Other previews must remain independent.
- Don't upload private family photographs/screenshots, TV LAN address, personal chat exports, notification contents or secrets to this *public* repository. Request privately attached sample screenshots from the user when needed.

## Product invariants
- Wallpaper-first: user personal photos from `Pictures/TVWallpapers` every 60s, always full-bleed `CENTER_CROP`, no stretching or blur letterboxing. Portrait photos intentionally crop outer edges.
- Preserve the confirmed custom **1050ms** smoothstep fade scheduled using `postOnAnimation` independently of Android animator-duration scale. Preserve off-thread image sampling/decode and `Bitmap.prepareToDraw()`. Media art never becomes wallpaper.
- Top ribbon: real Brunei-Muara **five prayer times** from verified offline PDI 2026 timetable only, source/out-of-range warnings, date/clock in Asia/Brunei, existing Open-Meteo weather.
- Dock: six installed-app/system shortcuts; original vendor icons; no fake brand logos, no auto-recommended ads; one glass shelf and one icy-blue focus ring; responsive Android TV remote D-pad.
- Stremio/YouTube playback hero: use only authorized Android media-session metadata and saved snapshots, show progress only for real duration, never promise deep resume when only opening an app. No available media => quiet compact strip.
- HDMI/AV chooser: published `TvInputInfo` and exact pass-through IDs; report connected/standby/disconnected/unknown **honestly**; unknown must never be fabricated as disconnected. Keep TCL physical Settings key scan-code **233** handling and system Settings launch.
- Avoid expensive runtime blur and continuous shaders on TCL Android 11; cache static surfaces, test focus and wallpaper transitions.

## Workflow
1. Inspect existing code and relevant docs before implementing. The user's latest question or on-device screenshot defines the next scope.
2. Implement one small reversible design iteration in the Glass preview.
3. Run existing GitHub Actions, `MODE=glass sh build-hybrid-stable.sh` where supported, and relevant Android TV emulator tests.
4. Ask for TCL screenshot/D-pad validation; do not assert success without user evidence.
5. Do not alter the stable launcher or release anything without clear owner approval.

## Status
Phase 5 UI design is **not finished**. See `docs/CODEX_HANDOFF.md` for the exact current stage and most recent screenshot mismatch.
