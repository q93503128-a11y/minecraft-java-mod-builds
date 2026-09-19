# TURNBOUND Balance / Economy Rules v1

> 전투 수치, 행동 경제, 성장, 장비, 재화, 소환의 정본.
> 외부 턴제 RPG의 action economy / pity / 저희귀도 활용 원리를 참고하되 TURNBOUND의 실제 전투 길이와 비과금 구조에 맞춰 변형한다.
> simulator/playtest 전 값은 “초기 production target”이다.

## 1. 전투 전역

| 항목 | v1 |
|---|---:|
| 아군 최대 | 4 |
| 일반 적 권장 | 1~4 |
| 일반 적 상한 | 5 |
| Turn Gauge | 1000 |
| 선택 중 논리시간 | 정지 |
| Basic CD | 0 |
| 공통 MP/AP | 없음 |
| 전투 중 파티 교체 | 기본 불가 |
| 플레이어블 희귀도 | ★3~★5 |
| 레벨 상한 | 60 |
| 전투 속도 | 1x / 2x presentation only |

## 2. TurnScheduler

### 2.1 의미
SPD는 선공만이 아니라 **단위 시간당 행동 횟수**를 결정한다.

- Gauge >= 1000 → 행동 가능
- 행동 후 1000 차감
- overflow 보존
- 자연 SPD 연속 행동 허용
- 플레이어 선택 중 Gauge 정지

### 2.2 fixed-point
- `GAUGE_SCALE = 1,000,000`
- 내부 gauge = 표시 Gauge × scale
- runtime / HUD preview / AUTO가 동일 `TurnScheduler` 사용

정수 pulse 근사 공식은 정본이 아니다.

## 3. SPD

초기 캐릭터 범위:
- 매우 느림: 80~86
- 느림: 87~94
- 표준: 95~103
- 빠름: 104~110
- 매우 빠름: 111~116

117 이상은 특별한 설계 이유 없이 사용하지 않는다.

SPD는 action economy이므로 ATK +10%와 동일한 값으로 평가하지 않는다.

장비 SPD:
- flat 소량
- Accessory 중심
- 높은 random roll 없음
- 한 캐릭터가 장비만으로 역할을 바꿀 정도의 SPD 폭증 금지

## 4. Gauge 조작 budget

초기 범위:
- 소형 단일 advance: +100~160
- 큰 단일 advance: +250~360
- 소형 단일 delay: -80~-140
- 공격+강한 단일 delay: 약 -160~-200
- 광역 delay: 단일보다 낮게
- 즉시 Ready: 특별한 mechanic 외 기본적으로 사용하지 않음

루메아 기준:
- Basic +120, 느린 아군이면 총 +160
- Time Leap +300, 느린 아군이면 총 +360
- 단일 공격 + delay -180
- 정확한 미래 순서 편집 각성 보상: 자기 Gauge +60

## 5. 캐릭터 power budget

다섯 축:
1. 직접 피해
2. 생존 기여
3. 행동 경제
4. 제어/디버프
5. 파티 시너지

한 캐릭터가 모든 축 상위권이면 안 된다.

평가:
- 10 regular actions
- 20 regular actions
- 실전 follow-up 기대 횟수
- 생성/삭제한 Gauge 가치
- 회복/방어량
- setup에 필요한 own turn
- single / multi / boss / short / long
- Auto 손실

## 6. Skill potency

### Basic
- 일반 단일: ATK 85~105%
- utility Basic: raw damage를 낮추고 실제 utility를 제공

### CD2 공격
- 대체로 150~200%
- setup 조건이 있으면 상단 초과 가능

### CD3~4
- 강한 AoE
- premium Gauge
- revive
- team protection
- signature loop payoff

단순 “Basic보다 숫자가 큼”만으로 CD3~4를 만들지 않는다.

### Reaction
Reaction은 무료 행동이므로:
- raw potency를 낮게 시작
- trigger frequency 포함 기대값으로 평가
- Reaction→Reaction 무한 재귀 금지
- per-action trigger cap 필요

## 7. 초기 P01~P08 전투 역할 baseline

| ID | 역할 | Rarity | SPD target | 핵심 자원 |
|---|---|---:|---:|---|
| P01 카이렌 | 단일 결투 DPS | ★4 | 105 | Focus 0~3 |
| P02 루메아 | Tempo Support | ★5 | 114 | Turn Order |
| P03 브람 | Redirect/Counter Tank | ★4 | 84 | Guard 0~100 |
| P04 엘리시아 | Rescue Healer | ★4 | 96 | Sanctuary |
| P05 리네트 | Follow-up DPS | ★4 | 108 | Sightline / Shot |
| P06 모르웬 | Event/Execute | ★5 | 98 | Records 0~5 |
| P07 마리온 | Partner Summoner | ★4 | 100 | Bond 0~100 |
| P08 라제 | Risk/Fury DPS | ★3 | 103 | Fury 0~100 |

희귀도는 정체성/복잡도/ceiling 차이이지 단순 stat tier가 아니다.

## 8. 전투 길이

초기 목표:
- 약한 필드전: 파티 regular action 3~6
- 표준 필드전: 6~12
- Elite: 10~18
- Boss: phase 포함 18~30

HP sponge로 난이도를 만들지 않는다.

Boss는:
- 행동 패턴
- 타깃 우선순위
- Gauge/position pressure
- phase 변화
로 난이도를 만든다.

## 9. 레벨 성장

Level 1~60.

원칙:
- SPD는 레벨로 증가하지 않음
- 레벨이 장비/kit를 압도하지 않음
- 레벨 부족이 즉사/무딜만 만드는 hard wall이 되지 않음

초기 target:
- Lv1→60 HP/ATK/DEF 총 성장: 약 2.1~2.5배 범위
- 실제 곡선은 초반 빠르고 후반 완만
- 지역 권장 레벨 차 5~8 정도는 조합/장비로 극복 가능
- 15 이상 차이는 고난도 선택 영역

정확한 curve는 simulator에서 확정한다.

## 10. 성장 축

### 10.1 Level
XP로만 증가.
Gold 요구 없음.

### 10.2 Equipment
3 slots:
- Weapon
- Armor
- Accessory

### 10.3 Signature
캐릭터 개인 특화 슬롯.
없어도 캐릭터 핵심 기능은 완성되어 있어야 한다.

획득:
- 개인 퀘스트
- 특정 boss
- 깊은 탐험
중 하나.

### 10.4 Awakening
캐릭터당 1회.
단순 stat +20%가 아니라 mechanic 확장.

조건:
- 캐릭터 개인 사건 완료
- 일정 레벨
- Gold
- 필요하면 고정 quest item 1개

전역 “Awakening Core” 화폐는 v1 장기 정본에서 폐기한다.

## 11. 장비

### 11.1 등급
초기 4단계:
- 일반
- 희귀
- 영웅
- 유물

등급 수를 더 늘리지 않는다.

### 11.2 구성
장비마다:
- main stat 1개
- trait 최대 1개
- 강화 +0~+10

random substat 4줄 구조는 사용하지 않는다.

### 11.3 강화
- Gold만 소모
- 실패 없음
- 파괴 없음
- 강화 이전 가능성을 우선 검토
- +10까지 단순한 main stat 증가

초기 main stat 강화:
- +1마다 base main stat의 약 4%
- +10 총 약 +40%

trait는 강화로 반복 상승하지 않거나 특정 milestone에서 1회만 강화한다.

### 11.4 장비 Gold cost baseline
아이템 등급별 +0→+10 총비용 초기 target:
- 일반: 4,000
- 희귀: 9,000
- 영웅: 18,000
- 유물: 35,000

정확값은 지역 수입량과 같이 시뮬레이션한다.

## 12. 재화

전역 core currency는 3개.

### Gold
용도:
- 장비 강화
- 상점 구매
- Awakening 비용 일부

획득:
- 전투
- 퀘스트
- 상자
- 던전
- boss

### Summon Crystal
용도:
- 캐릭터 소환만

획득:
- 메인/사이드 퀘스트
- 첫 클리어
- 발견 milestone
- boss
- 큰 탐험 보상

### Star Essence
용도:
- 중복 캐릭터 환원
- 영구 선택권/수집 교환

핵심 캐릭터 기능 unlock 비용으로 사용하지 않는다.

## 13. Gold economy baseline

초기 지역 수입 목표:
- 일반 필드전: 80~160
- Elite: 350~700
- 지역 quest: 500~1,500
- dungeon first clear: 1,500~3,000
- boss first clear: 3,000~6,000

초반 30~60분 안에:
- 희귀 장비 1~2개 구매/강화
- Gold 부족 때문에 진행이 막히지 않음
을 목표로 한다.

후반 Gold sink는:
- 고등급 장비 강화
- 상점 구매
- Awakening
정도로 충분하게 만든다.

## 14. 상점

상점은 무작위 새로고침 노동을 만들지 않는다.

- 거점별 theme inventory
- 진행에 따라 자동 확장
- 일반 consumable 남발 금지
- 장비/utility 중심
- manual refresh currency 없음

가격 초기 target:
- 일반 장비: 800~1,500
- 희귀: 2,500~5,000
- 영웅: 8,000~15,000
- 유물: 상점 일반 판매보다 boss/탐험 중심

## 15. Summon economy

### 15.1 비용
- 1회: 300 Crystal
- 10회: 3,000 Crystal

10회 할인은 넣지 않는다. 계산 단순성을 유지한다.

### 15.2 확률 baseline
- ★5: 2%
- ★4: 15%
- ★3: 83%

10회 내 최소 ★4 1명 보장.

### 15.3 pity
- soft pity: 45회부터
- hard pity: 60회
- pity는 pool 확장/게임 종료/세션 변경으로 사라지지 않음
- 초기 버전에서 50/50, weapon banner, 기간 한정 pity 분리 없음

### 15.4 수급 속도
비과금 게임이므로 상업 gacha보다 관대하게 설계한다.

목표:
- 초반 30~45분 안에 첫 10회
- 정상적인 새 콘텐츠 플레이 시 대략 2~3시간마다 추가 10회
- 메인 스토리에 특정 소환 캐릭터 요구 없음

## 16. 중복 캐릭터

첫 획득:
- 캐릭터 unlock

중복:
- Star Essence로 변환

초기 변환값:
- ★3: 15 Essence
- ★4: 60 Essence
- ★5: 250 Essence

중복으로:
- 스킬 해금
- 별 승급
- 핵심 passive
를 잠그지 않는다.

## 17. Star Essence 교환

영구 교환소.
기간 한정 rotation 없음.

초기 target:
- 150 Essence → 300 Crystal
- 450 Essence → 보유/해금된 ★4 선택권
- 1,200 Essence → 보유/해금된 ★5 선택권

선택권 pool은 스토리상 아직 등장하지 않은 캐릭터를 미리 공개하지 않는다.

## 18. 초기 roster 획득

메인 진행에서 초기 4인 파티를 확보 가능하게 한다.

소환은:
- 파티 다양성
- 다른 archetype
- 수집
을 위한 확장 수단이다.

“가챠에서 healer를 못 뽑아서 진행 불가” 같은 구조 금지.

## 19. 보상

### 일반 필드전
- XP
- Gold
- 낮은 확률의 장비/지역 loot

### Elite
- 높은 Gold/XP
- 장비 보정
- 첫 처치 bonus

### 탐험/발견
- Crystal
- 지도 marker
- shortcut
- 빠른 이동
- NPC/상점 access

### Boss
- 큰 XP/Gold
- 보장 장비
- Crystal
- 진행 access
- Signature/Awakening 연결 가능

## 20. 반복 사냥 제한

반복 필드 사냥이 모든 면에서 최고 효율이면 안 된다.

첫 발견/첫 클리어/퀘스트 보상 비중을 높이고,
반복 전투는:
- Gold
- XP
- 일반 장비
정도에 강하게 한다.

## 21. AUTO 밸런스

AUTO는 편의 기능이지 최적 플레이 bot이 아니다.

목표:
- 표준 필드전 80~90% 효율
- boss mechanic 완벽 대응 안 함
- character identity를 파괴하는 행동은 피함
- revive/heal/tempo 같은 명확한 긴급 조건은 처리

수동 플레이가 더 좋은 판단을 할 수 있어야 한다.

## 22. 보스 Gauge 저항

보스 blanket immunity는 피한다.

초기 rule:
- 일반 boss: Gauge delay 60~75% 효율
- phase 중 특정 state: 30~50%
- 완전 면역은 mechanic상 명확한 이유가 있을 때만

UI에서 저항을 숨기지 않는다.

## 23. 밸런스 완료 조건

캐릭터/경제 수치는 다음을 함께 확인한 뒤 확정한다.

- 캐릭터별 10/20 action 성과
- SPD 변화에 따른 실제 행동 횟수
- reaction 기대 횟수
- healing/guard 가치
- boss turn denial 위험
- Gold 수입/지출
- 장비 강화 속도
- 첫 10-pull 시점
- hard pity 도달 시간
- duplicate Essence 환원 속도
- Auto와 manual 차이

“숫자가 보기 좋아서” 확정하지 않는다.


## 24. Enemy rank baseline

Enemy difficulty는 HP multiplier만으로 만들지 않는다.

### Common
- mechanic 1개 이하
- 보통 1~3 unit composition의 구성원
- 표준 field combat의 중심

### Veteran
- common보다 약 20~35% 높은 effective durability/damage budget
- 추가 target rule/mechanic 1개
- 실루엣 차이 필수

### Elite
초기 budget:
- 동일 레벨 common 2~2.7명 정도의 total pressure
- 혼자 또는 support 1~2명
- 2개 이상 readable pattern
- first-clear reward

### Midboss
- common 3~4명 이상의 threat를 단순 HP로 환산하지 않음
- 2~3 pattern
- phase/threshold 변화 1개 이상
- route 또는 POI의 관문

### Boss
- 최소 2 phase/pattern shift
- telegraph
- action-economy pressure
- 장소와 연결된 mechanic
- add를 쓰더라도 무한 spawn 금지

### World Boss
- 메인 진행에 필수 아님
- 해당 지역 권장 progression보다 높을 수 있음
- 우회 가능
- 발견/도전 자체가 선택
