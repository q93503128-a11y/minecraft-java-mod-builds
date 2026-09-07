# Riftfrontier — M2-B Gameplay Adapter Contract

이 문서는 M2-A 원정 도메인을 실제 Minecraft 플레이 행동에 연결한 첫 production gameplay adapter의 정본이다.

## 1. 검증 기준

기준 코드 커밋: `c0ef978c81d36fe63cb9e69942a8aa7f3c6cae6d`

GitHub Actions `Build Riftfrontier` run `34094802012`에서 다음 gate가 모두 성공했다.

- clean / unit test / build
- native GameTest
- dedicated server smoke
- Xvfb client smoke
- executable JAR inspection
- report / deliverable upload

초기 두 CI 실패는 기능 삭제로 우회하지 않았다. Minecraft 26.2 매핑에서 제거된 `ServerPlayer.serverLevel()` / `ServerLevel.getSharedSpawnPos()` 의존을 현재 API 경계와 고정 technical cell로 교체했고, Brigadier feedback supplier 내부 checked exception 문제를 supplier 밖 조회로 분리했다.

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
├─ ContentRegistry      # build/load validation side
└─ ContentRuntimeSnapshot # published runtime side
```

`ExpeditionLifecycle`은 `ContentLookup`에만 의존한다. 따라서 GameTest/loader와 실제 gameplay가 같은 도메인 규칙을 쓰되 runtime에서 mutable registry가 새지 않는다.

## 4. 현재 플레이 가능한 technical vertical slice

현재 명령 입력 표면은 최종 UI가 아니다. QUALITY_STANDARD의 UI/design gate를 통과하기 전까지 domain adapter를 검증하기 위한 임시 입력 경계다.

```text
/riftfrontier expedition start
→ production contract 검증
→ authoritative ExpeditionRun 생성
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
→ technical hub 귀환
→ retained resources / world consequence 결과 출력
```

`/riftfrontier expedition status`는 현재 active run 상태를 읽는다. `/riftfrontier expedition abort`는 terminal failure를 명시적으로 발생시킨다.

## 5. failure policy

첫 vertical slice에서 다음 상황은 active expedition을 `FAILED`로 끝낸다.

- player death
- expedition 중 logout
- explicit abort

실패 처리는 `ExpeditionGameplayEvents → ExpeditionGameplayService → ExpeditionLifecycle → RiftfrontierWorldData` 경계를 따른다. 이벤트에서 상태를 직접 조작하지 않는다.

## 6. technical cells are not production art

현재 hub/region cell은 각각 고정된 작은 좌표 구역에 smooth stone 기반 안전 바닥과 resource interaction marker만 만든다.

이는 다음만 검증한다.

- 진입/귀환
- bounded world mutation
- 실제 block interaction
- authoritative persistence
- extraction lifecycle

이 구조를 최종 거점, 최종 지역 지형, UI, 건축물 또는 presentation으로 간주하지 않는다. M2-B 후반 및 M6에서 reference dossier와 production asset gate를 거쳐 교체한다.

## 7. 현재 제한

첫 vertical slice는 world-wide nonterminal expedition을 하나만 허용한다. `ExpeditionRun`에 player/party ownership schema가 아직 없기 때문에 멀티플레이 ownership을 가짜로 추론하지 않는다.

멀티플레이 확장은 실제 party/ownership 요구가 생길 때 persistence schema + validator + GameTest와 함께 추가한다.

## 8. 다음 정확한 작업

이미 검증된 gameplay adapter를 다시 확장하지 않는다. 다음 M2-B 묶음은 **원정 결과가 다음 준비에 영향을 주는 최소 시스템 상호작용**을 닫는다.

우선순위:

1. production `region_01` environment rule 1개를 실제 runtime effect로 연결
2. `region_01` 일반 적 역할 2종 + elite 1종의 server-authoritative encounter spawn/defeat 상태
3. extraction retained resource를 hub-side authoritative storage에 정산
4. storage → supply/preparation 비용 또는 보급 선택 1개 연결
5. extraction/world consequence로 다음 원정의 위험 또는 보급 조건이 실제 변경되는 world response 1계열
6. 위 전체를 native GameTest와 실제 플레이 검수로 묶기

위 루프가 닫히기 전에는 Region 02, 대규모 UI, 추가 content type을 시작하지 않는다.
