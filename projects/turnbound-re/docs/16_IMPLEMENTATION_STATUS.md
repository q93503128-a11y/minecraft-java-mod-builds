# 16 — IMPLEMENTATION STATUS

Updated: 2026-09-06

## Current milestone
M0 — Bootstrap & Contracts: IN PROGRESS

Implemented on main:
- NeoForge 26.2 / Java 25 scaffold using repository-validated NeoForge 26.2.0.38-beta and ModDevGradle 2.0.143.
- mod id `turnbound_re` and metadata/resources.
- CharacterDefinition and ActionDefinition Mojang Codecs.
- DefinitionValidator with role/affinity/star/action-reference validation.
- VanillaMobCoverageValidator with explicit PLAYABLE / ENEMY_ONLY / EXCLUDED coverage contract.
- DEBUG_ONLY command skeleton `/turnbound_re contracts`.
- PureBattleHarness deterministic initiative probe.
- JUnit M0 contract tests for deterministic replay, invalid data rejection, and unmapped vanilla mob detection.
- project workflow with clean build, unit test, JAR ZIP/metadata/class/resource/source checks and SHA-256 generation.

## M0 remaining before PASS
- Confirm GitHub Actions clean build on current main.
- Fix any compile/API/test/JAR verification failures surfaced by CI.
- Record final build/JAR verification result.
- Expand registry/resource-loading integration beyond pure contract definitions if required by the M0 acceptance gate.

## Design gate
Production UI, character appearance, VFX, and authored world visuals remain GATED and were not created.
