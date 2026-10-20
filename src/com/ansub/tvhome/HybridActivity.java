package com.ansub.tvhome;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ArrayList<View> dockTiles = new ArrayList<>();
    private final ArrayList<File> photos = new ArrayList<>();
    private ImageView photo, cinematic, cardArtwork;
    private TextView heading, details, footnote;
    private LinearLayout hero;
    private WeatherWidget weather;
    private boolean active;
    private int generation;
    private int index = -1;
    private float scale;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        scale = getResources().getDisplayMetrics().widthPixels / 1920f;
        FrameLayout stage = new FrameLayout(this);
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
        TextView greeting = text("GOOD EVENING", 22, 0xCCFFFFFF, false);
        clock.addView(greeting);
        TextClock time = new TextClock(this);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("HH:mm");
        time.setTextColor(Color.WHITE);
        time.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(118));
        time.setIncludeFontPadding(false);
        clock.addView(time);
        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
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

        hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(px(32), px(22), px(32), px(22));
        hero.setBackground(round(0xBD101B2D, 28, 0x3BFFFFFF));
        cardArtwork = new ImageView(this);
        cardArtwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cardArtwork.setVisibility(View.GONE);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(px(380), px(215));
        cp.rightMargin = px(26);
        hero.addView(cardArtwork, cp);
        LinearLayout writing = new LinearLayout(this);
        writing.setOrientation(LinearLayout.VERTICAL);
        writing.setGravity(Gravity.CENTER_VERTICAL);
        heading = text("", 43, Color.WHITE, true);
        details = text("", 28, 0xFFE8EDF5, false);
        footnote = text("", 22, 0xFFC4CFDD, false);
        LinearLayout.LayoutParams gap1 = new LinearLayout.LayoutParams(-1, -2);
        gap1.topMargin = px(10);
        writing.addView(heading);
        writing.addView(details, gap1);
        LinearLayout.LayoutParams gap2 = new LinearLayout.LayoutParams(-1, -2);
        gap2.topMargin = px(14);
        writing.addView(footnote, gap2);
        hero.addView(writing, new LinearLayout.LayoutParams(0, -1, 1f));
        hero.setVisibility(View.GONE);
        stage.addView(hero, pos(px(100), px(285), px(1340), px(320)));

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
        stage.setFocusableInTouchMode(true);
        stage.requestFocus(); // Family slideshow is the uncluttered default state.
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
            Drawable banner = null;
            try {
                banner = pm.getActivityBanner(intent);
                if (banner == null) banner = pm.getApplicationBanner(pkg);
            } catch (Exception ignored) { }
            Drawable icon = null;
            if (banner == null) {
                try { icon = pm.getApplicationIcon(pkg); } catch (Exception ignored) { }
            }
            addDockTile(dock, pkg, label, banner != null ? banner : icon,
                    () -> { intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(intent); });
        }
        addDockTile(dock, "inputs", "Inputs", null,
                () -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
        addDockTile(dock, "settings", "Settings", null,
                () -> startActivity(new Intent(Settings.ACTION_SETTINGS)));
    }

    private void addDockTile(LinearLayout dock, String pkg, String label,
                             Drawable icon, Runnable activate) {
        final LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setFocusable(true);
        tile.setClickable(true);
        tile.setPadding(px(12), px(9), px(12), px(8));
        tile.setBackground(round(0xCC1F2F48, 21, 0x10FFFFFF));
        tile.setTag(pkg);
        ImageView graphic = new ImageView(this);
        graphic.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (icon != null) graphic.setImageDrawable(icon);
        else graphic.setImageDrawable(new WeatherGlyph(-1, true)); // temporary neutral icon
        if (icon != null) tile.addView(graphic, new LinearLayout.LayoutParams(-1, px(80)));
        else tile.addView(graphic, new LinearLayout.LayoutParams(px(46), px(46)));
        TextView labelView = text(label, 24, Color.WHITE, true);
        labelView.setGravity(Gravity.CENTER);
        labelView.setSingleLine(true);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = px(4);
        tile.addView(labelView, labelParams);

        tile.setOnFocusChangeListener((v, focused) -> {
            tile.setBackground(round(focused ? 0xFFF2F5FA : 0xCC1F2F48,
                    21, focused ? 0xFFFFCF72 : 0x10FFFFFF));
            labelView.setTextColor(focused ? 0xFF0D1B2D : Color.WHITE);
            tile.animate().scaleX(focused ? 1.05f : 1f)
                    .scaleY(focused ? 1.05f : 1f).setDuration(145).start();
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

    private void showSelection(String pkg, String label) {
        cinematic.setVisibility(View.GONE);
        cinematic.setImageDrawable(null);
        cardArtwork.setVisibility(View.GONE);
        cardArtwork.setImageDrawable(null);
        hero.setVisibility(View.VISIBLE);
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
                footnote.setText((total > 0 && position >= 0)
                        ? fmt(position) + " / " + fmt(total) + " · "
                            + Math.max(0, Math.min(100, (int)(position * 100 / total))) + "% watched"
                        : "Position / total duration unavailable");
                Bitmap bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                if (bmp == null) bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (bmp == null) bmp = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (bmp != null) {
                    cardArtwork.setImageBitmap(bmp);
                    cardArtwork.setVisibility(View.VISIBLE);
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
