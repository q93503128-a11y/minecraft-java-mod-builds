# Open-World RPG — Critical Runtime Pass 21

> Status: **BASIC-ATTACK CRITICAL CHANCE / CRITICAL DAMAGE AUTHORITY IMPLEMENTED**
>
> Date: 2026-09-28

## Implemented

Project-owned ordinary weapon basic hits now resolve critical hits from server authority rather than donor damage state.

Canonical inputs:

- Base Critical Chance: 5%;
- DEX: +0.08 percentage points per point above 5, capped at +6 points;
- equipped Critical Chance affixes: additive, canonical gear contribution capped at +30 points;
- normal persistent total Critical Chance cap: 60%;
- Base Critical Multiplier: 1.50x;
- ordinary Critical Damage affixes add directly to that multiplier;
- normal persistent Critical Damage cap: 2.25x.

The server supplies the random roll. Donor melee/projectile proposed damage still does not become project damage authority.

Implemented paths:

- Better Combat project-owned melee basic hits;
- project-authoritative ranged projectile basics;
- bow basics using the already-authoritative captured draw power.

A critical hit changes direct HP damage only. It does not multiply poise damage.

## Affix state

The static ordinary-affix catalog remains 29 entries.

```text
LIVE RUNTIME-ADAPTED STATIC AFFIXES: 20
CRITICAL CHANCE: IMPLEMENTED
CRITICAL DAMAGE: IMPLEMENTED
CRITICAL AFFIX AUTHORITY READY: YES
```

## Deliberate gates

This pass does **not** invent weak-point anatomy or hit zones.

Regalhart's head/antler weak point and Earthloong's luminous head/crest window remain dependent on their accepted model/contact geometry. The existing canonical weak-point multipliers stay data/design authority, but runtime hit-zone detection is not fabricated.

Authored skills also do not automatically inherit critical hits. Their attack data must explicitly allow criticals as required by `COMBAT_BALANCE.md`.

Still gated ordinary-affix consumers include Attack Speed, weak-point damage, movement, healing/support, dodge/sprint cost reduction, Ultimate charge, negative-status duration reduction and potion/food effect strength.

```text
BASIC MELEE CRITICAL AUTHORITY: IMPLEMENTED
BASIC RANGED CRITICAL AUTHORITY: IMPLEMENTED
SERVER CRITICAL RNG: IMPLEMENTED
CRITICAL CHANCE AFFIX: IMPLEMENTED
CRITICAL DAMAGE AFFIX: IMPLEMENTED

MODEL-SPECIFIC WEAK-POINT HIT DETECTION: NO
ATTACK SPEED RUNTIME: NO
ALL ORDINARY AFFIX RUNTIMES: NO
R01 PLAYTESTED: NO
R01 MULTIPLAYER TESTED: NO
```

## Verification

GitHub Actions run `36382743863`, code state
`e8f017b943d3f41dbfb588b43e0ac334c4bd54ca`:

```text
UNIT TESTS: PASS
CLEAN BUILD / JAR: PASS
CORE DEDICATED SERVER SMOKE: PASS
GAMEPLAY DEPENDENCY SERVER SMOKE: PASS
GAMEPLAY CLIENT STARTUP SMOKE: PASS
R01 INTEGRATION PLAYTEST JAR BUILD: PASS

REAL PLAYER CRITICAL-HIT FEEL/PRESENTATION: NOT PLAYTESTED
WEAK-POINT HIT-ZONE ACCEPTANCE: NOT TESTED
MULTIPLAYER TESTED: NO
```
