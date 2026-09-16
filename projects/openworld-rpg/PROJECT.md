# Open-World RPG — Project Contract

> Working project slug: `openworld-rpg`  
> Player-facing title: **PRE-CODE BRANDING GATE — must be locked before the first player-visible build; never expose `TBD` or the internal slug as the finished title**  
> Current phase: **DESIGN CANON LATE PRE-PRODUCTION / GAMEPLAY SOURCE BOOTSTRAP BLOCKED BY PRE-CODE GATES**

## 1. Repository / authority contract

This project follows, in order:

1. current GitHub `main`;
2. root `AGENTS.md`;
3. `docs/BUILD_STANDARD.md`;
4. `docs/QUALITY_STANDARD.md`;
5. this `PROJECT.md` for technical/project contracts;
6. `GAME_DESIGN.md` for gameplay/design master canon;
7. explicitly indexed later refinement/content documents;
8. historical audits and old chats only as evidence/history.

When a current canon decision changes, update or replace the stale live wording. Git history is the archive. Do not preserve contradictory rules as if both were valid options.

`DESIGN_COMPLETENESS_AUDIT.md` is the current pre-code closure audit and records detected stale text/balance drift. It does not invent a second game design; it is used to identify and remove contradictions from the active canon.

---

## 2. Technical identity

- Minecraft Java: **26.2**
- Java: **25**
- Loader: **Fabric — locked for this existing project**
- Fabric Loader: **0.19.5**
- Fabric API: **0.160.0+26.2**
- Gradle: **9.5.1**
- Fabric Loom: **1.17.20**
- build plugin: `net.fabricmc.fabric-loom`
- mod id / namespace: `openworld_rpg`
- initial internal artifact version: `0.1.0-alpha.1`
- planned initial JAR basename: `openworld-rpg-0.1.0-alpha.1.jar`
- no gameplay source/save format exists yet

The internal artifact/version strings above are production identifiers. They are **not permission to display `alpha`, the slug or other development terminology inside normal gameplay UI/content.**

Pinned dependency ownership/version boundaries are canonical in `M0_DEPENDENCY_AUDIT.md`.

Baseline stack includes Fabric API plus the selected player-animation, GeckoLib, armor/ranged/trinket/combat/spell and curated creature dependencies recorded there. **AzureLib is not a second baseline animation engine.** Essential may be used for hosting/social convenience only and never owns gameplay/save authority.

Do not silently upgrade/substitute pinned runtime dependencies during implementation because a newer version happens to exist. Re-evaluate only for a real blocker or deliberate migration.

---

## 3. Product identity

This is a large authored open-world fantasy action RPG built on Minecraft, not vanilla-plus.

Minecraft supplies the block world, runtime, input base and hosting environment. Project-owned systems supply the player-facing RPG identity:

- Lv/EXP progression;
- stats/classes/skills;
- dodge/guard/parry/poise combat;
- RPG equipment/inventory;
- quests/world state;
- settlements/services/economy;
- custom/external creature ecology;
- dungeons/field bosses/world bosses;
- gathering/fishing/camps/housing/mounts;
- external-first UI/models/animation/VFX/audio;
- Anchor-network story and personal endings.

The design goal is not feature count. It is one cohesive game whose systems reinforce exploration, combat, progression and world consequence.

---

## 4. Personal-use / public-repository boundary

The intended gameplay build is private-use, but this GitHub repository is public.

Therefore:

- non-redistributable/private-use third-party bytes stay outside the public repository;
- the repository may store provenance and local import/integration instructions;
- committed third-party code/assets must satisfy their actual redistribution terms;
- permissive/open code and assets may be reused directly when allowed and useful;
- license claims are package/source specific — never infer `all assets by creator X use one license` when the source record says otherwise;
- no DRM/paywall/access-control bypass and no paid-asset piracy;
- a future public release requires a fresh provenance audit.

`EXTERNAL_SOURCES.md` and the asset-intake manifests own detailed provenance.

---

## 5. Active canon map

Primary gameplay/system canon:

- `GAME_DESIGN.md`
- `COMBAT_BALANCE.md`
- `CLASS_COMBAT_KITS.md`
- `CLASS_PROGRESSION.md`
- `STATUS_AND_R01_ENCOUNTERS.md`
- `LOOT_ECONOMY.md`
- `EQUIPMENT_BALANCE.md`
- `RECOVERY_PRODUCTION_APPEARANCE.md`
- `GATHERING_FISHING_CAMP_HOUSING.md`
- `FISHING_COLLECTION_HOUSING_MARKET.md`
- `MOUNTS.md`
- `QUEST_WORLD_STATE.md`
- `UI_DIRECTION.md`
- `M0_DEPENDENCY_AUDIT.md`

World/story/regional canon:

- `WORLD_STORY_CANON.md`
- `REGION_CROSS_AUDIT.md`
- `REGIONS.md` — current concise region index, not an archive of old candidates
- `R01_VERTICAL_SLICE.md`
- `R02_IMPLEMENTATION_PACKAGE.md` + `R02_CONTENT_BIBLE.md`
- `R03_IMPLEMENTATION_PACKAGE.md` + `R03_CONTENT_BIBLE.md`
- `R04_IMPLEMENTATION_PACKAGE.md` + `R04_CONTENT_BIBLE.md`
- `R05_IMPLEMENTATION_PACKAGE.md` + `R05_CONTENT_BIBLE.md`
- `R06_IMPLEMENTATION_PACKAGE.md` + `R06_CONTENT_BIBLE.md`
- `R07_IMPLEMENTATION_PACKAGE.md` + `R07_CONTENT_BIBLE.md`
- `R08_IMPLEMENTATION_PACKAGE.md` + `R08_CONTENT_BIBLE.md`
- `R09_IMPLEMENTATION_PACKAGE.md` + `R09_CONTENT_BIBLE.md`
- `R10_IMPLEMENTATION_PACKAGE.md` + `R10_CONTENT_BIBLE.md`
- `R11_IMPLEMENTATION_PACKAGE.md` + `R11_CONTENT_BIBLE.md`
- `R12_IMPLEMENTATION_PACKAGE.md` + `R12_CONTENT_BIBLE.md`

Quality/intake:

- `DESIGN_COMPLETENESS_AUDIT.md`
- `EXTERNAL_SOURCES.md`
- `R01_ASSET_INTAKE.md` and evidence snapshots where applicable.

The implementation package owns a region's traversal/ecology/encounter/dungeon/system contract. The later matching content bible closes settlement name, named cast, exact quests/scenes/rewards/reconnect state and story handoff. If an old package contains a working placeholder superseded by its content bible, the later content bible wins for that explicitly refined point.

---

## 6. Meaning of design-closed

The project uses a strict definition:

> An implementer should be able to build the planned player-facing game from the canon without inventing game design while coding.

Before a subsystem is handed to implementation, the canon must close, as applicable:

- behavior and state transitions;
- formulas, costs, rewards, limits and defaults;
- failure/recovery/edge behavior;
- multiplayer/server authority;
- save/rejoin/late-join handling;
- UI hierarchy/interactions/states/scaling;
- quest objectives, branches, visible aftermath and reward ownership;
- named NPC roles and scene beats where narrative depends on them;
- encounter composition, telegraphs, phases and reward rules;
- POI/settlement/service roles;
- data fields needed for later tuning;
- external presentation direction and the explicit gate that closes any unresolved exact asset.

Do **not** leave `decide during coding`, player-affecting `TBD`, `add something later`, vague placeholder bosses/resources or hidden implementation choices.

If a hard technical constraint invalidates canon:

```text
stop affected implementation
→ revise canon
→ review the new rule
→ implement the revised canon
```

Code does not silently become design authority.

---

## 7. Content-closed does not mean source-ready

R01–R12 now have concrete regional content authoring, but the project is **not yet gameplay-source-ready**.

Older package headers using `implementation-ready` must be interpreted narrowly as `the described mechanics/content flow no longer needs invention`. They do not waive these current pre-code gates:

1. final player-facing game title/branding;
2. exact external model/outfit/item/structure/VFX/animation/audio binding and provenance;
3. actual Azari coordinates, sightlines, route joins, travel-time and content-density validation;
4. exact asset-gated boss names/anatomy-supported attacks/weak points/signature materials;
5. R11 aquatic animation/action compatibility matrix;
6. global accessibility, difficulty/assist, subtitles/non-audio cues and final input map;
7. full music/audio-state coverage;
8. final stale-document/hidden-choice audit.

Only after these are closed should M0 create the gameplay source/resource/data layout.

---

## 8. External-first admission contract

For important player-facing content, use this order:

```text
identify gameplay need
→ find/verify strong external model/design/code/reference
→ classify license / dependency / local-only / editable-base boundary
→ verify Minecraft-scale readability and animation coverage
→ lock final player-facing identity/name/role/stats
→ record binding
→ implement
```

Do not invent a long generic list first and hope matching art exists later.

This applies especially to:

- bosses/creatures/mounts;
- weapons/armor/outfits/accessories;
- ores/herbs/resource nodes;
- structures/workstations/important props;
- UI screens/icons;
- animation/VFX/audio.

A missing exact source is a pre-code gate for that visible content, not permission for a vanilla/AI placeholder to become the final answer.

---

## 9. Player-facing development-language prohibition

Production documents/code/logs may use internal identifiers.

Normal gameplay must never expose development/process language such as:

- `P0`, `P1`;
- `alpha`, `beta`;
- `prototype`, `temporary`, `placeholder`;
- `TODO`, `debug`, `developer`;
- internal milestone/test-stage names;
- asset-intake/license/hash notes;
- raw state/controller IDs.

This applies to UI, quests, dialogue, item descriptions, tutorials, loading text, system messages and localization.

A missing binding/localization is a content/build failure, not a player-facing warning.

---

## 10. Multiplayer / authority contract

Important state is server-authoritative, including:

- damage/hit acceptance;
- HP/resources/status/poise;
- item ownership and loot eligibility;
- Gold;
- EXP/Lv/class progression;
- skill cost/success/cooldowns;
- quests and evidence;
- world-state changes;
- mounts/major encounter controllers;
- save data and reward transactions.

Essential never replaces this authority model.

Personal/shared/encounter ownership follows `QUEST_WORLD_STATE.md`. Rewards must be idempotent. Support contribution must count where specified. Host ownership does not make the host the only legitimate story player.

Do not label multiplayer successful until tested with real clients.

---

## 11. Performance / implementation cleanliness

Do not solve scale with hidden permanent tick cost.

Avoid:

- region-wide every-tick scans;
- huge permanent pathfinding populations;
- entity-heavy decoration where static/block composition works;
- full simulated ocean/aquifer/ecology networks;
- large VFX/Display Entity fields always running while unloaded/irrelevant.

Measure suspected hotspots with profiler/reproduction conditions.

After a replacement is verified at appropriate risk, remove dead/duplicate/prototype implementations and obsolete fallbacks. Keep compatibility/migration paths only when they have a live reason to exist.

---

## 12. Validation language

Use these states literally:

- `CODE REVIEWED`
- `TESTED`
- `BUILD VERIFIED`
- `JAR PRODUCED`
- `PLAYTESTED`
- `MULTIPLAYER TESTED`

A build does not prove combat feel, UI quality, traversal quality or multiplayer correctness.

Current project status before M0:

```text
DESIGN/CANON REVIEWED: YES
GAMEPLAY SOURCE: NONE
CODE REVIEWED: N/A
TESTED: NO
BUILD VERIFIED: NO
JAR PRODUCED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
