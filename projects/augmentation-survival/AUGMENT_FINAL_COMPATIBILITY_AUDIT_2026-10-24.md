# Augmentation Survival — 146 Augment Final Compatibility Audit

Date: 2026-10-24
Status: FINAL DESIGN AUDIT / IMPLEMENTATION GATE

이 문서는 신규 증강 제작 종료 시점의 전체 감사다. 이전 `AUGMENT_COMPATIBILITY_AUDIT_2026-10-23.md`의 124종 기준 감사는 역사 기록으로 남기고, 구현 판단에는 이 문서와 `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md` V2를 사용한다.

## 1. 감사 입력 정본

감사 기준:

1. `AUGMENTS.md` — 146종 정본
2. `AUGMENT_INTERACTION_CONTRACT_2026-10-24.md` — V2 상호작용 계약
3. `AUGMENT_PROPOSAL_HISTORY.md`
4. Batch S/T 포함 최신 결정 문서
5. `HUD_AND_PLAYER_TEXT_STANDARD.md`
6. `ENEMY_POWER_SCALING_DESIGN.md`
7. `GAME_DESIGN.md`
8. `MODPACK_BASELINE.md`
9. 현재 source/resource

확정 분포:

- 일반 20
- 고급 27
- 희귀 30
- 영웅 40
- 전설 20
- 신화 9
- 총 **146종**

## 2. 최종 중복 감사

### 삭제가 필요한 정확/의미 중복

**0개.**

비슷한 축을 공유하는 증강은 존재하지만 발동 조건, 결과, 플레이 목적이 달라 의도적인 조합으로 유지한다.

대표 예:

- `빠른 발` / `연속 처치` / `시간 가속`: 상시·처치 스택·액티브라는 역할 차이.
- `두 번 점프` / `공중 연계`: 기본 공중 점프와 공격 성공 기반 무한 재충전.
- `관성 전환` / `탄성 저장`: 착지 후 이동속도와 낙하 에너지의 다음 점프 변환.
- `관통 화살` / `연쇄 화살` / `분산 사격` / `추적 화살` / `도탄`: 모두 원거리지만 각각 관통·비물리 연쇄·물리 투사체 추가·유도·지형 반사.
- `이중 충전` / `전투 순환` / `연장전` / `재연`: 충전 저장·쿨다운 감소·지속 연장·자동 약화 재발동.
- `회복 증폭` / `넘치는 회복` / `회복 나눔` / `생명 파동`: 회복량·초과 체력·아군 공유·회복의 공격 전환.
- `거대화` / `작아지기`: 목적이 반대인 변신이며 동시 적용하지 않는 우선순위 계약을 둔다.

## 3. 무한루프 / 기하급수 위험 감사

### 안전하게 허용하는 강한 조합

- `관통 + 도탄 + 추적`: 같은 적을 여러 번 재관통 가능. 도탄 횟수로 유한.
- `분산 + 관통 + 도탄 + 추적`: 보조 화살도 독립적으로 실행. 강하지만 각 물리 투사체의 저작된 최대 횟수로 유한.
- `연쇄 폭발`: 파생 폭발로 발생한 처치가 다음 폭발을 만들 수 있으나 각 개체는 한 번만 죽으므로 유한.
- `공중 연계`: 적중을 계속 성공하면 무한 체공 가능. 재귀 버그가 아니라 의도된 플레이 규칙.
- `이중 충전 + 재연`: 수동 사용 2회가 각각 1회 재연 가능. 재연이 재연을 만들지 않으므로 유한.

### 반드시 차단할 재귀

- 분산 사격 보조탄 → 새 분산 사격 생성
- 연쇄 화살 피해 → 새 물리 투사체/연쇄 시퀀스 생성
- 벼락 피해 → 벼락 카운터 증가
- 메아리 타격 → 메아리 카운터 증가
- 되밟기 잔상 → ROOT/되밟기/메아리/벼락 생성
- 회복 나눔 → 회복 나눔 재귀
- 공유 회복 → 생명 파동 무한 파동
- 재연 → 재연 예약
- 생명 연결 공유 피해 → 생명 연결 재공유
- 보호막 폭발 → 같은 보호막 pool 재폭발

## 4. 전투 이벤트 계약 감사

모든 복잡한 전투 증강을 개별 event callback으로 직접 연결하면 서로가 서로를 다시 호출하게 된다. 구현은 공통 `CombatEventRouter`와 provenance context를 먼저 만든다.

최소 구분:

- ROOT_MELEE
- ROOT_PROJECTILE
- DERIVED_PROJECTILE
- DERIVED_CHAIN
- DERIVED_AOE
- ECHO
- PATH_ECHO
- ACTIVE_SPECIAL
- REPLAY
- AUTHORITY
- COMPANION
- REFLECTED
- SPECIAL_PERCENT_HP

이 분류가 먼저 존재해야 `직접 적중`, `플레이어 귀속 처치`, `보상 가능한 처치`, `재귀 금지`를 같은 기준으로 판정할 수 있다.

## 5. 투사체 감사

최종 projectile identity는 최소 다음 상태를 가진다.

- owner/root shooter
- provenance
- shotActionId
- physicalProjectileId
- flightSegment
- segmentHitSet
- pierceBudget
- ricochetCount
- chainStarted
- tracking target
- base damage snapshot

핵심 규칙:

- 같은 segment에서 같은 엔티티 1회.
- 도탄 시 segmentHitSet과 관통 예산만 초기화.
- chainStarted는 투사체 전체 수명에서 유지.
- 원본 화살은 도탄 후에도 ROOT_PROJECTILE 유지.
- 분산 보조탄은 DERIVED_PROJECTILE 유지.

따라서 사용자가 의도한 `보스 관통 → 벽 도탄 → 추적 재획득 → 같은 보스 재관통`이 정상 구현 가능하다.

## 6. 근접 / 이동 감사

### 두 번 점프 + 공중 연계

공중 점프를 서로 다른 두 시스템으로 만들지 않고 하나의 `air jump available` 상태를 공유한다. `두 번 점프`가 초기 권한을 주고 `공중 연계`가 ROOT 공중 근접 적중마다 다시 채운다.

### 탄성 저장

실제 낙하 거리 입력을 사용한다. 낙하 피해 감소/면역 때문에 저장량이 줄어들지 않는다. 3초 안의 다음 지상 점프가 소비한다.

### 되밟기

최근 2초 위치만 ring buffer로 보관한다. 긴 월드 이동 경로를 저장하지 않는다. activation당 대상 hit-set으로 다중 중복 적중을 차단한다.

### 작아지기 / 거대화

동시에 size multiplier를 곱하지 않는다. 거대화가 우선하며 작아지기를 일시 중지한다. 변신 종료 때 항상 collision-safe resize를 사용한다.

### 벽치기

일반 `hurt` 이벤트만으로 구현하지 않는다. 공격에서 만들어진 knockback event와 이후 짧은 충돌 window를 연결해야 한다. 이 window와 충돌 기준은 데이터 파라미터로 둔다.

## 7. 회복 / 보호막 감사

### 회복

`primary heal → 회복 증폭 → 실제 HP → 넘치는 회복 → 회복 나눔` 순서를 고정한다.

공유 회복은 다시 공유/초과체력/생명 파동의 새 1차 원인이 되지 않는다.

### 보호막

파란 별도 보호막과 노란 absorption을 UI에서는 구분한다. 내부에서도 `source pool`을 구분해야 `보호막 폭발`이 같은 pool에서 여러 번 터지지 않는다.

권장 내부 구조:

- sourceId
- createdAmount
- currentAmount
- expiry
- exploded flag

바닐라/외부 모드가 만든 출처 불명 absorption은 기본적으로 증강 보호막 pool로 승격하지 않는다.

## 8. 치명 / 사망 감사

고정 순서:

1. 한 번 버티기
2. 손의 Totem
3. 비상 토템의 인벤토리 Totem
4. 재림
5. 실제 사망

실제 사망에만 소지품 보존, 경험 보존, 사망 지점, 소생 표식을 적용한다.

`재림`을 death event 뒤에서 복구하는 방식으로 만들면 드롭/통계/외부 모드 death hook가 이미 실행되므로 안 된다. 치명 판정 파이프라인 안에서 실제 사망 전에 처리해야 한다.

## 9. 액티브 / 쿨다운 감사

공통 엔진 없이 증강마다 쿨다운 필드를 만들지 않는다.

필수 공통 상태:

- ability id
- charge count / max charge
- current recharge end time
- sequential recharge queue
- active instance id
- duration end time
- replay scheduled time
- replay eligibility/policy

`능력 가속`, `이중 충전`, `전투 순환`, `연장전`, `재연`이 모두 이 공통 엔진을 사용해야 한다.

신화 액티브는 기본적으로 능력 가속/이중 충전/재연 대상에서 제외한다.

## 10. 획득 / 두 번째 선택 감사

증강 장치 선택은 server transaction이어야 한다.

- 후보 생성 시 pool snapshot/token 발급.
- 서버가 player/core/tier/max-level을 다시 검증.
- `두 번째 선택`은 서로 다른 2개를 모두 확정한 뒤 한 transaction으로 획득/코어 소비.
- disconnect/취소/잘못된 packet에서 부분 획득이나 코어 손실 금지.

이 구조가 먼저 있어야 멀티플레이에서 복제/중복 선택 문제가 생기지 않는다.

## 11. 월드 위협 단계 감사

확정 구조:

- save-wide 단일 위협 단계 0~5.
- player별/지역별 단계 없음.
- world power high-water mark가 올라갈 때만 단계 상승.
- 일반/엘리트는 load/stage-change에서 재계산, 매 tick 계산 금지.
- 보스는 encounter 시작 때 스냅샷.
- 공격력보다 체력을 더 크게 증가.

현재 수치 테이블은 플레이테스트 시작값이며 최종 밸런스가 아니다.

## 12. 외부 모드 호환 매트릭스

| 대상 | 연동 방식 | 핵심 감사 포인트 |
|---|---|---|
| Better Combat | 서버 ROOT melee 정규화 adapter | 대상당 1 ROOT hit, 공속 축, 실제 hitbox, 방패 던지기 snapshot |
| Terralith | biome/worldgen data | 기존 월드젠 보존, 코어 콘텐츠를 별도 overlay |
| Repurposed Structures | loot/structure data | 기존 loot 보존 + 코어 pool 추가 |
| Dungeons and Taverns | loot/structure data | 동일 |
| Friends&Foes | entity/loot profile | 일반/엘리트 threat scaling, 코어 파밍 규칙 |
| Illager Invasion | entity/loot profile | 일리저 전투/스케일, 기존 Fabric warning 추적 |
| Naturalist | entity profile | 평범한 동물을 코어 파밍 대상으로 만들지 않음 |
| Farmer's Delight Refabricated | crop/food/processing adapter | age/우클릭/다중블록 작물과 영역 가속 |
| Chrono Dawn | boss API/profile/authority adapter | 공식 boss defeated API 우선, 제어/보상/권능 package |
| World Bosses | namespace/entity adapter | 원본 JAR 수정 금지, scripted boss 제어 예외 |
| MCEndgame | boss/progression adapter | 반복 보상 악용, 자체 성장축 경쟁, analytics off 우선 |
| GeckoLib | client presentation | 모델/애니메이션만, 판정 authority 아님 |
| Player Animation Library | client/player animation | 애니메이션과 서버 판정 분리 |

## 13. 권능 탈취 감사

이 증강은 외부 보스 코드를 통째로 복사하지 않는다.

필요 데이터:

`boss selector → authority package id → authored player skills[]`

- 최초 직접 처치만 영구 해금.
- 서버 저장 boss ID set으로 중복 해금 방지.
- 한 키로 모든 해금 package를 bounded scheduler에 넣는다.
- 수십 개 공격을 한 tick에 생성하지 않는다.
- 패키지 안의 공격은 보스 phase/무적/world script를 포함하지 않는다.
- AUTHORITY 처치는 플레이어 귀속은 가능하지만 ROOT 직접 처치로는 취급하지 않는다.

## 14. 영구 저장 구조 감사

### 플레이어 귀속

Data Attachment 계열의 서버 영구 상태에 적합:

- augment level map
- active slot layout
- cooldown/charge state
- 사냥 기록 entity type set
- 권능 탈취 boss ID set
- 마지막 실제 사망 지점/사망당 자원
- state schema version

사망 후 유지해야 하는 증강 상태는 copy-on-death 의미를 가져야 한다.

### 월드/공유 데이터

플레이어 상태와 분리:

- save-wide threat high-water mark/stage
- 원격 창고의 UUID segment/shared group
- 공간 연결의 cross-dimension portal graph

이들은 별도 world/server persistent store를 사용한다.

### 세이브 버전

초기 구현부터 `schemaVersion`을 넣는다. 146종이 많은 만큼 나중에 필드 구조가 바뀌었을 때 save migration 없이 직렬화 구조를 교체하지 않는다.

## 15. 네트워크 감사

클라이언트 packet은 권한 명령이 아니라 요청이다.

serverbound 후보:
- active use
- active slot edit
- authority release
- target selection confirm
- augment-device selection confirm

서버는 거리, 소유 증강, 레벨, 쿨다운, 대상 유효성, inventory/core 상태를 모두 다시 검증한다.

clientbound:
- login/full UI snapshot
- 작은 상태 delta
- cooldown/charge/HUD
- threat stage
- targeting preview/VFX result

플레이어 전체 증강 상태를 작은 변화마다 통째로 sync하지 않는다.

## 16. 성능 감사

금지:
- 매 tick 모든 엔티티 검색
- 매 tick 전체 월드 threat 재계산
- 매 tick 전체 인벤토리/모든 공유 창고 재직렬화
- 모든 권능을 같은 tick에 실행
- 투사체 hit history를 영구 보존
- VFX용 display/entity를 무제한 축적

허용 구조:
- event driven
- bounded radius query
- fixed-size path buffer
- entity lifetime-bound projectile context
- expiry queue / timestamp cooldown
- server scheduler with per-tick budget

## 17. HUD / player-facing 감사

- 기본 카드: 짧은 효과와 중요한 숫자만.
- Shift: 계수/레벨별 수치/범위/쿨다운/중첩/대상 수.
- ROOT/DERIVED/adapter/compat 같은 개발 내부 용어는 표시하지 않는다.
- 체력 HUD: 파란 보호막 / 빨간 실제 HP + 노란 absorption.
- 우측 상단: `위협 0~5` 작은 표시.
- 액티브 10칸과 권능 탈취 전용키/쿨다운 표시를 분리.

## 18. 구현 전 남은 콘텐츠 파라미터

아키텍처를 막지 않는 미확정 값:

1. 탄도 장막 발동 주기 / 범위 / 회당 파괴 수
2. 임시 발판 지속시간 / 쿨다운
3. 생명 연결 조건 / 대상 수 / 범위 / 지속시간 / 공유 비율
4. 작아지기 웅크리기 유지시간
5. 벽치기 충돌 window / 최소 충돌 판정
6. 코어 획득처별 정확한 등급 확률
7. 위협 단계 최종 스탯 표

이 값은 데이터 파라미터로 두고 플레이테스트에서 조정한다.

## 19. 구현 준비 결론

146종을 하나씩 독립 구현하는 방식은 금지한다. 먼저 공통 런타임 spine을 만든 뒤 handler/profile로 콘텐츠를 얹는다.

현재 판단:

- ROSTER CANON LOCKED: YES — 146
- EXACT/SEMANTIC DUPLICATE REQUIRING REMOVAL: 0
- INTERACTION CONTRACT V2: READY
- SERVER AUTHORITY MODEL: DEFINED
- SAVE/NETWORK BOUNDARY: DEFINED
- EXTERNAL COMPAT STRATEGY: DEFINED
- PERFORMANCE / RECURSION GUARDS: DEFINED
- CONTENT PARAMETERS REMAINING: YES — non-blocking
- RUNTIME IMPLEMENTED: NO
- CODE REVIEWED: bootstrap/source state reviewed
- TESTED: NO new runtime test
- BUILD VERIFIED: prior M0 pack baseline only; not this design pass
- JAR PRODUCED: prior M0 artifact only
- PLAYTESTED: NO
- MULTIPLAYER TESTED: NO
- BUILD/CI THIS PASS: NOT RUN — docs/design only
