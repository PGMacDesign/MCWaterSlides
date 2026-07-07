# MCWaterSlides — Project Instructions

**NeoForge 1.21.1** mod — water slides (concept/design doc pending; update this line when
the design lands). This repo deliberately mirrors **PGMacDesign/MC3DPrint** (local:
`~/_Development/_MiscWorkspace/mc3dprint`) in structure, build setup, and conventions —
when a question isn't answered here, do what MC3DPrint does.

- **Stack:** Java 21, NeoForge 1.21.1, official Mojang mappings, Gradle + ModDevGradle
  (`net.neoforged.moddev`). mod id `mcwaterslides`, package root
  `com.pgmacdesign.mcwaterslides`, MIT, solo (PGMacDesign). Single-target `main` — no
  Stonecutter/multi-version until the mod stabilizes (MC3DPrint added it late, on a branch).
- **Public repo:** no secrets/PII, original content only. `.env` is gitignored
  (`.env.example` is the committed template; CI secrets live in GitHub repo settings, not
  files). **Nothing that points at private systems or the owner goes in committed files** —
  no Linear links or ticket ids, no personal info (emails), no internal infra ids beyond
  what a deploy file genuinely needs. Tracker/issue references stay in Linear and local
  `~/.claude` memory, not the repo.

## Build · Test · Deploy

```bash
./gradlew compileJava -q        # fast compile check
./gradlew test                  # JUnit (src/test)
./gradlew build                 # full build + tests → build/libs/mcwaterslides-<ver>.jar
./gradlew runGameTestServer     # in-world GameTests (gametest/)
```

Rules that bite if skipped (learned on MC3DPrint):

1. **Deploy = replace, never duplicate.** Copy the fresh jar into the test instance's
   `mods/` folder, replacing any existing `mcwaterslides-*.jar` — never leave two.
2. **GameTest namespace:** `neoforge.enabledGameTestNamespaces` must be set on the
   client, server AND gameTestServer runs in `build.gradle` — if unset, ZERO gametests
   register and `runGameTestServer` exits 0 having run nothing (a false green).
3. **`test` green ≠ runtime-correct** — also run `runGameTestServer` to catch
   registration/NBT bugs the compiler can't.

Releases: publish a GitHub Release → `.github/workflows/release.yml` builds the jar(s)
with the tag's version and attaches them to the release. The workflow is the builder;
local release builds are for smoke-testing only.

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
