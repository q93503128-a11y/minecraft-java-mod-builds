# TURNBOUND — Drehmal 원작 콜백 12건의 실제 명령 효과 정적 감사

## 출처와 검증 범위

- [GitHub Actions #42745349874 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42745349874), 2026-11-10. 기존 감사 자료 SHA-256과 공식 Drehmal: APOTHEOSIS v2.2.2f 원본의 level.dat SHA를 확인하고 4개 데이터팩의 mcfunction 본문 **18,771개 모두** 함수별 원본 SHA를 대조했다.
- 이전 8번째 실험의 9개 원작 비무기 보상 콜백, 시설 후보 3개(foundry 레버, godmachine 버튼, avsal 터미널), 총 **12개**의 직접 명령 및 정적 함수 호출/예약 실행 도달 경로를 분석했다.
- 함수 명령을 메모리에서만 읽어 root/effective operation, 점수판 역할 및 용도 범주를 집계한다. 결과물에는 원작 명령 문자열, 선택자·NBT·원본 데이터팩/지도 바이너리가 없다. 실제 월드와 게임 소스 수정 **0건**.
- 단위 테스트 5건 PASS. 공개 결과는 파생된 명령 집계만 포함. 검사 도구 tools/inspect_drehmal_callback_command_effects.py; 대응 테스트 tools/test_inspect_drehmal_callback_command_effects.py.

## 시설 연동 핵심 증거

| 원본 업적 보상 콜백 | 확인한 정적 함수 범위 | 명령 종류·횟수 | 보존 판단 |
|---|---:|---|---|
| avsal 터미널 | 6개 함수 | setblock **4**, teleport **1**, schedule **2**, scoreboard **5**, kill **2**, function **3**, particle **77**, playsound **4** | 월드 구조 및 이동 장치와 잠재적으로 직접 연결. **차단 금지·시설 검증 우선** |
| foundry 레버 2_check | 3개 함수 | scoreboard **2**, tellraw **3**, playsound **1**, function **2**, advancement **1**; 직접 tp/teleport **0** | 레버 상태/진행을 점수판으로 제어할 가능성. **차단 금지** |
| godmachine 버튼 enable | 1개 함수 | scoreboard **1**, advancement **1**, playsound **4** | 직접 블록 변경은 없으나 원작 진행 상태 관련. **차단 금지** |

**분류 충돌 주의:** 이전 함수 그래프에서 foundry 경로에 teleport 범주가 표시되지만 이번에 확인된 3개 함수의 직접 명령 목록에는 이동 명령이 0개였다. 정적 범주 휴리스틱과 실제 리터럴 명령 종류가 다르다는 뜻이며, 게임 내 이동 불가능이나 기능 안전성을 의미하지 않는다.

## bowbladEffect는 단일 보상 콜백만의 데이터가 아님

- 서로 다른 원작 함수 **5개**에서 해당 점수판 목적을 직접 참조: core:init, entities:misc/arrow_data, players:items/ultva_bowblade/has_effect, players:items/ultva_bowblade/kill_entity, players:main.
- 실제 명령에서 파싱된 참조 역할 합계: objective_admin **1**, score_condition **6**, score_write **3**. 원작 kill_entity 콜백에는 직접 score_write **1**과 playsound **3**, advancement **1**이 확인됐다.
- 다른 참조 함수가 살아 있다면 해당 콜백의 차단이 점수판 갱신을 끊어 상태가 달라질 수 있다. 반대로 다른 실행 경로가 분리됐다는 사실만으로 안전을 확정할 수 없다. 함수별 트리거 활성화 및 실제 원작 몹/보스·아이템 효과는 추가 검증 대상.

## 기존 8번째 오프라인 실험의 9개 직접 함수 효과

| 기존 9개 대상 | 직접 확인한 효과 |
|---|---|
| kill_rhalon | advancement 1, tellraw 1 |
| hurt_by_dnzombie | advancement 1, effect 1, playsound 1 |
| hurt_by_blooddolphin | advancement 1, particle 1 |
| sleep_on_lodahr | advancement 1, tag 1 |
| get_craft_replace | tag 1 |
| virtuo_aegis | attribute 1, tag 2 |
| eat_rotten_meat | advancement 1, effect 3, title 2 |
| eat_gay_apple | advancement 1, effect 5, particle 7, playsound 3, team 1 |
| kill_with_bowblade | advancement 1, playsound 3, scoreboard write 1 |

9개 모두 원작 게임의 상태·연출·업적에 영향을 미칠 수 있으므로 실제 플레이용 자동 차단을 승인하지 않는다. 기존 실험은 독립 사본에서 이미 **완전 복구**된 기록이고, 이번에는 분리 적용 자체를 실행하지 않았다.

## 한계 및 다음 작업

1. 이 결과는 정적 리터럴 명령 종류·함수 호출/예약 경로다. execute 중첩, 조건, NBT, 매크로, 선택자, 외부 트리거까지 해석한 실제 Minecraft 의미 분석 또는 플레이테스트가 아니다.
2. bowblade 점수판 사용 함수 5개가 기존 TURNBOUND auto-root 차단 및 다른 원본 진입 경로에 따라 실행 가능한지 읽기 전용으로 더 분석할 필요가 있다. avsal 터미널은 입구·퍼즐 작동 테스트 전 분리하지 않는다.
3. 건축물·문·포탈·원작 퍼즐 유지에 필요한 명령은 보존하고, 검증된 비시설 전투/보상 경로에 한해 별도 가역 실험을 승인한다. 시점 현재 **실제로 게임용 차단 승인된 콜백 0건**.
4. 직접 플레이테스트, 플레이어 join/respawn, 멀티플레이, 전체 원작 게임 분리, TURNBOUND 완전 플레이 가능 판정: **NOT TESTED / NOT COMPLETE**. 원작 코드 재사용·배포 0건.


## 2026-11-10 후속 Bowblade 진입점 역방향 검사

[공식 CI #42747001893 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42747001893). 기존 원작 함수 18,771개 그래프와 업적 보상 220건, 명령 블록 내 직접 함수 참조 78건, 예약 실행 명령을 포함한 함수 672개를 이용한 정적 진입 경로 역추적. `bowbladEffect` 참조 5개 중 **4개는 원작 load/tick 자동 실행 진입점을 통해서만**, 나머지 **1개는 업적 `kill_with_bowblade` 보상 함수 경로로만** 알려진 리터럴 루트와 연결됐다. `avsal`, `foundry`, `godmachine` 콜백은 각각 다른 원작 업적에 연결돼 유지 중. 이 결과는 기존 8단계 **오프라인 실험**이 알려진 리터럴 루트 차단 측면에서 일관적임을 나타낼 뿐, 실제 런타임의 동적 실행 가능성이 없음을 증명하지 않는다. 외부 모드/실사용 월드 변경 없이 분석 전용 테스트 4건 PASS. [자세한 판정](DREHMAL_BOWBLADE_ENTRYPOINT_BOUNDARY_REVIEW.md).
