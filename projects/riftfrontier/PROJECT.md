# Riftfrontier — 균열 개척기

- Slug: `riftfrontier`
- Mod ID: `riftfrontier`
- Namespace: `riftfrontier`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `riftfrontier-0.1.0-alpha.1.jar`
- Existing-world compatibility: 첫 플레이어블 알파 이전에는 세이브 스키마를 실험할 수 있다. 첫 플레이어블 알파 이후부터 registry ID, 저장 키, content ID를 고정하고 migration을 우선한다.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: M0/M1에서 현재 26.2 호환성과 유지보수 상태를 다시 검증한 뒤 목적별로 단일 선택한다. 애니메이션은 GeckoLib 계열을 우선 검토하고, 복잡한 AI/UI 라이브러리는 실제 필요가 생긴 뒤 추가한다.
- Forbidden bundled dependencies: Minecraft 원본 파일, NeoForge 배포 파일, 외부 모드 JAR, 재배포 권한이 불명확한 모델·텍스처·음원·UI 자산
- Datagen task: M0 bootstrap에서 추가
- GameTest task: M1 content kernel에서 추가
- Server smoke-test task: M0 bootstrap에서 추가
- Client smoke-test task: M0 bootstrap에서 추가

## 프로젝트 정체성

Riftfrontier는 단순한 RPG 콘텐츠 팩이나 차원 추가 모드가 아니다.

**차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션 + 동적 세계 사건**을 하나의 게임 루프로 연결하는 대형 Minecraft total-conversion 지향 프로젝트다.

플레이어는 고정된 용사 역할을 강요받지 않는다. 탐험가, 사냥꾼, 용병, 기술자, 상인, 제작자, 물류 운영자, 전초기지 창립자 등 서로 다른 삶의 경로가 같은 세계 시스템과 연결되어야 한다.

## 핵심 게임 루프

```text
거점에서 준비
→ 균열/미개척 지역 진입
→ 탐사·사냥·의뢰·자원 회수·사건 해결
→ 귀환/철수
→ 장비·기술·시설·물류망에 투자
→ 세력/경제/위험도가 반응
→ 더 깊은 지역과 더 복잡한 선택지 개방
→ 세계 규모의 사건과 고난도 원정으로 확장
```

## 절대 핵심 규칙

1. `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`를 항상 상위 공용 규칙으로 따른다.
2. 프로젝트 작업 시작 시 반드시 `docs/CANONICAL.md`를 먼저 읽고 방향 잠금을 확인한다.
3. 차원/바이옴/몹/아이템의 개수만 늘려 규모를 과장하지 않는다. **시스템 간 상호작용과 지역 하나당 제작 밀도**를 우선한다.
4. 하나의 지역은 작은 DLC 수준의 완결 패키지로 만든다. 지형, 생태, 전투, 자원, 구조물, 사건, 던전/보스, 보상, 사운드, UI 연동까지 함께 완성한다.
5. 대량 콘텐츠는 Java 클래스를 하나씩 복제하지 않는다. 공통 builder/registry, data schema, datagen, validator, asset resolver, behaviour library 위에 얹는다.
6. 플레이어가 조합으로 새로운 플레이를 만들 수 있는 시스템을 선호한다. 개발자가 수백 개의 완성 스킬을 직접 만드는 것보다 재사용 가능한 동작/효과/증강 조합을 우선한다.
7. 세계 시뮬레이션은 의미 있는 상태 변화에 반응하는 event/request 기반을 우선한다. 매 tick 전체 월드 스캔으로 규모를 만들지 않는다.
8. 중요한 상태는 서버 권위로 처리한다. 클라이언트는 입력·표시·애니메이션·VFX를 담당한다.
9. UI/모델/애니메이션/사운드의 스타일을 AI가 즉석에서 발명하지 않는다. 실제 게임·대형 모드·공식 스크린샷·허용 자산을 조사하고, 프로젝트에 맞게 조합·수정한 뒤 실제 Minecraft 화면으로 검수한다.
10. 외부 프로젝트에서 가져오는 것은 **설계 원리와 제작 패턴**이다. 라이선스가 허용되지 않는 코드·자산을 복제하지 않는다.
11. 기능이 돌아간다는 이유만으로 완료하지 않는다. 실제 플레이, 시각 검수, GameTest, 성능 측정, 빌드/JAR 검증까지 품질 게이트를 통과해야 한다.
12. 범위가 커질수록 넓고 얕게 만들지 않는다. 먼저 수직 구간 하나를 상용 게임에 가까운 품질로 완성하고, 검증된 생산 체계를 복제해 확장한다.
13. 다른 프로젝트의 코드·자산·워크플로를 복제하지 않는다. 공용 표준과 공개 기술 자료는 참고하되 Riftfrontier의 구현은 독립적으로 유지한다.

## 정본 읽기 순서

작업자는 매 작업 시작 시 최소한 다음을 읽는다.

1. `/AGENTS.md`
2. `/docs/BUILD_STANDARD.md`
3. `/docs/QUALITY_STANDARD.md`
4. `/projects/riftfrontier/PROJECT.md`
5. `/projects/riftfrontier/docs/CANONICAL.md`
6. 작업 성격에 따라 `GAME_DESIGN_MASTER.md`, `CONTENT_ARCHITECTURE.md`, `REFERENCE_TARGETS.md`, `ROADMAP.md`

## 현재 단계

`M0 — Canon & Bootstrap`

현재 커밋은 프로젝트 방향, 시스템 경계, 콘텐츠 생산 규칙을 고정하는 단계다. 다음 구현 단계에서는 이 정본을 훼손하지 않고 빌드 가능한 NeoForge 26.2 프로젝트와 content kernel을 만든다.
