# 야수각인: Wildbound

## Identity

- Slug: `wildbound`
- Mod ID: `wildbound`
- Namespace: `wildbound`
- Java package root: `dev.moonseungjun.wildbound`
- Current version: `1.8.1`
- Final JAR: `wildbound-1.8.1.jar`

## Toolchain

- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- NeoForge: `26.2.0.38-beta`
- Gradle: `9.2.1`
- ModDevGradle: `2.0.143`
- GeckoLib: `5.5.3`

## Shared-repository scope

This repository is shared by multiple Minecraft Java mod projects and multiple workers. Work on Wildbound must remain isolated to:

- `projects/wildbound/**`
- `.github/workflows/build-wildbound.yml`
- a root index/document only when Wildbound registration genuinely requires it

Do not edit, rename, delete, reformat, rebuild, or replace another project's files while working on Wildbound. Do not create an arbitrary branch or pull request unless the user explicitly requests one. The default target is `main`, following the root `README.md`, `AGENTS.md`, and `docs/BUILD_STANDARD.md`.

## Compatibility contract

The build-fix process must preserve all existing gameplay behavior and persistent identifiers, including:

- player and monster UUID ownership
- monster species/form identifiers
- level, experience, evolution, party, ranch, rune, codex, and boss data
- registration IDs, payload IDs, SavedData keys, resource locations, recipes, tags, and structures
- existing-world and existing-save compatibility
- server-authoritative multiplayer behavior

Do not delete systems, mass-comment code, add no-op stubs, invent fake Minecraft/NeoForge APIs, or weaken server authority merely to make compilation pass.

## Dependency contract

- GeckoLib `5.5.3` is a required external runtime dependency.
- GeckoLib must not be shaded or bundled into the Wildbound JAR.
- e4mc `6.2.1` is an optional host-side external mod.
- e4mc must not be bundled into the Wildbound JAR.
- e4mc must not be declared as Wildbound's compile or runtime Gradle dependency.
- Joining players do not install e4mc merely because the host uses it; all players still need Wildbound and its required gameplay dependencies as appropriate for the loader.

## Required build sequence

1. Reconstruct or check out the exact current Wildbound source baseline.
2. Verify Java `25`, Gradle `9.2.1`, Minecraft `26.2`, NeoForge `26.2.0.38-beta`, ModDevGradle `2.0.143`, and GeckoLib `5.5.3` resolution.
3. Run the repository/source preflight tools.
4. Run an actual clean build:

```text
gradle --no-daemon clean build --stacktrace --warning-mode all
```

5. On failure, fix the earliest real root-cause error and repeat until `BUILD SUCCESSFUL`.
6. Run datagen and GameTest when the source exposes supported tasks.
7. Attempt a dedicated-server smoke test and client loading test when CI constraints permit.
8. Open and inspect the final JAR.
9. Generate SHA-256, report, and raw logs.

Static inspection or preflight success alone is not completion.

## Final JAR validation

The final JAR must:

- be a valid, non-empty ZIP/JAR
- contain the loader metadata required by this NeoForge version
- contain compiled `.class` files under `dev/moonseungjun/wildbound/`
- contain `assets/wildbound/`
- contain `data/wildbound/`
- report version `1.8.1`
- contain no `.java` source files
- contain no `.github/`, `tools/`, build logs, handoff documents, or development reports
- contain no duplicate ZIP entries
- contain no e4mc classes or artifacts
- contain no shaded GeckoLib implementation classes

## Source baseline

- Original archive: `Wildbound_Java_26.2_v1.8.1_Source.zip`
- Original archive SHA-256: `e84eb9c46728db13188cd16fe4aec8a52ecde629a732955374a1a04f74197cec`
- Baseline date: `2026-07-31`
- Cleaned CI source archive target: `projects/wildbound/source/source-min.tar.xz.b64.part*`
- Cleaned CI archive SHA-256: `1832e3458319d0ec0f626fc9be92c0532416afe9b0c772ba3668e5a8371c8524`

The cleaned CI baseline removes the stale e4mc Gradle runtime declaration while preserving Wildbound source, resources, IDs, and save compatibility.

## Delivery contract

Successful Actions artifacts must provide at least:

```text
wildbound-1.8.1.jar
wildbound-1.8.1.jar.sha256
BUILD_AND_RUNTIME_REPORT.md
raw-logs.zip
```

Do not report success until the GitHub-hosted Java 25 build has actually succeeded and the produced JAR has been opened and validated. The primary user-facing deliverable is the real `wildbound-1.8.1.jar` that can be placed directly into the Modrinth instance's content/files area.