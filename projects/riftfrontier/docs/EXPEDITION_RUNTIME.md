# Riftfrontier — Expedition Runtime Contract

이 문서는 M2부터 원정 콘텐츠 정의와 실제 세계 상태를 분리하는 정본이다.

현재 owner-bound lifecycle 검증 기준 코드는 `d1830698da654102b29f36aa81881f5526e5cc76`, GitHub Actions `Build Riftfrontier` run `34131862928`이다. 해당 run은 clean/unit/build, required native GameTest, dedicated server smoke, Xvfb client smoke, executable JAR inspection, report와 artifact upload까지 모두 성공했다. 실제 field-play/전투 체감 검증을 대신하지 않는다.

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

`ExtractionResultProfile`은 어떤 성공 철수가 어떤 보존율/위험 변화/세계 결과를 갖는지 정의한다. `ExpeditionRun`은 특정 세계에서 실제로 **어느 participant가** 무엇을 회수했고 어떤 상태와 terminal cause까지 갔는지 저장한다. 두 계층을 합치지 않는다.

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
owner_uuid
content_fingerprint
status
recovered_resources
started_game_time
ended_game_time
end_reason
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

terminal run은 다시 진행 상태로 돌아가지 않는다. recovered resource는 양수만 저장한다. run identity인 sequence/region/contract/owner는 생성 후 바꾸지 않는다.

`owner_uuid`는 production gameplay에서 원정을 시작한 player UUID다. M2의 **세계 전체 active run 최대 1개** 제한은 동시성 규칙일 뿐 participant ownership이 아니다. player-originated recover/extract/abort/death/logout/re-entry/review는 caller가 active run owner인지 증명해야 한다. 다른 플레이어의 death/logout으로 현재 owner의 원정을 실패시켜서는 안 된다.

`end_reason`은 terminal 상태의 원인을 기계 판독 가능하게 남긴다.

```text
EXTRACTED → extraction
FAILED    → player_abort | player_death | player_logout | server_restart | other_failure
active    → none
```

기존 save compatibility를 위해 `owner_uuid`와 `end_reason`은 codec에서 optional이다. legacy ownerless run을 다음 접속 플레이어에게 암묵적으로 귀속하지 않는다.

세부 ownership 계약은 `OWNER_BOUND_LIFECYCLE.md`, field evidence 계약은 `FIELD_PLAY_REVIEW.md`가 정본이다.

## 5. Content fingerprint

run은 생성 시점의 active content fingerprint를 저장한다. 이는 save schema version과 다른 개념이다.

- persistence schema: 저장 구조를 어떻게 읽을지 결정
- content fingerprint: 그 run을 어떤 validated content graph가 작성했는지 추적

향후 content reload 후에도 과거 run의 작성 기준을 진단할 수 있어야 한다.

## 6. ExpeditionLifecycle

상태 변경은 `ExpeditionLifecycle`의 도메인 규칙을 거친다.

- begin: region/contract 소속 관계 검증 + production adapter의 participant owner 전달
- deploy: PREPARING만 허용
- recover: active region이 노출한 resource만 허용
- requestExtraction: DEPLOYED + contract requirement 충족만 허용
- resolveExtraction: contract requirement 재검사 + extraction policy 적용
- fail: terminal 전 상태 + explicit failure reason만 허용

UI, teleport, VFX, 실제 아이템 지급은 lifecycle에 넣지 않는다. lifecycle은 서버가 신뢰할 수 있는 domain decision을 담당하고, 후속 adapter가 Minecraft 효과를 수행한다.

underfilled extraction은 상태 변경 전에 거부되어 DEPLOYED를 유지한다. 실패한 요청 때문에 `EXTRACTION_REQUESTED` soft-lock에 들어가지 않는다.

## 7. Persistence evolution

`riftfrontier:world_state`는 schema 2부터 `expeditions`를 저장한다.

```text
riftfrontier_schema_version = 2
world_revision
expedition_sequence
content_fingerprint
expeditions[]
```

schema 1 world는 `1 → 2` migration에서 빈 expedition list를 명시적으로 얻는다. migration step을 생략하지 않는다.

M2-B hub feedback loop에서 schema 3이 다음 world state를 추가했다.

```text
secured_region_01_salvage
expedition_supply
region_01_pressure
```

`2 → 3` migration은 기존 expedition history를 보존하며 기본값을 추가한다. `owner_uuid`와 `end_reason`은 `ExpeditionRun` 내부의 backward-compatible optional codec field이므로 이 둘 때문에 schema를 추가로 올리지 않는다.

`RiftfrontierWorldData`는 overworld storage의 단일 authoritative root이며:

- sequence를 단조 증가시킨다.
- PREPARING run을 생성한다.
- production run에 owner UUID를 저장한다.
- 동일 sequence의 region/contract/owner identity를 유지한 채 새 immutable snapshot으로 교체한다.
- mutation마다 world revision을 갱신한다.
- retained salvage, expedition supply, Region 01 pressure를 권위 상태로 관리한다.

미래 schema나 빠진 migration step을 묵시적으로 수용하지 않는다.

## 8. Gameplay ownership / failure boundary

새 production expedition 시작 시 validation snapshot과 authoritative allocation이 동일한 sequence와 owner UUID를 가져야 한다.

player event/request adapter는 `activeFor(player, world)` 경계를 사용한다.

```text
owner salvage interaction → recover 허용
non-owner salvage interaction → mutation 없음

owner extract/abort → 해당 run 전이
non-owner extract/abort → 해당 run 전이 금지

owner death/logout → 해당 run FAILED
unrelated player death/logout → 해당 run mutation 없음
```

server restart는 player-originated event가 아니므로 예외적으로 world-scoped다. M2에서 유일한 non-terminal run을 `FAILED/server_restart`로 끝내되 owner UUID를 보존한다. 이미 지불한 preparation supply는 환불하지 않는다.

restart 이후 process-local encounter ownership은 추정 복구하지 않는다. stable run tag를 가진 stale technical proxy는 entity-load 경계에서 orphan cleanup한다. startup/per-tick global scan은 사용하지 않는다.

## 9. Field review history

review는 caller owner UUID에 맞는 run history를 선택한다. `owner=<uuid>`를 evidence line에 포함한다.

Region 01 pressure는 현재 M2에서 성공 extraction당 정확히 1 증가한다. 따라서 owner-scoped 과거 run 뒤에 다른 participant의 성공 원정이 존재해도 run 시작 당시 pressure를 다음처럼 복원한다.

```text
pressure_at_start
= current_region_01_pressure
- count(EXTRACTED runs with sequence >= target.sequence)
```

persisted pressure와 history가 이 불변식과 모순되면 review는 추측값을 출력하지 않고 실패한다. 미래에 pressure 변경 원인이 늘어나면 이 역산식을 억지로 확장하지 말고 explicit persisted fact 또는 versioned event history로 승격한다.

## 10. 테스트 책임

### JUnit

Minecraft 런타임 없이 검사 가능한 것:

- contract requirement
- 잘못된 상태 전이 거부
- 잘못된 region/resource 거부
- immutable transition
- owner identity preservation / 다른 UUID와의 불일치
- legacy ownerless fixture compatibility
- terminal cause invariant
- cross-player historical pressure reconstruction
- history/pressure contradiction loud failure
- migration semantics
- content graph validation

### GameTest

실제 Minecraft 런타임에서 검사할 것:

- validated content runtime 존재
- authoritative SavedData root 사용
- expedition sequence monotonic allocation
- validated participant UUID와 persisted owner 일치
- PREPARING → DEPLOYED → resource recovery → extraction request → EXTRACTED
- updated run 재조회
- owner/recovered resource/content fingerprint persistence
- world revision advancement
- Region 01 encounter spawn/tracking/lure cleanup
- restart reconciliation + owner preservation + orphan proxy cleanup

Mojang Codec/실제 SavedData serialization의 최종 신뢰 경계는 Minecraft runtime test다. 순수 JUnit에 Minecraft/DFU classpath를 억지로 복제하지 않는다.

CI는 여기에 dedicated server smoke, Xvfb client initialization, executable JAR inspection, report와 deliverable/log artifact 생성을 추가한다.

## 11. 현재 gameplay integration 상태

첫 production `region_01`은 다음 technical loop를 실제 Minecraft interaction에 연결했다.

```text
expedition start
→ owner-bound authoritative run
→ supply 지불
→ Region 01 technical field 진입
→ pressure-scaled encounter
→ actual salvage block interaction
→ recovered resource persistence + hazard
→ objective 충족
→ 빠른 철수 또는 patrol-clear bonus 선택
→ extraction / failure
→ encounter cleanup
→ retained salvage + pressure settlement
→ technical hub 귀환 / re-entry reconciliation
→ provision → 다음 원정 supply
```

현재 hub/field cell, 명령 표면, Zombie/Skeleton/Ravager는 behaviour/lifecycle 검증용 technical proxy다. 최종 art/UI/combat quality로 승격하지 않는다.

## 12. 다음 integration gate

새 schema나 새 Region을 더 늘리는 것이 다음 단계가 아니다. 자동화로 검증 가능한 lifecycle/ownership/restart/evidence 경계는 현재 기준에서 닫혔다.

다음은 실제 Minecraft client에서 Region 01을 반복 플레이하며 owner-aware review snapshot을 남기는 manual field-play gate다.

```text
low pressure run
+ elevated pressure run
+ extraction / abort / death / logout / restart re-entry
→ owner/endReason evidence 확인
→ spawn spacing
→ aggro / pacing
→ salvage hazard fairness
→ fast extraction vs patrol-clear +1 trade-off
→ evidence-backed tuning only
```

이 수동 gate를 실제로 닫기 전에는 M2 combat quality 완료를 선언하지 않고, M3 production enemy/elite/boss art를 확정하지 않는다. field-play가 끝난 뒤 먼저 M3 combat/elite/boss reference dossier를 작성한다.
