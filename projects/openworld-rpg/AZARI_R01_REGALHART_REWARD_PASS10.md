# Open-World RPG — R01 Regalhart Personal Reward Pass 10

> Status: **PERSONAL FIRST/REPEAT REWARD AUTHORITY BUILD VERIFIED / EQUIPMENT DELIVERY + LIVE PRODUCTION SPAWN STILL GATED**
>
> Date: 2026-10-03
>
> Code state: `3a7d8af52c1bbf12f1fd97c617829fa1e5c69555`

## 1. Participation ownership

Regalhart now uses the same canonical co-op rule as the rest of R01 combat rewards:

- one server-accepted damaging hit is enough;
- one real encounter-linked heal that restores missing HP is enough;
- one positive encounter-linked barrier to another engaged player is enough;
- one successfully applied project control/debuff is enough;
- proximity, party membership and last hit do not own rewards;
- the root class active on that player's **first valid contribution** is persisted as the Class-XP
  owner for that encounter and cannot be swapped at defeat time.

Damage contribution is connected through `ProjectMinecraftDamageApplicator`.
Heal/barrier entry points now accept the actual Regalhart encounter actor, and the existing successful
control bridge recognizes only exact project-authored `threateningly_mobs:the_regalhart`.

## 2. Durable personal reward planning

Regalhart reward state is a persistent server-world attachment containing:

- active encounter instance -> participant UUID / first-contribution class;
- encounter cycle index;
- pending personal reward finalizations;
- the set of player UUIDs whose first eligible Regalhart defeat has already been committed.

On defeat, the exact eligible participant set is snapshotted and every player's loot result is rolled
and stored **before any delivery is attempted**. Only after those personal plans are durable does the
shared Regalhart territory controller record the valid defeat and advance the world cycle.

A disconnect therefore cannot reroll equipment grade/base/Item Lv, the optional second repeat item or
the 15% signature result. A player who earned a first-defeat plan but has not yet received all blocked
items also cannot earn a second first-defeat plan.

## 3. First eligible defeat package

For each eligible player independently:

- Combat EXP: **20%** of that player's current next-Lv requirement, with the Lv8 combat anti-farm
  modifier;
- Class XP: **15%** of the stored first-contribution class's current Class-Rank requirement, using
  the same combat modifier and normal late-class catch-up;
- Gold: **70**;
- one guaranteed normal-equipment result from the equal-weight Regalhart pool:
  - River Pike;
  - Riverwood Bow;
  - Wayfarer Daggers;
  - Wayfarer Leathers;
  - Greenwater Pendant;
  - Wayfarer's Token;
- that guaranteed item is **Superior+** using the canon field-boss first-clear 40:15
  Superior:Exalted weight;
- ordinary Item Lv is source Lv8 plus the global -2/-1/0/+1/+2 distribution;
- Wayfarer Leathers resolves one of the five armor slots at equal weight;
- Regalhart Antler ×**2**;
- independent **15%** Hartcrown Spear signature result.

There is no invented curated three-choice first-clear reward because Regalhart is the optional field
boss, not the R01 quarry's major first-dungeon milestone.

## 4. Repeat eligible defeat package

For each eligible player independently:

- Combat EXP: **7%** current next-Lv requirement;
- Class XP: **6%** current Class-Rank requirement;
- Gold: **35**;
- one guaranteed normal-equipment result from the same six-family pool;
- **25%** chance for a second independent normal-equipment result;
- normal field-boss grade table:
  - Standard 10%;
  - Refined 35%;
  - Superior 40%;
  - Exalted 15%;
- normal source-Lv8 ±2 Item-Lv distribution;
- Regalhart Antler ×**1**;
- independent **15%** Hartcrown Spear signature result.

Discovery and first-defeat bonuses do not repeat.

## 5. Delivery state

Currently live and reconnect-safe:

- Combat EXP;
- Class XP;
- Gold;
- Regalhart Antler into the Material Pouch.

Those use existing idempotent transaction/receipt services. Material-pouch capacity blocking leaves
the earned result pending rather than deleting it.

Currently pre-rolled/persisted but intentionally **not materialized**:

- guaranteed normal equipment;
- optional second repeat equipment;
- successful Hartcrown Spear roll.

The ordinary-equipment path still lacks the shared final sell/materialization value contract already
identified by the Nature Spirit reward pass. The Hartcrown Spear additionally requires its authored
Hart's Momentum runtime and accepted external item presentation/materialization. No placeholder item,
zero-value fake equipment or silent omission is used.

## 6. Production spawn remains closed

This reward pass does not flip
`R01ExternalActorCatalog.productionSpawnReady(REGALHART)`.

Live field-boss admission still waits for the remaining encounter/presentation contracts, including:

- Crown Charge ordinary guardability;
- Rear Kick guard pressure / player-poise pressure;
- mirrored Antler Sweep second-hit exact timing;
- exact 7.0 / 12.0 weighted-selection endpoint ownership;
- weak-point head/antler hit geometry;
- accepted physical attack movement / presentation;
- real camera/occlusion caller;
- blocked equipment/Hartcrown physical reward delivery.

## 7. Verification

Build Openworld RPG run `37116118675` / #321: **SUCCESS**.

Passed:

- clean unit tests / build;
- Regalhart first/repeat reward-rule boundary tests;
- persistent reward-state codec / first-defeat tests;
- control-support Regalhart registry/authored-spawn test;
- pinned gameplay/creature dependency verification;
- R01 creature registry byte inspection;
- Earthloong dependency inspection;
- bootstrap JAR;
- core-profile dedicated-server smoke;
- gameplay-profile dependency-server smoke;
- gameplay-profile client startup smoke;
- both verification JARs;
- Modrinth playtest-pack packaging;
- artifact upload.

Artifact:
`openworld-rpg-m0-3a7d8af52c1bbf12f1fd97c617829fa1e5c69555`
(`11271951185`)

Normal JAR SHA-256:
`05afbeff79729bd02270e9a67476a8476c189f86bb2ccb2d5e3de5887561582e`

```text
REGALHART PERSONAL PARTICIPATION AUTHORITY: YES
REGALHART FIRST/REPEAT RNG PLAN: YES
REGALHART EXP / CLASS XP / GOLD DELIVERY: YES
REGALHART ANTLER DELIVERY: YES
REGALHART NORMAL EQUIPMENT PLAN PERSISTED: YES
REGALHART NORMAL EQUIPMENT PHYSICAL DELIVERY: NO
HARTCROWN 15% RESULT PERSISTED: YES
HARTCROWN PHYSICAL DELIVERY/RUNTIME: NO
REGALHART LIVE PRODUCTION SPAWN: NO
BUILD VERIFIED: YES
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
