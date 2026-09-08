# EARTH TO STARS — Changelog

이 문서는 실제 정본 변경을 기록한다.

## 2026-09-08 — P0-G dedicated lifecycle + systems persistence

### Added

- mod version `0.1.0-alpha.7`
- `ShipSystemsSnapshot`: `ShipId`, current power, ammo amounts 영속 snapshot
- server-global `ShipSystemsSavedData` (`earth_to_stars:ship_systems`)
- central PowerGrid / AmmoPool 실제 SavedData persistence
- persisted power/ammo capacity validation
- persisted systems가 canonical `ShipSavedData`에 없는 unknown ShipId를 가리킬 때 orphan state 거부
- old ship save에 systems snapshot이 없는 경우 P0 기본값으로 명시적으로 bootstrap 후 snapshot 생성
- server tick checkpoint와 `ServerStoppingEvent` final flush
- sensor contact를 persistence 대상에서 제외하고 restart 후 재획득하는 volatile-cache 계약
- `ShipLifecycleProbe`: 일반 플레이에 노출되지 않고 CI 환경변수에서만 활성화되는 dedicated lifecycle 검증기
- dedicated server clean first boot → SavedData seed → clean shutdown → same world second boot → restore 검증
- `orbital_space` / `ship_interiors` custom dimension 실제 dedicated-server 등록 검증
- deterministic probe ShipId/owner/module slots/interior slot/power/ammo disk round-trip 검증
- systems snapshot restore/invalid capacity JUnit
- CI에 `runServer` 2회 same-world lifecycle gate 추가

### Architecture

영속 상태와 휘발 상태를 분리한다.

Persisted:

- `ShipState` / ownership / module slots
- `ShipId → interior slot`
- central current power
- central current ammo

Not persisted:

- `SensorGrid` contacts
- pilot/turret control leases
- logical projectiles
- temporary exterior entity IDs

센서/lease/entity ID를 저장하지 않는 것은 누락이 아니라 restart 후 ghost target, stale authority, invalid entity reference를 막기 위한 계약이다.

### Verification

최종 검증 기준 구현 커밋: `557d273ecfa78c1ba9cc62956cd78f6eb7c55153`

GitHub Actions `Build earth-to-stars` run `34188840459`:

- Java 25 / Gradle 9.2.1 / NeoForge 26.2.0.38-beta: `PASS`
- `clean test build`: `PASS`
- P0-A~F regression JUnit: `PASS`
- P0-G systems snapshot restore JUnit: `PASS`
- production JAR verifier: `PASS`
- dedicated server first boot: `PASS`
- `earth_to_stars:orbital_space` registration: `PASS`
- `earth_to_stars:ship_interiors` registration: `PASS`
- first boot SavedData seed: `PASS`
- clean server shutdown/all dimension save: `PASS`
- second dedicated-server boot on same `run/world`: `PASS`
- same ShipId / owner / module slots restore: `PASS`
- same interior slot restore: `PASS`
- central power `37.5` restore: `PASS`
- autocannon ammo `73` restore: `PASS`
- restored `ShipSystemsRuntime` initialization: `PASS`
- 생성 JAR: `earth_to_stars-0.1.0-alpha.7.jar`
- JAR SHA-256: `76bc15395500382f0acbc68826c7e6b95533b5ce9863de40e837c9a2856ac708`
- client smoke: `NOT RUN`
- actual Earth↔orbit player flight: `NOT TESTED`
- actual two-player pilot+gunner: `NOT TESTED`
- actual multi-player linked interior: `NOT TESTED`
- production visual/audio: `NOT IMPLEMENTED`

실제 로그 마커:

- `EARTH_TO_STARS_P0G_SEED_PASS ship=11111111-2222-3333-4444-555555555555 slot=0 power=37.5 ammo=73`
- `EARTH_TO_STARS_P0G_VERIFY_PASS ship=11111111-2222-3333-4444-555555555555 slot=0 power=37.5 ammo=73`

### Status

`P0-G DEDICATED LIFECYCLE VERIFIED / LIVE MULTIPLAYER NOT TESTED / P0-H NEXT`

다음 의미 있는 작업 단위는 **P0-H Nether/End Independence Validator**다. 메인 progression에서 Nether/End-only 자원·구조·advancement가 필수 ancestor가 되는 회귀를 CI에서 자동 차단한다.

---

## 2026-09-08 — P0-F central ship systems backend

### Added

- mod version `0.1.0-alpha.6`
- 함선당 하나의 `ShipSystemsRuntime`
- 중앙 `ShipPowerGrid`: capacity / stored energy / deterministic generation / authoritative consumption
- `ESSENTIAL`, `PROPULSION`, `WEAPONS`, `UTILITY` power priority와 reserve boundary
- 중앙 multi-type `ShipAmmoPool`
- P0 대표 기관포의 turret-local ammo 제거 및 `autocannon_round` 공용 탄약 사용
- `TurretProfile`에 ammo type / power-per-shot 계약 추가
- weapon fire에서 power+ammo 사전검사 후 함께 소비하는 authoritative transaction boundary
- 두 개 이상의 turret runtime이 같은 `ShipId`의 하나의 ammo pool을 공유하는 구조
- P0-E `ShipSensorGrid`를 중앙 함선 시스템으로 승격
- sensor scan power draw / interval scan / per-ShipId phase staggering / stale contact expiry
- propulsion input 크기에 비례하는 central power draw
- 전력 부족 시 서버가 추진 입력을 적용하지 않는 경계
- interior player가 `InteriorSavedData → ShipId → ShipRepository`를 통해 같은 함선 systems authority에 접근하는 경계
- `/earthtostars ship systems status` 기술검증 상태 조회
- turret status가 private ammo 대신 공용 ammo를 표시
- dimension change/logout에서 turret manual lease 정리 강화
- P0-F 중앙 튜닝을 `ShipSystemsTuning.P0` 한 곳에 집약
- PowerGrid / shared ammo / failed transaction / propulsion draw / stale sensors JUnit

### Architecture

P0-F부터 함선 자원은 weapon/entity별 임시 숫자가 아니라 `ShipId`에 연결된 중앙 서버 정본으로 취급한다.

```text
ShipId
 └─ ShipSystemsRuntime
     ├─ ShipPowerGrid
     ├─ ShipAmmoPool
     └─ ShipSensorGrid
          ↑
   propulsion / sensors / turret(s)
```

전력 우선순위는 현재 P0 기준 `ESSENTIAL → PROPULSION → WEAPONS → UTILITY`다. 낮은 우선순위 계통은 높은 우선순위를 위해 확보한 reserve를 침범하지 못한다. 구체 수치는 기술검증값이며 production balance 확정값이 아니다.

P0-E에서 각 포탑이 독립 탄약을 가지던 구조를 제거했다. 같은 함선의 수동/자동 포탑과 이후 추가될 다수 hardpoint는 동일 ammo/power authority를 사용해야 한다. 센서도 포탑별 broad scan이 아니라 함선당 하나의 캐시를 공유한다.

### Verification

최종 검증 기준 커밋: `8e65d142f2a4dc3edfd7ef30116ad0929d4bc622`

GitHub Actions `Build earth-to-stars` run `34187867167`:

- Java 25 / Gradle 9.2.1 / NeoForge 26.2.0.38-beta: `PASS`
- `clean test build`: `PASS`
- P0-A/P0-B/P0-C regression JUnit: `PASS`
- P0-D linked interior JUnit: `PASS`
- P0-E representative turret JUnit: `PASS`
- P0-F central PowerGrid / AmmoPool / SensorGrid JUnit: `PASS`
- priority reserve / generation monotonicity: `PASS`
- two turret runtimes share one ammo pool: `PASS`
- failed weapon transaction consumes neither ammo nor power: `PASS`
- propulsion draw uses central power: `PASS`
- stale sensor contact expiry: `PASS`
- interior-linked crew system lookup adapter compile: `PASS`
- Minecraft 26.2 central systems coordinator compile: `PASS`
- production JAR verifier: `PASS`
- 생성 JAR: `earth_to_stars-0.1.0-alpha.6.jar`
- JAR SHA-256: `527f02b6c3a70337c25a8aeebda3c0d2059818fc9e49efc77ab23bba016a07e6`
- central system runtime quantities persistence across restart: `NOT IMPLEMENTED / NOT TESTED`
- datagen: `NOT RUN`
- GameTest: `NOT REGISTERED / NOT RUN`
- dedicated server smoke: `NOT RUN`
- client smoke: `NOT RUN`
- live multiplayer session: `NOT TESTED`

첫 P0-F run `34187734949`은 production compile은 통과했지만 `priorityReservePreventsLowerPriorityBrownout` 테스트가 reserve 의미를 반대로 기대하여 실패했다. `UTILITY`의 40% reserve를 침범하는 소비를 허용하는 잘못된 기대값이었으며 production 로직은 변경하지 않고 테스트를 reserve 계약에 맞춰 수정했다. 재게이트 run `34187867167`에서 전체 성공했다.

### Status

`P0-F CENTRAL SYSTEMS BACKEND BUILD VERIFIED / SYSTEMS RESTART PERSISTENCE & LIVE ACCEPTANCE DEFERRED`

---

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
