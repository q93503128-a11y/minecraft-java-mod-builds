# Augmentation Survival — Enemy Power Scaling Design

Date: 2026-10-24
Status: CANONICAL INITIAL IMPLEMENTATION BASELINE / NUMBERS PLAYTEST-TUNABLE

구현 수치 정본:

- `src/main/resources/data/augmentation_survival/augmentation/enemy_scaling_contract.json`

이 문서는 적 성장의 의도와 상호작용을 설명하고, 실제 단계 경계/배율은 위 JSON 계약을 사용한다.

## 1. 목적

증강이 많이 쌓인 플레이어는 바닐라 및 외부 모드 일반몹을 매우 빠르게 압도할 수 있다.

목표는 플레이어의 강함을 취소하는 것이 아니라:

- 초반 Minecraft 생존 감각은 유지하고
- 중후반에는 일반몹도 최소한 전투 대상으로 남게 하며
- 영웅/전설/신화 증강의 파워 판타지는 계속 체감되고
- 외부 보스의 고유 패턴과 phase를 스탯 뻥튀기로 망치지 않는 것

이다.

따라서 적은 플레이어 성장의 100%를 따라가지 않고 **부분적으로만 단계 상승**한다. 특히 공격력보다 최대체력을 더 크게 올려 후반의 광역/연쇄/추가타 화력을 받아낼 시간을 확보한다.

위협 8은 적 성장의 **하드 캡**이다. 플레이어 증강 파워는 이후에도 계속 성장하므로 후반 파워 판타지를 영구적인 rubber-band scaling으로 상쇄하지 않는다.

## 2. 기술 구조

Minecraft/Fabric의 엔티티 attribute를 서버에서 조정하는 방식으로 기본 스케일을 구현한다.

주요 대상:

- max health
- attack damage
- movement speed
- armor
- armor toughness가 실제로 의미 있는 대상
- knockback resistance
- follow/target range는 필요한 경우에만

외부 모드 몹도 바닐라 attribute를 정상 사용하면 같은 modifier 레이어를 적용할 수 있다.

단, 자체 스크립트 피해/고유 체력 시스템/특수 AI가 attribute를 우회하는 외부 몹은 namespace/entity profile별 compat adapter를 사용한다.

## 3. 플레이어 증강 파워 점수

각 플레이어의 **획득한 증강 레벨**을 내부 power score로 환산한다.

초기 구현 가중치:

- 일반 레벨 1개: 1
- 고급 레벨 1개: 1.5
- 희귀 레벨 1개: 2
- 영웅 레벨 1개: 3
- 전설 레벨 1개: 5
- 신화 레벨 1개: 8

이 점수는 플레이어에게 직접 보여주는 레벨이 아니라 적 스케일링용 내부 지표다.

개별 증강의 실제 체감 차이가 매우 크므로 플레이테스트에서 필요하면 일부 고성능 증강에 별도 power weight를 줄 수 있다.

## 4. 월드 전체 공통 위협 단계

위협 단계는 지역별로 나누지 않는다. **한 세이브 전체가 하나의 위협 단계 0~8을 공유한다.**

- 오버월드, 네더, 엔드, 공식 팩의 추가 차원까지 같은 위협 단계를 기본으로 사용한다.
- entity profile에서 명시적으로 제외한 튜토리얼/특수 이벤트 개체만 예외로 둘 수 있다.
- 멀티플레이에서도 플레이어마다 다른 위협 단계를 사용하지 않는다.

### 월드 위협 점수

현재 세이브에서 플레이어가 달성한 증강 power score 중 **가장 높은 기록(high-water mark)** 을 월드 위협 점수로 사용한다.

- 플레이어가 로그아웃해도 위협 단계가 내려가지 않는다.
- 증강을 일시적으로 잃거나 장비를 바꿔도 위협 단계가 내려가지 않는다.
- 강한 플레이어가 성장시킨 월드는 계속 그만큼 위험한 월드로 남는다.
- 단계 하락/리셋이 필요하다면 일반 플레이 중 자동 처리하지 않고 명시적인 별도 시스템에서만 허용한다.

이 방식으로 위협 단계를 낮추기 위한 로그아웃/장비 교체 악용을 막고, 세이브 자체의 진행도를 적 성장과 연결한다.

### 단계 변경 시 적용

- 위협 단계가 오르면 이후 생성되는 적은 새 단계로 생성한다.
- 이미 로드된 일반/엘리트 적대몹도 단계 상승 이벤트 때 한 번 새 배율로 갱신한다.
- 언로드된 기존 몹은 다시 로드될 때 현재 월드 위협 단계와 자신의 scaleGroup을 확인해 갱신한다.
- 매 tick 스탯을 재계산하지 않는다.
- 체력 배율이 올라갈 때 현재 체력은 기존 체력 비율을 유지한다.

보스는 전투 도중 갑자기 체력이 튀는 것을 막기 위해 **encounter 시작 시 현재 월드 위협 단계를 스냅샷**하고 해당 전투 동안 고정한다.

## 5. 일반 적대몹 위협 단계 — 초기 구현 테이블

| 단계 | world power score | 최대체력 | 공격 피해 | 이동속도 | 방어력 | 넉백 저항 |
|---|---:|---:|---:|---:|---:|---:|
| 0 | 0~24 | x1.00 | x1.00 | x1.00 | +0 | +0 |
| 1 | 25~59 | x1.25 | x1.10 | x1.00 | +0 | +0 |
| 2 | 60~109 | x1.60 | x1.20 | x1.03 | +1 | +0 |
| 3 | 110~174 | x2.10 | x1.35 | x1.05 | +2 | +0.03 |
| 4 | 175~259 | x3.00 | x1.55 | x1.075 | +4 | +0.05 |
| 5 | 260~364 | x4.50 | x1.80 | x1.10 | +6 | +0.10 |
| 6 | 365~489 | x6.50 | x2.05 | x1.125 | +8 | +0.13 |
| 7 | 490~639 | x9.00 | x2.35 | x1.15 | +10 | +0.16 |
| 8 | 640+ | x12.00 | x2.70 | x1.175 | +12 | +0.20 |

이 수치는 첫 실제 전투 플레이테스트의 시작값이다.

중요:

- **체력 상승을 공격력보다 크게 둔다.** 후반 증강은 광역/추가타/폭딜이 크게 늘어나기 때문이다.
- 이동속도는 조금만 올린다. 과도한 속도 증가는 전투 난이도보다 조작 스트레스를 만든다.
- 6~8단계는 후반 증강 누적량을 받아내기 위한 추가 구간이며 공격력은 HP보다 훨씬 완만하게 오른다.
- 일반몹이 강해져도 플레이어의 영웅/전설/신화 파워 상승률이 더 크도록 한다.
- 위협 8 이후 적 배율은 더 오르지 않는다.
- 단계 8에서도 단순 HP 스펀지가 되는지는 실제 TTK로 검증하며, 전투가 늘어지는 증상이 나오면 공격력을 올리기보다 HP 배율을 먼저 낮춘다.

예시: 기본 최대체력 20인 일반 적은 위협 5에서 90 HP, 위협 8에서 240 HP가 된다. 그러나 640점 이후에도 플레이어는 증강을 계속 획득할 수 있으므로 위협 8은 영구적인 난이도 동기화가 아니다.

## 6. 위협 단계 HUD

현재 위협 단계는 플레이어 HUD에 작게 노출한다.

- 플레이어-facing 표기는 `위협 0` ~ `위협 8`.
- 기본 위치는 우측 상단 작은 상태 영역.
- 내부 power score는 일반 HUD에 노출하지 않는다.
- 단계 상승 시에만 짧은 전환 연출/사운드를 사용한다.
- 자세한 정보 화면에서는 현재 단계의 대표 일반몹 체력/공격 배율 정도는 확인 가능하게 할 수 있다.

세부 시각 규칙은 `HUD_AND_PLAYER_TEXT_STANDARD.md`를 따른다.

## 7. 엘리트 / 보스 스케일

외부 모드의 강한 적과 보스에 일반몹 배율을 그대로 적용하지 않는다.

### 엘리트 / 구조물 특수몹

일반몹 **추가 증가분의 65%**를 적용한다.

예: 일반몹 최대체력 x3.0 단계라면 추가 증가분은 +2.0이고, 엘리트는 `1 + 2.0×0.65 = x2.30`이다.

위협 8의 일반몹 HP x12.0이라면 엘리트는 `1 + 11.0×0.65 = x8.15`가 된다.

### 보스

보스는 자체 HP/패턴/phase를 보존하기 위해 일반몹 추가 증가분의 일부만 적용한다.

- HP: 일반몹 추가 증가분의 35%
- 공격 피해: 일반몹 추가 증가분의 25~35%
- 이동속도: 기본 추가 스케일 없음
- armor/knockback/stun immunity: 보스 자체 프로필 우선

따라서 위협 8에서 일반몹 HP가 x12.0이어도 보스 HP 기본값은 `1 + 11.0×0.35 = x4.85` 수준이다. 이미 체력이 큰 외부 보스를 일반몹처럼 12배로 만들지 않는다.

Chrono Dawn, World Bosses, MCEndgame 등 scripted boss는 개별 profile에서 multiplier, stun/pull/swap/time-stop 지원 여부를 함께 관리한다.

## 8. 데이터 기반 프로필

하드코딩된 entity ID 분기문을 늘리지 않는다.

프로필 필드:

- entity/tag/namespace selector
- scaleGroup: normal / elite / boss / excluded
- healthScaleWeight
- damageScaleWeight
- speedScaleWeight
- armorScaleWeight
- knockbackScaleWeight
- allowStun
- allowPull
- allowSwap
- allowTimeStop
- allowPercentHpDamage
- customAdapter

기본 적대몹은 `normal`로 넓게 적용하고, 강한 구조물 적은 `elite`, 인식된 보스 encounter는 `boss`, 문제가 있는 특수 개체만 예외를 추가한다.

코어 보상 분류와 적 스케일 분류는 의미상 연결될 수 있지만 같은 데이터로 강제 결합하지 않는다. 보상과 난이도는 독립적으로 튜닝 가능해야 한다.

## 9. 보상과 스케일 분리

적이 스케일됐다는 이유만으로 코어 드롭률을 자동 증가시키지 않는다.

이유:

- 강한 플레이어가 스케일을 악용해 코어 보상을 증폭시키는 루프 방지
- 코어 획득 밸런스와 전투 난이도를 분리
- mob grinder에서 power score 조작으로 보상량을 늘리는 악용 방지

XP 조정도 실제 플레이테스트 후 별도로 판단한다.

## 10. 증강과의 주요 상호작용

- `천벌`은 최대체력 비율 피해이므로 적 HP 스케일이 올라가도 표시된 비율을 그대로 유지한다.
- `우두머리 사냥`은 스케일된 보스에게 정상 적용한다.
- `유리 대포`, `속도 전환`, `광폭화` 같은 공격 증강 때문에 단계가 오른다고 해서 그 공격 증가량을 1:1 상쇄하지 않는다.
- `지배`로 아군화한 개체는 지배 순간 현재 스케일된 전투 스탯을 유지한다.
- 월드 위협 단계가 이후 더 올라가더라도 이미 지배된 아군은 자동 재스케일하지 않는다.
- `거대화`의 공포는 scaleGroup boss에는 기본 적용하지 않는다.

## 11. 구현/검증 순서

1. save-wide world threat score / stage 저장 구조 구현
2. `enemy_scaling_contract.json` loader + validation
3. vanilla hostile 몇 종에 attribute modifier 기반 단계 스케일 구현
4. 단계 상승 시 로드된 적 1회 갱신 + 언로드 적 재로드 갱신 검증
5. zombie/skeleton/creeper/spider 등 기본군 실제 전투 테스트
6. Better Combat 사용 상태에서 플레이어 DPS 대비 TTK 측정
7. Friends&Foes / Illager Invasion 일반몹 확인
8. Chrono Dawn / World Bosses / MCEndgame 보스 profile 추가
9. 일반/엘리트/보스 multiplier 조정
10. 멀티플레이 실제 테스트 가능 시 세이브 공통 위협 단계 동기화 검증

현재 상태:

- DESIGN DEFINED: YES
- WORLD-GLOBAL THREAT RULE: YES
- THREAT RANGE: 0~8
- INITIAL IMPLEMENTATION TABLE LOCKED: YES
- DATA CONTRACT WRITTEN: YES
- IMPLEMENTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

Docs/data only. Build/CI not run for this scaling-contract pass.
