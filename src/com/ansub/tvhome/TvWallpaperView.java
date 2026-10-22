package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Aspect-ratio-safe family wallpaper renderer.
 *
 * Two separate picture frames crossfade so images with different intrinsic
 * dimensions never share drawable bounds (which caused stretching when using
 * TransitionDrawable). Every wallpaper image is full-bleed CENTER_CROP: all
 * aspect ratios fill the 16:9 screen without distortion or letterboxing.
 * Narrow and portrait photos are cropped centrally, not squeezed or fitted.
 *
 * This view is preview-only and reads no files or network resources itself.
 */
final class TvWallpaperView extends FrameLayout {
    // Small custom frame callback keeps the fade visible even when the TV's
    // global Animator duration scale is set to zero (common on Android TVs).
    // It is only active during a photo change, never continuously.
    private static final String TAG = "HybridWallpaperFade";
    private static final int FADE_MS = 1050;
    private final Slide[] slides = new Slide[2];
    private int shown = -1;
    private int fadeGeneration;
    private Runnable pendingFade;

    private static final class Slide {
        final FrameLayout view;
        final ImageView photo;

        Slide(Context context) {
            view = new FrameLayout(context);
            photo = new ImageView(context);
            // Fill every screen, irrespective of wallpaper aspect ratio.
            // CENTER_CROP trims edges without stretching or letterbox bars.
            photo.setScaleType(ImageView.ScaleType.CENTER_CROP);
            photo.setColorFilter(0x2F000000, PorterDuff.Mode.SRC_ATOP);
            view.addView(photo, new FrameLayout.LayoutParams(-1, -1));
            view.setVisibility(View.INVISIBLE);
        }

        void clear() {
            photo.setImageDrawable(null);
        }
    }

    TvWallpaperView(Context context) {
        super(context);
        setBackgroundColor(0xFF0A1222);
        for (int i = 0; i < slides.length; i++) {
            slides[i] = new Slide(context);
            addView(slides[i].view, new FrameLayout.LayoutParams(-1, -1));
        }
    }

    /** Called on the UI thread after an aspect-preserving photo decode. */
    void show(Bitmap image) {
        if (image == null) return;
        // Stop any earlier fade before recycling the inactive frame. No global
        // Android animation settings are changed for the rest of the TV.
        cancelFade();
        final int incoming = shown == 0 ? 1 : 0;
        final Slide next = slides[incoming];
        next.clear();
        next.photo.setImageBitmap(image);
        // Set alpha BEFORE exposing the new photo to prevent a one-frame flash.
        next.view.setAlpha(shown < 0 ? 1f : 0f);
        next.view.setVisibility(VISIBLE);
        next.view.bringToFront();

        if (shown < 0) {
            shown = incoming;
            return;
        }
        final Slide previous = slides[shown];
        shown = incoming;
        final int token = fadeGeneration;
        final long startMs = SystemClock.uptimeMillis();
        Log.i(TAG, "Starting photo crossfade: " + FADE_MS + "ms");
        pendingFade = new Runnable() {
            int frames;
            @Override public void run() {
                if (token != fadeGeneration) return;
                frames++;
                long elapsed = SystemClock.uptimeMillis() - startMs;
                float t = Math.min(1f, Math.max(0f, elapsed / (float) FADE_MS));
                // Smoothstep ease-in/out: no abrupt start or end.
                float opacity = t * t * (3f - 2f * t);
                next.view.setAlpha(opacity);
                if (t < 1f) {
                    next.view.postOnAnimation(this);
                } else {
                    next.view.setAlpha(1f);
                    previous.view.setVisibility(INVISIBLE);
                    // Avoid holding the prior photo bitmap after the fade.
                    // Do not call Bitmap.recycle(): the GPU may still be drawing.
                    previous.clear();
                    pendingFade = null;
                    Log.i(TAG, "Photo crossfade complete: " + elapsed
                            + "ms, " + frames + " frames");
                }
            }
        };
        // View.postOnAnimation is not governed by the system's animator
        // duration scale; it schedules only one callback per display frame.
        next.view.postOnAnimation(pendingFade);
    }

    private void cancelFade() {
        ++fadeGeneration;
        if (pendingFade != null) {
            removeCallbacks(pendingFade);
            slides[0].view.removeCallbacks(pendingFade);
            slides[1].view.removeCallbacks(pendingFade);
            pendingFade = null;
        }
    }

    @Override protected void onDetachedFromWindow() {
        cancelFade();
        super.onDetachedFromWindow();
    }
}
