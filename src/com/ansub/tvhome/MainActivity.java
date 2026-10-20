diff --git a/src/com/ansub/tvhome/MainActivity.java b/src/com/ansub/tvhome/MainActivity.java
index 09dd180..dbe9461 100644
--- a/src/com/ansub/tvhome/MainActivity.java
+++ b/src/com/ansub/tvhome/MainActivity.java
@@ -11,18 +11,27 @@ import android.content.pm.ApplicationInfo;
 import android.content.pm.PackageManager;
 import android.content.pm.ResolveInfo;
 import android.graphics.Bitmap;
+import android.graphics.BitmapFactory;
+import android.graphics.BitmapRegionDecoder;
 import android.graphics.Canvas;
 import android.graphics.Color;
+import android.graphics.Matrix;
+import android.graphics.PorterDuff;
+import android.graphics.Rect;
 import android.graphics.Typeface;
 import android.graphics.drawable.BitmapDrawable;
 import android.graphics.drawable.ColorDrawable;
 import android.graphics.drawable.Drawable;
 import android.graphics.drawable.GradientDrawable;
 import android.graphics.drawable.LayerDrawable;
+import android.graphics.drawable.TransitionDrawable;
+import android.media.ExifInterface;
 import android.media.tv.TvContract;
 import android.media.tv.TvInputInfo;
 import android.media.tv.TvInputManager;
 import android.os.Bundle;
+import android.os.Handler;
+import android.os.Looper;
 import android.provider.Settings;
 import android.util.TypedValue;
 import android.view.Gravity;
@@ -46,6 +55,7 @@ import java.util.Arrays;
 import java.util.Collections;
 import java.util.TreeMap;
 import java.util.List;
+import java.util.Locale;
 
 public class MainActivity extends Activity {
 
@@ -84,6 +94,24 @@ public class MainActivity extends Activity {
     private TextView moveHint;  // shown above the apps while moving
     private boolean longPressed; // OK was held: swallow the key-up so the app doesn't open
 
+    // ---- Photo slideshow behind the home screen ----
+    // Put photos (jpg, png, webp) in the app's own folder, so no storage permission is needed:
+    //   /sdcard/Android/data/com.ansub.tvhome/files/wallpapers/
+    // With no photos in that folder the home screen looks exactly as it did before.
+    private static final long SLIDE_MS = 60000;      // how long each photo stays up
+    private static final int FADE_MS = 1000;         // fade between photos
+    private static final int PHOTO_DIM = 0x26000000; // 15% black over every photo
+    private static final int BOX_COLOR = 0x99000000; // 60% black box behind the date and time
+    private static final int BOX_PAD_X = 32, BOX_PAD_Y = 20, BOX_RADIUS = 28;
+    private final Handler ui = new Handler(Looper.getMainLooper());
+    private ImageView photoView;
+    private View bottomScrim;
+    private LinearLayout clockBox;
+    private List<File> photos = new ArrayList<>();
+    private int photoIndex = -1;
+    private boolean slideshowOn;
+    private int loadToken; // bumped to drop a photo that was still being decoded
+
     private final BroadcastReceiver packagesChanged = new BroadcastReceiver() {
         @Override public void onReceive(Context c, Intent i) { dirty = true; }
     };
@@ -116,14 +144,30 @@ public class MainActivity extends Activity {
         LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-2, -2);
         tp.topMargin = px(-20); // pull the big digits up under the date
         clock.addView(time, tp);
-        root.addView(clock, at(px(MARGIN), px(92), -2, -2));
+        clockBox = clock; // the dark box is drawn around it only while a photo is showing
+        clock.setPadding(px(BOX_PAD_X), px(BOX_PAD_Y), px(BOX_PAD_X), px(BOX_PAD_Y));
+        root.addView(clock, at(px(MARGIN - BOX_PAD_X), px(92 - BOX_PAD_Y), -2, -2));
 
         moveHint = text(px(24), 0xA8FFFFFF, regular);
         moveHint.setText("Use the arrows to move it, then press OK");
         moveHint.setVisibility(View.GONE);
         root.addView(moveHint, at(px(MARGIN), px(ROW1_TOP - 64), -2, -2));
 
-        setContentView(scroller);
+        // Layers, back to front: photo, a dark fade at the bottom for the labels, then the page.
+        FrameLayout stage = new FrameLayout(this);
+        photoView = new ImageView(this);
+        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
+        photoView.setColorFilter(PHOTO_DIM, PorterDuff.Mode.SRC_ATOP);
+        stage.addView(photoView, new FrameLayout.LayoutParams(-1, -1));
+        bottomScrim = new View(this);
+        bottomScrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
+                new int[] {0xCC000000, 0x00000000}));
+        bottomScrim.setVisibility(View.GONE);
+        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(-1, px(620));
+        sp.gravity = Gravity.BOTTOM;
+        stage.addView(bottomScrim, sp);
+        stage.addView(scroller, new FrameLayout.LayoutParams(-1, -1));
+        setContentView(stage);
 
         IntentFilter f = new IntentFilter();
         f.addAction(Intent.ACTION_PACKAGE_ADDED);
@@ -136,6 +180,9 @@ public class MainActivity extends Activity {
 
     @Override
     protected void onDestroy() {
+        slideshowOn = false;
+        loadToken++;
+        ui.removeCallbacksAndMessages(null);
         unregisterReceiver(packagesChanged);
         super.onDestroy();
     }
@@ -197,6 +244,21 @@ public class MainActivity extends Activity {
         }
         View f = getCurrentFocus();
         if (f == null && !tiles.isEmpty()) tiles.get(0).requestFocus();
+        startSlideshow();
+    }
+
+    @Override
+    protected void onPause() {
+        stopSlideshow();
+        super.onPause();
+    }
+
+    @Override
+    protected void onStop() {
+        // Away from the home screen (YouTube, Stremio...): give the photo's memory back.
+        photoView.setImageDrawable(null);
+        showPhotoStyle(false);
+        super.onStop();
     }
 
     @Override
@@ -733,6 +795,165 @@ public class MainActivity extends Activity {
         }
     }
 
+    // ---------------- Photo slideshow ----------------
+
+    private File photoDir() {
+        File base = getExternalFilesDir(null);
+        if (base == null) return null;
+        File d = new File(base, "wallpapers");
+        if (!d.exists()) d.mkdirs();
+        return d;
+    }
+
+    private void scanPhotos() {
+        List<File> found = new ArrayList<>();
+        try {
+            File dir = photoDir();
+            File[] all = dir == null ? null : dir.listFiles();
+            if (all != null) {
+                for (File f : all) {
+                    String n = f.getName().toLowerCase(Locale.US);
+                    if (f.isFile() && (n.endsWith(".jpg") || n.endsWith(".jpeg")
+                            || n.endsWith(".png") || n.endsWith(".webp"))) found.add(f);
+                }
+            }
+        } catch (Throwable t) { /* no photos: keep the normal look */ }
+        Collections.sort(found);
+        photos = found;
+    }
+
+    private void startSlideshow() {
+        if (slideshowOn) return;
+        slideshowOn = true;
+        scanPhotos(); // picks up photos you added since last time
+        if (photos.isEmpty()) { showPhotoStyle(false); return; }
+        loadNext();
+    }
+
+    private void stopSlideshow() {
+        slideshowOn = false;
+        loadToken++;
+        ui.removeCallbacksAndMessages(null);
+        Drawable d = photoView.getDrawable(); // stop mid-fade on the new photo
+        if (d instanceof TransitionDrawable) photoView.setImageDrawable(((TransitionDrawable) d).getDrawable(1));
+    }
+
+    private final Runnable nextSlide = new Runnable() {
+        @Override public void run() { loadNext(); }
+    };
+
+    private void loadNext() {
+        if (!slideshowOn || photos.isEmpty()) return;
+        photoIndex = (photoIndex + 1) % photos.size();
+        final File file = photos.get(photoIndex);
+        final int token = ++loadToken;
+        new Thread(new Runnable() {
+            @Override public void run() {
+                final Bitmap b = decodePhoto(file); // off the main thread: the remote stays responsive
+                ui.post(new Runnable() {
+                    @Override public void run() {
+                        if (token != loadToken || !slideshowOn) return; // left the home screen meanwhile
+                        if (b != null) showPhoto(b);
+                        if (photos.size() > 1) ui.postDelayed(nextSlide, b != null ? SLIDE_MS : 2000);
+                    }
+                });
+            }
+        }).start();
+    }
+
+    // Loads only the screen-shaped middle of the photo, at about screen size, and turns it upright if
+    // the file says it is rotated. A photo never takes more than one screen of memory (about 8 MB at
+    // 1080p), however big it is. To use half of that, at a little less colour smoothness, decode with
+    // inPreferredConfig = Bitmap.Config.RGB_565.
+    private Bitmap decodePhoto(File f) {
+        try {
+            BitmapFactory.Options o = new BitmapFactory.Options();
+            o.inJustDecodeBounds = true;
+            BitmapFactory.decodeFile(f.getPath(), o);
+            int ow = o.outWidth, oh = o.outHeight;
+            if (ow <= 0 || oh <= 0) return null;
+
+            int rot = 0; // phone photos can carry a "rotate me" flag
+            try {
+                int ex = new ExifInterface(f.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
+                if (ex == ExifInterface.ORIENTATION_ROTATE_90) rot = 90;
+                else if (ex == ExifInterface.ORIENTATION_ROTATE_180) rot = 180;
+                else if (ex == ExifInterface.ORIENTATION_ROTATE_270) rot = 270;
+            } catch (Throwable ignored) { /* no rotation info: show it as stored */ }
+            boolean turned = rot == 90 || rot == 270;
+            int pw = turned ? oh : ow, ph = turned ? ow : oh; // the photo's size as it is shown
+
+            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
+            float want = (float) dm.widthPixels / dm.heightPixels;
+            int tw = Math.min(dm.widthPixels, 1920);
+            int th = Math.max(1, Math.round(tw / want));
+
+            int cw = pw, ch = ph; // the middle part of the photo that has the screen's shape
+            if (cw > ch * want) cw = Math.max(1, Math.round(ch * want));
+            else ch = Math.max(1, Math.round(cw / want));
+            int rw = turned ? ch : cw, rh = turned ? cw : ch; // the same part, in the file's own orientation
+            int left = (ow - rw) / 2, top = (oh - rh) / 2;
+            Rect region = new Rect(left, top, left + rw, top + rh);
+
+            int sample = 1;
+            while (cw / (sample * 2) >= tw * 0.7f && ch / (sample * 2) >= th * 0.7f) sample *= 2;
+            BitmapFactory.Options d = new BitmapFactory.Options();
+            d.inSampleSize = sample;
+            Bitmap out;
+            BitmapRegionDecoder dec = BitmapRegionDecoder.newInstance(f.getPath(), false);
+            try { out = dec.decodeRegion(region, d); } finally { dec.recycle(); }
+            if (out == null) return null;
+
+            int sw = turned ? th : tw, sh = turned ? tw : th; // screen size, in the file's own orientation
+            if (out.getWidth() > sw) {
+                Bitmap small = Bitmap.createScaledBitmap(out, sw, sh, true);
+                if (small != out) out.recycle();
+                out = small;
+            }
+            if (rot != 0) {
+                Matrix m = new Matrix();
+                m.postRotate(rot);
+                Bitmap upright = Bitmap.createBitmap(out, 0, 0, out.getWidth(), out.getHeight(), m, true);
+                if (upright != out) out.recycle();
+                out = upright;
+            }
+            return out;
+        } catch (Throwable t) { // a bad file or too little memory: skip this photo
+            return null;
+        }
+    }
+
+    private void showPhoto(Bitmap b) {
+        BitmapDrawable next = new BitmapDrawable(getResources(), b);
+        Drawable from = photoView.getDrawable();
+        if (from instanceof TransitionDrawable) from = ((TransitionDrawable) from).getDrawable(1);
+        if (from == null) from = new ColorDrawable(Color.TRANSPARENT);
+        showPhotoStyle(true);
+        // TransitionDrawable keeps its own clock, so the fade still works with system animations off.
+        final TransitionDrawable t = new TransitionDrawable(new Drawable[] {from, next});
+        t.setCrossFadeEnabled(false); // new photo fades in over the old one: no dark dip in between
+        photoView.setImageDrawable(t);
+        t.startTransition(FADE_MS);
+        ui.postDelayed(new Runnable() { // after the fade, keep only the new photo so the old can be freed
+            @Override public void run() {
+                if (photoView.getDrawable() == t) photoView.setImageDrawable(t.getDrawable(1));
+            }
+        }, FADE_MS + 100);
+    }
+
+    // The dark box behind the date and time, and the dark fade at the bottom, only while a photo is up.
+    private void showPhotoStyle(boolean on) {
+        bottomScrim.setVisibility(on ? View.VISIBLE : View.GONE);
+        if (on) {
+            GradientDrawable box = new GradientDrawable();
+            box.setColor(BOX_COLOR);
+            box.setCornerRadius(px(BOX_RADIUS));
+            clockBox.setBackground(box);
+        } else {
+            clockBox.setBackground(null);
+        }
+    }
+
     private int px(int v) {
         return Math.round(v * scale);
     }
