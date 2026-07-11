#!/usr/bin/env python3
"""Generate the CLEAR (translucent glass) slide channel + tube variants.

Reuses the EXACT shape geometry from gen_channel_assets / gen_tube_assets, but swaps the
opaque ceramic body texture for a translucent glass texture and marks every body model
render_type=translucent — so the block is see-through all the way around. Water overlays and
the tube window are material-agnostic and reused verbatim. Two blocks only: clear_slide_channel
and clear_slide_tube (a standalone material, NOT a dye colour).

Deterministic. Run after gen_channel_assets.py / gen_tube_assets.py (it imports their builders).
"""
import json
from copy import deepcopy
from pathlib import Path

from PIL import Image

import texlib as T

import gen_channel_assets as ch
import gen_tube_assets as tb

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"
GLASS = f"{MOD}:block/slide_clear"


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def merge_tag(path, values):
    existing = json.loads(path.read_text())["values"] if path.exists() else []
    write_json(path, {"replace": False, "values": sorted(set(existing) | set(values))})


def gen_texture():
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)
    # Pale blue-white glass. Denser at the edges so a block's outline still reads
    # through the transparency; nearly clear in the middle, with one vanilla-style
    # diagonal glint at the top-left and an echo at the bottom-right.
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            img.putpixel((x, y), (205, 228, 242, 150 if edge else 70))
    for i in range(6):
        img.putpixel((2 + i, 7 - i), (245, 250, 253, 170))
        img.putpixel((3 + i, 7 - i), (245, 250, 253, 170))
    for i in range(3):
        img.putpixel((11 + i, 14 - i), (232, 242, 250, 140))
    img.save(tex / "slide_clear.png")


def as_clear(model):
    """Body model → glass texture + translucent render. Preserves a #water texture if present."""
    m = deepcopy(model)
    tex = m.setdefault("textures", {})
    for k in ("particle", "base", "lining"):
        tex[k] = GLASS
    m["render_type"] = "minecraft:translucent"
    return m


def clear_blockstate(base_state, token):
    """Reuse the coloured multipart but repoint every model at the clear_ set. The tube's
    shared slide_channel_ascending_water overlay is intentionally left alone (water is
    material-agnostic), because token='slide_tube' never matches 'slide_channel...'."""
    remapped = json.dumps(base_state).replace(f"{MOD}:block/{token}", f"{MOD}:block/clear_{token}")
    return json.loads(remapped)


def gen_channel_models(models):
    write_json(models / "block/clear_slide_channel.json", as_clear(ch.straight_body()))
    write_json(models / "block/clear_slide_channel_floor.json",
               as_clear(ch.model_of(ch.straight_floor_element())))
    write_json(models / "block/clear_slide_channel_wall_neg.json",
               as_clear(ch.model_of(ch.straight_wall_neg_element(), *ch.wall_neg_fillet(), *ch.wall_neg_lip())))
    write_json(models / "block/clear_slide_channel_wall_pos.json",
               as_clear(ch.model_of(ch.straight_wall_pos_element(), *ch.wall_pos_fillet(), *ch.wall_pos_lip())))
    write_json(models / "block/clear_slide_channel_corner.json", as_clear(ch.corner_body()))
    write_json(models / "block/clear_slide_channel_ascending.json", as_clear(ch.ascending_body()))
    for suffix, (x0, x1) in {"": (2, 14), "_neg": (0, 14), "_pos": (2, 16), "_full": (0, 16)}.items():
        write_json(models / f"block/clear_slide_channel_water{suffix}.json", ch.straight_water(x0, x1))
    write_json(models / "block/clear_slide_channel_corner_water.json", ch.corner_water())
    write_json(models / "block/clear_slide_channel_ascending_water.json", ch.ascending_water())
    write_json(models / "block/clear_slide_channel_inventory.json", as_clear(ch.inventory_model()))


def gen_tube_models(models):
    write_json(models / "block/clear_slide_tube.json", as_clear(tb.straight_tube_body()))
    write_json(models / "block/clear_slide_tube_floor.json", as_clear(tb.model_of(tb.floor_elements())))
    write_json(models / "block/clear_slide_tube_lid.json", as_clear(tb.model_of(tb.straight_lid_elements())))
    write_json(models / "block/clear_slide_tube_corner_lid.json",
               as_clear(tb.model_of(tb.corner_lid_elements())))
    for suffix, (yb, yt) in {"": (2, 14), "_up": (2, 16), "_down": (0, 14), "_full": (0, 16)}.items():
        write_json(models / f"block/clear_slide_tube_walls{suffix}.json",
                   as_clear(tb.model_of(tb.straight_wall_elements(yb, yt))))
        write_json(models / f"block/clear_slide_tube_corner_walls{suffix}.json",
                   as_clear(tb.model_of(tb.corner_wall_elements(yb, yt))))
    write_json(models / "block/clear_slide_tube_corner.json",
               as_clear(tb.model_of(tb.floor_elements() + tb.corner_wall_elements() + tb.corner_lid_elements())))
    write_json(models / "block/clear_slide_tube_vertical.json", as_clear(tb.vertical_tube_body()))
    # full model (not a parent ref) so render_type=translucent is guaranteed inline
    write_json(models / "block/clear_slide_tube_ascending.json", as_clear(ch.ascending_body()))
    write_json(models / "block/clear_slide_tube_water.json", tb.water_overlay(((2, 0), (14, 16))))
    write_json(models / "block/clear_slide_tube_corner_water.json", tb.water_overlay(((2, 2), (16, 16))))
    write_json(models / "block/clear_slide_tube_window_strip.json", tb.window_strip())
    write_json(models / "block/clear_slide_tube_corner_window.json", tb.corner_window())
    write_json(models / "block/clear_slide_tube_inventory.json", as_clear(tb.tube_inventory_model()))


def gen_data():
    gen_texture()
    models = RES / f"assets/{MOD}/models"
    gen_channel_models(models)
    gen_tube_models(models)

    write_json(RES / f"assets/{MOD}/blockstates/clear_slide_channel.json",
               clear_blockstate(ch.blockstate(), "slide_channel"))
    write_json(RES / f"assets/{MOD}/blockstates/clear_slide_tube.json",
               clear_blockstate(tb.blockstate(), "slide_tube"))

    write_json(models / "item/clear_slide_channel.json",
               {"parent": f"{MOD}:block/clear_slide_channel_inventory"})
    write_json(models / "item/clear_slide_tube.json",
               {"parent": f"{MOD}:block/clear_slide_tube_inventory"})

    for name in ("clear_slide_channel", "clear_slide_tube"):
        write_json(RES / f"data/{MOD}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "random_sequence": f"{MOD}:blocks/{name}",
            "pools": [{"rolls": 1, "bonus_rolls": 0,
                       "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        })

    # Recipes mirror the base tiers, glass-cheap: same copper+clay top, glass floor / glass ring.
    write_json(RES / f"data/{MOD}/recipe/clear_slide_channel.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "pattern": ["CKC", "GGG"],
        "key": {"C": {"item": "minecraft:copper_ingot"}, "K": {"item": "minecraft:clay_ball"},
                "G": {"item": "minecraft:glass"}},
        "result": {"id": f"{MOD}:clear_slide_channel", "count": 16},
    })
    write_json(RES / f"data/{MOD}/recipe/clear_slide_tube.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "group": "slide_tube",
        "pattern": ["GCG", "C C", "GCG"],
        "key": {"G": {"item": "minecraft:glass"}, "C": {"item": f"{MOD}:clear_slide_channel"}},
        "result": {"id": f"{MOD}:clear_slide_tube", "count": 4},
    })

    # Harvest tag only — keep clear OUT of the slide_channels/slide_tubes dye-recipe tags.
    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json",
              [f"{MOD}:clear_slide_channel", f"{MOD}:clear_slide_tube"])

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.clear_slide_channel"] = "Clear Slide Channel"
    lang[f"block.{MOD}.clear_slide_tube"] = "Clear Slide Tube"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_data()
    print("Generated clear (glass) slide channel + tube assets")
