#!/usr/bin/env python3
"""Generate Inner Tube (raft) assets: the entity skin (a uniform tube surface all 8 ring
segments share), the item icon (a leather donut), item model, recipe, and lang. The entity
model is Java (TubeRaftModel); this only supplies textures/data. Deterministic."""
import json
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def gen_entity_texture():
    """Grayscale tube surface. Every ring segment shares texOffs(0,0) on a 5x4x4 box, so the
    net lives in the top-left; fill a generous patch with a rounded vertical shade so the tube
    reads as a smooth inflated ring once the renderer tints it (natural leather / a dye)."""
    rng = random.Random(20260709)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    for y in range(0, 12):
        # top of the segment lighter, bottom darker → a rounded highlight.
        shade = 1.0 - abs(y - 3.5) / 9.0
        base = int(150 + 95 * shade)
        for x in range(0, 20):
            n = rng.randint(-8, 8)
            v = max(0, min(255, base + n))
            img.putpixel((x, y), (v, v, v, 255))
    (RES / f"assets/{MOD}/textures/entity").mkdir(parents=True, exist_ok=True)
    img.save(RES / f"assets/{MOD}/textures/entity/inner_tube.png")


def gen_item_texture():
    """A leather-brown donut icon."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    outer = (0x8B, 0x5A, 0x2B, 255)
    lite = (0xB0, 0x78, 0x40, 255)
    draw.ellipse([1, 1, 14, 14], fill=outer)
    draw.ellipse([1, 1, 13, 13], outline=lite)  # top-left highlight ring
    draw.ellipse([5, 5, 10, 10], fill=(0, 0, 0, 0))  # the hole
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
