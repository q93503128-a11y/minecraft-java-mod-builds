# M3 Region 01 Boss Field Play

Status: **HUMAN FIELD PLAY READY / NOT YET PLAYTESTED**

This document is the exact manual validation contract for the development-only Region 01 boss combat harness. It does not promote the boss into Region 01 production encounter composition and it does not approve final hit geometry, damage, targeting range, movement speed, material, animation mapping, VFX or sound.

## Purpose

The harness exists to exercise the already-authored server-authoritative Region 01 boss semantics in a real Minecraft world before production encounter integration:

- production boss profile `riftfrontier:boss/region_01_first_apex`;
- phase 1 attack pool: committed strike + line displacement;
- phase 2 attack pool: committed strike + line displacement + arena pressure;
- authoritative telegraph -> ACTIVE -> recovery attack clock;
- real server damage gated by ACTIVE only;
- one damage event per target per attack execution;
- attack-specific provisional field geometry that exposes the three authored combat roles;
- attack-start target commitment toward a nearby real player, without mid-attack homing;
- provisional server-owned ACTIVE travel for the authored `line_charge` role;
- semantic presentation payload delivery from the same validated server runtime.

The harness uses role-specific **provisional** geometry and `1.0F` damage. Committed strike is a broad short forward commitment, line displacement is a longer narrow forward lane, and arena pressure is a local area around the boss. These dimensions are **not final boss balance or attack geometry** and must not be tuned from automated results alone; they exist so human field play can decide whether the already-authored role contrast is actually readable and fair in Minecraft.

The authored line-displacement attack uses `delivery: line_charge` and counterplay `read_travel_lane`, `move_laterally`, `punish_recovery`. The field harness therefore gives only this role provisional physical travel: **0.5 blocks per authoritative ACTIVE tick** along the facing committed before TELEGRAPH. Committed strike and arena pressure have zero authored field travel. The 0.5 value is calibration scaffolding, not final boss movement speed or charge distance; human play must decide whether it needs revision. The move uses Minecraft entity collision resolution rather than teleporting through terrain.

The harness also uses a provisional **24-block target-acquisition radius**. It is only a field-test convenience, not final aggro, encounter leash, arena, perception, or AI policy. When no alive non-spectator player is inside that radius the actor waits instead of attacking empty space. When a new attack begins, the server faces the actor toward the nearest eligible player once; the direction is then committed for the whole TELEGRAPH/ACTIVE/RECOVERY execution so sidestep and lane-reading counterplay can be tested without homing rotation.

A client-side development readability overlay projects the same `Region01BossFieldImpactProfile` boundary with lightweight vanilla particles while no reviewed production boss render binding is published. `CLOUD` marks TELEGRAPH, `CRIT` marks ACTIVE, and `SMOKE` marks RECOVERY. This is diagnostic instrumentation, **not final VFX/art direction**. It changes no server hit logic and automatically disappears once a reviewed production render binding exists. During line displacement the projection follows the server actor as it advances; hit/miss remains authoritative if any visual disagreement is observed.

## Prerequisites

Use a current JAR built from the checkpoint that contains `Region01BossFieldPlayCommand`, `Region01BossFieldAimPolicy`, `Region01BossFieldImpactProfile`, `Region01BossFieldReadabilityOverlay`, and the `Region01BossEntity` field harness. Human testing must be performed in a real Minecraft client; CI client smoke does not count.

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

## Test A — actor, target acquisition and authoritative attack loop

1. Stand in an open flat area.
2. Run `/riftfrontier boss fieldtest spawn`.
3. Keep the boss loaded for at least several complete attack cycles while remaining within 24 blocks.
4. Move clearly beyond 24 blocks after the current attack finishes and wait long enough that another attack would normally begin.
5. Return inside 24 blocks and keep the actor loaded through additional cycles.

Expected:

- no crash, disconnect or repeated content-generation exception;
- while an alive non-spectator player is within the provisional acquisition radius, attacks continue to start only from the authored phase pool;
- with no eligible player within 24 blocks, a new attack does not start merely to swing at empty space;
- returning inside the radius allows the next authoritative attack to begin again;
- presentation sync does not create a second independent attack clock;
- no final Dragon material/VFX/sound is expected yet because the selected physical presentation manifest is intentionally still absent;
- while that production binding is absent, the diagnostic particle boundary should follow the current attack phase without affecting damage.

The 24-block threshold is instrumentation, not a balance recommendation. Record whether the transition behaves correctly; do not tune the radius from feel yet.

## Test B — committed aim, readable lane, ACTIVE charge and no mid-attack homing

1. Stand within the 24-block acquisition radius and let the actor become idle between attacks.
2. When the next TELEGRAPH begins, note the boss facing, actor position and visible diagnostic boundary.
3. After that direction is committed, move laterally across or out of the displayed lane/arc before ACTIVE.
4. For `region_01_line_displacement`, also watch the actor position through TELEGRAPH, ACTIVE and recovery.
5. Repeat line displacement once with a solid wall or obstacle in the committed travel lane.
6. Repeat for `region_01_committed_strike` and confirm it does not inherit charge travel.
7. Repeat once while another eligible player is present at a different angle if doing a real multiplayer session.

Expected:

- at each new attack start the server turns the boss toward the nearest eligible player rather than reusing its original spawn-facing direction;
- once TELEGRAPH has begun, the boss does **not** keep rotating to follow a moving target through TELEGRAPH/ACTIVE/RECOVERY;
- a lateral dodge can therefore move out of the committed line/arc instead of the attack homing onto the player;
- `region_01_line_displacement` remains stationary during TELEGRAPH, advances only during authoritative ACTIVE, and stops receiving field-charge travel in recovery;
- line-displacement travel remains on the committed facing rather than steering toward the player's new position;
- solid terrain constrains the charge through normal Minecraft entity collision instead of the actor teleporting through the obstacle;
- committed strike and arena pressure do not inherit the line-charge forward step;
- the diagnostic outline remains aligned with the committed attack facing and follows the actor during line travel;
- a later attack may choose a new facing from the then-nearest eligible player;
- multiplayer observations do not count unless a real two-client/dedicated or otherwise genuinely multiplayer session was run.

If the boss turns continuously during the execution, moves before ACTIVE, keeps receiving charge travel in recovery, passes through a solid obstacle, or the server hit result rotates away from the telegraphed lane, record video/player positions if possible. Those are field-harness movement/targeting/readability regressions, not requests to widen the hit volume or increase charge speed.

## Test C — ACTIVE-only damage and execution dedupe

1. Use Survival/Adventure mode with clearly visible health.
2. Move inside the current attack's provisional field shape.
3. Remain inside it through one complete attack execution.
4. Observe health over telegraph, ACTIVE and recovery.
5. Repeat several attacks.

Expected:

- telegraph alone causes no damage;
- recovery causes no new damage;
- the target can be damaged during ACTIVE;
- one attack execution cannot repeatedly damage the same target every ACTIVE tick;
- current diagnostic damage is `1.0F` before armor/effect handling and is not a final balance decision;
- the line-charge actor movement does not create a second independent damage clock or bypass per-execution target dedupe;
- the particle phase changes are readability hints only; if particle timing and actual damage disagree, record the exact mismatch rather than treating particles as authority.

If repeated damage occurs within one execution, record the attack/presentation phase and approximate server tick; that is a regression and should be fixed before tuning visuals.

## Test D — physical role contrast, charge travel and visual boundary agreement

Use the semantic/presentation observation tools already available in the field harness to identify which attack is executing, then deliberately probe the edge of each provisional shape. The diagnostic particle outline should make the provisional boundary visible, but the server damage result remains authoritative.

Expected:

- `region_01_committed_strike`: a target in front and close to the boss can be hit; a similarly close target clearly behind the boss cannot be hit; the usable forward area is broader than the line-displacement lane; its particle outline should remain entirely inside the same forward arc envelope used by the server resolver; the boss itself does not receive line-charge travel;
- `region_01_line_displacement`: the forward lane reaches farther than committed strike, but a target standing clearly to either side of the narrow lane is not hit; the two particle side rails should mark the same `halfWidth` boundary used by the server profile; during ACTIVE the boss physically advances along that same committed lane at the provisional 0.5-block-per-tick calibration, so later ACTIVE samples originate from the advanced actor position;
- `region_01_arena_pressure`: phase 2 only; nearby targets around the boss can be hit regardless of facing, while targets clearly outside the local pressure radius are not hit; the particle ring should remain facing-independent and use the exact provisional profile radius; the boss does not receive line-charge travel;
- all three shapes still obey the same authoritative ACTIVE-only and once-per-execution damage rules from Test C.

Record whether the contrast is immediately understandable in motion. For line displacement, specifically record whether physical travel makes the authored `read_travel_lane -> move_laterally -> punish_recovery` sequence more legible or instead creates unfair contact/camera/collision behavior. Do **not** change the 0.5 step merely because another value sounds better; capture video/position evidence first.

If a particle boundary says one thing while hit/miss behavior says another, capture player/boss positions or video: that is an overlay/server-projection regression, not a balance-tuning prompt. If both agree but a miss/hit still feels surprising, capture the same evidence before changing dimensions by intuition.

## Test E — phase composition

1. Spawn a fresh field-test boss and observe phase 1 for several complete attacks.
2. Run `/riftfrontier boss fieldtest phase2`.
3. Keep the same actor alive and loaded for several more attacks.
4. Optionally run `/riftfrontier boss fieldtest phase1` and repeat.

Expected:

- phase switching is server-authoritative and does not require replacing the entity;
- an in-flight attack is cancelled by the authoritative phase transition before the new phase pool begins;
- phase 1 may select only committed strike and line displacement;
- phase 2 may select committed strike, line displacement and arena pressure.

Because final animation/VFX/sound bindings are not published yet, this test verifies semantic/runtime composition and physical role contrast rather than final human readability of presentation assets.

## Test F — content reload continuity

1. Spawn an enabled field-test boss.
2. Let at least one attack begin.
3. Execute `/reload` while the actor remains loaded.
4. Continue observing the same actor after the reload completes.

Expected:

- the retained old validated runtime is retired when its published generation becomes stale;
- the entity rebuilds its field-test runtime from the newly published content snapshot;
- no stale-generation runtime resumes damage or charge movement after reload;
- the actor remains usable for subsequent field-test attacks.

## Test G — player weapon versus boss actor

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
- no conclusion about final boss health, hitbox, weapon balance, charge speed or encounter difficulty is valid from this harness alone.

## Evidence to record

For each human session record:

- exact JAR file name and SHA-256;
- singleplayer/integrated server or dedicated multiplayer;
- Minecraft/NeoForge versions;
- commands used;
- whether Tests A–G were attempted;
- PASS/FAIL per expected observation;
- screenshots/video for any visual, movement or timing issue where practical;
- exact symptom for any target-acquisition error, mid-attack homing, charge-before-ACTIVE, charge-after-ACTIVE, collision tunnelling, duplicate damage, stale runtime, phase-pool escape, particle/server boundary disagreement, surprising shape result, crash or desync.

Only after a real human session may the corresponding checkpoint be labeled `PLAYTESTED`. A real two-client/dedicated-session observation is required for `MULTIPLAYER TESTED`.
