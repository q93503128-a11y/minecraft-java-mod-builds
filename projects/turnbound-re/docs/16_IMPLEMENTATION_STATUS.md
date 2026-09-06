# 16 — IMPLEMENTATION STATUS

Updated: 2026-09-07

## Current milestone
M0 — Bootstrap & Contracts: **PASS**

M1 — Deterministic Battle Core: **PASS**

M2 — Minecraft Adapter & Network: **NEXT**

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

## M1 accepted on main
Validated M1 commit: `d4226799857c0bffed11c1bbc25ba8d4db1402c0`

GitHub Actions run: `34056905906` — PASS

Current verified deliverable:
- version `0.1.0-alpha.1`
- artifact `turnbound-re-0.1.0-alpha.1-deliverables`
- artifact id `9996240084`
- artifact archive SHA-256 digest: `75e2b7360b669c41c76f3f8ee486add80a102439769ff629df192f7807c3e128`
- production JAR SHA-256: `c62052ebb09a9d3a23e74a9a3c71ee47214fd91b4be5f04089fd59c8fd37c8cf`
- production JAR verification: PASS

Implemented:
- canonical BattleState enum/state-machine foundation.
- BattleInstance containing battleId, seed, revision, cycle, actor order, participants and event log.
- SPD-desc initiative with stable `participantOrdinal` tie-break.
- server-authoritative command acceptance with expected revision/current actor checks.
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
- shared battle-owned `StatusRuntime` stores deterministic active status ids and stack counts.
- shared `StatusService` is the mutation entry point for canonical status ids and data-defined max-stack enforcement.
- GUARD / EXPOSED / POISE_GUARD use the shared status ledger rather than independent boolean storage.
- battle-owned `EnemyIntent` model exposes action id, type, target category, risk, break-cancel flag and optional downgrade action.
- each live enemy publishes a deterministic Intent before its action window; the M1 AI stub resolves exactly the currently published Intent action.
- replacing an already-published Intent emits `INTENT_CHANGED` before the changed AI command can resolve.
- Poise break converts `breakCancelable=true` Intent to canonical `RECOVER`; recovered enemy turn emits `RECOVER` instead of silently selecting another action.
- non-cancelable Intent with `breakDowngradeAction` is replaced by the configured downgraded action and normalized to NORMAL risk for the M1 stub.
- defeated enemy Intent state is removed instead of leaving a stale telegraph.
- terminal outcome evaluation checks living PLAYER/ENEMY participants at `CHECK_END` and emits deterministic `BATTLE_RESULT` before entering `REWARD`.
- both VICTORY and DEFEAT paths are unit-tested through REWARD and CLEANUP.
- defeated participants are skipped during actor advancement and emit `DEFEATED_ACTOR_SKIPPED`, preventing dead actors from receiving action windows.
- end-of-battle cleanup transitions `REWARD -> CLEANUP -> NOT_IN_BATTLE`, clears enemy Intent state, zeros Energy and clears transient battle statuses.
- `BattleCommand` carries stable command identity and target ids while preserving the legacy constructor used by earlier M1 tests.
- strict `BattleCommandService` validates action ownership, cooldown readiness, status eligibility, target count, duplicate targets, target existence/alive state and SELF/ALLY/ENEMY/ANY team rule before mutating BattleInstance.
- duplicate/retransmitted command identity is rejected after the first accepted command.
- strict validation failures are mutation-free for revision, event log, battle state and Energy.
- defeated targets are explicitly rejected by the strict target gate.
- 100 complete player/enemy cycles are replayed twice with the same battle id, seed and command stream; both runs remain live without soft-lock and produce identical cycle/revision/RNG/event streams.

M1 PASS evidence:
- dependency resolution + clean build: PASS.
- full JUnit suite: PASS.
- explicit VICTORY and DEFEAT terminal flows: PASS.
- strict action/target/retransmission validation tests: PASS.
- 100-cycle deterministic/no-soft-lock test: PASS.
- production JAR verification: PASS.
- SHA-256 generation: PASS.
- build report + logs + deliverable upload: PASS.

## Next implementation work — M2
Continue in canonical backlog order:
1. Entity participant binding between Minecraft entities and battle participant ids without leaking vanilla world AI/damage into battle resolution.
2. world AI/damage isolation guards.
3. C2S command and S2C snapshot/event payload contracts, preserving server authority and revision validation.
4. DEBUG_ONLY battle HUD/inspection path only; do not create production UI.
5. disconnect, entity removal, dimension change and cleanup guards so orphan battles cannot remain.
6. establish the strongest practical runtime smoke/GameTest path and work toward the M2 PASS requirement: 20 repeated debug encounters with orphan battle count 0.

## Runtime verification status
- Datagen: NOT RUN; no generated production data is required by the current M0/M1 slice.
- GameTest: NOT RUN; no GameTest contract is implemented yet.
- Dedicated server smoke: NOT RUN; runtime smoke task not yet established.
- Client smoke: NOT RUN; M1 is a pure battle/data core and no production presentation is being claimed.

## Design gate
Production UI, character appearance/model/animation, VFX, icons/fonts/colors and authored world visuals remain GATED and were not created or guessed.
