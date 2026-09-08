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
- License / usage note: source page declares CC0; personal and commercial use allowed.
- Formats advertised: FBX, OBJ, Blend, glTF
- Animated: yes
- Modified: no repository asset imported yet
- Intended/possible use: Region 01 boss/elite model and rig candidate source; animation/conversion study
- Current decision: bounded candidate audit completed in `REGION_01_BOSS_CANDIDATE_AUDIT.md`. No production asset selected.

#### Dragon Evolved

- Status: `CANDIDATE`
- Author: Quaternius
- Original family source: https://quaternius.com/packs/ultimatemonsters.html
- Individual discovery source: https://poly.pizza/m/LlwD0QNUPj
- License: CC0 / public domain
- Formats observed: FBX / GLTF within the published family
- Technical evidence observed: 4,350 vertices, 7,438 triangles, 8 animations, 70 nodes, reported bounds 5.48 × 2.86 × 2.42 from a public GLB parser
- Immutable cross-check: public mirror commit `371f69f03e295c509faa65f5a4bd20ad32834b8f`, GLB blob SHA `114c2311759fd037d01ffd74c1ca4a1199f874eb`, 436,000 bytes
- Modified: no
- Intended use: direct binary rig/animation inspection for Region 01 first boss
- Current decision: conditional technical front-runner only. Exact rig, clip names, materials, deformation, Minecraft scale and style adaptation are not yet verified; **NOT SELECTED**.

#### Blue Demon

- Status: `REJECTED`
- Source: https://poly.pizza/m/S7jYW6Amye
- License: CC0 / public domain
- Intended use considered: Region 01 first boss
- Rejection: compact generic biped/club presentation does not provide enough distinct attack-bearing vocabulary for committed strike + displacement + arena pressure without redesigning most of the asset identity.

#### Goleling Evolved

- Status: `REJECTED`
- Source: https://poly.pizza/m/iHEuXiH6Aj
- License: CC0 / public domain
- Intended use considered: Region 01 first boss
- Rejection: readable flying silhouette, but insufficient verified attack-bearing mass/rig vocabulary for the current grounded commitment/displacement presentation contract.

#### Mushroom King

- Status: `REJECTED`
- Source: https://poly.pizza/m/grnFTziU8u
- License: CC0 / public domain
- Intended use considered: Region 01 first boss
- Rejection: recognizable cap silhouette but small attack-bearing limbs; would require presentation to be invented around the asset rather than supported by it.

### Quaternius — Bestiary: Dungeon Monsters Kit (2026)

- Status: `RESEARCHED`
- Author: Quaternius
- Sources:
  - https://quaternius.com/packs/bestiarydungeonmonsterskit.html
  - https://quaternius.itch.io/bestiary-dungeon-monsters-kit
- License: QAL v1.0, https://quaternius.com/license.html
- Source date observed: 2026-09-09
- Technical note: 7 monsters, 3 color variants each, optimized low-poly topology, humanoid retargetable rig; standard kit contains no animations.
- Repository distribution decision: **NOT APPROVED FOR RAW PUBLIC-SOURCE-REPOSITORY BUNDLING**. QAL allows incorporation into a completed Product but prohibits redistribution of the Assets themselves as standalone assets. Because this public source repository exposes raw resource files, do not commit QAL source/model/texture bytes without explicit clarification or permission.
- Current decision: may remain a design/rig study source; not a source-tree production asset source under the current evidence.

### Poly Pizza — Quaternius individual CC0 mirrors

- Status: `RESEARCHED`
- Author: Quaternius (individual mirrored entries inspected)
- Sources inspected:
  - https://poly.pizza/m/S7jYW6Amye — Blue Demon
  - https://poly.pizza/m/grnFTziU8u — Mushroom King
  - https://poly.pizza/m/LlwD0QNUPj — Dragon Evolved
  - https://poly.pizza/m/iHEuXiH6Aj — Goleling Evolved
- Source date observed: 2026-09-09
- License / usage note: inspected pages identify the entries as Public Domain / CC0.
- Modified: no
- Intended/possible use: discovery and provenance cross-check
- Current decision: prefer the original creator/source record when a production asset is eventually bundled.

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
