# 에르덴 지역마을 행정·치안 1차 검증

## 범위

이 문서는 에르덴 왕국의 두 번째 생활권인 6개 지역마을에 추가된 **행정·재정·경비·국도 치안 revision 1**의 실제 구현 범위와 검증 결과를 정본으로 기록한다.

대상 마을은 다음 6곳이다.

- `harvest_crossing` / 수확나루
- `silvermead` / 은초원
- `sunfield` / 해들판
- `pinewatch` / 솔망루
- `blackstone` / 흑석
- `ironvale` / 철골짜기

기존 지역사회 48가구 / 144명 주민 / 96명 노동자 / 48명 부양인구는 유지한다. 새 행정 시스템은 기존 주민 중 각 마을의 `reeve`와 `clerk`를 그대로 실제 행정 담당자로 사용하므로 별도의 가짜 촌장·서기 인구를 만들지 않는다.

## 구현 계약

### 1. 마을 의회와 재정

- 6개 독립 마을 의회
- 실제 `reeve` 6명 + `clerk` 6명, 총 12명 행정 담당자
- 마을별 영구 treasury
- 지역경제의 누적 생산 증가분을 기준으로 한 생산연동 세입
- 세금은 생산량과 무관하게 생성되는 공짜 화폐가 아니다.
- 일일 지출 우선순위: 경비 급료 → 충원 가능 전사자 → 공공계약
- 경비 충원비·계약비는 모두 공공지출 장부에 반영
- 촌장/서기 상호작용으로 재정, 세입, 계약, 경비, 치안·사건 현황 조회
- 로드된 촌장관에는 공공장부용 lectern을 물리화

### 2. 공공계약

현재 revision 1은 다음 공공계약을 실제 마을 예산과 연결한다.

- 비상곡물 비축
- 창고 야간경계
- 국도·진입로 보수
- 국도 순찰
- 시장 질서유지

### 3. 지역 경비대

- 마을당 2개 보직, 총 12개 경비 보직
- 기존 144명 지역주민 통계와 분리된 공공 경비 roster
- 실제 `watch_house_east`를 주둔지로 사용
- 철검 + 철 투구 + 사슬 흉갑
- 주간/야간 2교대
- 초소 → 광장 → 촌장관 → 창고 → 국도 진입부 순찰
- 이동은 navigation only
- 이동 경로가 로드되지 않은 경우 해당 경로를 억지로 로드하거나 텔레포트하지 않음
- 사망 즉시 동일 경비를 복제하지 않음
- 최소 3일 충원 대기 후 마을 treasury에 충원비가 있을 때만 다음 세대 경비 배치

### 4. 치안 사건과 전투

- 로드된 마을에서 `Monster` 계열 hostile 감지
- 사건은 해당 마을 의회 장부와 치안점수에 반영
- 로드된 경비는 `Monster`만 대상으로 근거리 추적/근접공격 루프 사용
- 플레이어와 중립 엔티티는 대상에서 제외
- 경비는 로드되지 않은 구간까지 추적하지 않음

중요: fresh CI는 **경비 전투 코드가 이벤트 루프에 연결되고 Java 25/NeoForge 26.2에서 빌드되는 것**까지 확인한다. 현재 CI는 hostile 한 마리를 의도적으로 생성해 실제 한 타격의 피해량까지 관측하는 전투 시뮬레이션은 하지 않는다. 따라서 `combat_runtime_wired=true`를 "실제 타격 관측 완료"로 해석하지 않는다.

### 5. 국도·역참 치안

- 기존 4개 역참을 6개 지역마을의 관할권으로 배분
- 언로드 상태에서는 aggregate road-watch 장부로 유지
- 플레이어가 실제 로드된 역참에 있을 때만 주변 hostile을 관측해 담당 의회의 사건으로 기록
- 이 시스템은 스스로 청크를 로드하지 않음

## 물리 acceptance

논리적인 경비 roster만으로 PASS하지 않도록 별도의 CI-only 물리 검증기를 둔다.

대표 마을 `harvest_crossing`의 `watch_house_east` 주변 3×3, 총 9청크를 CI에서만 `TicketType.PORTAL`로 일시 스트리밍한다. 실제 `ErdenRegionalSettlementBuilder`로 청크를 건축한 뒤 다음을 검사한다.

- 대표 watch house가 실제 구조 블록으로 존재
- roster와 이름이 일치하는 경비 2명이 실제 Villager 엔티티로 존재
- 2명 모두 철검·철 투구·사슬 흉갑 보유
- 2명 모두 watch-house billet 인근에 위치
- 9개 probe 청크의 지역마을 construction revision이 완료
- 검증 후 임시 티켓 9개 전부 해제
- 일반 게임에서는 이 CI-only probe 경로가 실행되지 않음

실제 성공 marker:

```text
LK_ERDEN_REGIONAL_GOVERNANCE_PHYSICAL_PASS revision=1 settlement=harvest_crossing guards=2 equipped_guards=2 billet_guards=2 probe_chunks=9 structural_blocks=8034 physical_watch_house=true roster_entity_identity=true persistent_equipment=true transient_probe_released=true navigation_runtime_wired=true combat_runtime_wired=true persistent_forced_chunks=false
```

## pre-main 검증 결과

최종 기능 source head `985408267fbde0c005ef28e7cdf8ce5281d0d953`에서 다음 검증이 성공했다.

### 전용 행정·치안 검증

- Workflow: `Validate Living Kingdoms regional governance r1`
- Run: **32561932251**
- Job: **97004767696**
- 결과: **SUCCESS**
- Java: Temurin 25.0.4+7
- Gradle: 9.2.1
- Java 25 clean build: PASS
- fresh dedicated server: PASS
- governance logical marker: PASS
- governance physical marker: PASS
- road security marker: PASS
- JAR 무결성/중복 엔트리/필수 클래스: PASS

논리 marker:

```text
LK_ERDEN_REGIONAL_GOVERNANCE_PASS revision=1 councils=6 officials=12 guard_posts=12 alive_guards=12 production_assessed_tax=true village_treasury=true public_contracts=true guard_payroll=true casualty_replacement=true watch_house_billet=true shift_patrol=true road_gateway_coverage=true hostile_incident_detection=true navigation_only=true loaded_route_guard=true aggregate_when_unloaded=true persistent_forced_chunks=false
```

국도 치안 marker:

```text
LK_ERDEN_REGIONAL_ROAD_SECURITY_PASS revision=1 waystations=4 assigned_settlements=6 covered_waystations=4 village_guard_roster=true aggregate_road_watch=true loaded_waystation_incident_detection=true council_incident_accounting=true no_chunk_loading=true persistent_forced_chunks=false
```

### 일반 Living Kingdoms 전체 PR gate

- Workflow: `Validate Living Kingdoms PR`
- Run: **32561932266**
- Job: **97004744044**
- 결과: **SUCCESS**
- Java 25 clean build: PASS
- fresh dedicated server complete Erden audit: PASS
- graphical client audit: PASS
- JAR verification: PASS

즉 행정·치안 추가 후에도 server-only 기능뿐 아니라 graphical client 진입 및 전체 모드 JAR 검증이 회귀하지 않았다.

## fresh-world 회귀 수치

동일 최종 source head의 fresh dedicated server에서 확인된 핵심 정본은 다음과 같다.

- 지역마을: revision 1 / 6개 / 60개 건축
- 지역사회: 48가구 / 주민 144 / 노동자 96 / 부양인구 48 / 직업 13종
- 지역경제: 시장 6 / 물리 시장 sample 1 / 누적 생산 250 / 소비 136 / 지역 반출 160 / 지역 반입 0 / 왕도 반출 374 / 부족일 2 / 활성 지역배송 9
- 국도: corridor 8 / 역참 4 / revision-1 모델 길이 33,519m
- 지역 물류: local route points 273 / capital route 연결 / escrow·projection·reobservation·return accounting PASS
- 왕도 중심 인구: 77가구 / 231명 / 154명 노동자
- 왕도 물리경제: 사업장 156 / 창고 15 / wallet 77 / 배송 292 / 제작 726 / 판매 616 / 임금 308 / 주문충족 64
- 외곽 생산권: node 18 / 74가구 / 주민 216 / 노동자 142 / 부양인구 74
- KingdomSupply: node 18 / producer 15 / wharf 3 / resource 6 / convoy 18

`33,519m`는 revision 1 국도 그래프의 **모델 길이 합계**다. 모든 33.5km가 항상 청크로 로드되어 있다는 뜻이 아니다. 실제 도로는 방문/CI probe에 따라 스트리밍된다.

## 보장하지 않는 범위 / 다음 단계

revision 1에서 아직 완료로 주장하지 않는 항목:

- 복수 세율·계층별 세법·면세·체납
- 법원·재판·구금·범죄 수사
- 지방 정치 파벌·선거·왕실 임명 갈등
- 개별 지역가구 wallet과 완전한 개인 가격/상거래
- 주민 생애주기, 연애·결혼·출산·상속
- 저녁 식사·여가·주점·예배·축제·이웃관계 등 풍부한 공동체 생활루틴
- CI에서 의도적으로 hostile을 생성한 뒤 실제 경비 공격 1회를 피해량까지 관측하는 전투 시뮬레이션

따라서 이번 단계의 완료 정의는 **실재 주민 행정권 + 생산연동 마을재정 + 공공계약 + 실제 물리 경비대/주둔지 + 로드 범위 순찰·Monster 전투 코드 + 4개 역참 치안권 + 영구 장부/언로드 aggregate 모델**까지다.
