# Hybrid TV Glass — Phase 4 TCL validation

This draft runs on the user's TCL Android TV 11. **Do not run any HOME switch, uninstall production apps, or change TV firmware** as part of visual/performance testing.

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
