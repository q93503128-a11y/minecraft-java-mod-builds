# CV-A Mossback Boar production visual

- Asset id: `cv_a_mossback_boar`
- TURNBOUND use: Capital Valley first-route common wildlife enemy (CV-A)
- Classification: editable_base / direct_asset
- Integrated: 2026-09-19

## Geometry and texture
- Project: Herbiary
- Author: Avetharun
- Source: https://github.com/avetharun/herbiary
- Files: `boar.geo.json`, `wild_swine.png`
- Geometry blob: `bb9242f63318681a599c95d93a74a654d61584ab`
- Texture blob: `fb58bc441593d4a4c07dbd63adbfa8ba8b24c7c2`
- License: MIT
- Changes: TURNBOUND identifier/visible bounds only; cube topology and texture artwork retained.

## Locomotion and attack motion base
- Project: Photosynthesis
- Author: Martin Floden
- Source: https://github.com/Zuiron/Photosynthesis
- Files: `BlockBenchModels/boar.bbmodel`, `BoarAnimations.java`
- License: MIT
- Changes: idle/walk/attack keyframe language retargeted from the source X-facing boar rig to the final Herbiary Z-facing rig.
- TURNBOUND combat-only hit/death/revive/victory states are authored on this final rig, not placeholders.

## Runtime
- `assets/turnbound/geckolib/models/entity/enemy/cv_a_mossback_boar.geo.json`
- `assets/turnbound/geckolib/animations/entity/enemy/cv_a_mossback_boar.animation.json`
- `assets/turnbound/textures/entity/enemy/cv_a_mossback_boar.png`
