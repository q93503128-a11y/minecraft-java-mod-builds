# Open-World RPG — R01 Backend Integrity Pass 14

> Date: 2026-10-03
>
> Scope: R01 ordinary Smithing + fast-travel server backend; no new visual design and no candidate
> spatial coordinate promotion.

## 1. Ordinary R01 Smithing backend

Code state `6b530f5532822702de9f5d32060893cd08cde33b` closes the twelve canon-locked
ordinary Holt Forge recipes:

- six Refined recipes;
- six Superior recipes;
- exact recipe material/Gold costs from canon;
- canonical fixed identity affix first, with the remaining affix slots rolled through the existing
  ordinary-equipment affix pipeline;
- canonical Refined/Superior Item Lv and normal equipment materialization;
- equipment sell-back value derived from the existing standard merchant price at the global 25%
  ordinary-equipment sell-back rule;
- Backpack capacity check before a normal craft commit;
- Pouch -> Vault material consumption through the existing idempotent inventory authority;
- Gold debit through the existing idempotent currency authority;
- persisted pending output so reconnect cannot reroll the crafted equipment;
- recovery fallback through Backpack -> Personal Storage -> Pending Claim after a committed
  transaction;
- Smithing Insight awards for first Refined, second distinct Refined base, and first Superior craft;
- Superior recipes remain locked until the player has actually encountered Verdant Crystal.

The Verdant Crystal unlock is source-neutral and permanent. Direct gathering, Nature Spirit reward
delivery, existing Pouch/Vault ownership, or the existing gathering discovery flag can establish the
same Smithing unlock. This prevents the Nature Spirit reward path from becoming a hidden progression
dead end.

Mythic/signature crafting and reforge remain separate systems; this pass does not approximate them
with the ordinary forge transaction.

Build Openworld RPG run `37122629844` / #328 is **SUCCESS** for
`6b530f5532822702de9f5d32060893cd08cde33b`. Artifact `11274456411` has
workflow-artifact digest
`sha256:821ee751afc52b25d6d170b77ba775bdeb7c9df3b7c71bfe93487d7f0509bb59`.

## 2. R01 fast-travel backend

Final code state `bcd4e41652b1eddc289aa4973bbeef6932f73984` adds the server-owned
node-to-node travel transaction without promoting any current Azari review coordinate.

Backend contract:

- the client supplies node ids only; it cannot supply destination coordinates;
- actual coordinates live only in `R01FastTravelNodeRegistry` accepted production bindings;
- the current R01 candidate spatial dataset is never read by the travel runtime;
- R01 travel is Overworld-only;
- Alderford Gate Shrine access uses the existing personal first-shrine activation;
- Quarry Waystone access uses the existing personal Waystone activation;
- origin interaction requires an exact <=6 block 3D distance;
- travel channel is exactly 20 ticks / 1.0 s;
- baseline Gold cost and cooldown are both zero;
- active combat rejects initiation;
- Downed and project-mount state are explicit authoritative caller gates, while vanilla/passenger
  mounted state is independently rejected server-side;
- an incompatible committed project action rejects travel through the shared action authority;
- entering combat/taking hostile combat activity during the channel cancels the travel;
- destination activation is revalidated at commit time;
- the server synchronously loads the destination chunk and validates player collision before
  teleporting;
- the primary authored arrival is tried first, then at most four authored fallback arrivals;
- every arrival also needs a supporting collision surface beneath the player;
- if every authored arrival is blocked, the transaction aborts and the player remains at origin;
- the transaction is personal and contains no party-drag behavior;
- respawn/disconnect clears transient travel state.

The production node registry is intentionally empty today because Alderford/Quarry spatial bindings
remain candidate-only. Therefore this pass closes the **travel rules/authority backend**, not the
final spatial binding or player-facing map/shrine invocation.

The first CI attempt (#329) reached compiled product code but failed a new unit fixture because
`Level.OVERWORLD` triggered Minecraft registry initialization outside the game bootstrap. R01 travel
is already canonically Overworld-only, so the redundant per-node dimension field was removed. The
next attempt (#330) exposed only the stale fixture arguments left by that test-only change. No
gameplay rule was changed by either repair.

Build Openworld RPG run `37123766645` / #331 is **SUCCESS** for
`bcd4e41652b1eddc289aa4973bbeef6932f73984`:

- clean tests/build: PASS;
- pinned dependency/creature inspection: PASS;
- core-profile dedicated server smoke: PASS;
- gameplay-profile dependency server smoke: PASS;
- gameplay-profile client startup smoke: PASS;
- verification JARs: PRODUCED;
- M0 mrpack: PRODUCED;
- normal JAR SHA-256:
  `ffc39e83dd6e6f9e185b18f8665740cf04fbb1796a666ccbc91a93108d73988b`;
- mrpack SHA-256:
  `04d2483b3ca0f09a477de75bd483ec1e3ee37f711bd5805d8ea165e962ea16b4`;
- workflow artifact: `11273673942`;
- workflow-artifact digest:
  `sha256:e012b187b2bacda094a200b5bae221f136ac50a71fbf9520d3a44996826ec226`.

## 3. Backend work still genuinely open

This pass does not label the whole game backend complete.

Known backend/runtime gaps that remain separate include:

- accepted production spatial binding and invocation transport for Fast Travel;
- final Camp spatial sampling / physical interaction / player-facing presentation bindings;
- mount pre-code asset/donor/multiplayer/traversal verification, then runtime/ownership/summon authority;
- Louxia combat-reward progression budget and Meadow Viper project actor/entity binding before their
  ordinary personal reward paths can be admitted;
- remaining class-Insight success detectors whose encounter/action evidence is not yet bound;
- remaining signature/reward materialization gates already documented for bosses/equipment;
- gameplay systems that are intentionally blocked by unresolved presentation/spatial canon.

Those should be closed in meaningful units rather than by inventing candidate coordinates,
placeholder actors, fake APIs or guessed reward contracts.

## Validation state

```text
CODE REVIEWED: YES
TESTED: YES
BUILD VERIFIED: YES
JAR PRODUCED: YES
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```


## 4. R01 ordinary-enemy personal reward authority follow-up

Final code state `8ea0eb99dbbc8a96436ea2a7a52fa524fb4a5bae` closes the currently canon-bindable ordinary/elite personal reward
transaction layer for authored **Cave Centipede, Bison, Grizzly and Steelboar** without promoting any
of those actors to production spawning.

Server authority now provides:

- one accepted project-damage contribution or one already-successful project control/debuff action
  grants personal eligibility; party membership, proximity, last hit and damage share do not;
- the class active on the first qualifying contribution is persisted as that player's Class-XP owner;
- Cave Centipede / Bison / Grizzly use the global appropriate-level common/sturdy targets
  (**~1% Combat EXP / ~0.8% Class XP**) with their exact R01 Gold/material rules;
- Steelboar uses the exact elite package (**~6% Combat EXP / ~5% Class XP / 18 Gold**, 60% Iron Ore
  1-3, 35% Tough Hide x1 and 30% one ordinary equipment roll);
- each online eligible player receives an independent server RNG plan at canonical project-HP death;
- ordinary rewards are not queued indefinitely for a qualified player who is offline at kill
  resolution, matching the multiplayer canon; once an online plan is committed, interrupted
  progression/material delivery remains reconnect-safe and idempotent;
- the Cave Centipede donor Leg remains excluded;
- project-owned external-actor vanilla/donor EXP and admitted project-loot-source death loot are
  suppressed so project rewards do not duplicate donor rewards;
- Steelboar equipment base / grade / Item Lv / armor slot / affix seed are committed before delivery,
  but physical ordinary drop materialization/presentation stays pending instead of inventing a
  missing player-facing contract;
- max Combat Lv + max qualifying Class Rank + zero-Gold sources do not attempt to create an illegal
  empty progression transaction; material resolution can still complete;
- Louxia is intentionally **not** admitted into this combat reward runtime because its exact
  Combat/Class XP budget is not closed in current canon; its registry target remains verified and its
  donor loot is not suppressed by this new ordinary-reward gate;
- Meadow Viper remains outside this runtime because its project-owned entity/project-HP binding is
  still not implemented.

The first implementation CI (#334) compiled successfully but exposed one stale test that still
classified authored Grizzly as unrelated to control participation. After the test contract was
updated, #336 reached server startup and exposed a Minecraft 26.2 mixin target rename; the death-loot
hook was corrected to `LivingEntity.dropAllDeathLoot(ServerLevel, DamageSource)`. A later one-line
Louxia loot-boundary correction initially removed Louxia from the wrong adjacent registry set; that
patch-location error was repaired before this final state.

Build Openworld RPG **#339**, run `37162201830`, is **SUCCESS** for `8ea0eb99dbbc8a96436ea2a7a52fa524fb4a5bae` across clean
tests/build, pinned gameplay/creature metadata inspection, R01 creature registry inspection,
Earthloong dependency inspection, core-profile dedicated-server smoke, gameplay-profile dependency
server smoke, gameplay client startup smoke, both verification JAR builds, Modrinth playtest-pack
packaging and artifact upload.

Normal JAR SHA-256:
`fea40cddb334a92b06ce8fdb87e4c0d0a1532e9f2995366579bcdc6d6b5c2935`

M0 mrpack SHA-256:
`33ddeaefd532c874b376187398fe92e33070fbaee5a4a9b9716242e0c7393474`

Workflow artifact:
`openworld-rpg-m0-8ea0eb99dbbc8a96436ea2a7a52fa524fb4a5bae` / ID `11288072772`

Workflow-artifact digest:
`sha256:85bf7609abc358b86394816acd7abc324324541b50323f3af3990314b36b158c`

Validation state for this pass:

```text
CODE REVIEWED: YES
TESTED: YES
BUILD VERIFIED: YES
JAR PRODUCED: YES
PRODUCTION SPAWN PROMOTION: NO
STEELBOAR EQUIPMENT PHYSICAL DELIVERY: GATED
LOUXIA ORDINARY COMBAT REWARD: NOT IMPLEMENTED
MEADOW VIPER ORDINARY COMBAT REWARD: NOT IMPLEMENTED
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
