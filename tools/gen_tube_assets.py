#!/usr/bin/env python3
"""Generate Slide Tube assets for all 17 colors.

Tube = channel body + lid (with a translucent window strip) + VERTICAL shaft model.
Reuses the channel textures (same tint layers: 0 = water, 1 = lining) plus a new
window texture. Blockstates are multipart: body (solid) + water overlay + window
(translucent), keyed on the 11 TubeShape values.
"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"

DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
ALL_TUBES = ["slide_tube"] + [f"{c}_slide_tube" for c in DYES]
CHANNEL_FOR = {"slide_tube": "slide_channel",
               **{f"{c}_slide_tube": f"{c}_slide_channel" for c in DYES}}

WALL_H = 14
WATER_Y = 12


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


def body_textures():
    return {
        "particle": f"{MOD}:block/slide_channel_base",
        "base": f"{MOD}:block/slide_channel_base",
        "lining": f"{MOD}:block/slide_channel_lining",
    }


def gen_textures():
    rng = random.Random(20260711)
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)
    window = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = rng.randint(-6, 6)
            a = 120 if (x + y) % 7 else 150  # faint streaks
            window.putpixel((x, y), (200 + v, 226 + v, 240 + v, a))
    window.save(tex / "slide_tube_window.png")


def all_sides(tex, tint=None):
    return {d: face(tex, tint=tint) for d in ("north", "south", "east", "west", "up", "down")}


def floor_elements():
    return [
        element([0, 0, 0], [16, 2, 16], {
            "down": face("#base", cull="down"), "up": face("#lining", tint=1),
            "north": face("#base", cull="north"), "south": face("#base", cull="south"),
            "east": face("#base", cull="east"), "west": face("#base", cull="west"),
        }),
    ]


def straight_wall_elements():
    # Fully tinted so the dye reads from outside too (and in the inventory GUI).
    return [
        element([0, 2, 0], [2, WALL_H, 16], {
            "west": face("#lining", tint=1, cull="west"), "east": face("#lining", tint=1),
            "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1, cull="south"),
        }),
        element([14, 2, 0], [16, WALL_H, 16], {
            "east": face("#lining", tint=1, cull="east"), "west": face("#lining", tint=1),
            "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1, cull="south"),
        }),
    ]


def straight_lid_elements():
    # lid rails either side of the window strip — tinted top so color shows from above
    return [
        element([0, WALL_H, 0], [5, 16, 16], {
            "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1),
            "west": face("#lining", tint=1, cull="west"), "east": face("#lining", tint=1),
            "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1, cull="south"),
        }),
        element([11, WALL_H, 0], [16, 16, 16], {
            "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1),
            "east": face("#lining", tint=1, cull="east"), "west": face("#lining", tint=1),
            "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1, cull="south"),
        }),
    ]


def corner_wall_elements():
    return [
        element([0, 2, 0], [16, WALL_H, 2], {
            "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1),
            "east": face("#lining", tint=1, cull="east"), "west": face("#lining", tint=1, cull="west"),
        }),
        element([0, 2, 2], [2, WALL_H, 16], {
            "west": face("#lining", tint=1, cull="west"), "east": face("#lining", tint=1),
            "south": face("#lining", tint=1, cull="south"),
        }),
    ]


def corner_lid_elements():
    # full lid frame with center window opening
    return [
        element([0, WALL_H, 0], [16, 16, 5], {
            "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1),
            "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1),
            "east": face("#lining", tint=1, cull="east"), "west": face("#lining", tint=1, cull="west"),
        }),
        element([0, WALL_H, 11], [16, 16, 16], {
            "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1),
            "south": face("#lining", tint=1, cull="south"), "north": face("#lining", tint=1),
            "east": face("#lining", tint=1, cull="east"), "west": face("#lining", tint=1, cull="west"),
        }),
        element([0, WALL_H, 5], [5, 16, 11], {
            "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1),
            "west": face("#lining", tint=1, cull="west"), "east": face("#lining", tint=1),
        }),
        element([11, WALL_H, 5], [16, 16, 11], {
            "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1),
            "east": face("#lining", tint=1, cull="east"), "west": face("#lining", tint=1),
        }),
    ]


def model_of(elements):
    return {"textures": body_textures(), "elements": elements}


def straight_tube_body():
    """Full straight body (inventory + legacy composite): floor + walls + lid."""
    return model_of(floor_elements() + straight_wall_elements() + straight_lid_elements())


def vertical_tube_body():
    """Four walls, open top/bottom."""
    return {
        "textures": body_textures(),
        "elements": [
            element([0, 0, 0], [2, 16, 16], {
                "west": face("#lining", tint=1, cull="west"), "east": face("#lining", tint=1),
                "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1, cull="south"),
                "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1, cull="down"),
            }),
            element([14, 0, 0], [16, 16, 16], {
                "east": face("#lining", tint=1, cull="east"), "west": face("#lining", tint=1),
                "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1, cull="south"),
                "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1, cull="down"),
            }),
            element([2, 0, 0], [14, 16, 2], {
                "north": face("#lining", tint=1, cull="north"), "south": face("#lining", tint=1),
                "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1, cull="down"),
            }),
            element([2, 0, 14], [14, 16, 16], {
                "south": face("#lining", tint=1, cull="south"), "north": face("#lining", tint=1),
                "up": face("#lining", tint=1, cull="up"), "down": face("#lining", tint=1, cull="down"),
            }),
        ],
    }


def window_strip():
    return {
        "render_type": "minecraft:translucent",
        "textures": {"particle": f"{MOD}:block/slide_tube_window", "w": f"{MOD}:block/slide_tube_window"},
        "elements": [element([5, WALL_H, 0], [11, 16, 16], {
            "up": face("#w"), "down": face("#w"),
        }, shade=False)],
    }


def corner_window():
    return {
        "render_type": "minecraft:translucent",
        "textures": {"particle": f"{MOD}:block/slide_tube_window", "w": f"{MOD}:block/slide_tube_window"},
        "elements": [element([5, WALL_H, 5], [11, 16, 11], {
            "up": face("#w"), "down": face("#w"),
        }, shade=False)],
    }


def water_overlay(region):
    fr, to = region
    return {
        "render_type": "minecraft:translucent",
        "textures": {"particle": "minecraft:block/water_still", "water": "minecraft:block/water_still"},
        "elements": [element([fr[0], WATER_Y - 0.2, fr[1]], [to[0], WATER_Y, to[1]],
                             {"up": face("#water", tint=0)}, shade=False)],
    }


# TubeShape -> (body family, rotation)
SHAPE_VARIANTS = {
    "north_south": ("slide_tube", 0),
    "east_west": ("slide_tube", 90),
    "ascending_south": ("slide_tube_ascending", 0),
    "ascending_west": ("slide_tube_ascending", 90),
    "ascending_north": ("slide_tube_ascending", 180),
    "ascending_east": ("slide_tube_ascending", 270),
    "south_east": ("slide_tube_corner", 0),
    "south_west": ("slide_tube_corner", 90),
    "north_west": ("slide_tube_corner", 180),
    "north_east": ("slide_tube_corner", 270),
    "vertical": ("slide_tube_vertical", 0),
}

# Flat families split into parts so tall bores (open_up/open_down) can drop the lid
# and floor per-state. Ascending/vertical never pair — single model, flags ignored.
# family -> (walls model, [floor-side models], [lid-side models])
FLAT_PARTS = {
    "slide_tube": ("slide_tube_walls",
                   ["slide_tube_floor", "slide_tube_water"],
                   ["slide_tube_lid", "slide_tube_window_strip"]),
    "slide_tube_corner": ("slide_tube_corner_walls",
                          ["slide_tube_floor", "slide_tube_corner_water"],
                          ["slide_tube_corner_lid", "slide_tube_corner_window"]),
}

WHOLE_FAMILY_OVERLAYS = {
    "slide_tube_ascending": ["slide_channel_ascending_water"],  # reuse channel cascade
    "slide_tube_vertical": [],
}


def blockstate():
    parts = []
    for shape, (family, rot) in SHAPE_VARIANTS.items():
        def apply_of(model):
            apply = {"model": f"{MOD}:block/{model}"}
            if rot:
                apply["y"] = rot
            return apply
        if family in FLAT_PARTS:
            walls, floor_side, lid_side = FLAT_PARTS[family]
            parts.append({"when": {"shape": shape}, "apply": apply_of(walls)})
            for model in floor_side:
                parts.append({"when": {"shape": shape, "open_down": "false"}, "apply": apply_of(model)})
            for model in lid_side:
                parts.append({"when": {"shape": shape, "open_up": "false"}, "apply": apply_of(model)})
        else:
            for model in [family] + WHOLE_FAMILY_OVERLAYS[family]:
                parts.append({"when": {"shape": shape}, "apply": apply_of(model)})
    return {"multipart": parts}


def gen_data():
    models = RES / f"assets/{MOD}/models"
    # composite (inventory parent) + the per-part models the blockstate composes
    write_json(models / "block/slide_tube.json", straight_tube_body())
    write_json(models / "block/slide_tube_floor.json", model_of(floor_elements()))
    write_json(models / "block/slide_tube_walls.json", model_of(straight_wall_elements()))
    write_json(models / "block/slide_tube_lid.json", model_of(straight_lid_elements()))
    write_json(models / "block/slide_tube_corner_walls.json", model_of(corner_wall_elements()))
    write_json(models / "block/slide_tube_corner_lid.json", model_of(corner_lid_elements()))
    write_json(models / "block/slide_tube_corner.json",
               model_of(floor_elements() + corner_wall_elements() + corner_lid_elements()))
    write_json(models / "block/slide_tube_vertical.json", vertical_tube_body())
    # ascending tube reuses the channel's stepped body (visually open-top on slopes)
    write_json(models / "block/slide_tube_ascending.json",
               {"parent": f"{MOD}:block/slide_channel_ascending"})
    write_json(models / "block/slide_tube_water.json", water_overlay(((2, 0), (14, 16))))
    write_json(models / "block/slide_tube_corner_water.json", water_overlay(((2, 2), (16, 16))))
    write_json(models / "block/slide_tube_window_strip.json", window_strip())
    write_json(models / "block/slide_tube_corner_window.json", corner_window())

    # inventory model: straight body reused directly (tint via item colors)
    write_json(models / "block/slide_tube_inventory.json",
               {"parent": f"{MOD}:block/slide_tube"})

    for name in ALL_TUBES:
        write_json(RES / f"assets/{MOD}/blockstates/{name}.json", blockstate())
        write_json(models / f"item/{name}.json", {"parent": f"{MOD}:block/slide_tube_inventory"})
        write_json(RES / f"data/{MOD}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "random_sequence": f"{MOD}:blocks/{name}",
            "pools": [{
                "rolls": 1,
                "bonus_rolls": 0,
                "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                "conditions": [{"condition": "minecraft:survives_explosion"}],
            }],
        })
        # 4 matching channels + 4 glass -> 4 tubes
        write_json(RES / f"data/{MOD}/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "group": "slide_tube",
            "pattern": ["GCG", "C C", "GCG"],
            "key": {
                "C": {"item": f"{MOD}:{CHANNEL_FOR[name]}"},
                "G": {"item": "minecraft:glass"},
            },
            "result": {"id": f"{MOD}:{name}", "count": 4},
        })

    ids = [f"{MOD}:{n}" for n in ALL_TUBES]
    merge_tag(RES / f"data/{MOD}/tags/item/slide_tubes.json", ids)
    merge_tag(RES / f"data/{MOD}/tags/block/slide_tubes.json", ids)
    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json", ids)

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.slide_tube"] = "Slide Tube"
    for color in DYES:
        pretty = " ".join(w.capitalize() for w in color.split("_"))
        lang[f"block.{MOD}.{color}_slide_tube"] = f"{pretty} Slide Tube"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_textures()
    gen_data()
    print(f"Generated tube assets for {len(ALL_TUBES)} colors under {RES}")
