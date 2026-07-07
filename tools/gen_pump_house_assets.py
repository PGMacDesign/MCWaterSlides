#!/usr/bin/env python3
"""Generate Pump House assets, reproducibly.

Furnace-style horizontal-facing block: copper body, front face with a porthole that
glows when lit. Emits textures, blockstate (facing x lit), models, loot, recipe
(iron + copper + furnace + bucket -> 1), and merges tags + lang.
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"

COPPER = (184, 115, 81)
IRON = (200, 200, 205)


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
    rng = random.Random(20260709)
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = IRON if y < 4 else COPPER  # iron roofline over copper body
            shade = -18 if x in (0, 15) or y in (0, 15) else 0
            if y >= 4 and x in (5, 10):
                shade -= 22  # panel seams
            side.putpixel((x, y), px(rng, base, shade))
    side.save(tex / "pump_house_side.png")

    top = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            shade = -18 if x in (0, 15) or y in (0, 15) else 0
            if 6 <= x <= 9 and 6 <= y <= 9:
                shade -= 30  # intake hatch
            top.putpixel((x, y), px(rng, IRON, shade))
    top.save(tex / "pump_house_top.png")

    for name, glow in (("pump_house_front", None), ("pump_house_front_lit", (255, 176, 74))):
        img = Image.new("RGBA", (16, 16))
        for y in range(16):
            for x in range(16):
                base = IRON if y < 4 else COPPER
                shade = -18 if x in (0, 15) or y in (0, 15) else 0
                img.putpixel((x, y), px(rng, base, shade))
        # porthole (round-ish window into the boiler)
        for y in range(6, 13):
            for x in range(5, 11):
                d = max(abs(x - 7.5), abs(y - 9))
                if d <= 2.5:
                    if glow:
                        img.putpixel((x, y), px(rng, glow, 0))
                    else:
                        img.putpixel((x, y), px(rng, (40, 48, 60), 0))
                elif d <= 3.4:
                    img.putpixel((x, y), px(rng, COPPER, -46))
        img.save(tex / f"{name}.png")


def gen_data():
    models = RES / f"assets/{MOD}/models"
    for suffix, front in (("", "pump_house_front"), ("_lit", "pump_house_front_lit")):
        write_json(models / f"block/pump_house{suffix}.json", {
            "parent": "minecraft:block/orientable",
            "textures": {
                "particle": f"{MOD}:block/pump_house_side",
                "front": f"{MOD}:block/{front}",
                "side": f"{MOD}:block/pump_house_side",
                "top": f"{MOD}:block/pump_house_top",
            },
        })
    write_json(models / "item/pump_house.json", {"parent": f"{MOD}:block/pump_house"})

    variants = {}
    for facing, rot in (("north", {}), ("south", {"y": 180}), ("east", {"y": 90}), ("west", {"y": 270})):
        for lit, suffix in (("false", ""), ("true", "_lit")):
            variants[f"facing={facing},lit={lit}"] = {"model": f"{MOD}:block/pump_house{suffix}", **rot}
    write_json(RES / f"assets/{MOD}/blockstates/pump_house.json", {"variants": variants})

    write_json(RES / f"data/{MOD}/loot_table/blocks/pump_house.json", {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/pump_house",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:pump_house"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

    write_json(RES / f"data/{MOD}/recipe/pump_house.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "pattern": ["IBI", "CFC", "III"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "C": {"item": "minecraft:copper_ingot"},
            "F": {"item": "minecraft:furnace"},
            "B": {"item": "minecraft:bucket"},
        },
        "result": {"id": f"{MOD}:pump_house", "count": 1},
    })

    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json", [f"{MOD}:pump_house"])

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.pump_house"] = "Pump House"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_textures()
    gen_data()
    print(f"Generated pump house assets under {RES}")
