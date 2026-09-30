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
9. `ENCOUNTER_ENEMY_PLACEMENT_v1.md`
10. `MULTIPLAYER_DESIGN_v1.md`
11. `OVERHAUL_ROADMAP_v1.md`
12. current source/resources
13. preserved TURNBOUND v0.4 system canon in Git history / archived design files, only where it was explicitly reaffirmed and not later superseded by the user

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
