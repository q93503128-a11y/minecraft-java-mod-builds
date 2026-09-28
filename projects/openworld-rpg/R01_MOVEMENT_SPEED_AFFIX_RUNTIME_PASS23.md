# R01 Movement Speed Affix Runtime — Pass 23

## Scope

This pass closes only the ordinary-equipment **Movement Speed** runtime consumer.

It does **not** reopen equipment-pool design, invent an armor movement penalty, or mark Nessa equipment purchase delivery complete.

Canonical rules retained:

- ordinary Movement Speed roll: **+1.0% .. +3.5%**;
- equipment contribution cap: **+15%**;
- Heavy armor has **no implicit movement-speed penalty**;
- project equipment/loadout remains server authoritative.

## Runtime binding

`openworld_rpg:affix/movement_speed` is now `implemented`.

The generated affix projects to `EquipmentCombatAffixKind.MOVEMENT_SPEED`.
All equipped Movement Speed affixes aggregate across slots and clamp at the canonical +15% equipment cap.

The effective equipment bonus is projected by the server onto Minecraft's `MOVEMENT_SPEED` attribute through one stable transient `ADD_MULTIPLIED_BASE` modifier.

The player base movement value is not overwritten.

Reprojection occurs through the existing combat-build refresh path on equipment/progression refresh and join. Respawn now refreshes the authoritative combat build so transient player attributes are restored after death.

## Nessa / materialization effect

Movement Speed no longer appears as a runtime blocker for R01 ordinary-equipment materialization.

The materializer still fails closed when another eligible affix is not implemented. Pool odds are not changed and unsupported affixes are not silently filtered out.

Static ordinary-affix runtime state after this pass:

```text
CANONICAL STATIC AFFIXES: 29
RUNTIME IMPLEMENTED: 21
RUNTIME STORED_ONLY: 8
```

Remaining static runtime consumers:

- Attack Speed
- weak-point damage
- dodge/sprint Stamina cost reduction
- Ultimate charge gain
- Healing Done
- Healing Received
- negative-status duration reduction
- potion/food effect strength

Parameterized element/status output remains a separate profile/runtime binding.

## Verification

Final code state:

`c114fa3bc14d478abd3594528ac3e526054ba1fc`

GitHub Actions:

`Build Openworld RPG` run `36387698342`

```text
UNIT TESTS: PASS
CLEAN BUILD / JAR: PASS
CORE DEDICATED SERVER SMOKE: PASS
GAMEPLAY DEPENDENCY SERVER SMOKE: PASS
GAMEPLAY CLIENT STARTUP SMOKE: PASS
JVM-FREE JOINED-PLAYER PLAYTEST JAR BUILD: PASS
R01 INTEGRATION PLAYTEST JAR BUILD: PASS
M0 MODRINTH PLAYTEST PACK: PASS

CODE REVIEWED: YES
TESTED: YES
BUILD VERIFIED: YES
JAR PRODUCED: YES
CORE SERVER TESTED: YES
GAMEPLAY DEPENDENCY SERVER TESTED: YES
CLIENT RUNTIME TESTED: YES

MOVEMENT-SPEED GAMEPLAY FEEL PLAYTESTED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```

Produced Actions artifact:

`openworld-rpg-m0-c114fa3bc14d478abd3594528ac3e526054ba1fc`

## Production boundary

This pass is backend/runtime work only.

It does not promote unresolved equipment visuals, Alderford/Quarry candidate spatial data, NPC/enemy models, UI or other player-facing assets to production.
