# TURNBOUND — 원작 4개 경계 통합 분리 복사본 준비

이번 단계는 이전의 개별 '적용 후 즉시 복구' 검증을 **일시적으로 보존 가능한 별도 오프라인 월드 사본**의 생성/재검증/복구 절차로 통합한다.

## 경계
- **원본**: 공식 Drehmal v2.2.2f의 SHA 검증된 3개 ZIP을 임시 폴더에 추출한다.
- **작업 대상**: `drehmal_pack_boundary.py stage`가 만든 **새로운 별도 사본**만 허용한다. 원본/실제 플레이/활성 TURNBOUND 월드는 거부.
- **적용 대상만 변경**: 원작 무기 업적 보상 39개 → 원작 자동 `load/tick` 3개 → Terminus 명령 블록 62개(30개 지역 파일) → 월드 저장 예약 선물 함수 1건. 각 원본 SHA와 검토 보고서 SHA가 맞지 않으면 적용 거부.
- **검사**: `prepare_drehmal_isolated_copy.py inspect/prepare/verify/restore`는 기존 검증 도구 4개를 한 곳에서 호출하고, 패치가 완료된 사본을 **즉시 복구하지 않고 유지한 상태에서 재검증**한다.
- **복구**: 저장 예약 → Terminus 명령 블록 → 자동 시작 루트 → 무기 업적 순서로 되돌리고 SHA 검증. 적용 중 실패하면 백업/영수증을 유지하여 데이터 유실을 막는다.
- **배포 금지**: 검증 CI는 임시 사본만 저장하고 원본/변경된 월드 자체는 GitHub/아티팩트에 업로드하지 않는다. 원작 저작권 및 배포 권한 문제와 별개다.
- **완료 아님**: 원작 보상·스토리·전투의 나머지 실행 경로와 수많은 점수판/장치 의존성은 아직 남아 있다. 이 복사본은 **오프라인 분리 작업을 검증하기 위한 후보**이지 게임 플레이가 가능한 최종 설치용 월드가 아니다.

검증: [GitHub workflow](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-retained-copy.yml).

원작 코드를 TURNBOUND 게임 로직으로 복사하거나 활용하는 것은 사용자의 별도 승인이 필요하다. 이번 단계는 원작 기능의 차단·복원만 수행한다.

## 공식 v2.2.2f 통합 검증 상태

[GitHub Actions #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42616136730): **SUCCESS** (2026-11-09). 단순 적용·복구 외에, 차단된 **사본을 복구 전에 유지하여 독립적인 `verify` 검사를 수행**했다. 4개 경계/62개 명령 블록/30개 지역 파일을 확인했고 원본 ZIP과 `level.dat` 해시를 복구하였다. 원본 또는 가공한 월드 전체는 커밋·아티팩트에 포함되지 않았다. 정식 플레이테스트와 전체 원작 시스템 분리는 여전히 필요하다.

## 2026-11-10 다섯 번째 선택적 분리 경계

기존 `prepare_drehmal_isolated_copy.py`에 `--include-gamemode`를 붙이면 [원작 `gamemode` 블록 29개](DREHMAL_GAMEMODE_COMMAND_SEPARATION.md)를 추가한다. 기존 4종은 기본 동작으로 유지한다. 검증 결과: [5종 공식 검사 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42725309538) / [4종 호환 검사 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42725309921). 작업은 검증된 월드 **사본**에서만 허용하며 배포/플레이는 아직 금지한다.

## 2026-11-10 여섯 번째 원작 시작 명령 경계 — 선택형 오프라인 실험

통합 도구에서 --include-gamemode --include-starter-experiment --direct-report (검증된 원본 직접 명령 보고서 경로)를 함께 지정할 때만 5종 분리 사본에 기존 24개 후보를 추가한다. [공식 원본 복사본 해시 체인·복구 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42733256037). 기본 4종/5종 절차는 유지. [실제 구조 연결 검토](DREHMAL_STARTER_CHAIN_INTEGRATION_REVIEW.md)에 따르면 **시작 체인 원작 명령 33개가 남고 시설 동작 미검증**이므로 실제 게임 월드에 사용할 수 없다.

## 2026-11-10 오프라인 7번째 시작 체인 실험

기존 5종 + 지급/발견도 24개와 별도로, tools/verify_drehmal_start_chain_isolation.py가 같은 비활성 복사본에 남은 시작 장치 명령 33개를 가역적으로 차단한다. [공식 실험 검사 SUCCESS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42735713657): 시작 체인 57개 명령 모두 빈 문자열 확인 → 7번째 오버레이 원본 복구 → 앞선 6단계 전체 복구. 기본 4/5종 동작에는 추가하지 않는다. **파일·해시 검사 성공일 뿐, 원작 시설의 실제 작동이나 안전한 플레이 맵 분리는 검증하지 않았다.** 자세한 내용은 DREHMAL_STARTER_CHAIN_INTEGRATION_REVIEW.md 참고.

## 2026-11-10 서버 구동 관측: 6/7단계 시작 장치 비교

[공식 Minecraft 1.20.1 Fabric 전용 서버 검사 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42738739482). 공식 원본에서 각각 새로 생성한 6단계와 7단계 임시 사본을 순차 서버 구동. 169/165틱과 시작 영역 청크 로딩, 명령 블록 57개(각각 Command 비움 24/57), 양쪽 레드스톤 주입 6곳, 각 1,440 구조 상태 전후 비교(주입 제외 변화 0), 정상 종료를 확인했다. **비활성 복사본에서 플레이어 없이 실행한 장면 스모크**로 한정하며 게임 입장·문·포탈·리스폰 기능 보존, 플레이테스트 및 멀티플레이는 주장하지 않는다. 시험 사본은 전부 폐기하고 원본/변경 월드·서버 전체 로그를 아티팩트에 올리지 않았다. 자세한 판정은 DREHMAL_STARTER_CHAIN_INTEGRATION_REVIEW.md 참고.

## 2026-11-10 8번째 임시 업적 보상 연결 분리

별도 도구 tools/preview_drehmal_nonweapon_callbacks.py가 기존 7단계 공식 사본 검증 후에만 원작 비구조 이벤트 업적 reward.function 9건을 SHA 고정 조건으로 가역 차단한다. [공식 #42741395799 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42741395799): 9개 변경, 다른 ZIP 구성원의 원시 내용과 업적 달성 조건 유지, 8→7→6 전체 복구. **8번째 처리는 기존 4/5종 기본 도구에 포함되지 않고 플레이 불가 실험에만 해당한다.** 실제 시설/플레이 기능 무영향은 입증되지 않았으며 남은 비무기 보상 콜백 172개, 다른 명령 블록/예약 함수 등은 여전히 조사 대상. [상세](DREHMAL_NONWEAPON_CALLBACK_EIGHTH_PREVIEW.md).
