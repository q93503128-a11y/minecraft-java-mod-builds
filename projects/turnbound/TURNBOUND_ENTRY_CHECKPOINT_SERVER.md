# TURNBOUND — 서버 권한 월드 진입·체크포인트 연결

## 2026-11-10 실제 구현 경계

현재 정본: `turnbound-content-rebuild`, `projects/turnbound/`, Minecraft Java 1.20.1 / Fabric 0.16.14 / Fabric API 0.92.6+1.20.1 / Java 17.

- `src/fabric/java/io/github/q93503128/turnbound/TurnboundCheckpointTarget.java`: 엄격한 목적지 좌표/차원 검사와 JSON·NBT 인코딩. **시작 좌표를 임의로 기입하지 않음**.
- `TurnboundCheckpointState.java`: 독립 저장 키 `turnbound_checkpoints_v1`에 UUID별 개인 체크포인트를 Minecraft `PersistentState`로 저장, 구조·좌표·중복 UUID 검증. 원작 플레이어 데이터나 구 TURNBOUND 세이브 포맷을 가져오지 않음.
- `TurnboundCheckpointService.java`: 원본과 분리된 활성화 월드에서만 (1) 명시된 시작점으로 최초 진입 처리, (2) 재접속 시 기존 위치 유지, (3) 서버 측 신뢰된 체크포인트 등록, (4) 파티 패배 복귀 API, (5) 예상치 못한 바닐라 치명타와 일반 리스폰 경로 대응. 별도의 플레이어 명령/네트워크 입력으로 체크포인트를 정할 수 없음.
- 기존 `Turnbound.java`가 해당 서비스를 등록한다. 기존 전투·블록 금지·월드 안전 게이트와 교체하지 않고 결합함.

## 활성화 조건과 위치 승인

**두 개의 파일을 모두 정식으로 준비하기 전까지 자동 입장 이동은 비활성이다.**

1. 이미 존재하는 opt-in `turnbound-world.marker`의 내용이 정확히 `TURNBOUND_DREHMAL_WORLD_V1`이어야 한다.
2. 그와 동일한 **완전히 별도의 검증된 사본** 월드 루트에 `turnbound-entry.json`을 *의도적으로* 배치해야 한다. 배포본/원작에는 이 파일을 포함하지 않았다. 현재 승인된 START 좌표는 **없으며**, 값을 만들어 넣거나 원작의 빈 시작 장치로부터 임의 채택하지 않는다.

설정 파일의 스키마는 `schema=1`, `dimension="minecraft:overworld"`, 숫자 `x/y/z/yaw/pitch` 7개 필드 **정확히**이다. 실제 안전 위치의 검증·선정이 끝난 후에만 좌표를 기록한다. 불필요한 숨겨진 필드·문자형 숫자·다른 차원·불가능한 높이나 각도를 거부한다. 심볼릭 설명이 아니라 실제로 서버 시작 시 파일을 확인한다.

**운영 전제:** 기존 마커는 다른 원작 게임 기능을 완전히 차단하지 않는다. 따라서 8단계 분리 실험을 운영용 맵으로 바꾸거나 사용자 월드에서 이 마커만 추가해서 플레이하는 행위는 **승인하지 않는다**.

## 안전 경로·서버 권한

- 서버가 플레이어 UUID별 첫 진입을 기록하고, 이후 정상 재접속 시 위치를 임의로 리셋하지 않는다. 각 체크포인트와 차원·방향은 `PersistentState`에 저장된다.
- 바닥의 온전한 지지 면, 머리 위까지의 충돌 방지, 액체·위험 블록, 실제 월드 높이, 기존에 로딩된 청크를 확인한 뒤에만 이동한다. 장치/블록/지형을 생성·제거하거나 누락된 청크를 새로 생성하지 않는다.
- 물리적인 위치가 안전하지 않으면 복귀를 거부한다. 개인 체크포인트가 부적합하면 등록된 초기 지점을 재검사한 후에만 대체한다. **체크포인트 없이 임의로 스폰 좌표를 추측하지 않는다.**
- 정상 전투 패배의 패널티 없는 복귀와 원작/바닐라 사망은 다른 경로다. `returnAfterDefeat(ServerPlayerEntity)`는 신뢰된 서버 측 전투 결과 처리기에서 호출할 수 있도록 구현되었지만, 실제 턴제 전투 패배·던전 패배 결정 로직과 **아직 연결하지 않았다**. 패배 보상이나 재화를 새로 발행하지 않는다.
- 바닐라 치명타 방지·리스폰 대응은 연결했으나 `/kill`, 타 모드의 강제 사망, 동적 차원 이동, 저장 손상 및 멀티플레이의 실제 런타임 동작은 **아직 미검증**이다.
- 원본, legacy TURNBOUND 저장 데이터 및 사용자 실제 월드에서 서비스 진입을 허용하지 않는다. `turnbound-world.marker`가 없으면 서비스는 아무것도 변경하지 않는다.

## 검사와 현재 한계

- [전용 서버 부팅 검증 #42749364808](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42749364808): **SUCCESS**. 원작 Drehmal과 무관한 새 폐기용 Minecraft 1.20.1 월드에 opt-in marker만 넣고 실제 Fabric 서버를 부팅했다. 의도적으로 시작 설정 파일이 없는 상태에서 TURNBOUND 게이트의 진입점 미설정 감지, 게임 서버 준비, 정상 종료를 확인. `server_started=true`, `checkpoint_gate_logged=true`, `server_stopped_cleanly=true`. 실제 접속한 플레이어는 0명이며 게임용 맵 활성화/월드 시설 기능/리스폰 성공으로 해석하지 않는다.


- [GitHub Actions Build TURNBOUND #42748867041](https://github.com/q93503128-a11y/minecraft-java-mod-builds/actions/runs/42748867041): **SUCCESS**. Java 17/Fabric 1.20.1 소스 컴파일, JUnit 및 기존 좁은 도구 검사, `turnbound-0.1.0-alpha.17.jar` JAR 검사/SHA 확인.
- JUnit `TurnboundCheckpointStateTest`: 잘못된 좌표/비승인 차원/문자형 값/추가 필드 거부; UUID별 서로 다른 체크포인트 NBT 직렬화·재로드; 훼손·중복 저장 데이터 거부.
- **CODE REVIEWED / TESTED / BUILD VERIFIED / JAR PRODUCED / DEDICATED SERVER BOOT: YES.** 플레이어 최초 진입·실제 체크포인트 전환·캐릭터 패배 복귀·무손실 강제 사망·시설 보존·2인 멀티는 별도 검증되기 전까지 **NOT TESTED**.
- 실제 서버에서 시작 위치가 *미로딩 청크*에 존재할 때는 자동 이동하지 않는 안전 제한을 둔다. 추후 정상 맵의 실측 시작 영역 로딩/안전성 검증 후에만 운영용 위치 설정을 적용할 수 있다.
- 무단 원작 함수 이식, 맵 파일 배포, 자동 위치 결정, 메인 게임 로직 재설계 **0건**.

## 다음 실질 검증

1. 가역적으로 준비된 별도 Drehmal 복사 월드에서 **승인된 위치를 확인**하고, 자체 TURNBOUND 마커·설정 파일의 안전성을 검사한다.
2. 일반 Fabric 테스트 서버에서 마커 활성 상태의 로딩/접속/리스폰을 순서대로 검증한다. 플레이어가 없었다면 실제 입장·리스폰 성공을 주장하지 않는다.
3. 기존 원작 시설(Avsal, Foundry, Godmachine 및 START 구조)의 실제 작동 전후를 비교한다. 블록 변형·명령 실행 여부만으로 문/포탈 정상 작동을 주장하지 않는다.
4. 이후 trusted 전투 세션 결과와 체크포인트·패배 복귀를 연결한다. 전멸 시 개인 장비/경험치/보석을 잃지 않는 승인된 규칙과 4인 멀티 구조를 유지한다.
