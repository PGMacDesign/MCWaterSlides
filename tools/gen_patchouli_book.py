#!/usr/bin/env python3
"""Generate the Park Builder's Manual (Patchouli book, soft dep — pure JSON).

Short, fun, how-to only. Claims are written against the shipped Java behavior:
verify before editing (MC3DPrint's doc-drift lesson).
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
MOD = "mcwaterslides"
BOOK = RES / f"assets/{MOD}/patchouli_books/guide"
# Patchouli REGISTERS the book from data/ (so the item resolves & opens); it loads the
# CONTENT from assets/ when use_resource_pack is true. book.json must live in BOTH — the
# data/ copy is what was missing (book content loaded, but the book itself never registered).
BOOK_DATA = RES / f"data/{MOD}/patchouli_books/guide"


def write_json(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def text(t, title=None):
    page = {"type": "patchouli:text", "text": t}
    if title:
        page["title"] = title
    return page


def crafting(recipe, t=None):
    page = {"type": "patchouli:crafting", "recipe": recipe}
    if t:
        page["text"] = t
    return page


def spotlight(item, t):
    return {"type": "patchouli:spotlight", "item": item, "text": t}


def entry(category, name, icon, pages, priority=False):
    data = {
        "name": name,
        "icon": icon,
        "category": f"{MOD}:{category}",
        "pages": pages,
    }
    if priority:
        data["priority"] = True
    return data


def gen():
    book_json = {
        "name": f"book.{MOD}.guide.name",
        "landing_text": f"book.{MOD}.guide.landing",
        "version": 1,
        "subtitle": "Build it. Ride it.",
        # No creative_tab: ModCreativeTabs inserts the book stack into our tab
        # explicitly (Patchouli's modded-tab auto-insert is unreliable). Leaving this
        # set too would double-list it.
        "model": "patchouli:book_blue",
        "book_texture": "patchouli:textures/gui/book_blue.png",
        "use_resource_pack": True,
        "show_progress": False,
        "index_icon": f"{MOD}:slide_channel",
        "dont_generate_book": False,
    }
    write_json(BOOK / "book.json", book_json)       # assets/ — content loading
    write_json(BOOK_DATA / "book.json", book_json)  # data/ — book registration (the fix)

    cats = BOOK / "en_us/categories"
    write_json(cats / "building.json", {
        "name": "Building Slides",
        "description": "Channels, tubes, and glass megastructures.",
        "icon": f"{MOD}:slide_channel",
        "sortnum": 0,
    })
    write_json(cats / "power.json", {
        "name": "Jets & Power",
        "description": "Making the water push back.",
        "icon": f"{MOD}:jet",
        "sortnum": 1,
    })
    write_json(cats / "safety.json", {
        "name": "Landings & Safety",
        "description": "Every good ride ends in a splash.",
        "icon": f"{MOD}:splash_pool",
        "sortnum": 2,
    })

    entries = BOOK / "en_us/entries"

    write_json(entries / "building/first_slide.json", entry("building", "Your First Slide",
            f"{MOD}:slide_channel", [
        text("$(item)Slide Channels$() snap together like rails — straights, corners, and "
             "slopes sort themselves out as you place them. No supports needed; slides "
             "float wherever you build them.$(br2)Lay a run downhill and hop in: "
             "$(thing)gravity alone$() gets you sliding."),
        crafting(f"{MOD}:slide_channel",
                 "Cheap on purpose. One craft paves 16 blocks of slide — build BIG."),
        text("$(bold)Extending a run:$() just aim at the last channel and click — the next "
             "one drops in ahead of you, climbing over a step if it must.$(br2)$(bold)Going "
             "wide:$() lay a second run right alongside the first and the wall between them "
             "vanishes — two lanes become one broad slide you can drift across.", title="Building fast"),
        text("While riding:$(br)$(li)$(thing)Crouch$() to brake$(li)$(thing)Jump$() to bail "
             "out (not inside a tube!)$(li)Downhill speeds you up, uphill slows you down — "
             "unless a Jet pays for the climb.$(br2)You take $(bold)no fall damage$() while "
             "riding a slide."),
        text("Recolor channels like concrete: surround a dye with eight of them. "
             "Sixteen colors, one water park.", title="Colors"),
    ], priority=True))

    write_json(entries / "building/tubes.json", entry("building", "Slide Tubes",
            f"{MOD}:slide_tube", [
        text("$(item)Slide Tubes$() are enclosed channels: you slip in from an open "
             "section and ride the bore — the view is out the window strip. Inside a tube "
             "you're $(bold)committed$(): no bailing until an opening.$(br2)Stack tubes "
             "vertically for $(thing)drop shafts$(). Drops bank speed — you exit the "
             "bottom moving FAST."),
        crafting(f"{MOD}:slide_tube", "Four channels, four glass, four tubes."),
        text("A single tube is a snug crawl-bore — perfect and fast, but cosy. Want "
             "headroom? $(bold)Stack a second tube directly on top$() and the pair merges "
             "into a full standing-height bore.$(br2)You never drown while riding — any "
             "tube, flooded or not.", title="Tall tubes"),
    ]))

    write_json(entries / "building/freeform.json", entry("building", "Glass Megastructures",
            f"{MOD}:flood_valve", [
        text("Jets push riders through $(bold)any water$(), not just slide blocks. Build a "
             "watertight glass tube of any size — 2x2, 4x4, a whale — flood it, line it "
             "with Jets, and it's a slide.$(br2)Filling by bucket is misery. Use a "
             "$(item)Flood Valve$()."),
        crafting(f"{MOD}:flood_valve",
                 "Give it redstone and RF: signal ON fills the sealed space it faces; "
                 "signal OFF drains it."),
        text("If your build isn't watertight, the valve refuses and $(bold)points at the "
             "leak$() — smoke rises at the hole and the coordinates land in your action "
             "bar. Patch it and flip the lever again.$(br2)$(bold)Right-click the valve$() "
             "any time to read its mode, power, sealed volume, or leak spot.", title="Finding leaks"),
    ]))

    write_json(entries / "building/mega_parks.json", entry("building", "Mega-Parks",
            "minecraft:filled_map", [
        text("Build as big as you dare — a park can span a hundred chunks and more. "
             "$(bold)You$() load the world as you ride: the slide ahead is always ready "
             "before you arrive, no chunk loaders needed."),
        text("Mobs and items only ride while a player is near enough to keep the world "
             "ticking. Wander off and your villager pauses mid-slide; come back and the "
             "ride picks up again. $(bold)That's by design$() — slides are for riding, "
             "not unattended freight.$(br2)Want a cross-map item line anyway? Pair the "
             "slides with a chunk-loading mod: currents run fine in any chunk something "
             "else keeps loaded.", title="Mobs, items & distance"),
    ]))

    write_json(entries / "power/jets.json", entry("power", "The Jet",
            f"{MOD}:jet", [
        text("The $(item)Jet$() shoves a current through the water ahead of it — about 24 "
             "blocks. Riders in the current accelerate hard.$(br2)$(bold)Aim the nozzle "
             "into the water.$() A jet mounted above the waterline pushes nothing."),
        crafting(f"{MOD}:jet",
                 "Spacing is your speed dial: sparse jets = lazy river, dense jets = "
                 "screamer. A redstone signal switches a jet off."),
        text("Jets aim all six ways — $(bold)straight up included$(). An up-jet under a "
             "water column or tube shaft launches riders skyward, and a climb costs "
             "exactly what a drop pays, so a well-jetted slide can run uphill... or "
             "forever.", title="Uphill & geysers"),
        text("Don't want jets on show? $(bold)Tuck one under the floor or against a side "
             "wall$() of a slide and it still pushes along the run — the current finds the "
             "water it's touching. Hide the whole engine and keep the ride looking clean.",
             title="Hidden jets"),
    ], priority=True))

    write_json(entries / "power/pump_house.json", entry("power", "Pump House & Conduits",
            f"{MOD}:pump_house", [
        text("Jets and valves drink $(thing)RF$(). No tech mods? The $(item)Pump House$() "
             "has you covered: feed it furnace fuel (one coal runs a long while), or just "
             "park it beside water for a slow trickle.$(br2)It glows while burning. "
             "Right-click for the gauge."),
        crafting(f"{MOD}:pump_house", None),
        crafting(f"{MOD}:water_conduit",
                 "Water Conduits carry the power along your slide's spine. Any other "
                 "RF mod's cables work too."),
        text("$(bold)Jets share power.$() Jets touching each other pass RF toward whichever "
             "neighbor holds less, hop by hop — wire $(bold)one$() jet in a row and the whole "
             "chain feeds itself. Flip a jet off with redstone to cut the chain there.",
             title="Daisy-chaining"),
    ]))

    write_json(entries / "safety/splash_pools.json", entry("safety", "Splash Pools",
            f"{MOD}:splash_pool", [
        text("End every ride in a $(item)Splash Pool$(): it swallows your speed in about a "
             "block and cancels $(bold)all$() landing damage — even a full-speed launch "
             "off an open ramp end.$(br2)Pools tile: place a grid and the inner walls "
             "disappear."),
        crafting(f"{MOD}:splash_pool", "Mobs and dropped items get caught too."),
    ]))

    # Conditional recipe: book + channel -> guide book (only when Patchouli is present).
    write_json(RES / f"data/{MOD}/recipe/guide_book.json", {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "patchouli"}],
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": "minecraft:book"}, {"tag": f"{MOD}:slide_channels"}],
        "result": {
            "id": "patchouli:guide_book",
            "components": {"patchouli:book": f"{MOD}:guide"},
        },
    })

    lang_path = RES / f"assets/{MOD}/lang/en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    lang[f"book.{MOD}.guide.name"] = "Park Builder's Manual"
    lang[f"book.{MOD}.guide.landing"] = ("Welcome to $(item)MC Waterslides$()! Everything here "
                                         "is cheap, fast, and wet. Build enormous slides, push "
                                         "them uphill with jets, and always land in a pool.")
    write_json(lang_path, dict(sorted(lang.items())))


if __name__ == "__main__":
    gen()
    print(f"Generated Park Builder's Manual under {BOOK}")
