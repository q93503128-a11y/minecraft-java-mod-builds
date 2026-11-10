# Drehmal 2.2.2f 원작 업적 콜백/시설 의존성 교차 검토

## 작업 의도와 원본 경계

TURNBOUND `turnbound-content-rebuild`의 비무기 업적 보상 함수 연결 **181건 전체**를 대상으로 기존 개별 감사 결과를 하나의 보존 위험표로 통합한다. 실제 Drehmal 월드 파일을 로드/수정하지 않는 **메타데이터 전용 감사**다. 기존 8단계 가역 분리 실험의 성공을 실제 게임용 자동 제거 승인을 의미하는 것으로 확대하지 않는다.

## 실제 검증 근거

- [GitHub Actions 교차 조사 수정본 #42743577751 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42743577751), Python 회귀 테스트 6건 통과.
- 원본 official function graph: SHA `e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826` (18,771 functions).
- 원본 명령/예약/업적 감사: SHA `9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef` (명령 2,232, 예약 함수를 포함한 원본 함수 672).
- 기존 181개 업적 위험 분류: SHA `e66e573ce57baf44dd5b4e386cc45190a13f5fed78b45f1691866f99c48ddf2f`.
- 원작 objective/storage 참조 표본 감사: SHA `37b8ba75e77e87cac6b62c7c7ef8665c85035143e36d08b1465c2e92b04ec95d` (저장된 objective 539, command storage 12).
- 소스와 회귀 검증: `tools/review_drehmal_callback_activation_boundaries.py`, `tools/test_review_drehmal_callback_activation_boundaries.py`, `.github/workflows/turnbound-drehmal-callback-cross-review.yml`. 공개 아티팩트는 원작 명령/함수 내용 또는 월드 파일이 아닌 파생 의존성 메타데이터만 포함한다.

## 확인된 중첩 위험 신호

| 위험 신호(정적 분석) | 원작 업적 보상 함수 연결 |
| --- | ---: |
| 예약 함수 호출 경로 | 45건 |
| world_edit 표식 함수로 도달 | 42건 |
| teleport 표식 함수로 도달 | 138건 |
| 위치·차원 감지 업적 | 53건 |
| 점수판 표본 참조 함수와 교집합 | 53건 |
| 명령 저장소 표본 참조 함수와 교집합 | 4건 |
| 정의되지 않은 함수 정적 참조 경로 | 5건 |
| 다른 함수의 직접 호출이 있는 업적 콜백 | 7건 |

위 항목들은 겹치므로 합산할 수 없다. `teleport`는 원본 감사의 정적 명령 **범주**로 실제 순간이동이나 기능 손상을 확인했다는 뜻이 아니다. 기존 점수판/저장소 감사 보고서는 함수 전체 명단이 아니라 일부 *sample_function_ids*를 기록하기 때문에 **양성 연결은 참고할 수 있지만 미검출을 독립성 증거로 사용할 수 없다**. 리터럴 명령 블록 함수 호출을 통해 현재 181개 콜백 ID에 도달하는 경로는 이 정적 그래프에서 관측되지 않았으나 동적 실행·JSON 조건·외부 트리거에 의한 진입은 여전히 가능하다.

## 기존 8단계 실험과의 대조

- 이전 8단계에서 원작 업적 보상 연결 9건을 별도 복사본에서 일시 비활성화하고 정확히 복원했다. 이번 검사에서는 9건 중 `players:items/ultva_bowblade/kill_entity` (`data/players/advancements/kill_with_bowblade.json`)가 점수판 `bowbladEffect`와 표본 함수 참조를 공유함을 발견했다. 이 연결은 기존의 위험 필터만으로 놓칠 수 있어 보존·추가 조사 대상으로 올린다.
- 나머지 8개는 현 자료의 정적 위험 신호가 상대적으로 적을 뿐, 원작 게임 내 효과나 구조물 연관성이 없다고 검증된 것이 **아니다**. 이 8개 역시 기존 임시 복사본의 실험 범위를 벗어나 실사용 월드에서 자동 차단하지 않는다.
- 현재 우선 보존/동작 조사 **173건**, 기존 8단계의 오프라인 실험 전용 **8건**으로 표기한다. 실제 사용에 승인된 자동 삭제/비활성화 건수는 **0**.

## 실패 경로와 한계

- 최초 CI [#42743302009](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42743302009)는 단위 테스트는 통과했지만 스케줄 목록을 `defaultdict`로 잘못 채워 예약 실행이 179건으로 과다 집계됐다. 이미 원본 감사와 모순됨을 확인하고 수정했다. **현재 정본은 672개 예약 source를 유지한 수정 CI #42743577751**이며 이전 결과를 판정 근거로 사용하지 않는다.
- 정적 함수 호출, schedule literal 관계와 점수판 참조 표본에는 동적 매크로, 업적 grant 조건, 저장된 명령 스토리지 실행, 원작 문·포탈 장치의 실제 실행 경로에 대한 완전성 보장이 없다.
- 실게임 플레이, 플레이어 join/respawn, 시설 동작/멀티플레이 **NOT TESTED**. 라이선스 확인 없는 원작 코드 이식/원본 맵 배포/운영용 데이터팩 반영 없음.

## 다음 개발/검증 경계

1. 우선 `kill_with_bowblade` → `bowbladEffect` 연결과, location/impossible/월드 변경/예약 실행 콜백 중 실제 원작 시설 상태에 영향을 주는 명령의 출처를 원본 공식 사본에서 **읽기 전용으로 기능별 분석**한다. 원문·NBT/원본 코드 배포 없이 명령 종류/대상/호출 구조만 추출한다.
2. 구조물·퍼즐/문/포탈 운영에 필수적인 경로와 스토리·보상만 담당하는 경로를 분리한다. 정적 증거만으로 여전히 구분할 수 없다면 판정을 유보하고, 필요한 경우 사람 없이 전용 서버에서 추가 검증한다.
3. 그 뒤 확인된 범위만 독립 가역 실험으로 편입하고, TURNBOUND 자체 월드 진입·리스폰 시스템을 승인된 장소/정책에 따라 구현한다. 그 전에는 플레이 가능한 맵으로 선언하지 않는다.

## 2026-11-10 직접 원본 명령 효과 비교: 정적 범주와 명령 효과 불일치 확인

- [공식 GitHub Actions #42745349874 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42745349874). 원본 4개 데이터팩에 있는 **18,771개 함수 본문의 SHA를 모두 검증**하고, 실험 중인 9개 콜백과 시설 3개 콜백의 직접/정적 간접 명령을 분류했다. 함수 내용을 GitHub에 재배포하지 않음.
- **avsal 터미널:** 정적 호출·예약 경로 6개 함수에 setblock **4**, teleport **1**, schedule **2**, scoreboard **5**, kill **2**가 존재한다. 원래 시설·이동 장치 사용과 관계될 가능성이 높아 현재 설정 유지 및 직접 제거 금지.
- **foundry 레버:** 정적 호출 경로 3개 함수에 scoreboard **2**, tellraw **3**, playsound **1**, function **2**, advancement **1**. 직접 명령 분류상 tp/teleport는 **0**이지만 이 사실만으로 다른 동적 실행이나 장치 연관성이 없다고 단정하지 않는다. 기존 그래프의 teleport 분류는 정적 위험 표시로 남기되 직접 이동 명령 확인과는 구별한다.
- **godmachine 버튼:** 원본 콜백 함수 1개의 직접 명령에 scoreboard **1**, advancement **1**, playsound **4**. 블록을 직접 변경하지 않는다는 이유로 해당 이벤트를 제거하지 않는다.
- **bowbladEffect:** 서로 다른 원작 함수 5개에서 점수판 참조를 검출했고, 총 역할은 objective_admin **1**, score_condition **6**, score_write **3**. 기존 8번째 실험의 bowblade 보상 함수가 실제 score_write **1**을 수행해 나머지 참조자와 연결되는 사실을 확인했다. 따라서 실제 게임용 차단 후보에서 제외 유지.
- 기타 8번째 실험 대상에도 effect/team/tag/attribute/advancement 등 원작 플레이어·엔티티 상태 변경 명령이 확인됐다. 정확한 기능을 게임 내에서 재현하지 않았으므로 **9개 모두 플레이용 차단 승인 0건**.

이번 검증은 원본을 수정하지 않은 read-only 명령 분류다. Minecraft 명령의 execute 중첩·매크로·NBT/동적 실행·구조물 행동을 완전 해석한 것이 아니며 실사용 승인이 아니다. [상세 명령 효과 보고](DREHMAL_ORIGINAL_CALLBACK_COMMAND_EFFECTS.md).


## 2026-11-10 Bowblade 점수판 활성화 경로 추가 교차 검토

[실제 GitHub Actions #42747001893 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42747001893)에서 기존 원작 근거 3종의 SHA와 역방향 함수 호출/예약 경로를 재검토했다. `bowbladEffect`에 관련한 다섯 함수는 기존 8단계 비활성 사본의 자동 태그/업적 보상 연결 차단 범위에 포함되지만, 동적 함수 호출이나 활성화된 플레이어 및 시설 상태는 검증하지 않았다. 반면 원작 시설 3개 콜백에는 독립 업적 보상 진입점이 남아 있다. **원작 시설 임의 분리 금지, 실제 게임용 차단 승인 0건 유지.** [분석 문서](DREHMAL_BOWBLADE_ENTRYPOINT_BOUNDARY_REVIEW.md).
