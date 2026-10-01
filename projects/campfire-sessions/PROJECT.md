# Campfire Sessions

Status: ALPHA.6 MUSIC FOUNDATION / ISLAND-LIFE DESIGN PHASE

- Mod ID: campfiresessions
- Version: 0.6.0-alpha.1
- Minecraft: 26.2
- Java: 25
- Loader: NeoForge 26.2.0.87
- Distribution target: private/personal play via Modrinth .mrpack with all required gameplay assets/config/world template provisioned; no manual map install
- Current product direction: Cozy Multiplayer Island Life Sim
- Canonical game design: `GAME_DESIGN.md`
- External asset/dependency planning: `ASSET_PLAN.md`
- Actually adopted third-party assets: `THIRD_PARTY_ASSETS.md`

## Current foundation

Alpha.6 remains the current implemented foundation:
1. Acoustic Guitar with external Minecraft-native model.
2. Wooden Chair with external furniture model and seating behavior.
3. 15 verified CC0 bundled tracks.
4. build-generated real OGG duration/BPM metadata.
5. inertial song-selection carousel.
6. Kenney external UI artwork.
7. PREV / PLAY-STOP / NEXT / REPEAT-AUTO controls.
8. outside-UI B/N/R key mappings.
9. BPM-driven note feedback.
10. local-player third-person guitar arm-pose prototype; final full-body instrument performance animation and multiplayer sync are NOT implemented yet.
11. up to 32 local custom OGG tracks through `config/campfiresessions/music/`.
12. repeat-one and auto-next.
13. Build Campfire Sessions CI success and produced 0.6.0-alpha.1 JAR.

## Product pivot

Campfire Sessions is no longer planned as only a compact music-and-rest mod.

The existing music system becomes one lifestyle pillar inside a larger island-life game:
- persistent island village.
- animal residents.
- multiplayer households.
- fixed authored buildings.
- housing debt and expansion.
- shops and catalog.
- museum and collecting.
- fishing, bugs, sea life, fossils, flora and mushrooms.
- cooking.
- seasons/weather/calendar.
- pier-based travel.
- exploration and limited combat.
- resident relationships, moving, gifts, mail and visits.
- contests, birthdays, seasonal events and a multi-day major festival.

The complete accepted direction is maintained in `GAME_DESIGN.md`.
Do not re-design these systems from scratch in future chats without an explicit user change.

## Visual production rule

Final player-facing design must be driven by high-quality external assets/reference implementations.
Do not default to improvised AI-authored:
- UI.
- buildings.
- furniture.
- resident appearance.
- tools.
- boats.
- clothing.
- festival props.

Use `ASSET_PLAN.md` for candidate tracking and `THIRD_PARTY_ASSETS.md` for adopted assets.

## World rule

Canonical base world: **Geming400 — Island map | 1024×1024 / Island - No WorldBorder.zip**.

Status: **SELECTED / REAL WORLD INSPECTED / 26.2 SERVER LOAD VERIFIED**.

The selected world is MIT licensed, directly obtainable, inspected from its actual region files and legally suitable for bundling in the Campfire Sessions Modrinth modpack.

The user should not need to manually:
- download a separate map ZIP.
- extract/copy a world into `saves/`.
- install schematics.
- repair version/loader-specific world files.

The modpack ships the canonical world as a protected template/config asset and Campfire provisions the playable save automatically without overwriting an existing progressed world during pack updates.

Map-layout direction:
- the selected world provides one dominant main landmass and multiple meaningful secondary islands.
- the main village uses the existing flatter central area plus targeted authored terrain grading.
- do not flatten the whole island; preserve coastlines, hills and natural identity.
- secondary islands remain available for travel, collection, exploration and event content.

Major building exterior positions are fixed and managed by the game.
Players decorate permitted interiors/yards/public decoration zones but do not freely destroy or rebuild critical village structures.

## Multiplayer authority

Server-authoritative state includes at minimum:
- money.
- loans.
- household membership.
- personal collection encyclopedia.
- houses.
- residents and moves.
- donations.
- shop state.
- public projects.
- calendar/seasons/weather.
- event state.
- protected-world state.

Do not claim multiplayer success until actually tested.

## Scope boundary

- No public-release assumption.
- No bypass of paid access/DRM/access restrictions.
- No player-facing debug/test/prototype/TODO/developer residue.
- No test resources masquerading as final game content.
- No unnecessary duplicate systems or throwaway code.
- No spontaneous AI visual design replacing available external quality assets.

## Current validation state

Implemented alpha.6 music foundation:
- CODE REVIEWED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- PLAYTESTED: NO
- CLIENT VISUAL TESTED: NO
- MULTIPLAYER TESTED: NO

Island-life expansion:
- DESIGN IN PROGRESS
- CANONICAL BASE WORLD SELECTED: YES
- WORLD REGION DATA INSPECTED: YES
- WORLD LOAD VERIFIED ON MINECRAFT 26.2: YES
- REAL CC0 MUSEUM CENTERPIECE SOURCES ACQUIRED: YES — Smithsonian Triceratops + woolly mammoth production candidates
- MUSEUM 26.2 GPU RENDER ARCHITECTURE SELECTED: YES
- MUSEUM VERIFIED STATIC MESH CONVERTER: YES
- MUSEUM STATIC MESH JAVA LOADER + GPU BUFFER UPLOAD CORE: BUILD VERIFIED
- MUSEUM FEATURE RENDERER / SOLID SUBMIT PATH: BUILD VERIFIED
- MUSEUM BASE-COLOR GPU DRAW PATH: BUILD VERIFIED
- MUSEUM NORMAL/OCCLUSION MATERIAL PASS: NOT IMPLEMENTED
- MUSEUM RUNTIME RENDERER: PARTIAL — verified static mesh + GPU buffer + FeatureRenderer path exists; exhibit carrier/world placement and visual acceptance remain
- MUSEUM NORMAL-BUILD ASSET PACKAGING: NO — actual Smithsonian candidates remain asset-review-only until client visual/performance acceptance
- RESIDENT PRODUCTION MODEL ACCEPTED: NO
- TEMPORARY / PLACEHOLDER RESIDENT MODEL ALLOWED: NO
- IMPLEMENTED: NO
- CLIENT VISUAL TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

## Museum renderer checkpoint — 2026-10-01

The first production static-mesh render path now has a concrete implementation target in source:

- `MuseumExhibitAssets.TRICERATOPS_100K` pins the verified 100k-derived Triceratops runtime mesh SHA-256 and its real Smithsonian-derived base/normal/occlusion texture paths.
- `MuseumRenderPipelines` defines a 32-byte CFMS-compatible triangle pipeline and base-color RenderType without per-frame CPU vertex submission.
- `MuseumStaticMeshRenderer` submits into NeoForge's `RenderPhaseKeys.SOLID`, uploads the verified mesh once per renderer lifetime, reuses the GPU buffers, and closes them with the FeatureRenderer lifecycle.
- the mesh is re-hashed before GPU upload; a missing or mismatched production resource fails instead of falling back to fake geometry.
- the initial accepted draw pass samples the real base-color texture. CFMS normal data and the verified normal/occlusion textures remain preserved for the dedicated material pass, which is still pending.
- Smithsonian runtime files are intentionally not bundled into the normal build yet. They remain an asset-review input until client visual/performance acceptance.
- Build Campfire Sessions #44 / run `36804701583`: SUCCESS. Clean build, resident asset contract, external client-review inventory, building bounds probe, alpha6 packaged-asset validation and artifact upload all passed.
- Produced artifact: `campfire-sessions-alpha6` (artifact ID `11136753409`).

This checkpoint is not a visual acceptance. `CLIENT VISUAL TESTED`, `PLAYTESTED`, and `MULTIPLAYER TESTED` remain NO.

## Next planning rule

Continue planning in batches of roughly eight genuinely new decisions.
Already-decided topics should be referenced briefly when needed, not recycled merely to fill the batch.
Before implementation, finish the major unresolved items listed in `GAME_DESIGN.md`, especially:
- final directly obtainable map and real-world inspection.
- building/house prefab set.
- resident model/rig set.
- furniture/UI/tool/boat asset selections.
- player/resident animation stack validation, including the Player Animation Library candidate and final full-body guitar performance.
- economy numbers.
- concrete save serialization details and remaining Household implementation edge cases.


## Museum asset-review placement checkpoint — 2026-10-01

The next museum integration unit now connects the existing GPU renderer to an actual client-world review scene without introducing a fake block/entity/model:

- `MuseumAssetReviewScene` exists only when `campfiresessions.assetReview=true`.
- `/campfire_museum_review triceratops` requires the real verified Triceratops CFMS + Smithsonian base-color resource before activation.
- the review state is extracted through `ExtractLevelRenderStateEvent`, frustum-culled, then submitted through `SubmitCustomGeometryEvent`.
- the verified Smithsonian source metre scale is kept at 1 block per metre for this first visual review.
- the source Z-up geometry is mapped to Minecraft Y-up, centered horizontally and grounded from the verified source bounds.
- the normal Campfire JAR is explicitly checked to contain no `assets/campfiresessions/museum/` review files.
- the Smithsonian probe workflow now emits `campfire-museum-review-pack.zip` as an asset-review-only resource pack.

This checkpoint does **not** count as client visual acceptance. Final transform, 20k-vs-100k LOD choice, and normal/occlusion material work remain gated on real client inspection.


### Museum asset-review verification — Build #46 / Probe #6

- code commit chain: `d48fd3f75d82d5b4fbf7a398e82568f125b93764` → `9dd27817a128b26a1aa8dbf039f03367b417532c`
- Build Campfire Sessions #46: **SUCCESS**, run `36806177255`
- packaged Campfire artifact: `campfire-sessions-alpha6`, artifact `11137424132`
- build workflow verified that `MuseumAssetReviewScene.class` is compiled and that normal Campfire JAR resources contain no `assets/campfiresessions/museum/` review assets.
- Probe Campfire Museum Assets #6: **SUCCESS**, run `36805989072`
- museum staging artifact: `campfire-museum-assets`, artifact `11137134340`
- generated review pack: `campfire-museum-review-pack.zip`
- review pack SHA-256 from the successful artifact: `95ddb0efcfcea5cf7006efddb207513b8018b68b85fda33c2ecfe51b2942c0c7`
- review pack contents were inspected after CI and contain the real Triceratops/Mammuthus CFMS meshes, manifests and Smithsonian-derived textures plus the 26.2 resource-pack metadata.
- first build attempt #45 failed only because the removed 26.2 client method `LocalPlayer.displayClientMessage` was used in the review helper; it was corrected to the current system-message API before #46.

Validation labels after this checkpoint:
- CODE REVIEWED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- MUSEUM REVIEW RESOURCE PACK PRODUCED: YES
- CLIENT VISUAL TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

The next gate is real client visual/performance review of the Triceratops, not more renderer architecture work.


## Canonical village physical-layout checkpoint — 2026-10-01

User playtest gate: do **not** request a user test for renderer/assets alone. The next user test is deferred until the canonical island is presented with the real main-village exterior shells placed as a coherent playable slice.

Completed in this checkpoint:
- canonical terrain probe #15 / run `36806856318`: SUCCESS.
- largest strict-flat connected village cluster is now recorded tile-by-tile, not only as an 80×128 bounding box.
- steep draft positions were corrected before placement: clothing and museum no longer use the earlier cliff-overlap coordinates.
- `VillageReviewBootstrap` adds a development-only physical placement path for **12 real external structures (11 Kogtyv Greece shells + 1 Currents of Trade dock)**:
  - resident services
  - general store
  - clinic
  - café
  - clothing shop
  - museum
  - real harbor dock / service structure
  - player early house
  - four first resident-house shells
- exact packaged NBT sizes are preflighted before any placement.
- placement filters Kogtyv worldgen-only jigsaw/barrier/structure markers so those do not become player-facing village content.
- terrain grading is footprint-local and capped; the system refuses a placement requiring excessive terrain destruction.
- ordinary Campfire gameplay does not activate this bootstrap. It is gated behind the `villageReviewClient` development profile.

Still required before asking the user to test:
- build verification of this placement checkpoint.
- canonical review-world provisioning/launch path.
- first coherent path/plaza connection and harbor/pier treatment.
- then one combined map + real-building client review, instead of separate micro-tests.


### Canonical village placement build verification — Build #49

- final code commit: `1b9c80cb920aa2700603f27a83ad9f0336e3c0b2`
- Build Campfire Sessions #49: **SUCCESS**, run `36807829486`
- clean build: SUCCESS
- resident asset contract: SUCCESS
- external client-review inventory: SUCCESS
- external building structure bounds probe: SUCCESS
- packaged asset/contract verification: SUCCESS
- packaged artifact: `campfire-sessions-alpha6`, artifact `11137444902`
- artifact digest: `sha256:5e266a1e0c65e436a1af658b0418b9e51b70f222d21008475239f57e554ca736`

A static footprint collision pass was also performed over all 12 first-pass structures. The initial café coordinate overlapped the museum; the café was moved to the verified west flat patch before this successful build. Current first-pass footprints have **0 overlaps**.

Validation status:
- CODE REVIEWED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CANONICAL TERRAIN PROBED: YES
- REAL BUILDING PLACEMENT PATH IMPLEMENTED: YES
- CLIENT VISUAL TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Per the current playtest gate, do not ask the user to test yet. Continue with the coherent path/plaza connection, harbor/pier treatment, and canonical review-world launch/provisioning first; then request one combined map + actual-building playtest.


## Connected canonical village review slice — 2026-10-01

This unit moves the next user test from isolated assets to an actual playable map slice.

Implemented:
- real MIT Currents of Trade dock selected as the physical harbor base instead of inventing a temporary pier.
- source pinned to commit `d3b769ec4cbf8d8e744785c4815b6c66107c7884`; the build downloads the exact dock NBT + MIT license from that commit.
- source-only Anchor Point identifiers are converted to a vanilla barrel; no Currents runtime dependency is added.
- `VillageReviewLandscape` adds the first connected plaza/path network across the verified stepped terrain.
- the harbor dock is rotated to extend west into the ocean and is preflighted against both shoreline height and water presence.
- `prepareVillageReviewWorld` provisions the verified Geming400 archive directly into the development review profile, verifies SHA-256, and refuses to overwrite an unrelated save.
- `runVillageReviewClient` now Quick Plays directly into that canonical review save.
- a dedicated `runVillageReviewServer` path lets CI apply the same authored slice to a disposable canonical-world copy.
- the canonical-world workflow now requires actual server-side success for the 12 external structures, landscape pass, harbor dock and v3 completion marker.

Verification for this checkpoint is pending the next Campfire build/world-probe runs. Do not label the slice CLIENT VISUAL TESTED until an actual Minecraft client is inspected.
