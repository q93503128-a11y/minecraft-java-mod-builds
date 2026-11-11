# TURNBOUND — Foundry 3개 레버 동시 활성화 및 입구 구조 관측

> 2026-11-11 / branch: turnbound-content-rebuild / Drehmal APOTHEOSIS 2.2.2f / Minecraft 1.20.1 Fabric

> **2026-11-11 원작 좌표 기반 보정:** [원본 clone 명령·함수 호출 정적 검증 #42931261435 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42931261435). 원작 foundry:levers/door는 소스 (-3274..-3270,134..142,1557..1558)를 **타깃 (-3278..-3274,156..164,1576..1577)** 으로 복제한다. 기존 #42929635789 검사 영역 Z=1454..1546에는 타깃이 없었으므로 그 범위의 변경 0건을 문 실패로 단정할 수 없다. 새 CI는 이 정확한 복제 대상과 소스 chunk를 명시적으로 로드해 세 레버 ON 전후를 비교한다.

## 측정 목적

공식 위키(https://www.wiki.drehmal.cyou/World/Points_of_Interest/Foundry/)는 3개 레버가 열어 주는 입구 장치와 별도의 6웨이브 전투 시설을 설명한다.

앞선 CI #42824376001은 레버를 하나씩 켜고 바로 원상복구하는 실제 우클릭 검증이었다. 이번에는 공식 원본 SHA 검증 → 별도 복사본 7단계 + 자동 시작 위치 복귀 보상 연결 1개만 제거한 9단계 → 원작 데이터팩 ON, 명령 블록 ON → 프로토콜 유저가 3개 레버를 동일 서버에서 동시에 powered=true 상태로 활성화하는 순서로 진행한다.

- 위키의 Foundry 중심점 (-3276,160,1576)을 문 정확 좌표로 해석하지 않는다. 주변 수평 46블록·수직 18블록 안의 원본 블록 종류 변화만 보수적으로 기록한다.
- basalt/bedrock → air 변경은 문 열림의 후보 증거이지, 실제 통행 가능/문 정확 위치의 확정 검증이 아니다.
- 같은 원작 데이터팩의 Foundry/Valhalla 관련 함수 이름과 명령 종류를 읽기 전용으로 집계한다. 원작 6웨이브 트리거 분리와 기능 보존은 이 작업만으로 검증되지 않는다.
- 사용자 월드, 원작 원본 및 원작 명령/자산은 바꾸거나 배포하지 않는다. 8단계 9개 미승인 콜백 차단은 적용하지 않는다.
- 실제 문 통과, 원작 전투 비재진입, 그래픽 클라이언트, 4인 멀티 미검증. MAP SEPARATION COMPLETE: NO.

도구: tools/probe_drehmal_foundry_three_switch_gate.py
CI: .github/workflows/turnbound-drehmal-foundry-three-switch-gate.yml
