# 30 — EXTERNAL WORLD BASE — DREHMAL: APOTHEOSIS

## 1. Decision

TURNBOUND: RE production world geometry is not authored by TURNBOUND code.

Selected external production base:
- **Drehmal: APOTHEOSIS v2.2.2f**
- official site: https://www.drehmal.net/downloads
- official release: https://github.com/Drehmal-Team/map/releases/tag/v2.2.2f
- public full-map target at review time: Minecraft Java 1.20.1
- TURNBOUND target: Minecraft Java 26.2 / NeoForge 26.2

The external world is installed from the official distribution. TURNBOUND does **not** vendor or redistribute the Drehmal world/resource pack because explicit redistribution permission was not confirmed during the 2026-09-15 review.

TURNBOUND does not recreate Drehmal towns, terrain, roads, structures or silhouettes by eye.

## 2. Data-driven binding contract

External-world coordinates are reloadable definition data, not Java layout constants.

Canonical profile:
- id: `turnbound_re:drehmal_apotheosis_2_2_2f`
- data file: `data/turnbound_re/turnbound_definitions/external_world_profiles.json`
- runtime adapter: `DrehmalExternalWorldBinding`

Every profile anchor declares:
- semantic `kind`: `FAST_TRAVEL`, `RESOURCE` or `ENCOUNTER`
- stable TURNBOUND locator
- external-world coordinate
- `enabled` gate
- source/provenance note

Registry validation rejects unknown kinds, unresolved TURNBOUND locators, duplicate profile locators and dimension mismatches. This keeps map placement data separate from combat/progression identity and lets a future Drehmal update or verified 26.2 migration change coordinates without rewriting game rules.

## 3. Enabled initial binding

Only two external anchors are enabled before migration inspection.

### HUB_01 — New Drabyel
- integration seed: **502, 67, 1801**
- semantic locator: `turnbound_re:hub_01/waypoint`
- role: initial Hub arrival and return point

### REGION_01 gateway — Stasis Facility
- integration seed: **778, 31, 668**
- semantic locator: `turnbound_re:region_01/waypoint`
- role: first Region / Capital Valley gateway

These are integration seeds, not a claim that the exact post-migration standing block has already passed visual/play validation.

`/turnbound_re_world_slice bind_drehmal` is operator setup. To reduce accidental binding of an unrelated Overworld, the operator must stand near the configured New Drabyel Hub seed before the bind is accepted.

The adapter:
1. reads the external-world profile from the atomic definition registry.
2. loads only enabled anchor chunks.
3. registers enabled fast-travel positions in server saved data.
4. creates deterministic invisible `Interaction` entities for enabled semantic anchors.
5. changes TURNBOUND respawn metadata to the configured Hub.
6. never copies, clears, fills or regenerates Drehmal blocks.

## 4. Recorded but disabled mapping candidates

The first profile records likely existing Drehmal locations for the first gameplay slice, but they remain `enabled=false` until the APOTHEOSIS world is actually opened under the target 26.2 environment.

| TURNBOUND semantic | External candidate | Status |
|---|---|---|
| `region_01/riverside_plot` | Drabyel farmhouse / wheat-field neighborhood near the Adventuring Merchant | disabled — migration inspection required |
| `region_01/ore_outcrop` | Primal Caverns | disabled — mining/readability/progression inspection required |
| `region_01/river_pool` | Solvei stream area | disabled — fishing access inspection required |
| `region_01/overworld_patrol` | Hunter's Crypt | disabled — encounter access/spacing inspection required |
| `region_01/rift_elite` | Ruins of Ihted | disabled — elite/readability inspection required |

Recording a candidate is not a visual or gameplay PASS. No disabled candidate spawns an Interaction anchor or changes TURNBOUND state.

## 5. Minecraft-native activity rule

External-map resource anchors do not turn mining/farming/fishing into generic click rewards.

- mining remains actual Minecraft block breaking / material acquisition.
- farming remains actual crop interaction.
- fishing remains actual Minecraft fishing.
- the external anchor only identifies where the activity belongs in the progression loop.

Encounter anchors likewise attach TURNBOUND battle meaning to existing world locations without constructing a replacement ruin, rift or arena.

## 6. Legacy generated world status

These remain mechanics/layout harnesses only:
- `FunctionalWorldSliceBuilder`
- `ProductionWorldSlicePrototypeBuilder`
- `AuthoredFirstRegionBuilder`
- `/turnbound_re_world_slice build`
- `/turnbound_re_world_slice prototype`

They are **not production world sources**. Production bootstrap must not call them automatically. `26_M6_WORLD_ASSET_GATE.md` is historical harness/reference documentation.

## 7. Compatibility and validation status

As of 2026-09-15:
- APOTHEOSIS public full-map distribution: 1.20.1-era, with Fabric-oriented companion setup.
- Drehmal `Archived Memory 2 — To Feel The Stars`: separately released for Java 26.2, proving a current team pipeline but not a 26.2 APOTHEOSIS release.
- TURNBOUND has **not yet loaded/migrated APOTHEOSIS v2.2.2f under Java 26.2 + NeoForge**.

Schema checkpoint history:
- Build #262 / run `34921535375` compiled Java and test sources successfully, then exposed two stale test-fixture assumptions.
- `M5BattleStageCharacterPresentationTest` had used Creeper as a character without dedicated presentation even though Creeper has dedicated presentation; the fallback fixture now uses Cow.
- `M6ExternalWorldProfileDefinitionTest` initially omitted fast-travel destinations.
- Build #264 / run `34927673779` then exposed the remaining fixture mismatch: reciprocal waypoint destinations must follow reciprocal authored region exits.
- The M6 fixture was aligned with production `world_regions.json` by declaring `hub_01 -> region_01` and `region_01 -> hub_01` exits.
- Build #265 / run `34927989993`, commit `abdfcd926ecf3a20ba323d7328e73d963d01e704`: `dependencies clean build` SUCCESS, JUnit SUCCESS, production JAR verification SUCCESS, deliverables uploaded.
- verified JAR: `turnbound_re-0.1.0-alpha.1.jar`
- JAR SHA-256: `64e422731b5bf751dcb7e1deb16ff4e04d0da5cbc4247f49cbb3b04e355afd45`

Current validation state:
- CODE REVIEWED: YES.
- TESTED: YES — current Gradle/JUnit suite passed in Build #265.
- BUILD VERIFIED: YES — Build #265.
- JAR PRODUCED: YES — `turnbound_re-0.1.0-alpha.1.jar` from Build #265.
- JAR VERIFIED: YES — ZIP integrity, NeoForge metadata, compiled mod class, asset/data namespaces, source/development-path exclusion and duplicate-entry checks passed.
- WORLD MIGRATION TESTED: NO.
- PLAYTESTED: NO.
- MULTIPLAYER TESTED: NO.
- GameTest: NOT RUN — no GameTest contract is currently established by the workflow.
- Dedicated server smoke test: NOT RUN — runtime smoke task is not established.
- Client smoke test: NOT RUN — headless presentation/runtime gate is not established.

Do not enable the recorded resource/encounter candidates or claim external-world compatibility until an actual copied test save loads successfully and its landmarks, datapack behavior and resource-pack dependencies are inspected.

## 8. Next world gate

After a compatible test copy exists:
1. load APOTHEOSIS under the target 26.2 environment without modifying the source copy.
2. verify New Drabyel and Stasis Facility and adjust only profile coordinates if the migration shifts safe arrival positions.
3. inspect the five disabled candidates in-game and enable only those that actually fit TURNBOUND flow.
4. audit Drehmal datapack/resource-pack mechanics against TURNBOUND server authority.
5. verify navigation, encounter readability, camera collision and UI readability in the actual external environment.
