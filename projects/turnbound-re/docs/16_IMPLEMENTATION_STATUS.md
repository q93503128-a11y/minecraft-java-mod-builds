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
Latest validated M1 slice commit: `0c4ef95348a0aac0a5e9bc3fb7b3e2ca133d3a55`

GitHub Actions run: `34050450947` — PASS

Current verified deliverable:
- version `0.1.0-alpha.1`
- artifact `turnbound-re-0.1.0-alpha.1-deliverables`
- artifact id `9994379220`
- artifact archive SHA-256 digest: `8658a657b0a1c3dd250ebf89206a149984c22a341c63d58804915f4a2b7ecd1b`
- production JAR SHA-256: `ed79072cc9473c1852998e64d931d76b876d7a667fd090583544d22d2af05fdc`
- production JAR verification: PASS

Implemented:
- canonical BattleState enum/state-machine foundation.
- BattleInstance containing battleId, seed, revision, cycle, actor order, participants and event log.
- SPD-desc initiative with stable `participantOrdinal` tie-break.
- server-authoritative command acceptance skeleton with expected revision/current actor checks.
- rejected stale/wrong-actor commands do not mutate revision or event log.
- deterministic actor/cycle progression across repeated cycles.
- canonical damage tags: MELEE / PROJECTILE / FIRE / BLAST / ARCANE / VOID.
- affinity grades with canonical HP and Poise multipliers: WEAK / NORMAL / RESIST / IMMUNE.
- deterministic per-battle RNG stream abstraction with explicit draw counting; damage resolution consumes a fixed two-draw order for crit then variance.
- canonical HP damage formula with crit, EXPOSED x1.20, [0.95, 1.05] deterministic variance, IMMUNE=0 exception and stable modifier ordering.
- canonical Poise affinity multipliers and IMMUNE Poise=0 behavior.
- replay-readable damage breakdown including raw, affinity, crit, exposed, variance, other modifier, final HP, Poise and RNG position.
- battle-owned mutable participant combat state for HP, Poise, Energy and active statuses.
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
- DefinitionRegistry supports immutable validated status definitions while preserving the previous action/character constructor path.
- status validation covers duplicate id, polarity, TURN/CYCLE duration, stack range and malformed refresh/tag/hook fields.
- canonical shared status ids are contract-tested: GUARD / EXPOSED / POISE_GUARD / BURN / SLOW / ATK_UP / DEF_DOWN.
- shared battle-owned `StatusRuntime` now stores deterministic active status ids and stack counts.
- shared `StatusService` is the mutation entry point for canonical status ids and data-defined max-stack enforcement.
- GUARD / EXPOSED / POISE_GUARD no longer use independent boolean storage; legacy combat-state accessors delegate to the shared runtime.
- Poise break/recovery/Guard lifecycle preserves the existing replay event contract while mutating the shared status runtime.
- battle-owned `EnemyIntent` model exposes action id, type, target category, risk, break-cancel flag and optional downgrade action.
- each live enemy publishes a deterministic Intent before its action window; the M1 AI stub resolves exactly the currently published Intent action.
- replacing an already-published Intent emits `INTENT_CHANGED` before the changed AI command can resolve.
- Poise break converts `breakCancelable=true` Intent to canonical `RECOVER`; recovered enemy turn emits `RECOVER` instead of silently selecting another action.
- non-cancelable Intent with `breakDowngradeAction` is replaced by the configured downgraded action and normalized to NORMAL risk for the M1 stub.
- defeated enemy Intent state is removed instead of leaving a stale telegraph.
- pure JUnit coverage includes initiative/revision/damage/Poise deterministic behavior, Energy gain/spend/rejection, Guard damage/lifecycle, status Codec/registry validation, shared status runtime migration/max-stack validation, Intent publication/AI consistency, cancel-to-RECOVER, downgrade, event ordering and identical event-stream checks for the implemented slice.

Validation evidence for latest slice:
- dependency resolution + clean build: PASS.
- unit tests: PASS.
- production JAR verification: PASS.
- SHA-256 generation: PASS.
- build report + logs + deliverable upload: PASS.

## M1 remaining
Continue in backlog/canonical order:
1. target/action validation beyond revision/current actor: ownership, cooldown/status, target count/team/alive, duplicate command.
2. victory/defeat/reward event and cleanup transitions; skip defeated actors safely.
3. deterministic same seed + same commands full event-stream test and explicit no-soft-lock transition tests for M1 PASS.

## Runtime verification status
- Datagen: NOT RUN; no generated production data is required by the current M0/M1 slice.
- GameTest: NOT RUN; no GameTest contract is implemented yet.
- Dedicated server smoke: NOT RUN; runtime smoke task not yet established.
- Client smoke: NOT RUN; current work is pure battle/data core and no production presentation is being claimed.

## Design gate
Production UI, character appearance/model/animation, VFX, icons/fonts/colors and authored world visuals remain GATED and were not created or guessed.
