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
