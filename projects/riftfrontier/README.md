# Riftfrontier — 균열 개척기

Riftfrontier는 Minecraft Java/NeoForge 26.2에서 개발하는 대형 **차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션** 프로젝트다.

## 현재 상태

`M2-B — GAMEPLAY ADAPTER + HUB FEEDBACK VERIFIED / REGION ENCOUNTER NEXT`

M0/M1의 빌드·runtime·content kernel 기반과 M2-A 원정 도메인 위에, 첫 production `region_01`의 실제 Minecraft gameplay adapter와 **원정 결과 → 거점 저장 → 보급 → 다음 원정 변화** 피드백 루프까지 연결했다.

현재 authoritative persistence root는 schema `3`이다. schema `2 → 3` migration은 기존 expedition run을 보존하면서 Region 01 secured salvage, expedition supply, region pressure를 추가한다.

기준 코드 커밋 `73b7d490ded496c7da24a5b658849b5476ecc16f`, GitHub Actions `Build Riftfrontier` run `34096694269`에서 **clean/unit test/build, native GameTest, dedicated server smoke, Xvfb client smoke, executable JAR 검사, report/artifact 단계가 모두 성공**했다.

따라서 다음 작업은 이미 검증된 원정 lifecycle/storage/supply를 반복하지 않고 **Region 01 환경 효과 + 실제 적 역할 2종 + elite 1종 + pressure 기반 위험 변화**를 실제 runtime encounter로 연결한다.

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
11. M2 gameplay 작업이면 `docs/M2B_GAMEPLAY_ADAPTER.md`
12. 디자인/자산 작업이면 `docs/REFERENCE_TARGETS.md`와 `THIRD_PARTY_ASSETS.md`

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

### Content/runtime

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
- gameplay read-only `ContentLookup`

### Persistence/expedition

- persistence schema root version `3` + explicit sequential migration registry
- schema `1 → 2`: expedition run domain
- schema `2 → 3`: hub salvage / supply / Region 01 pressure
- authoritative overworld-scoped `RiftfrontierWorldData` SavedData (`riftfrontier:world_state`)
- durable world revision / expedition sequence / active content fingerprint / expedition runs 저장
- durable `secured_region_01_salvage`, `expedition_supply`, `region_01_pressure`
- Minecraft/DFU와 분리된 순수 불변 `ExpeditionRun` 상태 머신: PREPARING → DEPLOYED → EXTRACTION_REQUESTED → EXTRACTED 또는 FAILED
- 별도 `ExpeditionRunCodec` persistence adapter
- `ExpeditionLifecycle`: region/contract/resource 소속 및 contract requirement 검증
- 서버 시작 시 active content fingerprint와 SavedData root 동기화

### M2 gameplay adapter

- fixture와 분리된 production `region_01` Region Pack
- temporary `/riftfrontier expedition start / extract / provision / abort / status` 입력 표면
- bounded technical hub/region cell 진입·귀환
- 실제 block interaction 기반 salvage 회수
- extraction retained salvage → authoritative hub storage 정산
- successful extraction → `region_01_pressure` 증가
- pressure에 따라 다음 Region 01 preparation supply cost 상승
- secured salvage 1 → expedition supply 2 provisioning
- 원정 시작 전 authoritative supply 소비; 부족하면 원정 생성 거부
- 사망/로그아웃/abort → FAILED, preparation supply 미환불
- technical cells/commands는 production art/UI가 아니며 디자인 gate 전 임시 검증 표면

### Verification

- `/riftfrontier runtime` read-only 진단 명령
- native Minecraft 26.2 test-function registry + data-driven `test_instance` GameTest
- GameTest에서 content runtime ↔ SavedData ↔ expedition lifecycle ↔ hub feedback 검증
- CI GameTest gate: non-zero 실행 marker + required tests passed marker 강제
- CI dedicated server/client smoke + executable JAR/SHA-256 검증

## 다음 개발 작업

이미 검증한 schema/gameplay adapter/storage/supply를 다시 넓히지 않는다. 다음은 **Region 01 원정 공간의 실제 위험/전투 상호작용**이다.

1. production `region_01` environment rule 1개를 server-authoritative runtime effect로 연결한다.
2. production content의 일반 적 역할 2종을 실제 spawn/defeat state에 연결한다.
3. elite 1종을 별도 역할과 최소 telegraph/counterplay 요구가 있는 encounter로 연결한다.
4. `region_01_pressure`가 environment 또는 encounter 위험 조건 하나 이상을 실제 변경하게 한다.
5. resource objective와 combat objective가 같은 원정에서 의미 있게 충돌하도록 composition한다.
6. native GameTest를 통과시킨다.
7. 실제 Minecraft 플레이 검수 전에는 전투/presentation 완료를 선언하지 않는다.

첫 `region_01`이 작은 DLC처럼 완결되기 전에는 추가 지역을 대량 생산하지 않는다.
