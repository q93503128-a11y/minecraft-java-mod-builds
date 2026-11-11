# TURNBOUND — Foundry 원작 입구 clone 및 아레나 호출 경로 후속

2026-11-11, 턴 R / turnbound-content-rebuild. 앞선 공식 7+9 서버 검사 #42929635789에서 레버 3개 powered=true 유지 확인. 위키 대략적인 Foundry 좌표 주변의 블록 비교 320,013곳은 변경 0. 최초 범위와 실제 입구의 일치 여부가 확인되지 않았으므로 원작 메커니즘 장애를 확정하지 않는다.

[기초 원작 함수 정적 감사 #42930789759](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42930789759)에서 111개 Foundry 함수, Foundry 업적 3개의 item_used_on_block reward, 원작 직접 fill/setblock 좌표와 Foundry 함수 호출 연결을 추출했다. foundry:open/3 → foundry:arena_start와 foundry:wave_logic → wave_1~wave_6의 정적 연결이 확인됐다. 여기서 arena_start와 wave_logic이 같은 기능이라는 추정은 금물.

이번에는 **원작 clone 좌표 (source/destination)** 를 추가 분석하고, foundry:open/1~3, levers/1_check~3_check, arena_start, wave_logic 같은 함수에 대한 **모든 네임스페이스의 리터럴 호출자**를 읽기 전용으로 추출한다. 함수 본문/NBT/지도 파일/원작 코드 재배포 없음. 직접 실행·플레이/문 통행·원작 전투 비재진입의 실서버 검증이 아니며 승인된 맵도 아니다.

도구: tools/inspect_drehmal_foundry_gate_wave_paths.py
신규 검사: .github/workflows/turnbound-drehmal-foundry-clone-callers.yml

> **2026-11-11 원작 좌표 기반 보정:** [원본 clone 명령·함수 호출 정적 검증 #42931261435 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42931261435). 원작 foundry:levers/door는 소스 (-3274..-3270,134..142,1557..1558)를 **타깃 (-3278..-3274,156..164,1576..1577)** 으로 복제한다. 기존 #42929635789 검사 영역 Z=1454..1546에는 타깃이 없었으므로 그 범위의 변경 0건을 문 실패로 단정할 수 없다. 새 CI는 이 정확한 복제 대상과 소스 chunk를 명시적으로 로드해 세 레버 ON 전후를 비교한다.

MAP SEPARATION COMPLETE: NO.


## 2026-11-11 후속 실서버·원작 조건 판정

- [실제 손 아이템 우클릭 #42933135850 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42933135850): 7+9 원본 사본, 원작 데이터팩/명령 블록 ON, 3개 Foundry 레버 초기 OFF→동시 ON. 각 클릭 전후 점수판 엔트리 2건 변경. 원래 door clone destination은 변경 0건. 실제 문 열림 또는 reward 인과 확정 아님.
- [3개 업적 item_used_on_block 조건 #42932597961 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42932597961): lever block powered, 특정 위치/차원 predicate 확인, 명시적 아이템 ID 제약 미검출. check 함수에 원작 score 조건 존재.
- **미해결:** levers/main의 3개 점수/조건 충족 여부, source/destination의 초기 동일성, clone 실행의 동적 인과. 실제 아레나 6웨이브 비활성화·문 통행·4인 멀티·최종 맵 승인 모두 미검증. 다음은 단순 레버 반복이 아닌 원본 상태/조건 대조.

MAP SEPARATION COMPLETE: NO.
