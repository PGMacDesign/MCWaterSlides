# MC Waterslides

[![License: MIT](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

**[Website](https://pgmacdesign.github.io/MCWaterSlides/)** · **[Guide](https://pgmacdesign.github.io/MCWaterSlides/guide.html)**

**Build enormous waterslides — and ride them fast.** A multi-version NeoForge mod
where RF-powered jets push riders downhill, **uphill**, and through enclosed tubes
of any size. Slides can loop, climb, and go on effectively forever.

**Fun-first, zero grind.** Everything crafts from copper, clay, and iron-tier
materials — no diamonds, no netherite, nothing rare. An iron pickaxe and a
riverbank fund a full water park.

> 🚧 **In development** — v0.1.0 is being built. This README grows with the mod.

## How it will work

1. Lay **Slide Channels** — rail-style auto-connecting U-channels, dyeable in
   16 colors, always full of water.
2. Place **Jets** — each one projects a current through the water ahead of it
   (any water, including a glass tube you built yourself). Aim one straight up
   to push riders uphill.
3. Power them with RF — the coal/water-fed **Pump House** ships in the mod, or
   use any RF mod's generators.
4. Hop in. Crouch to brake, jump to bail — or ride an enclosed tube and commit.

Also planned for v1: **Slide Tubes** (enclosed, vertical drops), the **Flood
Valve** (fills your sealed glass tube with water — and finds the leak when it
isn't sealed), **Splash Pools** (safe landings), mob & item riding, and a hidden
advancement for a single 10,000-block ride.

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

One codebase (Stonecutter) builds a jar per NeoForge target:
**1.21.1 · 1.21.8 · 1.21.9 · 1.21.10 · 1.21.11 · 26.1 · 26.2**

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
