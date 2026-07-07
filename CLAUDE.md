# MCWaterSlides — Project Instructions

**Multi-version NeoForge** mod — water slides (concept/design doc pending; update this
line when the design lands). This repo deliberately mirrors **PGMacDesign/MC3DPrint**
(local: `~/_Development/_MiscWorkspace/mc3dprint`, post-multi-version-merge `main`) in
structure, build setup, and conventions — when a question isn't answered here, do what
MC3DPrint does.

- **Stack:** Java 21, NeoForge, official Mojang mappings, Gradle + ModDevGradle
  (`net.neoforged.moddev`) + **Stonecutter** (`dev.kikugie.stonecutter`, Groovy
  controller). mod id `mcwaterslides`, package root `com.pgmacdesign.mcwaterslides`,
  MIT, solo (PGMacDesign).
- **Multi-version from day one:** `main` builds one jar per NeoForge node from a single
  tree — currently **1.21.1 · 1.21.8 · 1.21.9 · 1.21.10 · 1.21.11 · 26.1 · 26.2**
  (mirror MC3DPrint's node list; extend both as new MC versions ship). **1.21.1 is the
  base/vcs node** (plain code = 1.21.1; other nodes via guards/replacements). Per-node
  versions (`minecraft_version`, `neo_version`, `jei_version`, …) live in
  `versions/<node>/gradle.properties`; only shared metadata stays in the root file.
  The Gradle daemon runs on a **Java 21 launcher JVM** (`gradle/gradle-daemon-jvm.properties`,
  foojay-provisioned — Stonecutter needs 21 as launcher, not just toolchain); the 26.x
  nodes compile on a **Java 25 toolchain** Gradle provisions itself.
- **Public repo:** no secrets/PII, original content only. `.env` is gitignored
  (`.env.example` is the committed template; CI secrets live in GitHub repo settings, not
  files). **Nothing that points at private systems or the owner goes in committed files** —
  no Linear links or ticket ids, no personal info (emails), no internal infra ids beyond
  what a deploy file genuinely needs. Tracker/issue references stay in Linear and local
  `~/.claude` memory, not the repo.

## Build · Test · Deploy

Tasks are **node-scoped** (each Stonecutter node is a subproject):

```bash
./gradlew :1.21.1:compileJava -q        # fast compile check (any node)
./gradlew :1.21.1:test                  # JUnit (src/test)
./gradlew :1.21.1:assemble -x test      # build jar → versions/<node>/build/libs/
./gradlew :1.21.1:runGameTestServer     # in-world GameTests (gametest/) — 1.21.1 is the oracle
./scripts/build-all.sh [--version X.Y.Z]  # every shippable jar per node → dist/
```

**Stonecutter working rules** (MC3DPrint guard lore — it bites):

- Edit at active node `1.21.1`; version-variant code lives in
  `//? if >=<ver> {/*…*///?} else {…//?}` guards. After writing new guards, run
  `"Set active project to <node>"` to re-toggle before compiling; **reset active →
  `1.21.1` before every commit.**
- Replacement pairs (`build.gradle` stonecutter block) must be **single-hop** — an API
  that moves TWICE across versions needs `if/elif` guard chains (version-range
  replacement conditions don't fire). Each pair must be idempotent and uniquely
  reversible.
- Never hand-nest block-comment guards inside an already-commented region (hoist a
  class-level helper with a sibling chain); never start a guard block with bare `//`
  lines.
- Pin the `test` task's `workingDir` to the root project — node subprojects default to
  `versions/<node>/`, breaking root-relative resource paths.

Rules that bite if skipped (learned on MC3DPrint):

1. **Deploy = replace, never duplicate.** Copy the fresh jar into the test instance's
   `mods/` folder, replacing any existing `mcwaterslides-*.jar` — never leave two.
2. **GameTest namespace:** `neoforge.enabledGameTestNamespaces` must be set on the
   client, server AND gameTestServer runs in `build.gradle` — if unset, ZERO gametests
   register and `runGameTestServer` exits 0 having run nothing (a false green).
3. **`:NODE:test` green ≠ runtime-correct** — also run `runGameTestServer` to catch
   registration/NBT bugs the compiler can't. GameTests run on the base node; forward
   nodes get compile + boot-smoke until gametest seams are worth porting.

Releases: publish a GitHub Release → `.github/workflows/release.yml` derives the version
from the tag, runs `scripts/build-all.sh`, and attaches one
`mcwaterslides-<ver>-neoforge-<node>.jar` per node. CI is the builder; local release
builds are for smoke-testing only. Extend the script's node array when adding a version.

## Architecture (`src/main/java/com/pgmacdesign/mcwaterslides/`)

Not scaffolded yet — replace this section with a package table as code lands. Follow
MC3DPrint's layout: `McWaterSlides.java` mod entry (registries + event-listener wiring),
one package per feature domain, plus `registry/`, `item/`, `block/`, `client/`,
`command/`, `config/`, `network/`, `loot/`, `advancement/` as named, and
`integration/<mod>/` for soft-dep compat.

**Soft-dep compat pattern** (when valuing/handling other mods' content): an
`onCommonSetup` hook that **returns early unless `ModList.get().isLoaded("<id>")`**, then
`event.enqueueWork(...)`; pure `ResourceLocation` strings, **no gradle dependency** — so
it's invisible (no crash/warn/config) when the mod is absent.

## Conventions

- **Git:** commit → push every change, direct to `main` (solo repo). **Never** add
  `Co-Authored-By: Claude` or "Generated with Claude Code" to commits/PRs. Commit style:
  conventional prefix + scope, terse subject — `feat(block): …`, `fix(gui): …`.
- **Two doc surfaces — keep both in sync with code** (once they exist): a player-facing
  change means updating BOTH the in-game Patchouli guide (soft/optional dep) AND the
  website guide. They mirror each other and silently drift — verify claims against the
  Java before writing.
- **Skills:** repo-specific skills live in `.claude/skills/` (committed), prefixed
  `mcws-` (MC3DPrint uses `mc3dp-`). Create via `/skill-creator`, with trigger evals.
- **Generated assets:** if textures/GUIs are script-generated, generators live in
  `tools/*.py` (PIL), reproducible, with coords kept in lockstep with the Screen/Menu
  classes they draw for.
- **Gameplay invariants:** once mechanics exist, record their economy/anti-exploit
  invariants in a dedicated section here (MC3DPrint's "FU economy invariants" is the
  model) and pin them with tests.

## Where deeper context lives

`docs/ROADMAP.md` (state + next-up), design docs under `docs/`, `CHANGELOG.md`, and the
project memory — index in `MEMORY.md` under
`~/.claude/projects/<this-project>/memory/`.
