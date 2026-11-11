# TURNBOUND — Foundry 독립 시설 어댑터 및 실서버 증거

- 날짜: 2026-11-11
- 정본: `turnbound-content-rebuild` / `projects/turnbound/`
- Minecraft 1.20.1, Java 17, Fabric
- 구현 커밋: [475093b44eb1dab6859df3f2bba75a2a9b61ff6b](https://github.com/q93503128-a11y/minecraft-java-mod-builds/commit/475093b44eb1dab6859df3f2bba75a2a9b61ff6b)
- **판정: Foundry의 3개 레버 → 문 개방 블록 변경이 원작 tick 없이 작동함. 전체 맵 분리 완료 아님.**

## 왜 구현했는가

원작 Foundry 문의 리터럴 실행 경로는 `minecraft:tick → entities:main → players:main → players:overworld → players:overworld_p → foundry:levers/door`에 놓여 있다. 그러나 7+9 분리 사본은 원작 게임 시스템 전체가 재실행되지 않도록 자동 tick 진입점 `core:main`·`entities:main`을 제거한다. 본래 레버를 켜도 자동 문 복제가 일어나지 않은 원인이다. 원본 함수 복구는 다른 원작 게임 실행 경로까지 다시 연결할 위험이 있으므로 허용하지 않는다. [원인 추적](DREHMAL_FOUNDRY_TICK_ROOT_CAUSE.md).

## TURNBOUND 독립 구현

1. `src/fabric/java/io/github/q93503128/turnbound/TurnboundFoundryGateService.java`: 서버에서 실제 3개 레버의 블록 사용 입력을 감지하고 **바닐라 조작을 취소하지 않는다**. 서버 틱 종료 후 해당 레버의 실제 `POWERED` 값을 읽는다.
2. `TurnboundFoundryGateState.java`: 원작 점수판과 무관한 `turnbound_foundry_gate_v1` 저장 키에 조작 확인 비트·ON 상태·문 개방 여부를 영속화한다. 임의 저장 플래그와 버전 불일치 거부. 서버 재시작을 통한 실제 장기 지속 검증은 아직 없다.
3. 모든 레버를 **실제 사용으로 확인**했고 ON일 때에만, 실제 서버 플레이어가 원래 문 주변 반경 12 안에 있을 경우 개방을 검토한다. 원격 레버 청크를 강제로 로딩하지 않고 이벤트 시점 상태를 저장한다.
4. 소스 90블록과 닫힌 대상 90블록의 원본 기준 palette 수량, 완전 상태 차이 59개, 블록 엔티티 부재, 관련 청크 로딩 여부를 먼저 확인한다. 예상과 다른 지역에서는 **변경 거부**. 성공 시 소스 블록 상태를 기존 문 위치에 적용하고 90개 결과를 다시 검증한다. 블록 갱신 실패 때 되돌리기를 시도하되, 외부 모드가 동시에 바꾸는 모든 실패 경우의 원자성을 증명한 것은 아니다.
5. 한 번 열렸다면 저장된 개방 상태에 따라 이후 중복 변경을 방지한다. **원작 함수, 보상, score, advancement, `arena_start`, `wave_logic`, `wave_1…6`을 직접 호출하지 않는다.** 원작 월드 바이트나 원작 스크립트를 프로젝트에 포함하거나 배포하지 않는다.
6. `TurnboundWorldGate`의 정확한 opt-in 마커가 있는 월드에서만 코드가 활성이다. 사용자가 단독으로 마커만 넣어서 **실제 플레이용 맵을 승인하는 것은 아니다.** 서버 검사는 SHA 확인된 7+9 **독립 폐기 사본**에서만 진행했다. 미승인 8번째 보상 9건 차단을 합치지 않았다.

## 실제 검증

| 범위 | 근거 | 결과 |
| --- | --- | --- |
| Java 17/Fabric build, JUnit, JAR 확인 | [Build #42952475746](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42952475746) | **PASS** |
| 원작 자동 루트 차단, 마커 없는 사본의 기존 레버 3개 | [대조 #42952475741](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42952475741) | **PASS**, 소스/대상 차이 59 유지 |
| 7+9 폐기 사본 + opt-in TURNBOUND 모드 + 실제 프로토콜 클릭 3회 | [실서버 #42952475774](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42952475774) | **PASS**, 레버 3/3 ON |
| 문 근처 플레이어 접근 | 같은 CI 아티팩트 | **PASS**, 대상 차이 59→0, 정확한 대상 59개 변경, 소스/대상 90개 전체 상태 일치 |
| 원작 문 함수 명시적 실행 | 같은 CI 아티팩트 | **0회** |
| 재접속·실제 서버 재시작과 중복 개방 | JUnit은 저장 형식·상태 전이 검증 | **서버 장기 재현 미검증** |
| 실제 Minecraft 그래픽 클라이언트의 문 통과·카메라·시설 체험 | — | **NOT TESTED** |
| Foundry 원작 전투 6웨이브 미재진입 및 다른 원작 자동 경로 차단 | — | **NOT TESTED** |
| 4인 멀티·상호작용 경합·지역 언로드·서버 재시작 | — | **NOT TESTED** |
| 전체 지도 분리 및 실사용 START 승인 | — | **NO** |

공식 원본 맵은 읽기 전용, 프로젝트 게임 월드 무변경, 기존 8단계 9개 미승인 보상 차단 미적용. CI 아티팩트에는 전체 원본 맵·스크립트·월드 파일을 포함하지 않고 **파생된 상태 수치만** 올린다.

## 다음 순서

1. 문 전후의 물리적 통행 가능 위치를 직접 확인하고, 실패 시 예상 블록 상태와 이동 동선을 구분한다.
2. 7+9 분리 상태에서 원작 `arena_start`, `wave_logic`, `wave_1…6`이 문 개방 직후 및 재입장 후에 재개되지 않는지 증거 확보.
3. 동일 문을 두 플레이어 이상이 동시에 접근하고, 재접속/서버 재시작했을 때 중복 조작·원상복구·저장 상태 오류가 없는지 검사.
4. Godmachine, Av'Sal, Terminus 등 다른 핵심 시설의 필요한 동작만 원작 게임 로직과 분리해 보존. 모든 시설을 자동으로 같은 방식으로 복제하지 않는다.
5. 나머지 업적 보상·명령 블록·START·탐험 안전 복귀, 실제 클라이언트 및 4인 멀티 검증 뒤에만 지도 실사용 승격 판단.

**MAP SEPARATION COMPLETE = NO.**
