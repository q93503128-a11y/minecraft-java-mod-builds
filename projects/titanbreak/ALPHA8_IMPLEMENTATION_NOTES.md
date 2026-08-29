# alpha.8 implementation notes

## First production slice: T0 hunting foundation
alpha.8 is the first gameplay-production batch after the temporal/multipart risk checks. It starts the content bible's hunting loop at the point where actual enemies produce actual augmentation materials and persistent Research Data.

## Combat value conversion
The content bible uses a visible baseline of 100 Max Health while a normal Minecraft player internally uses 20 health. `CombatScale` now owns the 5:1 display/internal conversion so content values can stay in the bible's scale without multiplying Minecraft's underlying health system everywhere.

The HUD therefore shows an ordinary unmodified player as 100 / 100 instead of 20 / 20. Enemy attributes in this slice are authored from their content-bible visible values and converted into internal Minecraft values through the same layer.

## T0 enemies
### Ripper
- visible Max Health 120
- visible damage 14 per strike
- two-strike combo, with the follow-up advancing on the mob AI clock so Reflex Drive slows the combo cadence together with the enemy
- faster pursuit and periodic lateral/flanking path requests
- daylight does not disable the hunt
- drops M-KIN-01 High-Density Muscle Fiber x1-2
- 20% chance for M-NEU-01 High-Density Neural Fiber x1

### Skitter
- visible Max Health 100
- visible damage 12 per strike
- three-strike combo, advanced on the mob AI clock
- inherits Spider climbing/navigation behavior as the first wall-capable T0 chassis
- drops M-COM-02 Servo Bundle x1
- drops M-COM-05 Synthetic Tendon x1-2

The current vanilla-derived renderers are functional gameplay placeholders. Final enemy silhouettes/models/animations are a separate art-production pass and are not being treated as finished visual assets.

## Research Data persistence
Player profile schema 2 adds:
- persistent Research Data
- a persistent set of normal species already credited for first-kill research

The existing saved-data id remains unchanged so older profiles migrate forward. Ripper and Skitter each grant +10 Research Data only on the player's first credited kill of that species. The reward is shown as an in-world action-bar message and synced to the client snapshot as `rd` for later Fabricator/analysis UI work.

## Materials now present
The first physical augmentation-material items are registered:
- M-COM-02 Servo Bundle
- M-COM-05 Synthetic Tendon
- M-KIN-01 High-Density Muscle Fiber
- M-NEU-01 High-Density Neural Fiber

Their current icons deliberately reuse vanilla item models so the gameplay/data loop can be validated without locking in unlicensed or low-quality final art.

## Deliberate boundary
Natural spawn density is not fixed in the v0.3 content bible, so alpha.8 does not invent permanent biome weights yet. Ripper and Skitter are command-spawned for this validation slice. Encounter distribution will be added together with the trace/threat-region layer rather than hard-coding arbitrary spawn pressure now.

Fabricator I is also not represented by a normal crafting-table recipe. The content bible explicitly requires its own selection/preview/material/strain UI, so the next production batch should build the Fabricator/Surgical data model and dedicated interaction instead of shipping a temporary 3x3 recipe that would later need to be removed.
