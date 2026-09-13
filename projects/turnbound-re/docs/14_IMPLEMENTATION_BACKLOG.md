# 14 — IMPLEMENTATION BACKLOG

이 순서는 '예쁜 화면부터' 만들지 않고 위험한 기술/게임성 가정을 먼저 검증하기 위한 기본 개발 순서다.

## M0 — Bootstrap & Contracts
상태: **AUTOMATED PASS**
### 작업
- NeoForge 프로젝트 scaffold를 공용 BUILD_STANDARD 기준으로 생성.
- mod id `turnbound_re`.
- data definition Codec/registry/validator.
- vanilla Mob coverage validator.
- debug commands skeleton.
- pure battle test module.
### PASS
clean build, JAR verify, invalid data test, 미분류 mob test가 동작.

## M1 — Deterministic Battle Core
상태: **AUTOMATED PASS**
### 작업
- BattleInstance/state machine.
- initiative.
- command validation/revision.
- Damage/Affinity.
- Poise/EXPOSED/POISE_GUARD.
- Energy/Guard/status.
- Intent/AI stub.
- victory/defeat/reward event.
### PASS
순수 unit test로 동일 seed/commands 동일 event stream. soft-lock 상태전이 없음.

## M2 — Minecraft Adapter & Network
상태: **AUTOMATED GATE PASS / MANUAL CLIENT GATE PENDING**
### 작업
- Entity participant binding.
- world AI/damage isolation.
- C2S command/S2C snapshot+events.
- production battle presentation cache/HUD.
- cleanup/disconnect/dimension guards.
### PASS
자동 20-cycle soak는 통과. 실제 클라이언트 debug encounter 20회/orphan battle 0 수동 gate는 최종 통합 테스트 때 함께 수행.

## M3 — Representative Content
상태: **AUTOMATED PASS / FINAL BALANCE NOT LOCKED**
### 작업
- 대표 바닐라 8종.
- action 20개 이상.
- 일반 Encounter + elite/boss.
- 각 role/affinity/status/Intent 케이스 검증.
### PASS
Basic spam보다 Poise/Intent 대응이 유리한 상황이 명확히 존재하고 전투 로그로 확인.

## M4 — Progression & Reward
상태: **FULL AUTOMATED PASS / RUNTIME PERSISTENCE MANUAL CHECK DEFERRED**
### 작업
- character ownership.
- origin/current star/level caps.
- ascension.
- squad cost.
- Coin/Essence/Shard.
- save/version.
- Encounter reward.
- authored Encounter launcher.
- immutable encounter/reward snapshot.
- common VICTORY→REWARD persisted settlement.
### PASS
production authored Encounter를 직접 로드해 실제 data action 전투→승리→Encounter reward→one-shot claim→PlayerProgress 변경→SavedData Codec round-trip→unlock/level cap/ascend/party/Squad Cost→reload-equivalent round-trip까지 자동 검증. persistence callback 실패는 claim을 소비하지 않음.

실제 Minecraft world save/reload/재접속 체감 검증은 M2 manual client gate와 함께 최종 완성본에서 수행한다.

## M5 — Production UI / Presentation
상태: **AUTOMATED IMPLEMENTATION GATE PASS / SCREENSHOT VISUAL AUDIT PENDING**

완료된 자동 구현 범위:
- production Battle HUD / command input / world-first target chooser.
- Party Formation / Character Overview / Skills / Growth.
- server-authoritative progression write/reconciliation.
- selected-character adaptive 3D entity preview.
- authoritative Battle Result / Reward / ACK / world return lifecycle.
- EN/KO localization parity.
- shared `UiVisualLanguage` 및 외부 CC0 UI asset 적용.
- logical battle-stage participant rendering.
- data-driven action timeline, impact/travel accent.
- Skeleton / Enderman 대표 3D character presentation pass.

남은 gate:
- 실제 Minecraft screenshot quality.
- GUI Scale별 clipping/가독성/시선 이동.
- 480×270 실화면 밀도.
- final frame/icon/sprite source/license.
- animation/transition/reward reveal/audio timing.
- representative 3D model pose/centering/실제 체감.
- Skeleton aim / Enderman phase가 실제 플레이에서 충분히 읽히는지 검증.

**중요:** screenshot/reference 비교 전 M5 production visual PASS를 선언하지 않는다.

## M6 — World & Life Loop
상태: **IN PROGRESS — AUTHORED ENCOUNTER ENTRY/LIFECYCLE AUTO VERIFIED**

완료:
- authored Encounter를 메뉴/월드 anchor에서 여는 server-authoritative 진입 경로.
- world anchor locator/dimension/entity/range/encounter identity 최종 서버 재검증.
- anchor/Encounter 양쪽 `repeatable` 계약.
- 비반복 anchor 승리 시 reward + completion을 하나의 immutable save write로 정산.
- `PlayerProgress` schema 2의 `completedEncounterLocators`.
- schema 1 backward decode.
- 완료한 one-time anchor preview/confirm 차단.
- 반복형 encounter는 기존 farming loop 유지.
- 현재 대표 두 anchor는 의도대로 repeatable 유지.
- 상세 정본: `19_M6_WORLD_ENCOUNTER_LIFECYCLE.md`.

남음:
- authored hub/region prototype의 실제 월드 배치와 시각 gate.
- mining/farming/fishing/crafting 산출물을 성장 루프에 연결.
- fast travel/exploration/quest hooks.
- production non-repeatable anchor를 실제 콘텐츠로 배치한 뒤 playtest.

### PASS
각 활동의 산출이 성장 루프에 실제 사용되고 막힌 경로가 없으며, fixed-world encounter lifecycle이 실제 Minecraft 플레이에서도 의도대로 작동해야 한다.

## M7 — Full Vanilla Roster
### 작업
- 모든 eligible 바닐라 Mob STUB 제거.
- family별 kit/balance.
- 획득처.
- presentation status 추적.
### PASS
eligible 전수 PLAYABLE 이상, 미분류 0.

## M8 — Production Balance & Release Hardening
- difficulty curve.
- reward economy.
- accessibility.
- save migration.
- performance/network soak.
- dedicated server/multiplayer verification 가능 시 수행.
- 최종 visual regression.

## 지금 바로 할 일 — 2026-09-13 최신

마지막 TURNBOUND 코드 단위는 `turnbound-re: persist one-time world encounter clears`이며 Build turnbound-re #209가 clean build/JUnit/JAR verify까지 성공했다.

다음 코드 작업은 새 generic UI나 무작정 roster VFX 확장이 아니라 **authoritative action impact와 HP/Poise/EXPOSED/defeat 표시 타이밍 동기화**다.

현재 문제:
- 서버 snapshot은 행동 해결 직후 최종 HP/Poise를 authoritative하게 전달한다.
- client action presentation은 WINDUP → IMPACT → RECOVERY로 시각 타이밍을 늦춘다.
- 따라서 실제 타격 연출보다 HP/Poise bar 또는 EXPOSED/defeat 상태가 먼저 바뀌어 보일 수 있다.

구현 원칙:
1. 전투 결과를 client가 예측하지 않는다.
2. 이미 받은 이전/최종 authoritative snapshot만 presentation 용도로 사용한다.
3. WINDUP 동안 이전 authoritative 표시값 유지.
4. IMPACT에서 이전→최종 값을 짧게 easing.
5. RECOVERY 끝에서는 최종 authoritative 값과 정확히 일치.
6. heal / Poise damage / Poise break / EXPOSED / defeat도 같은 타이밍 언어를 따른다.
7. multi-hit은 서버가 제공하지 않은 hit별 수치를 창작하지 않는다. aggregate previous→final만 안전하게 연출한다.
8. battleId/revision/cue 변경 시 stale staged state를 즉시 reset한다.
9. non-target/no-cue participant는 불필요하게 지연하지 않는다.

필수 테스트:
- damage.
- healing.
- Poise damage.
- Poise break + EXPOSED.
- defeat.
- no cue / non-target.
- multi-target.
- battle/revision reset.
- recovery exact final.

이 코드는 전투 authority를 바꾸지 않는 presentation-only 수정이어야 한다. 의미 있는 한 단위 완료 후 관련 test + Build turnbound-re 1회만 수행한다.

그 다음에는 실제 screenshot/playtest 증거가 들어오기 전 representative visual styling을 무작정 확대하지 않는다.
