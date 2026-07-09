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

**Added 2026-07-08 (Tristan-request pass):** **Inner Tube** — a single-seat boat-like raft
(new `entity/` package) that reuses the ride engine (RideTicker generalized `LivingEntity`
→ `Entity`), so it flows through slides/tubes/funnels identically AND floats on real water;
upright seat, free camera, dye-recolor, no-fall. **The Funnel** — a marquee Howlin'-Tornado
bowl (`funnel/` package): a `Funnel Core` (S/M/L) auto-stamps a stepped parabolic bowl of
`funnel_wall`, and one central-force swirl controller (`FunnelPhysics`, unit-tested) yields
BOTH the wall-to-wall oscillation (radial entry) and the spiral-drain whirlpool (tangential
entry) from entry angle alone; passive gravity, no RF, drains out the center. **Swing
physics** — a rider that stalls climbing now reverses instead of freezing, so any U-valley
swings; drag + a settle-tick guard always terminate. 58 GameTests + 14 JUnit green, client
boots clean. Design forks resolved in a grill session (round funnel + center drain; tube
rides everywhere).

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

Race timers + checkpoints · multi-seat cloverleaf raft (inner tube done; this is the 2–4
person version) · literal wedge-shaped funnel with an off-center exit (round funnel done) ·
ascending-tube lid art ·
custom particle/sound assets · richer website (custom domain, media, gallery — a simple
GitHub Pages site ships from `site/`) · pool-entry splash burst.
