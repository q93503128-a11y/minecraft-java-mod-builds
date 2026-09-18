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
- Direct source inspection: 1 mesh, 1 material (`Atlas`), 1 skin (`CharacterArmature`), 46 joints, 4,437 vertices, 7,440 triangles, bounds 5.475212574 × 2.857860476 × 2.415597856 source units.
- Source clips: `Death`, `Fast_Flying`, `Flying_Idle`, `Headbutt`, `HitReact`, `No`, `Punch`, `Yes`
- Rig capability: independent head/neck, bilateral forelimb/finger chains, bilateral four-segment wing chains and four-segment body chain; source animation inventory proves articulated non-rigid motion, but clip names are not accepted as gameplay-semantic proof.
- Verified conversion receipt: `assets/sources/region_01_boss_dragon_evolved.acceptance.json`.
- Verified sanitizer output fingerprint: 681,773-byte canonical glTF, SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`; embedded geometry/skin/animation payload 381,212 bytes; 737 accessors and 737 bufferViews retained.
- Verified art stripping: source image bufferView and primitive material binding plus top-level materials/textures/images/samplers are absent from the sanitized derivation. Source `Atlas` pixels are not accepted as production art.
- Concrete production use: geometry/rig/unaltered-source-animation payload for the Region 01 first boss custom skinned-mesh pipeline. Animation semantics remain separately review-gated.
- Modified: yes — deterministic art-neutral sanitizer `tools/convert_region01_boss_geometry.py` strips all source art/material payload while retaining accepted geometry, skin and animation data.
- Runtime resource ID / Used in: `riftfrontier:boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`.
- Repository runtime path: `src/main/resources/assets/riftfrontier/boss_presentation/region_01/dragon_evolved.sanitized.v1.gltf`.
- Runtime consumers: `Region01BossRuntimeResources.ACCEPTED_GEOMETRY` → exact staged `RiftfrontierClientResources` reload → `Region01BossGeometryPreparation` → prepared-mesh provenance gate in `Region01BossClientRenderRuntime`.
- Runtime integrity: asset-intake tests and executable-JAR inspection both require exact SHA-256 `ff5041de9a0779d11eedcb40256bdaa1ff848efb99c834bdffaadaf20e121cac`.
- Explicit non-approval: original `Atlas` texture/style is **not** approved unchanged as Region 01 final art; final material/texture treatment, logical attack bindings, hitbox alignment, VFX/sound and field-play remain separate production gates.
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

### Region 01 production-creature source study — 2026-09-15

This study exists to replace the current Zombie/Skeleton/Ravager **behaviour proxies** with an evidence-backed art direction without inventing silhouettes from scratch. It does not change encounter authority, spawn counts, rewards, role semantics, or the settled boss selection.

#### Quaternius — Ultimate Monsters / Alien

- Status: `CANDIDATE`
- Author: Quaternius
- Family source: https://quaternius.com/packs/ultimatemonsters.html
- Creator distribution: https://drive.google.com/drive/folders/18m4KpzpEzhC9wl7jzr6dUc0N8Jozr79C
- Creator file ID: `1rWF4Jo_G7-odDa5LfkQ0e2d9_p9pxb3W`
- Individual cross-check: https://poly.pizza/m/sUTLXji0aL
- License: CC0 1.0 / public domain dedication.
- Exact source file: `Big/glTF/Alien.gltf`
- Exact source size: `1295881` bytes
- Exact source SHA-256: `e6fec42f9d4db3c3177da9027c5cdb2c4abd71cb934ea4f4788aea155f267124`
- Direct source inspection: glTF 2.0; 1 mesh, 1 skin, 1 material, 1 embedded PNG image, 45 nodes, 4,227 vertices, 7,676 triangles; source extent approximately 4.6491 × 3.5468 × 1.4879 units.
- Source clips: `Death`, `Duck`, `HitReact`, `Idle`, `Jump`, `Jump_Idle`, `Jump_Land`, `No`, `Punch`, `Run`, `Walk`, `Wave`, `Weapon`, `Yes`.
- Intended use: **visual/rig candidate for the Region 01 Hunter role**, replacing the Zombie silhouette while preserving the already-settled close pursuit behaviour contract.
- Why it fits: non-humanoid hostile silhouette, same already-approved creator/source family as Dragon Evolved, and inspected locomotion/melee/lifecycle clips (`Idle`, `Walk`, `Run`, `Punch`, `HitReact`, `Death`) that cover the current Hunter presentation needs without changing role semantics.
- Concrete review path: exact inspected source is available through the isolated `/summon riftfrontier:region_01_hunter_field_review` actor and the project's skinned-mesh path. This actor is review-only and is not wired into natural Region 01 encounters.
- Canonical evidence: `docs/provenance/region01_hunter_alien_source_inspection.json`, `docs/provenance/region01_hunter_alien_field_review_build.json`, `docs/provenance/region01_hunter_alien_localized_field_review_build.json`.
- Still not approved for production: `selected_for_production = false` and `human_visual_acceptance = false`. Human Minecraft review must establish player-relative scale, facing, ground alignment, UV/texture integrity, near/combat/medium-distance silhouette and deformation/readability of the required clips before this entry can become `SELECTED` or replace the authoritative Hunter proxy.
- Do not redo candidate search, exact-source intake/hash/clip inventory, or field-review plumbing. Do not ship the Poly Pizza preview as source evidence.

#### Quaternius — Ultimate Monsters / Armabee

- Status: `CANDIDATE`
- Author: Quaternius
- Family source: https://quaternius.com/packs/ultimatemonsters.html
- Creator distribution: https://drive.google.com/drive/folders/18m4KpzpEzhC9wl7jzr6dUc0N8Jozr79C
- Creator file ID: `1k7LbRse-00nyMQhTdJMvebPp8B-05hcG`
- Individual cross-check: https://poly.pizza/m/42djT5zJnx
- License: CC0 1.0 / public domain dedication.
- Exact source file: `Flying/glTF/Armabee.gltf`
- Exact source size: `297261` bytes
- Exact source SHA-256: `10ca05955ab7f7f6e2f9bd8fd85f28ed1351c945e390b1fd0fa84428b43a5916`
- Direct source inspection: glTF 2.0; 1 mesh, 1 skin, 1 material, 1 embedded PNG image, 15 nodes, 1,260 vertices, 2,280 triangles; source extent approximately 4.3039 × 1.8862 × 2.0592 units.
- Source clips: `Death`, `Fast_Flying`, `Flying_Idle`, `Headbutt`, `HitReact`, `No`, `Punch`, `Yes`.
- Intended use: **visual/rig candidate for the Region 01 Scout role**, replacing the Skeleton silhouette while preserving the already-settled ranged-pressure role.
- Why it fits: aerial/insectoid silhouette is immediately distinct from the ground Hunter and gives ranged pressure a readable spatial identity without changing the server-authoritative role contract; exact inspection confirms a compact single-skinned-mesh actor with authored idle/fast-flight and combat lifecycle vocabulary.
- Runtime review contract: `Region01ScoutArmabeeRuntimeAsset` locks the exact source hash, geometry counts and animation vocabulary. `FIELD_REVIEW_SEQUENCE` is `Flying_Idle → Fast_Flying → Punch → Headbutt → HitReact → Death`; this is review vocabulary, not permission to change gameplay semantics.
- Canonical evidence: `docs/provenance/region01_scout_armabee_source_inspection.json` plus `Region01ScoutArmabeeRuntimeAssetTest`.
- Important constraint: the exact source has no `Walk` or `Run` clip. Grounded locomotion must not be fabricated or silently mapped to misleading source motion, and the winged silhouette does **not** authorize flight gameplay. Preserve the existing server-authoritative Scout ranged-pressure contract.
- Still not approved for production: `selected_for_production = false` and `human_visual_acceptance = false`. The exact source bytes are not yet vendored as a runtime resource and no isolated Minecraft Scout field-review actor has been build-verified. Before selection, prove root transform/ground alignment, player-relative scale/facing, texture/UV integrity, hitbox and near/combat/medium-distance readability in Minecraft.
- Do not redo Armabee candidate search, creator-source discovery, exact-source hash/geometry/clip inspection or runtime-contract semantics unless evidence regresses them.

#### Quaternius — Goleling Evolved, reconsidered only as elite-role candidate

- Status: `CANDIDATE`
- Author: Quaternius
- Family source: https://quaternius.com/packs/ultimatemonsters.html
- Individual cross-check: https://poly.pizza/m/iHEuXiH6Aj
- License: CC0 / public domain; individual entry is Public Domain (CC0), animated, FBX/GLTF.
- Source date re-verified: 2026-09-15
- Intended use: **visual/rig candidate for the Region 01 Elite Anchor role only**.
- Scope clarification: this does not reverse the earlier `REJECTED` decision for **first-boss use**. Goleling Evolved remains rejected as the Region 01 first boss because it could not carry the boss attack vocabulary. Its compact heavy silhouette may still fit the much narrower elite-anchor role.
- Not yet approved: exact bytes/hash, clip inventory, shield-stun/counterplay readability, scale/hitbox and actual Minecraft render must be proven before selection. The existing Ravager shield-stun gameplay contract must not be deleted merely to fit this model.

#### Quaternius — Cube World Kit

- Status: `RESEARCHED`
- Author: Quaternius
- Source: https://quaternius.com/packs/cubeworldkit.html
- License: CC0
- Source date re-verified: 2026-09-15
- Technical note: creator page advertises 108 models, animated characters/animals/enemies/environment, FBX/OBJ/Blend/glTF, personal/commercial use.
- Possible use: Region 01 environment/prop vocabulary study where its cube-scaled forms adapt cleanly to Minecraft.
- Current decision: do not select or bundle wholesale. Exact pieces require visual reference fit and provenance before use; Region 01's final environment language is not being invented from this pack automatically.

#### Quaternius — Bestiary: Dungeon Monsters Kit (2026)

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
  - https://poly.pizza/m/sUTLXji0aL — Alien
  - https://poly.pizza/m/42djT5zJnx — Armabee
- Source date observed/re-verified: 2026-09-15
- License / usage note: inspected pages identify these entries as Public Domain / CC0.
- Modified: no
- Intended/possible use: discovery and provenance cross-check
- Current decision: original creator source remains authoritative for any selected production intake; mirrors are discovery/cross-check evidence, not an excuse to skip exact-source verification.

## Runtime/library note — not an asset

GeckoLib remains an available code/runtime dependency family rather than an art asset, but it is **not on the Region 01 first-boss critical path**. The current production path uses Riftfrontier's native custom skinned-mesh importer/renderer and consumes the accepted art-neutral glTF resource above directly. Do not add GeckoLib merely to re-express the already-working custom geometry path.

If a later authored asset family actually requires GeckoLib, re-verify its Minecraft 26.2 NeoForge coordinate and resource contract at that commit instead of treating the older research note as a standing dependency decision.

## Bundle rule

Before adding external bytes to `src/main/resources` or any distributable package:

1. set the exact asset entry to `SELECTED`;
2. record original author/source and license terms;
3. prefer an immutable source/revision/hash where practical;
4. record whether and how the file was modified;
5. record exact `Used in` paths/content IDs;
6. verify that redistribution of the actual downloaded file is allowed, not merely use in screenshots or local projects;
7. inspect the final JAR to ensure no unrelated source-pack files were accidentally bundled.

For `Dragon Evolved`, source selection, exact deterministic sanitizer acceptance, concrete runtime resource ID, vendored sanitized bytes and exact JAR-integrity gate are now recorded. Source `Atlas` art remains excluded. Logical animation semantics, final material/texture treatment, final encounter presentation and graphical/field-play review remain separate gates.