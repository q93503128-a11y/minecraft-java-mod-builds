# M3 Region 01 Arena-Pressure Displacement Field Check

Status: **HUMAN FIELD PLAY READY / NOT YET PLAYTESTED**

This focused check extends `M3_REGION01_BOSS_FIELD_PLAY.md` for the development-only Region 01 boss harness. It does not promote the boss into production encounter composition and does not approve final knockback, damage, arena geometry, animation, VFX, sound or encounter balance.

## What changed

`region_01_arena_pressure` keeps the existing server-authoritative local-area damage shape and authoritative attack timeline. The field harness now adds one provisional horizontal radial displacement when that attack first enters ACTIVE.

- provisional horizontal impulse strength: `0.85`
- vertical impulse: `0.0`
- application cadence: exactly once on ACTIVE entry, not once per ACTIVE tick
- target admission: the same server-authoritative eligible-target rule used by boss damage
- spatial admission: the same provisional `LOCAL_AREA` profile used by the field hit resolver
- facing: irrelevant; displacement points away from the boss center
- exact-center case: no arbitrary direction is invented, so an exactly coincident target receives no displacement from this calibration layer

`0.85` is a field-play calibration value, not final boss knockback balance.

## Setup

1. Use a JAR built from the checkpoint containing `Region01BossFieldImpulsePolicy` and `Region01BossFieldImpulseResolver`.
2. Enter a disposable flat test world in Survival or Adventure mode.
3. Spawn the development actor:

```text
/riftfrontier boss fieldtest spawn
```

4. Switch it to phase 2:

```text
/riftfrontier boss fieldtest phase2
```

5. Stay inside the provisional 24-block acquisition radius and wait for `region_01_arena_pressure`.

The existing diagnostic particle ring is still instrumentation only. Server hit/displacement results are authoritative if visuals disagree.

## Check A — one outward displacement at ACTIVE entry

1. Stand clearly inside the arena-pressure ring but not directly on the boss center.
2. During TELEGRAPH, stop moving so the displacement is easy to observe.
3. Observe the TELEGRAPH -> ACTIVE transition and the rest of ACTIVE.

Expected:

- TELEGRAPH causes no arena-pressure push;
- entering ACTIVE causes one horizontal movement away from the boss center;
- displacement is radial and does not depend on boss facing;
- later ACTIVE ticks do not repeatedly accelerate the same target from this field calibration;
- recovery causes no additional arena-pressure push;
- existing damage remains governed by the authoritative ACTIVE window and once-per-execution damage dedupe.

A repeated shove every ACTIVE tick is a regression, not a request to lower the strength.

## Check B — spatial agreement with the existing local-area threat

1. Repeat once clearly inside the particle ring.
2. Repeat once clearly outside the provisional ring.
3. Repeat at several angles around the boss.

Expected:

- an eligible target inside the local-area profile may receive the one ACTIVE-entry displacement regardless of facing;
- a target clearly outside the profile receives no field impulse;
- changing angle around the boss changes push direction outward but not eligibility radius;
- committed strike and line displacement never inherit this radial impulse.

If hit eligibility and displacement eligibility disagree, record exact positions/video before changing geometry.

## Check C — line-charge and arena-pressure remain different roles

1. Observe `region_01_line_displacement` in phase 2.
2. Observe `region_01_arena_pressure` in the same session.

Expected:

- line displacement moves the boss forward only during ACTIVE along its committed lane and does not radially shove targets through the arena-pressure calibration;
- arena pressure does not move the boss forward through the line-charge calibration;
- arena pressure instead creates a one-time outward target displacement at ACTIVE entry;
- both still use the same server-owned attack clock and existing damage authority.

The purpose is role readability, not numerical tuning. Record whether the difference is immediately understandable before proposing final values.

## Check D — multiple eligible targets

Run only if a genuine multiplayer or controlled multi-target session is available.

1. Place two eligible targets at different angles inside the pressure area.
2. Observe one arena-pressure ACTIVE entry.

Expected:

- each eligible target is displaced away from the same boss origin;
- targets are not forced into one shared facing direction;
- this observation counts as `MULTIPLAYER TESTED` only if a real multiplayer session was actually used.

## Evidence to record

Record:

- exact JAR filename and SHA-256;
- integrated server or real multiplayer topology;
- Minecraft/NeoForge versions;
- whether Checks A-D were attempted;
- PASS/FAIL for one-shot cadence, direction, range agreement and role separation;
- video or before/after positions for repeated push, inward/wrong-angle push, outside-radius push, missing push, damage/impulse disagreement, crash or desync.

Do not mark the checkpoint `PLAYTESTED` until a human performs it in Minecraft. CI, unit tests, GameTest, dedicated-server smoke and Xvfb client smoke are not human field play.
