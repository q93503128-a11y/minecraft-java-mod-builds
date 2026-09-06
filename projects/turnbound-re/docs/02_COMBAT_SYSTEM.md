# 02 — COMBAT SYSTEM

## 1. 전투 목표
빠르게 메뉴를 누르는 것이 아니라 `읽기 → 대응 → 기회 만들기 → 연계`가 한 사이클 안에 반복되게 한다.

## 2. 기본 구성
- 플레이어 활성 파티: 최대 4.
- 적: 일반적으로 1~5. 보스 예외 가능.
- 모든 전투 결과는 서버 권한.
- 한 actor는 기본적으로 한 cycle에 한 번 행동.

## 3. 상태기계
```text
NOT_IN_BATTLE
 -> ENCOUNTER_OPEN
 -> INTRO
 -> ACTOR_READY
 -> AWAIT_COMMAND        (플레이어 actor)
 -> RESOLVING
 -> CHECK_END
 -> ACTOR_READY          (다음 actor)
 -> VICTORY | DEFEAT
 -> REWARD
 -> CLEANUP
 -> NOT_IN_BATTLE
```
적 actor는 `ACTOR_READY -> RESOLVING`에서 서버 AI가 명령을 선택한다.

각 BattleInstance는 `battleId`, `seed`, `revision`, `cycle`, `actorOrder`, `participants`, `eventLog`를 가진다.

## 4. Initiative
기본 순서는 SPD 내림차순이다. 동일 SPD는 battle 생성 시 고정된 stable tie-break(`participantOrdinal`)로 결정한다. 매 cycle 순서를 다시 계산하되 동일 입력/seed에서 동일 결과가 나와야 한다.

행동에는 `priority`가 있으며 특수 즉시 행동이 필요할 때만 사용한다. 초기 코어는 priority 남용을 금지한다.

## 5. Enemy Intent
적은 자신의 차례 이전에 다음 행동 의도를 공개한다.
Intent 최소 정보:
- 공격/방어/버프/디버프/특수 중 유형.
- 예상 대상 범주: 단일/전체/자기/무작위 등.
- 위험도: NORMAL / DANGEROUS / ULTIMATE.
- Poise 붕괴 시 cancel 가능한지.

Intent는 장식 정보가 아니라 실제 다음 행동과 일치해야 한다. AI가 다른 행동으로 바꾸면 `INTENT_CHANGED` 이벤트를 먼저 발생시킨다.

## 6. Affinity
대미지 tag:
`MELEE`, `PROJECTILE`, `FIRE`, `BLAST`, `ARCANE`, `VOID`.

기본 배율:
- WEAK: 1.25
- NORMAL: 1.00
- RESIST: 0.75
- IMMUNE: 0.00

각 캐릭터가 모든 tag에 명시적 affinity를 가진다. 누락은 validation error로 처리한다.

## 7. Poise & EXPOSED
모든 전투 캐릭터는 `poiseMax`를 가진다. 공격은 `poisePower`를 가진다.
- NORMAL/RESIST/IMMUNE의 Poise 배율 기본 1.0/0.75/0.
- WEAK 적중의 Poise 피해 기본 x1.50.
- Poise가 0 이하가 되면 즉시 `EXPOSED`.

EXPOSED 기본 규칙:
1. 해당 적의 예약 Intent가 `breakCancelable=true`이면 취소. 아니면 `breakDowngradeAction`이 있으면 약화 행동으로 치환.
2. EXPOSED 동안 받는 HP damage x1.20.
3. EXPOSED는 **그 대상의 다음 turn 시작 직전까지** 유지한다. 대상의 turn이 시작되면서 EXPOSED를 제거하고 Poise를 최대치로 회복한 뒤 행동한다. 취소된 Intent는 그 turn에 `RECOVER` 행동을 사용하여 실질적으로 한 행동 창을 잃는다.
4. 회복 직후 `POISE_GUARD` 1 turn을 부여하여 Poise 피해 x0.5. 보스는 데이터로 더 강한 보호 가능.

이 구조는 영구 stun-lock을 막으면서 파티가 한 번의 명확한 공격 창을 얻도록 한다.

## 8. Energy
캐릭터별 0~100.
- Basic Attack: 기본 +10.
- Guard: 기본 +15.
- 일반 skill: 보통 20~60 소모.
- Burst: 100 소모를 기본 계약으로 사용하되 캐릭터 데이터가 명시적으로 다를 수 있다.
- Energy는 전투 종료 시 기본 0으로 초기화. 추후 pre-charge는 별도 시스템으로만 허용.

## 9. 기본 명령
- **Basic Attack**: 피해 + Energy 생성.
- **Skill**: 캐릭터 고유 행동.
- **Guard**: actor의 다음 turn 시작까지 받는 최종 피해 x0.5, Energy +15. 중첩 불가.
- **Burst**: Energy 100 사용 고유 필살기.
- **Inspect**: 턴 소비 없음, 알려진 affinity/status/intent 설명.

아이템 명령은 Vertical Slice 이후 별도 action source로 추가할 수 있다.

## 10. Damage 공식
```text
raw = hpPower * ATK / (DEF + 100)
mult = affinity * critical * exposed * variance * otherModifiers
finalDamage = floor(max(1, raw * mult))
```
- `hpPower=100`이 기준 공격력 계수.
- 기본 crit chance 0.05, crit multiplier 1.50.
- deterministic variance: seed 기반 [0.95, 1.05].
- IMMUNE는 예외적으로 0 damage이며 `max(1)`을 적용하지 않는다.
- modifier 적용 순서는 코드에서 고정하고 event log에 breakdown을 기록한다.

예: ATK 100, DEF 100, hpPower 100, 기타 배율 1이면 raw 50.

## 11. Healing
기본 회복 공식은 `healPower * HEALING_STAT_FACTOR` 같은 별도 숨은 능력치를 만들지 않는다. 초기에는 skill 정의의 flat/scaled component를 명시한다.
```text
heal = floor(flatHeal + healPower * ATK / 100)
```
회복에는 crit 없음이 기본이다.

## 12. Status
StatusDefinition은 최소:
- id
- polarity
- durationUnit(`TURN`/`CYCLE`)
- maxStacks
- refreshRule
- dispelTags
- hooks
을 가진다.

초기 공용 상태: `GUARD`, `EXPOSED`, `POISE_GUARD`, `BURN`, `SLOW`, `ATK_UP`, `DEF_DOWN`. 캐릭터별 고유 상태는 데이터로 추가한다.

## 13. Targeting
서버가 다음을 검증한다.
- battle/revision 일치.
- 현재 actor 일치.
- action 보유/사용 가능.
- energy/cooldown/상태 조건.
- target count와 team 관계.
- 살아 있는 유효 참가자.
- 재전송/중복 명령이 아님.
실패는 상태를 바꾸지 않고 오류 코드만 반환한다.

## 14. 승패
- VICTORY: 적 진영의 전투 지속 가능 actor가 0.
- DEFEAT: 플레이어 파티의 전투 지속 가능 actor가 0.
- flee는 초기 Vertical Slice 범위 밖. 추후 encounter별 허용 가능.

## 15. Boss 규칙
보스는 단순 HP 덩어리가 아니다.
- 2개 이상 Poise segment 또는 phase별 Poise 재설정 가능.
- phase 전환 이벤트와 Intent가 명확해야 함.
- EXPOSED 후 보호를 강화할 수 있음.
- 완전 면역을 남발하지 말고 준비/타이밍으로 대응 가능하게 설계.

## 16. 반응형 입력
실시간 parry/QTE는 코어 규칙이 아니다. 추후 실험 기능으로 격리한다. Minecraft 네트워크 지연 때문에 성공 판정이 핵심 밸런스를 좌우하지 않게 한다.
