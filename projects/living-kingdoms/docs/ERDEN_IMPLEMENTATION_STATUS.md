# 에르덴 왕국 구현 상태

이 문서는 《Living Kingdoms》의 에르덴 왕국이 실제로 어디까지 완성되었는지 판단하는 내부 정본이다. CI가 초록색이라는 이유만으로 왕국 전체를 완성으로 선언하지 않는다.

## 0. 현재 기준

- Minecraft Java: 26.2
- NeoForge: 26.2.0.38-beta
- Java: 25
- 모드 버전: `0.1.0-alpha.12`
- 최근 완전 canonical 기준: Build #798
- Build #798 run: `32577276870`
- Build #798 source commit: `941f0e50d2198730852464c1089224934aa67f09`
- Build #798: Java 25 clean / fresh dedicated server / graphical client / package 모두 PASS
- Build #798 JAR SHA-256: `46be5f9adc0e1ba3c39f4a8efa0273a3fe32f49a0274645dee0b27c29c8a28e2`
- 현재 소스에는 Build #798 이후 `Erden Gameplay Rules Lock R1`을 추가한다. 새 canonical release는 이 규칙 audit까지 다시 통과해야 한다.

## 1. 세계 규모와 완성 기준

월드 바이블 목표:

- 왕국 영역: 약 48km × 40km / 1,920km²
- 모의 인구: 약 68,000명
- 왕도 및 교외 인구 목표: 약 11,800명
- 시장도시 6곳
- 촌락 18곳
- 소촌 40곳
- 장원 12곳
- 수도원 4곳
- 국경 요새 3곳
- 광산·채석장 5곳
- 왕립 역참 14곳

현재 구현은 이 국가 전체 목표의 일부다. 다른 왕국과 종족은 에르덴의 완성 gate를 충족하기 전 플레이 가능 항목으로 열지 않는다.

## 2. 현재 플레이 가능한 시작

현재 서버가 허용하는 origin은 정확히 하나다.

- 인간
- 에르덴 왕국
- 평범한 주민
- 왕도 시민구 임대방

캐릭터 생성 데이터 구조는 향후 확장을 허용하지만, 실바나·카르둠이나 엘프·드워프 등 미완성 선택지는 현재 UI/서버에 노출하지 않는다.

## 3. 왕도 물리 구현

### 왕도 기반

- 1블록 = 1미터
- 성벽 내부 약 2.4km × 1.8km
- capital revision 4
- layout revision 15
- 청크 단위 스트리밍 건설
- 바닐라 마을·전초기지 등 부적절한 Living Realm 구조물 생성 차단
- 실제 왕실대로·간선·순환로·골목·성벽·수문·랜드마크 유지

### 연속 시가지

- 총 필지: 233
- 실제 출입구: 233
- facade style: 6
- 연립주택 77
- 상점 50
- 여관 31
- 제빵소 21
- 마구간 17
- 창고 15
- 경비초소 12
- 목욕시설 10

233개 필지는 가짜 외벽만 존재하는 것이 아니라 원본 구조의 실제 보행 가능한 내부를 기능 공간으로 사용한다.

### 내부/상층

- authored ground placements: 233 / 233
- 기능 fixture: 1,117
- 침대: 305
- economy container 대상: 156
- synthetic room: 0
- source blocks cut: 0
- upper buildings: 233 / 233
- 2개 이상 upper level 계획 건물: 50
- planned upper levels: 283
- planned rooms: 503
- 신규 source-air authored floor 대상: 80
- 기존 source upper route 대상: 153
- 추가 full interior physical expansion: 39 buildings / 39 levels / 78 rooms

상세 topology와 외부 구조 라이선스는 별도 urban validation 문서를 정본으로 한다.

## 4. 왕궁 Citadel

현재 기능 구역:

- audience_hall
- royal_council
- royal_chancery
- royal_archives
- guard_command
- service_quarter

Build #796에서 간헐적으로 나타났던 `connected_cells=1` 고착은 첫 밀폐 셀을 즉시 anchor로 고르던 공통 결함이었다. PR #114에서 anchor를 실제 connected floor와 unique fixture capacity로 사전검증하도록 수정했다.

재현 seed 220822의 audience_hall은 수정 전 1셀 고착에서 수정 후 `fixtures=4 / connected_cells=5`로 통과했고, 3개 독립 fresh world 연속 감사도 PASS했다.

Build #798 canonical에서는 royal_archives가 `fixtures=6 / connected_cells=17`로 통과했고 Citadel 6/6 전체 PASS했다.

acceptance 최소치 4/3은 낮추지 않았다.

## 5. 왕도 인구·경제·운송

왕도 정본:

- 가구 77
- 주민 231
- 노동자 154
- 부양가족 77
- 물리 경제 sites 156
- warehouses 15
- household wallets 77
- 상점 50
- 7일 휴무 분산
- 재고 기반 가격
- 가구 장보기 루틴
- 품절/휴무/구매력 부족/구매 성공 경로
- transport revision 2
- route planning / loaded obstacle checks / persistent jobs / authoritative escrow / aggregate fallback

## 6. 왕도 밖 현재 구현

### 외곽 생산·생활권

- exterior nodes: 18
- 가구 74
- 주민 216
- 노동자 142
- 부양가족 74

외곽 residence/workforce/lifecycle/estate/supply 계층이 존재하지만, 이것이 월드 바이블의 모든 시장도시·촌락·소촌·장원·광산·항구를 완성했다는 의미는 아니다.

### 지역 정착지 Phase 1

현재 실제 지역 정착지: 6

- harvest_crossing
- silvermead
- sunfield
- pinewatch
- blackstone
- ironvale

현재 합계:

- 건축 60
- 가구 48
- 주민 144
- 노동자 96
- 부양가족 48
- 시장 6

### 지역 행정·치안

- councils 6
- reeve + clerk = officials 12
- public guard posts 12
- 실제 경비 12
- 마을별 세금/재정/경비 급여/경비 사망·대체 기반
- 국도 보안 settlement assignment 6

### 지역 공동체 생활 Phase 1

기존 144명을 복제하지 않고 그대로 사용한다.

비근무 주민의 현재 생활 행동:

- 집/아침 식사
- 시장 심부름
- 어린이 광장
- 이웃 방문
- 가족 저녁
- 여관 모임
- 휴일 사교
- 물자 부족 반영

대표 마을 CI는 실제 집·광장·여관·시장을 순차 physical probe하며 실제 주민 identity와 보행 가능 목적지를 검사한다.

### 국도

- corridor 8
- waystation 4
- 총 modeled road length 33,519m
- 물리 도로/역참 streaming
- persistent forced chunk 없음
- loaded courier/cart projection 기반

## 7. Erden Gameplay Rules Lock R1

현재 게임 규칙의 정본은 `ERDEN_GAMEPLAY_RULES_LOCK.md`다.

핵심:

- 에르덴 단일 origin
- 개인 2×2 crafting 차단
- 바닐라 전문 생산 workstation/menu 우회 차단
- 이름 있는 관리 시민의 vanilla villager trade 우회 차단
- 침대 night skip 금지
- 개인 vanilla respawn checkpoint 금지
- 공유 world clock 유지
- 치명 피해 → death screen 대신 제도적 구조·후송
- 아이템 보존
- 최대 8은화 구조·치료비
- 30% 체력 복구 + 일시적 weakness/slowness
- 최초 입국/제도적 후송 외 무료 순간이동을 일반 교통으로 사용하지 않음
- 수도 성벽권과 6개 정착지는 시민 안전권
- 자연/청크생성/순찰 계열 ambient hostile spawn 차단
- 에르덴 custom ecology도 지역 정착지를 침범하지 않음
- 국도는 경비가 있으나 위험 유지
- 야생 위험 유지
- scripted/command/spawner 전투는 ambient 필터가 자동 제거하지 않음
- 멀티플레이 세계/NPC/경제/법/물류는 공유, origin/개인 경제/숙련은 플레이어별

## 8. UI와 클라이언트

현재 graphical CI에서 실제 Minecraft client render thread까지 확인한다.

검증 대상:

- 고정 에르덴 시민등록 화면
- origin 선택 우회 방지
- 왕국 기록부 overview/equipment/map/skills
- mastery/tree growth views
- atlas drag/zoom
- 작은 viewport 반응형 배치
- controls fit / overlap free
- realm loading screen

Linux CI의 narrator/OpenAL 경고는 모드 크래시가 아닌 테스트 환경 제한으로 분리한다.

## 9. 아직 에르덴 왕국 완성이 아닌 이유

다음은 여전히 핵심 미완성 범주다.

- 시장도시 6곳을 도시 규모와 독립 생활권으로 완성
- 촌락 18 / 소촌 40의 실제 분포와 역할
- 장원 12 / 수도원 4 / 국경 요새 3
- 광산·채석장 5의 실제 갱도·생산·숙소·운송
- 은빛강 수운, 부두, 세관, 선박, 계절 수위와 물류
- 왕도 8개 구역의 역할/건축/시각 차별화 강화
- 왕실 생활, 행정 인물, 왕궁 세부실과 실제 동선
- 주민 지속 관계·친밀도·갈등·소문·기억
- 결혼/출생/성장/노화/가구 재편/이주를 지역 사회까지 확대
- 고용·실업·계약·길드·사업체·소유권을 플레이어 게임플레이에 연결
- 플레이어 소유지에서 합법 건축/자동 생산 허가
- 병원·사원 물리 치료와 전투불능 후송 연결
- 여관·주택의 휴식/피로 기능
- 도로/역참/마차/배 authority를 사용하는 실제 장거리 여행 UI
- 법: 목격→조사→구금→법정→판결→형 집행의 플레이어 체험 완성
- 종교: 달력·장례·혼인·구호·서약과 실제 생활 연결
- 계절: 생산·행동·생태·수운에 실제 영향
- 에르덴 전용 동식물/몬스터의 수와 먹이사슬/행동/모델·텍스처 확대
- 전투·성장·탐험·퀘스트를 왕국 사회와 실제 연결
- 장시간 플레이/저장 호환/멀티플레이 실기 회귀감사

## 10. 다음 개발 우선순위

Gameplay Rules Lock이 canonical main에서 녹색이 된 뒤에는 국가 규모를 실제로 채운다.

1. 시장도시/촌락 hierarchy와 물리 정착지 확대
2. 농업·목축·광산·임업 생산지 밀도 확대
3. 은빛강 항구/수운
4. 플레이어 고용·계약·소유권·합법 생산
5. 병원/사원/여관 등 생활기관
6. 사회관계와 장기 lifecycle
7. 법/종교/계절/생태 심화
8. 장거리 여행·지도·정치 경계 완성
9. 전투/탐험/퀘스트 통합
10. 장시간/멀티플레이 실플레이 감사

이 기준을 충족하기 전에는 에르덴을 '다 만들었다'고 부르지 않는다.
