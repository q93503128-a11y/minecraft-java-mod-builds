# 14 — IMPLEMENTATION BACKLOG

이 순서는 '예쁜 화면부터' 만들지 않고 위험한 기술/게임성 가정을 먼저 검증하기 위한 기본 개발 순서다.

## M0 — Bootstrap & Contracts
상태: **AUTOMATED PASS**
### 작업
- NeoForge 프로젝트 scaffold를 공용 BUILD_STANDARD 기준으로 생성.
- mod id `turnbound_re`.
- data definition Codec/registry/validator.
- vanilla Mob coverage validator.
- debug commands skeleton.
- pure battle test module.
### PASS
clean build, JAR verify, invalid data test, 미분류 mob test가 동작.

## M1 — Deterministic Battle Core
상태: **AUTOMATED PASS**
### 작업
- BattleInstance/state machine.
- initiative.
- command validation/revision.
- Damage/Affinity.
- Poise/EXPOSED/POISE_GUARD.
- Energy/Guard/status.
- Intent/AI stub.
- victory/defeat/reward event.
### PASS
순수 unit test로 동일 seed/commands 동일 event stream. soft-lock 상태전이 없음.

## M2 — Minecraft Adapter & Network
상태: **AUTOMATED GATE PASS / MANUAL CLIENT GATE PENDING**
### 작업
- Entity participant binding.
- world AI/damage isolation.
- C2S command/S2C snapshot+events.
- production battle presentation cache/HUD.
- cleanup/disconnect/dimension guards.
### PASS
자동 20-cycle soak는 통과. 실제 클라이언트 debug encounter 20회/orphan battle 0 수동 gate는 최종 통합 테스트 때 함께 수행.

## M3 — Representative Content
상태: **AUTOMATED PASS / FINAL BALANCE NOT LOCKED**
### 작업
- 대표 바닐라 8종.
- action 20개 이상.
- 일반 Encounter + elite/boss.
- 각 role/affinity/status/Intent 케이스 검증.
### PASS
Basic spam보다 Poise/Intent 대응이 유리한 상황이 명확히 존재하고 전투 로그로 확인.

## M4 — Progression & Reward
상태: **FULL AUTOMATED PASS / RUNTIME PERSISTENCE MANUAL CHECK DEFERRED**
### 작업
- character ownership.
- origin/current star/level caps.
- ascension.
- squad cost.
- Coin/Essence/Shard.
- save/version.
- Encounter reward.
- authored Encounter launcher.
- immutable encounter/reward snapshot.
- common VICTORY→REWARD persisted settlement.
### PASS
production authored Encounter를 직접 로드해 실제 data action 전투→승리→Encounter reward→one-shot claim→PlayerProgress 변경→SavedData Codec round-trip→unlock/level cap/ascend/party/Squad Cost→reload-equivalent round-trip까지 자동 검증. persistence callback 실패는 claim을 소비하지 않음.

실제 Minecraft world save/reload/재접속 체감 검증은 M2 manual client gate와 함께 최종 완성본에서 수행한다.

## M5 — Production UI / Presentation
상태: **AUTOMATED IMPLEMENTATION GATE PASS / SCREENSHOT VISUAL AUDIT PENDING**

완료된 자동 구현 범위:
- production Battle HUD / command input / world-first target chooser.
- Party Formation / Character Overview / Skills / Growth / Equipment.
- server-authoritative progression write/reconciliation.
- selected-character adaptive 3D entity preview.
- authoritative Battle Result / Reward / ACK / world return lifecycle.
- EN/KO localization parity.
- shared `UiVisualLanguage` 및 외부 CC0 UI asset 적용.
- logical battle-stage participant rendering.
- data-driven action timeline, impact/travel accent.
- Skeleton / Enderman 대표 3D character presentation pass.
- authoritative previous/final snapshot만 사용하는 impact-synchronized HP/Poise/EXPOSED/defeat presentation projection.
- WINDUP 이전값 유지 → IMPACT aggregate easing → RECOVERY exact final.
- healing/multi-target/reset/interruption 계약 자동 검증.

마지막 impact sync 코드 기준:
- commit `41397f92b501820f519de56712663cba6fc62db1`
- Build turnbound-re #212 / run `34732981207`: Java 25 / clean build / 전체 JUnit / JAR verify / artifact upload PASS.

남은 gate:
- 실제 Minecraft screenshot quality.
- GUI Scale별 clipping/가독성/시선 이동.
- 480×270 실화면 밀도.
- final frame/icon/sprite source/license.
- animation/transition/reward reveal/audio timing.
- representative 3D model pose/centering/실제 체감.
- Skeleton aim / Enderman phase가 실제 플레이에서 충분히 읽히는지 검증.

**중요:** screenshot/reference 비교 전 M5 production visual PASS를 선언하지 않는다. 사용자의 현재 방침에 따라 중간 실플레이 테스트를 요구하지 않고, 통합 테스트 가치가 있는 완성 구간에서 한 번에 검증한다.

## M6 — World & Life Loop
상태: **IN PROGRESS — FIRST EXPEDITION AUTO-INTEGRATED / PRODUCTION VISUAL GATE PENDING**

완료:
- authored Encounter의 production 진입은 world anchor가 소유하는 server-authoritative 경로로 수렴.
- world anchor locator/dimension/entity/range/encounter identity 최종 서버 재검증.
- Expedition Journal은 authored route/reference 정보만 표시하고 encounter id만으로 전투를 직접 여는 C2S 우회 경로 제거.
- anchor/Encounter 양쪽 `repeatable` 계약.
- 비반복 anchor 승리 시 reward + completion을 하나의 immutable save write로 정산.
- `PlayerProgress`의 completed Encounter locator 저장/구버전 decode.
- 완료한 one-time anchor preview/confirm 차단.
- 반복형 encounter는 기존 farming loop 유지.
- `ResourceAnchor` MINING/FARMING/FISHING 기능 계약과 locator/dimension/range 검증.
- resource anchor 자체가 loot를 지급하지 않고 실제 Minecraft 채집을 유지하도록 고정.
- 실제 Minecraft 재료를 보조손에서 선택해 Encounter 준비물로 소비하는 battle preparation sink.
- Iron/Golden Carrot/Cooked Fish 대표 준비 효과, confirm 재검증, 성공한 battle 등록 후 정확히 1개 소비.
- preparation은 battle-local이며 PlayerProgress/적/SPD를 변경하지 않음.
- 장기 sink는 캐릭터당 장비 1슬롯, 고정 bonus, random affix/rarity/durability 없음으로 최소화.
- 장비 definition/save schema 3/구 schema 호환/forge-upgrade pure transaction/중복 장착 차단/전투 participant 적용 backend.
- 장비 효과는 HP/ATK/DEF/POISE만 허용하고 SPD는 구조적으로 제외.
- 실제 Minecraft main inventory material을 NeoForge transaction으로 정확히 소비하고 Coin/progress CAS와 결합.
- armor/offhand는 forge material 소비에서 제외하여 Battle Preparation과 충돌하지 않음.
- Smithing Table 근처에서만 Craft/Upgrade가 가능한 physical workstation gate.
- 장비 server-authored snapshot/action network와 stale token 검증.
- 기존 Character Detail에 네 번째 `Equipment` context 통합: 3개 장비 목록, 현재/다음 bonus, 실제 material 보유량, Coin 비용, forge 접근 상태, Craft/Upgrade/Equip/Unequip.
- 장비 UI는 새 dashboard/새 통화를 만들지 않고 기존 M5 visual language를 재사용.
- `HUB_01 -> REGION_01` 기능 slice의 실제 block/entity 배치 harness 구현.
- Hub Smithing Table/Furnace/Crafting Table과 동쪽 route를 실제 월드에 연결.
- `ore_outcrop`에 실제 Coal/Copper/Iron/Gold ore를 배치해 채광→제련→장비 material 흐름 연결.
- `riverside_plot`에 실제 farmland + mature Carrot/Wheat를 배치해 농사→전투 준비물 흐름 연결.
- `river_pool`에 실제 water pool을 배치해 vanilla fishing→Cooked Fish 준비물 흐름 연결.
- patrol/elite Interaction entity가 기존 authored locator tag를 사용해 world-first Encounter 진입 경로에 연결.
- 기능 slice offset은 production 좌표가 아니며 RegionDefinition에는 좌표를 추가하지 않음.
- World Asset Gate를 통해 HUB_01/REGION_01 첫 production-facing palette·footprint·landmark cadence를 정하고 forge camp/resource branches/patrol ruin/rift landmark prototype을 구현.
- `fastTravelAnchors` data contract와 별도 `FastTravelSavedData`를 추가해 월드 공용 waypoint 위치와 플레이어 개인 discovery를 분리.
- Hub/REGION_01에 실제 Lodestone + Interaction waypoint를 배치하고, 두 지점을 각각 직접 발견한 뒤에만 서버 권한 빠른 이동이 열리도록 연결.
- fast travel은 client 좌표/unlock 입력을 신뢰하지 않고 definition/tag/dimension/range/current registered position/discovery/link를 서버가 재검증.
- 전투 중 fast travel 차단, 낡은 prototype Interaction marker 위치 불일치 차단, 첫 two-point slice에서 다중 목적지면 이동 대신 selection-required로 중단.
- production `rift_elite` anchor를 one-time 목표로 전환하고 `overworld_patrol`은 반복 파밍 Encounter로 유지.
- 첫 임무 단계는 별도 quest save 없이 `FastTravelSavedData`의 개인 waypoint 발견 상태와 `PlayerProgress.completedEncounterLocators`에서 파생.
- Hub waypoint 발견 → REGION_01 waypoint 직접 발견 → rift elite 도전 → reward/completion 저장 성공 → Hub 귀환 안내의 최소 quest hook 연결.
- quest 안내는 실제 서버 waypoint 발견/성공한 reward settlement 뒤에만 발생하며 client가 quest stage를 제출하는 경로 없음.
- reward persistence 실패 시 one-time completion과 quest 완료 안내가 모두 진행되지 않아 기존 one-shot claim 원자성을 유지.
- 상세 정본: `19_M6_WORLD_ENCOUNTER_LIFECYCLE.md`, `21_M6_RESOURCE_NODE_CONTRACT.md`, `22_M6_BATTLE_PREPARATION_MATERIAL_SINK.md`, `23_M6_MINIMAL_EQUIPMENT_CONTRACT.md`, `24_M6_EQUIPMENT_FORGE_TRANSACTION.md`, `25_M6_FUNCTIONAL_WORLD_SLICE.md`, `26_M6_WORLD_ASSET_GATE.md`, `27_M6_FAST_TRAVEL_DISCOVERY.md`, `28_M6_FIRST_EXPEDITION_QUEST.md`.

남음:
- 3개 이상 travel destination이 실제 필요해질 때만 destination selection UX 추가.
- Hub → 채집 → 장비/준비 → discovery → one-time elite → reward → fast travel 귀환 전체의 자동 통합 acceptance를 한 단계 더 묶어 회귀를 잠근다.
- 첫 광산/농장/강 산출량과 장비 Lv1/준비물 비용의 정적 밸런스 범위를 검증해 명백한 막힘/과잉을 제거한다.
- 장비/준비물/채집/discovery/fast travel/첫 임무까지 포함한 통합 screenshot/playtest 및 밸런스 조정.
- production visual screenshot 비교 후 Hub/광산/농장/강/Encounter landmark 세부 수정.

### PASS
각 활동의 산출이 다음 시스템에 실제 사용되고, 메뉴 우회나 막힌 경로 없이 fixed-world loop가 성립하며, 실제 Minecraft 플레이에서도 의도대로 작동해야 한다.

## M7 — Full Vanilla Roster
### 작업
- 모든 eligible 바닐라 Mob STUB 제거.
- family별 kit/balance.
- 획득처.
- presentation status 추적.
### PASS
eligible 전수 PLAYABLE 이상, 미분류 0.

## M8 — Production Balance & Release Hardening
- difficulty curve.
- reward economy.
- accessibility.
- save migration.
- performance/network soak.
- dedicated server/multiplayer verification 가능 시 수행.
- 최종 visual regression.

## 지금 바로 할 일 — 2026-09-13 최신

현재 우선순위는 **첫 임무까지 닫힌 M6 월드 루프를 자동 통합 acceptance와 밸런스 사전검사로 잠근 뒤, 한 번의 의미 있는 실제 Minecraft 통합 playtest에 들어갈 준비를 끝내는 것**이다.

순서:
1. Hub → REGION waypoint discovery → one-time elite completion → Hub return eligibility의 전체 상태 전이를 production definitions 기준 자동 검증한다.
2. 광산 ore 수량, 농장 식량, 낚시 준비물 경로와 장비 Lv1/전투 준비 비용을 비교해 첫 사이클에서 막힌 선택지가 없는지 정적 검사한다.
3. one-time elite의 최초 reward가 다음 성장/장비 선택을 만들 수 있는지 현재 reward table 기준으로 검토한다.
4. 자동 회귀가 잠기면 Hub → 탐험 → 실제 채집 → 장비/준비 → waypoint discovery → visible encounter → battle → reward → fast travel 귀환 전체를 통합 screenshot/playtest에서 검증한다.
5. 실제 체감에서만 판단 가능한 동선·채집시간·전투시간·UI·연출·랜드마크 문제를 그 결과로 조정한다.

금지:
- 재료를 이유 없이 Coin/Essence로 환전해 모든 생활 활동을 같은 숫자로 평탄화.
- 새 활동마다 별도 통화/메뉴를 추가.
- 실제 월드 진입을 우회하는 encounter 선택 메뉴 부활.
- material을 소비하지 않는 가짜 장비 제작 네트워크 경로.
- 랜덤 옵션/희귀도/다중 슬롯을 필요 검증 없이 추가.
- World Asset Gate 없이 production 건축/외형을 즉흥 확정.
- 발견하지 않은 waypoint를 원격 메뉴/클라이언트 payload로 해금.
- 기존 discovery/completion과 중복되는 quest save를 따로 만들어 상태를 이중화.

자동 코드 검증은 의미 있는 단위마다 수행하되, 사용자에게 중간 수동 테스트를 요구하지 않는다. 실제 screenshot/playtest 및 M2/M4 수동 gate는 통합 테스트 가치가 있는 완성 구간에서 함께 수행한다.
