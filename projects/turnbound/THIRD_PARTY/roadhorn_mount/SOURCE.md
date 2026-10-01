# Roadhorn Mount production asset

- Asset id: `roadhorn_mount`
- TURNBOUND role: first physical-waystation rental mount
- Classification: editable_base / direct_asset
- Integrated: 2026-10-01

## Upstream
- Project: Tolkien Tweaks - Mobs Edition
- Repository: https://github.com/GreatOrator/TolkienTweaks-Mobs-Edition
- Immutable upstream commit: `2e3b65a4cbe6cdd5ececdfdbf9afb658ccb805d7`
- License: MIT
- Repository LICENSE applies; no asset-path license override was found in the already-audited source tree.

## Exact upstream files
- geometry: `src/main/resources/assets/tolkienmobs/geo/passive/goat.geo.json`
- animation: `src/main/resources/assets/tolkienmobs/animations/passive/goat.animation.json`
- texture: `src/main/resources/assets/tolkienmobs/textures/entity/goat/goat1.png`

## TURNBOUND mount adaptation
- unlike the Cavehorn Elite adaptation, the original chest and Saddle/Saddle2..Saddle7 geometry is retained;
- geometry identifier is normalized for the dedicated mount entity;
- upstream idle/walk motion is retained;
- gallop is a TURNBOUND-authored faster locomotion pass derived from the upstream walk leg language with stronger stride and body lift;
- jump is a TURNBOUND-authored mounted-air pose;
- the already imported unchanged upstream goat texture is reused from the existing TURNBOUND elite texture path, avoiding a duplicate binary;
- this is a separate entity/renderer from the hostile Cavehorn Elite and is never used as an enemy shell.

## Runtime destinations
- `assets/turnbound/geckolib/models/entity/mount/roadhorn_mount.geo.json`
- `assets/turnbound/geckolib/animations/entity/mount/roadhorn_mount.animation.json`
- texture reuse: `assets/turnbound/textures/entity/elite/elite_cv_cavehorn_ravager.png`
