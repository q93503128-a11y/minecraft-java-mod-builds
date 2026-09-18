# TURNBOUND Character Design v1

> P01~P08의 대격변 이후 정본.
> ID는 save continuity 때문에 유지할 수 있지만 옛 kit/수치는 자동 계승하지 않는다.
> 모든 캐릭터는 한 문장 역할, signature mechanic, 강점, 약점, 시각 표현을 가져야 한다.

## 1. 공통 설계 규칙

정식 캐릭터는:
- 한 문장으로 역할 설명 가능
- signature mechanic 1개
- 강한 조합 1개 이상
- 실제 약점 1개 이상
- Auto가 최소 역할 수행 가능
- 모델/무기/animation으로 역할이 읽힘
- 다른 캐릭터의 단순 상위호환이 아님

정식 희귀도:
- ★3: 단순/전문
- ★4: 완성된 signature loop
- ★5: 더 높은 선택지/ceiling

★1~2 fodder 체계는 폐기.

---

## 2. P01 카이렌 — 결투 추적 검사

**한 문장:** 같은 적을 계속 압박할수록 공격 흐름을 읽어 추가 베기를 만들어내는 단일 결투 DPS.

Rarity: ★4  
Role: Single DPS / Duel / Boss pressure  
SPD target: 105  
Signature: **Focus 0~3**

### 강점
- boss/elite 단일 대상
- 같은 적 장기 압박
- Focus 3에서 높은 action value

### 약점
- 잦은 target swap
- 다수전
- setup 전에 적이 빨리 죽는 전투

### Kit
**Basic · 추적 베기**
- 단일 ATK 95%
- 새 대상이면 Focus 1
- 같은 대상이면 Focus +1
- Focus 3이면 25% 추가 베기

**Active A · 파쇄 일격**
- CD2
- 단일 175%
- 같은 결투 대상 Focus 2: +30% follow-up
- Focus 3: +50% follow-up
- Focus 소비 없음

**Active B · 간파 베기**
- CD3
- 단일 110%
- 결투 대상 지정/유지
- Focus +1
- 자신 Gauge +100

**Passive · 집요한 추적**
- 현재 focusTarget에게 Focus 1당 직접 피해 +5%
- 다른 대상에는 보너스 없음

**Awakening · 끝나지 않는 칼끝**
- Focus 3 Basic 추가 베기 25% → 45%
- Focus 3으로 적 처치 시 다음 결투 대상 Focus 2 시작

### 시각
- 빠른 한손검/세검 계열
- Focus 단계가 자세/검광/표식으로 읽혀야 함
- target 위 작은 결투 표식
- Focus 3 추가타는 별도 animation beat

---

## 3. P02 루메아 — 템포 지휘자

**한 문장:** 턴을 생성하는 대신 가장 가치 있는 순간에 아군과 적의 행동 순서를 재배치하는 support.

Rarity: ★5  
Role: Tempo Support / Gauge Control  
SPD target: 114  
Signature: **Tempo Window**

### 강점
- 느린 강캐 지원
- 위험한 적 순서 지연
- revive/heal timing 보정

### 약점
- raw damage 낮음
- 이미 빠른 파티에서는 효율 감소
- 자동으로 막 눌러서는 ceiling이 나오지 않음

### Kit
**Basic · 가속**
- 다른 아군 Gauge +120
- 대상 SPD < 루메아 SPD이면 +40 추가
- 다른 일반 아군이 모두 쓰러진 경우만 self-target 허용

**Active A · 시간 도약**
- CD4
- 다른 아군 Gauge +300
- 대상이 루메아보다 느리면 +60 추가
- 즉시 1000은 사용하지 않음

**Active B · 시차 봉쇄**
- CD3
- 적 1명 85%
- Gauge -180

**Passive · Tempo Window**
- 느린 아군을 당길 때 추가 효율
- 옛 “느린 아군 행동마다 루메아 자기 Gauge” 자동 루프 폐기

**Awakening · 정확한 시차**
- 시간 도약으로 대상이 다음 2행동 안에 들어옴
- 또는 시차 봉쇄로 다음 2행동 안 적이 밖으로 밀림
- 실제 TurnScheduler 결과가 위 조건이면 루메아 Gauge +60

### 시각
- clock/tempo cliché보다 지휘/시간 절단 motif
- 스킬 사용 시 Turn Order rail이 즉시 재배치되는 것이 연출 핵심
- 캐릭터 주변 시계 HUD를 남발하지 않음

---

## 4. P03 브람 — 충격을 저장하는 수호자

**한 문장:** 아군 대신 피해를 받아 Guard를 쌓고, 저장한 충격을 적의 템포를 무너뜨리는 반격으로 되돌리는 tank.

Rarity: ★4  
Role: Tank / Redirect / Counter  
SPD target: 84  
Signature: **Guard 0~100**

### 강점
- 단일 강공/암살형 적
- 취약한 아군 보호
- enemy tempo 방해

### 약점
- 광역 지속 피해
- 보호 대상을 잘못 고르면 낮은 효율
- 느린 SPD

### Guard 획득
- Basic: +15
- 자신이 직접 피해를 받음: +10, 적 행동당 1회
- redirect 피해를 받음: +20, 적 행동당 1회
- 최대 100

### Kit
**Basic · 방패 강타**
- 단일 80%
- Guard +15
- 자신에게 MaxHP 4% Barrier

**Active A · 보호 전환**
- CD3
- 다른 아군 1명에게 2회의 해당 아군 regular action 동안 보호
- 대상에게 들어오는 단일 직접 피해 65%를 브람이 대신 받음
- redirect 발생 시 Guard +20
- self-target 불가

**Active B · 진동 방패**
- CD2
- 적 1명 90%
- Gauge -100
- Guard 50 이상이면 Guard 50 소비:
  - 피해 130%
  - Gauge -180
  - 자신 MaxHP 8% Barrier

**Passive · 되받는 방벽**
- 보호 전환으로 redirect가 실제 발생하면 공격자에게 45% counter
- 적 행동 하나당 최대 1회
- counter가 다시 counter를 유발하지 않음

**Awakening · 불굴의 전열**
- Guard 100 도달 후 다음 redirect를 처리할 때:
  - Guard 전부 소비
  - 그 redirect 피해 추가 30% 감소
  - 파티 전체에 브람 MaxHP 6% Barrier
- 전투당 반복 가능하되 Guard를 다시 100까지 쌓아야 함

### 시각
- 큰 방패와 낮은 무게중심
- Guard가 쌓일수록 방패 표면/자세 변화
- redirect는 순간이동보다 실제 방패를 끼워 넣는 짧은 이동/방어 animation

---

## 5. P04 엘리시아 — 구조 치료사

**한 문장:** 위기가 발생한 뒤 숫자를 채우는 힐러가 아니라, 미리 Sanctuary를 준비해 치명적인 순간을 구조하는 healer.

Rarity: ★4  
Role: Heal / Rescue / Revive  
SPD target: 96  
Signature: **Sanctuary Mark**

### 강점
- 초보 파티 안정성
- 급격한 단일 피해
- revive 후 재사망 방지

### 약점
- 지속적인 압도적 광역 피해
- 딜 기여 낮음
- Sanctuary를 잘못 분배하면 낭비

### Sanctuary
- 동시에 여러 아군에게 존재 가능
- 각 대상별 1회 trigger
- 2 대상 regular action 동안 유지
- 대상이 HP 35% 이하로 내려가면 소비

### Kit
**Basic · 안식의 손길**
- 아군 1명 ATK 50% 회복
- Sanctuary Mark 부여/갱신

**Active A · 안식의 빛**
- CD3
- 아군 전체 ATK 55% 회복
- 이미 Sanctuary가 있는 대상은 추가 ATK 20% 회복
- 새 Sanctuary를 전원에게 뿌리지는 않음

**Active B · 되돌아온 숨**
- CD5
- 전투불능 아군 1명 MaxHP 30%로 부활
- Gauge +150
- Sanctuary Mark 부여

**Passive · 미리 남긴 빛**
- Sanctuary 대상이 35% 이하 진입:
  - 즉시 ATK 45% Reaction heal
  - Mark 소비
- 같은 damage event에 중복 발동 금지

**Awakening · 돌아온 사람의 보호**
- 부활한 대상:
  - MaxHP 15% Barrier
  - 다음 regular action까지 받는 피해 -20%

### 시각
- 흰빛 폭발 반복보다 작고 명확한 표식
- Sanctuary가 있는 대상은 발밑/어깨 주변의 낮은 강도 symbol
- revive는 캐릭터 모델의 실제 기상 animation과 연결

---

## 6. P05 리네트 — 추격 사수

**한 문장:** 한 적을 Sightline으로 지정하고 동료의 공격을 자신의 사격 기회로 바꾸는 follow-up DPS.

Rarity: ★4  
Role: Follow-up / Single DPS / Team synergy  
SPD target: 108  
Signature: **Sightline + Shot**

### 강점
- 빠른 파티
- 단일 집중 공격
- 팀 전체 direct hit가 많은 조합

### 약점
- target swap
- 혼자 남았을 때
- 광역전에 낮은 효율

### Sightline
- 동시에 적 1명
- 새 대상 지정 시 이전 해제
- allied direct hit가 Sightline 대상에 적중하면 Shot +1
- 리네트 자신의 direct hit는 Shot 생성하지 않음
- Shot 최대 2
- 리네트 regular action 사이 follow-up 최대 1회

### Kit
**Basic · 조준 사격**
- Sightline 대상 지정/유지
- 단일 90%
- 이미 같은 Sightline이면 Shot +1은 생성하지 않지만 다음 ally trigger를 유지

**Active A · 관통 사격**
- CD2
- 단일 165%
- Shot 1 이상이면 전부 소비
- Shot 1: +35%
- Shot 2: +70%

**Active B · 사냥 신호**
- CD3
- 적 1명을 Sightline으로 즉시 지정
- 단일 100%
- Shot +1
- 기존 Sightline 대상 교체 가능

**Passive · 교차 사격**
- 다른 아군이 Sightline 대상에게 direct damage:
  - Shot +1
- Shot 2가 되면 50% follow-up 발동 후 Shot 0
- 리네트 regular action 사이 1회 제한

**Awakening · 두 번째 탄도**
- 교차 사격 follow-up이 발동할 때:
  - 대상 Gauge -60
  - 다음 관통 사격의 Shot 2 bonus를 +70% → +90%로 강화
- 이 강화는 1회 사용 후 소멸

### 시각
- Sightline은 적 머리 위 작은 조준 표식
- Shot 0/1/2는 portrait 주변 작은 탄창 indicator
- follow-up은 과한 camera cut 없이 전장 흐름 속 짧은 사격

---

## 7. P06 모르웬 — 사건 기록 집행자

**한 문장:** 전투의 중요한 사건을 Records로 저장하고 마무리 순간에 소비하는 execute/comeback DPS.

Rarity: ★5  
Role: Event Resource / Execute / Comeback  
SPD target: 98  
Signature: **Records 0~5**

### 강점
- 긴 전투
- boss phase
- 위기/역전 상황
- 마무리

### 약점
- 매우 짧고 완벽하게 끝나는 전투
- 초반 burst
- Records가 쌓이기 전 평범한 성능

### Records 획득
- 적 최초 처치: +1
- 각 아군이 전투 중 처음 HP 30% 이하 진입: +1
- 아군 revive 발생: +2
- 실제 아군 사망: +2
- boss phase 전환: +1
- 같은 사건 중복 farming 금지
- 최대 5

### Kit
**Basic · 잔향**
- 단일 90%
- Record 1당 피해 +5%
- Record 소비 없음

**Active A · 조문**
- CD2
- 단일 150%
- 최대 Record 3개 소비
- 소비 1개당 +30%p
- 3개 소비 시 총 240%

**Active B · 장송 명령**
- CD3
- 단일 130%
- 대상 HP 30% 이하이면 205%
- 처치 시:
  - Record +1
  - Gauge +150

**Passive · 전투 기록**
- 위 사건들을 Records로 변환

**Passive · 마지막 페이지**
- 전투당 1회
- 모르웬 전투불능 후 다른 unit regular action 2회가 끝나면 MaxHP 35%로 자가 부활
- Records 유지

**Awakening · 다시 쓰는 결말**
- 자가 부활 시:
  - Record +2
  - Gauge +350
  - 다음 Active는 Record를 소비하지 않고 보너스만 계산
- 전투당 1회

### 시각
- Records를 화면 전체 카드로 만들지 않음
- portrait 옆 작은 5칸 기록 indicator
- 사건 발생 시 짧은 glyph 하나가 기록으로 흡수
- 부활은 어두운 페이지/잉크 motif를 3D VFX로 단순화

---

## 8. P07 마리온 — 계약수 동반자

**한 문장:** 별도 full turn을 하나 더 얻는 소환사가 아니라, 계약수가 마리온의 행동에 반응하며 공격과 보호를 수행하는 partner controller.

Rarity: ★4  
Role: Partner / Utility / Combo  
SPD target: 100  
Signature: **Bond 0~100**

### 핵심 원칙
- 계약수는 전투 시작부터 존재
- 별도 regular Turn Gauge를 갖지 않음
- 마리온 action/reaction에만 행동
- targetable companion으로 둘 수 있지만 scheduler actor가 아님
- 별도 full action body를 통해 파티 행동권을 공짜로 늘리지 않음

### 계약수 baseline
- HP: 마리온 MaxHP 45%
- ATK: 마리온 ATK 70%
- DEF: 마리온 DEF 80%
- down 가능
- down 중 partner attack/guard 불가

### Bond 획득
- Basic command: +15
- partner가 damage를 대신 받음: +20
- Active utility 성공: +20
- 최대 100

### Kit
**Basic · 공명 명령**
- 마리온 단일 80%
- 계약수 생존 시 계약수 55% follow-up
- Bond +15

**Active A · 수호 명령**
- CD2
- 다른 아군 1명
- 계약수가 대상의 다음 단일 direct hit 50%를 대신 받음
- 계약수에게 자신의 MaxHP 10% Barrier
- redirect가 실제 발생하면 Bond +20

**Active B · 합동 돌진**
- CD3
- 마리온 110%
- 계약수 생존 시 계약수 90% follow-up
- Bond 50 이상이면 50 소비:
  - 계약수 follow-up 90% → 130%
- 계약수 down이면 마리온 단독 145%

**Passive · 끊기지 않은 계약**
- 전투 시작 시 계약수 자동 생성
- 별도 소환 버튼 없음

**Awakening · 두 번째 계약**
- 계약수 전투불능 시 전투당 1회
- 마리온의 다음 regular action 종료 후 계약수 HP 50%로 복귀
- Bond 50 획득

### 시각
- 계약수는 별도 캐릭터만큼 중요한 모델/animation 필요
- 항상 마리온 근처 같은 위치에 붙어 있지 않고 formation 내 partner anchor 사용
- follow-up/guard 시 실제 이동

---

## 9. P08 라제 — 과열 광전사

**한 문장:** HP를 깎고 피해를 주고받아 Fury를 올린 뒤 짧은 폭주 구간을 만드는 저희귀도 risk DPS.

Rarity: ★3  
Role: Risk DPS / Fury / Burst  
SPD target: 103  
Signature: **Fury 0~100**

### 강점
- ★3 중 높은 공격 ceiling
- healer/guard와의 조합
- 짧은 burst

### 약점
- 안정성
- HP 관리 실패
- 장기 폭주 유지 불가

### Fury 획득
- Basic hit: +15
- 피의 돌진 사용: +30
- 적 direct hit를 받음: +10, 적 행동당 1회
- HP 50% 이하에서 위 획득 +5 추가
- 최대 100

### Kit
**Basic · 난격**
- 단일 95%
- Fury +15
- Fury 80 이상이면 20% 추가 hit

**Active A · 피의 돌진**
- CD2
- 현재 HP 10% 소모, 최소 1
- 단일 180%
- Fury +30

**Active B · 과열**
- CD3
- Fury 60 소비
- 2 regular action 동안:
  - ATK +20%
  - SPD +10%
  - DEF -15%
- Fury 60 미만이면 사용 불가

**Passive · 전투열**
- direct hit를 주고받으며 Fury 획득
- HP 50% 이하에서 Fury gain 증가
- 단순 “저HP ATK +30%” 상시 passive는 사용하지 않음

**Awakening · 죽지 않는 난전**
- 전투당 1회 lethal damage를 HP 1로 버팀
- Fury 100
- Gauge +250
- 다음 regular action 종료까지 받는 heal 효과 -30%
  - 즉시 완전 복구로 위험 구조가 사라지는 것을 방지

### 시각
- Fury 0~100을 빨간 화면 vignette로 표현하지 않음
- 자세/무기 motion/짧은 열기 VFX로 단계 읽힘
- Overheat는 이동/공격 animation 속도감이 실제로 변함

---

## 10. F01~F04 처리

### F01 민병 견습생
- 가챠 filler에서 제거
- 튜토리얼/지역 NPC companion 후보
- 정식 캐릭터 승격 시 이름/외형/signature mechanic 새 설계 필요

### F02 야전 견습생
- 동일
- 단순 Basic healer를 정식 roster에 남기지 않음

### F03 변경 사냥꾼
- ★3 정식 field recruit 후보
- 역할: 첫타/정찰/약점 표식 전문
- P05의 하위호환이 되지 않게 별도 niche 필요

### F04 방패 용병
- ★3 정식 field recruit 후보
- 자기 방어 + 1회 ally protection specialist
- P03처럼 장기 Guard resource를 사용하지 않음

---

## 11. Party synergy 예시

### 느린 강공 파티
브람 + 루메아 + 카이렌 + 엘리시아
- 브람/카이렌의 중요한 턴을 루메아가 당김
- 브람이 엘리시아/루메아 보호
- 카이렌이 boss 집중

### 빠른 추격 파티
리네트 + 라제 + 루메아 + 엘리시아
- 잦은 direct hit로 Sightline follow-up
- 라제 위험을 엘리시아가 구조
- 루메아가 폭주 turn을 앞당김

### 장기 boss 파티
카이렌 + 모르웬 + 브람 + 엘리시아
- Focus 장기 누적
- Records 자연 축적
- 브람/엘리시아가 comeback 시간을 확보

### partner tempo
마리온 + 리네트 + 루메아 + 브람
- partner follow-up은 별도 full turn이 아님
- 리네트 Shot trigger의 허용 source 여부는 simulator에서 별도 제한
- 무료 reaction 연쇄가 과도해지면 partner hit는 Sightline trigger에서 제외

---

## 12. 캐릭터 외형

외부 디자인/모델을 적극 조사한다.

분류:
- reference
- editable base
- direct asset
- code library
- unknown-license

주요 캐릭터를:
- vanilla player skin
- 색만 바꾼 갑옷
- particle만 다른 스킬
로 끝내지 않는다.

필수 animation 후보:
- idle
- ready
- basic
- active A
- active B
- hit
- down
- revive
- victory

중요 캐릭터는 GeckoLib 또는 적합한 authored animation pipeline을 사용한다.

## 13. Portrait

우선순위:
1. 최종 3D 모델을 고정 light/camera로 offscreen render
2. head/upper-body preset
3. 캐릭터별 pose override
4. atlas/cache 생성

자동 crop 품질이 나쁘면 사용하지 않는다.

같은 portrait를:
- party
- battle HUD
- Turn Order
- character
- result
- summon summary
에 공용한다.

## 14. 밸런스 확정 전 확인

P01~P08 모두:
- 10/20 action 성과
- 팀 contribution
- free reaction 기대값
- Gauge value
- single/multi/boss
- Auto behavior
- low rarity niche
를 simulator로 비교한 뒤 최종 수치를 고정한다.
