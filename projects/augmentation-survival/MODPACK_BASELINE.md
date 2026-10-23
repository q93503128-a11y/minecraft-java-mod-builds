# Official Small Modpack Baseline — M0

Date: 2026-10-23
Minecraft: 26.2
Loader: Fabric 0.19.5
Java: 25

이 문서는 Augmentation Survival을 중심으로 사용할 공식 소형 모드팩의 첫 동시 로딩 후보를 고정한다. `동시 부팅 검증 완료`와 `최종 채택`은 같은 뜻이 아니다. 실제 월드·전투·플레이테스트를 통과하기 전까지 콘텐츠 모드는 후보 상태다.

## 1. 콘텐츠 모드 pins

| 역할 | 프로젝트 | 고정 버전 | Maven pin | 상태 |
|---|---|---:|---|---|
| 오버월드 바이옴/지형 | Terralith | 2.6.4 Fabric | `maven.modrinth:8oi3bsk5:OxfI2n80` | smoke verified candidate |
| Terralith 필수 worldgen lib | Lithostitched | 1.8.0 Fabric 26.2 | `maven.modrinth:XaDC71GB:sGGUpIGT` | required / smoke verified |
| 구조물 변형 | Repurposed Structures | 7.7.6+26.2-fabric | `maven.modrinth:muf0XoRe:7.7.6+26.2-fabric` | smoke verified candidate |
| 던전/여관/구조물 | Dungeons and Taverns | 5.3.2+mod | `maven.modrinth:tpehi7ww:y9AUViOD` | smoke verified candidate |
| 바닐라풍 몹 | Friends&Foes | 4.0.27+mc26.2 | `maven.modrinth:POQ2i9zu:rJBCX3gG` | smoke verified candidate |
| 일리저/레이드 확장 | Illager Invasion | 26.2.0 Fabric | `maven.modrinth:jSV9w0J5:1EML9Osa` | smoke verified candidate; warning tracked |
| 생태계/동물 | Naturalist | 2.0.6+26.2-fabric | `maven.modrinth:F8BQNPWX:zrGHo70e` | smoke verified candidate; warning tracked |
| 농사/요리 | Farmer's Delight Refabricated | 26.2-3.6.22 | `maven.modrinth:7vxePowz:tLeFs9pK` | smoke verified candidate |
| 전투 체감 | Better Combat | 3.2.2+26.2-fabric | `maven.modrinth:5sy6g3kz:enlZuzkJ` | conditional candidate; smoke verified only |

## 2. 명시적 지원 라이브러리 pins

Modrinth Maven의 전이 dependency만 믿지 않고 실제 `fabric.mod.json`을 검사해 필요한 라이브러리를 명시적으로 핀한다.

| 라이브러리 | 버전 | Maven pin | 주 사용처 |
|---|---:|---|---|
| Resourceful Lib | 5.0.3 Fabric | `maven.modrinth:G1hIVOrD:SmfwDCLg` | Friends&Foes |
| Puzzles Lib | 26.2.3 Fabric | `maven.modrinth:QAGBst4M:KU5rZUAR` | Illager Invasion |
| Forge Config API Port | 26.2.1 Fabric | `maven.modrinth:ohNO6lps:rSd3GiG8` | Puzzles Lib / Illager Invasion |
| MidnightLib | 1.9.3+26.2-fabric | `maven.modrinth:codAaoxh:3uBvRFE9` | Repurposed Structures |
| Multiloader Data Extensions | 26.2.0 Fabric | `fuzs.multiloaderdataextensions:multiloaderdataextensions-fabric:26.2.0` | Illager Invasion |
| GeckoLib | 5.5.5 Fabric | `maven.modrinth:8BmcQJ2H:7gaQHok7` | 향후 애니메이션/호환 후보; M0 probe 유지 |
| Player Animation Library | 1.2.6+26.2 | `maven.modrinth:ha1mEyJS:Hq1CwB9w` | Better Combat |
| Cloth Config API | 26.2.155 Fabric | `me.shedaniel.cloth:cloth-config-fabric:26.2.155` | Better Combat |

Fabric API `0.160.0+26.2`는 프로젝트 기본 dependency로 별도 관리한다.

## 3. 왜 이 구성인가

### Terralith
오버월드 탐험의 지형·바이옴 밀도를 높인다. 바이옴 자체를 코어 단일 등급과 묶지 않고, 바이옴 태그와 그 안의 구조물/몹/코어 콘텐츠에 등급 분포를 부여한다.

주의: Terralith는 월드 생성 정체성을 크게 바꾸므로 기존 월드에 뒤늦게 추가·제거하는 것을 정상 지원 경로로 간주하지 않는다.

### Repurposed Structures + Dungeons and Taverns
증강 코어를 자연스럽게 배치할 상자·던전·탐험 지점을 크게 늘린다. 우리 호환 레이어는 원본 loot를 덮어쓰지 않고 코어 pool을 추가하는 방식이 기본이다.

두 구조물 모드와 Terralith를 동시에 사용하므로 실제 월드에서 구조물 밀도가 과해지는지는 별도 플레이테스트가 필요하다.

### Friends&Foes + Illager Invasion + Naturalist
전투 적과 비전투 생태계를 동시에 넓힌다. 모든 동물을 코어 파밍 대상으로 만들지는 않는다. Naturalist는 탐험·생존·비전투 증강의 활용 폭을 넓히는 역할이 우선이다.

Friends&Foes와 Illager Invasion은 일리저 계열 역할이 겹칠 가능성이 있으므로 실제 raid/entity 동작과 config를 확인해야 한다.

### Farmer's Delight Refabricated
농업·음식·생산 계열 증강이 실제로 쓸 콘텐츠를 만든다. 별도의 RPG 성장축이 아니라 Minecraft 생존 루프를 확장하기 때문에 방향과 맞는다.

### Better Combat
전투 체감 개선 후보. 향후 증강의 공격 범위, 추가타, 광역 피해, 공속 변경이 Better Combat의 공격 판정과 충돌할 수 있으므로 `conditional candidate`다. 단순 동시 로딩만으로 최종 채택하지 않는다.

## 4. 실제 JAR에서 확인한 증강 코어 연동 표면

최종 M0 probe는 17개 외부 JAR을 실제로 resolve하고 Fabric metadata와 데이터 엔트리를 검사한다.

대표 결과:

- Terralith: loot 65 / structure 254 / biome 관련 334
- Repurposed Structures: loot 219 / structure 관련 3725 / biome 관련 187
- Dungeons and Taverns (`mr_dungeons_andtaverns`): loot 679 / structure 관련 5559 / biome 관련 54
- Friends&Foes: loot 46 / structure 관련 92 / biome 관련 24
- Illager Invasion: loot 29 / structure 관련 74 / biome 관련 7
- Naturalist: loot 83 / biome 관련 116
- Farmer's Delight Refabricated: loot 116 / structure 관련 8 / biome 관련 15 / recipe 589

따라서 외부 구조물·몹·바이옴을 증강 코어 획득 루프에 결합할 데이터 표면은 충분히 존재한다.

### 구조물 상자
바닐라와 외부 모드 loot table에 원본 보상을 보존하면서 우리 코어 pool을 추가한다.

### 외부 몹
loot table이 있으면 data layer로, 자체 보상 코드라면 Fabric 서버 이벤트/호환 adapter로 추가한다. 중요 드랍 판정은 서버 권한으로 처리한다.

### 외부 바이옴
바이옴 자체가 코어를 직접 주는 구조보다 biome tag/worldgen hook를 통해 코어 저장고, 희귀 자원, 특수 스폰 또는 탐험 목표를 배치하는 방식을 우선한다.

### 핵심 불변조건
`장소 = 코어 한 등급`으로 고정하지 않는다. 모든 콘텐츠는 필요에 따라 여러 인접 등급의 확률 분포를 가진다. 정확한 확률은 플레이테스트 전 임의 확정하지 않는다.

## 5. 코어 분포 데이터 계약

`data/augmentation_survival/augmentation/core_distribution_contract.json`에 M1 구현이 읽을 수 있는 분포 계약을 추가했다.

현재는 의도적으로 `runtime_active=false`, `exact_weights_defined=false`다.

정의된 범위:

- 일반 탐험: 일반 ~ 희귀
- 위험한 중간 콘텐츠: 고급 ~ 영웅
- 위험한 상위 콘텐츠: 희귀 ~ 전설
- 최고난도 콘텐츠: 전설 ~ 신화, 전설 중심

이 파일은 확률표나 실제 드랍 구현이 아니라 다음 단계의 데이터 구조 기준이다.

## 6. JAR 수정 정책

외부 모드 JAR을 직접 패치·재배포하는 것을 기본 해법으로 사용하지 않는다.

우선순위:
1. 우리 모드의 loot/event/data 호환 코드
2. 데이터팩/resource overlay
3. 외부 모드가 제공하는 config/API
4. 필요할 때만 최소 compatibility mixin/adapter

외부 모드 자산을 복사하거나 라이선스가 허용하지 않는 수정본을 번들하지 않는다.

## 7. 자동 검증

`inspectPackDependencies`는 pinned JAR을 실제로 resolve한 뒤 JAR 존재, Fabric metadata, 중복 mod id, loot/structure/biome/recipe 엔트리를 기록한다.

`runPackServer -PaugmentationPackRuntime=true`는 dedicated server가 같은 런타임 조합으로 준비 완료 상태까지 도달하는지 검사한다.

`runPackClient -PaugmentationPackRuntime=true`는 GitHub Actions의 Xvfb 가상 화면에서 클라이언트를 기동해 Fabric client entrypoint, LWJGL/OpenGL 초기화, ResourceManager reload, texture atlas 생성까지 도달한 뒤 120초 smoke window 동안 치명적 종료가 없는지 검사한다.

## 8. 검증 이력 / 현재 상태

### Run 1 — GitHub Actions 39893877935

- TOOLCHAIN: PASS — Java 25 / Gradle 9.5.1 / Loom 1.17.20
- BUILD: PASS
- INITIAL 15 ARTIFACT RESOLUTION: PASS
- JAR METADATA/DATA INVENTORY: PASS
- RUNTIME CLASSPATH RESOLUTION: PASS
- PACK SERVER: FAIL
- FIRST REAL FAILURE: Repurposed Structures가 요구하는 `midnightlib` 누락
- FOLLOW-UP FOUND BY METADATA AUDIT: Illager Invasion이 `multiloaderdataextensions`도 hard dependency로 선언
- 판정: 코드 회귀나 모드 간 충돌이 아니라 dependency manifest 누락

이후 두 라이브러리를 명시적으로 추가했다.

### Run 3 — GitHub Actions 39896924152

- TOOLCHAIN: PASS
- BUILD: PASS
- PINNED ARTIFACT RESOLUTION: PASS — 17/17
- JAR METADATA/DATA INVENTORY: PASS
- RUNTIME CLASSPATH RESOLUTION: PASS — 144 files
- PACK SERVER SMOKE: PASS — dedicated server ready state 도달
- PACK CLIENT SMOKE: PASS — client renderer/resource 초기화 후 120초 유지

확인된 비차단 경고:

- Illager Invasion 리소스에 포함된 `neoforge:acceptable_villager_distances` data-map은 Fabric에서 존재하지 않는 NeoForge data-map으로 무시된다. 서버/클라이언트 smoke에는 영향이 없었지만 주민의 적대 몹 감지거리 동작이 NeoForge와 완전히 같은지는 플레이테스트 전까지 미검증으로 둔다.
- Naturalist가 외부 Field Guide 클래스 3개를 대상으로 하는 mixin target-not-found 경고를 출력한다. 현재 조합에서는 치명적 mixin 실패 없이 client smoke를 통과했다. 실제 Field Guide 연동 기능이 필요한지는 추후 플레이테스트에서 판단한다.
- CI Linux 환경에는 `libflite`와 실제 오디오 장치가 없어 narrator/OpenAL 초기화가 실패한다. 렌더러와 리소스 로딩은 계속 정상 진행됐으므로 모드팩 충돌로 분류하지 않는다.
- Mod Menu/YACL 권장 dependency, 개발환경 refmap/translation 경고는 현재 hard dependency가 아니며 smoke를 차단하지 않는다.

현재 상태:

- VERSION/PIN RESEARCHED: YES
- JAR METADATA/DATA REVIEWED: YES — 17 pinned artifacts
- BUILD VERIFIED: YES — Run 3
- JAR PRODUCED: YES — Run 3 artifact
- PACK SERVER SMOKE TESTED: YES / PASS
- PACK CLIENT SMOKE TESTED: YES / PASS
- CORE DISTRIBUTION CONTRACT ADDED: YES
- CORE LOOT INTEGRATION IMPLEMENTED: NO
- WORLDGEN DENSITY/VISUAL TESTED: NO
- BETTER COMBAT + AUGMENT INTERACTION TESTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
