# TURNBOUND — Foundry 세 레버 동시 활성화 후 정적 실행 경로 분석

> 2026-11-11 · TURNBOUND / turnbound-content-rebuild

## 검증된 관측

[공식 7+9 실서버 세 레버 동시 활성화 #42929635789](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42929635789): 세 레버 모두 초기 OFF에서 실제 클릭으로 ON 전환했고, 이후 세 곳 모두 ON인 상태가 확인됐다. 다만 공식 위키의 대략적인 시설 중심 좌표 (-3276,157,1500) 주변 수평 46·수직 18블록 내 비교한 320,013개 위치의 **블록 ID 변경 0건**. 이는 해당 범위에서 입구 구조 변경이 관측되지 않았다는 뜻이며, 정확한 출입문 좌표/메커니즘/통행 여부 미확인이므로 전체 Foundry 문 고장으로 단정 금지.

공식 원본 데이터팩에서 Foundry 관련 함수 111개, 원본 summon 직접 명령 26개, wave_1 ~ wave_6 및 arena_start/arena_victory 같은 후보 함수 명칭이 확인됐다. 이름의 존재만으로 실제 전투 시작 지점·실행 인과관계가 입증되지는 않았다.

## 이번 정적 감사

코드: tools/inspect_drehmal_foundry_gate_wave_paths.py
CI: .github/workflows/turnbound-drehmal-foundry-gate-wave-static.yml

공식 v2.2.2f SHA 고정 원본에서 원작 ZIP과 레버 주변 명령 블록을 읽기만 한다.
- Foundry/Valhalla 함수의 리터럴 fill/setblock 좌표를 집계하여 위키 근처 출입문 후보 위치를 도출한다.
- wave_1~wave_6, arena_start, arena_victory의 명시적 함수 호출·업적 보상 진입점의 역방향 참조를 표시한다.
- 레버 인접 4블록의 원작 명령 블록 개수와 function ID 참조만 확인한다.
- 원작 함수 본문/명령어/NBT/지도/저작물은 게시하거나 TURNBOUND에 이식하지 않는다.
- execute 상대좌표, 동적 매크로, 보스 전투 호출 등의 완전 추적이나 실제 문 통과/게임 비재진입/실사용 게임 맵 승인 아님.

**MAP SEPARATION COMPLETE: NO**. 실제 좌표와 진입 경로가 확인되기 전 원작 맵/데이터팩을 일괄 차단하거나 원작 시설을 제거하지 않는다.
