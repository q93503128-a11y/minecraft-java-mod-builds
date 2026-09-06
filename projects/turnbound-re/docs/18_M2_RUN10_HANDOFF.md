# 18 — M2 RUN 10 HANDOFF

Updated: 2026-09-07

## Authority
This handoff supplements `16_IMPLEMENTATION_STATUS.md` and `17_M2_RUN9_HANDOFF.md`. Canon remains `00`–`15`, especially `07_TECHNICAL_ARCHITECTURE.md`, `14_IMPLEMENTATION_BACKLOG.md`, `15_READINESS_AUDIT.md`, plus repository `docs/BUILD_STANDARD.md` and `docs/QUALITY_STANDARD.md`.

## Validated main slice
Validated code commit: `0bda75165cf459ea44a62efb669103e3424e70b0`

GitHub Actions run: `34062799166` — PASS

Version: `0.1.0-alpha.1`

Artifact: `turnbound-re-0.1.0-alpha.1-deliverables`

Artifact id: `9998005741`

Artifact archive SHA-256 digest: `30d9644751cbcfe2bb6924d60da1aa91b1b16f9b264feb4c66a8ecf70de7ee08`

Production JAR SHA-256: `e167a775d13aac27f33913a4afce85e2acedb535532de7bd8c86910b13fc7b74`

Production JAR verification: PASS

## Implemented this run
- Connected the M2 `BattleWorldIsolation` policy to live NeoForge 26.2.x events through `BattleWorldEventHooks`.
- Added a process-wide server/common `BattleManager` owned by the mod entrypoint so live hooks and future network/debug adapters share one authoritative battle registry.
- `LivingIncomingDamageEvent` cancels normal-world incoming damage for battle-owned living entities on the logical server.
- `LivingKnockBackEvent` cancels vanilla knockback for battle-owned living entities on the logical server.
- `EntityTickEvent.Pre` cancels the server tick for battle-owned `Mob` entities, strongly suppressing vanilla goal/brain AI and ordinary mob despawn logic while the deterministic core owns the participant.
- Player ticks are deliberately not cancelled.
- `EntityLeaveLevelEvent` performs idempotent `BattleManager.cleanup` before a bound entity leaves the level, covering the common removal/dimension/disconnect teardown path without attempting to cancel the non-cancellable event.
- Cleanup restores normal world policy automatically because the isolation layer is ownership-based and does not permanently mutate vanilla entity flags.

## Verification evidence
- Official NeoForge 26.2.x event signatures were checked before implementation.
- Java/Gradle toolchain: PASS.
- dependency resolution: PASS.
- `clean build` + full JUnit suite: PASS.
- live hook API compilation against NeoForge `26.2.0.38-beta`: PASS.
- production JAR ZIP/metadata/class/assets/data/source/development-path/duplicate-entry verification: PASS.
- SHA-256 generation: PASS.
- deliverable upload: PASS.
- Datagen: NOT RUN; current slice has no generated production data requirement.
- GameTest: NOT RUN.
- dedicated server smoke: NOT RUN.
- client smoke: NOT RUN.

## M2 status
M2 remains **IN PROGRESS**.

The live hook layer now compiles against the actual target loader and enforces the isolation decisions, but `EntityTickEvent.Pre` intentionally suppresses the entire bound-Mob tick. Runtime testing must verify that this strong isolation does not interfere with desired battle animation/position/lifecycle behavior before M2 can pass.

## Next exact work from latest main
Continue M2 backlog order without reverting this slice:
1. implement C2S battle-command and S2C snapshot/event payload contracts using the 26.2.x `RegisterPayloadHandlersEvent`/`PayloadRegistrar` API; preserve battle id + expected revision + sender/participant ownership checks before any mutation.
2. add DEBUG_ONLY inspection/HUD only after the network snapshot/event path exists; production UI remains design-gated.
3. strengthen disconnect/entity-removal/dimension lifecycle guards around the shared `BattleManager`, including repeated/idempotent teardown tests.
4. establish the strongest practical GameTest or debug encounter runtime harness, then run repeated encounters toward the M2 PASS requirement: 20 debug encounters, orphan battle count 0.
5. if runtime testing shows whole-Mob-tick cancellation is too broad, replace only that AI suppression mechanism with a narrower verified 26.2.x hook while keeping damage/knockback/removal contracts intact.

## Design gate
Do not create or guess production HUD/UI, character appearance/model/animation, VFX, icons/fonts/colors or authored world visuals. External-reference-gated presentation work stays GATED.
