# Campfire Sessions — External Asset / Dependency Plan

Status: PLANNING
Updated: 2026-09-28

This file tracks visual/content dependency direction.
It is NOT the same as THIRD_PARTY_ASSETS.md:
- THIRD_PARTY_ASSETS.md records assets actually used/packaged.
- this file records candidates, constraints and selection status before implementation.

## 1. Absolute visual rule

For final player-facing art, prefer strong external assets/designs over improvised AI-authored final design.

Search external sources first for:
- island/world map.
- village buildings.
- player houses.
- interiors.
- cute animal residents.
- resident rigs/animations.
- furniture.
- kitchen/cooking assets.
- plants/flowers/mushrooms.
- tools.
- boats.
- clothing/accessories.
- UI panels/buttons/icons.
- museum displays.
- fossil/dinosaur models.
- aquariums/terrariums.
- festival decoration/stages.
- photography/camera presentation.
- contest props.

If an external asset needs technical conversion, adapt it to Minecraft 26.2 while preserving the quality/design language.

This rule also applies to **systems**, not only texture/model files.
Before building a major custom substitute, search for a high-quality compatible external implementation/mod/library where it could materially improve:
- cooking/kitchen interaction.
- furniture behavior.
- camera/photography.
- player clothing/model presentation.
- inventory/tool-belt convenience.
- plants/crops/mushrooms.
- swimming/diving presentation.
- UI framework/components.
- resident animation/rig support.

External-first does not mean dependency-maximal.
Adopt a system only when its real 26.2 NeoForge compatibility, visual fit, multiplayer/runtime behavior, maintenance state, performance and overlap with existing systems are acceptable.

## 2. Private-use boundary

The project is for private play rather than public distribution.

This means candidate selection can include assets/mods that are usable privately but not freely redistributable.

Still forbidden:
- bypassing paid access.
- DRM bypass.
- circumventing account/access restrictions.
- illicit copies of paid assets.

Record source and usage notes so the project remains understandable later.

## 3. Current confirmed assets already in Campfire Sessions

Already used in the existing alpha.6 music foundation:
- Acoustic Guitar from Tchongas Musical Instruments Pack.
- Wooden Chair from Voxelized Furniture.
- Kenney UI assets for music UI.
- 15 CC0 music tracks documented in THIRD_PARTY_ASSETS.md.

These remain valid unless later replaced intentionally.

## 4. Map

### Requirements
Final map must:
- be an island/archipelago world.
- provide large flat buildable zones.
- support fixed building anchors.
- have enough buildable land for roughly 10–12 homes plus public facilities.
- have useful coast/rivers/cliffs/caves where possible.
- be directly obtainable by the development workflow.
- require no user-side manual world download/install.
- survive conversion/loading in Minecraft 26.2.

### Candidate status
4K Flat Islands Map for Creative:
- attractive on paper due to flatness and scale.
- not yet canonical.
- actual world file still needs direct acquisition and inspection.
- if direct acquisition is blocked, replace it rather than asking the user to download it.

Previous 1024×1024 7-island candidate:
- useful reference.
- not final.
- may be too small or not flat enough for the new scope.

No island roles are final until a real world file is inspected.

## 5. Buildings

Final buildings must use external authored builds/prefabs where possible.

Needed categories:
- resident services.
- general store tiers.
- museum.
- café.
- clothing shop.
- clinic if retained.
- pier/harbor.
- player house tiers/variants.
- resident house variants.
- construction-site variants.
- festival/plaza staging.

The implementation should store:
- anchor position.
- rotation.
- footprint/protection bounds.
- entry point.
- interior destination.
- NPC work points.
- interaction anchors.
- replacement/upgrade prefab ID.

## 6. Interior assets

Prefer external interior builds/furniture sets.

Large interior spaces may live in dedicated managed cells/space separate from the small exterior footprint.

Need strong external interior references for:
- homes.
- resident houses.
- museum wings.
- store.
- café.
- wardrobe/clothing area.
- public-service interior.

## 7. Furniture

Candidate external sources/mods previously discussed:
- Skniro's Furniture.
- BetterDeco.

Final selection requires:
- Minecraft 26.2 NeoForge compatibility.
- style compatibility with the chosen village/building art.
- acceptable performance.
- no major duplication.
- sufficient interactive furniture coverage.

Furniture should be interactive only when the interaction improves life-sim play.

## 8. Cooking / crops

Leading candidates:
- Croptopia.
- Cooking for Blockheads.

Why:
- broad food/crop variety.
- real kitchen presentation and recipe UX.
- avoids a shallow custom cooking system.

Farmer’s Delight-style options may be reconsidered if a stable 26.2 NeoForge version is verified later.

## 9. Flora / mushrooms

Previously discussed candidate/reference sources:
- Flora Expansion.
- Shroomcraft/Shroomcrafted-style mushroom expansion.
- CC0 plant/mushroom asset packs from public asset libraries.

Final selection should emphasize:
- seasonal appearance.
- flower breeding/rare colors.
- mushrooms.
- fruit/crops.
- decorative variety.
- consistent Minecraft-friendly art.

## 10. Residents

Do not use vanilla villagers as the core cast.

Need an external cute-animal visual base with:
- large readable heads/faces.
- short/charming proportions.
- multiple species/variants.
- consistent rig.
- compatible animation pipeline.

Previously discussed reference candidate:
- Kenney Cube Pets as a CC0 stylistic/model starting point/reference.

Actual final resident pack/rig remains unresolved.

Resident identity rule:
- one approved external appearance/model/variant maps to one fixed authored resident.
- do not randomly assign different names/personalities to the same appearance on different saves.
- final name, birthday, personality traits, hobbies, home theme and dialogue identity are assigned after the actual model roster is selected.
- the art/model is therefore inspected first, then the character identity is authored to fit it.

Animation should support:
- idle.
- walk/run.
- talk.
- happy/sad/angry/surprised.
- sit.
- eat.
- fish.
- sing/dance.
- sleep.

GeckoLib remains a likely animation foundation where technically appropriate.

## 11. Tools

Do not finalize tool tiers until a coherent external tool-art set is selected.

Desired properties:
- fishing rod and major lifestyle tools share one design language.
- multiple visually distinct upgrade tiers.
- high-quality first- and third-person appearance.
- no disposable durability loop.

Previously researched references:
- Hardware Reforged-style coherent tool visuals.
- 3D fishing-rod resource packs with multiple material variants.

These are references/candidates, not automatically adopted assets.

## 12. Boats

Need external boat models suitable for:
- starter boat.
- improved purchased boat(s).
- potentially special/festival/long-route variants.

Boat progression is money-purchase based, not an RPG upgrade tree.

## 13. UI

Existing Kenney UI assets establish a precedent.

Potential external families:
- Kenney UI Pack.
- Kenney Fantasy UI Borders.
- other coherent external UI packs if they better fit the final island-life tone.

Required screens:
- calendar/weather.
- shop.
- loan/housing.
- encyclopedia.
- mail.
- catalog.
- wardrobe.
- museum info.
- resident interaction.
- route/pier UI.

Do not mix many incompatible UI styles.
Choose a coherent system and use it consistently.

## 14. Inventory / player model / camera candidate systems

Potential roles:
- external tool-belt system for fast lifestyle-tool access.
- optional backpack only if inventory pressure justifies it.
- external player-model/clothing system such as Customizable Player Models if integration fits.
- external camera/photo system if 26.2 compatibility remains stable.

Do not add dependencies just because they exist.
Each must materially improve final quality/UX.

## 15. Museum / fossils / displays

Need external assets for:
- dinosaur/fossil skeletons.
- display stands.
- aquariums.
- terrariums.
- exhibit props.
- art/antique displays.

Multipart fossil completion should visually build the exhibit rather than remain an inventory checklist.

## 16. Festival and event art

The multi-day festival needs external:
- stalls.
- banners.
- lights.
- stage/performance props.
- themed food stands.
- seasonal decoration.
- contest props/trophies.

Festival presentation must not be temporary programmer art.

## 17. Selection protocol

Before adopting any candidate:
1. verify current Minecraft 26.2 / NeoForge compatibility where relevant.
2. inspect actual visuals, not just description.
3. check whether it clashes with the chosen overall art direction.
4. check performance impact.
5. check multiplayer/runtime behavior where relevant.
6. record source and usage status.
7. integrate only after it has a clear role.

When a candidate is actually adopted, move/document it in THIRD_PARTY_ASSETS.md with source and usage notes.
