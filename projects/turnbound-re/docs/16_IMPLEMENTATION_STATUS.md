# 16 — IMPLEMENTATION STATUS

Updated: 2026-09-07

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
Latest validated M1 slice commit: `3f8bc66177d53b0775496e20ec004448c9546090`

GitHub Actions run: `34041122937` — PASS

Current verified deliverable:
- version `0.1.0-alpha.1`
- artifact `turnbound-re-0.1.0-alpha.1-deliverables`
- artifact id `9991703874`
- artifact archive SHA-256 digest: `371e19f20a942f08ce15708735de5414bad838596c7c9cbe344b3d90c72ce9d7`
- production JAR verification: PASS
- per-JAR SHA-256 file generation/upload: PASS (contained in deliverable artifact)

Implemented:
- canonical BattleState enum/state-machine foundation.
- BattleInstance containing battleId, seed, revision, cycle, actor order, participants and event log.
- SPD-desc initiative with stable `participantOrdinal` tie-break.
- server-authoritative command acceptance skeleton with expected revision/current actor checks.
- rejected stale/wrong-actor commands do not mutate revision or event log.
- enemy AI basic-command stub.
- deterministic actor/cycle progression across repeated cycles.
- canonical damage tags: MELEE / PROJECTILE / FIRE / BLAST / ARCANE / VOID.
- affinity grades with canonical HP and Poise multipliers: WEAK / NORMAL / RESIST / IMMUNE.
- deterministic per-battle RNG stream abstraction with explicit draw counting; damage resolution consumes a fixed two-draw order for crit then variance.
- canonical HP damage formula with crit, EXPOSED x1.20, [0.95, 1.05] deterministic variance, IMMUNE=0 exception and stable modifier ordering.
- canonical Poise affinity multipliers and IMMUNE Poise=0 behavior.
- replay-readable damage breakdown including raw, affinity, crit, exposed, variance, other modifier, final HP, Poise and RNG position.
- battle-owned mutable participant combat state for HP, Poise, Energy, EXPOSED, POISE_GUARD and Guard flags.
- BattleInstance now owns the deterministic RNG stream and applies DamageService results to mutable HP/Poise state.
- damage resolution emits replay-readable DAMAGE / EXPOSED_APPLIED / PARTICIPANT_DEFEATED events.
- Poise break enters EXPOSED; target turn start removes EXPOSED, restores Poise to max and applies one-turn POISE_GUARD.
- POISE_GUARD halves Poise damage and expires on the following turn start, preventing permanent stun-lock loops.
- Energy storage is bounded to 0..100 with validated gain/spend primitives ready for command wiring.
- pure JUnit coverage for stable initiative, revision rejection, repeated battle event streams, participant validation, same-seed damage stream identity, affinity/immune rules, EXPOSED multiplier, canonical tag coverage, invalid damage inputs, mutable HP/Poise mutation, EXPOSED recovery, POISE_GUARD mitigation and identical mutable state/event results for identical seed/input.

Validation evidence for latest slice:
- dependency resolution + clean build: PASS.
- unit tests: PASS.
- production JAR verification: PASS.
- SHA-256 generation: PASS.
- build report + logs + deliverable upload: PASS.

## M1 remaining
Continue in backlog/canonical order:
1. finish Energy command semantics: Basic +10, Guard +15, Skill/Burst spend validation.
2. Guard command lifecycle and final-damage halving through command resolution.
3. shared StatusDefinition/runtime status hooks for GUARD / EXPOSED / POISE_GUARD / BURN / SLOW / ATK_UP / DEF_DOWN.
4. Intent model/AI intent consistency, break-cancel/recover behavior and `INTENT_CHANGED` event.
5. target/action validation beyond revision/current actor: ownership, energy/cooldown/status, target count/team/alive, duplicate command.
6. victory/defeat/reward event and cleanup transitions; skip defeated actors safely.
7. deterministic same seed + same commands full event-stream test and explicit no-soft-lock transition tests for M1 PASS.

## Runtime verification status
- Datagen: NOT RUN; no generated production data is required by the current M0/M1 slice.
- GameTest: NOT RUN; no GameTest contract is implemented yet.
- Dedicated server smoke: NOT RUN; runtime smoke task not yet established.
- Client smoke: NOT RUN; current work is pure battle/data core and no production presentation is being claimed.

## Design gate
Production UI, character appearance/model/animation, VFX, icons/fonts/colors and authored world visuals remain GATED and were not created or guessed.
