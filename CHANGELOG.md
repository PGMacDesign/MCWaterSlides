# Changelog

All notable changes to MC Waterslides. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions track the mod jar.

## [Unreleased]

### Added
- **Inner Tube** — a placeable, single-seat boat-like raft. Right-click onto water or a
  slide to place, right-click to ride. You sit upright with a free camera; it flows through
  slides, tubes, and funnels exactly like an on-foot rider and floats on real water. Cheap
  (leather + copper), dye-recolorable (sneak + dye), no fall damage.
- **The Funnel** — a Howlin'-Tornado bowl. A `Funnel Core` (Small / Medium / Large, ~5/7/9
  blocks across) auto-builds a stepped bowl of `Funnel Wall` and clears it when broken.
  Passive gravity (no RF): drop in aimed across to swish wall-to-wall, or along the rim to
  spiral the drain — both empty out the center hole into whatever exit you build below.
- **Swing valleys** — riders that stall while climbing now swing back down instead of
  freezing on the ramp, so any U-shaped slide valley oscillates and settles.

### Changed
- The ride engine now drives any `Entity`, not just `LivingEntity`, so non-player rideables
  (the inner tube) share the exact same momentum/slope/corner/jet/funnel physics.
- **Full art pass.** Every texture redrawn to vanilla's material language — palettes sampled
  from 1.21.1 copper/iron/quartz, structured shading instead of pixel noise, hue-shifted
  ramps, top-left lighting, dark item outlines. Machines are now cut-copper plate bodies
  with iron accents (grate intakes, glowing nozzles, riveted valve faces, boiler portholes);
  slides are pale moulded fibreglass; glass got vanilla-style glint streaks; the funnel bowl
  is glazed ceramic tile over a shower-drain core.
- **Pump House screen rebuilt** in the vanilla container style: furnace-style burn flame,
  a cyan RF gauge with a hover tooltip (exact RF + rate), and a live rate readout.
- The mod now ships a proper logo + pack icon (a flume diving into a splash pool), shown in
  the mods list and resource-pack screen.
- **Smoother flumes.** Channel troughs and tube bores now round with half-pixel quarter-ellipse
  stepping (double the resolution, vertical tangent at the wall) so the cross-section reads as
  a real half-pipe instead of stairs. Corner channels got the trough rounding they were missing,
  vertical tube shafts got bore rounding, and channel walls grew a rolled rim lip like a real
  fibreglass flume. Despite the finer curve, long runs render *leaner* — buried faces are
  skipped and end caps cull at block seams.
