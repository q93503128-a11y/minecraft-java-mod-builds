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

- real 3D vault / frame / stage / lock assets
- authored model animation for locks and door movement
- authored fracture pieces for the highest-rarity branch
- texture/material-based emissive lighting
- dedicated VFX textures or shaders only where they materially improve the scene
- code limited primarily to camera, timeline, rarity branching, sound synchronization and asset visibility/playback

The target reveal structure remains:

1. enter the dimensional chamber
2. approach the vault
3. rotate the seal mechanism
4. release four locks sequentially
5. normal/rare branch opens normally
6. highest-rarity branch attempts normal opening and fails
7. brief silence and internal impacts
8. violet signal cuts out
9. gold-white light leaks from the seam
10. authored vault fracture bursts outward
11. backlit silhouette appears
12. reward/character reveal motion
13. minimal result UI appears after the reveal lands

### Asset canon

See `GACHA_VAULT_ASSET_PLAN.md` before further implementation.

Primary external base currently selected for preproduction:

- Quaternius `Modular Sci-Fi MegaKit` Standard pack
- License: CC0 1.0 Universal
- Main candidate pieces: `Door_DarkMetal`, `Door_Frame_SquareTall`, `Prop_Clamp`, `Platform_Round1`, `Column_Hollow` plus supporting structural pieces

Do not return to code-first visual design if the real-asset mockup is not convincing.

## Validation language

Do not claim visual quality from compilation alone.

- CODE REVIEWED
- BUILD VERIFIED
- JAR PRODUCED
- PLAYTESTED
- MULTIPLAYER TESTED

are reported separately.
