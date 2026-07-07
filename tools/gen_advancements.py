#!/usr/bin/env python3
"""Generate the six v1 advancement JSONs + lang entries."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"
ADV = RES / f"data/{MOD}/advancement"


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def display(key, icon, frame="task", hidden=False, background=None):
    d = {
        "icon": {"id": icon},
        "title": {"translate": f"advancement.{MOD}.{key}.title"},
        "description": {"translate": f"advancement.{MOD}.{key}.description"},
        "frame": frame,
        "show_toast": True,
        "announce_to_chat": True,
        "hidden": hidden,
    }
    if background:
        d["background"] = background
    return d


def ride_stat(stat, minimum):
    return {"trigger": f"{MOD}:ride_stat", "conditions": {"stat": stat, "min": minimum}}


def gen():
    write_json(ADV / "making_a_splash.json", {
        "display": display("making_a_splash", f"{MOD}:slide_channel",
                           background="minecraft:textures/gui/advancements/backgrounds/husbandry.png"),
        "criteria": {"placed_channel": {
            "trigger": "minecraft:placed_block",
            "conditions": {"location": [{
                "condition": "minecraft:location_check",
                "predicate": {"block": {"blocks": f"#{MOD}:slide_channels"}},
            }]},
        }},
    })
    write_json(ADV / "hydraulic_engineer.json", {
        "parent": f"{MOD}:making_a_splash",
        "display": display("hydraulic_engineer", f"{MOD}:jet"),
        "criteria": {"jet_energized": ride_stat("jet_energized", 1)},
    })
    write_json(ADV / "lazy_river.json", {
        "parent": f"{MOD}:hydraulic_engineer",
        "display": display("lazy_river", "minecraft:water_bucket"),
        "criteria": {"rode_100": ride_stat("distance", 100)},
    })
    write_json(ADV / "screamer.json", {
        "parent": f"{MOD}:lazy_river",
        "display": display("screamer", f"{MOD}:red_slide_channel", frame="goal"),
        "criteria": {"hit_cap": ride_stat("speed", 1)},  # speed only fires at cap
    })
    write_json(ADV / "tube_rat.json", {
        "parent": f"{MOD}:lazy_river",
        "display": display("tube_rat", f"{MOD}:slide_tube", frame="goal"),
        "criteria": {"enclosed_50": ride_stat("enclosed_distance", 50)},
    })
    write_json(ADV / "around_the_world.json", {
        "parent": f"{MOD}:screamer",
        "display": display("around_the_world", "minecraft:heart_of_the_sea",
                           frame="challenge", hidden=True),
        "criteria": {"rode_10000": ride_stat("distance", 10000)},
    })

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang.update({
        f"advancement.{MOD}.making_a_splash.title": "Making a Splash",
        f"advancement.{MOD}.making_a_splash.description": "Place your first Slide Channel",
        f"advancement.{MOD}.hydraulic_engineer.title": "Hydraulic Engineer",
        f"advancement.{MOD}.hydraulic_engineer.description": "Power up a Jet",
        f"advancement.{MOD}.lazy_river.title": "Lazy River",
        f"advancement.{MOD}.lazy_river.description": "Ride a slide for 100 blocks in one go",
        f"advancement.{MOD}.screamer.title": "Screamer",
        f"advancement.{MOD}.screamer.description": "Hit top speed on a slide",
        f"advancement.{MOD}.tube_rat.title": "Tube Rat",
        f"advancement.{MOD}.tube_rat.description": "Ride 50 blocks fully enclosed",
        f"advancement.{MOD}.around_the_world.title": "Around the World",
        f"advancement.{MOD}.around_the_world.description": "Ride a single slide for 10,000 blocks",
    })
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen()
    print(f"Generated advancements under {ADV}")
