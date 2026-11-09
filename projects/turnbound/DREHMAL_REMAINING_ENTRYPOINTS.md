# Drehmal 원작 시스템 잔여 실행 경로 (읽기 전용)

출처: 공식 v2.2.2f의 [함수 호출 그래프](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42568764777)와 [명령 블록 위치 분석](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42574103795). 해당 보고서 SHA-256을 도구에 고정했으며 원본 월드/원작 함수 본문을 포함하지 않는다.

- 전체 함수 18,771개. 원작 load/tick의 세 자동 진입점에서 도달 가능한 정적 함수 16,517개.
- 명령 블록에 포함된 정적 함수 참조의 전체 도달 가능 함수는 314개, 그중 자동 진입점 그래프에 없던 함수 173개.
- **오프라인 복사본에서 검증한 Terminus 62개 명령 블록 차단 이후에도** 직접 함수 호출 블록 17개(함수 참조 21건)가 남는다. 이들의 **정적 호출 그래프는 함수 225개까지 이어진다**. 이는 실행 가능성 점검 대상이지 225개가 실제로 실행된다는 뜻은 아니다.
- 명령 블록 2,232개 중 문자 그대로의 함수 호출이 없었던 **2,153개는 명령 종류 자체가 아직 분류되지 않았다**. 단순히 명령이 없거나 안전하다고 간주하면 안 된다.
- 지역 스캔 경고 737개와 함수 참조 미해결 168개, 업적 및 예약 함수 등 다른 시작 지점이 있어 맵 전체 분리 상태는 아직 아니다.

목표는 맵 지형·건물·문·포탈·특수 장치를 보존하면서 원작 스토리·전투·보상만 분리하는 것이다. 원작 Drehmal 코드의 사용 가치가 발견되면 후보로 보고하며, 별도 승인 없이 TURNBOUND에 이식/활용하지 않는다.

후속 작업: 비함수 명령 블록, 예약 함수 및 업적/JSON 트리거를 읽기 전용으로 분석한 뒤 시설 보존 위험을 검토한다. 이번 작업에서 변경하는 것은 자체 분석 코드/보고 문서뿐이다.

[Verified workflow #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42591270837) **SUCCESS**. Source reports were pinned by SHA-256, and output retained source ID/coordinate metadata only. No map bytes or original script code were altered or copied. See also [auxiliary execution audit](DREHMAL_AUXILIARY_EXECUTION_AUDIT.md) for remaining direct commands/JSON callback inspection.
