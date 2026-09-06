# 16 — IMPLEMENTATION STATUS

Updated: 2026-09-06

## Current milestone
M0 — Bootstrap & Contracts: **PASS**

M1 — Deterministic Battle Core: **IN PROGRESS**

## M0 accepted on main
Validated build commit: `8d462013cf7633b9a9c82142ad027151b37e1b03`

GitHub Actions run: `34038784028`

Version: `0.1.0-alpha.1`

Environment:
- Minecraft 26.2
- Java 25
- NeoForge 26.2.0.38-beta
- Gradle wrapper 9.2.1
- ModDevGradle 2.0.143

M0 PASS evidence:
- dependency resolution: PASS
- clean build: PASS
- JUnit M0 contracts: PASS
- invalid definition/action validation: PASS
- unmapped vanilla Mob coverage test: PASS
- production JAR ZIP/metadata/class/assets/data/source/development-path/duplicate-entry verification: PASS
- SHA-256 generation: PASS
- deliverable artifact upload: PASS

Implemented M0 contracts:
- mod id `turnbound_re` and NeoForge scaffold.
- CharacterDefinition and ActionDefinition Mojang Codecs.
- immutable validated DefinitionRegistry.
- DefinitionValidator with character/action duplicate, range and reference validation.
- VanillaMobCoverageValidator with explicit PLAYABLE / ENEMY_ONLY / EXCLUDED coverage contract.
- DEBUG_ONLY command skeleton `/turnbound_re contracts`, migrated to the NeoForge/Minecraft 26.2 permission API.
- PureBattleHarness deterministic long-run probe.
- Gradle unit-test runtime configured through ModDevGradle `unitTest` support so Minecraft/Codec classes are available to JUnit.
- CI produces logs, JAR, SHA-256 and `BUILD_AND_RUNTIME_REPORT.md`.

## M1 implemented and validated so far
Validated M1 slice commit: `e800978992a56e0faa70563f6f57abe811a6ad59`

GitHub Actions run: `34039088297` — PASS

Current verified JAR:
- `turnbound_re-0.1.0-alpha.1.jar`
- SHA-256: `b29f3b94e6fd644a603c4d138bc2c83d16d88a8fa8dc0a43095e7d1aa682e5f7`

Implemented:
- canonical BattleState enum/state-machine foundation.
- BattleInstance containing battleId, seed, revision, cycle, actor order, participants and event log.
- SPD-desc initiative with stable `participantOrdinal` tie-break.
- server-authoritative command acceptance skeleton with expected revision/current actor checks.
- rejected stale/wrong-actor commands do not mutate revision or event log.
- enemy AI basic-command stub.
- deterministic actor/cycle progression across repeated cycles.
- pure JUnit coverage for stable initiative, revision rejection, deterministic repeated event streams and participant membership validation.

## M1 remaining
Continue in backlog/canonical order:
1. Damage/Affinity service and deterministic single RNG stream with event breakdown.
2. mutable participant combat state (HP/Poise/Energy/status/Guard).
3. Poise → EXPOSED → recovery → POISE_GUARD lifecycle and anti-stunlock behavior.
4. Energy generation/spend, Guard and shared statuses.
5. Intent model/AI intent consistency and `INTENT_CHANGED` behavior.
6. target/action validation beyond revision/current actor.
7. victory/defeat/reward event and cleanup transitions.
8. deterministic same seed + same commands full event-stream test and explicit no-soft-lock transition tests for M1 PASS.

## Runtime verification status
- Datagen: NOT RUN; no generated production data is required by the current M0/M1 slice.
- GameTest: NOT RUN; no GameTest contract is implemented yet.
- Dedicated server smoke: NOT RUN; runtime smoke task not yet established.
- Client smoke: NOT RUN; current work is pure battle/data core and no production presentation is being claimed.

## Design gate
Production UI, character appearance/model/animation, VFX, icons/fonts/colors and authored world visuals remain GATED and were not created or guessed.
