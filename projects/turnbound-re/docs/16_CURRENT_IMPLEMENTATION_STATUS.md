# 16 — CURRENT IMPLEMENTATION STATUS

최종 갱신: 2026-09-08

이 문서는 TURNBOUND: RE의 현재 구현 상태와 다음 실제 개발 시작점을 기록하는 개발 인수인계 정본이다.
기획 정본을 대체하지 않는다. CANON/세부 규칙은 기존 문서가 우선하며, 이 문서는 "어디까지 구현/검증됐는가"만 기록한다.

## 1. 마지막 검증 기준

- 마지막 TURNBOUND: RE 코드 검증 커밋: `24f203db728d7d56b64602f67af76d0fbf715bb8`
- GitHub Actions: `Build turnbound-re` run `34187037774`
- 결과: **SUCCESS**
- 포함 검증: Java 25 toolchain, dependency resolution, `clean build`, 전체 JUnit, production JAR verify, artifact upload.
- 검증 JAR: `turnbound_re-0.1.0-alpha.1.jar`
- SHA-256: `adc1dc812d980f990d14ce343f338482b87fe0a9c7d078d21c7b6923032ed92b`

위 검증에는 M0~M4 회귀와 현재 M5 Battle HUD 구조/interaction/readability, Party Formation structure/authority, Character Overview/Skills/Growth presentation + server-authoritative growth write, selected-character adaptive 3D entity preview, Battle Result/Reward terminal lifecycle 자동 계약이 포함된다.
현재 공용 play-phase presentation protocol은 `v9`이며, 기존 battle target/progression/growth/visual catalog에 server-authored Battle Result / ACK / close 흐름이 추가되었다.

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
상태: **BATTLE UI + PARTY FORMATION + CHARACTER DETAIL/GROWTH + 3D PREVIEW + BATTLE RESULT/REWARD AUTO GATE PASS / VISUAL SCREENSHOT AUDIT PENDING**

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
- `MOONSEUNGJUN_MINECRAFT_HIGH_QUALITY_PLAYBOOK`의 UI 원칙과 현재 M5 방향성을 재대조: 정보구조 → interaction → 실제 화면 → screenshot audit 순서를 유지하고 즉흥 AI 임시 UI를 최종 디자인으로 굳히지 않음.

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

### 8.4 Protocol v9 / authoritative target + progression + growth + visual catalog + result
완료:
- play-phase presentation protocol `v9`.
- 기존 battle target presentation v5 계약 유지.
- `SnapshotAction.eligibleTargetIds`는 서버가 직접 계산.
- 후보 순서는 `participantOrdinal` 기준 안정 순서.
- `SnapshotParticipant.entityId`는 `BattleManager`의 authoritative participant↔entity binding에서만 옴.
- client가 주변 Mob을 검색해 target 후보를 추측하지 않음.
- server entity UUID가 없는 participant는 world marker를 임의 생성하지 않음.
- world marker `TARGET / FOCUS / SELECTED` + EN/KO localization.
- world marker와 target chooser slot의 동일 authoritative `#N` 번호.
- marker state는 `battleId + revision`에 귀속되어 stale snapshot에서 무효화.
- selected marker가 hovered marker보다 우선.
- progression presentation용 `RequestProgressC2S`, `SetPartyC2S`, `ProgressSnapshotS2C` 유지.
- progression snapshot은 current `DefinitionRegistry` + persisted `PlayerProgress`에서 서버가 작성.
- owned 캐릭터 표시 스탯은 canonical `ProgressionRules.stats()` 사용.
- `ActionView`, `EffectView`, `StatsView`, `CostView`, `GrowthView`, `GrowthC2S` 유지.
- Skills facts는 current `ActionDefinition`에서 서버가 publish.
- Growth current→next stats/cost/disabled reason은 current persisted progress + canonical `ProgressionRules`로 서버가 publish.
- `CharacterPresentationNetworkPayloads.CatalogS2C` 유지.
- visual catalog는 current `DefinitionRegistry`의 `CharacterDefinition.sourceEntity`에서 서버가 작성.
- progression state reply에서 visual catalog와 dynamic snapshot이 같은 captured registry를 사용.
- client character→entity hardcode / 주변 entity 추측 금지.
- `BattleResultNetworkPayloads.ResultS2C`, `AcknowledgeResultC2S`, `ResultClosedS2C` 추가.
- terminal result의 outcome/reward delta/total은 server-authored fact만 사용.
- client result screen은 reward roll을 재현하거나 battle cleanup을 직접 수행하지 않음.

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

### 8.8 Party Formation skeleton / progression authority
완료:
- canonical structural mockup의 `Roster → Active 4 slots → Selected detail` 실제 production Screen 구현.
- 기본 `O` configurable key + 별도 menu category.
- 화면 진입 시 client progression cache 초기화 후 서버 최신 snapshot 요청.
- snapshot 도착 전 loading state.
- roster는 current registry의 owned + locked 캐릭터를 표시하고 pagination 지원.
- owned 우선, 별/레벨/Squad Cost를 한 줄에서 스캔 가능.
- locked 캐릭터는 보이지만 assignment 불가.
- active party 4슬롯.
- 선택 캐릭터를 slot 클릭으로 배치.
- 이미 존재하는 캐릭터 재배치는 duplicate 대신 swap.
- Remove / Reset / Apply / Done.
- header에 live Squad Cost / capacity.
- over-capacity 즉시 warning + Apply 비활성.
- selected detail에 별/레벨/역할/HP/ATK/DEF/SPD/Poise/affinity/Basic/Skills/Burst 표시.
- client `PartyFormationDraft`는 temporary preview만 담당.
- `SetPartyC2S`는 `expectedParty + requestedParty`를 함께 보내 stale overwrite 방지.
- server persisted party가 expected와 다르면 `STALE_PARTY`로 거부 후 최신 snapshot 반환.
- 실제 저장은 기존 `PlayerProgressStore.setParty()` / `ProgressionService.setParty()` 경로 사용.
- server가 max4 / unique / owned / Squad Cost를 최종 재검증.
- 성공/실패 후 항상 fresh authoritative snapshot으로 reconciliation.
- 480×270 / 640×360 / 1280×720 / 1920×1080 Party Formation layout contract.
- wide root max width 960으로 과도한 scan distance 억제.
- EN/KO interaction/result/role/affinity localization + exact key parity.
- 상세 정본: `17C_M5_PARTY_FORMATION_SKELETON_GATE.md`.

### 8.9 Character Overview / Skills / Growth + write path
완료:
- selected character context를 유지하는 `Overview / Skills / Growth` 실제 interactive tabs.
- tab state는 client presentation-only state.
- Overview: current star/origin star, level/cap, roles, cost, HP/ATK/DEF/SPD/Poise, affinity, Basic/Skills/Burst/Passive.
- Skills: server-published action kind, Energy delta, HP/Poise power, damage tag, target team/shape/count, special effects.
- client가 전투 damage 결과나 target legality를 새로 계산하지 않음.
- Growth: current Coin/Essence/shard balance, Level current→next, next stats, level cost, Star current→next, next cap, post-ascension stats, ascension cost, disabled reason.
- 비용/결과를 버튼과 같은 detail 영역에서 사전 표시하여 surprise cost 방지.
- Level Up / Ascend 실제 버튼.
- `GrowthC2S`는 operation + character id + expected star + expected level을 전송.
- server가 ownership 및 expected star/level을 재확인하여 stale write 거부.
- 실제 mutation은 기존 `PlayerProgressStore.levelUp/ascend` → `ProgressionService` 경로만 사용.
- 비용/레벨캡/별캡/조각 조건은 서버가 최종 재검증.
- 성공/실패 뒤 fresh progression snapshot으로 즉시 reconciliation.
- EN/KO 탭/skill kind/effect/growth cost/disabled/result copy + exact key parity.
- 상세 정본: `17D_M5_CHARACTER_DETAIL_GROWTH_GATE.md`.

### 8.10 Selected-character adaptive 3D entity preview
완료:
- Overview tab에 selected character의 실제 Minecraft `LivingEntity` preview 추가.
- entity source는 client hardcode가 아니라 server-published `CharacterDefinition.sourceEntity`.
- `BuiltInRegistries.ENTITY_TYPE` resolve 후 presentation-only entity를 생성하고 실제 world에는 spawn하지 않음.
- NeoForge 26.2 GUI extraction 구조의 `InventoryScreen.renderEntityInInventoryFollowsAngle(...)` 재사용.
- 현재 `ClientLevel + sourceEntity` 조합 기준 preview entity cache.
- 매 frame entity 재생성 금지.
- source/world 변경 시 resolve 갱신, screen removal 시 cache clear.
- Overview에서만 preview를 보여 Skills/Growth 정보 폭은 보존.
- selected-detail region이 작으면 preview를 숨기고 text width를 100% 보존.
- normal region에서는 오른쪽 preview + 왼쪽 최소 readable text width를 함께 확보.
- entity type width/height 기반 자동 scale fit.
- Zombie/Enderman/Iron Golem/Spider 계열 대표 체형 자동 계약.
- malformed/unregistered/non-Living source는 debug text 없이 preview만 fail closed.
- 상세 정본: `17E_M5_CHARACTER_ENTITY_PREVIEW_GATE.md`.

### 8.11 Battle Result / Reward terminal presentation
완료:
- `BattleResultPresentationService`가 terminal battle presentation을 서버에서 소유.
- authored VICTORY는 `BattleRewardSettlementService` persistence 성공 뒤 실제 `RewardGrant + resulting PlayerProgress`를 result source로 사용.
- persistence 실패 중에는 reward claim이 소비되지 않으며 result UI도 열지 않고 다음 tick retry.
- DEFEAT / rewardless debug terminal은 임의 reward를 생성하지 않고 outcome + no-reward 상태만 publish.
- `ResultS2C`에 outcome / Coin delta+total / Essence delta+total / shard delta+total을 포함.
- `BattleResultClientState`는 presentation cache만 담당.
- `BattleResultClientEvents`가 새 result를 한 번만 `BattleResultScreen`으로 연다.
- result screen은 non-pausing compact overlay이며 world를 완전히 가리지 않음.
- Continue 또는 ESC는 `AcknowledgeResultC2S`를 보냄.
- server가 owner / published state / battleId / revision / outcome / terminal phase를 재검증.
- 검증 성공 시에만 `battle.cleanup()` + `BattleManager.cleanup()` 실행.
- `ResultClosedS2C` 성공 뒤 client result state를 제거하고 월드로 복귀.
- entity↔battle binding은 result acknowledgement 전까지 유지.
- minimum 480×270 / root max 520×230 / reward region >=160 / 320×180 fail-closed.
- EN/KO result/reward/close feedback + exact key parity.
- 첫 layout gate에서 480×270 reward 영역 152px 문제가 발견되어 test를 낮추지 않고 production max height를 220→230으로 수정.
- 상세 정본: `17F_M5_BATTLE_RESULT_REWARD_GATE.md`.

### 8.12 현재 자동 검증 범위
Battle UI:
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
- current Enemy Intent enum translation coverage.
- current 12 core status translation coverage.
- compact/normal party layout contracts.

Party / Character / Growth / Preview:
- 480×270 / 640×360 / 1280×720 / 1920×1080 Party Formation bounds.
- roster / active / selected region non-overlap.
- minimum roster/active/detail readable widths.
- wide root max width 960.
- unsupported 320×180 guard.
- progression snapshot payload round-trip.
- `expectedParty/requestedParty` set-party wire round-trip.
- owned/locked presentation facts.
- swap/remove draft behavior.
- over-capacity submit rejection.
- unowned submit rejection.
- `GrowthC2S` operation/id/expectedStar/expectedLevel round-trip.
- nested `ActionView`/`EffectView` snapshot round-trip.
- `GrowthView` stats/cost/block-state round-trip.
- blocked growth enable-state contracts.
- EN/KO exact key parity.
- Party role/affinity required copy.
- Character Detail/Skills/Growth required copy.
- server-published character→sourceEntity visual catalog round-trip.
- duplicate visual catalog id rejection.
- compact preview hide/text-width preservation.
- normal preview bounded layout + minimum text width.
- Zombie-like / Enderman-like / Iron-Golem-like / Spider-like body fit.
- invalid body dimensions fail closed.

Battle Result / Reward:
- authoritative result payload exact round-trip.
- Coin/Essence/shard delta + total fact preservation.
- rewardless DEFEAT explicit state.
- acknowledgement / close response round-trip.
- invalid outcome / impossible shard totals rejection.
- terminal battle registry.
- result ACK 전 entity binding 유지.
- cleanup 후 terminal registry/entity binding 제거.
- 480×270 / 640×360 / 1280×720 / 1920×1080 result bounds.
- result reward region minimum 160px.
- unsupported 320×180 guard.
- EN/KO result copy + exact key parity.

첫 Party Formation CI에서 발견 후 수정된 API 적응 오류:
- `CustomPacketPayload.type()` 3개 누락.
- 존재하지 않는 `minecraft.gui.getScreen()` 호출.
- 존재하지 않는 `ServerPlayer.getServer()` 호출 2개.
- 테스트를 완화하지 않고 실제 NeoForge 26.2 API에 맞게 수정.

Battle Result 첫 layout gate에서 발견 후 수정:
- Run `34186812650`에서 compile은 성공했으나 최소 480×270 result reward region이 152px로 계약 실패.
- assertion 완화 없이 production `MAX_ROOT_HEIGHT`를 220→230으로 수정.
- Run `34186892199` SUCCESS.

마지막 자동 검증:
- commit `24f203db728d7d56b64602f67af76d0fbf715bb8`
- `Build turnbound-re` Run `34187037774`
- Java Temurin 25.0.4+1.
- Gradle 9.2.1.
- NeoForge 26.2.0.38-beta.
- clean build/JUnit: **PASS**
- production JAR verify: **PASS**
- artifact upload: **PASS**
- JAR SHA-256: `adc1dc812d980f990d14ce343f338482b87fe0a9c7d078d21c7b6923032ed92b`

### 8.13 아직 PASS가 아닌 것
- 실제 Minecraft implementation screenshot quality.
- 실제 GUI Scale 옵션별 clipping/가독성/시선 이동 체감.
- 480×270에서 Skills/Growth/Result 실제 정보밀도와 텍스트 clipping 체감.
- final production sprite/icon/frame asset quality 및 source/license 기록.
- animation/transition/reward reveal timing 체감.
- 실제 Zombie/Enderman/Iron Golem/Spider preview의 중심/pose/시선/clipping 체감.
- Battle Result의 최종 frame/icon/motion/audio presentation quality.
- 캐릭터 외형 / skill VFX / world visual gate.
- 실제 client Level Up / Ascend 이후 save/reconnect persistence 체감.
- 실제 VICTORY/DEFEAT → result → ACK → world 복귀 runtime 체감.

중요:
- Battle UI, Party Formation, Character Detail/Growth, 3D Preview, Battle Result/Reward 자동 gate가 통과했다고 M5 전체 production visual PASS가 된 것은 아니다.
- 실제 Minecraft screenshot을 reference/mockup과 비교하기 전 **production visual PASS를 선언하지 않는다.**
- 사용자 방침상 지금 중간 JAR 테스트를 요구하지 않고 전체적인 integrated test 시 함께 확인한다.

## 9. 다음 실제 개발 시작점

1. 현재 GitHub `main` HEAD를 다시 조회한다. 위 검증 SHA를 최신 main이라고 가정하지 않는다.
2. `16_CURRENT_IMPLEMENTATION_STATUS.md`, `17_M5_UI_VISUAL_GATE.md`, `17A_M5_COMMAND_OVERLAY_LIFECYCLE_GATE.md`, `17B_M5_HUD_READABILITY_GATE.md`, `17C_M5_PARTY_FORMATION_SKELETON_GATE.md`, `17D_M5_CHARACTER_DETAIL_GROWTH_GATE.md`, `17E_M5_CHARACTER_ENTITY_PREVIEW_GATE.md`, `17F_M5_BATTLE_RESULT_REWARD_GATE.md`, `06_UI_UX_PRESENTATION.md`, `08_REFERENCE_CATALOG.md`를 확인한다.
3. Battle HUD 구조/interaction/readability, Party Formation authority, Character Detail/Growth write, 3D preview, Battle Result/Reward terminal authority의 자동 implementation gate는 닫힌 것으로 취급한다. 실제 screenshot visual QA는 integrated client test까지 **pending**으로 남긴다.
4. 다음 production 작업은 새로운 generic UI Java를 더 만드는 것이 아니라 **M5 전체 visual asset/reference + screenshot-ready polish**다.
   - Battle HUD / Command / Party / Character / Growth / Result 간 정보계층·spacing·상태 강조·시각 언어를 다시 비교한다.
   - production frame/icon/sprite 후보는 외부 reference/source/license를 확인하고 기록한 뒤 적용한다.
   - motion/transition/reward reveal/audio feedback을 설계하되 world visibility와 Minecraft 조작성을 해치지 않는다.
   - 즉흥적인 검은 반투명 카드/임의 컬러 border를 최종 디자인으로 굳히지 않는다.
5. visual presentation 기반이 충분히 통일되면 character exterior / skill VFX / world visual gate로 이동한다. 기존 `CharacterDefinition.sourceEntity`와 data-driven character/action 정본을 기준으로 하며 구 TURNBOUND 디자인을 계승하지 않는다.
6. 전체적으로 사용자가 한 번에 검토할 가치가 있는 상태가 되면 실제 Minecraft에서 통합 검증한다.
   - Battle / Party / Growth / Result GUI Scale 및 1280×720 / 1920×1080 screenshot audit.
   - M2 실제 client 20 encounter gate.
   - M4 world save/reconnect persistence gate.
   - VICTORY/DEFEAT → result → server ACK → world 복귀.
7. screenshot/reference 비교 전 M5 production visual PASS를 선언하지 않는다.

구 TURNBOUND는 계속 ZERO AUTHORITY다. UI/코드/수치/디자인을 구 프로젝트에서 자동 계승하지 않는다.
