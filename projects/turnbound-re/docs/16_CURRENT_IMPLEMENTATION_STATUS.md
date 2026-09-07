# 16 — CURRENT IMPLEMENTATION STATUS

최종 갱신: 2026-09-07

이 문서는 TURNBOUND: RE의 현재 구현 상태와 다음 실제 개발 시작점을 기록하는 개발 인수인계 정본이다.
기획 정본을 대체하지 않는다. CANON/세부 규칙은 기존 문서가 우선하며, 이 문서는 "어디까지 구현/검증됐는가"만 기록한다.

## 1. 마지막 검증 기준
- 마지막 TURNBOUND: RE 검증 커밋: `a1d46ea3827e335cc89e0ca4d6effa1711c79cef`
- GitHub Actions: `Build turnbound-re` run `34074495383`
- 결과: **SUCCESS**
- 포함 검증: Java 25 toolchain, dependency resolution, clean build, 전체 JUnit, production JAR verify, artifact upload.

공용 모노레포의 `main`은 다른 프로젝트 작업으로 계속 전진할 수 있으므로 새 작업 세션에서는 위 SHA를 최신 HEAD로 가정하지 말고 반드시 현재 `main`을 다시 읽는다.

## 2. M0 — Bootstrap & Contracts
상태: **AUTOMATED PASS**

완료:
- NeoForge 프로젝트 scaffold.
- `turnbound_re` namespace/mod id.
- data Codec/registry/semantic/cross-reference validation.
- atomic definition reload + SHA-256 snapshot.
- vanilla Mob coverage validator 기반.
- debug harness.
- clean build/JAR verification CI.

## 3. M1 — Deterministic Battle Core
상태: **AUTOMATED PASS**

완료:
- server-authoritative `BattleInstance` state machine.
- deterministic battle RNG/revision/event stream.
- SPD initiative.
- strict command validation.
- six damage tags + affinity.
- Poise → EXPOSED → recovery/POISE_GUARD.
- Guard/Energy.
- timed data status runtime.
- Enemy Intent / break cancel / Intent delay.
- victory/defeat/reward/cleanup state flow.
- same seed/input deterministic regression tests.

## 4. M2 — Minecraft Adapter & Network
상태: **AUTOMATED GATE PASS / MANUAL CLIENT GATE PENDING**

완료:
- Minecraft entity↔participant binding.
- battle isolation hooks.
- C2S command + S2C snapshot/events.
- stale revision/sender ownership/current actor/action ownership/target validation.
- disconnect/entity removal/dimension cleanup guards.
- DEBUG_ONLY client state/HUD.
- 20-cycle automated encounter/cleanup soak with orphan battle 0.

미수행:
- 실제 Minecraft client에서 20회 반복 조우 manual gate.

사용자 방침상 중간 테스트를 요구하지 않고 완성본 테스트 시 함께 수행한다.

## 5. M3 — Representative Content
상태: **AUTOMATED PASS / FINAL BALANCE NOT LOCKED**

production definition 현재 대표 세트:
- 8 characters: Zombie, Skeleton, Spider, Creeper, Blaze, Witch, Enderman, Iron Golem.
- 40 actions.
- 12 statuses.
- normal + elite debug Encounter/Reward tables.

구현:
- JSON → Codec → bundle merge → semantic/cross-reference validation → atomic registry.
- 전투 시작 시 definition registry/hash + participant→character mapping snapshot.
- `/reload`가 진행 중 전투 규칙을 바꾸지 않음.
- `BattleActionExecutor`가 DAMAGE/HEAL/APPLY_STATUS/REMOVE_STATUS/ENERGY/POISE_DAMAGE/INTENT_DELAY 실행.
- 상태 modifier가 ATK/DEF/SPD/damage taken/Poise taken 계산에 실제 반영.
- production data 기반 DEBUG encounter가 실제 Mob `sourceEntity`를 CharacterDefinition으로 매칭.
- player Basic/Skill/Burst가 실제 network gateway/strict gate/executor 경로 사용.
- enemy basic Intent도 production definition에서 실행.
- Basic spam보다 Poise 대응 스킬이 실제 EXPOSED/RECOVER 기회를 만드는 acceptance test.
- 0-power DAMAGE/HEAL 등 무효 콘텐츠는 validator에서 차단.

Witch healing의 `hpPower=0` 문제는 수정 완료.

## 6. M4 — Progression & Reward
상태: **CORE + PERSISTENCE AUTOMATED PASS / END-TO-END ENCOUNTER SETTLEMENT PENDING**

완료:
- `ProgressionDefinition`을 datapack definition군으로 관리.
- originStar ★1~★5 / currentStar 최대 ★6.
- level caps 20/30/40/50/60/70.
- visible stat growth/ascension 계산.
- Coin/Essence/Character Shard.
- unlock / level-up / ascend / party composition.
- 4 slots + Squad Cost capacity.
- 실패한 progression operation은 원본 immutable root를 유지.
- deterministic `RewardService`.
- COIN/ESSENCE independent rolls.
- CHARACTER_SHARD는 성공 후보 중 `weight` 기반 단일 선택으로 의미 고정.
- `PlayerProgress.CODEC` round-trip.
- 실제 Minecraft `SavedDataType` 기반 `TurnboundProgressSavedData`.
- UUID별 immutable `PlayerProgress` 저장.
- 변경 시에만 `setDirty()`.
- `PlayerProgressStore`가 current definition snapshot으로 unlock/level/ascend/party/reward를 저장 상태에 적용.
- `BattleRewardContext`가 battle open 시 owner/rewardTable/rewardSeed를 snapshot할 수 있음.
- `BattleManager.claimVictoryReward`는 VICTORY + REWARD에서만 단 한 번 claim 가능.
- reward callback 실패 시 claim은 소모되지 않아 안전하게 retry 가능.
- cleanup 시 reward metadata/claim state 제거.
- `BattleRewardSettlementService`가 one-shot battle claim을 persisted `PlayerProgressStore.applyReward`로 연결할 준비 완료.

아직 남음:
1. authored `EncounterDefinition`으로 전투를 여는 production encounter launcher가 `BattleRewardContext`를 등록하도록 연결.
2. 전투가 `VICTORY → REWARD`에 도달할 때 `BattleRewardSettlementService.settleIfReady`를 호출하는 공용 lifecycle 연결부 구현.
3. 같은 battle reward가 network/debug/AI 경로 중 어느 곳에서 terminal transition이 발생해도 정확히 한 번 저장되는 integration test.
4. DEBUG_ONLY progression inspection/action commands 또는 동등한 자동 server integration path로 `new save → battle → reward → level → ascend → save/reload`를 검증.
5. 실제 서버 save/reload 또는 재접속 검증 후 M4 full PASS 선언.

주의: debug single-mob encounter에 임의 RewardTable을 하드코딩해서 M4를 통과한 척하지 않는다. authored Encounter metadata가 reward source가 되어야 한다.

## 7. 코드 위생 원칙
매 배치에서 다음을 같이 검사한다.
- test-only production bypass 금지.
- deprecated compatibility constructor 누적 금지.
- 무효/미사용 data field 방치 금지.
- debug fixture가 production balance/canon이 되지 않게 분리.
- 실패 테스트를 삭제하거나 완화해서 통과시키지 않음.
- 다른 모노레포 프로젝트의 concurrent `main` 변경을 force push로 덮지 않음.
- 기능 교체 시 옛 호출부/테스트/리소스까지 제거.

## 8. 디자인/비주얼 Gate
계속 유지한다.
- M4 완료 전 production UI/캐릭터 외형/VFX/월드 미술을 임의 제작하지 않는다.
- M5 시작 시 반드시 `AGENT_RULES.md`, `06_UI_UX_PRESENTATION.md`, 공용 `QUALITY_STANDARD.md`를 다시 읽는다.
- 외부 실제 게임 UI/캐릭터/스킬 연출 레퍼런스 → 분석 → design tokens/information hierarchy → mockup → Minecraft 구현 → 실제 화면 비교 순서를 지킨다.
- AI가 상상으로 generic RPG UI를 즉석 제작하지 않는다.

## 9. 다음 세션의 정확한 시작점
1. 현재 GitHub `main` HEAD 재확인.
2. `AGENTS.md`, `AGENT_RULES.md`, 공용 BUILD/QUALITY STANDARD, 이 문서 읽기.
3. 마지막 TURNBOUND 검증 커밋 `a1d46e...`가 현재 main ancestry에 포함되는지 확인.
4. M4 남은 authored Encounter → reward context → one-shot persisted settlement 연결 구현.
5. 자동 integration test + clean build + JAR verify.
6. M4 full automated gate가 닫히면 M5 Visual Gate 준비로 이동하되, 외부 reference 조사/목업 전에 production UI를 만들지 않는다.

사용자에게 중간 JAR 테스트를 요구하지 않는다. 전체적으로 한 번에 검토할 만한 완성도까지 계속 개발하고, 최종 테스트에서 피드백 받은 부분은 옛 코드/리소스 잔재 없이 교체한다.
