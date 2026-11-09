# Drehmal 2.2.2f — 월드 저장 시점의 예약 실행 상태 확인

## 범위
- Minecraft **1.20.1**의 `level.dat` -> `Data.ScheduledEvents`만 실제 저장된 예약 실행 콜백으로 해석한다.
- 원작 함수의 `schedule function` 텍스트 854건은 예약 실행을 **만들 수 있는 정적 코드**이며, 저장된 큐와 별개다.
- 오프라인 읽기 전용으로 원본 `level.dat` SHA를 확인한 후, 저장된 이벤트 이름/콜백 타입/함수 ID/트리거 틱만 추출한다.
- `scoreboard.dat`의 *목표(Objective) 이름과 조건*만 조사하고 플레이어 점수/UUID는 수집하지 않는다.
- 원작 저장소의 `command_storage_*.dat`는 파일명/크기/SHA만 기록하고 저장 데이터 내용은 읽지 않는다.
- 명시적 재사용 승인이 없는 원작 함수·장치 코드는 TURNBOUND에 **전혀 이식하지 않는다**.

## 안전성
- 데이터셋: 해시 확인된 공식 Drehmal v2.2.2f의 임시 원본. 원본을 다른 브랜치에 복사하거나 재배포하지 않는다.
- 실패 시 중지. 예약 큐가 비어 있더라도 플레이 중 재예약 가능하므로 완전 분리로 판정하지 않는다.
- 월드 내용, 플레이어 저장 데이터, 데이터팩 함수를 수정하지 않는다.
- [GitHub Actions 결과](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-saved-schedule.yml)에서 실제 파일 검증 여부를 확인한다.
- 구조물 조작에 필요한 예약 실행은 삭제 대상이 아니다. 실제 콜백의 기능은 별도 분리 감사에서 판정한다.

## 이후
저장된 이벤트 중 존재하는 원작 콜백과 `scoreboard.dat`에 기록된 장치 진행도 상태를 기준으로, **스토리/전투/보상**과 **문/레버/퍼즐/포탈** 호출 관계를 나누고 복사본에서만 선별 비활성화를 검토한다. 모든 map separation 검사 통과 전 최종 맵 변환을 완료했다고 하지 않는다.

## 공식 원본 v2.2.2f 실검사 결과 (2026-11-09)

[저장된 함수 예약 CI #2](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42607952302) **SUCCESS**. 원본 1.20.1 `Data.ScheduledEvents`에는 `terminus:gifts/respawn_gift` 예약 이벤트 **1건**이 있고, 현재 저장된 월드 시간보다 예정 시간이 131틱 뒤임. 다른 저장 예약 이벤트는 발견되지 않았으나 실행 중 추가될 수 있음. 저장된 scoreboard objective **539개**, `command_storage_*.dat` 파일 **12개** 발견. 여기에는 원작 게임 진행도 및 구조물 제어가 섞일 수 있으므로 모두 보존 검토 대상으로 남긴다.

[저장 보상 예약 1건 + 기존 3종 통합 차단·복구 CI](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42608494226) **SUCCESS**. 예약 큐 수정은 별도 사본의 원본 `level.dat`에서만 수행되며 데이터팩·Terminus 차단 이후에 적용하고 복구 시 가장 먼저 되돌린다. 원작 코드는 TURNBOUND에 이식하지 않음. [분리 도구](DREHMAL_SAVED_REWARD_PREVIEW.md).
