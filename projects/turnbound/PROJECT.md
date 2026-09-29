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

Confirmed Standard Archive baseline restored on 2026-09-26:
- ★5 3%: P02 / P05 / P06
- ★4 12%: P01 / P03 / P04 / P07
- ★3 35%: P08
- ★2 30%: F03 / F04
- ★1 20%: F01 / F02

Do not revert this table to a ★3~★5-only pool without a new explicit user decision.

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
- the current P01~P08 v1 kits, Drehmal world transition, Lv1~60 XP growth, equipment structure, and removal of the global Awakening Core spending path are intentional overhaul work and must not be reverted as collateral damage.

Still unresolved and requiring an explicit user decision before implementation changes:
- exact `nativeStar` / `currentStar` progression semantics
- promotion / ★6 / Awakening relationship
- low-rarity material usage semantics
- F03 initial story grant/composition
- any other historical v0.4 system not individually classified as regression or intentional overhaul

Do not infer a restore/delete decision for those unresolved systems from historical implementation alone.

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
