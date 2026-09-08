# Riftfrontier Third-Party Assets Registry

This file records external assets and source families considered or used by Riftfrontier. An entry in this file is **not** permission to bundle an asset unless its status is `SELECTED` and the repository actually contains only files allowed by the stated license.

## Status values

- `RESEARCHED` — source/license inspected; nothing selected for production.
- `CANDIDATE` — legally eligible and technically plausible, but visual/quality gate not passed.
- `SELECTED` — approved for a concrete production use and safe to bundle under the recorded terms.
- `REJECTED` — inspected and intentionally not used.

## Registry

### Quaternius — Ultimate Monsters

- Status: `CANDIDATE`
- Author: Quaternius
- Source: https://quaternius.com/packs/ultimatemonsters.html
- Source date observed: 2026-09-09
- License / usage note: page declares CC0; personal and commercial use allowed.
- Formats advertised: FBX, OBJ, Blend, glTF
- Animated: yes
- Modified: no repository asset imported yet
- Intended/possible use: Region 01 boss/elite model and rig candidate source; animation/conversion study
- Current decision: no individual monster selected. Must pass `REGION_01_BOSS_PRESENTATION_GATE.md` silhouette, Minecraft-fit, rig, timing and performance review before any file is bundled.

### Poly Pizza — Quaternius individual CC0 mirrors

- Status: `RESEARCHED`
- Author: Quaternius (individual mirrored entries inspected)
- Sources inspected:
  - https://poly.pizza/m/S7jYW6Amye — Blue Demon
  - https://poly.pizza/m/grnFTziU8u — Mushroom King
- Source date observed: 2026-09-09
- License / usage note: inspected pages identify the entries as Public Domain / CC0.
- Modified: no
- Intended/possible use: discovery and provenance cross-check only
- Current decision: not selected. Prefer the original creator/source record when a production asset is eventually bundled.

## Runtime/library note — not an asset

GeckoLib is a code/runtime dependency candidate, not a third-party art asset. The 2026-09-09 GeckoLib 5 support table lists Minecraft 26.2 with GeckoLib 5.5.1. Dependency coordinates and NeoForge compatibility must be re-verified at the moment the dependency is actually added.

Source: https://wiki.geckolib.com/docs/geckolib5/

## Bundle rule

Before adding external bytes to `src/main/resources` or any distributable package:

1. set the exact asset entry to `SELECTED`;
2. record original author/source and license terms;
3. prefer an immutable source/revision/hash where practical;
4. record whether and how the file was modified;
5. record exact `Used in` paths/content IDs;
6. verify that redistribution of the actual downloaded file is allowed, not merely use in screenshots or local projects;
7. inspect the final JAR to ensure no unrelated source-pack files were accidentally bundled.
