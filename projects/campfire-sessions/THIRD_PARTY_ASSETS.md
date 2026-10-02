# Third-party assets

Campfire Sessions uses Minecraft-native open-source models, CC0 music, and CC0 UI assets.

## Canonical world template
- Project: **Island map | 1024×1024** — Geming400
- Distribution file: **Island - No WorldBorder.zip**
- Source page: https://www.curseforge.com/minecraft/worlds/island-map-1-19-1024x1024
- CurseForge file ID: `6229422`
- License: MIT
- Downloaded archive SHA-256: `7a3d98ff75feb26913e2d4c32ca7339448d3c660c14f986ce9f5e4ff340d3d3b`
- Actual stored world DataVersion: `3105`
- Real-file validation: 16 region files / 9,216 chunks parsed
- Minecraft 26.2 NeoForge server-load validation: SUCCESS
- Validation workflow: `Probe Campfire Candidate World` run `36675999301`
- Use here: canonical Campfire base world. The project will preserve the island/shoreline identity while adding the authored village, selective terrain grading, vegetation, paths, facilities, exploration content and other Campfire-specific world changes.
- Packaging: keep the original MIT notice/attribution with the packaged template. The Modrinth pack provisions a playable copy and does not overwrite progressed saves on updates.

## Village structure bases — kogtyv-Towny and Village
- Project: **kogtyv-Towny and Village** — kogtyv
- Source/distribution project: https://www.curseforge.com/minecraft/mc-mods/kogtyv-tav
- 26.2 NeoForge source artifact used by the build pipeline: `kogtyv-tav-963118-8762244.jar`
- Project version: 1.7
- License: MIT
- Embedded license notice verified from the distributed JAR: `LICENSE`, copyright 2023–2026 kogtyv.
- Campfire packages **31 selected Greece-village structure NBTs** as editable external bases:
  - 1 civic center: `ratush_1`
  - 8 small-house variants
  - 6 medium-house variants
  - 4 big-house variants
  - 5 small-shop variants
  - 3 medium-shop variants
  - 3 triple-shop variants
  - 1 larger `triple_1` house
- Generated target root: `data/campfiresessions/structure/external/kogtyv_greece/`
- Generated attribution/license: `META-INF/campfiresessions/licenses/kogtyv_tav_LICENSE.txt`
- Generated provenance manifest: `META-INF/campfiresessions/external_structure_manifest.json`
- Current intended roles include resident services, general store, clinic, café, clothing shop, museum shell, harbor-service shell, player-house progression and resident-house variants.
- Campfire may palette/role-dress these MIT bases while keeping the source/license notice and provenance manifest.
- Build verification: **Build Campfire Sessions run 31 / run ID 36702085816 — SUCCESS**.
- Package-contract verification confirms exactly 31 approved external structures and only source `kogtyv_tav`.

## Harbor dock base — Currents of Trade
- Project: **Currents of Trade** — Lexovian / RedLexo.
- Source repository: https://github.com/Lexovian/Currents-of-Trade
- Pinned source commit: `d3b769ec4cbf8d8e744785c4815b6c66107c7884`.
- Source structure: `src/main/resources/data/currents_of_trade/structure/village/dock.nbt`.
- Source NBT DataVersion: `3955`; Minecraft 26.2's structure loader applies its normal data-fix path when the resource is loaded.
- Measured source bounds: **11×10×15**.
- License: MIT; the complete `LICENSE` from the exact pinned source commit is packaged with Campfire.
- Campfire generated target: `data/campfiresessions/structure/external/currents_of_trade/dock.nbt`.
- Modification boundary:
  - the Currents-only `currents_of_trade:anchor_point` palette/block-entity identifier is rewritten to `minecraft:barrel`;
  - no Currents runtime code, economy, shipping system or custom blocks are copied;
  - jigsaw/structure markers are filtered by Campfire's placement processor.
- Review placement rotates the long dock axis 90° so the pier extends west from the canonical Geming400 shoreline.
- Current status: **production-base candidate packaged for the canonical village review; client visual acceptance pending**.
- Provenance and transformed/source SHA-256 values are emitted into `META-INF/campfiresessions/external_structure_manifest.json`.

### Explicitly not packaged as extracted structure assets
- **Towns & Towers** structures are reference/footprint material only. Its distributed JAR states CC BY-NC-ND 4.0 and disallows modified/repackaged structure extraction.
- **Villageria** remains compact-building reference material only. CurseForge labels the project MIT, but the tested distributed 26.2 NeoForge JAR does not embed the MIT notice; Campfire therefore does not extract/package its structures in the current provenance pipeline.

## Guitar model
- Project: **Musical Instruments Pack** — Tchongas
- Source repository: https://github.com/Tchongas/datapacks
- Pinned source commit: `0ff8ae11584f357129beec8cba145cf67b08f1ba`
- Source model: `1.21/flute/assets/minecraft/models/item/guitar.json`
- License: MIT
- Use here: model geometry/display transforms are retained; textures are remapped to vanilla Minecraft wood textures.

## Chair model
- Project: **Voxelized Furniture** — okil6dev
- Source repository: https://github.com/okil6dev/Voxelized-Furniture
- Pinned source commit: `e83c183de66c1b7114eb6b39a24f58bc87ab9d96`
- Source model: `src/main/resources/assets/voxelized_furniture/models/custom/chair_oak.json`
- License: MIT
- Use here: chair geometry is retained and mapped to vanilla `stripped_oak_log`.

## Music — all CC0
1. **Etirwer (Looped)** — Kistol — https://opengameart.org/content/etirwer
2. **Cozy Puzzle In-Game 3** — MintoDog — https://opengameart.org/content/cozy-puzzle-in-game-3
3. **Neon sign Circuit** — MintoDog — https://opengameart.org/content/neon-sign-circuit
4. **Underwater Ambient Pad** — isaiah658 — https://opengameart.org/content/underwater-ambient-pad
5. **Cozy Puzzle In-Game 1** — MintoDog — https://opengameart.org/content/cozy-puzzle-in-game-1
6. **Cozy Puzzle Title** — MintoDog — https://opengameart.org/content/cozy-puzzle-title
7. **Beach Stage** — MintoDog — https://opengameart.org/content/beach-stage
8. **Space Battle** — MintoDog — https://opengameart.org/content/space-battle
9. **Jazzy Battle Theme** — MintoDog — https://opengameart.org/content/jazzy-battle-theme
10. **Desert Pink and Navy Blue** — Some Weirdo — https://opengameart.org/content/desert-pink-and-navy-blue
11. **Nighttime Solitude** — celestialghost8 — https://opengameart.org/content/nighttime-solitude
12. **Fairy Adventure** — MintoDog — https://opengameart.org/content/fairy-adventure
13. **Other Center** — zesona — https://opengameart.org/content/other-center
14. **Magic Puzzle In-Game 1** — MintoDog — https://opengameart.org/content/magic-puzzle-in-game-1
15. **Urban Boss Battle** — MintoDog — https://opengameart.org/content/urban-boss-battle

The asset preparation script validates each OGG, derives its real duration from Ogg/Vorbis granule positions, and writes `track_metadata.json`.

## Local Custom Music
- No user-provided local audio is committed to this repository or packaged in the mod JAR.
- The client scans `config/campfiresessions/music/` for OGG/Vorbis files on the player's own machine.
- An optional same-name JSON file can define `title`, `artist`, `bpm`, and `theme`.
- Local files are exposed to Minecraft through an always-active generated client resource pack under the same config directory.
- Users are responsible for only adding audio they are allowed to use.

## UI
- UI assets are from the Kenney CC0 asset collection mirrored at https://github.com/shorepine/kenney
- Pinned mirror commit: `3694c6879e487c108f55677be7dd2ca75b07cc3b`
- Five themed sets are packaged: Clean, Neon, Ocean, Desert, Rough.
- The player uses the external nine-slice panels/cards/buttons directly and switches them per track theme.


## Village prefab review status — 2026-10-01

### Kogtyv Towny and Village / Greece structures
License/source status remains valid as already recorded, but the first actual Campfire client review **rejected this structure family as the final main-village civic set**. This is a quality/art-direction rejection, not a licensing rejection. Retain attribution records; do not silently delete provenance because the assets may remain useful as references or secondary-world structures.

### MineColonies schematic repository
Candidate source: `https://github.com/ldtteam/minecolonies-schematics`
- inspected repository license metadata: **GPL-3.0**
- purpose: dedicated MineColonies schematic distribution repository
- candidate use: editable external bases for differentiated Campfire civic/residential exteriors
- no files are accepted into Campfire yet.
- before packaging: pin exact branch/commit/file, inspect NBT block dependencies, verify vanilla/custom-block requirements, record original author/style metadata where available, preserve GPL license/source requirements.

### Brocraft Cobblemon Additions
Candidate repository inspected: `strikeknight57/BCA-Datapack`, master observed at `c50e264e90140c068e5945a1c639ea7e56a6621f`.
- README states MIT.
- repository tree inspection found **no LICENSE file**.
- despite useful centers/shops/paths/decor structure inventory, direct asset intake is blocked pending license resolution.


### Chek's Mint Structures
Source: `Chekmate90/Chek-s-Mint-Structures`
Pinned candidate commit: `6ffadba091c4a07bcdac9f1c89d3d1202f0015b9`
License: **MIT**; the exact pinned source includes `LICENSE`.

Campfire second-village-review intake uses 12 selected Plains Village NBTs as editable external bases. The inspected public NBT inventory shows the selected structures are built from Minecraft block content rather than a custom runtime block namespace. Campfire filters source jigsaw/structure markers during placement.

The source files and license are downloaded from the pinned commit during the generated-resource build, and their SHA-256 values are recorded in `external_structure_manifest.json`.

Status: **INTEGRATED REVIEW CANDIDATE — not final accepted visual art until actual Minecraft client inspection**.


### Chek's Mint Structures — v5 intake expansion
Pinned source and license remain unchanged:
- repository: `Chekmate90/Chek-s-Mint-Structures`
- commit: `6ffadba091c4a07bcdac9f1c89d3d1202f0015b9`
- license: MIT

V5 adds six residential NBTs from the same pinned source:
- `plains_small_house_3.nbt`
- `plains_small_house_4.nbt`
- `plains_small_house_5.nbt`
- `plains_small_house_6.nbt`
- `plains_small_house_7.nbt`
- `plains_small_house_8.nbt`

Current Campfire candidate intake is **18 Chek structures**. Together with the separately licensed Currents of Trade dock, the active external village review package contains 19 structures.

The source remains a review candidate rather than final accepted art until the v5 distributed-layout client review is completed.
