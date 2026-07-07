#!/usr/bin/env python3
"""Generate Jet block assets, reproducibly.

Emits textures (copper body, nozzle front off/on, intake back), the directional
blockstate (facing x energized variants), block/item models, loot table, recipe
(4 copper + 2 iron + 1 redstone -> 2), and merges tags + lang.
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def merge_tag(path: Path, values):
    existing = json.loads(path.read_text())["values"] if path.exists() else []
    merged = sorted(set(existing) | set(values))
    write_json(path, {"replace": False, "values": merged})


# ── textures ────────────────────────────────────────────────────────────────

COPPER = (184, 115, 81)


def copper_px(rng, shade=0):
    v = rng.randint(-10, 10) + shade
    return (max(0, min(255, COPPER[0] + v)),
            max(0, min(255, COPPER[1] + v)),
            max(0, min(255, COPPER[2] + v)), 255)


def gen_textures():
    rng = random.Random(20260708)
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            shade = -18 if x in (0, 15) or y in (0, 15) else 0
            # flow chevrons pointing "up" the texture (toward the nozzle)
            if (x + y) % 8 in (0, 1) and 2 < x < 13 and 2 < y < 13:
                shade -= 26
            side.putpixel((x, y), copper_px(rng, shade))
    side.save(tex / "jet_side.png")

    back = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            shade = -18 if x in (0, 15) or y in (0, 15) else 0
            if x % 3 == 0 or y % 3 == 0:
                shade -= 34  # intake grate
            back.putpixel((x, y), copper_px(rng, shade))
    back.save(tex / "jet_back.png")

    for name, core in (("jet_front", (44, 62, 74)), ("jet_front_on", (92, 200, 255))):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                d = max(abs(x - 7.5), abs(y - 7.5))
                if d >= 6:
                    img.putpixel((x, y), copper_px(rng, -8))
                elif d >= 4.4:
                    img.putpixel((x, y), copper_px(rng, -40))  # nozzle ring
                else:
                    j = rng.randint(-14, 14)
                    img.putpixel((x, y), (max(0, min(255, core[0] + j)),
                                          max(0, min(255, core[1] + j)),
                                          max(0, min(255, core[2] + j)), 255))
        img.save(tex / f"{name}.png")


# ── models / blockstate / data ──────────────────────────────────────────────

def gen_data():
    models = RES / f"assets/{MOD}/models"
    for suffix, front in (("", "jet_front"), ("_on", "jet_front_on")):
        write_json(models / f"block/jet{suffix}.json", {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"{MOD}:block/jet_side",
                "north": f"{MOD}:block/{front}",
                "south": f"{MOD}:block/jet_back",
                "east": f"{MOD}:block/jet_side",
                "west": f"{MOD}:block/jet_side",
                "up": f"{MOD}:block/jet_side",
                "down": f"{MOD}:block/jet_side",
            },
        })
    write_json(models / "item/jet.json", {"parent": f"{MOD}:block/jet"})

    # facing rotations (model front faces north) x energized model swap
    rot = {
        "north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270},
        "up": {"x": 270}, "down": {"x": 90},
    }
    variants = {}
    for facing, r in rot.items():
        for energized, suffix in (("false", ""), ("true", "_on")):
            variants[f"facing={facing},energized={energized}"] = \
                {"model": f"{MOD}:block/jet{suffix}", **r}
    write_json(RES / f"assets/{MOD}/blockstates/jet.json", {"variants": variants})

    write_json(RES / f"data/{MOD}/loot_table/blocks/jet.json", {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/jet",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:jet"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

    write_json(RES / f"data/{MOD}/recipe/jet.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "pattern": ["CIC", " R ", "CIC"],
        "key": {
            "C": {"item": "minecraft:copper_ingot"},
            "I": {"item": "minecraft:iron_ingot"},
            "R": {"item": "minecraft:redstone"},
        },
        "result": {"id": f"{MOD}:jet", "count": 2},
    })

    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json", [f"{MOD}:jet"])

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.jet"] = "Jet"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_textures()
    gen_data()
    print(f"Generated jet assets under {RES}")
