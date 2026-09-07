# Riftfrontier — 균열 개척기

Riftfrontier는 Minecraft Java/NeoForge 26.2에서 개발하는 대형 **차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션** 프로젝트다.

## 현재 상태

`M1 — RUNTIME FOUNDATION VERIFIED / M2 SCHEMA BOUNDARY NEXT`

M0의 빌드/JAR/runtime smoke 기반에 이어 M1의 content kernel과 authoritative runtime 기반을 실제 Minecraft에서 검증했다. stable content ID, typed JSON definition, merged reference validation, deterministic catalog fingerprint, server ResourceManager reload, last-known-good atomic snapshot, pack dependency/provenance, machine-readable validator code, sequential persistence migration에 더해 **실제 Minecraft SavedData root, read-only runtime diagnostics, native 26.2 GameTest와 CI gate**가 연결되어 있다.

검증된 기준 커밋 `c307034286dd62d6df71bea47cf721ede1d75957`의 GitHub Actions에서 clean/test/build, `runGameTestServer`, dedicated server smoke, Xvfb client smoke, executable JAR 검사가 모두 통과했다. GameTest 서버 로그에는 non-zero 테스트 실행과 required test 전체 통과가 실제로 기록되었다.

## 작업 시작 시 반드시 읽기

1. `/AGENTS.md`
2. `/docs/BUILD_STANDARD.md`
3. `/docs/QUALITY_STANDARD.md`
4. `PROJECT.md`
5. `docs/CANONICAL.md`
6. `docs/GAME_DESIGN_MASTER.md`
7. `docs/CONTENT_ARCHITECTURE.md`
8. `docs/ROADMAP.md`
9. 현재 content/runtime/persistence를 다루면 `docs/CONTENT_RUNTIME.md`
10. 디자인/자산 작업이면 `docs/REFERENCE_TARGETS.md`와 `THIRD_PARTY_ASSETS.md`

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

## 현재 구현된 기반

- versioned JSON content documents (`schema_version = 1`)
- stable `ContentId`
- typed core definitions: combat archetype / region / loot profile / creature / encounter
- isolated document decode + duplicate definition 거부
- merge 후 cross-document reference graph validation
- optional `depends_on`, dependency 존재/self/duplicate/cycle 검증
- deterministic topological pack ordering
- resource identifier 기반 pack provenance
- ERROR/WARN validator + stable machine-readable issue code
- deterministic `ContentCatalog` SHA-256 fingerprint
- `ContentRuntimeSnapshot` + atomic last-known-good publication
- NeoForge server ResourceManager reload listener
- persistence schema root version `1` + explicit sequential migration registry
- authoritative overworld-scoped `RiftfrontierWorldData` SavedData (`riftfrontier:world_state`)
- durable world revision / expedition sequence / active content fingerprint 저장
- 서버 시작 시 active content fingerprint와 SavedData root 동기화
- `/riftfrontier runtime` read-only 진단 명령
- native Minecraft 26.2 test-function registry + data-driven `test_instance` GameTest
- GameTest에서 active content snapshot ↔ authoritative SavedData mutation/linkage 검증
- CI GameTest gate: non-zero 실행 marker + required tests passed marker 강제
- CI dedicated server smoke: ready/content/authoritative world root 확인
- CI Xvfb client smoke: Riftfrontier initialization + fatal crash marker 부재 확인
- executable JAR structure + required GameTest asset + SHA-256 검증

## 다음 개발 작업

이제 M1에서 미완성 기반을 반복 확장하지 않는다. 다음 우선순위는 **M2 첫 Expedition vertical slice가 실제 데이터와 저장 상태로 존재하기 위한 schema 경계**다.

1. `region`을 실제 원정 단위로 확장하고 `expedition_resource`, `contract`, `extraction/result` 정의의 책임 경계를 고정한다.
2. 위 정의를 Java class 복제 없이 추가할 수 있도록 codec/builder/validator/reference rule을 함께 만든다.
3. SavedData root 아래 expedition/domain state adapter를 추가해 시작 → 진행 → 철수/실패 결과를 서버 권위로 저장한다.
4. 첫 `region_01` pack을 작은 DLC 단위로 작성하고 중앙 거점 ↔ 진입 ↔ 목표/회수 ↔ 철수 최소 루프를 GameTest 가능한 형태로 만든다.
5. 그 뒤에야 실제 전투/보스 presentation과 물류/산업을 넓힌다.

새 시스템을 넓히기 전에 각 schema와 lifecycle이 자동 검증 가능하고 기존 world state를 깨지 않는지 확인한다.
