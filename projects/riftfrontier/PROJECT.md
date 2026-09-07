# Riftfrontier — 균열 개척기

- Slug: `riftfrontier`
- Mod ID: `riftfrontier`
- Namespace: `riftfrontier`
- Mod version: `0.1.0-alpha.1`
- Minecraft: `26.2`
- Java: `25`
- Loader: `NeoForge`
- Loader version: `26.2.0.38-beta`
- Gradle: `9.2.1`
- Build plugin: `ModDevGradle 2.0.143`
- Final JAR: `riftfrontier-0.1.0-alpha.1.jar`
- Existing-world compatibility: 첫 플레이어블 알파 이전에는 세이브 스키마를 실험할 수 있다. 첫 플레이어블 알파 이후부터 registry ID, 저장 키, content ID를 고정하고 migration을 우선한다.
- Required dependencies: Minecraft, NeoForge
- Optional external mods/libraries: 현재 26.2 호환성과 유지보수 상태를 다시 검증한 뒤 목적별로 단일 선택한다. 애니메이션은 GeckoLib 계열을 우선 검토하고, 복잡한 AI/UI 라이브러리는 실제 필요가 생긴 뒤 추가한다.
- Forbidden bundled dependencies: Minecraft 원본 파일, NeoForge 배포 파일, 외부 모드 JAR, 재배포 권한이 불명확한 모델·텍스처·음원·UI 자산
- Datagen task: `runData` (생성 대상은 schema/content type 확장에 맞춰 단계적으로 추가)
- GameTest task: `runGameTestServer` / native 26.2 test-function registry + data-driven `test_instance` 사용
- Server smoke-test task: CI `Dedicated server smoke` → `runServer`, ready/content/authoritative-world marker 검증
- Client smoke-test task: CI `Client smoke under virtual display` → Xvfb `runClient`, init/fatal-crash marker 검증

## 프로젝트 정체성

Riftfrontier는 단순한 RPG 콘텐츠 팩이나 차원 추가 모드가 아니다.

**차원 탐사 액션 RPG + 개척/물류 + 산업/연구 + 세력 시뮬레이션 + 동적 세계 사건**을 하나의 게임 루프로 연결하는 대형 Minecraft total-conversion 지향 프로젝트다.

플레이어는 고정된 용사 역할을 강요받지 않는다. 탐험가, 사냥꾼, 용병, 기술자, 상인, 제작자, 물류 운영자, 전초기지 창립자 등 서로 다른 삶의 경로가 같은 세계 시스템과 연결되어야 한다.

## 핵심 게임 루프

```text
거점에서 준비
→ 균열/미개척 지역 진입
→ 탐사·사냥·의뢰·자원 회수·사건 해결
→ 귀환/철수
→ 장비·기술·시설·물류망에 투자
→ 세력/경제/위험도가 반응
→ 더 깊은 지역과 더 복잡한 선택지 개방
→ 세계 규모의 사건과 고난도 원정으로 확장
```

## 절대 핵심 규칙

1. `AGENTS.md`, `docs/BUILD_STANDARD.md`, `docs/QUALITY_STANDARD.md`를 항상 상위 공용 규칙으로 따른다.
2. 프로젝트 작업 시작 시 반드시 `docs/CANONICAL.md`를 먼저 읽고 방향 잠금을 확인한다.
3. 차원/바이옴/몹/아이템의 개수만 늘려 규모를 과장하지 않는다. **시스템 간 상호작용과 지역 하나당 제작 밀도**를 우선한다.
4. 하나의 지역은 작은 DLC 수준의 완결 패키지로 만든다. 지형, 생태, 전투, 자원, 구조물, 사건, 던전/보스, 보상, 사운드, UI 연동까지 함께 완성한다.
5. 대량 콘텐츠는 Java 클래스를 하나씩 복제하지 않는다. 공통 builder/registry, data schema, datagen, validator, asset resolver, behaviour library 위에 얹는다.
6. 플레이어가 조합으로 새로운 플레이를 만들 수 있는 시스템을 선호한다. 개발자가 수백 개의 완성 스킬을 직접 만드는 것보다 재사용 가능한 동작/효과/증강 조합을 우선한다.
7. 세계 시뮬레이션은 의미 있는 상태 변화에 반응하는 event/request 기반을 우선한다. 매 tick 전체 월드 스캔으로 규모를 만들지 않는다.
8. 중요한 상태는 서버 권위로 처리한다. 클라이언트는 입력·표시·애니메이션·VFX를 담당한다.
9. UI/모델/애니메이션/사운드의 스타일을 AI가 즉석에서 발명하지 않는다. 실제 게임·대형 모드·공식 스크린샷·허용 자산을 조사하고, 프로젝트에 맞게 조합·수정한 뒤 실제 Minecraft 화면으로 검수한다.
10. 외부 프로젝트에서 가져오는 것은 **설계 원리와 제작 패턴**이다. 라이선스가 허용되지 않는 코드·자산을 복제하지 않는다.
11. 기능이 돌아간다는 이유만으로 완료하지 않는다. 실제 플레이, 시각 검수, GameTest, 성능 측정, 빌드/JAR 검증까지 품질 게이트를 통과해야 한다.
12. 범위가 커질수록 넓고 얕게 만들지 않는다. 먼저 수직 구간 하나를 상용 게임에 가까운 품질로 완성하고, 검증된 생산 체계를 복제해 확장한다.
13. 다른 프로젝트의 코드·자산·워크플로를 복제하지 않는다. 공용 표준과 공개 기술 자료는 참고하되 Riftfrontier의 구현은 독립적으로 유지한다.
14. content document는 `pack_id`, optional `depends_on`, source provenance를 가지며 dependency graph를 통과한 전체 graph만 atomic publish한다.
15. validator 진단은 사람이 읽는 message 외에 stable machine-readable issue code를 가진다.
16. authoritative persistence는 schema version과 명시적 순차 migration을 사용하며 알 수 없는 미래 schema나 빠진 migration step을 묵시적으로 수용하지 않는다.
17. 세계 공용 authoritative SavedData는 overworld storage의 단일 `riftfrontier:world_state` root를 사용한다. 다른 차원이 별도 world progression을 만들지 않는다.
18. CI GameTest는 프로세스 종료 코드만 보지 않고 non-zero 실행 marker와 required-tests-passed marker를 모두 요구한다.
19. 원정 콘텐츠 정의와 실제 원정 상태를 섞지 않는다. `Region/ExpeditionResource/Contract/ExtractionResultProfile`은 불변 content policy이고, `ExpeditionRun`은 서버 권위 세계 사실이다.
20. 원정 상태 전이는 `PREPARING → DEPLOYED → EXTRACTION_REQUESTED → EXTRACTED` 또는 명시적 `FAILED`만 허용하며, 우회 상태 변경을 저장 데이터에서 직접 수행하지 않는다.
21. 원정은 자신을 만든 content fingerprint를 저장한다. 이후 datapack reload로 현재 콘텐츠가 달라져도 과거 run의 작성 기준을 추적할 수 있어야 한다.
22. 순수 원정 도메인은 Minecraft/DFU serialization에 직접 의존하지 않는다. `ExpeditionRun`과 `ExpeditionLifecycle`은 순수 상태/규칙이고, `ExpeditionRunCodec`과 SavedData가 persistence adapter를 담당한다.
23. 원정 보상은 메시지 숫자로 끝내지 않는다. retained resource는 authoritative hub storage에 정산되고 이후 준비/생산/연구 중 하나 이상의 실제 입력이 되어야 한다.
24. 다음 원정 변화는 문구가 아니라 저장된 세계 상태가 실제 비용·환경·encounter 중 하나 이상을 변경해야 한다. 첫 구현은 `region_01_pressure → preparation supply cost`로 검증한다.
25. M2의 salvage→supply 직접 변환은 첫 vertical slice의 최소 adapter다. M4에서 request/production 체계를 구현할 때 정식 생산 흐름으로 승격하되 **원정 결과가 다음 준비를 바꾼다는 인과관계**는 유지한다.

## 정본 읽기 순서

작업자는 매 작업 시작 시 최소한 다음을 읽는다.

1. `/AGENTS.md`
2. `/docs/BUILD_STANDARD.md`
3. `/docs/QUALITY_STANDARD.md`
4. `/projects/riftfrontier/PROJECT.md`
5. `/projects/riftfrontier/docs/CANONICAL.md`
6. `GAME_DESIGN_MASTER.md`
7. `CONTENT_ARCHITECTURE.md`
8. `ROADMAP.md`
9. content/runtime/persistence 작업이면 `CONTENT_RUNTIME.md`
10. expedition 작업이면 `EXPEDITION_RUNTIME.md`
11. M2 gameplay 작업이면 `M2B_GAMEPLAY_ADAPTER.md`
12. 디자인/자산 작업이면 `REFERENCE_TARGETS.md`, `THIRD_PARTY_ASSETS.md`

## 현재 단계

`M2-B — GAMEPLAY ADAPTER + HUB FEEDBACK VERIFIED / REGION ENCOUNTER NEXT`

M0/M1에서 빌드/JAR, typed content graph, atomic runtime snapshot, ResourceManager reload, pack dependency/provenance, validator issue code, overworld-authoritative SavedData, diagnostics, native GameTest/CI gate를 검증했다.

M2-A에서는 content policy와 authoritative `ExpeditionRun`/`ExpeditionLifecycle`, schema 2 저장 경계를 검증했다.

M2-B에서는 다음을 실제 Minecraft adapter로 구현하고 검증했다.

- fixture와 분리된 production `region_01`
- validated content snapshot 기반 authoritative expedition 시작
- bounded technical hub/region 진입·귀환
- block interaction 기반 salvage 회수
- extraction/FAILED lifecycle
- persistence schema 3 + `2 → 3` migration
- retained salvage → hub authoritative storage
- hub salvage → expedition supply provisioning
- expedition 시작 전 preparation supply 원자적 소비
- successful extraction → Region 01 pressure 증가
- pressure → 다음 preparation supply cost 증가
- native GameTest에서 준비 소비 → run → extraction 정산 → pressure → provisioning → SavedData 재조회 검증

현재 검증 기준 커밋은 `73b7d490ded496c7da24a5b658849b5476ecc16f`, GitHub Actions `Build Riftfrontier` run은 `34096694269`이다. clean/unit test/build, native GameTest, dedicated server smoke, Xvfb client smoke, executable JAR 검사, artifact/report 단계가 모두 성공했다.

## 다음 정확한 개발 경계

다음 묶음은 schema, 원정 시작/철수 adapter, hub storage/supply를 더 확장하는 작업이 아니다. **Region 01 자체의 실제 위험/전투 상호작용**을 만든다.

1. production `region_01` environment rule 1개를 server-authoritative runtime effect로 연결한다.
2. 이미 정의된 일반 적 역할 2종을 실제 spawn/defeat 상태에 연결한다.
3. elite 1종을 추가하되 단순 체력 배수가 아니라 역할·telegraph·counterplay가 있는 encounter로 만든다.
4. `region_01_pressure`가 environment 또는 encounter 위험 조건 하나 이상을 실제 변경하게 한다.
5. resource objective와 combat objective가 같은 원정에서 서로 의미 있는 선택 압력을 만들게 한다.
6. native GameTest를 추가하고 통과시킨다.
7. 실제 Minecraft 플레이/시각 검수를 하지 않은 상태에서 combat/presentation 완료를 선언하지 않는다.
