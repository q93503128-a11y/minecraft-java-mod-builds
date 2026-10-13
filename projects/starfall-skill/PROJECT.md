# Starfall Skill

- Slug: starfall-skill
- Mod ID: starfall_skill
- Namespace: starfall_skill
- Mod version: 0.1.0
- Minecraft: 1.21.1
- Java: 21
- Loader: Fabric
- Loader version: 0.19.5
- Gradle: 8.8 (CI provisioned)
- Build plugin: Fabric Loom 1.7.4
- Final JAR: starfall-skill-0.1.0.jar
- Existing-world compatibility: Additive; no persistent world schema
- Required dependencies: Fabric API 0.116.16+1.21.1
- Optional external mods: None
- Forbidden bundled dependencies: Minecraft client/server binaries, third-party assets without redistribution permission
- Datagen task: NOT PROVIDED
- GameTest task: NOT PROVIDED
- Server smoke-test task: NOT PROVIDED
- Client smoke-test task: NOT PROVIDED

## Skill contract

`별빛 강하` is a standalone spectacle skill prototype.

- Activation: right-click the Starfall Focus while aiming at a block within 64 blocks.
- Dimension: Overworld only.
- Time: night only (13000-23000).
- Environment: target must have open sky.
- Presentation: a single star flashes high above the target, then a white/cool-blue light mass accelerates down a curved-looking trail. It is intentionally light-like, not a rocky meteor.
- Impact: visible telegraph radius, damage radius, knockback, expanding light shockwave, and a small terrain crater are aligned around the same center.
- Cooldown: 12 seconds.
- Server authority: target validation, cooldown, damage, knockback, and terrain destruction are server-side.

## Quality limits

This prototype deliberately avoids custom character/weapon art and UI. Visual quality is concentrated into timing, layered vanilla light particles, sound layers, impact feedback, and environment reaction. It does not claim final commercial-grade VFX until visually playtested in Minecraft.
