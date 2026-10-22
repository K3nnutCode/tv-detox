package com.ansub.tvhome;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Glass-only Browse surface. Original app artwork, no remote recommendation feed. */
final class GlassBrowseView extends FrameLayout {
    private static final String YOUTUBE = "com.google.android.youtube.tv";
    private static final String STREMIO = "com.stremio.one";
    private final Activity host;
    private final float scale;
    private final ArrayList<App> apps = new ArrayList<>();
    private final ArrayList<Card> cards = new ArrayList<>();
    private View initialFocus;
    private int mediaGeneration;

    private static final class App {
        final String pkg, label;
        final Intent intent;
        final Drawable icon, banner;
        App(String pkg, String label, Intent intent, Drawable icon, Drawable banner) {
            this.pkg = pkg;
            this.label = label;
            this.intent = intent;
            this.icon = icon;
            this.banner = banner;
        }
    }

    private static final class Card {
        final App app;
        final ImageView art;
        final TextView title, detail;
        Card(App app, ImageView art, TextView title, TextView detail) {
            this.app = app;
            this.art = art;
            this.title = title;
            this.detail = detail;
        }
    }

    GlassBrowseView(Activity host, float scale, Runnable photoHome,
                    Runnable inputs, Runnable settings) {
        super(host);
        this.host = host;
        this.scale = scale;
        setContentDescription("Glass Browse");
        setBackground(surface(0xF2E7EDF2, 0xEADAE4ED, 30, 0xBFFFFFFF));
        setClipChildren(false);
        setClipToPadding(false);
        addView(text("Browse", 48, 0xFF182634, true), at(36, 18, 700, 66));
        addView(text("Your apps, together", 23, 0xFF526476, false), at(38, 88, 800, 36));
        TextView home = button("Photo Home", photoHome);
        addView(home, at(1178, 34, 224, 62));
        TextView inputButton = button("Inputs", inputs);
        TextView settingsButton = button("Settings", settings);
        addView(inputButton, at(1422, 34, 160, 62));
        addView(settingsButton, at(1602, 34, 174, 62));
        home.setNextFocusRightId(inputButton.getId());
        inputButton.setNextFocusLeftId(home.getId());
        inputButton.setNextFocusRightId(settingsButton.getId());
        settingsButton.setNextFocusLeftId(inputButton.getId());

        discoverApps();
        for (int i = 0; i < Math.min(3, apps.size()); i++) {
            View card = card(apps.get(i), i == 0);
            card.setNextFocusUpId(home.getId());
            addView(card, i == 0 ? at(36, 150, 970, 398)
                    : at(1030 + (i - 1) * 386, 150, 360, 398));
            if (i == 0) initialFocus = card;
        }
        if (apps.isEmpty()) {
            addView(text("No TV apps are available to open. Inputs and Settings are still accessible.",
                    30, 0xFF34485A, false), at(50, 220, 1500, 190));
            initialFocus = home;
        }

        addView(text("YOUR APPS", 19, 0xFF43596E, true), at(40, 579, 800, 34));
        HorizontalScrollView scroll = new HorizontalScrollView(host);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(px(12), px(12), px(12), px(12));
        scroll.setBackground(surface(0xDFFFFFFF, 0xBFEAF0F5, 30, 0xEFFFFFFF));
        scroll.setElevation(px(5));
        LinearLayout shelf = new LinearLayout(host);
        shelf.setGravity(Gravity.CENTER_VERTICAL);
        shelf.setPadding(px(10), 0, px(10), 0);
        for (App app : apps) {
            LinearLayout tile = new LinearLayout(host);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setPadding(px(10), px(7), px(10), px(7));
            ImageView icon = new ImageView(host);
            icon.setImageDrawable(copy(app.icon));
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(icon, new LinearLayout.LayoutParams(px(64), px(64)));
            TextView label = text(app.label, 19, 0xFF233749, true);
            label.setGravity(Gravity.CENTER);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            tile.addView(label, new LinearLayout.LayoutParams(-1, px(32)));
            focusable(tile, "Browse app: " + app.label, () -> open(app));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(210), px(118));
            lp.setMargins(px(5), 0, px(5), 0);
            shelf.addView(tile, lp);
        }
        scroll.addView(shelf);
        addView(scroll, at(36, 620, 1740, 152));
        addView(text("Back returns to photos  ·  Select an app to open it", 19,
                0xFF526476, false), at(42, 787, 1700, 30));
    }

    void focusFirstCard() { if (initialFocus != null) initialFocus.requestFocus(); }

    private void discoverApps() {
        PackageManager pm = host.getPackageManager();
        LinkedHashMap<String, Intent> found = new LinkedHashMap<>();
        for (String pkg : new String[]{YOUTUBE, STREMIO}) {
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch != null) found.put(pkg, launch);
        }
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        ArrayList<ResolveInfo> published = new ArrayList<>(pm.queryIntentActivities(query, 0));
        published.sort((a, b) -> String.valueOf(a.loadLabel(pm))
                .compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
        for (ResolveInfo info : published) {
            String pkg = info.activityInfo.packageName;
            if (pkg.startsWith("com.ansub.tvhome") || pkg.equals("com.gathin.tvhome")
                    || pkg.contains("launcher") || found.containsKey(pkg)) continue;
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch != null) found.put(pkg, launch);
        }
        for (String pkg : found.keySet()) {
            try {
                String label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                Drawable icon = pm.getApplicationIcon(pkg);
                Drawable banner = null;
                if (apps.size() < 3) {
                    banner = pm.getActivityBanner(found.get(pkg));
                    if (banner == null) banner = pm.getApplicationBanner(pkg);
                }
                apps.add(new App(pkg, label, found.get(pkg), icon, banner));
            } catch (PackageManager.NameNotFoundException ignored) {
                // An app can disappear between discovery and drawing.
            }
        }
    }

    private View card(App app, boolean featured) {
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(px(16), px(16), px(16), px(12));
        card.setClipToOutline(true);
        ImageView art = new ImageView(host);
        art.setBackground(surface(0xFFDAE3EC, 0xFFEDF2F6, 19, 0x00FFFFFF));
        art.setClipToOutline(true);
        art.setScaleType(ImageView.ScaleType.FIT_CENTER);
        art.setPadding(px(28), px(22), px(28), px(22));
        art.setImageDrawable(copy(app.banner != null ? app.banner : app.icon));
        card.addView(art, new LinearLayout.LayoutParams(-1, px(254)));
        TextView title = text(app.label, featured ? 32 : 28, 0xFF1D3041, true);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(px(8), px(10), px(8), 0);
        card.addView(title, new LinearLayout.LayoutParams(-1, px(82)));
        TextView detail = text("Open " + app.label, 20, 0xFF4A6177, false);
        detail.setSingleLine(true);
        detail.setEllipsize(TextUtils.TruncateAt.END);
        detail.setPadding(px(8), 0, px(8), 0);
        card.addView(detail, new LinearLayout.LayoutParams(-1, px(30)));
        cards.add(new Card(app, art, title, detail));
        focusable(card, "Browse featured: " + app.label, () -> open(app));
        return card;
    }

    /** Only read this preview's saved media after the owner has granted access. */
    void refreshSavedPlayback() {
        final int token = ++mediaGeneration;
        for (Card card : cards) {
            card.art.setScaleType(ImageView.ScaleType.FIT_CENTER);
            card.art.setPadding(px(28), px(22), px(28), px(22));
            card.art.setImageDrawable(copy(card.app.banner != null ? card.app.banner : card.app.icon));
            card.title.setText(card.app.label);
            card.detail.setText("Open " + card.app.label);
        }
        if (!hasMediaAccess()) return;
        new Thread(() -> {
            for (Card card : cards) {
                String title = null;
                Bitmap artwork = null;
                try {
                    if (YOUTUBE.equals(card.app.pkg)) {
                        YoutubeSnapshotStore.Snapshot saved = YoutubeSnapshotStore.read(host);
                        if (saved != null) { title = saved.title; artwork = saved.artwork; }
                    } else if (STREMIO.equals(card.app.pkg)) {
                        StremioSnapshotStore.Snapshot saved = StremioSnapshotStore.read(host);
                        if (saved != null) { title = saved.title; artwork = saved.artwork; }
                    }
                    if (artwork != null) artwork.prepareToDraw();
                } catch (RuntimeException | OutOfMemoryError ignored) {
                    // A missed optional snapshot must not crash navigation.
                    continue;
                }
                if (title == null) continue;
                final String savedTitle = title;
                final Bitmap savedArt = artwork;
                host.runOnUiThread(() -> {
                    if (token != mediaGeneration || !isAttachedToWindow() || !hasMediaAccess()) return;
                    card.title.setText(savedTitle);
                    card.detail.setText("Last seen · Open " + card.app.label);
                    if (savedArt != null) {
                        card.art.setPadding(0, 0, 0, 0);
                        card.art.setScaleType(ImageView.ScaleType.FIT_CENTER);
                        card.art.setImageBitmap(savedArt);
                    }
                });
            }
        }, "GlassBrowseMedia").start();
    }

    void cancelPendingMedia() { mediaGeneration++; }

    private boolean hasMediaAccess() {
        NotificationManager nm = (NotificationManager) host.getSystemService(Activity.NOTIFICATION_SERVICE);
        return nm != null && nm.isNotificationListenerAccessGranted(
                new ComponentName(host, HybridSessionListener.class));
    }

    private void open(App app) {
        try { host.startActivity(new Intent(app.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
        catch (RuntimeException e) { Toast.makeText(host, "App unavailable on this TV", Toast.LENGTH_SHORT).show(); }
    }

    private TextView button(String title, Runnable action) {
        TextView button = text(title, 23, 0xFF223A4F, true);
        button.setGravity(Gravity.CENTER);
        focusable(button, "Browse: " + title, action);
        return button;
    }

    private void focusable(View view, String description, Runnable action) {
        view.setId(View.generateViewId());
        view.setContentDescription(description);
        view.setFocusable(true);
        view.setClickable(true);
        Drawable idle = surface(0xDCFFFFFF, 0xBFEAF0F6, 22, 0x9FFFFFFF);
        Drawable focus = surface(0xFFF7FCFF, 0xFFE0F0FF, 22, 0xFF3D789E);
        view.setBackground(idle);
        Runnable appearance = () -> {
            boolean selected = view.hasFocus() && view.hasWindowFocus();
            view.setBackground(selected ? focus : idle);
            view.setElevation(selected ? px(8) : px(2));
            view.animate().scaleX(selected ? 1.025f : 1f)
                    .scaleY(selected ? 1.025f : 1f).setDuration(175).start();
        };
        view.setOnFocusChangeListener((v, focused) -> appearance.run());
        view.getViewTreeObserver().addOnWindowFocusChangeListener(focused -> appearance.run());
        view.setOnClickListener(v -> action.run());
    }

    private Drawable copy(Drawable drawable) {
        if (drawable == null) return null;
        Drawable.ConstantState state = drawable.getConstantState();
        return state == null ? drawable : state.newDrawable(getResources()).mutate();
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView text = new TextView(host);
        text.setText(value);
        text.setTextColor(color);
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, px(size));
        text.setGravity(Gravity.CENTER_VERTICAL);
        if (bold) text.setTypeface(null, Typeface.BOLD);
        return text;
    }

    private GradientDrawable surface(int top, int bottom, int radius, int stroke) {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        background.setCornerRadius(px(radius));
        background.setStroke(px(2), stroke);
        return background;
    }

    private FrameLayout.LayoutParams at(int x, int y, int width, int height) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(width), px(height));
        lp.leftMargin = px(x);
        lp.topMargin = px(y);
        return lp;
    }

    private int px(int value) { return Math.round(value * scale); }
}
