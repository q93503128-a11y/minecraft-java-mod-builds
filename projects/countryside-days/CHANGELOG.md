# Changelog

## 0.1.0-alpha.2 — 2026-07-31

- Added the official NeoForge 26.2 / Java 25 / ModDevGradle project scaffold.
- Added and verified the official Gradle 9.2.1 wrapper.
- Added the `countrysidedays` mod entrypoint and separated registries.
- Added the country kitchen counter block.
- Added wild herb, river fish, country stew, and recipe notebook items.
- Added a dedicated creative tab and Korean/English translations.
- Added initial block and item model definitions using redistributable vanilla references.
- Added a cooking ingredient tag.
- Added persistent world state for the restaurant anchor, per-counter herb preparation, and total meals prepared.
- Added the first multiplayer-shared cooking loop: wild herb preparation followed by river fish to complete country stew.
- Added empty-hand counter guidance and recipe-notebook status messages.
- Migrated player feedback to the Minecraft 26.2 `sendSystemMessage` and `sendOverlayMessage` APIs.
- Added reproducible CI, JAR structure verification, SHA-256 generation, build artifacts, compiler-error extraction, and recursive-run prevention.
- Verified Java 25 clean build, data generation, and JAR structure inspection.

This version is a foundation build with one real cooking interaction. It does not yet include the countryside world generator, natural ingredient acquisition, villagers, custom renderer, final UI, or runtime smoke tests.
