#!/usr/bin/env python3
"""Glass-only invariants; run by the existing Glass emulator CI entry point."""
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent.parent
src = root / "src/com/ansub/tvhome"
activity = (src / "HybridActivity.java").read_text()
wall = (src / "TvWallpaperView.java").read_text()
motion = (src / "GlassFocusMotion.java").read_text()
browse = (src / "GlassBrowseView.java").read_text()

def require(condition, message):
    if not condition:
        raise SystemExit("FAIL: " + message)

manifest = ET.parse(root / "AndroidManifest-hybrid-glass-preview.xml").getroot()
android = "{http://schemas.android.com/apk/res/android}"
require(manifest.attrib["package"] == "com.ansub.tvhome.hybrid.glass.preview", "Glass identity")
require({p.get(android + "name") for p in manifest.findall("uses-permission")} == {
    "android.permission.INTERNET", "android.permission.READ_EXTERNAL_STORAGE"
}, "no new Glass permission")
require(all(c.get(android + "name") != "android.intent.category.HOME"
            for c in manifest.iter("category")), "Glass must not become HOME")
require('"com.ansub.tvhome.hybrid.glass.preview".equals(context.getPackageName())\n'
        '                    ? new GlassPhotoFrame(context) : new FrameLayout(context)' in wall,
        "direct photo compositing must remain Glass-only")
require("getChildCount() != 1" in wall and "getChildAt(0).hasOverlappingRendering()" in wall,
        "overlapping content must fall back to normal group compositing")
require("getBackground() != null || getForeground() != null" in wall,
        "slide decoration must retain normal group compositing")
require("FADE_MS = 1050" in wall and "postOnAnimation(pendingFade)" in wall,
        "manual wallpaper timing")
require("SLIDE_MS = 60000L" in activity and "result.prepareToDraw();" in activity,
        "photo cadence and worker prewarm")
require("ScaleType.CENTER_CROP" in wall and "ScaleType.FIT_CENTER" not in wall,
        "full-bleed wallpaper")
require(".animate(" not in browse and ".animate(" not in motion
        and "ValueAnimator" not in motion, "Glass focus must not depend on animator scale")
require("DURATION_MS = 175L" in motion and "SystemClock.uptimeMillis()" in motion
        and "view.postOnAnimation(this)" in motion and "view.removeCallbacks(this)" in motion,
        "bounded, cancellable self-timed focus")
require("glassPreview ? new GlassFocusMotion(tile) : null" in activity,
        "shared dock motion remains Glass-only")
require("if (glassMotion != null)" in activity and "} else {\n                tile.animate()" in activity,
        "other launcher variants retain their original motion path")
require("getScanCode() == 233" in activity and "buildChannelUriForPassthroughInput" in activity,
        "TCL Settings and exact source IDs")
print("PASS: Glass package/permission isolation, conditional compositing, timing and utility guards")
