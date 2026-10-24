# Augmentation Survival — Core Acquisition Design

Date: 2026-10-24  
Status: CANONICAL INITIAL IMPLEMENTATION BASELINE / NUMBERS PLAYTEST-TUNABLE

이 문서는 증강 코어의 획득처, 등급 분포, 보스 보상, 반복 파밍 방지 원칙을 정의한다.

목표는 특정 한 활동만 반복하는 것이 아니라 Minecraft의 탐험·전투·제작·낚시·보스 콘텐츠가 모두 증강 성장으로 연결되게 만드는 것이다.

정확한 수치는 첫 플레이테스트 후 조정할 수 있으나, **획득 구조와 상대적 우선순위는 구현 기준으로 사용한다.**

관련 정본은 역할을 분리한다.

- 확률/등급 분포: `src/main/resources/data/augmentation_survival/augmentation/core_distribution_contract.json`
- 보스 reward band/첫 처치/반복 정책: `src/main/resources/data/augmentation_survival/augmentation/boss_core_reward_contract.json`
- 실제 구조물·몹·이벤트·보스 출처 매핑: `src/main/resources/data/augmentation_survival/augmentation/core_source_profiles.json`
- 출처 매핑 설명/감사 기록: `CORE_SOURCE_PROFILE_MATRIX_2026-10-24.md`

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

일반 환산 가치:

- 일반 = 1
- 고급 = 3
- 희귀 = 9
- 영웅 = 27
- 전설 = 81
- 신화 = 243

이 구조 자체가 운이 나쁜 플레이어의 확정 진척 역할을 하므로 별도 숨겨진 pity 게이지는 초기 구현에 넣지 않는다.

## 3. 일반 코어 제작

`GAME_DESIGN.md`의 현재 레시피를 사용한다.

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

일반 코어를 대량 자동생산하는 별도 기계는 초기 구현에 넣지 않는다.

## 4. 자연 구조물 전리품

자연 생성 loot container는 기존 전리품을 유지하고 추가 core roll을 수행한다.

- 한 컨테이너에서 추가 core roll 최대 1회
- 플레이어 설치 상자 제외
- 이미 자연 loot가 소비된 컨테이너 재시드 금지
- 복제 컨테이너에서 새 natural core roll 생성 금지

### 4.1 `routine_exploration`

추가 core roll: **8%**

성공 시:

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

마을의 일반 생활 상자는 안전하고 밀집되어 있으므로 기본 코어 공급처에서 제외한다.

### 4.2 `dangerous_mid`

추가 core roll: **12%**

성공 시:

- 고급 70%
- 희귀 25%
- 영웅 5%

대표 대상:

- 사막 피라미드
- 정글 사원
- 약탈자 전초기지
- 네더 요새
- Stronghold
- 일반 Trial Chamber 보상/금고 계열
- 중형 던전과 전투형 구조물

### 4.3 `dangerous_high`

추가 core roll: **18%**

성공 시:

- 희귀 70%
- 영웅 25%
- 전설 5%

대표 대상:

- 보루 잔해의 위험도가 높은 loot
- 고대 도시
- 우드랜드 맨션 핵심 보상
- 엔드 시티
- Ominous/고난도 Trial 계열 보상
- Dungeons and Taverns의 대형 전투 던전
- MCEndgame의 고난도 던전 컨테이너
- Chrono Dawn / World Bosses의 보스 전 단계 핵심 캐시

### 4.4 `pinnacle_cache`

일반 구조물 상자가 아니라 **명시적으로 지정한 최고급 완료 보상 캐시**에만 사용한다.

추가 core roll: **35%**

성공 시:

- 전설 90%
- 신화 10%

일반적인 다중 상자 던전에 대량 적용하지 않는다.

## 5. 구조물 밀도 보정

구조물별 `coreRollMultiplier`를 둘 수 있다.

초기 기준:

- 일반 다중 상자 구조물: x1.0
- 단일 핵심 보상 상자: x1.5~x2.5
- 고밀도 던전: x0.5~x0.8 가능

이 multiplier는 **아이템 수량이 아니라 추가 core roll 확률**에 적용한다.

예를 들어 buried treasure처럼 사실상 한 상자가 콘텐츠 전부면 상향하고, 고대 도시나 대형 외부 던전처럼 컨테이너가 많은 곳은 하향한다.

구체적인 source별 보정은 `core_source_profiles.json`에서 관리한다.

## 6. 일반 몹 코어 드랍

일반 몹 파밍은 코어의 주력 루트가 아니다. 전투 중 드물게 코어가 나오는 기대감만 유지한다.

### 6.1 `normal_hostile`

플레이어 귀속 처치 시 core roll: **0.20%**

- 일반 90%
- 고급 10%

### 6.2 `special_hostile`

플레이어 귀속 처치 시 core roll: **1.5%**

- 일반 65%
- 고급 30%
- 희귀 5%

### 6.3 `elite_hostile`

플레이어 귀속 처치 시 core roll: **4%**

- 고급 65%
- 희귀 30%
- 영웅 5%

### 6.4 몹 드랍 안전 규칙

- 자동 환경사/낙사 장치/호퍼 자동화만으로는 core roll을 만들지 않는다.
- 플레이어 귀속 처치만 인정한다.
- `분신`, `지배` 등 COMPANION 단독 처치는 일반 코어 roll을 만들지 않는다.
- 플레이어가 만든 적법한 파생 피해의 kill attribution은 인정할 수 있다.
- Looting은 코어 확률/수량에 적용하지 않는다.
- `풍요`, `전리품 챙기기` 등 일반 드롭 증가 효과는 코어를 복제하지 않는다.
- 한 개체의 한 번의 사망 처리에서 일반 core roll은 최대 1회다.
- 별도 이벤트/보스 보상을 받는 개체는 같은 사망에서 일반 core roll을 중복하지 않는다.

## 7. 이벤트 보상

### Raid Victory

참여 플레이어 개인 보상 코어 **1개 보장**.

- 고급 65%
- 희귀 30%
- 영웅 5%

### Elder Guardian

직접 처치 보상 코어 **1개 보장**.

- 고급 50%
- 희귀 45%
- 영웅 5%

### Warden

플레이어 귀속 처치 시 코어 **1개 보장**.

- 희귀 65%
- 영웅 30%
- 전설 5%

Warden은 보스 UI 여부와 무관하게 고난도 특수 적 이벤트 profile을 사용한다.

## 8. 낚시

보물 범주 낚시에 성공했을 때 기존 보상과 별개로 추가 core roll **12%**.

성공 시:

- 일반 70%
- 고급 28%
- 희귀 2%

`보물 낚시`처럼 보물 판정을 높이는 증강은 자연스럽게 이 루트를 강화한다.

코어 자체는 `풍요`로 두 배가 되지 않는다.

## 9. 보스 코어 보상

보스 원래 보상은 유지하고, 증강 코어는 서버 권한의 개인 추가 보상으로 지급한다.

### 9.1 `boss_standard`

처치 시 코어 **1개 보장**.

- 희귀 75%
- 영웅 25%

정식 보스이지만 최종급은 아닌 encounter에 사용한다.

### 9.2 `boss_major`

처치 시 코어 **1개 보장**.

- 영웅 80%
- 전설 20%

초기 대표 대상:

- Wither
- Chrono Dawn 인식 보스 기본값
- World Bosses 인식 보스 기본값
- MCEndgame 반복 던전 보스 기본값

### 9.3 `boss_pinnacle`

처치 시 코어 **1개 보장**.

첫 처치 기본 분포:

- 전설 90%
- 신화 10%

대표 대상:

- Ender Dragon
- 실제 최종급 외부 보스
- MCEndgame Beastweaver는 exact ID 검증 후 승격할 pinnacle 후보

### 9.4 첫 처치 보너스

플레이어별 보스 type 최초 처치에는 **같은 reward band의 코어 1개 추가**.

즉 최초 처치는 기본적으로 2개, 이후 반복 처치는 1개다.

목적:

- 한 보스 반복보다 여러 보스 탐험 장려
- 공식 모드팩의 다양한 보스를 증강 성장에 연결
- 첫 도전 보상 체감 강화

### 9.5 반복 가능한 pinnacle

반복 pinnacle의 초기 분포:

- 전설 95%
- 신화 5%

Ender Dragon 반복 처치의 초기 기본값도 이 반복 pinnacle 분포다.

반복 가능한 외부 pinnacle은 실제 파밍 속도에 따라 직접 신화 확률을 더 낮출 수 있지만 전설 보상 자체는 제거하지 않는다.

## 10. 외부 모드 연결

### Terralith

바이옴 자체에서 코어를 무작위 생성하지 않는다. 해당 바이옴의 구조물/몹/탐험 콘텐츠를 통해 얻는다.

### Repurposed Structures

가능하면 대응 vanilla 구조물 위험도를 상속한다.

- mineshaft/shipwreck 계열 -> routine
- fortress/stronghold/pyramid/outpost 계열 -> dangerous_mid
- mansion/bastion/ancient-city/end-city급 계열 -> dangerous_high
- village 계열 -> 제외

개별 variant를 대량 exact 하드코딩하지 않고 path family + namespace fallback을 사용한다.

### Dungeons and Taverns

구조물/상자 밀도가 높으므로 크기와 위험도에 따라 routine/mid/high로 나누며 기본 multiplier를 낮춘다.

정확한 family 보정은 `core_source_profiles.json`에 둔다.

### Friends&Foes

확인된 특수 전투 개체를 special/elite로 승격한다.

이번 source audit에서 exact registry ID가 확인된 대상:

- iceologer -> elite
- illusioner -> elite
- mauler -> special
- wildfire -> elite

비적대 Friends&Foes 개체는 코어 드랍 대상이 아니다.

### Illager Invasion

26.2.x namespace의 적대 개체는 기본 special로 시작한다.

개별 exact registry ID를 추측하지 않으며, 실제 ID와 난이도가 검증된 강한 raid unit만 elite override로 승격한다.

raid victory는 Minecraft 공통 event profile을 따른다.

### Naturalist

평범한 야생동물은 코어 드랍 대상이 아니다.

### Farmer's Delight Refabricated

일반 농사/요리 생산에서 progression core를 직접 생성하지 않는다.

### Chrono Dawn

recognized boss encounter는 기본 `boss_major`.

실제 난이도/진행 단계 확인 후 일부를 standard 또는 pinnacle로 조정한다.

### World Bosses

recognized boss encounter는 기본 `boss_major`.

실제 최종급만 pinnacle로 승격한다. raid cache는 구조물/cache profile을 사용한다.

### MCEndgame

- 일반 고난도 던전 상자 -> dangerous_high
- recognized 반복 던전 보스 -> boss_major 기본
- Beastweaver -> boss_pinnacle 후보

Beastweaver exact registry ID는 이번 audit에서 검증되지 않았으므로 `core_source_profiles.json`에 비활성 pending override로만 기록한다.

## 11. 코어 획득에 영향을 주지 않는 효과

별도 명시가 없는 한 다음은 코어에 적용하지 않는다.

- 풍요
- 전리품 챙기기
- Looting
- Fortune
- 자동 채굴/수확 추가 블록
- 동료/분신 처치 보상 귀속

코어는 일반 아이템 drop pipeline과 별도의 progression reward다.

## 12. 플레이 진행 목표

강제 타이머가 아니라 첫 플레이테스트 목표 체감이다.

- 일반: 첫 10~30분 안에 최소 1개 접근 가능
- 고급: 첫 30~60분 안에 현실적으로 접근 가능
- 희귀: 약 1~2시간대부터 탐험/전투로 직접 노릴 수 있음
- 영웅: 약 2~5시간대부터 위험 콘텐츠/보스로 현실적으로 등장
- 전설: 약 5~10시간대부터 고난도 구조물/주요 보스로 노릴 수 있음
- 신화: 약 8~15시간대부터 pinnacle 보스 또는 전설 3:1 합성으로 첫 획득 가능

이는 첫 획득 가능 시점이지 전체 로스터 완성 시간 목표가 아니다.

신화는 Ender Dragon 이후에만 허용하지 않는다. 다른 pinnacle 콘텐츠를 먼저 공략하면 드래곤 이전에도 얻을 수 있어야 한다.

## 13. 플레이테스트 지표

- 시간당 획득 코어 수 / 등급
- 획득 경로별 비율: 제작 / 상자 / 일반몹 / 이벤트 / 낚시 / 보스 / 합성
- 첫 고급/희귀/영웅/전설/신화 도달 시간
- 고밀도 외부 던전의 코어 과잉 여부
- 몹팜이 탐험보다 효율적인지 여부
- 동일 보스 반복이 다양한 보스 탐험을 압도하는지 여부
- 3:1 합성으로 소모되는 저등급 코어 비율
- 현재 전체 증강 후보 풀에서 원하는 빌드 완성 속도

## 14. 구현 데이터 구조

세 파일의 책임을 섞지 않는다.

### `core_distribution_contract.json`

관리 대상:

- structure distribution template
- mob profile
- event profile
- fishing profile
- core roll chance
- tier weights

### `boss_core_reward_contract.json`

관리 대상:

- boss_standard / major / pinnacle
- first kill bonus
- repeat distribution
- per-player reward
- 외부 boss adapter 기본 정책

### `core_source_profiles.json`

관리 대상:

- source ID / selector
- source kind
- exact override
- path family
- namespace default
- global fallback
- `coreRollMultiplier`
- exclusions
- event/boss redirect

선택자 우선순위:

1. exact override
2. path prefix/family
3. namespace default
4. global fallback

먼저 맞은 규칙이 승리한다.

확인하지 못한 외부 exact registry ID는 절대로 추측해서 활성화하지 않는다.

자세한 매핑은 `CORE_SOURCE_PROFILE_MATRIX_2026-10-24.md`를 따른다.

## 15. 런타임 구현 원칙

- 서버가 최종 보상을 결정한다.
- 구조물 loot 초기화/소비 시점, mob death, event completion, boss adapter 같은 **이벤트 기반 훅**에서만 판정한다.
- 주변 전체 구조물/몹을 매 tick 검색하지 않는다.
- 동일 사망/동일 loot container에서 중복 판정을 막는 provenance/state가 필요하다.
- 보스 개인 보상은 shared ground drop에 의존하지 않는다.
- source resolver는 JSON을 읽고 profile ID를 반환하며 확률 자체를 코드에 복제하지 않는다.

## 16. 현재 상태

- ACQUISITION LOOP DESIGNED: YES
- EXACT INITIAL WEIGHTS DEFINED: YES
- SOURCE PROFILE MATRIX WRITTEN: YES
- DATA SOURCE RESOLVER CONTRACT WRITTEN: YES
- VERIFIED FRIENDS&FOES EXACT HOSTILE IDS: YES
- ILLAGER INVASION EXACT PER-ENTITY IDS LOCKED: NO
- EXTERNAL BOSS EXACT PER-BOSS IDS LOCKED: NO
- RUNTIME CONSUMER IMPLEMENTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
- FINAL BALANCE LOCKED: NO

Docs/data contract only. Build/CI not run.
