#!/bin/sh
# Execute the real focus scheduler against a deterministic clock/view queue.
# This checks timing/cancellation, not Android rendering or TCL performance.
set -eu
cd "$(dirname "$0")/.."
GLASS_FOCUS_TEST_DIR=$(mktemp -d "${TMPDIR:-/tmp}/glass-focus-XXXXXX")
mkdir -p "$GLASS_FOCUS_TEST_DIR/android/os" "$GLASS_FOCUS_TEST_DIR/android/view" \
    "$GLASS_FOCUS_TEST_DIR/com/ansub/tvhome" "$GLASS_FOCUS_TEST_DIR/classes"
cat > "$GLASS_FOCUS_TEST_DIR/android/os/SystemClock.java" <<'JAVA'
package android.os;
public final class SystemClock {
    public static long now;
    public static long uptimeMillis() { return now; }
}
JAVA
cat > "$GLASS_FOCUS_TEST_DIR/android/view/View.java" <<'JAVA'
package android.view;
import java.util.ArrayList;
import android.os.SystemClock;
public class View {
    public interface OnAttachStateChangeListener {
        void onViewAttachedToWindow(View v);
        void onViewDetachedFromWindow(View v);
    }
    public float x = 1f, y = 1f;
    public boolean attached = true, windowFocus = true;
    public final ArrayList<Runnable> queued = new ArrayList<>();
    public OnAttachStateChangeListener listener;
    public void addOnAttachStateChangeListener(OnAttachStateChangeListener l) { listener = l; }
    public void removeCallbacks(Runnable r) { queued.removeIf(item -> item == r); }
    public void postOnAnimation(Runnable r) { queued.add(r); }
    public boolean isAttachedToWindow() { return attached; }
    public boolean hasWindowFocus() { return windowFocus; }
    public float getScaleX() { return x; }
    public float getScaleY() { return y; }
    public void setScaleX(float n) { x = n; }
    public void setScaleY(float n) { y = n; }
    public void frame(long time) {
        SystemClock.now = time;
        ArrayList<Runnable> due = new ArrayList<>(queued);
        queued.clear();
        for (Runnable r : due) r.run();
    }
}
JAVA
cat > "$GLASS_FOCUS_TEST_DIR/com/ansub/tvhome/GlassFocusMotionTest.java" <<'JAVA'
package com.ansub.tvhome;
import android.os.SystemClock;
import android.view.View;
public final class GlassFocusMotionTest {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
    private static void near(float actual, float wanted, String why) {
        check(Math.abs(actual - wanted) < 0.00001f, why + ": " + actual);
    }
    public static void main(String[] args) {
        View v = new View();
        GlassFocusMotion motion = new GlassFocusMotion(v);
        motion.moveTo(1.045f);
        near(v.x, 1f, "focus cannot jump to its end");
        v.frame(87);
        check(v.x > 1f && v.x < 1.045f, "focus must advance at intermediate wall time");
        check(v.queued.size() == 1, "only one frame callback may be pending");
        near(v.x, v.y, "uniform focus scale");
        v.frame(175);
        near(v.x, 1.045f, "finish at 175ms");
        check(v.queued.isEmpty(), "settled focus must stop scheduling");
        motion.moveTo(1f);
        v.frame(220);
        float partial = v.x;
        motion.moveTo(1.045f);
        near(v.x, partial, "rapid reversal must retain its current value");
        check(v.queued.size() == 1, "reversal must cancel the prior callback");
        v.frame(395);
        near(v.x, 1.045f, "reversed animation completes");
        motion.moveTo(1f);
        v.windowFocus = false;
        v.frame(410);
        near(v.x, 1f, "lost window settles to its requested resting scale");
        check(v.queued.isEmpty(), "lost window stops scheduling");
        v.windowFocus = true;
        motion.moveTo(1.045f);
        v.attached = false;
        v.listener.onViewDetachedFromWindow(v);
        check(v.queued.isEmpty(), "detachment cancels pending work");
        v.attached = true;
        SystemClock.now = 500;
        motion.moveTo(1.045f);
        v.frame(675);
        near(v.x, 1.045f, "reattached tile can animate again");
        motion.moveTo(1.045f);
        check(v.queued.isEmpty(), "unchanged scale must schedule nothing");
        System.out.println("PASS: Glass focus timing, bounded scheduling, reversal, window loss and detach/reattach");
    }
}
JAVA
javac -d "$GLASS_FOCUS_TEST_DIR/classes" \
    "$GLASS_FOCUS_TEST_DIR/android/os/SystemClock.java" \
    "$GLASS_FOCUS_TEST_DIR/android/view/View.java" \
    src/com/ansub/tvhome/GlassFocusMotion.java \
    "$GLASS_FOCUS_TEST_DIR/com/ansub/tvhome/GlassFocusMotionTest.java"
java -cp "$GLASS_FOCUS_TEST_DIR/classes" com.ansub.tvhome.GlassFocusMotionTest
