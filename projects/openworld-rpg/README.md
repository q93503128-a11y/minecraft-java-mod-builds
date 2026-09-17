# Open-World RPG

Large Minecraft Java open-world action RPG project with very low dependence on vanilla progression systems.

## Current status

`DESIGN CANON LATE PRE-PRODUCTION / NO SOURCE BOOTSTRAP YET`

No implementation exists yet. Do not treat the project as buildable until M0 explicitly creates the loader/toolchain/source layout.

The major gameplay systems, Lv 1–80 progression, class/combat framework, equipment/economy, world-state model, main story spine, mounts, field systems, party/co-op rules and R01–R12 regional packages are already specified. **R01–R12 now all have concrete named-NPC, exact quest-condition, scene/dialogue, reconnect/reward-state and story-evidence authoring locked.** Remaining pre-bootstrap work is no longer regional content invention. It is primarily exact external-asset binding, Azari spatial placement/travel-time validation, global audio/accessibility/input closure and the final cross-document design-closure audit.

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
5. [`WORLD_STORY_CANON.md`](./WORLD_STORY_CANON.md) — protagonist/story/faction/ending spine
6. [`REGION_CROSS_AUDIT.md`](./REGION_CROSS_AUDIT.md) — R01–R12 anti-repetition and regional-identity contract
7. [`R01_VERTICAL_SLICE.md`](./R01_VERTICAL_SLICE.md) — Alderford opening and exact first-session content
8. [`R02_IMPLEMENTATION_PACKAGE.md`](./R02_IMPLEMENTATION_PACKAGE.md) + [`R02_CONTENT_BIBLE.md`](./R02_CONTENT_BIBLE.md)
9. [`R03_IMPLEMENTATION_PACKAGE.md`](./R03_IMPLEMENTATION_PACKAGE.md) + [`R03_CONTENT_BIBLE.md`](./R03_CONTENT_BIBLE.md)
10. [`R04_IMPLEMENTATION_PACKAGE.md`](./R04_IMPLEMENTATION_PACKAGE.md) + [`R04_CONTENT_BIBLE.md`](./R04_CONTENT_BIBLE.md)
11. [`R05_IMPLEMENTATION_PACKAGE.md`](./R05_IMPLEMENTATION_PACKAGE.md) + [`R05_CONTENT_BIBLE.md`](./R05_CONTENT_BIBLE.md)
12. [`R06_IMPLEMENTATION_PACKAGE.md`](./R06_IMPLEMENTATION_PACKAGE.md) + [`R06_CONTENT_BIBLE.md`](./R06_CONTENT_BIBLE.md)
13. [`R07_IMPLEMENTATION_PACKAGE.md`](./R07_IMPLEMENTATION_PACKAGE.md) + [`R07_CONTENT_BIBLE.md`](./R07_CONTENT_BIBLE.md)
14. [`R08_IMPLEMENTATION_PACKAGE.md`](./R08_IMPLEMENTATION_PACKAGE.md) + [`R08_CONTENT_BIBLE.md`](./R08_CONTENT_BIBLE.md)
15. [`R09_IMPLEMENTATION_PACKAGE.md`](./R09_IMPLEMENTATION_PACKAGE.md) + [`R09_CONTENT_BIBLE.md`](./R09_CONTENT_BIBLE.md)
16. [`R10_IMPLEMENTATION_PACKAGE.md`](./R10_IMPLEMENTATION_PACKAGE.md) + [`R10_CONTENT_BIBLE.md`](./R10_CONTENT_BIBLE.md)
17. [`R11_IMPLEMENTATION_PACKAGE.md`](./R11_IMPLEMENTATION_PACKAGE.md) + [`R11_CONTENT_BIBLE.md`](./R11_CONTENT_BIBLE.md)
18. [`R12_IMPLEMENTATION_PACKAGE.md`](./R12_IMPLEMENTATION_PACKAGE.md) + [`R12_CONTENT_BIBLE.md`](./R12_CONTENT_BIBLE.md)

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
- dead/superseded/duplicate implementation or documentation is removed/updated after safe replacement rather than kept as a competing rule set.

## Current pre-bootstrap design queue

1. **Exact external-asset intake closure** — resolve all remaining boss/creature models, NPC outfits, weapons/items, structures, Anchor machinery, VFX, UI bindings, animation, SFX/BGM and source/license/hash records. Important open gates include R05/R06/R07/R08/R09 dungeon/field-boss identities, R10 final guardian accept/replace, R11 sea-fort/deep combat assets and R12 final guardian.
2. **Azari spatial closure** — inspect the actual terrain and lock every major settlement/POI/road/dungeon/boss/route/depth band with coordinates, sightlines, travel-time and content-density targets. Confirm R11 depth bands and R12 flight/no-fly volumes here.
3. **Global presentation/comfort closure** — final music/sound-state coverage, accessibility, difficulty options, subtitles/cues, HUD comfort, party HUD/ping readability and final key/input mapping.
4. **Final pre-bootstrap closure audit** — remove remaining stale direct contradictions, especially combat/recovery numeric residue, old mount-placement wording, obsolete `next work` blocks and boss HP ranges that disagree with their stated TTK formula.
5. **M0 source bootstrap only after the above gates** — create the Fabric 26.2 / Java 25 toolchain/source/data/resource layout and begin implementation from the canon instead of redesigning during coding.

Do not reopen already-closed class topology, weapon/resource fundamentals, party reward ownership, R01–R12 quest/story ownership, ending structure or regional macro direction unless implementation/playtest evidence exposes a real problem.
