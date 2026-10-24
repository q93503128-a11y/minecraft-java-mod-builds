# Augmentation Survival (working title)

> 정식 게임명은 아직 정하지 않았다. `Augmentation Survival`과 slug `augmentation-survival`은 저장소 내 작업 식별을 위한 임시 명칭이다.

- Slug: `augmentation-survival`
- Mod ID: `augmentation_survival` (working technical id)
- Namespace: `augmentation_survival`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `Fabric`
- Loader version: `0.19.5`
- Fabric API: `0.160.0+26.2`
- Gradle: `9.5.1`
- Build plugin: `Fabric Loom 1.17.20`
- Final JAR: `augmentation-survival-0.1.0-alpha.1.jar` (development target; not release-ready)
- Existing-world compatibility: 가능한 한 유지하되 Terralith는 새 월드 기준. 월드젠 외부 모드 교체/제거는 별도 migration 계약 없이는 지원하지 않는다.
- Required dependency: Fabric API
- Official pack candidate mods: `MODPACK_BASELINE.md` 참조
- Forbidden bundled dependencies: 재배포 권한이 불명확한 모드/자산을 저장소 JAR에 직접 번들하지 않는다. 외부 모드는 원본 배포처를 참조하는 모드팩 의존성으로 관리한다.
- Datagen task: 아직 미구성
- GameTest task: 아직 미구성
- Server smoke-test task: `runPackServer -PaugmentationPackRuntime=true`
- Client smoke-test task: `runPackClient -PaugmentationPackRuntime=true` + 실제 화면 검수

## 현재 상태

`M1C CORE ECONOMY + DEVICE FUNCTIONAL BASELINE / COMBAT+MOVEMENT+PROJECTILE+ACTIVE+SURVIVAL+HARVEST+CONSUMPTION SHARED RUNTIMES IMPLEMENTED / THREAT APPLICATION BULK NEXT`

외부 모드팩 M0 부팅/의존성 기준선 위에 실제 Java runtime 구현을 진행 중이다. 현재는 **146종 runtime catalog, 영구 플레이어 증강 상태, 세이브 전역 위협 high-water, 코어 획득 경로, 기본/강화 증강 장치, 3후보 선택 transaction, 두 번째 선택, 3:1 코어 합성**까지 기능 기준선이 구현되어 있다.

Reference vertical slice는 증강 1~2개를 독립 구현하는 방식이 아니라 shared-system batch로 진행한다. stat → combat/protection/heal/death → movement/aerial → projectile shot-action/flight-segment → active/combat expansion → survival/resource → harvest/drop → consumption/fishing/breeding 순으로 공통 런타임 경계를 확장했다.

현재 146종 기준 gameplay runtime 진행도는 다음과 같다.

- 완전 구현: **82 / 146**
- 부분 구현: **4 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`
- 완전 구현되지 않음: **64 / 146**
- gameplay runtime 자체가 아직 없음: **60 / 146**
- 최신 consumption/fishing/breeding batch: **8종 touched** — 완전 구현 6, 부분 구현 2

여기서 “완전 구현”은 canonical gameplay 효과의 runtime 구현을 뜻한다. 실제 플레이테스트, 최종 VFX/사운드/HUD/UI polish까지 끝났다는 뜻은 아니다.

survival/resource batch에서는 `광부의 손`, `벌목 요령`, `적게 먹기`, `물갈퀴`, `숨 오래 참기`, `추위 적응`, `긴 팔`, `물속 작업`, `발 안 묶이기`, `도구 관리`, `방어구 관리`, `밤눈`, `연속 채굴`, `자동 보충`, `회복력`을 연결하고 기존 부분 구현이던 `야행성`의 야간 채굴속도 facet과 `햇빛 체질`의 주간 자연회복 간격 facet을 완료했다.

resource-harvest batch에서는 `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`를 완전 구현 baseline으로 연결했다. 자동 수확/벌목/굴파기는 raw block deletion이 아니라 서버 `destroyBlock` 경로를 재사용해 Fortune/Silk Touch, 도구 판정, block-break hook, 기존 내구도 절약과 조합되게 했다. `전리품 챙기기`는 일반 몹 item-bundle 복제 baseline까지 구현했지만 외부 보스/혼잡 드롭 provenance가 남아 부분 구현이다.

최신 consumption/fishing/breeding batch에서는 `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`를 완전 구현 baseline으로 연결했다. `보물 낚시`는 +1/+2/+3 vanilla fishing luck-equivalent를 사용하므로 canonical +2/+4/+6%p와 정확히 동일하지 않아 부분 구현이다. `풍요`는 block/mob/fishing의 최종 ordinary item stack을 한 번 복제하는 공통 baseline을 구현했지만 아직 spatial fresh-item capture이고 외부 progression/unique item provenance가 완전하지 않아 부분 구현이다. 코어는 `풍요` 복제 대상에서 제외한다.

`비축 식사`의 작은 영구 수치를 위해 player state schema를 v2로 올리고 optional `persistent_ints` map을 추가했다. 기존 save field는 유지되고 새 field는 없으면 빈 map으로 decode한다. 섭취시간/사용 중 이동처럼 local prediction이 필요한 값만 작은 S2C snapshot으로 보내며 허기, 비축량, 낚시 결과, 번식 결과, drop duplication은 서버가 최종 결정한다.

채굴속도 계열은 같은 스탯의 보너스를 합산한 뒤 한 번 적용하고, `긴 팔`, `물속 작업`, `발 안 묶이기`, `숨 오래 참기`는 가능한 범위에서 vanilla attribute를 재사용한다. 자연회복 계열은 별도 가짜 heal tick을 만들지 않고 vanilla natural-regeneration timer를 가속한다. 내구도 계열은 vanilla Unbreaking 앞의 durability-loss event 자체를 확률적으로 취소한다. `자동 보충`은 선택된 hotbar stack이 소진됐을 때 main inventory의 동일 item+components stack을 이동시킨다.

기존 common active engine에서는 `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`과 `능력 가속`, `이중 충전`, `재연`, `전투 순환`을 처리한다. broad combat runtime은 `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`, `우두머리 사냥`을 처리한다.

Projectile runtime은 `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`을 한 shot-action/flight-segment 엔진으로 처리하며 ROOT/derived/reflected provenance를 분리한다. Movement runtime은 `숨 고르기`, `야행성`, `햇빛 체질`, `물갈퀴`, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`를 포함한다.

최신 gameplay checkpoint `fb7c963a93ef721810512d3d4510195f1ae5fd1f`는 Build Augmentation Survival Pack #50에서 build, 23개 pinned pack dependency inspection, 150-file runtime classpath verification, dedicated-server smoke, client boot smoke를 모두 통과했다. 서버는 92 mods, 클라이언트는 108 mods로 full-pack 부팅했고 Better Combat 3.2.2와 새 consumption/fishing contract 동시 로드도 확인했다. 다만 실제 플레이어를 사용한 음식/허기/낚시/번식/풍요 수치와 조작감, 기존 harvest matrix, Better Combat cadence, projectile matrix, active input, multiplayer correctness는 아직 플레이테스트하지 않았다. 세부 구현 현황은 `IMPLEMENTATION_STATUS_2026-10-24.md`를 따른다.

## 프로젝트 정체성

Minecraft 장기 서바이벌을 유지하면서 다양한 증강을 계속 획득해 플레이어가 매우 강해지는 파워 판타지 모드다.

전투 전용 로그라이크가 아니다. 전투뿐 아니라 채광, 이동, 탐험, 생존, 농사, 낚시, 건축·자원 수집 등 Minecraft의 여러 활동이 증강으로 크게 변해야 한다.

핵심 방향:

- 증강은 미세 보너스가 아니라 플레이 방식을 크게 바꾸는 중심 성장 시스템이다.
- 장기 월드에서 한 번의 운 때문에 빌드가 망가지지 않게 한다.
- 높은 등급 증강도 엔더 드래곤 처치 이후에만 열지 않는다.
- 특정 장소 하나가 특정 코어 등급 하나만 독점하지 않는다. 장소마다 여러 인접 등급의 코어가 서로 다른 확률로 나온다.
- 외부 모드는 증강을 사용할 세계와 콘텐츠를 확장해야 하며, 별도 성장 시스템으로 증강과 경쟁하지 않는다.
- 강한 증강 조합은 의도적으로 허용하고 무한/기하급수 재귀만 차단한다.
- 적은 플레이어 성장을 1:1 상쇄하지 않고 세이브 전체 위협 단계로 부분 추격한다.
- 코어는 제작/탐험/전투/낚시/보스/합성으로 분산 획득하며 한 경로가 전체 경제를 독점하지 않는다.

## 정본 우선순위

프로젝트 내부 기본 우선순위:

1. `AUGMENTS.md`
2. `AUGMENT_PROPOSAL_HISTORY.md`
3. `GAME_DESIGN.md`
4. `PROJECT.md`
5. `MODPACK_BASELINE.md`
6. 실제 source/resource
7. 최신 playtest feedback
8. 과거 대화

세부 구현 문서인 `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md`, `CORE_ACQUISITION_DESIGN_2026-10-24.md`, `AUGMENT_FINAL_COMPATIBILITY_AUDIT_2026-10-24.md`, `DEVELOPMENT_IMPLEMENTATION_PLAN_2026-10-24.md`, `IMPLEMENTATION_STATUS_2026-10-24.md`는 위 정본을 구체화한다. 충돌 시 위 기본 우선순위를 따른다.

저장소 공용 `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`는 상위 공통 작업 계약으로 항상 함께 적용한다.

## 현재 확정된 핵심 규칙

- 등급: `일반 → 고급 → 희귀 → 영웅 → 전설 → 신화`
- 현재 확정 로스터: **146종**
  - 일반 20
  - 고급 27
  - 희귀 30
  - 영웅 40
  - 전설 20
  - 신화 9
- 각 증강의 최대 레벨과 현재 효과는 `AUGMENTS.md`에 고정한다.
- 등급별 증강 코어가 존재한다.
- 증강 장치에 코어를 넣으면 같은 등급의 유효한 후보 3개를 제시한다.
- 기본 선택은 1개, 신화 `두 번째 선택` 보유 시 서로 다른 2개를 한 transaction으로 획득한다.
- 이미 최대 레벨인 증강은 후보 풀에서 제거한다.
- 특정 등급 전체를 최대치까지 획득했다면 그 등급 뽑기는 불가능하며 코어도 소비하지 않는다.
- 동일 등급 코어 3개를 바로 위 등급 코어 1개로 합성할 수 있다.
- 코어 합성은 강화 증강 장치에서 제공한다.
- 일반 코어는 레드스톤/자수정/구리 레시피로 확정 제작 가능하다.
- 자연 구조물은 기존 loot를 유지하면서 위험도별 추가 core roll을 가진다.
- 일반 적대몹은 매우 낮은 확률의 저등급 core roll만 가지며 Looting/풍요 등으로 코어를 복제하지 않는다.
- 정식 보스는 코어 보상을 보장하고 플레이어별 최초 처치에는 추가 코어 1개를 지급한다.
- 신화 직접 획득은 pinnacle 콘텐츠에서만 낮은 확률로 허용하며 전설 3:1 합성으로도 확정 접근 가능하다.
- 총 증강 보유 개수 제한은 없다.
- 액티브는 일반 10칸 빠른 슬롯을 사용하고 `권능 탈취`는 별도 전용키 하나를 사용한다.
- 월드 위협 단계는 지역/플레이어별이 아니라 **세이브 전체 공통 0~8**이다.

아직 고정하지 않은 것:

- 일부 구조물/외부 보스의 개별 risk/reward profile 세부 매핑
- 일부 증강의 세부 파라미터 (`탄도 장막`, `임시 발판`, `생명 연결`, `작아지기`, `벽치기`)
- 위협 단계 수치 테이블의 최종 플레이테스트 값
- 코어 획득 확률의 최종 플레이테스트 튜닝 값

현재 코어 획득 확률은 구현 가능한 초기 기준값으로 고정하고 실제 플레이테스트에서 조정한다.

## 외부 모드 연동 계약

- 외부 모드 JAR 자체를 수정하는 방식보다 우리 모드의 loot/event/data/profile/adapter 레이어를 우선한다.
- 외부 구조물 상자에는 원본 loot를 보존하면서 증강 코어 pool을 추가한다.
- 외부 몹은 loot table 또는 서버 권한 이벤트를 통해 코어 드랍을 추가한다.
- `장소 = 단일 등급`이 아니라 `장소 = 여러 등급의 확률 분포`로 설계한다.
- 호환 데이터는 가능한 한 data-driven으로 분리한다.
- Better Combat은 하나의 서버 ROOT melee hit로 정규화한다.
- Chrono Dawn / World Bosses / MCEndgame 보스는 control/reward/authority profile을 따로 둔다.
- Farmer's Delight는 작물/음식/생산 adapter로 연결한다.

## 구현 시작 순서

세부 계획은 `DEVELOPMENT_IMPLEMENTATION_PLAN_2026-10-24.md`를 따른다.

1. Runtime/state foundation
2. Core item / acquisition profile / loot-event reward layer
3. Core / device / 3-choice transaction / functional UI
4. Reference vertical slice: 스탯·회복·사망·이동·투사체·액티브·자원 최소 한 경로
5. 일반·고급 bulk implementation
6. 희귀·영웅 복잡 파이프라인
7. 전설·신화 및 공유 저장/권능
8. save-wide threat scaling gameplay application
9. 외부 모드 adapter 확장
10. HUD/VFX/animation/sound polish
11. gameplay/performance/multiplayer validation

현재 1~3의 기능 기준선과 4번의 주요 shared runtime vertical slice가 구현되어 있다. 앞으로도 한 증강씩 독립 하드코딩하지 않고 **현재 공통 기반으로 구현 가능한 쉬운/중간 난도 증강을 먼저 전수 분류하고, 같은 runtime boundary에 맞는 것은 한 번에 전부 연결하는 대형 묶음**으로 진행한다. 최소 개수 목표보다 “현재 엔진으로 안전하게 구현 가능한 것을 남기지 않는다”를 batching 기준으로 삼는다.

다음 large system batch는 **save-wide threat live hostile scaling + elite/boss application boundary**다. 이미 저장되는 threat high-water를 실제 hostile HP/attack/movement/armor/knockback resistance에 연결하고, ordinary hostile / elite / boss coefficient를 data-driven하게 적용한다. spawn/load와 실제 stage 상승 경계에서만 reconcile하고 전역 매 tick scan은 만들지 않는다. Chrono Dawn / World Bosses / MCEndgame은 안전하게 식별 가능한 boss/elite profile부터 adapter로 연결하고 외부 AI/phase/loot는 보존한다.

## 현재 검증 상태

검증된 최신 gameplay-code 체크포인트:

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `fb7c963a93ef721810512d3d4510195f1ae5fd1f`
- JAR PRODUCED: YES — Actions run `40081679446`
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- CANONICAL AUGMENT CATALOG INITIALIZATION: PASS — 146 definitions
- STAT CONTRACT INITIALIZATION: PASS — 7 definitions / 7 modifier groups
- SURVIVAL CONTRACT INITIALIZATION: PASS
- RESOURCE-HARVEST CONTRACT INITIALIZATION: PASS
- CONSUMPTION/FISHING CONTRACT INITIALIZATION: PASS
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
- EXPANSION CONTRACT INITIALIZATION: PASS
- GitHub Actions: `40081679446` / Build Augmentation Survival Pack #50
- Verification artifact: `augmentation-survival-pack-smoke-50` / ID `12555731050`
- Artifact ZIP SHA-256: `8908a66e3f83e3fc7746883d8abb7379f72f61ec7f7e9570a6888e0a2639d30c`

현재 구현 범위:

- ROSTER LOCKED: YES — 146
- GAMEPLAY RUNTIME FULLY IMPLEMENTED: **82 / 146**
- GAMEPLAY RUNTIME PARTIAL: **4 / 146** — `회복 증폭`, `전리품 챙기기`, `보물 낚시`, `풍요`
- NOT FULLY IMPLEMENTED: **64 / 146**
- NO GAMEPLAY RUNTIME YET: **60 / 146**
- CURRENT BATCH TOUCHED: **8** — 6 full / 2 partial
- FINAL COMPATIBILITY AUDIT: YES
- INTERACTION CONTRACT V2: YES
- CORE ACQUISITION LOOP: IMPLEMENTED BASELINE
- PERSISTENT PLAYER AUGMENT STATE: IMPLEMENTED — schema v2 optional `persistent_ints`
- SAVE-WIDE THREAT STATE: IMPLEMENTED / LIVE HOSTILE SCALING INCOMPLETE
- CORE / DEVICE / 3-CHOICE TRANSACTION: IMPLEMENTED BASELINE
- SECOND CHOICE ATOMIC 2-PICK: IMPLEMENTED
- 3:1 CORE UPGRADE ONE/ALL: IMPLEMENTED
- FUNCTIONAL DEVICE CARD UI: IMPLEMENTED BASELINE
- UNCONDITIONAL/UTILITY STAT CONTRACT: IMPLEMENTED — 7 definitions / 7 modifier groups
- SHARED COMBAT/PROTECTION/HEAL/LETHAL RUNTIMES: IMPLEMENTED BASELINE
- SHARED MOVEMENT / AERIAL RUNTIME: IMPLEMENTED BASELINE
- PROJECTILE SHOT-ACTION / FLIGHT-SEGMENT RUNTIME: IMPLEMENTED BASELINE
- COMMON ACTIVE-ABILITY ENGINE: IMPLEMENTED BASELINE
- BROAD COMBAT EXPANSION RUNTIME: IMPLEMENTED BASELINE
- SHARED SURVIVAL / RESOURCE RUNTIME: IMPLEMENTED BASELINE
- RESOURCE-HARVEST / DERIVED BREAK RUNTIME: IMPLEMENTED BASELINE
- CONSUMPTION / FISHING / BREEDING RUNTIME: IMPLEMENTED BASELINE
- FINAL ORDINARY DROP `풍요` RUNTIME: PARTIAL BASELINE
- `야행성`: COMPLETE GAMEPLAY RUNTIME / LIVE TEST PENDING
- `햇빛 체질`: COMPLETE GAMEPLAY RUNTIME / LIVE TEST PENDING
- `풍작`, `씨앗 챙기기`, `한 조각 더`, `다시 심기`, `싹쓸이 수확`, `나무 베기`, `굴파기`: IMPLEMENTED / LIVE TEST PENDING
- `든든한 한 끼`, `낚시 감각`, `빨리 먹기`, `쌍둥이`, `먹으면서 걷기`, `비축 식사`: IMPLEMENTED / LIVE TEST PENDING
- FINAL DEVICE/ACTIVE/COMBAT/RESOURCE MODEL-TEXTURE-VFX-SOUND-HUD: NOT COMPLETE
- CONSUMPTION/FISHING/BREEDING LIVE-PLAYER TESTED: NO
- ABUNDANCE DROP MATRIX LIVE TESTED: NO
- RESOURCE-HARVEST LIVE-PLAYER TESTED: NO
- SURVIVAL/RESOURCE LIVE-PLAYER TESTED: NO
- ACTIVE ENGINE LIVE-PLAYER TESTED: NO
- PROJECTILE LIVE-PLAYER TESTED: NO
- MOVEMENT LIVE-PLAYER TESTED: NO
- PROTECTION PIPELINE LIVE-PLAYER TESTED: NO
- HEALING PIPELINE LIVE-PLAYER TESTED: NO
- LETHAL CHAIN LIVE-PLAYER TESTED: NO
- STAT AUGMENT LIVE-PLAYER TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO
