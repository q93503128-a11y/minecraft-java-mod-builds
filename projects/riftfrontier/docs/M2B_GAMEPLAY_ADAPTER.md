# Riftfrontier — M2-B Gameplay Adapter Contract

이 문서는 M2-A 원정 도메인을 실제 Minecraft 플레이 행동에 연결한 production gameplay adapter와 첫 원정→거점→다음 원정 피드백 루프의 정본이다.

## 1. 검증 기준

초기 gameplay adapter 기준 코드 커밋 `c0ef978c81d36fe63cb9e69942a8aa7f3c6cae6d`, GitHub Actions `Build Riftfrontier` run `34094802012`에서 clean/unit test/build, native GameTest, dedicated server, Xvfb client, executable JAR 검사와 artifact/report 단계가 모두 성공했다.

첫 authoritative hub feedback loop 기준 코드는 `73b7d490ded496c7da24a5b658849b5476ecc16f`, GitHub Actions `Build Riftfrontier` run `34096694269`이다. 이 run에서 다음 gate가 모두 성공했다.

- clean / unit test / build
- native GameTest
- dedicated server smoke
- Xvfb client smoke
- executable JAR inspection
- report / deliverable upload

schema 3 도입 직후 첫 CI는 기존 `PersistenceSchemaTest`가 schema 2를 고정 기대해 실패했다. persistence schema를 되돌리거나 테스트를 제거하지 않고, 테스트의 안정 계약을 schema 3/current-vs-unknown 의미에 맞게 갱신한 뒤 전체 gate를 다시 통과시켰다.

## 2. production content boundary

`region_01.json`은 `vertical_slice_fixture.json`과 stable ID가 완전히 분리된 첫 production Region Pack이다.

현재 production graph:

```text
region/region_01
├─ archetype/region_01_pursuer
├─ archetype/region_01_skirmisher
├─ resource/region_01_salvage
├─ contract/region_01_salvage_recovery
│  ├─ loot/region_01_salvage
│  └─ extraction/region_01_secured_return
├─ creature/region_01_scout
├─ creature/region_01_hunter
└─ encounter/region_01_patrol
```

이 데이터는 ResourceManager reload에서 fixture와 함께 merged graph validation을 통과한 뒤에만 runtime snapshot에 publish된다.

## 3. ContentLookup boundary

Gameplay code는 mutable `ContentRegistry`를 직접 받지 않는다.

```text
ContentLookup
├─ ContentRegistry          # build/load validation side
└─ ContentRuntimeSnapshot   # published runtime side
```

`ExpeditionLifecycle`은 `ContentLookup`에만 의존한다. 따라서 GameTest/loader와 실제 gameplay가 같은 도메인 규칙을 쓰되 runtime에서 mutable registry가 새지 않는다.

## 4. 현재 플레이 가능한 technical vertical slice

현재 명령 입력 표면은 최종 UI가 아니다. QUALITY_STANDARD의 UI/design gate를 통과하기 전까지 domain adapter를 검증하기 위한 임시 입력 경계다.

```text
/riftfrontier expedition start
→ production contract 검증
→ 현재 Region 01 preparation supply cost 확인
→ authoritative expedition supply 소비
→ ExpeditionRun 생성
→ PREPARING → DEPLOYED
→ bounded technical region cell 진입

아메시스트 표시 salvage node 우클릭
→ 실제 block interaction
→ resource/region_01_salvage +1
→ SavedData recovered_resources 갱신

/riftfrontier expedition extract
→ contract requirement 검사
→ EXTRACTION_REQUESTED
→ extraction policy 적용
→ EXTRACTED
→ retained Region 01 salvage를 hub authoritative storage에 정산
→ Region 01 pressure +1
→ technical hub 귀환

/riftfrontier expedition provision
→ secured Region 01 salvage 1 소비
→ expedition supply +2
→ 다음 원정 준비 재원으로 사용
```

`/riftfrontier expedition status`는 active run뿐 아니라 hub salvage, expedition supply, region pressure, 다음 preparation supply cost도 읽는다. `/riftfrontier expedition abort`는 terminal failure를 명시적으로 발생시킨다.

## 5. authoritative hub feedback loop

Persistence schema 3은 첫 vertical slice에 필요한 최소 거점/세계 반응 상태만 추가한다.

```text
secured_region_01_salvage
expedition_supply
region_01_pressure
```

fresh world의 expedition supply는 2다. Region 01 원정 시작은 반드시 authoritative supply를 먼저 지불한다. 공급 부족 시 원정 생성 자체가 거부되며, 실패/사망/로그아웃/abort는 준비 비용을 환불하지 않는다.

성공 extraction은 retained salvage를 hub storage에 더하고 `region_01_pressure`를 1 증가시킨다. 현재 preparation supply cost는 다음 규칙을 따른다.

```text
cost = min(3, 1 + floor(region_01_pressure / 2))
```

따라서 같은 성공 루프를 반복하면 다음 원정 준비 부담이 실제로 상승한다. 이는 M2에서 필요한 첫 world-response 축이며, 장기 밸런스 수치로 확정된 것이 아니다.

`provision`은 secured salvage 1을 소비해 expedition supply 2를 만든다. 별도 산업/생산 시스템을 성급히 만들지 않고도 **원정 결과 → 거점 자원 → 다음 원정 준비**의 인과를 먼저 검증하기 위한 최소 authoritative 변환이다. M4에서 production/request 체계가 들어오면 이 임시 변환을 그 시스템의 정식 recipe/request로 승격한다.

## 6. persistence contract

schema 2 → 3 migration은 기존 expedition run을 보존하면서 다음 기본값을 명시적으로 추가한다.

- `secured_region_01_salvage = 0`
- `expedition_supply = 2`
- `region_01_pressure = 0`

미래 schema, 빠진 migration step, 음수 storage/supply/pressure는 묵시적으로 수용하지 않는다. GameTest는 실제 overworld-scoped `RiftfrontierWorldData`에서 preparation 소비 → run lifecycle → extraction settlement → provision → 재조회까지 검증한다.

## 7. failure policy

첫 vertical slice에서 다음 상황은 active expedition을 `FAILED`로 끝낸다.

- player death
- expedition 중 logout
- explicit abort

실패 처리는 `ExpeditionGameplayEvents → ExpeditionGameplayService → ExpeditionLifecycle → RiftfrontierWorldData` 경계를 따른다. 이벤트에서 상태를 직접 조작하지 않는다. 이미 지불한 preparation supply는 환불하지 않는다.

## 8. technical cells are not production art

현재 hub/region cell은 각각 고정된 작은 좌표 구역에 smooth stone 기반 안전 바닥과 resource interaction marker만 만든다.

이는 다음만 검증한다.

- 진입/귀환
- bounded world mutation
- 실제 block interaction
- authoritative persistence
- extraction lifecycle
- hub settlement / provisioning / next-run cost feedback

이 구조를 최종 거점, 최종 지역 지형, UI, 건축물 또는 presentation으로 간주하지 않는다. M2-B 후반 및 M6에서 reference dossier와 production asset gate를 거쳐 교체한다.

## 9. 현재 제한

첫 vertical slice는 world-wide nonterminal expedition을 하나만 허용한다. `ExpeditionRun`에 player/party ownership schema가 아직 없기 때문에 멀티플레이 ownership을 가짜로 추론하지 않는다.

멀티플레이 확장은 실제 party/ownership 요구가 생길 때 persistence schema + validator + GameTest와 함께 추가한다.

현재 pressure는 준비 비용만 변화시킨다. environment/encounter 난이도 변화까지 한 번에 가짜로 연결하지 않는다. 그 부분은 다음 묶음에서 실제 runtime effect와 encounter state로 구현한다.

## 10. 다음 정확한 작업

이미 검증된 gameplay adapter와 storage/supply feedback loop를 반복하지 않는다. 다음 M2-B 묶음은 **Region 01 자체의 위험과 전투가 실제 Minecraft 공간에서 작동하는 것**을 닫는다.

우선순위:

1. production `region_01` environment rule 1개를 실제 server-authoritative runtime effect로 연결
2. `region_01` 일반 적 역할 2종을 production content archetype/creature 정의와 실제 spawn/defeat state에 연결
3. elite 1종을 별도 역할·telegraph/counterplay 요구가 있는 encounter로 연결
4. `region_01_pressure`가 위 environment 또는 encounter의 실제 위험 조건 하나 이상을 변경하게 연결
5. resource objective와 combat objective가 같은 원정에서 서로 의미 있게 충돌하도록 encounter composition 구성
6. 위 전체를 native GameTest로 검증
7. 실제 Minecraft 플레이 검수 전에는 presentation/전투 품질 완료를 선언하지 않음

위 루프가 닫히기 전에는 Region 02, 대규모 UI, 추가 content type을 시작하지 않는다.
