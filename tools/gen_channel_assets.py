#!/usr/bin/env python3
"""Generate all Slide Channel assets, reproducibly (mirrors MC3DPrint's tools/ convention).

Emits, under src/main/resources:
  - textures: slide_channel_base.png (ceramic body), slide_channel_lining.png (near-white,
    dye-tinted at runtime via tintindex 1)
  - block models: body + translucent water overlay per shape family (straight/corner/ascending)
  - one inventory model (body + water combined)
  - blockstates (multipart: body part + water part per RailShape) x17 colors (identical files)
  - item models x17, loot tables x17, mineable/pickaxe + slide_channels tags
  - recipes: base (16x) + 16 ring-redye (8 + dye -> 8)
  - lang: merges block names into en_us.json (preserves existing keys)

Deterministic: fixed RNG seed, sorted iteration. Rerun any time; output is stable.
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
ALL_NAMES = ["slide_channel"] + [f"{c}_slide_channel" for c in DYES]

WALL_H = 14   # side wall top (px)
WATER_Y = 12  # waterline (px), brimming just under the walls


# ── textures ────────────────────────────────────────────────────────────────

def gen_textures():
    rng = random.Random(20260707)
    tex_dir = RES / f"assets/{MOD}/textures/block"
    tex_dir.mkdir(parents=True, exist_ok=True)

    base = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 178 + rng.randint(-7, 7)
            if x in (0, 15) or y in (0, 15):
                v -= 14  # subtle edge shading so blocks read as tiles
            base.putpixel((x, y), (v, v + 2, v + 4, 255))
    base.save(tex_dir / "slide_channel_base.png")

    lining = Image.new("RGBA", (16, 16))
    for y in range(16):
        streak = rng.randint(-4, 4)
        for x in range(16):
            v = 236 + streak + rng.randint(-3, 3)
            v = max(215, min(248, v))
            lining.putpixel((x, y), (v, v, v, 255))
    lining.save(tex_dir / "slide_channel_lining.png")


# ── block models ────────────────────────────────────────────────────────────

def face(texture, tint=None, cull=None):
    f = {"texture": texture}
    if tint is not None:
        f["tintindex"] = tint
    if cull:
        f["cullface"] = cull
    return f


def element(fr, to, faces, shade=None, rotation=None):
    e = {"from": fr, "to": to, "faces": faces}
    if shade is not None:
        e["shade"] = shade
    if rotation is not None:
        e["rotation"] = rotation
    return e


def ramp_rotation(reverse):
    """45° tilt about X so a flat plate reads as a ramp. reverse flips the rise direction."""
    return {"origin": [8, 8, 8], "axis": "x", "angle": 45 if reverse else -45, "rescale": True}


def body_textures():
    return {
        "particle": f"{MOD}:block/slide_channel_base",
        "base": f"{MOD}:block/slide_channel_base",
        "lining": f"{MOD}:block/slide_channel_lining",
    }


def straight_floor_element():
    return element([0, 0, 0], [16, 2, 16], {
        "down": face("#base", cull="down"),
        "up": face("#lining", tint=1),
        "north": face("#base", cull="north"),
        "south": face("#base", cull="south"),
        "east": face("#base", cull="east"),
        "west": face("#base", cull="west"),
    })


def straight_wall_neg_element():
    """West wall (neg X for a north-south run). Tinted on every face (color shows outside too)."""
    return element([0, 2, 0], [2, WALL_H, 16], {
        "west": face("#lining", tint=1, cull="west"),
        "east": face("#lining", tint=1),
        "up": face("#lining", tint=1),
        "north": face("#lining", tint=1, cull="north"),
        "south": face("#lining", tint=1, cull="south"),
    })


def straight_wall_pos_element():
    """East wall (pos X for a north-south run). Tinted on every face."""
    return element([14, 2, 0], [16, WALL_H, 16], {
        "east": face("#lining", tint=1, cull="east"),
        "west": face("#lining", tint=1),
        "up": face("#lining", tint=1),
        "north": face("#lining", tint=1, cull="north"),
        "south": face("#lining", tint=1, cull="south"),
    })


def _wedge_faces():
    # Only the chamfer face (up) + the z-caps render; the rest buries into the wall/floor.
    return {d: face("#lining", tint=1) for d in ("up", "north", "south")}


# Octagonal trough bottom: cut each floor↔wall corner with a SOLID 45° wedge (a plate rotated
# about Z whose rendered face is the chamfer diagonal, body buried in the wall/floor so there
# are no see-through gaps). Same technique as the straight-tube bore — reads as a clean beveled
# trough, not a staircase. D=3.5 → a ~5px flat bottom + 45° sides rising to the wall (then the
# vertical wall lip above). Model-only: collision stays the flat-floor U in SlideChannelShapes,
# and each wedge rides the conditional wall model, so it drops with the wall on a merged side.
_CHAMFER_D = 3.5
_CHAMFER_TH = 2.5   # perpendicular thickness > corner depth (D/√2≈2.47) so it fully fills the corner


def wall_neg_fillet():
    """West floor↔wall corner as a 45° chamfer (rides the west wall model)."""
    d, th = _CHAMFER_D, _CHAMFER_TH
    diag = d * (2 ** 0.5)
    return [element([2 + d - diag, 2 - th, 0], [2 + d, 2, 16], _wedge_faces(),
                    rotation={"origin": [2 + d, 2, 8], "axis": "z", "angle": -45})]


def wall_pos_fillet():
    """East floor↔wall corner as a 45° chamfer (mirror of {@link wall_neg_fillet})."""
    d, th = _CHAMFER_D, _CHAMFER_TH
    diag = d * (2 ** 0.5)
    return [element([14 - d, 2 - th, 0], [14 - d + diag, 2, 16], _wedge_faces(),
                    rotation={"origin": [14 - d, 2, 8], "axis": "z", "angle": 45})]


def straight_body():
    """North-south run: open ends N/S, walls E/W + rounded corners (composite for inventory)."""
    return {
        "textures": body_textures(),
        "elements": [straight_floor_element(), straight_wall_neg_element(), straight_wall_pos_element(),
                     *wall_neg_fillet(), *wall_pos_fillet()],
    }


def model_of(*elements):
    return {"textures": body_textures(), "elements": list(elements)}


def corner_body():
    """South-east corner: exits S/E, walls N/W."""
    return {
        "textures": body_textures(),
        "elements": [
            element([0, 0, 0], [16, 2, 16], {
                "down": face("#base", cull="down"),
                "up": face("#lining", tint=1),
                "north": face("#base", cull="north"),
                "south": face("#base", cull="south"),
                "east": face("#base", cull="east"),
                "west": face("#base", cull="west"),
            }),
            element([0, 2, 0], [16, WALL_H, 2], {
                "north": face("#lining", tint=1, cull="north"),
                "south": face("#lining", tint=1),
                "up": face("#lining", tint=1),
                "east": face("#lining", tint=1, cull="east"),
                "west": face("#lining", tint=1, cull="west"),
            }),
            element([0, 2, 2], [2, WALL_H, 16], {
                "west": face("#lining", tint=1, cull="west"),
                "east": face("#lining", tint=1),
                "up": face("#lining", tint=1),
                "south": face("#lining", tint=1, cull="south"),
            }),
        ],
    }


def ascending_body():
    """Rises toward south (+Z): a smooth 45° ramp floor between full-height walls E/W.
    Collision stays stepped (SlideChannelShapes) — this is the visual-only slope."""
    return {
        "textures": body_textures(),
        "elements": [
            element([0, 2, 0], [2, 16, 16], {
                "west": face("#lining", tint=1, cull="west"),
                "east": face("#lining", tint=1),
                "up": face("#lining", tint=1, cull="up"),
                "north": face("#lining", tint=1, cull="north"),
                "south": face("#lining", tint=1, cull="south"),
            }),
            element([14, 2, 0], [16, 16, 16], {
                "east": face("#lining", tint=1, cull="east"),
                "west": face("#lining", tint=1),
                "up": face("#lining", tint=1, cull="up"),
                "north": face("#lining", tint=1, cull="north"),
                "south": face("#lining", tint=1, cull="south"),
            }),
            # Ramp plate: a flat slab whose TOP face sits on the pivot plane (y=8), so a
            # 45° rescaled tilt sweeps its surface corner-to-corner — (z0,y0) up to
            # (z16,y16) — meeting the flat neighbor's floor at the bottom and the
            # next-level floor at the top. (Off the pivot plane it overshoots the cell,
            # which is what made the junctions look wonky.)
            element([2, 6, 0], [14, 8, 16], {
                "up": face("#lining", tint=1),
                "down": face("#base"),
                "north": face("#base"),
                "south": face("#base"),
            }, rotation=ramp_rotation(False)),
        ],
    }


def water_quad(fr, to):
    return element(fr, to, {"up": face("#water", tint=0)}, shade=False)


def straight_water(x0=2, x1=14):
    """Water sheet across the channel. x0/x1 push it out to the block edge (0/16) on any
    merged side so wide slides show continuous water — no bare floor strip at the seam."""
    return {
        "render_type": "minecraft:translucent",
        "textures": {"particle": "minecraft:block/water_still", "water": "minecraft:block/water_still"},
        "elements": [water_quad([x0, WATER_Y - 0.2, 0], [x1, WATER_Y, 16])],
    }


def corner_water():
    return {
        "render_type": "minecraft:translucent",
        "textures": {"particle": "minecraft:block/water_still", "water": "minecraft:block/water_still"},
        "elements": [water_quad([2, WATER_Y - 0.2, 2], [16, WATER_Y, 16])],
    }


def ascending_water():
    """Ramp water sheet: a tilted quad tracking the sloped floor, a hair above its surface."""
    quad = element([2, 6.2, 0], [14, 8.2, 16], {"up": face("#water", tint=0)},
                   shade=False, rotation=ramp_rotation(False))
    return {
        "render_type": "minecraft:translucent",
        "textures": {"particle": "minecraft:block/water_still", "water": "minecraft:block/water_still"},
        "elements": [quad],
    }


def inventory_model():
    m = straight_body()
    m["elements"] = m["elements"] + straight_water()["elements"]
    m["textures"]["water"] = "minecraft:block/water_still"
    m["render_type"] = "minecraft:translucent"
    m["parent"] = "minecraft:block/block"
    return m


# ── blockstates / items / loot / tags / recipes / lang ─────────────────────

# Straight shapes split floor/walls so wide-slide merges drop the shared wall
# per-state (wall_neg/wall_pos). shape -> y rotation.
STRAIGHT_VARIANTS = {"north_south": 0, "east_west": 90}

# (wall_neg, wall_pos) present -> water model suffix. A dropped wall (false) pushes the
# water to that edge: neg→x0, pos→x16.
WATER_SUFFIX = {(True, True): "", (False, True): "_neg", (True, False): "_pos", (False, False): "_full"}

# Corner/ascending shapes keep the whole body baked in. shape -> (family, y rotation).
WHOLE_VARIANTS = {
    "ascending_south": ("slide_channel_ascending", 0),
    "ascending_west": ("slide_channel_ascending", 90),
    "ascending_north": ("slide_channel_ascending", 180),
    "ascending_east": ("slide_channel_ascending", 270),
    "south_east": ("slide_channel_corner", 0),
    "south_west": ("slide_channel_corner", 90),
    "north_west": ("slide_channel_corner", 180),
    "north_east": ("slide_channel_corner", 270),
}


def blockstate():
    parts = []

    def apply_of(model, rot):
        a = {"model": f"{MOD}:block/{model}"}
        if rot:
            a["y"] = rot
        return a

    for shape, rot in STRAIGHT_VARIANTS.items():
        parts.append({"when": {"shape": shape}, "apply": apply_of("slide_channel_floor", rot)})
        # Water extends to the block edge on any merged side (wall dropped) so lanes read
        # as one continuous sheet. neg = west (x→0), pos = east (x→16) before rotation.
        for (neg, pos), suffix in WATER_SUFFIX.items():
            parts.append({
                "when": {"shape": shape, "wall_neg": str(neg).lower(), "wall_pos": str(pos).lower()},
                "apply": apply_of("slide_channel_water" + suffix, rot),
            })
        parts.append({"when": {"shape": shape, "wall_neg": "true"},
                      "apply": apply_of("slide_channel_wall_neg", rot)})
        parts.append({"when": {"shape": shape, "wall_pos": "true"},
                      "apply": apply_of("slide_channel_wall_pos", rot)})
    for shape, (family, rot) in WHOLE_VARIANTS.items():
        for model in (family, f"{family}_water"):
            parts.append({"when": {"shape": shape}, "apply": apply_of(model, rot)})
    return {"multipart": parts}


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def merge_tag(path: Path, values):
    existing = json.loads(path.read_text())["values"] if path.exists() else []
    merged = sorted(set(existing) | set(values))
    write_json(path, {"replace": False, "values": merged})


def loot_table(name):
    return {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/{name}",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    }


def gen_data():
    # models
    models = RES / f"assets/{MOD}/models"
    write_json(models / "block/slide_channel.json", straight_body())
    write_json(models / "block/slide_channel_floor.json", model_of(straight_floor_element()))
    write_json(models / "block/slide_channel_wall_neg.json",
               model_of(straight_wall_neg_element(), *wall_neg_fillet()))
    write_json(models / "block/slide_channel_wall_pos.json",
               model_of(straight_wall_pos_element(), *wall_pos_fillet()))
    write_json(models / "block/slide_channel_corner.json", corner_body())
    write_json(models / "block/slide_channel_ascending.json", ascending_body())
    # 4 water spans keyed on which walls merged (default 2-14; extend to 0/16 per side).
    for suffix, (x0, x1) in {"": (2, 14), "_neg": (0, 14), "_pos": (2, 16), "_full": (0, 16)}.items():
        write_json(models / f"block/slide_channel_water{suffix}.json", straight_water(x0, x1))
    write_json(models / "block/slide_channel_corner_water.json", corner_water())
    write_json(models / "block/slide_channel_ascending_water.json", ascending_water())
    write_json(models / "block/slide_channel_inventory.json", inventory_model())

    for name in ALL_NAMES:
        write_json(RES / f"assets/{MOD}/blockstates/{name}.json", blockstate())
        write_json(models / f"item/{name}.json", {"parent": f"{MOD}:block/slide_channel_inventory"})
        write_json(RES / f"data/{MOD}/loot_table/blocks/{name}.json", loot_table(name))

    # tags (merge-write so other generators' entries survive a re-run)
    ids = [f"{MOD}:{n}" for n in ALL_NAMES]
    merge_tag(RES / f"data/{MOD}/tags/item/slide_channels.json", ids)
    merge_tag(RES / f"data/{MOD}/tags/block/slide_channels.json", ids)
    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json", ids)

    # recipes (1.21.1 ingredient form; build.gradle rewrites for >=1.21.2 nodes)
    write_json(RES / f"data/{MOD}/recipe/slide_channel.json", {
        "type": "minecraft:crafting_shaped",
        "category": "building",
        "pattern": ["CKC", "SSS"],
        "key": {
            "C": {"item": "minecraft:copper_ingot"},
            "K": {"item": "minecraft:clay_ball"},
            "S": {"item": "minecraft:smooth_stone"},
        },
        "result": {"id": f"{MOD}:slide_channel", "count": 16},
    })
    for color in DYES:
        write_json(RES / f"data/{MOD}/recipe/{color}_slide_channel.json", {
            "type": "minecraft:crafting_shaped",
            "category": "building",
            "group": "slide_channel_dye",
            "pattern": ["###", "#D#", "###"],
            "key": {
                "#": {"tag": f"{MOD}:slide_channels"},
                "D": {"item": f"minecraft:{color}_dye"},
            },
            "result": {"id": f"{MOD}:{color}_slide_channel", "count": 8},
        })

    # lang (merge, preserve existing keys)
    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"block.{MOD}.slide_channel"] = "Slide Channel"
    for color in DYES:
        pretty = " ".join(w.capitalize() for w in color.split("_"))
        lang[f"block.{MOD}.{color}_slide_channel"] = f"{pretty} Slide Channel"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_textures()
    gen_data()
    print(f"Generated slide channel assets for {len(ALL_NAMES)} colors under {RES}")
