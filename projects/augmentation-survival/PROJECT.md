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

`M1C CORE ECONOMY + DEVICE FUNCTIONAL BASELINE / COMBAT+MOVEMENT+PROJECTILE+ACTIVE SHARED RUNTIMES IMPLEMENTED / NEXT BULK SYSTEM BATCH`

외부 모드팩 M0 부팅/의존성 기준선 위에 실제 Java runtime 구현을 진행 중이다. 현재는 **146종 runtime catalog, 영구 플레이어 증강 상태, 세이브 전역 위협 high-water, 코어 획득 경로, 기본/강화 증강 장치, 3후보 선택 transaction, 두 번째 선택, 3:1 코어 합성**까지 기능 기준선이 구현되어 있다.

Reference vertical slice는 증강 1~2개를 독립 구현하는 방식이 아니라 shared-system batch로 진행한다. 영구 stat runtime을 시작점으로 combat provenance, ROOT 공격 보정, post-mitigation 피해 감소, 처치/timed state, augment-generated healing, lethal interception, protection/delayed-damage, shared movement/aerial, shot-action + projectile flight-segment에 이어 **common active engine + broad combat expansion**까지 확장했다.

최신 대형 배치에서는 공통 액티브 입력/쿨다운/charge/replay 경계를 만들고 `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`을 연결했다. 여기에 `능력 가속`, `이중 충전`, `재연`, `전투 순환`을 같은 엔진의 메타 규칙으로 붙였고, 기존 전투/회복/provenance 경계를 재사용해 `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`, `우두머리 사냥`까지 한 번에 연결했다. 총 20종이 이번 shared-system batch에 포함된다.

현재 액티브 입력 기능 기준선은 `증강 스킬 모드` 기본키인 Left Alt를 누른 채 `1..0`으로 10개 슬롯을 요청하는 방식이다. 클라이언트는 슬롯 번호만 보내고 실제 보유 여부, 슬롯 해석, cooldown/charge, 대상 판정과 결과는 서버가 결정한다. 최종 active-slot 관리 UI는 아직 없으므로 기존 persistent `activeSlots`를 우선 사용하고, 설정되지 않은 경우 보유한 지원 액티브를 자동 채우는 functional fallback이 있다. 이 입력은 아직 실제 플레이로 조작감을 검수하지 않았으며 modifier+숫자키가 vanilla hotbar 전환도 동시에 일으킬 수 있어 최종 UX로 간주하지 않는다.

기존 projectile batch의 `화살 아끼기`, `관통 화살`, `연쇄 화살`, `분산 사격`, `추적 화살`, `도탄`과 movement batch의 `숨 고르기`, `야행성` 이동속도 facet, `햇빛 체질` 이동속도 facet, `성큼걸음`, `멀리뛰기`, `관성 전환`, `두 번 점프`, `공중 연계`도 유지된다. `야행성` 채굴속도와 `햇빛 체질` 자연회복 간격, `발 안 묶이기`, `물살 타기`, `매달리기`, `탄성 저장`, `물 위 달리기`처럼 별도 매질/충돌 물리가 필요한 이동기는 별도 physics batch 대상으로 남아 있다. `탄성 저장`의 추진력 변환식은 정본에 충분히 명시되지 않아 임의 수치를 만들지 않는다.

최신 active/combat expansion batch는 build, 23개 pinned pack dependency inspection, 150-file runtime classpath verification, dedicated-server smoke, client boot smoke까지 통과했다. Better Combat 3.2.2를 포함한 full pack 동시 부팅도 확인했다. 다만 실제 플레이어 액티브 입력/쿨다운/이중충전/재연, Better Combat 실제 공격 cadence, 새 전투 효과의 수치/체감, 외부 보스 control, multiplayer mark ownership은 아직 플레이테스트하지 않았다. 세부 구현 현황은 `IMPLEMENTATION_STATUS_2026-10-24.md`를 따른다.

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

현재 1~3의 기능 기준선과 4번의 stat + shared combat/protection/heal/death + movement/aerial + projectile + active/combat vertical slice가 구현 중이다. 앞으로도 한 증강씩 독립 하드코딩하지 않고 **현재 공통 기반으로 구현 가능한 쉬운/중간 난도 증강을 먼저 전수 분류하고, 같은 runtime boundary에 맞는 것은 한 번에 전부 연결하는 대형 묶음**으로 진행한다. 최소 개수 목표보다 “현재 엔진으로 안전하게 구현 가능한 것을 남기지 않는다”를 batching 기준으로 삼는다.

## 현재 검증 상태

검증된 최신 gameplay-code 체크포인트:

- CODE REVIEWED: YES
- BUILD VERIFIED: YES — `960bc9739ac92989499f6d177438aec730503ae4`
- JAR PRODUCED: YES
- PACK DEPENDENCY INSPECTION: PASS — 23 pinned external artifacts
- PACK RUNTIME CLASSPATH: PASS — 150 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- STAT CONTRACT INITIALIZATION: PASS
- COMBAT/PROTECTION/HEAL/DEATH CONTRACT INITIALIZATION: PASS
- MOVEMENT CONTRACT INITIALIZATION: PASS
- PROJECTILE CONTRACT INITIALIZATION: PASS
- EXPANSION CONTRACT INITIALIZATION: PASS
- GitHub Actions: `40076587240` / Build Augmentation Survival Pack #46
- Verification artifact: `augmentation-survival-pack-smoke-46` / ID `12552963409`
- Artifact ZIP SHA-256: `940bf01dca404c463dd54a6f1a88caeb0535152f6b5d4743bb471cf910ff8d04`

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
- UNCONDITIONAL STAT CONTRACT: IMPLEMENTED BASELINE
- SHARED COMBAT PROVENANCE: IMPLEMENTED BASELINE
- PROTECTION / DELAYED-DAMAGE ENGINE: IMPLEMENTED BASELINE
- AUGMENT-GENERATED HEALING PATH: IMPLEMENTED BASELINE
- LETHAL-DAMAGE CHAIN: IMPLEMENTED BASELINE
- SHARED MOVEMENT / AERIAL RUNTIME: IMPLEMENTED BASELINE
- PROJECTILE SHOT-ACTION / FLIGHT-SEGMENT RUNTIME: IMPLEMENTED BASELINE
- COMMON ACTIVE-ABILITY ENGINE: IMPLEMENTED BASELINE
- BROAD COMBAT EXPANSION RUNTIME: IMPLEMENTED BASELINE
- `정화`, `끌어당기기`, `자리 바꾸기`, `보호막`, `광폭화`, `치유 파동`: IMPLEMENTED BASELINE / LIVE TEST PENDING
- `능력 가속`, `이중 충전`, `재연`, `전투 순환`: PARTIAL ROSTER INTEGRATION — CURRENT SIX ACTIVES
- `연쇄 폭발`, `벼락`, `메아리 타격`, `약점 표식`, `속도 전환`, `육중한 일격`, `생명 파동`, `난전`, `유리 대포`: IMPLEMENTED BASELINE / LIVE TEST PENDING
- `우두머리 사냥`: PARTIAL — EXACT CONFIGURED BOSS IDENTITIES ONLY
- `약점 표식`: MULTIPLAYER OWNERSHIP FIX PENDING — CURRENTLY ONE MARK OWNER PER TARGET
- `야행성`: PARTIAL — NIGHT MOVEMENT IMPLEMENTED / MINING SPEED PENDING
- `햇빛 체질`: PARTIAL — DAY MOVEMENT IMPLEMENTED / NATURAL REGEN INTERVAL PENDING
- COLLISION/MEDIUM-HEAVY MOVEMENT MECHANICS: NOT IMPLEMENTED
- SAVE-WIDE THREAT LIVE HOSTILE SCALING: NOT COMPLETE
- FINAL DEVICE/ACTIVE/COMBAT MODEL-TEXTURE-VFX-SOUND-HUD: NOT COMPLETE
- ACTIVE ENGINE LIVE-PLAYER TESTED: NO
- ACTIVE INPUT/COOLDOWN/DOUBLE-CHARGE/REPLAY LIVE TESTED: NO
- BROAD COMBAT EXPANSION LIVE TESTED: NO
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
