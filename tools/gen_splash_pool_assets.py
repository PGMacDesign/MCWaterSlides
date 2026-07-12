#!/usr/bin/env python3
"""Generate Splash Pool assets: floor + per-side wall models composed via multipart
(walls appear only where no pool neighbors), water overlay, loot, recipe, tags + lang."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"

WALL_H = 8
WATER_Y = 6


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def merge_tag(path: Path, values):
    existing = json.loads(path.read_text())["values"] if path.exists() else []
    merged = sorted(set(existing) | set(values))
    write_json(path, {"replace": False, "values": merged})


def face(texture, tint=None, cull=None):
    f = {"texture": texture}
    if tint is not None:
        f["tintindex"] = tint
    if cull:
        f["cullface"] = cull
    return f


def element(fr, to, faces, shade=None):
    e = {"from": fr, "to": to, "faces": faces}
    if shade is not None:
        e["shade"] = shade
    return e


def textures():
    return {
        "particle": f"{MOD}:block/slide_channel_base",
        "base": f"{MOD}:block/slide_channel_base",
        "lining": f"{MOD}:block/slide_channel_lining",
    }


def gen_data():
    models = RES / f"assets/{MOD}/models"

    write_json(models / "block/splash_pool_floor.json", {
        "textures": textures(),
        "elements": [element([0, 0, 0], [16, 2, 16], {
            "down": face("#base", cull="down"), "up": face("#lining", tint=1),
            "north": face("#base", cull="north"), "south": face("#base", cull="south"),
            "east": face("#base", cull="east"), "west": face("#base", cull="west"),
        })],
    })
    # wall along the north edge; blockstate rotates per side
    write_json(models / "block/splash_pool_wall.json", {
        "textures": textures(),
        "elements": [element([0, 2, 0], [16, WALL_H, 2], {
            "north": face("#base", cull="north"), "south": face("#lining", tint=1),
            "up": face("#base"), "east": face("#base"), "west": face("#base"),
        })],
    })
    write_json(models / "block/splash_pool_water.json", {
        "render_type": "minecraft:translucent",
        "textures": {"particle": "minecraft:block/water_still", "water": "minecraft:block/water_still"},
        "elements": [element([0, WATER_Y - 0.2, 0], [16, WATER_Y, 16],
                             {"up": face("#water", tint=0)}, shade=False)],
    })
    # inventory: floor + all four walls + water. The block/block parent supplies the
    # standard isometric GUI transform — without it the icon renders face-on (a blank
    # wall) and the water on top is invisible in the inventory.
    inv = {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:translucent",
        "textures": {**textures(), "water": "minecraft:block/water_still"},
        "elements": [
            element([0, 0, 0], [16, 2, 16], {
                "down": face("#base"), "up": face("#lining", tint=1),
                "north": face("#base"), "south": face("#base"),
                "east": face("#base"), "west": face("#base"),
            }),
            element([0, 2, 0], [16, WALL_H, 2], {"north": face("#base"), "south": face("#lining", tint=1), "up": face("#base")}),
            element([0, 2, 14], [16, WALL_H, 16], {"south": face("#base"), "north": face("#lining", tint=1), "up": face("#base")}),
            element([0, 2, 2], [2, WALL_H, 14], {"west": face("#base"), "east": face("#lining", tint=1), "up": face("#base")}),
            element([14, 2, 2], [16, WALL_H, 14], {"east": face("#base"), "west": face("#lining", tint=1), "up": face("#base")}),
            element([2, WATER_Y - 0.2, 2], [14, WATER_Y, 14], {"up": face("#water", tint=0)}, shade=False),
        ],
    }
    write_json(models / "block/splash_pool_inventory.json", inv)
    write_json(models / "item/splash_pool.json", {"parent": f"{MOD}:block/splash_pool_inventory"})

    wall = f"{MOD}:block/splash_pool_wall"
    write_json(RES / f"assets/{MOD}/blockstates/splash_pool.json", {"multipart": [
        {"apply": {"model": f"{MOD}:block/splash_pool_floor"}},
        {"apply": {"model": f"{MOD}:block/splash_pool_water"}},
        {"when": {"north": "false"}, "apply": {"model": wall}},
        {"when": {"south": "false"}, "apply": {"model": wall, "y": 180}},
        {"when": {"east": "false"}, "apply": {"model": wall, "y": 90}},
        {"when": {"west": "false"}, "apply": {"model": wall, "y": 270}},
    ]})

    write_json(RES / f"data/{MOD}/loot_table/blocks/splash_pool.json", {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/splash_pool",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:splash_pool"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

    write_json(RES / f"data/{MOD}/recipe/splash_pool.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "pattern": ["WKW", "CCC"],
        "key": {
            "W": {"item": "minecraft:white_wool"},
            "K": {"item": "minecraft:clay_ball"},
            "C": {"tag": f"{MOD}:slide_channels"},
        },
        "result": {"id": f"{MOD}:splash_pool", "count": 2},
    })

    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json", [f"{MOD}:splash_pool"])

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.splash_pool"] = "Splash Pool"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_data()
    print(f"Generated splash pool assets under {RES}")
