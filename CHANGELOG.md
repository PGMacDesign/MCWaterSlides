# Changelog

All notable changes to MC Waterslides. Format loosely follows
[Keep a Changelog](https://keepachangelog.com/); versions track the mod jar.

## [Unreleased]

### Added
- **Inner Tube** — a placeable, single-seat boat-like raft. Right-click onto water or a
  slide to place, right-click to ride. You sit upright with a free camera; it flows through
  slides, tubes, and funnels exactly like an on-foot rider and floats on real water. Cheap
  (leather + copper), dye-recolorable (sneak + dye), no fall damage.
- **The Tornado Funnel** — a giant cone lying on its side, the real Howlin'-Tornado layout.
  Place a `Tornado Funnel` core (Small / Medium / Large — mouths ~6/8/11 blocks tall) where
  the exit should be: it faces the way you look and auto-builds the whole half-open cone
  behind it in alternating pinwheel stripes. Feed a slide into the wide mouth, swish wall to
  wall as the cone narrows, and fire out the throat level, keeping your momentum — you barely
  lose height. Passive gravity + water current, no RF; the current always washes everyone out
  the exit (nothing can get stuck); crouch bails. The exit is purely geometric — no drain, no
  speed gates. The swing is a true pendulum on the cone wall — a hot side-entry carries its
  momentum all the way up the far wall and back, decaying only through drag. (Replaces the vertical drain-bowl funnel design entirely; funnel shell blocks
  are machine-stamped only and no longer craftable items.)
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
