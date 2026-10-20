package com.ansub.tvhome;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.TransitionDrawable;
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
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
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
 * The prayer timetable is intentionally UNAVAILABLE until verified official
 * Brunei times are imported. Never render fabricated prayer timings.
 */
public final class HybridActivity extends Activity {
    private static final String STREMIO = "com.stremio.one";
    private static final String YOUTUBE = "com.google.android.youtube.tv";
    private static final int PHOTO_REQUEST = 31;
    private static final long SLIDE_MS = 60000L;
    private static final long GREETING_REFRESH_MS = 60000L;
    private static final TimeZone BRUNEI_ZONE = TimeZone.getTimeZone("Asia/Brunei");
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<View> dockTiles = new ArrayList<>();
    private final ArrayList<File> photos = new ArrayList<>();
    private ImageView photo, cinematic, heroArtwork;
    private TextView heading, details, footnote, greetingView, heroEyebrow, heroAction;
    private ProgressBar playbackProgress;
    private FrameLayout hero;
    private final Runnable greetingTick = new Runnable() {
        @Override public void run() {
            if (!active) return;
            updateGreeting();
            ui.postDelayed(this, GREETING_REFRESH_MS);
        }
    };
    private FrameLayout stageView;
    private WeatherWidget weather;
    private boolean active;
    private int generation;
    private int index = -1;
    private float scale;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        FrameLayout stage = new FrameLayout(this);
        stageView = stage;
        stage.setBackgroundColor(0xFF0A1222);

        photo = new ImageView(this);
        photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
        photo.setColorFilter(0x2F000000, PorterDuff.Mode.SRC_ATOP);
        stage.addView(photo, new FrameLayout.LayoutParams(-1,-1));

        cinematic = new ImageView(this);
        cinematic.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cinematic.setColorFilter(0x90000000, PorterDuff.Mode.SRC_ATOP);
        cinematic.setVisibility(View.GONE);
        stage.addView(cinematic, new FrameLayout.LayoutParams(-1,-1));

        View shade = new View(this);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{0xCD07101B, 0x55091322, 0x00000000}));
        FrameLayout.LayoutParams scrim = new FrameLayout.LayoutParams(-1, px(730), Gravity.BOTTOM);
        stage.addView(shade, scrim);

        LinearLayout clock = new LinearLayout(this);
        clock.setOrientation(LinearLayout.VERTICAL);
        clock.setGravity(Gravity.CENTER_VERTICAL);
        clock.setPadding(px(26), px(12), px(18), px(12));
        greetingView = text("GOOD NIGHT", 22, 0xCCFFFFFF, false);
        updateGreeting();
        clock.addView(greetingView);
        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("HH:mm");
        time.setTimeZone("Asia/Brunei");
        time.setTextColor(Color.WHITE);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(118));
        time.setIncludeFontPadding(false);
        clock.addView(time);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
        date.setTimeZone("Asia/Brunei");
        date.setTextColor(0xDDFFFFFF);
        date.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(24));
        clock.addView(date);
        clock.setBackground(round(0x5224344C, 23, 0x25FFFFFF));
        stage.addView(clock, pos(px(60), px(44), px(380), -2));

        // The times remain intentionally blank until the official Brunei
        // timetable has been parsed and verified against the original source.
        LinearLayout prayers = new LinearLayout(this);
        prayers.setOrientation(LinearLayout.VERTICAL);
        prayers.setGravity(Gravity.CENTER_VERTICAL);
        prayers.setPadding(px(28), px(23), px(28), px(23));
        prayers.setBackground(round(0x9E182536, 24, 0x35FFFFFF));
        prayers.addView(text("☪  WAKTU SOLAT  ·  BRUNEI–MUARA", 26, Color.WHITE, true));
        prayers.addView(text("Official prayer timetable: awaiting verification", 24, 0xFFE2C795, false));
        prayers.addView(text("Asia/Brunei  ·  Times deliberately hidden in this preview", 18, 0xFFD4DCE5, false));
        stage.addView(prayers, pos(px(465), px(44), px(890), -2));

        weather = new WeatherWidget(this, scale);
        weather.setBackground(round(0x8F162739, 24, 0x30FFFFFF));
        FrameLayout.LayoutParams wp = new FrameLayout.LayoutParams(px(455), -2, Gravity.TOP | Gravity.RIGHT);
        wp.topMargin = px(44);
        wp.rightMargin = px(60);
        stage.addView(weather, wp);

        // FEATURED HERO: reference layout at 1920x1080.
        // Approx. x=68..1378, y=275..810, leaving 30px before the dock.
        // Preserve the v0.3.1 hero frame at 1310x536 (1920x1080 layout).
        // Stremio's thumbnail is typically 16:9 (455x256 in our TCL test).
        // Cropping that image across the 2.44:1 whole hero discarded its top
        // and bottom. Instead, position a near-16:9 viewport on the RIGHT:
        // it keeps the character and most of the composition visible.
        hero = new FrameLayout(this);
        hero.setBackground(round(0xD1141C2B, 30, 0x62FFFFFF));
        hero.setClipToOutline(true);
        heroArtwork = new ImageView(this);
        heroArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        heroArtwork.setVisibility(View.GONE);
        FrameLayout.LayoutParams artParams = new FrameLayout.LayoutParams(
                px(850), -1, Gravity.RIGHT | Gravity.TOP);
        hero.addView(heroArtwork, artParams);

        // Heavy contrast strictly behind left-side metadata; gentle fade into
        // the actual episode artwork at the right. Never add a flat dark tint
        // directly to the image itself or use an enlarged thumbnail square.
        View heroShade = new View(this);
        heroShade.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {
                    0xFA121A2A,  // opaque behind title / progress
                    0xEF141B2A,
                    0xC5161D2A,
                    0x69151B26,
                    0x07151B26   // almost transparent over the art at right
                }));
        hero.addView(heroShade, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout writing = new LinearLayout(this);
        writing.setOrientation(LinearLayout.VERTICAL);
        writing.setGravity(Gravity.CENTER_VERTICAL);
        writing.setPadding(px(48), px(34), px(12), px(34));
        heroEyebrow = text("CONTINUE WATCHING", 25, 0xFFD8E1F0, true);
        heroEyebrow.setLetterSpacing(0.13f);
        writing.addView(heroEyebrow);

        heading = text("", 66, Color.WHITE, true);
        heading.setMaxLines(2);
        heading.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = px(33);
        writing.addView(heading, titleParams);

        details = text("", 30, 0xFFEBF0F7, false);
        details.setMaxLines(2);
        details.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams detailsParams = new LinearLayout.LayoutParams(-1, -2);
        detailsParams.topMargin = px(15);
        writing.addView(details, detailsParams);

        playbackProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        playbackProgress.setMax(1000);
        playbackProgress.setProgress(0);
        playbackProgress.setProgressTintList(ColorStateList.valueOf(0xFF9C7BFA));
        playbackProgress.setProgressBackgroundTintList(ColorStateList.valueOf(0x70FFFFFF));
        playbackProgress.setVisibility(View.GONE);
        LinearLayout.LayoutParams trackParams = new LinearLayout.LayoutParams(-1, px(10));
        trackParams.topMargin = px(30);
        writing.addView(playbackProgress, trackParams);

        footnote = text("", 23, 0xFFE1E6F0, false);
        footnote.setMaxLines(2);
        footnote.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams footnoteParams = new LinearLayout.LayoutParams(-1, -2);
        footnoteParams.topMargin = px(14);
        writing.addView(footnote, footnoteParams);

        heroAction = text("▶  Press OK to open", 23, Color.WHITE, true);
        heroAction.setGravity(Gravity.CENTER_VERTICAL);
        heroAction.setPadding(px(24), px(13), px(24), px(13));
        heroAction.setBackground(round(0xAA452C83, 18, 0xFF9B78ED));
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-2, -2);
        actionParams.topMargin = px(29);
        writing.addView(heroAction, actionParams);

        // Keep text within the left half, leaving the right half visually cinematic.
        FrameLayout.LayoutParams textParams = new FrameLayout.LayoutParams(
                px(730), -1, Gravity.LEFT | Gravity.TOP);
        hero.addView(writing, textParams);
        hero.setVisibility(View.GONE);
        stage.addView(hero, pos(px(68), px(274), px(1310), px(536)));

        LinearLayout dock = new LinearLayout(this);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setGravity(Gravity.CENTER);
        dock.setClipChildren(false);
        dock.setClipToPadding(false);
        dock.setPadding(px(17), px(20), px(17), px(20));
        dock.setBackground(round(0xC3172130, 30, 0x4EFFFFFF));
        FrameLayout.LayoutParams dockParams = new FrameLayout.LayoutParams(-1, px(182), Gravity.BOTTOM);
        dockParams.leftMargin = px(56);
        dockParams.rightMargin = px(56);
        dockParams.bottomMargin = px(56);
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
        if (dockTiles.isEmpty()) return super.dispatchKeyEvent(event);
        final int code = event.getKeyCode();
        final boolean direction = code == KeyEvent.KEYCODE_DPAD_LEFT
                || code == KeyEvent.KEYCODE_DPAD_RIGHT
                || code == KeyEvent.KEYCODE_DPAD_UP
                || code == KeyEvent.KEYCODE_DPAD_DOWN;
        final boolean select = code == KeyEvent.KEYCODE_DPAD_CENTER
                || code == KeyEvent.KEYCODE_ENTER
                || code == KeyEvent.KEYCODE_NUMPAD_ENTER;
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
        if (cinematic != null) {
            cinematic.setVisibility(View.GONE);
            cinematic.setImageDrawable(null);
        }
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
        addDockTile(dock, "inputs", "Inputs", null, null, this::openFirstTvInput);
        addDockTile(dock, "settings", "Settings", null, null,
                () -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
    }

    /** Original Android TV app banner, fitted to the unchanged TV dock tile. */
    private void addDockTile(LinearLayout dock, String pkg, String label,
                             Drawable banner, Drawable appIcon, Runnable activate) {
        final LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setFocusableInTouchMode(true);
        tile.setClickable(true);
        tile.setClipToOutline(true);
        // Preserve names to screen readers, but never put a caption below art.
        tile.setContentDescription("Hybrid dock: " + label);
        tile.setTag(pkg);
        tile.setBackground(dockSurface(pkg, false));
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
            tile.addView(system, new LinearLayout.LayoutParams(px(78), px(78)));
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
            tile.setBackground(dockSurface(pkg, focused));
            // One rounded highlight only; original TV banner stays unchanged.
            tile.setForeground(focused ? dockFocusOutline() : null);
            tile.setElevation(focused ? px(12) : 0);
            tile.animate().scaleX(focused ? 1.045f : 1f)
                    .scaleY(focused ? 1.045f : 1f).setDuration(155).start();
            if (focused) showSelection(pkg, label);
        });
        tile.setOnClickListener(v -> {
            try { activate.run(); } catch (Exception e) {
                footnote.setText("App unavailable on this TV");
            }
        });
        dockTiles.add(tile);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, px(136), 1f);
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
        stroke.setStroke(px(3), 0xFF95BDFB);
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
            top = 0xF4465278;
            bottom = 0xF1283556;
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

    private void openFirstTvInput() {
        try {
            TvInputManager manager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
            if (manager != null) {
                for (TvInputInfo input : manager.getTvInputList()) {
                    if (input.isPassthroughInput()) {
                        Intent view = new Intent(Intent.ACTION_VIEW,
                                TvContract.buildChannelUriForPassthroughInput(input.getId()));
                        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(view);
                        return;
                    }
                }
            }
        } catch (Exception ignored) { }
        // Some TCL firmware restricts TV input intents to its own system UI.
        startActivity(new Intent(Settings.ACTION_SETTINGS));
    }

    private void showSelection(String pkg, String label) {
        cinematic.setVisibility(View.GONE);
        cinematic.setImageDrawable(null);
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
            details.setText("Continue Watching");
            footnote.setText("Playback history preview pending YouTube compatibility test");
        } else if ("inputs".equals(pkg) || "settings".equals(pkg)) {
            details.setText("TV controls");
            footnote.setText("Press OK to open " + label);
        } else {
            details.setText("Featured app");
            footnote.setText("Press OK to open " + label + " · No viewing data accessed");
        }
    }

    private void showStremio() {
        NotificationManager notifications = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ComponentName listener = new ComponentName(this, HybridSessionListener.class);
        if (notifications == null || !notifications.isNotificationListenerAccessGranted(listener)) {
            details.setText("Continue Watching");
            footnote.setText("Opt-in media session access required · No data collected");
            return;
        }
        try {
            MediaSessionManager manager = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
            if (manager == null) return;
            for (MediaController controller : manager.getActiveSessions(listener)) {
                if (!STREMIO.equals(controller.getPackageName())) continue;
                MediaMetadata metadata = controller.getMetadata();
                PlaybackState state = controller.getPlaybackState();
                if (metadata == null) break;
                String title = value(metadata, MediaMetadata.METADATA_KEY_TITLE);
                String episode = value(metadata, MediaMetadata.METADATA_KEY_ARTIST);
                if (title != null) heading.setText(title);
                details.setText(episode == null ? "Stremio · Continue Watching" : episode);
                long total = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                long position = state == null ? -1 : state.getPosition();
                if (total > 0 && position >= 0) {
                    long watched = Math.min(position, total);
                    int progress = (int) Math.max(0L, Math.min(1000L, watched * 1000L / total));
                    playbackProgress.setProgress(progress);
                    playbackProgress.setVisibility(View.VISIBLE);
                    float percentage = 100f * watched / total;
                    footnote.setText(fmt(watched) + " watched  ·  " +
                            fmt(total - watched) + " left  ·  " +
                            String.format(Locale.US, "%.1f%%", percentage));
                } else {
                    playbackProgress.setVisibility(View.GONE);
                    footnote.setText("Playback duration unavailable");
                }
                Bitmap bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                if (bmp == null) bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (bmp == null) bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (bmp != null) {
                    heroArtwork.setImageBitmap(bmp);
                    heroArtwork.setVisibility(View.VISIBLE);
                    cinematic.setImageBitmap(bmp);
                    cinematic.setVisibility(View.VISIBLE);
                }
                return;
            }
            details.setText("Continue Watching unavailable");
            footnote.setText("Play and pause a title in Stremio, then return here");
        } catch (SecurityException e) {
            details.setText("Access not granted");
            footnote.setText("Notification listener access requires explicit permission");
        } catch (RuntimeException e) {
            details.setText("Media session unavailable");
            footnote.setText("Press OK to open Stremio");
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

    @Override protected void onResume() {
        super.onResume();
        active = true;
        weather.start();
        updateGreeting();
        ui.removeCallbacks(greetingTick);
        ui.postDelayed(greetingTick, GREETING_REFRESH_MS);
        startPhotos();
        View focus = getCurrentFocus();
        if (focus != null && STREMIO.equals(focus.getTag())) showSelection(STREMIO, "Stremio");
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
                int w = getResources().getDisplayMetrics().widthPixels;
                while (options.outWidth / sample > w * 1.5 && sample < 32) sample *= 2;
                options.inJustDecodeBounds = false;
                options.inSampleSize = sample;
                bitmap = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
            } catch (Exception ignored) { }
            Bitmap result = bitmap;
            ui.post(() -> {
                if (!active || token != generation) {
                    if (result != null && !result.isRecycled()) result.recycle();
                    return;
                }
                if (result != null) {
                    Drawable old = photo.getDrawable();
                    if (old instanceof TransitionDrawable) old = ((TransitionDrawable) old).getDrawable(1);
                    if (old == null) old = new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT);
                    TransitionDrawable fade = new TransitionDrawable(new Drawable[]{
                            old, new BitmapDrawable(getResources(), result)});
                    fade.setCrossFadeEnabled(true);
                    photo.setImageDrawable(fade);
                    fade.startTransition(850);
                }
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
