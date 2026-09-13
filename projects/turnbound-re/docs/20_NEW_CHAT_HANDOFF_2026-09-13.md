# TURNBOUND: RE — New Chat Handoff — 2026-09-13

이 문서는 새 채팅에서 현재 TURNBOUND: RE 작업을 이어가기 위한 최신 인수인계다.

## 0. 저장소

- Repo: `q93503128-a11y/minecraft-java-mod-builds`
- Branch: `main`
- Project: `projects/turnbound-re/`
- Minecraft Java: 26.2
- NeoForge: 26.2.0.38-beta
- Java: 25
- Mod ID: `turnbound_re`
- Version: `0.1.0-alpha.1`

공용 모노레포의 `main`은 다른 프로젝트 작업으로 빠르게 전진한다. 이 문서에 적힌 SHA를 최신 HEAD라고 가정하지 말고 새 채팅 시작 즉시 remote `main`을 다시 조회한다.

구 `projects/turnbound/`는 폐기 프로젝트이며 **ZERO AUTHORITY**다.

## 1. 현재 정본 우선순위

1. 현재 GitHub `main`
2. project `AGENTS.md`
3. `AGENT_RULES.md`
4. `docs/CANON.md`
5. 관련 상세 docs
6. root `docs/BUILD_STANDARD.md`
7. root `docs/QUALITY_STANDARD.md`
8. 실제 source/resources
9. 최근 playtest feedback
10. 과거 대화

현재 상태를 빠르게 복구할 때는 최소한 다음도 읽는다.

- `docs/14_IMPLEMENTATION_BACKLOG.md`
- `docs/16_CURRENT_IMPLEMENTATION_STATUS.md` — 2026-09-08 기준이라 일부 상태가 낡았음
- `docs/17_M5_UI_VISUAL_GATE.md` 및 17A~17F
- `docs/18_CHARACTER_VFX_REFERENCE_GATE.md`
- `docs/19_M6_WORLD_ENCOUNTER_LIFECYCLE.md`
- 이 파일 `docs/20_NEW_CHAT_HANDOFF_2026-09-13.md`

`14_IMPLEMENTATION_BACKLOG.md`는 2026-09-13 현재 상태에 맞게 갱신했다.

## 2. 마지막 TURNBOUND 코드 검증

마지막 의미 있는 TURNBOUND 코드 커밋:

`16c1bbcbc17b19709bfa26fcbff893a7079bfc70`

commit:

`turnbound-re: persist one-time world encounter clears`

GitHub Actions:

- Workflow: `Build turnbound-re`
- Run number: **#209**
- Run ID: **34731396578**
- Result: **SUCCESS**
- clean build/JUnit: PASS
- production JAR verify: PASS
- artifact upload: PASS

검증 상태:

- CODE REVIEWED: YES
- TESTED: YES
- BUILD VERIFIED: YES
- JAR PRODUCED / VERIFIED: YES
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO

이후 `main`에 들어온 다수 커밋은 EARTH TO STARS / Riftfrontier 등 다른 프로젝트 변경이며, 이 인수인계 작성 시점까지 TURNBOUND code를 덮은 변경은 관측되지 않았다.

문서 최신화 커밋:

`4233ecf5a4705e959bc9f287893c5e4d45d71e17`

commit:

`docs(turnbound-re): refresh backlog after world encounter lifecycle`

문서-only 변경이므로 이 커밋 때문에 TURNBOUND build를 다시 실행하지 않았다.

## 3. 현재 게임 구현 요약

핵심 루프:

Minecraft exploration/gathering/crafting → visible encounter → 4인 파티 턴 전투 → Enemy Intent 대응 → affinity/Poise 공략 → EXPOSED → 보상/성장 → 다시 탐험.

대표 production 캐릭터 8종:

- Zombie
- Skeleton
- Spider
- Creeper
- Blaze
- Witch
- Enderman
- Iron Golem

현재 40 actions / 12 statuses / normal + elite authored Encounter/Reward 세트가 있다.

M0~M4 automated gate는 닫혀 있다.

M5는 Battle HUD, command, Party Formation, Character Detail/Skills/Growth, server-authoritative growth write, 3D preview, Battle Result/Reward, visual language, battle-stage virtual entity rendering까지 자동 implementation gate가 크게 닫혔다.

그러나 실제 Minecraft screenshot/reference 비교가 아직 없으므로 **M5 production visual PASS는 아니다.**

## 4. 대표 3D character/action presentation 최신 상태

Skeleton / Enderman을 대표 vertical pair로 먼저 고도화했다.

Skeleton:

- 실제 virtual Skeleton entity가 Bow를 main hand에 장착.
- projectile action WINDUP/초기 IMPACT에서 실제 mob aggressive/aim state 사용.
- render angle로 bow/arm silhouette를 보강.
- Arrow Storm은 semantic VOLLEY travel accent를 유지.
- 예전 2D Bow/Arrow actor decoration은 제거.

Enderman:

- VOID action에서 실제 3D Enderman entity가 horizontal phase movement.
- render view angle 변화.
- curved Rift travel + target impact accent 유지.
- 예전 2D pearl/nested-frame actor decoration은 제거.

원칙:

- character identity는 3D stage entity가 담당.
- `BattleStageSignatureFx`는 attack travel/impact information만 보강.
- 실제 screenshot 없이 Blaze/Witch/Iron Golem 등으로 무작정 같은 styling을 확장하지 않는다.

Fresh Animations는 reference only다. 해당 custom-license asset/animation을 repo에 복제하지 않는다.

## 5. M6 최신 — authored world encounter lifecycle

`16c1bbcb`에서 `repeatable=false`인데도 재도전 가능했던 계약 구멍을 닫았다.

완료:

- world anchor + Encounter 양쪽 repeatable을 함께 적용.
- 둘 중 하나라도 false면 player별 one-time locator.
- completion identity는 Encounter id가 아니라 logical anchor `locator`.
- one-time anchor는 **VICTORY + reward settlement 성공 후에만** 완료.
- reward와 completed locator를 하나의 immutable `PlayerProgress` 최종 상태로 합성하여 SavedData write 1회.
- 패배는 완료 처리하지 않음.
- repeatable anchor는 completion locator를 추가하지 않음.
- `PlayerProgress` schema 2에 `completedEncounterLocators: Set<String>` 추가.
- schema 1 backward decode 유지.
- accepted progression/reward mutation은 completion locator를 보존.
- preview와 final confirm 모두 `ANCHOR_CLEARED`를 서버에서 판정.
- final confirm은 locator/dimension/entity UUID+tag/range/encounter identity/party/battle/completion을 다시 검증.
- EN/KO cleared player-facing copy.

현재 production 대표 locator 둘은 의도대로 repeatable이라 gameplay data는 바꾸지 않았다.

- `turnbound_re:region_01/overworld_patrol`
- `turnbound_re:region_01/rift_elite`

non-repeatable은 synthetic contract test로 검증한다.

## 6. 다음 실제 코드 작업 — 가장 먼저 할 것

**Authoritative action impact와 HP/Poise/EXPOSED/defeat 표시 타이밍을 동기화한다.**

현재 문제:

- server는 action resolve 직후 authoritative final snapshot을 보낸다.
- client는 별도의 WINDUP → IMPACT → RECOVERY presentation timeline으로 공격을 보여준다.
- 이 때문에 HP/Poise bar, EXPOSED, defeat가 실제 타격 frame보다 먼저 바뀌어 보일 수 있다.
- 이전 feedback 경로는 snapshot target delta를 aggregate하게 다루므로 multi-hit을 hit별 damage처럼 임의 분해하면 안 된다.

### 설계 원칙

1. combat result를 client가 예측하지 않는다.
2. authoritative **previous snapshot + final snapshot**만 이용한 presentation projection을 둔다.
3. WINDUP: affected participant의 표시 HP/Poise를 previous authoritative 값으로 유지.
4. IMPACT: previous → final을 짧게 easing.
5. RECOVERY 완료: exact final authoritative 값.
6. heal도 동일한 흐름.
7. Poise break / EXPOSED / defeat의 시각 상태도 실제 IMPACT보다 먼저 튀지 않게 한다.
8. multi-hit은 서버가 주지 않은 hit별 수치를 창작하지 않는다. aggregate previous→final transition 1회로 처리.
9. multi-target은 각 affected participant의 previous→final을 독립 처리.
10. non-target / cue 없는 변화는 필요 이상 지연하지 않는다.
11. battleId/revision/cue 변경 또는 interruption 시 stale staged state 즉시 reset.
12. `BattleInstance`, server legality, damage calculation, reward authority는 건드리지 않는 **presentation-only** 작업이어야 한다.

먼저 확인할 코드:

- `BattleActionPresentation.java`
- `BattleActionTimelineHud.java`
- `BattleStageHud.java`
- `BattleHud.java`
- `BattleClientState` 및 snapshot/event cache
- stage feedback/motion/signature helper
- 관련 M5 battle presentation tests

### 필수 테스트

- damage previous→impact→final
- healing previous→impact→final
- Poise damage
- Poise break + EXPOSED
- defeat
- no cue / non-target immediate authoritative display
- multi-target
- battleId reset
- revision reset
- cue replacement/interruption
- RECOVERY exact final

테스트를 완화해서 통과시키지 않는다.

이 작업은 state/presentation code 변경이므로 의미 있는 한 단위를 끝낸 뒤 관련 unit test + `Build turnbound-re` 1회 수행한다. 중간의 작은 수정마다 build/CI를 반복하지 않는다.

## 7. 그 이후 우선순위

위 impact/meter synchronization이 닫히면:

1. actual Minecraft screenshot/playtest가 가능해질 때 representative pair부터 visual QA.
2. Skeleton aim이 vanilla pose로 약하면 own model/GeckoLib arm/equipment animation 검토.
3. Enderman phase가 spatial motion이 아니라 단순 shaking처럼 보이면 bone pose/afterimage/model animation 검토.
4. representative pair가 실제 화면에서 통과한 뒤 Blaze/Witch/Iron Golem 등으로 character visual process 확장.
5. M6 authored hub/region 실제 월드 prototype.
6. mining/farming/fishing/crafting 산출물을 progression loop에 연결.
7. fast travel/exploration/quest hooks.
8. production non-repeatable fixed anchor 실제 배치 + playtest.
9. 이후 Full Vanilla Roster.

기능 수를 늘리기 위해 독립 메뉴/재화/잡기능을 추가하지 않는다.

## 8. 실제 통합 테스트 때 같이 볼 것

사용자는 중간 JAR 테스트를 자주 요구하지 않고, 한 번에 검토할 가치가 있는 상태에서 통합 테스트하는 방향이다.

그때 함께 확인:

- Battle/Party/Growth/Result GUI Scale.
- 1280×720 / 1920×1080 screenshot audit.
- 480×270 logical canvas 실가독성.
- Skeleton / Enderman 3D presentation.
- action impact ↔ HP/Poise/EXPOSED/defeat timing.
- M2 actual client 20 encounter / orphan battle 0.
- M4 world save/reload/reconnect persistence.
- VICTORY/DEFEAT → Result → server ACK → world return.
- world anchor distance/entity/dimension/one-time lifecycle.

실행하지 않은 항목을 PASS라고 쓰지 않는다.

## 9. shared main 작업 규칙

이 repo는 여러 Minecraft 프로젝트가 같은 `main`을 공유한다.

- 작업 시작/커밋 직전 remote main 재확인.
- 다른 프로젝트 커밋은 덮지 않는다.
- force push 금지.
- unrelated change면 최신 main 위에 재베이스/재구성.
- CI 실패 시 로그의 첫 실제 실패를 읽고 수정 후 재실행.
- 같은 실패 workflow를 원인 수정 없이 반복 실행하지 않는다.

## 10. 새 채팅 행동 지시

새 채팅에서는 계획만 길게 쓰고 멈추지 않는다.

현재 main 재확인
→ canonical docs / 이 handoff 확인
→ `16c1bbcb` 이후 TURNBOUND 변경 유무 확인
→ action impact vs meter state 실제 코드 추적
→ presentation-only staged authoritative value 구조 결정
→ 구현
→ 관련 테스트
→ 의미 있는 단위 완료 후 Build turnbound-re 1회
→ 결과를 CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED / PLAYTESTED / MULTIPLAYER TESTED로 정확히 구분
→ main 직접 반영

실제 screenshot이 없으면 visual PASS를 선언하지 않는다.
