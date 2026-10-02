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


## Canonical village review slice verification — Build #57 / Probe #25

The combined map + real-building review slice is now server-runtime verified on the pinned Geming400 canonical world.

Final canonical review placement corrections were driven by actual runtime block-column scans rather than coarse terrain estimates:
- general store: moved west onto the verified flat tile after the old east edge hit Y83 terrain.
- café: `(-287, 73, -54)`; its full 7×8 footprint is runtime surface Y72 throughout, worst grading delta 0.
- museum: `(-292, 75, -2)`; its full 21×16 footprint is runtime surface Y71..78, worst grading delta 4.
- grading safety remains capped at 8 blocks. No limit was relaxed to force these structures into the terrain.
- static footprint review for the current land-building set and plaza reports zero overlaps.

Verification:
- code/design checkpoint: `c7f7ab6610e0f82d3c2ae58f1dce0b4ebbb3585b`
- Build Campfire Sessions #57 / run `36812656837`: **SUCCESS**
- packaged artifact: `campfire-sessions-alpha6`, artifact `11140500942`
- artifact digest: `sha256:ca64f13d8ad00e80a79a478987f9fd568f58c16c7b0804c068ed9b4ba443dac3`
- Probe Campfire Canonical World Runtime #25 / run `36812656475`: **SUCCESS**
- probe artifact: `campfire-world-probe`, artifact `11140187283`
- canonical world SHA-256: VERIFIED
- Minecraft 26.2 canonical-world server load: SUCCESS
- full external/runtime dependency stack server load: SUCCESS
- canonical village review bootstrap: SUCCESS
- real external structures placed: 12
- connected village path/plaza pass: APPLIED
- MIT Currents of Trade harbor dock: APPLIED
- v3 completion marker: VERIFIED by the probe workflow.

Review launch path:
- `prepareVillageReviewWorld` automatically provisions the pinned canonical world only when the review save is absent.
- an existing marked review save is preserved rather than overwritten.
- `runVillageReviewClient` Quick Plays directly into `campfire-village-review`.
- ordinary Campfire saves do not run the review bootstrap.

Validation labels after this checkpoint:
- CODE REVIEWED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CANONICAL WORLD SERVER TESTED: YES
- CANONICAL VILLAGE REVIEW SLICE SERVER TESTED: YES
- REAL EXTERNAL BUILDINGS PLACED ON CANONICAL WORLD: YES
- HARBOR DOCK SERVER PLACEMENT VERIFIED: YES
- VILLAGE PATH / PLAZA SERVER PLACEMENT VERIFIED: YES
- CLIENT VISUAL TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

The next meaningful gate is the first combined **client visual/play review** of the canonical island slice. That review should judge building orientation/entrances, path readability, terrain cuts/fills, dock shoreline fit, village composition and sightlines as one scene rather than as isolated assets.


## One-click Modrinth village-review delivery — 2026-10-01

The user-facing review handoff must follow the same product rule as the final Campfire distribution: **one Modrinth import, no manual map/JAR/resource-pack/Gradle setup**.

Implemented review artifact:
- `Campfire-Sessions-Village-Review-0.6.0-alpha.1.mrpack`
- generated by Probe Campfire Canonical World Runtime #27
- contains the Campfire Sessions JAR, all 13 required external runtime mod JARs for the current stack, and the already-authored `campfire-village-review` save under `overrides/saves/`.
- Modrinth dependencies: Minecraft 26.2 / NeoForge 26.2.0.87.
- the included save already contains the verified v3 village-layout marker, 12 real external structures, plaza/path pass and harbor dock placement.
- no separate map download/copy, schematic import, resource-pack install, JAR copy or Gradle command is part of the user review flow.

Important scope distinction:
- this is a **layout-review save**, so exterior shells for not-yet-open facilities are intentionally visible in order to judge the whole village composition.
- those shells do **not** mean the facilities are available from first-day gameplay.
- canonical first-day available village facilities remain: resident services / administration, small general store, pier/harbor, and the basic communal plaza context.
- museum, clothing, clinic and other later facilities require their proper shared progression/construction/opening state before the normal gameplay start-state can be considered implemented.
- the café is intended to exist from early village life in modest form, but its actual service/progression state is separate from this layout-only review shell.

Packaging validation:
- Probe Campfire Canonical World Runtime #27: SUCCESS.
- private review mrpack artifact produced and uploaded.
- extracted mrpack structure verified locally: valid `modrinth.index.json`, 14 bundled mod JARs including Campfire Sessions, canonical review save `level.dat`, v3 placement marker and review README all present.


## Main-village client visual review — 2026-10-01

Actual Minecraft client inspection of the canonical island + first Kogtyv/Currents village slice has now occurred.

Observed result:
- canonical Geming400 island loads correctly.
- the authored review village and Currents dock are physically present.
- the first Kogtyv Greece civic/residential shell set is visually unacceptable for Campfire's main village.
- the structures read as repeated narrow stone towers with similar timber caps rather than clearly differentiated resident services / store / café / clinic / clothing / museum buildings.
- individual footprint grading produces visible terrace cuts and fragmented circulation.
- the mountain/forest backdrop overwhelms the civic center; the village does not read immediately as the primary cozy life-sim hub.
- this is not a missing-chunk or renderer bug. It is an art-direction / prefab-selection / village-composition failure.

Validation labels:
- CLIENT VISUAL TESTED: **YES**
- FIRST VILLAGE VISUAL ACCEPTANCE: **FAILED**
- KOGTYV GREECE AS FINAL MAIN-CIVIC PREFAB SET: **REJECTED**
- CURRENTS OF TRADE DOCK: still a usable harbor candidate; not rejected by this building review.
- CANONICAL BASE WORLD: retained, but the main-village terrain/layout requires a wider authored terrace pass before final placement.

Required correction:
1. stop optimizing the rejected Kogtyv civic layout.
2. create a broader 2–3 terrace village life-sim zone while preserving coastline and the outer mountain silhouette.
3. replace the main civic shells with role-readable external prefabs.
4. re-evaluate the full scene only after those changes are integrated.


## Second village slice verification — Build #68 / Probe #35

The first Kogtyv civic slice remains visually rejected. The second integrated review slice replaces it with a pinned MIT Chek's Mint Plains structure set and a broader authored village terrain pass.

Final implementation checkpoint:
- code commit: `7a2d753b1f5d7881ce8d49d369b6ab87e73fca06`
- selected external village structures: **12 Chek's Mint + 1 Currents of Trade dock**
- rejected Kogtyv Greece structures packaged in the normal JAR: **0**
- layout footprint collision check: **0 overlaps**
- terrain strategy:
  - natural waterfront + existing Currents dock retained
  - broad civic-west terrace around the mountain toe
  - separate civic-southeast commercial terrace
  - upper residential/museum terrace
  - connected 3-block-wide soft path network
- the east mountain is preserved rather than cut through.
- ocean columns are excluded from broad terrace grading.
- grading safety cap remains 12 blocks for broad terrain remaster; observed runtime worst deltas:
  - civic_west: 5
  - civic_southeast: 11
  - upper: 11
- harbor dock preflight: shoreline connector Y64, outer-water samples 18/18.

Verification:
- Build Campfire Sessions #68 / run `36832104709`: **SUCCESS**
- build artifact ID `11148006460`
- build artifact digest `sha256:ffa052438eaa04f06102805ef71d5b2c09bae8e75ae41be4e13bb29840664154`
- Probe Campfire Canonical World Runtime #35 / run `36832104506`: **SUCCESS**
- one-click Modrinth review-pack artifact ID `11148116319`
- review-pack artifact digest `sha256:5ce436dadad763727ec3d73391b6ee5d7bfb43aea878788c465f202896a50438`
- staged review-world artifact ID `11147447363`
- v4 completion marker: VERIFIED
- 13 external structures: SERVER PLACEMENT VERIFIED
- terraces: SERVER PLACEMENT VERIFIED
- connected paths: SERVER PLACEMENT VERIFIED
- harbor dock: SERVER PLACEMENT VERIFIED
- one-click review `.mrpack`: PRODUCED

Validation labels:
- CODE REVIEWED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CANONICAL WORLD SERVER TESTED: YES
- SECOND VILLAGE SLICE SERVER TESTED: YES
- SECOND VILLAGE SLICE CLIENT VISUAL TESTED: NO
- SECOND VILLAGE VISUAL ACCEPTANCE: PENDING
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Because the baked review world changed materially, the next client review requires a **fresh v4 Modrinth review-pack import**, not a JAR-only replacement. Continue to provide both the JAR and `.mrpack` to the user as requested.


## Second village client review — v4 visual rejection

The user completed a real Minecraft client inspection of the Build #68 / Probe #35 Chek's Mint village slice.

Observed result:
- the Chek's Mint exterior family is materially stronger than the rejected Kogtyv tower set and remains a candidate.
- the **layout itself failed**:
  - homes and facilities are packed too tightly around one compact center;
  - the five-house review count is below the canonical starting-village target of roughly 10–12 houses;
  - there is insufficient parcel reserve for yards, exterior customization and believable housing upgrades;
  - paths do not consistently meet the actual building entrances and read as an imposed road grid;
  - residential life does not spread across the island as intended;
  - black/path-like cutouts are visible around structures.
- source inspection shows the selected Chek structure NBTs contain large explicit `minecraft:air` volumes. Because v4 placed those templates after terrain authoring, those AIR states could carve the prepared ground. Review placement now filters AIR as well as jigsaw/structure markers; the next client slice must verify that this removes the black cutouts.

Validation labels:
- SECOND VILLAGE SLICE CLIENT VISUAL TESTED: **YES**
- SECOND VILLAGE VISUAL ACCEPTANCE: **FAILED**
- CHEK'S MINT EXTERIOR FAMILY: **RETAINED AS CANDIDATE**
- V4 DENSE TERRACE / ROAD LAYOUT: **REJECTED**

Third-slice requirements:
1. use **real parcels/lots**, not only building footprints;
2. starting housing review must contain **10 houses minimum**, matching the lower bound of the canonical 10–12 target;
3. each housing parcel reserves yard and upgrade room around the current starter shell;
4. keep civic services around a readable center, but move café/clothing/clinic/museum to nearby satellite parcels;
5. distribute homes across several island areas, with only a few homes per local cluster;
6. long-distance homes use natural approaches / short front trails rather than a paved road across the entire island;
7. paths must terminate at the actual entrance side of each prefab;
8. preserve the outer mountain/coastline and avoid another giant rectangular village platform.

The third client review is not requested until this parcel-based world is integrated and server-runtime verified.


## Third village physical slice — v5 distributed housing verification

The v4 client rejection is now addressed by a parcel-first third slice rather than another dense village block.

Integrated v5 layout:
- total real external structures placed: **19**
  - 7 civic/service exteriors
  - 1 Currents of Trade harbor dock
  - 11 houses: 1 player starter house + 10 resident houses
- the housing count now sits inside the canonical starting-village target of roughly 10–12 houses.
- homes are distributed across multiple parts of the main island rather than packed around one civic rectangle.
- each housing location reserves an approximately **25×25 parcel** for yard/upgrade use.
- only the central **17×17 inner pad** is initially graded, preserving natural parcel edges while reserving outer expansion room.
- the public center remains compact; clinic and museum are satellite facilities rather than members of the dense plaza block.
- island-wide formal roads were rejected. V5 only authors short civic/harbor paths; outlying homes retain natural approaches.
- Chek prefab placement ignores explicit AIR as well as jigsaw/barrier/structure markers so source template voids do not carve the authored ground.
- the large mountain/coastline outside authored parcels remains preserved.

Canonical-runtime evidence:
- Build Campfire Sessions #76 / run `36952798401`: **SUCCESS**
- build artifact `campfire-sessions-alpha6`: ID `11204623299`
- build artifact digest: `sha256:fcb7a0fbe6b5322c94f5b0367a97ed18b8a73f91d98b58bffc4ee20ba7333162`
- Probe Campfire Canonical World Runtime #40 / run `36953200394`: **SUCCESS**
- review-pack artifact ID `11204629263`
- review-pack artifact digest: `sha256:c9e54524a56da76f6d7301f6ae5b47c1ae37c6e121a06a09697a72b67ba93763`
- staged review-world artifact ID `11204634086`
- canonical-world probe artifact ID `11204996418`
- v5 marker: VERIFIED
- 19 external structures: SERVER PLACEMENT VERIFIED
- 16 authored public/residential terrain areas: SERVER PLACEMENT VERIFIED
- short civic/harbor paths: SERVER PLACEMENT VERIFIED
- harbor dock shoreline/water preflight: VERIFIED

Observed maximum runtime grading deltas stayed within the existing 12-block area safety cap. The cap was not relaxed to make the layout pass.

Validation labels:
- CODE REVIEWED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED: YES
- CANONICAL WORLD SERVER TESTED: YES
- V5 DISTRIBUTED VILLAGE SLICE SERVER TESTED: YES
- V5 CLIENT VISUAL TESTED: NO
- V5 VISUAL ACCEPTANCE: PENDING
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Because the baked village world changed materially from v4, client review requires a **fresh v5 Modrinth review-pack import**. A JAR-only replacement cannot update the already-baked v4 review save.
