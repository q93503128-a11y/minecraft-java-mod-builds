# 28 — M6 FIRST EXPEDITION QUEST HOOK

이 문서는 `HUB_01 -> REGION_01 -> rift_elite -> HUB_01` 첫 임무 흐름을 정본화한다.

목표는 별도 퀘스트 메뉴·퀘스트 재화·클릭 노동을 추가하는 것이 아니라, 이미 존재하는 **월드 발견 상태와 일회성 Encounter 완료 상태 자체를 진행도로 사용**하는 것이다.

## 1. 첫 임무 흐름

1. Hub의 실제 이동석을 직접 발견한다.
2. 동쪽 길을 따라 REGION_01로 이동한다.
3. 광산/농장/강 분기와 반복 patrol을 통해 준비 재료와 전투 흐름을 익힌다.
4. REGION_01 이동석을 직접 발견한다.
5. `turnbound_re:region_01/rift_elite` 균열 선봉대를 도전한다.
6. 최초 승리의 reward settlement와 encounter completion이 같은 기존 저장 흐름에서 성공한다.
7. 완료 안내 후 발견한 이동석을 사용해 Hub로 돌아갈 수 있다.

## 2. 진행도 저장을 새로 만들지 않는다

첫 임무 단계는 다음 두 정본만 읽어 파생한다.

- `FastTravelSavedData.discovered(player)`
- `PlayerProgress.completedEncounterLocators`

단계:

- REGION_01 waypoint 미발견 -> `FIND_REGION_WAYPOINT`
- REGION_01 waypoint 발견 + elite 미완료 -> `DEFEAT_RIFT_VANGUARD`
- elite locator 완료 -> `COMPLETE`

따라서 quest save와 world/progression save가 서로 어긋나는 이중 상태가 없다.

## 3. 일회성 목표

`rift_elite` anchor는 production data에서 `repeatable=false`다.

- `overworld_patrol`: 반복 가능, 재료/성장 파밍용 일반 루프.
- `rift_elite`: 최초 임무의 one-time 목표.

기존 `WorldEncounterAnchorAccessPolicy`가 anchor/Encounter repeatable 계약을 합쳐 처리하므로, 완료된 elite는 preview/confirm 단계에서 다시 열리지 않는다.

## 4. 안내 방식

새 퀘스트 대시보드를 만들지 않는다.

- Hub waypoint 최초 발견 후: 기존 localized expedition/REGION 명칭 조합으로 다음 지역을 짧게 안내.
- REGION waypoint 최초 발견 후: 기존 localized challenge/elite 명칭 조합으로 목표를 안내.
- elite settlement 성공 후: 기존 localized victory/Hub 명칭 조합으로 귀환 방향을 안내.

안내는 서버가 이미 성공한 실제 사건 뒤에만 발생한다. 클라이언트가 quest stage를 제출하지 않는다.

## 5. 보상/실패 원자성

임무 완료는 별도 체크박스가 아니다.

`VICTORY -> reward settlement -> non-repeatable locator completion`

기존 immutable save write가 성공한 뒤에만 완료 안내가 발생한다. persistence 실패 시 BattleManager reward claim은 소비되지 않고 임무 완료 안내도 발생하지 않는다.

## 6. 시스템 연결

첫 임무는 새 시스템을 추가하기보다 현재 루프를 한 번 통과하게 만든다.

`Hub forge -> world travel -> mining/farming/fishing -> equipment/preparation -> patrol -> waypoint discovery -> one-time elite -> Coin/Essence/Shard reward -> Hub return`

각 단계가 다음 단계의 실제 선택에 연결되어야 한다.

## 7. 자동 gate

- fresh state는 REGION waypoint 탐색 단계.
- REGION waypoint 발견 후 elite 목표 단계.
- elite completion은 discovery와 무관하게 최종 완료 상태로 우선.
- production `rift_elite` anchor는 non-repeatable.
- production patrol anchor는 repeatable 유지.
- reward persistence 성공 이전에는 completion guidance 없음.
- 기존 3-argument BattleRewardLifecycleHooks / 1-argument WorldFastTravelHooks 생성자 호환 유지.
- clean build / JUnit / production JAR verify.

## 8. 아직 남은 것

- 실제 Minecraft에서 안내가 너무 짧거나 과한지 확인.
- elite 전투 시간과 첫 장비/준비물 획득 시간 비교.
- reward가 다음 성장 선택을 실제로 만들어내는지 확인.
- Hub -> 생활 분기 -> waypoint -> elite -> 귀환을 screenshot/playtest로 통합 검증.
- 이후 필요가 확인될 때만 더 긴 quest chain/대화/전용 UI를 설계한다.
