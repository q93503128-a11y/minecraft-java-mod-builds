# Riftfrontier — Expedition Runtime Contract

이 문서는 M2부터 원정 콘텐츠 정의와 실제 세계 상태를 분리하는 정본이다.

## 1. 핵심 경계

```text
Content policy (immutable after publish)
Region
→ ExpeditionResource
→ Contract
→ ExtractionResultProfile

World fact (authoritative SavedData)
ExpeditionRun
→ ExpeditionLifecycle transition
→ RiftfrontierWorldData persistence
```

`ExtractionResultProfile`은 어떤 성공 철수가 어떤 보존율/위험 변화/세계 결과를 갖는지 정의한다. `ExpeditionRun`은 특정 세계에서 실제로 누가 무엇을 회수했고 어떤 상태까지 갔는지 저장한다. 두 계층을 합치지 않는다.

## 2. Content definitions

### Region

원정의 gameplay rule과 사용할 combat archetype, expedition resource, contract stable ID를 노출한다.

### ExpeditionResource

단순 아이템 ID가 아니라 원정 루프에서 의미를 갖는 자원 정의다.

- owning region
- category
- carry weight
- field value

실제 ItemStack 연결은 후속 integration layer가 담당한다.

### Contract

- owning region
- objective
- required resource counts
- reward loot profile
- extraction result profile
- world consequence

계약 진행도 자체는 content definition에 저장하지 않는다.

### ExtractionResultProfile

- outcome
- retained percent
- threat delta
- world consequence

이는 terminal result policy이며 한 원정의 실제 결과 객체가 아니다.

## 3. Reference validation

atomic content publish 전에 다음을 전체 merged graph에서 검증한다.

- Region → CombatArchetype
- Region → ExpeditionResource
- Region → Contract
- ExpeditionResource → Region
- Contract → Region
- Contract → ExpeditionResource requirements
- Contract → LootProfile
- Contract → ExtractionResultProfile

깨진 참조는 런타임에서 fallback하지 않고 publish 자체를 거부한다.

## 4. ExpeditionRun

`ExpeditionRun`은 불변 스냅샷으로 취급한다.

필드:

```text
sequence
region_id
contract_id
content_fingerprint
status
recovered_resources
started_game_time
ended_game_time
```

상태:

```text
PREPARING
→ DEPLOYED
→ EXTRACTION_REQUESTED
→ EXTRACTED

PREPARING / DEPLOYED / EXTRACTION_REQUESTED
→ FAILED
```

terminal run은 다시 진행 상태로 돌아가지 않는다. recovered resource는 양수만 저장한다. run identity인 sequence/region/contract는 생성 후 바꾸지 않는다.

## 5. Content fingerprint

run은 생성 시점의 active content fingerprint를 저장한다. 이는 save schema version과 다른 개념이다.

- persistence schema: 저장 구조를 어떻게 읽을지 결정
- content fingerprint: 그 run을 어떤 validated content graph가 작성했는지 추적

향후 content reload 후에도 과거 run의 작성 기준을 진단할 수 있어야 한다.

## 6. ExpeditionLifecycle

상태 변경은 `ExpeditionLifecycle`의 도메인 규칙을 거친다.

- begin: region/contract 소속 관계 검증
- deploy: PREPARING만 허용
- recover: active region이 노출한 resource만 허용
- requestExtraction: DEPLOYED만 허용
- resolveExtraction: contract requirement 충족 + extraction policy 적용
- fail: terminal 전 상태에서만 허용

UI, teleport, VFX, 실제 아이템 지급은 lifecycle에 넣지 않는다. lifecycle은 서버가 신뢰할 수 있는 domain decision을 담당하고, 후속 adapter가 Minecraft 효과를 수행한다.

## 7. Persistence schema 2

`riftfrontier:world_state`는 schema 2부터 `expeditions`를 저장한다.

```text
riftfrontier_schema_version = 2
world_revision
expedition_sequence
content_fingerprint
expeditions[]
```

schema 1 world는 `1 → 2` migration에서 빈 expedition list를 명시적으로 얻는다. migration step을 생략하지 않는다.

`RiftfrontierWorldData`는 overworld storage의 단일 authoritative root이며:

- sequence를 단조 증가시킨다.
- PREPARING run을 생성한다.
- 동일 sequence의 identity를 유지한 채 새 immutable snapshot으로 교체한다.
- mutation마다 world revision을 갱신한다.

## 8. 테스트 책임

### JUnit

Minecraft 런타임 없이 검사 가능한 것:

- contract requirement
- 잘못된 상태 전이 거부
- 잘못된 region/resource 거부
- immutable transition
- migration semantics
- content graph validation

### GameTest

실제 Minecraft 런타임에서 검사할 것:

- validated content runtime 존재
- authoritative SavedData root 사용
- expedition sequence monotonic allocation
- PREPARING → DEPLOYED → resource recovery → extraction request → EXTRACTED
- updated run 재조회
- recovered resource persistence
- content fingerprint persistence
- world revision advancement

Mojang Codec/실제 SavedData serialization의 최종 신뢰 경계는 Minecraft runtime test다. 순수 JUnit에 Minecraft/DFU classpath를 억지로 복제하지 않는다.

## 9. 다음 integration gate

다음 구현은 새 schema를 더 만드는 것이 아니라 이 도메인을 실제 플레이에 연결한다.

```text
Hub contract selection
→ authoritative run creation
→ region entry
→ real resource interaction
→ objective progress
→ extraction request
→ return/settlement
→ world consequence
```

첫 production `region_01`이 이 루프를 실제 플레이에서 닫기 전에는 추가 지역을 대량 생산하지 않는다.
