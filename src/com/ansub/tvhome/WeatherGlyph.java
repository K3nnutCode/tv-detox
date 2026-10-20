package com.ansub.tvhome;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

/**
 * Lightweight outlined weather glyphs. No image resources or extra dependencies.
 * Follows Open-Meteo WMO weather codes, with day/night information.
 */
final class WeatherGlyph extends Drawable {
    private final int code;
    private final boolean day;
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    WeatherGlyph(int code, boolean day) {
        this.code = code;
        this.day = day;
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(5f);
    }

    @Override public void draw(Canvas canvas) {
        canvas.save();
        canvas.translate(getBounds().left, getBounds().top);
        canvas.scale(getBounds().width() / 100f, getBounds().height() / 100f);
        boolean clear = code == 0;
        boolean partly = code >= 1 && code <= 2;
        boolean fog = code == 45 || code == 48;
        boolean rain = (code >= 51 && code <= 67) || (code >= 80 && code <= 82);
        boolean snow = (code >= 71 && code <= 77) || (code >= 85 && code <= 86);
        boolean thunder = code >= 95;

        if (clear) {
            if (day) sun(canvas, 50, 50, 17);
            else moon(canvas);
        } else {
            if (partly) {
                if (day) sun(canvas, 34, 35, 13);
                else moon(canvas);
            }
            cloud(canvas);
            if (fog) {
                white();
                canvas.drawLine(29, 80, 70, 80, stroke);
                canvas.drawLine(37, 90, 63, 90, stroke);
            } else if (thunder) {
                stroke.setColor(0xFFFFD77F);
                stroke.setStyle(Paint.Style.STROKE);
                path.reset();
                path.moveTo(53, 70);
                path.lineTo(43, 84);
                path.lineTo(56, 84);
                path.lineTo(48, 96);
                canvas.drawPath(path, stroke);
            } else if (rain) {
                stroke.setColor(0xFFB2DDF7);
                canvas.drawLine(35, 78, 30, 89, stroke);
                canvas.drawLine(55, 78, 50, 89, stroke);
                canvas.drawLine(75, 78, 70, 89, stroke);
            } else if (snow) {
                white();
                for (int x = 37; x <= 67; x += 30) {
                    canvas.drawLine(x - 4, 82, x + 4, 90, stroke);
                    canvas.drawLine(x - 4, 90, x + 4, 82, stroke);
                }
            }
        }
        canvas.restore();
    }

    private void white() { stroke.setColor(0xFFF6F9FC); }

    private void sun(Canvas c, float x, float y, float radius) {
        stroke.setColor(0xFFFFD77F);
        c.drawCircle(x, y, radius, stroke);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * i / 4d;
            float dx = (float) Math.cos(a), dy = (float) Math.sin(a);
            c.drawLine(x + dx * (radius + 7), y + dy * (radius + 7),
                    x + dx * (radius + 13), y + dy * (radius + 13), stroke);
        }
    }

    private void moon(Canvas c) {
        stroke.setColor(0xFFFFDDA0);
        path.reset();
        path.moveTo(52, 19);
        path.cubicTo(30, 23, 26, 54, 45, 67);
        path.cubicTo(55, 74, 69, 69, 75, 59);
        path.cubicTo(53, 63, 39, 36, 52, 19);
        c.drawPath(path, stroke);
    }

    private void cloud(Canvas c) {
        white();
        path.reset();
        path.moveTo(26, 69);
        path.cubicTo(15, 69, 13, 53, 22, 47);
        path.cubicTo(25, 44, 30, 43, 35, 44);
        path.cubicTo(39, 27, 64, 24, 72, 43);
        path.cubicTo(84, 41, 90, 51, 85, 61);
        path.cubicTo(83, 66, 79, 69, 71, 69);
        path.lineTo(26, 69);
        c.drawPath(path, stroke);
    }

    @Override public void setAlpha(int alpha) { stroke.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter filter) { stroke.setColorFilter(filter); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    @Override public int getIntrinsicWidth() { return 90; }
    @Override public int getIntrinsicHeight() { return 90; }
}
