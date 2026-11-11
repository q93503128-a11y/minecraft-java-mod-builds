# TURNBOUND — 원작 Foundry 3레버: 업적 조건 및 실행 연결 확인

2026-11-11 / 공식 Drehmal: APOTHEOSIS 2.2.2f / turnbound-content-rebuild.

실서버 #42929635789 및 #42931701776에서는 원작 데이터팩 ON, 명령 블록 ON, 7+9 복사본에서 실제 블록 우클릭 후 **세 레버 powered=true**를 재현했다. 그러나 후속 원본 함수 감사 #42931261435에서 확인한 `foundry:levers/door`의 정확한 clone 대상에서도 블록 ID 변경이 0건이었다. 즉, 물리 레버 ON이 곧 원작 업적 보상·점수판 콜백 실행을 보장하지 않을 수 있다.

이 단계에서는 원본 업적 `foundry:1`, `foundry:2`, `foundry:3`의 `item_used_on_block` criterion 조건 구조와 `levers/1_check~3_check`, `levers/door`, `open/0~3`의 함수 호출·명령 종류만 읽기 전용으로 비교한다. 현재 실제 서버에서 업적 grant/동적 콜백 발생 자체는 **미검증**이다.

분석 결과에 따라 실제 아이템을 든 프로토콜 플레이어의 자연 업적 트리거 여부, 레버 3개 전제조건 및 문 변화를 단일 폐기용 사본에서 실험해야 한다. 원작 코드·지도·함수 소스의 수정/재사용/배포 0건. MAP SEPARATION COMPLETE: NO.

도구: tools/inspect_drehmal_foundry_lever_predicates.py
워크플로: .github/workflows/turnbound-drehmal-foundry-lever-predicate.yml
