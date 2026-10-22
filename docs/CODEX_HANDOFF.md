# Codex handoff — Hybrid TV Glass

**Last updated:** 22 October 2026 (Brunei, UTC+08). This is a **working-state summary**, not a verbatim import of the ChatGPT conversation, test images or private materials.

## Start here
- Repository: https://github.com/K3nnutCode/tv-detox
- **Active development branch:** `feature/hybrid-tv-glass-v1.1-preview`
- **Active draft PR:** https://github.com/K3nnutCode/tv-detox/pull/10
- **Functional dependency:** draft PR #9, branch `feature/hybrid-youtube-preview-v1.0.1`. Do not assume it was merged.
- The working production Home app is `com.ansub.tvhome.hybrid.stable` (**v1.0**, default launcher). Don't change it.
- Separate functional preview: `com.ansub.tvhome.hybrid.youtube.preview` (**v1.0.1**).
- Glass design preview: `com.ansub.tvhome.hybrid.glass.preview` (**v1.1**, not a HOME candidate). Its test APK is independently/disposably signed.
- Read root `AGENTS.md`, `docs/hybrid-tv-glass-design.md`, `docs/hybrid-tv-glass-validation.md`, and current PR discussion/comments. Commit history on the Glass branch records each refinement and CI build.

## User goal
A premium but very lightweight Android TV launcher inspired by Apple TV's clarity and Free My TV's floating-glass shelf, but distinctly **personal-photo-first** on a TCL Android TV 11. The wallpaper should show personal family photographs rather than streaming media posters. Screenshots and direct television acceptance testing drive design revisions. The user has repeatedly said the **design is not finished**; do not interpret good benchmark numbers as authorization to release.

### Current visual composition at 1920×1080 reference resolution
- Full-screen family photo wallpaper, updated every 60 seconds. **All images** use `CENTER_CROP` to fill the whole TV; portrait/4:3/ultrawide crops edges; never distort dimensions or use inset `FIT_CENTER`. Previously there was a blurry-fill mode; user explicitly rejected it.
- **1050ms image crossfade** via manually timed `View.postOnAnimation()` smoothstep, independent of global system animator duration. Verified from a real TCL log: began 11:55:35.027, completed 11:55:36.088, **1061ms / 64 animation callbacks**; the owner subsequently confirmed the fade was visibly working. Decode in background and prewarm textures with `Bitmap.prepareToDraw()`.
- Upper thin smoked-glass information ribbon. Left greeting and clock/date; center **all five Brunei-Muara PDI prayer times** with current and next prayer and verified source/coverage guards; right Open-Meteo weather. Exact local timezone Asia/Brunei. Prayer table covers only verified late-2026 date rows; never estimate beyond coverage.
- Center-left contextual Stremio / YouTube media hero with actual thumbnail, title, source, and progress if real metadata exists. Streaming artwork must remain inside hero. If media permission absent or no saved playback, **use a small lower-left status strip above dock**, not an oversized dark rectangle over subjects' faces. The owner supplied a screenshot approving this change.
- Bottom center floating glass dock with six accessible slots on TCL: YouTube, Stremio, Google Play Store, Quick Panel, Inputs, Settings (actual installed-app discovery for the middle items). Original application icons plus labels, matching dim glass substrates, one icy-blue focus border, small focus scale animation, D-pad navigation via remote. No system app disabling.
- HDMI/AV Inputs dialog uses actual `TvInputManager` records, pass-through `TvContract` IDs and honest gray unknown statuses (not fake disconnected). The owner supplied an open-selector screenshot and confirmed navigation, Back dismissal and correct source switching. **The dock/window-focus polish is also TCL-validated:** the open dialog has the sole active outline and Back immediately restores the Inputs dock highlight. TCL BT_RC813_MA physical Settings scan code **233** handling remains preserved in code; it was not retested in this pass.

## Recent owner tests and observations
1. Initial Glass v1.1 preview had too-large YouTube/Stremio empty-state hero and oversized white TV banner tiles.
2. Following owner screenshots, icons were made smaller and uniform (native brand icons only); dock reduced to a floating glass shelf with readable labels. User's last screenshot shows a clean cohesive dock, full-bleed personal photograph, one focus border.
3. When Stremio is highlighted without opt-in notification-listener access, hero is now a low-left compact status strip. User's screenshot at ~14:58 Brunei showed it no longer covered the couple's faces. **This is the owner-accepted state for absent media data**.
4. After the earlier misnamed screenshot, the owner supplied the actual open HDMI/AV selector. All four visible rows fit, status text is legible and the selected row is clear. The owner confirmed navigation through rows, Back dismissal and correct source switching. Pressing Home after using a source correctly returns to Stable, which remains the default.
5. The screenshot revealed two prominent outlines: the active dialog row and the retained Inputs dock selection. The owner authorized a small Glass-only refinement in `db7c8ad` and explicitly approved updating only Glass. After the update, an open-selector screenshot showed the HDMI row highlighted and the underlying dock resting. The owner confirmed Back immediately restored the Inputs dock outline. **This specific visual/restoration check passes on the TCL.** All three GitHub workflows passed for `db7c8ad`; physical Input-key entry and broader post-change source/return scenarios were not separately confirmed.
6. Owner already validated that photos now fully fill the TV, without visible letterboxing or stretching; no need to revisit fill mode unless a fresh screenshot shows an issue.
7. Playback permissions: uninstall/reinstall Glass preview resets its own notification-listener authorization and saved snapshots. `Opt-in media session access required` is expected until the user explicitly authorizes the **broad** notification listener; not automatically a bug. Don't grant it silently or suggest copying privacy-sensitive media data to the repo.

## Performance evidence, properly qualified
- Separate rapid remote navigation run: **456 frames, 1 janky (0.22%), p99 11ms**, but 57 high-input-latency flags; counters do not equal tangible remote delay.
- 15-minute same-process memory snapshots: total PSS **96.45 MiB down to 77.41 MiB**, no monotonic increase in that interval; not proof of universal leak-freedom.
- Early wallpaper-only logs had very low frame counts and apparently 0% jank, but **the fade was actually skipping** on the TCL. Owner corrected this. Do not use those runs to claim successful animated 60 FPS wallpaper rendering.
- Fade was repaired with manual `postOnAnimation` and **visually verified**. A full post-fix `gfxinfo` with actual fading is still useful for final release gates.
- Some Skia OpenGL GPU timing reports show `4950ms` sentinel buckets; treat these as unreliable. Variable purgeable GPU texture-cache sizes are not direct evidence of leaks.

## Exact next actions
1. Read the branch, this handoff, `AGENTS.md`, and PR #10 review notes. Identify any unexpected changes before editing.
2. **Continue Phase 5 design**, not stable-release engineering. Inputs layout/navigation/source switching and the dock/window-focus refinement have owner acceptance; do not repeat the completed screenshot/Back check without a regression. Physical Input-key entry remains an additional regression scenario if delivered by the TV. Any further preview installation needs explicit owner approval.
3. Review top ribbon and source-menu polish before design sign-off. A subsequent YouTube screenshot confirmed the compact no-access strip fits without covering faces. The owner chose to leave broad notification-listener access disabled for now; real Stremio/YouTube media-card validation remains **untested**, not failed. Do not enable access or repeatedly request it as a prerequisite for other design work.
4. Preserve branch-only reversible edits, run CI/Android 11 emulator, have owner capture screenshots and give design approval.
5. Only when UI and functionality tests pass, discuss a separate production signing/promotion plan **after explicit user permission**. Do not merge PR #9/#10 or change default HOME automatically.

## Current Browse direction: one dark app grid
After seeing the lighter Browse screenshots, the owner said the design felt cluttered and lacked a premium Smart TV character. A new [dark grid concept](mockups/glass-browse-dark-concept.png) was shared through GitHub because in-chat image previews did not render. The owner then instructed: **build it now and change along as we go**. That authorizes this implementation iteration, not a Stable promotion or final design sign-off.

The current Glass Browse implementation replaces the pale panel, large app-banner cards and duplicate shelf with one four-column smoked-glass grid. Original installed-app icons appear once. Up to six apps plus Inputs/Settings occupy the first two rows; remaining discovered apps scroll below, without duplicates or invented destinations. YouTube/Stremio/Play Store are preferred when actually launchable. A Photo Home text control and Back return to the original photo view. Existing native Inputs/Settings actions and physical TCL Settings handling are preserved.

Browse adds a static dim gradient over the existing cycling photos, not a bundled landscape or live blur. Only while Browse is open, its live ribbon is slightly slimmer, the clock/greeting treatment quieter and the actual current-prayer marker an underline. All five prayer times, source/out-of-range guards, weather and timezone remain real and unchanged. Closing Browse restores Photo Home's original styling. This app-grid iteration has no Browse playback cards or media reads; existing authorized Photo Home media behavior remains. Notification access stays disabled by owner choice.

The grid implementation and its updated emulator navigation test need current CI verification and a separately approved TCL screenshot/D-pad check. Owner acceptance of earlier Browse builds does not validate this redesign. Inspect all apps via vertical scrolling, first/last-row focus, utility dialogs, Photo Home style restoration, real photo fades and long labels. No final release, HOME-role change or merge is authorized.

## Earlier Browse experiment: light dashboard (superseded)
The owner shared a light streaming-dashboard reference and explicitly chose to **add a separate Browse screen while keeping current Photo Home**. A Glass-only prototype now opens with Up from idle; Back/Photo Home returns to photos. The live ribbon remains above one wide featured card, two smaller cards and a light scrollable app shelf. Content uses actual installed TV apps and original artwork; only existing authorized preview-local saved snapshots may substitute real Last seen media. No catalogs, account integration, new permissions or access grants were added. Notification access remains disabled by owner choice.

The Browse prototype `e1dcca1` passed all three GitHub workflows, including the dedicated Android TV 11 Browse navigation check. The owner then explicitly approved the Browse update and installed the verified build. A privately supplied TCL screenshot shows the full ribbon, wide YouTube app card, two smaller native-art cards, header controls and app shelf with a single focus outline. The owner confirmed card/shelf navigation, opening an app, and Back from Browse to Photo Home all worked. These functional checks pass; final visual design approval is still outstanding.

After that functional acceptance, the owner authorized the next visual refinement. Browse now has a softer, more translucent outer panel and a shorter content surface, 348px-high cards instead of 398px, smaller aspect-safe app banners and a 126px shelf instead of 152px. Resting shelf items share one common glass surface rather than separate white boxes; focus remains explicit. App labels, actions and navigation are retained. All three workflows passed for refined build `2052461`, and the owner approved the next TV update. A subsequent private TCL screenshot shows the lighter panel, smaller original app banners and slimmer shelf; text is legible on that photo. **This is screenshot evidence, not final visual sign-off or a new functional/performance test.** Photo Home and the live ribbon remain separate and preserved.

That refined screenshot exposed the next shelf item drawing outside the rounded right edge. A narrow follow-up enables padding, child and outline clipping on the existing horizontal viewport. It retains page-level focus lift and all app actions; partially visible edge items should stay inside the shelf while remaining reachable by D-pad. **The clipping correction has not yet been installed or verified on the TCL.** Do not copy the private screenshot into this public repository.

Inputs/Settings within Browse, app-return focus, physical keys, post-change fade/performance and authorized media states were not separately confirmed by the latest owner reply. See the Browse validation gates. Any subsequent TV update requires explicit approval; no merge or release is authorized. Stable Home and previous preview packages remain independent. Private screenshots remain outside the repository.

## Build and TCL workflow (user performs TV commands)
- Current CI: `.github/workflows/hybrid-stable.yml` (the Glass preview artifact `hybrid-tv-glass-v1.1-preview-apk`), plus repository tests. Glass build: `MODE=glass sh build-hybrid-stable.sh` with disposable cert, Android SDK available. Check the branch's actual workflow before assuming successful runs.
- For ADB, have user connect their local TV via `adb connect <TV_LAN_IP>:5555` in Windows Git Bash. **Do not put their private IP in this repository.** App component: `com.ansub.tvhome.hybrid.glass.preview/com.ansub.tvhome.HybridActivity`.
- Read-only screenshot command: `adb exec-out screencap -p > "$HOME/Downloads/hybrid-glass-inputs-open.png"`. With selector OPEN, the screenshot must actually show its window.
- A test build uses a disposable key, so upgrade of the same preview package may require uninstalling **only** the Glass package and reinstalling. Explicitly warn that this resets its Glass-only saved media data and listener access. **Never uninstall stable or functional preview.**
- User owns the TV; no Codex cloud runner can be assumed to reach the user's private TV network. Local ADB is user-approved only.

## Privacy and source provenance
- This is a public GitHub repository. Do not commit users' personal family photos, images of family/friends, chat exports, notification contents, unique network details, signing keys or credentials here.
- Screenshots attached privately in the earlier ChatGPT conversation are **not automatically present in Codex** and have not been added to the repo. User can attach selected screenshots directly to the Codex task.
- This document preserves **critical project context**, not a one-to-one copy of the whole conversation. ChatGPT chat history and Codex task history remain separate.
