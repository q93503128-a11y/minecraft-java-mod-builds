# M3 Region 01 Boss Field Play

Status: **HUMAN FIELD PLAY READY / NOT YET PLAYTESTED**

This document is the exact manual validation contract for the development-only Region 01 boss combat harness. It does not promote the boss into Region 01 production encounter composition and it does not approve final hit geometry, damage, material, animation mapping, VFX or sound.

## Purpose

The harness exists to exercise the already-authored server-authoritative Region 01 boss semantics in a real Minecraft world before production encounter integration:

- production boss profile `riftfrontier:boss/region_01_first_apex`;
- phase 1 attack pool: committed strike + line displacement;
- phase 2 attack pool: committed strike + line displacement + arena pressure;
- authoritative telegraph -> ACTIVE -> recovery attack clock;
- real server damage gated by ACTIVE only;
- one damage event per target per attack execution;
- semantic presentation payload delivery from the same validated server runtime.

The harness deliberately uses one neutral diagnostic hit volume and `1.0F` damage for every attack. Those values are **not final boss balance or attack geometry** and must not be tuned from automated results alone.

## Prerequisites

Use a current JAR built from the checkpoint that contains `Region01BossFieldPlayCommand` and the `Region01BossEntity` field harness. Human testing must be performed in a real Minecraft client; CI client smoke does not count.

For damage observations, use Survival or Adventure mode and remove armor/resistance effects that would make one-health-point changes hard to read.

## Spawn and cleanup

Spawn the explicit development actor six blocks in front of the invoking player:

```text
/riftfrontier boss fieldtest spawn
```

Switch the nearest enabled field-test boss within 64 blocks to phase 2:

```text
/riftfrontier boss fieldtest phase2
```

Return it to phase 1:

```text
/riftfrontier boss fieldtest phase1
```

Cleanup after the session:

```text
/kill @e[type=riftfrontier:region_01_boss]
```

Do not add this entity to natural spawning or production Region 01 encounter data merely because the field-test command works.

## Test A — actor and authoritative attack loop

1. Stand in an open flat area.
2. Run `/riftfrontier boss fieldtest spawn`.
3. Keep the boss loaded for at least several complete attack cycles.
4. Verify that the server/client remain stable and that the actor does not require a production encounter to advance its validated attack runtime.
5. Move out of range, back into range, and keep the actor loaded through additional cycles.

Expected:

- no crash, disconnect or repeated content-generation exception;
- attacks continue to start only from the authored phase pool;
- presentation sync does not create a second independent attack clock;
- no final Dragon material/VFX/sound is expected yet because the selected physical presentation manifest is intentionally still absent.

## Test B — ACTIVE-only damage and execution dedupe

1. Use Survival/Adventure mode with clearly visible health.
2. Move close enough to intersect the current diagnostic local volume.
3. Remain inside it through one complete attack execution.
4. Observe health over telegraph, ACTIVE and recovery.
5. Repeat several attacks.

Expected:

- telegraph alone causes no damage;
- recovery causes no new damage;
- the target can be damaged during ACTIVE;
- one attack execution cannot repeatedly damage the same target every ACTIVE tick;
- current diagnostic damage is `1.0F` before armor/effect handling and is not a final balance decision.

If repeated damage occurs within one execution, record the attack/presentation phase and approximate server tick; that is a regression and should be fixed before tuning visuals.

## Test C — phase composition

1. Spawn a fresh field-test boss and observe phase 1 for several complete attacks.
2. Run `/riftfrontier boss fieldtest phase2`.
3. Keep the same actor alive and loaded for several more attacks.
4. Optionally run `/riftfrontier boss fieldtest phase1` and repeat.

Expected:

- phase switching is server-authoritative and does not require replacing the entity;
- an in-flight attack is cancelled by the authoritative phase transition before the new phase pool begins;
- phase 1 may select only committed strike and line displacement;
- phase 2 may select committed strike, line displacement and arena pressure.

Because final animation/VFX/sound bindings are not published yet, this test verifies semantic/runtime composition rather than final human readability of the three roles.

## Test D — content reload continuity

1. Spawn an enabled field-test boss.
2. Let at least one attack begin.
3. Execute `/reload` while the actor remains loaded.
4. Continue observing the same actor after the reload completes.

Expected:

- the retained old validated runtime is retired when its published generation becomes stale;
- the entity rebuilds its field-test runtime from the newly published content snapshot;
- no stale-generation runtime resumes damage after reload;
- the actor remains usable for subsequent field-test attacks.

## Test E — player weapon versus boss actor

1. Provision either supported player weapon loadout, for example:

```text
/riftfrontier weapon mobile
```

2. Bind the two Riftfrontier combat actions in Controls.
3. Hold the issued carrier in the main hand.
4. Attack the field-test boss from several ranges/angles.
5. Repeat with:

```text
/riftfrontier weapon reach
```

Expected:

- the boss is a normal authoritative server target for the existing player weapon impact path;
- player field-impact behavior stays governed by the existing `M3_PLAYER_COMBAT_FIELD_PLAY.md` contract;
- no conclusion about final boss health, hitbox, weapon balance or encounter difficulty is valid from this harness alone.

## Evidence to record

For each human session record:

- exact JAR file name and SHA-256;
- singleplayer/integrated server or dedicated multiplayer;
- Minecraft/NeoForge versions;
- commands used;
- whether Tests A–E were attempted;
- PASS/FAIL per expected observation;
- screenshots/video for any visual or timing issue where practical;
- exact symptom for any duplicate damage, stale runtime, phase-pool escape, crash or desync.

Only after a real human session may the corresponding checkpoint be labeled `PLAYTESTED`. A real two-client/dedicated-session observation is required for `MULTIPLAYER TESTED`.
