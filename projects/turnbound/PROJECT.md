# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.17`

### Project isolation

**TURNBOUND and TURNBOUND: RE are different projects.**

- Current project canon/runtime/source: `projects/turnbound/`
- `projects/turnbound-re/` is not a dependency, migration source, design authority, or fallback canon for TURNBOUND.
- TURNBOUND: RE documents, code, balance, roster decisions, UI assumptions, and save rules must never be imported into TURNBOUND unless the user explicitly requests a specific transfer.
- Similar names, shared character IDs, or old conversation context are not sufficient evidence for cross-project reuse.

## Toolchain
- Minecraft Java 26.2
- NeoForge 26.2.0.62
- Java 25
- Gradle 9.2.1
- GeckoLib retained for authored model/animation

## Canon authority

1. latest explicit user decisions
2. `01_GAME_DESIGN_v1.md`
3. `02_BALANCE_RULES_v1.md`
4. `03_CHARACTER_DESIGN_v1.md`
5. `UI_DESIGN_SYSTEM.md`
6. `WORLD_OVERHAUL_DREHMAL.md`
7. `DREHMAL_MAP_REFERENCE_v1.md`
8. `FIRST_ROUTE_CAPITAL_VALLEY_v1.md`
9. `AVSAL_EXPANSION_v1.md`
10. `ENCOUNTER_ENEMY_PLACEMENT_v1.md`
11. `MULTIPLAYER_DESIGN_v1.md`
12. `OVERHAUL_ROADMAP_v1.md`
13. current source/resources
14. preserved TURNBOUND v0.4 system canon in Git history / archived design files, only where it was explicitly reaffirmed and not later superseded by the user

The old **Aster March physical-world layout** is superseded by the Drehmal production world.
That world replacement does **not** automatically invalidate unrelated TURNBOUND system canon such as rarity, roster identity, or economy rules.

A newer document is not allowed to override a later explicit user decision merely because its filename says `v1`.
If current docs/source conflict with a later user decision, classify the current state as stale/regressed and repair the docs before treating it as canon.

## Current identity

TURNBOUND is an independent 3D party turn-based RPG hosted inside Minecraft.

Core:
world exploration → NPC/event/visible encounter → 4-person turn battle → reward/growth → new route/character/content.

## Combat

Preserve:
- max 4 allies
- normal max 5 enemies
- Gauge 1000
- action subtracts 1000, overflow survives
- player decision time stops logical battle time
- cooldowns tick on owner regular actions
- Basic CD0
- server authority
- 1x/2x presentation only

Overhaul:
- deterministic fixed-point TurnScheduler
- runtime/HUD/AUTO share scheduler
- SPD is action frequency
- Gauge manipulation uses common semantics
- P01~P08 follow `03_CHARACTER_DESIGN_v1.md`

## Camera

Camera pivot = battle center.

Required:
- terrain-aware footprint
- ally/enemy centroid
- wall/cliff fallback
- skill camera then center return
- no Aster-specific yaw/coordinate
- camera state restoration on every end path

## Characters

- native rarity range is **★1~★5**
- P01~P08 remain the core hero roster
- F01/F02 are ★1 low-rarity material-type characters
- F03/F04 are ★2 low-rarity material-type characters
- ★1~★2 are not deleted, legacy-only, or automatically promoted to ★3
- ★3+ characters carry the stronger role/signature-mechanic expectation; ★1~★2 may be simpler and intentionally lower power
- duplicate copies never required for core kit
- models/animations/VFX/SFX are part of completion
- portrait uses final model when quality permits
- the current 12 playable characters (P01~P08 + F01~F04) are the **initial production baseline, not a permanent roster cap**
- after the first route/core combat/runtime is stable, add new playable characters in later content updates
- roster expansion must add genuinely different combat choices and complete model/animation/VFX/SFX/acquisition/codex integration; do not inflate the roster with minor stat variants

Confirmed Standard Archive baseline restored on 2026-09-26:
- ★5 3%: P02 / P05 / P06
- ★4 12%: P01 / P03 / P04 / P07
- ★3 35%: P08
- ★2 30%: F03 / F04
- ★1 20%: F01 / F02

Do not revert this table to a ★3~★5-only pool without a new explicit user decision.

## Duplicate +Level canon — 2026-09-29

Latest explicit user decision:
- a duplicate character keeps its existing rarity-based Star Essence reward
- the same duplicate also grants that character **+Level +1**
- +Level is independent from battle/quest XP and the base Lv60 cap
- +Level caps at +10
- effective combat level = base XP level + duplicate +Level
- therefore the maximum effective level is Lv70
- a duplicate after +10 still grants Star Essence but no additional +Level
- +Level extends HP/ATK/DEF through the existing level curve; SPD does not scale
- Lv60 gates for Awakening/Signature progression continue to refer to the base XP level, so a lower base level cannot bypass them with duplicates
- this system does not decide the still-unresolved nativeStar/currentStar/promotion/★6/Awakening relationship

Star Essence permanent exchange is part of the same collection loop:
- 150 Essence → 300 Crystal
- 450 Essence → eligible ★4 selector
- 1,200 Essence → eligible ★5 selector
- current selector UI is spoiler-safe and shows owned matching-rarity characters until an explicit story-exposure flag exists
- selector copies use the same duplicate reward path: Essence refund + +Level where below +10

## Canon reconciliation status — 2026-09-29

The independent TURNBOUND canon/regression audit has been reviewed against the current workbranch.

Confirmed:
- TURNBOUND is separate from TURNBOUND: RE.
- ★1~★2 remain part of TURNBOUND.
- F01/F02 remain ★1.
- F03/F04 remain ★2.
- P05 remains ★5.
- the five-tier Standard Archive table above is the restored baseline.
- commit `93a0c9d02b25fd8e46643ba10f1009165b7a2780` reintroduced a stale ★3~★5-only summon assumption after the 2026-09-26 restoration.
- the current P01~P08 v1 kits, Drehmal world transition, base Lv1~60 XP growth, duplicate +0~+10 bonus-level axis, equipment structure, and removal of the global Awakening Core spending path are intentional overhaul work and must not be reverted as collateral damage.

Still unresolved and requiring an explicit user decision before implementation changes:
- exact `nativeStar` / `currentStar` progression semantics
- promotion / ★6 / Awakening relationship
- low-rarity material usage semantics
- F03 initial story grant/composition
- any other historical v0.4 system not individually classified as regression or intentional overhaul

Do not infer a restore/delete decision for those unresolved systems from historical implementation alone.

## Current canon repair implementation — 2026-09-29

This workbranch now restores the confirmed five-tier Standard Archive runtime contract:
- ★1~★5 production summon pool
- F01~F04 production summon eligibility
- P05 native ★5 while preserving the current Sightline/Shot kit
- soft pity 65 / hard pity 80
- duplicate Star Essence 5 / 15 / 40 / 100 / 250
- all 12 registered playable characters projected to character/codex UI
- ★1~★5 rarity filtering and five-tier Archive probability display

The unresolved nativeStar/currentStar/promotion/★6/Awakening relationship listed above remains intentionally untouched. Duplicate +Level is a separately confirmed growth axis.

Validation for this repair must be recorded separately after the branch build. Client runtime/playtest/multiplayer remain unverified until actually run.

## P1 regression repair checkpoint — 2026-09-29

The first post-audit runtime repair block now also includes:
- normal `HUB_REACHED` progress no longer qualifies as legacy direct-arrival migration provenance
- Drehmal meta snapshots carry server-authoritative Awakening readiness without reviving retired Aster/B05 Signature Trial copy
- compact collection paging no longer forces rows outside available vertical space
- compact Archive controls use two bounded rows and the five-tier probability text fits the compact content area
- skill descriptions and every passive line remain reachable through wheel scrolling instead of being truncated to three lines
- pending promotion/★6 canon is no longer presented to players as a settled "no promotion" rule

Static 320×240 GUI math now yields one safe codex character row, non-overlapping Archive controls, and a scrollable two-line skill-detail viewport. Actual rendering still requires client playtest.

Shared-battle offline settlement, New Drabyel physical service placement, resource-pack edge cases, and remaining production-world field validation are separate follow-up units.

## Shared-battle settlement repair — 2026-09-29

Post-audit multiplayer reliability work now:
- lets remaining online participants leave a finished shared battle without waiting indefinitely for disconnected owners
- settles an offline owner's victory into the existing durable reward WAL before the shared session is removed
- keeps transaction ids/idempotence so a cold reload can replay the WAL without duplicate rewards
- resolves a claimed Drehmal field encounter by the original claimant UUID even when that initiator is offline
- preserves same-server reconnect behavior and server authority

This is **CODE REVIEWED**, not multiplayer-playtested. 2/3/4-player disconnect/reconnect, server-stop recovery, camera, return-position, and duplicate-reward scenarios remain required real multiplayer tests.

## Post-audit integration validation checkpoint — 2026-09-29

The current branch now contains one integrated post-audit repair set covering:
- five-tier summon/codex/economy canon restoration
- P05 native ★5 without reverting the current kit
- normal-hub vs legacy-direct-arrival save provenance
- external Drehmal Awakening readiness projection
- compact 320×240 collection/archive layout and scrollable skill/passive details
- offline shared-battle reward WAL settlement and claimant-safe field encounter release

Targeted regression tests were added/restored for the summon contract, compact paging, external Awakening projection, legacy-arrival provenance, and offline reward-journal recovery.

A branch build is requested by this checkpoint commit. Until its workflow result is directly observed, validation remains:
- CODE REVIEWED: YES
- TESTED: NOT YET CONFIRMED FOR THIS HEAD
- BUILD VERIFIED: NO
- JAR PRODUCED: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## New Drabyel automatic placement bridge — 2026-09-29

New Drabyel service placement does **not** require the user to author six coordinates or repeatedly enter survey commands.

Source analysis:
- pinned public reference: `zachaa/DrehmalMap@72d82180cbe3f950f068cf2d8e8668c6b09d5c58`
- inspected `data/paths.geojson`, `data/all_entity_data.json` and `data/locations.json`
- 76 nearby New Drabyel source records were inspected; TURNBOUND retains only small derived semantic anchors, road points and exclusion zones
- source landmarks include the town-entry sign, Verdant Saddle, Adventuring Merchant, Goibhniu's Smithy / Runic Blacksmith, church, graveyard, map displays, Primal Cache and farmhouse Cat Map
- external map JSON/images are reference-only and are not vendored as TURNBOUND assets

Runtime placement:
- runs only in a bound Drehmal world and only when New Drabyel is relevant
- each of the six services has multiple data-driven search seeds, expected elevation, preferred roadside distance, facing target and protected source-content exclusions
- the live 26.2 world supplies the final Y and exact adjacent standing block
- candidates reject blocked headroom, fluids, unstable local ground, excessive source-height drift, authored road centers, nearby block entities and existing villagers/traders
- services keep minimum spacing from one another
- safe candidates are selected deterministically and cached
- failed scans retry after 200 ticks instead of rescanning every tick
- no terrain or source entity is rewritten; a role with no safe candidate stays absent

The static `new_drabyel_services_v1.json` remains fail-closed. Runtime placement creates transient derived service positions rather than pretending the 1.20.1 coordinates are already verified 26.2 blocks.

The six admin `/turnbound survey ...` commands are optional diagnostics/forensics only. Normal testing is one walkthrough of the automatically populated hub; the user is not expected to place NPCs one by one.

Source-backed micro-layout used by the resolver:
- town approach reference: about 502,67,1801
- stables / Verdant Saddle: entrance-right area around 506,68,1836
- Adventuring Merchant: 516,67,1854
- Runic Blacksmith: 526,65,1839; Goibhniu's Smithy sign around 527,67,1844
- central booths: Oak around 530,67,1830 / Coal around 535,67,1838 / Wheat around 541,67,1833
- church entrance: around 527,68,1854
- graveyard: east/southeast of the church, protected from service placement
- farmhouse basement Cat Map: 516,65,1861, explicitly protected as original content

Current verification state for this block:
- CODE REVIEWED: YES
- TESTED: requested by the branch workflow
- BUILD VERIFIED: pending workflow result
- JAR PRODUCED: pending workflow result
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Capital Valley automatic placement — 2026-09-29

The first-route placement path is now source-aware and does not require manual per-NPC/per-encounter coordinate entry.

Current scope:
- all 7 first-route combat slots are covered: 6 Common + 1 optional Warning Cave Elite
- all 3 field NPCs are covered: 길잡이 세라 / 순찰대원 로엔 / 탐험가 미라
- the pinned Drehmal map source now records roads, locations, towers and `data/all_entity_data.json`
- strict encounter/NPC sites are scored toward readable road shoulders rather than the authored road center
- live 26.2 placement rejects blocked headroom, fluid, unstable ground and nearby authored block/entity content
- battle arena candidates use a wider source-content clearance check
- field NPC/enemy materialization rechecks source-content clearance when the player actually approaches
- the first roadhead arrival also rejects authored-content collisions
- the first guide's stale `roadhead_reveal` zone assignment was corrected to the actual Temple → Tower road corridor
- same-zone failure remains fail-closed; no encounter is silently moved to a different landmark

The admin survey commands remain optional diagnostics. Required user validation is a normal first-route playthrough, not manual coordinate authoring.

### Optional Graul world-boss binding

The Capital Valley optional world-boss candidate is now bound as `CV_WORLD_BOSS_GRAUL` without restoring the retired Aster March chapter flow.

- placement is derived from the pinned Capital Valley route geometry around the Tower and then resolved against live 26.2 terrain; no new fixed boss coordinate is authored
- the resolver requires an off-road, low-slope meadow outside route safety zones and at least two camera-safe 4-player battle footprints
- original Drehmal block/entity content is rechecked before activation; failure is fail-closed and leaves the optional boss dormant
- the field actor uses the existing production Graul model/texture/boss animation set and the boss-specific in-world telegraph behavior
- the boss remains optional and never becomes a main-route navigation target
- first victory is persisted as an encounter-specific world clear, so Graul does not naturally respawn while walking the route; any future replay must be an explicit challenge interaction
- the new encounter reuses the existing B01 combat reward basis (12,000 Gold / 5,000 XP / 60 Essence) plus the separated boss-side first-clear package (1,200 Crystal + one T2 choice)
- `CV_WORLD_BOSS_GRAUL` cannot trigger retired `BATTLE_B01` Archive/P08/story quest/legacy-region progression even though both encounters use the Graul combatant

Build TURNBOUND #932 failed at `compileTestJava` because the first regression test directly instantiated `TurnboundWorldSavedData`, crossing the intentionally narrow plain-JVM test classpath boundary for Minecraft `SavedData`. Production sources had already compiled. The test was repaired by extracting the clear-key decision into the pure `WorldEncounterClearPolicy`; production persistence delegates to that same policy instead of widening the test runtime or removing the regression check.

Build TURNBOUND #933 (run `36524938895`) then verified commit `ade00b325a78cb87f38c3164db69329ef70615f4`:
- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click mrpack verification: PASS
- artifact upload: PASS
- artifact: `turnbound-v04-workbranch` (ID `11014151809`)
- JAR SHA-256: `366957e80d61a01ac8d59d643a71a69e68ec5b371337c01ca03a1372533c79c5`
- MRPACK SHA-256: `1d08e86bb8c1d0b532fdf1289e2d0f0ba29a69a4e43593b34006cb8eae9f9328`

Validation for this block:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Source-aware first-route validation checkpoint — 2026-09-29

The integrated Capital Valley placement pass now covers the complete initial route density without manual coordinate authoring:
- 6 Common encounter sites
- 1 optional Elite site
- 3 non-service field NPC sites
- roadhead arrival protection
- source-content-safe arena selection
- materialization-time source-content revalidation

Targeted tests now cover:
- road-shoulder scoring
- sparse corridor segment distance
- complete encounter/NPC placement coverage
- semantic field-NPC zone assignment
- pinned structured-map entity provenance

This checkpoint requests the branch `Build TURNBOUND` workflow. Until its result is directly observed:
- CODE REVIEWED: YES
- TESTED: NOT YET CONFIRMED FOR THIS HEAD
- BUILD VERIFIED: NO
- JAR PRODUCED: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## 26.2 compile repair checkpoint — 2026-09-29

The first source-aware Capital Valley build failed before tests because Minecraft 26.2 moved villager classes under the villager package. Both hub and first-route collision guards now depend on the stable 26.2 `AbstractVillager` base type, which still excludes normal villagers and wandering traders without duplicating concrete class imports.

This is the only correction made in response to Build TURNBOUND #36515077187 before requesting a new build.

Build TURNBOUND #36515501797 then reached the next compile error: a reassigned local patrol candidate was captured by a stream lambda. That single compile issue was replaced with an explicit duplicate loop; no gameplay rule changed.

## Capital Valley post-compile revalidation — 2026-09-29

The source-aware first-route implementation is complete for this work unit:
- 6 Common encounters + 1 optional Warning Cave Elite
- 3 physical non-service field NPCs (길잡이 세라 / 순찰대원 로엔 / 탐험가 미라)
- source-aware road-shoulder site selection
- live 26.2 collision/source-content checks
- source-safe arena selection and materialization-time revalidation
- manual per-NPC/per-encounter coordinate authoring is not required

The two compile-only issues found by Build TURNBOUND #36515077187 and #36515501797 have both been corrected:
- Minecraft 26.2 villager package/API import
- non-effectively-final patrol candidate captured by a stream lambda

This checkpoint requests one fresh branch build of the corrected integrated state. Until that workflow result is directly observed:
- CODE REVIEWED: YES
- TESTED: NOT YET CONFIRMED FOR THIS HEAD
- BUILD VERIFIED: NO
- JAR PRODUCED: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Unit-test classpath repair — 2026-09-29

Build TURNBOUND #36516550042 compiled production and test sources successfully, then failed only in two JVM unit tests with `NoClassDefFoundError: net/minecraft/world/level/BlockGetter`.

Root cause:
- pure route-geometry/source-plan assertions referenced runtime classes (`DrehmalAdaptiveRoutePlacement` / `DrabyelHubAutoPlacement`)
- loading those runtime classes on the plain unit-test JVM pulled Minecraft world types that are not present on that test runtime classpath

Correction:
- corridor-segment geometry moved into the existing Minecraft-free `DrehmalRoutePlacementRules`
- route geometry test now targets that pure helper
- hub source-plan coverage test derives its six roles from the data-only placement/service catalogs instead of loading the live-world auto-placement class
- production placement behavior is unchanged

This checkpoint requests one fresh build after the test-boundary repair.

Build TURNBOUND #927 / run `36516751921` completed successfully for commit
`6c3cdf2711f7994a1e98a6f499628eaadabdbfc8`.

Verified:
- Gradle test/build/one-click pack: PASS
- NeoForge dedicated-server smoke: PASS — `Done (5.798s)`
- JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — `turnbound-v04-workbranch`, artifact id `11011003401`
- JAR SHA-256: `1b14b4a618d8e229845b4ba6e01a07600bb254c729a1cd610c43a0da213d7de6`
- MRPACK SHA-256: `2cd30fad3d82508de5bce3c9b0c676414db0581be0bf0a8eebe8a9b70c60733e`

Validation state:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Summon presentation production pass — 2026-09-29

The Standard Archive presentation now follows one synchronized server/client reveal timeline instead of opening directly on the result card.

Implemented flow:
- rarity pre-signal before the character identity is shown
- short silhouette/anticipation beat using the production 3D character model
- player-private in-world GeckoLib actor appears in front of the player
- reveal actor starts in ready body language, then plays the character's authored victory pose
- owner-private world particles scale by rarity and new-character status
- rarity changes color/effect/audio intensity rather than simply making high-rarity pulls wait longer
- name / rarity / new-vs-duplicate result is delayed until the reveal beat
- duplicate result still reports the exact Star Essence conversion
- 10-pull summary remains the final readable overview
- SKIP is available from the start

Highlight policy:
- every newly owned character receives a 3D spotlight
- every ★4/★5 pull receives a spotlight even when it is a duplicate
- therefore a new low-rarity character can no longer hide a duplicate ★5 reveal
- if a batch has no new character and no ★4+, its highest-rarity result receives one focus reveal

Audio:
- the existing production CC0 summon/spawn layer is used for the anticipation cue
- P01~P08 reuse their authored hero timbre at model reveal
- F01~F04 use the generic production skill/reveal layer
- no new unlicensed audio asset was introduced

The 3D actor, client overlay and audio consume the same pure timeline contract so the signal, appearance, pose and name beats cannot silently drift to different frame counts.

Required remaining validation for this block is visual/client-side:
- world actor framing against real New Drabyel terrain
- silhouette readability
- 1★/2★/3★/4★/5★ differentiation
- 10-pull pacing when several new/high-rarity characters appear
- skip cleanup
- actual audio balance

Build TURNBOUND #928 / run `36518239813` completed successfully for commit
`3f16733edb6507085426cc9bb91ff06b33b27d55`.

Verified:
- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS — `Done (3.231s)`
- JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — `turnbound-v04-workbranch`, artifact id `11011891955`
- JAR SHA-256: `cfd3764b3ada1eb5a0c356f3e60da3807c03802d9e3aac3ebdf170cce32fc4ff`
- MRPACK SHA-256: `8db85f1a66e7b293d36a71ed150d28f6282d4e7b7dcfe7e211ed0358bf5671a2`

Until actual client observation:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Duplicate +Level / Essence loop implementation checkpoint — 2026-09-29

The latest duplicate-growth decision is now wired through the authoritative campaign path.

Implemented:
- base battle/quest XP remains Lv1..60
- every duplicate summon grants its existing rarity-based Star Essence **and** +Level +1 for that character
- +Level is independent from base XP and caps at +10
- effective combat level = base level + +Level, up to Lv70
- HP/ATK/DEF consume effective level through the existing level curve; SPD remains fixed
- base-Lv60 requirements for Awakening/Signature remain base-level checks and cannot be bypassed by +Level
- further duplicates at +10 continue to grant Star Essence
- summon history/result presentation shows the Essence reward and +Level gain/MAX state
- party/character/codex/growth/battle-result UI projects +Level instead of hiding it
- campaign save schema 6 persists character +Level and summon-history bonus metadata; schemas 1/4/5 remain readable with missing bonus fields defaulting safely
- the permanent Archive exchange now supports 150 Essence → 300 Crystal
- owned ★4 and ★5 characters can be selected at 450 / 1,200 Essence respectively without exposing unseen characters
- selector copies use the same duplicate path, including rarity Essence refund and +Level where below +10
- Archive facility proximity remains server-authoritative for all exchange actions
- compact Essence selector lists are paged instead of overflowing 320×240-class layouts

This does **not** settle nativeStar/currentStar promotion, ★6, or the exact Awakening/★6 relationship.

Regression coverage was added for:
- +10 duplicate cap and effective Lv70
- XP preserving +Level while base level stops at 60
- Lv70 HP/ATK/DEF scaling with unchanged SPD
- permanent Crystal exchange
- ★4 selector duplicate reward/+Level behavior and +10 cap
- unowned-character spoiler-safe selector rejection
- schema 5 → schema 6 missing-+Level migration
- schema 6 full round-trip

Validation requested by this checkpoint:
- CODE REVIEWED: YES
- TESTED: pending branch workflow
- BUILD VERIFIED: NO
- JAR PRODUCED: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Duplicate +Level final validation checkpoint — 2026-09-29

Build TURNBOUND #929 reached the unit-test phase with production/test compilation successful, then failed one newly added assertion because the test supplied only 999,999 XP from Lv20 and incorrectly assumed that amount reached Lv60. The actual curve correctly reached Lv52. No production growth value was changed; the cap test now supplies enough XP to exercise the intended Lv60 clamp.

Post-#929 cleanup also:
- preserves +Level in battle-result projection and shows Lv60 +10 / effective level instead of hiding bonus growth
- keeps legacy meta character rows readable
- pages Essence selector cards on compact GUI
- avoids overlapping Essence explanatory text and selector buttons
- preserves old constructor shapes used by existing tests/callers

This commit requests the final integrated branch build for the +Level / Essence work unit.

Validation before that result:
- CODE REVIEWED: YES
- TESTED: previous run reached tests; final result pending
- BUILD VERIFIED: NO
- JAR PRODUCED: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Duplicate +Level / Essence loop build verification — 2026-09-29

Build TURNBOUND #930 / run `36522072236` completed successfully for commit
`244814829f2a25d4aefdd46ff48b6010e9928abc`.

Verified:
- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS — `Done (4.734s)`
- JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — `turnbound-v04-workbranch`, artifact id `11013472356`
- JAR SHA-256: `df68b83e2b81cbc88ebec2d36ee5a8fac82d977b2b4e9c9bf61051cae8f3388c`
- MRPACK SHA-256: `06baa291f37ceb38d84e297bded72bd1d31478b4899a4e255fb210a6c0ad95b6`

Build #929 is not the verification baseline. It compiled successfully but one newly added test used insufficient XP and incorrectly expected Lv20→60; the test input was corrected without changing the production growth curve.

Current validation:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Summon spotlight synchronization repair — 2026-09-29

During final review of the duplicate +Level integration, one presentation desynchronization was found:
- the server-side 3D actor plan correctly spotlights every newly owned character and every ★4/★5 pull
- the client overlay still had the older rule that preferred only newly owned characters when any new pull existed
- a batch such as new ★1 + duplicate ★5 could therefore show a server-side ★5 actor without a matching client reveal slot

Correction:
- the server now computes authoritative spotlight indices from the same pure `GachaPresentationPlan`
- each summon result row carries its spotlight flag to the client
- the client follows those flags directly
- an older-payload fallback keeps the same new-or-★4/★5 policy
- a regression test pins the exact spotlight indices for new-low-rarity + duplicate-★5 batches

This checkpoint requests one final integrated build after the synchronization repair.

## Duplicate +Level / summon integration verification — 2026-09-29

Build TURNBOUND #931 / run 36522629646 succeeded for commit c60317aa935cc6150ff4491df49bcafbb9a9947b.

- Gradle test/build: PASS
- dedicated-server smoke: PASS
- JAR / one-click pack verification: PASS
- artifact: turnbound-v04-workbranch, id 11013640375
- JAR SHA-256: 0e9eaa2ce3981d441778d19a2b8dfd5055c6f3db4efad3cbd61d62ea4f859ff1
- MRPACK SHA-256: 1b030d0136e17c9b685b8afe3b0f533bb78a34a834233c4035e7be54090ffb0b
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

This supersedes #930 as the latest verification baseline because it includes the authoritative summon spotlight synchronization repair.

## Economy

Long-term core currencies:
- Gold
- Summon Crystal
- Star Essence

The old global Awakening Core currency is superseded by character quest + level + Gold + fixed quest item where necessary.

No equipment gacha.
No real-money purchase.
No time-limited FOMO banner in initial game.

## UI

Production UI:
- external verified asset/reference first
- no temporary player-facing visual pass; first visible binding uses a vetted external/final-quality asset
- Korean readability
- portrait-based party/Turn Order where quality allows
- redesigned minimap/world map
- shallow resource paths
- SOURCE/LICENSE tracking
- no improvised black-panel production UI

See `UI_DESIGN_SYSTEM.md` and `ASSET_PIPELINE_v1.md`.

## World

Production base:
Drehmal: APOTHEOSIS v2.2.2f, separately installed.

- TURNBOUND: RE is not dependency/canon
- do not vendor original world/resource pack without permission
- map survey precedes NPC/encounter/quest
- verified 26.2 terrain before critical anchor
- no Aster reconstruction
- semantic world data only after actual inspection

## Player-facing copy

Normal gameplay never exposes:
- internal IDs
- development-stage labels
- debug/log
- raw exceptions
- implementation terminology

Player sees only authored game/world language.

## Cleanup

When replacement completes:
- obsolete Aster builders/routers/maps removed
- dead UI removed
- duplicate helpers consolidated
- obsolete duplicate/placeholder character data removed, while canonical F01~F04 low-rarity characters are retained
- superseded global Awakening Core path removed/migrated
- compatibility code kept only for real save/network reason

## Planning / validation policy

**Design-document work does not run build/CI/JAR verification.**

For:
- design docs
- balance docs
- character kit planning
- world placement planning
- UI planning
use docs-only commits with `[skip ci]`.

Build/test starts only when source/runtime work resumes and a meaningful implementation chunk is complete.

Validation labels stay distinct:
- CODE REVIEWED
- TESTED
- BUILD VERIFIED
- JAR PRODUCED
- CLIENT RUNTIME TESTED
- PLAYTESTED
- MULTIPLAYER TESTED

## Historical verified implementation checkpoint

- CODE REVIEWED: YES
- TESTED: YES — fixed-point scheduler + P01~P08 v1 combat runtime + Drehmal first-route binding + battle-center camera + CV-A/B/C production enemies + Warning Cave Cavehorn Elite asset/AI/reward contract + Drabyel-road Hill Marksman two-step aim telegraph/retarget contract + shallow RPG quick-menu navigation + source-backed Drehmal world-map replacement + New Drabyel physical service NPC runtime/facility gating + R_PG-derived single-representative field encounters + deterministic roam/dwell pacing + Minecraft terrain-aware PathfinderMob navigation with blocked-route recovery + horizontal Tower/camp/Drabyel safety-ring aggro exclusion and visible-enemy return behavior + R_PG-derived short in-world alert prelude + survey-gated transient location banners + close-range server-authored service prompts + server-authored Drehmal first-route navigation + authoritative pre-battle return view and immediate field-context resync after battle + external-vs-legacy field-command authority isolation + Drehmal-specific first-hub facility/summon gating + Drehmal world-map/minimap routing + external-world meta-surface isolation + legacy world-writer fail-closed guards + external-runtime eviction of retained legacy sessions + admin-only direct summon commands + explicit battle-transition field handoff + unified client presentation ownership + field HUD/shortcut suppression during handoff + short post-outro result reveal + progression-first contextual first-route guidance + persistent one-step New Drabyel onboarding + complete six-role FableCraft New Drabyel service visual family + v1 summon economy/roster migration + private world-first 3D summon reveal + v1 level/equipment/Awakening growth migration + schema-5 legacy-save conversion + P01~P08 v1 signature-resource/target presentation binding + P01~P08 role-prop asset contract + distinct two-layer core-hero action audio + shared live-3D portrait binding across battle/meta/summon/result UI + persistent live-3D signature body-language states for P01/P03/P05/P06/P07+Toto/P08 including P08 Overheat + dedicated 3D relation sigils for P01 Duel/P05 Sightline/P04 Sanctuary/P07 partner protection that follow the actual target actor + duplicate-safe animated Turn Order rail shifts so P02/Gauge manipulation is read as movement rather than a snapped list + authoritative signature payoff beats for P01/P03/P04/P05/P06/P07+Toto/P08, including Morwen Last Page and Signature Toto authored-animation parity + first Capital Valley battle contextual onboarding that teaches Basic → Turn Order → Active/CD in the existing action header without a modal tutorial + durable Tower → Explorer camp → Drabyel approach → hub route milestones so navigation/objectives never rewind when the player backtracks + opt-in one-click test-pack bootstrap that downloads/verifies official Drehmal 2.2.2f only on first install and retains the world for later JAR-only TURNBOUND updates
- BUILD VERIFIED: YES — Build TURNBOUND #847
- verified code commit: `dc566d62a2db420a99f8cb7147df6059f8abd850`
- SERVER SMOKE: YES — NeoForge 26.2 dedicated server load, Done (4.337s)
- JAR PRODUCED: YES — artifact `turnbound-v04-workbranch`, id `10726407574`, JAR SHA-256 `9a85a7ee4ce1779fb4b54ed4eb95833e468da92ea311a0ba20dd9d337538960e`
- ONE-CLICK PACK PRODUCED: YES — `TURNBOUND-oneclick-0.1.0-alpha.17.mrpack`, SHA-256 `2c29fa7a7296b181830251c07e334f8e9d31771b27627a9e7ec8fda2a2182efd`
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


## First client playtest corrective pass — 2026-09-29

The first real #933 client walkthrough exposed production-facing problems that static tests could not establish:

- the Home quick menu spent too much area on a 2-column orb layout
- Party presets/apply/status controls consumed too much space relative to the roster
- Character/Codex grids showed too few entries per page
- character Skill detail hid passives below a narrow shared scroll surface
- New Drabyel objective text described a state instead of naming the next concrete action
- the physical entrance greeter incorrectly routed directly into the Quest menu instead of speaking
- a fresh/affected one-click save could be recognized in New Drabyel before the Capital Valley roadhead arrival was recorded, effectively skipping the first-route encounter/NPC pass
- only part of the six-role New Drabyel service set was easy to resolve because live-town collision rejection was too broad
- vanilla hostile monsters could still exist outside the hub safety radius
- minimap/world-map information density was too low
- the migrated Drehmal resource pack still emitted legacy `minecraft:item/spawn_egg_2D` model errors on the client

Corrective implementation:
- Home quick entries now use a smaller 3-column × 2-row layout
- Party roster density scales up to six columns; three preset load/save pairs, co-op entry and Apply share one compact footer row; the fixed policy/status strip was removed
- Character/Codex grids scale up to six columns on wide layouts
- wide Character Skill detail uses separate skill and always-visible passive panels; wheel scrolling remains only for compact layouts that actually need it
- hub guidance now names the next physical action/service instead of repeating the generic "prepare and check the next road" sentence
- physical greeter/story/field NPC interactions use a dedicated small dialogue surface; they no longer fall through to the Quest menu
- missing first-route arrival provenance while physically in New Drabyel is repaired by returning the player to the source-backed Capital Valley roadhead; ordinary `HUB_REACHED` alone is still not legacy-direct-arrival provenance
- New Drabyel service placement keeps explicit source exclusion zones but no longer rejects a 5×5 block-entity neighborhood or overly flat town ground; all six authored service roles remain the target
- every vanilla `Monster` is rejected in the bound Drehmal overworld and already-loaded hostile monsters around active players are swept; villagers, animals, iron golems and non-hostile authored ambience remain
- minimap terrain sampling is larger/denser and shows discovered travel points; world map allocates more area to the route, labels landmarks more aggressively and surfaces the current navigation target/distance
- Drehmal resource compatibility version advanced to 5 and readiness now validates the live archive rather than trusting a marker alone, forcing repair of the observed uppercase spawn-egg parent path

Expected first-slice physical content after the repair:
- 3 Capital Valley field NPCs: 길잡이 세라 / 순찰대원 로엔 / 탐험가 미라
- 6 New Drabyel service roles: entrance greeter / travel / market / blacksmith / story / summon
- 8 authored Capital Valley encounter records: 6 Common + Warning Cave Elite + optional Graul World Boss

Build TURNBOUND #934 / run `36529914501` compiled production successfully and ran 431 tests; only two newly changed onboarding assertions still expected the old wording. No production compile/API regression was present. Those assertions were corrected without changing gameplay behavior.

Build TURNBOUND #935 / run `36530083914` then verified commit
`1c7355ad00e276a2150adc3ee0add27d81b72283`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — `turnbound-v04-workbranch`, artifact id `11016401800`
- JAR SHA-256: `de76ef8b74c169e4f0716991f00b0c65b6774e1d8bd7f8e579472acdb8b87c7b`
- MRPACK SHA-256: `e01b8be2b1d111fd95e177009e075b1fa8cb3ab61b937e5ea6dd866746e38408`

Current validation for this corrective pass:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO — #933 supplied the failure evidence; the #935 repair still requires a fresh client check
- PLAYTESTED: NO for the repaired #935 state
- MULTIPLAYER TESTED: NO


## Repeated client playtest reset and physical-facility boundary — 2026-09-29

Until the first playable route is visually/gameplay-complete, the current local playtest workflow intentionally reuses one
Minecraft instance. To prevent stale player position/progression from invalidating each client pass, the bound Drehmal
runtime temporarily enables `PreReleaseSessionReset.ENABLED`.

While enabled, every login:
- restores a fresh TURNBOUND campaign profile
- clears that player's Drehmal onboarding / route / fast-travel discovery
- clears temporary shared encounter/world-clear state in the single-instance test world
- places the player at the current opening entry
- immediately rewrites the canonical player attachment with that fresh state

**Release gate:** this automatic login reset MUST be disabled/removed before completion/release. Persistent progression is
the production behavior; this switch exists only so repeated alpha client checks behave like first contact.

The first playable entry no longer starts at the Primal roadhead or Explorer camp. It starts at New Drabyel's
source-backed north entrance. The first combat is a short excursion outside the gate rather than a long walk to earn
the town; older Capital Valley camp/tower/cave/roadhead content remains normal optional regional exploration.

Physical New Drabyel NPC facilities are not aliases for global E-menu categories:
- equipment merchant: dedicated buy/sell screen
- blacksmith: dedicated enhancement screen
- stable: dedicated discovered-waypoint travel screen
- spirit trace: dedicated summon / Star Essence exchange screen
- greeter / story NPC: dialogue surfaces

The global E menu owns player management only. Equipment there means inspect/equip; it does not buy, sell or enhance.
The summon history category is record-only and does not perform summons. Server facility gates remain authoritative.

Client-playtest correction:
- current fast-travel nodes are no longer drawn as a second green marker on top of the player arrow
- resource-pack compatibility discovery also recognizes the exact legacy `minecraft:item/spawn_egg_2D` signature, so
  older reused instances without the newer TURNBOUND marker are repaired before world resource load


### Verification — repeated-playtest / physical-facility pass

Build TURNBOUND #936 / run `36532538930` reached production compile and ran 433 tests. One static
`DrabyelHubServiceCatalog` validation test rejected the newly introduced `TRAVEL` and `SUMMON` facility hints.
The runtime implementation was not rolled back; the catalog contract was corrected to include the two dedicated physical
service routes.

Build TURNBOUND #937 / run `36532792176` verified commit
`b9ec578abfaa6922b44b2be48c8d719a7a8403dd`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — `turnbound-v04-workbranch`, artifact id `11017141553`
- JAR SHA-256: `4cc66479415c0619fb22f656f40ac7e9fd29bbc865225d86cc8288768518c6c6`
- MRPACK SHA-256: `190e5c3e1d761c01a24bbfb251e6bb1f1ced1b71e4561eebab49e3677dcb9a3d`

Validation state:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO — this #937 state still requires the next repeated-instance client pass
- PLAYTESTED: NO for #937
- MULTIPLAYER TESTED: NO


## Opening density / external NPC UI / cleanup contract — 2026-09-29

Latest explicit client-playtest decision:

Opening:
- current fresh-session entry is New Drabyel's north entrance, using the source-backed town-approach area rather than the town center
- opening loop: entrance guide → E-menu party check → short CV_DRABYEL_ROAD excursion → return to town → hub services
- CV_DRABYEL_ROAD is the first tutorial battle and uses CV-B + CV-C with its approach seed kept under 100 m from the entry seed
- the town is the story anchor from the start; outdoor combat still happens immediately, but distance itself is never treated as content
- Explorer camp / Tower / Warning Cave / Primal roadhead remain optional Capital Valley exploration and starting in town does not auto-mark them discovered
- actual sightline, patrol readability and travel feel remain CLIENT RUNTIME / PLAYTEST pending

NPC/facility UI:
- do not design final NPC chrome from scratch
- use the adopted external **Foozle RPG UI Set 1 (CC0)** as the primary visual skin; keep source/license records
- merchant / blacksmith / stable / summon are separate physical service surfaces, not shortcuts into global E-menu categories
- global Equipment is inspect/equip management only; purchase/sale/enhancement stay with physical NPC services
- global summon/history area may show records but cannot execute summon actions
- reduce wasted panel space and padding before shrinking readable text
- compile/build does not approve UI; client screenshots at multiple GUI scales are required

Implementation hygiene:
- do not keep placeholder/no-op APIs, commented-out systems, deleted tests, or duplicated economy authority merely to pass builds
- temporary pre-release reset is explicit technical debt with a release-removal gate; it must not silently become production persistence behavior
- physical facility screens share the common FacilityScreen shell; new NPC facilities must extend that shell instead of copying lifecycle/chrome/close logic
- when an external asset already solves the visual primitive, reuse it rather than creating another bespoke visual language


Implementation checkpoint:
- FacilityScreen now centralizes Foozle-skinned frame, compact sizing, refresh, close keys and world-preserving behavior for merchant/forge/stable/summon screens.
- The four physical service screens retain only task-specific controls and use smaller default panels.
- First-route landmark flags are independent; HUB_REACHED no longer implies Tower/camp/approach discovery.
- PreReleaseSessionReset remains temporary and must still be disabled/removed before release completion.


## Drabyel-gate opening + facility-shell validation — 2026-09-29

Implemented opening topology:
- fresh pre-release sessions start at New Drabyel's source-backed north entrance rather than the Primal roadhead or Explorer camp
- the entrance guide establishes the immediate story problem
- after one E-menu party check, the main objective/navigation points to the nearby CV_DRABYEL_ROAD encounter
- the approach placement seed is under 100 m from the entrance seed and remains outside the town safety ring
- CV_DRABYEL_ROAD is now CV-B + CV-C, keeping the first outdoor fight readable and short
- victory points the player back to New Drabyel; blacksmith/market/stable/summon onboarding continues from the physical hub
- HUB_REACHED no longer auto-marks Tower/camp/approach discovery; the older Capital Valley route remains optional exploration

Facility UI structure:
- merchant / forge / stable / summon now extend shared `FacilityScreen`
- shared shell owns compact panel sizing, Foozle production skin, refresh, close keys and world-preserving background behavior
- concrete screens own only task-specific controls/data
- new physical facilities must reuse this shell rather than duplicating screen lifecycle/chrome code

Build TURNBOUND #939 / run `36537666943` compiled production successfully but failed two stale assertions:
- the old 3-enemy CV_DRABYEL_ROAD composition contract
- a hub-guidance edge case where `HUB_REACHED` existed before a runtime hub site was promoted

The composition contract was intentionally updated to the new 2-enemy tutorial, while the hub edge case was fixed in runtime guidance rather than weakening the test.

Build TURNBOUND #940 / run `36537913723` verified commit
`12b44c003071ceceb1358d13cca5bdeb8e036635`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — `turnbound-v04-workbranch`, artifact id `11018834810`
- JAR SHA-256: `5b1f0db7bd4d4f1f6a8d068da8a52e7163a58b6f4f1c7fa6c5fb638f062f4c1e`
- MRPACK SHA-256: `6d00f616ef55fcc08f386cf847c470780b40e5b73c47cc357e3018aa2cd3e672`

Validation:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for this gate-start build
- PLAYTESTED: NO for this gate-start build
- MULTIPLAYER TESTED: NO


## Client corrective pass — dialogue / passive / navigation / tutorial materialization — 2026-09-29

Client evidence from the first Drabyel-gate walkthrough established:
- physical NPC dialogue chrome was acceptable, but the body was too shallow and silently clipped overflow
- Home 2×3 quick-menu orbs had been reduced too far
- Party preset footer labels disappeared because the controls were compressed below useful text width
- Codex character browsing spent too much height on filters and exposed level information that belongs in character management
- playable characters have a current roster contract of 3 combat skills (basic + 2 actives) plus a separate passive slot; P06 may show multiple passive entries inside that slot
- HUD objective could request the north-road patrol while navigation had no patrol target when generic live placement failed
- the player reached the authored patrol seed around 509/1733 without visible enemies, proving that target copy without a materialized encounter is not acceptable

Implemented contract:
- NPC dialogue uses a larger compact panel, tighter speaker/body spacing, wrapped multi-line text and mouse-wheel scrolling with a visible scroll indicator
- Home quick-menu orbs return to readable click-target size
- Party presets use three readable columns across two rows; co-op/apply use their own full-width action column
- Codex character filters are Ownership / Rarity / Role only; character cards do not show level
- Character management retains level information
- Skill detail exposes an explicit fourth Passive selector; active skill text and passive text use the same scrollable detail surface
- first tutorial navigation falls back to the source-backed Drabyel approach seed if the runtime site cannot be promoted, so objective text and navigation do not diverge
- first tutorial encounter activation no longer depends on optional roam-patrol resolution
- the Drabyel approach receives a source-seed site fallback and a locally surveyed two-enemy battle-footprint fallback when generic source-aware placement fails
- minimap always renders a target-direction arrow near the player when a navigation target exists

Build TURNBOUND #941 / run `36561739850` verified commit
`5e9758f55a3d79f741c524092d7cdfa11fa1651d`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11030790900`
- JAR SHA-256: `f929e78ee00a6868cd33a10cf4dec1f265e46609a3b4c40fb2e6139e5d7dff19`
- MRPACK SHA-256: `130de852e957484482e512ce5b096898b1383dbc60ce1a68e216d2b37c3f86d6`

Validation:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for the #941 repair
- PLAYTESTED: NO for the #941 repair
- MULTIPLAYER TESTED: NO


## Opening target + Gecko field-actor crash correction — 2026-09-29

Client playtest on Build #941 established two separate issues:
- while the active objective was to speak to the New Drabyel entrance guide, minimap navigation had no matching positional target and fell back to nearby exploration/travel presentation
- approaching the first visible CV-B/C field actors could crash the client inside GeckoLib render-state extraction with a null `ANIMATABLE_MANAGER`

Corrections:
- before the GREETER onboarding flag is set, navigation now targets the actual runtime GREETER service position; only if that service cannot be resolved does it fall back to the New Drabyel hub anchor
- after the greeter conversation, the non-positional E-menu instruction does not invent a world target
- discovered fast-travel markers no longer share the objective target's yellow cross presentation; they use the normal blue primary marker
- CV-B/C keep their visible held weapons, but `BattleActorHeldItemRenderer` no longer owns a parallel custom GeoRenderState data map
- held-item actors now use GeckoLib's normal LivingEntityRenderState/GeoRenderState path, preventing the render-state manager from being split across two stores

Build TURNBOUND #942 failed only because the minimap change referenced a nonexistent `BLUE` token. This was corrected to the existing `PRIMARY` token without changing behavior.

Build TURNBOUND #943 / run `36563883597` verified commit
`fe765605017312593e908fc8da49a43ff9c8d474`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11031345777`
- JAR SHA-256: `f699e8467643408e39d32dd128faa63deb39456089704a5ecb1afd2adaa25fde`
- MRPACK SHA-256: `ae702179eced490c7d0c8a7e48097200b1d3dc8714347573db4681a43cb4dc8f`

Validation:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for the #943 correction
- PLAYTESTED: NO for the #943 correction
- MULTIPLAYER TESTED: NO


## Client roster/settings + opening patrol visibility correction — 2026-09-29

Build #943 client playtest exposed three presentation/runtime issues:

- unowned character cards spent scarce compact-card width on the literal `미보유` label, truncating star/role information
- the New Drabyel opening objective tracked the patrol's authored home site instead of the roaming physical actors
- the opening-site fallback could promote a standing position that the visible-enemy runtime would later reject because source-content clearance was never checked during fallback selection

Corrections:

- unowned roster/codex cards keep their star + role line intact; ownership is communicated by desaturated/muted presentation plus a small portrait lock badge
- opening-patrol navigation follows the shared encounter runtime pivot while the patrol exists, with the authored site retained only as a fallback before materialization
- opening fallback now scans source-backed site + patrol seeds at a wider radius and requires standing clearance, source-content clearance, route-distance acceptance, and safety-zone exclusion before promotion
- if no valid site exists, the encounter remains dormant instead of exposing a marker with no physical enemies

Client settings were also separated from endgame content:

- the root menu now exposes distinct `도전` and `설정` destinations
- TURNBOUND-specific settings persist under `config/turnbound-client.properties`
- current game-specific controls: TURNBOUND music on/off + volume, TURNBOUND SFX on/off + volume, impact-camera feedback on/off, minimap on/off
- Minecraft video/input/master-audio options are intentionally not duplicated

Build TURNBOUND #944 / run `36566779865` verified commit
`2f76a57d52a98ad8acf3014e59ddd2474eec2b64`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11032003009`
- JAR SHA-256: `e016a714f0ee768eeb356792e54ba8a20e0d11249021ef1025ba41c97d3c9812`
- MRPACK SHA-256: `e45c85272dd575112ffa84901fd89a913e2b8b5e63c5cd900e9e82d52d63c2d8`

Validation:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #944
- PLAYTESTED: NO for Build #944
- MULTIPLAYER TESTED: NO

## Opening patrol physical locomotion + battle view correction — 2026-09-29

Build #944 client playtest established two separate runtime failures while the encounter/session itself remained alive:

- the New Drabyel north-road actors materialized, alerted and played GeckoLib walk clips, but their world coordinates did not actually advance
- contact still entered combat and battle UI state synchronized, but the 3D battlefield view was displaced into an almost sky-only composition

The original client log from that playtest is no longer available. This correction is therefore based on the supplied screenshots plus direct source tracing; no missing log evidence is being inferred.

Field-locomotion root causes found in source:

- PathNavigation.moveTo(..., speedModifier) was fed values such as 0.035, 0.075 and 0.095 as though they were direct blocks-per-tick speeds; they are navigation speed modifiers, leaving the field actor effectively crawling
- a successful path request immediately enabled the walk animation even before entity coordinates changed
- physical-progress detection compared the actor against pivot after pivot had already been overwritten with that same current actor position in the same tick, so real displacement could not be measured reliably
- ALERT path targets reused the lead actor's Y instead of the player's live Y, making slope/step pursuit unnecessarily fragile
- stalled recovery only handled completed patrol navigation; an active path that made no physical progress could remain in a false walking state
- follower placement reused the lead Y without grounding its local formation point

Corrections:

- PATROL / RETURN / ALERT now use ordinary navigation speed modifiers (0.72 / 0.90 / 1.05)
- world-coordinate delta is captured before replacing the runtime pivot and is the authority for locomotion presentation
- walk/idle state is driven by recent physical coordinate progress, not by path-request acceptance
- ALERT targets the player's live position while engagement distance remains horizontal
- patrol, return and alert all receive bounded no-progress recovery; blocked patrol points are skipped after the recovery window
- follower formation points are grounded to nearby live terrain when the elevation difference remains locally plausible

Battle staging/view corrections:

- a surveyed fixed arena is re-grounded from its X/Z against the live world's current motion-blocking surface immediately before battle acceptance
- the client-local detached camera anchor now synchronizes its previous transform with every authored transform; because that anchor is intentionally not part of the client level entity tick list, leaving its previous position stale can make camera interpolation sample a distant origin rather than the battle pivot

Narrow regression coverage was added for physical-progress thresholds, walk-animation truth, and active-navigation stall recovery.

Validation at implementation checkpoint:
- CODE REVIEWED: YES
- TESTED: PENDING Build TURNBOUND
- BUILD VERIFIED: PENDING Build TURNBOUND
- JAR PRODUCED: PENDING Build TURNBOUND
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Build TURNBOUND #945 / run 36573039923 verified commit
3cb1e083445be10c5bd4cd094d08f84299ec754c:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id 11036836387
- JAR SHA-256: 8bd8ec7c3a3ece57936eb29595ad0eb1328a50f85d6a96ce65fc893e9f89ba67
- MRPACK SHA-256: 4632867ca7bb40662997215e5b4fd30db7127de11a075441c934e80e1ebac817

Validation after Build #945:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #945
- PLAYTESTED: NO for Build #945
- MULTIPLAYER TESTED: NO

## Single field encounter representative — 2026-09-30

Design correction after the Build #945 patrol discussion:

- a field encounter is a world-space contact proxy, not a literal rendering of every combatant that will appear after battle transition
- production field encounters now use exactly one authoritative physical representative entity regardless of combat enemy count
- `fieldVisibleCount` remains in route schema v1 only for compatibility, but production validation requires the value to be exactly `1`
- all current Capital Valley / New Drabyel route entries were normalized to one field representative
- `DrehmalVisibleEncounterService` now treats the field object explicitly as a representative and no longer maintains follower formation movement for decorative second actors
- persisted older multi-actor field groups fail adoption against the new one-representative contract and are rebuilt as a single proxy
- contact still resolves the unchanged authored battle encounter, so the New Drabyel opening remains the two-enemy `CV_DRABYEL_ROAD` fight

Presentation decision:

- ordinary patrols and wildlife groups use one readable representative silhouette in the world
- group identity is communicated through encounter/objective naming and context rather than extra pathfinding entities
- do not fake a crowd with multiple independently moving field entities solely for decoration
- if a later high-importance encounter needs multiple silhouettes, use one authored composite model/animation controlled by one entity; that is a presentation asset decision, not extra combat/navigation authority

This supersedes the Build #945 follower-grounding work for ordinary field encounters. The #945 lead navigation, physical-progress locomotion truth, stall recovery, arena grounding and client camera-anchor fixes remain active.

Validation at implementation checkpoint:
- CODE REVIEWED: YES
- TESTED: PENDING Build TURNBOUND
- BUILD VERIFIED: PENDING Build TURNBOUND
- JAR PRODUCED: PENDING Build TURNBOUND
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Build TURNBOUND #946 / run `36647758356` verified commit
`8aa7b6f45017474bd7828b7b55a219409c8bb24f`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11068739658`
- JAR SHA-256: `e7761717c1937bd2c457a115f633c88e4946119d4cd3e112bc85fed00838ca3d`
- MRPACK SHA-256: `9af1a3363892c44a3b2c35b9a1a831904e3075020bf4a40d269d825e82e1bec6`

Validation after Build #946:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #946
- PLAYTESTED: NO for Build #946
- MULTIPLAYER TESTED: NO

## Build #946 client regression: objective present but field proxy absent — 2026-09-30

Client evidence:
- the HUD/minimap still resolved the New Drabyel north-road patrol objective at roughly 19 m
- no visible field representative was present in the world view
- the supplied error-only log did not contain TURNBOUND runtime/materialization lines; it contained an early Minecraft framerate-tracker input NPE and repeated invalid vanilla spawn-egg model identifiers (`minecraft:item/spawn_egg_2D`), so those entries are not used as the cause of the missing TURNBOUND proxy

Source tracing found a materialization lifecycle weakness independent from the single-proxy design:
- `DrehmalAdaptiveRoutePlacement` caches the route snapshot once per ServerLevel
- the field service rechecked `sourceContentClear` against that cached survey origin on every tick before accepting an existing actor
- source-map decoration/entity loading can complete after the snapshot is resolved; a later conflict at the cached origin could therefore make the service silently discard/refuse the representative while navigation still retained the encounter position
- the opening combat slot intentionally has no formal Patrol binding, so the proxy also had no normal patrol points even though source-backed presentation patrol seeds exist
- `TurnboundBattleActors.spawn` previously ignored the boolean result of `ServerLevel.addFreshEntity`, so a failed insertion had no direct materialization signal

Correction:
- keep an already-live/adopted TURNBOUND representative authoritative instead of rechecking and deleting it against the old survey origin every tick
- when no representative exists, resolve a fresh live-ground materialization point; if the cached origin is blocked, search nearby source-backed site/patrol seeds with terrain, safety-zone, route-corridor and source-content checks
- move the encounter pivot/HUD authority to the recovered proxy position
- derive presentation-only patrol points from source-backed map placement seeds when no formal Patrol is bound; this preserves encounter activation independence while allowing the opening proxy to actually stroll
- observer materialization now follows the live proxy pivot instead of remaining centered only on the original site
- verify `addFreshEntity` success and emit one-shot TURNBOUND warnings for no-safe-point or insertion failure instead of silently presenting an empty objective
- single-proxy battle separation remains unchanged: the world uses one representative; `CV_DRABYEL_ROAD` still expands to two real enemies in combat

Validation at implementation checkpoint:
- CODE REVIEWED: YES
- TESTED: PENDING Build TURNBOUND
- BUILD VERIFIED: PENDING Build TURNBOUND
- JAR PRODUCED: PENDING Build TURNBOUND
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Build TURNBOUND #947 / run `36649740807` verified commit
`da253f6cfd5c95808662648c37d731e29246971d`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11070022864`
- JAR SHA-256: `17a9dcbcb18b1b6965f44ca9450cce9b44ad39b33e0d36c97a4aadc0e37ed62f`
- MRPACK SHA-256: `3362f77d7d57dd42f0867182725f8d838d27550ea952398f8578fc111737712b`

Validation after Build #947:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #947
- PLAYTESTED: NO for Build #947
- MULTIPLAYER TESTED: NO


## Build #947 client battle cleanup pass — 2026-09-30

Observed in the real Build #947 client:
- the New Drabyel north-road encounter can now reach a normal 3D battle and proceed through victory to the result screen
- the battle arena/camera is visible rather than the earlier sky-only staging failure
- a defeated road enemy remained in the arena indefinitely in its held death pose; the authored CV-B/C death clip lowers/rotates body bones and becomes visually broken when held forever
- the result screen preserved all four party members but the fourth growth row intruded beyond the framed panel / footer area at the tested compact GUI viewport
- the supplied error-only log contains repeated invalid vanilla/external resource model paths using `minecraft:item/spawn_egg_2D`; no TURNBOUND battle exception is present in that supplied excerpt, so those resource errors are not treated as the cause of either presentation issue

Downed/revive presentation correction:
- normal defeated enemies now play a short death beat and then retire from the 3D arena instead of occupying a slot as a permanent broken corpse
- bosses retain a slightly longer defeat beat before retirement
- recoverable non-summon allies play their authored down animation, then their model is hidden and the formation slot becomes a low in-world `전투불능 · 이름` recovery marker
- the marker is presentation only; DEAD_ALLY_SINGLE targeting continues to use the same server-authoritative combatant/formation coordinates
- on revive, the marker is removed and the actor becomes visible at the same slot with the existing authored revive/get-up animation and VFX
- delayed/self-revive uses the same path
- downed summons may retire visually and respawn with revive presentation when their state becomes living again
- retired downed visuals are tracked so snapshot refreshes do not immediately respawn defeated actors

Battle Result compact correction:
- result layout now reserves a footer area for `현장 복귀` / inventory-cleanup controls
- when four full growth rows cannot fit above that footer, party growth switches to a 2×2 compact grid
- compact cells retain portrait, name, level transition / +Level, XP bar and XP text
- large viewports keep the existing detailed single-column rows
- pure layout tests pin the compact viewport and large viewport behavior

This pass intentionally does not redesign rewards, progression or combat balance.

Validation at implementation checkpoint:
- CODE REVIEWED: YES
- TESTED: PENDING Build TURNBOUND
- BUILD VERIFIED: PENDING Build TURNBOUND
- JAR PRODUCED: PENDING Build TURNBOUND
- CLIENT RUNTIME TESTED: Build #947 YES for the reported battle/result path; new fixes NOT YET
- PLAYTESTED: PARTIAL — New Drabyel opening battle reached victory/result on Build #947
- MULTIPLAYER TESTED: NO

Build TURNBOUND #948 / run `36651974962` verified commit
`9f971bc5cae5ac17ff0b615decb5c2b7e31e89f1`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11071375001`
- JAR SHA-256: `0bd3c3d352c929908ce4a40e1007e69751e7dfcd54846ae9a515bc704d9e116d`
- MRPACK SHA-256: `5333cb5fbd0e04a91625f881c8932b9a3f60f53519764b3f855ff57f1ee7b963`

Validation after Build #948:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #948
- PLAYTESTED: NO for Build #948
- MULTIPLAYER TESTED: NO


## Build #948 follow-up: revive/UI density/reference pass — 2026-09-30

Real-client feedback from the Build #948 playtest established four presentation problems and one reward-clarity gap:
- enemy downed visuals also need a persistent logical recovery target because future enemy revive kits are allowed
- character Skill detail used too much height for navigation and too little for the actual description
- the field minimap and objective panel covered too much of the authored world
- Turn Order portraits were harder to identify without names
- battle equipment drops were committed server-side but the drop was not being appended to the result-summary equipment list, so the existing `획득 · ...` result line could remain empty

Reference review for the Skill screen:
- Honkai: Star Rail character/skill surfaces keep category/selection navigation separate from a large selected-detail reading area
- Reverse: 1999 character Details/Skills surfaces likewise prioritize the current kit text rather than compressing it into a short bottom strip
- TURNBOUND therefore keeps the existing Foozle visual language but changes information architecture: vertical skill rail on the left, full-height selected skill/passive reading pane on the right

Implemented:
- every non-summon combatant, ally or enemy, now transitions from authored down animation to a low in-world `전투불능 · 이름` marker instead of deleting enemy recovery state
- summons may still retire visually while down
- true same-action `AUTO_REVIVE_ONCE` passives can use `REVIVE_IMMEDIATE_TURN`; their revive animation/VFX is presented even though no intermediate downed snapshot reaches the client
- normal revive Turn Gauge is target-specific and data-driven through `reviveStartGauge`
- current v1 playable baseline ranges from 90 to 340; P06 Last Page uses 500 after its two-action delay; P04 Returned Breath adds its existing +150 on top of the target's own return value
- Skill tab now uses a vertical Basic/Active/Passive rail and a much larger right-side description pane with scroll support
- minimap and objective panel are rendered at 80% of their previous visual scale, including text/padding/markers
- objective width remains viewport/state-driven, not text-length-driven; scaling is anchored to the upper-right so longer copy cannot expand left into the centered navigation cue
- Turn Order reserves a second line and renders a small character name below every portrait
- battle equipment drops are appended to the authoritative result summary; the existing result screen now renders `획득 · <장비명>`, or marks it as `보상 대기` when inventory overflow queued it

Equipment canon reconfirmed:
- normal equipment sources are **battle drops + merchant + quests + bosses**, not merchant-only
- normal merchant sells T1/T2; higher-tier sources are progression/combat rewards
- equipment remains a streamlined turn-RPG system: Weapon/Armor/Accessory + Signature, predictable main/sub stats, at most one clear fixed trait, +0..+10 Gold enhancement, no fail/break and no random multi-line substat reroll loop

The supplied client log was reviewed separately. It contains repeated Minecraft model-parser failures caused by the invalid resource identifier `minecraft:item/spawn_egg_2D` and no TURNBOUND package/stack entries. That resource issue is not used as the cause of these gameplay/UI symptoms.

Validation requested by this checkpoint:
- CODE REVIEWED: YES
- TESTED: PENDING Build TURNBOUND
- BUILD VERIFIED: PENDING Build TURNBOUND
- JAR PRODUCED: PENDING Build TURNBOUND
- CLIENT RUNTIME TESTED: NO for this follow-up
- PLAYTESTED: Build #948 screenshots/log reviewed; new follow-up NOT YET
- MULTIPLAYER TESTED: NO


Build TURNBOUND #949 / run `36659991560` verified commit
`b23e4dcb4ce680bd4a40214c3a2acabc4ca7b4e3`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11074026549`
- JAR SHA-256: `ffe96e1347a1b9514a136e7bdd382e7e27b84187e4a162d8e28321ac29b5d92c`
- MRPACK SHA-256: `501f75e9fe2cf028822b7b927cf73dc2b5acc02bdbb332d7f204a03a21fe24ee`

Validation after Build #949:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #949
- PLAYTESTED: NO for Build #949
- MULTIPLAYER TESTED: NO


## Action Gauge + SPD/Gauge audit — 2026-09-30

Post-Build-949 battle UI/system pass requested before the next client playtest.

Reference finding:
- modern turn RPGs commonly expose more than a flat portrait queue: Honkai: Star Rail exposes portrait order with optional action-value information, while Epic Seven exposes a Combat Readiness gauge whose movement is distinct from Speed
- TURNBOUND keeps its own visual language and combines the useful information: authoritative future order + current Gauge + effective SPD

Implemented:
- the old top-center portrait-only Turn Order rail is replaced by a left-edge **행동 게이지** panel
- each regular scheduler combatant row shows portrait, name, current Gauge, current effective SPD and authoritative upcoming order slot(s)
- repeated future actions are visible as repeated slot numbers instead of requiring the player to infer them from raw Gauge
- Gauge >= 1000 is READY; overflow is shown as `READY+N` because overflow survives the action cost
- downed units and P07-style non-scheduler summons do not occupy the action gauge
- standard solo 4v5 can display all 9 regular combatants; shared battles compact the visible set and disclose hidden row count
- server snapshot now carries effective SPD plus scheduler participation, and the future-order preview horizon was extended from 8 to 12
- Gauge manipulation still uses the previous short movement/accent feedback instead of snapping invisibly

SPD/Gauge audit:
- fixed-point TurnScheduler remains the single authority for runtime actor selection and HUD preview
- SPD changes future Gauge fill rate; it does not rewrite Gauge already accumulated
- action cost remains exactly 1000 Gauge and overflow is preserved
- lower current Gauge can correctly act before a higher-Gauge unit when its effective SPD makes its time-to-ready shorter
- player-choice time still pauses logical Gauge progression
- P01-P08 base SPD remains 84..114; F04=78 is retained as the explicit ultra-slow low-rarity shield exception
- enemy 117+ SPD entries remain only where the enemy role is intentionally tempo-heavy rather than being blanket-clamped to player bands
- equipment flat SPD already reaches final battle stats correctly

A real runtime gap was found and repaired:
normal-equipment Gauge traits were being copied into CombatantDefinition rules but had no combat consumer.
The authoritative engine now consumes:
- `START_GAUGE_N` once at battle creation and stacks different equipped sources
- `DIRECT_HIT_GAUGE_N` once per hostile direct action when the wearer survives
- `ALLY_GAUGE_GRANT_PLUS_N` on positive Gauge grants to another ally

Past v0.4 Signature rule strings are not automatically reinterpreted here because current v1 Signature mechanical details are not fully specified by the present v1 design docs. This pass deliberately fixes only semantics that are explicit in the current normal-equipment data.

Regression coverage added for:
- lower-Gauge/faster actor ordering
- SPD modifier changing future fill rate without rewriting accumulated Gauge
- >1000 overflow retention
- Action Gauge row/order/READY overflow projection
- scheduler exclusion of downed/non-regular summons
- client snapshot effective-SPD/scheduler fields
- normal equipment start-Gauge, direct-hit Gauge and ally-grant Gauge traits
- left Action Gauge geometry across small/standard viewports

Validation requested:
- CODE REVIEWED: YES
- TESTED: PENDING Build TURNBOUND
- BUILD VERIFIED: PENDING Build TURNBOUND
- JAR PRODUCED: PENDING Build TURNBOUND
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO for this pass
- MULTIPLAYER TESTED: NO


Build TURNBOUND #950 / run `36662577804` verified code commit
`a7dcbc568747ea67402b6f7947cab6d2628989ee`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11074543891`
- JAR SHA-256: `bbb3221ae023b6febf4a9cf9911593b527bc2c804c671fb42970f3b7d242a0dc`
- MRPACK SHA-256: `1e9e629dffaaa5c02a3bff34c9729c7e1bb79a1a71bfd1121d4e18ee05a80938`

Validation after Build #950:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #950
- PLAYTESTED: NO for Build #950
- MULTIPLAYER TESTED: NO


## Build #950 client regression: intermittent missing field representative — 2026-09-30

Observed:
- the opening objective/minimap still resolved the north-road patrol at about 5 m, but no enemy representative was visible
- the same encounter has appeared in some previous client runs and disappeared in others, indicating lifecycle/materialization instability rather than a permanently missing model
- Build #950 itself did **not** touch any `world/` placement/runtime file; the #949→#950 diff only changed battle HUD, combat Gauge runtime, snapshot codecs, tests and docs

The supplied error-only log contains no `TURNBOUND`, Drabyel, or field-representative line. It consists of repeated Minecraft model-load failures for the invalid identifier `minecraft:item/spawn_egg_2D`; therefore it does not establish the cause of the missing patrol.

Source audit found three independent instability paths in the existing field runtime:

1. **observer-gap deletion**
   - every tick with no eligible observer within 72 blocks called `discardActors()` and `resetToRoute()`
   - this contradicted `setPersistenceRequired()` and turned a persistent encounter object into repeated delete/recreate churn

2. **load-timing-sensitive placement**
   - respawn/materialization reused `sourceContentClear`, the broad arena/content-protection rule
   - a villager, item frame, armor stand, or nearby decorative block entity several blocks away could invalidate a single roaming proxy
   - because those source entities can finish loading at different times, an identical save could select/accept the proxy on one run and reject it on another
   - the opening logical site and presentation patrol points used the same overly broad rule

3. **temporary chunk absence treated as actor loss**
   - `actorsAlive()` sees an unloaded UUID as absent
   - recovery could clear the tracked UUID and attempt reconstruction without first proving the pivot chunk was loaded
   - tagged adoption was also limited to an 80-block box even though the opening patrol span can exceed that distance

Correction:
- no-observer state now pauses navigation/alert state but retains the representative and its physical pivot
- true cooldown/battle claim still removes the proxy as intended
- actor recovery waits while the pivot chunk is unloaded; a missing actor in a loaded pivot chunk is now logged as an ERROR and recovered
- adoption radius is widened to 128 blocks
- a dedicated narrow `fieldProxyContentClear` rule replaces arena-style broad clearance for opening-site, field materialization and patrol-waypoint decisions
- broad `sourceContentClear` remains intact for battle arenas/source-content-sensitive placement
- materialization and insertion failures are ERROR-level one-shot diagnostics so the user's error log will contain the relevant TURNBOUND reason if this invariant fails again
- additional lifecycle audit found that `DrehmalVisibleEncounterService.tick` could be called for an ACTIVE player after changing dimensions; because the service is a single bound-level runtime, this could rebind away from the external Overworld and clear its field actors. The service now ignores callers unless `ExternalWorldBootstrap.active(caller)` is true.

Build TURNBOUND #951 verified the main observer-retention / narrow-clearance / loaded-chunk recovery changes before this final cross-dimension guard. The guard is included in the next checkpoint build rather than treating #951 as the final client artifact.

Validation requested:
- CODE REVIEWED: YES
- TESTED: PENDING Build TURNBOUND
- BUILD VERIFIED: PENDING Build TURNBOUND
- JAR PRODUCED: PENDING Build TURNBOUND
- CLIENT RUNTIME TESTED: NO for this correction
- PLAYTESTED: Build #950 screenshot reviewed; corrected runtime NOT YET
- MULTIPLAYER TESTED: NO


Build TURNBOUND #952 / run `36664175020` verified final lifecycle commit
`ed9a349805d7b27f7f17f1db1da7f5d1500a7713`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11075761772`
- JAR SHA-256: `2d295243d33fa7c75dbf656f81ad8c5c0541a9f91b68f2217b4603009544aaa3`
- MRPACK SHA-256: `2cfd6e7750f93525814746be3d1a0b31cf83973884f39642d5506cb691484855`

Validation after Build #952:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #952
- PLAYTESTED: NO for Build #952
- MULTIPLAYER TESTED: NO


## Action Gauge simplification before playtest — 2026-09-30

User direction:
- the action gauge only needs roughly Epic Seven-level readability
- adding raw SPD, raw Gauge numbers and repeated future-slot telemetry to every row made the HUD harder to scan

Correction:
- keep the authoritative server TurnScheduler ordering from the previous pass
- keep one thin Gauge readiness bar per combatant
- keep portrait + small name + current actor emphasis
- remove always-visible raw Gauge numbers, SPD numbers and repeated future-slot strings from the battle HUD
- narrow the action-gauge panel from the telemetry-heavy 222/184/148 px design to 142/124/108 px by viewport class
- row position/order itself communicates turn sequence; the thin bar communicates current readiness
- speed/Gauge combat rules and snapshot data remain intact internally; this is a presentation simplification, not a scheduler rollback

The design system now explicitly treats detailed SPD/Gauge telemetry as secondary information rather than permanent combat-HUD chrome.

Validation requested:
- CODE REVIEWED: YES
- TESTED: PENDING Build TURNBOUND
- BUILD VERIFIED: PENDING Build TURNBOUND
- JAR PRODUCED: PENDING Build TURNBOUND
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO for this simplification
- MULTIPLAYER TESTED: NO


Build TURNBOUND #953 / run `36665400253` verified commit
`db6183e62782219cd432df0b2c75aa079e873ac8`:

- Gradle test/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click Modrinth pack verification: PASS
- artifact upload: PASS — artifact id `11076390361`
- JAR SHA-256: `55846c7c364edf1893d28321243329835fa43f0d8e0cc65fc1747e9cf98eb0a3`
- MRPACK SHA-256: `03052601d2439f40d11f238c4a314bac2906fccf250250c7da1bf55659c1eefd`

Validation after Build #953:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CLIENT RUNTIME TESTED: NO for Build #953
- PLAYTESTED: NO for Build #953
- MULTIPLAYER TESTED: NO


## Av'Sal expansion direction — 2026-09-30

Latest explicit direction:
- story, quest structure, objective variety and chapter outcome variants may be authored as part of the expansion without requiring one-off approval for every beat
- achievements remain inside the Quest journal; Hard Boss/Rift are a separate high-difficulty activity surface
- next roster growth starts with P09~P12 and must avoid near-duplicate kits
- new hero visual design is **external-asset-first**; do not invent final costumes/faces internally
- preferred directly usable/editable sources are clearly licensed assets such as CC0 Quaternius/Kenney packs
- choose the visual base first, then finalize name/personality/weapon/kit around what the asset actually supports
- the next major world/content slice is `AVSAL_EXPANSION_v1.md`


## Av'Sal first production slice — Build TURNBOUND #954 — 2026-09-30

Code commit:
- `bf228f7de380896c8cc9c070d8c996a236f021bc`
- commit: `feat(turnbound): build Av'Sal first production route`

Implemented:
- New Drabyel story briefing after the opening patrol and map review
- source-backed west-road X/Z seeds from `zachaa/DrehmalMap@72d82180cbe3f950f068cf2d8e8668c6b09d5c58`
- live 26.2 Y / collision / source-content / battle-footprint checks
- MQ_AV01 roadside echo milestone, visible road patrol, optional Elite, Av'Sal outskirts arrival
- MQ_AV01 completion persisted through existing external-world SavedData
- MQ_AV02 becomes the active journal objective at the outskirts
- common patrol battle pattern = opportunist frontliner + telegraphed marksman + field support
- field presentation remains one physical representative; battle composition expands only after contact
- multiplayer encounter visibility/participation is gated to players who have actually started the Av'Sal chapter

Build TURNBOUND #954:
- GitHub Actions run: `36676970799`
- Gradle clean/test/build/oneClickPack: PASS
- NeoForge dedicated-server smoke: PASS
- JAR verification: PASS
- one-click mrpack verification: PASS
- artifact upload: PASS

Validation:
- CODE REVIEWED: YES
- TESTED: YES (automated tests in Build #954)
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- DEDICATED SERVER SMOKE: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

The next production unit is Av'Sal outskirts investigation -> north dock Midboss. MQ_AV02 is intentionally started but not claimed complete by this slice.


## Open-world objective / navigation expansion — 2026-09-30

Current production direction:
- quest rewards scale upward with meaning, danger and discovery value; hidden or high-difficulty objectives may pay above ordinary side objectives
- no daily/weekly/login chore economy; rewards come from exploration, quests, encounters, secrets, bosses and authored repeatable regional activity
- New Drabyel service NPCs and field NPCs use the existing licensed external-model pipeline; future playable characters, NPCs and enemies remain external-asset-first
- creature identity is intentionally unconstrained: humanoids, animals, constructs, spirits, animated objects, plants/fungi, tiny creatures, massive creatures and other silhouettes are valid when gameplay identity supports them
- content expansion favors distinct mechanics/silhouettes over simply adding recolored humans or generic beasts
- multiple objectives may coexist; the legacy tracker capacity is 5 and the authored-world map can project several active quest targets at once
- exact server-resolved NPC/service positions are projected to the world map/minimap with small readable names
- current navigation points directly at the next required hub NPC when that NPC has a verified runtime position
- field NPCs can reveal hidden quests by conversation; hidden quests remain invisible before discovery
- New Drabyel's former player-facing "마구간" service is now **역참**. Fast travel and mount rental are separate physical actions at the same waystation service.
- the first production mount is the external-asset-based 길뿔 산양; it is a dedicated rental entity, not an enemy or vanilla-horse reskin
- avoid travel padding: first-time traversal may establish scale, but repeated routes receive discovered waypoints/shortcuts; long roads should contain meaningful encounters, NPCs, discoveries or route choices instead of empty walking
- the New Drabyel → Av'Sal road gains return waypoints at 끊긴 가도 and 아브살 외곽 so the long first journey does not become repeated commuting


## World-scale / physical-NPC / early-distance canon — 2026-09-30

- Physical NPC services are authoritative. Global E-menu is management only; shop, sell, forge, summon and travel actions must stay at their corresponding world NPC/facility.
- MetaFacilityActionGate remains the server-side anti-shortcut boundary.
- Entrance guide Aren is now the main-quest giver for the New Drabyel opening sequence.
- Post-hub early movement targets a 64-110 block ring, with a 2-of-3 local investigation before larger regional travel.
- Quest targets represented by actual entities use a wall-through glowing outline while active; coordinate-only targets should be promoted to physical interactable proxies when they need the same treatment.
- Av'Sal is no longer an immediate early-game destination. It requires the local New Drabyel arc plus at least one meaningful Capital Valley regional milestone and map review.
- Source data shows a much larger world than the current route: 285 structured overworld locations and hundreds of path features, so later development should unlock whole regions over time rather than treating the existing road as the entire game.
- Physical waystations now support the 길뿔 산양 rental mount. Long first-time journeys may establish scale, but repeated empty walking remains unacceptable.


## New Drabyel local-flow / physical travel checkpoint — 2026-10-01

Direct source recheck:
- pinned structured source remains `zachaa/DrehmalMap@72d82180cbe3f950f068cf2d8e8668c6b09d5c58`
- `data/locations.json`: 376 total locations / 285 overworld locations
- overworld structured-location bounds span about 31,409 blocks east-west and 11,122 blocks north-south
- `data/paths.geojson`: 644 features = 643 LineStrings + 1 Polygon, with 11,066 LineString coordinate points
- no separate named structured location sits in the >100 to <=300 block band around the New Drabyel source hub; Explorer's Campsite is the nearest next named macro landmark at roughly 313 blocks from the current opening reference
- early progression therefore uses compact live-resolved micro-content around the hub instead of treating the lack of a named macro POI as empty travel space
- Av'Sal remains preserved content, but its first briefing stays behind the local 2-of-3 investigation plus an additional meaningful Capital Valley milestone

Physical-service / navigation repair:
- world-map fast-travel markers are informational only; clicking the world map no longer teleports the player
- server authority accepts fast travel only while the player is physically beside a New Drabyel stablemaster or a remote waystation keeper
- waystation travel uses a short departure fade, delayed server-authoritative safe teleport, then arrival fade
- current tracked physical objective outline is projected per player and only within the final 52-block approach
- the server no longer sets a shared glowing tag from the union of all players' quest targets
- the entrance greeter player-facing name is now `문지기 아렌`, matching the current masculine model read
- after the local 2-of-3 main investigation, physically speaking with 기록관 세린 can open the optional `북쪽 옛길의 야영지` side objective; it reuses the source-backed Explorer's Campsite and naturally chains into the existing camp NPC/hidden-quest content instead of inventing another menu
- waystation departure/arrival now has short vanilla horse-travel feedback layered onto the existing fade; no battle/skill sound is repurposed

Validation for this checkpoint:
- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- NEW JAR: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

No build/CI was requested for this content batch; validation remains intentionally deferred until a larger playable unit is ready.


## Fast field respawn / repeatable progression checkpoint — 2026-10-01

Latest explicit direction:
- ordinary field enemies should repopulate quickly; long empty cooldowns are not desirable for farming/exploration loops
- bosses/world bosses may remain long-reset or first-clear persistent
- repeatable regional contracts are a major fallback growth path when authored main/side content is temporarily too difficult
- co-op participants who actually join and win a shared battle must receive their own battle-based quest/contract progress

Runtime/data changes:
- Capital Valley common respawn seconds: 20 / 22 / 25 seconds depending on encounter
- Av'Sal common road patrol: 30 seconds
- Warning Cave Elite and Av'Sal road Elite: 300 seconds (5 minutes), reduced from 900
- Graul remains first-clear world-state content and does not naturally reappear on the road
- regional_contracts_v1.json adds three repeatable Capital Valley contracts
- contracts unlock after the New Drabyel north-road opening battle
- contracts are accepted/reported physically through 기록관 세린, one active at a time
- active contract progress is visible in the existing quest journal
- rewards are repeatable Gold + party XP; no repeatable Crystal is granted by these contracts
- battle equipment drops continue separately, so the combined loop improves level + Gold-funded equipment progression
- contract state reuses the existing per-player QuestProgress persistence maps; no save-format field was added
- shared-battle settlement already commits each participant separately; regional contract battle progress now sits inside that same per-owner commit path
- non-participating distant party members do not receive free battle quest/contract credit

Validation state for this checkpoint:
- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- NEW JAR: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Build/CI remains deferred until the current content batch is larger, matching the present project testing cadence.


## Open-world roaming / contract-tier / non-humanoid checkpoint — 2026-10-01

Direction locked:
- enemies and playable parties are not humanoid-only
- Capital Valley must visibly mix wildlife, humanoids and supernatural/construct/nature silhouettes
- roaming field presence should be distributed through meaningful territories instead of isolated stationary spawn dots
- higher repeatable-contract tiers unlock from party growth, not from a hard dependency on main-story completion
- repeatable contracts have their own physical New Drabyel NPC rather than piggybacking on the story archivist

Current data/runtime additions:
- new common encounter CV_HOUND_ROAM — Ash Hound pair, level 5, 22s respawn
- new common encounter CV_SPORE_GROVE — Spore Lantern + Root Guard, level 8, 25s respawn
- new Elite CV_BRIAR_STAG — Briar Stag, level 12, 180s respawn
- route/map placement adds live-resolved sites/arenas/patrol seeds for all three
- existing first-region common sites gain roaming presentation patrol seeds where available
- New Drabyel adds physical 지역 의뢰관 로웬 as the regional-contract NPC
- contract Tier 1: level 1+
- contract tiers are not hard-gated by average party level; tier/minPartyLevel is recommendation metadata only
- harder contracts may be accepted early; actual encounter difficulty is the readiness check
- contract tier selection is independent of specific main-quest completion after the player reaches New Drabyel
- contract pool contains 9 contracts across three tiers
- future playable roster selection explicitly evaluates beasts/constructs/spirits and other non-humanoid bodies

External-asset recheck:
- Quaternius Ultimate Animated Animal Pack remains CC0 with 12 animated animals
- Quaternius Ultimate Monsters remains CC0 with 50 animated monsters
- current Capital Valley uses already integrated creature assets first; new imports happen only when a new silhouette is actually needed

Validation:
- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- NEW JAR: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

No build/CI for this content/data batch yet.


## New Drabyel → Av'Sal first playtest gate — 2026-10-01

Latest explicit milestone:
- continue expanding content/equipment rather than stopping at feature scaffolding
- first user playtest happens after the route through Av'Sal arrival is content-complete enough to judge
- map placement quality is part of the gate

Implemented in the current content batch:
- Av'Sal west road gains Ash Hounds, a courier NPC, an overturned-cart discovery and rusted-sentry encounter in addition to the existing echo/patrol/Elite
- all added route positions are source-corridor search seeds; live 26.2 terrain and source-content clearance still decide final actor/arena positions
- Av'Sal road encounter levels now progress through Lv6 / Lv7 / Lv9 with the optional Elite at Lv11
- Capital Valley common fights feed T1 equipment; harder Capital Valley and Av'Sal feed T2 equipment
- early equipment drops use the existing durable settlement transaction as the deterministic roll source
- New Drabyel normal T1/T2 shop has no party-level/story hard gate; price and route danger are the readiness checks
- stale loot-data shop/sale numbers were aligned to the current equipment-economy canon
- Av'Sal physical discovery adds Tier 4 regional contracts with no party-level hard gate
- the Tier 4 pool is region-discovery gated, not hard-wired to party level or a later MQ completion
- Av'Sal outskirts gains a physical contract broker sharing the player's existing server-owned regional-contract state
- multiplayer field NPC eligibility checks the interacting player's own Av'Sal flags

Remaining before calling the first route playtest-ready:
- static/compile validation of this integrated batch at the chosen checkpoint
- actual client validation of live map placement, travel density, encounter sightlines, NPC collision and equipment pacing
- travel transition SFX is implemented with horse travel/landing feedback
- the rideable 길뿔 산양 rental mount is implemented from a tracked MIT external rig

Validation for this content checkpoint:
- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- NEW JAR: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


## Av'Sal first boss / summon-stage checkpoint — 2026-10-01

The first user playtest gate now extends through the first Av'Sal boss rather than stopping at outskirts arrival.

Route/content density before the boss:
- MQ_AV01: New Drabyel -> Av'Sal approach
- side: Ash Hounds, issued from Aren's expedition briefing
- physical courier Deren: missing cart / rusted sentries / optional off-road Elite cluster
- MQ_AV02: outskirts investigation, any 2 of 3
- MQ_AV03: inner relay loop, any 2 of 3
  - west distribution room: environment interaction
  - aqueduct relay guard: combat route
  - east relay bypass: environment interaction
- MQ_AV04: first boss at the north aqueduct gate

First boss:
- id: AV_B01 / encounter AV_FIRST_BOSS
- player label: 수로 집행기 카르논
- Barrier cycle: direct hits while the Barrier is active trigger a real reaction counter
- 55% HP transition: fresh Barrier + permanent speed pressure
- phase-2 heavy attack is telegraphed before the following all-target breach
- field representative stays at the gate instead of roaming the city
- current visual production base reuses the authored 3D rusted-centurion rig at boss scale; combat mechanics and presentation timing are boss-specific
- unique final Av'Sal boss art remains a visual-quality review item, not falsely marked complete

Level gating:
- regional contracts do not require average party level
- minPartyLevel remains recommendation copy only
- Drehmal T1/T2 physical shop does not require average party level or legacy chapter completion
- discovering Av'Sal is still required before Av'Sal-local contracts can appear, because undiscovered-region jobs should not be offered out of context

Summon presentation replacement:
- summon results remain server-resolved and persisted before presentation
- a safe in-world position near the physical summon facility becomes a player-private 3D summon stage
- client camera detaches onto the stage, performs an authored orbit/push-in and restores cleanly afterwards
- the local vanilla player shell is hidden during the stage
- signal/charge ring is rendered in the world and the actual BattleActorEntity performs reveal/pose beats
- the previous central 2D silhouette/bust overlay is removed from the reveal; 2D portraits remain only in the post-reveal summary cards

Validation state for this checkpoint remains:
- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- NEW JAR: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


## Boss roster / six-beat Av'Sal road / ten-pull result skip — 2026-10-01

Locked direction:
- boss support mobs are authored per boss as a fixed encounter roster; no random add composition
- solo bosses remain valid
- Av'Sal first slice uses seven main-quest beats including a post-boss physical report
- ten-pull summon presentation supports a result-only skip without discarding the results screen

Current first-boss route:
MQ_AV01 road echo
→ MQ_AV02 courier Deren
→ MQ_AV03 Av'Sal outskirts arrival
→ MQ_AV04 2-of-3 outskirts investigation
→ MQ_AV05 2-of-3 relay resolution
→ MQ_AV06 Karnon boss
→ MQ_AV07 report the opened aqueduct gate to Sael.

AV_FIRST_BOSS fixed roster:
- AV_B01 Karnon
- E009 fixed sentry
- E011 fixed support unit

10-pull:
- 결과만 보기 immediately clears the private summon actor/camera presentation
- all ten cards become visible
- the same control becomes 닫기

Validation:
- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


## Physical waystation network checkpoint — 2026-10-01

Fast travel authority is now explicitly physical:
- map markers are information only
- New Drabyel departure uses the existing TRAVEL stablemaster
- Primal Roadhead / Capital Valley Tower / Broken West Road / Av'Sal Outskirts use physical waystation keepers
- remote keepers reuse the authored stablemaster 3D asset, not a placeholder
- keepers resolve against live safe ground and reject nearby source villagers, item frames, armor stands and block-entity content
- the server accepts TRAVEL only while the player is actually beside a physical waystation keeper/stablemaster
- discovery is proximity-based and destinations remain player-specific

Current network size for the implemented first-route/Av'Sal slice: 5 nodes.
This is not the final whole-map station count; later regions extend the network as their content is authored.

Validation:
- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


## Av'Sal first-slice completion candidate — 2026-10-01

The implementation target for the first user playtest is now closed at the content/system level.

Main chain:
MQ_AV01 road echo
→ MQ_AV02 courier Deren
→ MQ_AV03 Av'Sal outskirts arrival
→ MQ_AV04 2-of-3 outskirts investigation
→ MQ_AV05 2-of-3 relay resolution
→ MQ_AV06 Karnon + fixed support roster
→ MQ_AV07 physical report to Sael.

Post-boss closure:
- clearing AV_FIRST_BOSS records a dedicated first-boss-clear state
- navigation returns to Sael
- reporting to Sael marks the first Av'Sal slice CLEARED
- subsequent Sael interaction resumes the regional repeat-contract loop

Travel completeness:
- five physical waystation nodes in the current first-route slice
- map markers are information-only
- close-range waystation interaction prompt is available at remote nodes
- departure/arrival fade now has travel/landing sound feedback
- 길뿔 산양 can be rented from a physical waystation
- rental mount uses a dedicated MIT external saddle rig, custom gallop/jump, server-authoritative rider movement inherited from the 26.2 horse base
- one transient rental per player; battle entry / fast travel / logout recalls it

First-boss presentation:
- Karnon is classified as boss music, not normal-battle music
- Barrier counter / phase transition / telegraphed breach are authoritative combat rules
- support roster is fixed, not random

Remaining gate before asking the user to playtest:
- integrated compile/tests/build
- dedicated-server smoke/JAR production
- then client-side user validation of live placement, summon camera, mount feel, Karnon readability and economy pacing

Validation before that integrated build:
- CODE REVIEWED: YES
- TESTED: NO
- BUILD VERIFIED: NO
- JAR PRODUCED: NO
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


### Roadhorn rental ownership hardening — 2026-10-01

Multiplayer rental ownership is server-fixed:
- the waystation rental stores the renter UUID on the transient Roadhorn entity;
- only that renter can remount after dismounting;
- another player cannot steal/reassign the rental by interacting with it;
- logout, battle entry, fast travel, dimension/runtime exit and server clear still recall the transient mount.

This is the final code-side correction before the integrated first-slice build checkpoint.


## Av'Sal first-slice build verification — Build #961

Verified code commit:
`25b2db6e1d2acd7b6640d778703bafe0ff1b4ca4`

GitHub Actions:
- workflow: Build TURNBOUND
- run: 36811002004 / #961
- Gradle clean test build oneClickPack: PASS
- NeoForge dedicated-server smoke: PASS — Done (6.039s)
- built JAR verification: PASS
- one-click mrpack verification: PASS
- artifact upload: PASS
- artifact: turnbound-v04-workbranch / ID 11139483913

Hashes:
- JAR turnbound-0.1.0-alpha.17.jar
  - SHA-256 9e6ec9355dec4ab5e1ca870454e9fc13bdb93a26998ff4a9d63ffd42942649da
- TURNBOUND-oneclick-0.1.0-alpha.17.mrpack
  - SHA-256 4843d8fc931c66320c364676715bdd9f22b0bf8e41133df2c8cc7d54ff11ea5c
- uploaded artifact ZIP
  - SHA-256 c10f085ad788ca5311286c464fd5da4259230c862e5464846df3aff7b3cfa54a

This is the handoff point to the first real client playtest.
The required route is documented in AVSAL_FIRST_PLAYTEST_CHECKLIST.md.

Validation:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- DEDICATED SERVER TESTED: YES
- ONE-CLICK PACK VERIFIED: YES
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


## Command-driven playtest reset — Build #962

Verified code commit:
`835742cf00947b599bb8ba6e9f615e5ad8662aba`

GitHub Actions:
- workflow: Build TURNBOUND
- run: 36815324409 / #962
- Gradle tests/build: PASS
- NeoForge dedicated-server smoke: PASS
- built JAR verification: PASS
- one-click mrpack verification: PASS
- artifact upload: PASS

Behavior change:
- normal login/reconnect no longer resets campaign progress;
- explicit OP command `/turnbound reset` performs the former fresh-playtest reset and returns the player to the New Drabyel opening state;
- this keeps ordinary persistence testable while retaining a reusable fresh-run command.

Build outputs:
- JAR `turnbound-0.1.0-alpha.17.jar`
  - SHA-256 `06db5a045d4ba16efd52a55206bed089c030ca3f15525261bb5fa459dc41fbf5`
- one-click `TURNBOUND-oneclick-0.1.0-alpha.17.mrpack`
  - SHA-256 `fbff676fcd5c95b761b509f852514eb05563814bc5e4c17343f0c90e8508eaca`
- uploaded artifact ZIP
  - SHA-256 `0e56d655c3647bae156e05b38bff3c8618292f5315157c6e9a89c1003759abfb`

Client runtime observation still open:
- a real join reported repeated model-load errors from legacy Drehmal parent id `minecraft:item/spawn_egg_2D`;
- the repository already contains a tested 26.2 resource-pack migrator for this exact legacy parent, so the next reproduction must distinguish the installed world `resources.zip` from any other active/cached resource-pack source before changing unrelated assets.

Validation:
- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- DEDICATED SERVER TESTED: YES
- ONE-CLICK PACK VERIFIED: YES
- CLIENT RUNTIME TESTED: PARTIAL — join reached; resource-pack model errors observed
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO


## First-playtest correction slice — 2026-10-02

The first client playtest exposed a presentation/flow mismatch rather than a missing content catalog. Before resuming the Av'Sal slice:

- Capital Valley field NPCs and Av'Sal NPC/clue sites materialize beside authored content rather than fail closed under battle-arena clearance.
- The post-New-Drabyel Capital Valley step is an explicit Aren main-quest offer. The player accepts it before the three regional choices appear on HUD/map.
- Regional repeatable contracts are chosen from multiple offers at the physical contract NPC; recommended level is advisory, not a hard gate.
- Summoning uses the physical summoner's fixed ritual stage. The reveal camera frames that stage rather than selecting arbitrary ground in front of the player.
- Existing side quests remain physical-NPC/discovery driven and are not replaced by the regional main quest.


## Chapter 1 scope — Av'Sal endpoint

Latest explicit direction (2026-10-02):

- New Drabyel → Capital Valley → the long western road → Av'Sal outskirts → relay line → Karnon → Sael report is **Chapter 1**, not a tutorial slice.
- The roughly 1.4k-block westward route must carry open-world density: multiple simultaneous roaming packs, wildlife/human/monster/mechanical variety, physical NPC stops, discoveries, side quests and repeatable contracts.
- Chapter 1 should not feel like empty travel between scripted markers. Ordinary field encounters are deliberately frequent; elites and bosses remain rarer.
- Field presentation may show 2–3 members of an ordinary pack before contact. Turn battle composition remains server-authoritative and can expand independently.
- The first Karnon clear and Sael report are the Chapter 1 climax/closure checkpoint. Do not describe this route in player-facing copy as tutorial, prototype, first-slice testing, or onboarding-only content.
