# M3 Player Combat Field-Play Gate

Status: **BUILD VERIFIED / HUMAN FIELD PLAY REQUIRED**

This is the bounded human validation gate for the first production player-weapon slice. It exists because the server-authoritative move pipeline, provisioning path and provisional field-impact geometry are now executable, but CI cannot decide whether the combat actually reads and feels correct in Minecraft.

This document does not approve final keys, weapon art, hit geometry, damage, timing, VFX or sound. It defines exactly what must be observed before any of those values are tuned or presentation is promoted.

## Verified build under test

Use the deliverable produced by GitHub Actions `Build Riftfrontier` run `34739314980` for code commit:

`2d94c54cfddcb8d81d0ae7567a4d90adb0cd0dca`

Verified executable JAR:

`riftfrontier-0.1.0-alpha.1.jar`

JAR SHA-256:

`310257a14a6e8d7c990aa21fb2bff314dd0cfea3234825c5379a1912fb80b8d1`

GitHub Actions deliverable artifact:

`riftfrontier-0.1.0-alpha.1-deliverables` from run `34739314980`.

The CI baseline passed:

- Java 25 / Gradle 9.2.1 toolchain check;
- asset-intake tests;
- `clean test build`;
- all 9 required native GameTests;
- dedicated-server smoke;
- Xvfb client initialization smoke;
- executable-JAR inspection;
- exact selected Region 01 boss glTF integrity check;
- deliverable/report upload.

These results are BUILD VERIFIED and JAR PRODUCED. They are not PLAYTESTED.

## Current player-combat calibration under review

These are diagnostic field-play values, not final balance:

| Move | Geometry | Reach | Half width | Diagnostic damage |
|---|---|---:|---:|---:|
| `mobile_pressure_entry` | forward arc | 2.4 | 1.45 | 1.0 |
| `mobile_pressure_finisher` | forward lane | 2.9 | 1.10 | 1.0 |
| `reach_commitment_strike` | forward lane | 4.0 | 0.85 | 1.0 |

Authoritative attack clocks remain:

| Move | Telegraph | ACTIVE | Recovery |
|---|---:|---:|---:|
| `mobile_pressure_entry` | 3 ticks | 2 ticks | 3 ticks |
| `mobile_pressure_finisher` | 5 ticks | 2 ticks | 7 ticks |
| `reach_commitment_strike` | 7 ticks | 3 ticks | 8 ticks |

Do not tune any number in these tables merely because it looks unusual. Record an observed gameplay symptom first.

## One-time test setup

1. Install the verified JAR above in a Minecraft 26.2 + NeoForge 26.2.0.38-beta test instance.
2. Open a world with commands enabled.
3. In Minecraft Controls, find the Riftfrontier combat category and assign temporary test keys to:
   - Weapon Action 1
   - Weapon Action 2
4. The project intentionally ships these actions unbound. The temporary test bindings are not a final control-layout decision.
5. Use Creative mode for repeatable geometry checks so hostile AI does not disturb target placement:

```mcfunction
/gamemode creative
```

## Provisioning commands

Use only the supported server-owned loadout path:

```mcfunction
/riftfrontier weapon mobile
/riftfrontier weapon mobile pivot
/riftfrontier weapon reach
/riftfrontier weapon reach pivot
```

The issued iron sword is a temporary carrier for the authoritative `riftfrontier:player_weapon_loadout` component. Do not evaluate its vanilla model as final Riftfrontier weapon art.

For `mobile_pressure`:

- Weapon Action 1 = ordinary mobile entry.
- Weapon Action 2 = committed finisher.

For `reach_commitment`:

- Weapon Action 1 = reach commitment strike.
- Weapon Action 2 has no authored second move and should not start an attack.

## Controlled target setup

Use a fresh no-AI zombie for each isolated check. Stand still and look horizontally before summoning so local `^` coordinates are meaningful.

Example target directly ahead:

```mcfunction
/summon minecraft:zombie ^ ^ ^2 {NoAI:1b,Silent:1b,PersistenceRequired:1b}
```

Before and after an attack, the nearest zombie health can be inspected with:

```mcfunction
/data get entity @e[type=minecraft:zombie,sort=nearest,limit=1] Health
```

If the exact command grammar differs in the active 26.2 client, use Minecraft's command suggestions to select the nearest zombie and inspect the same `Health` field. Do not substitute guessed damage from visual hurt animation alone when verifying once-per-execution delivery.

Remove the previous target before repositioning:

```mcfunction
/kill @e[type=minecraft:zombie,distance=..8]
```

## Pass A — basic delivery and deduplication

1. Provision mobile pressure.
2. Summon one zombie roughly 2 blocks straight ahead.
3. Record its health.
4. Press Weapon Action 1 exactly once.
5. Wait until the full attack has completed.
6. Read health again.

Expected mechanical result:

- target loses exactly `1.0` health from the Riftfrontier execution;
- a multi-tick ACTIVE window does not deal the diagnostic hit multiple times to the same target;
- holding or repeatedly pressing the key is not part of this check: use one discrete action intent.

Failure examples to record verbatim:

- no damage at all;
- more than 1.0 damage from one execution;
- target is damaged twice during the same ACTIVE window;
- attack starts while the Riftfrontier carrier is not in the server-recognized main hand.

## Pass B — facing direction

1. Provision mobile pressure.
2. Place a target about 1.5 blocks behind the player:

```mcfunction
/summon minecraft:zombie ^ ^ ^-1.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}
```

3. Without turning around, press Weapon Action 1 once.

Expected result: the target behind the player is not hit.

Then turn to face it and repeat with a fresh target. Expected result: the same close target becomes hittable when it is in front.

This checks readable directional authority. It does not approve the final arc width.

## Pass C — family reach contrast

Use a fresh target around 3.3 blocks directly ahead. This is intentionally separated from the exact profile edges so target bounding-box size is less likely to make the comparison ambiguous.

```mcfunction
/summon minecraft:zombie ^ ^ ^3.3 {NoAI:1b,Silent:1b,PersistenceRequired:1b}
```

Test in this order with fresh targets:

1. `/riftfrontier weapon mobile` → Weapon Action 1.
2. `/riftfrontier weapon mobile` → Weapon Action 2.
3. `/riftfrontier weapon reach` → Weapon Action 1.

Expected role-level result:

- mobile entry is materially the shortest practical engagement tool;
- mobile finisher remains close/committed rather than replacing the reach family;
- reach commitment can connect from a spacing band where mobile pressure cannot reliably connect.

Do not fail the gate because an exact decimal edge differs by entity bounding-box interaction. Fail it if the two families are not meaningfully distinguishable in practical positioning.

## Pass D — lane width / lateral miss

Provision reach commitment. Place a zombie forward and materially offset sideways, for example:

```mcfunction
/summon minecraft:zombie ^1.6 ^ ^2.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b}
```

Press Weapon Action 1 without turning toward the zombie.

Expected result: the narrow reach lane should not behave like a 360-degree or huge-area attack. A clearly side-offset target should miss.

Repeat after facing the target. Expected result: the same target should become hittable when the attack lane is actually aimed at it.

Record the observed position if this feels overly strict or overly generous; do not change width from intuition alone.

## Pass E — authoritative telegraph and no instant hit

The player moves deliberately have nonzero telegraph windows. With a fresh target in valid geometry:

1. record target health;
2. press one combat action;
3. watch whether damage occurs immediately on key press or after the authored anticipation;
4. wait through recovery and inspect final health.

Expected result:

- damage is not applied at key-press time before the ACTIVE window;
- the target takes at most one diagnostic hit during the execution;
- no second damage event appears during recovery.

For human review, the key symptom is whether the delayed hit feels causally connected to the action rather than whether a human can count individual 50 ms server ticks by eye. Automated AttackStateMachine/GameTest coverage remains the exact tick-level authority check.

## Pass F — loadout swap / unequip cancellation

Use the longest telegraph to make the cancellation easy to perform:

1. `/riftfrontier weapon reach`
2. place a valid target roughly 2.5–3 blocks ahead;
3. record target health;
4. press Weapon Action 1;
5. immediately switch to an empty hotbar slot before the ACTIVE window;
6. wait past the original attack duration;
7. inspect target health.

Expected result: the target remains undamaged because the server-owned main-hand loadout changed before impact.

Repeat by replacing reach with mobile before the old reach attack becomes ACTIVE. Expected result: the old execution is invalidated; it must not complete under the successor loadout.

## Pass G — authored action-slot behavior

With mobile pressure equipped:

- Action 1 should start mobile entry.
- Action 2 should start the committed finisher.

With reach commitment equipped:

- Action 1 should start the reach strike.
- Action 2 should not fabricate an unauthored second move.

A client message, extra attack or fallback vanilla action created for reach Action 2 is a failure. Silence/no attack is correct for the current one-move family.

## Optional integration pass — Region 01

Only after the isolated checks above are understandable, verify that the same loadout remains functional inside the existing expedition loop:

```mcfunction
/riftfrontier expedition start
/riftfrontier expedition review
```

Use the weapon against the current M2 technical hunter/scout/elite proxies, recover salvage and inspect the expedition state. The current 1.0 diagnostic weapon damage is deliberately not tuned for clearing speed, so do not score combat balance from this integration pass yet.

Do not mistake Zombie/Skeleton/Ravager proxy art or the iron-sword carrier for production presentation.

## Evidence to report back

For each failed or questionable pass, record:

- weapon command used;
- action slot used;
- approximate target position/distance;
- health before/after where relevant;
- what was expected;
- what actually happened;
- whether the symptom reproduced at least twice;
- screenshot/video when the issue is primarily visual or timing-related.

Useful symptom wording includes:

- `mobile entry feels like it reaches behind the player`;
- `reach strike hits targets far outside the visible lane`;
- `one action causes two health drops`;
- `damage happens immediately before any readable anticipation`;
- `switching off the weapon does not cancel the pending hit`;
- `mobile and reach engagement bands feel indistinguishable`.

Do not report only `weak`, `slow` or `bad range` when a more concrete spatial/timing symptom can be captured.

## Gate decision

The field-impact implementation is allowed to move from diagnostic calibration toward production policy only after this pass supplies actual observations.

If the mechanics pass but the attack feels visually disconnected, the next task is presentation/hit feedback using reference-backed animation/VFX/sound, not hidden range inflation or instant damage.

If geometry itself fails, fix the observed shape/positioning problem first, then repeat the affected pass. Repeated numeric tuning should be migrated out of the temporary hard-coded calibration class into the proper data-driven production policy.

Until a human completes this gate:

- `PLAYTESTED`: **NO**
- `MULTIPLAYER TESTED`: **NO**
- final geometry/damage/timing: **NOT APPROVED**
- final player weapon art/VFX/sound: **NOT APPROVED**
