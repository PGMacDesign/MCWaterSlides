#!/usr/bin/env python3
"""Generate Pump House assets, reproducibly.

Furnace-style horizontal-facing block: copper body, front face with a porthole that
glows when lit. Emits textures, blockstate (facing x lit), models, loot, recipe
(iron + copper + furnace + bucket -> 1), and merges tags + lang.
"""
import json
from pathlib import Path

from PIL import Image

import texlib as T

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


def _body(with_streak=True):
    """Iron roofline plate over two tall copper body plates."""
    img = Image.new("RGBA", (16, 16))
    T.plate(img, 0, 0, 15, 3, T.IRON, streak=False)
    T.plate(img, 0, 4, 7, 15, T.COPPER, streak=with_streak)
    T.plate(img, 8, 4, 15, 15, T.COPPER, streak=with_streak)
    T.rivet(img, 2, 1, T.IRON)
    T.rivet(img, 12, 1, T.IRON)
    return img


def gen_textures():
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    _body().save(tex / "pump_house_side.png")

    # top: iron deck plate with a recessed intake hatch
    top = Image.new("RGBA", (16, 16))
    T.plate(top, 0, 0, 15, 15, T.IRON, streak=False)
    T.frame(top, 5, 5, 10, 10, T.IRON, base=3)
    for y in range(6, 10):
        for x in range(6, 10):
            top.putpixel((x, y), T.R(T.DARK, 2 if (x + y) % 2 else 1))
    T.rivet(top, 1, 1, T.IRON)
    T.rivet(top, 13, 1, T.IRON)
    T.rivet(top, 1, 13, T.IRON)
    T.rivet(top, 13, 13, T.IRON)
    top.save(tex / "pump_house_top.png")

    # front: the body with a round boiler porthole — dark water inside, fire when lit
    for name, lit in (("pump_house_front", False), ("pump_house_front_lit", True)):
        img = _body(with_streak=False)
        cx, cy = 7.5, 9.5
        for y in range(5, 15):
            for x in range(3, 13):
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d >= 4.4:
                    continue
                if d >= 3.2:
                    upper = (x - cx) + (y - cy) < 0
                    img.putpixel((x, y), T.R(T.DARK, 1 if upper else 3))
                elif lit:
                    img.putpixel((x, y), T.R(T.FLAME, 4 if d < 1.4 else (3 if d < 2.3 else 1)))
                else:
                    img.putpixel((x, y), T.R(T.DARK, 2 if d < 2.3 else 1))
        if not lit:  # glass glint on the idle porthole
            img.putpixel((6, 8), T.R(T.DARK, 4))
            img.putpixel((7, 9), T.R(T.DARK, 3))
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
