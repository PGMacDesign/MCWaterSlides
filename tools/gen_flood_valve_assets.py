#!/usr/bin/env python3
"""Generate Flood Valve assets: iron valve body with a copper wheel face,
directional blockstate (facing x powered), loot, recipe, tags + lang merge."""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"

IRON = (200, 200, 205)
COPPER = (184, 115, 81)


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def merge_tag(path: Path, values):
    existing = json.loads(path.read_text())["values"] if path.exists() else []
    merged = sorted(set(existing) | set(values))
    write_json(path, {"replace": False, "values": merged})


def px(rng, base, shade=0):
    v = rng.randint(-9, 9) + shade
    return (max(0, min(255, base[0] + v)),
            max(0, min(255, base[1] + v)),
            max(0, min(255, base[2] + v)), 255)


def gen_textures():
    rng = random.Random(20260712)
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            shade = -18 if x in (0, 15) or y in (0, 15) else 0
            if y in (4, 11):
                shade -= 26  # pipe banding
            side.putpixel((x, y), px(rng, IRON, shade))
    side.save(tex / "flood_valve_side.png")

    for name, open_valve in (("flood_valve_front", False), ("flood_valve_front_open", True)):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                shade = -18 if x in (0, 15) or y in (0, 15) else 0
                img.putpixel((x, y), px(rng, IRON, shade))
        # copper valve wheel: rim + spokes; center glows cyan when open
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                r = (dx * dx + dy * dy) ** 0.5
                if 4.5 <= r <= 6:
                    img.putpixel((x, y), px(rng, COPPER, -10))
                elif r < 4.5 and (abs(dx) < 1.2 or abs(dy) < 1.2):
                    img.putpixel((x, y), px(rng, COPPER, -30))
        core = (92, 200, 255) if open_valve else (44, 62, 74)
        for y in range(7, 9):
            for x in range(7, 9):
                img.putpixel((x, y), px(rng, core, 0))
        img.save(tex / f"{name}.png")


def gen_data():
    models = RES / f"assets/{MOD}/models"
    for suffix, front in (("", "flood_valve_front"), ("_open", "flood_valve_front_open")):
        write_json(models / f"block/flood_valve{suffix}.json", {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"{MOD}:block/flood_valve_side",
                "north": f"{MOD}:block/{front}",
                "south": f"{MOD}:block/flood_valve_side",
                "east": f"{MOD}:block/flood_valve_side",
                "west": f"{MOD}:block/flood_valve_side",
                "up": f"{MOD}:block/flood_valve_side",
                "down": f"{MOD}:block/flood_valve_side",
            },
        })
    write_json(models / "item/flood_valve.json", {"parent": f"{MOD}:block/flood_valve"})

    rot = {
        "north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270},
        "up": {"x": 270}, "down": {"x": 90},
    }
    variants = {}
    for facing, r in rot.items():
        for powered, suffix in (("false", ""), ("true", "_open")):
            variants[f"facing={facing},powered={powered}"] = \
                {"model": f"{MOD}:block/flood_valve{suffix}", **r}
    write_json(RES / f"assets/{MOD}/blockstates/flood_valve.json", {"variants": variants})

    write_json(RES / f"data/{MOD}/loot_table/blocks/flood_valve.json", {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/flood_valve",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:flood_valve"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

    write_json(RES / f"data/{MOD}/recipe/flood_valve.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "pattern": ["ICI", "CBC", "IRI"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "C": {"item": "minecraft:copper_ingot"},
            "B": {"item": "minecraft:bucket"},
            "R": {"item": "minecraft:redstone"},
        },
        "result": {"id": f"{MOD}:flood_valve", "count": 1},
    })

    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json", [f"{MOD}:flood_valve"])


if __name__ == "__main__":
    gen_textures()
    gen_data()
    print(f"Generated flood valve assets under {RES}")
