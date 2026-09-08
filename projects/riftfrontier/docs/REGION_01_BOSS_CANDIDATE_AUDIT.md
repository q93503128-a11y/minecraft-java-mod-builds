# Region 01 Boss Candidate Audit

Status: **SOURCE RIG SELECTED — PRODUCTION CONVERSION / IN-GAME PRESENTATION NOT YET COMPLETE**

Date: 2026-09-09

This audit follows `REGION_01_BOSS_PRESENTATION_GATE.md`. Selection here means the exact CC0 geometry/rig is approved as the production derivation source for the Region 01 first boss. It does **not** mean the original texture/style, source animations, renderer integration, hitbox alignment, VFX/sound, or human combat readability are production-complete.

## Evidence boundary

The original creator-hosted `Dragon_Evolved.gltf` was obtained directly from the Quaternius Ultimate Monsters Google Drive distribution linked by the creator's 2022 release post. The exact downloaded file was inspected locally rather than relying on a mirror parser.

Exact source evidence:

- Google Drive file ID: `1Mcfuavq7F4itG9xhqc-20_IL257ZY2_3`
- file name: `Dragon_Evolved.gltf`
- downloaded size: `991335` bytes
- SHA-256: `39ba6ea24b5f27acf68bbf4c19fe80ba070dbec167ff14bbe933453303426f5c`
- glTF: 2.0, Blender glTF exporter
- meshes: 1
- materials: 1 (`Atlas`)
- skins: 1 (`CharacterArmature`)
- skin joints: 46
- estimated vertices: 4,437
- estimated triangles: 7,440
- local POSITION extent: approximately `5.4752 × 2.8579 × 2.4156`
- animations: 8, all named: `Death`, `Fast_Flying`, `Flying_Idle`, `Headbutt`, `HitReact`, `No`, `Punch`, `Yes`

The earlier public parser reported 4,350 vertices / 7,438 triangles. The direct source-file inspection is now authoritative for Riftfrontier intake; the small count difference is treated as a parser/export accounting difference rather than hidden geometry.

## Exact rig inspection

The source has one root armature and a single skinned mesh. Important hierarchy branches are independently controllable:

- `Root → Torso → Neck → Head`
- `Torso → Shoulder.L → UpperArm.L → LowerArm.L → fingers`
- `Torso → Shoulder.R → UpperArm.R → LowerArm.R → fingers`
- `Torso → Wing1.L → Wing2.L → Wing3.L → Wing4.L`
- `Torso → Wing1.R → Wing2.R → Wing3.R → Wing4.R`
- `Root → Body1 → Body2 → Body3 → Body4`

This gives the model separate head, bilateral forelimb/claw, bilateral wing and body-chain presentation channels instead of one rigid whole-body attack channel.

Source animation sampling also confirms meaningful articulated motion rather than whole-body translation only. In the existing `Punch` clip the right lower arm rotates by roughly 88° and upper arm by roughly 85° relative to its clip-start pose; wing chains also articulate. `Headbutt` contains distinct head/body/wing/finger articulation. Sampled skinning remained finite and bounded across source `Headbutt` and `Punch` poses, with no catastrophic exploded-vertex deformation observed in the deterministic local pose inspection.

The source clips are **reference motion only**. They are not mapped one-to-one to authoritative Riftfrontier attacks.

## Gate assessment — Dragon Evolved

### Readable facing — PASS

The head/horns, wing sweep and body axis produce an obvious front/back direction at combat distance. This was already visible in public preview inspection and is consistent with the actual mesh bounds/hierarchy.

### Attack-bearing mass — PASS

The forelimbs/claws, head/horns, wings and body chain can support physically distinct hit volumes. The source `Punch` and `Headbutt` clips prove those channels are skinned and independently animated.

### Telegraph headroom — PASS FOR PRODUCTION AUTHORING

The rig has enough independent joints to author materially different anticipation, ACTIVE and recovery poses. The project is not constrained to reusing the eight source clips.

### Line/displacement vocabulary — PASS FOR AUTHORING

The body axis, wings and root/body chain can visibly commit toward a lane while the head/forelimbs preserve facing and impact direction. The final line/displacement clip must still be authored against the authoritative `AttackPattern` timing.

### Arena-pressure vocabulary — PASS FOR AUTHORING

The bilateral wings and head/body channels allow an area-pressure telegraph to use a different silhouette from the committed strike. The future danger area must still be communicated with separate shape/placement VFX rather than color alone.

### Minecraft scale/performance — PASS FOR SOURCE SELECTION

~7.4k triangles, one material and one 46-joint skin are acceptable as a source baseline for a single major boss. Final runtime scale, draw-call behavior and surrounding encounter worst case remain to be measured in Minecraft.

### Texture/style fit — SOURCE TEXTURE NOT APPROVED AS FINAL

The original bright rounded low-poly `Atlas` is not accepted unchanged as Region 01 production art. Selection is for the geometry/rig derivation source. A Region 01-specific material/texture treatment must pass the project reference gate before presentation completion.

### License/provenance — PASS

The original Quaternius Ultimate Monsters source page declares CC0 and the creator release post states the pack is free to use in any project, including commercially. The exact source hash above must be preserved in the third-party registry.

## Decision

**`Dragon Evolved` is SELECTED as the Region 01 first-boss geometry/rig derivation source.**

This closes the source-candidate search and unlocks the real conversion/runtime integration spike. It does not approve the original source look as final art and does not declare the boss presentation complete.

Do not reopen the rejected candidate set unless this exact source fails a later renderer/conversion or in-Minecraft quality gate that cannot be fixed without replacing the rig.

## Rejected candidates retained

### Blue Demon — REJECTED

Compact generic biped/club presentation does not provide enough distinct attack-bearing vocabulary for committed strike + displacement + arena pressure without redesigning most of the asset identity.

### Goleling Evolved — REJECTED

Readable flying silhouette, but weaker verified attack-bearing vocabulary for the current commitment/displacement contract than the selected Dragon Evolved rig.

### Mushroom King — REJECTED

Strong recognition silhouette but insufficient attack-bearing limbs for the required first-boss combat language.

## Newer Quaternius Bestiary kit

Still **NOT APPROVED FOR RAW PUBLIC-SOURCE-REPOSITORY BUNDLING** under current QAL evidence. This decision is unchanged and unrelated to the CC0 Dragon Evolved selection.

## Exact next boundary

1. Preserve the exact selected source SHA-256 and provenance whenever the source/derived asset is imported.
2. Re-verify GeckoLib 5.5.1 coordinates for NeoForge 26.2 immediately before adding the dependency.
3. Establish the smallest deterministic conversion path from the selected glTF rig into the chosen Minecraft animation/render format; do not hand-author an unrelated replacement skeleton.
4. Author distinct committed-strike telegraph/ACTIVE/recovery, line/displacement telegraph/ACTIVE/recovery, arena-pressure telegraph/ACTIVE/recovery and phase-transition presentation on the selected rig.
5. Create the first real `presentation_assets` manifest only after physical derived resources exist.
6. Connect `BossPresentationRenderResolver` to the renderer and keep `AttackPattern` as the single authoritative timing source.
7. Validate scale/hitbox alignment, animation deformation, resource reload, VFX/sound timing and worst-case boss encounter performance in Minecraft.
8. Human field-play remains mandatory before M3 presentation completion.
