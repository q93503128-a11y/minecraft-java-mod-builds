# 원작 저장 예약 보상 1건 가역 분리 검증

Drehmal v2.2.2f 공식 맵 저장 시점에 실제 예약된 이벤트 `terminus:gifts/respawn_gift`가 1건 발견됨. `Data.Time=1135083463`, 이벤트 `TriggerTime=1135083594`, 저장 기준 차이 131 tick. 해당 함수의 정적 `called_by`에는 `core:init`과 자기 자신이 있으며, 원작 선물 관련 함수를 호출함. 따라서 `load/tick`만 차단해도 이 **저장된 이벤트가 실행되어 원작 보상 처리가 재개될 위험**이 존재함.

## 오프라인 분리 경계
- 공식 `level.dat` SHA, 저장 이벤트 이름, 콜백 타입, 실행 tick, 저장 월드 시간을 정확히 확인한 **복사본만** 수정.
- `Data.ScheduledEvents`의 검증된 목록 항목 **1개만** 제거. NBT 나머지 필드 의미는 동일하도록 확인하고, 원본 `level.dat`는 백업 및 SHA 검증 후 복원.
- 기존 원작 무기 업적 보상 39개, 자동 load/tick 진입점 3개, Terminus 명령 블록 62개 차단·복원과 함께 통합 확인.
- 원본 월드 및 활성 TURNBOUND 월드는 절대 수정하지 않음. 원작 코드를 재사용하거나 역참/보상 시스템을 개발하지 않음.
- 게임 내부 문/포탈/퍼즐 동작과 새로운 예약 함수 발생은 아직 미검증. **맵 분리 미완료**.

검증: [공식 월드 4단계 가역 분리 CI](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-saved-reward-preview.yml).
