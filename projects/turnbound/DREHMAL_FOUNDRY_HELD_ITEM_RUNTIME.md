# TURNBOUND — Foundry 실제 손 아이템을 든 우클릭 A/B 후속

기준: 2026-11-11, Fabric 1.20.1, Drehmal 2.2.2f, turnbound-content-rebuild.

공식 7+9 사본 동시 레버 ON 실험 #42929635789는 실제 레버 자체의 powered 상태를 확인했다. 정확한 입구 clone 타깃까지 추가한 #42931701776에서도 블록 변경 0건이었다. 별도 읽기 전용 #42932597961에서는 foundry:1~3 업적이 minecraft:item_used_on_block 및 원래 레버 위치/전원 상태에 반응하도록 구성됨을 확인했다. 아이템 ID 제한은 검사한 조건에서 발견되지 않았으나 빈손 프로토콜 사용으로 그 보상 콜백이 실제 발동했는지는 미검증이다.

이번 검사는 완전히 독립된 공식 7+9 일회성 사본에서, 동일한 3개 레버에 **minecraft:stick 실제 손 슬롯을 가진 프로토콜 클라이언트**가 물리 우클릭을 실행한다. 레버 상태, 점수판의 원본 소유자/이름을 가린 변경 건수, 원래 문 clone 영역 블록 변화를 기록한다. 데이터팩·명령 블록 ON. 명시적인 advancement grant/function 강제 호출은 하지 않는다.

**판정 제한:** 점수판/문 상태 변경이 없으면 업적 조건 미충족·콜백 상태·서버 틱·진입 조건 중 무엇 때문인지 추가 분리가 필요하다. 원작 함수 수정·코드 이식·사용자 월드 변경 없음. 8단계 9개 미승인 차단 미적용. 직접 문 통과/6웨이브 억제/그래픽 클라이언트/4인 플레이 미검증, MAP SEPARATION COMPLETE = NO.

검증 도구: tools/probe_drehmal_foundry_held_item.py
CI: .github/workflows/turnbound-drehmal-foundry-held-item.yml


## 실서버 결과 — 2026-11-11

- [실제 손 아이템/레버/점수판 검사 #42933135850 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42933135850): 원본 SHA 고정 7+9 폐기용 사본에서 프로토콜 플레이어에게 `minecraft:stick`을 쥐여 주고 세 레버를 각 한 번씩 우클릭함. 세 레버 초기 OFF→각 ON·모두 ON 유지. 각 클릭 전후 `scoreboard.dat` 변경 항목 2개 관측. **이것만으로 특정 보상 함수의 호출 인과나 문 개방을 확정하지 않는다.** 원작 데이터팩 ON·명령 블록 ON.
- [정확한 문 clone 대상 실서버 #42931701776 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42931701776): 원작 `foundry:levers/door`의 source (-3274~-3270, Y134~142, Z1557~1558) 및 dest (-3278~-3274, Y156~164, Z1576~1577)를 강제 로드하고 실제 3레버 동시 ON 전후 6,069블록 확인. 변경 0건. 문이 원래 열려 있거나 clone이 무변화일 가능성, 3레버 점수판 조건 미충족, 콜백 미실행을 아직 구분하지 못함.
- [원작 업적 조건 #42932597961 PASS](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42932597961): `foundry:1/2/3`의 `minecraft:item_used_on_block` 조건에 레버 powered·차원·위치 제약이 존재함. 검사한 predicate에 아이템 ID 요구 없음. `foundry:levers/1_check~3_check`는 점수 조건을 경유하여 `foundry:levers/1~3`를 호출.
- 원작 wave 경로는 `foundry:open/3 → foundry:arena_start → foundry:wave_logic → wave_1~wave_6`의 정적 함수 호출 후보. 실제 전투 발동·차단 **NOT TESTED**.

**다음 검증:** 먼저 비활성 원본에서 clone source/destination 초기 블록 구성 동일 여부와 `foundry:levers/main`의 삼중 조건을 메타데이터로 검사하고, 이후 동일한 입구·레버 조건을 가진 대조 사본 간 실서버 인과 A/B 시험. 원작 코드·자산/맵 원본 이식·배포하지 않는다. 기존 8단계 9건은 실사용 차단 미승인. **MAP SEPARATION COMPLETE = NO.**
