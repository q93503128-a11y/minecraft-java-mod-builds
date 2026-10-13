# 테스트

- Slug: test
- Mod ID: moon_test
- Namespace: moon_test
- Mod version: 0.1.0
- Minecraft: 1.21.1
- Java: 21
- Loader: Fabric
- Loader version: 0.19.5
- Gradle: 8.8 (CI provisioned)
- Build plugin: Fabric Loom 1.7.4
- Final JAR: test-0.1.0.jar
- Existing-world compatibility: Experimental; no persistent schema guarantee
- Required dependencies: Fabric API 0.116.16+1.21.1
- Optional external mods: None
- Forbidden bundled dependencies: Minecraft client/server binaries, third-party assets without redistribution permission
- Datagen task: NOT PROVIDED
- GameTest task: NOT PROVIDED
- Server smoke-test task: NOT PROVIDED
- Client smoke-test task: NOT PROVIDED

## Purpose

`projects/test/` is a reusable Minecraft experiment playground, not a standalone finished game.

Use it for quick but real JAR prototypes such as:
- combat skills
- VFX and sound timing
- experimental items
- movement mechanics
- entity behavior tests
- UI experiments
- rendering tests

Experiments should still compile and remain testable, but they do not need to share a single game theme.

## Current experiment: 별빛 강하

- Right-click the Starfall Focus while aiming at terrain within 64 blocks.
- Overworld only.
- Night only (13000-23000).
- Target must have open sky.
- Presentation is a stellar light strike, not a rocky meteor.
- A distant star flashes, a white/cool-blue light mass accelerates toward the target, then detonates in a wide luminous impact.
- Damage, knockback, light shockwave and terrain crater share the same impact center.
- Cooldown: 12 seconds.
- Server authority: validation, cooldown, damage, knockback and terrain destruction.

## Validation language

Do not claim visual quality from compilation alone.

- CODE REVIEWED
- BUILD VERIFIED
- JAR PRODUCED
- PLAYTESTED
- MULTIPLAYER TESTED

are reported separately.
