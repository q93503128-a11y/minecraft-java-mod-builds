# Third-party assets

This prototype vendors Minecraft-native models from open-source Minecraft projects instead of converting generic 3D marketplace models.

## Guitar model
- Project: **Musical Instruments Pack** — Tchongas
- Modrinth: https://modrinth.com/datapack/musical-instruments-pack
- Source repository: https://github.com/Tchongas/datapacks
- Pinned source commit: `0ff8ae11584f357129beec8cba145cf67b08f1ba`
- Source model: `1.21/flute/assets/minecraft/models/item/guitar.json`
- License: MIT
- Use here: Blockbench/Minecraft model geometry and display transforms are retained. Texture slots are remapped to vanilla Minecraft oak/spruce/dark-oak textures; source texture PNGs are not redistributed.

## Chair model
- Project: **Voxelized Furniture** — okil6dev
- Modrinth: https://modrinth.com/mod/voxelized-furniture
- Source repository: https://github.com/okil6dev/Voxelized-Furniture
- Pinned source commit: `e83c183de66c1b7114eb6b39a24f58bc87ab9d96`
- Source model: `src/main/resources/assets/voxelized_furniture/models/custom/chair_oak.json`
- License: MIT
- Use here: model geometry is retained and mapped to vanilla `stripped_oak_log` texture. Blockstate rotation and collision were adapted for Campfire Sessions.

## Music
- Etirwer (Looped) — Kistol — CC0
  - https://lpc.opengameart.org/content/etirwer
- Cozy Puzzle In-Game 3 — MintoDog — CC0
  - https://opengameart.org/content/cozy-puzzle-in-game-3
- Neon sign Circuit — MintoDog — CC0
  - https://opengameart.org/content/neon-sign-circuit
- Underwater Ambient Pad — isaiah658 — CC0
  - https://opengameart.org/content/underwater-ambient-pad

The music preparation task logs SHA-256 hashes for each downloaded OGG.
