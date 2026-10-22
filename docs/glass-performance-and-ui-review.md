# Glass performance candidate and readability audit

The owner requested measurement, performance work, then a readability audit, and
subsequently authorized continuing without phase approval prompts. This does not
promote Glass to Stable, change HOME, enable notification access, or establish
on-device acceptance. PR #10 remains draft. Personal capture files stay private.

## Measured TCL baseline

Owner-supplied `am start -W`, `dumpsys meminfo`, separately reset `gfxinfo
framestats`, and PID-filtered fade logs were inspected together. The installed
package reports version 1.1.0-glass-preview / code 110; its source commit is
unknown. All memory samples belong to the same process. KiB are divided by 1024
for MiB. Duplicate process/window summaries are counted once.

| Window | Glass PSS (MiB) | Timing and frame evidence |
| --- | --- | --- |
| Cold launch | 60.21 | COLD, TotalTime 561 ms; WaitTime 569 ms |
| Initial idle, 135.6 s | 73.39 → 114.49 | 4/138 janky (2.90%); p99 34 ms |
| Additional uncontrolled 225.2 s | Highest following snapshot 142.81 | 62/207 additional janky (29.95%) |
| Navigation capture, 13.1 s | 142.81 → 94.24 | One frame, 137.15 ms; 30 visible moves unverified |
| YouTube / Home / explicit Glass return | 94.23 → 58.02 away → 78.70 immediate → 69.12 settled | HOT return 181 ms; three recorded frames, one with Flags=1 |

The additional interval comes from subtracting the idle totals from the
navigation-reset dump **after confirming the same Stats-since timestamp**. It
must not be labeled a controlled slideshow or navigation test. The idle raw
frame ring contains only the latest 120 of its 138 frames; it cannot locate all
four aggregate janky frames. GPU completion timestamps and 4950 ms buckets are
unreliable and excluded.

Three measured priorities:

1. Native and graphics allocation: during initial idle these categories rose
   14.42 and 25.61 MiB, accounting for 97.39% of the PSS increase. Java heap was
   roughly steady. This is not proof of a leak.
2. Later pacing: the uncontrolled interval added 60 slow-draw-command and 34
   slow-UI events. Two later fades logged 34/33 callbacks, versus 64/65 in the
   initial sample. Callback counts are not measured display FPS.
3. Renderer stalls: 125.83 ms of the single 137.15 ms navigation frame occurred
   between issuing draw commands and SwapBuffers. This includes possible driver
   waiting and does not prove GPU execution time or its cause.

All memory snapshots retained 78 views. Slow bitmap-upload counters were zero.
No view-count reduction, additional prewarming, image quality reduction, forced
GC, aggressive bitmap recycling, or background cleanup is justified by this
capture alone.

## Candidate changes and before/after status

| Change | Before | Candidate / local evidence | TCL after |
| --- | --- | --- | --- |
| Glass photo compositing | Fade alpha applied to a normal FrameLayout; initial idle graphics 18.64 → 44.25 MiB and a full-screen scratch render target in gfxinfo | Single-image Glass frames report non-overlapping drawing, allowing direct alpha modulation. Background/foreground/extra or overlapping children restore ordinary group compositing. Other packages keep a normal FrameLayout. Compilation and source guards pass. | Pending; PSS/jank improvement and non-regression are **not established** |
| Glass focus timing constraint | Dock and Browse used ViewPropertyAnimator; standard effects depend on global animator scale | Reusable 175 ms wall-clock/postOnAnimation callback, retaining existing scale targets. Local deterministic tests cover intermediate progress, completion, rapid reversal, one pending callback, window loss, detach and reattach. Non-Glass dock retains its old path. | Pending; original one-frame navigation recording is inadequate for comparison |

The first change tests an allocation/rendering mechanism suggested by the
graphics counters; it does not claim to fix all measured memory growth. The
second corrects the owner's explicit animation constraint and is not claimed as
a performance gain. Keep full-bleed CENTER_CROP, 60 s rotation, the 1050 ms manual
fade, worker decoding/prewarm, native icons, exact source IDs, Settings scan code
233, timetable and opt-in media behavior.

The candidate is version **1.1.1-glass-preview / code 111** to distinguish its
captures from the original code-110 baseline. Only Glass's version fields change;
no intent, permission, signing or other manifest is changed.

The existing Glass emulator CI entry point now invokes the new source guards
and focus-clock tests. It refuses physical-device serials and verifies QEMU.
Synthetic landscape/portrait PNGs exercise two real completed photo fades with
animator scale zero; counters and screenshots remain emulator diagnostics.
Workflow definitions, manifest intents/permissions, dependencies and signing
code are unchanged. A build is not a TCL performance result.

On-device acceptance requires matched before/after workloads and no PSS or jank
regression. Repeat a longer idle window as well as the initial two fades; repeat
navigation with 30 visibly changing selections and record the raw counters.
Record identical photo sets privately without putting their names or images in
the repo. Current owner TV installation and follow-up captures are pending.

The owner-side [capture helper](collect-glass-profile.sh) accepts the connected
ADB serial as an argument and verifies the expected Glass versionCode (111 by
default). It uses only the permitted package reads, counter resets, fade logs
and `am start -W`; it does not force-stop or update anything. It captures the
initial 135 s followed by a controlled 225 s continuation, then manual navigation
and the app cycle. On exit it automatically combines its text files into one
`Downloads/glass-profile.<id>.txt`, including partial results on interruption.
This removes the earlier manual ZIP/combination step. Launch state remains as
reported; a HOT start must not be compared as cold-start improvement.

## 10-foot readability audit — proposals, no visual layout changes applied

Reference geometry is 1920×1080; source dimensions scale with display width.
This is a source/analytical audit, not final rendered or sofa-distance approval.

| Proposal | Source finding and one-line rationale |
| --- | --- |
| Raise secondary text to at least 24 reference px, with layout reflow | Dock labels are 19; Browse Home/footer 23/19; ribbon greeting/date/prayer labels/current/source are 16/19/18/16/15; compact weather city/condition/attribution 14/18/12; source hint/status 18/20; compact media details/note 21/17. Larger type needs reflow so all five prayers, coverage/source and unknown-input notices remain visible. |
| Strengthen secondary ribbon text contrast | Over a white source photo, existing photo tint and ribbon composite produce about 3.89:1 for prayer source, 4.31:1 for weather city and 3.57:1 for weather attribution, below a 4.5:1 normal-text target. |
| Keep content inside a configurable safe area | A conservative 5% box is x96–1824/y54–1026; ribbon decoration starts at x54/y44 and Browse text/grid content near x86. Inset text/focus where needed while retaining full-bleed wallpaper. |
| Retain the current 2 px icy-blue focus stroke and window-focus ownership | Dock/Browse already have a 2 px stroke; validate its edges under lift/scroll and over bright/dark photos rather than add more visual effects. |
| Standardize internal spacing on an 8 px rhythm during approved layout work | Existing 10/14/20/22 px insets and gaps vary; aligning related controls can improve consistency without adding surfaces. |

Contrast calculation: alpha-composite the existing 0x2F000000 photo tint over
white, then ribbon 0xA626343F. Composite each foreground with its alpha, apply
the sRGB transfer function, and compute (lighter luminance + .05) / (darker +
.05). The resulting ribbon RGB is approximately (97.33,106.45,113.61); opaque
white is about 5.48:1. This ignores additional scrims and cannot certify actual
pixels, antialiasing, all current-prayer backgrounds or every rendered state.

These remain proposals. No typography, ribbon geometry, spacing, photo policy,
or weather location has been changed by this performance candidate. An owner
TCL screenshot is required before any future visual change counts as finished.

## Weather file configuration — proposal only

Use `getExternalFilesDir(null)/weather.json`, owned by **Glass Preview**. Android
11 permits the app to read its own external-files directory without another
permission. A local configuration file and its coordinates must never be
committed. Do not put configuration in the public photo/media folder or change
the shared widget's other package behavior.

- Fields: `label` (bounded display text), `latitude` (finite number −90…90),
  `longitude` (finite number −180…180), and an optional schema version.
- Read and validate a size-limited file off the UI thread on startup/resume;
  avoid a continuous watcher. Missing/invalid input retains the shipped city
  default and exposes a concise fallback status, without logging coordinates.
- Build the same HTTPS weather request from validated values using Locale.US.
  Key cached weather by the effective location so one city's readings cannot be
  displayed under another label.
- Keep the clock and verified Brunei-Muara prayer timetable tied to Asia/Brunei.
  Changing weather location must not change either.
- Owner file creation/update is a separate local action; this proposal does not
  add a permission, settings screen, dependency, file reader or network change.

## Validation status

Local Glass compilation, APK verification, current source guards, deterministic
focus tests, shell syntax and the existing 92-entry prayer test passed. GitHub
Android TV checks and all new owner TCL after-measurements are pending at the
time of this candidate's preparation. Neither phase 2 performance acceptance nor
phase 3 screenshot acceptance is complete.
