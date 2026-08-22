# 에르덴 Gameplay Rules Lock R1

이 문서는 《Living Kingdoms》의 에르덴 왕국을 실제 게임으로 완성하기 위해 alpha.12 이후 유지할 플레이 규칙 정본이다.

목표는 바닐라 기능을 무조건 삭제하는 것이 아니라, 바닐라의 빠른 제작·시간 스킵·부활·무작위 몬스터 생성이 왕국의 경제·거리·치안·생활 시뮬레이션을 무의미하게 만드는 우회로가 되지 않게 하는 것이다.

## 1. 현재 플레이 가능 출신

현재 실제 플레이 가능 origin은 정확히 하나다.

- 종족: 인간 (`human`)
- 국가: 에르덴 왕국 (`erden_kingdom`)
- 배경: 평범한 주민 (`common_resident`)
- 시작 거주지: 왕도 시민구 임대방 (`erden_city_room`)

`FoundationCatalog`, `PlayableOriginCatalog`, 서버 제출 검증, 시작 장비와 플레이어 배치가 모두 이 조합만 허용한다.

실바나·카르둠, 엘프·드워프, 낚시꾼·방랑자·학자 시작은 아이디어나 미래 확장 대상일 수 있으나 현재 플레이 가능 콘텐츠가 아니다. 에르덴 완성 전에는 다시 노출하지 않는다.

서버 시작 시 이 계약이 깨지면 fail-fast한다.

## 2. 시작 UI

선택지가 하나뿐인 현재 단계에서는 가짜 다중 선택 UI를 만들지 않는다.

- 시민등록 화면은 현재 정본 종족/국가/신분/거주지를 고정 정보로 보여준다.
- 플레이어는 `에르덴에서 삶을 시작하기` 한 번으로 확정한다.
- ESC로 등록을 우회할 수 없다.
- 다른 종족과 왕국은 해당 지역이 실제 완성된 뒤에만 다시 선택지로 추가한다.

## 3. 제작과 생산

에르덴의 경제에서 플레이어가 손바닥 조합과 바닐라 설비만으로 전문 생산망을 우회하지 못한다.

현재 서버 권위 규칙:

- 개인 2×2 crafting 결과 차단
- crafting table 차단
- furnace / blast furnace / smoker 차단
- smithing table / anvil / grindstone 차단
- stonecutter 차단
- brewing stand / enchanting table 차단
- loom / cartography table / crafter 차단
- 다른 모드나 명령으로 바닐라 생산 메뉴가 열려도 서버가 닫는다.

생산은 향후 길드, 공방, 장인, 계약, 사업체와 연결한다.

야생 채집 자체를 전부 금지하지는 않는다. 대신 채집한 바닐라 아이템이 곧바로 왕국 은화나 공식 생산량으로 변환되는 무료 우회로가 되어서는 안 된다.

관리되는 이름 있는 시민 NPC는 우클릭 시 Living Kingdoms 상호작용을 사용하며 바닐라 주민 거래창을 열지 않는다.

## 4. 시간과 수면

에르덴의 시간은 서버 전체가 공유한다.

침대 하나를 사용한 플레이어가 밤을 통째로 삭제하면 다음 시스템이 동시에 왜곡된다.

- 주민 출퇴근과 가족 생활
- 시장 영업시간
- 경비 교대
- 지역 공동체 일정
- 생산과 임금
- 운송과 도로 이동
- 범죄와 야간 위험

따라서 일반 플레이어는 Living Realm에서 침대로 시간을 건너뛰지 못한다.

수면은 현재 시간 스킵 기능이 아니다. 향후 여관·주택·치료·피로 시스템을 추가할 때도 세계 시계 자체는 멀티플레이 공유 권위를 유지한다.

## 5. 전투불능과 회복

에르덴에서는 일반 플레이어의 치명 피해를 바닐라 death screen + 아이템 폭발로 처리하지 않는다.

현재 규칙:

- 치명 피해 시 death 취소
- 소지품 보존
- 등록 거주지로 구조·후송
- 최대 체력의 30%로 회복
- 약화 45초
- 둔화 20초
- 치료·구조비 최대 은화 8
- 보유 은화가 8보다 적으면 가진 금액까지만 지불
- 전 세계 시간을 강제로 앞으로 넘기지 않음

멀티플레이에서 한 명의 패배 때문에 다른 플레이어와 모든 NPC의 시간이 순간 이동하면 안 된다. 회복 시간 비용은 개인 상태 이상과 이동 손실로 표현하고, 공유 세계는 정상 속도로 계속 진행한다.

현재 물리 병원/사원 회복 거점이 완성 정본이 아니므로 안전하게 검증된 등록 거주지를 후송 목적지로 사용한다. 병원·사원 생활 기능이 완성되면 이 목적지는 제도 시설로 교체한다.

## 6. 부활 지점

일반 침대 또는 명령으로 개인 무료 체크포인트를 만드는 것은 제도적 회복 규칙과 충돌한다.

- Living Realm의 등록 플레이어는 vanilla respawn point 변경을 사용할 수 없다.
- 최초 입국은 왕국 건설 완료 뒤 검증된 거주지로 한 번 배치한다.
- 전투불능 후송은 별도 제도적 예외다.

## 7. 이동과 거리

1블록=1m 국가에서 무료 순간이동을 일반 교통으로 사용하지 않는다.

허용되는 예외:

- 최초 입국 배치
- 전투불능 구조·후송
- 개발/진단용 creative·spectator 작업

일반 여행은 도로, 역참, 마차, 수운, 숙박과 비용/시간을 사용해야 한다. 향후 빠른 이동 UI를 만들더라도 실제 교통망의 출발지·도착지·요금·소요시간·운행 상태를 authority로 사용한다.

## 8. 치안과 위험 구역

### 왕도

- 실제 성벽: X -1200~1200, Z -900~900
- 안전 buffer: 32m
- 자연/청크 생성/순찰 계열 적대 몹의 ambient spawn 차단
- 일반 플레이어의 공공 건축·기반시설 파괴 차단

### 지역 정착지

현재 6개 정착지와 각 `SETTLEMENT_RADIUS=220` 생활권을 시민 안전권으로 취급한다.

- harvest_crossing
- silvermead
- sunfield
- pinewatch
- blackstone
- ironvale

이 범위에서도 ambient hostile spawn과 무단 공공 구조 파괴를 차단한다.

### 국도

국도는 안전권이 아니다.

- 실제 경비·역참이 위험을 줄인다.
- 야간 습격이나 야생 생물의 가능성은 남긴다.
- 따라서 성벽 밖 이동, 경비, 마차와 숙박의 의미가 유지된다.

### 야생

야생은 완전한 시민 안전권이 아니다. 에르덴 전용 생태와 위험이 존재한다.

스크립트·명령·스포너 기반의 의도된 사건/전투는 시민권 ambient spawn 필터가 자동 삭제하지 않는다.

## 9. 멀티플레이 권위

공유 상태:

- Living Realm 월드와 시간
- NPC와 주민 사회
- 시장·생산·재고
- 왕국/지역 물류
- 행정·치안·법
- 공공 구조물

플레이어별 상태:

- origin profile
- 개인 은화/명망/직업 기록
- 숙련과 개인 진행
- 범죄 기록 중 개인 귀속 상태
- 향후 개인 소유 재산·계약

서버가 origin 및 경제 상태의 authority다. 클라이언트가 임의로 다른 국가나 종족을 제출해도 거부한다.

## 10. 바닐라 우회 정책

### 즉시 차단

- 개인 crafting/생산 설비
- 침대 night skip
- 개인 respawn checkpoint
- 시민권 ambient hostile spawn
- 관리 NPC vanilla trade
- 시민권 공공 구조물 파괴

### 허용하되 왕국 경제와 분리

- 야생 채집
- 탐험 중 획득한 일반 아이템
- 성외에서의 전투와 드랍

이 아이템들은 별도 합법 거래·길드·매입·세금 시스템이 연결되기 전에는 공식 은화/생산량을 자동 생성하지 않는다.

### 후속 구현 필요

- 플레이어 소유 토지/건물에서의 합법 건축 예외
- 자동 생산시설 허가/사업체 시스템
- 병원·사원 물리 회복
- 여관·주택 휴식/피로
- 실제 교통망 기반 빠른 이동 UI
- 밀수/장물/세금과 야생 전리품의 합법 시장 연결

## 11. 서버 진단 계약

서버 시작 시 다음 marker를 남긴다.

```text
LK_ERDEN_GAMEPLAY_RULES_LOCK revision=1 erden_only=true playable_species=1 playable_homelands=1 playable_backgrounds=1 playable_residences=1 sleep_skip=false bed_respawn=false shared_clock=true institutional_defeat=true recovery_fee=8 item_loss=false personal_crafting=false vanilla_workstations=false capital_safe=true settlement_safe=true settlements=6 road_danger=true wild_danger=true scripted_spawns_preserved=true initial_placement_teleport_only=true multiplayer_world_shared=true
```

이 marker는 단순 문자열만 찍는 용도가 아니다. origin catalog 수와 현재 6개 settlement catalog 수를 먼저 검사하고 drift가 있으면 서버 시작을 실패시킨다.
