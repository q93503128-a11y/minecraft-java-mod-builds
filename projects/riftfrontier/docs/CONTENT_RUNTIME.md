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

참조 검증은 document 단위가 아니라 merge 이후 전체 graph에서 수행한다. 따라서 한 Region Pack을 region / creature / encounter / loot 등 여러 파일로 나누어도 stable content ID로 상호 참조할 수 있다.

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

검증 결과는 사람이 읽는 message와 stable machine-readable code를 함께 가진다. 현재 대표 코드는 `MISSING_REFERENCE`, `EMPTY_BEHAVIOUR_SET`, `MISSING_UNIQUE_GAMEPLAY_RULE`, `NO_REGION_ARCHETYPES`, `EMPTY_LOOT_POOLS`, `EMPTY_ENCOUNTER_PARTICIPANTS`, `EMPTY_ENCOUNTER_OBJECTIVE`, `NO_WORLD_CONSEQUENCE`다. CI와 후속 개발자 진단은 message 문자열 파싱 대신 code를 기준으로 분류한다.

## Snapshot / Fingerprint 계약

런타임 소비자는 mutable loading registry를 직접 소유하지 않는다. `ContentRuntimeSnapshot`에서 generation, loadedAt, pack IDs, definition count, deterministic catalog fingerprint, typed immutable definitions만 읽는다. 성공 publish만 generation을 증가시킨다.

`ContentCatalog` SHA-256 fingerprint는 현재 활성 콘텐츠 집합의 진단/호환성 breadcrumb다. persistence schema version을 대신하지 않는다.

## Authoritative persistence

세계 공용 저장 상태의 단일 root는 `RiftfrontierWorldData`이며 SavedData ID는 `riftfrontier:world_state`다. 어느 차원에서 접근하더라도 `server.overworld().getDataStorage()`에서 같은 root를 해석한다. 차원별 진행도 fork를 만들지 않는다.

현재 root 필드:

```text
riftfrontier_schema_version
world_revision
expedition_sequence
content_fingerprint
```

- `PersistenceSchema.CURRENT = 1`
- legacy schema `0 → 1`은 `PersistenceMigrationRegistry`를 통해 순차 migration한다.
- 미래 schema 또는 중간 migration 누락은 실패한다.
- decode가 끝난 뒤에만 migrated state를 gameplay code에 노출한다.
- 서버 시작 시 활성 validated content fingerprint를 root와 동기화한다.
- 동일 fingerprint는 불필요한 revision/dirty write를 만들지 않는다.
- expedition sequence allocation은 서버 권위 mutation이며 sequence와 world revision을 증가시키고 active fingerprint를 함께 기록한다.

향후 settlement/faction/expedition 같은 domain state도 이 root의 schema/migration 계약을 따르며, 임의의 별도 SavedData 섬을 만들지 않는다.

## Runtime diagnostics

`/riftfrontier runtime`은 read-only 서버 진단 명령이다. 현재 다음을 노출한다.

- content snapshot generation
- active pack IDs
- definition count
- catalog fingerprint
- persistence schema
- world revision
- expedition sequence
- persisted content fingerprint

진단 명령은 상태를 수정하지 않는다. 이후 reload 실패 이력이나 definition-level provenance를 추가하더라도 동일한 read-only 원칙을 유지한다.

## 테스트 계약

JUnit/순수 로직 회귀 테스트는 content publication, cross-document reference, dependency/provenance, validator codes, migration을 검사한다.

실제 Minecraft GameTest는 native 26.2 test-function registry와 data-driven `test_instance`를 사용한다. 현재 `riftfrontier:authoritative_runtime_state` 함수와 `authoritative_runtime_state.json`, 최소 empty structure가 JAR에 포함되며 다음을 실제 서버 월드에서 검증한다.

1. validated content snapshot이 존재하고 비어 있지 않다.
2. fingerprint가 존재한다.
3. authoritative SavedData root를 가져온다.
4. expedition sequence를 한 번 할당한다.
5. 재조회한 SavedData가 동일 authoritative cached root다.
6. sequence가 정확히 +1 된다.
7. persisted content fingerprint가 active snapshot과 일치한다.
8. world revision이 증가한다.

## CI runtime gate

M0의 정적 build만으로 완료를 선언하지 않는다. workflow는 다음을 모두 요구한다.

- clean/unit test/build 성공
- `runGameTestServer` 정상 종료
- GameTest 로그에 **1개 이상 테스트 실제 실행 marker** 존재
- GameTest 로그에 **required tests 전체 통과 marker** 존재
- 실패/crash marker 부재
- dedicated server에서 core load, content publish, authoritative world root attach, ready (`Done (`) 확인
- Xvfb client에서 Riftfrontier initialization과 fatal crash marker 부재 확인
- executable JAR에 실제 class, metadata, assets/data, required GameTest instance가 존재하고 ZIP 검사가 성공
- SHA-256 생성

기준 커밋 `c307034286dd62d6df71bea47cf721ede1d75957`의 실제 CI에서는 GameTest 서버가 non-zero 테스트를 실행했고 required tests가 모두 통과했으며, dedicated server/client/JAR gate도 성공했다. headless Linux runner의 narrator `libflite` 및 audio device 부재 경고는 Minecraft client가 계속 초기화되는 환경 제약으로 기록하며 Riftfrontier 기능 성공으로 오인하거나 모드 크래시로 오인하지 않는다.

## 다음 확장

M1 runtime foundation을 반복해서 넓히지 않는다. 다음 작업 묶음은 M2 첫 Expedition vertical slice를 위한 schema/lifecycle 경계다.

- `region / expedition_resource / contract / extraction-result` content type 계약
- codec/builder/validator/reference rule
- SavedData root 아래 expedition domain state와 lifecycle transition
- 첫 `region_01` pack
- 시작 → 진행 → 철수/실패 결과를 실제 GameTest로 검증
- 필요 시 definition-level provenance와 reload failure diagnostics 확장
