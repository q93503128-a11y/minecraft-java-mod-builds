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
- Optional external mods/libraries: 현재 26.2 호환성과 유지보수 상태를 다시 검증한 뒤 목적별로 단일 선택한다. Region 01 첫 보스는 이미 Riftfrontier의 custom skinned-mesh importer/renderer 경로를 사용하므로 GeckoLib는 현재 critical path가 아니며, 이후 실제 자산 요구가 생길 때만 재검토한다. 복잡한 AI/UI 라이브러리도 실제 필요가 생긴 뒤 추가한다.
- Forbidden bundled dependencies: Minecraft 원본 파일, NeoForge 배포 파일, 외부 모드 JAR, 재배포 권한이 불명확한 모델·텍스처·음원·UI 자산
- Datagen task: `runData`
- GameTest task: `runGameTestServer` / native 26.2 test-function registry + data-driven `test_instance`
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
6. 플레이어가 조합으로 새로운 플레이를 만들 수 있는 시스템을 선호한다.
7. 세계 시뮬레이션은 의미 있는 상태 변화에 반응하는 event/request 기반을 우선한다. 매 tick 전체 월드 스캔으로 규모를 만들지 않는다.
8. 중요한 상태는 서버 권위로 처리한다. 클라이언트는 입력·표시·애니메이션·VFX를 담당한다.
9. UI/모델/애니메이션/사운드의 스타일을 AI가 즉석에서 발명하지 않는다. 실제 게임·대형 모드·공식 스크린샷·허용 자산을 조사하고 실제 Minecraft 화면으로 검수한다.
10. 외부 프로젝트에서 가져오는 것은 **설계 원리와 제작 패턴**이다. 라이선스가 허용되지 않는 코드·자산을 복제하지 않는다.
11. 기능이 돌아간다는 이유만으로 완료하지 않는다. 실제 플레이, 시각 검수, GameTest, 성능 측정, 빌드/JAR 검증까지 품질 게이트를 통과해야 한다.
12. 범위가 커질수록 넓고 얕게 만들지 않는다. 먼저 수직 구간 하나를 상용 게임에 가까운 품질로 완성하고, 검증된 생산 체계를 복제해 확장한다.
13. 다른 프로젝트의 코드·자산·워크플로를 복제하지 않는다.
14. content document는 `pack_id`, optional `depends_on`, source provenance를 가지며 dependency graph를 통과한 전체 graph만 atomic publish한다.
15. validator 진단은 사람이 읽는 message 외에 stable machine-readable issue code를 가진다.
16. authoritative persistence는 schema version과 명시적 순차 migration을 사용하며 알 수 없는 미래 schema나 빠진 migration step을 묵시적으로 수용하지 않는다.
17. 세계 공용 authoritative SavedData는 overworld storage의 단일 `riftfrontier:world_state` root를 사용한다.
18. CI GameTest는 프로세스 종료 코드만 보지 않고 non-zero 실행 marker와 required-tests-passed marker를 모두 요구한다.
19. 원정 콘텐츠 정의와 실제 원정 상태를 섞지 않는다. `Region/ExpeditionResource/Contract/ExtractionResultProfile`은 불변 content policy이고, `ExpeditionRun`은 서버 권위 세계 사실이다.
20. 원정 상태 전이는 `PREPARING → DEPLOYED → EXTRACTION_REQUESTED → EXTRACTED` 또는 명시적 `FAILED`만 허용한다.
21. 원정은 자신을 만든 content fingerprint를 저장한다.
22. 순수 원정 도메인은 Minecraft/DFU serialization에 직접 의존하지 않는다. `ExpeditionRun`/`ExpeditionLifecycle`과 persistence adapter를 분리한다.
23. 원정 보상은 메시지 숫자로 끝내지 않는다. retained resource는 authoritative hub storage에 정산되고 이후 준비/생산/연구의 실제 입력이 된다.
24. 다음 원정 변화는 저장된 세계 상태가 실제 비용·환경·encounter 중 하나 이상을 변경해야 한다. `region_01_pressure`는 preparation cost뿐 아니라 Region 01 적 수와 salvage hazard에도 영향을 준다.
25. M2의 salvage→supply 직접 변환은 첫 vertical slice의 최소 adapter다. M4에서 정식 request/production 흐름으로 승격하되 인과관계는 유지한다.
26. M2 전투에서 바닐라 엔티티를 사용하는 것은 **behaviour/runtime 검증용 technical proxy**에 한정한다. 이를 production creature art/최종 AI로 간주하지 않고 M3 reference/visual gate 전에 외형을 확정하지 않는다.
27. elite 역할은 단순 HP 배수만으로 만들지 않는다. 최소 하나 이상의 관찰 가능한 대응/반격 창이 있어야 한다. Region 01 technical elite는 Ravager의 shield-stun 상호작용으로 이 원칙을 검증한다.
28. resource와 combat은 별도 체크리스트가 아니라 같은 원정 안에서 선택 압력을 만들어야 한다. Region 01에서는 빠른 extraction과 patrol suppression + bonus salvage를 실제 보상 차이로 연결한다.
29. patrol-clear 보상은 **살아 있는 위협의 위치**로 우회할 수 없어야 한다. 현재 M2 runtime은 run sequence가 소유한 entity handle을 추적하며, 기술 cell 밖으로 유인된 살아 있는 위협도 여전히 patrol을 미완료 상태로 유지한다.
30. extraction 요구조건은 `EXTRACTION_REQUESTED` 상태를 저장하기 전에 검증한다. 실패한 extraction 요청은 authoritative run을 `DEPLOYED`에 남겨 회수 플레이를 계속할 수 있어야 한다.
31. 명시적 player abort는 FAILED 처리와 encounter cleanup 후 technical hub로 귀환한다. death/logout은 실패 정책만 적용하며 임의 순간이동으로 실제 이벤트 의미를 숨기지 않는다.
32. server restart 뒤 process-local encounter ownership을 증명할 수 없는 persisted non-terminal expedition은 성공/진행 상태로 추정하지 않고 명시적으로 `FAILED` 처리한다. 이미 지불한 preparation supply는 환불하지 않는다.
33. persisted technical proxy 정리는 stable run tag + entity-load event로 수행한다. restart cleanup을 startup/per-tick broad world scan이나 위치 기반 ownership 추정으로 구현하지 않는다.

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
12. restart/encounter persistence 작업이면 `RESTART_RECONCILIATION.md`
13. 디자인/자산 작업이면 `REFERENCE_TARGETS.md`, `THIRD_PARTY_ASSETS.md`
14. 자동화/작업 재개 시 `AUTOMATION_HANDOFF.md`에서 현재 검증 체크포인트와 반복 금지 경계를 확인한다.

## 현재 단계

`M3 — PLAYER COMBAT BUILD VERIFIED / HUMAN FIELD PLAY + BOSS PRESENTATION NEXT`

M0/M1의 content graph, validator, atomic runtime snapshot, persistence, GameTest/CI/JAR 기반은 완료된 생산 기반으로 취급한다. M2의 원정 vertical slice도 preparation supply → Region 01 진입 → salvage/전투 선택 → extraction/failure → hub 정산 → pressure/다음 원정 변화와 restart/owner 경계까지 자동 검증되어 있으며, 같은 authority/lifecycle 작업을 회귀 근거 없이 반복하지 않는다.

M2는 자동화 검증과 별개로 실제 플레이 감각 검수가 남아 있으므로 `PLAYTESTED` 완료로 선언하지 않는다.

M3에서 현재까지 구축·검증된 production 경계:

- `mobile_pressure` / `reach_commitment` 두 player weapon family와 `recovery_pivot` technique module
- `telegraph → ACTIVE → recovery` 단일 authoritative attack clock
- server-owned main-hand ItemStack loadout component와 move-id-only authenticated serverbound intent
- 두 개의 configurable client combat action slot; 최종 control layout이 승인되지 않았으므로 기본 키는 임의로 배정하지 않음
- field-play용 provisioning 명령: `/riftfrontier weapon mobile`, `/riftfrontier weapon mobile pivot`, `/riftfrontier weapon reach`, `/riftfrontier weapon reach pivot`
- 실제 대상에게 도달하는 server-authoritative field-impact bridge
- `mobile_pressure entry < finisher < reach_commitment` 역할 차이를 보존하는 임시 field calibration geometry
- 세 공격 모두 `1.0F` diagnostic damage로 유지하여 승인되지 않은 damage hierarchy를 만들지 않음
- ACTIVE-only target admission, per-execution UUID deduplication, server-side damage authority
- Region 01 first-boss source rig로 Quaternius CC0 `Dragon Evolved` 선택
- source Atlas art/material을 제거한 sanitized glTF geometry/skin/animation payload와 custom skinned-mesh importer/renderer
- server-authoritative boss semantic timeline → reviewed animation sample → skinned frame → Minecraft custom geometry submission 경로
- client resource reload에서 geometry/animation/material publication generation을 분리·검증하는 presentation gate

현재 플레이어 field-impact 검증 기준은 code commit `2d94c54cfddcb8d81d0ae7567a4d90adb0cd0dca`, GitHub Actions `Build Riftfrontier` run `34739314980`이다. 해당 run은 toolchain, asset-intake tests, `clean test build`, 9개 required native GameTests, dedicated-server smoke, Xvfb client initialization smoke, executable-JAR 검사와 artifact/report 업로드까지 성공했다.

검증 executable JAR:

- `riftfrontier-0.1.0-alpha.1.jar`
- SHA-256 `310257a14a6e8d7c990aa21fb2bff314dd0cfea3234825c5379a1912fb80b8d1`

이 체크포인트의 정확한 상태는 `CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED`다. Human player combat field play와 multiplayer field play는 아직 `NOT TESTED`다.

Region 01 boss는 geometry/rig/custom renderer 경로까지 준비됐지만 **final material/texture, attack-specific production animation authoring, VFX, sound, real hitbox/scale alignment, encounter integration과 human readability는 아직 승인·검수되지 않았다.** Source `Atlas`를 그대로 되살리거나 AI가 임의의 색/재질 언어를 발명해 이 게이트를 우회하지 않는다.

## 다음 정확한 개발 경계

1. 플레이어 전투는 `docs/M3_PLAYER_COMBAT_FIELD_PLAY.md`에 따라 실제 Minecraft에서 검수한다. facing geometry, mobile-vs-reach 실전 거리 차이, 한 실행당 1회 타격, telegraph/ACTIVE/recovery 인과, loadout swap/unequip 취소를 확인한다.
2. 관찰된 증상 없이 field-impact 거리·폭·damage를 조정하지 않는다. 반복 튜닝이 시작되면 임시 Java calibration을 production data policy로 승격한다.
3. 위 human gate와 독립적으로 진행 가능한 다음 visible-quality 작업은 **이미 선정된 Dragon Evolved 기반 Region 01 boss material/animation/VFX/sound/readability**다. 실제 상용 게임/대형 모드 reference와 합법적 외부 자산을 우선하고 provenance를 기록한다.
4. 현재 working custom skinned-mesh renderer를 GeckoLib로 재구현하지 않는다. 이후 별도 자산이 실제로 GeckoLib를 요구할 때만 버전·효익을 다시 검증한다.
5. Region 01 boss를 production encounter graph에 넣기 전에는 최소한 material/presentation, Minecraft scale, 보이는 공격 범위와 hit geometry, authoritative damage/resource policy가 근거와 함께 잠겨야 한다. 임의 수치나 placeholder art로 gate를 통과시키지 않는다.
6. M2 원정 루프의 manual field play도 여전히 필요하다. Xvfb/client smoke나 자동 GameTest를 `PLAYTESTED` 또는 `MULTIPLAYER TESTED`로 확대 해석하지 않는다.
7. 첫 vertical slice의 목표는 기능 수 증가가 아니라 `준비 → 진입 → 탐사/전투/회수 → 철수 → 투자 → 다음 원정 변화`와 M3 전투/보스 presentation이 실제 Minecraft에서 하나의 게임처럼 연결되는 것이다.
