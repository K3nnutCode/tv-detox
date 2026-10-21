package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Opt-in, device-local snapshot of the last usable YouTube TV media session.
 * YouTube does not guarantee duration, channel or artwork: do not invent them.
 * No YouTube account/API access, notification text, or remote thumbnail fetches.
 */
final class YoutubeSnapshotStore {
    private static final String PREFS = "hybrid_youtube_last_seen";
    private static final String ART = "youtube_last_seen.jpg";
    private static final String TMP = "youtube_last_seen.tmp";
    private static final long TTL_MS = 14L * 24 * 60 * 60 * 1000;
    private static final ExecutorService WRITE = Executors.newSingleThreadExecutor();
    private static long lastCaptureElapsed;
    private static int generation;

    static final class Snapshot {
        final String title, subtitle;
        final long duration, position, savedAt;
        final Bitmap artwork;
        Snapshot(String title, String subtitle, long duration, long position,
                 long savedAt, Bitmap artwork) {
            this.title = title;
            this.subtitle = subtitle;
            this.duration = duration;
            this.position = position;
            this.savedAt = savedAt;
            this.artwork = artwork;
        }
    }

    private YoutubeSnapshotStore() { }

    private static String value(CharSequence text) {
        if (text == null) return null;
        String s = text.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** In-memory preview of the live session; no disk write or invented fields. */
    static Snapshot current(MediaMetadata metadata, PlaybackState state) {
        if (metadata == null || state == null) return null;
        int kind = state.getState();
        if (kind != PlaybackState.STATE_PLAYING && kind != PlaybackState.STATE_PAUSED
                && kind != PlaybackState.STATE_BUFFERING) return null;
        MediaDescription description = metadata.getDescription();
        String title = value(metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE));
        if (title == null) title = value(metadata.getText(MediaMetadata.METADATA_KEY_TITLE));
        if (title == null && description != null) title = value(description.getTitle());
        if (title == null) return null;
        String subtitle = value(metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE));
        if (subtitle == null) subtitle = value(metadata.getText(MediaMetadata.METADATA_KEY_ARTIST));
        if (subtitle == null && description != null) subtitle = value(description.getSubtitle());
        Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (art == null && description != null) art = description.getIconBitmap();
        long duration = metadata.containsKey(MediaMetadata.METADATA_KEY_DURATION)
                ? metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) : -1L;
        return new Snapshot(title, subtitle == null ? "" : subtitle, duration,
                state.getPosition(), System.currentTimeMillis(), art);
    }

    static void capture(Context context, MediaMetadata metadata, PlaybackState state) {
        if (metadata == null || state == null) return;
        int kind = state.getState();
        if (kind != PlaybackState.STATE_PLAYING && kind != PlaybackState.STATE_PAUSED
                && kind != PlaybackState.STATE_BUFFERING) return;
        MediaDescription description = metadata.getDescription();
        String title = value(metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE));
        if (title == null) title = value(metadata.getText(MediaMetadata.METADATA_KEY_TITLE));
        if (title == null && description != null) title = value(description.getTitle());
        if (title == null) return; // Never show a made-up video title.
        String subtitle = value(metadata.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE));
        if (subtitle == null) subtitle = value(metadata.getText(MediaMetadata.METADATA_KEY_ARTIST));
        if (subtitle == null && description != null) subtitle = value(description.getSubtitle());

        final String savedTitle = title;
        final String savedSubtitle = subtitle == null ? "" : subtitle;
        final long duration = metadata.containsKey(MediaMetadata.METADATA_KEY_DURATION)
                ? metadata.getLong(MediaMetadata.METADATA_KEY_DURATION) : -1L;
        final long position = state.getPosition();
        final long savedAt = System.currentTimeMillis();
        Bitmap artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (artwork == null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (artwork == null) artwork = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (artwork == null && description != null) artwork = description.getIconBitmap();
        final Bitmap sourceArt = artwork;
        final Uri artUri = description == null ? null : description.getIconUri();
        final Context app = context.getApplicationContext();

        final int token;
        synchronized (YoutubeSnapshotStore.class) {
            long now = SystemClock.elapsedRealtime();
            if (now - lastCaptureElapsed < 5000) return;
            lastCaptureElapsed = now;
            token = generation;
        }
        WRITE.execute(() -> {
            if (token != currentGeneration()) return;
            try {
                SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                String previousTitle = prefs.getString("title", "");
                Bitmap decoded = null;
                Bitmap art = sourceArt;
                // Only open on-device resources. Never fetch HTTP(S) artwork.
                if ((art == null || art.isRecycled()) && artUri != null
                        && ("content".equals(artUri.getScheme())
                            || "android.resource".equals(artUri.getScheme()))) {
                    try (InputStream stream = app.getContentResolver().openInputStream(artUri)) {
                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inPreferredConfig = Bitmap.Config.RGB_565;
                        opts.inSampleSize = 2;
                        decoded = BitmapFactory.decodeStream(stream, null, opts);
                        art = decoded;
                    } catch (Exception | OutOfMemoryError ignored) { }
                }
                File target = new File(app.getFilesDir(), ART);
                boolean fileAvailable = target.isFile();
                if (art != null && !art.isRecycled()) {
                    int width = art.getWidth(), height = art.getHeight();
                    double shrink = Math.min(1d,
                            Math.min(760d / Math.max(1, width), 430d / Math.max(1, height)));
                    int w = Math.max(1, (int) Math.round(width * shrink));
                    int h = Math.max(1, (int) Math.round(height * shrink));
                    Bitmap small = Bitmap.createScaledBitmap(art, w, h, true);
                    File temp = new File(app.getFilesDir(), TMP);
                    try (FileOutputStream stream = new FileOutputStream(temp)) {
                        small.compress(Bitmap.CompressFormat.JPEG, 82, stream);
                    }
                    if (small != art) small.recycle();
                    if (target.exists()) target.delete();
                    fileAvailable = temp.renameTo(target) && target.isFile();
                } else if (!savedTitle.equals(previousTitle)) {
                    target.delete();
                    fileAvailable = false;
                }
                if (decoded != null && !decoded.isRecycled()) decoded.recycle();
                if (token != currentGeneration()) return;
                prefs.edit().putString("title", savedTitle)
                        .putString("subtitle", savedSubtitle)
                        .putLong("duration", duration)
                        .putLong("position", position)
                        .putLong("saved_at", savedAt)
                        .putBoolean("art", fileAvailable).apply();
            } catch (Exception | OutOfMemoryError ignored) {
                // A missing/corrupt thumbnail must never crash the Home launcher.
            }
        });
    }

    private static synchronized int currentGeneration() { return generation; }

    static Snapshot read(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String title = prefs.getString("title", "");
        long savedAt = prefs.getLong("saved_at", 0);
        if (title == null || title.trim().isEmpty() || savedAt <= 0) return null;
        long age = System.currentTimeMillis() - savedAt;
        if (age < 0 || age > TTL_MS) {
            clear(context);
            return null;
        }
        Bitmap art = null;
        if (prefs.getBoolean("art", false)) {
            File file = new File(context.getFilesDir(), ART);
            try {
                if (file.isFile()) art = BitmapFactory.decodeFile(file.getAbsolutePath());
            } catch (Exception | OutOfMemoryError ignored) { }
        }
        return new Snapshot(title, prefs.getString("subtitle", ""),
                prefs.getLong("duration", -1), prefs.getLong("position", -1),
                savedAt, art);
    }

    static void clear(Context context) {
        synchronized (YoutubeSnapshotStore.class) {
            generation++;
            lastCaptureElapsed = 0;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        new File(context.getFilesDir(), ART).delete();
        new File(context.getFilesDir(), TMP).delete();
    }
}
