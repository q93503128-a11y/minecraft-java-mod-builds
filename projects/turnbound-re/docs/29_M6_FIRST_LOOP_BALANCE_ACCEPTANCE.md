# 29 — M6 FIRST LOOP BALANCE ACCEPTANCE

이 문서는 `HUB_01 -> REGION_01 -> patrol -> 준비 -> rift_elite -> reward -> HUB_01` 첫 사이클의 **자동 사전 밸런스 계약**을 기록한다.

이 gate의 목적은 실제 플레이 감각을 숫자로 대신하는 것이 아니다. 수동 통합 테스트에 들어가기 전에 명백한 경제 단절, 과도한 반복 요구, 첫 지역과 맞지 않는 적 스케일을 제거하는 것이다.

## 1. 시작 상태

신규 플레이어는 현재 다음 상태로 시작한다.

- Coin `0`
- Essence `0`
- starter party: Zombie / Skeleton / Spider / Creeper
- 모든 starter는 Lv1, 각자의 origin star 유지

따라서 첫 장비와 성장은 REGION_01의 실제 전투 보상에서 시작되어야 한다.

## 2. 첫 순찰 보상과 선택

`debug_overworld_patrol`의 보장 최소 보상:

- Coin `60`
- Essence `20`

첫 장비 비용:

- Iron Bulwark Lv1: Coin `60` + Iron Ingot `6`
- Copper Edge Lv1: Coin `60` + Copper Ingot `6`
- Golden Heart Lv1: Coin `80` + Gold Ingot `4`

starter 전원 Lv1 -> Lv2 비용 합계:

- Coin `59`
- Essence `32`

따라서 최소 보상 기준으로 첫 순찰 1회 뒤에는 장비와 성장 사이에 실제 선택이 생긴다. 한 번의 최소 보상으로 `60 Coin 장비 + 전원 Lv2`를 동시에 살 수는 없다.

두 번의 최소 순찰 보상은 Coin `120` / Essence `40`이다. 이는 `60 Coin 첫 장비 1개 + starter 전원 Lv2`의 Coin `119` / Essence `32` 준비선을 충족한다.

즉 첫 elite를 보기 위해 장시간 동일 순찰을 반복하도록 요구하지 않는다.

## 3. authored quarry 재료 예산

첫 quarry의 보수적 one-pass 기준:

- Coal Ore `8`
- Copper Ore `10`
- Iron Ore `8`
- Gold Ore `4`

Fortune 같은 외부 증폭은 계산에 넣지 않는다.

이 예산으로:

- Iron Bulwark `6 Iron`을 만든 뒤에도 Iron `2`가 남아 Iron Reinforcement 준비물 1개를 확보할 수 있다.
- Copper Edge는 `6 Copper`로 만들 수 있고 Iron은 전투 준비물로 그대로 남는다.
- Coal이 존재하므로 첫 제련 루프가 외부 연료 획득에 막히지 않는다.
- Golden Heart는 재료는 가능하지만 Coin `80`이라 첫 최소 patrol 직후 항상 보장되는 선택은 아니다. 이를 상위 대안으로 둔다.

## 4. 첫 rift elite 재조정

이전 `debug_rift_elite` 레벨:

- Creeper 24
- Blaze 30
- Witch 30
- Enderman 36

첫 REGION_01 목표로서는 late-game 스케일이어서 폐기했다. starter가 첫 임무를 끝내기 전에 과도한 순찰 반복을 요구하는 구조였기 때문이다.

현재 레벨:

- Creeper Lv4 / 3★
- Blaze Lv3 / 4★
- Witch Lv3 / 4★
- Enderman Lv4 / 5★

높은 origin star 자체가 elite 정체성을 유지하므로, 높은 수십 레벨을 추가로 겹치지 않는다.

## 5. 구조적 readiness envelope

자동 acceptance의 준비 기준은 다음과 같다.

- starter 4명 전원 Lv2
- Zombie에 Iron Bulwark Lv1 장착
- Iron Reinforcement battle preparation 사용

현재 정수 스탯 합계 기준:

### Prepared player
- HP `487`
- ATK `152`
- DEF `89`
- SPD `100`
- POISE `338`

### Rift Vanguard
- HP `604`
- ATK `218`
- DEF `110`
- SPD `116`
- POISE `339`

적은 HP/ATK/DEF에서 준비된 starter보다 강하게 유지한다. 동시에 첫 지역에서 벗어난 수치 폭주를 막기 위해 자동 ceiling을 둔다.

- HP <= prepared player의 `130%`
- ATK <= `150%`
- DEF <= `130%`
- SPD <= `120%`
- POISE <= `110%`

이 비율은 final balance가 아니라 **첫 지역 스케일 이탈 방지선**이다. 실제 난이도는 action kit, Intent, Poise 대응, 타깃 선택과 체감 전투 시간까지 포함한 playtest로 조정한다.

## 6. Elite 보상은 다음 선택으로 연결

`debug_rift_elite` 보장 최소 보상:

- Coin `180`
- Essence `60`

starter 전원 Lv2 -> Lv3 비용 합계:

- Coin `72`
- Essence `41`

따라서 첫 elite 승리는 최소 보상만으로도 다음 파티 성장 단계를 열고, 남은 Coin은 장비/기타 성장 선택에 다시 사용할 수 있다.

첫 임무 보상이 단순 완료 표시로 끝나지 않고 다음 성장 선택으로 연결된다.

## 7. 자동 acceptance

`M6FirstExpeditionBalanceAcceptanceTest`가 production definitions를 직접 읽어 다음을 잠근다.

- patrol 보장 최소 Coin/Essence
- 첫 quarry + patrol 최소 보상으로 Iron Bulwark/Copper Edge가 실제 첫 장비 선택인지
- 첫 patrol은 gear-vs-growth 선택을 남기는지
- 두 최소 patrol이면 gear 1개 + starter 전원 Lv2 준비선이 가능한지
- 첫 gear 선택 후 Iron Reinforcement 준비물 경로가 남는지
- quarry에 제련 연료가 존재하는지
- first elite roster level이 `[4, 3, 3, 4]`인지
- 실제 Progression/Equipment/Preparation 공식을 적용한 enemy/player aggregate threat ceiling
- elite 최소 reward가 starter 전원 Lv2 -> Lv3 비용을 충족하는지

## 8. 검증

Build turnbound-re #233 / run `34793967823`:

- Java 25 toolchain: PASS
- clean build: PASS
- 전체 JUnit: PASS
- first-loop balance acceptance: PASS
- production JAR verify: PASS
- artifact upload: PASS

## 9. 아직 자동으로 증명하지 못하는 것

현재 상태는 `PLAYTESTED`가 아니다.

다음 항목은 실제 Minecraft 통합 테스트에서만 확정한다.

- Hub -> REGION 이동 시간이 지루하지 않은가
- 채광/제련/장비 제작 시간이 전투 템포와 맞는가
- patrol 1~2회가 실제로 적당한가
- first elite가 수치상 가능해도 action/Intent 조합 때문에 불합리하지 않은가
- 전투 시간이 너무 길거나 너무 짧지 않은가
- waypoint/광산/농장/강/patrol/rift landmark가 실제 화면에서 즉시 읽히는가
- Equipment UI / Battle HUD / Battle Result의 가독성과 동선
- 보상 후 Hub로 돌아가고 다시 성장시키는 흐름이 자연스러운가

이제 M6의 다음 gate는 더 많은 backend 기능이 아니라 **실제 Minecraft 첫 사이클 통합 playtest + screenshot visual audit**이다.
