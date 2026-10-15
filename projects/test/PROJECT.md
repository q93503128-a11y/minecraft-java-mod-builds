# 테스트

- Slug: test
- Mod ID: moon_test
- Namespace: moon_test
- Mod version: 0.3.0
- Minecraft: 26.3
- Java: 25
- Loader: Fabric
- Loader version: 0.19.5
- Gradle: 9.6.0 (CI provisioned)
- Build plugin: Fabric Loom 1.17.20
- Final JAR: test-0.3.0.jar
- Existing-world compatibility: Experimental; no persistent schema guarantee
- Required dependencies: Fabric API 0.162.0+26.3
- Optional external mods: None
- Forbidden bundled dependencies: Minecraft client/server binaries, third-party assets without redistribution permission
- Datagen task: NOT PROVIDED
- GameTest task: NOT PROVIDED
- Server smoke-test task: NOT PROVIDED
- Client smoke-test task: NOT PROVIDED

## Purpose

`projects/test/` is a reusable Minecraft experiment playground, not a standalone finished game.

Use it for quick but real JAR prototypes such as combat skills, VFX/sound timing, experimental items, movement mechanics, entity behavior, UI and rendering tests.

Experiments should still compile and remain testable, but they do not need to share a single game theme.

## Current experiment: 차원 보관고 가챠 연출 v2 — asset preproduction

This experiment tests presentation only. It does not implement currency, probabilities, banners, inventory rewards or character ownership.

### Rejected prototype boundary

The existing `0.3.0` full-screen code-drawn gacha cinematic is a rejected visual prototype.

It was useful only to validate timing/key-input plumbing. Its rectangles, lines, gradients, procedural shards and code-authored vault silhouette are **not** the visual baseline for v2 and must not be polished or extended into the final presentation.

Do not treat build success of that prototype as evidence of visual quality.

### v2 direction

The v2 cinematic is asset-driven:

- a real 3D hero gate with authored mechanical detail
- authored model animation for gate/containment movement
- real stage/chamber geometry
- texture/material-based emissive lighting
- dedicated VFX textures or shaders only where they materially improve the scene
- code limited primarily to camera, timeline, rarity branching, sound synchronization and asset visibility/playback

The reveal concept is a **dimensional containment gate**, not a literal bank-vault door.

Target structure:

1. enter the dimensional chamber
2. approach the containment gate
3. mechanical activation begins
4. normal/rare branch stabilizes and opens the dimensional membrane normally
5. highest-rarity branch begins the same way but stalls
6. brief silence
7. internal impacts deform the membrane / disturb real mechanical parts
8. normal emissive signal cuts out
9. gold-white light leaks from inside
10. containment field tears or inverts
11. authored mechanical movement crosses the camera path
12. backlit reward/character silhouette appears
13. reveal motion lands
14. minimal result UI appears afterward

### Asset canon

See `GACHA_VAULT_ASSET_PLAN.md` before further implementation.

Primary hero source direction:

- Helley — `Portal gate Sci-Fi`
- Official source: Sketchfab model `86f65251573843bcbf74d3857c8dd9d1`
- License shown on the model page: Creative Commons Attribution
- Status: visual hero candidate locked; official raw download must be supplied by the user and then pass real 3D inspection

Secondary environment source:

- Quaternius `Modular Sci-Fi MegaKit`
- License: CC0 1.0 Universal
- Role: stage, side supports, rails, background machinery and restrained secondary dressing only

Important preproduction results:

- Quaternius-only hero mockups: rendered and rejected
- CC0 bank-vault hero and Quaternius composite: rendered and rejected
- OpenGameArt `Eldritch Portal`: public PBR `.blend` rendered and rejected because its dark-fantasy design language conflicts with the target
- Blendkit anonymous acquisition route: rejected because public access/search was unreliable
- 3DAssets.dev recovery-facility door/arch: source metadata reviewed; CI API/CDN access failed and no visual approval was granted
- Do not return to any rejected composition or lower the hero quality gate merely because a source is easier to download

### Implementation gate

Minecraft v2 implementation must not begin until:

1. the user downloads Helley's model through the official Sketchfab flow and provides the original file/archive
2. exact attribution/license information is recorded
3. raw mesh/animation/materials are inspected using ordinary non-generative 3D tooling
4. a real preview is produced with minimal supporting environment
5. that preview passes visual review at the intended camera distance

If the raw asset fails, replace the hero source instead of filling the scene with more particles or generic kit pieces.

## Validation language

Do not claim visual quality from compilation alone.

- CODE REVIEWED
- BUILD VERIFIED
- JAR PRODUCED
- PLAYTESTED
- MULTIPLAYER TESTED

are reported separately.

Current v2 state:

- SOURCE/licensing research: REVIEWED
- Rejected real-mesh previews: COMPLETED
- Current legacy 0.3.0 prototype BUILD VERIFIED: YES
- Final hero raw-asset inspection: BLOCKED ON OFFICIAL Helley DOWNLOAD
- Minecraft v2 implementation: NOT STARTED
- v2 BUILD VERIFIED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
