# Open-World RPG — Project Contract

> Working project slug: `openworld-rpg`  
> Player-facing title: **WORKING / CANDIDATE ONLY — not yet locked, and NOT a gameplay-source-bootstrap gate; `Anchorwake` is an evaluated candidate only**  
> Current phase: **M0 GAMEPLAY DEPENDENCY RUNTIME BOOT VERIFIED / R01 GAMEPLAY + CONTENT DESIGN CLOSED / PLAYER-FACING R01 IMPLEMENTATION STILL BLOCKED BY ASSET + AZARI SPATIAL BINDING; LATER-REGION FINAL-ENCOUNTER GATES REMAIN**

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

When a current canon decision changes, update or replace stale live wording. Git history is the archive. Do not preserve contradictory rules as if both were valid options.

`DESIGN_COMPLETENESS_AUDIT.md` is the design-quality audit that found several historical conflicts, but its blocker list may lag behind later closure work. **The current pre-code gate list in §7 of this file is authoritative when a later dedicated canon has already closed an older audit blocker.**

### 1.1 Git workflow

Routine project work is **direct `main` only**.

- do not create temporary branches;
- do not create feature branches;
- do not create PR branches merely to perform normal project work;
- do not force-push;
- update `main` only by normal fast-forward commits on top of the current remote `main`;
- never reset or rewrite unrelated projects in this shared repository;
- branch creation requires an explicit user request for that specific branch/workflow.

A tool offering `create_branch` is not permission to use it automatically.

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
- initial JAR basename: `openworld-rpg-0.1.0-alpha.1.jar`
- M0 core bootstrap source exists and remains available through the explicit `core` isolation profile.
- 10 foundation/safety JARs + 3 curated creature JARs are pinned to exact admitted versions/bytes and co-load in the `gameplay` profile. Latest completed Openworld verification run `36133334102` at code state `a3389725f8e6ee97b1102cf2183e5d5d0a15dfbd` passes unit tests, clean build/JAR, core dedicated-server boot, gameplay dedicated-server boot and gameplay-client startup smoke. The verified R01 authority layer now additionally includes persistent personal active-world time, semantic server-only world-action bridges for Lost Cargo / Meadow Viper contribution / broken road marker / valid R01 gathering / Roadside Trouble, persistent personal R01 gathering-node state with Field-tool validation, canon-locked yield/cooldown/timing metadata, mastery/discovery progression, full-amount Material Pouch delivery and reconnect-safe harvest receipts, server-owned Recovery Belt use timing with 0.72 s resolution / 0.95 s action / 6.0 s shared lockout, exact Healing Potion / Focus Draught / Cleansing Tonic effect resolution, pre-resolution poise-break cancellation, Focus 3.0 s Mana tail, and the corrected 10 s Cleansing buildup-resistance window, persistent shared Roadside Trouble cycle/lifecycle state, exact 4-minute first eligibility / 12-minute repeat delay / 120-second abandon / 2→4 threat scaling rules, reconnect-safe repeat rewards at 5% combat EXP + 4% Class XP + 20 Gold with the contribution-time class preserved, bounded repeat-reward receipts, and bounded `Dust on the Quarry Road` class-attribution evidence so last-second class switching cannot steal the quest Class XP. Physical Azari volumes, wagon/marker/node placement, Meadow Viper asset/entity binding and player-facing presentation remain outside this verified backend layer.
- normal runtime now defaults to the `gameplay` profile; `core` remains an explicit isolation/test profile, so a production-shaped launch cannot silently omit the admitted gameplay stack.
- broad player-facing R01 world/UI implementation is still incomplete, but persistent server-owned save state now exists for combat Lv/EXP, per-root Class Rank/XP, Attributes, equipped combat loadout, personal R01 progression, Gold, four-slot Recovery Belt state, 36-slot Backpack/Personal Storage, Material Pouch/Vault, Key Items, Pending Reward Claim, and reconnect-safe pending/completed reward transactions

The internal folder/mod/artifact identifiers remain stable production IDs. They are **not player-facing branding** and do not need to be renamed merely because the eventual display title differs.

The internal artifact/version strings above are production identifiers. They are **not permission to display `alpha`, the slug or other development terminology inside normal gameplay UI/content.**

Pinned dependency ownership/version boundaries are canonical in `M0_DEPENDENCY_AUDIT.md`.

Baseline stack includes Fabric API plus the selected player-animation, GeckoLib, armor/ranged/trinket/combat/spell and curated creature dependencies recorded there. **AzureLib is not a second baseline animation engine.** Essential may be used for hosting/social convenience only and never owns gameplay/save authority.

Do not silently upgrade/substitute pinned runtime dependencies during implementation because a newer version happens to exist. Re-evaluate only for a real blocker or deliberate migration.

---

## 3. Product identity

This project is a large authored open-world fantasy action RPG built on Minecraft, not vanilla-plus.

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
- party/co-op multiplayer;
- external-first UI/models/animation/VFX/audio;
- Anchor-network story and personal endings.

The design goal is not feature count. It is one cohesive game whose systems reinforce exploration, combat, progression and world consequence.

`BRANDING.md` owns title-candidate evaluation and the eventual player-facing title lock. `Anchorwake` is currently an evaluated candidate, **not final canon**. Title selection is intentionally lightweight and does **not** block gameplay source bootstrap. Exact logo/font/graphic bytes remain external-first visual assets and therefore stay inside the ordinary presentation/provenance gate.

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

`EXTERNAL_SOURCES.md`, `PRODUCTION_ASSET_BINDING_MATRIX.md` and the asset-intake manifests own detailed provenance/binding state.

---

## 5. Active canon map

Primary gameplay/system canon:

- `GAME_DESIGN.md`
- `BRANDING.md` — title-candidate evaluation and eventual player-facing title; branding is not a gameplay-source-bootstrap blocker
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
- `PARTY_MULTIPLAYER.md` — formal party UX, participation eligibility, non-split personal EXP/Class XP, personal loot/Gold, co-op scaling, friendly-fire baseline and multiplayer acceptance matrix
- `UI_DIRECTION.md`
- `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md` — world challenge presets, personal accessibility assists, final frequent-action input map, Essential-safe defaults, subtitles/captions, non-audio combat cues, camera/VFX comfort and dynamic audio/music state behavior
- `R11_AQUATIC_ACTION_MATRIX.md` — closes R11 frequent-action `AQUATIC_NATIVE / AQUATIC_ADAPTED / AQUATIC_DISABLED_WITH_FALLBACK` classification, accepted UAL swim/combat/cast/guard motion strategy, 3D targeting, fallback ownership and server-authority rules
- `M0_DEPENDENCY_AUDIT.md`
- `M0_INTEGRATION_ARCHITECTURE.md` — external-mod composition contract: adapter/data-overlay/tag/event boundaries, authority firewall, validation/sync rules and anti-fork/update-containment strategy

World/story/regional canon:

- `WORLD_STORY_CANON.md`
- `MAIN_QUEST_SCENE_PACKAGE.md` — cross-region main-route requirements, recurring-character functions, evidence counting, rejoin points, sequence-break handling, personal finale choice and multiplayer story ownership
- `REGION_CROSS_AUDIT.md`
- `REGIONS.md` — current concise region index, not an archive of old candidates
- `R01_VERTICAL_SLICE.md` + `R01_CONTENT_BIBLE.md` + `R01_UI_PRODUCTION_SPEC.md` + `R01_PLAYER_TEXT_SPEC.md`
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
- `PRODUCTION_ASSET_BINDING_MATRIX.md` — cross-region binding state, true model-selection queue, dependency-validation queue and the no-temporary-player-facing-design production rule; explicit status corrections here supersede older broad source-status summaries where they directly conflict
- `R01_ASSET_INTAKE.md` and evidence snapshots where applicable;
- `R01_ASSET_PHASE_B_PASS5_ACQUISITION_EVIDENCE_2026-09-17.md` — creator-controlled direct ZIP locators, current Standard/Source boundary correction and honest binary/hash limitation.
- `R01_ASSET_PHASE_E_APPAREL_POTION_MOTION_EXACT_REVIEW_2026-09-18.md` — direct extracted glTF structure/geometry review for Wizard/Ranger/Knight and Potion_1..4 plus version-sensitive UAL Drink/Consume evidence; downstream snapshots are corroboration only.
- `R01_ASSET_PHASE_F_KENNEY_EXACT_SHORTLIST_2026-09-18.md` — exact ordinary Kenney VFX/SFX shortlist with direct candidate SHA-256, PNG dimensions and audio durations; audition/Minecraft composition and signature layers remain open.

The implementation package owns a region's traversal/ecology/encounter/dungeon/system contract. The later matching content bible closes settlement name, named cast, exact quests/scenes/rewards/reconnect state and story handoff. **R01 uses `R01_VERTICAL_SLICE.md` as its opening/system package and `R01_CONTENT_BIBLE.md` as the later full-region content-bible refinement.** If an old package contains a working placeholder superseded by its content bible, the later content bible wins for that explicitly refined point.

`MAIN_QUEST_SCENE_PACKAGE.md` owns only the cross-region main investigation and its rejoin/state rules. It does not overwrite local regional quests/rewards/aftermath already owned by the matching content bible.

`PARTY_MULTIPLAYER.md` is a subordinate refinement of `GAME_DESIGN.md` §23, `QUEST_WORLD_STATE.md`, `COMBAT_BALANCE.md` and `CLASS_PROGRESSION.md`. It does not replace their solo rules; it closes the missing co-op reward/party behavior details.

`R11_AQUATIC_ACTION_MATRIX.md` is the dedicated refinement/audit required by the older R11 package wording. It closes the aquatic action-compatibility design gate; runtime retarget/render/playtest proof remains validation work rather than a reason to invent a second underwater combat system.

`PRODUCTION_ASSET_BINDING_MATRIX.md` is the dedicated cross-region visual/source triage. A row marked `DEPENDENCY_VALIDATE` must not trigger another broad model search; a row marked `OPEN_MODEL_SELECTION` is a true visual pre-code gate. Its Threateningly Mobs Continued storefront-license conflict rule overrides stale `MIT` shorthand for raw-byte reuse decisions until exact upstream licensing is resolved.

### 5.1 Current canon-sync corrections

These are not new design options. They identify older live phrases that are already superseded by later canon and must be cleaned from their original documents during the final stale-text pass.

- **The final player-facing title is NOT locked, but it is NOT a gameplay-source-bootstrap blocker.** `Anchorwake` was researched and collision-screened but returned to candidate status after first-contact owner feedback showed that `Anchor` is not self-explanatory before the setting is learned. `BRANDING.md` owns lightweight later finalization.
- **Alderford is the final player-facing R01 starting-settlement name.** Older wording saying the starting-settlement name/lore will be decided later is stale.
- `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md` closes the global difficulty/assist, frequent-action input, subtitle/non-audio cue, camera/VFX comfort and audio/music **behavioral** contracts. Exact SFX/BGM files remain an asset-intake problem, not an open behavior-design problem.
- `R11_AQUATIC_ACTION_MATRIX.md` closes the former R11 aquatic action-compatibility design blocker. Runtime retarget/render/playtest proof remains validation work.
- **R03 Basalt Wyvern references in older class-progression prose are stale.** Basalt Wyvern belongs to R10. The live `CLASS_PROGRESSION.md` now uses current R03 collapsed-mine/lift-route/Griffin identities instead of resurrecting Basalt Wyvern or the obsolete `Rocky Roller` slot.
- `R01_ASSET_INTAKE.md` now includes the later R01 fish intake narrowing: old Quaternius clownfish/Sea-Life/tuna candidates are rejected for Heartland, and the current four-role direct-review queue is **Small Fish Common Minnow / CDmir Fish / Quaternius Armored Catfish / CDmir Esox**. Broad fish discovery is stopped unless one role fails direct review. None is yet binary-hashed/3D/Minecraft accepted; `R01 ASSET READY` remains `NO`.
- Phase E exact-file review now adds technical corroboration for the selected Wizard/Ranger/Knight apparel families (shared 65-joint humanoid skin, real BIN/texture payloads), confirms `Potion_1..4` are distinct geometry payloads, and corrects the UAL evidence boundary: the 2025-06-10 46-clip UAL1 Standard mirror has no `Drink`, while a newer pinned integration corroborates exact `Drink`/`Consume` names. Creator-current archive acquisition, project-local SHA-256, direct visual/retarget/Minecraft acceptance remain open, so `R01 ASSET_BINDING COMPLETE` and `R01 SOURCE READY` remain `NO`.
- Phase F Kenney binding now pins exact ordinary baseline sprite/audio candidates and direct candidate hashes/timing. This stops generic filename discovery for those baseline roles, but **does not** close Earthloong electrical signature presentation, final Burning/Frostbite shapes, audio audition, spatial mix, Low-VFX review or Minecraft composition. `R01 ASSET_BINDING COMPLETE` and `R01 SOURCE READY` therefore remain `NO`.
- Older `Threateningly Mobs Continued = MIT` shorthand is not sufficient for raw-byte reuse. Current storefront metadata conflicts; use dependency-only handling until the exact canonical upstream license is resolved as recorded in `PRODUCTION_ASSET_BINDING_MATRIX.md`.
- `M0_INTEGRATION_ARCHITECTURE.md` closes the former open question of how multiple large external mods are combined: dependencies stay isolated behind project adapters/data overlays/tags, while project state remains authoritative. KubeJS/FTB Quests/Cobblemon/Create/Mine & Slash are architecture references unless separately admitted as runtime dependencies; this does not reopen the Fabric loader choice or import a second RPG progression stack.

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

## 6.1 R01 design-closure status

R01 now has a dedicated closure split:

```text
R01 GAMEPLAY / CONTENT DESIGN CLOSED: YES
R01 IMPLEMENTATION-TIME GAMEPLAY CHOICES REMAIN: NO
R01 ASSET_BINDING COMPLETE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 SOURCE READY: NO
R01 IMPLEMENTED: NO
R01 PLAYTESTED: NO
R01 MULTIPLAYER TESTED: NO
```

The current authoritative closure table is `R01_CONTENT_BIBLE.md` §26–§28.

This means **do not reopen R01 systems, rewards, quest text, merchant logic, fish mechanics, mount behavior, Camp/furniture rules, fast travel or UI flow during coding.** If real asset/spatial/play evidence invalidates a value, revise canon first.

---

## 7. Content-closed does not mean source-ready

R01–R12 now have concrete regional content authoring and the cross-region main quest/rejoin structure is closed in `MAIN_QUEST_SCENE_PACKAGE.md`, but the project is **not yet fully gameplay-source-ready**.

Older package headers using `implementation-ready` must be interpreted narrowly as `the described mechanics/content flow no longer needs invention`. They do not waive these current pre-code gates:

1. **exact external presentation binding and provenance** for unresolved models, outfits, items, structures, Anchor machinery, final logo/font/graphic, VFX, animations, SFX/BGM and local-only/dependency boundaries;
2. **actual Azari spatial closure** — coordinates, footprints, sightlines, route joins, travel-time targets, POI/dungeon/boss placement and content-density validation;
3. **asset-gated final encounter sheets** — exact player-facing guardian/boss names, anatomy-supported attacks/weak points/signature materials only after their accepted models are known;
4. **final stale-document / hidden-choice audit** — remove obsolete alternatives and ensure no implementation-time gameplay decision remains hidden in older live text.

**Player-facing title selection is not one of these gates.** It may remain a lightweight working decision while gameplay source is prepared, provided internal slug/version/development strings are not exposed as finished player-facing branding. Final title/logo packaging is closed before the finished player-facing release/presentation pass.

The global accessibility/difficulty/assist, subtitles/non-audio cues, frequent-action input map and audio/music state-behavior design are closed in `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`. Exact music/SFX bytes remain part of the external-asset gate in item 1.

The R11 aquatic action/animation compatibility design is closed in `R11_AQUATIC_ACTION_MATRIX.md`. Runtime animation retarget/render validation remains required before R11 can be called tested or play-ready, but it is no longer a design blocker.

The cross-region main-route region requirements, evidence counts, recurring-character scene functions, rejoin logic, sequence-break handling, finale handoff and personal ending commit behavior are closed in `MAIN_QUEST_SCENE_PACKAGE.md`. Exact scene locations/cameras/outfits/props/audio remain subject to gates 1–3 where applicable.

`PRODUCTION_ASSET_BINDING_MATRIX.md` narrows gate 1: already-selected dependency actors proceed to runtime acceptance, concrete external candidates proceed to acquisition/conversion review, and only `OPEN_MODEL_SELECTION` rows require new visual search.

The narrow M0/source-bootstrap and early R01 authority layer now includes persistent combat Lv/EXP, independent per-root Class Rank/XP, Attributes, persistent equipped-loadout authority, personal R01 progression state, server-owned Gold, four-slot Recovery Belt state, canonical opening-loadout projection (Heartland Arming Sword + Watch Buckler + one loaded Healing Potion + 150 Gold), first-root-class starter weapon projection with displaced starter preservation, server-owned 36-slot Backpack + 36-slot Alderford Personal Storage + Material Pouch/Vault + Key Items + Pending Reward Claim, Pouch-first settlement material sourcing, persistent personal active-world time, persistent personal R01 gathering-node cooldown/tool/mastery/discovery state with reconnect-safe lossless Material Pouch delivery, semantic world-action authority for the five `Dust on the Quarry Road` categories, bounded objective class-attribution evidence, `Dust` activation/completion/reward authority with `Roots Below Stone` handoff and reconnect repair, persistent shared `Roadside Trouble` lifecycle/timing/participation state, bounded reconnect-safe repeat rewards whose Class XP stays on the contribution-time class, reconnect-safe reward plans, the first **actual Spell Engine spell resource** for `openworld_rpg:arc_bolt`, canonical Better Combat melee replacement for project-owned actors, authoritative Bow draw-power replacement for project-owned actors, project-owned player vitals/resources, and the canonical active-defense state machine. Arc Bolt keeps the locked 22-block range, project CUSTOM impact handler and donor-neutral cost; its current 1.0 s cast / 1.0 blocks-per-tick projectile timing remains playtest-tunable while damage, Mana, cooldown and Poise stay project-owned. The dedicated `-m0-playtest.jar` prepares a legal Lv8 Mage +7 INT / ItemLv8 Staff test build and exposes only marker-gated integration commands; the normal JAR remains free of that verification marker. A real joined-player playtest has confirmed the authored Earthloong spawn path, Arc Bolt cast/release, held-input behavior, project cooldown presentation and a successful Arc Bolt impact without the target disappearing or corrupting. CI run `36133334102` verifies the canonical Better Combat melee foundation, player MaxHP/VIT and END/Stamina runtime, canonical dodge/guard/perfect-guard/guard-break timing/math including perfect-guard-only charges, the persistent armor/shield defensive-equipment publisher, and the fail-closed outgoing HP-damage boundary for project-owned external actors. The project now also publishes defensive equipment independently of the offensive weapon build: armor archetype + slot + Item Lv produce canonical per-slot-rounded Defense/MR, shield family produces canonical GuardRating, and Defense/MR/Guard Strength affixes feed one server-owned defensive snapshot. No weapon-guard rating is invented because the current canon has not assigned one. A normalized server bridge now accepts only explicit project incoming-hit data and resolves it through the player's project Defense/MR/Stamina/guard/dodge state before applying HP damage. Project-owned external actors can no longer fall back to donor-origin HP damage against players. R01 Earthloong now shares one data-backed encounter source for action selection plus the canon-closed Phase-1 physical impact rules: Claw Sweep 10%/medium guard, Tail Scythe 20%/heavy guard, and Quarry Rush 24%/heavy perfect-guard-only. Those percentages are converted to fixed raw physical attack data from the canonical same-Lv benchmark rather than runtime target-MaxHP damage. R01 Earthloong Phase-1 physical start geometry is now data-backed: Claw Sweep uses the locked 0°..120° front/side arc within 3.5 blocks, Tail Scythe uses the locked 60°..180° side/rear arc within 4.5 blocks, and Quarry Rush requires 5.0..9.0 blocks plus a clear committed line. Claw Sweep and Quarry Rush also have runtime-verified dependency-only presentation-state bridges to the pinned donor SkillNumber states 1 and 2 respectively; Tail Scythe deliberately remains presentation-unbound because the pinned donor surface exposes no accepted dedicated tail-scythe animation. Full attack-state scheduling/movement/contact invocation, real client visual acceptance and player-facing dodge movement/animation/input remain separate connections; these are not replaced with temporary motion or donor combat authority. Better Combat's real joined-player network/presentation hit also still requires one manual acceptance pass before M0 gate 9 can be called closed.

This does **not** waive the remaining gates above. Broad player-facing R01 implementation still waits for exact asset/spatial closure. Narrow technical work that does not lock unresolved presentation may continue, but it must not create placeholders that later become de facto canon.

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
- UI screens/icons/branding visuals;
- animation/VFX/audio.

### 8.1 No temporary player-facing design

The first production-facing implementation must already use the accepted external visual direction.

- UI is not first implemented as generic black panels/vanilla buttons and reskinned later;
- important actors are not first implemented as vanilla stand-ins and replaced later;
- signature attacks are not first presented as generic particle clouds and treated as acceptable until polish;
- settlements/dungeons are not first authored as temporary vanilla shells that quietly become permanent;
- placeholder icons/frames/models/VFX/SFX are not part of the player-facing production path.

If the exact external visual is unresolved, leave that visible slot gated and work on another closed unit. **Do not create temporary design debt merely to make a feature look implemented.**

External designs/assets may be adapted, recomposed, retargeted or integrated to fit the game's canon and technical constraints. This does not authorize improvised AI visual language that competes with the selected external art direction.

A missing exact source is a pre-code gate for that visible content, not permission for a vanilla/AI placeholder to become the final answer.

### 8.2 External-mod composition contract

External mods are treated as **components**, not as independent games that retain final authority inside Openworld RPG.

The canonical composition shape is:

```text
external runtime/content
→ project integration adapter / data overlay / semantic tags
→ project domain request/state
→ server-authoritative validation/transaction
→ project presentation sync
```

Project core code should not scatter donor implementation classes across combat, quests, loot, progression or world state.

For an external dependency, prefer in this order:

```text
documented public API
→ registry/resource ID
→ tag/datapack/data registry
→ published event/callback
→ supported config
→ narrow compatibility shim
→ donor-internal mixin/reflection only as a documented last resort
```

An accepted dependency actor/system gets explicit policy by dimension rather than a vague `use this mod` decision. The detailed policy vocabulary (`PASS_THROUGH / ADAPT / OVERRIDE / SUPPRESS / REFERENCE_ONLY`), overlay schema, integration-module contract, startup validation and server→client data-sync rules live in `M0_INTEGRATION_ARCHITECTURE.md`.

Invariants:

- external presentation/animation/AI primitives may be retained when they improve quality;
- project spawn/stat/damage/loot/progression/world-state rules override donor defaults where canon requires;
- no donor recipe/loot/worldgen/progression is accepted merely because the mod is installed;
- external actor changes should normally target registry IDs through project overlay data rather than editing/forking donor files;
- required integration failure is explicit during development, never silently replaced by a vanilla stand-in;
- optional integrations disable cleanly;
- one dependency update should normally require changes in its adapter/binding layer, not in unrelated project gameplay systems;
- the project does not install another complete RPG/class/economy/quest system merely to obtain one useful primitive.

The point of using many good mods is to reduce low-value reinvention while still shipping **one coherent game**.

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
- EXP/Lv/Class XP;
- skill cost/success/cooldowns;
- class/progression;
- quests and evidence;
- world-state changes;
- mounts/major encounter controllers;
- party membership/leadership;
- save data and reward transactions.

Essential never replaces this authority model.

Personal/shared/encounter ownership follows `QUEST_WORLD_STATE.md`. Rewards must be idempotent. Support contribution must count where specified. Host ownership does not make the host the only legitimate story player.

### Co-op reward invariant

For a jointly defeated enemy or encounter:

```text
each eligible participating player receives their own normal personal EXP
+ their own normal Class XP for the qualifying active class
+ their own eligible personal loot/Gold roll
```

**EXP/Class XP are not divided by party size.** Each receiver uses their own Lv/Class Rank and the existing encounter-level anti-farm modifier. Last hit and party leader status have no reward ownership value.

Formal party membership alone never grants rewards; actual participation does. Detailed qualification/scaling/reconnect behavior lives in `PARTY_MULTIPLAYER.md`.

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
