# Drehmal 24개 시작·발견도 명령 물리 체인 및 6종 오프라인 통합 판정

## 실제 검증

- 대상: 공식 Drehmal APOTHEOSIS 2.2.2f, Minecraft 1.20.1, SHA 확인된 비활성 독립 월드 복사본.
- [공식 물리 체인 전체 감사 #2 — PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733734444): 명령 블록의 1.20.1 palette LongArray, 좌표, NBT 명령 해시, facing, conditional, auto, powered, 주변 반경 2블록과 방향성 체인 실측. 24/24개 원본과 일치.
- [기존 5종 + 24개 여섯 경계 통합 실험 — PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256037): inspect, prepare, verify, restore, 중복 지역 파일의 단계별 SHA와 완전 복구 검증.
- [기존 5종 회귀 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733255944), [기존 4종 회귀 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256158), [24개 독립 시험 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256005).
- 검증 대상 24개는 9개 방향성 연결 컴포넌트, 이 중 22개가 다른 명령 블록과 체인으로 연결되어 있다. 신호 관련 인접 블록은 10/24개 위치에서 관측. 조사된 체인 내 conditional 블록 0개, 무한/초과 추적 0건.

## 시작 아이템 지급 21개 = 원작 6개 시작 체인 중 일부

| x,z (상단 y=214부터) | 전체 체인 | 이번 차단 give | 원작의 나머지 동작 유형 |
|---|---:|---:|---|
| 26458,-288 | 10 | 3 | spawnpoint, execute advancement, execute scoreboard, setblock 2, effect, tp |
| 26461,-288 | 10 | 3 | 위와 동일 |
| 26464,-288 | 10 | 3 | 위와 동일 |
| 26457,-260 | 9 | 4 | execute advancement, execute scoreboard, gamemode, gamerule, spreadplayers |
| 26461,-260 | 9 | 4 | 위와 동일 |
| 26465,-260 | 9 | 4 | 위와 동일 |

6개 체인의 합계는 57블록이며 선택한 give는 21개다. 나머지 원작 명령 36개 중 gamemode 3개는 기존 5종에 포함되어 있다. 따라서 **기존 5종 + 24개 실험에서도 시작 체인의 원작 명령 33개가 차단되지 않고 남는다.**

체인의 대상 블록은 조건부 실행이 아니지만, 이것만으로 나머지 블록의 안전한 비실행을 증명할 수 없다. 특히 월드 구조를 바꾸는 setblock, 이동/부활 위치를 제어하는 tp/spreadplayers/spawnpoint, gamerule이 남아 있어 이 24개만을 원작 시작 기능 제거로 선언할 수 없다. 반대로 33개 일괄 삭제도 구조물 보존을 검증하기 전에는 허용되지 않는다.

## 발견도 지급 3개

- discoveries:homligrave — (2291,31,-2959), 자동 반복 명령 블록, 직접 연결 체인 없음.
- primordial:enteryavhlix — (26907,57,-219), 자동 반복 명령 블록, 직접 연결 체인 없음.
- discoveries:divineruins — (27221,182,-169), 2개 블록의 체인에 위치하며 다른 명령은 gamemode(기존 5종 차단 대상). 가까운 별도 spawnpoint 명령도 확인됨.

두 개가 직접 연결되지 않았다는 사실은 감지 조건·원작 스토리·시설 의존성까지 없다는 증거가 아니다.

## 통합 범위와 안전 경계

- 기존 prepare_drehmal_isolated_copy.py에 추가한 --include-starter-experiment는 명시적 오프라인 실험 옵션이다. --include-gamemode와 SHA 검증된 --direct-report가 함께 있을 때만 24개를 다섯 번째 경계 뒤에 적용한다.
- 기존 4종 기본과 5종 옵션은 그대로 유지한다. 통합 실험은 SHA 중첩과 정확 복구를 증명하지만, 원작 시설·버튼·문·입구·포탈의 기능 및 실제 TURNBOUND 입장/멀티플레이 안전성을 증명하지 않는다.
- 원작 명령 본문·함수/JSON 로직을 TURNBOUND에 이식하거나 재사용하지 않았다. 원본과 변경 지도 파일을 저장소·아티팩트에 게시하지 않았다.

## 바로 다음 작업

1. 원작 시작 장치의 6개 루트/트리거를 실제 서버·클라이언트의 비활성 복사본에서 재현한다. 남은 33개 명령 중 원작 보상/스폰/이동과 구조물·장치 유지 기능을 구분한다.
2. TURNBOUND가 소유하는 시작 위치·리스폰·게임모드 관리 경로를 먼저 검증한 후, 맵 보존 증거가 있는 기능만 추가 가역 경계에서 차단한다. 원작 코드를 복사해 대체하지 않는다.
3. 차단 후 기존 지형·문·포탈·시작 시설과 단독/멀티플레이의 플레이 흐름을 확인한다. 원작 예약 실행, 다른 업적·명령·점수판/스토리지도 별도 선별 검토한다.

**검증:** CODE REVIEWED, ORIGINAL WORLD OFFLINE TESTED, EXACT RESTORE VERIFIED.
**미검증:** PLAYTESTED, MULTIPLAYER TESTED, STRUCTURE FUNCTIONALITY TESTED, MAP SEPARATION COMPLETE.
