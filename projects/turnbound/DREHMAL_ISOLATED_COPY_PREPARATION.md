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
