package com.ansub.tvhome;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Glass-only, photo-first app grid. No catalog, duplicate shelf or media reads. */
final class GlassBrowseView extends FrameLayout {
    private static final int COLUMNS = 4;
    private final Activity host;
    private final float scale;
    private final ArrayList<Runnable> focusAppearances = new ArrayList<>();
    private final ArrayList<View> cells = new ArrayList<>();

    private static final class App {
        final String label;
        final Intent intent;
        final Drawable icon;
        App(String label, Intent intent, Drawable icon) {
            this.label = label;
            this.intent = intent;
            this.icon = icon;
        }
    }

    GlassBrowseView(Activity host, float scale, Runnable photoHome,
                    Runnable inputs, Runnable settings) {
        super(host);
        this.host = host;
        this.scale = scale;
        setContentDescription("Glass Browse");
        setClipChildren(false);
        setClipToPadding(false);

        TextView home = text("‹  Photo Home", 23, 0xFFE0E9F3, false);
        home.setGravity(Gravity.CENTER_VERTICAL);
        home.setPadding(px(14), 0, px(14), 0);
        focusable(home, "Browse: Photo Home", photoHome, true);
        addView(home, at(72, 36, 250, 56));
        addView(text("Browse", 50, 0xFFFFFFFF, true), at(86, 100, 920, 66));

        ArrayList<App> apps = discoverApps();
        int firstPageApps = Math.min(6, apps.size());
        for (int i = 0; i < firstPageApps; i++) cells.add(appTile(apps.get(i)));
        cells.add(tile("Inputs", new LineIcon(LineIcon.HDMI), "Browse: Inputs", inputs));
        cells.add(tile("Settings", new LineIcon(LineIcon.SETTINGS), "Browse: Settings", settings));
        for (int i = firstPageApps; i < apps.size(); i++) cells.add(appTile(apps.get(i)));

        ScrollView scroll = new ScrollView(host);
        scroll.setContentDescription("Browse app grid");
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setClipChildren(true);
        scroll.setClipToPadding(true);
        scroll.setPadding(px(14), px(14), px(14), px(14));
        LinearLayout rows = new LinearLayout(host);
        rows.setOrientation(LinearLayout.VERTICAL);
        for (int start = 0; start < cells.size(); start += COLUMNS) {
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < COLUMNS; col++) {
                int i = start + col;
                View child = i < cells.size() ? cells.get(i) : new View(host);
                LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, px(184), 1f);
                cp.setMargins(px(10), 0, px(10), 0);
                row.addView(child, cp);
            }
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(-1, px(184));
            rp.bottomMargin = px(20);
            rows.addView(row, rp);
        }
        scroll.addView(rows);
        addView(scroll, at(62, 206, 1104, 432));

        // Explicit grid neighbours prevent focus escaping to the hidden Home dock.
        for (int i = 0; i < cells.size(); i++) {
            View cell = cells.get(i);
            cell.setNextFocusLeftId(cells.get(i % COLUMNS == 0 ? i : i - 1).getId());
            cell.setNextFocusRightId(cells.get(i % COLUMNS == COLUMNS - 1
                    || i + 1 == cells.size() ? i : i + 1).getId());
            cell.setNextFocusUpId(i < COLUMNS ? home.getId() : cells.get(i - COLUMNS).getId());
            int below = i + COLUMNS;
            if (below >= cells.size() && i / COLUMNS < (cells.size() - 1) / COLUMNS)
                below = cells.size() - 1;
            cell.setNextFocusDownId(cells.get(below < cells.size() ? below : i).getId());
        }
        home.setNextFocusDownId(cells.get(0).getId());
        home.setNextFocusLeftId(home.getId());
        home.setNextFocusRightId(home.getId());
        home.setNextFocusUpId(home.getId());

        String hint = "Arrows Navigate  ·  OK Open  ·  Back Photo Home";
        if (cells.size() > 8) hint += "  ·  Scroll for all apps";
        addView(text(hint, 19, 0xFFD2DFED, false), at(86, 744, 1660, 38));
    }

    void focusFirstItem() { cells.get(0).requestFocus(); }

    @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        for (Runnable appearance : focusAppearances) appearance.run();
    }

    private ArrayList<App> discoverApps() {
        PackageManager pm = host.getPackageManager();
        LinkedHashMap<String, Intent> found = new LinkedHashMap<>();
        for (String pkg : new String[]{"com.google.android.youtube.tv", "com.stremio.one", "com.android.vending"}) {
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
                    || pkg.equals("com.android.tv.settings") || pkg.equals("com.android.settings")
                    || pkg.contains("launcher") || found.containsKey(pkg)) continue;
            Intent launch = pm.getLeanbackLaunchIntentForPackage(pkg);
            if (launch != null) found.put(pkg, launch);
        }
        ArrayList<App> apps = new ArrayList<>();
        for (String pkg : found.keySet()) {
            try {
                String label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                apps.add(new App(label, found.get(pkg), pm.getApplicationIcon(pkg)));
            } catch (PackageManager.NameNotFoundException ignored) {
                // A disappearing app must not leave a fake shortcut behind.
            }
        }
        return apps;
    }

    private View appTile(App app) {
        return tile(app.label, app.icon, "Browse app: " + app.label, () -> {
            try { host.startActivity(new Intent(app.intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (RuntimeException e) {
                Toast.makeText(host, "App unavailable on this TV", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View tile(String label, Drawable icon, String description, Runnable action) {
        LinearLayout tile = new LinearLayout(host);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(px(12), px(20), px(12), px(18));
        tile.setClipToOutline(true);
        ImageView artwork = new ImageView(host);
        Drawable.ConstantState state = icon == null ? null : icon.getConstantState();
        artwork.setImageDrawable(state == null ? icon : state.newDrawable(getResources()).mutate());
        artwork.setScaleType(ImageView.ScaleType.FIT_CENTER);
        artwork.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        tile.addView(artwork, new LinearLayout.LayoutParams(px(72), px(72)));
        TextView caption = text(label, 24, 0xFFF4F7FC, false);
        caption.setSingleLine(true);
        caption.setEllipsize(TextUtils.TruncateAt.END);
        caption.setGravity(Gravity.CENTER);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, px(38));
        cp.topMargin = px(16);
        tile.addView(caption, cp);
        focusable(tile, description, action, false);
        return tile;
    }

    private void focusable(View view, String description, Runnable action, boolean quiet) {
        view.setId(View.generateViewId());
        view.setContentDescription(description);
        view.setFocusable(true);
        view.setClickable(true);
        Drawable idle = surface(quiet ? 0x001A2B40 : 0xA3263547,
                quiet ? 0x001A2B40 : 0x8A182333, quiet ? 0x00FFFFFF : 0x24FFFFFF);
        Drawable focus = surface(0xDB304C66, 0xD4253B52, 0xFFA9D9FF);
        view.setBackground(idle);
        GlassFocusMotion motion = new GlassFocusMotion(view);
        Runnable appearance = () -> {
            boolean selected = view.hasFocus() && view.hasWindowFocus();
            view.setBackground(selected ? focus : idle);
            view.setElevation(selected ? px(6) : 0);
            motion.moveTo(selected ? 1.025f : 1f);
        };
        focusAppearances.add(appearance);
        view.setOnFocusChangeListener((v, focused) -> appearance.run());
        view.setOnClickListener(v -> action.run());
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

    private GradientDrawable surface(int top, int bottom, int stroke) {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{top, bottom});
        background.setCornerRadius(px(22));
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
