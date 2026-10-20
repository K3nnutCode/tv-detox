package com.ansub.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Real frosted-glass backdrop for Android TV 11 (API 30).
 *
 * RenderEffect / window background blur are not available on Android 11. Instead,
 * each slideshow photo is blurred at 1/8th resolution on its decoding thread.
 * Both panels sample the matching on-screen portion of that tiny bitmap.
 * Drawing costs one small shader fill, a translucent tint and a soft edge.
 */
final class FrostedBackdrop extends Drawable {
    private static final int THUMB_WIDTH = 240;
    private final View view;
    private final int screenWidth;
    private final int screenHeight;
    private final float radius;
    private final Paint blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint tint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint edge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path clipping = new Path();
    private final Matrix matrix = new Matrix();
    private final int[] location = new int[2];
    private BitmapShader shader;
    private int blurWidth = 1, blurHeight = 1;

    FrostedBackdrop(View view, int screenWidth, int screenHeight, float radius) {
        this.view = view;
        this.screenWidth = Math.max(1, screenWidth);
        this.screenHeight = Math.max(1, screenHeight);
        this.radius = radius;
        tint.setColor(0x60313B4E); // lighter slate tint: let the blurred photograph show through
        edge.setStyle(Paint.Style.STROKE);
        edge.setStrokeWidth(1.2f);
        edge.setColor(0x1AFFFFFF); // barely perceptible glass edge, not a white tile outline
    }

    void setBlurredPhoto(Bitmap blurred) {
        shader = blurred == null ? null : new BitmapShader(blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        if (blurred != null) { blurWidth = blurred.getWidth(); blurHeight = blurred.getHeight(); }
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom);

        int save = canvas.save();
        clipping.reset();
        clipping.addRoundRect(rect, radius, radius, Path.Direction.CW);
        canvas.clipPath(clipping);

        if (shader != null) {
            view.getLocationOnScreen(location);
            // Source pixels map to full-screen positions, then are offset into the panel.
            matrix.reset();
            matrix.setScale((float) screenWidth / blurWidth,
                    (float) screenHeight / blurHeight);
            matrix.postTranslate(bounds.left - location[0], bounds.top - location[1]);
            shader.setLocalMatrix(matrix);
            blurPaint.setShader(shader);
            canvas.drawRect(rect, blurPaint);
            blurPaint.setShader(null);
        } else {
            canvas.drawColor(0x58334456); // fallback when photos are unavailable
        }
        canvas.drawRect(rect, tint);
        // Pale top-to-bottom light reflection adds a frosted-glass feel without a solid grey box.
        sheen.setShader(new LinearGradient(rect.left, rect.top, rect.left, rect.bottom,
                0x2AFFFFFF, 0x04FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawRect(rect, sheen);
        sheen.setShader(null);
        canvas.restoreToCount(save);
        canvas.drawRoundRect(rect.left + 0.6f, rect.top + 0.6f,
                rect.right - 0.6f, rect.bottom - 0.6f, radius, radius, edge);
    }

    /**
     * Small software box blur, run on a background decoding thread, not the UI thread.
     * Never retain the full-resolution source bitmap.
     */
    static Bitmap blurPhoto(Bitmap source, int screenWidth, int screenHeight) {
        if (source == null || screenWidth <= 0 || screenHeight <= 0) return null;
        try {
            int width = THUMB_WIDTH;
            int height = Math.max(1, Math.round((float) width * screenHeight / screenWidth));
            Bitmap tiny = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            float aspect = (float) screenWidth / screenHeight;
            int sw = source.getWidth(), sh = source.getHeight();
            int cw = sw, ch = sh;
            if (sw > sh * aspect) cw = Math.max(1, Math.round(sh * aspect));
            else ch = Math.max(1, Math.round(sw / aspect));
            Rect crop = new Rect((sw - cw) / 2, (sh - ch) / 2,
                    (sw - cw) / 2 + cw, (sh - ch) / 2 + ch);
            Canvas c = new Canvas(tiny);
            Paint filtered = new Paint(Paint.FILTER_BITMAP_FLAG);
            c.drawBitmap(source, crop, new Rect(0, 0, width, height), filtered);

            int[] first = new int[width * height], second = new int[first.length];
            tiny.getPixels(first, 0, width, 0, 0, width, height);
            for (int pass = 0; pass < 2; pass++) {
                boxBlur(first, second, width, height, 8, true);
                boxBlur(second, first, width, height, 8, false);
            }
            tiny.setPixels(first, 0, width, 0, 0, width, height);
            return tiny;
        } catch (Throwable ignored) { // slideshow still works if low-memory TV cannot blur
            return null;
        }
    }

    private static void boxBlur(int[] input, int[] output, int w, int h, int r, boolean horizontal) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int red = 0, green = 0, blue = 0, count = 0;
                for (int d = -r; d <= r; d++) {
                    int xx = horizontal ? Math.max(0, Math.min(w - 1, x + d)) : x;
                    int yy = horizontal ? y : Math.max(0, Math.min(h - 1, y + d));
                    int p = input[yy * w + xx];
                    red += Color.red(p);
                    green += Color.green(p);
                    blue += Color.blue(p);
                    count++;
                }
                output[y * w + x] = Color.rgb(red / count, green / count, blue / count);
            }
        }
    }

    @Override public void setAlpha(int alpha) { tint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter cf) { tint.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
