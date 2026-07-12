# Changelog

All notable changes to MC Waterslides. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions track the mod jar.

## [Unreleased]

### Added
- **Swing valleys** — riders that stall while climbing now swing back down instead of
  freezing on the ramp, so any U-shaped slide valley oscillates and settles.

- **Jets daisy-chain RF.** Jets placed against each other pass power toward whichever
  neighbor holds less (half the difference per tick, rate-capped) — wire one jet in a row
  and the chain feeds itself, hop by hop. Redstone-disabling a jet cuts the chain there.
  Config: `jet.shareRf` (0 disables).

- **The ride has a voice.** A synthesized water-rush loop follows every rider — you, other
  players, mobs — swelling and pitching up with speed; rides open with a soft whoosh and end
  in a proper splash-down (sound + burst) at the pool. Plus a subtle FOV kick near the speed
  cap. All generated audio (no sampled assets), all config-toggleable (`fx.*`).

- **Self-explanatory items.** Every item now carries a one-line tooltip (what it does, in
  plain words; Shift for the detail line) — the mod teaches itself even without Patchouli
  installed. New "Stick the Landing" advancement closes the guided chain: place → power →
  ride → splash-land.

### Changed
- The ride engine now drives any `Entity`, not just `LivingEntity` — groundwork for
  future non-player rideables.
- **Full art pass.** Every texture redrawn to vanilla's material language — palettes sampled
  from 1.21.1 copper/iron/quartz, structured shading instead of pixel noise, hue-shifted
  ramps, top-left lighting, dark item outlines. Machines are now cut-copper plate bodies
  with iron accents (grate intakes, glowing nozzles, riveted valve faces, boiler portholes);
  slides are pale moulded fibreglass; glass got vanilla-style glint streaks.
- **Pump House screen rebuilt** in the vanilla container style: furnace-style burn flame,
  a cyan RF gauge with a hover tooltip (exact RF + rate), and a live rate readout.
- The mod now ships a proper logo + pack icon (a flume diving into a splash pool), shown in
  the mods list and resource-pack screen.
- Jet side chevrons now point the way the jet pushes (they pointed straight up before,
  whatever the facing); the splash pool's inventory icon renders isometric with visible
  water instead of a blank face-on wall.
- **Smoother flumes.** Channel troughs and tube bores now round with half-pixel quarter-ellipse
  stepping (double the resolution, vertical tangent at the wall) so the cross-section reads as
  a real half-pipe instead of stairs. Corner channels got the trough rounding they were missing,
  vertical tube shafts got bore rounding, and channel walls grew a rolled rim lip like a real
  fibreglass flume. Despite the finer curve, long runs render *leaner* — buried faces are
  skipped and end caps cull at block seams.
