# Riftfrontier — Content Runtime Contract

이 문서는 `CONTENT_ARCHITECTURE.md`의 data-driven 원칙을 실제 런타임과 저장 상태에 적용하는 계약이다.

## 목적

Riftfrontier의 대규모 Region Pack과 확장 콘텐츠는 Java 코드 복제 대신 여러 versioned JSON document로 구성될 수 있어야 한다. 리소스 리로드 중 일부 문서가 잘못되더라도 플레이 중인 서버가 반쯤 갱신된 상태를 보아서는 안 되며, 검증된 콘텐츠와 세계 저장 상태의 관계도 추적 가능해야 한다.

## 서버 리로드 순서

```text
ResourceManager
→ data/riftfrontier/riftfrontier/content/**/*.json 수집
→ resource id 기준 deterministic sort
→ 각 document schema/decode + source provenance 보존
→ isolated per-document registry
→ pack_id / depends_on metadata 수집
→ pack dependency 존재 여부 + cycle 검증
→ deterministic topological pack order
→ 모든 document merge
→ duplicate pack id / duplicate definition id 거부
→ 전체 reference graph validation
→ ERROR가 없을 때만 immutable runtime snapshot 생성
→ atomic publish
```

참조 검증은 document 단위가 아니라 merge 이후 전체 graph에서 수행한다. 따라서 한 Region Pack을 region / creature / encounter / loot / resource / contract 등 여러 파일로 나누어도 stable content ID로 상호 참조할 수 있다.

## Pack metadata 계약

```json
{
  "schema_version": 1,
  "pack_id": "riftfrontier:region/region_01",
  "depends_on": ["riftfrontier:core"],
  "definitions": []
}
```

- `pack_id`는 활성 pack 집합에서 유일해야 한다.
- `depends_on`은 중복, 자기 자신 참조, 존재하지 않는 pack 참조를 허용하지 않는다.
- dependency cycle은 publish 전에 실패한다.
- 입력 파일 순서가 달라도 dependency와 `pack_id` 기준 publish 순서는 deterministic해야 한다.
- 각 pack은 원본 resource identifier를 provenance로 보존한다.

## Last-known-good 규칙

새 candidate가 decode, dependency, duplicate, schema 또는 reference validation 단계에서 실패하면 기존 `ContentRuntimeSnapshot`을 교체하지 않는다. 부분 적용, 일부 registry만 갱신, 오류 항목만 조용히 생략하는 방식은 금지한다.

## Validator issue code

검증 결과는 사람이 읽는 message와 stable machine-readable code를 함께 가진다. 대표 코드는 `MISSING_REFERENCE`, `EMPTY_BEHAVIOUR_SET`, `MISSING_UNIQUE_GAMEPLAY_RULE`, `NO_REGION_ARCHETYPES`, `NO_REGION_RESOURCES`, `NO_REGION_CONTRACTS`, `EMPTY_CONTRACT_REQUIREMENTS`, `EMPTY_ENCOUNTER_PARTICIPANTS`, `NO_WORLD_CONSEQUENCE`다. CI와 후속 개발자 진단은 message 문자열 파싱 대신 code를 기준으로 분류한다.

## M2 content graph

현재 core content type은 다음과 같다.

```text
CombatArchetype
Region
LootProfile
Creature
Encounter
ExpeditionResource
Contract
ExtractionResultProfile
```

원정 관련 필수 참조:

```text
Region → ExpeditionResource
Region → Contract
ExpeditionResource → Region
Contract → Region
Contract → ExpeditionResource requirements
Contract → LootProfile
Contract → ExtractionResultProfile
```

이 관계가 깨지면 atomic publish 전에 ERROR로 거부한다.

## Snapshot / Fingerprint 계약

런타임 소비자는 mutable loading registry를 직접 소유하지 않는다. `ContentRuntimeSnapshot`에서 generation, loadedAt, pack IDs, definition count, deterministic catalog fingerprint, typed immutable definitions만 읽는다. 성공 publish만 generation을 증가시킨다.

`ContentCatalog` SHA-256 fingerprint는 현재 활성 콘텐츠 집합의 진단/호환성 breadcrumb다. persistence schema version을 대신하지 않는다.

## Authoritative persistence

세계 공용 저장 상태의 단일 root는 `RiftfrontierWorldData`이며 SavedData ID는 `riftfrontier:world_state`다. 어느 차원에서 접근하더라도 `server.overworld().getDataStorage()`에서 같은 root를 해석한다.

schema 2 root 필드:

```text
riftfrontier_schema_version
world_revision
expedition_sequence
content_fingerprint
expeditions[]
```

- `PersistenceSchema.CURRENT = 2`
- legacy schema `0 → 1 → 2`를 `PersistenceMigrationRegistry`로 순차 migration한다.
- schema `1 → 2`는 빈 expedition list를 명시적으로 추가한다.
- 미래 schema 또는 중간 migration 누락은 실패한다.
- decode가 끝난 뒤에만 migrated state를 gameplay code에 노출한다.
- expedition sequence allocation은 서버 권위 mutation이다.
- persisted `ExpeditionRun`은 자신을 작성한 content fingerprint를 보존한다.
- run identity(sequence/region/contract)는 생성 뒤 변경하지 않는다.

원정 상태의 세부 계약은 `EXPEDITION_RUNTIME.md`를 정본으로 한다.

## Runtime diagnostics

`/riftfrontier runtime`은 read-only 서버 진단 명령이다. 현재 content snapshot과 persistence schema, world revision, sequence, fingerprint를 노출하며 SavedData diagnostic summary는 전체/active expedition count도 포함한다. 진단 경로는 상태를 수정하지 않는다.

## 테스트 계약

JUnit/순수 로직 회귀 테스트는 content publication, cross-document reference, dependency/provenance, validator codes, migration과 expedition domain transition을 검사한다.

실제 Minecraft GameTest는 native 26.2 test-function registry와 data-driven `test_instance`를 사용한다. M2에서는 실제 서버 월드에서 다음을 확인한다.

1. validated M2 content snapshot 존재
2. authoritative overworld SavedData root 존재
3. expedition sequence 단조 증가
4. PREPARING run 생성
5. DEPLOYED 전이
6. region-scoped resource 회수
7. extraction request와 contract requirement 검증
8. EXTRACTED terminal state를 SavedData에 갱신
9. 재조회한 run에 recovered resource가 유지
10. content fingerprint와 world revision 유지/증가

Mojang Codec과 실제 SavedData serialization의 최종 신뢰 경계는 Minecraft runtime/GameTest다. 순수 JUnit source set에 Minecraft/DFU 런타임 classpath를 복제해서 테스트를 가장하지 않는다.

## CI runtime gate

workflow는 다음을 모두 요구한다.

- clean/unit test/build 성공
- `runGameTestServer` 정상 종료
- GameTest 로그에 1개 이상 테스트 실제 실행 marker
- GameTest 로그에 required tests 전체 통과 marker
- 실패/crash marker 부재
- dedicated server에서 core/content/authoritative root/ready 확인
- Xvfb client에서 Riftfrontier initialization + fatal crash marker 부재 확인
- executable JAR 구조/필수 GameTest asset/ZIP 검사
- SHA-256 생성

M2 코드의 전체 CI gate가 green으로 확인되기 전에는 M2 기반 완료를 선언하지 않는다.

## 다음 확장

schema 자체를 반복 확장하지 않는다. 다음 작업은 `EXPEDITION_RUNTIME.md`의 domain을 첫 production `region_01` 실제 플레이에 연결한다.

```text
hub contract selection
→ authoritative run creation
→ region entry
→ real resource interaction
→ objective progress
→ extraction
→ return/settlement
→ world consequence
```

이 최소 루프가 실제 플레이와 GameTest에서 닫힌 뒤 전투/보스 presentation, 물류/산업으로 확장한다.
