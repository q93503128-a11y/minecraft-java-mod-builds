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

## Time Acceleration mining regression discovered while validating Bundle G

The stronger Area Acceleration tests exposed an older Time Acceleration mining regression that the previous constant-only checks had missed.

Root cause:

- the main Time Acceleration runtime already applied its +60% mining bonus as a transient vanilla `BLOCK_BREAK_SPEED` attribute modifier;
- `TimeAccelerationMiningAttributeMixin` immediately removed that modifier;
- `SurvivalTimeAccelerationMiningMixin` attempted to re-add the bonus through a separate shared mining calculation, but that bridge did not survive into the actual vanilla final destroy-speed result;
- an attempted two-argument `Player#getDestroySpeed(BlockState, BlockPos)` hook was rejected by the real 26.2 runtime because that overload does not exist on `Player`.

Final repair:

- keep the canonical +60% modifier directly on vanilla `Attributes.BLOCK_BREAK_SPEED`;
- remove the attribute-stripping and duplicate time-mining bridge mixins;
- restore the real one-argument `Player#getDestroySpeed(BlockState)` hooks used by the remaining passive/client mining prediction layer;
- stop manually adding Time Acceleration again on the client, avoiding double application because the server attribute is authoritative and synchronized;
- regression test now verifies movement +60%, attack speed +60%, `BLOCK_BREAK_SPEED` +60%, final `getDestroySpeed` x1.60, no duplicate shared-mining bonus, item-use x1.40 and owned-projectile x1.40 after the real three-second focus completes.

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

Bundle G verified checkpoint: `2833f6864e39c53e96f3493ccea5db8728cc71f6`.

Workflow **#537** completed successfully:

- **132/132 required GameTests PASS**
- clean build / dependency checks PASS
- World Bosses localization capture PASS
- external Korean localization audit PASS
- dedicated-server boot smoke PASS
- client boot smoke PASS
- verified MRPack generation PASS
- production JAR structure/checksum PASS

Production JAR SHA-256: `d33698f3745cb2355e3907823b5895a44592148e5459bd41bb560bec22688855`

MRPack SHA-256: `1a7bdfa00a1b539cace7fd48dba86fb714331fac0ce7e4de8b4f999ad43b0da3`

Manual Bundle G playtesting, multiplayer testing and performance profiling remain **NOT RUN**.
