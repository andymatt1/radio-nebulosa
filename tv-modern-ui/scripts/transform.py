#!/usr/bin/env python3
"""
Restyle 'Xuper' (com.lite.fczx) from an apktool-decoded directory.

Usage:
    python3 transform.py /path/to/decoded-apk

Requires Pillow and numpy. It only touches resources and the manifest:
smali logic is never modified, so app behaviour remains intact.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("folder", nargs="?", default=".", help="decoded apktool folder")
parser.add_argument(
    "--safe",
    action="store_true",
    help="keep the original manifest and AppCompat theme parent (recommended if the app hangs on splash)",
)
parser.add_argument("--no-anim", action="store_true", help="do not touch animation durations")
parser.add_argument("--no-png", action="store_true", help="do not rewrite PNG assets")
args = parser.parse_args()

ROOT = Path(args.folder).resolve()
if not (ROOT / "AndroidManifest.xml").is_file():
    sys.exit(f"No decoded APK found in {ROOT}")

# ---------------------------------------------------------------------------
# Material 3 dark palette
# ---------------------------------------------------------------------------
PALETTE = {
    "color_main": "#ffa78bfa",          # legacy accent used everywhere
    "color_6442EC": "#ff7c5cff",        # legacy secondary accent
    "colorPrimary": "#ffa78bfa",
    "colorPrimaryDark": "#ff6d5fe6",
    "colorAccent": "#ff66e3ff",
    "color_0b0b0b": "#ff0a0c12",
    "color_0a0a0a": "#ff0a0c12",
    "color_1a1a1a": "#ff151927",
    "color_191919": "#ff151927",
    "color_00191919": "#00151927",
    "color_373636": "#ff1d2234",
    "color_font_main": "#fff7f5ff",
    "color_font_second": "#ffc9c7d6",
    "color_font_white": "#fff7f5ff",
    "color_c4c4c4": "#ffc9c7d6",
    "color_b3b3b3": "#ff9b99ad",
    "color_bbc0c4": "#ffc9c7d6",
    "color_c7c5c4": "#ff9b99ad",
    "color_888787": "#ff9b99ad",
    "color_ff3333": "#ff66e3ff",        # notice text: red -> cyan
    "design_default_color_primary": "#ffa78bfa",
    "design_default_color_primary_dark": "#ff6d5fe6",
    "design_dark_default_color_primary": "#ffa78bfa",
}

M3_COLORS = """
    <!-- Added by tv-modern-ui: Material 3 dark palette -->
    <color name="m3_background">#ff0a0c12</color>
    <color name="m3_surface">#ff151927</color>
    <color name="m3_surface_2">#ff1d2234</color>
    <color name="m3_surface_3">#ff262c42</color>
    <color name="m3_primary">#ffa78bfa</color>
    <color name="m3_primary_dark">#ff6d5fe6</color>
    <color name="m3_on_primary">#ff1a1233</color>
    <color name="m3_secondary">#ff66e3ff</color>
    <color name="m3_secondary_dark">#ff2aa9c9</color>
    <color name="m3_on_secondary">#ff05202b</color>
    <color name="m3_tertiary">#ffffb586</color>
    <color name="m3_on_surface">#fff7f5ff</color>
    <color name="m3_on_surface_variant">#ffc9c7d6</color>
    <color name="m3_outline">#ff47465c</color>
    <color name="m3_error">#ffffb4ab</color>
"""

BASE_THEME_PARENT = (
    "@style/Theme.AppCompat.Light.NoActionBar"
    if args.safe
    else "@style/Theme.MaterialComponents.NoActionBar"
)

BASE_THEME = """<style name="BaseAppTheme" parent="@style/Theme.MaterialComponents.NoActionBar">
        <item name="colorPrimary">@color/m3_primary</item>
        <item name="colorPrimaryVariant">@color/m3_primary_dark</item>
        <item name="colorOnPrimary">@color/m3_on_primary</item>
        <item name="colorSecondary">@color/m3_secondary</item>
        <item name="colorSecondaryVariant">@color/m3_secondary_dark</item>
        <item name="colorOnSecondary">@color/m3_on_secondary</item>
        <item name="colorSurface">@color/m3_surface</item>
        <item name="colorOnSurface">@color/m3_on_surface</item>
        <item name="colorError">@color/m3_error</item>
        <item name="android:colorBackground">@color/m3_background</item>
        <item name="android:textColorPrimary">@color/m3_on_surface</item>
        <item name="android:textColorSecondary">@color/m3_on_surface_variant</item>
        <item name="android:windowBackground">@color/m3_background</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:windowFullscreen">true</item>
        <item name="android:windowSoftInputMode">adjustNothing</item>
        <item name="viewInflaterClass">com.interactive.brasiliptv.utils.CustomLayoutInflater</item>
    </style>""".replace("@style/Theme.MaterialComponents.NoActionBar", BASE_THEME_PARENT)

WELCOME_THEME = """<style name="welcome_style" parent="@style/BaseAppTheme">
        <item name="android:windowBackground">@color/m3_background</item>
    </style>"""

# Components that add cold-start work but are not needed for playback.
DISABLE_COMPONENTS = [
    "com.taobao.accs.ChannelService",
    "com.taobao.accs.ChannelService$KernelService",
    "com.taobao.accs.data.MsgDistributeService",
    "com.taobao.accs.EventReceiver",
    "com.taobao.accs.ServiceReceiver",
    "com.taobao.accs.internal.AccsJobService",
    "org.android.agoo.accs.AgooService",
    "com.taobao.agoo.AgooCommondReceiver",
    "com.umeng.message.UmengIntentService",
    "com.umeng.message.XiaomiIntentService",
    "com.umeng.message.UmengMessageIntentReceiverService",
    "com.umeng.message.component.UmengIntentService",
    "com.umeng.message.component.UmengMessageReceiverService",
    "com.umeng.message.component.UmengMessageHandlerService",
    "com.umeng.message.component.UmengNotificationReceiver",
    "com.qiniu.android.dns.NetworkReceiver",
    "com.google.android.gms.measurement.AppMeasurementReceiver",
    "com.google.android.gms.measurement.AppMeasurementService",
    "com.google.android.gms.measurement.AppMeasurementJobService",
    "com.google.android.datatransport.runtime.backends.TransportBackendDiscovery",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.JobInfoSchedulerService",
    "com.google.android.datatransport.runtime.scheduling.jobscheduling.AlarmManagerSchedulerBroadcastReceiver",
]

XML_HEADER = '<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android">\n'
XML_FOOTER = "</shape>\n"


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def sub_once(text: str, pattern: str, repl: str, label: str) -> str:
    new, n = re.subn(pattern, repl, text, count=1, flags=re.S)
    if n != 1:
        print(f"  ! pattern not found: {label}")
    return new


# ---------------------------------------------------------------------------
# 1. Colors and theme
# ---------------------------------------------------------------------------
colors_path = ROOT / "res/values/colors.xml"
colors = read(colors_path)
for name, value in PALETTE.items():
    colors = re.sub(
        rf'(<color name="{re.escape(name)}">)[^<]*(</color>)',
        rf"\g<1>{value}\g<2>",
        colors,
        count=1,
    )
if "m3_background" not in colors:
    colors = colors.replace("</resources>", M3_COLORS + "</resources>")
write(colors_path, colors)
print("[1/7] palette Material 3 applied")

styles_path = ROOT / "res/values/styles.xml"
styles = read(styles_path)
styles = sub_once(styles, r'<style name="BaseAppTheme".*?</style>', BASE_THEME, "BaseAppTheme")
styles = sub_once(styles, r'<style name="welcome_style".*?</style>', WELCOME_THEME, "welcome_style")
write(styles_path, styles)
print("[2/7] theme switched to MaterialComponents dark")

# ---------------------------------------------------------------------------
# 2. Manifest: stop unused push/analytics components at startup
# ---------------------------------------------------------------------------
manifest_path = ROOT / "AndroidManifest.xml"


def disable_component(match: re.Match[str]) -> str:
    tag, attrs = match.group(1), match.group(2)
    if 'android:enabled="true"' in attrs:
        attrs = attrs.replace('android:enabled="true"', 'android:enabled="false"', 1)
    elif 'android:enabled="false"' not in attrs:
        attrs = ' android:enabled="false"' + attrs
    return tag + attrs


if args.safe:
    print("[3/7] manifest left untouched (safe mode)")
else:
    manifest = read(manifest_path)
    for component in DISABLE_COMPONENTS:
        pattern = rf'(<(?:service|receiver|activity-alias)\b)([^>]*android:name="{re.escape(component)}"[^>]*>)'
        manifest, n = re.subn(pattern, disable_component, manifest, count=1)
        if n != 1:
            print(f"  ! manifest component not found: {component}")
    write(manifest_path, manifest)
    print("[3/7] background push/analytics components disabled")

# ---------------------------------------------------------------------------
# 3. Drawables: rounded cards, focus rings, scrims
# ---------------------------------------------------------------------------
d = ROOT / "res/drawable"

def shape(body: str) -> str:
    return XML_HEADER + body + XML_FOOTER

new_drawables = {
    "m3_bottom_scrim.xml": shape(
        '    <gradient android:startColor="@color/m3_surface" android:endColor="@android:color/transparent" android:angle="90" />\n'
    ),
    "m3_top_scrim.xml": shape(
        '    <gradient android:startColor="@android:color/transparent" android:endColor="@color/m3_surface" android:angle="90" />\n'
    ),
    "m3_card.xml": shape(
        '    <gradient android:startColor="@color/m3_surface_2" android:endColor="@color/m3_surface" android:angle="270" />\n'
        '    <corners android:radius="14dp" />\n'
        '    <stroke android:width="1dp" android:color="@color/m3_outline" />\n'
    ),
    "m3_focus_card.xml": shape(
        '    <gradient android:startColor="@color/m3_primary" android:endColor="@color/m3_secondary" android:angle="0" />\n'
        '    <corners android:radius="14dp" />\n'
        '    <stroke android:width="2dp" android:color="@color/m3_on_surface" />\n'
    ),
    "shape_home_tab_bg.xml": shape(
        '    <gradient android:startColor="@color/m3_background" android:endColor="@android:color/transparent" android:angle="0" />\n'
    ),
    "bg_home_navigation_focus.xml": shape(
        '    <gradient android:startColor="@color/m3_primary" android:endColor="@color/m3_secondary" android:angle="0" />\n'
        '    <corners android:radius="14dp" />\n'
        '    <stroke android:width="1dp" android:color="@color/m3_on_surface" />\n'
    ),
    "bg_home_live_channel_item.xml": shape(
        '    <gradient android:startColor="@color/m3_surface_2" android:endColor="@color/m3_surface" android:angle="270" />\n'
        '    <corners android:radius="12dp" />\n'
        '    <stroke android:width="1dp" android:color="@color/m3_outline" />\n'
    ),
    "shape_home_live_channel_focus.xml": shape(
        '    <gradient android:startColor="@color/m3_primary" android:endColor="@color/m3_secondary" android:angle="45" />\n'
        '    <corners android:radius="12dp" />\n'
        '    <stroke android:width="2dp" android:color="@color/m3_on_surface" />\n'
    ),
    "shape_home_live_bottom_bg.xml": shape(
        '    <gradient android:startColor="@android:color/transparent" android:endColor="@color/m3_background" android:angle="90" />\n'
    ),
    "bg_match_card.xml": shape(
        '    <solid android:color="@color/m3_surface_2" />\n'
        '    <corners android:radius="14dp" />\n'
        '    <stroke android:width="1dp" android:color="@color/m3_outline" />\n'
    ),
    "bg_common_toast.xml": shape(
        '    <solid android:color="@color/m3_surface_3" />\n'
        '    <corners android:radius="14dp" />\n'
        '    <stroke android:width="1dp" android:color="@color/m3_primary" />\n'
    ),
    "bg_corners_dialog.xml": shape(
        '    <solid android:color="@color/m3_surface" />\n'
        '    <corners android:radius="18dp" />\n'
        '    <stroke android:width="1dp" android:color="@color/m3_outline" />\n'
    ),
    "shape_bg_new_message.xml": shape(
        '    <solid android:color="@color/m3_surface_2" />\n'
        '    <corners android:radius="16dp" />\n'
        '    <stroke android:width="1dp" android:color="@color/m3_primary" />\n'
    ),
    "sel_function_bg.xml": """<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true" android:drawable="@drawable/m3_focus_card" />
    <item android:state_hovered="true" android:drawable="@drawable/m3_card" />
    <item android:drawable="@drawable/m3_card" />
</selector>
""",
    "shape_personal_btn.xml": """<?xml version="1.0" encoding="utf-8"?>
<selector xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:state_focused="true">
        <shape android:shape="rectangle">
            <solid android:color="@color/m3_primary" />
            <corners android:radius="12dp" />
        </shape>
    </item>
    <item>
        <shape android:shape="rectangle">
            <solid android:color="@color/m3_surface_2" />
            <corners android:radius="12dp" />
            <stroke android:width="1dp" android:color="@color/m3_outline" />
        </shape>
    </item>
</selector>
""",
}
for name, content in new_drawables.items():
    write(d / name, content)
print("[4/7] Material 3 cards/focus/scrims applied")

# ---------------------------------------------------------------------------
# 4. Color state lists: keep text readable on dark surfaces
# ---------------------------------------------------------------------------
c = ROOT / "res/color"
for name in [
    "selector_tab_text_color.xml",
    "color_item_text.xml",
    "color_person_tab.xml",
    "color_text_tab_title.xml",
    "color_vod_search_nav.xml",
    "selector_color_coupons_des.xml",
]:
    p = c / name
    if not p.is_file():
        continue
    text = read(p)
    text = text.replace("@color/color_1a1a1a", "@color/m3_on_surface_variant")
    text = text.replace("@color/color_c4c4c4", "@color/m3_on_surface_variant")
    text = text.replace("@color/color_b3b3b3", "@color/m3_on_surface_variant")
    text = text.replace("@color/color_c7c5c4", "@color/m3_on_surface_variant")
    text = text.replace("@color/color_888787", "@color/m3_on_surface_variant")
    text = text.replace('@color/color_ffffff" />', '@color/m3_primary" />', 1)
    write(p, text)

# ---------------------------------------------------------------------------
# 5. Layout tweaks (IDs and view hierarchy untouched)
# ---------------------------------------------------------------------------
layout_replacements = {
    "fragment_home_live.xml": [
        ("@drawable/shape_home_live_bottom_bg", "@drawable/m3_bottom_scrim"),
        ('android:divider="@color/transparent"', 'android:divider="@color/m3_outline"'),
    ],
    "activity_home.xml": [
        ('android:background="@color/color_1a1a1a"', 'android:background="@color/m3_surface"'),
        ('android:textColor="@color/color_ff3333"', 'android:textColor="@color/m3_secondary"'),
    ],
    "layout_live_player_controller.xml": [
        ('android:background="@android:color/black"', 'android:background="@color/m3_surface"'),
        ('android:background="@color/color_000000"', 'android:background="@color/m3_surface"'),
    ],
    "activity_vod_home.xml": [
        ('app:defaultBgId="@color/color_0b0b0b"', 'app:defaultBgId="@color/m3_background"'),
    ],
}
for name, pairs in layout_replacements.items():
    p = ROOT / "res/layout" / name
    if not p.is_file():
        continue
    text = read(p)
    for old, new in pairs:
        text = text.replace(old, new)
    write(p, text)
print("[5/7] layout backgrounds and text contrast updated")

# ---------------------------------------------------------------------------
# 6. Snappier animations: cap every duration at 180 ms
# ---------------------------------------------------------------------------
duration_count = 0

def cap_duration(match: re.Match[str]) -> str:
    global duration_count
    value = int(match.group(2))
    if value <= 180:
        return match.group(0)
    duration_count += 1
    return f'{match.group(1)}180{match.group(3)}'

for folder in (() if args.no_anim else ("anim", "animator")):
    for p in (ROOT / "res" / folder).glob("*.xml"):
        text = read(p)
        new = re.sub(r'(android:duration=")(\d+)(")', cap_duration, text)
        if new != text:
            write(p, new)
print(f"[6/7] capped {duration_count} animation durations at 180ms")

# ---------------------------------------------------------------------------
# 7. PNG assets: lighter splash + M3-tinted buttons/dialogs (9-patch guides kept)
# ---------------------------------------------------------------------------
def gradient_png(path: Path, start: tuple[int, int, int], end: tuple[int, int, int], preserve_edges: bool) -> None:
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    orig = np.asarray(im).copy()
    t = np.linspace(0.0, 1.0, w, dtype=np.float32)[None, :, None]
    s = np.array(start, dtype=np.float32)[None, None, :]
    e = np.array(end, dtype=np.float32)[None, None, :]
    rgb = np.repeat(s * (1 - t) + e * t, h, axis=0)
    out = np.empty_like(orig)
    out[..., :3] = np.clip(rgb, 0, 255).astype(np.uint8)
    out[..., 3] = orig[..., 3]
    if preserve_edges:
        out[0] = orig[0]
        out[-1] = orig[-1]
        out[:, 0] = orig[:, 0]
        out[:, -1] = orig[:, -1]
    Image.fromarray(out, "RGBA").save(path, optimize=True, compress_level=9)


def splash_png(path: Path) -> None:
    w, h = 640, 360
    y, x = np.mgrid[0:h, 0:w]
    base = np.array([10.0, 12.0, 18.0])
    purple = np.array([124.0, 92.0, 255.0])
    cyan = np.array([102.0, 227.0, 255.0])
    g1 = np.clip(1 - (((x - w * 0.38) / (w * 0.50)) ** 2 + ((y - h * 0.54) / (h * 0.72)) ** 2), 0, 1) ** 2
    g2 = np.clip(1 - (((x - w * 0.68) / (w * 0.34)) ** 2 + ((y - h * 0.40) / (h * 0.55)) ** 2), 0, 1) ** 2
    rgb = base + g1[..., None] * purple * 0.55 + g2[..., None] * cyan * 0.22
    Image.fromarray(np.clip(rgb, 0, 255).astype(np.uint8), "RGB").save(path, optimize=True, compress_level=9)


def png_paths(pattern: str):
    return [] if args.no_png else ROOT.glob(f"res/drawable*/{pattern}")


for p in png_paths("img_welcome_loading1.png"):
    splash_png(p)

for pattern, start, end, preserve in [
    ("bg_btn_focused.png", (167, 139, 250), (102, 227, 255), False),
    ("bg_btn_unfocused.png", (29, 34, 52), (21, 25, 39), False),
    ("bg_btn_dialog_focus.9.png", (167, 139, 250), (102, 227, 255), True),
    ("bg_btn_dialog_default.9.png", (29, 34, 52), (21, 25, 39), True),
]:
    for p in png_paths(pattern):
        gradient_png(p, start, end, preserve)

for p in png_paths("bg_dialog*.9.png"):
    gradient_png(p, (21, 25, 39), (29, 34, 52), True)

for p in png_paths("dialog_title_bg.png"):
    im = Image.open(p).convert("RGBA")
    w, h = im.size
    orig = np.asarray(im).copy()
    out = orig.copy()
    out[..., :3] = np.array([167, 139, 250], dtype=np.uint8)
    out[..., 3] = (orig[..., 3] * 0.45).astype(np.uint8)
    Image.fromarray(out, "RGBA").save(p, optimize=True, compress_level=9)

print("[7/7] splash/button/dialog assets recolored")
print("Done. Rebuild with: apktool b <folder> -o modern.apk")
