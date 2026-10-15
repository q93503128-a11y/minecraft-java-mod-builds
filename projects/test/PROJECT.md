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

## Current experiment: 차원 보관고 가챠 연출

This experiment tests presentation only. It does not implement currency, probabilities, banners, inventory rewards or character ownership.

- Press `G` to play the cinematic.
- Successive uses cycle `normal -> rare -> legendary -> normal` so every presentation state can be reviewed without commands or RNG.
- The sequence is built as a full-screen cinematic using Minecraft's 26.3 `GuiGraphicsExtractor` render-state path.
- Presentation beats: dark vault approach -> sequential lock release -> rarity light hint -> gate opening -> transition -> silhouette scan -> rarity reveal.
- Normal and rare results open the vault through the expected mechanism.
- Legendary initially presents a violet hint, then the normal opening fails. Pressure builds, the hint flips to gold, and the gate is blown outward from inside before the reveal.
- The world is not paused while the cinematic is visible.
- Escape closes the cinematic at any point.

### Visual rules

- Do not substitute particle quantity for composition.
- Do not use Minecraft's default menu/button stack as the cinematic language.
- The reveal must have an anticipation beat before the rarity confirmation.
- Legendary must differ through event choreography, not only a color swap.
- The current humanoid is deliberately an anonymous silhouette; no permanent character design is claimed by this experiment.

## Validation language

Do not claim visual quality from compilation alone.

- CODE REVIEWED
- BUILD VERIFIED
- JAR PRODUCED
- PLAYTESTED
- MULTIPLAYER TESTED

are reported separately.
