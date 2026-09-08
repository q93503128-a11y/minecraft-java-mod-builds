# EARTH TO STARS — Changelog

이 문서는 실제 정본 변경을 기록한다.

## 2026-09-08 — alpha.12 Live-Acceptance Rescue

### Fixed / Changed

- mod version `0.1.0-alpha.12`
- ArmorStand starter ship runtime 제거, custom `ShipExteriorEntity` 도입
- pilot을 실제 passenger / `startRiding()` 구조로 전환
- 탑승 중인 실제 vehicle과 일치하는 player에게만 ship control packet 승인
- fake `tetherController` 제거
- Shift를 비행 pitch-down에서 제거해 vanilla 하차 동작 보존; pitch-down은 Ctrl/sprint-key 경로 사용
- Earth↔Orbit transition을 하차 → player teleport → same-ShipId target exterior 생성 → 재탑승 → 새 control session 발급 구조로 연결
- 함선을 때리는 동작은 비파괴로 변경; owner의 명시적 pack-up interaction만 authoritative retirement 수행
- retirement transaction이 repository / ShipSavedData / ShipSystemsSavedData / interior / turret / mission / runtime control을 함께 정리해 orphan ShipState 방지
- 보급 full/no-ship 반복 feedback을 chat 누적 대신 actionbar로 전환
- Minecraft 26.2 actionbar / ItemDisplay / collision API에 맞게 runtime 교정
- 6개 M1 recipe ingredient를 Minecraft 26.2 string syntax로 교정하고 validator로 회귀 차단
- Kenney Space Kit CC0 OBJ/MTL 직접 패키징
  - starter craft: `craft_speederA`
  - orbital salvage: `craft_miner`
  - first interceptor: `craft_racer`
- `launch_craft_kit`도 vanilla placeholder 대신 spacecraft OBJ model 사용
- alpha.12 acceptance validator에 ArmorStand/tether/obsolete recipe/obsolete 26.2 API/model packaging 회귀 gate 추가

### Verification

Build source commit: `52dadef15fa0bb6faf5048c51ae67892db191653`

GitHub Actions `Build earth-to-stars` run `34232842854`: `PASS`

- progression/M1 launch/Nether-End independence validators: `PASS`
- alpha.12 static acceptance: `PASS`
- existing JUnit regression: `PASS`
- `clean test build`: `PASS`
- production JAR verify: `PASS`
- JAR: `earth_to_stars-0.1.0-alpha.12.jar`
- SHA-256: `b44f85ba2d05b885b1319822a0044e70535bff93696900b8ba5e191e04b51c15`

Dedicated resource-load smoke run `34233144671`: `PASS`

- dedicated server reached normal startup completion
- 1591 recipes loaded
- alpha.11 recipe parsing error pattern not observed
- `earth_to_stars:ship_interiors` / `earth_to_stars:orbital_space` present

Persistence schema/layout did not change, so the expensive two-boot save/restart lifecycle was not rerun. Last verified two-boot run remains `34197931566`.

- live Earth→Orbit→salvage→combat→Earth return: `NOT PLAYTESTED`
- client visual quality/scale/camera: `NOT PLAYTESTED`
- live multiplayer: `NOT TESTED`

### Status

`ALPHA.12 LIVE-ACCEPTANCE RESCUE BUILD + DEDICATED RESOURCE LOAD VERIFIED / LIVE CLIENT ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED`

---

## 2026-09-08 — M1-D Orbital salvage + first contact + return upgrade

### Added / Changed

- mod version `0.1.0-alpha.11`
- starter craft에 비어 있는 `sensor` UTILITY slot 추가
- `orbital_scanner_mk1` module definition 추가
- `recovered_sensor_core` player-facing item 추가
- 첫 Earth Orbit 진입의 ship-scoped salvage encounter backend
- salvage 접근 시 실제 `autocannon_mk1`을 빈 turret hardpoint에 설치하고 ShipSavedData 즉시 갱신
- 실제 autocannon module이 없으면 기술 command/runtime도 함포를 가짜 생성하지 못하는 capability gate
- 회수 직후 동일 turret runtime을 `AUTO_DEFENSE`로 자동 기동
- 이후 같은 무장을 `MANUAL`로 전환 가능한 기존 control contract 유지
- first unmanned interceptor logical encounter
- server-authoritative hostile position / health / movement / attack interval
- hostile 공격이 중앙 `ShipPowerGrid`를 실제 drain
- mission hostile을 함선 중앙 `SensorGrid` contact에 합류
- turret logical projectile가 first-contact hostile health를 서버 판정으로 타격
- hostile 격파 시 `recovered_sensor_core` 보상 생성
- 지구 귀환 후 sensor core 사용으로 `orbital_scanner_mk1` 설치
- scanner 설치 후 sensor range `64 → 96`
- sensor core는 Earth-only / accessible ship / free sensor slot 조건을 만족할 때만 소비
- 같은 Orbit session에서 hostile/reward 즉시 무한 재생성 방지
- sensor core를 놓치거나 잃은 상태에서 Earth 귀환 시 session clear를 해제하여 다음 Orbit에서 first-contact 재수행이 가능한 anti-soft-lock 처리
- controlling pilot과 authoritative exterior의 64-block input-authority drift를 막는 M1 technical cockpit tether
- ShipState schema `1 → 2`
- schema 1 save decode 시 ShipId / owner / crew / 기존 slots / modules를 보존하고 누락된 `sensor` slot만 migration
- unknown future schema는 reset하지 않고 계속 거부
- starter empty turret/sensor contract JUnit
- recovered autocannon/scanner progression JUnit
- sensor range upgrade JUnit
- schema 1→2 migration JUnit
- production JAR verifier에 M1-D classes/resources 계약 추가

### Fixes found during alpha.11 review

- stale encounter cleanup에서 collection mutation 위험이 없도록 현재 stale-key copy/remove 패턴 유지 확인
- first hostile clear 후 core를 획득하지 못한 채 Orbit을 떠났을 때 이후 progression이 영구 막힐 수 있던 session-state soft-lock 수정
- 무장 모듈이 실제 ShipState에 없는데 기술 경로로 TurretRuntime을 생성할 수 있던 P0-era drift 제거

### Architecture

첫 Orbit 원정은 단순 loot 숫자가 아니라 두 번의 capability 변화로 연결된다.

```text
starter: turret 없음 / scanner 없음
→ orbital salvage 접근
→ autocannon_mk1 획득
→ AUTO_DEFENSE + MANUAL weapon capability
→ first interceptor contact
→ recovered_sensor_core
→ Earth return
→ orbital_scanner_mk1 설치
→ sensor range 64 → 96
```

Salvage/interceptor의 ArmorStand는 기술 proxy일 뿐 reward/health/authority의 정본이 아니다. encounter state, module install, hostile health, damage, power drain, reward resolution은 서버가 결정한다.

현재 cockpit tether 역시 production 탑승/카메라가 아니라 M1 integrated flight를 실제로 시험하기 위한 기술 연결이다.

### Verification

최종 검증 기준 구현 커밋: `afe0d181667582877e911ef279a869c7e31d0c23`

GitHub Actions `Build earth-to-stars` run `34197931566`: `PASS`

- P0-H progression validator: `PASS`
- M1 launch recipe dependency closure: `PASS`
- M1 launch recipe Nether/End independence: `PASS`
- starter empty weapon/sensor slots JUnit: `PASS`
- recovered autocannon/scanner progression JUnit: `PASS`
- sensor range `64 → 96` JUnit: `PASS`
- ShipState schema 1→2 migration JUnit: `PASS`
- existing regression JUnit: `PASS`
- `clean test build`: `PASS`
- Minecraft 26.2 / NeoForge 26.2.0.38-beta compile: `PASS`
- production JAR verify: `PASS`
- orbital mission / recovered sensor resources packaged: `PASS`
- dedicated server first boot/save: `PASS`
- clean shutdown: `PASS`
- same-world second boot/restore: `PASS`
- generated JAR: `earth_to_stars-0.1.0-alpha.11.jar`
- JAR SHA-256: `f818686c7dde57e7a33e27967069c7e2074ce6b97febd0b13721e1165ac37fb0`
- live Earth→Orbit→salvage→combat→Earth return cycle: `NOT TESTED`
- live multiplayer pilot/gunner/interior session: `NOT TESTED`
- client visual quality: `NOT TESTED`

ShipState schema가 바뀌었기 때문에 alpha.11에서는 expensive dedicated two-boot lifecycle을 의도적으로 한 번 다시 실행했다. 성공 후 workflow는 다시 explicit `workflow_dispatch`에서만 lifecycle을 반복하도록 복귀했다.

### Status

`M1-D ORBITAL RECOVERY + FIRST CONTACT BACKEND VERIFIED / SHIPSTATE SCHEMA 1→2 LIFECYCLE VERIFIED / FULL EARTH→ORBIT→RETURN LIVE ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED`

다음은 Moon 기능 추가가 아니라 첫 Earth→Orbit→salvage→combat→Earth→scanner upgrade 사이클의 live acceptance와 feel 교정이다.

---

## 2026-09-08 — M1-A/B Earth preparation + first launch craft

### Added

- mod version `0.1.0-alpha.9`
- 실제 Minecraft item registry 기반 Earth launch 제작 아이템 6종
  - `reinforced_frame`
  - `avionics_unit`
  - `propellant_cell`
  - `oxygen_cartridge`
  - `life_support_unit`
  - `launch_craft_kit`
- 신규 지구 광석 없이 Iron/Copper/Redstone/Gold/Amethyst/Gunpowder/Paper/Water/Leather를 우주 진입 제작 루프에 재연결
- `launch_craft_kit` 실제 survival crafting chain
- Overworld-only launch package deployment
- 3×3×3 deployment clearance 검사
- 기존 소유 함선을 새 패키지가 덮어쓰지 않는 ownership guard
- 성공 시에만 package 소비
- player-facing Korean/English item/message localization
- current client item-definition + model resource packaging
- `LaunchCraftBlueprint` canonical starter craft loadout
- `life_support_mk1` utility module
- starter craft slots: core / engine / power / cargo / life_support / turret
- starter craft 기본 설치: command core / engine / battery / cargo / life support
- turret hardpoint는 의도적으로 비워 첫 orbital reward가 capability upgrade가 되도록 구성
- survival deployment가 authoritative `ShipState` 생성 → repository → `ShipSavedData` → systems runtime → server pilot lease로 연결
- P0 command spawn도 같은 starter blueprint를 사용하여 기술용 spawn과 실제 survival craft의 구조 drift 방지
- `tools/validate_m1_launch.py`: 실제 `launch_craft_kit` recipe closure를 재귀 검사하여 Nether/End mandatory regression 차단
- `LaunchCraftBlueprintTest`
- production JAR verifier에 M1 recipes/item definitions/launch classes 확인 추가
- M1 정본 `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md`

### Design

M1 Earth preparation은 신규 광석과 중간 재화를 대량 추가하지 않는다. 첫 우주 진입은 기존 Minecraft 자원이 새로운 의미를 얻는 구조로 시작한다.

```text
vanilla Earth resources
→ frame / avionics / propellant / oxygen
→ life support
→ launch craft assembly package
→ Earth deployment
→ authoritative modular ship
```

첫 함선은 이동/전력/화물/생명유지 능력을 갖지만 weapon hardpoint는 비어 있다. 따라서 첫 Earth Orbit salvage/combat가 실제 플레이 방식과 함선 능력을 바꾸는 성장으로 연결될 여지를 남긴다.

현재 vanilla-texture item model과 ArmorStand exterior는 production art가 아니라 client/기술 proxy다.

### Verification

최종 검증 기준 구현 커밋: `bc8e51ba30d2e3eec07dfd868b0f79dc9460e73f`

GitHub Actions `Build earth-to-stars` run `34191142069`:

- P0-H progression validator self-tests: `PASS`
- canonical main progression graph: `PASS`
- M1 launch recipe dependency closure: `PASS`
- M1 launch recipe Nether/End independence: `PASS`
- starter craft blueprint JUnit: `PASS`
- P0-A~G JUnit regression: `PASS`
- `clean test build`: `PASS`
- Minecraft 26.2 / NeoForge 26.2.0.38-beta compile: `PASS`
- production JAR verifier: `PASS`
- M1 recipes/client item definitions packaged: `PASS`
- generated JAR: `earth_to_stars-0.1.0-alpha.9.jar`
- JAR SHA-256: `c2f033c73de080c90cff7ed77ae0b3d2d07d6ec5d14aa22cc0f173766e223264`
- P0-G dedicated lifecycle: `NOT RERUN`; last verified run `34188840459`
- live survival recipe crafting: `NOT TESTED`
- live in-world launch package deployment: `NOT TESTED`
- live Earth↔orbit flight: `NOT TESTED`
- live multiplayer: `NOT TESTED`
- client visual quality: `NOT RUN`

### Status

`M1-A/B EARTH PREPARATION + FIRST LAUNCH CRAFT BACKEND BUILD VERIFIED / LIVE CLIENT PLAY NOT TESTED / M1-C LAUNCH READINESS + ATMOSPHERE NEXT`

다음 작업은 propellant/oxygen을 실제 authoritative reserve와 연결하고 atmosphere ascent/Orbit transition readiness를 만드는 **M1-C**다.

---

## 2026-09-08 — P0-H Nether/End independence validator

### Added

- mod version `0.1.0-alpha.8`
- `data/earth_to_stars/progression/main_path.json` canonical progression graph
- `requires_any` 기반 대체 선행경로 표현
- Earth Industry → Launch Craft → Earth Orbit → Orbital Salvage → Moon → Near-Earth Asteroids → Mars → Main Belt → Outer System → Deep Space 메인 마일스톤
- optional `nether_heat_shortcut`, `nether_propellant_variant`, `end_navigation_sidegrade` side-route nodes
- `tools/validate_progression.py`
- `tools/test_progression_validator.py`
- Nether-only required route rejection
- End-only required route rejection
- clean alternative route acceptance
- optional Nether/End side-route acceptance
- unknown dependency rejection
- dependency cycle rejection
- production JAR verifier에 progression graph packaging 확인 추가
- ordinary push에서 P0-G dedicated two-boot lifecycle을 반복하지 않고 explicit `workflow_dispatch`에서만 다시 실행하도록 validation budget 조정

### Architecture

P0-H는 Nether/End 문자열 자체를 금지하지 않는다.

메인 milestone마다 dependency graph를 탐색하여 적어도 하나의 완전한 prerequisite derivation이 `minecraft:the_nether`와 `minecraft:the_end`를 거치지 않으면 통과한다.

따라서 다음은 허용된다.

```text
Earth route → main progression
Nether route → optional shortcut
End route → optional sidegrade
```

반대로 메인 progression이 Nether/End 경로만 남게 되면 CI가 실패한다.

### Verification

최종 검증 기준 구현 커밋: `8b3b4edda64505d476418e0b08fbe85baea6b0ba`

GitHub Actions `Build earth-to-stars` run `34189697283`:

- progression validator self-tests: `PASS`
- canonical main progression graph: `PASS`
- Nether-only required route rejection: `PASS`
- End-only required route rejection: `PASS`
- clean alternative route acceptance: `PASS`
- optional Nether/End route acceptance: `PASS`
- dependency cycle / unknown dependency rejection: `PASS`
- P0-A~G JUnit regression: `PASS`
- `clean test build`: `PASS`
- production JAR verifier: `PASS`
- progression graph packaged in production JAR: `PASS`
- 생성 JAR: `earth_to_stars-0.1.0-alpha.8.jar`
- JAR SHA-256: `3af179b7cdb16236722507434a000f38dcc82fc59079aab584e1f79771f2e688`
- P0-G dedicated save/restart lifecycle: `NOT RERUN`; last verified run `34188840459`
- live Earth↔orbit flight: `NOT TESTED`
- actual two-player pilot+gunner: `NOT TESTED`
- live multiplayer linked interior: `NOT TESTED`
- client/production visual quality: `NOT TESTED`

### Status

`P0 AUTOMATED TECHNICAL GATES COMPLETE / LIVE ACCEPTANCE DEFERRED / LIVE MULTIPLAYER NOT TESTED / M1 EARTH-ORBIT GAMEPLAY SLICE NEXT`

다음 의미 있는 작업 단위는 **M1-A + M1-B Earth Preparation + First Launch Craft**다. 기술 proof를 더 늘리지 않고 실제 플레이어 progression과 Earth→Orbit 첫 게임 루프 제작으로 전환한다.

---

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
- live multiplayer session: `NOT TESTED`

### Status

`P0-E TURRET BACKEND BUILD VERIFIED / LIVE COMBAT & MULTIPLAYER ACCEPTANCE DEFERRED`

---

## 2026-09-08 — P0-D linked ship interior backend

- mod version `0.1.0-alpha.4`
- stable `InteriorRef(shipId, slot)` / `InteriorSlotLayout`
- one `earth_to_stars:ship_interiors` technical dimension
- persistent `ShipId → interior slot`
- collision/corruption rejection
- entry/exit/recovery adapter
- P0 technical room
- linked-interior JUnit

Verification run `34183711601`: build/JAR `PASS`; live multi-interior `NOT TESTED`.

---

## 2026-09-08 — P0-C orbital transition + Minecraft persistence adapter

- mod version `0.1.0-alpha.3`
- `earth_to_stars:orbital_space`
- Earth↔orbit transition policy/runtime
- transfer rollback
- server-global `ShipSavedData`
- restore command
- P0-C JUnit

Verification run `34181251912`: build/JAR `PASS`; live Earth↔orbit `NOT TESTED`.

---

## 2026-09-08 — P0-B ship movement backend

- mod version `0.1.0-alpha.2`
- authoritative movement transform
- throttle/yaw/pitch
- control lease/session/replay rejection
- client input-only payload
- temporary ArmorStand exterior proxy

Verification run `34176522000`: build/JAR `PASS`; live control feel `NOT TESTED`.

---

## 2026-09-08 — M0 bootstrap + P0-A authoritative ship kernel

- mod version `0.1.0-alpha.1`
- Minecraft 26.2 / Java 25 / NeoForge 26.2.0.38-beta / Gradle 9.2.1 scaffold
- mod metadata / workflow / JAR verifier
- `ShipId`, `ShipState`, `ShipRepository`
- owner/crew/guest permission
- module/hardpoint compatibility
- versioned persistence codec

Verification run `34175374292`: build/JAR `PASS`.

---

## 2026-09-08 — Project registration / M0 canon lock

- 새 프로젝트 `projects/earth-to-stars/` 등록
- Overworld = Earth
- Nether/End 비필수
- B형 모듈식 함선
- server-authoritative multiplayer-first
- manual/automatic turret common weapon system
- stable linked interior
- central power/ammo/sensor
- Earth→Orbit→Moon→Asteroids→Planets→Deep Space progression
- external-reference visual gate
- third-party asset ledger
