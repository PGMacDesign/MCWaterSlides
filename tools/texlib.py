#!/usr/bin/env python3
"""Shared pixel-art library for the asset generators.

Palettes are sampled from vanilla 1.21.1 blocks (copper_block, iron_block,
smooth_stone, prismarine_bricks, quartz) so the mod reads as native next to
vanilla builds. Drawing follows the Minecraft style guide: structured shading
(no per-pixel noise — "noise adds no information"), hue-shifted ramps, light
from the top-left, dark seams to define features, dark outlines on item sprites.
"""
from PIL import Image


def _hex(s):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


# ── ramps (dark → light) ────────────────────────────────────────────────────

# vanilla copper_block / cut_copper, all 8 tones
COPPER = [_hex(c) for c in
          ("904931", "9a5038", "a75a40", "b26247", "c26b4c", "c87456", "d67b5b", "e3826c")]
# vanilla iron_block family, extended one step darker for seams
IRON = [_hex(c) for c in
        ("7a7a7a", "929292", "a8a8a8", "b9b9b9", "c9c9c9", "d6d6d6", "e6e6e6", "f2f2f2")]
# dark blued steel (blast-furnace family) — frames, grates, drains
DARK = [_hex(c) for c in ("1f2325", "2e3233", "3a3f40", "464c4d", "555b5c", "6a7172")]
# warm off-whites (quartz family) — the ceramic/fiberglass slide shell
CERAMIC = [_hex(c) for c in ("b5ac9d", "c6bdae", "d5cdbf", "e2dbce", "ece6da", "f6f1e7")]
# energized cyan — jet nozzles, open valves (hue-shifted dark→bright)
GLOW = [_hex(c) for c in ("134a63", "1d6f8f", "2fa3c7", "62c9e8", "a9e8fa", "e3f9ff")]
# fire — lit boiler window, GUI flame
FLAME = [_hex(c) for c in ("7a2f0e", "b4501e", "e07b39", "f4a94b", "ffd97a", "fff3c4")]
# rubber browns — the natural inner tube
RUBBER = [_hex(c) for c in ("3d2812", "5c3a1b", "7a4d24", "94602f", "ad763f", "c68e52")]


def R(ramp, i):
    """Ramp lookup, clamped to the ends."""
    return ramp[max(0, min(len(ramp) - 1, i))]


# ── structured drawing ──────────────────────────────────────────────────────

def plate(img, x0, y0, x1, y1, ramp, base=4, streak=True):
    """One metal plate, vanilla cut_copper style: midtone fill, top/left edge
    highlight, bottom/right edge shadow (which doubles as the seam against the
    next plate), and a diagonal streak highlight across the upper-left half."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), R(ramp, base))
    for x in range(x0, x1 + 1):
        img.putpixel((x, y0), R(ramp, base + 2))
        img.putpixel((x, y1), R(ramp, base - 3))
    for y in range(y0, y1 + 1):
        img.putpixel((x0, y), R(ramp, base + 1))
        img.putpixel((x1, y), R(ramp, base - 2))
    img.putpixel((x1, y0), R(ramp, base))          # soften the corner steps
    img.putpixel((x0, y1), R(ramp, base - 1))
    if streak:
        w, h = x1 - x0, y1 - y0
        for i in range(1, min(w, h) - 1):
            img.putpixel((x0 + i + 1, y0 + i), R(ramp, base + 3))
            if i > 1:
                img.putpixel((x0 + i + 1, y0 + i - 1), R(ramp, base + 2))


def plates2x2(ramp, base=4, streak=True):
    """A 16×16 four-plate face (the machine body workhorse)."""
    img = Image.new("RGBA", (16, 16))
    for oy in (0, 8):
        for ox in (0, 8):
            plate(img, ox, oy, ox + 7, oy + 7, ramp, base, streak=streak)
    return img


def rivet(img, x, y, ramp, base=4):
    """A 2×2 rivet: light catch top-left, shadow bottom-right."""
    img.putpixel((x, y), R(ramp, base + 3))
    img.putpixel((x + 1, y), R(ramp, base))
    img.putpixel((x, y + 1), R(ramp, base))
    img.putpixel((x + 1, y + 1), R(ramp, base - 3))


def frame(img, x0, y0, x1, y1, ramp, base=2):
    """A 1px raised frame: light on top/left, dark on bottom/right."""
    for x in range(x0, x1 + 1):
        img.putpixel((x, y0), R(ramp, base + 2))
        img.putpixel((x, y1), R(ramp, base - 2))
    for y in range(y0, y1 + 1):
        img.putpixel((x0, y), R(ramp, base + 1))
        img.putpixel((x1, y), R(ramp, base - 1))
    img.putpixel((x1, y0), R(ramp, base))
    img.putpixel((x0, y1), R(ramp, base))


def quarter_profile(x0, x1, y_top, y_base, step=0.5, min_rise=0.25):
    """Stepped quarter-ellipse columns for trough/bore rounding: a wall at x0 (column top
    y_top, vertical tangent) falling to a floor at x1 (y_base). Returns (cx0, cx1, top)
    tuples; columns whose rise above the floor is sub-texel (< min_rise) are trimmed.
    Axis-aligned boxes only — rotated geometry z-fights/gaps from non-riding angles."""
    import math
    rx = float(x1 - x0)
    ry = float(y_top - y_base)
    cols = []
    x = float(x0)
    while x < x1 - 1e-9:
        t = (x1 - x) / rx  # 1 at the wall → 0 at the floor
        y = round((y_base + ry * (1 - math.sqrt(max(0.0, 1 - t * t)))) * 100) / 100
        if y - y_base >= min_rise:
            cols.append((x, min(x + step, float(x1)), y))
        x += step
    return cols


def outline_sprite(img, color=(0, 0, 0, 255), threshold=8):
    """Dark outline around every opaque region (the style guide's item rule)."""
    w, h = img.size
    src = img.copy()
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] > threshold:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < w and 0 <= ny < h and src.getpixel((nx, ny))[3] > threshold:
                    img.putpixel((x, y), color)
                    break
