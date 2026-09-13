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
상태: **IN PROGRESS — WORLD REFERENCE GATE PASS / PRODUCTION PROTOTYPE IMPLEMENTED / VISUAL PLAYTEST PENDING**

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
- HUB_01 / REGION_01 World Asset Gate reference 조사 완료: Minecraft Legends / Minecraft Dungeons Camp / MineColonies style family를 구조 원리 reference로 채택하고 proprietary/오픈소스 asset은 복제하지 않음.
- `ProductionWorldSlicePlan`으로 23×19 Hub, 5-block main road, resource branch breathing room, patrol→elite escalation distance를 pure contract로 고정.
- `ProductionWorldSlicePrototypeBuilder`로 stone/tuff + spruce forge hall, readable road, quarry/farm/river, patrol ruin, rift landmark의 production-facing prototype 구현.
- forge hall 내부에 기존 Smithing Table/Furnace/Crafting/Anvil/Grindstone 동선을 실제 공간으로 통합.
- quarry prototype yield를 Coal 8 / Copper 10 / Iron 8 / Gold 4로 조정해 첫 장비 선택과 preparation resource 경쟁을 실제 Minecraft material로 연결.
- `/turnbound_re_world_slice prototype` operator harness 추가. 기존 `build` 기능 slice는 비교용으로 보존.
- 상세 정본: `19_M6_WORLD_ENCOUNTER_LIFECYCLE.md`, `21_M6_RESOURCE_NODE_CONTRACT.md`, `22_M6_BATTLE_PREPARATION_MATERIAL_SINK.md`, `23_M6_MINIMAL_EQUIPMENT_CONTRACT.md`, `24_M6_EQUIPMENT_FORGE_TRANSACTION.md`, `25_M6_FUNCTIONAL_WORLD_SLICE.md`, `26_M6_WORLD_ASSET_GATE.md`.

남음:
- production prototype 실제 screenshot side-by-side audit 및 palette/scale 보정.
- fast travel/exploration discovery의 최소 server-authoritative hook.
- quest hook은 이동/전투/채집 루프를 방해하지 않는 최소 범위로 연결.
- production non-repeatable anchor를 실제 콘텐츠로 배치하고 completion state와 월드 표현을 연결.
- 장비/준비물/채집까지 포함한 통합 screenshot/playtest 및 밸런스 조정.
- 최종 structure/worldgen 배치 방식 확정.

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

현재 우선순위는 **production-facing world prototype 위에 최소한의 탐험 진행 상태를 연결하는 것**이다.

순서:
1. fast travel / exploration discovery를 기존 world locator와 서버 권한 save에 최소 hook으로 연결한다.
2. HUB_01 / REGION_01을 발견하기 전후의 이동 가능 상태를 명확히 하되 새 통화/별도 관리 메뉴는 만들지 않는다.
3. 첫 production non-repeatable Encounter를 실제 locator에 배치하고 승리 후 completion이 월드에서 읽히게 한다.
4. quest는 위 동선에 자연스럽게 얹히는 첫 목표 1개만 연결하고 독립 quest grind를 만들지 않는다.
5. 그 뒤 world exploration → 실제 채집 → 장비/준비 → visible encounter → battle → reward → 귀환 전체를 통합 screenshot/playtest에서 검증한다.

금지:
- 재료를 이유 없이 Coin/Essence로 환전해 모든 생활 활동을 같은 숫자로 평탄화.
- 새 활동마다 별도 통화/메뉴를 추가.
- 실제 월드 진입을 우회하는 encounter 선택 메뉴 부활.
- material을 소비하지 않는 가짜 장비 제작 네트워크 경로.
- 랜덤 옵션/희귀도/다중 슬롯을 필요 검증 없이 추가.
- World Asset Gate를 무시하고 production 건축/외형을 즉흥 변경.
- reference 게임/오픈소스 schematic을 출처·license 검토 없이 직접 복제.

자동 코드 검증은 의미 있는 단위마다 수행하되, 사용자에게 중간 수동 테스트를 요구하지 않는다. 실제 screenshot/playtest 및 M2/M4 수동 gate는 통합 테스트 가치가 있는 완성 구간에서 함께 수행한다.
