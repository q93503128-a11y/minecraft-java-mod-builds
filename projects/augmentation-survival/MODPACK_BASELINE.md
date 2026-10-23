# Official Small Modpack Baseline — M0

Date: 2026-10-23
Minecraft: 26.2
Loader: Fabric 0.19.5
Java: 25

이 문서는 Augmentation Survival을 중심으로 사용할 공식 소형 모드팩의 첫 동시 로딩 후보를 고정한다. `채택 후보`와 `런타임 검증 완료`는 같은 뜻이 아니다. 실제 CI와 플레이테스트를 통과하기 전까지는 후보 상태다.

## 1. 콘텐츠 모드 pins

| 역할 | 프로젝트 | 고정 버전 | Maven pin | 상태 |
|---|---|---:|---|---|
| 오버월드 바이옴/지형 | Terralith | 2.6.4 Fabric | `maven.modrinth:8oi3bsk5:OxfI2n80` | candidate |
| Terralith 필수 worldgen lib | Lithostitched | 1.8.0 Fabric 26.2 | `maven.modrinth:XaDC71GB:sGGUpIGT` | required candidate |
| 구조물 변형 | Repurposed Structures | 7.7.6+26.2-fabric | `maven.modrinth:muf0XoRe:7.7.6+26.2-fabric` | candidate |
| 던전/여관/구조물 | Dungeons and Taverns | 5.3.2+mod | `maven.modrinth:tpehi7ww:y9AUViOD` | candidate |
| 바닐라풍 몹 | Friends&Foes | 4.0.27+mc26.2 | `maven.modrinth:POQ2i9zu:rJBCX3gG` | candidate |
| 일리저/레이드 확장 | Illager Invasion | 26.2.0 Fabric | `maven.modrinth:jSV9w0J5:1EML9Osa` | candidate |
| 생태계/동물 | Naturalist | 2.0.6+26.2-fabric | `maven.modrinth:F8BQNPWX:zrGHo70e` | candidate |
| 농사/요리 | Farmer's Delight Refabricated | 26.2-3.6.22 | `maven.modrinth:7vxePowz:tLeFs9pK` | candidate |
| 전투 체감 | Better Combat | 3.2.2+26.2-fabric | `maven.modrinth:5sy6g3kz:enlZuzkJ` | conditional candidate |

## 2. 명시적 지원 라이브러리 pins

Modrinth Maven은 전이 dependency 정보를 신뢰할 수 있는 형태로 항상 제공하지 않으므로 required/support library는 명시적으로 핀한다.

| 라이브러리 | 버전 | Maven pin | 주 사용처 |
|---|---:|---|---|
| Resourceful Lib | 5.0.3 Fabric | `maven.modrinth:G1hIVOrD:SmfwDCLg` | Friends&Foes |
| Puzzles Lib | 26.2.3 Fabric | `maven.modrinth:QAGBst4M:KU5rZUAR` | Illager Invasion 계열 |
| Forge Config API Port | 26.2.1 Fabric | `maven.modrinth:ohNO6lps:rSd3GiG8` | Illager Invasion/config 계열 |
| GeckoLib | 5.5.5 Fabric | `maven.modrinth:8BmcQJ2H:7gaQHok7` | entity animation dependency coverage |
| Player Animation Library | 1.2.6+26.2 | `maven.modrinth:ha1mEyJS:Hq1CwB9w` | Better Combat |
| Cloth Config API | 26.2.155 Fabric | `me.shedaniel.cloth:cloth-config-fabric:26.2.155` | Better Combat |

Fabric API `0.160.0+26.2`는 프로젝트 기본 dependency로 별도 관리한다.

## 3. 왜 이 구성인가

### Terralith

오버월드 탐험의 지형·바이옴 밀도를 높인다. 바이옴 자체를 코어 단일 등급과 묶지 않고, 바이옴 태그와 그 안의 구조물/몹/코어 콘텐츠에 등급 분포를 부여한다.

주의: Terralith는 월드 생성 정체성을 크게 바꾸므로 기존 월드에 뒤늦게 추가·제거하는 것을 정상 지원 경로로 간주하지 않는다.

### Repurposed Structures + Dungeons and Taverns

증강 코어를 자연스럽게 배치할 상자·던전·탐험 지점을 크게 늘린다. 우리 호환 레이어는 원본 loot를 덮어쓰지 않고 코어 풀을 추가하는 방식이 기본이다.

두 구조물 모드와 Terralith를 동시에 사용하므로 실제 월드에서 구조물 밀도가 과해지는지는 별도 플레이테스트가 필요하다.

### Friends&Foes + Illager Invasion + Naturalist

전투 적과 비전투 생태계를 동시에 넓힌다. 모든 동물을 코어 파밍 대상으로 만들지는 않는다. Naturalist는 탐험·생존·비전투 증강의 활용 폭을 넓히는 역할이 우선이다.

Friends&Foes와 Illager Invasion은 일리저/Illusioner 계열 역할이 겹칠 가능성이 있으므로 실제 raid/entity 등록과 config를 확인해야 한다.

### Farmer's Delight Refabricated

농업·음식·생산 계열 증강이 실제로 쓸 콘텐츠를 만든다. 별도의 RPG 성장축이 아니라 Minecraft 생존 루프를 확장하기 때문에 방향과 맞는다.

### Better Combat

전투 체감 개선 후보. 다만 향후 증강의 공격 범위, 추가타, 광역 피해, 공속 변경이 Better Combat의 공격 판정과 충돌할 수 있으므로 `conditional candidate`다. 단순 동시 로딩만으로 최종 채택하지 않는다.

## 4. 증강 코어 연동 가능성

### 구조물 상자

가장 직접적인 연동 대상이다. 바닐라와 외부 모드 loot table에 우리 코어 pool을 추가한다.

예시 원칙:

- 폐광/소형 던전: 일반~희귀
- 중간 위험 구조물: 일반/고급~영웅
- 고위험 구조물: 희귀~전설, 아주 낮은 신화 가능

이는 예시 범위이며 실제 확률은 사용자 밸런스 단계에서 확정한다.

### 외부 몹

loot table이 있으면 data layer로, 자체 보상 코드라면 Fabric 서버 이벤트/호환 adapter로 추가한다. 중요 드랍 판정은 서버 권한으로 처리한다.

### 외부 바이옴

바이옴 자체가 코어를 직접 주는 구조보다 biome tag/worldgen modifier를 통해 코어 저장고, 희귀 자원, 특수 스폰 또는 탐험 목표를 배치하는 방식을 우선한다.

### 핵심 불변조건

`장소 = 코어 한 등급`으로 고정하지 않는다. 모든 콘텐츠는 필요에 따라 여러 인접 등급의 확률 분포를 가진다.

## 5. JAR 수정 정책

외부 모드 JAR을 직접 패치·재배포하는 것을 기본 해법으로 사용하지 않는다.

우선순위:

1. 우리 모드의 loot/event/data 호환 코드
2. 데이터팩/resource overlay
3. 외부 모드가 제공하는 config/API
4. 필요할 때만 최소 compatibility mixin/adapter

외부 모드 자산을 복사하거나 라이선스가 허용하지 않는 수정본을 번들하지 않는다.

## 6. 자동 검증

Gradle `inspectPackDependencies`는 pinned JAR을 실제로 resolve한 뒤 다음을 기록한다.

- JAR 존재/비어 있지 않은지
- `fabric.mod.json` id/version/environment/depends
- 중복 mod id
- loot table 엔트리 수와 샘플
- structure/worldgen 엔트리 수와 샘플
- biome/tag 엔트리 수와 샘플
- recipe 엔트리 수

`runPackServer -PaugmentationPackRuntime=true`는 한 런타임에서 dedicated server가 실제로 로딩 가능한지 검사한다.

## 7. 검증 상태

이 문서 작성 시점:

- VERSION/PIN RESEARCHED: YES
- SOURCE/METADATA PARTIALLY REVIEWED: YES
- DEPENDENCY RESOLUTION: PENDING CI
- BUILD VERIFIED: NO
- PACK SERVER TESTED: NO
- PACK CLIENT TESTED: NO
- PLAYTESTED: NO
- CORE LOOT INTEGRATION IMPLEMENTED: NO

CI 결과가 나오면 이 상태를 갱신한다.
