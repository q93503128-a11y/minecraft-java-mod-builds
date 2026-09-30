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

### Canonical world selection — ACCEPTED

**Geming400 — Island map | 1024×1024 / Island - No WorldBorder.zip**

Status: **ADOPTED AS CAMPFIRE BASE WORLD**

Verified package/source facts:
- creator: Geming400.
- license: MIT.
- direct downloadable archive obtained successfully by CI.
- archive SHA-256: `7a3d98ff75feb26913e2d4c32ca7339448d3c660c14f986ce9f5e4ff340d3d3b`.
- actual archive size: about 51.2 MB.
- `level.dat` world name: `1024x1024 | Island map`.
- actual stored DataVersion: **3105**.
- region files: **16**.
- parsed chunks: **9,216**.
- actual 26.2 NeoForge server load/conversion: **SUCCESS**.
- validation workflow run: **36675999301**.

Real-terrain probe:
- surface probe read block-state data directly because the WorldPainter chunks contain empty Heightmaps compounds.
- sample spacing: 4 blocks.
- dominant main landmass estimated surface area: **~111,472 blocks**.
- dominant landmass approximate bounds: **552×332**.
- meaningful secondary land components include approximate sampled areas of ~8.3k, ~8.2k, ~7.8k, ~4.6k, ~4.0k, ~2.8k and ~2.3k blocks, plus smaller islets.
- this is consistent with the intended main-island + several destination-island structure.

Strict flatness probe:
- highly-flat 16×16 tiles: **150** world-wide.
- largest connected strict-flat cluster: **25 tiles / ~6,400 blocks**, about **80×128** bounding size.
- best 128×128 window: 27 strict-flat tiles.
- best 160×160 window: 34 strict-flat tiles.
- best 192×192 window: 40 strict-flat tiles.

Interpretation:
- the main island is large enough.
- it is not naturally a completely flat city canvas.
- the selected compact external building shells make the available flatter zone usable.
- Campfire will author a **limited landscaped/graded village plateau and path network** instead of rejecting the 7-island travel structure for a flatter but less useful map.
- preserve coastlines, broad terrain character and non-village natural areas.

Packaging direction:
- preserve the original MIT license/credit.
- store the approved edited Campfire version as a protected world template in the Modrinth-pack workflow.
- provision a playable copy on first use.
- never overwrite an existing progressed save during pack updates.
- no user-side world ZIP installation.

### Rejected / reference-only alternatives

**4K Flat Islands Map for Creative**
- useful extreme-flatness reference.
- only a few major landmasses and less attractive for Campfire's repeated island-travel structure.
- redistribution status is less straightforward than the selected MIT map.
- no longer an active base-world candidate.

**Worldpainter 4000×4000 island for cities**
- useful flat-main-island geometry reference.
- not currently a directly packable production candidate.

**Wullestor**
- rich exploration/archipelago reference with caves and stronger terrain.
- more vertical and less suitable for the compact authored civic core than the selected base.

Map selection is now closed unless later real placement/playtest reveals a blocking issue.

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
- CI downloaded the actual 26.2 NeoForge JAR and parsed its packaged structure NBT successfully.
- exact packaged bounds:
  - Town Hall: **11×9×11**, ground footprint 11×11.
  - Village Shop: **11×9×11**, ground footprint 11×11.
  - Mini Hospital: **11×9×11**, ground footprint 11×11.
  - spruce variants use the same 11×9×11 bounds.
- these are substantially smaller than the previous conservative civic-building envelope and strongly support the separate-large-interior approach.
- strong first extraction/reference candidate for **resident services, general store and clinic exterior envelopes**.
- do not adopt the rest of Villageria's villager/combat/economy gameplay; only inspect/reuse permitted structure assets or treat the mod as an asset source when technically cleaner.

**SY Village**
- current Minecraft 26.2 NeoForge latest release observed during the follow-up pass: `syvillage-1.0.0.jar`.
- earlier 0.3.0 remains in its file history; do not treat it as latest.
- MIT licensed.
- CI downloaded and inspected the actual 1.0.0 JAR.
- packaged custom structure NBT found in that JAR:
  - gatehouse: **25×10×9**, ground footprint 25×9.
  - tower: **17×10×17**, ground footprint 17×17.
  - rampart: **2×6×5**, ground footprint 2×5.
- this means SY Village is **not currently a useful direct house-prefab source** despite its broader blueprint gameplay description; the packaged custom NBT is defensive infrastructure.
- retain it as a technical blueprint/structure-placement reference, not the primary player/resident housing-art source.

**Towns and Towers**
- current Minecraft 26.2 release 1.13.11 supports NeoForge/Fabric/Quilt.
- exact current 26.2 CurseForge file: `t_and_t-fabric-neoforge-1.13.11.jar`, file ID 7886369.
- contains dozens of village structures and several coastal/biome architectural families, including a dedicated beach/fishing-village style.
- current CurseForge license page explicitly publishes the project under **CC BY 4.0**.
- attribution and modification notice are required for any extracted/adapted structures.
- added to the analysis-only CI structure probe so actual packaged NBT footprints can be measured before selecting café, clothing-shop, museum-like civic shells, houses or harbor structures.
- do not import the entire worldgen system merely to obtain one building if a small credited structure subset can cleanly satisfy the role.

Actual CI NBT probe findings:
- combined probe (Towns & Towers + Villageria + SY Village) parsed **851 structures with explicit size**.
- the **Mediterranean** family is especially promising for the island-life village:
  - small houses commonly **5×5**.
  - medium/large shells commonly **5×10, 10×5 or 10×10**.
  - med_library_1: **10×10×10**, useful civic/shop/café-shell reference.
  - med_meeting_point_1: **25×17×23**, footprint 25×23; likely too large for a routine shop shell but useful plaza/civic reference.
- **Iberian** family offers larger upgraded-looking homes:
  - small houses roughly 8×6–8×8.
  - medium houses roughly 11×9–15×11.
  - large houses **16×14** and **17×15**.
  - strong candidate source for visually meaningful house-upgrade stages without increasing interior-space requirements.
- **Beach lighthouse** family provides:
  - beach_main_house_1: **9×10×14**, footprint 9×14.
  - beach_outdoor_shack_1: **7×5×9**, footprint 7×9.
  - beach_meeting_point_1: **25×35×29**, footprint 25×29.
  - useful coastal/harbor/lighthouse reference pool.
- **Birch Romanian** family provides compact 7×7 small homes and ~13×10 to 15×9 larger shells.
- **Classic** family provides compact 6×6/6×7 homes and ~12×11 large-house shells.
- exact role selection remains visual-first: footprint suitability alone does not make a structure final.

Current visual direction from these measured families:
- prioritize **Mediterranean + selected Beach/Iberian structures** for the next visual inspection because their scale and coastal/life-sim silhouette are the strongest fit.
- do not mix every biome style in one village.
- aim for one coherent main-village architecture family, with secondary-island structures allowed to diverge more.

**kogtyv-Towny and Village**
- current Minecraft 26.2 NeoForge build exists.
- MIT licensed with source linked.
- expands village generation toward larger town/city structures.
- useful additional permissive structure pool/reference if Villageria/SY Village do not provide a suitable civic/house shell.
- not automatically preferred: generated-city scale may be too large or visually busy for Campfire's compact island village.

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

### Building acquisition status after current research pass
- **Resident services:** Villageria Town Hall — exact 11×11 footprint verified.
- **General store:** Villageria Village Shop — exact 11×11 footprint verified.
- **Clinic:** Villageria Mini Hospital — exact 11×11 footprint verified.
- **Player/resident houses:** still unresolved. SY Village was directly probed and does not provide the expected custom house NBT pool; continue with Towns and Towers and other legally extractable house sets.
- **Café / clothing shop / museum shell:** still unresolved; Towns and Towers is now the strongest current structure-pool probe because its 26.2 JAR and CC BY 4.0 distribution terms are both available.
- **Harbor/pier:** Peterwolf's boat mod is entering direct runtime build validation; Campfire still needs its own authored main-harbor structure/anchor plan.
- current measured civic shells are small enough that the earlier 160×160 village test envelope is deliberately conservative, not evidence that the 7-island map is cramped.
- do not declare the building set complete until actual structure bounds and screenshots/models are inspected together for one coherent village language.

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

### Current leading candidate: Skniro's Furniture
Skniro's Furniture is now the strongest primary furniture candidate found in the current pass.

Verified:
- Minecraft 26.2 NeoForge support.
- current 26.2 NeoForge release observed in this pass: **1.5.2**.
- published on Modrinth, so the final .mrpack can reference the project/version directly instead of rebundling an arbitrary JAR.
- client + server.
- MIT licensed.
- public source with a maintained `26.2` branch.
- source-inspected 26.2 branch commit during this pass: `00fd0eba1805ded3c16546ef51366c5ad138ec7c`.
- broad Minecraft-native style rather than a separate high-poly aesthetic.

Source inspection confirms real interactive systems rather than decoration-only blocks:
- ChairBlock creates a server-side ChairEntity and mounts the player for actual sitting.
- SofaBlock similarly supports sitting and connected sofa pieces.
- dedicated block entities/classes exist for cabinets, drawers, bedside/desk/kitchen/wall cabinets, kitchen sink, oven, TV and TV stand.
- lamps and functional beds are implemented as real blocks/systems.

The 26.2 source contains a very large variant/model set. Filename-level inventory inspection found, among others:
- 251 chair-related block models.
- 103 table-related models.
- 68 sofa-related models.
- 271 cabinet-related models.
- 99 desk-related models.
- 41 sink variants.
- 41 oven variants.
- 107 TV/stand variants.
- 66 lamp variants.
- 51 fridge parts/variants.
- 17 bookshelf variants.
- 249 window-related models.
- many wood/color bed/cushion/door variants.

The enormous raw count includes generated color/wood/part variants, so it is **not** the same as thousands of unique furniture designs.
Nevertheless, breadth is clearly sufficient for homes, café/shop furnishing and ordinary resident interiors.

Adoption gate:
- visually inspect the actual 26.2 NeoForge build in Campfire.
- verify collision, rotation, seating anchors and multiplayer sync.
- measure performance with multiple furnished interiors.
- decide whether Campfire wraps its storage/kitchen behavior or uses the mod UI directly.
- verify whether furniture placement preview/undo needs a thin Campfire layer.
- if adopted, avoid duplicating the same furniture categories with another large mod.

### Supplement candidate: BetterDeco
BetterDeco retains a current 26.2 NeoForge build and permissive MIT licensing.
Use it only when it fills a meaningful visual/category gap that Skniro does not cover well.
Do not stack two full furniture ecosystems merely for item count.

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

### Croptopia — leading crop/food-content candidate
Verified current direction:
- Minecraft 26.2 NeoForge.
- client + server.
- MIT licensed.
- current public description advertises 250+ foods, 58 crops and 26 fruit trees.
- strong external base for Campfire's crop/fruit/food breadth.

Adoption intent:
- use the crop/fruit/food catalogue where its art fits.
- Campfire still owns season availability, watering cadence, economy and collection rules.
- do not expose irrelevant survival/advancement progression merely because the dependency includes it.

### Cooking for Blockheads — leading kitchen interaction candidate
Verified:
- Minecraft 26.2 NeoForge.
- client + server.
- mature multiblock kitchen.
- shows recipes available from ingredients the player currently has.
- provides real kitchen blocks such as counters, fridges, oven/cooking table/tool rack/spice-rack-style pieces.
- current distribution license is ARR/custom; use as a dependency when permitted, not as a model/code extraction source.

Potential role split:
- **Skniro's Furniture** supplies broad home/furniture language.
- **Cooking for Blockheads** supplies cooking-specific station UX and recipe interaction if integration tests prove worthwhile.
- overlapping decorative kitchen blocks should be hidden/disabled/de-emphasized where practical so the player does not see two redundant kitchen ecosystems.
- Croptopia supplies the broad crop/food content.

This stack is stronger than writing a shallow custom kitchen from scratch, but direct runtime compatibility and recipe integration must be tested before final adoption.

Farmer's Delight-style options may be revisited only if they provide a clearer benefit than the above stack on 26.2.

## 9. Flora / mushrooms

### Flora Expansion — leading general flora supplement
Verified:
- current Minecraft 26.2 NeoForge build.
- client + server.
- MIT licensed with source linked.
- adds plants, crops, blocks and mechanics.
- useful permissive supplement for decorative/seasonal vegetation beyond Croptopia's food focus.

### Shroomcraft / Shroomcrafted — leading mushroom-theme candidate
Verified:
- current Minecraft 26.2 NeoForge build.
- client + server.
- MPL-2.0.
- adds orange/purple/blue mushrooms, multiple growth scales, matching shroomwood/building/decorative families and mushroom-themed creatures.
- visually much richer than simply recoloring vanilla mushrooms.
- evaluate whether its creature/worldgen content fits Campfire; mushroom flora/building assets may be the main useful role.

### Tiny Flowers
- current Minecraft 26.2 NeoForge build.
- MPL-2.0.
- adds small/petal-like variants of vanilla/modded flowers.
- useful low-impact visual-density supplement if Flora Expansion alone leaves gardens too coarse.
- optional; do not stack plant mods purely for item count.

Final selection should emphasize:
- seasonal appearance.
- flower/color variety without requiring a dedicated breeding-genetics subsystem.
- mushrooms.
- fruit/crops.
- decorative variety.
- consistent Minecraft-friendly art.
- avoid uncontrolled worldgen that would overwrite the authored island template; prefer configured placement/Campfire-controlled ecology where needed.

Current likely division:
- Croptopia → crop/fruit/food breadth.
- Flora Expansion → general decorative/seasonal flora.
- Shroomcraft → richer mushrooms/mushroom decor if its visuals pass.
- Tiny Flowers → optional fine-detail garden layer only if needed.

## 10. Residents

Do not use vanilla villagers as the core cast.

Need an external cute-animal visual base with:
- large readable heads/faces.
- short/charming proportions.
- multiple species/variants.
- consistent rig.
- compatible animation pipeline.

Current external resident/model source pool:

**Kenney Cube Pets**
- CC0.
- current Kenney page exposes 24 package files/assets metadata, while the downloadable OpenGameArt package description identifies **16 actual cubic pet models**; do not mistake file count for resident-species count.
- animated.
- current pack is explicitly supplied in standard 3D interchange formats and is optimized/low-poly.
- version 2.0 is described as a complete remake with added animals and animations.
- strongest current **style/model source** for the cute cubic resident direction.
- important limitation: these are pet-style animals, not already-finished biped villagers with Campfire's full social animation set.
- use as a possible species/head/proportion base only if conversion to the final resident rig remains visually strong.

**Quaternius Cube World Kit**
- CC0.
- 108 models.
- animated/textured.
- includes cube-world characters, animals, enemies and environmental models in FBX/OBJ/Blend/glTF.
- useful broader model/reference source when Kenney lacks a species or prop style.
- inspect carefully for style consistency before mixing with Kenney-derived residents.

**Quaternius Ultimate Animated Animal Pack**
- CC0.
- 12 animals with more than 12 animations each.
- useful animation/model reference and possible editable base for animal motion.
- animals are natural quadrupeds rather than ready-made anthropomorphic residents.

**Quaternius Universal Base Characters**
- CC0.
- six game-ready humanoid base models with a retargetable Humanoid rig and 20 hairstyles.
- average around 13k triangles.
- not a resident visual candidate by itself because the default art is human.
- useful as a rig/topology reference if a cute animal head/body system needs a stable humanoid animation base.

Current conclusion:
- **no coherent external pack found yet that already supplies 20–40 cute anthropomorphic villagers with one consistent biped rig and the required lifestyle animation support.**
- do not prematurely lock residents to a mediocre/inconsistent pack.
- continue searching before authoring a large resident conversion pipeline.
- if no complete pack exists, the preferred fallback is one coherent permissive animal style source + one stable shared rig, not mixing random animal models resident-by-resident.

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

Current tool-art candidates / references:

**Glowing 3D Tools**
- current Minecraft 26.2 resource-pack support.
- 3D models for ordinary tools plus misc items including the brush and fishing rod.
- ARR, but the author explicitly permits use in Modrinth and CurseForge modpacks.
- strong pack-level visual candidate if its style fits Campfire after in-game inspection.
- not an editable asset source for custom watering can/net tiers unless separate permission exists.

**3D Vanilla Items**
- current 26.2-compatible MIT resource/model source exists for a broad set of vanilla items.
- useful permissive source for selected 3D held-item treatment, but does not by itself solve the full lifestyle-tool set.

Additional permissive editable-base candidates:

**Kenney Survival Kit**
- CC0.
- 80 low-poly 3D assets.
- includes shovel, hoe, axe, pickaxe and upgraded variants, plus workbench/resource props.
- useful for establishing a coherent basic/improved tool silhouette language.
- does not by itself solve fishing rod + watering can + bug net.

**Quaternius Survival Pack**
- CC0.
- 50+ low-poly survival assets.
- includes a fishing rod, shovel, bucket and other exploration items.
- useful supplement/reference when Kenney lacks a lifestyle-tool category.
- do not mix its art with Kenney automatically; first compare proportions/material language in Minecraft.

**Bugseid's Crafty Farmy Survival Tools**
- CC0 asset license.
- unusually complete lifestyle list: fishing rod, watering can, shovel, hoe, rake, sickle, bucket and more.
- the complete pack currently requires a small paid download, so it is **not** part of the zero-user-action acquisition path and is not adopted.
- keep only as a fallback option if the free/permissive sources cannot produce one coherent tool family.

Still unresolved:
- one coherent external watering-can model family.
- one coherent bug-net model family.
- enough visually meaningful tier variants for all key lifestyle tools.
- first/third-person transforms and player-animation fit.

Do not manufacture arbitrary material recolors only to reach a tier count.
Finalize the number/names of tool tiers after the actual coherent external art set is selected.

## 12. Boats

### Current leading candidate: Peterwolf's Boats & Ships
This is now the strongest current 26.2 boat candidate.

Verified:
- Minecraft 26.2 NeoForge release **1.0.19**, CurseForge file ID **8752063**.
- no required external dependency is listed for the 26.2 NeoForge file.
- added as the second controlled Campfire runtime/build trial after Skniro's Furniture.
- **Campfire clean build + existing package-contract checks passed with Skniro and Peterwolf enabled together.**
- this is BUILD VERIFIED compatibility only; no client visual/physics/multiplayer playtest has occurred.
- client + server.
- MPL-2.0.
- custom 3D wooden watercraft.
- current 26.2 NeoForge line remains available while the project also continues to newer Minecraft versions.

The mod's three-boat progression maps unusually well onto Campfire's planned lifestyle progression:
- **River Skiff**: small 2-seat starter/social boat with oar animation.
- **Explorer Sloop**: 4 passengers, 9-slot cargo, sail/lantern/helm presentation — strong improved Household/exploration boat candidate.
- **Merchant Schooner**: 6 passengers, 27-slot cargo, dual-mast/cabin presentation — strong premium/far-route/community boat candidate.

Reported system features include:
- walkable decks.
- steerable helm.
- server-authoritative movement/physics.
- animated/furlable sails.
- cargo.
- multiple passengers.

Campfire adoption direction:
- evaluate the boat entities/models/physics as the useful dependency.
- do **not** automatically adopt its unrelated settlement/waterman/economy content.
- test collision, passenger sync, dismount behavior, shoreline docking, performance and compatibility with Campfire's registered Household boat ownership.
- if integration is clean, this may close most of the custom boat-model problem without authoring a parallel vehicle system.

Other references / fallback asset sources:

**Kenney Watercraft Kit**
- CC0.
- 45+ low-poly boats/watercraft in OBJ/FBX/glTF-style source formats.
- excellent permissive editable model source if Peterwolf's runtime/physics stack fails or if Campfire needs an additional cosmetic boat class.
- because these are generic engine models rather than Minecraft-native entities, conversion, collision, attachment and animation work would still be required.
- prefer Peterwolf's working Minecraft entity/physics implementation when its visuals and multiplayer behavior pass testing.

- Small Ships remains a strong visual reference but currently lacks a 26.2 release and is not the leading adoption path.
- larger floating-base boat mods are not preferred because Campfire wants lifestyle travel assets, not mobile bases.

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
- latest observed 26.2 release in this pass: **3.26.3.2170**, CurseForge file ID **8992917**.
- required core selected for the same trial: **Sophisticated Core 1.5.0.2349**, CurseForge file ID **8985852**.
- available through current distribution channels suitable for .mrpack dependency delivery despite ARR licensing.
- **Current Campfire dependency-stack validation:** clean build and existing alpha.6 package-contract checks passed with Sophisticated Core + Sophisticated Backpacks present alongside Skniro's Furniture, Peterwolf's Boats & Ships, Traveler Tool Belt and Player Animation Library.
- this proves build/classpath compatibility only; it does not yet prove catch-routing API suitability, client visuals, storage UX or multiplayer runtime behavior.
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
- latest observed 26.2 NeoForge release in this pass: **1.0.4**.
- available on Modrinth.
- project page explicitly allows public/private modpack use.
- client + server.
- configurable quick-swap/radial tool access.
- useful candidate for lifestyle-tool convenience if its presentation fits the final player model/UI.
- Curios/Trinkets/Accessories integration is optional; on NeoForge the belt can function directly from player inventory without Curios installed.
- ARR means use it as a dependency; do not copy/repackage source/assets outside the allowed modpack path.

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
- current Minecraft 26.2 NeoForge build exists; latest observed 26.2 release in this pass is 1.2.0.
- client + server.
- separates functional armor from a separately rendered cosmetic armor set.
- per-slot visibility toggles.
- useful framework candidate for keeping life-sim clothing visible while retaining hidden/secondary exploration protection.
- current project page identifies the fork under Minecraft Mod Public License/custom licensing and publishes source alongside releases.
- this is a presentation/slot framework candidate, NOT a source of the actual final outfit art.
- its stock inventory GUI is technical/vanilla-like; Campfire should not expose it as the final wardrobe experience if the system can be integrated behind the curated wardrobe UI.
- must be tested against Player Animation Library, the chosen clothing/model assets and Campfire inventory UX before adoption.

**SimpleHats-Lite**
- current Minecraft 26.2 NeoForge release exists; latest observed 26.2 NeoForge file in this pass is 0.3.0.
- MIT licensed.
- client + server.
- Curios-based lightweight cosmetic-hat system with source published.
- strong candidate for external hat/accessory models and/or accessory-slot behavior if the actual art quality fits Campfire.
- very new project, so test multiplayer rendering, animation clipping and content consistency before adoption.

**Quaternius Universal Base Characters + Modular Character Outfits - Fantasy**
- both CC0 source-asset families.
- Universal Base Characters provides a retargetable humanoid base.
- Modular Character Outfits provides 12 outfits built from 62 modular parts with three texture variants and compatible humanoid rigging.
- not Minecraft-native art and not automatically final clothing.
- useful permissive editable-base/reference option if no coherent Minecraft-native clothing set covers rain/winter/festival/work/exploration categories.
- any conversion must be visually adapted to the final Campfire player proportions rather than dropped in as mismatched high-detail models.

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
- current Minecraft 26.2 NeoForge release exists; latest observed 26.2 release in this pass is **v0.6.27c**.
- available on Modrinth.
- client + server.
- MIT licensed.
- source is published.
- strong candidate for player-model/avatar rendering and preset model support.
- its full editor is more technical than the desired Campfire first-run UX, so do not expose the raw editor as the default onboarding screen.
- evaluate whether curated predefined models/layers can be selected by Campfire while CPM handles rendering/sync.
- must be tested with Player Animation Library, cosmetic armor/clothing and guitar/fishing/photo animations.

**NCL Skins**
- current Minecraft 26.2 NeoForge release exists; latest observed 26.2 release in this pass is **1.2.1**.
- available on Modrinth and explicitly permits inclusion in modpacks.
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
- Minecraft 26.2 release **1.2.6** supports both Fabric and NeoForge in one merged JAR.
- available on Modrinth and CurseForge.
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

### Fossil / archaeology candidates

**Better Archeology — leading permissive mechanics/reference candidate**
- current Minecraft 26.2 NeoForge release exists.
- client + server.
- MIT licensed.
- public source; current 26.2.x branch inspected during this pass.
- source contains fossil blocks/items for multiple Minecraft creatures with separate head/body/full forms, archaeology table/content, fossiliferous blocks and excavation-related systems.
- useful external base/reference for excavation, identification and multipart-display implementation.
- limitation: its fossils are Minecraft-mob fossils, not the large prehistoric dinosaur skeleton collection required for the Campfire museum.
- do not adopt its whole worldgen/progression blindly; reuse/integrate only pieces that materially save work and fit Campfire.

**The Fossils Mod — visual/content reference only for now**
- its museum concept closely matches Campfire's target: many physical fossil skeleton/slab displays and multipart exhibit presentation.
- current public release target remains old Minecraft 1.20.1 and licensing is restrictive/ARR.
- therefore do not make it a 26.2 dependency or copy assets.
- use only as a quality/reference benchmark unless a compatible licensed path appears.

**Public-domain / CC0 real fossil scans**
- museum/Smithsonian-style public-domain scans can be authoritative shape references for a small number of centerpiece fossils.
- raw scan meshes are often far too high-poly for Minecraft.
- if used, rebuild/decimate into a deliberate Minecraft-friendly model rather than shipping the scan mesh directly.

Current conclusion:
- archaeology mechanics/reference coverage is strong.
- **high-quality, coherent, Minecraft-friendly prehistoric skeleton art is still unresolved** and remains an active external-asset search task.

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

## 17.5 Current model/asset search status

This research pass has narrowed several categories enough to justify direct runtime/visual trials.

### First runtime-trial stack
When implementation moves from planning to dependency validation, test these **one controlled group at a time**, not all at once:
1. **Skniro's Furniture 1.5.2 / NeoForge 26.2** — **BUILD VERIFIED** with Campfire / Java 25 / NeoForge 26.2.0.87; visual, seating and multiplayer acceptance still untested.
2. **Peterwolf's Boats & Ships 1.0.19 / NeoForge 26.2** — **BUILD VERIFIED together with Skniro**; boat visuals, physics, docking and multiplayer passengers still untested.
3. **Traveler Tool Belt 1.0.4 / NeoForge 26.2** — **BUILD VERIFIED** in the current Skniro + Peterwolf stack; radial UX/visual acceptance remains untested.
4. **Sophisticated Backpacks 3.26.3.2170 + Sophisticated Core 1.5.0.2349 / NeoForge 26.2** — **BUILD VERIFIED** inside the current Skniro + Peterwolf + Tool Belt + Player Animation Library stack; catch-routing/API integration and visual UX remain untested.
5. **Player Animation Library 1.2.6** — **BUILD VERIFIED** in the current Skniro + Peterwolf + Tool Belt stack; no real full-body Campfire animation has been authored/tested yet.
6. **CPM v0.6.27c** — curated player appearance/model backend; test separately against PAL first.
7. **NCL Skins 1.2.1** — compare as an alternate wardrobe/look backend; do not keep both CPM and NCL unless each proves a unique necessary role.
8. photography: compare **Camerapture** against **Camera Mod** independently; adopt one.

Do not call these adopted until the exact 26.2 runtime combination is tested and the visual result is inspected.
For Modrinth distribution, prefer dependencies already published on Modrinth where quality is equivalent; this reduces manual packaging and licensing friction.

### Current build-validation checkpoint
Verified through Campfire's real GitHub Actions clean-build workflow:
- **Skniro's Furniture 1.5.2** — BUILD VERIFIED.
- **Peterwolf's Boats & Ships 1.0.19** — BUILD VERIFIED in combination with Skniro.
- **Traveler Tool Belt 1.0.4** — BUILD VERIFIED in the same combined stack.
- **Player Animation Library 1.2.6** — BUILD VERIFIED in the same combined stack.
- **Sophisticated Core 1.5.0.2349 + Sophisticated Backpacks 3.26.3.2170** — BUILD VERIFIED in the same combined stack.
- external building probe also resolves Villageria, SY Village and Towns & Towers and parses their structure NBT during CI.

Latest successful storage-stack workflow checkpoint:
- commit: `dc7345ca24dc727be257cbddfaf3447b4bc0c9ee`.
- Build Campfire Sessions run: **18** / run ID **36665249692**.
- clean build: SUCCESS.
- external structure probe: SUCCESS.
- existing alpha.6 packaged-asset/contracts: SUCCESS.

Validation boundary:
- **BUILD VERIFIED:** YES for the dependency stack above.
- **CLIENT VISUAL TESTED:** NO.
- **DEPENDENCY GAMEPLAY TESTED:** NO.
- **PLAYTESTED:** NO.
- **MULTIPLAYER TESTED:** NO.

### Visual/model sources worth retaining even if no runtime mod is adopted
- **Kenney Cube Pets (CC0)** — resident style/species base candidate; actual pack describes 16 pet models.
- **Kenney Watercraft Kit (CC0)** — 45+ boat/watercraft model fallback.
- **Kenney Survival Kit (CC0)** — tool/resource/prop base with basic/upgraded tool variants.
- **Quaternius Survival Pack (CC0)** — fishing/exploration tool supplement.
- **Quaternius Ultimate Animated Animal Pack / Cube World Kit (CC0)** — animal rig/motion/reference pool.
- **Quaternius LowPoly Crops Pack (CC0)** — 100+ crop models across growth stages; reference/editable art source if Minecraft-native crop mods leave visual gaps.



**Strong leading trials**
- Furniture: Skniro's Furniture.
- Boats: Peterwolf's Boats & Ships.
- Crops/food: Croptopia.
- Cooking interaction: Cooking for Blockheads.
- General flora: Flora Expansion.
- Mushrooms: Shroomcraft.
- Cosmetic armor separation: Armor Cosmetic.
- Player full-body animation: Player Animation Library.
- Fossil/excavation mechanics/reference: Better Archeology.

**Still materially unresolved**
- final coherent resident biped animal roster/rig.
- final museum dinosaur/prehistory skeleton art.
- coherent full lifestyle-tool family including watering can + bug net + fishing tiers.
- final house/building set for café, clothing shop, museum and housing progression.
- final coherent clothing art set beyond hats/accessory/backend candidates.
- festival/event prop set.

Do not compensate for unresolved art by creating placeholder final visuals.
Continue external search or adapt one permissive coherent source only after visual tests prove it is strong enough.

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
