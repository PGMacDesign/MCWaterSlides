#!/usr/bin/env python3
"""Generate Funnel assets: the drain-grate core (one model, three size-variants that only
differ in the bowl they stamp) and the funnel_wall the core auto-builds the bowl from —
textures, models, blockstates, item models, escalating S/M/L recipes, size-keyed loot, tags,
lang. Deterministic."""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"
SIZES = ["small", "medium", "large"]


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def merge_tag(path: Path, values):
    existing = json.loads(path.read_text())["values"] if path.exists() else []
    write_json(path, {"replace": False, "values": sorted(set(existing) | set(values))})


def face(texture, cull=None):
    f = {"texture": texture}
    if cull:
        f["cullface"] = cull
    return f


def gen_textures():
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    # funnel_wall: coppery ceramic with a faint tile grout — the bowl surface.
    rng = random.Random(20260709)
    wall = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = rng.randint(-10, 10)
            grout = (x % 8 == 0 or y % 8 == 0)
            r = (0x9C if grout else 0xC2) + n
            g = (0x60 if grout else 0x7C) + n
            b = (0x42 if grout else 0x54) + n
            wall.putpixel((x, y), (max(0, min(255, r)), max(0, min(255, g)), max(0, min(255, b)), 255))
    wall.save(tex / "funnel_wall.png")

    # funnel_core: a dark drain grate — three slots the water pours through.
    core = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = rng.randint(-6, 6)
            slot = x in (4, 5, 8, 9, 12, 13) and 2 <= y <= 13
            base = 0x14 if slot else 0x50 + n
            core.putpixel((x, y), (max(0, base), max(0, base + 6), max(0, base + 10), 255))
    core.save(tex / "funnel_core.png")


def gen_data():
    gen_textures()
    models = RES / f"assets/{MOD}/models"

    write_json(models / "block/funnel_wall.json",
               {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/funnel_wall"}})

    # The core: a low drain grate (no collision in Java — riders drop through the center).
    write_json(models / "block/funnel_core.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{MOD}:block/funnel_core",
                     "grate": f"{MOD}:block/funnel_core", "side": f"{MOD}:block/funnel_wall"},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 3, 16],
            "faces": {
                "up": face("#grate"), "down": face("#side", cull="down"),
                "north": face("#side"), "south": face("#side"),
                "east": face("#side"), "west": face("#side"),
            },
        }],
    })

    write_json(RES / f"assets/{MOD}/blockstates/funnel_wall.json",
               {"variants": {"": {"model": f"{MOD}:block/funnel_wall"}}})
    write_json(RES / f"assets/{MOD}/blockstates/funnel_core.json",
               {"variants": {f"size={s}": {"model": f"{MOD}:block/funnel_core"} for s in SIZES}})

    write_json(models / "item/funnel_wall.json", {"parent": f"{MOD}:block/funnel_wall"})
    for s in SIZES:
        write_json(models / f"item/funnel_core_{s}.json", {"parent": f"{MOD}:block/funnel_core"})

    # funnel_wall drops itself; the core drops the size-matching item.
    write_json(RES / f"data/{MOD}/loot_table/blocks/funnel_wall.json", {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/funnel_wall",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": f"{MOD}:funnel_wall"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    })
    write_json(RES / f"data/{MOD}/loot_table/blocks/funnel_core.json", {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/funnel_core",
        "pools": [{
            "rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:funnel_core_{s}"}],
            "conditions": [
                {"condition": "minecraft:survives_explosion"},
                {"condition": "minecraft:block_state_property",
                 "block": f"{MOD}:funnel_core", "properties": {"size": s}},
            ],
        } for s in SIZES],
    })

    # Copper + clay tier; cost escalates with size. Never rare.
    write_json(RES / f"data/{MOD}/recipe/funnel_wall.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": [" K ", "KCK", " K "],
        "key": {"K": {"item": "minecraft:clay_ball"}, "C": {"item": "minecraft:copper_ingot"}},
        "result": {"id": f"{MOD}:funnel_wall", "count": 4},
    })
    write_json(RES / f"data/{MOD}/recipe/funnel_core_small.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": [" C ", "CKC", " C "],
        "key": {"C": {"item": "minecraft:copper_ingot"}, "K": {"item": "minecraft:clay_ball"}},
        "result": {"id": f"{MOD}:funnel_core_small", "count": 1},
    })
    write_json(RES / f"data/{MOD}/recipe/funnel_core_medium.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["CCC", "CKC", "CCC"],
        "key": {"C": {"item": "minecraft:copper_ingot"}, "K": {"item": "minecraft:clay_ball"}},
        "result": {"id": f"{MOD}:funnel_core_medium", "count": 1},
    })
    write_json(RES / f"data/{MOD}/recipe/funnel_core_large.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": ["CCC", "CBC", "CCC"],
        "key": {"C": {"item": "minecraft:copper_ingot"}, "B": {"item": "minecraft:copper_block"}},
        "result": {"id": f"{MOD}:funnel_core_large", "count": 1},
    })

    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json",
              [f"{MOD}:funnel_core", f"{MOD}:funnel_wall"])

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.funnel_core"] = "Funnel Core"
    lang[f"block.{MOD}.funnel_wall"] = "Funnel Wall"
    lang[f"item.{MOD}.funnel_core_small"] = "Small Funnel Core"
    lang[f"item.{MOD}.funnel_core_medium"] = "Medium Funnel Core"
    lang[f"item.{MOD}.funnel_core_large"] = "Large Funnel Core"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_data()
    print(f"Generated funnel assets under {RES}")
