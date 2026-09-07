# Riftfrontier — Content Runtime Contract

이 문서는 `CONTENT_ARCHITECTURE.md`의 data-driven 원칙을 실제 런타임에 적용하는 계약이다.

## 목적

Riftfrontier의 대규모 Region Pack과 확장 콘텐츠는 Java 코드 복제 대신 여러 versioned JSON document로 구성될 수 있어야 한다. 리소스 리로드 중 일부 문서가 잘못되더라도 플레이 중인 서버가 반쯤 갱신된 상태를 보아서는 안 된다.

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

핵심은 **참조 검증을 document 단위가 아니라 merge 이후 전체 graph에서 수행한다**는 것이다. 따라서 한 Region Pack을 region / creature / encounter / loot 등 여러 파일로 나누어도 서로 stable content ID로 참조할 수 있다.

## Pack metadata 계약

content document의 루트는 현재 다음 메타데이터를 지원한다.

```json
{
  "schema_version": 1,
  "pack_id": "riftfrontier:region/region_01",
  "depends_on": ["riftfrontier:core"],
  "definitions": []
}
```

- `pack_id`는 활성 pack 집합에서 유일해야 한다.
- `depends_on`은 선택 필드이며 중복 값, 자기 자신 참조, 존재하지 않는 pack 참조를 허용하지 않는다.
- pack dependency cycle은 publish 전에 실패한다.
- 입력 파일 순서가 달라도 dependency와 `pack_id`를 기준으로 publish 순서가 deterministic해야 한다.
- 각 pack은 원본 resource identifier를 provenance로 보존해 오류 메시지와 진단 로그에서 실제 source를 찾을 수 있어야 한다.

## Last-known-good 규칙

새 candidate가 decode, dependency, duplicate, schema 또는 reference validation 단계에서 실패하면 기존 `ContentRuntimeSnapshot`을 교체하지 않는다.

부분 적용, 일부 registry만 갱신, 오류 항목만 조용히 생략하는 방식은 금지한다.

## Validator issue code

검증 결과는 사람이 읽는 message만 제공하지 않는다. 각 issue에는 stable machine-readable code를 함께 둔다.

현재 코드 예:

- `MISSING_REFERENCE`
- `EMPTY_BEHAVIOUR_SET`
- `MISSING_UNIQUE_GAMEPLAY_RULE`
- `NO_REGION_ARCHETYPES`
- `EMPTY_LOOT_POOLS`
- `EMPTY_ENCOUNTER_PARTICIPANTS`
- `EMPTY_ENCOUNTER_OBJECTIVE`
- `NO_WORLD_CONSEQUENCE`

CI와 후속 개발자 진단 UI는 message 문구를 파싱하지 않고 code를 기준으로 분류한다.

## Snapshot 계약

런타임 소비자는 mutable loading registry를 직접 소유하지 않는다. `ContentRuntimeSnapshot`을 통해 다음만 읽는다.

- generation
- loadedAt
- pack IDs
- definition count
- deterministic catalog fingerprint
- typed definition lookup / immutable definition collection

성공한 publish마다 generation이 증가한다. 실패한 candidate는 generation을 증가시키지 않는다.

## Fingerprint

`ContentCatalog`의 deterministic SHA-256 fingerprint는 현재 활성 콘텐츠 집합을 식별하는 진단 값이다. 이는 저장 데이터 migration version을 대신하지 않는다.

## Persistence schema와 migration

authoritative saved-data 계층은 `PersistenceSchema.CURRENT`를 기준으로 명시적인 schema version을 가진다.

현재 최초 고정값:

```text
CURRENT = 1
VERSION_KEY = riftfrontier_schema_version
```

`PersistenceMigrationRegistry`는 한 버전씩 순서대로 이동하는 migration만 허용한다.

- 현재보다 미래 schema는 즉시 거부한다.
- 중간 migration step이 없으면 조용히 건너뛰지 않고 실패한다.
- migration 성공 시 VERSION_KEY를 다음 버전으로 명시적으로 갱신한다.
- 현재 pre-alpha legacy schema 0 → schema 1의 기본 migration은 payload를 보존하면서 version marker를 추가한다.

실제 Minecraft `SavedData` 도메인이 추가되면 각 domain state는 이 registry 계약을 통과한 뒤 decode한다.

## 테스트 계약

최소 회귀 테스트:

1. valid fixture가 snapshot으로 publish된다.
2. invalid candidate가 last-known-good snapshot을 교체하지 않는다.
3. 성공한 replacement만 generation을 증가시킨다.
4. 서로 다른 JSON document 사이 reference가 merge 후 해결된다.
5. duplicate pack ID는 두 source와 함께 거부한다.
6. missing pack dependency는 publish 전에 거부한다.
7. dependency cycle은 deterministic하게 거부한다.
8. resource provenance가 merge 결과에 보존된다.
9. validator issue code가 message와 독립적으로 조회된다.
10. persistence migration이 순차적으로 current schema까지 이동하거나 명시적으로 실패한다.

## CI runtime gate

M0의 정적 build만으로 완료를 선언하지 않는다. 프로젝트 workflow는 다음 runtime gate를 수행하도록 유지한다.

- dedicated server: EULA 동의가 격리된 CI run directory에서만 생성되고, `Riftfrontier core loaded`, content snapshot publish, 서버 ready (`Done (`) 로그를 확인한다.
- client: Xvfb 가상 디스플레이에서 `runClient`를 제한 시간 실행하고 Riftfrontier initialization과 fatal crash marker 부재를 확인한다.
- timeout 종료는 Minecraft가 정상적으로 계속 실행 중인 smoke 특성상 허용하되, 성공 marker가 없으면 실패다.

## 다음 확장

M1 후속 우선순위:

- 실제 Minecraft `SavedData` root와 domain별 persistence adapter
- reload 성공/실패 진단 명령 또는 개발자 화면
- content provenance를 definition 단위까지 확장할 필요성 평가
- GameTest에서 resource/runtime linkage 검증
- M2 첫 Expedition vertical slice용 region/contract/resource schema 추가
