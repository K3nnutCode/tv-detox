# Hybrid TV Glass — Phase 4 TCL validation

This draft runs on the user's TCL Android TV 11. **Do not run any HOME switch, uninstall production apps, or change TV firmware** as part of visual/performance testing.

## Phase 5 Inputs acceptance and focus refinement
- Owner evidence: open-selector screenshot reviewed for layout/readability; owner confirmed row navigation, Back dismissal and correct source selection. Home returning to Stable after source use is expected.
- Glass-only refinement `db7c8ad`, owner-validated on TCL Android TV 11 after an explicitly approved preview update: the open-selector screenshot shows the active HDMI row outline with the underlying Inputs dock tile resting. The owner confirmed Back immediately restores the Inputs dock highlight. This validates the screenshot and restoration behavior; it does not measure animation timing or performance.
- Additional regression scenarios not separately confirmed after this change: reopen, source selection/return to Glass, and physical Input-key entry if delivered by the TV (from another dock tile or idle). Original selector navigation, Back dismissal and correct source switching had owner acceptance before this refinement.
- No notification-listener access is needed. Full media-card validation remains untested while the owner leaves that permission disabled.

## Separate Browse screen: validation gates
Owner checkpoint for `e1dcca1`: all three GitHub workflows passed, including the dedicated Browse emulator test. After an explicitly approved update, the owner supplied a TCL Browse screenshot and confirmed card/shelf navigation, app launching and Back to Photo Home. These checks pass. Visual refinement/sign-off and the other device scenarios below remain open; screenshot evidence stays private.

1. With notification-listener access still disabled, confirm Photo Home retains its ribbon, six-slot dock, photos and idle behavior. Down enters the dock; Up returns to idle; a further Up opens Browse only in Glass Preview.
2. Browse keeps the real information ribbon above a light panel: original app artwork, one wide and up to two smaller cards, scrollable app shelf, Photo Home/Inputs/Settings buttons. No fake movie rows or missing-permission playback slabs appear. Fewer installed apps must not create fake cards.
3. Check D-pad movement between featured cards, header buttons and every shelf item; long labels truncate safely. Opening a real app must work. Returning to Glass should retain Browse and restore its focus.
4. Open Inputs in Browse, check the underlying highlight rests, and dismiss with Back. Back again returns to idle Photo Home. Reopen Browse and use the Photo Home button. Confirm physical TCL Settings and Android Home behavior separately on the owner TV.
5. Existing authorized saved-media cards, if available, must say Last seen and open the provider app without claiming deep resume. Revoke access outside the preview and return: app cards must replace saved-media content. This path remains untested when permission is disabled.
6. Collect an owner Browse screenshot privately and confirm photo rotation after two real fades. No TCL deployment or design sign-off is implied by emulator checks. The emulator script `ci/glass-browse-smoke-test.sh` exercises entry, shelf/header focus, Inputs/Back and Photo Home return without HOME changes or notification grants.

## Visual checks (1920 × 1080)
1. Launch Glass preview by explicit component; expect photo-only idle state.
2. Top: single glass ribbon, clock/date left, all five prayer entries and verified source marker in the center, compact weather right. Nothing overlaps, even with a long next-prayer label.
3. D-pad DOWN/LEFT/RIGHT: dock focus remains readable and only one border appears; UP reveals clean wallpaper.
4. Focus Stremio and YouTube after at least 60 seconds of playback; media hero shows actual metadata and optional artwork, **never** changes wallpaper.
5. Wait at least 70 seconds focused; slideshow rotates and video artwork stays within the smaller hero.
6. Press physical TCL Settings key; settings launch. Use Inputs to select explicit HDMI and return to Home; status dots reflect reported states, unknown stays grey.
7. Confirm stable launcher remains Android Home: `adb shell cmd shortcut get-default-launcher`.

## Frame and memory collection (read-only)
While preview is open and after a few minutes of D-pad use:
```bash
adb shell dumpsys meminfo com.ansub.tvhome.hybrid.glass.preview
adb shell dumpsys gfxinfo com.ansub.tvhome.hybrid.glass.preview
adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity'
```
`dumpsys gfxinfo` can be empty or show insufficient samples until focus animations/repaints occur. Collect after 20+ navigation moves. Capture smoothness with a short phone video if animation drops are not reflected in gfxinfo.

## Optimization included in preview
- No continuously recomputed runtime blur/backdrop shaders. Glass uses translucent GradientDrawables.
- Cache focused/default dock drawable instances instead of allocating on each focus transition.
- Move dock focus by small transform + elevation in 180 ms.
- Keep wallpaper bitmap downsampling and aspect-preserving display; Stremio/YouTube card thumbnails stay bounded.
- Preserve cached weather with 30-minute successful refresh schedule and bounded network retries.

## Not yet proven
- FPS, memory reduction, overdraw, input latency and long-term playback listener reliability **are not measured** until owner sends TCL outputs. Do not state 60 fps, reduced RAM or successful reboot listener recovery without evidence.

## Fallback
Glass preview package: `com.ansub.tvhome.hybrid.glass.preview`. To remove **only this prototype** if needed:
```bash
adb uninstall com.ansub.tvhome.hybrid.glass.preview
```
Original `com.ansub.tvhome.hybrid.stable`, `com.ansub.tvhome.hybrid.youtube.preview`, and Home Photos remain untouched.

## Photo fade acceptance — correction from actual TCL observation (22 Oct 2026)
The owner reported **instant/snappy photo switching with no visible fade**. The previously measured 0 janky frames over only 12–13 frames in ~150 s were **not proof of a successful crossfade**. In fact, such tiny frame counts are consistent with no continuous opacity animation happening. One possible cause is the TCL's system `animator_duration_scale` being set to 0, but this has **not been confirmed**.

New Glass-only crossfade implementation uses `View.postOnAnimation()` and `SystemClock.uptimeMillis()` to manually advance the incoming slide's alpha over **1050 ms** (smoothstep), independent of `ViewPropertyAnimator` global duration scale. It does **not** change the TV's global developer settings; photo aspect handling, 60 s timing, off-thread decode and texture prewarm remain unchanged.

### Verify on the owner TV (requires the *new* independent Glass preview APK)
1. In Git Bash, first optionally inspect the system setting with `adb shell settings get global animator_duration_scale` (read-only; zero means standard property animations can be instant).
2. Launch new Glass preview. Two or more photos must exist under `Pictures/TVWallpapers`; keep activity visible and do not press Home.
3. Wait for a photo change and **watch for an obvious approximately one-second opacity crossfade**, not a hard cut.
4. Confirm from read-only logcat:
   ```
   adb logcat -d -s HybridWallpaperFade:I
   ```
   A successful transition should log `Starting photo crossfade: 1050ms` and `Photo crossfade complete: [elapsed]ms, [frames] frames`. About 45–70 callbacks is typical on a healthy 60 Hz device, but refresh/frame availability may vary. A single start without completion is a regression.
5. Then collect **a new post-fix** slideshow-only `dumpsys gfxinfo` after resetting and watching at least two crossfades. Expect more rendered frames than the previous 12–13-frame snapshot, because the fade actually runs. Evaluate new jank rates; do not compare the old 0% as a true fade baseline.
6. If photo switch still hard-cuts or log completion absent, stop promotion and investigate view scheduling/visibility and GPU behavior.

No default Home change, app uninstall outside the Glass preview, firmware tweak or permission reset on other apps is permitted for this test.

## Owner-requested universal full-bleed wallpaper acceptance (22 Oct 2026)
- On the new **Glass preview only**, `TvWallpaperView.Slide.photo` always uses `ImageView.ScaleType.CENTER_CROP`. No separate blurred backdrop or `FIT_CENTER` logic.
- Choose **two visibly different landscape** family pictures, **a 4:3** picture and **a portrait** photo from `Pictures/TVWallpapers`. Let each display without touching the remote.
- Every photo must visually reach **all four edges** of the TV, without colored side bars, stretching, square image inside the frame or residual blurred fill. Photo subjects and aspect proportions must remain natural. Portrait and 4:3 edges **will be cropped intentionally**; confirm whether the crop is acceptable.
- Between photos, verify a **real ~1.05-second crossfade**, not the original hard cut; use `adb logcat -d -s HybridWallpaperFade:I` to confirm a start and completion event around 1050ms. The owner previously observed **1061ms and 64 animation callbacks** with the fixed preview.
- Check first photo immediately after launching Glass, then at least two 60-second transitions. The clock, prayer times, weather, app dock and playback continue to render over the wallpaper.
- If the frame is still inset on a particular photo, inspect whether **borders are baked into the image itself**, because the renderer cannot remove them without content-aware cropping.
- Do not install/uninstall any stable Home package, alter launcher defaults or replace personal files. For optional graphics sampling, clear then collect `dumpsys gfxinfo` after visually confirming actual photo fades.
