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
Latest validated M1 slice commit: `4135dc08c1ea8720c6c14cd5d418bcffe0480af1`

GitHub Actions run: `34044270475` — PASS

Current verified deliverable:
- version `0.1.0-alpha.1`
- artifact `turnbound-re-0.1.0-alpha.1-deliverables`
- artifact id `9992614156`
- artifact archive SHA-256 digest: `41bfc1a57022df2c3e315e58023de17d73d47892bcc5d56b5c1976971b791c93`
- production JAR SHA-256: `59b8c18ce34e1d6697361ac26c40986d04ee76d4d175f7a4140c6525efb6f975`
- production JAR verification: PASS

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
- BattleInstance owns the deterministic RNG stream and applies DamageService results to mutable HP/Poise state.
- damage resolution emits replay-readable DAMAGE / EXPOSED_APPLIED / PARTICIPANT_DEFEATED events.
- Poise break enters EXPOSED; target turn start removes EXPOSED, restores Poise to max and applies one-turn POISE_GUARD.
- POISE_GUARD halves Poise damage and expires on the following turn start, preventing permanent stun-lock loops.
- Energy storage is bounded to 0..100.
- Basic command generates canonical +10 Energy.
- Guard command generates canonical +15 Energy, applies non-stacking Guard and Guard expires at the actor's next turn start.
- Guard halves incoming final HP damage while active.
- data-defined Skill/Burst commands validate and spend their explicit Energy cost; insufficient-Energy rejection is mutation-free.
- invalid action kind/action-id mismatch rejection is mutation-free.
- StatusDefinition Mojang Codec with canonical minimum fields: id, polarity, durationUnit, maxStacks, refreshRule, dispelTags and hooks.
- DefinitionRegistry now supports immutable validated status definitions while preserving the previous action/character constructor path.
- status validation covers duplicate id, polarity, TURN/CYCLE duration, stack range and malformed refresh/tag/hook fields.
- canonical shared status ids are contract-tested: GUARD / EXPOSED / POISE_GUARD / BURN / SLOW / ATK_UP / DEF_DOWN.
- pure JUnit coverage includes initiative/revision/damage/Poise deterministic behavior, Energy gain/spend/rejection, Guard damage/lifecycle, status Codec/registry validation and identical event-stream checks for the implemented command slice.

Validation evidence for latest slice:
- dependency resolution + clean build: PASS.
- unit tests: PASS.
- production JAR verification: PASS.
- SHA-256 generation: PASS.
- build report + logs + deliverable upload: PASS.

## M1 remaining
Continue in backlog/canonical order:
1. shared Status runtime/StatusService hooks and migrate GUARD / EXPOSED / POISE_GUARD lifecycle onto the shared runtime without inventing presentation behavior.
2. Intent model/AI intent consistency, break-cancel/recover behavior and `INTENT_CHANGED` event.
3. target/action validation beyond revision/current actor: ownership, cooldown/status, target count/team/alive, duplicate command.
4. victory/defeat/reward event and cleanup transitions; skip defeated actors safely.
5. deterministic same seed + same commands full event-stream test and explicit no-soft-lock transition tests for M1 PASS.

## Runtime verification status
- Datagen: NOT RUN; no generated production data is required by the current M0/M1 slice.
- GameTest: NOT RUN; no GameTest contract is implemented yet.
- Dedicated server smoke: NOT RUN; runtime smoke task not yet established.
- Client smoke: NOT RUN; current work is pure battle/data core and no production presentation is being claimed.

## Design gate
Production UI, character appearance/model/animation, VFX, icons/fonts/colors and authored world visuals remain GATED and were not created or guessed.
