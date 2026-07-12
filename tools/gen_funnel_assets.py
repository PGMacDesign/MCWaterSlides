#!/usr/bin/env python3
"""Generate Tornado Funnel assets: the exit-collar core (one model, three size-variants that
only differ in the cone they stamp) and the two pinwheel shell blocks the core auto-builds
the side-lying cone from (ceramic + warm accent — no items, machine-stamped only) —
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

# warm glazed accent — the pinwheel's second colour (coral, hue-shifted dark→light)
ACCENT = [T._hex(c) for c in ("8a3524", "a84430", "c25640", "d4684e", "e07d5f", "ea9273")]


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


def _tile_texture(ramp):
    """Glazed 2×2 tile face in the given ramp — the shell surface."""
    img = Image.new("RGBA", (16, 16))
    for oy in (0, 8):
        for ox in (0, 8):
            for y in range(oy, oy + 8):
                for x in range(ox, ox + 8):
                    img.putpixel((x, y), T.R(ramp, 4))
            for x in range(ox, ox + 8):  # tile bevel: light top, grout-dark bottom
                img.putpixel((x, oy), T.R(ramp, 5))
                img.putpixel((x, oy + 7), T.R(ramp, 1))
            for y in range(oy, oy + 8):
                img.putpixel((ox, y), T.R(ramp, 5))
                img.putpixel((ox + 7, y), T.R(ramp, 2))
            for i in range(3):  # glaze glint
                img.putpixel((ox + 2 + i, oy + 4 - i), T.R(ramp, 5))
    return img


def gen_textures():
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    _tile_texture(T.CERAMIC).save(tex / "funnel_wall.png")
    _tile_texture(ACCENT).save(tex / "funnel_wall_accent.png")

    # funnel_core sides: ceramic tile ringed like the shell it anchors
    _tile_texture(T.CERAMIC).save(tex / "funnel_core_side.png")

    # funnel_core front (the exit face riders fire over): a copper port collar
    front = Image.new("RGBA", (16, 16))
    T.plate(front, 0, 0, 15, 15, T.CERAMIC, streak=False)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d > 7.2:
                continue
            if d > 5.2:
                # copper collar ring, lit toward the top-left
                front.putpixel((x, y), T.R(T.COPPER, 6 if (x - 7.5) + (y - 7.5) < -3 else 4))
            else:
                front.putpixel((x, y), T.R(T.GLOW, 2 if d < 3 else 1))
    T.rivet(front, 1, 1, T.COPPER)
    T.rivet(front, 13, 1, T.COPPER)
    T.rivet(front, 1, 13, T.COPPER)
    T.rivet(front, 13, 13, T.COPPER)
    front.save(tex / "funnel_core_front.png")


def _shell_model(texture_name):
    """A shell cube whose TOP is intrinsic water (cauldron-style overlay, biome-tinted, no
    FluidState) — upward faces of the stamped cone read as water sheeting down the trough."""
    return {
        "render_type": "minecraft:translucent",
        "textures": {"particle": f"{MOD}:block/{texture_name}",
                     "all": f"{MOD}:block/{texture_name}", "water": "minecraft:block/water_still"},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 16, 16],
            "faces": {
                "down": face("#all", cull="down"),
                "up": {"texture": "#water", "tintindex": 0},
                "north": face("#all", cull="north"), "south": face("#all", cull="south"),
                "east": face("#all", cull="east"), "west": face("#all", cull="west"),
            },
        }],
    }


def gen_data():
    gen_textures()
    models = RES / f"assets/{MOD}/models"

    write_json(models / "block/funnel_wall.json", _shell_model("funnel_wall"))
    write_json(models / "block/funnel_wall_accent.json", _shell_model("funnel_wall_accent"))

    # The core: a solid exit collar — ceramic like the shell, copper port on the exit face,
    # a thin water film on top (riders skid over it and out).
    write_json(models / "block/funnel_core.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:translucent",
        "textures": {"particle": f"{MOD}:block/funnel_core_side",
                     "front": f"{MOD}:block/funnel_core_front",
                     "side": f"{MOD}:block/funnel_core_side",
                     "water": "minecraft:block/water_still"},
        "elements": [
            {
                "from": [0, 0, 0], "to": [16, 16, 16],
                "faces": {
                    "up": face("#side"), "down": face("#side", cull="down"),
                    "north": face("#front", cull="north"), "south": face("#side", cull="south"),
                    "east": face("#side", cull="east"), "west": face("#side", cull="west"),
                },
            },
            {
                "from": [1, 16, 1], "to": [15, 16.2, 15], "shade": False,
                "faces": {"up": {"texture": "#water", "tintindex": 0}},
            },
        ],
    })

    write_json(RES / f"assets/{MOD}/blockstates/funnel_wall.json",
               {"variants": {"": {"model": f"{MOD}:block/funnel_wall"}}})
    write_json(RES / f"assets/{MOD}/blockstates/funnel_wall_accent.json",
               {"variants": {"": {"model": f"{MOD}:block/funnel_wall_accent"}}})
    # model front faces north; rotate per exit facing (size never changes the model)
    write_json(RES / f"assets/{MOD}/blockstates/funnel_core.json", {
        "variants": {
            "facing=north": {"model": f"{MOD}:block/funnel_core"},
            "facing=south": {"model": f"{MOD}:block/funnel_core", "y": 180},
            "facing=east": {"model": f"{MOD}:block/funnel_core", "y": 90},
            "facing=west": {"model": f"{MOD}:block/funnel_core", "y": 270},
        },
    })

    for s in SIZES:
        write_json(models / f"item/funnel_core_{s}.json", {"parent": f"{MOD}:block/funnel_core"})

    # Only the core drops (the size-matching item); shell blocks are stamped, not owned.
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
              [f"{MOD}:funnel_core", f"{MOD}:funnel_wall", f"{MOD}:funnel_wall_accent"])

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.funnel_core"] = "Tornado Funnel Core"
    lang[f"block.{MOD}.funnel_wall"] = "Funnel Shell"
    lang[f"block.{MOD}.funnel_wall_accent"] = "Funnel Shell (Accent)"
    lang[f"item.{MOD}.funnel_core_small"] = "Small Tornado Funnel"
    lang[f"item.{MOD}.funnel_core_medium"] = "Medium Tornado Funnel"
    lang[f"item.{MOD}.funnel_core_large"] = "Large Tornado Funnel"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_data()
    print(f"Generated funnel assets under {RES}")
