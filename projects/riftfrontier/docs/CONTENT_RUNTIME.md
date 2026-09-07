# Riftfrontier — Content Runtime Contract

이 문서는 `CONTENT_ARCHITECTURE.md`의 data-driven 원칙을 실제 런타임에 적용하는 계약이다.

## 목적

Riftfrontier의 대규모 Region Pack과 확장 콘텐츠는 Java 코드 복제 대신 여러 versioned JSON document로 구성될 수 있어야 한다. 리소스 리로드 중 일부 문서가 잘못되더라도 플레이 중인 서버가 반쯤 갱신된 상태를 보아서는 안 된다.

## 서버 리로드 순서

```text
ResourceManager
→ data/riftfrontier/riftfrontier/content/**/*.json 수집
→ resource id 기준 deterministic sort
→ 각 document schema/decode
→ isolated per-document registry
→ 모든 document merge
→ duplicate pack id / duplicate definition id 거부
→ 전체 reference graph validation
→ ERROR가 없을 때만 immutable runtime snapshot 생성
→ atomic publish
```

핵심은 **참조 검증을 document 단위가 아니라 merge 이후 전체 graph에서 수행한다**는 것이다. 따라서 한 Region Pack을 region / creature / encounter / loot 등 여러 파일로 나누어도 서로 stable content ID로 참조할 수 있다.

## Last-known-good 규칙

새 candidate가 decode, duplicate, schema 또는 reference validation 단계에서 실패하면 기존 `ContentRuntimeSnapshot`을 교체하지 않는다.

부분 적용, 일부 registry만 갱신, 오류 항목만 조용히 생략하는 방식은 금지한다.

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

## Persistence schema

authoritative saved-data 계층은 `PersistenceSchema.CURRENT`를 기준으로 명시적인 schema version을 가진다.

현재 최초 고정값:

```text
CURRENT = 1
VERSION_KEY = riftfrontier_schema_version
```

알 수 없는 schema를 묵시적으로 현재 형식으로 읽지 않는다. 실제 SavedData 구현이 추가될 때 migration registry를 별도로 둔다.

## 테스트 계약

최소 회귀 테스트:

1. valid fixture가 snapshot으로 publish된다.
2. invalid candidate가 last-known-good snapshot을 교체하지 않는다.
3. 성공한 replacement만 generation을 증가시킨다.
4. 서로 다른 JSON document 사이 reference가 merge 후 해결된다.
5. duplicate pack ID는 거부한다.
6. unknown persistence schema는 거부한다.

## 다음 확장

M1 후속에서 다음을 추가한다.

- schema별 migration registry
- resource provenance와 오류 source path 보존
- Region Pack manifest / dependency metadata
- validator issue code와 machine-readable report
- reload 성공/실패 진단 명령 또는 개발자 화면
- 실제 GameTest 및 server smoke에서 resource reload 검증
