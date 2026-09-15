# EARTH TO STARS

Minecraft Java `26.2` / Fabric 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **현재 상태: `0.3.0-alpha.1` FABRIC STANDALONE REBASE — 외부 우주/함선 모드 전체를 런타임 의존성으로 요구하지 않는 26.2 독립형 기반으로 전환 중. 기존 loader-neutral ship kernel과 CC0 자산은 보존하고, Minecraft 연동부는 Fabric으로 실제 포팅한다.**

## 한 줄 설명

오버월드를 지구로 두고 Minecraft 생존에서 시작해, 직접 조종하는 함선으로 대기권을 벗어나 우주를 탐험하고 하나의 authored modular ship을 이동수단 → 집 → 공장 → 전함으로 성장시키는 대형 게임이다.

## 현재 기술 스택

```text
Minecraft 26.2
Fabric Loader 0.19.5
Fabric API 0.160.0+26.2
Java 25
Gradle 9.5.1
EARTH TO STARS standalone game/runtime
```

현재 제품 계약상 플레이어에게 Valkyrien Skies, Genesis, ZPS, ZPL 같은 별도 우주/함선 모드를 설치하도록 요구하지 않는다. 외부 프로젝트는 코드·알고리즘·모델·텍스처·UI·사운드 등의 **선별적 재사용/포팅/레퍼런스** 대상으로 활용하고, 실제 재사용은 라이선스와 출처를 `THIRD_PARTY_ASSETS.md`에 기록한다.

## 왜 다시 26.2 / Fabric인가

1.20.1 Forge reboot는 실제 block ship 기술 검증에는 성공했지만, 게임이 여러 외부 모드 설치에 종속되는 구조가 되었다. 이는 “외부 구현을 개발 재료로 적극 활용하되 ETS 자체가 하나의 게임처럼 설치·플레이되어야 한다”는 제품 방향과 맞지 않는다.

Fabric 26.2 rebase에서는:

- 현재 버전의 Minecraft와 Java 25를 사용하고,
- Fabric API 외 런타임 의존성을 최소화하며,
- 기존 ETS의 loader-neutral 도메인/시스템 코드는 보존하고,
- 실패했던 old 26.2 Minecraft glue를 그대로 되살리지 않고,
- 허용되는 외부 코드/자산을 선별적으로 가져와 Fabric 방식으로 통합한다.

## 반드시 읽는 순서

1. 저장소 `/AGENTS.md`
2. 저장소 `/docs/BUILD_STANDARD.md`
3. 저장소 `/docs/QUALITY_STANDARD.md`
4. 저장소 `/docs/QUALITY_STANDARD_GAME_DESIGN.md`
5. 이 프로젝트 `PROJECT.md`
6. 이 프로젝트 `AGENTS.md`
7. `docs/00_MASTER_GAME_DESIGN.md`
8. `docs/07_FABRIC_26_2_STANDALONE_REBASE.md`
9. 현재 작업 분야 문서
10. `THIRD_PARTY_ASSETS.md`
11. 실제 source/resource와 최신 playtest 기록

`docs/06_VS_GENESIS_REBOOT.md`는 1.20.1 Forge 기술 실험 기록으로 남지만 현재 기술 정본이 아니다.

## 핵심 제품 규칙

- Overworld = Earth.
- Nether / End는 메인 진행 필수가 아니다.
- 함선은 unrestricted moving-block sandbox가 아니라 **B형 authored modular Ship Object**다.
- surface → atmosphere → space는 플레이어 관점에서 연속된 여행이어야 한다.
- 중요 상태는 server-authoritative다.
- manual / auto turret은 같은 authoritative weapon state를 공유한다.
- solo는 automation으로 가능하고 multiplayer 역할은 강제 노동이 아니다.
- Power / Ammo / Sensor / Propellant / Oxygen 등은 함선 수준의 중앙 simulation을 지향한다.
- 최종 ship/UI/weapon/space visual은 외부 reference/asset gate를 통과해야 한다.
- 외부 모드는 통째로 설치시키기보다 라이선스가 허용하는 코드/리소스를 선별적으로 포팅한다.

## 현재 Fabric migration boundary

현재 Fabric artifact에 우선 포함하는 것은 기존 코드 중 loader-neutral한 핵심이다.

```text
ship identity / ownership / permissions
module catalog / slots / instances
power / ammo / propellant / oxygen
sensor / turret domain logic
flight/control math regression kernel
interior assignment
launch readiness / first-orbit progression
ship-state codec / migration tests
```

NeoForge registry/network/save/render/world glue와 1.20.1 Forge reboot stack은 현재 Fabric artifact에서 제외되어 있다. 파일을 삭제하거나 가짜 API로 우회한 것이 아니라, 기능 단위로 실제 Fabric 구현으로 교체하기 위한 migration boundary다.

## 외부 자산

Kenney Space Kit CC0 함선 mesh 3종은 이미 출처를 기록해 repository에 vendoring되어 있다. Fabric 시각 파이프라인에서도 재사용 후보지만 old NeoForge OBJ adapter를 그대로 가정하지 않고 실제 Fabric 렌더링 방식에 맞게 다시 연결한다.

## 현재 검증 상태

- Fabric 26.2 build contract: **CODE REVIEWED**
- loader-neutral kernel regression tests: **PENDING CI**
- Fabric build: **PENDING CI**
- production Fabric JAR: **PENDING CI**
- dedicated Fabric boot: **PENDING CI**
- Fabric client smoke: **PENDING CI**
- actual starter craft flight: **NOT PLAYTESTED**
- Earth→space continuity: **NOT TESTED**
- multiplayer: **NOT TESTED**

## 다음 실제 작업

```text
Fabric build/kernel gate 확정
→ registry/network/save authority bridge
→ 외부 허용 자산 기반 starter craft visual
→ pilot seat + control session
→ 외부 오픈소스 연구를 반영한 collision/movement
→ 실제 power/propellant/oxygen 연결
→ atmosphere→space continuity
→ Earth 귀환
→ 실제 플레이 감각 검수
```

Moon/asteroid 콘텐츠 확장은 이 starter-craft vertical slice가 실제 플레이에서 합격한 뒤 진행한다.
