# Augmentation Survival — Game Design Canon

> 작업명 기준 문서. 정식 명칭은 미정이다.

Date: 2026-10-24
Status: 146-AUGMENT DESIGN LOCK / IMPLEMENTATION PREP

개별 증강의 정확한 현재 효과와 레벨은 `AUGMENTS.md`가 우선한다. 상호작용 계산은 `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md` V2를 따른다. 코어 획득/확률/보스 보상은 `CORE_ACQUISITION_DESIGN_2026-10-24.md`를 따른다.

## 1. 한 줄 정의

Minecraft 장기 서바이벌을 그대로 살리면서, 여러 경로로 증강 코어를 모으고 증강을 계속 쌓아 전투·채광·이동·탐험·생존·생산 전반에서 플레이어가 과감하게 강해지는 파워 판타지 모드.

## 2. 핵심 디자인 원칙

### 2.1 증강이 주인공

- 증강은 보조 옵션이 아니라 플레이 방식 자체를 바꾸는 중심 성장 시스템이다.
- 낮은 등급도 실제로 쓸 만해야 한다.
- 높은 등급일수록 더 큰 규칙 변화와 강한 체감을 허용한다.
- 강한 조합은 의도적으로 허용한다. 무한/기하급수 재귀만 차단한다.
- 전투 수치만 올리지 않고 Minecraft의 여러 활동을 바꾼다.

### 2.2 전투 전용 로그라이크가 아니다

증강 축:

- 전투
- 채광 / 자원 수집
- 이동
- 탐험
- 생존
- 농사 / 목축
- 낚시
- 생산 / 작업

146종 로스터는 이 축 전체를 포함한다.

### 2.3 장기 월드용 랜덤성

랜덤성은 후보 선택의 기대감과 조합 발견을 만든다.

피한다:
- 최대 레벨 증강이 꽝으로 반복 등장
- 초반 운 하나가 장기 월드를 망치는 구조
- 특정 핵심 증강 하나를 얻지 못하면 진행 불가능한 구조

최대 레벨 증강은 후보 pool에서 제거한다.

## 3. 증강 등급과 확정 수량

등급:

1. 일반
2. 고급
3. 희귀
4. 영웅
5. 전설
6. 신화

현재 확정 로스터:

- 일반 20
- 고급 27
- 희귀 30
- 영웅 40
- 전설 20
- 신화 9
- 총 **146종**

각 증강의 최대 레벨도 `AUGMENTS.md`에 현재 정본으로 확정되어 있다.

원칙:
- 상위 등급을 하위 등급의 단순 수치 상위호환으로만 만들지 않는다.
- 영웅/전설/신화는 새로운 전투 규칙·이동 방식·자원 규칙·후반 루프를 열 수 있다.
- 신화도 엔더 드래곤 처치 이후 전용으로 잠그지 않는다.

## 4. 증강 코어와 선택

### 4.1 등급별 코어

일반 / 고급 / 희귀 / 영웅 / 전설 / 신화 증강 코어가 각각 존재한다.

### 4.2 기본 선택 규칙

1. 증강 장치에 원하는 등급 코어를 넣는다.
2. 같은 등급의 유효 후보 3개를 서버가 만든다.
3. 플레이어가 1개를 선택한다.
4. 선택 transaction이 유효하면 코어를 소비하고 증강을 획득한다.

상위 코어에 하위 등급 증강을 섞지 않는다.

### 4.3 두 번째 선택

신화 `두 번째 선택` 보유자는 코어 1개에서 나온 서로 다른 후보 2개를 얻는다.

- 코어 소비는 1개 그대로.
- 두 선택을 서버 transaction 하나로 commit한다.
- 부분 획득/부분 소비가 남지 않는다.

### 4.4 중복과 최대 레벨

- 1회 획득형은 보통 최대 Lv.1.
- 레벨형은 현재 정본의 Lv.2/3/5 등을 사용한다.
- 최대 레벨 증강은 후보 pool에서 제거한다.
- 특정 등급 전체가 최대 레벨이면 해당 코어 뽑기를 시작할 수 없고 코어도 소비하지 않는다.

## 5. 제작

### 5.1 일반 증강 코어

```text
R A R
A C A
R A R
```

- `R` = 레드스톤 가루
- `A` = 자수정 조각
- `C` = 구리 주괴
- 결과 = 일반 증강 코어 x1

### 5.2 기본 증강 장치

```text
I C I
C F C
I C I
```

- `I` = 철 주괴
- `C` = 구리 주괴
- `F` = 화로
- 결과 = 증강 장치 x1

철 시대 안에서 접근 가능하게 유지한다.

기본 기능:
- 등급별 코어 투입
- 같은 등급 후보 3개
- 선택/획득

### 5.3 강화 증강 장치

```text
I D I
G A G
I D I
```

- `I` = 철 주괴
- `D` = 다이아몬드
- `G` = 금 주괴
- `A` = 기본 증강 장치
- 결과 = 강화 증강 장치 x1

기본 기능 + 코어 합성을 제공한다.

## 6. 코어 합성

```text
일반 x3 -> 고급 x1
고급 x3 -> 희귀 x1
희귀 x3 -> 영웅 x1
영웅 x3 -> 전설 x1
전설 x3 -> 신화 x1
```

- 별도 촉매 없음.
- 역분해는 현재 없음.
- 다량 합성 UX를 지원한다.
- 3:1 비율은 현재 정본이나 실제 경제 플레이테스트에서 필요하면 조정 가능하다.

## 7. 코어 획득

### 7.1 장소는 등급 하나를 의미하지 않는다

`폐광 = 고급`, `고대 도시 = 전설` 같은 고정 대응을 사용하지 않는다.

각 콘텐츠는 여러 등급이 섞인 확률 분포를 가진다. 위험도/희소도가 높아질수록 분포가 상위 등급 쪽으로 이동한다.

### 7.2 획득 경로

- 일반 코어 제작
- 바닐라 구조물 상자
- 바닐라 몹 / 특수 몹
- 레이드·엘리트·특수 적 보상
- 낚시 보물
- 외부 모드 구조물
- 외부 모드 몹 / 보스
- 코어 합성

코어 제작 하나에만 의존하지 않는다.

초기 구현 확률은 `CORE_ACQUISITION_DESIGN_2026-10-24.md`에 정의한다.

핵심 시작값:

- routine 구조물 추가 roll 8%
- dangerous_mid 12%
- dangerous_high 18%
- pinnacle_cache 35%
- 일반 적대몹 player-attributed kill 0.20%
- 보스는 등급별 core 1개 보장 + 플레이어별 최초 처치 bonus 1개

Looting/Fortune/풍요/일반 드롭 복제 증강은 progression core를 증폭하지 않는다.

### 7.3 고등급 접근

- 초반에도 운/위험 감수로 고급·희귀를 볼 수 있다.
- 중반에는 희귀·영웅을 실제 목표로 삼을 수 있다.
- 전설·신화도 엔더 드래곤 이후 전용이 아니다.
- 전설 3개를 합성하면 신화 1개를 확정적으로 만들 수 있다.
- 최고급 보스/캐시에서는 신화 코어가 낮은 확률로 직접 등장한다.

## 8. 바닐라 콘텐츠와 loot

기존 Minecraft 콘텐츠를 버리지 않는다.

대표 연결 대상:
- 폐광
- 던전
- 난파선
- 사막 피라미드
- 정글 사원
- 폐허 포탈
- 네더 요새
- 보루 잔해
- 우드랜드 맨션
- 고대 도시
- 엔드 시티
- Trial 계열 보상
- 레이드 / Elder Guardian / Warden / Wither / Ender Dragon

원칙:
- 기존 loot를 통째로 교체하지 않는다.
- 가능한 경우 기존 loot에 코어 pool을 추가한다.
- 동일 장소에서 여러 등급이 나올 수 있다.
- 마을 생활 상자처럼 지나치게 안전하고 밀집된 source는 기본 주력 코어 source로 만들지 않는다.

## 9. 액티브 UX

일반 액티브:

- 증강 스킬 모드 키를 누르는 동안 숫자키 `1~0` = 액티브 빠른 슬롯 1~10.
- 평상시 숫자키는 기존 핫바 기능을 유지한다.
- 배치는 인벤토리 증강 탭 → `액티브 관리`에서 편집한다.
- 슬롯은 보유 제한이 아니라 빠른 접근 제한이다.
- 쿨다운/충전/지속시간은 능력 자체에 귀속한다.
- 실패/취소/서버 검증 실패는 비용과 쿨다운을 소비하지 않는다.

`권능 탈취`:
- 일반 10칸을 사용하지 않는다.
- 전용 권능 키 1개.
- 지금까지 탈취한 모든 지원 보스 권능 패키지를 한 번에 순차 해방한다.

## 10. 카드 / HUD

### 카드

- 기본 툴팁: 짧은 효과 + 중요한 숫자.
- Shift: 레벨별 계수, 범위, 지속시간, 쿨다운, 중첩, 대상 수 등 상세정보.
- 내부 provenance/adapter/재귀 차단 문구는 플레이어에게 노출하지 않는다.

### 체력

- 빨강: 실제 HP / max HP
- 노랑: absorption / 임시 추가 체력
- 파랑: 별도 보호막 pool
- 큰 체력에서도 하트 여러 줄보다 숫자+게이지 중심.
- 액체처럼 흐르는 표현을 사용하되 가독성을 해치지 않는다.

### 위협

우측 상단 작은 영역에 `위협 0~5` 표시.

## 11. 세이브 전체 위협 단계

지역별/플레이어별이 아니라 **세이브 전체가 하나의 위협 단계 0~5를 공유**한다.

- 플레이어가 달성한 augment power score 최고 기록을 world high-water mark로 사용.
- 로그아웃/장비 교체/일시적 약화로 내려가지 않는다.
- 일반/엘리트는 단계 상승/로드 시 갱신.
- 보스는 encounter 시작 때 단계 스냅샷.
- 매 tick 전체 적 재계산 금지.
- 적 공격력보다 최대체력을 더 크게 올린다.
- 플레이어 파워를 1:1 상쇄하지 않는다.

정확한 초기 배율 표는 `ENEMY_POWER_SCALING_DESIGN.md`에 있으며 플레이테스트 전용 시작값이다.

## 12. 외부 모드와 공식 소형 모드팩

현재 기술 스택과 공식 팩 후보는 `MODPACK_BASELINE.md`에 고정되어 있다.

핵심 후보:
- Terralith
- Repurposed Structures
- Dungeons and Taverns
- Friends&Foes
- Illager Invasion
- Naturalist
- Farmer's Delight Refabricated
- Better Combat (조건부)
- Chrono Dawn
- World Bosses
- MCEndgame
- GeckoLib
- Player Animation Library

원칙:
- 외부 모드가 없어도 본체는 정상 동작한다.
- 설치 시 해당 compat만 활성화한다.
- 외부 모드 JAR 자체 수정/재배포보다 우리 쪽 data/event/profile/adapter를 우선한다.
- 외부 구조물은 원 loot 보존 + 코어 pool 추가.
- 외부 몹/보스는 loot/reward adapter와 threat/control profile 사용.
- 외부 성장 시스템이 증강 핵심 루프를 무력화하면 공식 팩 포함을 재검토한다.

코어 획득 기본 연결:

- Terralith: 바이옴 자체 드랍 없음, 해당 바이옴 콘텐츠를 통해 획득
- Repurposed Structures: 대응하는 vanilla 구조물 위험도 profile 상속 우선
- Dungeons and Taverns: 소형 routine / 중형 dangerous_mid / 대형·심층 dangerous_high
- Naturalist: 일반 야생동물 core drop 없음
- Farmer's Delight: 일반 생산에서 직접 core 생성 없음
- Chrono Dawn / World Bosses: 보스는 기본 boss_major에서 시작
- MCEndgame: 고난도 던전 dangerous_high, 반복 보스 boss_major, 최종급 보스 pinnacle 후보

## 13. 현재 기술 스택

이미 확정:

- Minecraft Java 26.2
- Java 25
- Fabric
- Fabric Loader 0.19.5
- Fabric API 0.160.0+26.2
- Gradle 9.5.1
- Fabric Loom 1.17.20
- mod id `augmentation_survival`
- version `0.1.0-alpha.1`

로더 재비교 단계는 끝났다. 명확하고 큰 이득이 없는 한 개발 중 loader를 변경하지 않는다.

## 14. 외부 호환 핵심 계약

- Better Combat: 서버 ROOT melee hit 하나로 정규화.
- Farmer's Delight: crop/food/processing adapter.
- Chrono Dawn / World Bosses / MCEndgame: 보스 control/reward/authority profile.
- GeckoLib / Player Animation Library: 모델/애니메이션 표현, 서버 gameplay authority를 대체하지 않음.

상세는 `AUGMENT_FINAL_COMPATIBILITY_AUDIT_2026-10-24.md`를 따른다.

## 15. 구현 전에 남은 미확정 값

현재 정본에서 의도적으로 남긴 것은 다음뿐이다.

- 정식 게임명
- 일부 구조물/외부 보스의 개별 risk/reward profile 세부 매핑
- 코어 획득 확률의 최종 플레이테스트 튜닝 값
- `탄도 장막` 주기/범위/회당 파괴 수
- `임시 발판` 지속시간/쿨다운
- `생명 연결` 연결 조건/대상 수/범위/지속/공유 비율
- `작아지기` 웅크리기 유지시간
- `벽치기` 충돌 window/판정 기준
- 위협 단계 최종 수치 튜닝
- 최종 UI 미술 자산/세부 연출

146종 목록/등급/최대 레벨과 코어 획득 경제의 초기 구현 수치는 더 이상 미확정 항목이 아니다.

## 16. 다음 개발 순서

`DEVELOPMENT_IMPLEMENTATION_PLAN_2026-10-24.md`를 정본으로 사용한다.

1. runtime spine
2. core item / acquisition profile / loot-event reward layer
3. core/device/3-choice transaction
4. reference vertical slice
5. 일반·고급 bulk
6. 희귀·영웅 복잡 시스템
7. 전설·신화
8. world threat scaling
9. external compat
10. HUD/VFX/animation/sound
11. playtest/performance/multiplayer

현재는 신규 증강 기획 단계가 아니라 **코어 경제까지 잠근 구현 단계 진입 직전**이다.
