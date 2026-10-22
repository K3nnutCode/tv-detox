# Hybrid TV Glass — Phase 2 design system (v1.1 preview)

This branch is a **visual experiment** based on the approved 16:9 mockup. It is separate from the v1.0 default Home and from PR #9's v1.0.1 functionality fixes.

## Separate Browse experiment (22 October 2026)
The owner supplied a light streaming-dashboard reference and chose **a separate Browse screen**, retaining Photo Home. This is a Phase 5 prototype, not design sign-off or approval to install a new build.

- Glass-only entry: **Up from idle** opens Browse; Up from the existing dock still returns to idle first. A small Browse entry also supports clicking. Back or the **Photo Home** button returns to the idle photo composition. Android Home remains handled by the Stable default launcher.
- The existing live clock/date, five verified prayer times/source warnings and weather remain visible above Browse. The wallpaper and its 60-second cadence/1050ms crossfade continue independently behind the translucent surface.
- Reference-inspired light layout: one wide featured app card, two smaller app cards, original banner/icon assets, a horizontally scrollable floating app shelf, restrained cached focus surfaces and small transform animations. No runtime blur, new dependencies, font downloads or continuous shaders.
- Content is actual launchable installed TV apps. YouTube/Stremio are preferred when installed; remaining published apps are sorted by label. No pretend Movies/Shows catalogs, provider recommendations or invented titles. All cards open their app; none promise deep resume.
- If the owner already enabled Glass's broad notification-listener access and a valid preview-local saved snapshot exists, the corresponding top card may display real saved title/art, labeled **Last seen**. Reads and image prewarming run off the UI thread and recheck access before display. No access request or grant is made; without access the cards remain app shortcuts. Artwork is aspect-safe and stays inside its card, never wallpaper. No fabricated progress.
- Inputs and Settings reuse the existing host actions, including the native source chooser and TCL physical Settings handling. Opening the chooser quiets the underlying Browse selection; Back restores it. The six-slot Photo Home dock is unchanged.
- Required owner review after a separately approved update: compare Browse screenshot to the reference, check readable typography and native logos from sofa distance, shelf scroll/focus, Back/Photo Home, Inputs, Settings, photo rotations and return from launched apps. No screenshot or private media is committed.

## Design principles
1. **Photos are the wallpaper**, never YouTube/Stremio art. Every photo is displayed full-bleed using `CENTER_CROP`, regardless of aspect ratio; no stretching or `FIT_CENTER` bars. Slideshow remains independent and preserves aspect ratio by cropping edges of portrait/4:3/ultrawide images.
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

## Full-bleed wallpaper policy (owner choice, 22 Oct 2026)
- **Always fill the entire 16:9 TV screen** for every uploaded photograph using Android `ImageView.ScaleType.CENTER_CROP`. A portrait or 4:3 photo necessarily loses portions outside the central TV crop; image pixels are never geometrically stretched.
- Previous blurred-backdrop + `FIT_CENTER` presentation was intentionally removed; no more inset image or visible side borders.
- Preserve the owner-tested **1050 ms smoothstep opacity crossfade** via `postOnAnimation`, independent of TCL `animator_duration_scale`. Continue switching every 60 seconds.
- Continue decoding wallpaper bitmaps off the main thread with size sampling, then `Bitmap.prepareToDraw()` before foreground presentation. Do not use video-media artwork as wallpaper.
- Edge case: embedded borders already saved **within the image pixels** will remain part of the picture; a true screen-filling crop cannot automatically identify and remove pre-existing decorative borders.
- This is a **Glass preview-only experiment**, requiring real TCL visual testing on landscape and portrait images before any stable promotion.

## Phase 5 dock visual prototype — implemented for screenshot review (22 Oct 2026)
- **Status: experimental; owner has not approved the look on the TCL.**
- Retains six slots in installed device order (YouTube, Stremio, next two published TV apps, Inputs, Settings), with no changes to app intents, hardware remote Settings scan 233, input chooser, or media snapshots.
- Moves from oversized native app TV banners to **original application icons**, about 68×68 units, with consistent one-line 19-unit labels. Falls back to native Leanback banner fitted *inside* a 156×66 area if the app has no icon; never stretches or redraws another company's brand.
- System Input and Settings outline icons use the same visual baseline (62×62), with their caption underneath.
- The whole shelf is approximately 1,352×142 units at 1920×1080, bottom inset 55 units; slightly more transparent slate backing and six matching rounded dark-glass substrates. Retains **one** icy-blue focus ring, now a 1.045× rise over 175 ms (no expensive runtime blur).
- All photos remain full-bleed CENTER_CROP behind the dock with the user-verified 1.05-second crossfade.
- **Review criteria:** authentic app icons not tiny or washed out, all six labels visible at 2–3m on TCL, actual icon fitting/clip and focus ring correct, photo prominent and brand colors no longer turn entire tiles bright white. If any app icon has a large built-in white tile/border, try a bounded vendor-art presentation rather than fake/recolor the asset. Check a real YouTube/Stremio media hero screenshot before sign-off.

## Phase 5 owner screenshot review — second refinement (22 Oct 2026)
The owner supplied two real TCL 16:9 screenshots of the native-icon dock:
- **Idle**: the revised common glass shelf fits six dock apps and preserves full-bleed photos. Brand icons and single-line labels are legible; focus is unobtrusive. The YouTube native icon includes a white square supplied by the app itself; do not redraw the brand.
- **Stremio-focused**: the focus ring looks correct, but a **large 720×278 no-media hero** covers the man's face, saying media-session access is required. This is expected for a reinstalled independent preview without notification-listener authorization, and **not** a playback regression.

**Experimental fix**: with no actual Stremio/YouTube playback snapshot, retain source/permission feedback but compress the hero into a **785×151 lower-left strip (x≈74, y≈695)** just above the dock, with smaller typography and no redundant button. When real saved/active media exists, the full **1065×412** media card and original progress/thumbnail return. App launching and listener permission semantics are not changed.

**On-device acceptance:** screenshot the Stremio-no-access case to verify no card covers faces or clips the message. Grant *only* Glass Preview's opt-in broad notification-listener access if desired (separate app permission) and test real Stremio/YouTube saved-media card; never copy saved session data from Stable. Leave actual third-party app icons untouched and keep PR draft.

## Phase 5 top ribbon + HDMI/AV Glass preview (22 October 2026)
Following owner screenshot approval of the compact, lower-left **Stremio no-playback** strip, we retain the dock and no-data UI as-is. The source screenshot shows the empty strip above the dock, no longer covering faces; it is not proof of a functioning Stremio listener because permission was explicitly not granted to the independently installed Glass package.

This pass makes only small visual changes:
- Top ribbon remains the same 1920×1080 reference positions; slightly softened cool glass fill and separators without reducing sofa-distance type sizes, hiding any of five prayer times, changing Brunei-Muara PDI offline data/source notices, or changing Open-Meteo weather.
- HDMI/AV selector stays a native `AlertDialog` for Android TV D-pad accessibility/Back compatibility. Its window gains a slate-glass rounded background (~960 reference px width), restrained dim amount, and source rows gain a consistent glass design with cached idle and focus drawables rather than per-navigation allocations.
- Input row labels/status values remain **exactly** from published `TvInputInfo` and `TvInputManager`, including honest grey when disconnected/unknown. The existing `TvContract.buildChannelUriForPassthroughInput(...)` switching path is unchanged.
- No system overlay, HOME takeover, or changing TCL internal settings. Stable `com.ansub.tvhome.hybrid.stable` untouched.

**Acceptance:** after preview build passes, screenshot the top ribbon and separately open Inputs using the dock/remote. Check all published HDMI/AV rows appear and are navigable, status semantics unchanged, Back dismisses, one actual desired input still launches and remote Settings key still works. Focus on visual fit/readability before moving to a finalized design.
