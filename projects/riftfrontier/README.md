# Riftfrontier — 균열 개척기

Riftfrontier는 Minecraft Java/NeoForge 26.2에서 개발하는 대형 **차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션** 프로젝트다.

## 현재 상태

`M2-B — REGION 01 ENCOUNTER RUNTIME VERIFIED / FIELD PLAY REVIEW NEXT`

M0/M1의 빌드·runtime·content kernel, M2-A 원정 도메인, M2-B의 **원정 결과 → 거점 저장 → 보급 → 다음 원정 변화** 위에 첫 production `region_01`의 실제 환경 위험과 encounter runtime까지 연결했다.

현재 authoritative persistence root는 schema `3`이다. 이번 encounter 묶음은 새 저장 schema를 만들지 않고 기존 `region_01_pressure`를 실제 전투/환경 조건에 사용한다.

검증 기준 코드 커밋은 `a7791123eade916c14041b4d32306c4befd8fc6a`, GitHub Actions `Build Riftfrontier` run `34099256007`이다. **clean/unit test/build, required native GameTest, dedicated server smoke, Xvfb client smoke, executable JAR 검사, report/artifact 단계가 모두 성공**했다.

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
- authoritative overworld-scoped `RiftfrontierWorldData` SavedData (`riftfrontier:world_state`)
- durable world revision / expedition sequence / active content fingerprint / expedition runs
- durable `secured_region_01_salvage`, `expedition_supply`, `region_01_pressure`
- Minecraft/DFU와 분리된 순수 불변 `ExpeditionRun` 상태 머신
- 별도 `ExpeditionRunCodec` persistence adapter
- `ExpeditionLifecycle`: region/contract/resource 소속 및 contract requirement 검증

### M2 gameplay adapter / Region 01

- fixture와 분리된 production `region_01` Region Pack
- temporary `/riftfrontier expedition start / extract / provision / abort / status` 입력 표면
- bounded technical hub/region cell 진입·귀환
- 실제 block interaction 기반 salvage 회수
- extraction retained salvage → authoritative hub storage 정산
- secured salvage 1 → expedition supply 2 provisioning
- 원정 시작 전 authoritative supply 소비; 부족하면 원정 생성 거부
- 사망/로그아웃/abort → FAILED, preparation supply 미환불
- successful extraction → `region_01_pressure` 증가 → 다음 preparation supply cost 상승
- **Region 01 hunter 역할:** 근거리 추격 technical proxy
- **Region 01 scout 역할:** 원거리 압박 technical proxy
- **elite anchor 역할:** 단순 체력 배수 대신 방패 stun counterplay가 존재하는 Ravager technical proxy
- salvage 회수 시 server-authoritative rift-drag movement hazard 적용
- pressure가 높을수록 hunter/scout 수와 hazard 지속시간/강도가 실제 증가
- patrol을 제거하고 철수하면 retained salvage +1 bonus; 빠른 철수는 가능하지만 bonus를 포기
- 원정 종료/실패 시 해당 run encounter entity 정리
- vanilla entity는 M2 behaviour 검증용 proxy일 뿐 production art/최종 AI가 아님

### Verification

- `/riftfrontier runtime` read-only 진단 명령
- native Minecraft 26.2 test-function registry + data-driven required GameTest
- authoritative runtime/state GameTest
- Region 01 encounter GameTest: pressure plan 변화 + 실제 hunter/scout/elite spawn/tracking/cleanup
- CI GameTest gate: non-zero 실행 marker + required tests passed marker 강제
- CI dedicated server/client smoke + executable JAR/SHA-256 검증

## 다음 개발 작업

이번 묶음으로 **환경 효과 + 일반 적 2역할 + elite technical role + pressure 위험 변화 + resource/combat 선택 압력 + native GameTest**는 runtime 수준에서 검증했다. 같은 시스템을 다시 만드는 작업은 하지 않는다.

다음은 M2 vertical slice의 **실제 field-play 품질 검수와 production encounter 경계**다.

1. 실제 Minecraft client에서 Region 01 원정을 반복 플레이해 combat pacing, spawn spacing, salvage hazard, 빠른 철수/순찰 제거 선택을 검수한다.
2. 기술 proxy의 문제를 기록하되 임의 모델/UI를 확정하지 않는다. M3용 combat/visual reference dossier를 먼저 만든다.
3. 전투 중 원정 실패·철수·mob 이탈 등 lifecycle edge case와 encounter cleanup을 강화한다.
4. 실제 플레이 결과를 바탕으로 Region 01 encounter composition의 수치/공간을 조정한다.
5. M2 완료에 필요한 `준비 → 진입 → 탐사/전투/회수 → 철수 → 투자 → 다음 원정 변화` 전체 흐름을 실제 플레이 검수한다.
6. 실제 화면/플레이 검수를 통과하기 전에는 combat/presentation 완료를 선언하지 않는다.

첫 `region_01`이 작은 DLC처럼 완결되기 전에는 추가 지역을 대량 생산하지 않는다.
