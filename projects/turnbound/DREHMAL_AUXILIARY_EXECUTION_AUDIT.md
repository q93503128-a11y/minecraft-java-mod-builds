# Drehmal 원작의 비함수 실행 경로 추가 감사

2026-11-09 현재 자동 load/tick 차단 + 62 Terminus 명령 블록 차단은 정식 맵 분리 완료가 아니다.

이후 필요한 검증 경계: 기존 통계에 없는 **명령 블록의 직접 명령** (scoreboard, execute, give, summon, setblock, tp 등), 업적 달성 보상 함수, mcfunction 안의 예약 schedule function 호출, 데이터팩 내부 JSON 콘텐츠 종류를 읽기 전용으로 조사한다.

도구: tools/audit_drehmal_auxiliary_sources.py. 공식 v2.2.2f ZIP 세 개의 SHA-256 검증 후 임시 월드를 구성하며, 원작 명령어·JSON 본문·함수 본문을 보고서에 담지 않고 명령 종류·참조 ID·원본 SHA·위치·호출 분류만 기록한다.

분류는 **제거 허가가 아니다**. 지형, 건축물, 문, 포탈, 효과와 원작 게임 기능이 얽혀 있을 수 있으므로 각 원작 장치의 실제 역할을 판정해야 한다. 검토된 원작 코드 재사용은 반드시 사용자 승인 별도로 필요하다.

실행: [auxiliary execution audit workflow](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/workflows/turnbound-drehmal-auxiliary-audit.yml). 실행 전/후 사용자의 월드, 기존 Git 작업, TURNBOUND 게임 코드는 변경하지 않는다.

## 공식 월드 감사 결과

[TURNBOUND Drehmal original auxiliary execution audit #1](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42591982023): **SUCCESS**. 지역 파일 2,461개 검사 중 경고 737개(짧은 region header), 명령 블록·광차 2,232개. 업적 501개 중 보상 필드가 있는 기록 292개(함수 보상 220, 경험치 보상만 72), 예약 함수를 포함하는 원작 함수 672개(호출 854건) 발견. `playsound`와 `particle`, `setblock` 등은 원작 건축물 연출과 관련될 수 있어 일괄 비활성화 금지. `gamemode`, `give`, `scoreboard`, `summon` 등도 이름만으로 제거를 확정할 수 없다.
