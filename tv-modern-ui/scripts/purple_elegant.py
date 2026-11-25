#!/usr/bin/env python3
"""Capa 'morado elegante' sobre una carpeta ya decodificada y saneada.

Uso recomendado (manifiesto intacto):
    python3 transform.py decoded --safe --no-anim --no-png
    python3 purple_elegant.py decoded
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(sys.argv[1]).resolve() if len(sys.argv) > 1 else Path(".")
if not (ROOT / "AndroidManifest.xml").is_file():
    sys.exit(f"No decoded APK found in {ROOT}")


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


def replace_attr(text: str, tag_pattern: str, attr: str, value: str) -> str:
    """Replace an attribute only inside the first tag matching tag_pattern."""
    m = re.search(tag_pattern, text, flags=re.S)
    if not m:
        print(f"  ! tag not found: {tag_pattern[:60]}")
        return text
    tag = m.group(0)
    if re.search(rf'{re.escape(attr)}="[^"]*"', tag):
        new_tag = re.sub(rf'{re.escape(attr)}="[^"]*"', f'{attr}="{value}"', tag, count=1)
    elif tag.endswith("/>"):
        new_tag = tag[:-2] + f' {attr}="{value}" />'
    else:
        new_tag = tag.replace(">", f' {attr}="{value}">', 1)
    return text[: m.start()] + new_tag + text[m.end() :]


# ---------------------------------------------------------------------------
# 1. Purple palette (replaces the lavender/cyan values from transform.py)
# ---------------------------------------------------------------------------
PURPLE = {
    "color_main": "#ffb388ff",
    "color_6442EC": "#ff9b6bff",
    "colorPrimary": "#ffb388ff",
    "colorPrimaryDark": "#ff6a3fd1",
    "colorAccent": "#ffd1a2ff",
    "color_font_main": "#fff7f1ff",
    "color_font_second": "#ffd9cdea",
    "color_font_white": "#fff7f1ff",
    "color_c4c4c4": "#ffd9cdea",
    "color_b3b3b3": "#ffb9a7d1",
    "color_bbc0c4": "#ffd9cdea",
    "color_888787": "#ffb9a7d1",
    "color_c7c5c4": "#ffb9a7d1",
    "m3_background": "#ff0d0817",
    "m3_surface": "#ff171026",
    "m3_surface_2": "#ff211833",
    "m3_surface_3": "#ff2c2140",
    "m3_primary": "#ffb388ff",
    "m3_primary_dark": "#ff6a3fd1",
    "m3_secondary": "#ffd1a2ff",
    "m3_secondary_dark": "#ff7e4fd6",
    "m3_on_surface": "#fff7f1ff",
    "m3_on_surface_variant": "#ffd9cdea",
    "m3_outline": "#ff4a3a63",
    "design_default_color_primary": "#ffb388ff",
    "design_dark_default_color_primary": "#ffb388ff",
}
colors_path = ROOT / "res/values/colors.xml"
colors = read(colors_path)
for name, value in PURPLE.items():
    colors = re.sub(
        rf'(<color name="{re.escape(name)}">)[^<]*(</color>)',
        rf"\g<1>{value}\g<2>",
        colors,
        count=1,
    )
write(colors_path, colors)

# Keep the safe AppCompat parent (startup works) but force dark background.
styles_path = ROOT / "res/values/styles.xml"
styles = read(styles_path)
styles = re.sub(
    r'(<style name="BaseAppTheme".*?</style>)',
    lambda m: m.group(1).replace("@color/m3_background", "@color/m3_background"),
    styles,
    count=1,
    flags=re.S,
)
write(styles_path, styles)
print("[1/4] paleta morada aplicada")

# ---------------------------------------------------------------------------
# 2. Rounded, purple UI shapes
# ---------------------------------------------------------------------------
d = ROOT / "res/drawable"
HEAD = '<?xml version="1.0" encoding="utf-8"?>\n<shape xmlns:android="http://schemas.android.com/apk/res/android">\n'
SEL = '<?xml version="1.0" encoding="utf-8"?>\n<selector xmlns:android="http://schemas.android.com/apk/res/android">\n'

shapes = {
    "m3_category_bar.xml": HEAD
    + '    <gradient android:startColor="@color/m3_surface_2" android:endColor="@color/m3_surface" android:angle="270" />\n'
    + '    <corners android:radius="18dp" />\n'
    + '    <stroke android:width="1dp" android:color="@color/m3_outline" />\n</shape>\n',
    "m3_icon_circle.xml": HEAD
    + '    <solid android:color="@color/m3_surface_3" />\n'
    + '    <corners android:radius="999dp" />\n'
    + '    <stroke android:width="2dp" android:color="@color/m3_primary" />\n</shape>\n',
    "m3_icon_circle_focus.xml": HEAD
    + '    <gradient android:startColor="@color/m3_primary" android:endColor="@color/m3_secondary_dark" android:angle="45" />\n'
    + '    <corners android:radius="999dp" />\n'
    + '    <stroke android:width="2dp" android:color="@color/m3_on_surface" />\n</shape>\n',
    "shape_home_live_channel_icon_bg.xml": HEAD
    + '    <solid android:color="@color/m3_surface_3" />\n'
    + '    <corners android:radius="999dp" />\n'
    + '    <stroke android:width="1dp" android:color="@color/m3_outline" />\n</shape>\n',
    "bg_home_navigation_focus.xml": HEAD
    + '    <gradient android:startColor="@color/m3_primary" android:endColor="@color/m3_secondary_dark" android:angle="0" />\n'
    + '    <corners android:radius="18dp" />\n'
    + '    <stroke android:width="1dp" android:color="@color/m3_on_surface" />\n</shape>\n',
    "shape_home_tab_bg.xml": HEAD
    + '    <gradient android:startColor="@color/m3_surface" android:endColor="@android:color/transparent" android:angle="0" />\n'
    + '    <corners android:radius="18dp" />\n</shape>\n',
    "selector_icon_circle.xml": SEL
    + '    <item android:state_focused="true" android:drawable="@drawable/m3_icon_circle_focus" />\n'
    + '    <item android:state_selected="true" android:drawable="@drawable/m3_icon_circle_focus" />\n'
    + '    <item android:drawable="@drawable/m3_icon_circle" />\n</selector>\n',
    "selector_home_live_channel_icon_bg.xml": SEL
    + '    <item android:state_focused="true" android:drawable="@drawable/m3_icon_circle_focus" />\n'
    + '    <item android:state_selected="true" android:drawable="@drawable/m3_icon_circle_focus" />\n'
    + '    <item android:drawable="@drawable/shape_home_live_channel_icon_bg" />\n</selector>\n',
    "selector_live_select.xml": SEL
    + '    <item android:state_focused="true" android:drawable="@color/m3_primary" />\n'
    + '    <item android:drawable="@android:color/transparent" />\n</selector>\n',
    "selector_category_list.xml": SEL
    + '    <item android:state_focused="true" android:drawable="@drawable/m3_category_bar" />\n'
    + '    <item android:drawable="@android:color/transparent" />\n</selector>\n',
    "sel_vod_category.xml": SEL
    + '    <item android:state_focused="true" android:drawable="@color/m3_primary" />\n'
    + '    <item android:state_hovered="true" android:drawable="@color/m3_surface_3" />\n'
    + '    <item android:drawable="@android:color/transparent" />\n</selector>\n',
    "shape_btn_focus.xml": '<?xml version="1.0" encoding="utf-8"?>\n<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n'
    + '    <item><shape android:shape="rectangle">\n'
    + '        <solid android:color="@color/m3_primary" />\n'
    + '        <corners android:radius="22dp" />\n'
    + '    </shape></item>\n</layer-list>\n',
}
for name, content in shapes.items():
    write(d / name, content)
print("[2/4] iconos redondos y tarjetas moradas aplicados")

# ---------------------------------------------------------------------------
# 3. Layouts: categories in a top strip, rounded icons
# ---------------------------------------------------------------------------
home = ROOT / "res/layout/activity_home.xml"
text = read(home)
text = replace_attr(text, r'<View android:id="@id/mGridTabBg"[^>]*>', "android:background", "@drawable/m3_category_bar")
text = replace_attr(text, r'<View android:id="@id/mGridTabBg"[^>]*>', "android:layout_width", "fill_parent")
text = replace_attr(text, r'<View android:id="@id/mGridTabBg"[^>]*>', "android:layout_height", "104.0dip")
text = replace_attr(text, r'<View android:id="@id/mGridTabBg"[^>]*>', "android:layout_marginTop", "92.0dip")

grid_pat = r'<com\.open\.leanback\.widget\.VerticalGridView android:id="@id/mGridTab"[^>]*>'
text = replace_attr(text, grid_pat, "android:paddingTop", "0.0dip")
text = replace_attr(text, grid_pat, "android:paddingLeft", "18.0dip")
text = replace_attr(text, grid_pat, "android:layout_width", "fill_parent")
text = replace_attr(text, grid_pat, "android:layout_height", "104.0dip")
text = replace_attr(text, grid_pat, "android:layout_marginTop", "92.0dip")
text = replace_attr(text, grid_pat, "app:numberOfColumns", "6")

text = replace_attr(
    text, r'<androidx\.viewpager2\.widget\.ViewPager2 android:id="@id/mViewPager"[^>]*>',
    "android:layout_marginTop", "204.0dip",
)
text = replace_attr(text, r'<com\.module\.view\.notice\.NoticeTextView[^>]*>', "android:layout_marginTop", "42.0dip")
write(home, text)

vod = ROOT / "res/layout/activity_vod_home.xml"
text = read(vod)
grid_pat = r'<com\.open\.leanback\.widget\.HorizontalGridView android:id="@id/mGridTab"[^>]*>'
text = replace_attr(text, grid_pat, "android:background", "@drawable/m3_category_bar")
text = replace_attr(text, grid_pat, "android:layout_marginTop", "76.0dip")
text = replace_attr(text, grid_pat, "android:layout_height", "58.0dip")
text = replace_attr(text, grid_pat, "android:paddingLeft", "12.0dip")
text = replace_attr(
    text, r'<com\.widget\.custom\.CustomViewPager android:id="@id/mViewPager"[^>]*>',
    "android:layout_marginTop", "144.0dip",
)
write(vod, text)

tab_item = ROOT / "res/layout/layout_tab_item.xml"
text = read(tab_item)
img_pat = r'<ImageView android:layout_gravity="center_vertical" android:id="@id/mIvTab"[^>]*>'
text = replace_attr(text, img_pat, "android:background", "@drawable/selector_icon_circle")
text = replace_attr(text, img_pat, "android:layout_width", "32.0dip")
text = replace_attr(text, img_pat, "android:layout_height", "32.0dip")
text = replace_attr(text, img_pat, "android:layout_marginLeft", "14.0dip")
text = replace_attr(text, img_pat, "android:scaleType", "fitCenter")
write(tab_item, text)

cat_item = ROOT / "res/layout/layout_tab_category_item.xml"
text = read(cat_item)
text = replace_attr(text, r'<RelativeLayout android:id="@id/mRlCategoryItem"[^>]*>', "android:layout_height", "82.0dip")
for pat in (
    r'<TextView android:textSize="20\.0sp"[^>]*>',
    r'<com\.module\.view\.FixGifImageView[^>]*>',
):
    text = replace_attr(text, pat, "android:layout_marginLeft", "12.0dip")
    text = replace_attr(text, pat, "android:layout_width", "112.0dip")
write(cat_item, text)

# Round the three player buttons and channel icons via their tintable PNGs.
for pattern, tint in [
    ("home_live_btn_all_focus.png", (179, 136, 255)),
    ("home_live_btn_fav_focus.png", (179, 136, 255)),
    ("home_live_btn_search_focus.png", (179, 136, 255)),
    ("home_live_btn_all_normal.png", (44, 32, 66)),
    ("home_live_btn_fav_normal.png", (44, 32, 66)),
    ("home_live_btn_search_normal.png", (44, 32, 66)),
    ("vod_category_tab_bg.png", (124, 79, 214)),
    ("bg_btn_focused.png", (179, 136, 255)),
    ("bg_btn_dialog_focus.9.png", (179, 136, 255)),
]:
    for p in ROOT.glob(f"res/drawable*/{pattern}"):
        im = Image.open(p).convert("RGBA")
        arr = np.asarray(im).copy()
        edge = pattern.endswith(".9.png")
        if edge:
            original = arr.copy()
        arr[..., :3] = np.array(tint, dtype=np.uint8)
        if edge:
            arr[0] = original[0]
            arr[-1] = original[-1]
            arr[:, 0] = original[:, 0]
            arr[:, -1] = original[:, -1]
        Image.fromarray(arr, "RGBA").save(p, optimize=True, compress_level=9)
print("[3/4] categorias arriba e iconos redondeados aplicados")

# ---------------------------------------------------------------------------
# 4. Sanity check: manifest must be byte-for-byte identical
# ---------------------------------------------------------------------------
base = Path("/tmp/opencode/tv-decoded/AndroidManifest.xml")
if base.is_file() and base.read_bytes() != (ROOT / "AndroidManifest.xml").read_bytes():
    print("  ! WARNING: el manifiesto no es idéntico al original")
else:
    print("[4/4] permisos/manifiesto intactos")
