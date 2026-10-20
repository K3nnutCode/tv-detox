package com.ansub.stremioprobe;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/**
 * Local, read-only diagnostic for Stremio's Android media session.
 * Never enumerates notifications, queries other apps' metadata, sends requests,
 * persists viewing data, changes Home, or starts/stops media playback.
 */
public final class MainActivity extends Activity {
    private static final String STREMIO = "com.stremio.one";
    private static final int TEXT = 0xFFF3F6FA;
    private static final int MUTED = 0xFFBCC8D6;
    private TextView output;
    private ImageView artwork;
    private Button refreshButton;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setBackgroundColor(0xFF101827);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(46), dp(28), dp(46), dp(24));
        scroll.addView(root);

        TextView heading = text("STREMIO · MEDIA SESSION TEST", 27, TEXT);
        heading.setTypeface(null, 1);
        root.addView(heading);
        TextView notice = text("Local read-only test · No network · No Home change", 18, MUTED);
        LinearLayout.LayoutParams noticeParams = new LinearLayout.LayoutParams(-1, -2);
        noticeParams.topMargin = dp(5);
        root.addView(notice, noticeParams);

        TextView privacy = text(
                "Notification-listener access is broad: Android may allow this app to see " +
                "other apps' notifications. This test does not read notification contents; " +
                "it queries only Stremio's media session. Turn access off after testing.", 17, MUTED);
        LinearLayout.LayoutParams privacyParams = new LinearLayout.LayoutParams(-1, -2);
        privacyParams.topMargin = dp(17);
        root.addView(privacy, privacyParams);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams buttonsParams = new LinearLayout.LayoutParams(-1, -2);
        buttonsParams.topMargin = dp(18);
        root.addView(buttons, buttonsParams);
        refreshButton = button("Refresh Stremio status");
        buttons.addView(refreshButton);
        Button settings = button("Notification access settings");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-2, -2);
        sp.leftMargin = dp(18);
        buttons.addView(settings, sp);

        output = text("Waiting for check...", 23, TEXT);
        output.setLineSpacing(dp(7), 1f);
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(-1, -2);
        op.topMargin = dp(26);
        root.addView(output, op);

        artwork = new ImageView(this);
        artwork.setAdjustViewBounds(true);
        artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);
        artwork.setVisibility(View.GONE);
        LinearLayout.LayoutParams artworkParams = new LinearLayout.LayoutParams(dp(400), dp(200));
        artworkParams.topMargin = dp(16);
        root.addView(artwork, artworkParams);

        setContentView(scroll);
        refreshButton.setOnClickListener(v -> refresh());
        settings.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
            catch (Exception e) { output.setText("This TV cannot open notification-access settings automatically. Please return to Home and use TV settings, or ask for the explicit ADB method."); }
        });
        refreshButton.requestFocus();
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (output != null) refresh();
    }

    private void refresh() {
        artwork.setVisibility(View.GONE);
        artwork.setImageDrawable(null);

        NotificationManager notifications =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ComponentName listener = new ComponentName(this, SessionAccessService.class);
        boolean enabled = notifications != null &&
                notifications.isNotificationListenerAccessGranted(listener);

        if (!enabled) {
            output.setText("STATUS: Notification-listener access is OFF.\n\n" +
                    "Enable 'Stremio Session Test access' through Android settings, " +
                    "then return here and press Refresh.\n\n" +
                    "This access applies system-wide; please disable it when the test is complete.");
            return;
        }

        MediaSessionManager manager =
                (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        if (manager == null) {
            output.setText("STATUS: Android media-session service unavailable.");
            return;
        }

        try {
            List<MediaController> sessions = manager.getActiveSessions(listener);
            int count = 0;
            StringBuilder result = new StringBuilder("ACCESS GRANTED · No Internet access\n\n");
            for (MediaController controller : sessions) {
                if (!STREMIO.equals(controller.getPackageName())) continue;
                count++;
                MediaMetadata metadata = controller.getMetadata();
                PlaybackState state = controller.getPlaybackState();

                result.append("STREMIO MEDIA SESSION DETECTED\n");
                result.append("Package: ").append(STREMIO).append("\n");
                if (state == null) result.append("Playback state: unavailable\n");
                else {
                    result.append("Playback state: ").append(stateName(state.getState())).append("\n");
                    result.append("Position: ").append(duration(state.getPosition())).append("\n");
                    result.append("Resume action exposed: ")
                            .append((state.getActions() & PlaybackState.ACTION_PLAY) != 0 ? "yes" : "no")
                            .append("\n");
                }

                if (metadata == null) {
                    result.append("Metadata: unavailable\n");
                    continue;
                }
                result.append("Title: ").append(field(metadata, MediaMetadata.METADATA_KEY_TITLE)).append("\n");
                result.append("Display title: ").append(field(metadata, MediaMetadata.METADATA_KEY_DISPLAY_TITLE)).append("\n");
                result.append("Subtitle / artist: ").append(field(metadata, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)).append(" / ")
                        .append(field(metadata, MediaMetadata.METADATA_KEY_ARTIST)).append("\n");
                result.append("Album / series: ").append(field(metadata, MediaMetadata.METADATA_KEY_ALBUM)).append("\n");
                long durationMs = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);
                result.append("Duration: ").append(durationMs > 0 ? duration(durationMs) : "not supplied").append("\n");
                if (state != null && durationMs > 0 && state.getPosition() >= 0) {
                    long percentage = Math.min(100, state.getPosition() * 100 / durationMs);
                    result.append("Progress: ").append(percentage).append("%\n");
                } else {
                    result.append("Progress: cannot calculate without duration\n");
                }

                Bitmap image = metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
                if (image == null) image = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
                if (image == null) image = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
                if (image != null) {
                    result.append("Artwork bitmap: YES (").append(image.getWidth())
                            .append(" x ").append(image.getHeight()).append(")\n");
                    if (artwork.getVisibility() != View.VISIBLE) {
                        artwork.setImageBitmap(image);
                        artwork.setVisibility(View.VISIBLE);
                    }
                } else {
                    String uri = field(metadata, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI);
                    if ("not supplied".equals(uri)) uri = field(metadata, MediaMetadata.METADATA_KEY_ART_URI);
                    if ("not supplied".equals(uri)) uri = field(metadata, MediaMetadata.METADATA_KEY_ALBUM_ART_URI);
                    // Never display or fetch raw artwork URIs; they may contain access tokens.
                    result.append("Artwork URI: ").append("not supplied".equals(uri) ? "no" : "yes, not fetched").append("\n");
                }
                result.append("\n");
            }
            if (count == 0) result.append(
                    "No active Stremio session visible. Pause a video in Stremio, return here and Refresh.");
            output.setText(result.toString());
        } catch (SecurityException e) {
            output.setText("Android refused media-session access. Recheck 'Stremio Session Test access' " +
                    "in notification-access settings and try again.");
        } catch (RuntimeException e) {
            output.setText("Couldn't read Stremio's session: " + e.getClass().getSimpleName() +
                    ". No changes were made.");
        }
    }

    private static String field(MediaMetadata metadata, String key) {
        CharSequence value = metadata.getText(key);
        return value == null || value.length() == 0 ? "not supplied" : value.toString();
    }

    private static String duration(long ms) {
        if (ms < 0) return "unknown";
        long seconds = ms / 1000;
        return String.format(Locale.US, "%d:%02d:%02d",
                seconds / 3600, (seconds / 60) % 60, seconds % 60);
    }

    private static String stateName(int code) {
        switch (code) {
            case PlaybackState.STATE_PLAYING: return "PLAYING";
            case PlaybackState.STATE_PAUSED: return "PAUSED";
            case PlaybackState.STATE_STOPPED: return "STOPPED";
            case PlaybackState.STATE_NONE: return "NONE";
            case PlaybackState.STATE_BUFFERING: return "BUFFERING";
            default: return "code " + code;
        }
    }

    private Button button(String title) {
        Button b = new Button(this);
        b.setText(title);
        b.setTextSize(16f);
        b.setAllCaps(false);
        b.setFocusable(true);
        b.setTextColor(Color.WHITE);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF33465F);
        bg.setCornerRadius(dp(12));
        b.setBackground(bg);
        b.setPadding(dp(20), dp(12), dp(20), dp(12));
        return b;
    }

    private TextView text(String label, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setGravity(Gravity.START);
        return t;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
