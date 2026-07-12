# Changelog

All notable changes to MC Waterslides. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions track the mod jar.

## [Unreleased]

### Added
- **Swing valleys** — riders that stall while climbing now swing back down instead of
  freezing on the ramp, so any U-shaped slide valley oscillates and settles.

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
- **Smoother flumes.** Channel troughs and tube bores now round with half-pixel quarter-ellipse
  stepping (double the resolution, vertical tangent at the wall) so the cross-section reads as
  a real half-pipe instead of stairs. Corner channels got the trough rounding they were missing,
  vertical tube shafts got bore rounding, and channel walls grew a rolled rim lip like a real
  fibreglass flume. Despite the finer curve, long runs render *leaner* — buried faces are
  skipped and end caps cull at block seams.
