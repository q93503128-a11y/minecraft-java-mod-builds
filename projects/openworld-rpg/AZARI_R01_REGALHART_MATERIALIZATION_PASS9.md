# Open-World RPG — R01 Regalhart Materialization / Reset Pass 9

> Status: **MATERIALIZATION + DISENGAGE RESET BACKEND BUILD VERIFIED / LIVE PRODUCTION SPAWN STILL GATED**
>
> Date: 2026-10-03
>
> Map build: `AzariNEW4252026`
>
> Code state: `137d3c3e65bfbcd0e5a4619f166cf70fb5095cd1`

## 1. Scope

This pass continues directly from `AZARI_R01_REGALHART_SPATIAL_PASS8.md`.
It does not invent another terrain search area or replace the three accepted Rootshade start centers.

The selected deterministic center either passes the real donor-entity placement checks or the
operation fails closed. There is **no arbitrary fallback radius or offset search**.

## 2. Materialization plan

`R01RegalhartMaterializationRuntime` now validates the selected center against:

- the accepted Regalhart Rootshade territory;
- the actual loaded support block at the stored Anvil-derived surface coordinate;
- a sturdy upward support face;
- the exact registered `threateningly_mobs:the_regalhart` EntityType;
- that EntityType's real `getSpawnAABB(...)`, rather than a guessed width/height;
- server collision against that real spawn AABB;
- liquid intersection;
- all active same-level non-spectator living players;
- the canonical **<24-block** materialization exclusion;
- an explicit camera-visibility result supplied by the future real camera/occlusion adapter.

If any gate fails, no alternate point is fabricated.

## 3. Spawn admission remains closed

The materializer exposes a future repeat-spawn seam, but it checks
`R01ExternalActorCatalog.productionSpawnReady(REGALHART)` before mutation.

That catalog still admits only Earthloong. Regalhart remains closed because this pass does not yet
provide the full Regalhart reward transaction and does not claim final in-client presentation
acceptance. Therefore the new spawn seam currently returns empty rather than creating an incomplete
field boss.

The pinned dependency byte audit already proves that the Regalhart donor surface contains its exact
entity, renderer/model, textures/sounds and idle/walk/run + four attack animation definitions. That
does not substitute for final visual/play acceptance.

## 4. Disengage reset

For an already-existing exact project-authored Regalhart, the reset path now:

1. re-derives the same current-cycle start center from world seed + persisted cycle;
2. validates the same support/collision/liquid/24-block/camera gates, ignoring the boss itself during
   collision testing;
3. stops Mob target/navigation;
4. returns the actor to the accepted controller start center and zeroes residual velocity;
5. clears project Regalhart Sweep/Sovereign execution state and removes the Sovereign movement
   modifier;
6. restores canonical project HP to full;
7. replaces poise with a fresh full boss-poise state at the current server tick;
8. clears project-owned hostile control/debuff runtime state without deleting unrelated donor-owned
   passive/presentation effects;
9. acknowledges the 25-second disengage only after the reset succeeds;
10. grants no reward.

If the start center is currently unsafe or hidden-camera legality is not established, reset fails
closed and the controller does not acknowledge completion.

## 5. Remaining gates

This pass does not make Regalhart a playable production boss yet.

Still open:

- final Regalhart personal reward transaction / repeat package delivery;
- actual live spawn admission;
- real camera/occlusion caller;
- production selector/executor orchestration;
- Crown Charge ordinary guardability;
- Rear Kick guard-pressure/player-poise values;
- mirrored Antler Sweep second-hit exact timing;
- exact 7.0 / 12.0 weighted-selection boundary ownership;
- weak-point head/antler hit geometry;
- final physical attack movement/presentation and play-feel verification.

## 6. Verification

Build Openworld RPG run `37112353400` / #320: **SUCCESS**.

Passed:

- clean tests/build;
- pinned gameplay/creature dependency verification;
- R01 creature registry byte inspection;
- Earthloong dependency inspection;
- bootstrap JAR;
- core-profile dedicated-server smoke;
- gameplay-profile dependency-server smoke;
- gameplay-profile client startup smoke;
- both verification JARs;
- Modrinth playtest-pack packaging;
- artifact upload.

Artifact:
`openworld-rpg-m0-137d3c3e65bfbcd0e5a4619f166cf70fb5095cd1`
(`11270975370`)

Normal JAR SHA-256:
`b2d010d833f7c6998b3a62b63701f3fbc2b85bbcbd89a0912e731bca942d2a4c`

```text
REGALHART MATERIALIZATION BACKEND: YES
REGALHART DISENGAGE RESET BACKEND: YES
REGALHART LIVE PRODUCTION SPAWN: NO
REGALHART REWARD TRANSACTION COMPLETE: NO
BUILD VERIFIED: YES
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
R01 SPATIAL_BINDING COMPLETE: NO
```
