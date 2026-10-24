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

`M1C CORE ECONOMY + DEVICE FUNCTIONAL BASELINE / PROTECTION+MOVEMENT SHARED RUNTIMES IMPLEMENTED / PROJECTILE FLIGHT-SEGMENT NEXT`

외부 모드팩 M0 부팅/의존성 기준선 위에 실제 Java runtime 구현을 진행 중이다. 현재는 **146종 runtime catalog, 영구 플레이어 증강 상태, 세이브 전역 위협 high-water, 코어 획득 경로, 기본/강화 증강 장치, 3후보 선택 transaction, 두 번째 선택, 3:1 코어 합성**까지 기능 기준선이 구현되어 있다.

Reference vertical slice는 증강 1~2개를 독립 구현하는 방식이 아니라 shared-system batch로 진행한다. 영구 stat runtime을 시작점으로 combat provenance, ROOT 공격 보정, post-mitigation 피해 감소, 처치/timed state, augment-generated healing, lethal interception, protection/delayed-damage에 이어 shared movement/aerial runtime까지 확장했다.

현재 실제 gameplay runtime에는 stat/combat/protection/healing 계열 외에 `숨 고르기`, `야행성`의 이동속도 facet, `햇빛 체질`의 이동속도 facet, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`가 추가로 연결되어 있다. `야행성`의 채굴 속도와 `햇빛 체질`의 자연회복 간격은 아직 남아 있으며, `발 안 묶이기`, `물살 타기`, `매달리기`, `탄성 저장`, `물 위 달리기`, `속도 전환`처럼 지형/매질/충돌 물리를 크게 바꾸는 이동기는 별도 physics batch 대상으로 남겼다. `탄성 저장`은 저장 낙하거리의 추진력 변환식이 정본에 충분히 명시되지 않아 임의 수치를 만들지 않았다.

최신 movement+aerial batch는 build, 23개 pinned pack dependency inspection, 150-file runtime classpath verification, dedicated-server smoke, client boot smoke까지 통과했다. Better Combat 3.2.2를 포함한 full pack 동시 부팅도 확인했다. 다만 실제 플레이어를 사용한 이동 체감, 이중 점프 입력/동기화, 착지 판정, 공중 연계 재충전, Better Combat과의 실제 전투 중 coexistence는 아직 플레이테스트하지 않았다. 세부 구현 현황은 `IMPLEMENTATION_STATUS_2026-10-24.md`를 따른다.

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

현재 1~3의 기능 기준선과 4번의 stat + shared combat/protection/heal/death + movement/aerial vertical slice가 구현 중이다. 앞으로도 한 증강씩 독립 하드코딩하지 않고 **공통 런타임 하나를 완성하면서 그 경계에 맞는 canonical 증강을 한 번에 연결하는 대형 묶음**으로 진행한다. movement+aerial batch 다음은 `관통 화살` + `추적 화살` + `도탄`을 중심으로 관련 투사체 증강을 묶는 projectile flight-segment runtime이며, 그 다음 common active-ability batch로 이어간다.

## 현재 검증 상태

검증된 최신 gameplay-code 체크포인트:

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `446bf22d1de6517c70b71d36f864d2a31509eefa`
- JAR PRODUCED: YES
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- GitHub Actions: `40072354505` / Build Augmentation Survival Pack #42
- Verification artifact: `augmentation-survival-pack-smoke-42` / ID `12552215797`
- Artifact ZIP SHA-256: `4bc0b680aa58beb4394dcdc36d5176e2c6d8a11998411836dd0f2d31b2a314ba`

현재 구현 범위:

- ROSTER LOCKED: YES — 146
- FINAL COMPATIBILITY AUDIT: YES
- INTERACTION CONTRACT V2: YES
- CORE ACQUISITION LOOP DESIGNED: YES
- INITIAL CORE WEIGHTS DEFINED: YES
- IMPLEMENTATION PLAN: YES
- PERSISTENT PLAYER AUGMENT STATE: IMPLEMENTED
- SAVE-WIDE THREAT STATE: IMPLEMENTED
- 146 RUNTIME CATALOG: IMPLEMENTED
- CORE ACQUISITION RUNTIME: IMPLEMENTED BASELINE
- CORE / DEVICE / 3-CHOICE TRANSACTION: IMPLEMENTED BASELINE
- SECOND CHOICE ATOMIC 2-PICK: IMPLEMENTED
- 3:1 CORE UPGRADE ONE/ALL: IMPLEMENTED
- FUNCTIONAL DEVICE CARD UI: IMPLEMENTED BASELINE
- UNCONDITIONAL STAT CONTRACT: IMPLEMENTED — 4 definitions / 4 modifier groups
- SHARED COMBAT PROVENANCE: IMPLEMENTED BASELINE
- ROOT MELEE / PROJECTILE DAMAGE AUGMENT PATH: IMPLEMENTED BASELINE
- POST-MITIGATION DAMAGE-REDUCTION PATH: IMPLEMENTED BASELINE
- TIMED COMBAT EFFECT STATE: IMPLEMENTED BASELINE
- PROTECTION / DELAYED-DAMAGE ENGINE: IMPLEMENTED BASELINE
- SOURCE-OWNED SHIELD / AUGMENT ABSORPTION POOLS: IMPLEMENTED ENGINE
- AUGMENT-GENERATED HEALING PATH: IMPLEMENTED BASELINE
- LETHAL-DAMAGE CHAIN: IMPLEMENTED BASELINE
- SHARED MOVEMENT / AERIAL RUNTIME: IMPLEMENTED BASELINE
- `강건함`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `속공`: IMPLEMENTED / BETTER COMBAT LIVE TEST PENDING
- `빠른 발`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `버티기`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `날 선 감각`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `정확한 조준`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `질긴 피부`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `가벼운 착지`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `튼튼한 폐`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `마지막 힘`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `연속 처치`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `다급한 발걸음`: IMPLEMENTED / LIVE-PLAYER TEST PENDING
- `흡혈`: IMPLEMENTED DIRECT PLAYER-KILL BASELINE / LIVE-PLAYER TEST PENDING
- `회복 증폭`: PARTIAL — COMMON AUGMENT-HEAL PATH ONLY
- `피해 적응`: IMPLEMENTED BASELINE / LIVE-PLAYER TEST PENDING
- `피해 지연`: IMPLEMENTED BASELINE / LIVE-PLAYER TEST PENDING
- `넘치는 회복`: IMPLEMENTED ON ROUTED AUGMENT HEALS / HUD+LIVE TEST PENDING
- `회복 나눔`: IMPLEMENTED TEAM+TAMED BASELINE / DOMINATION ADAPTER+LIVE TEST PENDING
- `한 번 버티기`: IMPLEMENTED BASELINE / LIVE TOTEM-ORDER TEST PENDING
- `비상 토템`: IMPLEMENTED BASELINE / LIVE INVENTORY-TOTEM TEST PENDING
- `재림`: IMPLEMENTED BASELINE / LIVE STASIS+ORDER TEST PENDING
- `보호막`: POOL/TUNING/API IMPLEMENTED / ACTIVE TRIGGER NOT IMPLEMENTED
- `숨 고르기`: IMPLEMENTED / LIVE MOVEMENT TEST PENDING
- `야행성`: PARTIAL — NIGHT MOVEMENT IMPLEMENTED / MINING SPEED NOT IMPLEMENTED
- `햇빛 체질`: PARTIAL — DAY MOVEMENT IMPLEMENTED / NATURAL REGEN INTERVAL NOT IMPLEMENTED
- `성큼걸음`: IMPLEMENTED / LIVE STEP-COLLISION TEST PENDING
- `멀리뛰기`: IMPLEMENTED / LIVE SPRINT-JUMP TEST PENDING
- `관성 전환`: IMPLEMENTED / LIVE FALL-LANDING TEST PENDING
- `두 번 점프`: IMPLEMENTED SERVER-AUTHORITATIVE BASELINE / LIVE INPUT+SYNC TEST PENDING
- `공중 연계`: IMPLEMENTED ROOT-MELEE RECHARGE BASELINE / LIVE BETTER-COMBAT TEST PENDING
- COLLISION/MEDIUM-HEAVY MOVEMENT MECHANICS: NOT IMPLEMENTED
- PROJECTILE FLIGHT-SEGMENT RUNTIME: NOT IMPLEMENTED
- COMMON ACTIVE-ABILITY ENGINE: NOT IMPLEMENTED
- FINAL DEVICE MODEL/TEXTURE/VFX/SOUND: NOT IMPLEMENTED
- REMAINING AUGMENT GAMEPLAY EFFECT RUNTIME: NOT IMPLEMENTED
- MOVEMENT LIVE-PLAYER TESTED: NO
- DOUBLE-JUMP INPUT/SYNC LIVE TESTED: NO
- AERIAL-COMBO LIVE TESTED: NO
- PROTECTION PIPELINE LIVE-PLAYER TESTED: NO
- HEALING PIPELINE LIVE-PLAYER TESTED: NO
- LETHAL CHAIN LIVE-PLAYER TESTED: NO
- COMBAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT AUGMENT LIVE-PLAYER TESTED: NO
- STAT RECONNECT/DEATH TESTED: NO
- DEVICE FLOW PLAYTESTED: NO
- UI VISUAL REVIEW: NO
- BETTER COMBAT GAMEPLAY TESTED: NO
- BETTER COMBAT MOVEMENT COEXISTENCE PLAYTESTED: NO
- BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO
