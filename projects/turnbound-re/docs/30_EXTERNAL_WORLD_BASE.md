# 30 — EXTERNAL WORLD BASE — DREHMAL: APOTHEOSIS

## 1. Decision

TURNBOUND: RE production world geometry is not authored by TURNBOUND code.

Selected external production base:

- **Drehmal: APOTHEOSIS v2.2.2f**
- official site: https://www.drehmal.net/downloads
- official release source: https://github.com/Drehmal-Team/map/releases/tag/v2.2.2f
- official public version at this decision point: Minecraft Java 1.20.1
- TURNBOUND target: Minecraft Java 26.2 / NeoForge 26.2

The external world must be installed from the official distribution. The TURNBOUND repository does **not** redistribute the Drehmal world or resource pack because explicit redistribution permission was not found during the 2026-09-15 review.

## 2. What TURNBOUND may add

TURNBOUND may add game meaning on top of the installed world without rebuilding its visual environment:

- server-authoritative Encounter locators
- invisible `Interaction` entities used as TURNBOUND anchors
- fast-travel/save metadata
- party/progression/quest state
- turn-based battle ownership and presentation
- non-visual data needed to connect Minecraft activity to the progression loop

TURNBOUND does not recreate Drehmal towns, terrain, roads, structures or silhouettes by eye.

## 3. Initial binding

The first integration uses two public, documented Drehmal landmarks.

### HUB_01 — New Drabyel

Official wiki location:
- approximate coordinates: **502, 67, 1801**
- role in Drehmal: first town normally reached by following the path away from the Stasis Facility

TURNBOUND role:
- initial Hub arrival
- party/growth/forge/shop/quest service neighborhood
- exact workstation and service bindings must attach to existing external-world landmarks or directly usable external assets; do not construct an AI-designed replacement town.

### REGION_01 gateway — Stasis Facility

Official wiki location used by the adapter:
- coordinates near the inside holo-door: **778, 31, 668**

TURNBOUND role:
- first Region travel endpoint / Capital Valley entry reference
- a physical external-world landmark, not a TURNBOUND-built waypoint structure

These coordinates are integration seeds, not a claim that all future encounters should use Drehmal's original story progression.

## 4. Runtime contract

`DrehmalExternalWorldBinding`:

1. requires an operator-confirmed bind action; no unreliable auto-detection of arbitrary saves.
2. loads the two known landmark chunks.
3. registers existing TURNBOUND `HUB_01` / `REGION_01` fast-travel locator semantics in server saved data.
4. adds deterministic invisible Interaction anchor UUIDs.
5. changes TURNBOUND respawn metadata to the bound Hub without changing map blocks.
6. never copies or regenerates Drehmal blocks.

Operator binding command:

`/turnbound_re_world_slice bind_drehmal`

The command is intended for development/setup. It is not player-facing game copy.

## 5. Legacy generated world status

The following remain only because they are useful mechanics/layout harnesses:

- `FunctionalWorldSliceBuilder`
- `ProductionWorldSlicePrototypeBuilder`
- `AuthoredFirstRegionBuilder`
- `/turnbound_re_world_slice build`
- `/turnbound_re_world_slice prototype`

They are **not production world sources**. Production bootstrap must not call them automatically.

`26_M6_WORLD_ASSET_GATE.md` therefore records a historical prototype/reference gate. Its hand-authored Wayfarer Forge Court / Riverward Foothill layout is no longer the production visual direction where it conflicts with this document or `CANON.md`.

## 6. Compatibility status

As of 2026-09-15:

- APOTHEOSIS public full-map distribution: 1.20.1 and Fabric-oriented companion setup.
- Drehmal `Archived Memory 2 — To Feel The Stars`: officially released as a Java 26.2 world, which confirms the team has a 26.2 content pipeline, but it is a separate short teaser and not a 26.2 release of APOTHEOSIS.
- TURNBOUND has **not yet loaded/migrated APOTHEOSIS v2.2.2f under Java 26.2 + NeoForge**.

Therefore:

- CODE REVIEWED: binding architecture only.
- BUILD VERIFIED: NO for this external-world change.
- WORLD MIGRATION TESTED: NO.
- PLAYTESTED: NO.
- MULTIPLAYER TESTED: NO.

Do not claim external-world compatibility until an actual copied test save successfully loads and the relevant landmarks, entities, datapack behavior and resource-pack dependencies are checked.

## 7. Next world work

After the first compatible test copy exists:

1. load APOTHEOSIS under the target 26.2 environment without modifying the source copy.
2. verify New Drabyel and Stasis Facility survive migration visually and functionally.
3. identify existing world landmarks for the first patrol and elite Encounter; anchor gameplay to them instead of building replacement scenery.
4. map mining/farming/fishing loops onto existing geography and resources.
5. audit original Drehmal datapack/resource-pack mechanics for conflicts with TURNBOUND server authority.
6. verify navigation, encounter readability, camera clipping and UI readability in the actual external environment.
