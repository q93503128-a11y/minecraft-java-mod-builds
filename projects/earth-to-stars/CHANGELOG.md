# EARTH TO STARS — Changelog

이 문서는 실제 정본 변경을 기록한다.

## 2026-09-08 — P0-B ship movement backend

### Added

- pure-Java `ShipVec3`, `ShipTransform`, forward/right/up orientation basis
- throttle / yaw / pitch 입력 계약
- acceleration / braking / forward/reverse speed tuning
- server-side `ShipMovementSimulator`
- `ShipFlightRuntime`와 서버 발급 `ShipControlLease`
- control session UUID + monotonic sequence를 이용한 stale/replay input rejection
- owner/crew `PILOT` 권한과 guest 조종 거부
- control lease TTL과 release/expiry 후 zero-input 안전 상태
- client→server 입력 payload; 좌표/속도/회전 결과를 client payload에 포함하지 않는 server-authoritative 경계
- logout / dimension change 시 control lease 해제 경로
- `/earthtostars ship spawn`, `control`, `release` 조종 연결 명령
- Minecraft-side 임시 `ArmorStand` exterior proxy와 서버 transform projection
- mouse가 game에 grab되지 않은 상태에서 조종 입력을 zero로 보내는 client guard
- 이동 basis / 가속·감속 / pitch clamp / lease permission / replay / expiry JUnit
- mod version `0.1.0-alpha.2`

### Boundary

현재 `ArmorStand` exterior는 movement/backend 연결을 검증하기 위한 내부 기술 프록시다. 최종 함선 모델, cockpit, production visual 또는 함선 형태로 간주하지 않는다.

### Verification

최종 검증 기준 커밋: `cc89f0c1693fa063de5bb091ca355a35a5413b8f`

GitHub Actions `Build earth-to-stars` run `34176522000`:

- Java 25 / Gradle 9.2.1 / NeoForge 26.2.0.38-beta: `PASS`
- `clean test build`: `PASS`
- P0-A regression JUnit: `PASS`
- P0-B pure movement/lease JUnit: `PASS`
- Minecraft-side proxy/network/client adapter compile: `PASS`
- production JAR verifier: `PASS`
- 생성 JAR: `earth_to_stars-0.1.0-alpha.2.jar`
- JAR SHA-256: `a834a372008b1604d4591b595b88b10ba06446e1c149ce044a6842db0f3a2413`
- datagen: `NOT RUN`
- GameTest / Minecraft reload persistence: `NOT REGISTERED / NOT RUN`
- dedicated server smoke: `NOT RUN`
- client smoke: `NOT RUN`
- live multiplayer session: `NOT TESTED`
- 실제 조종감/카메라/interpolation/reconnect acceptance: `NOT TESTED`

첫 P0-B build run `34176273788`은 Minecraft 26.2 API 차이 3건으로 `compileJava`에서 실패했다. 이는 순수 movement/lease 설계 실패가 아니었고, client screen 접근, payload player 정적 타입, server clock 접근을 26.2 API에 맞춘 뒤 같은 게이트를 재실행해 최종 성공했다.

### Status

`P0-B BACKEND BUILD VERIFIED / LIVE MINECRAFT ACCEPTANCE DEFERRED`

반복 테스트를 피하기 위해 P0-B의 실제 Minecraft 조종·재접속 검증은 즉시 별도 사용자 테스트로 요청하지 않는다. 다음 P0-C 및 persistence integration과 묶어 더 큰 의미의 플레이 검증 게이트에서 확인한다.

다음 의미 있는 작업 단위는 **P0-C Earth → Orbital Space Transition + Minecraft persistence integration**이다.

---

## 2026-09-08 — M0 bootstrap + P0-A authoritative ship kernel

### Added

- Minecraft 26.2 / Java 25 / NeoForge 26.2.0.38-beta / Gradle 9.2.1 실행 골격
- `earth_to_stars` mod metadata, entrypoint, assets/data namespace
- 프로젝트 전용 GitHub Actions build workflow와 production JAR verifier
- 순수 Java `ShipId` / `ShipState` / `ShipRepository`
- owner / crew / guest 권한 정책
- `ModuleDefinition`, `ModuleInstance`, slot/hardpoint compatibility
- command / propulsion / power / cargo / weapon hardpoint 최소 모듈 계약
- stable ship identity와 module/crew/slot을 보존하는 versioned schema 1 persistence codec
- 잘못된 schema를 자동 초기화하지 않고 거부하는 저장 안전성 규칙
- module install/remove, duplicate rejection, permission, serialization round-trip JUnit

### Verification

검증 기준 커밋: `d1c34db306680944c5696ecabd5f018943fef772`

GitHub Actions `Build earth-to-stars` run `34175374292`:

- Java 25 / Gradle 9.2.1 wrapper: `PASS`
- `clean test build`: `PASS`
- P0-A JUnit: `PASS`
- production JAR verifier: `PASS`
- 생성 JAR: `earth_to_stars-0.1.0-alpha.1.jar`
- JAR SHA-256: `baea44c12f5383781967c503c41363831312676643f1870fac82df9108c46848`
- datagen: `NOT RUN`
- GameTest / Minecraft reload persistence: `NOT REGISTERED / NOT RUN`
- dedicated server smoke: `NOT RUN`
- client smoke: `NOT RUN`
- live multiplayer session: `NOT TESTED`

### Status

`M0 BUILD BOOTSTRAP VERIFIED / P0-A PURE SHIP KERNEL VERIFIED / MINECRAFT INTEGRATION GATE PENDING`

P0-A의 순수 서버 정본 커널은 구현·자동 검증되었다. Minecraft SavedData/GameTest를 통한 실제 서버 생성→저장→reload→동일 shipId/module 복원 검증은 아직 수행하지 않았으므로 P0-A 전체 통합 완료라고 표현하지 않는다.

다음 의미 있는 작업 단위는 **P0-B Ship Exterior / Movement Backend**를 시작하되, 첫 Minecraft 서버 통합 시점에 P0-A persistence integration gate를 함께 닫는 것이다.

---

## 2026-09-08 — Project registration / M0 canon lock

### Added

- 새 프로젝트 `projects/earth-to-stars/` 등록
- `PROJECT.md`에 Minecraft 26.2 / Java 25 / NeoForge 26.2.0.38-beta 목표 환경 기록
- Overworld = Earth 확정
- Nether/End 비필수 메인 진행 확정
- B형 모듈식 함선 확정
- server-authoritative multiplayer-first 구조 확정
- manual/automatic turret 공통 weapon-system 구조 확정
- linked stable ship interior 방향 확정
- centralized power/ammo/sensor simulation 방향 확정
- 지구→궤도→달→소행성→행성→심우주 progression 정본 작성
- UI/함선/무기/천체/VFX/사운드 external-reference gate 작성
- third-party reference/asset ledger 작성
- P0 기술검증과 첫 Earth/Orbit/Moon vertical slice 순서 작성

### Status

`M0 CANON LOCKED / BUILD BOOTSTRAP NEXT`

### Verification

- Documentation/source-of-truth setup only.
- Gradle project bootstrap: `NOT IMPLEMENTED`
- Clean build: `NOT RUN`
- GameTest: `NOT RUN`
- Dedicated server: `NOT RUN`
- Client: `NOT RUN`
- Multiplayer session: `NOT RUN`
- Playable JAR: `NOT AVAILABLE`

다음 의미 있는 작업 단위는 **M0 Build Bootstrap + P0-A Authoritative Ship Kernel**이다.
