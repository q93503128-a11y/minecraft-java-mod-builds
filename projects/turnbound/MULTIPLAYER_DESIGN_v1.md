# TURNBOUND Multiplayer Design v1

> 플레이어 social party, shared-world battle visibility, future co-op/PvP의 정본.
> 캐릭터 4인 파티와 플레이어 파티를 혼동하지 않는다.

## 1. 기본 원칙

- Minecraft 월드는 공유된다.
- 플레이어 파티 최대 4명.
- 각 플레이어의 수집/장비/재화/퀘스트 저장은 개인 소유가 기본이다.
- world boss나 일부 월드 진행만 명시적으로 shared world state를 사용한다.
- 모든 전투 판정, 보상, PvP 결과는 서버 권위다.

## 2. 플레이어 파티

1차 기능:
- 초대
- 수락/거절
- 탈퇴
- 최대 4명
- 파티장
- 추후 미니맵/월드맵 파티원 표시

Co-op PvE에서는 각 참가자가 자신의 활성 캐릭터 파티(최대 4명)를 그대로 가져온다.
따라서 2/3/4인 co-op은 최대 8/12/16명의 정규 아군 캐릭터가 같은 BattleState에 존재할 수 있다.

## 3. 필드 조우

- encounter는 server가 claim한다.
- 같은 파티가 한 field representative를 중복 시작하지 않는다.
- 멀리 떨어진 파티원을 자동으로 끌어오지 않는다.
- shared PvE 참가 방식은 nearby eligible member를 확인하는 구조를 우선한다.
- 참가하지 않은 사람은 field state 유지.

## 4. 다른 사람의 전투 보기

주변 플레이어에게 공유:
- ally/enemy BattleActorEntity
- 이동/공격/피격/다운 animation
- boss silhouette

owner-only:
- target/focus marker
- 선택 cursor
- 개인 HUD
- 개인 tutorial cue

VFX는 겹친 전투의 노이즈를 막기 위해 처음부터 전부 공유하지 않고 effect class별로 spectator-safe 승격한다.
관전자는 battle actor를 때리거나 현재 BattleEngine state를 변경할 수 없다.

## 5. Co-op PvE

single-owner `BattleSession`을 다중 입력으로 땜질하지 않는다.
별도 `SharedBattleSession` 계열을 사용한다. 현재 정적 구현은 존재하지만 실제 멀티플레이 검증 전에는 완료로 판정하지 않는다.

확정:
- 1인 전투는 기존처럼 아군이 한 줄로 선다.
- 2인 이상은 **플레이어 1명당 자기 캐릭터 최대 4명을 2행 2열 사각형 블록**으로 배치한다.
- 각 플레이어의 2x2 블록을 좌우로 나열한다.
- 두 줄은 순수한 시각 배치다. 앞줄/뒷줄에 방어, 사거리, 타깃 우선도 차이를 두지 않는다.
- 적 HP 배율은 참가 플레이어 수와 정확히 같다: 1인 x1 / 2인 x2 / 3인 x3 / 4인 x4.
- HP 외 ATK/DEF/SPD는 이 인원 배율로 올리지 않는다.
- 같은 캐릭터를 서로 다른 플레이어가 가져올 수 있으므로 actor instance는 owner별로 분리한다.

현재 정적 구현:
- 행동 owner별 client input routing
- owner별 AUTO, 파티장 speed/flee 권한
- 같은 서버 메모리 기반 disconnect/rejoin
- 참가자별 reward/quest credit 및 transaction id
- battle-clear 기반 authored quest와 활성 반복 지역 의뢰도 **실제 shared battle 참가자 각자에게** 진행 기록
- 멀리 떨어져 shared battle에 참가하지 않은 파티원에게는 encounter/quest/contract credit을 자동 지급하지 않음
- 온라인 참가자 결과 확인과 오프라인 참가자 WAL 기반 지연 정산
- 참가 인원별 적 HP 배율, owner별 2×2 진형
- 승리 후 shared field encounter/world clear 복귀

남은 검증/보강:
- 실제 2/3/4인 동시 입력과 owner 권한 검증
- 전투 중/결과 화면 중 disconnect/rejoin 실검증
- 오프라인 참가자가 있는 상태의 서버 종료 및 WAL 복구 실검증
- wipe/flee/보스 phase/field return의 실제 멀티 흐름
- 중복 보상·저장 실패 재시도·카메라/귀환 위치 검증

## 6. PvP

- duel request → accept
- 별도 arena/session
- 일반 오버월드 friendly fire 금지
- 패배 장비/재화 손실 없음
- progression farming 방지
- server-authoritative action order
- 필요하면 PvP 전용 계수를 PvE와 분리
- Ranked는 duel의 network/reconnect/balance가 멀티 실테스트된 뒤 추가

## 7. 현재 구현 단계

구현:
- server-authoritative player party membership foundation
- invite / accept / decline / leave command path
- party member position + battle-state snapshot
- exploration minimap party marker
- battle actor spectator visibility
- owner-only helper marker isolation

구현:
- 파티 관리 UI가 관리 메뉴의 파티 화면 안에 통합됨
- shared battle capacity: 최대 16 regular allies + 플레이어별 summon
- shared encounter roster blueprint / actor owner map
- player-count enemy HP scaling
- 2x2 owner block battle formation
- co-op 폭에 맞춘 battle camera framing 범위 확대
- SharedBattleSession owner input/AUTO/speed/flee lifecycle
- field encounter → nearby eligible party shared-battle start
- 같은 서버 재접속 resume
- 참가자별 보상 transaction + 오프라인 WAL 정산
- initiator 로그아웃 상태에서도 claimant UUID로 Drehmal field encounter 종료

아직 미구현/미검증:
- duel battle
- ranked
- 실제 multiplayer playtest
- 2~4인 최대 구성에서의 카메라/귀환/저장 실패/재접속 edge case 실검증


## 8. 현재 검증 상태

- CODE REVIEWED: YES — shared session/owner routing/offline settlement 경로 정적 검토
- TESTED: 회귀 테스트 추가, 최신 branch 실행 결과는 별도 build checkpoint에서 확인
- BUILD VERIFIED: 최신 shared-settlement 수정 기준 아직 미확인
- CLIENT RUNTIME TESTED: NO
- MULTIPLAYER TESTED: NO

실제 멀티 테스트 전까지 파티 위치 동기화, 전투 관전 가시성, 재접속/이탈 동작을 성공으로 간주하지 않는다.
