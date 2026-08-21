# 에르덴 왕국 구현 상태

이 문서는 사용자 배포용 패치 노트가 아니라 《Living Kingdoms》의 에르덴 왕국 완성도를 판단하기 위한 내부 정본 문서다.

## 0. 현재 정본 체크포인트

- 기준 실행: GitHub Actions `Build Living Kingdoms` #790
- 검증 대상 커밋: `9b717818771f65f1abb302e2f9b9947989bae96c`
- Minecraft Java: 26.2
- NeoForge: 26.2.0.38-beta
- Java: 25
- 모드 버전: `0.1.0-alpha.12`
- Java 25 clean build + licensed asset audit: PASS
- 새 월드 dedicated server 완전 에르덴 감사: PASS
- graphical UI / client startup audit: PASS
- JAR 압축 무결성 검사: PASS
- 공개 사용자 테스트 빌드: 아직 RELEASED 아님

현재 alpha.12 테스트 JAR SHA-256:

`bdf00d88b4cd1548ce9fb8f2004f5bebf28f345429ba0ea1728be1a83fc5c6a2`

## 1. 세계와 왕도 기반

- 1블록 = 1미터 기준의 고정 에르덴 국토와 Living Realm 전용 월드 생성 구조를 사용한다.
- 바닐라 마을·전초기지·사원·폐허 포털 등 바닐라 구조물 시작을 Living Realm에서 차단한다.
- 왕도는 한 번에 가짜 배경으로 그리는 것이 아니라 청크 단위 스트리밍 건설로 물리 블록을 생성한다.
- 왕실대로·간선도로·순환로·생활 골목·성벽·수문·랜드마크·주거·시가지를 실제 월드 블록으로 유지한다.
- Build #790의 realm diagnostic은 `regions=1`, `residences=1`, `metre_scale=true`, `structureless_generator=true`, `vanilla_structures_blocked=true`, `debris_zero=true`로 PASS했다.
- 왕도 생성 진단 기준 capital revision 4 / layout revision 15가 현재 정본이다.

## 2. 왕도 연속 시가지 — 현재 정본

Build #790에서 연속 시가지는 다음과 같이 검증됐다.

- 총 필지: **233**
- 실제 출입구: **233**
- facade style: **6**
- 역할 배치:
  - 연립주택 77
  - 상점 50
  - 여관 31
  - 제빵소 21
  - 마구간 17
  - 창고 15
  - 경비초소 12
  - 목욕시설 10
- 구조 리소스 배치:
  - `all_in_one_house.schem`: 80
  - `player_castle_house.schem`: 64
  - `fantasy_castle_house.schem`: 39
  - `medieval_manor.schem`: 39
  - `medieval_tavern_inn.schem`: 11

옛 문서나 로그에 남아 있던 **4 facade style** 수치는 폐기된 값이다. 현재 정본은 6 style이다.

## 3. 외부 건축 소스와 라이선스

현재 시가지 확장에 사용하는 외부 건축은 명시적 provenance/license 검증을 통과한 소스만 유지한다.

- 기존 `all_in_one_house.schem`
- 기존 `fantasy_castle_house.schem` — retained crop variant 2개
- 기존 `medieval_manor.schem`
- `player_castle_house.schem`
  - source reachable: **611**
  - exit proof: **4**
  - crop side: SOUTH
  - source door: 30,46
  - fragment: 34×38
  - candidate columns: 2
- `medieval_tavern_inn.schem`
  - source reachable: **422**
  - exit proof: **4**
  - crop side: WEST
  - source door: 32,45
  - fragment: 38×34
  - candidate columns: 2

시가지 확장 소스는 CC BY 4.0 조건을 기록하며 attribution은 `assets/livingkingdoms/licenses/ERDEN_URBAN_EXPANSION_CC_BY_4_0.txt`에 포함한다.

독립 소스 수용 기준은 `MIN_INDEPENDENT_SOURCE_REACHABLE = 160`을 유지한다. 이 기준은 편의를 위해 낮추지 않는다.

현재까지 실제 검사 후 거부한 후보:

- Medieval Market Hall: reachable 8
- Medieval Town Hall: 유효 fixed-footprint candidate 없음
- Villager Trading Hall: 보존 가능한 유용한 저층 출입문 없음
- Mega Storage Hall: reachable 10
- Medieval Barn: reachable 102
- Medieval Horse Stable: 실제 reachable 5

위 후보는 crop 모델 자체를 근본적으로 바꾸고 전체 재감사를 하지 않는 한 다시 정본에 넣지 않는다.

## 4. 233개 건물의 실제 내부와 상층

### 4.1 지상층

Build #790의 authored ground functional plan:

- placements: **233 / 233**
- 최소 ground cells: 293
- 기능 fixture: 1,117
- 침대: 305
- economy container 대상: 156
- synthetic room: 0
- source blocks cut: 0

즉 233개 필지는 가짜 카운터가 아니라 원본 건물 내부 바닥을 사용해 기능 공간을 물리적으로 배치한다.

### 4.2 상층 기회와 경로

현재 upper-room opportunity 정본:

- fragments: 6
- buildings: **233**
- `AUTHOR_NEW_FLOOR_IN_VOID`: **80**
- `ROUTE_TO_EXISTING_ROOM`: **153**
- source-only 분석
- world read 없음
- 분석 단계 mutation 0

옛 **116 new-floor candidate** 수치는 폐기된 하드코딩 값이다. 현재 실제 route truth는 **80**이다.

### 4.3 전체 상층 계획

Build #790의 full interior source plan:

- buildings with upper: **233 / 233**
- buildings with 2+ upper levels: **50**
- planned upper levels: **283**
- planned rooms: **503**
- SINGLE_UPPER_PLAN: 183
- MULTI_UPPER_PLAN: 50
- source blocks cut: 0

구조별 대표 topology:

- Fantasy Castle House variant #0: existing `[16:135/4]`
- Fantasy Castle House variant #1: existing `[16:76/3]`
- Medieval Manor: existing `[15:162/1]` + authored `[20:108/2]`
- Medieval Tavern Inn: existing `[19:256/3, 34:28/1]`
- Player Castle House: existing `[11:317/2]`
- All In One House: authored `[18:56/1]`

### 4.4 신규 상층 물리 생성

신규 바닥 구조 승인:

- route-truth candidate: **80**
- classified: 80
- approved: **80**
- needs authored access: 0
- 역할: tenement 24 / bakery 14 / shop 27 / warehouse 15
- source blocks cut: 0

실제 materialization:

- eligible: **80**
- authored floor cells 총합: 4,480
- zero-cut route 유지
- source-air floor 사용
- `LK_ERDEN_AUTHORED_NEW_FLOOR_PASS`: PASS

### 4.5 기존 상층 연결과 추가 전체 확장

기존 source upper room으로 연결되는 건물: **153**.

별도 full interior physical expansion 정본:

- buildings: **39**
- added levels: **39**
- added rooms: **78**
- floor cells: 4,173
- route nodes: 468
- stair blocks: 390
- source blocks cut: 0
- source-air only: true

옛 **40 buildings / 40 levels / 80 rooms** 수치는 폐기된 하드코딩 값이다.

## 5. 에르덴 왕궁 Citadel 기능 내부

Build #787의 유일한 blocker였던 왕궁 내부 furnishing 문제는 revision 4에서 해결됐다.

기존 실패 원인:

- fixture를 놓을 수 있는지 판단할 때 실제 방 연결성 대신 근처 벽 방향 수를 사용해 넓은 홀 중앙을 잘못 거부함
- 최초 연결성 수정은 같은 Y의 평평한 바닥만 따라가 계단·1블록 단차가 있는 왕궁 내부에서 일부 구역이 0 fixture로 멈춤
- 반복 block type이 같은 실제 셀을 여러 fixture로 세는 가능성도 별도로 제거함

현재 revision 4 규칙:

- 이미 enclosure가 증명된 anchor에서 시작
- 같은 실내의 바닥/headroom/ceiling이 유효한 셀만 탐색
- 수평 이동 중 1블록 높이 단차를 실제 보행처럼 허용
- anchor floor 기준 ±1 높이 밖으로는 확장하지 않아 다른 층 침범 방지
- 벽을 통과하지 않음
- fixture마다 서로 다른 실제 x/z 셀을 요구
- 외벽·지붕·원본 장식을 carve하거나 교체하지 않음
- 표준 zone fixture requirement 4, guard/service requirement 3을 그대로 유지

Build #790 실제 결과:

- audience_hall: **6 fixtures / required 4 / connected cells 17**
- royal_council: **6 / 4 / 146**
- royal_chancery: **6 / 4 / 17**
- royal_archives: **6 / 4 / 17**
- guard_command: **6 / 3 / 61**
- service_quarter: **6 / 3 / 60**
- 총 fixture: **36**
- zones: **6 / 6**
- audience reachable: true
- door blocks: 8
- perimeter starts: 489
- traversal walk nodes: 11,269
- `furnishing_connected=true`
- `stepped_furnishing=true`
- `unique_fixture_cells=true`
- `facade_replaced=false`

`LK_ERDEN_CITADEL_INTERIOR_PASS`: PASS.

## 6. 인구와 생활

Build #790 정본 population diagnostic:

- households: **77**
- residents: **231**
- workers: **154**
- dependents: 77
- shortages: 0
- ownership: true
- shifts: true
- residence mode: verified upper or ground

주민은 가구·직업·직장·교대·생애단계·사망 상태를 저장 데이터로 유지하며, 로드된 생활권에서 실제 집과 직장을 기반으로 실체화한다.

## 7. 물리 경제와 운송

Build #790 physical economy:

- worksites: **156**
- warehouses: **15**
- household wallets: **77**
- deliveries: **292**
- crafted: **726**
- sales: **616**
- wages: **308**
- fulfilled households: **64**
- purchase outcomes: **77**
- purchase failures: 13
- wallet coins: 2,172
- physical inventory containers sampled: 3
- authoritative transport: true
- kingdom supply: true

Living economy는 50개 상점의 영업시간, 7일 휴무 분산, 재고 기반 가격, 장보기 루틴, 휴무·품절·구매력 부족·성공 경로를 모두 보존한다.

Transport revision 2 정본:

- manifests: **292**
- modeled travel ticks: 422,571
- route planning: true
- loaded obstacle checks: true
- persistent jobs: true
- authoritative escrow: true
- aggregate fallback: true

## 8. 클라이언트/UI 검증

Build #790 graphical audit는 Linux llvmpipe 환경에서도 실제 Minecraft client render thread까지 구동했다.

PASS marker:

- origin selection diagnostic
- Codex overview/equipment/map/skills
- mastery/tree growth views
- atlas drag/zoom
- responsive 427×240 viewport layout
- controls fit
- overlap free
- realm loading screen

CI 환경에 `flite` narrator native library 및 OpenAL 장치가 없어 narrator/sound warning이 발생하지만 Minecraft client rendering과 Living Kingdoms UI diagnostic은 계속 정상 진행되어 전체 graphical step이 SUCCESS로 끝났다. 이는 모드 크래시가 아니라 CI Linux 오디오/내레이터 환경 제한이다.

## 9. Build #790에서 요구한 핵심 PASS marker

다음 marker를 새 월드에서 모두 확인했다.

- `LK_REALM_DIAGNOSTIC_PASS`
- `LK_ERDEN_CITADEL_INTERIOR_PASS`
- `LK_ERDEN_LANDMARK_INTERIOR_PASS`
- `LK_ERDEN_AUTHORED_GROUND_FUNCTIONAL_PLAN_PASS`
- `LK_URBAN_INTERIOR_DIAGNOSTIC_PASS`
- `LK_ERDEN_AUTHORED_NEW_FLOOR_PASS`
- `LK_ERDEN_AUTHORED_UPPER_ROUTE_PASS`
- `LK_ERDEN_POPULATION_DIAGNOSTIC_PASS`
- `LK_ERDEN_FULL_INTERIOR_EXPANSION_PASS`
- `LK_ERDEN_PHYSICAL_ECONOMY_PASS`
- `LK_ERDEN_LIVING_ECONOMY_PASS`
- `LK_ERDEN_TRANSPORT_PASS`
- `LK_CLIENT_DIAGNOSTIC_PASS`
- `LK_CLIENT_CODEX_DIAGNOSTIC_PASS`
- `LK_CLIENT_LOADING_DIAGNOSTIC_PASS`

## 10. 아직 왕국 완성으로 보지 않는 항목

Build #790은 현재 구현의 회귀검사를 통과한 것이지 에르덴 왕국 전체 콘텐츠가 완성됐다는 뜻은 아니다. 다음 범주는 계속 확장·감사가 필요하다.

- 왕도 밖 농촌·마을·생산지·광산·목축지·항구·도로망의 밀도와 실제 생활 기능 확대
- 왕궁 내부의 더 세밀한 방 역할·가구·왕실 생활·행정 동선과 시각 품질 확대
- 도시 233개 건물의 역할별 장식·가구 다양성과 반복감 감소
- 상점·길드·제작·법·치안·세금·신분·소유권·계약을 플레이어 gameplay와 더 깊게 연결
- 주민 사회관계, 가족 변화, 이주, 출생·성장·노화, 고용·실업, 사건 기반 AI 확장
- 왕도/농촌 간 생산·운송·소비가 실제 지형과 물류 병목에 더 강하게 의존하도록 확장
- 에르덴 지역별 생태계, 전용 동식물·몬스터의 모델/텍스처/먹이사슬/행동 완성
- 전투·성장·탐험·퀘스트가 완성된 왕국 사회와 물리적으로 연결되는 콘텐츠 확대
- 외부 UI 디자인 자산을 왕국 기능별 화면에 일관된 최종 디자인으로 통합
- 성능·저장 호환·장시간 플레이·멀티플레이 회귀 감사

위 항목들을 계속 진행하며, 단순히 CI가 초록색이라는 이유로 왕국을 완성했다고 선언하지 않는다.
