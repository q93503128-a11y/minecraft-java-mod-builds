# Open-World RPG — R01 Trail Stag Input / Summon Authority

> Date: 2026-10-13
> Scope: rebindable mount input transport and server-owned summon-channel authority. This checkpoint does not claim a visible rideable Trail Stag.

## Closed in this pass

- default mount action is bound to `U` in its own traversal key category and remains rebindable;
- the client sends only a monotonically increasing request sequence plus protocol version;
- summon position, timing, cooldown, Resolve, movement and other gameplay values are never accepted from the client;
- replayed/out-of-order request sequences are rejected server-side;
- server pre-channel authority checks permanent unlock/selection, living non-spectator state, the 12 s Resolve-break cooldown, presentation-runtime readiness and the exact 4.0 s hostile-HP quiet requirement;
- summon channel duration is exactly 20 ticks / 1.0 s;
- a newer hostile HP interaction during the channel cancels it;
- final authored-volume and placement checks remain completion-time responsibilities so movement during the channel cannot reuse a stale candidate position;
- disconnect removes transient request/channel state;
- English and Korean key labels are player-facing and do not expose implementation terminology.

## Deliberately still gated

The live entity binder is fail-closed until the exact accepted Stag runtime artifact and project-owned rideable renderer/entity exist. This prevents the input path from ever exposing a vanilla horse, invisible entity, or static placeholder.

The source/license identity is already recorded in `external-assets/quaternius-trail-stag/SOURCE.md`, but the repository still does not contain the accepted `Stag.glb` bytes. GitHub also rejects directly reusing the public mirror's blob object across repositories, so the binary must be admitted through a real byte transfer rather than a cross-repository object reference.

Still required for a playable Trail Stag:

1. acquire and pin the accepted Stag bytes in this repository;
2. record the actual runtime SHA-256 and inspect mesh/material/skin/animation structure;
3. register the project-owned `trail_stag` rideable entity and exact model/animation runtime;
4. bind completion to the already-authored server placement probes and safe dismount search;
5. bind rider steering, acceleration, fixed hop, Resolve damage interception and clean despawn;
6. run the integrated Minecraft client review for scale, ground contact, rider seat, collision footprint, movement/animation agreement and safe dismount behavior.

## Verification

Validated code commit:

`970b1571ebbd24e70abfba952c11b25598004ebf`

GitHub Actions:

- workflow: `Build Openworld RPG`
- run: `38357388207` / Build Openworld RPG #361
- result: SUCCESS
- `Test and build M0 core`: SUCCESS
- core-profile dedicated server smoke: SUCCESS
- gameplay-profile dependency server smoke: SUCCESS
- gameplay-profile client startup smoke: SUCCESS
- JVM-free joined-player Arc Bolt playtest JAR build: SUCCESS
- non-mutating R01 integration playtest JAR build: SUCCESS
- artifact id: `11793161956`
- artifact: `openworld-rpg-m0-970b1571ebbd24e70abfba952c11b25598004ebf`
- artifact SHA-256: `312dbfdab9cecb13bb4a0130835225a9795bd4a8c53826d23310965438c77be3`

```text
CODE REVIEWED: YES
TESTED: YES
BUILD VERIFIED: YES
JAR PRODUCED: YES
CORE SERVER TESTED: YES
GAMEPLAY DEPENDENCY SERVER TESTED: YES
CLIENT STARTUP TESTED: YES
U-KEY CLIENT INPUT BOUND: YES
SERVER SUMMON CHANNEL AUTHORITY BOUND: YES
LIVE TRAIL STAG ENTITY: GATED
STAG RUNTIME ASSET PINNED: NO
CLIENT WORLD JOIN: NOT RUN
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
