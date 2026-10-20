package com.ansub.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * High-contrast, filled weather icons on a 100x100 virtual grid, readable from a sofa.
 * Weather is provided by Open-Meteo (WMO codes). No downloaded assets or fonts.
 */
final class WeatherGlyph extends Drawable {
    private final int code;
    private final boolean day;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    WeatherGlyph(int code, boolean day) {
        this.code = code;
        this.day = day;
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override public void draw(Canvas canvas) {
        if (getBounds().isEmpty()) return;
        int save = canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 100f, getBounds().height() / 100f);

        boolean clear = code == 0;
        boolean partlyCloudy = code == 1 || code == 2;
        boolean fog = code == 45 || code == 48;
        boolean raining = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
        boolean snowing = (code >= 71 && code <= 77) || (code >= 85 && code <= 86);
        boolean thunder = code >= 95;

        if (clear) {
            if (day) sun(canvas, 50, 50, 20);
            else moon(canvas, 48, 50);
        } else {
            if (partlyCloudy) {
                if (day) sun(canvas, 34, 31, 15);
                else moon(canvas, 34, 31);
            }
            cloud(canvas, (raining || thunder || snowing || fog) ? 0 : 3);
            if (fog) {
                style(0xFFF4F8FF, 5f, true);
                canvas.drawLine(28, 83, 72, 83, paint);
                canvas.drawLine(38, 93, 62, 93, paint);
            } else if (thunder) {
                style(0xFFFFCE65, 0, false);
                path.reset();
                path.moveTo(53, 72);
                path.lineTo(39, 87);
                path.lineTo(52, 87);
                path.lineTo(45, 99);
                path.lineTo(68, 81);
                path.lineTo(54, 81);
                path.close();
                canvas.drawPath(path, paint);
            } else if (raining) {
                style(0xFF8BCFF9, 6f, true);
                canvas.drawLine(32, 77, 26, 91, paint);
                canvas.drawLine(53, 77, 47, 91, paint);
                canvas.drawLine(74, 77, 68, 91, paint);
            } else if (snowing) {
                style(0xFFF4F8FF, 5f, true);
                for (int x = 35; x <= 69; x += 34) {
                    canvas.drawLine(x - 5, 82, x + 5, 92, paint);
                    canvas.drawLine(x - 5, 92, x + 5, 82, paint);
                }
            }
        }
        canvas.restoreToCount(save);
    }

    private void style(int color, float stroke, boolean line) {
        paint.setColor(color);
        paint.setStyle(line ? Paint.Style.STROKE : Paint.Style.FILL);
        if (line) paint.setStrokeWidth(stroke);
    }

    private void sun(Canvas c, float x, float y, float r) {
        style(0xFFFFD46F, 0f, false);
        c.drawCircle(x, y, r, paint);
        style(0xFFFFD46F, 6f, true);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * i / 4d;
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
            c.drawLine(x + dx * (r + 7), y + dy * (r + 7),
                    x + dx * (r + 16), y + dy * (r + 16), paint);
        }
    }

    private void moon(Canvas c, float x, float y) {
        style(0xFFFFDE92, 0f, false);
        path.reset();
        path.moveTo(x + 2, y - 22);
        path.cubicTo(x - 22, y - 20, x - 26, y + 12, x - 8, y + 23);
        path.cubicTo(x + 3, y + 30, x + 18, y + 22, x + 23, y + 10);
        path.cubicTo(x + 2, y + 14, x - 5, y - 8, x + 2, y - 22);
        path.close();
        c.drawPath(path, paint);
    }

    private void cloud(Canvas c, int offset) {
        style(0xFFF6F9FF, 0f, false);
        path.reset();
        path.moveTo(26, 72 + offset);
        path.cubicTo(14, 72 + offset, 10, 58 + offset, 17, 49 + offset);
        path.cubicTo(22, 43 + offset, 29, 42 + offset, 35, 44 + offset);
        path.cubicTo(41, 26 + offset, 64, 23 + offset, 72, 43 + offset);
        path.cubicTo(85, 42 + offset, 94, 52 + offset, 90, 63 + offset);
        path.cubicTo(88, 69 + offset, 81, 72 + offset, 73, 72 + offset);
        path.close();
        c.drawPath(path, paint);
    }

    @Override public void setAlpha(int a) { paint.setAlpha(a); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter cf) { paint.setColorFilter(cf); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 100; }
    @Override public int getIntrinsicHeight() { return 100; }
}
