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

우선 구현:

- stable content ID
- content registry boundary
- data schema loader
- reference resolver
- validator severity(ERROR/WARN)
- datagen provider 구조
- content catalog 생성
- test fixture
- persistence schema version 기반

첫 schema 후보:

- creature profile
- combat archetype
- region profile
- loot profile
- encounter profile

완료 조건:

- 잘못된 참조를 게임 실행 전 검출
- 샘플 콘텐츠를 Java 복제 없이 데이터/공통 behaviour로 등록
- unit/GameTest 가능한 core path 확보

## M2 — Expedition Vertical Slice Core

목표: 첫 실제 플레이 루프 구축.

- 중앙 거점 최소 기능
- 원정 진입/귀환
- region_01 runtime
- 환경 규칙 1개
- 일반 적 archetype 여러 역할
- elite 1종
- 자원 회수
- 저장/보급/가공의 최소 물류 연결
- 계약/목표 1계열
- world response 1계열

완료 조건:

`준비 → 진입 → 목표/탐사 → 철수 → 투자 → 다음 원정 변화`가 실제 게임에서 끊기지 않고 동작.

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
