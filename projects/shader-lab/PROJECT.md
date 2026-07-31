# Shader Lab

- Slug: shader-lab
- Mod ID: shaderlab
- Namespace: shaderlab
- Mod version: 0.1.0-alpha.1
- Minecraft: 26.2
- Java: 25
- Loader: NeoForge
- Loader version: 26.2.0.38-beta
- Gradle: CI-managed Gradle 9.6.1
- Build plugin: net.neoforged.moddev 2.0.142
- Final JAR: shaderlab-0.1.0-alpha.1.jar
- Existing-world compatibility: No persistent world data is written
- Required dependencies: Minecraft 26.2, NeoForge 26.2.0.38-beta or newer compatible 26.2 build
- Optional external mods: None
- Forbidden bundled dependencies: Minecraft, NeoForge, shader loaders, third-party shader packs
- Datagen task: NOT APPLICABLE
- GameTest task: NOT APPLICABLE for GPU post-processing
- Server smoke-test task: NOT APPLICABLE; physical-client-only mod
- Client smoke-test task: gradle runClient (interactive GPU/display required)

## Test contract

Shader Lab is a non-distribution, client-side rendering experiment. Version 0.1 applies one
vanilla post-processing chain after a client world is loaded. It must never write world data,
register gameplay content, or load on a dedicated server.

The first effect is `shaderlab:lush_grade`. It performs a restrained filmic colour grade:

- mild highlight compression
- slightly richer saturation
- warm sunlight bias
- subtle cool shadow bias
- small contrast lift
- no temporal accumulation
- no depth sampling
- no camera shake
- no full-scene blur

If another post effect is already active, Shader Lab does not replace it. If loading the custom
effect throws an exception, the mod logs the failure and clears only its own effect.
