# Open-World RPG

Large Minecraft Java open-world action RPG project with very low dependence on vanilla progression systems.

## Current status

`DESIGN CANON LATE PRE-PRODUCTION / NO SOURCE BOOTSTRAP YET`

No implementation exists yet. Do not treat the project as buildable until M0 explicitly creates the loader/toolchain/source layout.

The major gameplay systems, Lv 1–80 progression, class/combat framework, equipment/economy, world-state model, main story spine, mounts, field systems and R01–R12 regional packages are already specified. The remaining pre-bootstrap work is primarily exact external-asset binding, concrete quest/NPC scene authoring, Azari spatial placement/travel-time validation, global audio/accessibility/input closure and a final design-closure audit.

## Canon priority / stale-document rule

Use the following order whenever two statements differ:

1. current GitHub `main`;
2. [`GAME_DESIGN.md`](./GAME_DESIGN.md) — gameplay/design master canon;
3. [`PROJECT.md`](./PROJECT.md) — technical/build/project contracts;
4. later explicit refinement documents referenced by the master canon, including [`REGION_CROSS_AUDIT.md`](./REGION_CROSS_AUDIT.md), [`WORLD_STORY_CANON.md`](./WORLD_STORY_CANON.md) and [`FISHING_COLLECTION_HOUSING_MARKET.md`](./FISHING_COLLECTION_HOUSING_MARKET.md);
5. subordinate regional/system documents;
6. historical audits, old completion estimates and previous conversation summaries.

Do **not** merge conflicting historical rules. If an older subordinate document still contains a superseded rule, the later/master rule replaces it entirely for implementation. Git history is the archive; stale rules are not alternative design options.

Known examples of supersession:

- housing is **one residence at a time** with the later trade-up/resale rules; older multi-property wording is obsolete;
- later `REGION_CROSS_AUDIT.md` anti-repetition refinements override older regional-package encounter/discovery grammar where explicitly changed;
- older completion percentages and `next work` sections are historical snapshots, not current scheduling authority.

When touching a stale section during future work, update or remove the obsolete wording instead of preserving contradictory variants.

## Canon start here

1. [`GAME_DESIGN.md`](./GAME_DESIGN.md) — gameplay/design source of truth
2. [`PROJECT.md`](./PROJECT.md) — technical/build/project contracts
3. [`EXTERNAL_SOURCES.md`](./EXTERNAL_SOURCES.md) — external code/assets/UI/maps/structures provenance and adoption status
4. [`WORLD_STORY_CANON.md`](./WORLD_STORY_CANON.md) — protagonist/story/faction/ending spine
5. [`REGION_CROSS_AUDIT.md`](./REGION_CROSS_AUDIT.md) — R01–R12 anti-repetition and regional-identity contract
6. `R02_IMPLEMENTATION_PACKAGE.md` through `R12_IMPLEMENTATION_PACKAGE.md` — regional implementation packages

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
- data-driven content and explicit performance budgets are required;
- dead/superseded/duplicate implementation or documentation is removed/updated after safe replacement rather than kept as a competing rule set.

## Current pre-bootstrap design queue

1. **Exact external-asset intake expansion** — unresolved core models, bosses, structures, VFX, sound and R12 final guardian; record exact source/license/hash/animation bindings where applicable.
2. **Concrete main-quest / recurring-NPC scene packages** — convert the story spine and regional quest roles into actual quest beats, dialogue/scene intents, rejoin states, failure/recovery and visible consequences.
3. **Azari spatial closure** — inspect the actual terrain and lock POI/road/settlement/dungeon/boss placement using coordinates, sightlines and travel-time/content-density targets.
4. **Global presentation/comfort closure** — music and sound-state coverage, accessibility, difficulty options and final key/input mapping.
5. **Final pre-bootstrap closure audit** — remove remaining stale rules, verify cross-document consistency and identify only genuine implementation-time unknowns.

Do not reopen already-closed class topology, weapon/resource fundamentals or regional macro direction unless implementation/playtest evidence exposes a real problem.