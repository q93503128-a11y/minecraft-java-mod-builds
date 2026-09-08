# 17D — M5 CHARACTER DETAIL / SKILLS / GROWTH GATE

최종 갱신: 2026-09-08

이 문서는 `17C_M5_PARTY_FORMATION_SKELETON_GATE.md` 다음 단계인 selected-character detail flow의 현재 구현/검증 상태를 고정한다.
시각적 최종 품질 PASS 문서가 아니다. 실제 Minecraft screenshot audit 전까지 visual PASS를 선언하지 않는다.

## 1. 이번 단계 목표

Party Formation의 `Roster → Active Party → Selected Character` 문맥을 유지한 채 selected detail만 전환한다.

- Overview: 현재 서버 사실을 읽기 쉬운 정보계층으로 정리.
- Skills: 실제 `ActionDefinition`의 cost/target/power/effect 사실을 표시.
- Growth: 현재→다음 성장 결과, 비용, 차단 이유를 행동 버튼과 같은 영역에서 보여준다.
- Level Up / Ascend: client가 성장 결과를 계산하거나 저장하지 않고 기존 server-owned progression 경로를 호출한다.

## 2. 구현 상태

### 2.1 Detail tabs

- `Overview / Skills / Growth` 실제 interactive tabs.
- tab state는 `PartyFormationScreen`의 client presentation-only state.
- tab을 바꿔도 selected character, active-party draft, server snapshot 문맥은 유지.
- locked character는 identity는 보이되 growth write 불가.

### 2.2 Overview

현재 authoritative progression snapshot에서 다음을 표시한다.

- current/origin star
- level / cap
- roles
- Squad Cost
- HP / ATK / DEF / SPD / Poise
- affinities
- Basic / Skills / Burst / Passive

client는 별/레벨에서 표시 스탯을 새로 계산하지 않는다.

### 2.3 Skills

`ProgressionNetworkPayloads.ActionView`를 추가하여 서버가 current `DefinitionRegistry`의 `ActionDefinition`에서 presentation facts를 작성한다.

현재 publish 항목:

- action id / kind
- `energyDelta`
- `hpPower`
- `poisePower`
- damage tag
- target team / shape / count
- effects: type / status / value / duration / chance

Skills pane은 위 서버 사실을 표시할 뿐 실제 전투 damage 결과나 target legality를 새로 예측하지 않는다.

### 2.4 Growth preview

`GrowthView`는 서버가 기존 `ProgressionRules`와 current persisted `PlayerProgress`를 사용해 작성한다.

표시:

- current Coin / Essence
- selected-character shard balance
- Level current → next
- next-level stats
- level-up cost
- current Star → next Star
- next-star level cap
- post-ascension stats
- ascension cost
- 차단 이유

현재 차단 코드:

- `NOT_OWNED`
- `LEVEL_CAP`
- `NOT_AT_LEVEL_CAP`
- `MAX_STAR`
- `INSUFFICIENT_COIN`
- `INSUFFICIENT_ESSENCE`
- `INSUFFICIENT_SHARDS`

따라서 비용은 버튼을 누른 뒤 처음 알게 되는 surprise cost가 아니다.

### 2.5 Server-authoritative growth writes

새 `GrowthC2S`는 다음 intent만 전송한다.

- operation: `LEVEL_UP` / `ASCEND`
- character id
- expected current star
- expected current level

서버 처리:

1. persisted `PlayerProgress` 재조회.
2. character ownership 확인.
3. expected star/level과 현재 값 비교.
4. stale이면 `GROWTH_STALE` 거부 + fresh snapshot.
5. 실제 write는 기존 `PlayerProgressStore.levelUp/ascend` → `ProgressionService` 경로 사용.
6. 비용/level cap/star cap/shard 조건은 서버가 최종 재검증.
7. 성공/실패 모두 fresh authoritative snapshot으로 reconciliation.

client preview는 편의용이며 authoritative write gate가 아니다.

## 3. Protocol v7

공용 play-phase protocol은 `v7`.

기존 battle/party 계약을 유지하며 progression presentation에 다음이 추가됐다.

- `ActionView`
- `EffectView`
- `StatsView`
- `CostView`
- `GrowthView`
- `GrowthC2S`

progression snapshot wire round-trip은 action/effect/growth facts까지 검증한다.

## 4. Localization / presentation contract

EN/KO exact key parity를 유지한다.

자동 required-copy 계약에 추가:

- Overview / Skills / Growth tab labels
- skill kinds
- special effect labels
- Level Up / Ascend
- wallet / cost / current→next preview
- 모든 현재 growth disabled reason
- stale/success/server rejection feedback

## 5. 자동 검증

최종 코드 검증:

- commit: `f2983305d753913883df95bc6eb1be1eadeb81d1`
- GitHub Actions: `Build turnbound-re` run `34175475720`
- result: **SUCCESS**
- Java: Temurin 25.0.4+1
- Gradle: 9.2.1
- NeoForge: 26.2.0.38-beta
- clean build + 전체 JUnit: **PASS**
- production JAR verify: **PASS**
- artifact upload: **PASS**
- JAR: `turnbound_re-0.1.0-alpha.1.jar`
- SHA-256: `0e55eded19facae7de7fe05ec4cd2f2c1754d1fd710e19e8b3f3cf6dd09ae806`

검증 계약:

- `GrowthC2S` operation/id/expectedStar/expectedLevel round-trip.
- `ActionView` + nested `EffectView` snapshot round-trip.
- `GrowthView` + cost/stats/block state snapshot round-trip.
- blocked growth action의 client enable 상태 계약.
- Party Formation 기존 swap/remove/cost/ownership 계약 회귀.
- EN/KO exact key parity.
- character-detail/growth required copy coverage.
- 기존 M0~M5 전체 JUnit 회귀.
- JAR integrity / metadata / namespace / source exclusion / duplicate-entry 검사.

## 6. 아직 PASS가 아닌 것

- 실제 Minecraft screenshot에서 tab/button/정보밀도 품질.
- 480×270 및 실제 GUI Scale별 Growth pane clipping/가독성.
- Skills가 한 화면에 많은 action/effect를 표시할 때 실제 읽기 흐름.
- 최종 icon/frame/sprite asset quality 및 source/license 기록.
- selected character 실제 3D entity preview.
- hover/transition/micro-animation 체감.
- 실제 client에서 Level Up / Ascend 클릭 후 save/reconnect persistence 체감 검증.

위 항목은 자동 CI 성공만으로 PASS 처리하지 않는다.

## 7. 다음 시작점

selected-character interaction 구조와 server authority는 닫혔다.
다음은 기능을 더 얹기 전에 **실제 character identity presentation**을 강화한다.

우선순위:

1. Party selected detail의 3D entity preview renderer feasibility 조사/구현.
2. clipping, entity scale, rotation, mob별 bounding box 차이, performance 확인.
3. 임시 AI식 panel 장식을 늘리지 말고 현재 structural hierarchy를 유지.
4. production icon/frame/sprite는 reference/source/license gate 후 적용.
5. 이후 battle result/reward transition presentation으로 이동.
6. Battle/Party/Growth가 함께 검토할 만한 시점에 screenshot/GUI Scale/M2 20-cycle/M4 save-reconnect를 통합 수동 검사한다.
