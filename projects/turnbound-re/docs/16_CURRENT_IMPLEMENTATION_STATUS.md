# 16 — CURRENT IMPLEMENTATION STATUS

최종 갱신: 2026-09-07

이 문서는 TURNBOUND: RE의 현재 구현 상태와 다음 실제 개발 시작점을 기록하는 개발 인수인계 정본이다.
기획 정본을 대체하지 않는다. CANON/세부 규칙은 기존 문서가 우선하며, 이 문서는 "어디까지 구현/검증됐는가"만 기록한다.

## 1. 마지막 검증 기준
- 마지막 TURNBOUND: RE 코드 검증 커밋: `744043f6ed1111b146593336f4116616eb59268f`
- GitHub Actions: `Build turnbound-re` run `34103168177`
- 결과: **SUCCESS**
- 포함 검증: Java 25 toolchain, dependency resolution, `clean build`, 전체 JUnit, production JAR verify, artifact upload.
- 검증 JAR: `turnbound_re-0.1.0-alpha.1.jar`
- SHA-256: `5a7c16256693066800d07ff44c1d61fa70e9d5082837243dfa7b43df3d7163b6`

위 검증에는 M0~M4 회귀와 M5 production Battle HUD + 실제 command input/target selection/network protocol v4 계약이 포함된다.
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
- DEBUG_ONLY client inspection state.
- 20-cycle automated encounter/cleanup soak with orphan battle 0.
- M5에서 production client presentation cache/HUD로 승격하면서 옛 `DebugBattleHud`는 제거하고 `DebugBattleClientState`만 production cache를 읽는 inspection facade로 축소.

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

## 8. M5 — Production UI / Presentation
상태: **PRODUCTION BATTLE HUD + COMMAND INPUT AUTOMATED PASS / VISUAL SCREENSHOT AUDIT PENDING**

완료된 visual gate:
- 공용 `QUALITY_STANDARD.md`와 `AGENT_RULES.md` 재확인.
- Persona 5 Royal, OCTOPATH TRAVELER II, Honkai: Star Rail, Metaphor: ReFantazio, Slay the Spire, Darkest Dungeon II, Pokémon Scarlet/Violet, Into the Breach, Clair Obscur 등 상용 사례 비교.
- Cobblemon, Cobblemon Extended Battle UI, TurnBasedMinecraftMod, FTB/Questify 계열 Minecraft UI 사례 비교.
- proprietary UI art를 복제하지 않고 문제/원리/변환만 기록.
- 선택 방향: **Minecraft-native tactical overlay**.
- `08_REFERENCE_CATALOG.md` M5 UI reference set 보강.
- `17_M5_UI_VISUAL_GATE.md` 생성.
- Battle HUD P0/P1/P2/P3 information hierarchy 확정.
- Party/Character/Growth information hierarchy 확정.
- spacing/surface/typography/icon/motion semantic token contract 확정.
- Battle HUD structural mockup 정본화.
- Party Formation structural mockup 정본화.
- Minecraft 26.2/NeoForge `Screen`, GUI-scale relative layout, `blitSprite`, `nine_slice`, scissor/tooltip feasibility 확인.
- 첫 production pass는 Vanilla/NeoForge GUI를 사용하고 대형 UI dependency를 추가하지 않기로 결정.

production Battle HUD 자동 구현 완료:
- S2C participant snapshot에 `team`, `participantOrdinal`, `characterId`, active statuses, Enemy Intent를 서버 권위 데이터로 제공.
- 현재 player actor에 대해 Basic / Skill 1 / Skill 2 / Guard / Burst를 `SnapshotAction`으로 서버가 제공.
- action마다 Energy cost, target team/shape/count, `usable`, `disabledReason`을 서버가 판정.
- `BattleClientState`가 authoritative snapshot/event의 유일 client cache.
- read-only `BattlePresentationModel`이 현재 actor부터 시작하는 turn rail을 만들되 party slot은 `participantOrdinal` 기준으로 안정적으로 유지.
- `UiLayoutMetrics`가 battle HUD logical layout을 pure helper로 관리하고 지원 불가 초소형 canvas에서는 겹쳐 그리지 않고 HUD를 생략.
- production `BattleHud`가 `VanillaGuiLayers.HOTBAR` 위에 등록되어 chat/title/subtitle를 `AboveAll` 방식으로 덮지 않음.
- 좌측 turn rail, enemy HP/Poise/Intent/EXPOSED, 하단 party HP/Energy/status, server-published command strip을 렌더.
- 중앙 Minecraft world viewport를 상시 가리는 대형 panel을 두지 않음.
- 첫 구조 검증 자산은 Minecraft 자체 advancement frame / title box / boss bar sprite를 사용하며, 임의 AI 제작 최종 frame/icon을 정본으로 굳히지 않음.
- 옛 `DebugBattleHud`는 삭제. duplicate production/debug HUD를 남기지 않음.
- production definition 기반 M5 테스트에서 Skeleton의 5 action slot, Energy 부족 disabled state, 다중 target 부족 disabled state를 검증.
- turn rail 회전과 안정적인 party slot 순서를 회귀 테스트.
- 1280×720 / 1920×1080 / 640×360 layout bounds 자동 검증 및 320×180 unsupported guard 검증.

production command input / target selection 자동 구현 완료:
- battle network protocol을 `v4`로 승격.
- `SnapshotAction.eligibleTargetIds`를 서버가 직접 계산하여 전송. client가 `targetTeam`을 보고 후보를 재계산하지 않음.
- 후보 타깃은 `participantOrdinal` 기준 안정 순서로 publish하여 SPD/initiative 변화 때문에 selection UI가 매 턴 뒤섞이지 않음.
- configurable `KeyMapping` 기반 기본 `B` 키로 전투 명령 화면 진입. Minecraft Controls에서 재지정 가능하도록 구현.
- `BattleCommandScreen`은 non-pausing / in-game UI이며 상시 world viewport를 덮는 메뉴로 쓰지 않음.
- 서버가 publish한 action만 버튼으로 표시하고 `usable=false` action은 선택 불가.
- 단일 타깃, 다중 타깃, 페이지 분할, cancel/confirm 흐름 구현.
- 후보 수와 요구 수가 정확히 같으면 self/강제 다중 타깃을 자동 결정하되, 그 후보 자체는 서버 snapshot에서만 가져옴.
- pure `BattleCommandSelection`이 current snapshot의 action 존재 여부, disabled state, target count, published candidate membership을 검증하고 `BattleCommand`을 생성.
- 실제 제출은 `ClientPacketDistributor.sendToServer(BattleCommandC2S)` 경로.
- 제출 후 legality는 기존 server `BattleNetworkGateway`/`BattleCommandService`가 revision/sender/current actor/action/targets를 다시 검증. client selection은 권위가 아님.
- `M5CommandSelectionTest`에서 서버가 publish하지 않은 같은 ENEMY 팀 participant를 client가 임의 타깃으로 만들 수 없음을 검증.
- production Skeleton + 3 Zombie fixture로 Basic/Guard/Burst candidate와 다중 target command build를 검증.
- `en_us` + `ko_kr` key/screen translation 추가.
- 첫 CI에서 initiative 순서와 고정 slot 순서 기대가 충돌한 것을 발견했고 테스트를 완화하지 않고 production 후보 publish를 `participantOrdinal` 안정 순서로 수정.
- `Build turnbound-re` run `34103168177`: clean build/JUnit + production JAR verify **SUCCESS**.

아직 완료가 아닌 것:
- command strip selected/hover 상태의 HUD 직접 시각 연결.
- action tooltip 및 `ENERGY` / `TARGETS` 등 상세 disabled reason의 사용자 친화 presentation.
- 실제 world/entity target marker 또는 전투 대상과 화면 selection의 더 강한 시각 연결.
- 실제 TURNBOUND production sprite/icon asset 선정·제작/도입과 source/license 기록.
- Party Formation 실제 Screen 구현.
- Character Overview / Skills / Growth 실제 Screen 구현.
- 실제 Minecraft screenshot side-by-side audit.
- 여러 GUI scale/1280×720/1920×1080 실화면 검증.
- 구현 후 visual regression 수정.

중요:
- Battle HUD + command input 자동 gate가 통과했다고 M5 전체가 PASS인 것은 아니다.
- 실제 Minecraft screenshot을 reference/mockup과 비교하기 전 **production visual PASS를 선언하지 않는다.**
- client presentation은 서버 snapshot을 표시하고 선택만 담당한다. combat 결과/소유권/자원/target legality를 client가 권위적으로 재계산하지 않는다.

## 9. 다음 세션의 정확한 시작점
1. 현재 GitHub `main` HEAD 재확인. 위 검증 SHA가 최신 main이라고 가정하지 않는다.
2. `16_CURRENT_IMPLEMENTATION_STATUS.md`, `17_M5_UI_VISUAL_GATE.md`, `06_UI_UX_PRESENTATION.md`, `08_REFERENCE_CATALOG.md` 재확인.
3. M5 battle interaction polish를 이어서 구현:
   - HUD에 configurable command key hint 및 selected/hover/targeting 상태 연결.
   - action tooltip.
   - `ENERGY`, `TARGETS` 등 disabled reason의 localized player-facing 설명.
   - target selection과 실제 participant/world entity의 시각 연결/marker를 현재 binding 구조를 확인한 뒤 구현.
4. pure layout/selection 계약과 서버 권위 경계를 유지하며 JUnit 보강.
5. clean build/JUnit/JAR verify 후 main 반영.
6. 이어서 Party Formation Screen 구현.
7. Character Overview / Skills / Growth Screen 구현.
8. 실제 Minecraft screenshot을 reference/structural mockup과 비교해 visual QA 반복.
9. 사용자에게 중간 JAR 테스트를 요구하지 않고 전체적으로 검토할 만한 완성도까지 계속 개발한다.

구 TURNBOUND는 계속 ZERO AUTHORITY다. UI도 구 프로젝트에서 자동 계승하지 않는다.
