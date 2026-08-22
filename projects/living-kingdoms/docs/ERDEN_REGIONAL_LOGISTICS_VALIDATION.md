# 에르덴 지역 국도·장거리 물류 1차 검증

## 범위

6개 지역마을의 기존 생활권·시장·escrow 경제를 왕도와 실제 도로/운송으로 연결한다.

이번 단계의 정본 범위는 다음과 같다.

- 8개 지역 국도 corridor
- 4개 역참
- 6개 지역마을과 왕도 성문을 하나의 연결된 국도 그래프로 구성
- 방문/로드된 청크에서만 실제 지형을 따라 도로 시공
- 지역마을 진입부는 외곽 진입로 → 기존 마을 순환도로 → 창고 적재장으로 연결
- 지역 시장 간 `TradeShipment`와 `regional:*` 왕도 공급 shipment의 기존 escrow를 경제 정본으로 유지
- 플레이어 근처 shipment만 기존 운송 엔티티 런타임에 authoritative delivery job으로 물리화
- 왕도 진입 뒤 기존 왕도 도로 A*를 이용해 실제 창고까지 연결
- 관측 중에는 aggregate 도착시계가 물리 운송보다 먼저 정산하지 못하도록 보류
- 비관측 상태에서는 엔티티 없이 aggregate 시뮬레이션으로 복귀
- 재관측 시 비물리 projection만 현재 aggregate 진행 위치로 전진 동기화
- 관측 중 장애물은 실제 지연 또는 반송을 유발할 수 있음
- 반송 화물은 출발지로 돌아가며 transport 회계의 `totalReturned`에 기록
- 일반 게임에서는 persistent forced chunk를 만들지 않음

## 도로 정본

검증 marker:

```text
LK_ERDEN_REGIONAL_ROADS_PASS revision=1 corridors=8 waystations=4 road_metres=33519 network_connected=true streamed=true terrain_following=true physical_road=true physical_waystation=true persistent_forced_chunks=false
```

도로는 왕국 전체를 서버 시작 시 한 번에 생성하지 않는다. 로드된 청크에 필요한 구간만 시공하며, 도로 SavedData가 revision별 시공 상태를 저장한다.

주요 물리 요소:

- carriageway: `PACKED_MUD`
- shoulder: `GRAVEL`
- 수로/불안정 지형 구간의 석재 보강
- 역참의 `BARREL`, 건초, 작업대, 등불, 울타리/계류 요소

`road_metres=33519`는 revision 1 국도 그래프의 모델 길이이며 모든 33,519m가 항상 동시에 로드되어 있다는 뜻이 아니다.

## 장거리 화물 정본

검증 marker:

```text
LK_ERDEN_REGIONAL_LOGISTICS_PASS revision=1 corridors=8 waystations=4 local_route_points=273 capital_route=true authoritative_escrow=true loaded_projection=true reobservation_resync=true navigation_only=true observed_blockage_delays_or_returns=true aggregate_when_unloaded=true return_accounting=true persistent_forced_chunks=false
```

물류 권위는 다음처럼 분리한다.

1. 기존 지역경제 shipment/escrow가 재화 정본이다.
2. 플레이어가 관측 가능한 구간에서는 해당 shipment를 authoritative physical delivery job으로 투영한다.
3. 물리화된 동안에는 기존 aggregate arrival이 앞질러 정산하지 않는다.
4. 청크가 언로드되거나 관측자가 사라지면 물리 엔티티만 제거하고 경제 시뮬레이션은 aggregate 상태로 계속 진행한다.
5. 다시 관측할 때 이미 사라진 projection만 현재 진행률까지 전진 동기화한다. 실제 로드된 운송 엔티티를 순간이동시키지 않는다.
6. 관측 중 길이 막혀 배송 실패/반송으로 판정되면 재화와 회계를 출발지로 되돌린다.

기존 왕도 내부 운송과 기존 18개 외곽 생산지 물류는 별도 기존 시스템이 계속 담당한다. 따라서 이번 지역 운송이 동일 화물을 이중 배송하지 않는다.

## pre-main 검증

PR #111의 기능 정본 head:

```text
8baa15e7827ed71bd7f512d49b6d9c42a81e14fe
```

전용 full validation:

- Workflow: `Validate Living Kingdoms regional logistics r1`
- Run ID: `32481229762`
- Run number: `8`
- Job ID: `96767835467`
- 결과: SUCCESS

통과 항목:

- Java 25 clean build
- Minecraft Java 26.2 + NeoForge 26.2.0.38-beta fresh dedicated server
- 신규 국도 physical marker
- 신규 regional logistics marker
- 기존 6개 지역마을/사회/경제 회귀
- 기존 왕도 인구/물리경제/living economy/transport 회귀
- 기존 외곽 workforce 회귀
- graphical client origin/loading/codex audit
- 최종 JAR unzip/중복 entry/신규 class/version 검증

같은 기능 head의 일반 `Validate Living Kingdoms PR`도 SUCCESS였다.

## 회귀 기준

이번 단계에서도 아래 기존 수치는 유지되어야 한다.

- 지역마을: 6
- 지역 건물: 60
- 지역 가구: 48
- 지역 주민: 144
- 지역 노동자: 96
- 지역 부양인구: 48
- 지역 시장: 6
- 왕도 가구: 77
- 왕도 주민: 231
- 왕도 노동자: 154
- 왕도 물리 경제 sites: 156
- 왕도 warehouses: 15
- 왕도 wallets: 77
- 기존 외곽 생산 nodes: 18
- 기존 외곽 가구: 74
- 기존 외곽 주민: 216
- 기존 외곽 노동자: 142
- 기존 외곽 부양인구: 74
- 왕도 transport schema/revision: 2

## 이 단계에서 일부러 하지 않은 것

이번 작업은 지역 도로/화물의 물리화에 한정한다. 다음 항목은 후속 단계다.

- 6개 마을의 행정권·세금·계약·공공장부 심화
- 지역 경비대·순찰·치안 사건과 국도 안전
- 도적/습격/호위 계약이 실제 물류 위험에 미치는 영향
- 결혼·출생·이주 등 장기 인구 lifecycle
- 더 다양한 일과 후 활동, 여가, 종교/축제/공동체 루틴
- 운송 수요에 따른 역참 인력·말/차량 자산의 장기 경제화

이 문서는 pre-main 결과를 기록한다. 최종 정본 여부는 main 병합 후 canonical Living Kingdoms build와 permanent regional logistics audit가 모두 성공해야 확정한다.
