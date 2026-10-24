# Augmentation Survival — Core Acquisition Design

Date: 2026-10-24
Status: CANONICAL INITIAL IMPLEMENTATION BASELINE / NUMBERS PLAYTEST-TUNABLE

이 문서는 증강 코어의 획득처, 등급 분포, 보스 보상, 반복 파밍 방지 원칙을 정의한다.

목표는 특정 한 활동만 반복하는 것이 아니라 Minecraft의 탐험·전투·제작·낚시·보스 콘텐츠가 모두 증강 성장으로 연결되게 만드는 것이다.

정확한 수치는 첫 플레이테스트 후 조정할 수 있으나, **획득 구조와 상대적 우선순위는 구현 기준으로 사용한다.**

## 1. 핵심 경제 원칙

코어 획득의 역할을 다음처럼 나눈다.

- 제작: 일반 코어의 확정 루트
- 일반 탐험: 일반/고급 중심, 낮은 확률의 희귀
- 위험 구조물/이벤트: 고급~영웅
- 고난도 구조물/엘리트: 희귀~전설
- 보스: 상위 코어의 가장 신뢰할 수 있는 직접 루트
- 최고난도 보스/캐시: 전설 중심, 신화 직접 획득 가능
- 3:1 합성: 낮은 등급 코어를 버리지 않고 확정적으로 상향하는 장기 안전장치

한 장소나 몹이 항상 한 등급만 주는 구조는 사용하지 않는다.

## 2. 3:1 합성과 가치 기준

현재 확정 합성:

- 일반 3 -> 고급 1
- 고급 3 -> 희귀 1
- 희귀 3 -> 영웅 1
- 영웅 3 -> 전설 1
- 전설 3 -> 신화 1

따라서 같은 가치 기준으로 보면:

- 일반 = 1
- 고급 = 일반 3
- 희귀 = 일반 9
- 영웅 = 일반 27
- 전설 = 일반 81
- 신화 = 일반 243

이 구조 자체가 운이 나쁜 플레이어의 확정 진척 역할을 한다.

그래서 별도의 숨겨진 pity 게이지는 초기 구현에 넣지 않는다.

## 3. 일반 코어 제작

`GAME_DESIGN.md`의 현재 레시피를 그대로 사용한다.

```text
R A R
A C A
R A R
```

- R = 레드스톤 가루
- A = 자수정 조각
- C = 구리 주괴
- 결과 = 일반 증강 코어 1개

제작은 초반에 증강 시스템을 반드시 체험할 수 있게 하는 확정 루트다.

다만 일반 코어를 대량 자동생산하는 별도 기계는 초기 구현에 넣지 않는다.

## 4. 자연 구조물 전리품

자연 생성 loot container는 기존 전리품을 유지하고 **추가 코어 roll**을 한 번 수행한다.

한 컨테이너에서 코어 추가 roll은 최대 1회다.

플레이어가 설치한 상자, 이미 자연 loot table이 소비된 컨테이너, 복제된 컨테이너에는 새 코어 roll을 만들지 않는다.

### 4.1 routine_exploration

코어 추가 roll 확률: **8%**

성공했을 때 등급:

- 일반 84%
- 고급 15%
- 희귀 1%

대표 대상:

- 폐광
- 난파선
- 폐허 포탈
- 일반적인 소형 탐험 구조물
- Repurposed Structures의 낮은 위험도 구조물
- Dungeons and Taverns의 소형/비전투 중심 구조물

마을의 일반 생활 상자는 기본적으로 코어 주력 공급처로 사용하지 않는다. 너무 안전하고 반복적으로 밀집된 상자는 개별 allowlist에서 제외할 수 있다.

### 4.2 dangerous_mid

코어 추가 roll 확률: **12%**

성공했을 때 등급:

- 고급 70%
- 희귀 25%
- 영웅 5%

대표 대상:

- 사막 피라미드
- 정글 사원
- 약탈자 전초기지
- 네더 요새
- 요새(Stronghold)
- 일반 Trial Chamber 보상/금고 계열
- 중형 던전과 전투형 구조물

### 4.3 dangerous_high

코어 추가 roll 확률: **18%**

성공했을 때 등급:

- 희귀 70%
- 영웅 25%
- 전설 5%

대표 대상:

- 보루 잔해의 위험도가 높은 loot
- 고대 도시
- 우드랜드 맨션의 핵심 보상
- 엔드 시티
- Ominous/고난도 Trial 계열 보상
- Dungeons and Taverns의 대형 전투 던전
- MCEndgame의 고난도 던전 컨테이너
- Chrono Dawn / World Bosses의 보스 전 단계 핵심 캐시

### 4.4 pinnacle_cache

일반 구조물 상자가 아니라 **명시적으로 지정한 최고급 보상 캐시**에만 사용한다.

코어 추가 roll 확률: **35%**

성공했을 때 등급:

- 전설 90%
- 신화 10%

일반적인 여러 칸짜리 던전 상자에 이 프로필을 대량 적용하지 않는다.

Boss raid cache, 최종 보상실, 일회성에 가까운 최고급 캐시처럼 콘텐츠 완료를 대표하는 보상에만 사용한다.

## 5. 단일 핵심 보상 구조물 보정

상자가 하나뿐인 구조물은 다수 상자 던전보다 기대 보상이 지나치게 낮아질 수 있다.

따라서 구조물별 데이터 프로필에 `coreRollMultiplier`를 둘 수 있다.

초기 기준:

- 일반 다중 상자 구조물: x1.0
- 단일 핵심 보상 상자: x1.5~x2.5
- 고밀도 던전: x0.5~x0.8 가능

예를 들어 buried treasure처럼 사실상 한 상자가 콘텐츠 전부인 경우 routine/mid 프로필의 기본 확률을 그대로 쓰지 않고 별도 multiplier를 적용한다.

이 값은 구조물 단위 data profile에서 관리하며 코드에 구조물 ID를 대량 하드코딩하지 않는다.

## 6. 일반 몹 코어 드랍

일반 몹 파밍은 코어의 주력 루트가 아니다. 그러나 전투 중 드물게 코어가 튀어나오는 기대감은 유지한다.

### 6.1 일반 적대몹

플레이어 귀속 처치 시 코어 roll: **0.20%**

성공 시:

- 일반 90%
- 고급 10%

### 6.2 특수 적대몹

특수몹/강화몹 profile의 코어 roll: **1.5%**

성공 시:

- 일반 65%
- 고급 30%
- 희귀 5%

### 6.3 엘리트 / 레이드 특수몹

엘리트 profile의 코어 roll: **4%**

성공 시:

- 고급 65%
- 희귀 30%
- 영웅 5%

### 6.4 일반 몹 드랍 안전 규칙

- 자동 환경사, 낙사 장치, 호퍼 자동화만으로는 core roll을 만들지 않는다.
- 플레이어 귀속 처치만 인정한다.
- `분신`, `지배` 등 COMPANION 처치는 일반 코어 roll을 만들지 않는다.
- 플레이어가 만든 파생 피해로 죽은 적은 플레이어 귀속 처치로 인정할 수 있다.
- Looting은 코어 roll 확률/수량에 적용하지 않는다.
- `풍요`, `전리품 챙기기` 등 일반 드롭 증가 효과는 코어를 복제하지 않는다.
- 한 개체의 한 번의 사망 처리에서 코어 roll은 최대 1회다.

## 7. 이벤트 보상

일반 몹과 보스 사이에 있는 콘텐츠 완료 보상을 따로 둔다.

### Raid Victory

레이드 승리 참여 플레이어에게 개인 보상 코어 **1개 보장**.

등급:

- 고급 65%
- 희귀 30%
- 영웅 5%

### Elder Guardian

직접 처치 보상 코어 **1개 보장**.

등급:

- 고급 50%
- 희귀 45%
- 영웅 5%

### Warden

플레이어 귀속 처치 시 코어 **1개 보장**.

등급:

- 희귀 65%
- 영웅 30%
- 전설 5%

Warden은 보스 UI 여부와 무관하게 고난도 특수 적으로 별도 profile을 사용한다.

## 8. 낚시

낚시는 전투를 하지 않는 플레이어도 낮은 등급 코어 진척을 얻을 수 있는 보조 루트다.

**보물 범주 낚시에 성공했을 때** 기존 보상과 별개로 코어 추가 roll **12%**.

성공 시:

- 일반 70%
- 고급 28%
- 희귀 2%

`보물 낚시`처럼 보물 판정을 높이는 증강은 자연스럽게 이 루트를 강화한다.

코어 자체는 `풍요`로 두 배가 되지 않는다.

## 9. 보스 코어 보상

실제 보스 처치는 구조물 상자보다 확실하게 좋은 성장 루트여야 한다.

보스 원래 보상은 유지하고, 증강 코어는 서버 권한의 개인 추가 보상으로 지급하는 방향을 기본으로 한다.

### 9.1 boss_standard

처치 시 코어 **1개 보장**.

- 희귀 75%
- 영웅 25%

대상:

- 미니보스보다 명확히 강하지만 최종급은 아닌 정식 보스
- 외부 모드의 기본 보스 profile

### 9.2 boss_major

처치 시 코어 **1개 보장**.

- 영웅 80%
- 전설 20%

대표 대상:

- Wither
- Chrono Dawn 주요 보스의 초기 기본값
- World Bosses의 주요 보스 기본값
- MCEndgame의 반복형 던전 보스 중 상위급

외부 보스는 실제 난이도 측정 후 standard/major/pinnacle 사이에서 개별 재분류한다.

### 9.3 boss_pinnacle

처치 시 코어 **1개 보장**.

첫 처치 기본 분포:

- 전설 90%
- 신화 10%

대표 대상:

- Ender Dragon 첫 처치
- 외부 모드의 실제 최종급/pinnacle boss
- MCEndgame Beastweaver처럼 최종급으로 검증된 보스

### 9.4 첫 처치 보너스

플레이어별 보스 type 최초 처치에는 **같은 reward band의 코어 1개를 추가 지급**한다.

즉 첫 처치는 기본적으로 2개, 이후 반복 처치는 1개다.

목적:

- 같은 보스만 반복하는 것보다 여러 보스를 찾아다니는 것을 장려
- 공식 모드팩의 여러 보스 콘텐츠를 실제 증강 성장 루프에 연결
- 첫 도전의 보상 체감 강화

### 9.5 반복 가능한 pinnacle 보스

반복 소환/반복 던전이 쉬운 pinnacle boss는 첫 처치 이후 직접 신화 확률을 낮출 수 있다.

초기 반복 분포:

- 전설 95%
- 신화 5%

특히 Ender Dragon 반복 처치는 첫 처치 이후 `boss_major` 또는 이 반복 pinnacle 프로필 중 실제 플레이테스트에 맞는 쪽을 사용한다. 초기 구현 기본값은 **반복 pinnacle 95/5**로 둔다.

## 10. 외부 모드별 기본 연결

### Terralith

바이옴 자체에서 코어를 무작위로 떨어뜨리지 않는다.

해당 바이옴에 배치되는 구조물/몹/탐험 콘텐츠의 profile을 통해 코어를 얻는다.

### Repurposed Structures

가능하면 원본 vanilla 구조물의 위험도 profile을 상속한다.

예:

- mineshaft 계열 -> routine
- fortress/stronghold 계열 -> dangerous_mid
- mansion/bastion/ancient-city급 계열 -> dangerous_high

개별 예외는 data profile로 수정한다.

### Dungeons and Taverns

구조물 밀도와 상자 수가 많으므로 크기/위험도 기준으로 profile을 나눈다.

- 소형 -> routine
- 중형 전투 -> dangerous_mid
- 대형/심층 -> dangerous_high

상자가 특히 많은 구조물은 `coreRollMultiplier < 1`로 보정한다.

### Friends&Foes

일반 추가 몹은 normal/special mob profile을 사용한다.

명확한 특수 전투 개체만 special 또는 elite로 승격한다.

### Illager Invasion

일반 일리저는 normal/special, 강한 raid unit은 elite profile을 사용한다.

raid victory 보상은 Minecraft 공통 raid completion 보상을 따른다.

### Naturalist

평범한 야생동물은 코어 드랍 대상이 아니다.

코어 파밍을 위해 야생동물을 대량 학살하는 플레이를 유도하지 않는다.

### Farmer's Delight Refabricated

일반 농사/요리 생산에서 코어를 직접 생성하지 않는다.

비전투 증강 활용처를 늘리는 역할에 집중한다.

### Chrono Dawn

6개 작성 보스는 기본적으로 boss_major에서 시작한다.

실제 난이도/진행 단계 확인 후 일부를 boss_standard 또는 boss_pinnacle로 조정한다.

시간 차원의 핵심 캐시는 dangerous_high 또는 명시적 pinnacle_cache로 연결할 수 있다.

### World Bosses

주요 보스는 기본 boss_major.

실제 최종급으로 확인된 보스만 boss_pinnacle로 승격한다.

raid cache는 dangerous_high, 최고급 완료 캐시만 pinnacle_cache 사용.

### MCEndgame

- 일반 고난도 던전 상자: dangerous_high
- 반복 던전 보스: boss_major 기본
- 최종급 Beastweaver: boss_pinnacle 후보

반복 보스에서 신화가 지나치게 빠르게 누적되면 직접 신화 확률을 낮추되 전설 보상 자체를 제거하지 않는다.

## 11. 코어 획득에 영향을 주지 않는 증강

별도 명시가 없는 한 다음은 코어에 적용하지 않는다.

- 풍요
- 전리품 챙기기
- Looting
- Fortune
- 자동 채굴/수확 추가 블록
- 동료/분신 처치 보상 귀속

코어는 일반 아이템 드롭 파이프라인과 별도의 progression reward로 취급한다.

## 12. 플레이 진행 목표

정확한 시간은 플레이 스타일과 월드 seed에 따라 달라지므로 강제 타이머로 쓰지 않는다.

첫 플레이테스트 목표 체감:

- 일반: 첫 10~30분 안에 최소 1개 접근 가능
- 고급: 첫 30~60분 안에 현실적으로 접근 가능
- 희귀: 약 1~2시간대부터 탐험/전투로 직접 노릴 수 있음
- 영웅: 약 2~5시간대부터 위험 콘텐츠/보스로 현실적으로 등장
- 전설: 약 5~10시간대부터 고난도 구조물/주요 보스로 노릴 수 있음
- 신화: 약 8~15시간대부터 최고급 보스 또는 전설 3:1 합성으로 첫 획득 가능

이는 `첫 획득 가능 시점`이지 전체 로스터 완성 시간 목표가 아니다.

신화는 엔더 드래곤 처치 이후에만 허용하지 않는다. 공식 팩의 다른 pinnacle 콘텐츠를 먼저 공략하면 드래곤 이전에도 얻을 수 있어야 한다.

## 13. 플레이테스트에서 볼 지표

초기 수치의 성공 여부는 다음으로 판단한다.

- 시간당 획득 코어 수 / 등급
- 획득 경로별 비율: 제작 / 상자 / 일반몹 / 이벤트 / 낚시 / 보스 / 합성
- 첫 고급/희귀/영웅/전설/신화 도달 시간
- 상자 수가 많은 외부 던전의 코어 과잉 여부
- 몹팜이 탐험보다 효율적인지 여부
- 동일 보스 반복이 다양한 보스 탐험을 압도하는지 여부
- 3:1 합성으로 소모되는 저등급 코어 비율
- 146종 후보 풀에서 원하는 빌드가 지나치게 늦게 완성되는지 여부

## 14. 구현 데이터 구조

소스별 값은 data-driven으로 둔다.

권장 필드:

- source id / selector
- source kind
- distribution profile
- coreRollChance
- coreRollMultiplier
- tier weights
- maxCoreRolls
- requirePlayerAttributedKill
- firstKillBonus
- repeatDistribution
- perPlayerReward
- exclusions

구현 시 `core_distribution_contract.json`과 `boss_core_reward_contract.json`을 이 문서의 수치로 갱신한다.

## 15. 현재 상태

- ACQUISITION LOOP DESIGNED: YES
- EXACT INITIAL WEIGHTS DEFINED: YES
- RUNTIME IMPLEMENTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
- FINAL BALANCE LOCKED: NO

Docs/design only. Build/CI not run.
