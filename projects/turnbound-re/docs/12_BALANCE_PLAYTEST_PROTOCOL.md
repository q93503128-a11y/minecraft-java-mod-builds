# 12 — BALANCE & PLAYTEST PROTOCOL

밸런스는 '느낌상 세다'를 데이터로 바꾸기 위한 문서다.

## 1. 전투별 기록
- encounter id / seed.
- party ids, origin/current star, level.
- 총 cycle/turn/실시간 초.
- 받은/가한 damage.
- Poise damage.
- EXPOSED 횟수/공격 창 활용률.
- 위험 Intent 중 cancel/downgrade 비율.
- Energy 생성/소비/overflow.
- KO/회복/Guard 횟수.
- 보상 가치.

## 2. 캐릭터 지표
DPR 하나로 평가하지 않는다.
- HP damage per action.
- Poise damage per action.
- survival turns.
- party damage prevented.
- buff/debuff value.
- Energy efficiency.
- EXPOSED window contribution.
- squad cost efficiency.

## 3. 목표
일반전에서 플레이어가 최소 한 번 이상 Intent와 Poise를 의식할 이유가 있어야 한다. 약점 무시 basic spam이 대부분의 전투에서 최선이면 시스템 실패다.

## 4. 저태생 검증
- 동일 level/currentStar 조건에서 저태생이 평균 raw stat으로 고태생을 역전하도록 의도하지 않는다.
- 대신 12 cost 편성에서 저태생 포함 파티가 특정 전투에서 합리적인 최적/준최적 선택이 되는 사례가 있어야 한다.
- 사용률이 0에 가까운 캐릭터는 단순 stat buff보다 역할/비용/상호작용을 먼저 점검한다.

## 5. 보상 검증
`reward per minute`를 Minecraft 생활 활동과 비교한다. 턴제 전투가 긴데 드롭이 바닐라 수준이면 실패다. 반대로 모든 다른 활동을 무의미하게 만들 정도로 높아도 실패다.

## 6. 난이도
난이도 증가는 단순 HP 증가보다:
- 읽어야 할 Intent 조합.
- 서로 보완하는 적 composition.
- Poise window timing.
- phase mechanic.
순으로 우선한다.

## 7. 변경 규율
한 playtest batch에서 핵심 변수 여러 개를 동시에 크게 바꾸지 않는다. tuning commit에는 변경 이유와 기대 지표를 기록한다.
