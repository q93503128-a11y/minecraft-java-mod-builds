# 에르덴 지역마을 주민사회 1차 검증

이 문서는 《Living Kingdoms》의 **에르덴 지역 생활권 2차** 가운데 첫 완성 단위인
“6개 물리 지역마을을 실제 가구·주민·직업·출퇴근이 존재하는 주민사회로 전환”한 상태를 기록한다.

## 기준

- 모드 버전: `0.1.0-alpha.12`
- 이전 정본 회귀 기준: `Build Living Kingdoms #792` / Run `32446457627` / SUCCESS
- 이전 런타임 기준 커밋: `d0220131522027c364fe577ce962e3afdf9531a7`
- 기능 사전 검증: Run `32452540068` / SUCCESS
- Java: 25
- Minecraft Java: 26.2
- NeoForge: 26.2.0.38-beta
- Gradle: 9.2.1

최종 main canonical build 번호와 artifact는 이 변경이 main에 반영된 뒤 `.github/build-pointers/living-kingdoms.json`이 권위값을 기록한다.

## 1. 이번에 완료한 실제 기능 단위

기존 6개 지역마을과 60개 건축을 삭제·대체하지 않고 그 위에 주민사회 계층을 결합했다.

대상 마을:

- `harvest_crossing` / 수확나루
- `silvermead` / 은초원
- `sunfield` / 해들판
- `pinewatch` / 솔망루
- `blackstone` / 흑석
- `ironvale` / 철골짜기

정본 인구 계획:

- 지역마을: **6**
- 마을당 실제 가구: **8**
- 총 가구: **48**
- 총 주민: **144**
- 노동자: **96**
- 부양가족: **48**
- 가구당 구성: 노동 가능한 성인 2명 + 아이 또는 노인 1명

가구는 숫자 슬롯이 아니라 기존 60개 실제 건물 가운데 다음 주거 역할의 실제 좌표에 고정된다.

- `farmstead_west`
- `farmstead_east`
- `artisan_house_west`
- `craft_house_east`
- `homestead_west`
- `homestead_east`
- `reeve_hall`
- `village_inn`

## 2. 직업 연결

현재 지역사회 정본 직업은 13종이다.

- 농부
- 목동
- 탄광 광부
- 철광 광부
- 지역 상인
- 상인
- 장인
- 창고지기
- 시장 일꾼
- 지방관리(reeve)
- 서기
- 여관주인
- 역참 담당

마을의 기존 물리 산업과 직업이 연결된다.

- `grain` → 농부
- `ranch` → 목동
- `colliery` → 탄광 광부
- `iron_mine` → 철광 광부
- `river_market` → 지역 상인

관리·여관·창고·장인·시장 보조 직종은 각 마을의 실제 `reeve_hall`, `village_inn`, `storehouse_west`, 제작주택, 시장/광장 좌표를 사용한다.

## 3. 주민 실체화 규칙

`ErdenRegionalSocietySavedData`가 가구·주민·직업·교대·휴무·사망 상태를 영구 저장한다.

주민 엔티티는 단순 통계가 아니다.

1. 해당 주택 청크가 로드되어 있어야 한다.
2. `ErdenRegionalSettlementSavedData`에서 그 청크가 지역마을 건축 revision 1로 실제 완공됐어야 한다.
3. 그 조건을 만족할 때만 실제 vanilla `Villager` 엔티티가 주택 주변의 서 있을 수 있는 물리 블록 위에 생성된다.
4. 같은 이름의 지역 주민이 이미 로드되어 있으면 다시 생성하지 않는다.
5. 사망한 founding resident는 `dead_residents` 장부에 기록되어 청크 재로딩이나 재접속 뒤 복제되지 않는다.

왕도·기존 외곽 주민과 이름 키가 충돌하지 않도록 지역 주민 이름에는 소속 마을 이름이 포함된다.

## 4. 집 ↔ 직장 일상 루틴

노동자는 직업별 교대시간과 7일 주기 분산 휴무일을 가진다.

로드된 생활권에서는 현재 시각과 휴무 여부에 따라 실제 집 또는 실제 근무지로 vanilla navigation을 요청한다.

중요한 물리성 규칙:

- 출퇴근에 텔레포트를 사용하지 않는다.
- 주민사회 관리자는 출퇴근을 위해 청크 티켓을 만들지 않는다.
- 집과 근무지 사이를 샘플링했을 때 경로 청크 하나라도 언로드 상태면 이동 명령을 내리지 않는다.
- 언로드된 지역의 인구 상태는 SavedData로 유지하되 가짜 물리 이동을 연출하지 않는다.
- 기존 지역마을 스트리밍의 persistent forced chunk 규칙을 바꾸지 않는다.

## 5. 물리 월드 감사

CI 대표 가구는 `harvest_crossing`의 `farmstead_east`다.

사전 검증 Run `32452540068`은 fresh dedicated server에서 다음을 동시에 요구했고 SUCCESS로 종료됐다.

- 6개 지역마을 / 60개 건축 1차 물리 감사 PASS
- 대표 가구 실제 주택 구조 블록 존재
- 대표 농부의 실제 곡창 작업지 `DIRT_PATH` 존재
- 집과 작업장이 32m 이상 분리되어 가짜 동일좌표 출퇴근이 아님
- 대표 가구 주민 3명이 실제 Villager 엔티티로 로드됨
- 먼 `ironvale` 방향의 언로드 경로에서 navigation guard 작동
- 영구 사망 장부 존재
- 주민사회 계층이 persistent forced chunk를 만들지 않음

권위 PASS marker:

`LK_ERDEN_REGIONAL_SOCIETY_PASS revision=1 settlements=6 households=48 residents=144 workers=96 dependents=48 professions=13 loaded_sample=3 physical_home=true physical_workplace=true distinct_workplace=true commute_schedule=true navigation_only=true loaded_route_guard=true permanent_death_ledger=true aggregate_when_unloaded=true persistent_forced_chunks=false`

## 6. 기존 정본 회귀

같은 fresh dedicated server 검증에서 다음 기존 계층을 함께 요구했다.

- 왕도 인구: 77가구 / 231명 / 노동자 154명
- 왕도 물리 경제: sites 156 / warehouses 15 / wallets 77
- 왕도 생활 경제 revision 1
- transport revision 2
- 기존 외곽 노동자 사회: nodes 18 / households 74 / residents 216 / workers 142 / dependents 74
- 6개 신설 지역마을 물리 건축 revision 1

따라서 이번 48가구는 기존 왕도 77가구나 기존 외곽 74가구를 대체한 수치가 아니다. 서로 다른 실제 생활권 계층이다.

## 7. 빌드·클라이언트·패키징 검증

Run `32452540068` 결과:

- Java 25 clean build: PASS
- fresh dedicated server regional society audit: PASS
- graphical client audit: PASS
- JAR ZIP 무결성/중복 엔트리/버전 검사: PASS
- 신규 `ErdenRegionalSocietyManager.class`: JAR 포함 확인
- 신규 `ErdenRegionalSocietySavedData.class`: JAR 포함 확인

첫 사전 검증 Run `32452260840`에서는 NeoForge 26.2의 `ChunkPos` API와 맞지 않는 `toLong()` 호출 1건이 clean build에서 검출됐다. 이를 기존 정본과 동일한 명시적 `(chunkX, chunkZ)` 64비트 패킹으로 교체한 뒤 Run `32452540068`에서 처음부터 전 단계 재검증했다.

## 8. 아직 완료 처리하지 않는 범위

이번 단계는 “지역마을 주민사회 1차”다. 아래는 다음 깊이 구현 대상이며 아직 완료라고 기록하지 않는다.

- 마을 주민의 실제 소비와 지역 시장 재고 순환
- 지역 생산물의 왕도/기존 에르덴 물류망 편입
- 6개 마을과 기존 간선망을 잇는 실제 지역 간 도로·운송
- 지방 행정의 세금·허가·계약
- 치안 인력과 범죄/사건 대응
- 가족 관계·결혼·출생·성장·노화·이주를 지역마을까지 확장
- 보다 세밀한 휴식·식사·사교·여가·종교/축제 일상 루틴

이 항목들이 숫자 장부만 존재하는 상태에서는 완료 처리하지 않는다.
