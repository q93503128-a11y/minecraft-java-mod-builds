# Drehmal 2.2.2f — 점수판·명령 저장소 의존성

## 작업 범위
- 원작 공식 v2.2.2f에서 확인된 scoreboard **539개**와 command storage 파일 **12개**를 유지한 채, 데이터팩 함수와 명령 블록에서 참조하는 목적·사용 형태를 읽기 전용으로 분석.
- 파일명과 목표 ID가 비슷하다는 이유로 '퍼즐용' 또는 '원작 전투용'이라고 단정하지 않음. 명시적 참조가 없는 목표도 **제거 승인 대상 아님**.
- 공식 이전 [저장 상태 감사](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42607952302)와 [함수 그래프](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568764777) SHA-256을 고정하여 대조.
- 직접 command-block, `execute if score`, `execute store score`, `scoreboard players`, selector `scores={...}`, `data ... storage` 등의 정적 명령을 검사. **완전한 Minecraft 명령 인터프리터가 아니며** JSON, 동적 매크로, 손상 지역/다른 NBT 직접 참조는 놓칠 수 있음.
- 원본의 함수 몸체, 원시 명령문, player score/UUID, command storage NBT 본문은 저장소나 결과물에 기록하지 않음.
- 구조물 변경 명령을 함께 포함하는 함수는 **'동일 함수 내 연관'**만 표시. 점수판이 실제로 문·포탈을 제어하는지 플레이로 검증하지 않았으므로 자동 보존/삭제 판정은 하지 않음.

## 검증 및 이후 조치
[공식 GitHub Actions 검사](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-score-dependencies.yml)는 공식 파일을 임시로 다운로드·해시 검증하고 원본 전체를 읽기 전용으로 검사한 뒤 ID/함수/차원/좌표/참조 유형 등 요약 정보만 업로드한다. 본 단계에서는 맵 및 TURNBOUND 코드를 바꾸지 않으며, **원작 코드 재사용 승인 없음**.

조사 결과를 바탕으로 퍼즐/문/포탈과 원작 서사·보상·전투를 구분하고, 필요한 경우 **이미 검증된 월드 복사본에서만** 가역적으로 차단·복구한다. 해당 작업과 실제 실게임 플레이 검증 전까지 맵 분리 완료로 선언하지 않는다.
