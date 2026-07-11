#!/usr/bin/env python3
"""Generate Water Conduit assets: copper-pipe textures, multipart blockstate
(core + arm per connected side), models, loot, recipe (6 copper + 2 clay -> 12),
tag + lang merges."""
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


# vertical pipe cylinder: specular band left-of-center, rolling darker to the edges
_PIPE_COLS = (1, 2, 3, 5, 6, 7, 6, 5, 5, 4, 4, 3, 3, 2, 2, 1)


def gen_textures():
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    pipe = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            pipe.putpixel((x, y), T.R(T.COPPER, _PIPE_COLS[x]))
    for band_y in (4, 11):  # iron coupling collars
        for x in range(16):
            pipe.putpixel((x, band_y), T.R(T.IRON, max(1, _PIPE_COLS[x] - 1)))
            pipe.putpixel((x, band_y + 1), T.R(T.IRON, max(0, _PIPE_COLS[x] - 3)))
        for x in (3, 8, 13):
            pipe.putpixel((x, band_y), T.R(T.IRON, 6))
            pipe.putpixel((x, band_y + 1), T.R(T.IRON, 1))
    pipe.save(tex / "water_conduit.png")


def element(fr, to):
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        faces[f] = {"texture": "#pipe"}
    return {"from": fr, "to": to, "faces": faces}


def gen_data():
    models = RES / f"assets/{MOD}/models"
    textures = {"particle": f"{MOD}:block/water_conduit", "pipe": f"{MOD}:block/water_conduit"}
    write_json(models / "block/water_conduit_core.json",
               {"textures": textures, "elements": [element([5, 5, 5], [11, 11, 11])]})
    # arm extends north; blockstate rotates it per side
    write_json(models / "block/water_conduit_arm.json",
               {"textures": textures, "elements": [element([5, 5, 0], [11, 11, 5])]})
    write_json(models / "item/water_conduit.json", {
        "parent": f"{MOD}:block/water_conduit_core",
    })

    arm = f"{MOD}:block/water_conduit_arm"
    write_json(RES / f"assets/{MOD}/blockstates/water_conduit.json", {"multipart": [
        {"apply": {"model": f"{MOD}:block/water_conduit_core"}},
        {"when": {"north": "true"}, "apply": {"model": arm}},
        {"when": {"south": "true"}, "apply": {"model": arm, "y": 180}},
        {"when": {"east": "true"}, "apply": {"model": arm, "y": 90}},
        {"when": {"west": "true"}, "apply": {"model": arm, "y": 270}},
        {"when": {"up": "true"}, "apply": {"model": arm, "x": 270}},
        {"when": {"down": "true"}, "apply": {"model": arm, "x": 90}},
    ]})

    write_json(RES / f"data/{MOD}/loot_table/blocks/water_conduit.json", {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/water_conduit",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:water_conduit"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

    write_json(RES / f"data/{MOD}/recipe/water_conduit.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "pattern": ["CCC", "K K", "CCC"],
        "key": {
            "C": {"item": "minecraft:copper_ingot"},
            "K": {"item": "minecraft:clay_ball"},
        },
        "result": {"id": f"{MOD}:water_conduit", "count": 12},
    })

    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json", [f"{MOD}:water_conduit"])

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.water_conduit"] = "Water Conduit"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_textures()
    gen_data()
    print(f"Generated conduit assets under {RES}")
