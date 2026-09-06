# 17 — M2 RUN 9 HANDOFF

Updated: 2026-09-07

## Authority
This handoff supplements `16_IMPLEMENTATION_STATUS.md`. Canon remains `00`–`15`, especially `07_TECHNICAL_ARCHITECTURE.md`, `14_IMPLEMENTATION_BACKLOG.md`, `15_READINESS_AUDIT.md`, plus repository `docs/BUILD_STANDARD.md` and `docs/QUALITY_STANDARD.md`.

## Validated main slice
Validated code commit: `ccadaad718624622900674bc1f2c1a587a7d920e`

GitHub Actions run: `34059874291` — PASS

Version: `0.1.0-alpha.1`

Artifact: `turnbound-re-0.1.0-alpha.1-deliverables`

Artifact id: `9997129002`

Artifact archive SHA-256 digest: `dbd44d7a037bd4be0f2e8c8f04e8008a97853560d913c2ebcd23540eb8e62474`

Production JAR SHA-256: `decdf1be82dd50f1196d3f52996b52d4d32ed77951c4c2f4879d2ba466792362`

Production JAR verification: PASS

## Implemented this run
- Added `BattleWorldIsolation`, a server-authoritative policy layer backed by `BattleManager` entity ownership.
- A battle-owned entity rejects normal-world AI, external damage, vanilla knockback and ordinary despawn policy through explicit adapter-facing decisions.
- Removal of a battle-owned entity is marked as requiring battle cleanup first, preparing disconnect/entity-removal/dimension lifecycle guards without silently orphaning a battle.
- The policy deliberately does not permanently mutate vanilla entity flags. Once `BattleManager.cleanup(battleId)` removes ownership, all world interaction decisions return to normal automatically.
- Added `M2WorldIsolationTest` proving both player/enemy bindings are isolated only while battle-owned, unrelated entities remain unaffected, and cleanup restores all decisions.

## Verification evidence
- Java/Gradle toolchain: PASS.
- dependency resolution: PASS.
- `clean build` + full JUnit suite: PASS.
- production JAR ZIP/metadata/class/assets/data/source/development-path/duplicate-entry verification: PASS.
- SHA-256 generation: PASS.
- deliverable upload: PASS.
- Datagen: NOT RUN; current slice has no generated production data requirement.
- GameTest: NOT RUN; runtime contract not established yet.
- dedicated server smoke: NOT RUN.
- client smoke: NOT RUN.

## M2 status
M2 remains **IN PROGRESS**. This run establishes and validates the isolation policy contract, but does **not** claim that NeoForge event hooks already enforce every decision in a live world.

## Next exact work from latest main
Continue M2 backlog order without reverting this slice:
1. connect `BattleWorldIsolation` to the current NeoForge/Minecraft 26.2 server event/lifecycle APIs for external damage, knockback/removal/despawn and practical AI suppression; restore normal behavior on cleanup and prove no permanent vanilla-state mutation.
2. only after the live world isolation hooks compile and validate, implement C2S battle command and S2C snapshot/event payload contracts with server revision authority.
3. DEBUG_ONLY inspection/HUD only; production UI remains design-gated.
4. disconnect, entity removal and dimension-change cleanup guards.
5. establish GameTest/server/client smoke where practical and work toward 20 repeated debug encounters with orphan battle count 0.

## Design gate
Do not create or guess production HUD/UI, character appearance/model/animation, VFX, icons/fonts/colors or authored world visuals. External-reference-gated presentation work stays GATED.
