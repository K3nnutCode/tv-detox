package com.ansub.tvhome;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Collections;
import java.util.List;

/**
 * Owner-enabled Android media-session observation for Stremio and YouTube TV.
 * Android's notification-listener permission is broad, but this implementation
 * NEVER examines notification contents; only opted-in Stremio and YouTube sessions are read.
 * Runs only if the user has explicitly enabled notification access.
 */
public final class HybridSessionListener extends NotificationListenerService {
    private static final String STREMIO = "com.stremio.one";
    private static final String YOUTUBE = "com.google.android.youtube.tv";
    private final Handler main = new Handler(Looper.getMainLooper());
    private MediaSessionManager sessions;
    private MediaController stremioController;
    private MediaController youtubeController;
    private boolean connected;

    private final MediaController.Callback controllerCallback = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata metadata) { saveCurrent(); }
        @Override public void onPlaybackStateChanged(PlaybackState state) { saveCurrent(); }
        @Override public void onSessionDestroyed() { attach(null); }
    };

    private final MediaController.Callback youtubeCallback = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata metadata) { saveCurrent(); }
        @Override public void onPlaybackStateChanged(PlaybackState state) { saveCurrent(); }
        @Override public void onSessionDestroyed() { attach(null); }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener sessionListener =
            controllers -> attach(controllers);

    private static final long SAMPLE_INTERVAL_MS = 15000L;
    private final Runnable sampling = new Runnable() {
        @Override public void run() {
            if (!connected) return;
            // TCL/YouTube can expose a valid active media session without
            // sending OnActiveSessionsChangedListener to this service.
            // Discover sessions again every 15s; merely sampling a cached
            // youtubeController cannot find a previously missed session.
            refreshActiveSessions();
            if (connected) main.postDelayed(this, SAMPLE_INTERVAL_MS);
        }
    };

    private void refreshActiveSessions() {
        if (!connected || sessions == null) return;
        try {
            ComponentName component = new ComponentName(this, HybridSessionListener.class);
            attach(sessions.getActiveSessions(component));
        } catch (SecurityException ignored) {
            // Android may withdraw the owner's opt-in access at any time.
        } catch (RuntimeException ignored) {
            // TV firmware can briefly reject active-session queries.
            // Retain the last valid snapshot and retry on the next sample.
        }
    }

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        main.removeCallbacks(sampling);
        connected = true;
        sessions = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        if (sessions == null) return;
        ComponentName component = new ComponentName(this, HybridSessionListener.class);
        try {
            sessions.addOnActiveSessionsChangedListener(sessionListener, component, main);
        } catch (RuntimeException ignored) {
            // Still poll: some Android TV firmware cannot register callbacks.
        }
        refreshActiveSessions();
        main.postDelayed(sampling, SAMPLE_INTERVAL_MS);
    }

    private void attach(List<MediaController> controllers) {
        if (!connected) return;
        MediaController chosen = null;
        MediaController chosenYoutube = null;
        if (controllers == null) controllers = Collections.emptyList();
        for (MediaController controller : controllers) {
            if (STREMIO.equals(controller.getPackageName())) chosen = controller;
            if (YOUTUBE.equals(controller.getPackageName())) chosenYoutube = controller;
        }
        if (chosen != stremioController) {
            if (stremioController != null) {
                try { stremioController.unregisterCallback(controllerCallback); }
                catch (RuntimeException ignored) { }
            }
            stremioController = chosen;
            if (chosen != null) {
                try { chosen.registerCallback(controllerCallback, main); }
                catch (RuntimeException ignored) { stremioController = null; }
            }
        }
        if (chosenYoutube != youtubeController) {
            if (youtubeController != null) {
                try { youtubeController.unregisterCallback(youtubeCallback); }
                catch (RuntimeException ignored) { }
            }
            youtubeController = chosenYoutube;
            if (chosenYoutube != null) {
                try { chosenYoutube.registerCallback(youtubeCallback, main); }
                catch (RuntimeException ignored) { youtubeController = null; }
            }
        }
        saveCurrent();
    }

    private void saveCurrent() {
        if (!connected) return;
        if (stremioController != null) {
            try {
                StremioSnapshotStore.capture(this, stremioController.getMetadata(),
                        stremioController.getPlaybackState());
            } catch (RuntimeException ignored) { }
        }
        if (youtubeController != null) {
            try {
                YoutubeSnapshotStore.capture(this, youtubeController.getMetadata(),
                        youtubeController.getPlaybackState());
            } catch (RuntimeException ignored) { }
        }
    }

    private void detach() {
        connected = false;
        main.removeCallbacks(sampling);
        if (sessions != null) {
            try { sessions.removeOnActiveSessionsChangedListener(sessionListener); }
            catch (RuntimeException ignored) { }
        }
        if (stremioController != null) {
            try { stremioController.unregisterCallback(controllerCallback); }
            catch (RuntimeException ignored) { }
        }
        stremioController = null;
        if (youtubeController != null) {
            try { youtubeController.unregisterCallback(youtubeCallback); }
            catch (RuntimeException ignored) { }
        }
        youtubeController = null;
        sessions = null;
    }

    @Override public void onListenerDisconnected() {
        detach();
        // If owner revoked broad listener access, purge private saved Stremio
        // data too. A temporary system disconnect with permission still active
        // should NOT erase their most recent valid viewing card.
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ComponentName component = new ComponentName(this, HybridSessionListener.class);
        if (nm == null || !nm.isNotificationListenerAccessGranted(component)) {
            StremioSnapshotStore.clear(this);
            YoutubeSnapshotStore.clear(this);
        }
        super.onListenerDisconnected();
    }

    @Override public void onDestroy() {
        detach();
        super.onDestroy();
    }

    // Required service callbacks; intentionally do not inspect notifications.
    @Override public void onNotificationPosted(StatusBarNotification notification) { }
    @Override public void onNotificationRemoved(StatusBarNotification notification) { }
}
