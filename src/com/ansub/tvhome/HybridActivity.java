package com.ansub.tvhome;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.InputDevice;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Isolated Smart TV hybrid prototype. No HOME intent, no access to permanent
 * Home Photos preferences, and no writes to the shared photo directory.
 * CleanYourTV-inspired TV dock + the existing Home Photos weather widget.
 *
 * Prayer schedule: OFFLINE Brunei-Muara 2026 verified Oct-Dec rows only.
 * Out-of-range days NEVER show fabricated or estimated prayer times.
 */
public final class HybridActivity extends Activity {
    private static final String STREMIO = "com.stremio.one";
    private static final String YOUTUBE = "com.google.android.youtube.tv";
    private static final int PHOTO_REQUEST = 31;
    private static final long SLIDE_MS = 60000L;
    private static final long GREETING_REFRESH_MS = 60000L;
    private static final long PRAYER_REFRESH_MS = 30000L;
    private static final TimeZone BRUNEI_ZONE = TimeZone.getTimeZone("Asia/Brunei");
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<View> dockTiles = new ArrayList<>();
    private final ArrayList<File> photos = new ArrayList<>();
    private TvWallpaperView photo;
    private ImageView heroArtwork;
    private TextView heading, details, footnote, greetingView, heroEyebrow, heroAction;
    private TextView prayerNextView, prayerPeriodView, prayerSourceView;
    private final TextView[] prayerTimeViews = new TextView[5];
    private final LinearLayout[] prayerTiles = new LinearLayout[5];
    private ProgressBar playbackProgress;
    private FrameLayout hero;
    private final Runnable greetingTick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            updateGreeting();
            ui.postDelayed(this, GREETING_REFRESH_MS);
        }
    };
    private final Runnable prayerTick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            updatePrayerPanel();
            ui.postDelayed(this, PRAYER_REFRESH_MS);
        }
    };
    private FrameLayout stageView;
    private HybridWeatherWidget weather;
    private boolean active;
    private static long lastListenerRebindElapsed;
    private int generation;
    private int index = -1;
    private float scale;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        FrameLayout stage = new FrameLayout(this);
        stageView = stage;
        stage.setBackgroundColor(0xFF0A1222);

        // Two crossfading frames preserve EACH photo's original aspect ratio.
        // Landscape uses center-crop, portrait/4:3 gets a blurred fill behind
        // its fully visible fit-centered image. Never stretch a family photo.
        photo = new TvWallpaperView(this);
        stage.addView(photo, new FrameLayout.LayoutParams(-1,-1));


        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xA90C1320, 0x390D1723, 0x00000000}));
        FrameLayout.LayoutParams scrim = new FrameLayout.LayoutParams(-1, px(730), Gravity.BOTTOM);
        stage.addView(shade, scrim);


        // One quiet shared glass ribbon replaces three visually heavy panels.
        // Individual information sections have no separate tinted card backing.
        View ribbon = new View(this);
        ribbon.setBackground(round(0xAC26323F, 30, 0x42FFFFFF));
        stage.addView(ribbon, pos(px(54), px(44), px(1812), px(160)));
        for (int x : new int[]{405, 1435}) {
            View separator = new View(this);
            separator.setBackgroundColor(0x55FFFFFF);
            stage.addView(separator, pos(px(x), px(67), px(1), px(111)));
        }

        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        clock.setGravity(Gravity.CENTER_VERTICAL);
        clock.setPadding(px(6), px(5), px(9), px(5));
        greetingView = text("GOOD NIGHT", 16, 0xD4FFFFFF, false);
        updateGreeting();
        clock.addView(greetingView);
        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("HH:mm");
        time.setTimeZone("Asia/Brunei");
        time.setTextColor(Color.WHITE);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(76));
        time.setIncludeFontPadding(false);
        clock.addView(time);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
        date.setTimeZone("Asia/Brunei");
        date.setTextColor(0xDDFFFFFF);
        date.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(19));
        clock.addView(date);
        stage.addView(clock, pos(px(82), px(52), px(298), -2));

        // Live current/next prayer panel, with immutable local PDI timetable.
        // Outside verified 2026-10 .. 2026-12, deliberately show no times.
        LinearLayout prayers = new LinearLayout(this);
        prayers.setOrientation(LinearLayout.VERTICAL);
        prayers.setPadding(px(12), px(5), px(12), px(5));
        // The shared ribbon supplies the glass background.

        LinearLayout prayerHeading = new LinearLayout(this);
        prayerHeading.setGravity(Gravity.CENTER_VERTICAL);
        prayerHeading.setOrientation(LinearLayout.HORIZONTAL);
        TextView prayerTitle = text("☪  WAKTU SOLAT · BRUNEI–MUARA", 20, Color.WHITE, true);
        prayerTitle.setSingleLine(true);
        prayerHeading.addView(prayerTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        prayerNextView = text("Checking timetable", 16, 0xFFF2D9A1, true);
        prayerNextView.setSingleLine(true);
        prayerNextView.setGravity(Gravity.RIGHT);
        prayerHeading.addView(prayerNextView, new LinearLayout.LayoutParams(0, -2, 1f));
        prayers.addView(prayerHeading);

        LinearLayout prayerRow = new LinearLayout(this);
        prayerRow.setOrientation(LinearLayout.HORIZONTAL);
        prayerRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, px(70));
        rp.topMargin = px(3);
        prayers.addView(prayerRow, rp);
        for (int i = 0; i < 5; i++) {
            LinearLayout cell = new LinearLayout(this);
            cell.setOrientation(LinearLayout.VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(px(3), px(1), px(3), px(1));
            TextView name = text(PrayerSchedule2026.PRAYERS[i], 17, 0xFFE5EBF5, false);
            name.setGravity(Gravity.CENTER);
            cell.addView(name);
            TextView value = text("--:--", 24, Color.WHITE, true);
            value.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(-1, -2);
            vp.topMargin = px(2);
            cell.addView(value, vp);
            prayerTiles[i] = cell;
            prayerTimeViews[i] = value;
            LinearLayout.LayoutParams cellParams = new LinearLayout.LayoutParams(0, -1, 1f);
            cellParams.leftMargin = px(3);
            cellParams.rightMargin = px(3);
            prayerRow.addView(cell, cellParams);
        }

        LinearLayout prayerFooter = new LinearLayout(this);
        prayerFooter.setOrientation(LinearLayout.HORIZONTAL);
        prayerPeriodView = text("", 14, 0xFFF0D9A9, true);
        prayerPeriodView.setSingleLine(true);
        prayerFooter.addView(prayerPeriodView, new LinearLayout.LayoutParams(0, -2, 1f));
        prayerSourceView = text("2026 PDI · Offline · Asia/Brunei", 13, 0xFFD2DAE6, false);
        prayerSourceView.setGravity(Gravity.RIGHT);
        prayerSourceView.setSingleLine(true);
        prayerFooter.addView(prayerSourceView, new LinearLayout.LayoutParams(0, -2, 1f));
        prayers.addView(prayerFooter);
        stage.addView(prayers, pos(px(430), px(52), px(980), -2));
        updatePrayerPanel();

        weather = new HybridWeatherWidget(this, scale, true);
        weather.setBackgroundColor(Color.TRANSPARENT);
        FrameLayout.LayoutParams wp = new FrameLayout.LayoutParams(px(380), -2, Gravity.TOP | Gravity.RIGHT);
        wp.topMargin = px(57);
        wp.rightMargin = px(85);
        stage.addView(weather, wp);

        // FEATURED HERO: reference layout at 1920x1080.
        // Approx. x=68..1378, y=275..810, leaving 30px before the dock.
        // Preserve the v0.3.1 hero frame at 1310x536 (1920x1080 layout).
        // Stremio's thumbnail is typically 16:9 (455x256 in our TCL test).
        // Cropping that image across the 2.44:1 whole hero discarded its top
        // and bottom. Instead, position a near-16:9 viewport on the RIGHT:
        // it keeps the character and most of the composition visible.
        hero = new FrameLayout(this);
        hero.setBackground(round(0xCE242E39, 30, 0x66FFFFFF));
        hero.setClipToOutline(true);
        // Media art belongs exclusively inside the selected-app hero card.
        // The underlying TvWallpaperView keeps cycling personal photos and is
        // never replaced by Stremio or YouTube thumbnails.
        heroArtwork = new ImageView(this);
        heroArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        heroArtwork.setVisibility(View.GONE);
        FrameLayout.LayoutParams artParams = new FrameLayout.LayoutParams(
                px(446), px(260), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        artParams.rightMargin = px(21);
        heroArtwork.setBackground(round(0xFF303A47, 17, 0x20FFFFFF));
        heroArtwork.setClipToOutline(true);
        hero.addView(heroArtwork, artParams);

        // Heavy contrast strictly behind left-side metadata; gentle fade into
        // the actual episode artwork at the right. Never add a flat dark tint
        // directly to the image itself or use an enlarged thumbnail square.
        View heroShade = new View(this);
        heroShade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {
                    0xEF1B2532,  // readable title without a black slab
                    0xD51B2532,
                    0x9D1B2532,
                    0x171B2532,
                    0x001B2532   // photo and art remain distinct
                }));
        hero.addView(heroShade, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout writing = new LinearLayout(this);
        writing.setOrientation(LinearLayout.VERTICAL);
        writing.setGravity(Gravity.CENTER_VERTICAL);
        writing.setPadding(px(30), px(18), px(10), px(18));
        heroEyebrow = text("CONTINUE WATCHING", 19, 0xFFD8E1F0, true);
        heroEyebrow.setLetterSpacing(0.085f);
        writing.addView(heroEyebrow);

        heading = text("", 52, Color.WHITE, true);
        heading.setMaxLines(2);
        heading.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = px(23);
        writing.addView(heading, titleParams);

        details = text("", 25, 0xFFEBF0F7, false);
        details.setMaxLines(2);
        details.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, -2);
        detailsParams.topMargin = px(9);
        writing.addView(details, detailsParams);

        playbackProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        playbackProgress.setMax(1000);
        playbackProgress.setProgress(0);
        playbackProgress.setProgressTintList(ColorStateList.valueOf(0xFF94C8FF));
        playbackProgress.setProgressBackgroundTintList(ColorStateList.valueOf(0x70FFFFFF));
        playbackProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(-1, px(6));
        trackParams.topMargin = px(20);
        writing.addView(playbackProgress, trackParams);

        footnote = text("", 19, 0xFFE1E6F0, false);
        footnote.setMaxLines(2);
        footnote.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams footnoteParams = new LinearLayout.LayoutParams(-1, -2);
        footnoteParams.topMargin = px(9);
        writing.addView(footnote, footnoteParams);

        heroAction = text("▶  Press OK to open", 20, Color.WHITE, true);
        heroAction.setGravity(Gravity.CENTER_VERTICAL);
        heroAction.setPadding(px(18), px(10), px(18), px(10));
        heroAction.setBackground(round(0x9A345477, 18, 0xA0B1D5FF));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-2, -2);
        actionParams.topMargin = px(17);
        writing.addView(heroAction, actionParams);

        // Keep text within the left half, leaving the right half visually cinematic.
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                px(611), -1, Gravity.LEFT | Gravity.TOP);
        hero.addView(writing, textParams);
        hero.setVisibility(View.GONE);
        stage.addView(hero, pos(px(74), px(275), px(1065), px(412)));

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        dock.setPadding(px(18), px(15), px(18), px(15));
        dock.setBackground(round(0xB5293542, 33, 0x56FFFFFF));
        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(-1, px(165), Gravity.BOTTOM);
        dockParams.leftMargin = px(164);
        dockParams.rightMargin = px(164);
        dockParams.bottomMargin = px(51);
        stage.addView(dock, dockParams);
        buildDock(dock);

        setContentView(stage);
        // Preserve the photo-only landing screen until the first remote arrow.
        // Some Android TV remotes cannot move focus out of this root FrameLayout
        // automatically, so dispatchKeyEvent below manages dock focus explicitly.
        stage.setFocusable(true);
        stage.setFocusableInTouchMode(true);
        stage.requestFocus();
        showIdle();
    }

    /**
     * TCL / Android TV D-pad navigation:
     * DOWN or RIGHT from idle enters the dock; LEFT/RIGHT moves between tiles;
     * UP returns to the unobstructed family-photo view; OK launches focused item.
     * Never consume BACK, HOME, volume, or other system keys.
     */
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        final int code = event.getKeyCode();
        final InputDevice source = event.getDevice();
        final String remoteName = source == null ? "" : source.getName();
        final boolean tclRemote = "BT_RC813_MA".equals(remoteName);

        // TCL BT_RC813_MA emits Linux KEY_FORWARDMAIL (233), mapped by
        // Generic.kl to vendor TCL_SET. This is a *scan code*, NOT Android
        // KEYCODE_233. Handle only this remote and scan code, and only when
        // the firmware actually delivers the event to the application.
        if (tclRemote && event.getScanCode() == 233) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                Log.i("HybridTclKeys", "TCL_SET delivered: androidKeyCode=" + code
                        + " scanCode=" + event.getScanCode());
                if (event.getRepeatCount() == 0) openTvSettings();
            }
            return true;
        }
        // Diagnostics for other non-navigation TCL remote keys, in case
        // OEM firmware changes the scan-code mapping on this model.
        if (tclRemote && event.getAction() == KeyEvent.ACTION_DOWN
                && code != KeyEvent.KEYCODE_DPAD_UP
                && code != KeyEvent.KEYCODE_DPAD_DOWN
                && code != KeyEvent.KEYCODE_DPAD_LEFT
                && code != KeyEvent.KEYCODE_DPAD_RIGHT
                && code != KeyEvent.KEYCODE_DPAD_CENTER
                && code != KeyEvent.KEYCODE_ENTER
                && code != KeyEvent.KEYCODE_VOLUME_UP
                && code != KeyEvent.KEYCODE_VOLUME_DOWN) {
            Log.i("HybridTclKeys", "Remote key delivered: androidKeyCode=" + code
                    + " scanCode=" + event.getScanCode()
                    + " device=" + remoteName);
        }
        if (dockTiles.isEmpty()) return super.dispatchKeyEvent(event);
        final boolean direction = code == KeyEvent.KEYCODE_DPAD_LEFT
                || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_UP
                || code == KeyEvent.KEYCODE_DPAD_DOWN;
        final boolean select = code == KeyEvent.KEYCODE_DPAD_CENTER
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
        // The vendor may intercept hardware Settings/Input keys before they
        // reach the Home app. This handles them whenever Android delivers them.
        if (code == KeyEvent.KEYCODE_SETTINGS || code == KeyEvent.KEYCODE_MENU
                || code == KeyEvent.KEYCODE_TV_INPUT) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (code == KeyEvent.KEYCODE_TV_INPUT) showTvInputSelector();
                else openTvSettings();
            }
            return true;
        }
        if (!direction && !select) return super.dispatchKeyEvent(event);

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            int at = focusedDockIndex();
            if (code == KeyEvent.KEYCODE_DPAD_UP) {
                if (stageView != null) stageView.requestFocus();
                showIdle();
            } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (at < 0) dockTiles.get(0).requestFocus();
            } else if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
                dockTiles.get(at < 0 ? 0 : Math.max(0, at - 1)).requestFocus();
            } else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                dockTiles.get(at < 0 ? 0 : Math.min(dockTiles.size() - 1, at + 1)).requestFocus();
            } else if (select && event.getRepeatCount() == 0) {
                if (at >= 0) dockTiles.get(at).performClick();
                else dockTiles.get(0).requestFocus();
            }
        }
        return true; // Also consume key-UP so OK cannot trigger twice.
    }

    private int focusedDockIndex() {
        View selected = getCurrentFocus();
        for (int i = 0; i < dockTiles.size(); i++) {
            if (dockTiles.get(i) == selected || dockTiles.get(i).hasFocus()) return i;
        }
        return -1;
    }

    private void showIdle() {
        if (hero != null) hero.setVisibility(View.GONE);
        if (playbackProgress != null) playbackProgress.setVisibility(View.GONE);
        if (heroArtwork != null) {
            heroArtwork.setVisibility(View.GONE);
            heroArtwork.setImageDrawable(null);
        }
    }

    private void buildDock(LinearLayout dock) {
        PackageManager pm = getPackageManager();
        List<String> preferred = Arrays.asList(YOUTUBE, STREMIO);
        List<String> apps = new ArrayList<>();
        for (String pkg : preferred) if (launch(pm, pkg) != null) apps.add(pkg);
        Intent tvQuery = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo ri : pm.queryIntentActivities(tvQuery, 0)) {
            String pkg = ri.activityInfo.packageName;
            if (apps.size() >= 4) break;
            if (pkg.equals(getPackageName()) || pkg.startsWith("com.ansub.tvhome")
                    || pkg.equals("com.gathin.tvhome") || pkg.contains("launcher")
                    || apps.contains(pkg)) continue;
            apps.add(pkg);
        }
        for (String pkg : apps) {
            final Intent intent = launch(pm, pkg);
            if (intent == null) continue;
            String label = pkg;
            try { label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString(); }
            catch (Exception ignored) { }
            // Use the ORIGINAL banner published by the installed Android TV app.
            // Do not replace logos/wordmarks with recreated vector branding.
            Drawable banner = null;
            try { banner = pm.getActivityBanner(intent); } catch (Exception ignored) { }
            if (banner == null) {
                try { banner = pm.getApplicationBanner(pkg); } catch (Exception ignored) { }
            }
            Drawable icon = null;
            if (banner == null) {
                try { icon = pm.getApplicationIcon(pkg); } catch (Exception ignored) { }
                if (icon == null) {
                    try { icon = pm.getActivityIcon(intent); } catch (Exception ignored) { }
                }
            }
            addDockTile(dock, pkg, label, banner, icon,
                    () -> { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); });
        }
        addDockTile(dock, "inputs", "Inputs", null, null, this::showTvInputSelector);
        addDockTile(dock, "settings", "Settings", null, null, this::openTvSettings);
    }

    /** Original Android TV app banner, fitted to the unchanged TV dock tile. */
    private void addDockTile(LinearLayout dock, String pkg, String label,
                             Drawable banner, Drawable appIcon, Runnable activate) {
        final LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setClipToOutline(true);
        // Preserve names to screen readers, but never put a caption below art.
        tile.setContentDescription("Hybrid dock: " + label);
        tile.setTag(pkg);
        // Cache drawables once, not on every remote focus transition.
        // The TCL GPU should animate transform/elevation, not allocate glass.
        final Drawable restingGlass = dockSurface(pkg, false);
        final Drawable focusedGlass = dockSurface(pkg, true);
        final Drawable focusRing = dockFocusOutline();
        tile.setBackground(restingGlass);
        // Focus outline lives only in the foreground. Do NOT paint a blue
        // background stroke as well, or TCL shows two offset rectangles.
        tile.setForeground(null);

        if (banner != null) {
            // Fill the full landscape card with the actual Android TV banner.
            // CENTER_CROP maintains the native proportions (unlike FIT_XY)
            // while removing the letterbox bars caused by FIT_CENTER.
            // Keep a 2px image inset. The selected border is overlaid
            // independently on top of the artwork (not behind the banner).
            ImageView artwork = new ImageView(this);
            artwork.setImageDrawable(banner);
            artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
            artwork.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            tile.setPadding(px(2), px(2), px(2), px(2));
            tile.addView(artwork, new LinearLayout.LayoutParams(-1, -1));
        } else if ("inputs".equals(pkg) || "settings".equals(pkg)) {
            // These are system controls, not app logos: simple TV-style glyphs.
            ImageView system = new ImageView(this);
            system.setImageDrawable(new LineIcon("inputs".equals(pkg)
                    ? LineIcon.HDMI : LineIcon.SETTINGS));
            system.setScaleType(ImageView.ScaleType.FIT_CENTER);
            system.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            tile.addView(system, new LinearLayout.LayoutParams(px(55), px(55)));
            TextView caption = text(label, 19, 0xE8FFFFFF, true);
            caption.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, -2);
            cp.topMargin = px(5);
            tile.addView(caption, cp);
        } else {
            // TV-shaped fallback for apps with NO Leanback banner:
            // a horizontal brand-mark + wordmark INSIDE the tile.
            // Never enlarge a mobile square icon to fill the whole TV card.
            LinearLayout brandRow = new LinearLayout(this);
            brandRow.setGravity(Gravity.CENTER);
            brandRow.setOrientation(LinearLayout.HORIZONTAL);
            brandRow.setPadding(px(12), px(8), px(12), px(8));
            if (YOUTUBE.equals(pkg)) {
                TextView play = text("▶", 34, Color.WHITE, true);
                play.setGravity(Gravity.CENTER);
                play.setBackground(round(0xFFFF2025, 13, 0x00FFFFFF));
                brandRow.addView(play, new LinearLayout.LayoutParams(px(67), px(51)));
            } else if (STREMIO.equals(pkg)) {
                TextView logo = text("◆", 62, 0xFF9C83FF, true);
                logo.setGravity(Gravity.CENTER);
                brandRow.addView(logo, new LinearLayout.LayoutParams(px(65), px(75)));
            } else if (appIcon != null) {
                ImageView smallMark = new ImageView(this);
                smallMark.setImageDrawable(appIcon);
                smallMark.setScaleType(ImageView.ScaleType.FIT_CENTER);
                smallMark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                brandRow.addView(smallMark, new LinearLayout.LayoutParams(px(64), px(64)));
            } else {
                ImageView generic = new ImageView(this);
                generic.setImageDrawable(new LineIcon(LineIcon.PLAY));
                generic.setScaleType(ImageView.ScaleType.FIT_CENTER);
                generic.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                brandRow.addView(generic, new LinearLayout.LayoutParams(px(60), px(60)));
            }
            TextView wordmark = text(STREMIO.equals(pkg) ? "STREMIO" : label,
                    (YOUTUBE.equals(pkg) || STREMIO.equals(pkg)) ? 31 : 27,
                    Color.WHITE, true);
            wordmark.setSingleLine(true);
            wordmark.setEllipsize(TextUtils.TruncateAt.END);
            wordmark.setGravity(Gravity.CENTER_VERTICAL);
            wordmark.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams wordParams = new LinearLayout.LayoutParams(0, -2, 1f);
            wordParams.leftMargin = px(12);
            brandRow.addView(wordmark, wordParams);
            tile.addView(brandRow, new LinearLayout.LayoutParams(-1, -1));
        }

        tile.setOnFocusChangeListener((v, focused) -> {
            tile.setBackground(focused ? focusedGlass : restingGlass);
            // One rounded highlight only; original TV banner stays unchanged.
            tile.setForeground(focused ? focusRing : null);
            tile.setElevation(focused ? px(12) : 0);
            tile.animate().scaleX(focused ? 1.06f : 1f)
                    .scaleY(focused ? 1.06f : 1f).setDuration(180).start();
            if (focused) showSelection(pkg, label);
        });
        tile.setOnClickListener(v -> {
            try { activate.run(); } catch (Exception e) {
                footnote.setText("App unavailable on this TV");
            }
        });
        dockTiles.add(tile);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, px(129), 1f);
        tp.leftMargin = px(8);
        tp.rightMargin = px(8);
        dock.addView(tile, tp);
    }

    /**
     * A SINGLE focus stroke, drawn after the banner, inset enough that every
     * pixel of its rounded corners fits inside TCL's rounded tile clip.
     */
    private Drawable dockFocusOutline() {
        GradientDrawable stroke = new GradientDrawable();
        stroke.setColor(Color.TRANSPARENT);
        stroke.setCornerRadius(px(18));
        stroke.setStroke(px(2), 0xFFB6D9FF);
        return new InsetDrawable(stroke, px(3));
    }

    /** Dark glossy backing; focus outline also overlays the child TV banner. */
    private GradientDrawable dockSurface(String pkg, boolean focused) {
        int top = 0xEA26354D;
        int bottom = 0xE5172236;
        if (STREMIO.equals(pkg)) {
            top = 0xEA373159;
            bottom = 0xE51A1C38;
        } else if (YOUTUBE.equals(pkg)) {
            top = 0xEA3C3441;
            bottom = 0xE5222533;
        } else if ("inputs".equals(pkg) || "settings".equals(pkg)) {
            top = 0xEA343C47;
            bottom = 0xE5202836;
        }
        if (focused) {
            top = 0xE748617E;
            bottom = 0xDA354863;
        }
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        background.setCornerRadius(px(21));
        // IMPORTANT: never draw a selected background border here.
        // It sits UNDER the native banner and creates the second, cut-off
        // line seen on TCL TV. Selected focus is foreground-only.
        if (!focused) background.setStroke(px(1), 0x35FFFFFF);
        return background;
    }

    private static final class SourceEntry {
        final TvInputInfo info;
        final String label;
        final int state;
        SourceEntry(TvInputInfo info, String label, int state) {
            this.info = info;
            this.label = label;
            this.state = state;
        }
    }

    private String sourceLabel(TvInputInfo input) {
        CharSequence custom = null;
        CharSequence systemLabel = null;
        try { custom = input.loadCustomLabel(this); }
        catch (RuntimeException ignored) { }
        try { systemLabel = input.loadLabel(this); }
        catch (RuntimeException ignored) { }
        String label = custom != null && custom.length() > 0 ? custom.toString()
                : systemLabel != null ? systemLabel.toString() : "";
        if (label.isEmpty()) {
            label = input.getType() == TvInputInfo.TYPE_HDMI ? "HDMI"
                    : input.getType() == TvInputInfo.TYPE_COMPOSITE ? "AV"
                    : "TV source";
        }
        return label;
    }

    private int sourceSortKey(SourceEntry item) {
        if (item.info.getType() == TvInputInfo.TYPE_HDMI) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                    "(?i)HDMI\\s*[-#:]?\\s*(\\d+)").matcher(item.label);
            if (m.find()) {
                try { return Integer.parseInt(m.group(1)); }
                catch (NumberFormatException ignored) { }
            }
            return 50;
        }
        return item.info.getType() == TvInputInfo.TYPE_COMPOSITE ? 100 : 200;
    }

    private String sourceStatus(int state) {
        if (state == TvInputManager.INPUT_STATE_CONNECTED) return "Reported connected";
        if (state == TvInputManager.INPUT_STATE_CONNECTED_STANDBY) return "Standby";
        if (state == TvInputManager.INPUT_STATE_DISCONNECTED) return "Reported disconnected";
        return "Status unknown";
    }

    private int sourceStatusColor(int state) {
        if (state == TvInputManager.INPUT_STATE_CONNECTED) return 0xFF55D58A;
        if (state == TvInputManager.INPUT_STATE_CONNECTED_STANDBY) return 0xFFFFCA70;
        return 0xFF9DA6B8; // Grey for disconnected OR unknown, not false red.
    }

    /**
     * Show actual TvInputInfo records, never guessed HDMI ids. TCL exposes
     * four physical HDMI ports in dumpsys, but a cable status of 0 means
     * UNKNOWN rather than physically unplugged. Public TvInputManager state
     * may differ, so labels explicitly say "Reported".
     */
    private void showTvInputSelector() {
        TvInputManager manager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        ArrayList<SourceEntry> sources = new ArrayList<>();
        if (manager != null) {
            try {
                for (TvInputInfo input : manager.getTvInputList()) {
                    if (!input.isPassthroughInput()) continue;
                    String label = sourceLabel(input);
                    // Hide tuner/app inputs; show hardware HDMI/AV and inputs
                    // which TCL itself labels HDMI/AV even if type is generic.
                    String upper = label.toUpperCase(Locale.US);
                    boolean physical = input.getType() == TvInputInfo.TYPE_HDMI
                            || input.getType() == TvInputInfo.TYPE_COMPOSITE
                            || input.getType() == TvInputInfo.TYPE_COMPONENT
                            || upper.startsWith("HDMI") || upper.equals("AV");
                    if (!physical) continue;
                    int state;
                    try { state = manager.getInputState(input.getId()); }
                    catch (RuntimeException ignored) { state = -1; }
                    sources.add(new SourceEntry(input, label, state));
                }
            } catch (RuntimeException ignored) {
                // Some OEMs do not publish TV input records to third parties.
            }
        }
        Collections.sort(sources, (a,b) -> {
            int sort = Integer.compare(sourceSortKey(a), sourceSortKey(b));
            return sort != 0 ? sort : a.label.compareToIgnoreCase(b.label);
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(px(24), px(8), px(24), px(6));
        TextView hint = text("Green: reported connected  ·  Amber: standby  ·  Grey: disconnected/unknown",
                21, 0xFFD7DFEC, false);
        hint.setPadding(0, px(6), 0, px(20));
        content.addView(hint);

        final View[] first = new View[1];
        final AlertDialog[] holder = new AlertDialog[1];
        for (SourceEntry source : sources) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(px(18), px(13), px(18), px(13));
            row.setFocusable(true);
            row.setClickable(true);
            row.setBackground(round(0xEC29354C, 16, 0x55FFFFFF));
            if (first[0] == null) first[0] = row;
            TextView title = text(source.label, 28, Color.WHITE, true);
            title.setSingleLine(true);
            row.addView(title, new LinearLayout.LayoutParams(0,-2,1f));
            TextView status = text("●  " + sourceStatus(source.state), 20,
                    sourceStatusColor(source.state), true);
            status.setGravity(Gravity.RIGHT);
            row.addView(status, new LinearLayout.LayoutParams(-2,-2));
            row.setOnFocusChangeListener((v,focused) -> row.setBackground(
                    round(focused ? 0xFF394F79 : 0xEC29354C,16,
                            focused ? 0xFF95BDFB : 0x55FFFFFF)));
            row.setOnClickListener(v -> switchTvInput(source, holder[0]));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,px(84));
            lp.bottomMargin=px(8);
            content.addView(row,lp);
        }
        if (sources.isEmpty()) {
            TextView info = text("This TV has not exposed selectable HDMI/AV inputs to Hybrid TV."
                    + " Use TCL's built-in Source menu; no port numbers will be guessed.",
                    25, Color.WHITE, false);
            content.addView(info);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this,
                android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Select TV source")
                .setView(scroll)
                .setNegativeButton("Back", (d,which) -> d.dismiss())
                .create();
        holder[0] = dialog;
        dialog.setOnShowListener(d -> {
            if (first[0] != null) first[0].requestFocus();
        });
        dialog.show();
    }

    private void switchTvInput(SourceEntry selected, AlertDialog dialog) {
        // A pass-through URI targets this exact published input, not the first
        // available entry (which was AV on the user's TCL).
        Intent view = new Intent(Intent.ACTION_VIEW,
                TvContract.buildChannelUriForPassthroughInput(selected.info.getId()));
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            // startActivity can resolve an intent even when Android 11 package
            // visibility prevents resolveActivity() from seeing its handler.
            startActivity(view);
            if (dialog != null) dialog.dismiss();
        } catch (RuntimeException e) {
            Toast.makeText(this, "TCL blocked this source; try the TV's Input key",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openTvSettings() {
        // ADB identified the exact Android TV Settings activity on this TCL.
        // Target it directly; if this firmware blocks exported activity launch,
        // fall back to the ordinary Android Settings intent.
        Intent explicit = new Intent(Intent.ACTION_MAIN);
        explicit.setClassName("com.android.tv.settings", "com.android.tv.settings.MainSettings");
        explicit.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(explicit);
            return;
        } catch (RuntimeException ignored) { }
        Intent ordinary = new Intent(Settings.ACTION_SETTINGS);
        ordinary.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(ordinary);
        } catch (RuntimeException e) {
            Toast.makeText(this,"TV Settings could not be opened", Toast.LENGTH_LONG).show();
        }
    }

    private void showSelection(String pkg, String label) {
        if (!STREMIO.equals(pkg) && !YOUTUBE.equals(pkg)) {
            showIdle(); // Utility navigation stays quiet; wallpaper remains primary.
            return;
        }
        heroArtwork.setVisibility(View.GONE);
        heroArtwork.setImageDrawable(null);
        hero.setVisibility(View.VISIBLE);
        playbackProgress.setVisibility(View.GONE);
        heroEyebrow.setText(STREMIO.equals(pkg) ? "CONTINUE WATCHING  ·  STREMIO"
                : YOUTUBE.equals(pkg) ? "YOUTUBE  ·  FEATURED"
                : "YOUR APPS  ·  " + label.toUpperCase(Locale.US));
        heroAction.setText("▶  Press OK to open " + label);
        heading.setText(label);
        if (STREMIO.equals(pkg)) {
            showStremio();
        } else if (YOUTUBE.equals(pkg)) {
            showYoutube();
        } else if ("inputs".equals(pkg) || "settings".equals(pkg)) {
            details.setText("TV controls");
            footnote.setText("Press OK to open " + label);
        } else {
            details.setText("Featured app");
            footnote.setText("Press OK to open " + label + " · No viewing data accessed");
        }
    }

    private void showYoutube() {
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ComponentName listener = new ComponentName(this, HybridSessionListener.class);
        if (notifications == null || !notifications.isNotificationListenerAccessGranted(listener)) {
            YoutubeSnapshotStore.clear(this);
            heading.setText("YouTube");
            details.setText("Playback preview unavailable");
            footnote.setText("Enable Hybrid TV media-session notification access");
            return;
        }
        YoutubeSnapshotStore.Snapshot snapshot = null;
        boolean live = false;
        try {
            MediaSessionManager manager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            if (manager != null) for (MediaController controller : manager.getActiveSessions(listener)) {
                if (!YOUTUBE.equals(controller.getPackageName())) continue;
                MediaMetadata metadata = controller.getMetadata();
                PlaybackState state = controller.getPlaybackState();
                snapshot = YoutubeSnapshotStore.current(metadata, state);
                if (snapshot == null) continue;
                // Listener saves in the background. Keep this fallback in case
                // TCL does not deliver a callback before the user presses HOME.
                YoutubeSnapshotStore.capture(this, metadata, state);
                live = true;
                break;
            }
        } catch (RuntimeException ignored) {
            // No crash if YouTube or the OEM media-session service is unavailable.
        }
        if (snapshot == null) snapshot = YoutubeSnapshotStore.read(this);
        if (snapshot == null) {
            heading.setText("YouTube");
            details.setText("Recent playback not available");
            footnote.setText("Play a video in YouTube, then press Home");
            return;
        }
        heroEyebrow.setText(live
                ? "YOUTUBE  ·  MEDIA SESSION"
                : "LAST SEEN  ·  YOUTUBE  ·  SAVED");
        heading.setText(snapshot.title);
        details.setText(snapshot.subtitle.isEmpty() ? "YouTube video" : snapshot.subtitle);
        if (snapshot.duration > 0 && snapshot.position >= 0) {
            long watched = Math.min(snapshot.position, snapshot.duration);
            playbackProgress.setProgress((int) Math.max(0L,
                    Math.min(1000L, watched * 1000L / snapshot.duration)));
            playbackProgress.setVisibility(View.VISIBLE);
            footnote.setText(fmt(watched) + (live ? " elapsed" : " saved") + "  ·  "
                    + fmt(snapshot.duration - watched) + " left");
        } else if (snapshot.position >= 0) {
            // YouTube TV may provide a title and position without a duration.
            // A percentage/progress bar would be fabricated in that case.
            footnote.setText(fmt(snapshot.position) + (live ? " elapsed" : " saved")
                    + "  ·  Total duration unavailable");
        } else {
            footnote.setText(live ? "Playback position unavailable"
                    : "Saved YouTube playback · Position unavailable");
        }
        if (snapshot.artwork != null && !snapshot.artwork.isRecycled()) {
            heroArtwork.setImageBitmap(snapshot.artwork);
            heroArtwork.setVisibility(View.VISIBLE);
        }
    }

    private void showStremio() {
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ComponentName listener = new ComponentName(this, HybridSessionListener.class);
        if (notifications == null || !notifications.isNotificationListenerAccessGranted(listener)) {
            // No access means no session collection and no retained media
            // snapshot. This is deliberately different from ordinary app data.
            StremioSnapshotStore.clear(this);
            details.setText("Continue Watching");
            footnote.setText("Opt-in media session access required · No data collected");
            return;
        }
        try {
            MediaSessionManager manager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            if (manager != null) for (MediaController controller : manager.getActiveSessions(listener)) {
                if (!STREMIO.equals(controller.getPackageName())) continue;
                MediaMetadata metadata = controller.getMetadata();
                PlaybackState state = controller.getPlaybackState();
                if (metadata == null || state == null) continue;
                String title = value(metadata, MediaMetadata.METADATA_KEY_TITLE);
                long total = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                long position = state.getPosition();
                int kind = state.getState();
                // A menu/stopped session is NOT currently playing media. Use
                // the most recent valid snapshot instead if one exists.
                boolean validKind = kind == PlaybackState.STATE_PLAYING
                        || kind == PlaybackState.STATE_PAUSED
                        || kind == PlaybackState.STATE_BUFFERING;
                if (title == null || total <= 0 || position < 0 || !validKind) continue;

                String episode = value(metadata, MediaMetadata.METADATA_KEY_ARTIST);
                heading.setText(title);
                details.setText(episode == null ? "Stremio · Continue Watching" : episode);
                long watched = Math.min(position, total);
                int progress = (int) Math.max(0L, Math.min(1000L, watched * 1000L / total));
                playbackProgress.setProgress(progress);
                playbackProgress.setVisibility(View.VISIBLE);
                heroEyebrow.setText("CONTINUE WATCHING  ·  STREMIO");
                footnote.setText(fmt(watched) + " watched  ·  " +
                        fmt(total - watched) + " left  ·  " +
                        String.format(Locale.US, "%.1f%%", 100f * watched / total));
                Bitmap bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                if (bmp == null) bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (bmp == null) bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (bmp != null) {
                    heroArtwork.setImageBitmap(bmp);
                    heroArtwork.setVisibility(View.VISIBLE);
                }
                // Also snapshot from foreground view (background service is
                // best-effort on vendor TV firmware).
                StremioSnapshotStore.capture(this, metadata, state);
                return;
            }
        } catch (SecurityException ignored) {
            // Service may have lost access between checking and reading.
        } catch (RuntimeException ignored) {
            // An inaccessible session must not bring down the TV launcher.
        }

        // Stremio often closes its media session when BACK returns to its
        // menu. The opt-in listener can retain the last VALID playing title.
        StremioSnapshotStore.Snapshot saved = StremioSnapshotStore.read(this);
        if (saved == null) {
            heading.setText("Stremio");
            details.setText("Continue Watching unavailable");
            footnote.setText("Play a title in Stremio, then return here");
            return;
        }
        heroEyebrow.setText("LAST SEEN  ·  STREMIO  ·  SAVED");
        heading.setText(saved.title);
        details.setText(saved.artist.isEmpty() ? "Previously watched on this TV"
                : saved.artist + " · previously watched");
        long watched = Math.min(saved.position, saved.duration);
        playbackProgress.setProgress((int) Math.max(0L,
                Math.min(1000L, watched * 1000L / saved.duration)));
        playbackProgress.setVisibility(View.VISIBLE);
        java.text.SimpleDateFormat timestamp =
                new java.text.SimpleDateFormat("d MMM, HH:mm", Locale.US);
        timestamp.setTimeZone(BRUNEI_ZONE);
        footnote.setText(fmt(watched) + " saved  ·  " +
                fmt(saved.duration - watched) + " left  ·  " +
                timestamp.format(new java.util.Date(saved.savedAt)));
        if (saved.artwork != null) {
            heroArtwork.setImageBitmap(saved.artwork);
            heroArtwork.setVisibility(View.VISIBLE);
        }
    }

    private String value(MediaMetadata m, String key) {
        CharSequence s = m.getText(key);
        return s == null || s.length() == 0 ? null : s.toString();
    }

    private String fmt(long millis) {
        long s = Math.max(0,millis) / 1000;
        return String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s/60)%60, s%60);
    }

    private Intent launch(PackageManager pm, String pkg) {
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        return i;
    }

    /**
     * Some TCL firmware retains the permission grant but does not bind a
     * secondary listener after reboot. Request a reconnect only after the
     * owner has enabled access and only if our service is actually offline.
     * This changes neither the default Home app nor other listener grants.
     */
    private void requestMediaListenerRebindIfNeeded() {
        if (HybridSessionListener.isLiveConnection()) return;
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ComponentName component = new ComponentName(this, HybridSessionListener.class);
        if (nm == null || !nm.isNotificationListenerAccessGranted(component)) return;
        long now = SystemClock.elapsedRealtime();
        if (lastListenerRebindElapsed != 0L
                && now - lastListenerRebindElapsed < 30000L) return;
        lastListenerRebindElapsed = now;
        try {
            NotificationListenerService.requestRebind(component);
        } catch (RuntimeException ignored) {
            // Best effort; Android retains sole control over binding.
        }
    }

    @Override protected void onResume() {
        super.onResume();
        active = true;
        requestMediaListenerRebindIfNeeded();
        weather.start();
        updateGreeting();
        ui.removeCallbacks(greetingTick);
        ui.postDelayed(greetingTick, GREETING_REFRESH_MS);
        ui.removeCallbacks(prayerTick);
        updatePrayerPanel();
        ui.postDelayed(prayerTick, PRAYER_REFRESH_MS);
        startPhotos();
        View focus = getCurrentFocus();
        if (focus != null && STREMIO.equals(focus.getTag())) showSelection(STREMIO, "Stremio");
        else if (focus != null && YOUTUBE.equals(focus.getTag())) showSelection(YOUTUBE, "YouTube");
    }

    @Override protected void onPause() {
        active = false;
        generation++;
        ui.removeCallbacksAndMessages(null);
        weather.stop();
        super.onPause();
    }

    private void updateGreeting() {
        if (greetingView == null) return;
        int hour = Calendar.getInstance(BRUNEI_ZONE).get(Calendar.HOUR_OF_DAY);
        String greeting = hour < 5 || hour >= 22 ? "GOOD NIGHT"
                : hour < 12 ? "GOOD MORNING"
                : hour < 18 ? "GOOD AFTERNOON" : "GOOD EVENING";
        greetingView.setText(greeting);
    }

    /**
     * Five daily times from PDI: Subuh, Zuhur, Asar, Maghrib, Isyak.
     * Syuruk is a boundary, not an obligatory prayer. Don't label Subuh
     * active after sunrise, or mislabel Asar during the following night.
     * The next event rolls over to TOMORROW's verified Subuh after Isyak.
     */
    private void updatePrayerPanel() {
        if (prayerNextView == null) return;
        Calendar now = Calendar.getInstance(BRUNEI_ZONE);
        int[] d = PrayerSchedule2026.get(now);
        if (d == null) {
            prayerNextView.setText("Timetable unavailable");
            prayerPeriodView.setText("No verified entry for this date");
            prayerSourceView.setText("Verified Oct–Dec 2026 only");
            for (int i = 0; i < 5; i++) {
                prayerTimeViews[i].setText("--:--");
                prayerTiles[i].setBackground(null);
            }
            return;
        }

        int[] indices = {0, 2, 3, 4, 5};
        int minutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        long nowMs = now.getTimeInMillis();
        int current = -1;
        if (minutes >= d[5]) current = 4;
        else if (minutes >= d[4]) current = 3;
        else if (minutes >= d[3]) current = 2;
        else if (minutes >= d[2]) current = 1;
        else if (minutes >= d[0] && minutes < d[1]) current = 0;

        String period;
        if (minutes < d[0]) period = "Night · awaiting Subuh";
        else if (minutes < d[1]) period = "Current: Subuh";
        else if (minutes < d[2]) period = "After Syuruk · before Zuhur";
        else period = "Current: " + PrayerSchedule2026.PRAYERS[current];
        prayerPeriodView.setText(period);
        prayerSourceView.setText("PDI 2026 · Offline · Brunei-Muara");

        for (int i = 0; i < 5; i++) {
            prayerTimeViews[i].setText(PrayerSchedule2026.format(d[indices[i]]));
            // The gold highlight only identifies a prayer period that has started.
            prayerTiles[i].setBackground(i == current
                    ? round(0xB5936F37, 18, 0xFFE8BD67) : null);
        }

        int next = -1;
        int targetMin = -1;
        Calendar eventDay = now;
        for (int i = 0; i < 5; i++) {
            int v = d[indices[i]];
            if (minutes < v) { next = i; targetMin = v; break; }
        }
        if (next < 0) {
            // After today's Isyak, countdown is to tomorrow's Subuh,
            // but ONLY if tomorrow is covered by the verified timetable.
            Calendar tomorrow = PrayerSchedule2026.nextDay(now);
            int[] later = PrayerSchedule2026.get(tomorrow);
            if (later == null) {
                prayerNextView.setText("Tomorrow: times unavailable");
                return;
            }
            next = 0;
            targetMin = later[0];
            eventDay = tomorrow;
        }

        Calendar event = (Calendar) eventDay.clone();
        event.set(Calendar.HOUR_OF_DAY, targetMin / 60);
        event.set(Calendar.MINUTE, targetMin % 60);
        event.set(Calendar.SECOND, 0);
        event.set(Calendar.MILLISECOND, 0);
        long delta = Math.max(0, event.getTimeInMillis() - nowMs);
        long remaining = (delta + 59999L) / 60000L;
        String eta = remaining >= 60
                ? (remaining / 60) + "h " + (remaining % 60) + "m"
                : remaining + "m";
        prayerNextView.setText("Next: " + PrayerSchedule2026.PRAYERS[next]
                + " " + PrayerSchedule2026.format(targetMin) + " · in " + eta);
    }

    private File photoFolder() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TVWallpapers");
    }

    private void startPhotos() {
        if (android.os.Build.VERSION.SDK_INT >= 23 && android.os.Build.VERSION.SDK_INT <= 32
                && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Avoid an unprompted runtime permission popup during the TV test.
            // User can grant from ADB after installation, as with Home Photos.
            return;
        }
        photos.clear();
        File[] files = photoFolder().listFiles();
        if (files != null) for (File f : files) {
            String n = f.getName().toLowerCase(Locale.US);
            if (f.isFile() && (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")
                    || n.endsWith(".webp"))) photos.add(f);
        }
        Collections.sort(photos);
        nextPhoto();
    }

    private void nextPhoto() {
        if (!active || photos.isEmpty()) return;
        index = (index + 1) % photos.size();
        final File f = photos.get(index);
        final int token = ++generation;
        new Thread(() -> {
            Bitmap bitmap = null;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(f.getAbsolutePath(), options);
                int sample = 1;
                int screenW = getResources().getDisplayMetrics().widthPixels;
                int screenH = getResources().getDisplayMetrics().heightPixels;
                // Cap both dimensions so tall portrait photographs do not
                // decode at huge resolutions on a low-memory Android TV.
                while (sample < 32 && (options.outWidth / sample > screenW * 1.5f
                        || options.outHeight / sample > screenH * 1.5f)) sample *= 2;
                options.inJustDecodeBounds = false;
                options.inSampleSize = sample;
                bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
            } catch (Exception ignored) { }
            Bitmap result = bitmap;
            Bitmap blurred = null;
            if (result != null) {
                try {
                    float screenRatio = (float) getResources().getDisplayMetrics().widthPixels
                            / Math.max(1, getResources().getDisplayMetrics().heightPixels);
                    blurred = TvWallpaperView.makeBlurredFill(result, screenRatio);
                } catch (OutOfMemoryError oom) {
                    // The original photo can still be shown undistorted.
                    android.util.Log.w("HybridWallpaper", "Backdrop memory limit", oom);
                }
            }
            final Bitmap background = blurred;
            ui.post(() -> {
                if (!active || token != generation) {
                    if (result != null && !result.isRecycled()) result.recycle();
                    if (background != null && !background.isRecycled()) background.recycle();
                    return;
                }
                if (result != null) photo.show(result, background);
                if (photos.size() > 1) ui.postDelayed(this::nextPhoto, SLIDE_MS);
            });
        }).start();
    }

    private TextView text(String value, int size, int color, boolean strong) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        t.setGravity(Gravity.START);
        if (strong) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }

    private GradientDrawable round(int fill, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(px(radius));
        d.setStroke(Math.max(1,px(1)), stroke);
        return d;
    }

    private FrameLayout.LayoutParams pos(int left,int top,int width,int height) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(width,height);
        p.leftMargin=left;
        p.topMargin=top;
        return p;
    }

    private int px(int n) { return Math.max(1, Math.round(n*scale)); }
}
