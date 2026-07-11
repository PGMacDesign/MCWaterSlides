#!/usr/bin/env python3
"""Generate the mod's identity art: logo.png (mods-list logo) and pack.png (pack icon).

One 32×32 pixel-art badge — a copper flume carrying white water down through a splash —
upscaled 4× nearest-neighbour to 128×128 so it stays crisp in every launcher/mod list.
"""
from pathlib import Path

from PIL import Image

import texlib as T

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"

SKY = [(0x2f, 0x8c, 0xbd, 255), (0x4a, 0xaa, 0xd4, 255),
       (0x6c, 0xc4, 0xe6, 255), (0x8f, 0xd8, 0xf0, 255)]
WATER_HI = (0xd9, 0xf2, 0xfb, 255)
WATER = (0x57, 0xb8, 0xe8, 255)
FOAM = (0xff, 0xff, 0xff, 255)


def _sky(y):
    return SKY[max(0, min(3, 3 - y // 8))]


def gen_badge():
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    # sky backdrop, light at the top
    for y in range(32):
        for x in range(32):
            img.putpixel((x, y), _sky(y))

    # pool at the bottom, foam line on the surface
    for y in range(25, 32):
        for x in range(32):
            img.putpixel((x, y), WATER_HI if y == 25 and x % 5 == 0 else WATER)

    # the flume: a slide-profile drop (gentle start, steep dive, level run-out into the pool)
    def center(x):
        t = x / 26.0
        return 4 + 20 * (3 * t * t - 2 * t * t * t)

    for x in range(27):
        c = int(round(center(x)))
        for dy, col in ((-3, T.R(T.COPPER, 2)), (-2, T.R(T.COPPER, 6)),
                        (-1, FOAM), (0, WATER_HI), (1, WATER),
                        (2, T.R(T.COPPER, 6)), (3, T.R(T.COPPER, 2))):
            y = c + dy
            if 0 <= y < 32:
                img.putpixel((x, y), col)

    # splash where the run-out hits the pool
    for x, y in ((27, 23), (28, 21), (29, 23), (30, 20), (28, 24), (30, 24), (27, 19)):
        if 0 <= x < 32 and 0 <= y < 32:
            img.putpixel((x, y), FOAM)

    # copper badge frame, rounded corners (transparent outside)
    for i in range(32):
        for x, y in ((i, 0), (i, 31), (0, i), (31, i)):
            img.putpixel((x, y), T.R(T.COPPER, 5 if (x < 16 and y < 16) else 3))
    for cx, cy in ((0, 0), (31, 0), (0, 31), (31, 31)):
        img.putpixel((cx, cy), (0, 0, 0, 0))
        img.putpixel((abs(cx - 1), cy), T.R(T.COPPER, 4))
        img.putpixel((cx, abs(cy - 1)), T.R(T.COPPER, 4))

    return img.resize((128, 128), Image.NEAREST)


def gen_data():
    badge = gen_badge()
    badge.save(RES / "logo.png")
    badge.save(RES / "pack.png")


if __name__ == "__main__":
    gen_data()
    print(f"Generated logo.png + pack.png under {RES}")
