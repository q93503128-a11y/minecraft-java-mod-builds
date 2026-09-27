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

플레이어 파티에 4명이 있다고 캐릭터 16명을 그대로 한 전투에 넣지 않는다.

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

현재 single-owner `BattleSession`을 다중 입력으로 땜질하지 않는다.
별도 `SharedBattleSession` 계열이 필요하다.

확정 필요:
- 2/3/4인일 때 4개 전투 슬롯 배분
- 행동 owner
- AUTO
- disconnect/rejoin
- reward/quest credit
- wipe/flee
- boss phase/world clear

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
- battle actor spectator visibility
- owner-only helper marker isolation

아직 미구현:
- party UI
- party minimap marker
- shared PvE battle
- duel battle
- ranked
- multiplayer playtest
