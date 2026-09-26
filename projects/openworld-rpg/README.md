# Open-World RPG

Large Minecraft Java open-world action RPG project with very low dependence on vanilla progression systems.

## Current status

`M0 GAMEPLAY DEPENDENCY RUNTIME VERIFIED / R01 SERVER-AUTHORITY FOUNDATION IN PROGRESS / PLAYER-FACING WORLD-UI NOT YET IMPLEMENTED`

The latest verified technical state is workflow run `36233374352` at code state `1a78709685fbfa9d6c870562bba307c231408ecb`. Persistent combat Lv/EXP, independent per-root Class Rank/XP, Attributes, the canonical 12-slot equipped loadout, personal R01 progression, server-owned Gold, a four-slot Recovery Belt, a 36-slot Backpack, 36-slot Alderford Personal Storage, Material Pouch/Vault, Key Items, Pending Reward Claim, personal active-world time, and personal R01 gathering-node cooldown/tool/mastery/discovery state with reconnect-safe harvest delivery are now persisted server-side. Recovery Belt use now has server-owned 0.72 s resolution / 0.95 s action timing, 6.0 s shared lockout, Healing Potion and Focus Draught effect application, Cleansing Tonic minor-dispellable cleanup plus the canon-correct 10 s negative-buildup resistance, and pre-resolution cancellation on authored poise break. The normal R01 runtime idempotently projects the locked opening state: Heartland Arming Sword + Watch Buckler, one loaded Healing Potion and 150 Gold; first class selection has canonical Hunter/Bow, Cleric/Staff and Mage/Wand starter projections while Warrior/Guardian keep the opening sword+buckler. Better Combat melee, Spell Engine Arc Bolt and Bow projectile basics replace donor damage with project authority for project-owned targets. `Dust on the Quarry Road` now has semantic server-only action bridges, bounded class-attribution evidence and reconnect-safe completion into `Roots Below Stone`; Class XP follows the strict-majority objective-contribution class when one exists rather than being stealable by a last-second class swap. `Roadside Trouble` now has persistent shared cycle/lifecycle timing, participation, reconnect finalization and bounded repeat rewards at 5% combat EXP + 4% Class XP + 20 Gold, with Class XP tied to the class active on the qualifying contribution. Earthloong first-clear reward transactions now persist one immutable Superior Item Lv8 choice among Ironroot Longsword / Riverthorn Bow / Lumenwood Staff, their deterministic 85th-percentile affix payloads and equipment grade, use Backpack → Personal Storage → Pending Reward Claim for lossless item delivery, and retain the two-scale Material Pouch transaction receipt across reconnects. Earthloong's canonical project HP reaching zero now closes the shared encounter, preserves each player's first valid contribution class, moves eligible players into reconnect-safe pending finalization, grants the first boss reward layer once, and commits personal first-clear only for a player with an active personal quarry run; helpers cannot inherit story completion merely by joining the kill. Physical Azari event volumes, actor/node/marker placement, concrete support/heal/barrier/control/revive participation callers, whole-run majority-class dungeon completion attribution, the guaranteed Superior+ / 15% Mythic boss-gear rolls, Lucifer reward-choice UI, reward models/icons, post-quarry aftermath and final player-facing presentation remain unbound. A real joined-player playtest has already confirmed the authored Earthloong spawn path and Arc Bolt cast/release/impact path; broader R01 world/UI flow is still not playtested.

The major gameplay systems, Lv 1–80 progression, class/combat framework, equipment/economy, world-state model, main story spine, mounts, field systems, party/co-op rules and R01–R12 regional packages are already specified. **R01–R12 now all have concrete named-NPC, exact quest-condition, scene/dialogue, reconnect/reward-state and story-evidence authoring locked.** Remaining pre-bootstrap work is no longer regional content invention. It is primarily exact external-asset binding, actual Azari spatial placement/travel-time validation and final stale-canon cleanup. Accessibility/difficulty/input/audio behavior is already design-closed; exact audio/UI/VFX assets remain part of presentation binding. The external-mod integration architecture is also now closed in `M0_INTEGRATION_ARCHITECTURE.md`.

## Canon priority / stale-document rule

Use the following order whenever two statements differ:

1. current GitHub `main`;
2. [`GAME_DESIGN.md`](./GAME_DESIGN.md) — gameplay/design master canon;
3. [`PROJECT.md`](./PROJECT.md) — technical/build/project contracts;
4. later explicit refinement documents referenced by the master/project canon, including [`REGION_CROSS_AUDIT.md`](./REGION_CROSS_AUDIT.md), [`WORLD_STORY_CANON.md`](./WORLD_STORY_CANON.md), [`PARTY_MULTIPLAYER.md`](./PARTY_MULTIPLAYER.md) and [`FISHING_COLLECTION_HOUSING_MARKET.md`](./FISHING_COLLECTION_HOUSING_MARKET.md);
5. subordinate regional/system/content documents;
6. historical audits, old completion estimates and previous conversation summaries.

Do **not** merge conflicting historical rules. If an older subordinate document still contains a superseded rule, the later/master rule replaces it entirely for implementation. Git history is the archive; stale rules are not alternative design options.

Known examples of supersession:

- housing is **one residence at a time** with the later trade-up/resale rules; older multi-property wording is obsolete;
- later `REGION_CROSS_AUDIT.md` anti-repetition refinements override older regional-package encounter/discovery grammar where explicitly changed;
- `R02_CONTENT_BIBLE.md` through `R12_CONTENT_BIBLE.md` close narrative/content-authoring blanks in their matching implementation packages without reopening already-locked regional system/combat contracts;
- R10 content canon resolves the old `Laviathan unlock in R10 or R11` ambiguity: **launch Laviathan unlock belongs to R11**;
- `PARTY_MULTIPLAYER.md` makes combat reward ownership explicit: **every eligible participant gets their own full normal personal EXP/Class XP; rewards are never divided by party size and last hit has no ownership value**;
- R01 ordinary Verdant Crystal is **Field-Pick accessible**; the old `maybe Refined Pick later` wording is obsolete;
- older completion percentages and `next work` sections are historical snapshots, not current scheduling authority.

When touching a stale section during future work, update or remove the obsolete wording instead of preserving contradictory variants.

## Canon start here

1. [`GAME_DESIGN.md`](./GAME_DESIGN.md) — gameplay/design source of truth
2. [`PROJECT.md`](./PROJECT.md) — technical/build/project contracts
3. [`PARTY_MULTIPLAYER.md`](./PARTY_MULTIPLAYER.md) — party/co-op reward, participation, scaling, friendly-fire and multiplayer acceptance rules
4. [`EXTERNAL_SOURCES.md`](./EXTERNAL_SOURCES.md) — external code/assets/UI/maps/structures provenance and adoption status
5. [`M0_DEPENDENCY_AUDIT.md`](./M0_DEPENDENCY_AUDIT.md) — Fabric 26.2 dependency/version/ownership boundaries
6. [`M0_INTEGRATION_ARCHITECTURE.md`](./M0_INTEGRATION_ARCHITECTURE.md) — how external mods are composed through adapters/data overlays/tags without surrendering project authority
7. [`WORLD_STORY_CANON.md`](./WORLD_STORY_CANON.md) — protagonist/story/faction/ending spine
8. [`REGION_CROSS_AUDIT.md`](./REGION_CROSS_AUDIT.md) — R01–R12 anti-repetition and regional-identity contract
9. [`R01_VERTICAL_SLICE.md`](./R01_VERTICAL_SLICE.md) — Alderford opening and exact first-session content
10. [`R02_IMPLEMENTATION_PACKAGE.md`](./R02_IMPLEMENTATION_PACKAGE.md) + [`R02_CONTENT_BIBLE.md`](./R02_CONTENT_BIBLE.md)
11. [`R03_IMPLEMENTATION_PACKAGE.md`](./R03_IMPLEMENTATION_PACKAGE.md) + [`R03_CONTENT_BIBLE.md`](./R03_CONTENT_BIBLE.md)
12. [`R04_IMPLEMENTATION_PACKAGE.md`](./R04_IMPLEMENTATION_PACKAGE.md) + [`R04_CONTENT_BIBLE.md`](./R04_CONTENT_BIBLE.md)
13. [`R05_IMPLEMENTATION_PACKAGE.md`](./R05_IMPLEMENTATION_PACKAGE.md) + [`R05_CONTENT_BIBLE.md`](./R05_CONTENT_BIBLE.md)
14. [`R06_IMPLEMENTATION_PACKAGE.md`](./R06_IMPLEMENTATION_PACKAGE.md) + [`R06_CONTENT_BIBLE.md`](./R06_CONTENT_BIBLE.md)
15. [`R07_IMPLEMENTATION_PACKAGE.md`](./R07_IMPLEMENTATION_PACKAGE.md) + [`R07_CONTENT_BIBLE.md`](./R07_CONTENT_BIBLE.md)
16. [`R08_IMPLEMENTATION_PACKAGE.md`](./R08_IMPLEMENTATION_PACKAGE.md) + [`R08_CONTENT_BIBLE.md`](./R08_CONTENT_BIBLE.md)
17. [`R09_IMPLEMENTATION_PACKAGE.md`](./R09_IMPLEMENTATION_PACKAGE.md) + [`R09_CONTENT_BIBLE.md`](./R09_CONTENT_BIBLE.md)
18. [`R10_IMPLEMENTATION_PACKAGE.md`](./R10_IMPLEMENTATION_PACKAGE.md) + [`R10_CONTENT_BIBLE.md`](./R10_CONTENT_BIBLE.md)
19. [`R11_IMPLEMENTATION_PACKAGE.md`](./R11_IMPLEMENTATION_PACKAGE.md) + [`R11_CONTENT_BIBLE.md`](./R11_CONTENT_BIBLE.md)
20. [`R12_IMPLEMENTATION_PACKAGE.md`](./R12_IMPLEMENTATION_PACKAGE.md) + [`R12_CONTENT_BIBLE.md`](./R12_CONTENT_BIBLE.md)

The implementation package owns the region's system/traversal/ecology/combat/dungeon contract. The matching content bible owns final settlement identity, named cast, concrete quest/scene/reward flow, reconnect behavior and story-evidence closure. Neither is permission to merge in older conflicting rules.

## Party / co-op reward invariant

Party play is cooperative, not competitive.

For a jointly defeated enemy or eligible shared encounter:

```text
player A → A's own full normal EXP + Class XP + eligible personal loot/Gold
player B → B's own full normal EXP + Class XP + eligible personal loot/Gold
player C/D → same rule independently
```

There is **no shared EXP pool and no party-size split**. Each receiver uses their own Lv/Class Rank and the existing encounter-Lv modifier. Last hit, highest DPS and party leader status do not change reward ownership.

Formal party membership alone is never enough: actual combat/support/objective participation establishes eligibility. Healing, barriers, control and revives can qualify support-oriented players without damage racing.

Ordinary enemies do not gain HP merely because a friend joined. Elite/miniboss/boss scaling uses actually engaged eligible players, not remote party members or total online players.

Detailed rules live in `PARTY_MULTIPLAYER.md`, `QUEST_WORLD_STATE.md` and `COMBAT_BALANCE.md`.

## Design-closure and player-facing-content rule

`PROJECT.md`'s pre-code design-completion contract is mandatory. The project is **not** ready for gameplay source bootstrap merely because the broad concept is understandable.

Before a subsystem/content package is considered design-complete, its canon must be specific enough that an implementer can finish it from the documents without inventing game design during coding. This includes, as applicable:

- exact player-facing behavior and state transitions;
- balance formulas, defaults, costs, rewards, limits and failure/edge behavior;
- multiplayer/server authority and late-join/rejoin behavior;
- UI information hierarchy, interactions, states and scaling;
- quest objectives, branches, recovery/failure behavior, visible aftermath and reward ownership;
- named NPC roles, scene beats, dialogue intent and recurring appearances where narrative content depends on them;
- encounter composition, boss phases/telegraphs/rewards and dungeon progression;
- POI/settlement/service roles and world placement requirements;
- accepted external visual/model/animation/VFX/audio direction and asset/provenance gates;
- data-driven content fields required for later tuning.

Do **not** leave player-facing `TBD`, `decide during coding`, vague `add something later`, or equivalent placeholders in a subsystem that is being handed to implementation. If a hard technical constraint discovered during implementation invalidates canon, revise the canon first and only then implement the revised rule. Code must not silently become the design authority.

Player-facing text and presentation must never expose the development process. Do not put internal/developer wording such as `P0`, `P1`, `alpha`, `temporary`, `prototype`, `TODO`, `debug`, `developer`, internal milestone names, test-stage labels, implementation notes, asset-intake language or similar production terminology into gameplay UI, item descriptions, quests, dialogue, system messages, tutorials, loading text or other player-visible content. Internal identifiers may exist in code/data/logs, but all shipped player-facing copy must be written from the game's world/player perspective.

## Locked direction summary

- open-world action RPG rather than vanilla-plus;
- custom Lv/EXP progression with launch cap Lv 80;
- custom RPG inventory/equipment presentation;
- HP + Mana + Stamina;
- dedicated dodge, guard and parry;
- 4 normal active skills + 1 high-impact ultimate;
- six primary stats: VIT / END / STR / DEX / INT / WIL;
- five root classes with deep advancement and broad weapon freedom;
- weapons differentiated by scaling, cadence and mechanics rather than class locks;
- one authored continent with R01–R12 regional identities and open recommended-Lv routing;
- Anchor-network main story with Restore / Release / Partition personal endings;
- custom/external creature ecology with vanilla living mobs excluded from finished world population;
- external-first final-quality UI, models, animation, VFX, sound and structures;
- gathering, fishing, camps, housing and mounts feed the same exploration/economy/progression loop without survival busywork;
- Essential-friendly multiplayer is a major technical goal and important state remains server-authoritative;
- launch formal party target is 4 players, with personal progression/rewards and no host/leader progression ownership;
- eligible co-op combat rewards are personal and **not divided by party size**;
- data-driven content and explicit performance budgets are required;
- external mods are treated as components behind project adapters/data overlays/tags; their useful presentation/runtime primitives may be retained, but project damage/progression/loot/quest/economy/world-state authority remains singular;
- dead/superseded/duplicate implementation or documentation is removed/updated after safe replacement rather than kept as a competing rule set.

## Current pre-bootstrap design queue

1. **Exact external-asset intake closure** — resolve the remaining player-facing model/outfit/item/structure/Anchor-machinery/VFX/UI/animation/SFX/BGM bindings and source/license/hash records. Important open gates remain the true `OPEN_MODEL_SELECTION` / runtime-acceptance rows in `PRODUCTION_ASSET_BINDING_MATRIX.md`.
2. **Azari spatial closure** — load the actual world and lock every major settlement/POI/road/dungeon/boss/route/depth band with coordinates, sightlines, travel-time and content-density targets. Confirm R11 depth bands and R12 flight/no-fly volumes from the real map, not the overhead render.
3. **Final stale-canon cleanup** — remove obsolete queues/status text and any remaining live contradiction. Do not reopen already-closed accessibility/input/audio behavior, class/combat design, R11 aquatic compatibility or external-mod composition.
4. **Finish the remaining R01 authority connections without placeholders** — Arc Bolt real-player impact, Better Combat melee authority, Bow draw-power authority, persistent R01/Gold/Lv-EXP/Class-Rank/inventory/active-world-time state and reconnect-safe reward plans are now in source and CI-verified. `Dust on the Quarry Road` activates only after the committed first-class/starter flow, rejects pre-activation credit, accepts only the five authored semantic action categories, completes from three distinct categories, preserves each first objective contribution's class and assigns quest Class XP to the strict-majority contribution class when one exists, then opens `Roots Below Stone`; login reconciliation closes the quest-state→reward-plan crash window. `Roadside Trouble` now has persistent shared cycle state and closed non-spatial lifecycle rules (4-minute first eligibility, 12-minute repeat delay, 120-second abandon, 2→4 threat scaling, three completion requirements), reconnect-safe participation finalization and bounded 5% combat EXP + 4% Class XP + 20 Gold repeat rewards whose Class XP stays on the contribution-time class. Personal R01 resource nodes now also have CI-verified server-owned Field-tool validation, exact R01 yield/cooldown metadata, active-world-time personal availability, mastery/first-discovery progression, all-or-nothing Material Pouch capacity handling, reconnect-safe delivery receipts, and Dust gather credit without any invented Azari coordinate. Physical Azari Lost Cargo / Viper / marker / gathering-node / Roadside Trouble volumes and actors remain unbound. Recovery Belt server resolution/effects are implemented, while player-facing `N` input, accepted drink/consume motion, 65% movement projection, dodge/guard input locking, and real reserve-item reload transactions still wait on their proper presentation/item bindings; physical item/icon + Lucifer inventory presentation also remains. Earthloong first-clear **server transaction/delivery + canonical defeat-participation backend** is now implemented and CI-verified: a server-accepted first contribution fixes reward class, project HP=0 resolves the encounter, disconnect-safe pending finalization preserves eligible players, the first boss reward layer is idempotent, and personal first-clear cannot be granted to a helper who lacks an active personal quarry run. Still incomplete are concrete support/heal/barrier/control/revive participation callers, whole-run majority-class attribution and delivery of the separate 100% EXP / 64% Class XP / 180 Gold dungeon-completion layer, guaranteed Superior+ normal boss gear, the 15% Rootquake Maul / Earthscale Ward Mythic roll, Lucifer 1-of-3 choice UI, external reward model/icon binding, post-quarry aftermath, dedicated Crit Chance / Attack Speed / Max Mana runtime consumers, manual save/reload/rejoin acceptance, multiplayer acceptance and Azari spatial binding.
5. **R01 vertical-slice implementation** — use R01 to prove the complete pipeline (external assets → adapters/data → server authority → UI/presentation → save/rejoin → real spatial placement → performance/playtest) before scaling the same architecture across R02–R12.

Do not reopen already-closed class topology, weapon/resource fundamentals, party reward ownership, R01–R12 quest/story ownership, ending structure, accessibility/input behavior, aquatic action design or regional macro direction unless implementation/playtest evidence exposes a real problem.
