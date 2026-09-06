# 04 — CHARACTER SYSTEM

## 1. CharacterDefinition 최소 계약
- id / display key
- sourceEntity(optional)
- originStar / squadCost
- roles
- baseStats / growth / ascensionFlat
- poiseMax
- affinities 6종 전부
- basicAction / skills / burst / passives
- availability
- presentationKey(시각 게이트 후 연결)

## 2. 역할
하나 이상의 역할을 가진다.
- `VANGUARD`: 생존/보호/위협 관리.
- `BREAKER`: 높은 Poise 피해/Intent 방해.
- `STRIKER`: EXPOSED/약점 창의 HP 피해.
- `SUPPORT`: 회복/버프/자원 지원.
- `CONTROLLER`: 디버프/속도/대상 통제.

역할은 클래스 제한이 아니라 밸런스/편성 설명용 태그다.

## 3. 기본 kit 크기
초기 표준:
- Basic 1
- Skill 2~3
- Burst 1
- Passive 1~2
캐릭터 수가 많기 때문에 모든 캐릭터를 복잡하게 만들지 않는다. ★1~2는 이해하기 쉬운 좁은 역할, ★4~5는 더 깊은 상호작용을 허용한다.

## 4. 스킬 설계 규칙
좋은 스킬은 최소 하나의 결정을 만든다.
- 지금 Energy를 쓸지.
- HP 피해 vs Poise 피해.
- Intent 대응 vs 공격 창 극대화.
- 단일 vs 다수.
- 상태 준비 vs 소비.

단순히 같은 공격의 숫자만 다른 스킬을 반복하지 않는다.

## 5. 캐릭터별 예산
`originStar`가 높을수록 평균 kit/stat budget이 높다. 다만 낮은 별도 특정 좁은 상황에서는 최상위 효율을 가질 수 있다.

초기 상대 지표(절대 공식 아님):
- ★1: budget index 70
- ★2: 80
- ★3: 90
- ★4: 100
- ★5: 110
승급은 레벨 상한/ascension 보너스를 올리되 이 origin 정체성을 지우지 않는다.

## 6. 대표 Vertical Slice 캐릭터
바닐라를 이용해 시스템 폭을 빠르게 검증한다.
- Zombie — VANGUARD/BREAKER, 단순 근접/버티기.
- Skeleton — STRIKER, PROJECTILE 단일 집중.
- Spider — CONTROLLER/STRIKER, 빠른 SPD/상태.
- Creeper — BLAST 고위험 행동/Intent 대응 검증.
- Blaze — FIRE/다중행동 또는 충전 Intent 검증.
- Witch — SUPPORT/CONTROLLER, 버프/디버프 검증.
- Enderman — 회피/VOID 상호작용 검증.
- Iron Golem — VANGUARD/BREAKER, 높은 Poise/느린 SPD.

이 목록은 최종 캐릭터 디자인이 아니라 기술/전투 메커니즘 대표 표본이다. 최종 외형/VFX는 시각 게이트 이후.

## 7. 캐릭터 제작 절차
1. source fantasy/바닐라 행동 특성 분석.
2. originStar/role/squadCost 결정.
3. 전투에서 담당할 한 문장 역할 정의.
4. Basic/Skill/Burst/Passive의 의사결정 연결.
5. affinity/Poise/Intent 상호작용 정의.
6. 자동 simulation로 DPR/Poise/생존 확인.
7. 실플레이.
8. 시각 레퍼런스가 필요한 경우 별도 Gate.

## 8. 금지
- 낮은 별을 살린다는 이유로 모든 낮은 별에 숨은 성장 보정.
- 별만 높고 역할/kit 차이가 없는 상위호환 복제.
- 모든 캐릭터에 stun/추가턴을 붙여 전투 순서를 무력화.
- 외형부터 만들고 나중에 스킬을 억지로 맞추기.
