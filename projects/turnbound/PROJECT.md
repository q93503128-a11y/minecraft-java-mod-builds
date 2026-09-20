# TURNBOUND Project Contract

## Identity
- Path: `projects/turnbound/`
- Mod ID: `turnbound`
- Display name: `TURNBOUND`
- Package: `io.github.q93503128.turnbound`
- Version: `0.1.0-alpha.17`

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
10. `OVERHAUL_ROADMAP_v1.md`
11. current source/resources

Old v0.4 Aster March physical-world canon and alpha-by-alpha deltas are superseded.
Git history is the archive.

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

- ★3~★5 only for formal roster
- no fodder character design
- one readable signature mechanic
- duplicate copies never required for core kit
- models/animations/VFX/SFX are part of completion
- portrait uses final model when quality permits

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
- old filler character data removed
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

## Last verified implementation checkpoint

- CODE REVIEWED: YES
- TESTED: YES — fixed-point scheduler + P01~P08 v1 combat runtime + Drehmal first-route binding + battle-center camera + CV-A/B/C production enemies + Warning Cave Cavehorn Elite asset/AI/reward contract + Drabyel-road Hill Marksman two-step aim telegraph/retarget contract + shallow RPG quick-menu navigation + source-backed Drehmal world-map replacement + New Drabyel physical service NPC runtime/facility gating + R_PG-derived single-representative field encounters + deterministic roam/dwell pacing + Minecraft terrain-aware PathfinderMob navigation with blocked-route recovery + horizontal Tower/camp/Drabyel safety-ring aggro exclusion and visible-enemy return behavior + R_PG-derived short in-world alert prelude + survey-gated transient location banners + close-range server-authored service prompts + server-authored Drehmal first-route navigation + authoritative pre-battle return view and immediate field-context resync after battle + external-vs-legacy field-command authority isolation + Drehmal-specific first-hub facility/summon gating + Drehmal world-map/minimap routing + external-world meta-surface isolation + legacy world-writer fail-closed guards + external-runtime eviction of retained legacy sessions + admin-only direct summon commands + explicit battle-transition field handoff + unified client presentation ownership + field HUD/shortcut suppression during handoff + short post-outro result reveal + progression-first contextual first-route guidance + persistent one-step New Drabyel onboarding + complete six-role FableCraft New Drabyel service visual family + v1 summon economy/roster migration + private world-first 3D summon reveal + v1 level/equipment/Awakening growth migration + schema-5 legacy-save conversion + P01~P08 v1 signature-resource/target presentation binding + P01~P08 role-prop asset contract + distinct two-layer core-hero action audio + shared live-3D portrait binding across battle/meta/summon/result UI + persistent live-3D signature body-language states for P01/P03/P05/P06/P07+Toto/P08 including P08 Overheat + dedicated 3D relation sigils for P01 Duel/P05 Sightline/P04 Sanctuary/P07 partner protection that follow the actual target actor + duplicate-safe animated Turn Order rail shifts so P02/Gauge manipulation is read as movement rather than a snapped list + authoritative signature payoff beats for P01/P03/P04/P05/P06/P07+Toto/P08, including Morwen Last Page and Signature Toto authored-animation parity + first Capital Valley battle contextual onboarding that teaches Basic → Turn Order → Active/CD in the existing action header without a modal tutorial
- BUILD VERIFIED: YES — Build TURNBOUND #842
- verified code commit: `769da0b41c4b2c640dd7d2f2557d11b49655af63`
- SERVER SMOKE: YES — NeoForge 26.2 dedicated server load, Done (5.739s)
- JAR PRODUCED: YES — artifact `turnbound-v04-workbranch`, id `10601497392`, JAR SHA-256 `5e1bbc824fb2bfc8fd4e2483f89f81f361424e4e4d4282d36491458e7d96cbf8`
- CLIENT RUNTIME TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
