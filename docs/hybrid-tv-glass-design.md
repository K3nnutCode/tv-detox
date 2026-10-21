# Hybrid TV Glass — Phase 2 design system (v1.1 preview)

This branch is a **visual experiment** based on the approved 16:9 mockup. It is separate from the v1.0 default Home and from PR #9's v1.0.1 functionality fixes.

## Design principles
1. **Photos are the wallpaper**, never YouTube/Stremio art. Wallpaper rotation remains independent and images are aspect-preserving.
2. A **single slim floating information ribbon**, with time/date, the existing locally sourced Brunei-Muara prayer timetable (all five entries and the source/out-of-range notices), and weather.
3. A **smaller featured playback card**, shown for Stremio or YouTube focus only, with title, episode/channel, thumbnail, real progress when duration exists, and one clear action.
4. A **centered floating dock**, original installed-app banners, clear focus with icy-blue border and approximately 6% lift.
5. **No video wallpaper, autoplay, expensive runtime blur, sponsored recommendations, new account access, or system app disabling**.

## Reference design rules (1920 × 1080 TV canvas, scaled proportionally)
- Safe side margins: approximately 56–176 px depending on component; preserve D-pad focus and avoid overscan.
- Top ribbon: left/right 56 px, top 42 px, target 164 px high.
- Hero: x ≈ 75, y ≈ 280, 1080 × 420 px, leaving the right side for wallpaper photos.
- Dock: centered, left/right ≈ 175 px, bottom ≈ 52 px, height ≈ 166 px.
- Corner radii: ribbon 30 px, hero 30 px, dock 32 px, focused tile 20 px.
- Surface palette: smoked grey glass `#A126303C`, dark readability overlay `#D71B2532` and quiet white highlights `#45FFFFFF`; no hard black blocks.
- Text: Android's platform sans-serif (no new font asset); white main; subdued pale blue-gray details.
- Accent: icy blue `#96C8FF` for focus/progress; retain source-status green/amber/grey semantics in the HDMI menu.
- Touch remote: 180 ms focus transition, 1.06× scale, soft elevation, no bounce.
- Media artwork: card only, approximately 16:9 viewport, never drawn to the fullscreen stage.

## Constraints / regression gates
- Preview is never a HOME candidate; original v1.0 remains the default.
- Stremio and YouTube read only Android-authorized media session data and keep last-seen snapshots; playback may not provide duration/artwork.
- TCL Settings physical BT_RC813_MA scan code 233 and native Settings dock action still work.
- Inputs chooser enumerates published TV-input IDs and reports honest connected/standby/disconnected/unknown state; no guessed HDMI IDs.
- Photo cadence 60 s and wallpaper fit/crop strategy are retained.
- Keep old signing key private; builds use disposable preview signing only.
- Device acceptance: remote navigation, input selection, both media cards, wallpaper changes after 60 s, clock/prayer/weather, clear readability on bright photos.
- Measure frame timing/memory with Android tools **before** claiming 60 fps or any RAM reduction. No such performance claim is implied by the mockup.

## Milestones
- **Phase 1:** Concept mockup done (family-photo wallpaper, glass ribbon, smaller Stremio hero, floating dock).
- **Phase 2:** This design system locked as an implementation baseline.
- **Phase 3:** New independently installable Glass preview APK, preserve stable and previous v1.0.1 preview.
- **Phase 4:** Automated Android TV build/navigation plus owner TCL screenshot and performance acceptance. Iterate before release.
