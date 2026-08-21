# 에르덴 지역 생활권 2차 — 지역경제 검증 계약

이 문서는 에르덴 6개 지역마을을 실제 주민사회 다음 단계인 지역 소비·시장·생산·장거리 공급망으로 연결하는 구현 단위의 정본 검증 계약이다.

현재 대상 모드 버전은 `0.1.0-alpha.12`이며, 이 문서가 추가된 시점의 기능은 아직 main canonical PASS 전이다. 아래 PASS 조건을 실제 Java 25 clean build, fresh dedicated server, graphical client, JAR audit에서 모두 확인한 뒤에만 완료 상태로 갱신한다.

## 1. 범위

대상 지역마을은 기존 물리 마을 6개를 그대로 사용한다.

- `harvest_crossing` — 남부 곡창
- `silvermead` — 남부 강변 시장
- `sunfield` — 남부 곡창
- `pinewatch` — 북부 목축
- `blackstone` — 서부 탄광
- `ironvale` — 서부 철광

기존 60동 건축과 48가구 / 144명 / 노동자 96명 지역사회는 삭제하거나 재생성하지 않는다.

이번 기능 단위는 다음만 추가한다.

1. 실제 `storehouse_west` 건축과 연결된 지역시장 저장공간
2. 살아 있는 해당 직종 노동자 수에 기반한 지역 생산
3. 살아 있는 가구 수에 기반한 식량·연료 소비
4. 마을 간 부족분 교환과 도착시간이 있는 운송 escrow
5. 지역 내 비축분을 제외한 진짜 잉여 생산물의 기존 에르덴 왕도 공급망 편입
6. 청크가 로드된 시장에서는 실제 컨테이너 재고를 권위 상태로 사용
7. 청크가 언로드된 동안에는 SavedData aggregate로만 처리하고 강제 로딩하지 않음

## 2. 물리 시장

각 마을의 기존 `storehouse_west` 건물 동쪽 적재 공간에 실제 `BarrelBlockEntity` 컨테이너를 둔다.

컨테이너 생성 규칙:

- 해당 청크가 실제로 로드되어 있어야 한다.
- `ErdenRegionalSettlementSavedData`가 그 청크를 현재 지역마을 revision으로 완공했다고 기록해야 한다.
- 기존 비공기 블록을 덮어쓰지 않는다.
- 시장 때문에 영구 forced chunk를 만들지 않는다.

관리 자원은 현재 지역 산업과 주민 기본생활에 필요한 다음 5종이다.

- wheat
- coal
- hay
- leather
- iron

한 번 물리화된 시장에서는 플레이어가 barrel에서 자원을 꺼내거나 넣은 결과를 다음 동기화에서 SavedData로 다시 읽는다. 관리 자원 외 플레이어 아이템은 삭제하지 않는다.

## 3. 주민 노동과 생산

지역 생산은 고정된 가상 생산량만 찍지 않는다. `ErdenRegionalSocietySavedData`의 실제 생존 노동자 중 해당 산업 직종이며 그날 휴무가 아닌 인원만 생산에 기여한다.

현재 생산 관계:

- grain: farmer → wheat
- ranch: shepherd → hay + leather
- colliery: coal_miner → coal
- iron_mine: iron_miner → iron
- river_market: 원재료 채굴/생산 없음

따라서 주민 사망과 휴무는 지역 생산량에 실제로 영향을 준다.

## 4. 가구 소비

각 마을에서 살아 있는 가구 수를 매일 다시 계산한다.

- wheat: 생활 식량 소비
- coal: 격일 난방/생활 연료 소비

재고가 필요량보다 적으면 실제 보유량까지만 소비하고 shortage day를 기록한다. 부족분을 음수 재고나 가짜 무한 보급으로 채우지 않는다.

## 5. 마을 간 교환

식량·석탄 비축량이 목표치보다 부족한 마을은 다른 5개 마을 가운데 자체 비축분보다 진짜 잉여가 있는 가장 가까운 공급지를 찾는다.

화물은 출발 시 공급지 재고에서 즉시 제거되고 `TradeShipment` escrow에 들어간다. 목적지에는 도착 tick이 지난 뒤에만 적재된다.

- 거리: 마을 중심 간 실제 metre-scale 좌표 기반
- 도착시간: 거리 기반 modeled ticks
- unloaded route: aggregate shipment 상태 유지
- 즉시 순간이동식 destination credit 금지

## 6. 왕도 공급망 연결

지역마을 잉여 생산물은 기존 18개 `ErdenKingdomSupply` 노드를 새 숫자로 덮어쓰거나 대체하지 않는다.

지역 비축분을 먼저 남긴 뒤 남은 물량만 `regional:<settlement_id>` 출발지의 기존 `ErdenKingdomSupplySavedData.ShipmentState`로 들어간다.

이 shipment는 기존 왕도 공급망과 동일하게:

- 실제 왕도 창고를 목적지로 선택
- 지역마을 → 가장 가까운 수도 관문 → 왕도 창고 거리 계산
- wagon 이동시간 부여
- 출발 시 지역 재고 차감
- transit 동안 escrow 유지
- 도착 뒤에만 `ErdenPhysicalEconomySavedData` 왕도 창고 stock과 supply metrics에 반영

즉 지역마을 생산물이 왕도에 도착하기 전에는 왕도 경제가 사용할 수 없다.

## 7. 회귀 금지

이번 기능 때문에 아래 기존 정본 수치를 변경하지 않는다.

- 왕도 continuous urban fabric 233동
- 왕도 인구 77가구 / 231명 / 노동자 154명
- 왕도 physical economy sites 156 / warehouses 15 / wallets 77
- 기존 exterior workforce 18 nodes / 74가구 / 216명 / 노동자 142명
- 기존 KingdomSupply nodes 18 / producers 15 / wharves 3
- 지역마을 건축 6개 마을 / 60동
- 지역사회 48가구 / 144명 / 노동자 96명 / 부양가족 48명

## 8. 완료 판정 marker

fresh dedicated server에서 다음 신규 marker를 요구한다.

`LK_ERDEN_REGIONAL_ECONOMY_PASS revision=1 settlements=6 households=48 markets=6`

동일 로그에는 최소한 다음 증거가 함께 있어야 한다.

- `physical_barrel=true`
- `player_inventory_authoritative=true`
- `local_reserve_first=true`
- `worker_linked=true`
- `household_consumption=true`
- `local_trade_escrow=true`
- `kingdom_supply_escrow=true`
- `unloaded_aggregate=true`
- `persistent_forced_chunks=false`

그리고 같은 fresh world에서 기존 지역마을, 지역사회, 왕도 population, physical economy, living economy, transport revision 2, exterior workforce PASS가 모두 유지되어야 한다.

## 9. 이번 단계에서 아직 완료로 보지 않는 것

- 마을 간 전체 국도/지방도로의 연속 물리 도로망
- 장거리 wagon/cart 엔티티가 전 구간을 실제 주행하는 시스템
- 지방 세금·행정 예산·치안 조직
- 여관/시장 개별 상점의 화폐 거래 심화
- 주민 개인 지갑과 지역시장 가격 형성

특히 장거리 운송은 이번 단계에서 거리·출발·도착·escrow를 권위적으로 저장하지만, 언로드된 5~10 km 전 구간을 가짜로 계속 로드하지 않는다. 실제 도로와 수레의 loaded-segment physicalisation은 후속 기능 단위에서 구현한다.
