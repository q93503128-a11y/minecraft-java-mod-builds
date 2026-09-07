# Riftfrontier — 균열 개척기

Riftfrontier는 Minecraft Java/NeoForge 26.2에서 개발하는 대형 **차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션** 프로젝트다.

## 현재 상태

`M2 — EXPEDITION DOMAIN FOUNDATION IMPLEMENTED / GAMEPLAY INTEGRATION NEXT`

M0/M1의 빌드·runtime·content kernel 기반 위에 M2 첫 원정 도메인 경계를 구현했다. `region / expedition_resource / contract / extraction_result`가 하나의 검증된 content graph로 연결되고, 실제 원정은 별도의 불변 `ExpeditionRun`과 `ExpeditionLifecycle`을 통해 서버 권위 SavedData에 저장된다.

Persistence root는 schema `2`로 올라갔으며 schema `1 → 2` migration이 명시되어 있다. 첫 vertical-slice fixture에는 salvage resource, salvage recovery contract, secured-return extraction policy가 들어가며 JUnit과 native GameTest가 상태 전이와 실제 authoritative SavedData 업데이트를 검증하도록 확장됐다.

**현재 M2 변경의 전체 CI gate가 green인지 확인되기 전에는 이 기반을 완료로 선언하지 않는다.** 직전 M1 기준 전체 runtime gate는 이미 검증되었다.

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
10. expedition 작업이면 `docs/EXPEDITION_RUNTIME.md`
11. 디자인/자산 작업이면 `docs/REFERENCE_TARGETS.md`와 `THIRD_PARTY_ASSETS.md`

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
- typed definitions: combat archetype / region / loot profile / creature / encounter / expedition resource / contract / extraction result
- isolated document decode + duplicate definition 거부
- merge 후 cross-document reference graph validation
- region ↔ expedition resource / contract 및 contract → resource / loot / extraction-result 참조 검증
- optional `depends_on`, dependency 존재/self/duplicate/cycle 검증
- deterministic topological pack ordering
- resource identifier 기반 pack provenance
- ERROR/WARN validator + stable machine-readable issue code
- deterministic `ContentCatalog` SHA-256 fingerprint
- `ContentRuntimeSnapshot` + atomic last-known-good publication
- NeoForge server ResourceManager reload listener
- persistence schema root version `2` + explicit sequential migration registry
- schema `1 → 2` migration에서 persisted expedition domain 도입
- authoritative overworld-scoped `RiftfrontierWorldData` SavedData (`riftfrontier:world_state`)
- durable world revision / expedition sequence / active content fingerprint / expedition runs 저장
- 불변 `ExpeditionRun` 상태 머신: PREPARING → DEPLOYED → EXTRACTION_REQUESTED → EXTRACTED 또는 FAILED
- `ExpeditionLifecycle`: region/contract/resource 소속 및 contract requirement 검증
- 서버 시작 시 active content fingerprint와 SavedData root 동기화
- `/riftfrontier runtime` read-only 진단 명령
- native Minecraft 26.2 test-function registry + data-driven `test_instance` GameTest
- GameTest에서 content runtime ↔ authoritative SavedData ↔ expedition lifecycle 연동 검증
- CI GameTest gate: non-zero 실행 marker + required tests passed marker 강제
- CI dedicated server/client smoke + executable JAR/SHA-256 검증

## 다음 개발 작업

이제 M1/M2 기반 schema를 반복해서 늘리지 않는다. 다음은 **첫 실제 플레이 가능한 Expedition vertical slice 연결**이다.

1. technical fixture와 분리된 production `region_01` Region Pack을 만든다.
2. 거점에서 contract 선택 → authoritative `ExpeditionRun` 생성 서버 경로를 만든다.
3. 실제 region 진입/배치와 expedition resource 회수 이벤트를 연결한다.
4. contract objective 충족 → extraction request → 귀환/정산을 Minecraft 플레이에 연결한다.
5. extraction result의 retained resource / threat delta / world consequence가 실제 다음 원정 상태에 영향을 주게 한다.
6. 사망·중도 이탈·강제 실패 시 `FAILED` 처리와 보존/손실 규칙을 명시한다.
7. 이 최소 루프가 GameTest와 실제 플레이에서 검증된 뒤에 전투/보스 presentation과 물류/산업을 넓힌다.

첫 `region_01`이 작은 DLC처럼 완결되기 전에는 추가 지역을 대량 생산하지 않는다.
