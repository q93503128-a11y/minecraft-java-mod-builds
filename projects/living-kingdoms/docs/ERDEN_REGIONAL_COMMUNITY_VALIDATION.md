# 에르덴 지역 공동체 생활 1차 검증

## 목적

지역 정착지의 기존 사회·경제·행정 정본을 유지한 채, 주민들이 근무 외 시간에 실제 마을 시설을 이용하는 생활 루틴을 갖도록 한 `Regional Community Life Phase 1`의 검증 기록이다.

이 단계는 새 주민을 복제하거나 숫자만 추가하는 단계가 아니다. 기존 6개 지역 정착지의 48가구·144명을 그대로 사용하며, 일·가정·시장·광장·여관을 물리 월드의 실제 목적지와 연결한다.

## 정본 인구 회귀 기준

- 지역 정착지: 6
- 건축: 60
- 가구: 48
- 주민: 144
- 노동자: 96
- 부양가족: 48
- 시장: 6
- 기존 주민 ID/이름을 그대로 사용하며 Community 계층은 별도 주민을 생성하지 않는다.

## 생활 루틴

근무가 필요한 주민은 기존 직업/경제 스케줄이 항상 우선한다. Community 루틴은 비근무 시간에만 적용된다.

현재 검증되는 생활 행동은 다음과 같다.

- 아침 식사 및 집 체류
- 시장 심부름
- 어린이 광장 활동
- 노년층 이웃 방문
- 가족 저녁 식사
- 여관 모임
- 휴일 사교 활동
- 지역 물자 부족을 반영한 일정 선택

동일한 주민·날짜·상태에서는 일정이 결정적으로 선택되며, 물이나 막힌 셀을 목적지로 사용하지 않는다.

## 실제 물리 월드 검증

대표 정착지는 `harvest_crossing`이다. CI는 대표 `farmstead_east` 가구를 대상으로 다음 네 목적지를 순차 검증한다.

1. 집
2. 마을 광장
3. 여관
4. 시장

최종 물리 acceptance:

- 실제 주민: 3명
- 실제 목적지: 4종
- 고유 probe 청크: 36
- 실제 집 구조 확인
- 실제 광장 구조 확인
- 실제 여관 구조 확인
- 실제 시장 배럴 및 시장 materialization 확인
- 저장된 가구 주민 이름과 실제 Villager 엔티티 3명의 identity 일치
- 네 목적지 모두 실제 보행 가능한 Activity target 존재
- 원거리 미로드 목적지에 대해 `routeLoaded`가 거부되는지 확인

검증 marker:

```text
LK_ERDEN_REGIONAL_COMMUNITY_PHYSICAL_PASS revision=1 settlement=harvest_crossing residents=3 destinations=4 probe_chunks=36 physical_home=true physical_square=true physical_inn=true physical_market=true resident_identity=true destinations_walkable=true transient_probe_released=true staged_probe=true refreshed_until_verification=true refresh_ticks=40 navigation_only=true loaded_route_guard=true persistent_forced_chunks=false
```

## 청크 로딩 원칙

정상 게임 플레이에서 Community 시스템은 청크를 강제로 불러오지 않는다.

- 주민 이동은 navigation only
- 이동 경로는 이미 로드된 경로만 사용
- 미로드 목적지를 위해 청크를 불러오지 않음
- persistent forced chunk 없음

물리 acceptance를 위한 CI에서만 `TicketType.PORTAL`을 사용한다. 네 목적지를 한꺼번에 유지하지 않고 현재 검증 중인 한 목적지 주변만 유지한다.

`PORTAL` ticket은 일시적이라 긴 fresh-world 감사 중 만료될 수 있으므로 현재 stage의 ticket만 40틱마다 갱신한다. acceptance가 완료되면 해당 stage의 ticket을 즉시 전부 제거하고 다음 stage로 넘어간다. timeout이나 acceptance 요구조건 자체는 완화하지 않았다.

## 회귀검사 범위

Community 전용 fresh server는 함께 다음 정본을 확인한다.

- 지역 정착지: 6 / 건축 60
- 지역 사회: 48가구 / 144명 / 노동자 96 / 부양가족 48
- 지역 경제: 6시장
- 지역 행정: 6 council / 12 officials / 12 guard posts / 12 alive guards
- 대표 마을 물리 경비대 2명 및 장비/병영
- 국도 보안: 4 waystation / 6 settlement assignment
- 국도: 8 corridor / 4 waystation
- 지역 물류: 8 corridor / 4 waystation
- 왕도 인구: 77가구 / 231명 / 노동자 154
- 왕도 물리 경제: 156 sites / 15 warehouses / 77 wallets
- 외곽 노동권: 18 nodes / 74가구 / 216명 / 노동자 142 / 부양가족 74

같은 fresh world에서 Citadel `royal_archives`도 `connected_cells=17`, 기준 `4` 이상으로 통과했다.

## 검증 결과

검증 기준 head:

`556ab6822a50a98b71098da0f954191f0c44fa38`

Community 전용 GitHub Actions:

- Run: `32573971527`
- Job: `97033473732`
- 결과: SUCCESS
- Java 25 clean build: PASS
- fresh dedicated server: PASS
- Community logical acceptance: PASS
- Community physical acceptance: PASS
- JAR integrity / duplicate entries / class presence / version: PASS

전체 Living Kingdoms PR 검증:

- Run: `32573971533`
- 결과: SUCCESS

## 다음 단계

Phase 1 이후에는 다음을 별도 단계로 확장한다.

- 결혼·출생·사망 이후 가구 재편·이주를 포함한 장기 lifecycle
- 주민 간 지속적인 관계와 친밀도/갈등
- 직업 외 여가 및 계절·행사 기반 공동체 활동
- 마을 단위 사건과 행정·경제·치안의 생활 반응 연결

이 후속 단계에서도 기존 인구·경제·행정의 authority와 물리 월드 정합성을 먼저 유지한다.
