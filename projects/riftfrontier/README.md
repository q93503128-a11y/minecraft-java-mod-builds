# Riftfrontier — 균열 개척기

Riftfrontier는 Minecraft Java/NeoForge 26.2에서 개발하는 대형 **차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션** 프로젝트다.

## 현재 상태

`M3 — CONNECTED REGION 01 TECHNICAL VERTICAL SLICE BUILD VERIFIED / PRODUCTION PRESENTATION + HUMAN PLAY NEXT`

Region 01의 첫 수직 구간은 현재 `거점 준비 → 출정 → 전투/회수 → 3개 확보 후 철수 또는 4~5번째 선택 회수/순찰 보너스 → 철수 정산 → 보급 → 다음 원정 압력 변화`까지 하나의 서버 권위 루프로 연결되어 있다. Salvage Recovery 계약, Rift Blackout 사건, 두 전투 장비 계열과 Recovery Pivot, five-node risk/reward 선택, restart reconciliation도 자동 검증 경계에 포함된다.

2026-09-20 수동 코드/리소스/정본 감사에서 production `region_01`의 extraction metadata가 실제 authoritative `region_01_pressure +1` 규칙과 반대로 적혀 있던 문제, 철수 결과/철수 잠금에서 raw English domain/exception 문자열이 한국어 UI에 섞일 수 있던 문제, 정상 플레이 문구의 개발 단계 표현과 Scout 번역 누락을 수정했다. 이 의미 계약은 production content test로 고정했다.

현재 검증 기준 코드는 `14a2a0b3b317c465f11d242cbf4f73bba5062568`, GitHub Actions `Build Riftfrontier` run `35452759876`이다. clean test/build, required native GameTests, dedicated-server smoke, Xvfb client smoke, executable-JAR 검사와 artifact 업로드가 모두 성공했다. 검증 JAR SHA-256은 `2d8d15f6ba5fb1df0615066082241c19405402ce1d86d623c991f0bb3c79fb24`다.

자동화 검증과 실제 인간 플레이 검수는 구분한다. Hunter Alien, boss Dark Rock 및 실제 전투/원정 feel은 아직 사람의 Minecraft field play 승인이 없으며, Armabee Scout는 review runtime plumbing만 build-verified이고 exact creator bytes가 runtime resource에 아직 들어오지 않았다.

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
12. restart/encounter persistence 경계를 다루면 `docs/RESTART_RECONCILIATION.md`
13. 디자인/자산 작업이면 `docs/REFERENCE_TARGETS.md`와 `THIRD_PARTY_ASSETS.md`

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
- contract 미충족 extraction은 상태 전이 전에 거부되어 DEPLOYED 상태를 보존
- server restart 시 persisted non-terminal expedition은 추정 복구하지 않고 명시적으로 `FAILED` 처리
- restart failure에서도 이미 사용한 preparation supply는 환불하지 않음

### M2 gameplay adapter / Region 01

- fixture와 분리된 production `region_01` Region Pack
- temporary `/riftfrontier expedition start / extract / provision / abort / status` 입력 표면
- bounded technical hub/region cell 진입·귀환
- 실제 block interaction 기반 salvage 회수
- extraction retained salvage → authoritative hub storage 정산
- secured salvage 1 → expedition supply 2 provisioning
- 원정 시작 전 authoritative supply 소비; 부족하면 원정 생성 거부
- 사망/로그아웃/abort → FAILED, preparation supply 미환불
- explicit abort → encounter cleanup 후 technical hub 귀환
- successful extraction → `region_01_pressure` 증가 → 다음 preparation supply cost 상승
- **Region 01 hunter 역할:** 근거리 추격 technical proxy
- **Region 01 scout 역할:** 원거리 압박 technical proxy
- **elite anchor 역할:** 단순 체력 배수 대신 방패 stun counterplay가 존재하는 Ravager technical proxy
- salvage 회수 시 server-authoritative rift-drag movement hazard 적용
- pressure가 높을수록 hunter/scout 수와 hazard 지속시간/강도가 실제 증가
- patrol을 제거하고 철수하면 retained salvage +1 bonus; 빠른 철수는 가능하지만 bonus를 포기
- run sequence가 소유한 direct threat tracking으로 technical cell 밖으로 유인한 살아 있는 적도 patrol-clear를 차단
- terminal cleanup이 같은 server process에서 추적 중인 run threat를 위치와 무관하게 제거
- proxy entity에 stable run/role tag를 기록하고, restart 이후 process-local tracker가 없는 persisted proxy는 `EntityJoinLevelEvent`에서 event-driven으로 제거
- restart orphan cleanup은 startup/per-tick broad world scan을 사용하지 않음
- vanilla entity는 M2 behaviour 검증용 proxy일 뿐 production art/최종 AI가 아님

### Verification

- `/riftfrontier runtime` read-only 진단 명령
- native Minecraft 26.2 test-function registry + data-driven required GameTest
- authoritative runtime/state GameTest
- Region 01 encounter GameTest: pressure plan 변화 + 실제 hunter/scout/elite spawn/tracking + 48블록 lure regression + cleanup
- restart reconciliation GameTest: non-terminal SavedData run → FAILED, no supply refund, orphan tagged proxy rejection/discard
- extraction lifecycle unit regression: underfilled 요청이 DEPLOYED 상태를 보존
- CI GameTest gate: non-zero 실행 marker + required tests passed marker 강제
- CI dedicated server/client smoke + executable JAR/SHA-256 검증

## 다음 개발 작업

이번 묶음으로 **active expedition server restart의 authoritative run 처리와 persisted technical proxy orphan cleanup**까지 닫았다. 같은 문제를 다시 구현하지 않는다.

다음은 M2 vertical slice의 **실제 field-play 품질 검수**다.

1. 실제 Minecraft client에서 Region 01 원정을 반복 플레이해 combat pacing, spawn spacing, aggro, salvage hazard, 빠른 철수/순찰 제거 선택을 검수한다.
2. death/logout/abort/extraction 직전·직후와 restart 이후 플레이어 재진입 체감을 실제 플레이로 재검수한다.
3. field-play 결과를 근거로 pressure scaling과 patrol reward를 조정한다.
4. 기술 proxy의 문제를 기록하되 임의 모델/UI를 확정하지 않는다.
5. M3로 넘어가기 전에 `REFERENCE_TARGETS.md` 원칙에 맞는 combat/visual reference dossier를 만든다.
6. 실제 화면/플레이 검수를 통과하기 전에는 combat/presentation 완료를 선언하지 않는다.

첫 `region_01`이 작은 DLC처럼 완결되기 전에는 추가 지역을 대량 생산하지 않는다.
