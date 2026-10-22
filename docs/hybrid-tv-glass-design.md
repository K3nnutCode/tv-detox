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

## Phase 4 measured baseline (22 Oct 2026; real TCL Android TV 11)
- Navigation-only reset: **456 frames / 1 janky (0.22%), p99 11 ms**; 57 high-input-latency flags (do not equate directly to delayed physical button presses).
- Original wallpaper-only reset: **45 frames / 4 janky (8.89%), p99 31 ms**, one slow bitmap upload.
- After the decoder worker started calling `Bitmap.prepareToDraw()` ahead of UI presentation, wallpaper-only resets: **13 frames / 0 janky / p99 13 ms**, and **12 frames / 0 janky / p99 7 ms**, no slow uploads. These small recordings are consistent with improvement, *not* proof that every crossfade was sampled or that jank is eliminated forever.
- Fifteen-minute memory test: total PSS **96.45 -> 77.41 MiB** (same pid), no observed monotonic growth. Native graphics caches fluctuate and are often purgeable; avoid treating cache growth alone as a leak. TCL `gfxinfo` reports many GPU-frame samples in a 4950 ms bucket; treat that as unreliable.
- Latest Glass CI on commit `eb38e33d1aeb0b13b6150595f7bd23fce5d993fe`: preview, Glass emulator navigation, and repo tests all passed.
- Performance is an acceptance checkpoint, **not final approval of the design**. Stable Home remains unchanged.

## Phase 5 proposed design refinement (NOT yet implemented or approved)
### Priority A — Glass dock: one cohesive shelf, not six large banners
- Current on-device screenshots show large white native YouTube/Google Play banners, dark Stremio card and solid Quick Panel artwork all at different levels of visual weight. The bar looks like a row of unrelated billboards.
- Target a centered, lower-profile glass shelf of **six equal-focus slots** across approximately **1,320 × 148 px** (coordinate basis: 1920 × 1080; final sizing dependent on actual safe margins).
- A uniform soft-smoke **tile substrate** for all six apps; **actual installed-app logo/icon assets retained** (no fake brand vector recreations). Prefer official app icon inside an unbranded glass tile when available, or letterbox its official banner using aspect-safe `FIT_CENTER` instead of cropping a white full-tile billboard. Test the logo visibility from a sofa before settling on scale.
- System Inputs and Settings retain simple white line glyphs, same baseline and intrinsic scale as installed-app glyphs. No double captions for apps whose artwork includes a wordmark.
- One clear focus indicator only: **2 px icy-blue ring**, small lift **1.04–1.06×**, 160–180 ms, restrained shadow. D-pad navigation and TCL settings hardware scan code stay unchanged.
- Keep the idle photo visible. Focused Stremio/YouTube can reveal their media card, but Inputs and Settings do not create an opaque hero.
- Do not add animated glass shaders or continuous blur; continue cached rounded drawables and small transform-only animations.

### Priority B — Continue Watching
- Wider content state only when a real saved/active media snapshot exists; empty state is smaller and has no redundant clipped action chip.
- Text must not overlap the artwork viewport. Show genuine provider title, optional subtitle/channel, progress only if duration exists, and source / last-seen distinction.
- Action semantics: opening the app is **not** guaranteed to deep-resume the episode. Label accordingly.

### Priority C — information ribbon and utility sheets
- Prioritize reading at 2–3 m viewing distance: time, five offline Brunei-Muara prayer times with timetable source and unavailable/out-of-range notices, compact weather.
- Inputs selector should adopt the same visual language **without hiding connectivity uncertainty**: connected/standby/unknown have distinct grounded indicators, use exact `TvInputInfo` IDs.
- Future ambient state should reduce chrome after inactivity without hiding access to utility functions or interfering with wallpaper/photo fade.

### Gates before design sign-off
1. Compare a new on-device screenshot to the revised dock concept; verify authentic app icons readable and not stretched/cropped.
2. Confirm photo visibility, five prayers and weather on light **and** dark personal photos.
3. Verify D-pad focus, long labels, Stremio/YouTube media and empty states; show one predictable focus ring.
4. Measure navigation after change (baseline 456 frames / 1 janky) and wallpaper crossfade after 2+ confirmed rotations.
5. Keep PR draft, separate preview package; get explicit owner approval before changing Stable v1.0 HOME.
