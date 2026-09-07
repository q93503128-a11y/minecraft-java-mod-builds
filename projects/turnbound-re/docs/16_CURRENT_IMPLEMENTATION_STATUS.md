# 16 — CURRENT IMPLEMENTATION STATUS

최종 갱신: 2026-09-07

이 문서는 TURNBOUND: RE의 현재 구현 상태와 다음 실제 개발 시작점을 기록하는 개발 인수인계 정본이다.
기획 정본을 대체하지 않는다. CANON/세부 규칙은 기존 문서가 우선하며, 이 문서는 "어디까지 구현/검증됐는가"만 기록한다.

## 1. 마지막 검증 기준
- 마지막 TURNBOUND: RE 검증 커밋: `9544e30487bf9d9025f5e5d258fca67784a92f28`
- GitHub Actions: `Build turnbound-re` run `34075982400`
- 결과: **SUCCESS**
- 포함 검증: Java 25 toolchain, dependency resolution, `clean build`, 전체 JUnit, production JAR verify, artifact upload.
- 검증 JAR: `turnbound_re-0.1.0-alpha.1.jar`
- SHA-256: `5133b0f38f618402b9e2abe49021d13e0b489a1ecbafed581c95a4a06c7da505`

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
- normal + elite Encounter/Reward tables.

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
상태: **FULL AUTOMATED PASS / RUNTIME SAVE-RECONNECT MANUAL CHECK DEFERRED**

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
- authored `EncounterDefinition`을 실제 전투로 여는 `AuthoredEncounterLauncher` 구현.
- launcher는 한 definition snapshot에서 Encounter, enemy level/currentStar, participant CharacterDefinition, RewardTableDefinition을 함께 캡처.
- `BattleRewardContext`는 owner/rewardSeed뿐 아니라 immutable `RewardTableDefinition` 자체를 캡처하여 전투 중 `/reload`가 이미 열린 Encounter의 보상을 바꾸지 못함.
- reward context가 captured battle definition snapshot 밖의 table을 참조하면 registration 단계에서 거부.
- `BattleManager.claimVictoryReward`는 VICTORY + REWARD에서만 단 한 번 claim 가능.
- reward callback/persistence가 실패하면 claim은 소모되지 않아 retry 가능.
- cleanup 시 reward metadata/claim state 제거.
- `BattleRewardSettlementService`가 captured RewardTableDefinition을 persisted `PlayerProgressStore.applyReward`로 연결.
- `BattleRewardLifecycleHooks`가 server tick의 공용 terminal lifecycle에서 reward-ready battle만 정산. player network action/enemy resolution/status 처리 중 어느 경로에서 승리가 발생해도 보상 지급 코드를 복제하지 않음.
- persistence 예외는 claim을 태우지 않고 pending 상태를 유지하여 다음 tick에서 재시도 가능.

통합 검증:
- production `vertical_encounters.json`의 authored `turnbound_re:debug_overworld_patrol` 직접 로드.
- authored Encounter → battle open → 실제 data action/network gateway → enemy intent/action executor → VICTORY → REWARD.
- Encounter metadata의 rewardTable이 실제 reward source임을 검증.
- deterministic reward roll → one-shot claim → duplicate claim 불가.
- reward 적용 후 `TurnboundProgressSavedData.CODEC` round-trip.
- unlock → level up → level cap 거부 → ascend → party/Squad Cost → save/reload equivalent round-trip.
- 잘못된 enemy world-binding count와 snapshot 밖 reward table은 fail-fast.
- 기존 M0~M3 및 M4 전체 JUnit + clean build + production JAR verify 회귀 통과.

남은 것은 자동 M4 blocker가 아니다:
- 실제 Minecraft runtime에서 world save/reload 또는 재접속을 통한 최종 persistence 체감 검증.
- 사용자 방침상 이 수동 검증은 M2 client 20회 gate와 함께 최종 완성본 테스트 시 수행한다.

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
M4가 닫혔으므로 다음 단계는 **M5 Production UI/Presentation Gate 준비**다.

그러나 production UI Java 코드를 바로 만들지 않는다.
반드시 다음 순서를 먼저 완료한다.
1. `AGENT_RULES.md`, `06_UI_UX_PRESENTATION.md`, 공용 `QUALITY_STANDARD.md` 재확인.
2. 실제 우수 턴제 RPG UI 다수 조사.
3. 실제 Minecraft UI/모드 구현 사례 조사.
4. `08_REFERENCE_CATALOG.md`에 채택/금지 원리와 출처 보강.
5. 화면별 information hierarchy 확정.
6. design tokens 확정.
7. mockup 작성 및 정본화.
8. 그 뒤에만 production HUD/menu 구현.
9. 실제 Minecraft 화면을 reference/mockup과 비교해 반복 수정.

AI가 상상으로 generic RPG UI를 즉석 제작하지 않는다.

## 9. 다음 세션의 정확한 시작점
1. 현재 GitHub `main` HEAD 재확인.
2. 공용 `QUALITY_STANDARD.md`, `AGENT_RULES.md`, `06_UI_UX_PRESENTATION.md`, `08_REFERENCE_CATALOG.md`, 이 문서 읽기.
3. 마지막 TURNBOUND 검증 커밋 `9544e304...`가 현재 main ancestry에 포함되는지 확인.
4. M5 Visual Gate 선행 연구부터 진행: 턴제 RPG UI + Minecraft 구현 사례를 여러 reference로 비교.
5. reference catalog → information hierarchy → design tokens → mockup 순으로 정본 갱신.
6. mockup gate가 닫히기 전 production UI Java/최종 visual asset을 만들지 않는다.

사용자에게 중간 JAR 테스트를 요구하지 않는다. 전체적으로 한 번에 검토할 만한 완성도까지 계속 개발하고, 최종 테스트에서 피드백 받은 부분은 옛 코드/리소스 잔재 없이 교체한다.
