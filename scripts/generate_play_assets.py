#!/usr/bin/env python3
"""Generate Google Play graphic assets for Kuckmal from the existing app icon."""
from PIL import Image, ImageDraw, ImageFont
import os, sys

SRC = "script/AppIcon_1024x1024.png"
OUT = "appstore/android/graphics"
os.makedirs(OUT, exist_ok=True)

TOP = (0, 99, 129)     # teal top of icon gradient
BOT = (0, 74, 97)      # teal bottom
FONT_B = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FONT_R = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def vgradient(size, top, bot):
    w, h = size
    img = Image.new("RGB", (1, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(h - 1, 1)
        d.point((0, y), fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
    return img.resize((w, h))


# Measured bounds of the rounded plate inside the 1024px source, and its corner radius.
PLATE_BOX = (102, 102, 922, 922)
PLATE_RADIUS = 180


def icon_glyph(px):
    """The icon artwork with its rounded-square plate, on a transparent canvas.

    The source PNG has an opaque white margin, so the plate's corners have to be
    masked out explicitly instead of relying on the source alpha.
    """
    im = Image.open(SRC).convert("RGBA")
    plate = im.crop(PLATE_BOX).resize((px, px), Image.LANCZOS)
    mask = Image.new("L", (px, px), 0)
    r = round(PLATE_RADIUS * px / (PLATE_BOX[2] - PLATE_BOX[0]))
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, px - 1, px - 1), radius=r, fill=255)
    plate.putalpha(mask)
    return plate


def play_icon():
    """512x512 store icon, full bleed, no transparency."""
    bg = vgradient((512, 512), TOP, BOT)
    # Full bleed: the plate fills the canvas and its rounded corners fall back to the
    # same gradient, so Play's own icon mask has clean pixels to work with.
    art = icon_glyph(512)
    bg.paste(art, (0, 0), art)
    bg.save(f"{OUT}/play_icon_512.png")
    return f"{OUT}/play_icon_512.png"


def feature_graphic():
    """1024x500 feature graphic. Safe zone: keep content inside the central 924x400."""
    W, H = 1024, 500
    bg = vgradient((W, H), (0, 112, 146), (0, 58, 78))
    d = ImageDraw.Draw(bg)

    art = icon_glyph(230)
    bg.paste(art, (90, (H - 230) // 2), art)

    f_title = ImageFont.truetype(FONT_B, 92)
    f_sub = ImageFont.truetype(FONT_R, 34)
    f_small = ImageFont.truetype(FONT_R, 27)

    x = 370
    d.text((x, 150), "Kuckmal", font=f_title, fill=(255, 255, 255))
    d.text((x + 5, 262), "Alle Mediatheken in einer App", font=f_sub, fill=(168, 216, 234))
    d.text((x + 5, 315), "ARD · ZDF · Arte · 3sat · BR · NDR · WDR · …",
           font=f_small, fill=(126, 178, 198))
    d.text((x + 5, 355), "werbefrei · kein Konto · kein Tracking",
           font=f_small, fill=(126, 178, 198))

    bg.save(f"{OUT}/feature_graphic_1024x500.png")
    return f"{OUT}/feature_graphic_1024x500.png"


def tv_banner():
    """1280x720 TV banner, only needed if the Android TV form factor is opted into."""
    W, H = 1280, 720
    bg = vgradient((W, H), (0, 112, 146), (0, 52, 70))
    d = ImageDraw.Draw(bg)
    art = icon_glyph(300)
    bg.paste(art, (170, (H - 300) // 2), art)
    d.text((530, 285), "Kuckmal", font=ImageFont.truetype(FONT_B, 110), fill=(255, 255, 255))
    d.text((536, 415), "Alle Mediatheken in einer App",
           font=ImageFont.truetype(FONT_R, 40), fill=(168, 216, 234))
    bg.save(f"{OUT}/tv_banner_1280x720.png")
    return f"{OUT}/tv_banner_1280x720.png"


if __name__ == "__main__":
    for p in (play_icon(), feature_graphic(), tv_banner()):
        im = Image.open(p)
        print(f"{p}  {im.size}  {im.mode}  {os.path.getsize(p) // 1024} KB")
