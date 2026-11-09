# Drehmal 무기 전용 업적 보상 분리 — 오프라인 검증용

목적: 원작 Drehmal 2.2.2f의 무기 업적에 포함된 **보상 함수 및 제작법 해금**을 테스트용 별도 월드 복사본에서만 비활성화한다. TURNBOUND 캐릭터/무기 시스템을 만들거나 원작 코드를 재사용하는 작업이 아니다.

공식 [함수 그래프 #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568764777)와 [업적/예약 함수 감사](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42591982023)의 해시를 고정하여 동일 자료만 사용한다.

- `hi_drehmal.zip`의 `data/weapons/advancements/*.json` 중 **39개**. 모두 달성 조건이 `minecraft:inventory_changed`, `rewards`는 `function`과 `recipes` 두 필드.
- 39개 보상 함수는 `weapons:*` 함수이며 기존 정적 함수 그래프상 다른 함수의 직접 호출이 없고 load/tick 루트에도 포함되지 않는다.
- 복사본에서 해당 업적의 `rewards`를 빈 객체로 바꾸되, `criteria` 및 그 밖의 업적 필드를 그대로 유지한다. **업적 달성 상태를 이용하는 시설은 별도 검증이 필요하며, 적용만으로 안전하다고 판정하지 않는다.**
- 원본 ZIP의 SHA를 확인한 뒤 백업하고, 복사본 수정 후 해시 기록/정확한 복구를 수행한다. 오프라인 `turnbound-stage.json`이 없는 원본 및 활성화된 월드는 거부한다.
- 기존 automatic load/tick 3개 진입점 및 Terminus 명령 블록 62개와 같은 사본에 모두 적용 후 복원하는 통합 검증: [GitHub workflow](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-weapon-preview.yml).
- 이번 분리 도구는 정식 모드팩 맵 설치기가 아니며 게임 안의 지형·장치·맵 플레이 적합성은 미검증. 다른 업적 462개, 기타 원작 기능은 그대로 남는다. 원작 스크립트는 TURNBOUND에 채택하지 않는다.

## 실측 검증 결과 (공식 원본 v2.2.2f)

[TURNBOUND original weapon advancement reward isolation #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42603106525): **SUCCESS**. 공식 전체 월드에서 39개 보상 필드를 선택적으로 비활성화한 뒤, 기존 자동 실행 진입점 차단 및 Terminus 62개 차단과 함께 역순 복원 성공. 원본 `hi_drehmal.zip` SHA-256 재일치. 이 작업은 재사용/이식이 아니라 기존 원작 보상만 차단한 안전성 시험이며, 실제 게임 속 무기 획득·퀘스트·퍼즐 연결 검증은 남아 있다.
