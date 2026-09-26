# Third-party assets

All imported assets in this prototype come from sources that explicitly state CC0 / public-domain dedication.

## Guitar
- Voxel Classical Guitar — MonoTone
- Source: https://opengameart.org/content/voxel-classical-guitar
- Download: https://opengameart.org/sites/default/files/voxelclassicalguitar.zip
- License: CC0
- The original MagicaVoxel geometry is downloaded and deterministically converted to triangulated OBJ/MTL at build time.
- The original voxel palette is preserved in a generated palette texture instead of being redrawn.

## Chair
- 3D Lowpoly Chair — Lyricsz
- Source: https://opengameart.org/content/3d-lowpoly-chair
- Download: https://opengameart.org/sites/default/files/chair_cc0.obj
- License: CC0
- Original OBJ geometry is normalized and triangulated for Minecraft. A simple generated wood texture is applied; the geometry itself is not redrawn.

## Music
- Etirwer (Looped) — Kistol
- Source: https://opengameart.org/content/etirwer
- Download: https://lpc.opengameart.org/sites/default/files/Etirwer%20%28Looped%29_0.ogg
- License: CC0
- Streamed OGG used as the first selectable track.

## UI
- UI Pack - Adventure — Kenney
- Publisher: https://kenney.nl/assets/ui-pack-adventure
- Mirror/download page: https://opengameart.org/content/ui-pack-adventure
- Download: https://opengameart.org/sites/default/files/kenney_ui-pack-adventure.zip
- License: CC0
- Used assets: panel_brown.png, button_brown.png, button_grey.png, button_red.png.
- The panel is stored as a GUI nine-slice sprite; buttons are generated from the licensed archive during build and also use nine-slice scaling.

The asset preparation task prints SHA-256 hashes for every downloaded source archive/file during clean builds.
