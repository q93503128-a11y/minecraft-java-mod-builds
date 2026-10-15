# Gacha Vault Asset Plan

## Goal

Build the `Dimensional Vault` gacha reveal as an asset-driven cinematic, not code-drawn UI.

The code may control camera, timeline, rarity branch, sound timing, asset visibility and animation playback. It must not invent the primary visual design from rectangles, lines, gradients or particle spam.

## Canonical external source

Quaternius — Modular Sci-Fi MegaKit

- Official pack: https://quaternius.com/packs/modularscifimegakit.html
- Official itch page: https://quaternius.itch.io/modular-sci-fi-megakit
- License: CC0 1.0 Universal
- Standard free pack: `Modular SciFi MegaKit[Standard].zip`
- Official page states the free Standard portion is roughly 60–70% of the full pack and is available in FBX / OBJ / glTF.
- The pack is allowed for personal, educational and commercial use under CC0.

A public GitHub mirror (`agentkaerf/FreeModels`) was used only to inspect the exact Standard glTF file inventory and geometry metadata. The original Quaternius pages above remain the source-of-truth for attribution/provenance.

## Adopted Standard-pack parts

### 1. Main vault leaves — `Door_DarkMetal`

Path:
`Modular SciFi MegaKit[Standard]/glTF/Platforms/Door_DarkMetal.gltf`

Observed geometry bounds from the glTF accessor:

- X: `-2.1057 .. 0.0`
- Y: `0.0 .. 4.0489`
- Z: `-0.1016 .. 0.1016`

Interpretation:

- The mesh is effectively a one-sided half-door panel rather than a complete centered door.
- This is useful for a mirrored left/right vault-leaf composition.
- Use two instances or an edited duplicate, not a stretched single quad.
- Keep each leaf as an independent animation part so the normal reveal can slide/open while the highest-rarity branch can break or be displaced separately.

Status: **ADOPT AS BASE**

### 2. Main frame — `Door_Frame_SquareTall`

Path:
`Modular SciFi MegaKit[Standard]/glTF/Platforms/Door_Frame_SquareTall.gltf`

Observed geometry bounds:

- X: `-2.4219 .. 2.4219`
- Y: about `0 .. 5.0`
- Z: `-0.2534 .. 0.2534`

Interpretation:

- Approximate visible frame width is 4.84 units and height 5 units.
- This proportion matches a central monumental vault opening better than the smaller ordinary-door frame.
- It should be scaled up as a hero prop and surrounded by additional supports, not presented as a normal corridor doorway.

Status: **ADOPT AS MAIN FRAME**

### 3. Lock clamps — `Prop_Clamp`

Path:
`Modular SciFi MegaKit[Standard]/glTF/Props/Prop_Clamp.gltf`

Observed geometry is compact (roughly sub-1-unit footprint) and uses the same trim material family as the pack.

Use:

- Four copies around the vault frame.
- Each clamp gets a real transform animation: retract / rotate / release.
- Timing should be sequential rather than simultaneous.
- Do not represent locks as UI icons or colored rectangles.

Status: **ADOPT AS LOCK BASE**

### 4. Central floor stage — `Platform_Round1`

Path:
`Modular SciFi MegaKit[Standard]/glTF/Platforms/Platform_Round1.gltf`

Observed outer bounds:

- X/Z: about `-2.9954 .. 2.9954`
- Overall diameter: about 6 units
- Thin vertical profile, suitable as a circular reveal stage.

Use:

- Main circular floor directly in front of the vault.
- Camera entry path should cross this stage before reaching the door.
- Inner circular material region can be repurposed as the reveal focal zone.

Status: **ADOPT AS HERO FLOOR**

### 5. Ring/support candidate — `Column_Hollow`

Path:
`Modular SciFi MegaKit[Standard]/glTF/Columns/Column_Hollow.gltf`

Observed bounds:

- X/Z: about `-0.6047 .. 0.6047`
- Y: `0 .. 5.0`

This is a tall hollow column rather than a ready-made horizontal ring.

Use:

- Good candidate for side machinery / tall energy conduits / framing supports.
- Do **not** force it into the central rotating ring role.

Status: **ADOPT FOR SIDE SUPPORTS, NOT MAIN RING**

## Additional Standard-pack candidates seen in the real inventory

Keep available for scene dressing and mechanical layering:

- `Column_Astra`
- `Column_MetalSupport`
- `Column_MetalSupport_Curve`
- `Column_Pipes`
- `Column_Round`
- `Prop_AccessPoint`
- `Prop_Fan_Small`
- `Prop_ItemHolder`
- `Prop_Light_Corner`
- `Prop_Light_Floor`
- `Prop_Light_Small`
- `Prop_Light_Wide`
- `Prop_PipeHolder`
- `Platform_CenterPlate_Curve`
- `Platform_DarkPlates_Curves`
- `Platform_Metal2_Curve`
- `Platform_Rails_4Wide`
- `Platform_Rails_4WideTall`

These are support assets only. Do not clutter the reveal chamber merely because the pack contains them.

## Main rotating seal/ring

The Standard pack does not expose an obvious ready-made hero ring that should simply be dropped in as-is.

Decision:

- Build the central seal/ring as a lightweight custom assembly using the pack's visual language.
- Reuse curves, trim materials and supports from the CC0 pack where useful.
- Keep the ring split into independently animatable pieces.
- The ring must visibly rotate/lock, not just change color.

Preferred structure:

1. Outer fixed frame
2. Large rotating ring
3. Smaller counter-rotating inner ring
4. Central seam / seal plate
5. Four clamp interfaces

## Highest-rarity fracture

Do not generate fake fracture pieces procedurally at runtime.

Create a prepared fracture variant of the two vault leaves:

- 5–8 major pieces total
- Each piece has a designed exit vector and spin
- One large foreground fragment may cross the camera path
- The normal door mesh is hidden at the fracture frame
- Fractured pieces then play a deterministic authored animation

This must look intentional and repeatable, not like physics debris spam.

## Materials and emission

The Quaternius Standard glTF files use shared trim texture families such as:

- `T_Trim_01_BaseColor_Red.png`
- `T_Trim_01_Normal.png`
- `T_Trim_01_ORM.png`
- `T_Trim_02_BaseColor_Red.png`
- `T_Trim_02_Normal.png`
- `T_Trim_02_ORM.png`

Rules:

- Preserve the underlying material design language.
- Recolor/emissive work should be authored as real texture/material variants, not screen-space rectangles.
- Normal / upper-rarity anticipation may use cool white / violet accents.
- Highest rarity initially shares the upper-rarity violet signal, then all emission cuts out briefly before gold-white fracture light appears from inside.

## Cinematic sequence target

Target duration: roughly 6–7 seconds for the highest-rarity branch.

1. Summon starts in the current world.
2. Camera transitions into the dimensional chamber.
3. Round stage and vault come into view.
4. Large seal ring begins rotating.
5. Four clamps release sequentially.
6. Normal branch: vault leaves open normally.
7. Highest-rarity branch: leaves attempt to open but stop.
8. Brief silence.
9. First internal impact.
10. Second impact + visible seam/fracture cue.
11. Violet emission cuts out.
12. Gold-white light leaks from the seam.
13. Third impact.
14. Authored fracture pieces burst outward.
15. Backlit silhouette appears.
16. Character/reward reveal animation.
17. Minimal result UI appears only after the cinematic reveal has already landed.

## Rejected approaches

- Fullscreen UI made primarily from `fill`, rectangles, lines or gradients
- TextDisplay glyphs pretending to be premium VFX
- Vanilla particle clouds as the hero visual
- Random runtime debris for the vault fracture
- A static generated image used as the final summon animation
- Copying another game's train, tape, wish-star, pod or exact visual identity

## Next production gate

Before implementing the Minecraft cinematic again:

1. Acquire the actual Quaternius Standard asset files from the official free distribution.
2. Assemble a vault mockup from the adopted parts in a real 3D authoring tool.
3. Produce a still or short preview showing the real geometry and camera framing.
4. Review that preview visually.
5. Only after the scene passes the visual quality gate, implement it in Minecraft.

Do not return to code-first visual design if the asset mockup is not yet convincing.
