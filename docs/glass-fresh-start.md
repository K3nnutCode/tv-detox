# Fresh validation with Google TV as Home

The owner confirmed switching the TCL's default Home back to Google TV with
apps retained and no factory reset, and wants a fresh validation start. Reuse
the installed apps and working code. This does not authorize app removal,
debloating, changing HOME, or a factory reset.

## Starting state

- Keep Google TV as default Home and fallback. Any remaining Stable, older
  previews and system apps stay unchanged.
- Keep testing `com.ansub.tvhome.hybrid.glass.preview` as a separately launched
  preview with no HOME intent. The current candidate is version 1.1.1 / code 111.
- Preserve full-bleed CENTER_CROP, 60 s photo rotation, manual 1050 ms fade,
  glass ribbon, native icons, Settings scan code 233, exact HDMI/AV inputs,
  verified prayer data and opt-in media access.
- Earlier memory/jank measurements describe the previous configuration. Google
  TV/background services can change the workload. Keep
  old results as history and collect a new baseline before comparing candidates.

## Read-only discovery first

Run local ADB on the owner's computer. Use the current TV address privately; do
not put it or the resulting inventory into the public repository. Package
queries can establish whether Glass, protected older variants and owner apps
remain installed and identify the exact Glass build. Home changing alone does
not establish that prior settings or permissions changed.

```bash
TV_SERIAL='<current-TV-address>:5555'
adb connect "$TV_SERIAL"
for package in \
  com.ansub.tvhome.hybrid.glass.preview \
  com.ansub.tvhome.hybrid.stable \
  com.ansub.tvhome.hybrid.youtube.preview \
  com.google.android.youtube.tv \
  com.stremio.one \
  bn.rtbgo.tv
do
  printf '\n%s\n' "$package"
  adb -s "$TV_SERIAL" shell dumpsys package "$package" |
    grep -E 'Unable to find package|versionCode=|versionName=|User 0:'
done
```

These commands do not install, remove, enable/disable or configure any app. If
ADB cannot connect, establish the owner's local connection before attempting
app operations. The cloud workspace cannot be assumed to reach a private TV.

## Choose the next step from the observed state

1. Glass absent: prepare a first install of only the verified code-111 APK.
   Nothing else needs to be removed. Device-changing commands remain separate
   from the read-only discovery and measurement scripts.
2. Glass already code 111: reuse it for the fresh baseline; an uninstall is not
   needed just to start validation again.
3. Older Glass present: inspect its reported version and plan only its update.
   Disposable preview keys may cause a signature conflict. Do not automatically
   uninstall on failure; Glass-only removal would reset its own preferences,
   media snapshots and listener access. Preserve the current default Home.
4. Confirm photo availability/access and owner app availability without replacing
   files or permissions. Keep any personal images outside Git.
5. Open Glass explicitly, check two actual photo fades and remote navigation,
   and verify Home returns to Google TV. Notification access remains opt-in.
6. Run [the read-only collector](collect-glass-profile.sh) against the identified
   build. Preserve default animation scales and other current system settings
   for the first fresh baseline; do not repeat old debloat/setup scripts.

Keep PR #10 draft. No new performance or visual acceptance is implied by this
context update; new TCL captures and screenshot confirmation remain necessary.
