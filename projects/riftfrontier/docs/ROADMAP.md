# Riftfrontier — Development Roadmap

이 로드맵은 범위를 넓히기 전에 생산 구조와 vertical slice를 완성하기 위한 순서다.

## M0 — Canon & Bootstrap

목표: 방향과 빌드 기반 고정.

- PROJECT.md / CANONICAL.md / GAME_DESIGN_MASTER.md / CONTENT_ARCHITECTURE.md / REFERENCE_TARGETS.md 정본화
- NeoForge 26.2 + Java 25 + Gradle 9.2.1 빌드 프로젝트 생성
- mod metadata, package, resources, logging
- datagen/runServer/runClient 기본 task 확인
- 프로젝트 전용 workflow
- JAR verify와 SHA-256
- 최소 client/server smoke test

완료 조건:

- clean build 성공
- 실제 컴파일 클래스가 든 JAR 생성
- 서버/클라이언트 최소 로딩 결과 기록
- 정본 읽기 순서가 README에 명시됨

## M1 — Content Kernel

목표: 콘텐츠를 확장할 수 있는 언어와 검증 체계 만들기.

구현됨:

- stable content ID
- content registry boundary
- versioned JSON schema loader
- merged cross-document reference resolver
- validator ERROR/WARN + machine-readable code
- deterministic content catalog/fingerprint
- pack dependency/provenance
- ResourceManager reload + atomic last-known-good snapshot
- persistence schema/migration root
- native GameTest + server/client/JAR CI gate

현재 core type:

- creature profile
- combat archetype
- region profile
- loot profile
- encounter profile
- expedition resource
- contract
- extraction result profile

M1 기반을 반복 확장하지 않는다. 새 type은 실제 M2+ 플레이 요구가 있을 때만 validator/test와 함께 추가한다.

## M2 — Expedition Vertical Slice Core

목표: 첫 실제 플레이 루프 구축.

### M2-A — Domain foundation — VERIFIED

기준 코드 커밋 `5d226045e3d6c2140bd810124548806c026b0073`, GitHub Actions run `34089894370`에서 clean/unit test/build, native GameTest, dedicated server, Xvfb client, executable JAR 검사와 artifact/report 단계가 모두 성공했다.

- `Region → ExpeditionResource / Contract` graph
- contract required resource / reward loot / extraction-result reference
- `ExtractionResultProfile` 정책 정의
- Minecraft/DFU 비의존 불변 `ExpeditionRun`
- 별도 `ExpeditionRunCodec` persistence adapter
- `ExpeditionLifecycle` 상태 전이와 소속/요구조건 검증
- persistence schema 2 + `1 → 2` migration
- `RiftfrontierWorldData.expeditions[]`
- salvage vertical-slice fixture
- JUnit lifecycle/migration/content graph 테스트
- native GameTest SavedData 원정 전이 검증

정본: `EXPEDITION_RUNTIME.md`

### M2-B — Gameplay integration — PARTIAL VERIFIED

기준 코드 커밋 `c0ef978c81d36fe63cb9e69942a8aa7f3c6cae6d`, GitHub Actions `Build Riftfrontier` run `34094802012`에서 clean/unit test/build, native GameTest, dedicated server, Xvfb client, executable JAR 검사와 artifact/report 단계가 모두 성공했다.

완료·검증됨:

- fixture와 stable ID가 분리된 production `region_01` Region Pack
- runtime gameplay용 read-only `ContentLookup` 경계
- 중앙 거점 최소 contract acceptance 명령 경로
- validated content snapshot 기반 authoritative expedition 시작
- bounded technical hub / region cell 진입과 귀환
- 실제 block 우클릭 resource interaction → `recovered_resources` 기록
- extraction request/resolve → terminal `EXTRACTED` → 귀환/정산 결과 출력
- abort / player death / logout → terminal `FAILED` policy
- production graph를 쓰는 native GameTest 원정 전이 검증
- technical cell이 production art가 아니라는 명시적 presentation gate
- first slice에서는 ownership schema가 없으므로 world-wide nonterminal expedition 1개만 허용

정본: `M2B_GAMEPLAY_ADAPTER.md`

다음 구현 묶음 — 이미 검증한 gameplay adapter를 반복하지 않는다:

- production `region_01` environment rule 1개를 실제 runtime effect로 연결
- 일반 적 archetype 역할 2종의 실제 encounter spawn/defeat 연결
- elite 1종
- extraction retained resource를 hub authoritative storage로 정산
- storage → 보급/준비 비용 또는 선택 1개 연결
- world response 1계열이 다음 원정 위험/보급 조건을 실제 변경
- 위 상호작용 전체 native GameTest + 실제 플레이 검수

M2 완료 조건:

`준비 → 진입 → 목표/탐사 → 철수 → 투자 → 다음 원정 변화`가 실제 게임에서 끊기지 않고 동작하며 GameTest와 실제 플레이 검수를 통과한다.

## M3 — Combat & Boss Quality Gate

목표: 전투가 별도 대형 모드와 비교 가능한 수준의 피드백을 갖추게 하기.

- 무기 계열 최소 2종 완성
- active/passive 또는 module composition 최소 1계열
- hit feedback
- telegraph
- recovery/counterplay
- boss state machine
- elite/boss animation integration
- sound/VFX timing
- actual hitbox/visual alignment 검사

디자인/아트는 이 단계 전에 별도 reference dossier를 만들고 잠근다.

완료 조건:

- 보스가 단순 고체력 몹이 아님
- 플레이어가 공격을 보고 대응 가능
- 실제 스크린샷/플레이 영상 기준 presentation 검수 완료

## M4 — Frontier Logistics & Industry

목표: 원정의 결과가 거점의 생산력과 다음 원정에 영향을 주게 한다.

- storage/request domain
- processing family
- repair/upgrade
- supply consumption
- transport abstraction
- outpost 최소 기능
- research choice

완료 조건:

- 물류가 단순 창고 UI가 아니라 부족/공급/원정 준비에 실제 영향
- production chain이 domain request/event로 연결

## M5 — Faction & Dynamic World

목표: 세계가 플레이어 행동에 제한적으로 반응하게 한다.

- faction state
- relationship
- demand/contract pool
- region threat
- domain world events
- event consequence persistence

완료 조건:

- 세력/사건이 랜덤 팝업이 아니라 경제·계약·위험도 중 하나 이상을 실제 변경
- 플레이어가 변화의 원인을 알아볼 수 있음

## M6 — Region 01 Production Complete

목표: 첫 지역을 '작은 DLC' 수준으로 마무리.

- worldgen
- landmark/structure
- ecology
- elite/apex
- major encounter/boss
- resources/loot
- research/crafting integration
- contracts/events
- guide/bestiary
- sound/presentation
- performance baseline

완료 조건은 `CANONICAL.md`의 Region Pack quality gate를 따른다.

**M6 통과 전에는 Region 02 이상의 대규모 제작을 시작하지 않는다.**

## M7 — Scale-Out Pipeline

목표: Region 01에서 검증된 생산 방식을 복제한다.

- region template/tooling
- asset checklist
- validator expansion
- behaviour catalog expansion
- encounter composition expansion
- regression suite

이 단계부터 지역 추가 속도를 높인다.

## M8+ — Long-term Systems

vertical slice와 scale-out이 안정된 뒤 우선순위를 결정한다.

후보:

- 다중 세력 경쟁
- 광역 전초기지/수송망
- 고급 산업
- 고난도 원정 modifiers
- 다중 지역 사건
- 확장된 파티 멀티플레이
- 더 깊은 research/build composition
- 고급 던전 procedural composition

## 범위 제어 규칙

다음 조건 중 하나라도 해당하면 새 시스템보다 기존 milestone 완성을 우선한다.

- placeholder가 production asset보다 빠르게 늘어남
- validator 없이 새 content type이 증가함
- 지역 수가 늘지만 region quality gate 통과가 없음
- 실제 Minecraft 검수 없이 UI가 증가함
- GameTest/회귀 테스트가 따라오지 못함
- profiler 없이 tick 작업이 증가함
- 빌드가 장기간 깨진 상태로 콘텐츠 작업이 계속됨
