# 음양태극

- Slug: test
- Mod ID: moon_test
- Namespace: moon_test
- Mod version: 0.4.0
- Minecraft: 26.3
- Java: 25
- Loader: Fabric
- Loader version: 0.19.5
- Fabric API: 0.162.0+26.3
- Gradle: 9.6.0
- Build plugin: Fabric Loom 1.17.20
- Final JAR: test-0.4.0.jar
- Existing-world compatibility: Safe; no persistent world data is written
- Required dependencies: Fabric API 0.162.0+26.3; Player Animation Library Fabric 1.2.8+26.3 or newer 26.3 build
- Optional external mods: None
- Forbidden bundled dependencies: Minecraft client/server binaries; PAL is a separate required mod and is not bundled
- Datagen task: NOT PROVIDED
- GameTest task: NOT PROVIDED
- Server smoke-test task: Gradle server launch if available; otherwise NOT RUN
- Client smoke-test task: Actual Minecraft launch and visual review; report status separately

## Purpose

`projects/test/` is a reusable experimental mod space. The active experiment replaces the old gacha presentation with a first-person and third-person two-hand casting skill, a 3D yin-yang taiji projectile, visual block impact, and phase-synchronized sound.

## Runtime design

- G requests a cast from the client. The server rejects dead/spectator players and enforces a short repeat interval, then broadcasts the caster UUID to players in that dimension.
- Player Animation Library drives both player arms, torso, first-person arms, and two custom hand anchor bones. The client animation asset is `assets/moon_test/player_animations/yin_yang.json`.
- Energy geometry reads PAL's world-space hand anchor pose. The render pipeline submits meshes through Fabric's 26.3 `LevelRenderEvents.COLLECT_SUBMITS` API.
- Taiji is a world-space, thick two-sided mesh. Its boundary uses two opposite semicircular arcs for the S curve, with raised contrasting dots and a narrow edge rim. A forward-facing spin keeps the symbol legible during travel.
- The projectile raycasts against block collision shapes on the client and renders an expanding ring and dispersal. No damage or persistent world state is changed.
- Cast state is held only for the current client world and expires after 132 ticks; disconnect, death, despawn, or dimension mismatch clears it.
- Sound uses vanilla game sound events with separate pitch/volume per phase. There are no downloaded sound assets.

## Validation language

Build, JAR inspection, client launch, visual PLAYTESTED status, and MULTIPLAYER TESTED status are reported independently in `BUILD_AND_RUNTIME_REPORT.md`. Compilation alone does not verify animation fit, taiji readability, sound balance, or frame cost.
