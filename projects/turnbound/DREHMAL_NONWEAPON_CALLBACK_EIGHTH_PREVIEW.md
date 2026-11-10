# TURNBOUND — Drehmal 비무기 업적 콜백 8번째 오프라인 분리 실험

**대상**: 공식 Drehmal: APOTHEOSIS v2.2.2f (Minecraft 1.20.1)의 SHA 검증된 임시, 비활성 복사본. TURNBOUND: RE가 아니다. 게임 기획과 실제 소스 코드는 수정하지 않았다.

## 대상 선정과 제한

- 원작 업적 보상 함수 연결 기록은 무기 39개 외에 **181개**로 기존 공식 감사에서 검증됨. 그중 **9개**의 `rewards.function` 연결만 선택. 이들은 기존 보고서상 `ENTITY_COMBAT_REVIEW` 또는 `PLAYER_REWARD_OR_STATE_REVIEW`이고 `minecraft:location` 트리거가 아니며, 정적 함수 그래프에서 직접 호출자·자동 루트·추가 함수·world-edit·schedule 경로가 관측되지 않았다는 조건을 만족한다.
- 실제 선정: 원작 `entities` 업적 `kill_rhalon`, `hurt_by_dnzombie` (2); 원작 `players` 업적 `hurt_by_blooddolphin`, `sleep_on_lodahr`, `get_craft_replace`, `virtuo_aegis`, `eat_rotten_meat`, `eat_gay_apple`, `kill_with_bowblade` (7). 파일명은 식별자이며 원작 함수·명령 텍스트를 인용·재사용하지 않는다.
- 제외: 6개 `near_trial` 위치 트리거, 추가 `oblivion_area` 위치 트리거, 세계 변경·텔레포트·예약/퍼즐/구조물 관련 172개 콜백 전부. **정적 그래프에 위험 플래그가 없다는 것과 실제 제거 안전성은 서로 다르다.** 9개도 제거 승인 아님.

## 실제 적용 범위

1. 기존 6단계 복사본을 생성한 뒤 7번째 시작 명령 차단까지 성공·검증해야 이 8번째 작업이 허용된다.
2. 별도 `tools/preview_drehmal_nonweapon_callbacks.py`의 `inspect`가 기존 공식 업적 콜백 검토 산출물의 전체 SHA-256 및 9개 원본 JSON 파일별 SHA/콜백 ID/범위를 확인한다.
3. `apply`에서 `hi_drehmal.zip`의 9개 업적 JSON에 있는 `rewards.function` **연결만 제거**하고 `criteria`, `display` 및 그 외 JSON 필드를 보존한다. 다른 ZIP 구성원은 원시 내용 완전 동일. ZIP 전체의 원본 백업과 SHA 영수증을 변경 전에 생성.
4. 독립 `verify`에서 변경 데이터팩/백업 SHA와 대상 9개 내용·비대상 ZIP 전체의 바이트 동일성을 검증. `restore`는 정확히 기존 7번째 상태의 ZIP 해시로 복구한 뒤 선행 7번째 검증을 다시 수행.
5. 별도 오프라인 사본에만 실험하고 8→7→6단계 역순 복구한 후 원래 세계 상태 SHA를 재검증한다. 정상 4/5종 운영 옵션과 원본·사용자 월드는 손대지 않는다.

## 근거 및 결과

- [8번째 원본 콜백 분리 실험, GitHub Actions #42741395799](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42741395799): **SUCCESS**. Python 회귀 3개 PASS, 9개 수정/재검증, 다른 ZIP 구성원 원시 내용 유지, 일곱 번째로 정확 복구 후 여섯 단계 복구 PASS.
- [기존 181개 콜백·예약 함수 정적 분류 #42605435141](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42605435141): source callback report SHA `e66e573ce57baf44dd5b4e386cc45190a13f5fed78b45f1691866f99c48ddf2f`.
- 실험 코드: `tools/preview_drehmal_nonweapon_callbacks.py`, 테스트: `tools/test_preview_drehmal_nonweapon_callbacks.py`, workflow: `.github/workflows/turnbound-drehmal-eight-callback-preview.yml`.

## 검증하지 않은 것

- START 시설 실제 사용, 플레이어 join/respawn, 포탈/문·퍼즐 작동, 9개 콜백의 체감 변화, 멀티플레이, TURNBOUND 실게임 흐름은 **NOT TESTED**.
- 해당 9개와 나머지 172개의 원작 의존성은 Minecraft 런타임에서 계속 검토해야 한다. 원작 나머지 직접 명령/보상/예약 함수와 명령 저장소도 아직 남아 있다.
- 원작 코드 이식·재사용/원본 맵 변경/맵 패키지 배포는 **0건**. 이 단계는 *완성된 게임용 지도*가 아니다.


## 후속 교차 감사로 확인된 보존 경고 (2026-11-10)

[모든 원작 콜백 181건의 교차 참조 확인 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42743577751). 8번째 실험에서 차단 대상이었던 `data/players/advancements/kill_with_bowblade.json`의 함수가 보존된 원작 점수판 `bowbladEffect`를 참조하는 **표본 증거**를 추가로 발견했다. 기존 단순 분류에서 이 의존성이 드러나지 않았으므로 **실제 플레이 월드에서의 안전한 차단 승인으로 해석할 수 없다.** 8번째 가역 실험은 원본 복원까지 완료되어 데이터 손상은 없었다. 향후 실사용 전 이 항목을 우선 메커니즘 조사 대상으로 보존한다. [교차 위험 보고](DREHMAL_CALLBACK_MECHANISM_CROSS_REVIEW.md).
