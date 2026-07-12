# MC Waterslides — Roadmap

State + next-up. Full v1 design: `design-mechanics-spec.html`, `blocks-recipes-spec.html`,
`architecture-delivery-plan.html` (planning snapshots — where code and spec disagree, code
and its tests are current truth).

## State (2026-07-07)

**M0–M4 complete + most of M5** on the 1.21.1 base node. The full v1 feature set is
implemented and verified: **33 GameTests + 9 JUnit green**, dev client boots with zero
asset errors, mod loads clean with no optional deps installed.

Shipped: Slide Channels (17 colors, rail-style auto-connect) · Slide Tubes (+ VERTICAL
shafts) · the Jet (cached slope-following current fields, RF-gated, client-predicted) ·
momentum engine (server state-of-record + local-player prediction; no server-side player
motion writes = no rubber-banding) · Pump House (160k RF/coal — MC3DPrint Clock Generator
parity at 2× rate) · Water Conduit (network flood) · Flood Valve (bounded fill/drain,
leak reporting) · Splash Pool · mob/item riding + toggles · feel-pass FX v1 · six
advancements (hidden "Around the World" at 10,000 blocks) · Park Builder's Manual
(Patchouli, soft dep).

**Tristan-request pass (2026-07-08 → scrapped 2026-07-11):** Inner Tube (boat-like raft)
and the Tornado Funnel (drain-bowl v1, then a side-lying cone v2 with true-pendulum physics)
were built, playtested across several rounds, and **removed by owner decision** — the funnel
never felt right in-game and the tube's look never landed. Everything lives in git history
(`5c24807`..`f97c475`) if either idea returns. Two keepers survived the pass: **swing
physics** (a rider that stalls climbing swings back down — any U-valley oscillates and
settles; `RideState.settleTicks` guards termination) and the **ride engine generalized to
`Entity`** (groundwork for future rideables). Lesson for a funnel v3, if ever: exits must be
purely geometric (position-gated exits ate the oscillation; velocity-reflecting containment
ate the swing energy — pin position at a rim line instead), and stateless per-tick physics
loses energy anywhere velocity is shaved.

Physics lore that cost real debugging (don't rediscover): current fields must step
diagonally with slopes (6-connectivity dies at the first ascending block); riders on
ascending steps have feet in the air block above the slope (resolve one block down);
vertical lift gates on field membership, not isInWater (surface-bob trap); jet nozzles
must touch the water path; gametest worlds run at noon (zombies burn — use husks);
config toggles are global (toggle gametests need their own serialized batches).

## Next up

1. **Forward ladder** (in progress): 1.21.8 census = 102 compile errors, all in known
   API-break classes with proven MC3DPrint seams to port (`BeData` save/load facade,
   `FuelCompat`, updateShape/neighborChanged/InteractionResult guard patterns, item
   color handler removal on 1.21.4+). Walk 1.21.8 → 26.2 per the guard lore in
   CLAUDE.md; then `scripts/build-all.sh` end-to-end.
2. **[HUMAN] dev-world playtest** — the M1 "is it FUN" gate: ride a real park (downhill,
   uphill jets, tube run, 4×4 valve-filled glass tube, splash pool), LAN rubber-band
   check, FX/audio tuning, Patchouli read-through with Patchouli installed.
3. **First release**: remote origin + push, draft GitHub Release to verify `release.yml`,
   CurseForge listing, v0.1.0.

## Later / v2 candidates

Race timers + checkpoints · ascending-tube lid art ·
custom particle/sound assets · richer website (custom domain, media, gallery — a simple
GitHub Pages site ships from `site/`) · pool-entry splash burst.
