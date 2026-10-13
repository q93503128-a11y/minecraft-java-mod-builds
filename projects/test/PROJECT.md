# 테스트

- Slug: test
- Mod ID: moon_test
- Namespace: moon_test
- Mod version: 0.2.0
- Minecraft: 26.3
- Java: 25
- Loader: Fabric
- Loader version: 0.19.5
- Gradle: 9.6.0 (CI provisioned)
- Build plugin: Fabric Loom 1.17.20
- Final JAR: test-0.2.0.jar
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

## Current experiment: 별빛 강하 0.2

- Starfall Focus is granted automatically when the player joins if they do not already carry one.
- Right-click while aiming at exposed terrain within 96 blocks to cast.
- Overworld and night only for the actual cast.
- Sneak + right-click opens the test night immediately, so normal testing does not require `/time set night`.
- No cooldown; this is an experiment playground.
- 0.5 seconds after casting, a distant star-like light appears roughly 150+ blocks away along the sky path.
- The light hangs and pulses briefly before beginning its descent.
- The descent starts very slowly, then continuously accelerates toward the target over several seconds.
- The visible meteor is built from Display Entities: one bright core plus a small number of delayed luminous afterimages. It is not a cloud of particles and has no rocky meteor body.
- On impact, the core blooms and a coherent ring of light expands from the exact damage center.
- Damage, knockback and terrain crater share the same impact center.
- Server authority: validation, damage, knockback and terrain destruction.

## Visual rule for this experiment

Do not increase quality by simply increasing particle count. The meteor must remain readable as one moving celestial light with a clear distant origin, held anticipation, long descent, accelerating streak and decisive impact.

## Validation language

Do not claim visual quality from compilation alone.

- CODE REVIEWED
- BUILD VERIFIED
- JAR PRODUCED
- PLAYTESTED
- MULTIPLAYER TESTED

are reported separately.
