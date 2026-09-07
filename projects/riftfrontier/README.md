# Riftfrontier — 균열 개척기

Riftfrontier는 Minecraft Java/NeoForge 26.2에서 개발하는 대형 **차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션** 프로젝트다.

## 현재 상태

`M1 — CONTENT KERNEL / RUNTIME FOUNDATION IN PROGRESS`

M0 빌드 골격과 실행용 JAR 생성/검사 workflow가 마련되어 있고, **dedicated server와 Xvfb client smoke를 실제 GitHub Actions에서 통과**시켰다. M1에서는 stable content ID, typed definition, JSON schema loader, merged reference validation, deterministic catalog fingerprint, server ResourceManager reload, last-known-good atomic snapshot에 이어 **pack dependency/provenance, machine-readable validator code, persistence migration registry**까지 구현했다.

실제 Riftfrontier GameTest fixture와 Minecraft SavedData domain adapter는 아직 구현/검증되지 않았으므로 M1 완료로 간주하지 않는다.

## 작업 시작 시 반드시 읽기

1. `/AGENTS.md`
2. `/docs/BUILD_STANDARD.md`
3. `/docs/QUALITY_STANDARD.md`
4. `PROJECT.md`
5. `docs/CANONICAL.md`
6. `docs/GAME_DESIGN_MASTER.md`
7. `docs/CONTENT_ARCHITECTURE.md`
8. `docs/ROADMAP.md`
9. 현재 content runtime을 다루면 `docs/CONTENT_RUNTIME.md`
10. 디자인/자산 작업이면 `docs/REFERENCE_TARGETS.md`와 `THIRD_PARTY_ASSETS.md`

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

- versioned JSON content documents (`schema_version = 1`)
- stable `ContentId`
- typed core definitions: combat archetype / region / loot profile / creature / encounter
- isolated document decode와 duplicate definition 거부
- 여러 document를 합친 뒤 cross-document reference graph 검증
- optional `depends_on` pack dependency metadata
- missing dependency / self dependency / duplicate dependency / dependency cycle 거부
- deterministic topological pack ordering
- resource identifier 기반 pack provenance 보존
- duplicate pack ID를 양쪽 source와 함께 진단
- ERROR/WARN validator + stable machine-readable issue code
- deterministic `ContentCatalog` SHA-256 fingerprint
- `ContentRuntimeSnapshot` + atomic last-known-good publication
- NeoForge `AddServerReloadListenersEvent` 기반 server content reload listener
- persistence schema root version `1`
- explicit sequential `PersistenceMigrationRegistry`
- pre-alpha schema 0 → schema 1 migration contract
- unit regression tests for runtime publication, graph references, pack dependency/provenance, validator codes, persistence migration
- CI dedicated server smoke: ready state + Riftfrontier content snapshot 확인
- CI Xvfb client smoke: Riftfrontier initialization + fatal crash marker 부재 확인
- executable JAR structure + SHA-256 검증

## 다음 개발 작업

우선순위는 다음과 같다.

1. **실제 Riftfrontier GameTest fixture를 최소 1개 등록하고 `runGameTestServer`를 CI gate로 연결한다.**
2. Minecraft `SavedData` root와 domain adapter를 만들고 migration registry를 실제 authoritative state load path에 연결한다.
3. reload 성공/실패와 활성 content fingerprint/pack source를 확인할 수 있는 개발자 진단 경로를 만든다.
4. M2에 필요한 region / expedition resource / contract schema 경계를 확정한다.
5. 이후 첫 Expedition vertical slice의 중앙 거점 ↔ region_01 진입/철수 최소 루프로 넘어간다.

새 시스템을 넓히기 전에 현재 milestone의 검증 가능한 기반을 닫는다.
