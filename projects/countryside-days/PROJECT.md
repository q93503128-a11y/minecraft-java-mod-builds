# Countryside Days

## Identity

- Slug: `countryside-days`
- Mod ID: `countryside_days`
- Namespace/package root: `kr.countrysidedays`
- Current build-fix version: `0.20.1`
- Final JAR: `countryside_days-0.20.1.jar`

## Toolchain

- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- NeoForge: `26.2.0.38-beta`
- Gradle: `9.2.1`
- ModDevGradle: `2.0.142`

## Compatibility Contract

- Keep the existing mod ID, namespace, registration IDs, SavedData keys, payload IDs, recipe/structure IDs, and existing-world compatibility.
- `e4mc` is an optional external mod and must not be bundled or added as a compile dependency.
- Do not shrink, rewrite, stub, or delete existing gameplay systems merely to make compilation pass.

## Build Tasks

- Clean build: `gradle --no-daemon clean build`
- Datagen: `gradle --no-daemon runData`
- GameTest: `gradle --no-daemon runGameTestServer`
- Local Modrinth pack: `gradle --no-daemon localMrpack`
- Release bundle: `gradle --no-daemon releaseBundle`

## Final Validation

The final JAR must contain:

- `META-INF/neoforge.mods.toml`
- compiled `.class` files below `kr/countrysidedays/`
- `assets/countryside_days/`
- `data/countryside_days/`

It must not contain Java sources, tools, GitHub workflow files, development reports, or duplicate ZIP entries.

## Current Source Baseline

- Archive: `Countryside_Days_NeoForge_26.2_v0.20.1_Source.zip`
- Archive SHA-256: `4d1ba572f320b77844ddbc3076bbffb44c5a1b8661047abc8cd921f28c230180`
- Baseline date: `2026-07-31`
- Source archive is stored as split Base64 text under `source/` so it can be reconstructed exactly by GitHub Actions.

## Delivery Contract

Do not report success until the GitHub-hosted Java 25 build has actually succeeded and the resulting JAR has been opened and inspected. The primary user-facing deliverable is the real `countryside_days-0.20.1.jar` for Modrinth App Content → file upload.