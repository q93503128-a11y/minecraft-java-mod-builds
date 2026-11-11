# TURNBOUND — Foundry 실제 손 아이템을 든 우클릭 A/B 후속

기준: 2026-11-11, Fabric 1.20.1, Drehmal 2.2.2f, turnbound-content-rebuild.

공식 7+9 사본 동시 레버 ON 실험 #42929635789는 실제 레버 자체의 powered 상태를 확인했다. 정확한 입구 clone 타깃까지 추가한 #42931701776에서도 블록 변경 0건이었다. 별도 읽기 전용 #42932597961에서는 foundry:1~3 업적이 minecraft:item_used_on_block 및 원래 레버 위치/전원 상태에 반응하도록 구성됨을 확인했다. 아이템 ID 제한은 검사한 조건에서 발견되지 않았으나 빈손 프로토콜 사용으로 그 보상 콜백이 실제 발동했는지는 미검증이다.

이번 검사는 완전히 독립된 공식 7+9 일회성 사본에서, 동일한 3개 레버에 **minecraft:stick 실제 손 슬롯을 가진 프로토콜 클라이언트**가 물리 우클릭을 실행한다. 레버 상태, 점수판의 원본 소유자/이름을 가린 변경 건수, 원래 문 clone 영역 블록 변화를 기록한다. 데이터팩·명령 블록 ON. 명시적인 advancement grant/function 강제 호출은 하지 않는다.

**판정 제한:** 점수판/문 상태 변경이 없으면 업적 조건 미충족·콜백 상태·서버 틱·진입 조건 중 무엇 때문인지 추가 분리가 필요하다. 원작 함수 수정·코드 이식·사용자 월드 변경 없음. 8단계 9개 미승인 차단 미적용. 직접 문 통과/6웨이브 억제/그래픽 클라이언트/4인 플레이 미검증, MAP SEPARATION COMPLETE = NO.

검증 도구: tools/probe_drehmal_foundry_held_item.py
CI: .github/workflows/turnbound-drehmal-foundry-held-item.yml
