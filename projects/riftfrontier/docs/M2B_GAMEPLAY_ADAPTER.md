# Riftfrontier — M2-B Gameplay Adapter Contract

이 문서는 M2-A 원정 도메인을 실제 Minecraft 플레이 행동에 연결한 production gameplay adapter와 첫 원정→거점→다음 원정 피드백 루프, 그리고 Region 01의 첫 server-authoritative encounter runtime 정본이다.

## 1. 검증 기준

초기 gameplay adapter 기준 코드 커밋 `c0ef978c81d36fe63cb9e69942a8aa7f3c6cae6d`, GitHub Actions `Build Riftfrontier` run `34094802012`에서 clean/unit test/build, native GameTest, dedicated server, Xvfb client, executable JAR 검사와 artifact/report 단계가 모두 성공했다.

첫 authoritative hub feedback loop 기준 코드는 `73b7d490ded496c7da24a5b658849b5476ecc16f`, GitHub Actions run `34096694269`이다.

Region 01 encounter runtime 기준 코드는 `a7791123eade916c14041b4d32306c4befd8fc6a`, GitHub Actions `Build Riftfrontier` run `34099256007`이다. 이 run에서 다음 gate가 모두 성공했다.

- clean / unit test / build
- required native GameTest suite
- dedicated server smoke
- Xvfb client smoke
- executable JAR inspection
- report / deliverable upload

encounter 구현 첫 시도는 Minecraft 26.2에서 이동된 monster package와 `EntityTypes`, `entityTags()`, `snapTo()` API 변화를 반영하지 못해 compile 단계에서 실패했다. 기능을 제거하거나 가짜 API로 우회하지 않고 26.2 Javadoc/NeoForge source를 기준으로 실제 API에 맞춰 수정한 뒤 전체 gate를 통과시켰다.

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

`ExpeditionLifecycle`은 `ContentLookup`에만 의존한다. GameTest/loader와 실제 gameplay가 같은 도메인 규칙을 쓰되 runtime에서 mutable registry가 새지 않는다.

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
→ pressure 기반 patrol spawn

아메시스트 표시 salvage node 우클릭
→ 실제 block interaction
→ resource/region_01_salvage +1
→ SavedData recovered_resources 갱신
→ server-authoritative rift-drag movement hazard

선택 A: contract 요구량을 확보한 뒤 빠르게 extraction
→ 살아 있는 patrol과의 전투 시간을 줄임
→ patrol bonus 없음

선택 B: patrol을 모두 제거한 뒤 extraction
→ 전투 위험/시간 부담
→ retained Region 01 salvage +1 bonus

/riftfrontier expedition extract
→ contract requirement 검사
→ EXTRACTION_REQUESTED → EXTRACTED
→ retained salvage + optional patrol bonus를 hub storage에 정산
→ Region 01 pressure +1
→ run encounter cleanup
→ technical hub 귀환

/riftfrontier expedition provision
→ secured Region 01 salvage 1 소비
→ expedition supply +2
→ 다음 원정 준비 재원
```

## 5. Region 01 encounter runtime

`Region01EncounterRuntime`은 M2에서 전투 역할과 세계 피드백을 검증하는 **server-authoritative technical adapter**다.

현재 역할:

- `hunter`: 근거리 pursuit pressure. Zombie를 behaviour proxy로 사용한다.
- `scout`: ranged/skirmish pressure. Skeleton + bow를 behaviour proxy로 사용한다.
- `elite_anchor`: 전투 공간에 강한 압박을 주되 단순 HP multiplier가 아닌 counterplay를 요구한다. Ravager의 shield-stun 상호작용을 technical counterplay proxy로 사용한다.

중요: 위 vanilla entity의 모델, 텍스처, 명칭, animation, 최종 AI는 Riftfrontier production creature design이 아니다. M3 combat/reference gate 전에 이 proxy를 최종 자산으로 승격하지 않는다.

각 encounter entity에는 run tag와 role tag를 붙인다. gameplay는 개별 Java 객체 참조를 장기 저장하지 않고 현재 run의 live threat를 공간+tag로 조회한다. extraction/FAILED에서는 해당 run의 encounter entity를 정리한다.

## 6. pressure → 실제 위험

기존 `region_01_pressure`는 더 이상 preparation cost 문구만 바꾸지 않는다.

현재 technical scaling:

```text
hunters = 1 + min(2, floor(pressure / 2))
scouts  = 1 + min(2, floor(pressure / 3))
elites  = 1
hazard_ticks = 60 + min(180, pressure * 20)
hazard_amplifier = min(2, floor(pressure / 3))
```

따라서 성공 원정이 누적되면:

- 다음 preparation supply cost가 상승하고
- Region 01 patrol의 일반 적 수가 증가하며
- salvage를 회수하는 순간의 이동 제약이 더 오래/강하게 지속된다.

이는 M2 vertical slice에서 **세계 상태 → 다음 원정의 실제 비용 + 실제 encounter/environment 위험**이라는 인과를 검증하기 위한 수치다. 장기 밸런스는 field play 없이 확정하지 않는다.

## 7. resource/combat choice pressure

resource와 combat을 각각 독립 목표로 두지 않는다.

Region 01은 contract 요구량의 salvage를 확보하면 patrol이 살아 있어도 extraction을 요청할 수 있다. 대신 patrol을 전부 제거한 상태에서 extraction하면 retained salvage에 +1 bonus가 붙는다.

따라서 플레이어는 같은 원정에서 다음을 비교하게 된다.

```text
빠른 철수
= 전투 노출 시간 감소
= patrol bonus 포기

순찰 제거
= 전투 위험/시간 증가
= +1 retained salvage
= 다음 provisioning에 쓸 수 있는 hub 자원 증가
```

이 선택은 메시지 설명이 아니라 authoritative settlement 결과를 실제로 바꾼다.

## 8. authoritative hub feedback loop

Persistence schema 3은 첫 vertical slice에 필요한 최소 거점/세계 반응 상태만 가진다.

```text
secured_region_01_salvage
expedition_supply
region_01_pressure
```

fresh world의 expedition supply는 2다. Region 01 원정 시작은 authoritative supply를 먼저 지불한다. 공급 부족 시 원정 생성 자체가 거부되며, 실패/사망/로그아웃/abort는 준비 비용을 환불하지 않는다.

성공 extraction은 retained salvage를 hub storage에 더하고 `region_01_pressure`를 1 증가시킨다. 현재 preparation supply cost:

```text
cost = min(3, 1 + floor(region_01_pressure / 2))
```

`provision`은 secured salvage 1을 소비해 expedition supply 2를 만든다. M4에서 production/request 체계가 들어오면 이 임시 변환을 정식 recipe/request로 승격한다.

## 9. persistence contract

schema 2 → 3 migration은 기존 expedition run을 보존하면서 다음 기본값을 추가한다.

- `secured_region_01_salvage = 0`
- `expedition_supply = 2`
- `region_01_pressure = 0`

encounter runtime은 새 persistence schema를 만들지 않는다. 전투 개체 자체를 장기 SavedData에 중복 저장하지 않고, run lifecycle과 기존 pressure state를 권위 기준으로 사용한다.

미래 schema, 빠진 migration step, 음수 storage/supply/pressure는 묵시적으로 수용하지 않는다.

## 10. failure policy

첫 vertical slice에서 다음 상황은 active expedition을 `FAILED`로 끝낸다.

- player death
- expedition 중 logout
- explicit abort

실패 처리는 `ExpeditionGameplayEvents → ExpeditionGameplayService → ExpeditionLifecycle → RiftfrontierWorldData` 경계를 따른다. 이미 지불한 preparation supply는 환불하지 않으며 해당 run encounter cleanup을 요청한다.

## 11. native GameTest contract

required GameTest suite에는 두 개의 production regression 축이 있다.

### `authoritative_runtime_state`

- content runtime snapshot
- preparation supply 소비
- expedition lifecycle
- extraction settlement
- pressure 증가
- hub provisioning
- SavedData 재조회

### `region_01_encounter_runtime`

- pressure 0의 최소 hunter/scout/elite plan
- pressure 6에서 hunter/scout 수 증가
- hazard duration/amplifier 증가
- 실제 GameTest `ServerLevel`에서 hunter/scout/elite entity spawn
- run 기반 live-threat tracking
- patrol-cleared 판정
- run cleanup 후 threat 0

CI는 non-zero test execution marker와 `All N required tests passed` marker를 모두 요구한다.

## 12. technical cells/proxies are not production art

현재 hub/region cell, 명령 입력, Zombie/Skeleton/Ravager proxy는 다음만 검증한다.

- 진입/귀환
- actual block interaction
- authoritative persistence
- encounter composition
- pressure scaling
- resource/combat reward trade-off
- extraction/failure lifecycle
- 실제 Minecraft entity spawn/cleanup

이를 최종 거점, 최종 지역 지형, production creature, UI, animation, sound 또는 combat presentation으로 간주하지 않는다.

## 13. 현재 제한

첫 vertical slice는 world-wide nonterminal expedition을 하나만 허용한다. `ExpeditionRun`에 player/party ownership schema가 아직 없기 때문에 멀티플레이 ownership을 가짜로 추론하지 않는다.

현재 encounter tracking은 bounded technical Region 01 공간의 run tags를 기준으로 한다. 실제 field play에서 mob을 장거리 유인하거나 비정상 종료했을 때 cleanup 범위를 검수해야 한다. 이 edge case를 검증하기 전에는 encounter lifecycle 완성으로 확대 해석하지 않는다.

pressure/적 수/hazard/patrol bonus 수치는 시스템 인과를 검증하는 vertical-slice 값이다. 실제 플레이 근거 없이 장기 밸런스로 잠그지 않는다.

## 14. 다음 정확한 작업

이미 검증한 environment effect, hunter/scout/elite spawn, pressure scaling, patrol bonus, native GameTest를 반복하지 않는다.

다음 M2-B 묶음은 **field-play review + edge-case hardening**이다.

1. 실제 Minecraft client에서 Region 01을 반복 플레이해 spawn spacing, aggro/pacing, salvage hazard, extraction 선택을 검수
2. mob 장거리 이탈, player death, abort, logout, extraction 직전/직후 등 cleanup edge case 강화
3. field play 결과로 pressure scaling과 patrol bonus 조정
4. M3 전에 combat/elite/boss reference dossier 작성
5. 기술 proxy를 production art로 승격하지 않음
6. `준비 → 진입 → 탐사/전투/회수 → 철수 → 투자 → 다음 원정 변화` 전체를 실제 플레이 검수한 뒤 M2 완료 여부 판단

위 실제 플레이 검수가 끝나기 전에는 Region 02, 대규모 UI, production boss 확장을 시작하지 않는다.
