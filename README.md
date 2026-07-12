# MC Waterslides

[![License: MIT](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

**[Website](https://pgmacdesign.github.io/MCWaterSlides/)** · **[Guide](https://pgmacdesign.github.io/MCWaterSlides/guide.html)**

**Build enormous waterslides — and ride them fast.** A multi-version NeoForge mod
where RF-powered jets push riders downhill, **uphill**, and through enclosed tubes
of any size. Slides can loop, climb, and go on effectively forever.

**Fun-first, zero grind.** Everything crafts from copper, clay, and iron-tier
materials — no diamonds, no netherite, nothing rare. An iron pickaxe and a
riverbank fund a full water park.

## How it works

1. Lay **Slide Channels** — rail-style auto-connecting flumes, dyeable in 16
   colors, always full of water. Lanes placed side-by-side merge into wide slides.
2. Place **Jets** — each one projects a current through the water ahead of it
   (any water, including a glass tunnel you built yourself). Aim one straight up
   to push riders uphill. Jets touching each other share RF down the row.
3. Power them — the coal/water-fed **Pump House** ships in the mod, or use any
   RF mod's generators.
4. Hop in. Crouch to brake, jump to bail — or ride an enclosed **Slide Tube**
   (loops, long drops, vertical shafts) and commit to the exit.

And the rest: the **Flood Valve** floods your sealed glass build into a rideable
water tunnel (and points at the leak when it isn't sealed), **Splash Pools** catch
riders safely, **mobs and dropped items ride too**, every rider gets a water-rush
soundtrack with a proper splash-down, a guided advancement chain teaches the loop —
and a hidden advancement waits for a single 10,000-block ride.

📖 In-game manual via [Patchouli](https://modrinth.com/mod/patchouli) (optional,
recommended) — or read the same guide [on the web](https://pgmacdesign.github.io/MCWaterSlides/guide.html).

## Screenshots

> 🖼️ Coming with the showcase park world — watch this space.

## Big parks & chunk loading — a deliberate design decision

MC Waterslides ships **no chunk loading, on purpose** — that's a commitment, not an
oversight. Riders load chunks themselves the way any moving player does, so a park
can span a hundred chunks and only the parts near players ever tick.

Mobs and dropped items only ride while a player is close enough to keep their chunks
ticking: leave, and they pause mid-slide; return, and they resume. **That's intended
behavior.** Slides are not designed as an unattended item-transit system — though if
you want one, they compose fine with chunk-loader mods (currents run in any chunk
something else keeps loaded).

## Versions

**Shipping now: NeoForge 1.21.1.** The codebase is multi-version (Stonecutter,
one tree) and the rest of the ladder — 1.21.8 · 1.21.9 · 1.21.10 · 1.21.11 ·
26.1 · 26.2 — lands as point releases as each port is finished.

## Build from source

```bash
./gradlew :1.21.1:compileJava -q        # fast compile check
./gradlew :1.21.1:test                  # JUnit
./gradlew :1.21.1:assemble -x test      # jar → versions/1.21.1/build/libs/
./gradlew :1.21.1:runGameTestServer     # in-world GameTests
./scripts/build-all.sh                  # every node's jar → dist/
```

Design docs live in [`docs/`](docs/): mechanics, blocks & recipes, architecture.

## License

MIT © PGMacDesign. Free to use, modify, and share.
