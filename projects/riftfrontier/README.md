# Riftfrontier — 균열 개척기

Riftfrontier는 Minecraft Java/NeoForge 26.2에서 개발하는 대형 **차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션** 프로젝트다.

## 현재 상태

`M1 — CONTENT KERNEL IN PROGRESS / RUNTIME RELOAD FOUNDATION IMPLEMENTED`

M0 빌드 골격과 실행용 JAR 생성/검사 workflow는 마련되어 있다. M1에서는 stable content ID, typed definition, JSON schema loader, registry/reference validator, deterministic catalog fingerprint에 이어 **NeoForge 서버 ResourceManager reload → 전체 content graph merge/validate → last-known-good atomic snapshot publish** 기반까지 구현했다.

M0의 실제 server/client smoke와 GameTest는 아직 실행 검증되지 않았으므로 완료로 간주하지 않는다.

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

## 현재 구현된 M1 기반

- versioned JSON content documents (`schema_version = 1`)
- stable `ContentId`
- typed core definitions: combat archetype / region / loot profile / creature / encounter
- isolated document decode와 duplicate definition 거부
- 여러 document를 합친 뒤 cross-document reference graph 검증
- duplicate pack ID 거부
- ERROR/WARN validator
- deterministic `ContentCatalog` SHA-256 fingerprint
- `ContentRuntimeSnapshot` + atomic last-known-good publication
- NeoForge `AddServerReloadListenersEvent` 기반 server content reload listener
- persistence schema root version `1`
- unit regression tests for valid/invalid publish, generation, cross-document references, duplicate pack IDs, persistence schema

## 다음 개발 작업

우선순위는 다음과 같다.

1. 현재 M1 runtime reload 묶음의 최종 CI/JAR 검증을 녹색으로 유지한다.
2. M0에서 남은 dedicated server / client smoke를 실제 실행하고 결과를 정직하게 기록한다.
3. resource provenance + machine-readable validator issue code + Region Pack manifest/dependency metadata를 추가한다.
4. 실제 SavedData 계층과 persistence migration registry를 만든다.
5. 그 뒤 M2 첫 Expedition vertical slice의 중앙 거점 ↔ region_01 진입/철수 최소 루프로 넘어간다.

새 시스템을 넓히기 전에 현재 milestone의 검증 가능한 기반을 닫는다.
