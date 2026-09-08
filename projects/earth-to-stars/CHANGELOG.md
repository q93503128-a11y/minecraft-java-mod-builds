# EARTH TO STARS — Changelog

이 문서는 실제 정본 변경을 기록한다.

## 2026-09-08 — P0-E representative turret backend

### Added

- mod version `0.1.0-alpha.5`
- 공통 `TurretControlMode`: `OFF`, `MANUAL`, `AUTO_DEFENSE`
- P0 `autocannon_mk1` profile: ammo / cooldown / range / firing arc / projectile speed / lifetime / damage 계약
- server-owned `TurretRuntime`
- owner/crew `WEAPON_CONTROL` 권한 검증
- 수동 함포 단일 control lease / session UUID / monotonic sequence replay rejection
- mode switch / logout 시 manual lease 회수 경계
- manual aim + authoritative fire solution
- no-ammo / cooldown / invalid-arc shot rejection
- 함선당 공유 `ShipSensorGrid` contact cache
- hostile/neutral 분리와 threat→distance 우선순위 target selection
- Minecraft P0 adapter의 10-tick staggered ship sensor scan
- 포탑별 per-tick broad world scan 금지 구조
- P0에서는 `Enemy` 계열만 auto-defense hostile로 취급하는 임시 target policy
- server-side logical projectile movement/lifetime/collision/damage adapter
- `/earthtostars ship turret off|manual|auto|control|release|fire|status` 기술 검증 조작면
- P0-E manual lease / replay / ammo / cooldown / arc / auto target JUnit
- build workflow에서 `gradle.properties`의 `mod_version`을 자동 읽어 artifact/report 이름에 반영하는 version-aware CI

### Architecture

Manual과 AUTO_DEFENSE를 별도 무기 구현으로 나누지 않는다. 동일 `TurretRuntime`의 ammo/cooldown/arc 상태를 두 모드가 공유한다. 자동 방어는 각 포탑이 개별적으로 월드를 검색하지 않고 함선 단위 SensorGrid 캐시를 이용한다.

현재 manual command는 최종 포수 UI/키가 아니라 P0 서버 상태머신 검증용 조작면이다. 현재 projectile 역시 렌더 entity가 아닌 서버 논리 shot이다. 최종 함포 모델, 회전 애니메이션, 트레이서, 총구화염, 피격 VFX, 사운드, camera feedback은 `docs/03_UI_ART_REFERENCE_GATE.md` 이후 production 작업에서 구현한다.

Ammo는 아직 P0 turret-local이다. P0-F에서 중앙 AmmoPool/PowerGrid/SensorGrid로 승격하여 여러 weapon이 하나의 authoritative 함선 자원을 공유하도록 한다.

### Verification

최종 검증 기준 커밋: `85e4003223839dd3fe24e87e8fd8382a1931e693`

GitHub Actions `Build earth-to-stars` run `34186350799`:

- Java 25 / Gradle 9.2.1 / NeoForge 26.2.0.38-beta: `PASS`
- dynamic mod-version resolve: `PASS`
- `clean test build`: `PASS`
- P0-A/P0-B/P0-C regression JUnit: `PASS`
- P0-D interior allocation/layout JUnit: `PASS`
- P0-E representative turret JUnit: `PASS`
- OFF / MANUAL / AUTO_DEFENSE state machine: `PASS`
- exclusive manual lease + replay rejection: `PASS`
- ammo / cooldown / firing-arc rules: `PASS`
- shared SensorGrid target selection: `PASS`
- Minecraft 26.2 turret adapter compile: `PASS`
- production JAR verifier: `PASS`
- 생성 JAR: `earth_to_stars-0.1.0-alpha.5.jar`
- JAR SHA-256: `53499a1cf6f14bd21cfefc8cdd095b2e2999c322c20c1be8c660d005efd48835`
- datagen: `NOT RUN`
- GameTest: `NOT REGISTERED / NOT RUN`
- dedicated server smoke: `NOT RUN`
- client smoke: `NOT RUN`
- 실제 manual/auto 포탑 전투: `NOT TESTED`
- 실제 projectile 시각/타격감: `NOT TESTED`
- 2인 pilot+gunner session: `NOT TESTED`
- live multiplayer session: `NOT TESTED`

첫 P0-E run `34186139690`은 Minecraft 26.2에서 `Level#getEntities` overload가 추가되어 `null` 인자가 모호해진 컴파일 오류 2건으로 실패했다. 호출의 source entity 타입을 명시하여 API ambiguity만 수정했고, 설계/기능 삭제 없이 다음 gate에서 성공했다.

이후 성공 run `34186225022`의 실제 JAR은 alpha.5였지만 기존 workflow가 artifact/report 라벨을 alpha.4/P0-D로 하드코딩한 문제를 자체 검수에서 발견했다. workflow를 version-aware하게 수정하고 P0-E 보고서 항목을 갱신한 뒤 run `34186350799`를 다시 성공시켜 산출물 이름과 정본 버전을 일치시켰다.

### Status

`P0-E TURRET BACKEND BUILD VERIFIED / LIVE COMBAT & MULTIPLAYER ACCEPTANCE DEFERRED`

다음 의미 있는 작업 단위는 **P0-F Central Ship Systems**다.

---

## 2026-09-08 — P0-D linked ship interior backend

### Added

- mod version `0.1.0-alpha.4`
- stable `InteriorRef(shipId, slot)` / `InteriorSlotLayout`
- 하나의 `earth_to_stars:ship_interiors` 기술 공간을 함선별 격리 cell로 나누는 구조
- 2048-block cell spacing / 8192×8192 allocation grid
- stable `ShipId → interior slot` assignment와 reverse lookup
- persisted interior slot collision을 조용히 재할당하지 않고 거부하는 corruption boundary
- server-global `InteriorSavedData`
- `/earthtostars ship interior enter` / `exit` P0 entry/return adapter
- 현재 live exterior의 dimension/transform으로 복귀하는 `ExteriorAnchor` boundary
- exterior가 없거나 interior link가 손상된 경우 Earth recovery 경로
- login 시 unlinked interior player recovery adapter
- `INTERIOR_ACCESS` 권한 기반 entry 검색
- 13×13 기술용 P0 room 생성기; 최종 interior 디자인이 아님
- P0-D slot round-trip / stable allocation / collision rejection / out-of-grid JUnit

### Architecture

함선마다 동적 dimension을 새로 만들지 않는다. 하나의 안정된 interior 공간에서 각 `ShipId`가 영구적인 격리 cell을 가진다. 외부 함선이 이동하거나 Earth↔orbit transition을 해도 내부 플레이어는 이 안정된 좌표계에 남고, 같은 `ShipId`를 통해 외부 함선의 현재 위치/상태와 연결된다. 따라서 내부 승무원의 좌표를 외부 translation/rotation에 맞춰 매 tick 변환하지 않는다.

현재 interior assignment는 별도 `InteriorSavedData`에 저장된다. 첫 playable-alpha compatibility freeze 전에는 필요 시 unified save schema로 migration할 수 있으며, 그 이후에는 명시적 migration 없이 save key/registry ID를 변경하지 않는다.

### Verification

최종 검증 기준 구현 커밋: `492d8fa0536b23881591ad9a31b0501c7048b6e3`

GitHub Actions `Build earth-to-stars` run `34183711601`:

- Java 25 / Gradle 9.2.1 / NeoForge 26.2.0.38-beta: `PASS`
- `clean test build`: `PASS`
- P0-A/P0-B/P0-C regression JUnit: `PASS`
- P0-D interior allocation/layout JUnit: `PASS`
- `ship_interiors` dimension data packaged: `PASS`
- linked-interior Minecraft adapter compile: `PASS`
- production JAR verifier: `PASS`
- 생성 JAR: `earth_to_stars-0.1.0-alpha.4.jar`
- JAR SHA-256: `a5f3d8ffb24869c6085079af40106a3830b12ce7ea53e57775930b372fc03284`
- datagen: `NOT RUN`
- GameTest/save→disk→restart→restore: `NOT RUN`
- dedicated server custom-dimension boot: `NOT RUN`
- client smoke: `NOT RUN`
- 실제 exterior↔interior 출입: `NOT TESTED`
- 두 플레이어 동시 interior: `NOT TESTED`
- exterior 이동/transition 중 interior crew 유지: `NOT TESTED`
- live multiplayer session: `NOT TESTED`

첫 alpha.4 build run `34183610858`은 Minecraft 26.2에서 제거된 `ServerLevel#getSharedSpawnPos()` 호출 하나 때문에 `compileJava`에서 실패했다. 기능 삭제 없이 current 26.2 respawn-data API로 교체했고, 같은 게이트를 다시 실행해 최종 성공했다.

### Status

`P0-D LINKED INTERIOR BACKEND BUILD VERIFIED / LIVE INTEGRATION DEFERRED`

자동 빌드 성공은 실제 내부 출입, custom dimension live boot, 2인 동시 체류, 서버 재시작 복원까지 검증했다는 뜻이 아니다. 반복적인 사용자 테스트를 피하기 위해 이 항목들은 P0-E/F 이후 P0-G multiplayer/lifecycle gate에서 의미 있게 묶어 검증한다.

---

## 2026-09-08 — P0-C orbital transition + Minecraft persistence adapter

### Added

- mod version `0.1.0-alpha.3`
- `earth_to_stars:orbital_space` P0 기술용 orbital dimension data
- Earth upward boundary / orbit downward boundary를 분리한 `ShipTransitionPolicy`
- transition destination transform에서 x/z/yaw/pitch 보존 및 vertical velocity 제한
- 서버 권한 Earth↔orbit pilot transfer transaction
- transfer 전 control lease 회수, 성공 후 새 exterior에 새 session 재발급
- target orbital/Earth level 미등록 시 상태를 건드리지 않는 실패 경계
- target exterior 생성 실패 시 pilot을 origin으로 되돌리는 rollback 경로
- 같은 `ShipFlightRuntime`/`ShipState`를 새 exterior에 연결하여 in-memory `ShipId` 유지
- 서버 전역 `ShipSavedData` adapter와 versioned `ShipStateCodec` payload 저장
- 저장 corruption/key↔shipId 불일치를 조용히 초기화하지 않고 거부
- server start 시 저장된 ship repository 복원 구조
- `/earthtostars ship restore` 저장 함선 재연결 명령
- P0 bootstrap module catalog 중앙화
- Earth→orbit / wrong-direction / orbit→Earth transition-policy JUnit

### Multiplayer boundary

P0-C의 실제 transition 구현은 현재 **pilot-first proof**다. 다인 승객과 interior crew의 원자적 이동은 linked interior/crew lifecycle 위에서 확장한다. 이 단계에서 multiplayer passenger transfer를 구현/검증 완료했다고 표현하지 않는다.

### Verification

검증 기준 구현 커밋: `a99f7b8470b09cfd509ec4f0aaf054c537db0f3f`

GitHub Actions `Build earth-to-stars` run `34181251912`:

- Java 25 / Gradle 9.2.1 / NeoForge 26.2.0.38-beta: `PASS`
- `clean test build`: `PASS`
- P0-A/P0-B regression JUnit: `PASS`
- P0-C transition-policy JUnit: `PASS`
- Minecraft SavedData adapter compile: `PASS`
- orbital dimension data packaged: `PASS`
- transition runtime adapter compile: `PASS`
- production JAR verifier: `PASS`
- 생성 JAR: `earth_to_stars-0.1.0-alpha.3.jar`
- JAR SHA-256: `d49b228ad0fee44ceb395d56b040f2a796e55f452b9b410117ed6f947c3d1fd8`
- datagen: `NOT RUN`
- GameTest/save→disk→restart→restore: `NOT RUN`
- dedicated server smoke/datapack boot: `NOT RUN`
- client smoke: `NOT RUN`
- 실제 Earth↔orbit 비행: `NOT TESTED`
- live multiplayer session/passenger transfer: `NOT TESTED`

### Status

`P0-C TRANSITION BACKEND BUILD VERIFIED / LIVE INTEGRATION DEFERRED`

---

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
