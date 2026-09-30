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
