#!/usr/bin/env python3
"""Generate Funnel assets: the drain-grate core (one model, three size-variants that only
differ in the bowl they stamp) and the funnel_wall the core auto-builds the bowl from —
textures, models, blockstates, item models, escalating S/M/L recipes, size-keyed loot, tags,
lang. Deterministic."""
import json
from pathlib import Path

from PIL import Image

import texlib as T

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

    # funnel_wall: glazed ceramic tiles (2×2, grouted) — the bowl surface.
    wall = Image.new("RGBA", (16, 16))
    for oy in (0, 8):
        for ox in (0, 8):
            for y in range(oy, oy + 8):
                for x in range(ox, ox + 8):
                    wall.putpixel((x, y), T.R(T.CERAMIC, 4))
            for x in range(ox, ox + 8):  # tile bevel: light top, grout-dark bottom
                wall.putpixel((x, oy), T.R(T.CERAMIC, 5))
                wall.putpixel((x, oy + 7), T.R(T.CERAMIC, 1))
            for y in range(oy, oy + 8):
                wall.putpixel((ox, y), T.R(T.CERAMIC, 5))
                wall.putpixel((ox + 7, y), T.R(T.CERAMIC, 2))
            for i in range(3):  # glaze glint
                wall.putpixel((ox + 2 + i, oy + 4 - i), T.R(T.CERAMIC, 5))
    wall.save(tex / "funnel_wall.png")

    # funnel_core: a shower-drain grate — concentric slots in blued steel.
    core = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))  # square rings match the block
            if d > 6.5:
                ring = 3       # outer frame
            elif d > 5.5:
                ring = 0       # slot
            elif d > 3.5:
                ring = 3       # web
            elif d > 2.5:
                ring = 0       # slot
            else:
                ring = 3       # hub
            core.putpixel((x, y), T.R(T.DARK, ring))
    for x in range(16):  # light catches the top edge of each metal ring
        for y in range(15):
            here = core.getpixel((x, y))
            below = core.getpixel((x, y + 1))
            if here == T.R(T.DARK, 3) and below == T.R(T.DARK, 0):
                core.putpixel((x, y), T.R(T.DARK, 2))
            if here == T.R(T.DARK, 0) and below == T.R(T.DARK, 3):
                core.putpixel((x, y + 1), T.R(T.DARK, 4))
    T.rivet(core, 7, 7, T.DARK, base=3)  # center screw
    core.save(tex / "funnel_core.png")


def gen_data():
    gen_textures()
    models = RES / f"assets/{MOD}/models"

    # The bowl wall: a ceramic cube whose TOP is intrinsic water (cauldron-style overlay, biome-
    # tinted, no FluidState), so the stamped bowl reads as terraced water sheeting to the drain.
    write_json(models / "block/funnel_wall.json", {
        "render_type": "minecraft:translucent",
        "textures": {"particle": f"{MOD}:block/funnel_wall",
                     "all": f"{MOD}:block/funnel_wall", "water": "minecraft:block/water_still"},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 16, 16],
            "faces": {
                "down": face("#all", cull="down"),
                "up": {"texture": "#water", "tintindex": 0},
                "north": face("#all", cull="north"), "south": face("#all", cull="south"),
                "east": face("#all", cull="east"), "west": face("#all", cull="west"),
            },
        }],
    })

    # The core: a low drain grate (no collision in Java — riders drop through the center), with a
    # thin water film pooling over it so the drain reads as wet.
    write_json(models / "block/funnel_core.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:translucent",
        "textures": {"particle": f"{MOD}:block/funnel_core",
                     "grate": f"{MOD}:block/funnel_core", "side": f"{MOD}:block/funnel_wall",
                     "water": "minecraft:block/water_still"},
        "elements": [
            {
                "from": [0, 0, 0], "to": [16, 3, 16],
                "faces": {
                    "up": face("#grate"), "down": face("#side", cull="down"),
                    "north": face("#side"), "south": face("#side"),
                    "east": face("#side"), "west": face("#side"),
                },
            },
            {
                "from": [1, 3, 1], "to": [15, 3.2, 15], "shade": False,
                "faces": {"up": {"texture": "#water", "tintindex": 0}},
            },
        ],
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
