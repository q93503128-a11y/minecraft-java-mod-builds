# Cavehorn Ravager production asset

- Asset id: `elite_cv_cavehorn_ravager`
- TURNBOUND role: Capital Valley Warning Cave optional Elite
- Classification: editable_base / direct_asset
- Integrated: 2026-09-19

## Upstream
- Project: Tolkien Tweaks - Mobs Edition
- Repository: https://github.com/GreatOrator/TolkienTweaks-Mobs-Edition
- Immutable upstream commit: `2e3b65a4cbe6cdd5ececdfdbf9afb658ccb805d7`
- License: MIT
- License scope check: no asset-path override or separate resource license found in the inspected repository tree; repository LICENSE is used.
- NOTICE: no NOTICE/CREDITS/ATTRIBUTION file found in the inspected tree.

## Exact upstream files
- geometry: `src/main/resources/assets/tolkienmobs/geo/passive/goat.geo.json`
  - blob: `fb7b74c4db1e85e82e3c1f9e5ed4e2dd5c3e5cf5`
- animation: `src/main/resources/assets/tolkienmobs/animations/passive/goat.animation.json`
  - blob: `ca4120da79d25aae017f34cdff1ab62449fb32ed`
- texture: `src/main/resources/assets/tolkienmobs/textures/entity/goat/goat1.png`
  - blob: `44a1e6aaa4dff677fd3267fdf30e4fdd07cea680`
- license: `LICENSE`
  - blob: `ce9bbf9ee79a6964a1da29f2cd19579ccb024968`

## Provenance
No upstream declaration was found indicating that these specific goat files came from another asset pack. The immediate source and credited repository owner are therefore recorded as the current provenance root. If upstream provenance is later clarified, this note must be extended rather than overwritten.

## TURNBOUND modifications
- geometry identifier and visible bounds normalized;
- mount-only `chest` and `Saddle*` bones removed for the wild elite silhouette;
- horn, head, hair, body, tail and articulated leg geometry retained from upstream;
- upstream idle and walk clips are reused as locomotion bases;
- TURNBOUND combat-ready, gore, charge, stomp, hit, death, revive and victory clips are authored on the same final rig;
- texture is imported unchanged;
- no vanilla entity shell or temporary placeholder is used.

## Runtime destinations
- `assets/turnbound/geckolib/models/entity/elite/elite_cv_cavehorn_ravager.geo.json`
- `assets/turnbound/geckolib/animations/entity/elite/elite_cv_cavehorn_ravager.animation.json`
- `assets/turnbound/textures/entity/elite/elite_cv_cavehorn_ravager.png`
