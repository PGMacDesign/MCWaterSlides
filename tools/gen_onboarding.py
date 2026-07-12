#!/usr/bin/env python3
"""Generate the no-book onboarding layer: item tooltip strings (rendered by
ClientTooltips from tip.mcwaterslides.* lang keys — dyed variants share their
family's tip) and the splash-landing advancement that closes the guided chain."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"

TIPS = {
    "slide_channel": (
        "Ride it! Step in and slopes carry you — 16 dye colors, cheap by design.",
        "One craft makes 16. Place along a run to extend it; lanes placed side-by-side "
        "merge into wide slides. Crouch to brake, jump to bail."),
    "slide_tube": (
        "An enclosed slide — build loops, long drops, and vertical shafts.",
        "Stack tubes into a column for a drop shaft. Once inside you ride to the exit. "
        "The glass-ring recipe makes see-through tubes."),
    "jet": (
        "Pushes riders and water the way it faces. Needs RF and a wet nozzle.",
        "Energizes ~24 blocks of current — even uphill. Touching jets share RF down the "
        "row, so wire just one. Redstone toggles it."),
    "pump_house": (
        "Burns coal into RF for jets and valves. Water beside it trickles power for free.",
        "Wire it with Water Conduits or any RF mod's cables. One Pump House feeds a whole "
        "daisy-chained jet row."),
    "water_conduit": (
        "Carries RF along your slide's spine.", None),
    "flood_valve": (
        "Redstone ON floods the sealed space it faces — glass tunnels become water rides.",
        "Right-click any time to read its status. If the space leaks, smoke rises at the "
        "hole and the spot lands in your action bar."),
    "splash_pool": (
        "The soft landing — catches riders safely at ride's end.",
        "Place pools side-by-side and they merge into one big pool. Mobs and dropped "
        "items get caught too."),
}


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def gen():
    # Stick the Landing — teaches that pools end rides safely; fires from RideTicker's
    # splash-pool catch (ride_stat "splash_land").
    write_json(RES / f"data/{MOD}/advancement/stick_the_landing.json", {
        "parent": f"{MOD}:lazy_river",
        "display": {
            "icon": {"id": f"{MOD}:splash_pool"},
            "title": {"translate": f"advancement.{MOD}.stick_the_landing.title"},
            "description": {"translate": f"advancement.{MOD}.stick_the_landing.description"},
            "frame": "task",
            "show_toast": True,
            "announce_to_chat": True,
            "hidden": False,
        },
        "criteria": {"splash_landed": {
            "trigger": f"{MOD}:ride_stat",
            "conditions": {"stat": "splash_land", "min": 1},
        }},
    })

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    for key, (tip, more) in TIPS.items():
        lang[f"tip.{MOD}.{key}"] = tip
        if more:
            lang[f"tip.{MOD}.{key}.more"] = more
        else:
            lang.pop(f"tip.{MOD}.{key}.more", None)
    lang[f"tip.{MOD}.hold_shift"] = "Hold Shift for more"
    lang[f"advancement.{MOD}.stick_the_landing.title"] = "Stick the Landing"
    lang[f"advancement.{MOD}.stick_the_landing.description"] = "End a ride in a Splash Pool"
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen()
    print(f"Generated onboarding assets under {RES}")
