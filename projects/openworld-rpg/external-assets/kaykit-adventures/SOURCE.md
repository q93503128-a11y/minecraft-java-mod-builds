# KayKit Adventures — Radiant Lance source binding

Upstream repository: `KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0`

Pinned upstream commit: `672074b73ba276876a19e8816ecdc5241817ab47`

License: CC0 1.0 Universal (upstream `LICENSE.txt`; upstream README also states free personal/commercial use and no attribution requirement).

Imported source identities:

- `arrow.gltf` — Git blob `1bc0ca184be48cf16eed8924c349aea4e189201f`
- `arrow.bin` — Git blob `a02c800802553ffda2494b75c16722f258eaecee`
- `rogue_texture.png` — Git blob `542954baba7281f028f93306943fc780b1ebcf55`
- `LICENSE.txt` — Git blob `877e44735b5869c10e17a59e3b757905aa390626`

Runtime use:

- `arrow.gltf` POSITION/TEXCOORD/NORMAL/index data is converted without topology changes into `RadiantLanceMeshData`;
- only coordinate-system remapping is applied so the source long axis aligns with Spell Engine `ALONG_MOTION`;
- the original `rogue_texture.png` bytes are shipped as `assets/openworld_rpg/textures/spell/radiant_lance_kaykit.png`;
- the renderer applies full-bright illumination and a warm radiant vertex tint; the source mesh remains 73 vertices / 52 triangles.

This binding is for the Cleric Radiant Lance projectile body and does not authorize unrelated KayKit assets automatically.
