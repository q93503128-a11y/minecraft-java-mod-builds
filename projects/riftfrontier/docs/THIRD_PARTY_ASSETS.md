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
- Creator release post: https://www.patreon.com/posts/ultimate-50-73633148
- Source date observed: 2026-09-09
- License / usage note: source page declares CC0; creator release post states the pack is free to use in any project, including commercially.
- Formats advertised: FBX, OBJ, Blend, glTF
- Animated: yes
- Modified: source family not bundled wholesale
- Intended/possible use: Region 01 boss/elite model and rig source family; animation/conversion study
- Current decision: one exact member, `Dragon Evolved`, is now selected as the Region 01 first-boss geometry/rig derivation source. The pack as a whole remains unselected.

#### Dragon Evolved

- Status: `SELECTED`
- Author: Quaternius
- Original family source: https://quaternius.com/packs/ultimatemonsters.html
- Creator release/download source: https://www.patreon.com/posts/ultimate-50-73633148 → Quaternius Google Drive distribution
- Original Drive file ID: `1Mcfuavq7F4itG9xhqc-20_IL257ZY2_3`
- Individual discovery cross-check: https://poly.pizza/m/LlwD0QNUPj
- License: CC0 1.0 / public domain dedication
- Exact source file: `Dragon_Evolved.gltf`
- Exact source size: `991335` bytes
- Exact source SHA-256: `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`
- Format: glTF 2.0
- Direct source inspection: 1 mesh, 1 material (`Atlas`), 1 skin (`CharacterArmature`), 46 joints, 4,437 estimated vertices, 7,440 estimated triangles, bounds approximately 5.4752 × 2.8579 × 2.4156
- Source clips: `Death`, `Fast_Flying`, `Flying_Idle`, `Headbutt`, `HitReact`, `No`, `Punch`, `Yes`
- Rig capability: independent head/neck, bilateral forelimb/finger chains, bilateral four-segment wing chains and four-segment body chain; source `Punch`/`Headbutt` demonstrate articulated non-rigid attack motion.
- Modified: no repository bytes imported yet; exact original source was inspected locally during selection.
- Concrete production use: geometry/rig derivation source for the Region 01 first boss.
- Explicit non-approval: original `Atlas` texture/style is **not** approved unchanged as Region 01 final art; final material/texture treatment, authored attack clips, renderer integration, hitbox alignment, VFX/sound and field-play remain separate production gates.
- Decision record: `REGION_01_BOSS_CANDIDATE_AUDIT.md`.

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
- Rejection: readable flying silhouette, but insufficient verified attack-bearing vocabulary relative to the selected Dragon Evolved rig for the current boss contract.

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
- Current decision: original creator source is authoritative for the selected Dragon Evolved asset.

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

For `Dragon Evolved`, step 1–3 are now satisfied for the geometry/rig derivation source. Physical repository import, derived resource paths and JAR inclusion are still pending and must be recorded when they actually occur.
