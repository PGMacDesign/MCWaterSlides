#!/usr/bin/env python3
"""Generate Water Conduit assets: copper-pipe textures, multipart blockstate
(core + arm per connected side), models, loot, recipe (6 copper + 2 clay -> 12),
tag + lang merges."""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"

COPPER = (184, 115, 81)


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def merge_tag(path: Path, values):
    existing = json.loads(path.read_text())["values"] if path.exists() else []
    merged = sorted(set(existing) | set(values))
    write_json(path, {"replace": False, "values": merged})


def gen_textures():
    rng = random.Random(20260710)
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    pipe = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = rng.randint(-8, 8)
            shade = -24 if y in (5, 10) else 0  # pipe banding
            pipe.putpixel((x, y), (max(0, min(255, COPPER[0] + v + shade)),
                                   max(0, min(255, COPPER[1] + v + shade)),
                                   max(0, min(255, COPPER[2] + v + shade)), 255))
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
