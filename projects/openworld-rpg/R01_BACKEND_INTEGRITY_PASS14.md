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
- Camp runtime/placement/rest ownership;
- mount runtime/ownership/summon/traversal authority;
- generic ordinary R01 enemy personal reward transactions where encounter-specific reward services
  do not yet exist;
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
