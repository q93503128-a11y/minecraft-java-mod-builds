# 17C — M5 PARTY FORMATION SKELETON GATE

최종 갱신: 2026-09-08
상태: **AUTO GATE PASS / VISUAL QA PENDING**

이 문서는 `17_M5_UI_VISUAL_GATE.md`에 확정된 Party Formation structural mockup을 실제 production Screen + server-authoritative progression read/write 흐름으로 연결한 첫 구현 게이트를 기록한다.

이 단계의 목표는 임시 메뉴를 하나 그리는 것이 아니다. `Roster → Active 4 slots → Selected detail` 구조를 실제 M4 저장 데이터와 연결하고, 파티 편성 변경이 클라이언트 임의 상태가 아니라 기존 server-owned `PlayerProgressStore`를 통해 저장되도록 만드는 것이다.

## 1. 정본 UI 구조

Party Formation은 다음 세 영역을 동시에 유지한다.

- **Roster**: 현재 definition registry의 캐릭터를 스캔하기 쉬운 목록으로 표시.
- **Active Party**: 실제 출전 파티 최대 4슬롯과 Squad Cost를 함께 표시.
- **Selected Detail**: 현재 선택한 캐릭터의 별/레벨/역할/스탯/속성/Basic/Skills/Burst를 고정 우측 영역에 표시.

넓은 화면에서 정보가 끝없이 벌어지지 않도록 root 폭은 최대 960 logical px로 제한한다.
480×270 이상을 지원하며, 320×180 같은 지나치게 작은 logical canvas에서는 억지로 UI를 겹쳐 그리지 않고 GUI 영역 확대 안내만 표시한다.

현재 구조는 `UiLayoutMetrics.PartyFormationLayout`의 pure layout contract로 관리한다.

## 2. 진입과 기본 UX

- 기본 키: `O`.
- Minecraft Controls에서 재지정 가능한 `KeyMapping`.
- 별도 `TURNBOUND: RE Menus` category 사용.
- `KeyConflictContext.IN_GAME`으로 게임 내 진입만 허용.
- 화면을 열 때 과거 client progression cache를 비우고 서버에 최신 progression snapshot을 요청한다.
- snapshot 도착 전에는 Loading 상태만 표시하고 임의 progression truth를 생성하지 않는다.

Roster:
- owned 캐릭터 우선 정렬.
- 별/레벨/Squad Cost를 한 줄에서 스캔 가능.
- 미해금 캐릭터도 목록에는 보이지만 `미해금/LOCKED` 상태로 표시.
- 현재 화면 높이에 따라 page size를 계산하고 이전/다음 페이지 제공.

Active Party:
- 4개 고정 슬롯.
- 선택한 owned 캐릭터를 슬롯 클릭으로 배치.
- 이미 파티에 있는 캐릭터를 다른 슬롯에 넣으면 중복을 만들지 않고 위치를 교환.
- 슬롯 비우기 / 되돌리기 / 적용 제공.

Selected Detail:
- 캐릭터 identity.
- origin/current star와 level/cap.
- role.
- server-published squad cost.
- HP / ATK / DEF / SPD / Poise.
- affinity 일부 요약.
- Basic / Skills / Burst identity.
- locked 캐릭터는 배치 불가 이유 표시.

## 3. Server-authoritative progression presentation

새 progression presentation path는 기존 M4 truth를 복제하지 않는다.

서버가 `ProgressionNetworkPayloads.CharacterView`를 만들 때:

- 캐릭터 기본 데이터는 current `DefinitionRegistry`에서 읽는다.
- 보유 여부/현재 별/레벨/현재 파티/재화/party capacity는 `PlayerProgress`에서 읽는다.
- owned 캐릭터의 표시 스탯은 기존 canonical `ProgressionRules.stats()`로 계산한다.
- level cap도 기존 `ProgressionRules.levelCap()`을 사용한다.
- client는 서버가 보내지 않은 보유 상태/스탯/Squad Cost를 독자적으로 재구성하지 않는다.

client의 `ProgressionClientState`는 presentation cache일 뿐 persistence 권위를 갖지 않는다.

## 4. 파티 변경 권위 흐름

현재 protocol은 `v6`이다.

파티 변경 한 사이클:

1. `RequestProgressC2S`로 최신 progression snapshot을 요청한다.
2. 서버가 canonical definitions + persisted `PlayerProgress`에서 `ProgressSnapshotS2C`를 작성한다.
3. client는 받은 snapshot으로만 temporary `PartyFormationDraft`를 만든다.
4. draft Squad Cost는 server-published character cost를 사용해 즉시 preview한다.
5. `Apply` 시 `SetPartyC2S(expectedParty, requestedParty)`를 보낸다.
6. 서버의 현재 persisted party가 `expectedParty`와 다르면 `STALE_PARTY`로 거부하고 최신 snapshot을 돌려준다.
7. 일치하면 기존 `PlayerProgressStore.setParty()` → `ProgressionService.setParty()`를 호출한다.
8. 서버가 다시 최대 4명 / 중복 금지 / owned / Squad Cost capacity를 검증한다.
9. 성공 시 기존 SavedData persistence 경로에 저장한다.
10. 성공/실패 결과를 포함한 최신 `ProgressSnapshotS2C`를 다시 보내 화면을 authoritative state로 refresh한다.

따라서 client-side preview가 버그나 조작으로 통과하더라도 실제 저장 권위는 서버에 있다.

`expectedParty`를 같이 보내는 이유는 화면을 오래 열어둔 사이 서버 파티가 바뀌었을 때 오래된 client draft가 새 상태를 덮어쓰는 stale-write를 막기 위해서다.

## 5. Squad Cost 처리

Squad Cost는 편성 완료 후 뒤늦게 실패시키는 정보가 아니다.

현재 Screen은:
- header에서 `현재 draft cost / capacity`를 계속 표시.
- 초과 시 warning copy 표시.
- 초과 상태에서는 Apply 비활성.
- client preview는 server-published cost만 사용.
- 서버는 적용 시 기존 canonical `ProgressionService`로 다시 계산하고 재검증.

즉 UX preview와 server truth를 분리하되 서로 다른 규칙을 새로 만들지 않는다.

## 6. 자동 계약

추가된 테스트:

### Layout
- 480×270.
- 640×360.
- 1280×720.
- 1920×1080.
- root/header/tabs/roster/active/detail/footer bounds.
- roster / active / detail 상호 비중첩.
- minimum roster >=160 logical px.
- minimum active >=110 logical px.
- minimum selected detail >=150 logical px.
- wide screen root max width 960.
- 320×180 unsupported guard.

### Progression presentation / draft
- `SetPartyC2S` expected/requested composition round-trip.
- progression snapshot owned/locked/star/level/stats/presentation facts round-trip.
- 기존 캐릭터 재배치 시 duplicate가 아니라 swap.
- remove 동작.
- server-published cost 기반 preview.
- capacity 초과 client submit 차단.
- unowned character client submit 차단.

### Localization
- `en_us` / `ko_kr` exact key parity.
- Party Formation interaction/result copy nonblank.
- VANGUARD / BREAKER / STRIKER / CONTROLLER / SUPPORT role copy.
- NORMAL / WEAK / RESIST / IMMUNE affinity grade copy.

## 7. CI에서 발견한 실제 문제와 수정

첫 CI에서는 설계 회귀가 아니라 Minecraft/NeoForge 26.2 API 적응 오류 6개가 발견되었다.

- 새 `CustomPacketPayload` 3종의 `type()` override 누락.
- 존재하지 않는 `minecraft.gui.getScreen()` 호출 1개.
- 존재하지 않는 `ServerPlayer.getServer()` 호출 2개.

수정:
- payload 3종에 실제 `TYPE` 반환 구현.
- 화면 진입 제한은 이미 `KeyConflictContext.IN_GAME`이 담당하므로 잘못된 GUI getter 의존성 제거.
- server 획득은 `player.level().getServer()` 경로로 교정.

테스트를 삭제하거나 조건을 완화하지 않았다.

## 8. 자동 검증 결과

검증 코드 commit:
- `08b4eba3f23ed70127ba9f72c665c0a8e06caf58`

GitHub Actions:
- workflow: `Build turnbound-re`
- Run: `34173238769`
- result: **SUCCESS**
- Java: Temurin 25.0.4+1
- Gradle: 9.2.1
- NeoForge: 26.2.0.38-beta
- dependency resolution + clean build: **PASS**
- compileJava / compileTestJava / 전체 JUnit: **PASS**
- production JAR verify: **PASS**
- artifact upload: **PASS**
- JAR: `turnbound_re-0.1.0-alpha.1.jar`
- JAR SHA-256: `0c0f378c88c6c54998af7f5ce692305eb228635007882a07129c1cc9ec280dd6`

## 9. 현재 판정

### PARTY FORMATION STRUCTURE + AUTHORITY AUTO GATE: PASS

완료:
- 실제 server progression snapshot.
- real M4 PlayerProgress 연동.
- Roster / Active 4 / Selected detail 구조.
- configurable O key.
- live Squad Cost preview.
- locked visibility + assignment rejection.
- swap/remove/reset/apply.
- stale write rejection.
- final server revalidation + persistence.
- fresh snapshot reconciliation.
- protocol v6.
- EN/KO presentation.
- supported logical canvas layout contracts.
- Java 25 / NeoForge 26.2 clean compile + JUnit + JAR verify.

### 아직 PASS가 아닌 것

- 실제 Minecraft 화면 screenshot quality.
- GUI Scale별 text clipping / scan distance / button density 체감.
- 실제 3D entity preview.
- `Overview / Skills / Growth` 실제 interactive tabs/panes.
- Growth action의 level-up / ascend server write UI.
- 캐릭터 고유 portrait/icon/final frame asset.
- final production sprite/icon source/license gate.
- animation/transition timing.

따라서 이 배치가 성공했다고 M5 전체 production visual PASS를 선언하지 않는다.

## 10. 다음 시작점

다음 M5 production UI는 Party Formation의 선택 context를 버리지 않고 **Character Overview → Skills → Growth detail panes**를 실제로 구현한다.

우선순위:
1. selected character context와 Overview/Skills/Growth tab state를 pure presentation state로 분리.
2. Overview는 현재 server snapshot 사실을 더 읽기 좋은 계층으로 정리.
3. Skills는 action 정의의 실제 설명/target/cost/effect facts를 server/client presentation 계약으로 전달.
4. Growth는 현재 level/star/cost/next-stat preview를 표시하되, 실제 level-up/ascend는 기존 server `PlayerProgressStore`를 호출하는 별도 C2S write path로 연결.
5. 마지막에 실제 Minecraft screenshot audit에서 density/spacing/asset 품질을 조정한다.
