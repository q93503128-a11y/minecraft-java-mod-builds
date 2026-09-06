# 14 — IMPLEMENTATION BACKLOG

이 순서는 '예쁜 화면부터' 만들지 않고 위험한 기술/게임성 가정을 먼저 검증하기 위한 기본 개발 순서다.

## M0 — Bootstrap & Contracts
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
### 작업
- Entity participant binding.
- world AI/damage isolation.
- C2S command/S2C snapshot+events.
- DEBUG_ONLY HUD.
- cleanup/disconnect/dimension guards.
### PASS
실제 클라이언트에서 debug encounter 20회 반복, orphan battle 0.

## M3 — Representative Content
### 작업
- 대표 바닐라 8종.
- action 20개 이상.
- 일반 Encounter + elite/boss.
- 각 role/affinity/status/Intent 케이스 검증.
### PASS
Basic spam보다 Poise/Intent 대응이 유리한 상황이 명확히 존재하고 전투 로그로 확인.

## M4 — Progression & Reward
### 작업
- character ownership.
- origin/current star/level caps.
- ascension.
- squad cost.
- Coin/Essence/Shard.
- save/version.
- Encounter reward.
### PASS
새 save→전투→성장→승급→재접속 흐름 성공.

## M5 — Production UI/Presentation Gate
### 선행
`06_UI_UX_PRESENTATION.md` Visual Gate 완료.
### 작업
- reference catalog 보강.
- information hierarchy/design tokens.
- mockup.
- production HUD/menu.
- screenshot comparison iteration.
### PASS
공용 QUALITY_STANDARD visual audit.

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
다음 개발 세션은 M0부터 시작한다. M5의 시각 디자인을 앞당기지 않는다. M0~M4 중 수치가 플레이테스트에서 달라지는 것은 정상이며, CANON을 건드리지 않는 튜닝은 데이터로 조정한다.
