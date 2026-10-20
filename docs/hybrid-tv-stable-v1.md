# Hybrid TV v1.0 — stable launcher release candidate

The stable launcher uses the **tested v0.4.3 UI and behavior**:
family slideshow with correct photo aspect ratio, Brunei-Muara offline
prayer times, responsive Open-Meteo weather, original Android TV banners,
complete focus border, Stremio media-session artwork, and a private
14-day saved-session fallback. No redesign is part of this release.

## Safety architecture

- Permanent Home Photos remains installed at:
  `com.ansub.tvhome.photos.stable/com.ansub.tvhome.MainActivity`.
- Stable Hybrid TV is a **different package**:
  `com.ansub.tvhome.hybrid.stable/com.ansub.tvhome.HybridActivity`.
- Existing Hybrid TV Preview remains a third separate package:
  `com.ansub.tvhome.hybrid.preview`.
- The stable manifest is a real, selectable Android TV **HOME** candidate
  using MAIN/HOME/DEFAULT. It also has its own LEANBACK_LAUNCHER entry, so
  it can be launched manually and tested before becoming the default.
- The APK does **not** request privileges to hide, uninstall, disable or
  change other installed apps. Do not modify system Home Photos or Stremio.
- Any choice of default HOME is a **separate, reversible owner action**;
  building or installing does not set it automatically.

## Release signing — required for an updatable v1.0

The project deliberately has **two** Android build modes:

1. `MODE=smoke sh build-hybrid-stable.sh`: creates a disposable-key emulator
   artifact called `hybrid-tv-v1-smoke-NOT-RELEASE.apk`. **NEVER install it
   as your permanent launcher**. Its key is destroyed after the build.
2. `MODE=release sh build-hybrid-stable.sh`: only runs when the owner's
   GitHub repository secrets contain a long-lived private signing keystore.
   Produces `hybrid-tv-v1.0.0.apk`. All later stable versions must use
   the **same keystore and alias**, with increasing versionCode.

Never send passwords, Base64 signing keys or the keystore to ChatGPT or
commit them to a public repository. Store an offline backup of the keystore
and its passphrase. Losing this key means the installed stable package
cannot be updated in place.

### Option 1: Dedicated Hybrid TV signing key (recommended)

On your Windows PC with the JDK installed, in **PowerShell**:

```powershell
New-Item -ItemType Directory -Force "$env:USERPROFILE\HybridTVSigning"
keytool -genkeypair -storetype PKCS12 -keystore "$env:USERPROFILE\HybridTVSigning\hybrid-tv-stable.jks" -alias hybridtvstable -keyalg RSA -keysize 3072 -validity 10000 -dname "CN=Hybrid TV Stable"
```

Choose a strong keystore password when prompted. Keep a secure backup.
Copy the Base64 data directly to your clipboard **without printing it**:

```powershell
$path = "$env:USERPROFILE\HybridTVSigning\hybrid-tv-stable.jks"
$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes($path))
$b64 | Set-Clipboard
"Base64 characters: $($b64.Length)"
```

GitHub → `K3nnutCode/tv-detox` → **Settings → Secrets and variables
→ Actions → New repository secret**. Add these secrets:

- `HYBRID_RELEASE_KEYSTORE_B64`: paste the full clipboard Base64 string.
- `HYBRID_RELEASE_KEYSTORE_PASS`: the password you chose.
- `HYBRID_RELEASE_KEY_ALIAS`: `hybridtvstable`.
- `HYBRID_RELEASE_KEY_PASS` (optional): only needed if the key has a
  different password from the keystore password.

Alternatively, if preferred, reuse your already-backed-up permanent Home
Photos signing keystore and enter its **real alias and password** in these
new repo-specific secrets. Keep your Home Photos repository and its keys
unchanged. A dedicated key keeps the two projects independent.

Release secrets belong in this repository; they are **not shared
automatically** between different GitHub repositories.

### CI order

1. On the stable branch, verify the **smoke build and Android TV 11
   D-pad emulator** checks pass. That artifact is test-only.
2. Review and merge the predecessor Hybrid TV Preview PR into `main`,
   then the v1 stable PR. This makes the stable release workflow accessible
   through the repository's Actions tab on the default branch.
3. Add persistent signing secrets described above.
4. In **Actions → Hybrid TV v1.0 Stable → Run workflow**, choose `main`.
   The `release-signed` job only runs for this **manual** dispatch,
   after the smoke tests pass. Download artifact
   `hybrid-tv-v1.0.0-persistently-signed`. Check v1.0.0, package ID and
   certificate SHA-256 fingerprint in the Actions build logs.
5. Never download the `hybrid-tv-v1-smoke-not-release` artifact for a
   production TV installation.

## Install without replacing the existing Home Photos package

Once the **persistently signed** APK is available, copy
`hybrid-tv-v1.0.0.apk` to your Windows Downloads folder. In Git Bash:

```bash
adb connect 192.168.68.55:5555
adb shell input keyevent KEYCODE_HOME
adb shell dumpsys window | grep mCurrentFocus
# Home Photos should still be focused.
adb install "$HOME/Downloads/hybrid-tv-v1.0.0.apk"
adb shell pm grant com.ansub.tvhome.hybrid.stable android.permission.READ_EXTERNAL_STORAGE
adb shell am start -W -n com.ansub.tvhome.hybrid.stable/com.ansub.tvhome.HybridActivity
```

Verify the slideshow, prayer times, weather, D-pad and app launching.
The stable package has no automatic access to Preview's private
Stremio session history. To capture new viewing data, optionally enable:

```bash
adb shell cmd notification allow_listener com.ansub.tvhome.hybrid.stable/com.ansub.tvhome.HybridSessionListener
```

**Privacy:** This is a broad Android permission. The app implementation
queries only Stremio media sessions and never reads notifications. Revoke
it if no longer needed.

## Make Hybrid TV the default, only after manual checks

Prefer the TCL system's **Settings → Apps → Default apps → Home app** if
available, choose **Hybrid TV**. If a Home chooser appears after pressing
the remote HOME key, select **Hybrid TV → Always**.

Alternatively use the normal Android package manager default-HOME choice,
not any commands to disable the factory Home or uninstall Home Photos:

```bash
adb shell cmd package set-home-activity com.ansub.tvhome.hybrid.stable/com.ansub.tvhome.HybridActivity
adb shell input keyevent KEYCODE_HOME
adb shell dumpsys window | grep mCurrentFocus
```

Expected foreground package:
`com.ansub.tvhome.hybrid.stable`. Reboot only once this succeeds.
After reboot run `adb shell dumpsys window | grep mCurrentFocus` again.
Some TCL firmware does not permit shell default-HOME selection; if so,
use its on-screen chooser or Settings instead. **Do not** disable the
factory launcher, Stremio, Home Photos or other apps.

### Exact rollback to permanent Home Photos

```bash
adb connect 192.168.68.55:5555
adb shell cmd package set-home-activity com.ansub.tvhome.photos.stable/com.ansub.tvhome.MainActivity
adb shell input keyevent KEYCODE_HOME
adb shell dumpsys window | grep mCurrentFocus
```

If TCL rejects the shell command, start Home Photos explicitly and choose
it again in the Home app selection screen:

```bash
adb shell am start -n com.ansub.tvhome.photos.stable/com.ansub.tvhome.MainActivity
```

No uninstall or clear-data step is needed for rollback.

## Release limitations worth tracking

- The verified offline Brunei-Muara timetable currently covers only
  **October–December 2026**. Outside that range the widget correctly
  reports unavailable instead of guessing prayer times. Official 2027
  source data must be added and verified before that range expires.
- YouTube cannot reliably expose playback progress on all app builds;
  Stremio's saved card is only **last observed**, not full cloud history.
- The signed production APK must be installed and checked on the real
  TCL before v1.0 can be described as fully deployed.
