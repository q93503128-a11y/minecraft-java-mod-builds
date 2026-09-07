# 16 — CURRENT IMPLEMENTATION STATUS

최종 갱신: 2026-09-08

이 문서는 TURNBOUND: RE의 현재 구현 상태와 다음 실제 개발 시작점을 기록하는 개발 인수인계 정본이다.
기획 정본을 대체하지 않는다. CANON/세부 규칙은 기존 문서가 우선하며, 이 문서는 "어디까지 구현/검증됐는가"만 기록한다.

## 1. 마지막 검증 기준

- 마지막 TURNBOUND: RE 코드 검증 커밋: `a367e3e1c8678b987c1a0714a807f579bf577135`
- GitHub Actions: `Build turnbound-re` run `34171544704`
- 결과: **SUCCESS**
- 포함 검증: Java 25 toolchain, dependency resolution, `clean build`, 전체 JUnit, production JAR verify, artifact upload.
- 검증 JAR: `turnbound_re-0.1.0-alpha.1.jar`
- SHA-256: `7b78790ce0825d9b55055e91ac9168068badcfb2e7e9e1a7661faa088b45c6ba`

위 검증에는 M0~M4 회귀와 현재 M5 Battle HUD 구조/interaction/readability 자동 계약이 포함된다.
현재 battle network presentation protocol은 `v5`이며, server-published eligible targets와 participant→entity UUID binding, world marker, command-strip target chooser, overlay lifecycle, localized HUD, 최소 logical canvas compact party layout까지 검증되었다.

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
상태: **BATTLE UI STRUCTURE + INTERACTION + READABILITY AUTO GATE PASS / VISUAL SCREENSHOT AUDIT PENDING**

### 8.1 Visual gate / 방향성
완료:
- 공용 `QUALITY_STANDARD.md`와 `AGENT_RULES.md` 재확인.
- Persona 5 Royal, OCTOPATH TRAVELER II, Honkai: Star Rail, Metaphor: ReFantazio, Slay the Spire, Darkest Dungeon II, Pokémon Scarlet/Violet, Into the Breach, Clair Obscur 등 상용 사례 비교.
- Cobblemon, Cobblemon Extended Battle UI, TurnBasedMinecraftMod, FTB/Questify 계열 Minecraft UI 사례 비교.
- proprietary UI art를 복제하지 않고 문제/원리/변환만 기록.
- 선택 방향: **Minecraft-native tactical overlay**.
- `08_REFERENCE_CATALOG.md`, `17_M5_UI_VISUAL_GATE.md` 정본화.
- Battle HUD 및 Party/Character/Growth information hierarchy 확정.
- spacing/surface/typography/icon/motion semantic token contract 확정.
- Battle HUD / Party Formation structural mockup 확정.
- 첫 production pass는 Vanilla/NeoForge GUI를 사용하고 대형 UI dependency를 추가하지 않음.

### 8.2 Production Battle HUD
완료:
- `BattleClientState` authoritative snapshot/event client cache.
- read-only `BattlePresentationModel`.
- 좌측 turn rail.
- enemy HP / Poise / Intent / EXPOSED.
- 하단 party HP / Energy / status.
- server-published command strip.
- `VanillaGuiLayers.HOTBAR` 위에 등록하여 chat/title/subtitle를 무작정 덮지 않음.
- 중앙 Minecraft world viewport를 상시 가리는 대형 panel 없음.
- 첫 구조 검증 자산은 Minecraft 자체 advancement frame/title box/boss bar sprite 사용.
- 옛 `DebugBattleHud` 삭제.

### 8.3 Command input / action presentation
완료:
- configurable `KeyMapping`, 기본 `B`, Minecraft Controls에서 재지정 가능.
- 실제 설정된 key name을 HUD에 표시.
- non-pausing/in-game `BattleCommandScreen`.
- 서버가 publish한 action만 선택 가능.
- Basic / Skill / Guard / Burst action picker.
- action concise identity + Energy cost.
- action tooltip: name / Energy / HP power / Poise power / damage tag / target rule.
- `ENERGY`, `TARGETS` disabled reason을 EN/KO player-facing copy로 표시.
- submit은 `BattleCommandC2S`; 최종 legality는 server strict gate가 재검증.

### 8.4 Protocol v5 / authoritative target presentation
완료:
- battle presentation network protocol `v5`.
- `SnapshotAction.eligibleTargetIds`는 서버가 직접 계산.
- 후보 순서는 `participantOrdinal` 기준 안정 순서.
- `SnapshotParticipant.entityId`는 `BattleManager`의 authoritative participant↔entity binding에서만 옴.
- client가 주변 Mob을 검색해 target 후보를 추측하지 않음.
- server entity UUID가 없는 participant는 world marker를 임의 생성하지 않음.
- world marker `TARGET / FOCUS / SELECTED` + EN/KO localization.
- world marker와 target chooser slot의 동일 authoritative `#N` 번호.
- marker state는 `battleId + revision`에 귀속되어 stale snapshot에서 무효화.
- selected marker가 hovered marker보다 우선.

### 8.5 World-first target chooser
완료:
- 중앙 target modal 제거.
- action 선택 후 기존 하단 우측 `commandStrip` 자체가 target chooser로 전환.
- single-target 즉시 실행.
- multi-target 선택/해제/confirm.
- cancel / pagination.
- target chooser가 reserved world viewport를 침범하지 않는 자동 layout 계약.

### 8.6 Command overlay lifecycle
완료:
- `BattleCommandOverlayState`는 presentation-only open state만 보관.
- interactive command screen이 열리면 passive HUD command strip만 숨김.
- turn rail/enemy/party 정보는 계속 보임.
- screen 종료/ESC/교체/command submit 시 passive HUD 복귀.
- `BattleCommandScreen.removed()`에서 overlay state + world target marker를 반드시 clear.
- passive/interactive command UI 중복 렌더 제거.
- 상세 정본: `17A_M5_COMMAND_OVERLAY_LIFECYCLE_GATE.md`.

### 8.7 HUD readability / localization
완료:
- `BattleHudPresentation`으로 HUD player-facing copy 중앙화.
- `NOW / INTENT / BREAK CANCEL / DEFEATED` 등 코드 내부 영문을 HUD에 직접 박던 경로 제거.
- Enemy Intent Risk 3종 / Type 5종 / Targeting 4종 EN/KO mapping.
- authored core status 12종 EN/KO mapping.
- status stack / remaining turns EN/KO presentation.
- unknown future status는 의미를 발명하지 않고 humanized id fallback.
- `en_us` / `ko_kr` exact key parity 자동 검증.
- 480×270에서 4인 party를 2×2 compact grid로 전환.
- 최소 compact cell width >=120 / height >=40 자동 계약.
- minimum reserved world viewport >=300×96 자동 계약.
- 640×360 이상 normal 조건에서는 4인 party 한 줄 유지.
- 상세 정본: `17B_M5_HUD_READABILITY_GATE.md`.

### 8.8 현재 자동 검증 범위
- 480×270 minimum supported logical canvas.
- 640×360 narrow layout.
- 1280×720.
- 1920×1080.
- unsupported 320×180 guard.
- commandStrip/target chooser region reuse.
- target chooser world viewport non-invasion.
- authoritative target order / world marker numbering.
- stale marker rejection.
- null entity binding marker rejection.
- EN/KO key parity.
- current Enemy Intent enum translation coverage.
- current 12 core status translation coverage.
- compact/normal party layout contracts.

마지막 자동 검증:
- commit `a367e3e1c8678b987c1a0714a807f579bf577135`
- `Build turnbound-re` Run `34171544704`
- clean build/JUnit: **PASS**
- production JAR verify: **PASS**
- artifact upload: **PASS**
- JAR SHA-256: `7b78790ce0825d9b55055e91ac9168068badcfb2e7e9e1a7661faa088b45c6ba`

### 8.9 아직 PASS가 아닌 것
- 실제 Minecraft implementation screenshot quality.
- 실제 GUI Scale 옵션별 clipping/가독성/시선 이동 체감.
- final production sprite/icon/frame asset quality 및 source/license 기록.
- animation/transition timing 체감.
- Party Formation production Screen.
- Character Overview / Skills / Growth production Screen.
- battle result/reward transition의 최종 presentation.
- 캐릭터 외형 / skill VFX / world visual gate.

중요:
- Battle UI 자동 gate가 통과했다고 M5 전체 production visual PASS가 된 것은 아니다.
- 실제 Minecraft screenshot을 reference/mockup과 비교하기 전 **production visual PASS를 선언하지 않는다.**
- 사용자 방침상 지금 중간 JAR 테스트를 요구하지 않고 전체적인 integrated test 시 함께 확인한다.

## 9. 다음 실제 개발 시작점

1. 현재 GitHub `main` HEAD를 다시 조회한다. 위 검증 SHA를 최신 main이라고 가정하지 않는다.
2. `16_CURRENT_IMPLEMENTATION_STATUS.md`, `17_M5_UI_VISUAL_GATE.md`, `17A_M5_COMMAND_OVERLAY_LIFECYCLE_GATE.md`, `17B_M5_HUD_READABILITY_GATE.md`, `06_UI_UX_PRESENTATION.md`, `08_REFERENCE_CATALOG.md`를 확인한다.
3. Battle HUD의 자동 구조/interaction/readability gate는 닫힌 것으로 취급한다. 실제 screenshot visual QA는 integrated client test까지 **pending**으로 남긴다.
4. 다음 production UI 개발은 정본 structural mockup을 따라 **Party Formation Screen skeleton**으로 이동한다.
   - M4의 persisted `PlayerProgress` / active party / unlocked characters / Squad Cost를 source로 사용.
   - client가 progression truth를 임의 계산하지 않도록 server-authoritative read/write 경계를 먼저 확정.
   - roster / active party / selected detail 영역을 structural mockup 기준으로 구현.
   - Squad Cost 초과 이유가 조합 시점에 명확히 보이게 한다.
5. 이어서 같은 character context를 유지하는 Character Overview → Skills → Growth detail pane을 구현한다.
6. production sprite/icon asset은 reference/source/license gate 후 적용한다.
7. 전체적으로 검토할 만한 상태가 되면 실제 Minecraft에서 Battle/Party/Growth GUI scale 및 screenshot audit, M2 20회 client gate, M4 save/reconnect gate를 한 번에 수행한다.

구 TURNBOUND는 계속 ZERO AUTHORITY다. UI/코드/수치/디자인을 구 프로젝트에서 자동 계승하지 않는다.
