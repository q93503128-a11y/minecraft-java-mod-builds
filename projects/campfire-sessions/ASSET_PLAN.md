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
- player animation/emote foundations.
- furniture.
- kitchen/cooking assets.
- plants/flowers/mushrooms.
- tools.
- boats.
- clothing/accessories.
- resident seasonal/weather outfit variants.
- UI panels/buttons/icons.
- museum displays.
- fossil/dinosaur models.
- aquariums/terrariums.
- festival decoration/stages.
- photography/camera presentation.
- ambient/weather/interior sound assets.
- non-verbal resident voice/chirp assets or reusable speech-sound systems.
- contest props.

If an external asset needs technical conversion, adapt it to Minecraft 26.2 while preserving the quality/design language.

This rule also applies to **systems**, not only texture/model files.
Before building a major custom substitute, search for a high-quality compatible external implementation/mod/library where it could materially improve:
- cooking/kitchen interaction.
- furniture behavior.
- camera/photography.
- player clothing/model presentation.
- cosmetic-vs-functional armor separation.
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

### Music/guitar integration
The existing Tchongas Acoustic Guitar remains the baseline purchasable guitar visual.
Do not replace the working Campfire guitar/music system merely because other instrument mods exist.

Additional guitar/instrument assets may be sourced later for:
- premium cosmetic guitar variants.
- the recurring special musician.
- café/plaza/festival staging.
- resident-home music decor.

The recurring musician requires:
- one fixed original special-character visual identity using the selected external animal/character pipeline.
- reliable seated/standing guitar performance poses.
- idle/practice/performance animation states.
- suitable stool/chair/stage props.
- optional microphone/amp/lighting props only when they fit the acoustic/island-life presentation.

Do not imitate K.K. Slider's exact species, model, clothing, name, dialogue, songs or visual presentation.
Use the life-sim role as structural inspiration only.

## 4. Map

### Requirements
Final map must:
- be an island/archipelago world.
- preferably provide roughly 6–10 **meaningful** island destinations, including one strong main-village island; do not chase island count with tiny useless rocks.
- provide a large flat buildable main zone.
- support fixed building anchors.
- have enough buildable land for roughly 10–12 homes plus public facilities.
- have useful coast/rivers/cliffs/caves where possible.
- be directly obtainable by the development workflow.
- permit redistribution/bundling inside the Campfire Sessions Modrinth .mrpack.
- require no user-side manual world download/install.
- survive conversion/loading in Minecraft 26.2.
- be provisionable from a packaged template without modpack updates overwriting a progressed save.

### Candidate status

**Geming400 — Island map | 1024×1024 — current leading packable candidate**
Recovered from the earlier Campfire planning pass and re-verified against the current CurseForge/Planet Minecraft listing.

Current verified source facts:
- creator: Geming400.
- world size: 1024×1024.
- 7 islands total: one large central island + six smaller surrounding islands.
- WorldPainter.
- caves: yes.
- caverns: yes.
- chasms: no.
- ores: yes.
- palm trees, other trees and bushes.
- current release: `Island - No WorldBorder.zip`.
- file size: about 51.2 MB.
- source version: Minecraft 1.21.4.
- license: MIT.

Why it is now strategically strong:
- the 7-island structure better supports repeated pier travel and destination identity than only 2–3 giant landmasses.
- MIT explicitly resolves the Modrinth-pack redistribution problem as long as license/copyright notices are preserved.
- current direct download remains available.
- 51.2 MB is practical for a bundled world template.

Unknowns that still block canonical selection:
- exact central-island flat/buildable acreage.
- whether 10–12 houses + civic buildings + roads/plaza/yard spacing fit without crowding.
- exact island-to-island distances and travel feel.
- actual biome/height distribution from chunks.
- Minecraft 26.2 conversion correctness.
- whether the 1024×1024 overall scale feels rich enough after multiplayer and long-term content are placed.

Required next action:
- obtain the actual ZIP.
- inspect level.dat/region data and load it in a controlled conversion copy.
- measure usable flat area and produce a first anchor/parcel sketch.
- reject it if the main island cannot comfortably support the village, even though the license/island count are attractive.

**4K Flat Islands Map for Creative — current leading candidate**
Source/visual inspection pass completed from the creator's current Planet Minecraft page and screenshots.

Creator-stated properties:
- roughly 4000×4000 total map.
- three large bare islands plus smaller islands/islets and a lake.
- broad river/channel around 100 blocks wide.
- terrain height varies only from about 26 to 32: roughly six blocks of vertical variation across the authored land.
- water depth around 6–15 blocks.
- endless ocean biome outside the custom map.
- WorldPainter-made, basic-block fresh export.
- creator describes it as a coastal-city base and says the player spawns roughly in the middle of the bottom-left island.
- current page again exposes a download entry; a 2024 creator reply says the old link was fixed.
- creator explicitly allowed another user to use the map for a city, preferring credit if published.

Visual-read conclusions from the published overview/close aerial images:
- southwest/bottom-left landmass appears to be the strongest **provisional main-village candidate** because it is very large, contiguous, mostly level and already contains the documented spawn region.
- east landmass is another very large buildable surface.
- northwest landmass is also substantial and mostly flat.
- the central Y-like water channel creates natural harbor/bridge/coastal-walk opportunities.
- the small inland lake/islets are useful landmark material without consuming much civic build area.
- the scale is much larger than needed for the 10–12 house village itself, so the civic/residential core should be deliberately clustered rather than spread across the whole 4k map.
- the extremely flat terrain is excellent for external building prefabs and controlled parcel/road layouts.
- current surface art is stone-heavy/bare in large areas; final Campfire use would need an authored vegetation/grass/path/environment pass rather than leaving it as a grey creative canvas.
- the map does not appear to provide the rich cliffs/caves/ecology required by the full Campfire design on its own. Those must come from selected authored additions, managed cave/exploration spaces, or carefully modified secondary areas.
- the ~100-block water channel is too wide to treat casually as a tiny creek; bridges, ferries/boats or route design must respect that scale.

Important limitation:
- the actual world ZIP/region data has **not** yet been obtained inside the current development tool environment.
- the Planet Minecraft binary download endpoint could not be materialized here, so this remains screenshot/source-description analysis rather than level.dat / region-chunk inspection.
- therefore exact island area, real heightmap, cave data, biome data, coordinates and Minecraft 26.2 conversion safety are still NOT VERIFIED.
- do not mark the candidate canonical until the binary world is directly obtained and loaded/inspected.

**Worldpainter 4000×4000 island for cities (FishyLava, 2025)**
- explicitly advertises a flat main island for city building plus two smaller islands.
- visually/conceptually matches the flat-main-island requirement very well.
- however the current page does not expose a downloadable map, and a later commenter explicitly asked the creator to make it downloadable.
- keep as a geometry/layout reference, not a production candidate unless a direct legal download appears.

**Wullestor (McMeddon, 4096×4096)**
- directly downloadable/source-file-backed archipelago with caves, ores, biomes and exploration content.
- explicitly includes high/flat archipelago terrain among its features.
- much richer exploration baseline than 4K Flat Islands.
- however it is substantially more vertical/terrain-heavy and less ideal for a clean large prefab village core.
- retain as fallback/reference if the leading flat map cannot be acquired or if exploration terrain becomes more important than civic flatness.

### Current map-selection priority
1. **Geming400 7-island MIT world** — inspect first because it combines useful island count, direct availability and clear redistribution rights.
2. search/compare any larger redistributable archipelago that still offers a genuinely flat main-village island.
3. **4K Flat Islands** — retain as the strongest extreme-flatness candidate/reference, but do not package it without explicit redistribution rights.
4. reject visually excellent ARR/no-redistribution maps if they would force manual user installation; that violates the project delivery requirement.

No island roles are final until a real world file is inspected.

## 5. Buildings

Final buildings must use external authored builds/prefabs where possible.

Accepted source forms can include:
- downloadable Minecraft world saves/maps.
- structure NBT.
- schematic/litematic-style building files that can be lawfully obtained and converted.
- directly downloadable build packs/prefabs.
- structures sourced from compatible external mods when technically and visually appropriate.

Minecraft block-built structures made by other creators are valid candidates.
It is acceptable to combine buildings from different creators when scale, palette, silhouette and village style can be made coherent.
Do not reject a strong building merely because another facility uses a different creator; reject it when the actual village would look incoherent or the asset cannot be directly obtained.

Housing stage count should be selected **after** the usable external house set is acquired.
Do not force an arbitrary six-stage sequence if the available high-quality houses support a cleaner progression with fewer or more meaningful stages.

Needed categories:
- resident services.
- general store tiers.
- museum.
- café.
- clothing shop.
- small clinic/medical building.
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

### Essential exterior acquisition / layout gate

Before deciding whether the 7-island map is too small, acquire or extract the **actual exterior structures** used for the first layout pass.

Required first-pass exteriors:
- resident services / administration.
- general store.
- museum shell.
- café shell.
- clothing shop shell.
- clinic shell.
- harbor/pier service shell.
- player house shell(s) and resident house shell(s), including at least the likely early/mid-size exterior footprints.
- notice-board/plaza props only insofar as they affect real public-space clearance.

The interiors may be much larger managed cells and therefore do **not** count toward main-island ground area.

Current external structure candidates:

**Villageria**
- current Minecraft 26.2 NeoForge build exists.
- MIT licensed.
- provides a dedicated Town Hall, Village Shop and Mini Hospital.
- buildings intentionally use compact vanilla-village-compatible footprints.
- strong first extraction/reference candidate for **resident services, general store and clinic exterior envelopes**.
- do not adopt the rest of Villageria's villager/combat/economy gameplay; only inspect/reuse permitted structure assets or treat the mod as an asset source when technically cleaner.
- exact structure bounds must be measured from the real 26.2 assets before placement decisions.

**SY Village**
- current Minecraft 26.2 NeoForge build exists.
- MIT licensed.
- blueprint/structure-oriented system and editable structure-block/NBT workflow.
- useful technical reference/source for compact village houses and structure-template placement.
- visual quality must be inspected before any building becomes final; compatibility alone is not a reason to adopt its aesthetic.

**Towns and Towers**
- current Minecraft 26.2 support exists.
- contains dozens of village structures and several coastal/biome architectural families, including beach/mediterranean-style references.
- CC-BY-NC-SA-4.0, so any extracted/adapted structure asset used in this noncommercial project requires correct attribution and share-alike handling for that adapted asset.
- useful candidate pool for **café, clothing shop, museum-like civic shell, houses and harbor/coastal structures** when a specific structure visually fits.
- do not import the entire worldgen system just to obtain one building if selected structure templates can be cleanly packaged instead.

**High-quality downloadable build/schematic packs**
- may be used as reference/editable base/direct asset only when redistribution rights are explicit enough for the Modrinth-pack workflow.
- a downloadable schematic with unclear license is NOT automatically packable.
- Animal Crossing recreation builds are reference-only; do not ship copied Nintendo building art/layouts.

### Provisional layout envelope

Until exact NBT bounds are measured, use only a **planning envelope**, not final dimensions.

Because interiors are separate, the expected village ground requirement is modest:
- civic/public buildings should generally target compact exterior shells, roughly village-building scale rather than giant interior-scale shells.
- 10–12 homes should be arranged as several small residential clusters rather than one huge suburban grid.
- roads should stay walkable and readable, typically narrow village streets with selected wider plaza approaches.
- harbor/pier consumes coastline more than inland build area.

For the first map-fit test, reserve a contiguous mostly-flat village planning zone of roughly **160×160 blocks**, with **~180×180 preferred** if the real island offers it naturally.
This is a conservative planning envelope that includes roads, plaza, 10–12 house parcels, civic buildings and greenery.
It is NOT a requirement to fill the whole square or a claim that current external buildings have those exact sizes.

If the actual selected exteriors produce a comfortable layout in less space, keep the compact layout.
If they require substantially more space, measure why before rejecting the map.

### First placement concept

Use a walkable clustered layout rather than spreading facilities across the whole main island:
- harbor/pier at the most natural accessible coast.
- main path from harbor into the civic core.
- resident services faces or anchors the central plaza.
- general store, café and clothing shop form a small commercial walk around/near the plaza rather than isolated compounds.
- museum sits slightly off the highest-traffic commercial edge so its exterior can have landscape breathing room without needing a huge shell.
- clinic stays near the civic/residential core.
- homes form 2–3 compact neighborhoods with short paths back to the plaza.
- community garden/public green sits between civic and residential activity if the actual terrain supports it.

Do not lock coordinates until the real map chunks and exact structure bounds are available.

## 6. Interior assets

Prefer external interior builds/furniture sets.

Large interior spaces may live in dedicated managed cells/space separate from the small exterior footprint.

Need strong external interior references for:
- homes.
- resident houses.
- museum wings.
- store.
- café.
- small clinic.
- wardrobe/clothing area.
- public-service interior.

Store interiors should support **physical merchandise display** rather than only menu-driven shopping.
Search external assets/builds for:
- furniture display platforms/floor arrangements.
- clothing racks/mannequins.
- premium/seasonal display stands.
- readable price/sign props.
- coherent shelving and checkout/service counters.

The shop layout must leave enough circulation space for multiple players and still make displayed items easy to inspect.

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

Furniture candidates should also be evaluated for:
- pre-placement preview/ghost support.
- non-destructive rotation before confirmation.
- safe reposition/undo behavior.
- stable multiplayer placement synchronization.

If a strong candidate already implements these well, reuse/adapt it before writing a parallel furniture-placement layer.

Furniture candidates should be evaluated for stable rotation/orientation support.
Prefer reusing a mod's proven placement/rotation behavior when it correctly rotates collision, seats and interaction anchors instead of rebuilding a second transform system.

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
- flower/color variety without requiring a dedicated breeding-genetics subsystem.
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

Resident animation is a high-priority presentation requirement, not optional polish.
The selected resident visual base should support enough rig/bone control for expressive lifestyle actions such as talk, sit, eat, fish, garden, clap, dance and play music.
It should also support practical outfit/accessory switching for rainwear, umbrellas, winter clothing and festival/performance states where possible.
Prefer acquiring/using existing animation and outfit sets where compatible, then author only the missing project-specific actions.

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

For shop-item inspection, a secondary 3D preview/character try-on panel may be added if the chosen rendering/UI framework supports it cleanly.
This is **supplemental** to physical in-store display, not a replacement.

Useful preview cases:
- rotate furniture.
- inspect color/material variants.
- preview clothing on the current player appearance.
- inspect catalog-only/reorder items.

Do not build a custom 3D preview renderer if an external framework already provides a stable equivalent and the implementation cost would be disproportionate.

Potential external families:
- Kenney UI Pack.
- Kenney Fantasy UI Borders.
- other coherent external UI packs if they better fit the final island-life tone.

**Kenney UI Pack**
- 430 UI assets.
- CC0.
- already consistent with Campfire's existing use of Kenney assets.
- strong visual-component source for panels/buttons/icons, but the final navigation hierarchy still needs to be based on coherent reference screens rather than assembling arbitrary large buttons from the pack.

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
- village notice-board schedule/detail UI.

### Animal Crossing: New Horizons UI reference study

Animal Crossing: New Horizons is a **visual/interaction reference**, not an asset source.
Do not copy Nintendo textures, icons, exact screen layouts, branding, sounds or proprietary artwork.

Observed reusable design principles from inventory, NookPhone, map, wardrobe/fitting and dialogue screens:

**1. Organic container silhouettes**
- primary panels are soft cream/off-white shapes with heavily rounded or organic edges.
- the inventory does not read as a dark rectangular Minecraft chest.
- screen background/game world often remains visible around the panel, preserving place/context.

**2. Slots are implied rather than boxed aggressively**
- inventory objects sit over subtle circular/dot positions instead of thick square cell borders.
- item art receives more attention than chrome.
- quantity numbers stay compact and close to the item.
- selected items gain a clean highlight rather than a heavy neon outline.

**3. Small contextual controls instead of permanent button walls**
- money/status can live in small rounded pills.
- secondary actions appear near the active item/context.
- choice menus use a small rounded list/pill close to the dialogue rather than huge full-width buttons.
- Campfire's catch-overflow organize/swap/safe-hold UI should follow this compact contextual pattern.

**4. Clear visual hierarchy with restrained information density**
- one main task dominates each screen.
- secondary information is visibly subordinate.
- empty space supports readability, but controls are not inflated merely to fill space.
- Campfire should copy the hierarchy principle, not the exact spacing.

**5. Pastel category color + warm neutral text**
- soft cream, mint/teal, yellow, peach and muted category colors communicate a relaxed tone.
- main text is dark warm gray/brown rather than pure black where contrast remains sufficient.
- color is supportive, never the only state indicator.

**6. Rounded icon-first navigation**
- NookPhone-style app navigation uses compact rounded icon tiles and direct labels.
- icons are highly legible silhouettes with limited detail.
- selection is reinforced by movement/highlight and pointer feedback.
- Campfire M-menu may borrow the principle of shallow icon-first top-level navigation, but must keep location-bound systems out of the menu.

**7. Central avatar/product preview**
- fitting-room/wardrobe screens keep the character large enough to inspect while item categories remain around the edges/top.
- this is a strong reference for Campfire clothing preview and possibly shop try-on.
- the preview does not require making every surrounding control huge.

**8. Map is a destination/identity screen, not a debug map**
- stylized terrain occupies most of the screen.
- important facilities/residents use simple symbols/portraits.
- technical coordinate information is subtle.
- this reinforces Campfire's no-debug-map/no-permanent-minimap direction.

**9. Dialogue remains part of the world**
- large cream dialogue bubble sits at the bottom/foreground while character/world stays visible.
- the speaker identity has a small colored name treatment.
- choices appear only when needed and stay visually close to the conversation.
- Campfire should preserve the same world-visible principle with its own external asset family.

**10. Motion is part of the UI feel**
- selection has soft bounce/scale/spring-like response.
- transitions feel weighted rather than instant hard swaps.
- use restrained motion for Campfire icon selection, page change, toast entry and compact choice prompts.
- avoid excessive glow, screen shake or long ornamental animation.

**11. Physical-world actions remain physical**
- a useful UX lesson from New Horizons is that some interactions are intentionally tied to places rather than available from a global menu.
- Campfire already follows this for mailbox, notice board, shops, museum donation, resident services, wardrobe/storage and harbor travel.
- do not erase the village loop in the name of menu convenience.

Known UX friction to avoid copying:
- repetitive dialogue before routine transactions.
- unnecessary multi-step museum/shop flows.
- shallow search/filtering in large catalogs.
- forcing repeated confirmations for batch actions.

Campfire should keep the visual softness and clear hierarchy while improving batch donation/selling, filtering, search and repeated-action efficiency.

### Rounded/cozy GUI resource candidates

The preferred Campfire visual direction is a friendly rounded life-sim UI, with Animal Crossing-like softness as a reference only.
Do not copy Nintendo's exact icons, layouts or branding.

**Round Up**
- Minecraft 26.2 compatible resource pack.
- makes the whole game UI rounder/cleaner.
- MIT project page, with additional private/modpack redistribution terms that must be respected.
- strong first candidate for the baseline vanilla inventory/container shape language because it already targets the exact "rounder UI" requirement.
- test actual slot readability, container coverage, font compatibility and modded-screen consistency before adoption.

**Pure UI**
- Minecraft 26.2 compatible.
- covers inventory, crafting, furnace, chest, enchanting, anvil, shulker, recipe book, hotbar and buttons.
- also advertises support for Sophisticated Storage / Sophisticated Backpacks among other mods.
- ARR licensed; treat as an installable/reference resource pack, not something to copy into Campfire.
- strong comparison candidate if its rounded/simple visual density fits better than Round Up.

**CozyUI+**
- excellent visual reference for soft pastel/rounded GUI and broad mod-UI restyling.
- current official compatibility listed through 1.21.10 rather than Minecraft 26.2.
- GPL-3.0-only.
- do NOT treat as a current 26.2 dependency unless compatibility is directly proven later.
- use as a visual/reference benchmark for cozy proportions, borders, slots and mod-screen consistency.

**Better GUI for Sophisticated Backpacks**
- a 26.2-compatible resource pack exists specifically to give Sophisticated Backpacks a CozyUI-like presentation.
- GPLv3.
- particularly relevant if Campfire adopts Sophisticated Backpacks, because it can prevent the backpack screen from visually breaking away from the cozy inventory language.
- evaluate together with the chosen baseline GUI pack rather than stacking blindly.

Selection workflow:
1. install/test Round Up and Pure UI independently on the real 26.2 Campfire client.
2. compare inventory slot shape, text readability, GUI scale behavior and compatibility with chosen storage/tool mods.
3. use CozyUI+ as visual-reference material even if it cannot be adopted directly on 26.2.
4. pick one baseline visual family.
5. adapt Campfire-specific M/menu/dialogue/calendar screens to that family using external assets/frameworks.
6. do not combine multiple full GUI reskins if they fight each other.

Inventory-specific rule:
- rounded slots are desirable.
- keep slot boundaries readable and selection/favorite/locked states unambiguous.
- do not enlarge slots excessively just to emphasize roundness.
- inventory capacity and information density remain practical.
- the full-inventory catch-resolution prompt should reuse the same rounded slot/button/icon family rather than falling back to vanilla confirmation buttons.
- compare external life-sim reference layouts for compact "keep/swap/manage" decisions; do not invent three giant centered buttons simply because three actions exist.

### Unified M-menu / UI framework candidates

The M-key menu is a Campfire life-information hub, not a remote replacement for physical village interactions.

**FancyMenu**
- current Minecraft 26.2 NeoForge release exists.
- client + server.
- supports custom GUI screens built from scratch.
- supports reusable/universal layouts across screens.
- provides developer integration paths.
- custom license: treat as a dependency/integration candidate, not code/assets to copy casually.
- strong prototype/implementation candidate for testing external-reference-driven menu layouts without hardcoding every screen immediately.
- must not expose FancyMenu's editor/menu bar to ordinary players as part of the final game UX.

**UI Lib by DAQEM**
- current Minecraft 26.2 NeoForge release exists.
- client + server.
- Apache-2.0 licensed.
- small UI framework/library with published source/wiki.
- candidate for a cleaner code-native Campfire UI implementation if it provides the widgets/layout behavior needed.
- evaluate against FancyMenu before adopting both; avoid redundant UI frameworks.

**Modern UI unofficial port**
- a current 26.2 NeoForge community port exists.
- powerful view/text/render framework, but it is an unofficial port with a larger rendering/UI footprint.
- evaluate only if Campfire actually needs its text/layout capabilities; do not add it merely for visual novelty.

Navigation-design rule:
- first select strong external life-sim/menu references.
- derive information hierarchy, control density, icon/tab behavior, spacing and screen proportions from those references.
- then choose the implementation framework.
- do not let the chosen framework dictate a generic Minecraft/mod-config aesthetic.

Scale/density rule:
- do not invent giant button/card dimensions simply to fill available screen space.
- preserve the selected external asset/reference proportions.
- validate at multiple real Minecraft resolutions and GUI scales before considering the UI complete.
- prioritize readable information density over empty decorative chrome.

Physical-interaction systems such as mailbox, notice board, shop transactions, museum donation, resident-services administration and Lost & Found must remain separate world interactions even if the same UI asset family is used.

### Island map candidates / references
**Xaero's World Map**
- current Minecraft 26.2 NeoForge release exists.
- client + server.
- mature fullscreen explored-world map implementation.
- All Rights Reserved.
- candidate as a dependency/reference for map rendering/technical behavior only; do not copy code/assets.
- its default technical exploration-map presentation is not automatically the desired Campfire visual style.
- if adopted, Campfire still needs an authored life-sim presentation focused on facilities, homes and landmarks rather than debug-like coordinate/waypoint density.

A permanent minimap is not currently required.
The main requirement is an authored map page inside the M-menu.

### Village notice-board candidates / references
Current search found no clean drop-in **Minecraft 26.2 NeoForge** notice-board mod that already matches the required civic schedule system.

Useful references / code-base candidates:

**Bulletin Board by IUnnamedUserI**
- MIT licensed.
- client + server.
- published for Fabric 1.20.1/1.21.1.
- physical wall board with visible note slots.
- full/small note slots.
- note editor/view UI.
- authored/anonymous player notes.
- hover/interaction highlighting.
- persistent/sealed-note concepts.
- current project roadmap mentions a future Forge port, but no current 26.2 NeoForge build exists.
- because the license is MIT, legitimately obtained source code may be used, modified and ported with the copyright/license notice preserved.
- this would be a real Fabric→NeoForge and 1.21.1→26.2 port, not a version-number edit; registry, networking, GUI/container and rendering APIs must be rewritten as required.
- current public project pages did not expose a source-repository link during this planning pass. Do not make the project depend on unavailable source. If clean source is obtained later, evaluate reuse versus a smaller native Campfire implementation.
- even if code is reused, Campfire's schedule/categories/event data remain project-specific rather than inheriting the mod's generic note model blindly.

**The Board by AkorpuzZ**
- NeoForge shared visual posting/feed concept with text/images.
- current published game version is 1.21.1 rather than 26.2.
- reference only unless a suitable update/source path appears.

**Welcome Board**
- supports 26.2 NeoForge.
- first-join welcome screen, not a persistent in-world civic schedule board.
- reject as the schedule-system solution.

**EaseGUI**
- supports 26.2 NeoForge.
- configurable GUI animation/polish.
- may be evaluated as optional presentation polish.
- does not replace Campfire's schedule/event data model.

Design references from Animal Crossing-style town boards:
- event notices.
- birthdays.
- weather warnings.
- shop/facility notices.
- competition results.
- villager/community flavor messages.
- player-written messages.

Preferred direction:
- source a strong external physical notice-board block/model/build or adapt a compatible asset.
- if usable MIT source for Bulletin Board is obtained, inspect it before deciding whether porting saves real work.
- keep schedule/event/category data Campfire-specific.
- render the UI using the chosen coherent external UI design language rather than copying vanilla book/sign screens.
- reuse interaction/design ideas where useful without cloning another game's board art.

Do not mix many incompatible UI styles.
Choose a coherent system and use it consistently.

## 14. Inventory / player model / camera candidate systems

### Portable storage / tool access
Specimen individuality makes portable storage a likely real need rather than an optional late concern.

Verified current 26.2 NeoForge candidates to investigate further:

**Sophisticated Backpacks**
- Minecraft 26.2 NeoForge support confirmed.
- client + server.
- tiered portable storage.
- wearable/placeable.
- configurable upgrades.
- advanced filters can distinguish item/mod/tag/NBT-style data.
- individual features/upgrades can be disabled if they clash with the cozy game direction.
- requires Sophisticated Core.
- strong functional candidate, but final adoption still requires visual/integration testing.

**Traveler Tool Belt**
- Minecraft 26.2 NeoForge support confirmed.
- client + server.
- configurable quick-swap/radial tool access.
- useful candidate for lifestyle-tool convenience if its presentation fits the final player model/UI.

**Packed Up**
- Minecraft 26.2 NeoForge support confirmed.
- simpler backpack alternative worth comparing against Sophisticated Backpacks if the latter is too feature-heavy for the project.

Dedicated portable fish/insect storage:
- no sufficiently convincing 26.2 NeoForge external solution has been selected yet.
- continue searching before writing a custom system.
- if none is suitable, prefer the chosen external general-backpack framework plus a small Campfire-specific category/filter container layer over a complete custom inventory replacement.
- the final container stack must support or allow Campfire to implement **automatic catch routing**: fish → fish container, bugs → insect container, sea-life → suitable specimen container, then general backpack, then normal inventory.
- routing must preserve specimen NBT/components/data exactly and never merge distinct variants incorrectly.
- evaluate whether the external container API/filter system can accept items programmatically without opening a screen; if not, this materially weakens the candidate for Campfire.

Display storage:
- Better Fishtanks supports Minecraft 26.2 / NeoForge and is a candidate for home/museum aquarium presentation.
- this is a display-system candidate, not a substitute for portable fish storage.

### Clothing / armor presentation candidates
Campfire should not rely on vanilla iron/diamond/netherite visuals as the normal player look.

External-asset-first targets:
- everyday clothing sets.
- hats/accessories.
- rainwear.
- winter outfits.
- festival/work outfits.
- diving suits/accessories.
- cave/exploration protection.
- light adventure/combat gear.

Actual clothing/armor art may come from compatible mods, model/resource packs or directly reusable/licensed model sets, but must fit:
- the final player rig.
- full-body player animations.
- sitting.
- fishing/net/watering.
- guitar performance.
- photography poses.
- multiplayer rendering.

**Armor Cosmetic**
- current Minecraft 26.2 NeoForge build exists.
- client + server.
- separates functional armor from a separately rendered cosmetic armor set.
- per-slot visibility toggles.
- useful framework candidate for keeping life-sim clothing visible while retaining hidden/secondary exploration protection.
- current project page identifies the fork as MMPL-licensed and publishes source alongside releases.
- this is a presentation/slot framework candidate, NOT a source of the actual final outfit art.
- must be tested against Player Animation Library, the chosen clothing/model assets and Campfire inventory UX before adoption.

**Tepox Cosmetic Armor**
- Minecraft 26.2 NeoForge candidate with separate cosmetic armor slots.
- smaller/newer alternative.
- custom license; licensing/source/integration must be inspected before any reuse.
- compare only if it offers a real advantage over Armor Cosmetic.

Selection principle:
- prefer curated Campfire wardrobe UX over exposing a generic technical cosmetic-armor screen as the final player-facing experience.
- if an external cosmetic-slot system is adopted, Campfire may wrap/integrate its functionality into the wardrobe/mirror flow.
- actual outfit art should be external high-quality assets, not recolored vanilla armor placeholders.

### Photography candidates
Photography is now a supported gameplay direction because viable 26.2 candidates exist.

**Camera Mod by henkelmax**
- Minecraft 26.2.
- NeoForge.
- client + server.
- captures actual rendered game images.
- creates usable picture items.
- supports albums.
- supports resizable in-world image frames.
- supports copying photos.
- records photographer/date metadata.
- multiplayer compatible.
- saves images with the world.
- license is restrictive/ARR on Modrinth, so packaging/distribution implications must be checked even though the current project target is private play.

**Camerapture**
- Minecraft 26.2.
- NeoForge among supported platforms.
- client + server.
- working in-game camera.
- shareable pictures.
- wall display.
- album support.
- MIT licensed.
- strong candidate to compare directly against Camera Mod.

Selection rule:
- test one at a time with the current NeoForge 26.2 project.
- prefer the option with cleaner runtime behavior, multiplayer sync, save behavior, UI fit and acceptable licensing.
- do not build a custom photo renderer/storage system unless both practical external options fail.
- do not install both merely for feature count.

### First-run character / appearance candidates
The desired UX is a **Campfire-specific first-time character creator**, not a raw mod editor.

Target flow:
display name
→ curated base appearance
→ hair/face/skin options supported by the chosen external appearance backend
→ starter outfit
→ birthday/profile confirmation
→ island arrival.

Candidate backends / references:

**Customizable Player Models (CPM)**
- current Minecraft 26.2 NeoForge release exists.
- client + server.
- MIT licensed.
- source is published.
- strong candidate for player-model/avatar rendering and preset model support.
- its full editor is more technical than the desired Campfire first-run UX, so do not expose the raw editor as the default onboarding screen.
- evaluate whether curated predefined models/layers can be selected by Campfire while CPM handles rendering/sync.
- must be tested with Player Animation Library, cosmetic armor/clothing and guitar/fishing/photo animations.

**NCL Skins**
- current Minecraft 26.2 NeoForge release exists.
- client + server.
- GPLv3.
- supports saved complete looks, preview/editor flow, skin/model/cape/outer-layer combinations and look switching.
- useful reference/candidate for wardrobe/look management after onboarding.
- do not copy GPL code into Campfire without deliberately accepting the resulting licensing obligations; using it as a separate dependency is a different integration decision.

**Avatar Editor (A)**
- current Minecraft 26.2 NeoForge support exists.
- client + server.
- provides in-game wardrobe/skin creation and live switching.
- ARR licensed, so treat as a dependency/reference only; do not copy its code/assets.
- powerful editor, but likely too editor-heavy for the simple first-arrival experience unless wrapped/limited.

**Simple Nicknames**
- current Minecraft 26.2 NeoForge release exists.
- client + server.
- generic configurable nickname system.
- ARR licensed.
- useful compatibility/reference candidate for rendered display names, but Campfire should own the actual player display-name field in PlayerProfile so resident dialogue, mail, museum records and permissions remain game-state aware.

Preferred direction:
- Campfire owns the first-run UX and server-authoritative profile/display-name data.
- external appearance systems provide high-quality model/skin/look rendering where they fit.
- use curated presets/layers rather than asking ordinary players to pixel-edit a Minecraft skin before they can play.
- retain later wardrobe/mirror access for appearance changes.
- do not add multiple overlapping skin/model mods unless each has a clearly separate role.

### Player animation candidate
**Player Animation Library by ZigyTheBird**
- Minecraft 26.2 release 1.2.6 supports both Fabric and NeoForge in one merged JAR.
- NeoForge integration is explicitly supported.
- client + server.
- MIT licensed.
- designed as a library for mods to animate players without conflicting animation stacks.
- supports animation data from Blender/Blockbench JSON, including GeckoLib and Bedrock-style formats.
- supports custom pivot points/bones and layered animation behavior.
- strong leading candidate for replacing the current guitar arm-only presentation with full-body guitar performance and for selected player lifestyle animations.
- final adoption requires direct compatibility testing against Campfire Sessions, NeoForge 26.2.0.87 and the final player-model/clothing stack.

Current guitar limitation:
- Campfire's existing GuitarArmPoseParams only transforms the two humanoid arms.
- CampfireClientSetup currently applies that pose only when the rendered entity is the local Minecraft player and local music playback is active.
- the guitar remains rendered as a hand-held item transform, which explains why the current presentation can still look like one-handed holding rather than a body-mounted performance.
- this is an implementation prototype, not the target final animation.

Target guitar animation:
- full-body keyframed stance.
- left/fretting hand aligned to the neck.
- right/strumming hand aligned to the strings.
- torso/head motion.
- standing and seated performance variants where feasible.
- synchronized remote-player state in multiplayer.
- instrument render transform/attachment aligned to the animation rather than relying only on vanilla hand attachment.

### Other candidate systems
Potential roles:
- external player-model/clothing system such as Customizable Player Models if integration fits.

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

## 16. Audio / ambience / resident voice candidates

The life-sim should not rely only on vanilla ambient audio.

External-first research should cover:
- rain/storm ambience.
- softened indoor rain/window ambience.
- shoreline/harbor wind and water.
- café/interior room tone.
- seasonal night/nature ambience.
- short non-verbal resident vocal chirps.

Requirements:
- no copyrighted game voice clips or direct imitation of another game's speech-sound identity.
- prefer CC0/permissive sound libraries or compatible mods/assets with clear private-use rights.
- resident speech sounds must remain short and subordinate to readable text.
- pitch/timbre variation may be data-driven by resident identity/personality.
- avoid excessive simultaneous loops and large always-loaded sound banks.

If a strong existing ambience mod/system supports 26.2 NeoForge and can be scoped/configured to the authored world, evaluate it before implementing a parallel system.

## 17. Festival and event art

The multi-day festival needs external:
- stalls.
- banners.
- lights.
- stage/performance props.
- themed food stands.
- seasonal decoration.
- contest props/trophies.

Festival presentation must not be temporary programmer art.

## 18. Selection protocol

Before adopting any candidate:
1. verify current Minecraft 26.2 / NeoForge compatibility where relevant.
2. inspect actual visuals, not just description.
3. check whether it clashes with the chosen overall art direction.
4. check performance impact.
5. check multiplayer/runtime behavior where relevant.
6. record source and usage status.
7. integrate only after it has a clear role.

When a candidate is actually adopted, move/document it in THIRD_PARTY_ASSETS.md with source and usage notes.
