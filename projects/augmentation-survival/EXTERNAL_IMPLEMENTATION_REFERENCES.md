# Augmentation Survival — External Implementation References

This file records external implementation references that materially informed project runtime work. It distinguishes permissive code references from reference-only material.

## WorldBinder — water-surface walking

- Source: `https://github.com/Philiipp06/WorldBinder`
- Relevant files at review time: `movement/WaterSurface.java`, `mixin/MovementWaterMixin.java`
- License: MIT
- Role: **editable implementation reference**
- Used concept: query-time `LiquidBlock#getCollisionShape` surface at the real water fluid height, enabled only when the querying entity approaches from above; crouching disables the surface. Augmentation Survival independently adapts that pattern to its own augment ownership, 5-block dive bypass, server/client prediction and fall-damage rules.
- No WorldBinder assets are included.

## Transcending Trident — dry Riptide behavior precedent

- Source: `https://github.com/Serilum/Transcending-Trident`
- License/status: source-visible but all-rights-reserved project terms; **reference only**
- Role: **behavior/reference only; do not copy source**
- Observed precedent: a player holding the required water item and a Riptide trident can be treated as satisfying the vanilla water/rain environmental check, allowing vanilla Riptide presentation to remain intact.
- Augmentation Survival uses its own implementation and keeps server authority/augment ownership checks separate.
