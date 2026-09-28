# Open-World RPG — R01 Integration Test Harness

> Status: **TECHNICAL TEST CONTRACT — M0 fixture and R01 integration-playtest roles separated**
>
> Date: 2026-09-28
>
> Authority: `PROJECT.md`, `M0_INTEGRATION_ARCHITECTURE.md`, `R01_VERTICAL_SLICE.md`, `R01_CONTENT_BIBLE.md`

## 1. Why two test modes exist

The project has two different verification needs and they must not be conflated.

### M0 playtest artifact

Role:

- isolate combat/dependency integration;
- force a known Lv8 combat profile;
- expose direct Earthloong and weapon/spell probes;
- make narrow authority bugs reproducible quickly.

The embedded M0 marker intentionally allows automatic test-profile mutation. It is **not** a valid artifact for judging the natural R01 opening/progression flow.

### R01 integration playtest artifact

Role:

- run the real opening and persistent R01 progression;
- preserve ordinary starting loadout/class/quest/reward behavior;
- keep server-owned save/reconnect behavior intact;
- retain the manual M0 diagnostic commands for focused reproduction;
- never auto-promote the player to Lv8 or replace the player's normal class/equipment on join.

The R01 artifact marker is:

```text
data/openworld_rpg/integration/r01_player_verification.enabled
```

The M0 auto-profile marker must be absent from that artifact.

## 2. Normal-build isolation

The normal production-shaped JAR contains neither verification marker.

Therefore:

- normal players do not receive verification commands from these markers;
- normal join flow is not replaced by a test profile;
- development labels remain outside normal player-facing gameplay;
- a test artifact cannot silently become the normal artifact.

CI checks marker isolation when producing both playtest JARs.

## 3. R01 startup verification

When R01 integration verification is enabled, startup additionally loads/validates:

- the bundled R01 spatial dataset;
- the Alderford structure/service/property binding dataset;
- cross-reference validation between structure shells and spatial anchors.

The log records the current counts and whether each binding set has reached production readiness.

This is a **contract check**, not proof that candidate coordinates or gated prefab compositions are visually accepted.

## 4. Manual diagnostic probes

The R01 integration artifact may use the existing explicit M0 commands when a focused combat diagnosis is needed:

```text
/owr_spawn_earthloong
/owr_earthloong_motion_1
/owr_earthloong_motion_2
/owr_earthloong_motion_3
/owr_earthloong_motion_4
/owr_test_sword
/owr_test_crossbow
/owr_test_magic
```

These commands are diagnostic actions. Using one may deliberately alter the current test state. Merely joining an R01 playtest world does not.

## 5. What the R01 test artifact does not prove

A successful build/startup does not prove:

- route pacing;
- FOV/sightlines;
- UI quality;
- animation feel;
- boss readability;
- final asset quality;
- multiplayer behavior.

Those require the first integrated R01 build and the acceptance checklist already owned by the R01 canon.

## 6. Current verification labels

```text
M0 TECHNICAL TEST HARNESS: IMPLEMENTED
M0 AUTO-PROFILE ISOLATION: IMPLEMENTED

R01 NON-MUTATING TEST MODE: IMPLEMENTED
R01 STATIC BINDING CONTRACT CHECK: IMPLEMENTED
R01 PLAYTEST ARTIFACT BUILD PATH: IMPLEMENTED

R01 INTEGRATED PLAYER-FACING BUILD: NO
R01 PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

The user is not required to run this artifact until the R01 first integrated build reaches the project's planned test handoff.
