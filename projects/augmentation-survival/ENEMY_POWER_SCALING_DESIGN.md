# Augmentation Survival — Enemy Power Scaling Design

Date: 2026-10-24
Status: canonical architecture / numeric table provisional until playtest

## 1. 목적

증강이 많이 쌓인 플레이어는 바닐라 및 외부 모드 일반몹을 매우 빠르게 압도할 수 있다.

목표는 플레이어의 강함을 취소하는 것이 아니라:

- 초반 Minecraft 생존 감각은 유지하고
- 중후반에는 일반몹도 최소한 전투 대상으로 남게 하며
- 영웅/전설/신화 증강의 파워 판타지는 계속 체감되고
- 외부 보스의 고유 패턴과 phase를 스탯 뻥튀기로 망치지 않는 것

이다.

따라서 적은 플레이어 성장의 100%를 따라가지 않고 **부분적으로만 단계 상승**한다.

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

각 플레이어의 획득 증강 레벨을 내부 power score로 환산한다.

초기 권장 가중치:

- 일반 레벨 1개: 1
- 고급 레벨 1개: 1.5
- 희귀 레벨 1개: 2
- 영웅 레벨 1개: 3
- 전설 레벨 1개: 5
- 신화 레벨 1개: 8

이 점수는 플레이어에게 직접 보여주는 레벨이 아니라 적 스케일링용 내부 지표다.

개별 증강의 실제 체감 차이가 매우 크므로 최종 구현에서는 필요 시 일부 고성능 증강에 별도 power weight를 줄 수 있다.

## 4. 지역 기반 스케일

일반 적대몹이 생성될 때 반경 96블록 안 플레이어 중 가장 높은 power score를 기준으로 위협 단계를 결정하고, 해당 몹에 스케일을 스냅샷한다.

- 먼 곳의 강한 플레이어 때문에 약한 플레이어 주변 몹까지 전역 강화하지 않는다.
- 이미 생성된 몹은 플레이어가 장비/증강을 바꿀 때마다 실시간 재계산하지 않는다.
- 같은 몹의 체력이 매 tick 출렁이는 구조를 금지한다.
- 멀티플레이에서는 해당 전투 지역에서 가장 강한 플레이어를 기준으로 생성된 적이 그룹 전체에 대응하게 한다.

보스는 일반 스폰 순간이 아니라 **전투/소환 시작 시 참가자 기준**으로 한 번 스냅샷하고 encounter 중에는 고정한다.

## 5. 일반 적대몹 위협 단계 — 초기 플레이테스트 테이블

| 단계 | power score | 최대체력 | 공격 피해 | 이동속도 | 방어력 | 넉백 저항 |
|---|---:|---:|---:|---:|---:|---:|
| 0 | 0~24 | x1.00 | x1.00 | x1.00 | +0 | +0 |
| 1 | 25~59 | x1.25 | x1.10 | x1.00 | +0 | +0 |
| 2 | 60~109 | x1.60 | x1.20 | x1.03 | +1 | +0 |
| 3 | 110~174 | x2.10 | x1.35 | x1.05 | +2 | +0.03 |
| 4 | 175~259 | x3.00 | x1.55 | x1.075 | +4 | +0.05 |
| 5 | 260+ | x4.50 | x1.80 | x1.10 | +6 | +0.10 |

이 수치는 첫 실제 전투 플레이테스트 전의 시작점이다.

중요:

- 체력 상승을 공격력보다 크게 둔다. 후반 증강은 광역/추가타/폭딜이 크게 늘어나기 때문이다.
- 이동속도는 조금만 올린다. 과도한 속도 증가는 전투 난이도보다 조작 스트레스를 만든다.
- 일반몹이 강해져도 플레이어의 영웅/전설/신화 파워 상승률이 더 크도록 한다.

## 6. 엘리트 / 보스 스케일

외부 모드의 강한 적과 보스에 일반몹 배율을 그대로 적용하지 않는다.

### 엘리트 / 구조물 특수몹

일반몹 추가 증가분의 약 65%를 적용하는 것을 기본값으로 한다.

예: 일반몹 최대체력 x3.0 단계라면 추가 증가분은 +2.0이고, 엘리트는 `1 + 2.0×0.65 = x2.30` 수준에서 시작한다.

### 보스

보스는 자체 HP/패턴/phase를 보존하기 위해 일반몹 추가 증가분의 약 35%만 기본 적용한다.

- HP는 약 35% 비율
- 공격 피해는 약 25~35% 비율
- 이동속도는 기본적으로 추가 스케일하지 않는다.
- armor/knockback/stun immunity는 보스 자체 프로필 우선.

Chrono Dawn, World Bosses, MCEndgame 등 scripted boss는 개별 profile에서 multiplier, stun/pull/swap/time-stop 지원 여부를 함께 관리한다.

## 7. 데이터 기반 프로필

하드코딩된 entity ID 분기문을 늘리지 않는다.

예정 프로필 필드:

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

기본값은 넓게 적용하고, 문제가 있는 외부 몹만 예외를 추가한다.

## 8. 보상과 스케일 분리

적이 스케일됐다는 이유만으로 코어 드롭률을 자동 증가시키지 않는다.

이유:

- 강한 플레이어가 스케일을 악용해 코어 보상을 증폭시키는 루프 방지
- 코어 획득 밸런스와 전투 난이도를 분리
- mob grinder에서 power score 조작으로 보상량을 늘리는 악용 방지

XP 조정도 실제 플레이테스트 후 별도로 판단한다.

## 9. 증강과의 주요 상호작용

- `천벌`은 최대체력 비율 피해이므로 적 HP 스케일이 올라가도 표시된 비율을 그대로 유지한다.
- `우두머리 사냥`은 스케일된 보스에게 정상 적용한다.
- `유리 대포`, `속도 전환`, `광폭화` 같은 공격 증강 때문에 단계가 오른다고 해서 그 공격 증가량을 1:1 상쇄하지 않는다.
- `지배`로 아군화한 개체는 지배 순간 현재 스케일된 전투 스탯을 유지하되, 이후 추가 플레이어 power 변화로 다시 스케일하지 않는다.
- `거대화`의 공포는 scaleGroup boss에는 기본 적용하지 않는다.

## 10. 구현/검증 순서

1. vanilla hostile 몇 종에 attribute modifier 기반 단계 스케일 구현
2. zombie/skeleton/creeper/spider 등 기본군 실제 전투 테스트
3. Better Combat 사용 상태에서 플레이어 DPS 대비 TTK 측정
4. Friends&Foes / Illager Invasion 일반몹 확인
5. Chrono Dawn / World Bosses / MCEndgame 보스 profile 추가
6. 일반/엘리트/보스 multiplier 조정
7. 멀티플레이 실제 테스트 가능 시 지역 최고 power score 스냅샷 검증

현재 상태:

- DESIGN DEFINED: YES
- IMPLEMENTED: NO
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

문서 설계만 추가하므로 build/CI는 실행하지 않는다.
