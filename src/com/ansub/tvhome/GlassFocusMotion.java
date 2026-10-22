package com.ansub.tvhome;

import android.os.SystemClock;
import android.view.View;

/** One reusable frame callback per Glass tile, independent of animator scale. */
final class GlassFocusMotion implements Runnable, View.OnAttachStateChangeListener {
    private static final long DURATION_MS = 175L;
    private final View view;
    private float fromX, fromY, target;
    private long started;

    GlassFocusMotion(View view) {
        this.view = view;
        view.addOnAttachStateChangeListener(this);
    }

    void moveTo(float scale) {
        view.removeCallbacks(this);
        target = scale;
        if (!view.isAttachedToWindow() || !view.hasWindowFocus()) {
            finish();
            return;
        }
        fromX = view.getScaleX();
        fromY = view.getScaleY();
        if (fromX == scale && fromY == scale) return;
        started = SystemClock.uptimeMillis();
        view.postOnAnimation(this);
    }

    @Override public void run() {
        if (!view.isAttachedToWindow() || !view.hasWindowFocus()) {
            finish();
            return;
        }
        float t = Math.min(1f, Math.max(0f,
                (SystemClock.uptimeMillis() - started) / (float) DURATION_MS));
        float eased = t * t * (3f - 2f * t);
        view.setScaleX(fromX + (target - fromX) * eased);
        view.setScaleY(fromY + (target - fromY) * eased);
        if (t < 1f) view.postOnAnimation(this);
        else finish();
    }

    private void finish() {
        view.removeCallbacks(this);
        view.setScaleX(target);
        view.setScaleY(target);
    }

    @Override public void onViewAttachedToWindow(View v) { }

    @Override public void onViewDetachedFromWindow(View v) {
        view.removeCallbacks(this);
    }
}
