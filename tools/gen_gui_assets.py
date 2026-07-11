#!/usr/bin/env python3
"""Generate the Pump House GUI texture (vanilla container style, so it reads as native).

Layout on a 256×256 sheet:
  (0,0)-(175,165)   background panel: bevelled frame, slot wells, flame recess, RF gauge frame
  (176,0) 14×14     lit flame sprite      (blitted bottom-up by burn progress)
  (192,0) 14×58     energy fill column    (blitted bottom-up by stored RF)

Slot coordinates mirror PumpHouseMenu: fuel (80,53), player inv (8,84), hotbar (8,142).
"""
from pathlib import Path

from PIL import Image

import texlib as T

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"

# vanilla GUI grays (sampled from the 1.21.1 furnace screen)
PANEL = (198, 198, 198, 255)
LIGHT = (255, 255, 255, 255)
DARK = (85, 85, 85, 255)
SLOT_DARK = (55, 55, 55, 255)
SLOT_FACE = (139, 139, 139, 255)
GAUGE_BG = (24, 26, 30, 255)
TICK = (85, 90, 96, 255)

GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H = 152, 20, 14, 58


def fill(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), c)


def panel(img, w, h):
    """The vanilla panel: light gray fill, white top/left bevel, dark bottom/right,
    stepped 3px rounded corners left transparent."""
    fill(img, 0, 0, w - 1, h - 1, PANEL)
    fill(img, 0, 0, w - 1, 1, LIGHT)          # top bevel
    fill(img, 0, 0, 1, h - 1, LIGHT)          # left bevel
    fill(img, 0, h - 2, w - 1, h - 1, DARK)   # bottom bevel
    fill(img, w - 2, 0, w - 1, h - 1, DARK)   # right bevel
    img.putpixel((w - 2, 1), PANEL)           # bevel corner blends
    img.putpixel((1, h - 2), PANEL)
    for cx, cy in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        sx = 1 if cx == 0 else -1
        sy = 1 if cy == 0 else -1
        for dx, dy in ((0, 0), (1, 0), (0, 1), (2, 0), (0, 2)):
            img.putpixel((cx + sx * dx, cy + sy * dy), (0, 0, 0, 0))
        img.putpixel((cx + sx, cy + sy), PANEL)


def slot(img, x, y):
    """An 18×18 recessed slot well whose item area starts at (x+1, y+1)."""
    fill(img, x, y, x + 17, y, SLOT_DARK)
    fill(img, x, y, x, y + 17, SLOT_DARK)
    fill(img, x, y + 17, x + 17, y + 17, LIGHT)
    fill(img, x + 17, y, x + 17, y + 17, LIGHT)
    img.putpixel((x + 17, y), SLOT_FACE)
    img.putpixel((x, y + 17), SLOT_FACE)
    fill(img, x + 1, y + 1, x + 16, y + 16, SLOT_FACE)


def gen_gui():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    panel(img, 176, 166)

    # player inventory + hotbar wells
    for row in range(3):
        for col in range(9):
            slot(img, 7 + col * 18, 83 + row * 18)
    for col in range(9):
        slot(img, 7 + col * 18, 141)

    # fuel slot + recessed flame window above it
    slot(img, 79, 52)
    fill(img, 80, 35, 95, 50, SLOT_DARK)
    fill(img, 80, 50, 95, 50, LIGHT)
    fill(img, 95, 35, 95, 50, LIGHT)
    img.putpixel((95, 35), SLOT_FACE)
    img.putpixel((80, 50), SLOT_FACE)
    fill(img, 81, 36, 94, 49, GAUGE_BG)

    # RF gauge: recessed frame + dark interior with 25% tick marks
    gx, gy, gw, gh = GAUGE_X, GAUGE_Y, GAUGE_W, GAUGE_H
    fill(img, gx - 1, gy - 1, gx + gw, gy - 1, SLOT_DARK)
    fill(img, gx - 1, gy - 1, gx - 1, gy + gh, SLOT_DARK)
    fill(img, gx - 1, gy + gh, gx + gw, gy + gh, LIGHT)
    fill(img, gx + gw, gy - 1, gx + gw, gy + gh, LIGHT)
    img.putpixel((gx + gw, gy - 1), SLOT_FACE)
    img.putpixel((gx - 1, gy + gh), SLOT_FACE)
    fill(img, gx, gy, gx + gw - 1, gy + gh - 1, GAUGE_BG)
    for frac in (1, 2, 3):
        ty = gy + (gh * frac) // 4
        fill(img, gx, ty, gx + 2, ty, TICK)
        fill(img, gx + gw - 3, ty, gx + gw - 1, ty, TICK)

    # ── dynamic sprites ──
    # flame (14×14 at 176,0): teardrop, wide bright base narrowing to a flickering tip
    for y in range(14):
        h = 13 - y  # height above the flame base
        wobble = ((h * 5) % 3 - 1) * 0.4
        half_w = 6.0 * (1.0 - h / 15.0) ** 1.4
        for x in range(14):
            dx = abs(x - 6.5 - wobble)
            if dx > half_w:
                continue
            d = (dx * dx + (h * 0.75) ** 2) ** 0.5  # heat falls off from the base core
            if d < 2.4:
                i = 5
            elif d < 4.4:
                i = 4
            elif d < 6.6:
                i = 3
            elif d < 8.6:
                i = 2
            else:
                i = 1
            img.putpixel((176 + x, y), T.R(T.FLAME, i))

    # energy column (14×58 at 192,0, flush with the gauge interior): cyan charge with
    # a bright left edge + diagonal wave bands
    for y in range(58):
        for x in range(14):
            i = 4 if x == 0 else (3 if x < 3 else 2)
            if (y + x) % 9 == 0:
                i += 1
            img.putpixel((192 + x, y), T.R(T.GLOW, i))

    out = RES / f"assets/{MOD}/textures/gui"
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / "pump_house.png")


if __name__ == "__main__":
    gen_gui()
    print(f"Generated GUI assets under {RES}")
