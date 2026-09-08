# 17F — M5 BATTLE RESULT / REWARD TRANSITION GATE

최종 갱신: 2026-09-08

이 문서는 TURNBOUND: RE M5의 **전투 종료 → 결과/보상 → 월드 복귀** presentation/authority gate를 기록한다.
기획 정본을 대체하지 않으며, 전투/보상 규칙은 기존 CANON 및 M4 정본이 우선한다.

## 1. 현재 상태

상태: **AUTOMATED PRESENTATION/AUTHORITY GATE PASS / IN-GAME VISUAL QA PENDING**

자동으로 닫힌 범위:
- 서버가 작성한 VICTORY / DEFEAT 결과 payload.
- 실제 persistence 성공 뒤에만 authored victory 보상 표시.
- Coin / Essence / Character Shard의 실제 지급량과 지급 후 총량 표시.
- rewardless/debug 전투에서 가짜 보상 생성 금지.
- Continue / ESC → C2S acknowledgement → 서버 검증 → battle cleanup.
- 결과 화면 최소 logical canvas 480×270 대응.
- EN/KO copy 및 exact key parity.
- Java 25 clean build / 전체 JUnit / production JAR verify.

아직 닫히지 않은 범위:
- 실제 Minecraft screenshot 품질.
- 실제 GUI Scale별 clipping/가독성.
- 최종 result sprite/icon/frame asset과 source/license.
- 승리/패배 motion, reward reveal timing, sound/audio feedback.

따라서 이 gate 통과를 **M5 production visual PASS**로 해석하지 않는다.

## 2. UX 원칙

결과 화면의 우선순위는 다음과 같다.

1. 결과를 즉시 이해한다 — 승리/패배.
2. 무엇을 얻었는지 즉시 이해한다 — delta.
3. 현재 보유량이 어떻게 되었는지 이해한다 — total.
4. 한 번의 명확한 Continue로 월드에 복귀한다.

구조 원칙:
- Minecraft world를 완전히 가리는 거대한 메뉴가 아니라 compact result presentation을 사용한다.
- 결과/보상은 server-authored fact만 보여준다.
- client에서 reward table을 다시 읽거나 roll을 재현하지 않는다.
- 유명 게임의 proprietary art를 복제하지 않는다.
- 현재 structural pass는 Minecraft 자체 advancement 계열 sprite를 사용한다.
- 최종 미술 품질은 screenshot audit와 asset/source/license gate 뒤에 확정한다.

## 3. 서버 권위 terminal lifecycle

### Authored victory

```text
Battle VICTORY
→ BattleState.REWARD
→ BattleRewardSettlementService.settleIfReady
→ persisted PlayerProgress mutation 성공
→ RewardService.RewardGrant + persisted resulting PlayerProgress
→ BattleResultPresentationService.presentSettlement
→ ResultS2C
→ client BattleResultScreen
→ Continue 또는 ESC
→ AcknowledgeResultC2S
→ server owner/revision/outcome/phase 재검증
→ battle.cleanup()
→ BattleManager.cleanup()
→ ResultClosedS2C
→ client result state clear / world 복귀
```

중요:
- persistence가 실패하면 claim은 소비되지 않는다.
- persistence 실패 중에는 결과 화면도 열지 않는다.
- 다음 server tick에서 settlement를 재시도한다.
- result presentation이 reward 지급 성공보다 앞설 수 없다.

### Defeat / rewardless debug terminal

- terminal outcome은 표시한다.
- reward table이 없으면 임의 reward를 만들지 않는다.
- `No rewards` 상태로 명시한다.
- cleanup은 동일하게 server acknowledgement 이후 수행한다.

## 4. Protocol v9

play-phase presentation protocol은 `v9`다.

추가 payload:
- `BattleResultNetworkPayloads.ResultS2C`
- `BattleResultNetworkPayloads.AcknowledgeResultC2S`
- `BattleResultNetworkPayloads.ResultClosedS2C`

`ResultView`의 authoritative facts:
- `battleId`
- `revision`
- `outcome` (`VICTORY` / `DEFEAT`)
- `coinDelta`
- `essenceDelta`
- `coinTotal`
- `essenceTotal`
- shard별 `characterId / amount / total`

클라이언트는 위 데이터를 표시만 한다.

## 5. ACK / cleanup 계약

클라이언트는 battle ownership을 직접 제거하지 않는다.

서버 ACK 검증:
- pending result 존재 여부.
- sender가 result owner인지.
- result가 실제 publish되었는지.
- `battleId`.
- `revision`.
- `outcome`.
- battle이 terminal `REWARD` phase인지.

검증 성공 시에만:
- `BattleInstance.cleanup()`.
- `BattleManager.cleanup()`.
- entity↔battle binding 해제.
- reward/context/claim metadata 정리.

stale/비소유/잘못된 phase 요청은 cleanup하지 않는다.

## 6. Client presentation

구현:
- `BattleResultClientState` — server result/close response presentation cache.
- `BattleResultClientEvents` — 새 result가 도착하면 한 번만 결과 화면 오픈.
- `BattleResultScreen` — compact non-pausing result UI.
- `BattleResultLayout` — pure responsive layout contract.

표시:
- VICTORY / DEFEAT.
- Coin delta + total.
- Essence delta + total.
- Character Shard delta + total.
- rewardless 상태.
- Continue.
- server close 대기/거부 피드백.

ESC는 result를 임의 폐기하지 않고 Continue와 동일하게 ACK를 보낸다.
서버 close response가 성공해야 실제 화면 state가 제거된다.

## 7. Layout contract

지원 logical canvas:
- 480×270.
- 640×360.
- 1280×720.
- 1920×1080.

계약:
- minimum width 480.
- minimum height 270.
- root max width 520.
- root max height 230.
- 480×270에서도 reward region >=160px.
- header/reward/footer non-overlap.
- 320×180은 fail closed.

최초 테스트에서는 root max height 220으로 인해 480×270 reward region이 152px밖에 확보되지 않았다.
테스트 assertion을 낮추지 않고 production layout을 230으로 늘려 수정했다.

## 8. 자동 테스트

추가/강화된 계약:
- result wire exact round-trip.
- 실제 reward delta/total 보존.
- rewardless DEFEAT 명시성.
- ACK / close response round-trip.
- invalid outcome 거부.
- impossible shard delta/total 거부.
- terminal battle registry.
- entity binding이 ACK cleanup 전까지 유지됨.
- cleanup 후 terminal registry/binding 제거.
- 480×270 ~ 1920×1080 bounds.
- reward 영역 최소 높이.
- EN/KO result copy + exact key parity.

## 9. CI 기록

첫 layout gate 실패:
- Run `34186812650`.
- compile 성공.
- `M5BattleResultLayoutTest` 1건 실패.
- 원인: 최소 화면 reward 영역 152px.
- 테스트 완화 없음.
- production `MAX_ROOT_HEIGHT` 220 → 230 수정.

수정 후 성공:
- Run `34186892199` — SUCCESS.

최종 코드 위생 포함 검증:
- commit `24f203db728d7d56b64602f67af76d0fbf715bb8`.
- `Build turnbound-re` Run `34187037774` — **SUCCESS**.
- Java Temurin 25.0.4+1.
- Gradle 9.2.1.
- NeoForge 26.2.0.38-beta.
- clean build/JUnit: **PASS**.
- production JAR verify: **PASS**.
- artifact upload: **PASS**.
- JAR: `turnbound_re-0.1.0-alpha.1.jar`.
- SHA-256: `adc1dc812d980f990d14ce343f338482b87fe0a9c7d078d21c7b6923032ed92b`.

## 10. 다음 단계

Battle Result / Reward의 구조·authority 자동 gate는 닫혔다.

다음 M5 작업은 새 임시 메뉴를 늘리는 것이 아니라 **전체 production visual asset/reference + screenshot-ready polish**로 이동한다.

우선순위:
1. Battle HUD / Command / Party / Character / Growth / Result 간 시각 언어 일관성 재검수.
2. 최종 frame/icon/sprite 후보의 reference/source/license 기록.
3. motion/transition/reward reveal/audio 피드백 설계.
4. 실제 client screenshot audit 준비.
5. 이후 character exterior / skill VFX / world visual gate로 이동.

실제 integrated test 시 함께 검증:
- GUI Scale / 1280×720 / 1920×1080 screenshot 비교.
- M2 실제 client 20 encounter gate.
- M4 world save/reconnect persistence.
- 실제 VICTORY/DEFEAT → result → ACK → world 복귀.

구 TURNBOUND는 계속 ZERO AUTHORITY다.
