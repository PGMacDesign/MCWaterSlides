#!/usr/bin/env python3
"""Generate Jet block assets, reproducibly.

Emits textures (copper body, nozzle front off/on, intake back), the directional
blockstate (facing x energized variants), block/item models, loot table, recipe
(4 copper + 2 iron + 1 redstone -> 2), and merges tags + lang.
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


# ── textures ────────────────────────────────────────────────────────────────

def gen_textures():
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    # side: cut-copper plates with embossed flow chevrons pointing toward the nozzle
    side = T.plates2x2(T.COPPER, streak=False)
    for cy in (2, 9):
        for i in range(5):
            for x in (3 + i, 12 - i):
                y = cy + 4 - i
                side.putpixel((x, y), T.R(T.COPPER, 1))
                side.putpixel((x, y + 1), T.R(T.COPPER, 6))
    side.save(tex / "jet_side.png")

    # back: one copper plate with a 3×3 grid of recessed intake ports
    back = Image.new("RGBA", (16, 16))
    T.plate(back, 0, 0, 15, 15, T.COPPER, streak=False)
    for hy in (3, 7, 11):
        for hx in (3, 7, 11):
            back.putpixel((hx, hy), T.R(T.DARK, 0))
            back.putpixel((hx + 1, hy), T.R(T.DARK, 0))
            back.putpixel((hx, hy + 1), T.R(T.DARK, 2))
            back.putpixel((hx + 1, hy + 1), T.R(T.DARK, 2))
            back.putpixel((hx, hy + 2), T.R(T.COPPER, 6))
            back.putpixel((hx + 1, hy + 2), T.R(T.COPPER, 6))
    T.rivet(back, 1, 1, T.COPPER)
    T.rivet(back, 13, 1, T.COPPER)
    T.rivet(back, 1, 13, T.COPPER)
    T.rivet(back, 13, 13, T.COPPER)
    back.save(tex / "jet_back.png")

    # front: copper plate, recessed dark-steel nozzle ring, round core (cyan when on)
    for name, on in (("jet_front", False), ("jet_front_on", True)):
        img = Image.new("RGBA", (16, 16))
        T.plate(img, 0, 0, 15, 15, T.COPPER, streak=False)
        for y in range(16):
            for x in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if d >= 6.4:
                    continue
                if d >= 4.6:
                    # recessed ring: shadow toward top-left, light catch bottom-right
                    upper = (x - 7.5) + (y - 7.5) < 0
                    img.putpixel((x, y), T.R(T.DARK, 1 if upper else 3))
                elif on:
                    img.putpixel((x, y), T.R(T.GLOW, 5 if d < 1.8 else (4 if d < 3.2 else 2)))
                else:
                    img.putpixel((x, y), T.R(T.DARK, 2 if d < 3.2 else 1))
        if not on:  # idle glass glint
            img.putpixel((6, 6), T.R(T.DARK, 4))
            img.putpixel((7, 7), T.R(T.DARK, 3))
        T.rivet(img, 1, 1, T.COPPER)
        T.rivet(img, 13, 1, T.COPPER)
        T.rivet(img, 1, 13, T.COPPER)
        T.rivet(img, 13, 13, T.COPPER)
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
