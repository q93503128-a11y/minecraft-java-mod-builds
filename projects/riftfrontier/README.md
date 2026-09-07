# Riftfrontier — 균열 개척기

Riftfrontier는 Minecraft Java/NeoForge 26.2에서 개발하는 대형 **차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션** 프로젝트다.

## 현재 상태

`M0 — CANON LOCKED / BUILD BOOTSTRAP NEXT`

현재는 방향과 생산 구조 정본을 먼저 고정했다. 아직 실행용 JAR이 있는 상태가 아니며, 다음 단계에서 NeoForge 프로젝트 bootstrap과 첫 clean build를 진행한다.

## 작업 시작 시 반드시 읽기

1. `/AGENTS.md`
2. `/docs/BUILD_STANDARD.md`
3. `/docs/QUALITY_STANDARD.md`
4. `PROJECT.md`
5. `docs/CANONICAL.md`
6. 작업 성격에 따라 아래 문서

### 프로젝트 정본

- `docs/GAME_DESIGN_MASTER.md` — 시스템 수준 게임 디자인
- `docs/CONTENT_ARCHITECTURE.md` — 대량 콘텐츠 생산 구조
- `docs/REFERENCE_TARGETS.md` — 대형 모드 조사에서 가져올 설계 원리
- `docs/ROADMAP.md` — milestone과 범위 제어
- `THIRD_PARTY_ASSETS.md` — 외부 자산 추적

## 방향 요약

```text
Prepare
→ Expedition
→ Explore/Fight/Gather
→ Decide/Extract
→ Invest in Gear/Research/Infrastructure
→ Faction/Economy/Threat responds
→ Next expedition changes
```

대형화 원칙:

- 콘텐츠 숫자보다 시스템 상호작용
- Java 클래스 복제보다 schema/builder/validator
- 지역 수보다 Region Pack 완성도
- 완성 스킬 수백 개보다 의미 있는 composition
- tick 전수 스캔보다 event/request
- 임의 디자인보다 실제 레퍼런스와 검증된 외부 자산
- 코드 성공보다 실제 Minecraft 화면/플레이/성능 검수

## 다음 개발 작업

`docs/ROADMAP.md`의 M0 bootstrap:

- NeoForge 26.2 프로젝트 골격
- Gradle/ModDevGradle 설정
- mod metadata
- 최소 common/client bootstrap
- datagen/server/client run task
- workflow/JAR verify
- clean build + smoke test

M0가 끝난 뒤 M1 Content Kernel에서 schema, registry, resolver, validator를 먼저 구현하고, 그 위에서 첫 vertical slice를 만든다.
