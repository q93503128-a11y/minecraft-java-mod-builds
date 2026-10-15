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

Why it remains selected after source research:

- It reads as a purpose-built hero object rather than a corridor prop or generic bank vault.
- The silhouette already has enough mechanical layering to hold the center of the cinematic without a code-authored glowing ring.
- The ring, supports, cables and asymmetric secondary structures provide a stronger mechanical hierarchy than every source that was actually rendered during this experiment.
- It already supports animation, which is preferable to manufacturing the primary visual identity from runtime primitives.

Status: **PRIMARY HERO CANDIDATE LOCKED — RAW ASSET INSPECTION BLOCKED ON OFFICIAL DOWNLOAD**

The official Sketchfab download requires an end-user authenticated download path. Do not scrape, reconstruct or bypass that path. The next production step is for the user to download the model from the official page and provide the original archive/file to this project for ordinary Blender inspection and game integration.

Do not call the asset final until that official file has been inspected.

### Artist / AI-use boundary

The artist profile states that the artist does not want their models used for training or developing AI.

Project rule:

- Do not upload the raw model or textures to generative-AI training, fine-tuning or model-generation services.
- Do not use the asset as an input reference for generative 3D reconstruction.
- Ordinary non-generative tooling such as Blender, format conversion, CI file validation and in-game rendering is allowed subject to the model license.
- Preserve required attribution when the asset is used or redistributed.
- At acquisition time, save the exact license text/version and attribution string beside the imported asset.

## Supporting environment — Quaternius only

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

Source:

- https://opengameart.org/content/vault-door
- Author: tbbk
- License: CC0

Verdict: **REJECTED AS HERO**

Useful finding:

- thick circular leaf, central handle and real hinge mechanism gave better physical weight than the Quaternius leaves

Reason for rejection:

- visual identity remained a conventional bank vault
- large hinge arm dominated the form
- combining it with the Quaternius frame still looked like kitbash rather than a purpose-built dimensional device

Keep as mechanical reference only.

### Previs E — OpenGameArt `Eldritch Portal`

Source:

- https://opengameart.org/content/eldritch-portal-model-with-pbr-textures
- Author: phantasmaCora
- License observed during acquisition: CC-BY 4.0
- Public `.blend` was acquired directly and rendered in Blender.

Verdict: **REJECTED AS HERO**

Reason:

- PBR material quality was stronger than the earlier free-kit blockouts
- however, the spikes/tentacle-like silhouette belongs to dark fantasy rather than precision mechanical sci-fi
- rotating or recoloring it would not solve the design-language mismatch

### Blendkit anonymous-download route

Verdict: **REJECTED AS A PRODUCTION SOURCE PATH**

Observed during CI:

- one public API host returned HTTP 403
- the alternative public search endpoint did not resolve the intended exact source reliably

Decision:

- do not spend more production time reverse-engineering or bypassing a provider download flow
- only use a future Blendkit source if its ordinary official download path is directly available and license terms are explicit

### 3DAssets.dev recovery-facility candidates

Sources researched:

- `Twin leaf blast door`
- `Decontamination arch`
- pack: `UFO Crash Site and Recovery Facility`
- license shown by source: CC0 1.0 Universal
- direct permanent CDN URLs are documented by the source

Technical observations from source metadata:

- blast door has separate left/right door nodes and useful mechanical proportions
- decontamination arch has real thickness, nozzle/rail/lamp details and can be useful as secondary chamber dressing
- both are low-poly static assets rather than authored cinematic hero props

CI result:

- API route first failed with HTTP 503 from the source API
- direct documented CDN route then failed on the GitHub runner because `cdn.3dassets.dev` could not resolve DNS
- no successful Blender visual inspection was produced from these files

Verdict: **NOT APPROVED AS HERO; NO FURTHER RETRY**

Even if access succeeds later, treat these as possible support/base geometry only unless a real visual preview proves otherwise. Do not lower the hero quality gate merely because they are CC0.

## Final visual language

The cinematic is still called the `Dimensional Vault`, but the hero device is a dimensional containment gate rather than a literal bank-vault door.

The gate should feel like a machine that seals something behind a dimensional membrane.

Visual hierarchy:

1. Helley hero gate mechanical structure
2. restrained physical locking/containment modules
3. real stage and chamber structure
4. controlled emissive accents
5. dimensional membrane / interior light
6. reward silhouette and reveal

Particles are support only, never the main visual language.

## Hero asset adaptation rules

When the official Helley asset is supplied:

- keep the raw archive/source untouched beside the working copy
- record exact license and attribution information before edits
- inspect the actual hierarchy, materials, textures and animation clips first
- remove or hide unrelated presentation/environment pieces only after confirming they are separable
- preserve authored proportions unless camera/runtime constraints require a change
- separate or re-rig moving groups only where the cinematic needs it
- do not flatten the model into a billboard or replace its identity with screen-space effects
- do not cover it with unrelated kit pieces merely to make it busier
- any Quaternius/custom additions remain subordinate to the hero silhouette

## Highest-rarity event

Preferred highest-rarity branch:

1. gate begins the same normal activation sequence as an upper-rarity result
2. mechanical activation stalls before the membrane stabilizes
3. normal emissive indicators cut out
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
- highest rarity may take over the full gate-breach sequence
- provide a skip path later, but do not design the base animation around skipping

## Rejected approaches

- Quaternius door/frame as the final hero object
- generic bank-vault door as the final hero object
- dark-fantasy `Eldritch Portal` as the final hero object
- lowering the quality bar to a low-detail CC0 door because it is easy to download
- fullscreen UI made primarily from `fill`, rectangles, lines or gradients
- TextDisplay glyphs pretending to be premium VFX
- vanilla particle clouds as the hero visual
- random runtime debris
- a static generated image used as the final summon animation
- copying another game's exact gacha vehicle, wish-star, pod, train or visual identity

## Next production gate

Minecraft v2 implementation is **BLOCKED** until all of the following are complete:

1. User downloads `Portal gate Sci-Fi` from Helley's official Sketchfab page and supplies the original model/archive.
2. Save the exact attribution/license information beside the imported source.
3. Inspect mesh groups, animation clips, textures and material layout in Blender or equivalent non-generative tooling.
4. Produce a real still or short video with the hero gate plus only the minimum required Quaternius supporting environment.
5. Review that preview at the intended Minecraft camera distance.
6. Confirm that the gate itself, without particle spam, already looks strong enough.
7. Only then choose the Minecraft rendering path and implement the cinematic.

If the official raw asset fails the visual or technical gate, replace the hero source. Do not fall back to the rejected Quaternius-only, bank-vault, Eldritch or low-detail door mockups.

## Validation state

- SOURCE/licensing research: REVIEWED
- Real Quaternius mesh previews: RENDERED, REJECTED
- CC0 bank-vault mesh preview: RENDERED, REJECTED AS HERO
- OpenGameArt Eldritch preview: RENDERED, REJECTED AS HERO
- Blendkit route: ACCESS/RELIABILITY REJECTED
- 3DAssets mechanical candidates: SOURCE REVIEWED, CI ACCESS FAILED, NOT VISUALLY APPROVED
- Helley hero candidate: WEB VISUALLY REVIEWED, RAW FILE NOT YET ACQUIRED
- Minecraft v2 implementation: NOT STARTED
- BUILD VERIFIED for current legacy 0.3.0 prototype: YES (latest successful normal build during this research session)
- BUILD VERIFIED for v2 cinematic: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
