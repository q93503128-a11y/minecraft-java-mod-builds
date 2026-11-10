# TURNBOUND — Drehmal 원작 Bowblade 점수판 실행 진입점 검증

## 기준과 근거

- 대상: TURNBOUND `turnbound-content-rebuild`, Minecraft 1.20.1 Fabric, 공식 Drehmal APOTHEOSIS 2.2.2f의 **원작 정적 감사 자료만 사용**.
- [GitHub Actions #42747001893 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42747001893). 회귀 테스트 **4개 PASS** 및 원작 SHA 고정 데이터 재검증 PASS.
- 원작 함수 그래프 **18,771개**, `rewards.function` 업적 보상 기록 **220건**, 직접 명령 블록 함수 호출 참조 **78건**, 예약 실행을 포함한 원작 함수 **672개**를 역방향 호출 그래프로 통합했다.
- 원본 그래프 SHA: `e556e386b5c2590330136c6d056d28d4aa209f8db6d0f380f8cf299946fea826`; 원본 보조 실행 감사 SHA: `9092055106167e69e55091151acfa2af86ed468b1aa171dcb2dfe07ecd1897ef`; 원본 18,771개 함수의 실제 명령 효과 감사 SHA: `5b90a636c23b9053e812facd1396c97327c8be4ccefa3e91d612e1845f448153`.
- 원본 `mcfunction`·명령문·NBT·Drehmal 지도 파일을 저장소 또는 결과물에 복제하거나 배포하지 않았다.

## bowbladEffect 관련 원작 함수 5개의 알려진 리터럴 진입점

| 점수판을 직접 참조한 함수 | 원작 정적 진입 경로 | 기존 8번째 **비활성 사본** 실험 적용 시 |
|---|---|---|
| `core:init` | 원작 `minecraft:load` → `core:init` | 자동 실행 태그 차단 대상 |
| `entities:misc/arrow_data` | `entities:main` → `entities:proj_tick` → `entities:spectral_arrow_tick` → `entities:misc/arrow_owner` → 대상 | `minecraft:tick` 자동 실행 태그 차단 영향권 |
| `players:items/ultva_bowblade/has_effect` | `entities:main` → `players:main` → 대상 | `minecraft:tick` 자동 실행 태그 차단 영향권 |
| `players:main` | `entities:main` → `players:main` | `minecraft:tick` 자동 실행 태그 차단 영향권 |
| `players:items/ultva_bowblade/kill_entity` | `data/players/advancements/kill_with_bowblade.json` 업적 보상 → 대상 | 8번째 오프라인 실험의 보상 연결 차단 대상 |

- 다섯 함수 모두 원작 전체 *리터럴* 함수 호출·예약·업적·명령 블록 정적 그래프에서 위 진입점 외의 다른 루트가 발견되지 않았다. 이 범위의 임시 **8단계 분리본**에서는 알려진 리터럴 루트가 **0개**로 계산된다.
- 하지만 **자동 실행 태그가 사라져도 함수 본문은 존재**하며, 동적 호출, 명령 블록의 다른 종류 명령, 원작 저장 상태와 이벤트에 따라 실행될 가능성은 별도로 남는다.
- 따라서 알려진 진입점 0은 '실제 게임에서 안전하게 완전히 제거됨'이 아니라 **고정된 정적 출처 모델의 범위**를 뜻한다.

## 원작 시설 콜백 3종의 독립 진입점

| 시설 | 별도 업적 보상 진입점 | 안전 조치 |
|---|---|---|
| Avsal 터미널 | `data/players/advancements/terms/avsal.json` → `core:scene/terminals/avsal_check` | 이동/블록 변경 명령이 확인되었으므로 보존 |
| Foundry 레버 | `data/foundry/advancements/2.json` → `foundry:levers/2_check` | 점수판/레버 상태 연관 가능성으로 보존 |
| Godmachine 버튼 | `data/core/advancements/godmachine/godbutton.json` → `core:scene/godmachine/enable` | 진행 상태 관련 가능성으로 보존 |

세 시설 모두 원작 자동 실행 루트 차단과 별개로 **업적 보상 연결 자체는 유지**된다. 이 사실은 시설이 실제로 정상 작동한다는 증거는 아니다.

## 검증 범위와 잔여 위험

1. 검사 코드 `tools/review_drehmal_bowblade_entrypoints.py`, 단위 테스트 `tools/test_review_drehmal_bowblade_entrypoints.py`, CI `.github/workflows/turnbound-drehmal-bowblade-entrypoints.yml`. 추정이 아닌 기존 공식 SHA 고정 조사 결과를 기반으로 결과를 재계산한다.
2. 원작 `load/tick` 태그 삭제와 9개 업적 보상 연결 삭제는 **오프라인 임시 사본에서 검증된 실험**일 뿐, 실제 게임 월드에 8단계를 승인한 적이 없다.
3. 동적/매크로 명령, 업적 달성의 실제 실행, 저장된 함수 예약, 레드스톤, 문·포탈, 플레이어 입장·리스폰, 멀티플레이는 **NOT TESTED**.
4. 원작 시스템을 그대로 이식하거나 TURNBOUND의 기존 게임 설계를 재작성하지 않았다. **실사용 차단 승인 0, 원본 월드 파일 변경 0, 전체 맵 분리 NOT COMPLETE**.
5. 다음 실질적 단계는 시설 안전성 확인과 TURNBOUND 자체 시작/리스폰 관리의 서버 소유 검증이다. 단순히 원작 함수 분류를 반복해 게임 완성으로 계산하지 않는다.
