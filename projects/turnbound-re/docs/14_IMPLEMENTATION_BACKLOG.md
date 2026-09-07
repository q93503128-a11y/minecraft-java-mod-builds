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
- DEBUG_ONLY HUD.
- cleanup/disconnect/dimension guards.
### PASS
자동 20-cycle soak는 통과. 실제 클라이언트 debug encounter 20회/orphan battle 0 수동 gate는 최종 완성본 테스트 때 함께 수행.

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

## M5 — Production UI/Presentation Gate
상태: **NEXT — RESEARCH/DESIGN GATE ONLY**
### 선행
`06_UI_UX_PRESENTATION.md` Visual Gate 완료.
### 작업 순서
1. 실제 우수 턴제 RPG UI 다수 조사.
2. 실제 Minecraft UI/모드 구현 사례 조사.
3. `08_REFERENCE_CATALOG.md` 보강.
4. 화면별 information hierarchy.
5. design tokens.
6. mockup.
7. production HUD/menu.
8. 실제 Minecraft screenshot comparison iteration.
### PASS
공용 QUALITY_STANDARD visual audit.

**금지:** reference catalog / hierarchy / tokens / mockup이 닫히기 전에 production UI Java를 먼저 구현하지 않는다.

## M6 — World & Life Loop
### 선행
`09_WORLD_ASSET_GATE.md` 통과.
### 작업
- authored hub/region prototype.
- mining/farming/fishing/crafting 연결.
- fast travel/exploration/quest hooks.
### PASS
각 활동의 산출이 성장 루프에 실제 사용되고 막힌 경로 없음.

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

## 지금 바로 할 일
M0~M4 automated gate는 닫혔다. 다음 작업은 **M5 Production UI/Presentation Gate의 연구/설계 단계**다.

production UI 코드를 즉시 만들지 않는다. 공용 `QUALITY_STANDARD.md`, `AGENT_RULES.md`, `06_UI_UX_PRESENTATION.md`, `08_REFERENCE_CATALOG.md`를 기준으로 외부 reference 조사→비교 분석→information hierarchy→design tokens→mockup을 먼저 정본화한다.

M0~M4 중 수치가 이후 플레이테스트에서 달라지는 것은 정상이며, CANON을 건드리지 않는 튜닝은 데이터로 조정한다.
