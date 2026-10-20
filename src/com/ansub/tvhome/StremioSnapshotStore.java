package com.ansub.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.os.SystemClock;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Small, optional, on-device snapshot of the LAST valid Stremio media session.
 * No notification text, other apps, Internet, accounts, or external storage.
 * Do not pretend snapshots are live Stremio Continue Watching history.
 */
final class StremioSnapshotStore {
    private static final String PREFS = "hybrid_stremio_last_seen";
    private static final String ART = "stremio_last_seen.jpg";
    private static final String TMP = "stremio_last_seen.tmp";
    private static final long TTL_MS = 14L * 24 * 60 * 60 * 1000;
    private static final long MIN_CAPTURE_MS = 5000;
    private static final ExecutorService WRITE = Executors.newSingleThreadExecutor();
    private static long lastCaptureElapsed;
    private static int generation;

    static final class Snapshot {
        final String title, artist;
        final long duration, position, savedAt;
        final Bitmap artwork;
        Snapshot(String title, String artist, long duration, long position,
                 long savedAt, Bitmap artwork) {
            this.title = title; this.artist = artist;
            this.duration = duration; this.position = position;
            this.savedAt = savedAt; this.artwork = artwork;
        }
    }

    private StremioSnapshotStore() { }

    /**
     * Call exclusively for com.stremio.one and only while notification
     * listener access is explicitly granted. A worker owns all disk writes.
     */
    static void capture(Context context, MediaMetadata metadata, PlaybackState state) {
        if (metadata == null || state == null) return;
        int kind = state.getState();
        if (kind != PlaybackState.STATE_PLAYING && kind != PlaybackState.STATE_PAUSED
                && kind != PlaybackState.STATE_BUFFERING) return;
        CharSequence rawTitle = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
        if (rawTitle == null || rawTitle.length() == 0) return;
        final String title = rawTitle.toString().trim();
        if (title.isEmpty()) return;
        CharSequence rawArtist = metadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
        final String artist = rawArtist == null ? "" : rawArtist.toString();
        final long duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
        final long position = state.getPosition();
        if (duration <= 0 || position < 0) return;
        final long boundedPosition = Math.min(position, duration);
        final long savedAt = System.currentTimeMillis();
        Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (art == null) art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        final Bitmap sourceArt = art;
        final Context app = context.getApplicationContext();

        final int token;
        synchronized (StremioSnapshotStore.class) {
            final long now = SystemClock.elapsedRealtime();
            if (now - lastCaptureElapsed < MIN_CAPTURE_MS) return;
            lastCaptureElapsed = now;
            token = generation;
        }
        WRITE.execute(() -> {
            if (token != currentGeneration()) return;
            try {
                SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                final String previousTitle = prefs.getString("title", "");
                boolean hasArt = sourceArt != null && !sourceArt.isRecycled();
                boolean fileAvailable = new File(app.getFilesDir(), ART).isFile();
                if (hasArt) {
                    File temp = new File(app.getFilesDir(), TMP);
                    int srcW = sourceArt.getWidth(), srcH = sourceArt.getHeight();
                    double shrink = Math.min(1d, Math.min(720d / srcW, 420d / srcH));
                    int w = Math.max(1, (int)Math.round(srcW * shrink));
                    int h = Math.max(1, (int)Math.round(srcH * shrink));
                    Bitmap small = Bitmap.createScaledBitmap(sourceArt, w, h, true);
                    try (FileOutputStream stream = new FileOutputStream(temp)) {
                        small.compress(Bitmap.CompressFormat.JPEG, 82, stream);
                    }
                    if (small != sourceArt) small.recycle();
                    File target = new File(app.getFilesDir(), ART);
                    // If atomic replacement fails, don't associate old art
                    // with a newly playing title.
                    if (!temp.renameTo(target)) {
                        if (target.exists()) target.delete();
                        if (!temp.renameTo(target)) temp.delete();
                    }
                    fileAvailable = target.isFile();
                } else if (!title.equals(previousTitle)) {
                    new File(app.getFilesDir(), ART).delete();
                    fileAvailable = false;
                }
                if (token != currentGeneration()) return;
                prefs.edit().putString("title", title).putString("artist", artist)
                        .putLong("duration", duration).putLong("position", boundedPosition)
                        .putLong("saved_at", savedAt).putBoolean("art", fileAvailable).apply();
            } catch (Exception | OutOfMemoryError ignored) {
                // Nonfatal: a missed snapshot must never crash the launcher.
            }
        });
    }

    private static synchronized int currentGeneration() { return generation; }

    static Snapshot read(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String title = prefs.getString("title", "");
        long saved = prefs.getLong("saved_at", 0);
        if (title == null || title.trim().isEmpty() || saved <= 0) return null;
        long age = System.currentTimeMillis() - saved;
        if (age < 0 || age > TTL_MS) {
            clear(context);
            return null;
        }
        long duration = prefs.getLong("duration", -1), position = prefs.getLong("position", -1);
        if (duration <= 0 || position < 0) return null;
        Bitmap art = null;
        if (prefs.getBoolean("art", false)) {
            File file = new File(context.getFilesDir(), ART);
            if (file.isFile()) {
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inPreferredConfig = Bitmap.Config.RGB_565;
                art = BitmapFactory.decodeFile(file.getAbsolutePath(), o);
            }
        }
        return new Snapshot(title, prefs.getString("artist", ""),
                duration, Math.min(position, duration), saved, art);
    }

    /** Revoke/expired-data cleanup; never affects photos or other apps. */
    static void clear(Context context) {
        synchronized (StremioSnapshotStore.class) {
            generation++;
            lastCaptureElapsed = 0;
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit();
        new File(context.getFilesDir(), ART).delete();
        new File(context.getFilesDir(), TMP).delete();
    }
}
