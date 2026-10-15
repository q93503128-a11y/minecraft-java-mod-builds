# Gacha Vault Asset Plan

## Goal

Build the `Dimensional Vault` gacha reveal as an asset-driven cinematic, not code-drawn UI.

Code may control camera, timeline, rarity branching, sound timing, asset visibility and animation playback. It must not invent the primary visual design from rectangles, lines, gradients, TextDisplay glyphs or particle spam.

## Current production decision

### Hero gate — SELECTED SOURCE DIRECTION

Primary hero candidate:

- Asset: `Portal gate Sci-Fi`
- Author: Helley
- Official model page: https://sketchfab.com/3d-models/portal-gate-sci-fi-86f65251573843bcbf74d3857c8dd9d1
- Model page status observed during preproduction: downloadable, animated, game-ready
- Observed geometry: about 32k triangles / 17.6k vertices
- License shown on the model page: Creative Commons Attribution

Why it was selected:

- It already reads as a hero object rather than a corridor prop.
- The silhouette has enough mechanical layering to hold the center of a cinematic without relying on emissive circles or particle density.
- The ring has real structural depth, supports, cabling and asymmetry.
- It is visually stronger than every locally rendered Quaternius-only and generic bank-vault mockup tested in this experiment.
- The asset already supports animation, which is preferable to reconstructing a premium gate from code-authored primitives.

Status: **PRIMARY HERO CANDIDATE LOCKED — RAW ASSET VISUAL APPROVAL STILL PENDING**

Do not call the asset final until the official downloadable file has been acquired and inspected in Blender/another real 3D authoring tool.

### Artist / AI-use boundary

The artist profile states that the artist does not want their models used for training or developing AI.

Project rule:

- Do not upload the raw model or textures to generative-AI training/fine-tuning/model-generation services.
- Do not use the asset as an input reference for generative 3D reconstruction.
- Ordinary non-generative tooling such as Blender, format conversion, CI file validation and in-game rendering is allowed subject to the model license.
- Preserve required attribution when the asset is used or redistributed.
- At acquisition time, save the exact license text/version and attribution string beside the imported asset.

### Supporting environment — Quaternius only

Quaternius `Modular Sci-Fi MegaKit` remains useful, but it is no longer the hero-gate source.

Official source:

- https://quaternius.com/packs/modularscifimegakit.html
- https://quaternius.itch.io/modular-sci-fi-megakit
- License: CC0 1.0 Universal

Allowed supporting roles:

- reveal floor / stage
- side columns
- wall machinery
- secondary clamps
- rails / access platform
- background structural dressing

Useful Standard-pack pieces already inspected:

- `Platform_Round1`
- `Prop_Clamp`
- `Column_Hollow`
- `Column_MetalSupport`
- `Column_MetalSupport_Curve`
- `Column_Pipes`
- `Prop_AccessPoint`
- `Prop_Light_*`
- `Platform_DarkPlates_Curves`
- `Platform_Rails_4Wide*`

Do not let Quaternius geometry replace the visual identity of the hero gate.

## Visual validation already performed

The following previews were rendered in headless Blender from real external meshes. They were deliberately rejected instead of being promoted just because the pipeline succeeded.

### Previs A — Quaternius door + code-authored neon ring

Verdict: **REJECTED**

Problems:

- central glowing torus dominated the silhouette
- read as a generic sci-fi portal template rather than a dimensional vault
- door and clamps disappeared behind the ring
- too much of the visual identity came from code-authored primitive geometry

### Previs B — Quaternius curved structural ring

Verdict: **REJECTED**

Problems:

- real curve meshes improved physicality but the composition still read as a circular UI ornament
- the central plate obscured the actual door
- mechanical hierarchy was weak

### Previs C — Quaternius physical-door emphasis

Verdict: **REJECTED**

Problems:

- the door became readable, but the hero still looked like an assembled free-kit prop
- clamps looked attached rather than structurally integrated
- insufficient authored complexity for a premium gacha focal object

### Previs D — OpenGameArt CC0 bank-vault door

Source inspected:

- https://opengameart.org/content/vault-door
- Author: tbbk
- License: CC0

Verdict: **REJECTED AS HERO**

Useful finding:

- the thick circular leaf, central handle and real hinge mechanism gave better physical weight than the Quaternius leaves

Reason for rejection:

- visual identity remained a conventional bank vault
- large hinge arm dominated the form
- combining it with the Quaternius frame still looked like kitbash rather than a purpose-built dimensional device

It may remain a mechanical-reference source only. Do not ship it as the hero gate.

## Final visual language

The cinematic is still called the `Dimensional Vault`, but the hero device is now a dimensional containment gate rather than a literal bank-vault door.

The gate should feel like a machine that seals something behind a dimensional membrane.

Visual hierarchy:

1. Helley hero gate mechanical ring
2. restrained physical locking/containment modules
3. real stage and chamber structure
4. controlled emissive accents
5. dimensional membrane / interior light
6. reward silhouette and reveal

Particles are support only, never the main visual language.

## Hero asset adaptation rules

When the Helley asset is acquired:

- remove or hide the desert/cactus presentation pieces if they are part of the downloadable scene
- keep the mechanical gate as the hero object
- preserve the original mesh quality and authored proportions unless a clear gameplay/camera reason requires modification
- separate moving mechanical groups only where animation requires it
- do not flatten the model into a single billboard or screen-space effect
- do not cover the model with unrelated Quaternius props just to make it look busier
- keep any new custom geometry subordinate to the original hero silhouette

## Highest-rarity event

The old `door leaf fracture` concept is no longer mandatory because the hero source is a portal/containment gate rather than a pair of vault doors.

Preferred highest-rarity branch:

1. gate begins the same normal activation sequence as an upper-rarity result
2. mechanical activation stalls before the membrane stabilizes
3. all normal emissive indicators cut out
4. 0.2–0.3 s of near-silence
5. an internal impact distorts the membrane from behind
6. a second impact forces several real mechanical segments out of alignment
7. gold-white light leaks through the unstable inner boundary
8. the containment field inverts / tears open
9. one or more authored mechanical pieces move across the camera path
10. backlit reward silhouette becomes visible inside the breach
11. character/reward reveal animation lands
12. result UI appears only afterward

The surprise must come from a different event and motion pattern, not merely a gold recolor.

## Normal / 10-pull pacing

Single high-rarity target duration: roughly 5–7 seconds.

10-pull:

- play the chamber/gate entry once
- reveal low-rarity results rapidly
- pause when a high-rarity result is reached
- highest rarity is allowed to take over the full gate-breach sequence
- provide a skip path later, but do not design the base animation around skipping

## Rejected approaches

- Quaternius door/frame as the final hero object
- generic bank-vault door as the final hero object
- fullscreen UI made primarily from `fill`, rectangles, lines or gradients
- TextDisplay glyphs pretending to be premium VFX
- vanilla particle clouds as the hero visual
- random runtime debris
- a static generated image used as the final summon animation
- copying another game's exact gacha vehicle, wish-star, pod, train or visual identity

## Next production gate

Minecraft implementation is **BLOCKED** until all of the following are complete:

1. Download the Helley asset from the official Sketchfab model page.
2. Record exact attribution/license information beside the imported source.
3. Inspect mesh groups, animation clips, textures and material layout in Blender or equivalent.
4. Produce a real still or short video with the hero gate plus only the minimum required Quaternius supporting environment.
5. Review the preview at the intended Minecraft camera distance.
6. Confirm that the gate itself, without particle spam, already looks strong enough.
7. Only then choose the Minecraft rendering path and implement the cinematic.

If the official raw asset fails the visual or technical gate, replace the hero source. Do not fall back to the rejected Quaternius-only or bank-vault mockups.

## Validation state

- SOURCE/Licensing research: REVIEWED
- Real Quaternius mesh previews: RENDERED, REJECTED
- CC0 bank-vault mesh preview: RENDERED, REJECTED AS HERO
- Helley hero candidate: WEB VISUALLY REVIEWED, RAW FILE NOT YET ACQUIRED
- Minecraft v2 implementation: NOT STARTED
- BUILD VERIFIED for v2 cinematic: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
