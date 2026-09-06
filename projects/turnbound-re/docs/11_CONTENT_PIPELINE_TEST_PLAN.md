# 11 — CONTENT PIPELINE & TEST PLAN

## 1. 자동 검증 레이어
### A. Schema
JSON/Codec decode 성공, 범위 검증.
### B. Referential integrity
character→action/passive, encounter→character/reward, status/effect 참조 존재.
### C. Semantic invariant
- originStar 1..5.
- currentStar save가 originStar..6.
- affinity 6종 누락 없음.
- squadCost > 0.
- skill cost/energy 범위 유효.
- target rule과 effect 대상 호환.
- 모든 eligible vanilla Mob 매핑/제외.
### D. Simulation
고정 seed 전투로 공식/상태전이 회귀.
### E. GameTest
실제 Entity binding, battle lifecycle, cleanup.
### F. Client manual
입력/HUD/애니메이션/해상도. 시각 gate 이후 screenshot audit 추가.

## 2. 핵심 Unit test
- initiative stable tie-break.
- stale revision rejection.
- IMMUNE가 0 damage.
- WEAK HP x1.25 및 Poise x1.5.
- EXPOSED 피해 x1.2.
- EXPOSED intent cancel/recover.
- POISE_GUARD 회복.
- Guard duration/중첩 불가.
- deterministic crit/variance.
- victory/defeat exactly once.
- reward exactly once(idempotency).

## 3. GameTest
- entity battle 등록/해제.
- 외부 damage 차단.
- vanilla AI suppression/restore.
- dimension/world cleanup.
- save progression.
- resource reload validation.

## 4. 데이터 검증 명령
개발자는 로컬에서 최소:
```text
clean build
unit tests
GameTests (가능 범위)
data validation
JAR content/version verification
```
정확한 Gradle task명은 프로젝트 bootstrap에서 NeoForge/공용 BUILD_STANDARD에 맞춰 정의한다.

## 5. CI 실패 조건
- 미분류 바닐라 Mob.
- duplicate id.
- dangling reference.
- invalid range.
- deterministic combat snapshot 변화가 승인되지 않음.
- build/JAR verify 실패.

## 6. 로그
battle debug export는 최소 header(seed/version/definitions hash), command sequence, event sequence, final result를 제공하여 사용자가 스크린샷만 보내도 재현 단서를 만들 수 있게 한다.
