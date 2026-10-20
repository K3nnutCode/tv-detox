package com.ansub.tvhome;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Aspect-ratio-safe family wallpaper renderer.
 *
 * Two separate picture frames crossfade so images with different intrinsic
 * dimensions never share drawable bounds (which caused stretching when using
 * TransitionDrawable). Every ImageView uses CENTER_CROP or FIT_CENTER, NEVER
 * FIT_XY. Portrait / narrow landscape photos use an offline blurred copy as
 * the screen-filling background and a complete, uncropped image in front.
 *
 * This view is preview-only and reads no files or network resources itself.
 */
final class TvWallpaperView extends FrameLayout {
    // Standard TV is 16:9. Treat 4:3 and portrait images as narrow so they
    // can be seen without cutting faces off at the top/bottom.
    static final float MIN_CROP_ASPECT = 1.55f;
    private static final int FADE_MS = 850;
    private final Slide[] slides = new Slide[2];
    private int shown = -1;

    private static final class Slide {
        final FrameLayout view;
        final ImageView blurred;
        final ImageView photo;

        Slide(Context context) {
            view = new FrameLayout(context);
            blurred = new ImageView(context);
            blurred.setScaleType(ImageView.ScaleType.CENTER_CROP);
            blurred.setColorFilter(0x35000000, PorterDuff.Mode.SRC_ATOP);
            view.addView(blurred, new FrameLayout.LayoutParams(-1, -1));

            photo = new ImageView(context);
            photo.setColorFilter(0x2F000000, PorterDuff.Mode.SRC_ATOP);
            view.addView(photo, new FrameLayout.LayoutParams(-1, -1));
            view.setVisibility(View.INVISIBLE);
        }

        void clear() {
            blurred.setImageDrawable(null);
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

    /** Decide from image dimensions whether preserving the entire photo wins. */
    static boolean useBlurredFill(int sourceWidth, int sourceHeight) {
        return sourceHeight > 0 && sourceWidth > 0
                && (float) sourceWidth / sourceHeight < MIN_CROP_ASPECT;
    }

    /**
     * Prepare a low-resolution, blurred, aspect-ratio-preserving backdrop.
     * Run this on the wallpaper decode thread, NEVER on the Android UI thread.
     */
    static Bitmap makeBlurredFill(Bitmap source, float tvAspect) {
        if (source == null || !useBlurredFill(source.getWidth(), source.getHeight())) {
            return null;
        }
        // Small texture, scaled up only AFTER applying two gentle blur passes.
        final int h = 120;
        final int w = Math.max(120, Math.min(256, Math.round(h * tvAspect)));
        Bitmap backdrop = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(backdrop);

        // Center-crop via src/dst rectangles. Never stretch the original ratio.
        int sw = source.getWidth();
        int sh = source.getHeight();
        float ratio = (float) w / h;
        int cropW = sw, cropH = sh;
        if ((float) sw / sh > ratio) cropW = Math.max(1, Math.round(sh * ratio));
        else cropH = Math.max(1, Math.round(sw / ratio));
        Rect crop = new Rect((sw - cropW) / 2, (sh - cropH) / 2,
                (sw + cropW) / 2, (sh + cropH) / 2);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(source, crop, new Rect(0, 0, w, h), paint);

        // Two separable 7-pixel box blur passes on a tiny texture.
        // No RenderEffect dependency (TCL uses Android 11 / API 30).
        int[] a = new int[w * h];
        int[] temp = new int[a.length];
        int[] output = new int[a.length];
        backdrop.getPixels(a, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 2; pass++) {
            blurHorizontal(a, temp, w, h, 7);
            blurVertical(temp, output, w, h, 7);
            int[] exchange = a; a = output; output = exchange;
        }
        backdrop.setPixels(a, 0, w, 0, 0, w, h);
        return backdrop;
    }

    private static void blurHorizontal(int[] source, int[] dest,
                                       int w, int h, int radius) {
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int red = 0, green = 0, blue = 0, count = 0;
            for (int offset = -radius; offset <= radius; offset++) {
                int px = Math.min(w - 1, Math.max(0, x + offset));
                int color = source[y * w + px];
                red += (color >> 16) & 255;
                green += (color >> 8) & 255;
                blue += color & 255;
                count++;
            }
            dest[y * w + x] = Color.rgb(red / count, green / count, blue / count);
        }
    }

    private static void blurVertical(int[] source, int[] dest,
                                     int w, int h, int radius) {
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int red = 0, green = 0, blue = 0, count = 0;
            for (int offset = -radius; offset <= radius; offset++) {
                int py = Math.min(h - 1, Math.max(0, y + offset));
                int color = source[py * w + x];
                red += (color >> 16) & 255;
                green += (color >> 8) & 255;
                blue += color & 255;
                count++;
            }
            dest[y * w + x] = Color.rgb(red / count, green / count, blue / count);
        }
    }

    /** Called on the UI thread after a photo and optional blur are decoded. */
    void show(Bitmap image, Bitmap backdrop) {
        if (image == null) return;
        final boolean narrow = useBlurredFill(image.getWidth(), image.getHeight());
        final int incoming = shown == 0 ? 1 : 0;
        final Slide next = slides[incoming];
        next.view.animate().cancel();
        next.clear();
        next.photo.setScaleType(narrow ? ImageView.ScaleType.FIT_CENTER
                : ImageView.ScaleType.CENTER_CROP);
        next.photo.setImageBitmap(image);
        next.blurred.setImageBitmap(narrow ? backdrop : null);
        next.blurred.setVisibility(narrow && backdrop != null ? VISIBLE : INVISIBLE);
        next.view.setVisibility(VISIBLE);
        next.view.bringToFront();

        if (shown < 0) {
            next.view.setAlpha(1f);
            shown = incoming;
            return;
        }
        final Slide previous = slides[shown];
        previous.view.animate().cancel();
        next.view.setAlpha(0f);
        shown = incoming;
        next.view.animate().alpha(1f).setDuration(FADE_MS).withEndAction(() -> {
            // Remove old bitmap references to avoid long-lived slideshow RAM.
            // No explicit Bitmap.recycle(): GPU may still be finishing a frame.
            previous.view.setVisibility(INVISIBLE);
            previous.clear();
        }).start();
    }
}
