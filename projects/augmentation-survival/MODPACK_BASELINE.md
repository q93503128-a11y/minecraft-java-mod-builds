# Official Small Modpack Baseline — M0

Date: 2026-10-24
Minecraft: 26.2
Loader: Fabric 0.19.5
Java: 25

이 문서는 Augmentation Survival을 중심으로 사용할 공식 소형 모드팩 기준선이다. `동시 부팅 검증 완료`와 `최종 채택`은 같은 뜻이 아니다. 실제 월드·전투·플레이테스트를 통과하기 전까지 콘텐츠 모드는 후보 상태다.

## 1. 콘텐츠 모드 pins

| 역할 | 프로젝트 | 고정 버전 | 상태 |
|---|---|---:|---|
| 오버월드 지형/바이옴 대규모 확장 | Terralith | 2.6.4 Fabric | smoke verified candidate |
| 바닐라 구조물 변형 대량 추가 | Repurposed Structures | 7.7.6+26.2-fabric | smoke verified candidate |
| 던전·여관·탐험 구조물 대량 추가 | Dungeons and Taverns | 5.3.2 | smoke verified candidate |
| 바닐라풍 몹 추가 | Friends&Foes | 4.0.27 | smoke verified candidate |
| 일리저·레이드 적/구조물 확장 | Illager Invasion | 26.2.0 Fabric | smoke verified candidate; warning tracked |
| 야생동물·생태계 확장 | Naturalist | 2.0.6 | smoke verified candidate; warning tracked |
| 농사·요리·생산 확장 | Farmer's Delight Refabricated | 26.2-3.6.22 | smoke verified candidate |
| 근접 전투 모션·공격감 개선 | Better Combat | 3.2.2 | conditional candidate; smoke verified only |
| 손에 든 발광 아이템 동적 조명 | LambDynamicLights | 4.12.4+26.2 | client-only pinned candidate; smoke verification required |
| 시간 차원 + 제작형 보스 6종 | Chrono Dawn | 0.12.0 | smoke verified boss candidate |
| 월드 제단/레이드 캐시/보스 차원 + 주요 보스 11종 | World Bosses | 2.0.2 | smoke verified boss candidate; visual warnings tracked |
| 반복형 절차 던전 + 던전 보스/후반 보스 | MCEndgame | 1.7.0+26.2 | smoke verified boss candidate; progression overlap tracked |

LambDynamicLights는 클라이언트 시각 편의 모드다. 횃불·랜턴 등 발광 아이템을 손에 들었을 때 주변을 동적으로 밝히지만 서버의 실제 block light, 몹 스폰 조건, 코어 보상 로직은 변경하지 않는다.

## 2. 주요 지원 라이브러리

- Fabric API `0.160.0+26.2`
- Lithostitched `1.8.0`
- Resourceful Lib `5.0.3`
- Puzzles Lib `26.2.3`
- Forge Config API Port `26.2.1`
- MidnightLib `1.9.3`
- Multiloader Data Extensions `26.2.0`
- GeckoLib `5.5.5`
- Player Animation Library `1.2.6+26.2`
- Cloth Config API `26.2.155`
- Architectury API `21.1.9`
- Fabric Language Kotlin `1.14.0+kotlin.2.4.20`

Fabric API는 Augmentation Survival 자체의 직접 구현 dependency이기도 하며 `0.160.0+26.2`로 명시적으로 고정한다.

Modrinth Maven의 전이 dependency만 믿지 않고 실제 `fabric.mod.json`을 검사해 필요한 지원 라이브러리를 명시적으로 핀한다.

## 3. 각 콘텐츠의 역할

### Terralith
오버월드 지형과 바이옴을 크게 확장한다. 바이옴 자체를 코어 한 등급과 묶지 않고, 해당 바이옴의 구조물·몹·탐험 콘텐츠가 여러 등급 분포를 갖도록 한다.

### Repurposed Structures + Dungeons and Taverns
코어를 자연스럽게 배치할 상자·던전·탐험 지점을 크게 늘린다. 기존 loot를 교체하지 않고 증강 코어 pool을 추가하는 것이 기본이다.

### Friends&Foes + Illager Invasion + Naturalist
일반 적, 일리저 전투, 야생 생태계를 확장한다. Naturalist의 평범한 동물을 코어 파밍 대상으로 만들지는 않는다.

### Farmer's Delight Refabricated
농업·음식·생산 증강이 실제로 쓸 콘텐츠를 늘린다. 별도의 RPG 성장축이 아니라 생존 루프 보강용이다.

### Better Combat
전투 체감 개선 후보. 증강의 공격 범위, 추가타, 광역 피해, 공속 변경과 실제 판정이 맞는지 플레이테스트가 필요하다.

### LambDynamicLights
동굴·야간 탐험의 시인성을 높인다. 클라이언트 전용 표현 효과이므로 적 성장, 실제 광원 레벨, 스폰 규칙과는 분리한다. RyoamicLights/Sodium Dynamic Lights 같은 다른 동적 조명 구현과 중복 설치하지 않는다.

## 4. 보스 콘텐츠 확장

### Chrono Dawn — 시간 차원과 보스 6종

전용 시간 테마 차원과 6종의 작성된 보스를 추가한다. 가장 중요한 장점은 제작자가 `BossDefeatedEvents` 애드온 API를 제공한다는 점이다.

증강 연동 방식:

- 공식 보스 처치 이벤트를 서버에서 수신
- 보스 ID를 증강 보상 프로필에 매핑
- 기존 보상을 유지하면서 추가 코어를 지급
- 정확한 등급 확률은 실제 보스 난이도 측정 후 확정

라이선스가 LGPL-3.0이므로 필요하면 호환을 위한 소스 수준 수정도 가능하다.

### World Bosses — 월드 곳곳의 주요 보스 11종

제단/의식, raid cache, 별도 보스 차원과 함께 현재 2.0.2 기준 11개의 주요 보스 보상 흐름을 제공한다. 실제 서버 로그에서 11종 raid cache 설정 로딩과 보스 차원 `worldbosses:worldbreaker` 등록을 확인했다.

증강 연동 방식:

- 외부 JAR을 수정하지 않는다.
- 우리 모드의 서버 사망 이벤트/entity namespace adapter로 보스 처치를 감지한다.
- 필요하면 World Bosses의 기존 raid cache loot에 코어 pool을 추가한다.
- 기존 보상은 보존한다.

이 모드는 All Rights Reserved이므로 직접 수정본을 재배포하는 방식은 사용하지 않는다.

### MCEndgame — 반복형 던전과 보스

절차 생성되는 후반 던전, 반복 가능한 던전 보스와 Beastweaver 계열 후반 보스 이벤트를 추가한다. 오픈소스 LGPL-3.0-or-later라 필요하면 소스 수준 호환도 가능하다.

주의점:

- 자체 장비/던전 성장축이 증강과 경쟁할 수 있다.
- 최종 팩에서는 증강이 주인공이 되도록 자체 보상·진행을 축소/조정할 가능성이 있다.
- 반복 보스에서 신화 코어가 무한히 쉽게 공급되지 않도록 별도 정책이 필요하다.
- 기본 설정은 익명 gameplay analytics가 켜져 있으므로 공식 팩 배포 설정에서는 `mcendgame.json`의 `sendAnalytics=false`를 기본으로 두는 방향을 사용한다.

## 5. 실제 JAR에서 확인한 연동 표면

Run 5에서는 22개 외부 JAR을 resolve했다. 이후 LambDynamicLights를 추가해 official pin은 23개가 되었으며, 최신 Build #431에서 pinned dependency inspection과 full-pack dedicated-server/client smoke가 통과했다. 실제 게임에서 동적 조명 동작과 외부 모드별 gameplay/visual 호환을 확인하는 일은 남아 있다.

Run 5 대표 결과:

- Terralith: loot 65 / structure 254 / biome 334
- Repurposed Structures: loot 219 / structure 3725 / biome 187
- Dungeons and Taverns (`mr_dungeons_andtaverns`): loot 679 / structure 5559 / biome 54
- Friends&Foes: loot 46 / structure 92 / biome 24
- Illager Invasion: loot 29 / structure 74 / biome 7
- Naturalist: loot 83 / biome 116
- Farmer's Delight Refabricated: loot 116 / structure 8 / biome 15 / recipe 589
- Chrono Dawn (`chronodawn`): loot 203 / structure 63 / biome 25 / recipe 415
- World Bosses (`worldbosses`): loot 51 / structure 36 / biome 5 / recipe 61
- MCEndgame (`mcendgame`): loot 5 / structure 67 / biome 4 / recipe 18

외부 구조물·몹·보스를 증강 코어 획득 루프에 결합할 데이터/이벤트 표면은 충분하다.

## 6. 증강 코어 연동 원칙

- `장소 = 코어 한 등급`으로 고정하지 않는다.
- 각 장소/몹/보스는 여러 인접 등급의 분포를 가진다.
- 기존 loot와 보스 보상은 가능한 한 보존한다.
- 중요 보상 판정은 서버 권한으로 처리한다.
- 일반 탐험은 일반~희귀, 위험 콘텐츠는 상위 등급 쪽으로 분포가 이동한다.
- 실제 보스는 평범한 구조물보다 명확하게 좋은 코어 획득처가 된다.
- 최고급 보스는 전설 중심 + 신화 가능 구조를 사용할 수 있으나 정확한 확률은 플레이테스트 전에 고정하지 않는다.

데이터 계약:

- `data/augmentation_survival/augmentation/core_distribution_contract.json`
- `data/augmentation_survival/augmentation/boss_core_reward_contract.json`

세 계약은 현재 구조물·적대몹·이벤트·낚시 및 보스 코어 보상 runtime에서 읽는다. `core_distribution_contract.json`, `boss_core_reward_contract.json`, `core_source_profiles.json`의 `runtime_active` 값은 실제 활성 상태를 표시하며, source identity 해석은 `core_source_profiles.json`과 `entity_compatibility_profiles.json`이 담당한다.

## 7. 외부 모드 수정 정책

우선순위:

1. 우리 모드의 loot/event/API 호환 코드
2. 데이터팩/resource overlay
3. 외부 모드 공식 config/API
4. 라이선스가 허용하고 실제 필요가 있을 때만 최소 소스 수정 또는 compatibility mixin/adapter

World Bosses처럼 ARR인 모드는 JAR 수정/재배포를 하지 않는다. Chrono Dawn과 MCEndgame은 LGPL 범위에서 필요 시 소스 호환 작업이 가능하다.

## 8. 자동 검증

`inspectPackDependencies`
- pinned JAR 실제 resolve
- Fabric metadata
- 중복 mod id
- loot/structure/biome/recipe 데이터 인벤토리

`runPackServer -PaugmentationPackRuntime=true`
- 같은 모드 조합의 dedicated server가 ready state까지 도달하는지 검사

`runPackClient -PaugmentationPackRuntime=true`
- Xvfb 가상 화면에서 client entrypoint, LWJGL/OpenGL, ResourceManager, texture atlas 초기화 후 smoke window 동안 치명적 종료가 없는지 검사

## 9. 검증 이력

### Run 1 — GitHub Actions 39893877935

- BUILD: PASS
- 초기 JAR inspection: PASS
- PACK SERVER: FAIL
- 첫 실질 실패: Repurposed Structures의 MidnightLib 누락
- 추가 발견: Illager Invasion의 Multiloader Data Extensions 누락
- 판정: 코드 충돌이 아니라 dependency manifest 누락

### Run 3 — GitHub Actions 39896924152

기존 17 JAR 기준:

- BUILD: PASS
- JAR RESOLUTION: PASS 17/17
- SERVER SMOKE: PASS
- CLIENT SMOKE: PASS

### Run 5 — GitHub Actions 39901741533

기존 22 JAR 기준:

- TOOLCHAIN: PASS — Java 25 / Gradle 9.5.1 / Loom 1.17.20
- BUILD: PASS
- JAR RESOLUTION: PASS — 22/22
- JAR METADATA/DATA INVENTORY: PASS
- RUNTIME CLASSPATH: PASS — 149 files
- PACK SERVER SMOKE: PASS
- PACK CLIENT SMOKE: PASS
- Chrono Dawn 차원 로딩 확인: `chronodawn:chronodawn`
- World Bosses 차원 로딩 확인: `worldbosses:worldbreaker`
- GeckoLib client resource load: 44 models / 41 animations

## 10. 현재 추적할 비차단 문제

- LambDynamicLights: client-only pin. 23-artifact client boot smoke는 #431에서 통과; 실제 dynamic-light 표현과 기존 렌더링 모드 공존은 플레이테스트 필요.
- Illager Invasion: Fabric에서 NeoForge 전용 `neoforge:acceptable_villager_distances` data-map을 무시한다. 실제 주민 감지거리 기능차는 플레이테스트 필요.
- Naturalist: 선택적 Field Guide 클래스가 없을 때 mixin target-not-found 경고가 3개 발생한다. client smoke는 통과.
- World Bosses: client resource load에서 `worldbreaker_*` 도구의 particle texture reference와 `varkhul_trophy`의 `minecraft:block/magma_block` 참조 경고가 발생했다. 실제 화면에서 깨지는지 시각 검수 필요.
- MCEndgame: 기본 analytics가 활성화된다. 공식 팩 설정에서는 비활성화를 우선한다.
- CI Linux: `libflite`/오디오 장치 부재로 narrator/OpenAL이 실패하지만 렌더링과 리소스 로딩은 정상 진행한다.
- Better Combat과 증강 공격 판정의 실제 공존은 아직 플레이테스트하지 않았다.

## 11. 현재 상태

최신 자동 검증은 Build Augmentation Survival Pack #431 (run ID `40795138452`)이다. 실제 job log에서 workflow의 `build` task graph가 `compileGametestJava`, `processGametestResources`, `gametestClasses`, `runGameTest`를 실행했으며, 74개 required GameTest가 모두 통과했다. 23 pinned dependency 검사, build, dedicated-server smoke, client boot smoke와 검증된 Modrinth pack 생성도 통과했다. 이 후속 turn에서는 이를 재실행하지 않았다. 수동 플레이 검증 상태는 `FINAL_AUDIT_2026-10-29.md` 참조.

- VERSION/PIN RESEARCHED: YES
- CURRENT PIN COUNT: 23 external artifacts
- JAR METADATA/DATA REVIEWED: YES — Build #431 handoff
- BUILD VERIFIED: YES — Build #431 handoff
- JAR PRODUCED: YES — verified Modrinth pack from Build #431 handoff
- PACK SERVER SMOKE TESTED: YES / PASS — Build #431 handoff
- PACK CLIENT BOOT SMOKE TESTED: YES / PASS — Build #431 handoff; visual gameplay remains untested
- LAMBDYNAMICLIGHTS 23-ARTIFACT CLIENT BOOT SMOKE: YES — Build #431 handoff; dynamic-light visual behavior not playtested
- BOSS PACK SERVER/CLIENT CO-LOAD: YES / PASS — previous 22-artifact baseline
- BOSS CORE REWARD CONTRACT ADDED: YES
- CORE LOOT INTEGRATION IMPLEMENTED: YES — structure, hostile/event, fishing and boss runtime services
- WORLDGEN DENSITY/VISUAL TESTED: NO
- BOSS GAMEPLAY TESTED: NO
- BETTER COMBAT + AUGMENT INTERACTION TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
