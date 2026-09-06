# 10 — VERTICAL SLICE IMPLEMENTATION

## 목표
최종 미술 없이도 TURNBOUND: RE의 핵심 재미와 기술 구조가 실제 Minecraft에서 성립하는지 가장 작은 완결 루프로 증명한다.

## Slice 범위
### 전투
- 4인 플레이어 파티.
- 3인 일반 적 Encounter 1개.
- 엘리트/보스 Encounter 1개.
- SPD turn order.
- Intent.
- 6 affinity.
- Poise/EXPOSED.
- Energy/Basic/Guard/Skill/Burst.
- status 최소 7종.
- victory/defeat/reward/cleanup.

### 캐릭터
대표 바닐라 8종 정의. 한 번의 플레이에서 4명을 편성할 수 있어야 하며 각 역할이 겹치지 않게 최소 20개 이상의 action definition을 통해 시스템을 검증한다.

### 성장
- 보유 캐릭터.
- level up.
- origin/current star.
- 최소 한 번의 ascension.
- party squad cost.
- Coin/Essence/Shard 보상/소비.

### 월드
- debug hub.
- debug resource corner 또는 임시 재료 지급 수단.
- 보이는 Encounter 2종.
- 전투 종료 후 원래 월드 상태 복귀.

### UI
`DEBUG_ONLY` 전투 HUD/성장 화면만. production art 금지.

## 필수 플레이 시나리오
1. 신규 save 시작.
2. 4명 편성.
3. Encounter preview 후 전투 진입.
4. 적 Intent 확인.
5. WEAK 적중으로 Poise를 빠르게 감소.
6. EXPOSED 발생, Intent 취소, 공격 창 사용.
7. 전투 승리 및 보상.
8. 캐릭터 level up/승급.
9. 두 번째 Encounter에서 성장 차이 확인.
10. save/reload 후 진행 보존.

## 실패 시나리오
- stale command 패킷.
- 죽은 target 선택.
- energy 부족 skill.
- participant 강제 제거.
- player disconnect(가능한 로컬 테스트 범위).
- `/reload` 중 definition 참조 오류.
- 전투 종료 후 AI suppression이 남는 문제.

## Slice PASS
- 위 시나리오가 crash/soft-lock 없이 반복 20회.
- 동일 seed+command sequence에서 동일 combat event 결과.
- 전투 종료 후 orphan battle 0.
- save reload 후 핵심 progression 동일.
- debug UI만 사용했다는 이유로 시각 완성으로 보고하지 않음.
