#!/usr/bin/env python3
"""Generate Inner Tube (raft) assets: the entity skin (a uniform tube surface all 8 ring
segments share), the item icon (a leather donut), item model, recipe, and lang. The entity
model is Java (TubeRaftModel); this only supplies textures/data. Deterministic."""
import json
import math
from pathlib import Path

from PIL import Image

import texlib as T

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def gen_entity_texture():
    """Grayscale tube surface. Every ring segment shares texOffs(0,0) on a 5x4x4 box, so the
    net lives in the top-left; fill a generous patch with a rounded vertical shade so the tube
    reads as a smooth inflated ring once the renderer tints it (natural leather / a dye).
    Structured banding (no noise): bright crown near the top, rolling darker below, with a
    faint weld seam every 5 columns to match the model's 5px ring segments."""
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    rows = (196, 232, 250, 252, 242, 226, 208, 188, 168, 150, 136, 126)
    for y in range(12):
        for x in range(20):
            v = rows[y] - (12 if x % 5 == 0 else 0)
            img.putpixel((x, y), (v, v, v, 255))
    (RES / f"assets/{MOD}/textures/entity").mkdir(parents=True, exist_ok=True)
    img.save(RES / f"assets/{MOD}/textures/entity/inner_tube.png")


def gen_item_texture():
    """The rubber ring icon: shaded donut with a top-left highlight arc and the
    style guide's dark item outline."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            r = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if not 2.6 <= r <= 6.4:
                continue
            # light from the top-left: highlight arc there, core shadow bottom-right
            toward_light = (x - cx) + (y - cy)
            if toward_light < -5:
                i = 5
            elif toward_light < -1:
                i = 4
            elif toward_light < 3:
                i = 3
            else:
                i = 2
            if r > 5.6 or r < 3.4:  # rolled edges turn away from the light
                i -= 1
            img.putpixel((x, y), T.R(T.RUBBER, i))
    img.putpixel((11, 12), T.R(T.RUBBER, 0))  # valve stub
    T.outline_sprite(img, T.R(T.RUBBER, 0))
    (RES / f"assets/{MOD}/textures/item").mkdir(parents=True, exist_ok=True)
    img.save(RES / f"assets/{MOD}/textures/item/inner_tube.png")


def gen_data():
    gen_entity_texture()
    gen_item_texture()

    write_json(RES / f"assets/{MOD}/models/item/inner_tube.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{MOD}:item/inner_tube"},
    })

    # Leather-forward + a copper valve: cheap, renewable, reads as an inflatable.
    write_json(RES / f"data/{MOD}/recipe/inner_tube.json", {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": [" L ", "LCL", " L "],
        "key": {"L": {"item": "minecraft:leather"}, "C": {"item": "minecraft:copper_ingot"}},
        "result": {"id": f"{MOD}:inner_tube", "count": 1},
    })

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"item.{MOD}.inner_tube"] = "Inner Tube"
    lang[f"entity.{MOD}.inner_tube"] = "Inner Tube"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_data()
    print(f"Generated inner tube assets under {RES}")
