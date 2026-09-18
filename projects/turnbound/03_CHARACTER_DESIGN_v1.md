# TURNBOUND Character Design v1

> 역할: 대격변 이후 플레이어블 캐릭터의 역할/컨셉/스킬 방향 정본.
> 아래 수치는 concept budget이며 최종 계수는 `02_BALANCE_RULES_v1.md`의 simulator pass 이후 고정한다.

## 1. 캐릭터 설계 규칙

모든 정식 캐릭터는 최소한 다음 질문에 답해야 한다.

- 한 문장으로 무엇을 하는 캐릭터인가?
- 플레이어가 이 캐릭터를 넣으면 전투 방식이 무엇이 달라지는가?
- 대표 mechanic을 화면에서 어떻게 알아보는가?
- 어떤 파티와 강한가?
- 어떤 전투에서 약한가?
- Auto가 이 캐릭터를 망치지 않고 최소한의 역할을 수행할 수 있는가?
- 외형/무기/애니메이션이 역할을 말해주는가?

단순 `ATK +10%` passive만으로 정체성을 만들지 않는다.

## 2. 희귀도

정식 플레이어블 희귀도는 ★3~★5.

- ★3: 규칙이 단순하고 좁은 전문성이 강함
- ★4: 한 가지 완성된 signature loop
- ★5: 높은 ceiling 또는 복합적인 선택지를 가질 수 있음

★5가 ★3의 상위호환이 되는 구조를 목표로 하지 않는다.

F01~F04의 기존 “재료형” 분류는 폐기한다.

## 3. P01 카이렌 — 결투 흐름을 쌓는 추적 검사

### 유지할 좋은 점
- 한 대상을 오래 압박하는 단일 딜러
- Focus를 쌓고 큰 공격으로 연결하는 직관적인 목표
- 보스/엘리트에 강하고 잦은 타겟 변경에 약한 정체성

### 현재 문제
- 기존 Active 2가 피해 없이 “대상 지정 + Focus + Gauge”만 수행해 버튼 감각이 약하다.
- Focus가 단순 누적 피해 보너스로만 읽히면 조작 재미가 작다.
- 스킬 2 runtime 문제까지 겹쳐 핵심 루프가 체감되지 않는다.

### v1 컨셉
**“같은 적의 공격 흐름을 읽고 점점 더 깊게 파고드는 결투가.”**

Signature: `Duel Focus 0~3`

- 같은 대상에게 직접 행동을 이어갈수록 Focus 증가
- 다른 대상을 직접 공격하면 기존 Focus 정리
- Focus는 피해뿐 아니라 스킬의 행동 방식도 바꾼다

### Kit 방향
- Basic: 가벼운 접근 베기. Focus 유지/증가.
- Active A: Focus를 소비하지 않는 강한 단일기. Focus가 높을수록 추가 hit/방어 관통 등 명확한 변화.
- Active B: 기존 무피해 “결투 고정” 폐기. 대상에게 실제 공격/간파 동작을 하며 Focus 전환 손실을 줄이고 다음 상호작용을 만든다.
- Passive: 같은 대상과의 장기전에서 공격이 단순 수치가 아니라 리듬/추가 베기/간파로 확장.
- Awakening: 최대 Focus에서 Basic/Active animation과 추가 행동이 확장되되 기존 플레이를 제거하지 않음.

Role: Single DPS / Duel / Boss pressure.

## 4. P02 루메아 — 행동 순서를 편집하는 템포 지휘자

### 유지할 좋은 점
- TURNBOUND의 Turn Gauge 정체성을 가장 잘 보여주는 캐릭터
- 느린 강캐의 턴을 끌어오고 위험한 적을 지연시키는 역할

### 현재 문제
- “Gauge를 바로 1000” 효과는 행동 경제상 지나치게 강해지기 쉽다.
- Basic까지 높은 Gauge 공급이면 본인 피해/생존 기여 없이도 거의 모든 파티의 정답이 될 위험.
- SPD가 제대로 적용되지 않는 현재 엔진에서는 캐릭터 가치 자체가 왜곡됨.

### v1 컨셉
**“턴을 생성하는 것이 아니라, 가장 가치 있는 순간에 팀의 시간을 재배치한다.”**

Signature: `Tempo Window`

- 아군 행동 순서를 앞당기거나 적을 늦추지만 연속 남발은 불가
- 느린 아군을 도울 때 효율이 더 좋고 이미 곧 행동할 대상에는 효율이 낮다

### Kit 방향
- Basic: 중간 크기 단일 Gauge push. 누구에게 쓸지 선택하는 행동.
- Active A: 큰 action advance. 즉시 1000 고정은 제거하거나 강한 조건/CD를 붙인다.
- Active B: 적 전체가 아니라 지정 적/소수에 더 강한 delay, 또는 전체에는 작은 delay.
- Passive: “자신보다 느린 아군”과의 시너지 유지하되 무한 자기 Gauge 루프 방지.
- Awakening: turn order를 읽고 사용했을 때 보너스를 주는 방향.

Role: Tempo Support / Gauge Control.

## 5. P03 브람 — 피해를 가로채 반격 자원으로 바꾸는 수호자

### 유지할 좋은 점
- Redirect + Counter가 매우 읽기 쉬운 탱커 정체성
- 단일 강공/암살형 적에 강하고 AoE에 약한 구조

### 현재 문제
- Basic이 자기 Barrier만 반복하면 자기 턴이 “준비만 하는 턴”으로 느껴질 수 있다.
- Counter가 무조건 자동으로만 나오면 플레이어가 설계했다는 감각이 약함.

### v1 컨셉
**“아군 대신 맞고 그 충격을 저장했다가 전열을 밀어낸다.”**

Signature: `Guard 0~100`

- Redirect/직접 피해로 Guard 축적
- Guard가 쌓일수록 반격 또는 방패 기술이 강화

### Kit 방향
- Basic: 짧은 방패 타격 + 소형 Barrier 또는 Guard gain.
- Active A: 한 아군 보호/redirect.
- Active B: Guard를 활용한 압박/도발/게이지 지연.
- Passive: 보호 중 받은 직접 공격에 counter.
- Awakening: Guard가 가득 찼을 때 한 번의 강한 team protection/retaliation window.

Role: Tank / Redirect / Counter.

## 6. P04 엘리시아 — 위험 상태를 회복 창으로 바꾸는 구조 치료사

### 유지할 좋은 점
- 초보자에게 필요한 안정적인 healer/revive
- “실수 한두 번을 복구”하는 역할

### 현재 문제
- Basic heal + AoE heal + Revive만으로는 다른 RPG의 일반 힐러와 차이가 약하다.
- heal 수치만 올리는 캐릭터가 되기 쉽다.

### v1 컨셉
**“큰 피해가 들어오기 전에 회복 지점을 준비하고, 무너진 아군을 다시 전선에 세운다.”**

Signature: `Sanctuary Mark`

- 아군에게 짧은 보호/회복 표식을 남김
- 표식 대상이 큰 피해를 받거나 위험 HP에 진입하면 저장된 회복/피해완화가 발동

### Kit 방향
- Basic: 작은 즉시 회복 + Sanctuary 준비.
- Active A: 위기 아군을 즉시 안정화하는 강한 단일 구조.
- Active B: 팀 전체 회복은 유지하되 표식 대상과 상호작용.
- Revive: 캐릭터 핵심 identity는 유지하되 premium CD.
- Passive: “처음 30% 이하” 자동 heal 같은 1회성 응급 구조를 Sanctuary 시스템과 통합.
- Awakening: revive 직후 바로 다시 쓰러지는 문제를 보호막/DR/turn support로 해결.

Role: Heal / Rescue / Revive.

## 7. P05 리네트 — 팀 공격 사이에 끼어드는 추격 사수

### 유지할 좋은 점
- Follow-up이 파티 조합을 바꾸는 좋은 mechanic
- 빠른 파티/다단 행동과 자연스럽게 시너지

### 현재 문제
- Expose stack + Hunt Target + follow-up 제한이 동시에 있어 bookkeeping이 늘어난다.
- 화면에서 어떤 적이 추격 가능한지 즉시 읽히기 어려울 수 있다.

### v1 컨셉
**“한 명을 조준하면 동료의 공격을 사격 기회로 바꾸는 spotter.”**

Signature: `Sightline`

- 동시에 한 적만 명확하게 표시
- 아군이 Sightline 대상을 공격하면 Shot Window가 쌓임
- 일정 조건에서 Lynette follow-up

### Kit 방향
- Basic: Sightline 지정/유지 + 경량 사격.
- Active A: 축적한 Shot Window를 쓰는 관통 단일기.
- Active B: Sightline을 즉시 새 대상에 설정하고 일정 시간 follow-up 강화.
- Passive: allied direct hit를 제한된 follow-up으로 변환.
- Awakening: 두 번째/세 번째 팀 행동에 리듬 보너스.

Role: Follow-up / Single DPS / Team synergy.

## 8. P06 모르웬 — 전투의 사건을 기록해 마무리에 쓰는 집행자

### 유지할 좋은 점
- “죽음을 기록”하는 컨셉은 강한 캐릭터성
- 고난도전/역전 상황에서 강해지는 구조

### 현재 문제
- 아무도 죽지 않는 짧고 깔끔한 전투에서는 mechanic이 사실상 비어 있음.
- 캐릭터가 잘 굴러가려면 아군 사망을 바라야 하는 역설이 생길 수 있음.

### v1 컨셉
**“죽음뿐 아니라 전투의 결정적 사건을 기록하고, 죽음은 가장 큰 기록이 된다.”**

Signature: `Records`

기록 후보:
- 적 최초 처치
- Elite/Boss phase break
- 아군 최초 위험 HP 진입
- 부활
- 실제 사망

죽음은 가장 많은 Records를 주지만 필수 조건은 아니다.

### Kit 방향
- Basic: Records에 따라 조금 강화.
- Active A: Records 일부를 소비하는 안정적 burst.
- Active B: 낮은 HP 적을 마무리하면 Records/게이지 회수.
- Passive: 사건 기록.
- 자가부활은 캐릭터의 고유 comeback으로 유지하되 전투당 1회.
- Awakening: 부활/마무리 이후 짧은 execution window.

Role: Event Resource / Execute / Comeback.

## 9. P07 마리온 — 계약수와 한 몸처럼 싸우는 동반자 소환술사

### 유지할 좋은 점
- 소환물이 캐릭터 정체성 절반인 강한 시각/게임플레이 컨셉
- extra body와 utility 가능

### 현재 문제
- 독립 Turn Gauge 소환물은 사실상 파티에 추가 정규 행동권을 하나 더 주므로 power budget이 매우 큼.
- 전투원 수/타임라인/UI가 복잡해짐.
- 첫 몇 턴을 “소환부터 하기”에 쓰면 캐릭터 재미가 늦게 시작됨.

### v1 컨셉
**“계약수는 처음부터 전장에 있고, 마리온의 행동에 반응하는 partner.”**

Signature: `Bond`

- 계약수는 기본적으로 별도 full regular turn을 갖지 않는다.
- Marion의 Basic/Active/특정 ally action에 Reaction/Command로 움직임
- Bond가 쌓이면 강한 합동 행동 가능

### Kit 방향
- 전투 시작: 계약수 자동 동반.
- Basic: 계약수 command attack.
- Active A: 계약수 방어/위치/utility stance 전환.
- Active B: Bond를 소비하는 합동 공격.
- Passive: 계약수가 피해를 대신 받거나 down되면 Marion의 kit 변화.
- Awakening: 계약수의 second form 또는 한 번의 자동 복귀.

Role: Summon Partner / Utility / Combo.

## 10. P08 라제 — 자기 몸을 연료로 속도를 올리는 난전 광전사

### 유지할 좋은 점
- 위험 관리
- 낮은 희귀도에서 높은 공격 ceiling
- HP self-cost라는 즉시 이해되는 테마

### 현재 문제
- “HP 낮으면 ATK 증가”만으로는 전형적인 berserker에서 벗어나기 어렵다.
- 과도한 heal과 상충하는데 이를 재미있는 선택으로 바꾸는 장치가 적음.

### v1 컨셉
**“피해를 주고 받으며 Fury를 올리고, 원하는 순간에 과열한다.”**

Signature: `Fury 0~100`

- 자신이 피해를 주거나 받거나 HP cost를 지불하면 Fury 증가
- Fury 구간에 따라 attack animation/추가효과 변화
- 낮은 HP는 Fury 효율을 높이는 보조 조건이지 유일한 mechanic이 아님

### Kit 방향
- Basic: Fury 수급.
- Active A: HP cost + 큰 Fury/공격.
- Active B: Fury를 소비해 일정 기간 폭주.
- Passive: 저HP에서 Fury 효율/생존 tradeoff.
- Awakening: 전투당 1회 lethal survival은 유지 가능하되 Fury loop와 연결.

Role: Risk DPS / Fury / Burst.

## 11. F01~F04 처리

### F03 변경 사냥꾼
- 정식 이름/외형을 가진 ★3 field recruit 후보.
- 단순하지만 “표적 약점/첫타/정찰” 같은 좁은 전문성 부여.

### F04 방패 용병
- ★3 field recruit 후보.
- Bram의 하위호환이 아니라 짧은 자기 방어 + 파티 한 번 보호 같은 저비용 specialist.

### F01 민병 견습생 / F02 야전 견습생
- 가챠 filler로는 제거.
- 필요하면 튜토리얼/지역 NPC companion으로 재설계.
- 정식 캐릭터로 승격하려면 이름, 외형, 역할, signature mechanic을 새로 받아야 함.

## 12. 캐릭터 얼굴/portrait

목표:
- 파티창
- 전투 하단 파티 상태
- Turn Order rail
- 결과창
- 캐릭터 상세
에서 같은 portrait를 사용한다.

우선순위:
1. 실제 최종 3D 모델을 고정 조명/카메라로 offscreen render해서 portrait cache 생성
2. 모델 head bone/texture 구조가 안정적이면 얼굴 영역을 authored icon으로 제작
3. 품질/라이선스가 해결되지 않으면 상징 icon 사용

자동 crop이 얼굴을 잘라먹거나 캐릭터마다 구도가 달라지는 상태를 production으로 사용하지 않는다.

## 13. 외형 제작 원칙

- 외부 캐릭터 디자인/모델을 적극 조사
- 직접 사용 가능 asset은 라이선스 확인 후 수정/결합 가능
- reference-only 디자인은 실루엣/색 분배/장비 구조 원리를 참고하고 그대로 복제하지 않음
- 바닐라 스킨 + 작은 장식 수준으로 주요 영웅을 끝내지 않음
- 스킬의 보이는 범위와 실제 판정 일치
- 각 캐릭터의 idle/ready/basic/active/hit/down/revive/victory motion을 역할에 맞춤

## 14. 수치 확정 전 금지

대격변 동안 기존 v0.4 계수를 “새 정본”이라고 그대로 복사하지 않는다.

먼저:
- fixed-point TurnScheduler
- 전투 simulator
- P01~P08 concept prototype
- 표준 적/보스 baseline
을 맞춘 뒤 최종 HP/ATK/DEF/SPD/skill potency를 갱신한다.
