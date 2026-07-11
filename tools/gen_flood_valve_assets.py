#!/usr/bin/env python3
"""Generate Flood Valve assets: iron valve body with a copper wheel face,
directional blockstate (facing x powered), loot, recipe, tags + lang merge."""
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


# horizontal pipe cylinder: light band along the top, rolling darker to the bottom
_PIPE_ROWS = (5, 6, 7, 6, 6, 5, 5, 4, 4, 3, 3, 3, 2, 2, 1, 1)


def gen_textures():
    tex = RES / f"assets/{MOD}/textures/block"
    tex.mkdir(parents=True, exist_ok=True)

    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            side.putpixel((x, y), T.R(T.IRON, _PIPE_ROWS[y]))
    for band_y in (3, 10):  # bolted collar bands
        for x in range(16):
            side.putpixel((x, band_y), T.R(T.IRON, _PIPE_ROWS[band_y] + 1))
            side.putpixel((x, band_y + 1), T.R(T.IRON, 2))
            side.putpixel((x, band_y + 2), T.R(T.IRON, 0))
        for x in (2, 7, 12):
            side.putpixel((x, band_y + 1), T.R(T.COPPER, 5))
            side.putpixel((x, band_y + 2), T.R(T.COPPER, 2))
    side.save(tex / "flood_valve_side.png")

    for name, open_valve in (("flood_valve_front", False), ("flood_valve_front_open", True)):
        img = Image.new("RGBA", (16, 16))
        T.plate(img, 0, 0, 15, 15, T.IRON, streak=False)
        T.rivet(img, 1, 1, T.IRON)
        T.rivet(img, 13, 1, T.IRON)
        T.rivet(img, 1, 13, T.IRON)
        T.rivet(img, 13, 13, T.IRON)
        # copper handwheel: rim highlighted toward the light, four spokes, glowing hub when open
        for y in range(16):
            for x in range(16):
                dx, dy = x - 7.5, y - 7.5
                r = (dx * dx + dy * dy) ** 0.5
                if 4.6 <= r <= 6.2:
                    img.putpixel((x, y), T.R(T.COPPER, 6 if dx + dy < -3 else (2 if dx + dy > 3 else 4)))
                elif r < 4.6 and (abs(dx) < 1.0 or abs(dy) < 1.0):
                    img.putpixel((x, y), T.R(T.COPPER, 2))
        hub = T.GLOW if open_valve else T.DARK
        for y in range(6, 10):
            for x in range(6, 10):
                d = max(abs(x - 7.5), abs(y - 7.5))
                img.putpixel((x, y), T.R(hub, 4 if d < 1 else 2))
        img.save(tex / f"{name}.png")


def gen_data():
    models = RES / f"assets/{MOD}/models"
    for suffix, front in (("", "flood_valve_front"), ("_open", "flood_valve_front_open")):
        write_json(models / f"block/flood_valve{suffix}.json", {
            "parent": "minecraft:block/cube",
            "textures": {
                "particle": f"{MOD}:block/flood_valve_side",
                "north": f"{MOD}:block/{front}",
                "south": f"{MOD}:block/flood_valve_side",
                "east": f"{MOD}:block/flood_valve_side",
                "west": f"{MOD}:block/flood_valve_side",
                "up": f"{MOD}:block/flood_valve_side",
                "down": f"{MOD}:block/flood_valve_side",
            },
        })
    write_json(models / "item/flood_valve.json", {"parent": f"{MOD}:block/flood_valve"})

    rot = {
        "north": {}, "south": {"y": 180}, "east": {"y": 90}, "west": {"y": 270},
        "up": {"x": 270}, "down": {"x": 90},
    }
    variants = {}
    for facing, r in rot.items():
        for powered, suffix in (("false", ""), ("true", "_open")):
            variants[f"facing={facing},powered={powered}"] = \
                {"model": f"{MOD}:block/flood_valve{suffix}", **r}
    write_json(RES / f"assets/{MOD}/blockstates/flood_valve.json", {"variants": variants})

    write_json(RES / f"data/{MOD}/loot_table/blocks/flood_valve.json", {
        "type": "minecraft:block",
        "random_sequence": f"{MOD}:blocks/flood_valve",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"{MOD}:flood_valve"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

    write_json(RES / f"data/{MOD}/recipe/flood_valve.json", {
        "type": "minecraft:crafting_shaped",
        "category": "redstone",
        "pattern": ["ICI", "CBC", "IRI"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "C": {"item": "minecraft:copper_ingot"},
            "B": {"item": "minecraft:bucket"},
            "R": {"item": "minecraft:redstone"},
        },
        "result": {"id": f"{MOD}:flood_valve", "count": 1},
    })

    merge_tag(RES / "data/minecraft/tags/block/mineable/pickaxe.json", [f"{MOD}:flood_valve"])

    gen_lang()


def gen_lang():
    """Merge every Flood Valve string (block name, tooltip, right-click status) — keeps
    them reproducible instead of hand-edited into en_us.json."""
    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang.update({
        f"block.{MOD}.flood_valve": "Flood Valve",
        # item tooltip
        f"tooltip.{MOD}.flood_valve.what": "Floods a sealed space it faces with water.",
        f"tooltip.{MOD}.flood_valve.how": "Redstone ON fills · OFF drains. Needs RF.",
        f"tooltip.{MOD}.flood_valve.leaks": "Not watertight? It points at the leak.",
        # right-click status readout
        f"message.{MOD}.flood_valve.status": "Flood Valve",
        f"message.{MOD}.flood_valve.mode_fill": "Mode: filling (redstone on)",
        f"message.{MOD}.flood_valve.mode_drain": "Mode: draining (no redstone)",
        f"message.{MOD}.flood_valve.energy": "Energy: %s / %s RF",
        f"message.{MOD}.flood_valve.volume": "Sealed volume: %s blocks",
        f"message.{MOD}.flood_valve.no_target": "⚠ Facing a solid block — aim the wheel at the open space to fill",
        f"message.{MOD}.flood_valve.leak_at": "⚠ Leak at %s, %s, %s — not watertight",
        f"message.{MOD}.flood_valve.no_rf": "Out of RF — connect a Pump House or RF source",
        f"message.{MOD}.flood_valve.placed": "Give it RF + a redstone signal to flood the space it faces",
        # legacy actionbar leak ping (kept)
        f"message.{MOD}.flood_valve_leak": "Flood Valve: volume not sealed — leak near %s, %s, %s",
    })
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen_textures()
    gen_data()
    print(f"Generated flood valve assets under {RES}")
