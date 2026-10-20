package com.ansub.tvhome;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;

/**
 * Full-bleed landscape TV artwork for apps whose stock Leanback banners have
 * baked-in whitespace or mobile-icon presentation.
 *
 * This is deliberately offline, self-contained vector art. No network image
 * queries, APK resource overrides, stored permissions or custom font files.
 * Renders at any dock tile size; touch and remote focus are owned by the tile.
 */
final class TvDockArt extends Drawable {
    static final int YOUTUBE = 1;
    static final int STREMIO = 2;
    static final int PLAY_STORE = 3;
    static final int QUICK_PANEL = 4;

    private static final float W = 260f;
    private static final float H = 136f;
    private final int style;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private int alpha = 255;
    private ColorFilter filter;

    TvDockArt(int style) { this.style = style; }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        int save = canvas.save();
        canvas.translate(b.left, b.top);
        canvas.scale(b.width() / W, b.height() / H);
        canvas.clipPath(roundedFrame());

        switch (style) {
            case YOUTUBE:
                gradient(canvas, 0xFF301019, 0xFF080E1C);
                ribbon(canvas, 104, 0xB5B90C24, 0x702B0C26);
                youtubeLogo(canvas);
                break;
            case STREMIO:
                gradient(canvas, 0xFF1D0C48, 0xFF07112E);
                ribbon(canvas, 115, 0x9A4222C1, 0x8A071A59);
                filmStroke(canvas);
                stremioLogo(canvas);
                break;
            case PLAY_STORE:
                gradient(canvas, 0xFF071E23, 0xFF0A1424);
                // App-branded colour sweep, not a white image in a dark box.
                sweep(canvas);
                playStoreLogo(canvas);
                break;
            case QUICK_PANEL:
                gradient(canvas, 0xFF2459A0, 0xFF0D2546);
                ribbon(canvas, 100, 0xA72D9BE9, 0x83142664);
                quickPanelLogo(canvas);
                break;
            default:
                gradient(canvas, 0xFF25334B, 0xFF121A29);
        }

        // A very light inner edge; exterior selection outline is rendered by
        // the parent tile on top of this independent TV artwork.
        p.reset(); p.setAntiAlias(true); p.setStyle(Paint.Style.STROKE);
        p.setColor(0x42FFFFFF); p.setStrokeWidth(1f);
        canvas.drawRoundRect(1, 1, W - 1, H - 1, 18, 18, p);
        p.setStyle(Paint.Style.FILL);
        canvas.restoreToCount(save);
    }

    private Path roundedFrame() {
        Path path = new Path();
        path.addRoundRect(0, 0, W, H, 18, 18, Path.Direction.CW);
        return path;
    }

    private void gradient(Canvas c, int left, int right) {
        p.reset(); p.setAntiAlias(true); p.setAlpha(alpha);
        p.setShader(new LinearGradient(0, 0, W, H, left, right, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, W, H, p);
        p.setShader(null);
    }

    private void ribbon(Canvas c, float baseY, int upper, int lower) {
        p.reset(); p.setAntiAlias(true); p.setColor(upper);
        Path x = new Path();
        x.moveTo(0, baseY + 17);
        x.cubicTo(65, baseY - 14, 148, baseY + 70, W, baseY - 45);
        x.lineTo(W, H);
        x.lineTo(0, H);
        x.close();
        c.drawPath(x, p);
        p.setColor(lower);
        Path y = new Path();
        y.moveTo(0, H - 12);
        y.cubicTo(80, H - 68, 178, H + 7, W, H - 54);
        y.lineTo(W, H); y.lineTo(0, H); y.close();
        c.drawPath(y, p);
    }

    private void filmStroke(Canvas c) {
        p.reset(); p.setAntiAlias(true);
        p.setColor(0x273F87D9); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3);
        c.drawLine(151, 139, 244, 68, p);
        c.drawLine(173, 144, 259, 85, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x2245BFF0);
        for (int i = 0; i < 6; i++) {
            c.drawRoundRect(156 + i * 16, 128 - i * 11, 164 + i * 16,
                    134 - i * 11, 2, 2, p);
        }
    }

    private void youtubeLogo(Canvas c) {
        p.reset(); p.setAntiAlias(true); p.setColor(0xFFFF2025);
        c.drawRoundRect(18, 40, 94, 94, 13, 13, p);
        triangle(c, 49, 53, 49, 81, 73, 67, Color.WHITE);
        word(c, "YouTube", 103, 78, 33, true, Color.WHITE);
    }

    private void stremioLogo(Canvas c) {
        p.reset(); p.setAntiAlias(true);
        p.setShader(new LinearGradient(22, 28, 88, 105, 0xFFA77FFF, 0xFF5147F7,
                Shader.TileMode.CLAMP));
        Path diamond = new Path();
        diamond.moveTo(52, 25); diamond.lineTo(94, 68);
        diamond.lineTo(52, 111); diamond.lineTo(10, 68); diamond.close();
        c.drawPath(diamond, p); p.setShader(null);
        triangle(c, 42, 53, 42, 83, 67, 68, Color.WHITE);
        word(c, "stremio", 104, 79, 35, false, Color.WHITE);
    }

    private void playStoreLogo(Canvas c) {
        // Multicolour triangular Play mark, with landscape-safe wordmark.
        polygon(c, 20, 38, 20, 101, 53, 68, 0xFF34B7E4);
        polygon(c, 20, 38, 53, 68, 83, 59, 0xFF44BF74);
        polygon(c, 20, 101, 53, 68, 83, 77, 0xFFE84E52);
        polygon(c, 53, 68, 83, 59, 97, 68, 83, 77, 0xFFF6CB39);
        word(c, "Google Play", 100, 78, 30, true, Color.WHITE);
    }

    private void quickPanelLogo(Canvas c) {
        diamond(c, 130, 59, 58, 25, 0xFF6FC9FA);
        diamond(c, 130, 86, 58, 25, 0xFFB3C5E8);
        diamond(c, 130, 72, 58, 25, 0xFF3190E5);
    }

    private void diamond(Canvas c, float cx, float cy, float width, float height,
                         int color) {
        Path d = new Path();
        d.moveTo(cx, cy - height / 2f);
        d.lineTo(cx + width / 2f, cy);
        d.lineTo(cx, cy + height / 2f);
        d.lineTo(cx - width / 2f, cy);
        d.close();
        p.reset(); p.setAntiAlias(true); p.setColor(color);
        c.drawPath(d, p);
    }

    private void triangle(Canvas c, float x1, float y1,
                          float x2, float y2, float x3, float y3, int color) {
        Path t = new Path();
        t.moveTo(x1, y1); t.lineTo(x2, y2); t.lineTo(x3, y3); t.close();
        p.reset(); p.setAntiAlias(true); p.setColor(color);
        c.drawPath(t, p);
    }

    private void polygon(Canvas c, float x1, float y1, float x2, float y2,
                         float x3, float y3, int color) {
        triangle(c, x1, y1, x2, y2, x3, y3, color);
    }

    private void polygon(Canvas c, float x1, float y1, float x2, float y2,
                         float x3, float y3, float x4, float y4, int color) {
        Path t = new Path();
        t.moveTo(x1,y1); t.lineTo(x2,y2); t.lineTo(x3,y3);
        t.lineTo(x4,y4); t.close();
        p.reset(); p.setAntiAlias(true); p.setColor(color);
        c.drawPath(t,p);
    }

    private void word(Canvas c, String text, float x, float baseline,
                      float size, boolean bold, int color) {
        p.reset(); p.setAntiAlias(true); p.setFilterBitmap(true);
        p.setColor(color); p.setTextSize(size);
        p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        // Never overflow when native font metrics differ from emulator.
        float maxWidth = W - x - 12;
        float desired = p.measureText(text);
        if (desired > maxWidth) p.setTextScaleX(maxWidth / desired);
        c.drawText(text, x, baseline, p);
    }

    @Override public void setAlpha(int value) { alpha = value; invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter value) { filter = value; invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
