# TURNBOUND — Foundry 원작 입구 clone 및 아레나 호출 경로 후속

2026-11-11, 턴 R / turnbound-content-rebuild. 앞선 공식 7+9 서버 검사 #42929635789에서 레버 3개 powered=true 유지 확인. 위키 대략적인 Foundry 좌표 주변의 블록 비교 320,013곳은 변경 0. 최초 범위와 실제 입구의 일치 여부가 확인되지 않았으므로 원작 메커니즘 장애를 확정하지 않는다.

[기초 원작 함수 정적 감사 #42930789759](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42930789759)에서 111개 Foundry 함수, Foundry 업적 3개의 item_used_on_block reward, 원작 직접 fill/setblock 좌표와 Foundry 함수 호출 연결을 추출했다. foundry:open/3 → foundry:arena_start와 foundry:wave_logic → wave_1~wave_6의 정적 연결이 확인됐다. 여기서 arena_start와 wave_logic이 같은 기능이라는 추정은 금물.

이번에는 **원작 clone 좌표 (source/destination)** 를 추가 분석하고, foundry:open/1~3, levers/1_check~3_check, arena_start, wave_logic 같은 함수에 대한 **모든 네임스페이스의 리터럴 호출자**를 읽기 전용으로 추출한다. 함수 본문/NBT/지도 파일/원작 코드 재배포 없음. 직접 실행·플레이/문 통행·원작 전투 비재진입의 실서버 검증이 아니며 승인된 맵도 아니다.

도구: tools/inspect_drehmal_foundry_gate_wave_paths.py
신규 검사: .github/workflows/turnbound-drehmal-foundry-clone-callers.yml

MAP SEPARATION COMPLETE: NO.
