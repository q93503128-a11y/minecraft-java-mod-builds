# R01 Trail Stag — production traversal runtime binding

> Recorded: 2026-10-13
> Scope: exact Trail Stag source admission, project-owned rideable entity, server traversal authority, Resolve, safe summon/dismount, client presentation.

## Exact source runtime

The runtime uses the repository-pinned Quaternius Stag GLB directly. It is not a vanilla horse substitute and is not reconstructed from an approximate Minecraft model.

```text
runtime: src/main/resources/assets/openworld_rpg/models/entity/trail_stag/Stag.glb
Git blob: 18927223c253a8ac497a442c1a419d3c287fb84c
bytes: 685176
SHA-256: 34818768f3e9298c9580ea79bfca2b79854a36877888a5e1e5df374b015f9e89
nodes: 56
meshes: 2
primitives: 6
skin joints: 38
materials: 6
animations: 13
```

`TrailStagGltfModel` verifies the SHA-256 and pinned structural/animation contract before accepting the resource, parses every primitive from both meshes, and CPU-skins the original joint-weighted geometry. The presentation runtime currently selects the source Idle, Walk, Gallop and Gallop_Jump clips from authoritative movement state. The remaining embedded clips stay admitted and contract-checked rather than being discarded.

## Server authority bound

`openworld_rpg:trail_stag` is a project-owned entity. The existing U-key request channel now completes into the real entity only after the server validates the authored summon rules.

Bound behavior:

- permanent unlock state remains attachment-backed;
- U starts the existing 1.0 s summon channel;
- hostile HP activity during the channel cancels it;
- the 4.0 s hostile-combat quiet gate is rechecked at completion;
- summon candidates use the authored ordered search and real collision/support/entity probes;
- support must be non-liquid and stay within the authored 0.75 block footprint variance;
- a no-mount-volume server seam is present for authored region integration;
- one active transient summon is kept per owner;
- owner-only riding authority is enforced;
- a world-reloaded/unbound Stag removes itself rather than becoming a persistent stray animal.

## Traversal authority bound

The server owns mounted movement rather than trusting a client-provided speed.

```text
cruise speed: 6.4 blocks/s
0 -> 90% cruise target: ~1.0 s
90% -> stop target: ~0.75 s
full-speed yaw cap: 120 deg/s
<=50% speed yaw cap: 180 deg/s
strafe: disabled
max authored step-up: 1.25 blocks
fixed hop apex target: 1.65 blocks
hop horizontal retention: 85%
landing recovery: 0.15 s
```

The hop is fixed rather than chargeable, cannot be repeated in air, and exposes an authoritative airborne state to the renderer. Normal project combat skill and dodge keys are consumed without casting while the player rides the Trail Stag.

## Resolve authority bound

Fresh legal summon initializes:

```text
ResolveMax = PlayerMaxHP * 1.00
ResolveCurrent = ResolveMax
```

Mounted player damage is intercepted at the final player-damage application boundary so armor/project mitigation is not recalculated. Final damage consumes Resolve first. If one hit exceeds remaining Resolve, only the overflow continues to player HP from that same application. Resolve break records the existing 12 s forced cooldown and forcibly separates the rider.

A hit whose collision lands on the Stag body while it is actively carrying its owner is routed through the owner's normal damage path first, so the same mitigation -> Resolve order is retained instead of giving the mount an independent HP model.

## Safe separation bound

U while mounted performs the authored safe-dismount search instead of starting another summon channel. Vanilla shift-dismount is intercepted at the same server authority boundary.

Candidate order remains:

1. left;
2. right;
3. behind-left;
4. behind-right;
5. behind.

If no legal supported collision-clear player volume exists, ordinary dismount is rejected and the rider remains mounted. A successful ordinary separation arms the Stag's 1.0 s clean despawn. Resolve break is not allowed to leave the player mounted; it prefers the same legal search and uses an emergency collision-clear vertical separation only for the forced-break case.

## Integration still intentionally outside this gate

These are not accepted merely because the runtime compiles:

- final Minecraft model scale and ground contact;
- rider-seat alignment;
- collision box agreement with the visible body/antlers;
- Walk/Gallop/hop visual timing and foot contact;
- authored R01 no-mount region data provider;
- `A Stag at the Ford` narrative unlock event integration if that event is not yet live;
- mounted basic-attack -> safe dismount -> 0.10 s attack buffer integration;
- integrated R01 client-world spatial acceptance;
- multiplayer behavior.

The mounted-basic-attack path is kept separate from this change because the current melee authority is Better Combat context-bound; replaying `Player.attack` two ticks later would silently lose that donor attack context and would be an incorrect implementation of the 0.10 s buffer.

## Verification

This commit intentionally triggers the normal Openworld RPG integration workflow. Do not promote the following fields until that exact runtime has completed its checks.

```text
SOURCE ASSET ACQUIRED: YES
SOURCE HASH VERIFIED: YES
LICENSE EVIDENCE RECORDED: YES
SOURCE FILE PINNED IN REPO: YES
RUNTIME ENTITY BOUND: YES
SERVER TRAVERSAL AUTHORITY BOUND: YES
RESOLVE RUNTIME BOUND: YES
SAFE DISMOUNT AUTHORITY BOUND: YES
SOURCE ANIMATION RUNTIME BOUND: YES
TESTED: PENDING FINAL WORKFLOW
BUILD VERIFIED: PENDING FINAL WORKFLOW
JAR PRODUCED: PENDING FINAL WORKFLOW
CLIENT STARTUP: PENDING FINAL WORKFLOW
CLIENT WORLD JOIN: NOT RUN
MINECRAFT SCALE ACCEPTED: NO
GROUND CONTACT ACCEPTED: NO
RIDER SEAT ACCEPTED: NO
TRAVERSAL VISUAL ACCEPTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
