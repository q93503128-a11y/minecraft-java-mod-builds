# Augmentation Survival — Live Playtest Fix Ledger — 2026-10-31 — Bundle G

## Trigger

Live play showed roughly ten wheat plants inside a Legendary Lv.2 Area Acceleration field with no visible growth stage increase during the full prior 30-second activation. Even though the previous x7 implementation preserved long-run random-tick opportunity math, the player-facing result was too weak and too stochastic for a Legendary active.

## Area Acceleration — redesigned for visible Legendary impact

- Lv.1 remains x3.
- Lv.2 changes from x7 to **x30**.
- Base duration changes from **30 seconds to 60 seconds**.
- Radius remains **24 blocks**, server-authoritative and spherical in 3D.
- Base cooldown remains **120 seconds**.
- The growth sampler uses the same authoritative multiplier as machine/production consumers instead of keeping a separate hard-coded 3/7 table.
- At vanilla `randomTickSpeed=3`, a stationary Lv.2 field schedules enough extra samples that every position in the radius-24 sphere receives about 25 additional random-tick opportunities over one 60-second activation. This preserves Minecraft crop rules while making the Legendary field visibly consequential.
- Overlapping fields still use the strongest multiplier rather than stacking multiplicatively.
- Overtime can still extend one activation by up to 50% of its new base duration.

## Player-facing presentation

The card does not describe implementation details such as random-tick sampling or claim a vague “felt” multiplier.

- Summary: `60초 동안 주변 24블록의 성장과 생산을 크게 가속합니다.`
- Detail communicates the actual Lv.1 / Lv.2 values as **3배 / 30배**, supported production categories, 24-block radius, 60-second duration and 120-second cooldown.
- Sustained HUD/profile duration now reads the authoritative `AreaAccelerationService.durationTicks()` value instead of a stale hard-coded 30 seconds.

## Revive design clarification discovered during the same playtest discussion

Current runtime already records and revives the caster's permanent companions when they are either:

- vanilla/modded tamed `TamableAnimal` companions such as wolves, or
- permanent companions owned through `지배`.

The allied-player branch is still missing from runtime and remains a separate multiplayer death/respawn authority pass. Agreed behavior for that pass:

- an allied-player death creates a 20-second revive marker;
- completing the 3-second channel after that player has already respawned moves the existing live player back to the recorded death position and restores health to 50% maximum;
- inventory, XP and already-created death drops are not rolled back;
- no duplicate `ServerPlayer` entity is ever created;
- allied permanent companions should be treated as revive allies as well, not merely temporary summons. Exact party/shared-owner eligibility must use the existing ally model when the multiplayer pass is implemented.

## Validation

Bundle G runtime checkpoint: `6e4927db07fdf7643d2d855f4c4d2db11fb400eb`.

Workflow #529 is the validation run for this checkpoint. Until it completes successfully, Bundle G must not be marked BUILD VERIFIED or used as the final playtest artifact.
