# Open-World RPG — Azari R01 Regalhart Spatial Pass 8

> Status: **ACTUAL-ANVIL-DERIVED ROOTSHADE TERRITORY BOUND / 3 REPEAT START ANCHORS RECORDED / MATERIALIZATION EXECUTOR STILL GATED**
>
> Date: 2026-10-03
>
> Canon owners: `R01_CONTENT_BIBLE.md`, `STATUS_AND_R01_ENCOUNTERS.md`
>
> Map build: `AzariNEW4252026`

## 1. Evidence boundary

This pass does not return to the old public-overhead estimate. It uses the persisted results of the
creator-acquired R01 slice whose Anvil region bytes were already decoded in Passes 2/4/5/7.

The relevant direct-world evidence is:

- Rootshade Grove accepted terrain pocket: `x -2760..-2600 / z 4160..4340`;
- Rootshade reference probe: `(-2680, 68, 4240)`, direct surface `grass_block`;
- southwestern tree-cover probe: `(-2696, 65, 4192)`, direct surface `grass_block`;
- eastern Root-Split Cairn terrain probe: `(-2600, 76, 4296)`;
- nearby direct surface samples include grass at `(-2712,66,4296)`,
  `(-2672,66,4176)`, calcite at `(-2624,72,4232)` and andesite at
  `(-2624,72,4252)`.

Rootshade was already selected specifically as the Lv6–8 optional danger pocket and the prior field
pass explicitly assigned it the Regalhart clue/start-boundary relationship.

## 2. Dedicated Regalhart runtime binding

The broader `r01_spatial_candidates.json` remains candidate-only. Regalhart therefore receives a
separate validated binding so accepting this boss territory cannot accidentally promote unrelated
Alderford, Quarry or gathering candidates.

Territory:

```text
id: openworld_rpg:r01/regalhart/territory
x: -2760 .. -2600
z:  4160 ..  4340
```

Core arena used by the post-eligibility 60-second empty condition:

```text
id: openworld_rpg:r01/regalhart/core_arena
x: -2744 .. -2600
z:  4176 ..  4320
```

The core arena is intentionally inset from the full Rootshade territory so merely skirting the
outer danger pocket does not hold the repeat-empty timer open.

## 3. Three deterministic start anchors

Exactly three authored start centers are recorded:

| ID suffix | Surface coordinate | Existing evidence |
|---|---:|---|
| `start_southwest` | `(-2696, 65, 4192)` | direct Anvil grass surface / denser Rootshade side |
| `start_center` | `(-2680, 68, 4240)` | established direct-Anvil Rootshade reference probe |
| `start_east_ridge` | `(-2600, 76, 4296)` | Root-Split Cairn eastern-boundary terrain probe |

These are controller start **centers**, not permission to place the entity through a tree, resource
presentation or occupied block. The later physical materializer must resolve a clear entity footprint
around the chosen center without moving outside the authored territory.

## 4. Runtime authority now closed

The server now owns:

- persisted Regalhart `cycle_index`, incremented once per valid defeat;
- deterministic start-center selection from world seed + persisted cycle;
- reconnect/reload cannot reroll the selected cycle;
- Rootshade territory membership;
- core-arena membership;
- the exact minimum player-distance materialization rule: reject at **<24 blocks**, allow the
  distance rule at exactly 24 blocks;
- explicit rejection when the real materialization caller reports that the point is directly visible
  in any player's camera;
- real loaded-player Rootshade/core presence can feed the existing 20 min + 60 s repeat timer and
  25 s disengage timer.

No synthetic FOV angle was invented. Camera visibility is deliberately an explicit caller-owned
result until the real materialization/presentation adapter is attached.

## 5. Still gated

This pass does **not** claim a spawned/playable Regalhart encounter.

Still separate:

- clear-footprint search around the selected start center;
- camera/occlusion implementation that supplies the direct-visibility fact;
- project-authored Regalhart spawn admission (the current production-spawn catalog still excludes it);
- reset executor that restores HP/poise/status and returns the actor to its controller start state;
- accepted physical movement/animation/presentation;
- weak-point head/antler geometry;
- unresolved Crown Charge / Rear Kick / mirrored Sweep / exact 7.0 and 12.0 attack-contract gaps.

## 6. Verification

The first CI for this pass, run `37110825529`, failed at compile because
`R01RegalhartSpatialAuthority.java` was written with literal `\n` characters. The source file
formatting was corrected without changing gameplay values.

Corrected code state:

`7e800c41c9563bb17298ef3ceab824af90c7e1d4`

Build Openworld RPG run `37110933916` / #319: **SUCCESS**.

Passed:

- clean tests/build;
- spatial binding + deterministic anchor tests;
- persistent cycle codec tests;
- 24-block materialization-boundary tests;
- pinned creature/dependency inspection;
- bootstrap JAR;
- core-profile dedicated-server smoke;
- gameplay-profile dependency-server smoke;
- gameplay-profile client startup smoke;
- verification JAR packaging;
- Modrinth playtest-pack packaging;
- artifact upload.

Artifact: `11269947593`

Normal JAR SHA-256:
`efb8e8153444ff9f1e1493cb4634cf20570b77ef02b712e5b200a42e451122b1`

```text
ACTUAL AZARI ANVIL-DERIVED ROOTSHADE EVIDENCE USED: YES
REGALHART TERRITORY BINDING: YES
REGALHART 3 START CENTERS: YES
REGALHART MATERIALIZATION EXECUTOR: NO
REGALHART PRODUCTION SPAWN: NO
BUILD VERIFIED: YES
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
R01 SPATIAL_BINDING COMPLETE: NO
```
