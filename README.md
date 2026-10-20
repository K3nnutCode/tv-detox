<div align="center">

# TV Detox

### Make an old, slow Android TV fast again.

Claude Code or Codex does the work. You just press OK on the remote.

[![Release](https://img.shields.io/github/v/release/Ansub/tv-detox?color=black)](https://github.com/Ansub/tv-detox/releases/latest)
[![Tests](https://img.shields.io/github/actions/workflow/status/Ansub/tv-detox/build.yml?branch=main&label=tests)](https://github.com/Ansub/tv-detox/actions)
[![License](https://img.shields.io/badge/license-MIT-black)](LICENSE)
[![YouTube](https://img.shields.io/badge/YouTube-@ansubkhan-FF0000?logo=youtube&logoColor=white)](https://www.youtube.com/@ansubkhan)

<img src="docs/home-screen.png" alt="The TV Detox home screen" width="100%">

</div>

<table>
<tr>
<td width="33%" valign="top"><b>🧹 Removes the junk</b><br>Bloatware, ads and sponsored rows, gone.</td>
<td width="33%" valign="top"><b>📺 A calm home screen</b><br>Your apps, your HDMI inputs and a clock. Nothing else.</td>
<td width="33%" valign="top"><b>↩️ Undo anytime</b><br>Apps are turned off, never deleted.</td>
</tr>
</table>

## Watch it work

<a href="https://www.youtube.com/watch?v=fP2SGGVsaB0"><img src="https://img.youtube.com/vi/fP2SGGVsaB0/maxresdefault.jpg" alt="Watch: Claude Made My Old Android TV Feel Brand New" width="100%"></a>

**[▶ Claude Made My Old Android TV Feel Brand New](https://www.youtube.com/watch?v=fP2SGGVsaB0)** · the full setup on a real TV, in 4 minutes.

## Get started

**1. Turn on developer mode on the TV.**
Settings → Device Preferences → About → press **Build** 7 times. Then turn on **USB debugging** in **Developer options**. [Stuck?](#help)

**2. Open [Claude Code](https://claude.com/claude-code) or [Codex](https://openai.com/codex) on your laptop.**
The laptop and TV must be on the same Wi-Fi.

**3. Paste this in.**

```text
Help me clean up my Android TV and make it faster.
My TV is a <brand and model>.

1. Connect to my TV over Wi-Fi. Walk me through anything
   I need to tap on the TV.
2. Before changing anything, list every app in plain English:
   safe to turn off, needed by the TV, and apps I use.
   Check free memory.
3. Ask me before every change. Turn apps off instead of
   deleting them. Keep a list of what you changed and how
   to undo it.
4. Never turn off: Google Play services
   (com.google.android.gms), Google Play, Settings,
   System UI, setup apps, the keyboard, Bluetooth, my HDMI
   inputs, the TV's input and tuner framework, or anything
   else the TV needs to start. If you're not sure, leave
   it on. The one exception: if install.sh offers to turn
   off Google TV's setup app because it's taking over Home,
   ask me first.
5. Install the home screen: download
   https://github.com/Ansub/tv-detox and run its
   install.sh with my TV and these apps: <your apps>.
6. Turn off animations and the screensaver, and make the TV
   sleep after 5 minutes.
7. Wait 30 seconds after the last change, then restart the
   TV once and check it starts up normally before you say
   you're done. Show me free memory before
   and after.
```

That's it. It asks you before every change.

> [!NOTE]
> This is what worked for us, not professional advice. Use it at your own risk. You're responsible for your own TV, and I'm not liable if something goes wrong.

## Help

<details>
<summary><b>How do I turn on developer mode?</b></summary>
<br>

1. On the TV, open **Settings**, then **Device Preferences** (or **System**), then **About**.
2. Press OK on **Build** 7 times until you see "You are now a developer."
3. Go back, open **Developer options** and turn on **USB debugging**. If you see **Network debugging** or **Wireless debugging**, turn that on too.
4. The first time your laptop connects, the TV asks "Allow debugging?" Tick "Always allow" and press **Allow**.

Menus differ a bit by brand. If you're stuck, ask Claude or Codex: *"I have a \<brand and model\> TV. How can you connect to it from my laptop and take control of it?"*

</details>

<details>
<summary><b>Which TVs does it work on?</b></summary>
<br>

Android TV 9 (a real TV), 11 and 12, and Google TV 14 (also a real TCL TV, see [TCL notes](#tcl-notes)). Every change is tested automatically on emulators.

</details>

<details>
<summary><b>Can I do this on a Fire TV, Roku, Samsung or LG?</b></summary>
<br>

- **Amazon Fire TV:** partly. Fire TV is Android underneath, so the cleanup works. Amazon blocks changing the home screen on current Fire TVs, so you keep Amazon's. Tell Claude what you want to keep, like Alexa voice search on the remote.
- **Roku:** no. Roku runs its own system, not Android, so nothing can connect to it to turn things off.
- **Samsung (Tizen):** no, for the same reason.
- **LG (webOS):** no. You can still cut down the ads by turning off **Home Promotion** and **Content Recommendation** in the TV's home settings.

If your TV or streaming box runs Android TV or Google TV, it should work. That includes many Sony, TCL, Hisense, Philips and Xiaomi TVs, Chromecast with Google TV and Nvidia Shield. Some of those brands also sell Roku or other non-Android models, so check **Settings > About** if you're not sure.

</details>

<details>
<summary><b>What about projectors or Android phones?</b></summary>
<br>

- **Android projectors:** probably. If it has **Developer options** and **USB debugging**, the cleanup should work. Cheap projectors often run a custom Android, so the home screen may act a bit differently.
- **Android phones:** only the cleanup. The home screen is made for a TV remote, not touch.

</details>

<details>
<summary><b>Anything to watch out for on Chromecast with Google TV?</b></summary>
<br>

It runs the same Google TV this is tested on, so it should work. People who've done it report two things:

- The YouTube button on the remote can stop working, and some lose casting. Tell Claude to keep casting on if you use it.
- Google TV can bring its own home screen back. If that happens, ask Claude to turn off Google TV's home screen. Leave its setup app on unless it's taking over Home (see below).

</details>

<details>
<summary><b>Home opens Google TV's setup screen instead (Sony BRAVIA and others)</b></summary>
<br>

On some Google TVs, once Google TV's home screen (`com.google.android.apps.tv.launcherx`) is off, Android picks the setup app (`com.google.android.tungsten.setupwraith`) for Home instead. `install.sh` spots this and asks before turning the setup app off too. It only asks if Home opens exactly that app, Google TV's home screen is already off and the TV has finished its first setup. `YES=1` doesn't turn it off; use `YES_SETUP=1` for that. This worked on a Sony BRAVIA and a TCL Smart TV Pro (Google TV 14), but it hasn't been tested on every TV. `undo.sh` turns it back on.

To undo it by hand:

```sh
adb shell pm enable com.google.android.tungsten.setupwraith
adb shell pm enable com.google.android.apps.tv.launcherx
```

If the TV ever asks to be set up again, or a setup screen won't finish, turn the setup app back on first.

</details>

<a id="tcl-notes"></a>
<details>
<summary><b>TCL TVs: what worked, and what to keep</b></summary>
<br>

Tested on a TCL Smart TV Pro (Google TV, Android 14, 2.3 GB of RAM). Every step below was followed by a restart, and the TV started normally each time. Free memory went from about 660 MB to about 1 GB, though part of that is just from restarting: the first number was taken after the TV had been on for 15 days.

**Home screen.** With Google TV's home screen off, Home opened the setup app's recovery screen, as in [#16](https://github.com/Ansub/tv-detox/issues/16). Turning the setup app off too (`YES_SETUP=1`) made this home screen stick, through several restarts.

**Connect on port 5555.** If you connected with **Wireless debugging** (pairing code), turning the setup app off ends that connection halfway through `install.sh`. The script then says it couldn't turn the setup app off, even though it did. On this TV, the classic port also worked, so you can reconnect with `adb connect <TV_IP>:5555`, check with `adb shell pm list packages -d`, and run `install.sh` again with that address.

**Safe to turn off**, if you don't use them:

| App | Package |
|---|---|
| TCL account | `com.tcl.usercenter` |
| Built-in manual | `com.tcl.ocean.instructions` |
| TCL Channel (free channels with ads) | `com.tcl.channelplus` |
| TCL recommendations | `com.tcl.waterfall.overseas` |
| TCL notifications and promos | `com.tcl.messagebox` |
| Store demo | `com.tcl.esticker`, `com.tcl.exhibit` |
| Alexa built-in | `com.tcl.smartalexa` |
| Phone link | `com.tcl.magiconnectfree` |
| Repair guide, logs | `com.tcl.repairguide`, `com.tcl.logkit` |
| Screen mirroring | `com.tcl.miracast` |
| TCL analytics and Alexa hooks | `com.tcl.ttvs` |
| TCL interactive and ads service | `com.tcl.interactive` |
| Game bar for consoles | `com.tcl.gamebar` |
| Photo viewer, USB media player | `com.tcl.gallery`, `com.tcl.ui_mediaCenter` |
| App manager | `com.tcl.guard` |
| Bluetooth "listen alone" | `com.tcl.t_solo` |
| Google TV's screensaver (about 200 MB) | `com.google.android.apps.tv.dreamx` |

**Tiles that stay after the cleanup.** Two TCL tiles can still show up on this home screen. **TCL Channel** is `com.tcl.partnercustomizer`, which also handles the TCL button on the remote. **TCL Home** is `com.tcl.dashboard`, TCL's quick settings panel. Both turned off without trouble.

**Keep these on:**

- `com.tcl.tvinput`: the HDMI inputs and the tuner.
- `com.tcl.suspension`: the input picker.
- `com.tcl.inputmethod.international`: the TCL keyboard.
- `com.tcl.providers.config`: other TCL apps read their settings from it.
- `com.tcl.useragreement`: TCL's privacy switches.

`com.tcl.tv` is Live TV, the channel guide and channel scanning. Turn it off only if you never watch antenna or cable TV.

**Optional: the phone keyboard keeps popping up.** On this TV, Google TV's phone-remote service made its phone keyboard the TV's keyboard again after every restart. If that happens to you, first note which keyboard you have now:

```sh
adb shell settings get secure default_input_method
```

Then switch to the TCL keyboard and turn the phone-remote service off:

```sh
adb shell ime set com.tcl.inputmethod.international/.T_IME
adb shell pm disable-user --user 0 com.google.android.tv.remote.service
```

This also stops your phone from working as a remote: no typing, voice search or control from the Google TV or Google Home app. `undo.sh` doesn't undo it. To undo it by hand, turn the service back on and switch back to the keyboard you noted:

```sh
adb shell pm enable com.google.android.tv.remote.service
adb shell ime set <the keyboard you noted>
```

**The TCL logo at startup** is part of the TV's firmware, so it can't be turned off. On this TV, turning it off with the remote (not at the wall) let TCL's quick start wake it straight to the home screen, with no logo.

</details>

<details>
<summary><b>The Claude app says it can't reach my TV</b></summary>
<br>

The chat in the Claude app (and claude.ai) runs on Anthropic's servers, not on your laptop, so it can't see your TV. You need something that runs on your own laptop: [Claude Code](https://claude.com/claude-code) or [Codex](https://openai.com/codex).

Don't want to pay for either? Skip the AI and use `install.sh` instead (see "Can I install the home screen without AI?" below).

</details>

<details>
<summary><b>Can I do it from my phone, without a laptop?</b></summary>
<br>

Not yet. Claude has to run on a computer on the same network as your TV. A phone hotspot is fine as that network, as long as the laptop and TV both join it.

</details>

<details>
<summary><b>My TV is on Ethernet, not Wi-Fi</b></summary>
<br>

That works the same. Your laptop talks to the TV over its IP address on your home network, so cable or Wi-Fi doesn't matter. The laptop just has to be on the same network. You'll find the TV's IP in its network settings, or ask Claude to find it.

</details>

<details>
<summary><b>Can I just do the cleanup and keep my TV's home screen?</b></summary>
<br>

Yes. Leave out step 5 of the prompt ("Install the home screen"). Your TV looks the same, just with the junk turned off.

</details>

<details>
<summary><b>Why not just use Projectivy Launcher?</b></summary>
<br>

You can. The cleanup is the part that makes the TV faster, and it works with any home screen. This one is here if you want something tiny and open source: one screen, no ads, no account, about 30 MB of RAM.

</details>

<details>
<summary><b>The TV still opens Live TV or HDMI when it turns on</b></summary>
<br>

That's a TV setting, not this home screen. Ask it: *"My TV opens Live TV at startup instead of the home screen. Find that setting and turn it off."*

</details>

<details>
<summary><b>A button on the remote stopped working</b></summary>
<br>

Some remote buttons are handled by the TV's own home screen or by apps the cleanup turned off. To see which app handles which button, ask Claude: *"Run `adb shell dumpsys window policy` and tell me which apps my remote's global keys go to."* If it's an app other than your TV's home screen, turn that app back on.

**The YouTube button on some Google TVs** (seen on Xiaomi and Vu): it goes to Google TV's own home screen, which `install.sh` turns off, so the button does nothing. Turning that home screen back on brings the button back, but your TV may then open Google TV's home screen again instead of this one. Until there's a safe fix, open YouTube from its tile on the home screen.

</details>

<details>
<summary><b>An update turned something back on</b></summary>
<br>

```sh
sh check.sh <TV_IP>                  # check the home screen is still in charge
sh check.sh <TV_IP> --fix            # and fix it
sh check.sh <TV_IP> --fix my-tv.txt  # also check your own list of changes
```

Your list has one change per line, like `off com.example.app`, `global window_animation_scale 0` or `device_config activity_manager max_cached_processes 4`. Ask Claude to write it from the changes it made.

</details>

<details>
<summary><b>Can I install the home screen without AI?</b></summary>
<br>

Yes. You only need `adb`. On a Mac, run `brew install android-platform-tools`. On Windows, download Google's [SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools), unzip it, and run the commands below from Git Bash. Download this repo, then:

```sh
sh install.sh <TV_IP>                                               # picks your TV apps for you
sh install.sh <TV_IP> "com.netflix.ninja,com.hulu.livingroomplus"   # or choose them
sh install.sh <TV_IP> "" "bluetooth,sound,picture"    # add settings shortcuts
```

Most TVs keep opening their own home screen, so the script offers to turn that off. Nothing is uninstalled.

</details>

<details>
<summary><b>How do I change the apps on the home screen?</b></summary>
<br>

Use the remote. Hold **OK** on an app (or press **Menu**) to:

- **Move** it: use the arrows to slide it to a new spot, then press **OK** to finish.
- **Replace** it: pick another app to take its place.
- **Remove** it from the home screen. The app stays installed, and you can add it back any time.
- **Uninstall** it, for apps you installed yourself. The TV asks you to confirm first.
- **App info**, for apps that came with the TV. They can't be uninstalled, but from there you can turn them off.

To add an app, pick the **Add app** tile in the bottom row. Add as many apps as you like: they wrap into rows of six, and the page scrolls once there are more than twelve. Your changes are kept until you run `install.sh` with a new app list.

</details>

<details>
<summary><b>Can I get Wi-Fi and Bluetooth one press away?</b></summary>
<br>

Yes. A small network icon always sits next to the clock. It shows **Wi-Fi** (with signal bars), **Ethernet** or a crossed-out Wi-Fi when you're offline, and opens your network settings. **Bluetooth**, **Sound** and **Picture** shortcuts can join it. They're off unless you ask for them:

```sh
sh install.sh <TV_IP> "" "bluetooth,sound,picture"   # pick any of them, in any order
sh install.sh <TV_IP> "" "none"                                    # take them away
```

Press **Up** from your apps to reach them. A shortcut your TV has no screen for is left out. On TCL TVs, Sound and Picture open TCL's own panels.

</details>

<details>
<summary><b>How do I undo it?</b></summary>
<br>

```sh
sh undo.sh <TV_IP>
```

This turns your TV's own home screen back on and removes this one. To undo the cleanup too, ask Claude or Codex to reverse everything on its list.

</details>

<details>
<summary><b>My TV is stuck restarting</b></summary>
<br>

Your laptop can often still connect for a few seconds each time the TV starts. Use that to turn everything back on:

```sh
adb connect <TV_IP>
adb wait-for-device
adb shell pm list packages -d
```

That lists everything that's turned off. Turn each one back on with `adb shell pm enable <package>`, or all of them at once:

```sh
for p in $(adb shell pm list packages -d | tr -d '\r' | sed 's/package://'); do adb shell pm enable "$p"; done
```

To undo just the home screen, run `sh undo.sh <TV_IP>`. Then restart the TV. You can redo the cleanup later, one app at a time.

If the laptop never connects, use the TV's recovery mode or a factory reset. How you get there depends on the brand and model, so check your TV maker's support page.

</details>

<details>
<summary><b>For developers: build it yourself</b></summary>
<br>

1. Put your default apps in `apps.txt` (package names, one per line). You can also pass them to `install.sh` instead.
2. Install Java 17 and the Android SDK. On a Mac:
   ```sh
   brew install openjdk@17
   brew install --cask android-commandlinetools
   sdkmanager "platforms;android-34" "build-tools;34.0.0"
   ```
3. `sh build.sh`, then `sh install.sh <TV_IP>` installs your build instead of the release.
4. `sh ci/smoke-test.sh <TV_IP or emulator>` checks it all works.

HDMI inputs are found on their own. The labels follow the TV's language, with 16 languages built in. To add yours or fix a translation, edit `src/com/ansub/tvhome/Strings.java`. Keep `key.jks`, which is made on your first build: updates must be signed with the same key.

It's one screen of plain Java, no libraries, no Gradle. The background is drawn once, the only animation is the focus lift, and it uses about 30 MB of RAM.

</details>

### Photo slideshow on Android 11 / Android TV

Place JPG, PNG or WebP files in `/sdcard/Pictures/TVWallpapers/`. The home screen displays these behind the clock and tiles, changing images every 60 seconds.

For Android 11 (SDK 30), the installed app **must declare and be granted** `READ_EXTERNAL_STORAGE`. The home screen asks for read-only photo access once when the shared wallpaper folder exists. You can instead grant it from a connected PC after installing the updated APK:

```sh
adb shell pm grant com.ansub.tvhome android.permission.READ_EXTERNAL_STORAGE
adb shell am force-stop com.ansub.tvhome
adb shell input keyevent KEYCODE_HOME
```

Confirm that the installed APK actually contains the permission before attempting to grant it:

```sh
adb shell dumpsys package com.ansub.tvhome | grep -E 'versionCode=|READ_EXTERNAL_STORAGE'
adb shell ls -lh /sdcard/Pictures/TVWallpapers/
adb logcat -d -s TVHomePhotos:V
```

If the grant says **"has not requested permission"**, the TV is still running an APK built without the permission: rebuild and install the updated APK first.

**Preserve your existing home screen:** Install a rebuild only with the **same signing key** as the installed app (keep the original `key.jks` / release keystore). Use `adb install -r out/tvhome.apk` for an in-place update. If Android reports `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, **stop**: do not uninstall your home screen or run an install script that automatically uninstalls it, as that may reset app preferences and leave the TV without its configured Home app.

### Safe visual preview (keeps your existing Home Photos installation)

The **Home Photos Preview** app uses a third package, `com.ansub.tvhome.photos.preview`, so it can be installed alongside the original offline Home (`com.ansub.tvhome`) and the existing Home Photos (`com.ansub.tvhome.photos`). It is for testing the condition-specific weather icons and unified translucent app tiles with the same shared wallpaper folder.

1. Open the GitHub Actions run for this branch and download its **`home-photos-preview-apk`** artifact. Extract `home-photos-preview.apk`.
2. Install only the preview APK with `adb install -r home-photos-preview.apk` (use the correct local file path).
3. Open it without changing the default Home app:

```sh
adb shell pm grant com.ansub.tvhome.photos.preview android.permission.READ_EXTERNAL_STORAGE
adb shell am start -n com.ansub.tvhome.photos.preview/com.ansub.tvhome.MainActivity
```

To return immediately to the current Home Photos, use:

```sh
adb shell am start -n com.ansub.tvhome.photos/com.ansub.tvhome.MainActivity
```

Do **not** change your default Home to the preview. The CI-produced preview APK has temporary signing; later test builds may not install over it. You can uninstall **only** the preview if needed, after returning to the working Home Photos. Never uninstall `com.ansub.tvhome` or `com.ansub.tvhome.photos` for this visual test.

## Home Photos: separate photo launcher with live weather

**Home Photos** is a second Android TV home app, separate from the existing `com.ansub.tvhome`. Its package is `com.ansub.tvhome.photos`. It preserves the slideshow, rounded clock box, app tiles and settings shortcuts and adds a small weather panel at the upper-right.

- **Weather:** temperature and conditions for **Bandar Seri Begawan, Brunei**, using a **fixed city location**, not your device's location. Fetched from [Open-Meteo](https://open-meteo.com/) over HTTPS about every 30 minutes **while Home Photos is visible**. Free Open-Meteo API use is for non-commercial purposes. [Weather data: Open-Meteo.com](https://open-meteo.com/) (CC BY 4.0).
- **Network privacy:** the *only* network call in Home Photos source code is to the Open-Meteo weather endpoint. The manifest includes Android's general `INTERNET` permission, which cannot technically be limited to only one website by a manifest. No location permission, user account, advertising SDK or analytics. If the TV is offline, the most recent reading is labeled “saved.”
- **Photos:** Android 11's read-only storage permission lets the app display JPG, PNG or WebP images from `/sdcard/Pictures/TVWallpapers/` every 60 seconds. The clock gets a rounded translucent dark background when photos appear.

### Safe install: do NOT uninstall the existing home screen

From a computer with Java 17 and Android SDK platform and build-tools 34:

```sh
git fetch origin
git switch feature/home-photos-live-weather
sh build-photos.sh
adb connect 192.168.68.55:5555
adb install -r out-photos/home-photos.apk
adb shell am start -n com.ansub.tvhome.photos/com.ansub.tvhome.MainActivity
```

- The first build creates **`home-photos.jks`**. Back up this file in a private, safe location. Keep it and its password for all subsequent builds. Losing it means Android will reject in-place updates to the new Home Photos package.
- Only `build-photos.sh` produces the separately signed new APK. It never overwrites the original `out/tvhome.apk`.
- Use **`adb install -r`**, **not `install.sh`**. The original script targets the old launcher and may uninstall apps on signature mismatches. If `adb install` reports a signing conflict, STOP, do not uninstall anything.
- If the TV doesn't request photo permission automatically, use `adb shell pm grant com.ansub.tvhome.photos android.permission.READ_EXTERNAL_STORAGE` and reopen Home Photos.
- Weather will appear after the first successful HTTPS request, if the TV has internet access. See `adb logcat -d -s HomePhotosWeather:V TVHomePhotos:V` for diagnostics.
- The GitHub Actions artifact is for automated testing and uses an **ephemeral signing key** unless you separately configure persistent signing. **Do not use the CI-generated APK as your permanent installation** if you want to update Home Photos without reinstalling in the future.

When satisfied with the new app, you may choose it in Android TV Home settings or run:

```sh
adb shell cmd package set-home-activity com.ansub.tvhome.photos/com.ansub.tvhome.MainActivity
adb shell input keyevent KEYCODE_HOME
```

To switch back to the **still-installed** original launcher:

```sh
adb shell cmd package set-home-activity com.ansub.tvhome/com.ansub.tvhome.MainActivity
adb shell input keyevent KEYCODE_HOME
```

The second app keeps a **separate** app-shortcut configuration because it is a separate Android package. The `apps-photos.txt` defaults place **YouTube and Stremio** first, with other installed TV apps filling remaining positions. Any additional saved arrangements must be configured again in Home Photos. No existing app is removed or disabled by this build or installation method.

## Permanent Home Photos (safe migration)

The permanent launcher is `com.ansub.tvhome.photos.stable` (shown on TV as **Home Photos**). It has the current widescreen TV banners, stronger frosted clock/weather panels, larger weather icons and photo slideshow. The separate package avoids the lost-signing-key problem for the previously installed `com.ansub.tvhome.photos` test APK. Installing it **does not remove or overwrite any working launcher**.

### 1. Generate and protect a signing key (only once)

Install JDK 17 on your **own computer** if `keytool` is not available. From Git Bash inside `~/tv-detox` run:

```sh
keytool -genkeypair -alias homephotosstable -keyalg RSA -keysize 3072 -validity 10000 -storetype PKCS12 -keystore home-photos-stable.jks -dname "CN=Home Photos"
```

Choose a strong private password when prompted; **never share the password or private key in chat, issues, commits or screenshots**. Keep at least two **secure private backups** of `home-photos-stable.jks` and your password. Losing the key will prevent future in-place updates.

For a GitHub Actions build (so Windows does not need the Android SDK), copy the key as base64 to the clipboard using Git Bash:

```sh
base64 -w 0 home-photos-stable.jks | clip
```

In GitHub, open **repository Settings → Secrets and variables → Actions**, then add two *repository secrets*:

- `HOME_PHOTOS_RELEASE_KEYSTORE_B64`: paste the base64 contents from your clipboard
- `HOME_PHOTOS_RELEASE_KEYSTORE_PASS`: your private keystore password

Do not commit these values. The workflow keeps the key out of build artifacts and refuses to produce a permanent APK if either secret is missing. The private key remains your responsibility to back up. Use a private computer, and clear the clipboard afterward.

### 2. Produce a permanently signed APK

After the reviewed PRs containing this workflow are merged into the repository's **default branch**, open **GitHub → Actions → Permanent Home Photos APK → Run workflow**. Download the **`home-photos-permanent-signed-apk`** artifact from the successful run and extract `home-photos-stable.apk`.

Unlike the regular PR preview artifacts, this APK is signed with your **persistent** key from the GitHub repository secrets. The workflow runs only when manually started and does **not** run on pull requests.

### 3. Install and test without changing the default Home

```sh
adb connect 192.168.68.55:5555
adb install -r "$HOME/Downloads/home-photos-stable.apk"
adb shell pm grant com.ansub.tvhome.photos.stable android.permission.READ_EXTERNAL_STORAGE
adb shell am start -n com.ansub.tvhome.photos.stable/com.ansub.tvhome.MainActivity
```

Check the full photo rotation, clock, weather icon, app launch and HDMI switching before changing Home. If any command fails, **stop**. The old apps should remain usable.

### 4. Make permanent only after it works

```sh
adb shell cmd package set-home-activity com.ansub.tvhome.photos.stable/com.ansub.tvhome.MainActivity
adb shell input keyevent KEYCODE_HOME
adb shell dumpsys window | grep mCurrentFocus
```

Confirm the focus shows `com.ansub.tvhome.photos.stable`. Only then, and only when you wish to tidy up the Home chooser, uninstall the **two temporary photo launchers**:

```sh
adb uninstall com.ansub.tvhome.photos.preview
adb uninstall com.ansub.tvhome.photos
```

Keep the original `com.ansub.tvhome` installed as a fallback. Any app arrangement saved in the two temporary photo apps is not automatically transferred to the permanent package; its default shortcuts include YouTube and Stremio.

**Important:** Do not use the old `install.sh` script, do not uninstall the original backup launcher, and never use a CI artifact signed with an ephemeral key as the permanent launcher. Future permanent updates must use this **same** keystore and increase the stable manifest versionCode.

## Contributing

Pull requests are welcome for fixes that keep working on every TV. This project sticks to official apps and Android's own settings, so changes that add ad blockers or unofficial apps won't be merged. If you want those, fork it and make it yours.

---

<div align="center">

Made by **Ansub Khan**. I build things like this with AI every week.

[**Watch on YouTube →**](https://www.youtube.com/@ansubkhan)

<sub>MIT License</sub>

</div>
