# EARTH TO STARS — P0 & Vertical Slice Plan

이 문서는 “기획은 좋지만 Minecraft 26.2에서 핵심 기술이 안 된다”는 실패를 최대한 빨리 발견하기 위한 기술 검증 순서와 첫 플레이어블 수직 구간을 정의한다.

P0는 콘텐츠를 많이 만드는 단계가 아니다. 핵심 위험을 작은 실제 구현으로 닫는 단계다. 빌드 성공, dedicated-server 생명주기 검증, 실제 플레이 검증을 구분하며 작은 커밋마다 사용자 테스트를 반복해서 요구하지 않는다.

---

# 1. 현재 상태

`M0 VERIFIED / P0-A SAVEDDATA VERIFIED / P0-B BACKEND BUILD VERIFIED / P0-C TRANSITION BACKEND BUILD VERIFIED / P0-D LINKED INTERIOR + DISK RESTORE VERIFIED / P0-E TURRET BACKEND BUILD VERIFIED / P0-F CENTRAL SYSTEMS BACKEND BUILD VERIFIED / P0-G DEDICATED LIFECYCLE VERIFIED / LIVE MULTIPLAYER NOT TESTED / P0-H NEXT`

## 자동/서버 검증된 기술축

- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 build scaffold
- authoritative `ShipState`, module/permission/persistence codec
- server-authoritative movement/control lease backend
- Earth↔orbital-space transition policy/runtime adapter
- server-global `ShipSavedData`
- stable linked interior allocation + `InteriorSavedData`
- representative autocannon manual/auto state machine
- exclusive turret control lease + replay rejection
- cooldown / legal firing arc / server damage boundary
- central `ShipPowerGrid` / `ShipAmmoPool` / `ShipSensorGrid`
- priority reserve / deterministic generation / shared ammo contention
- propulsion / sensor / weapon central power consumption
- interior-linked crew → same `ShipId` system authority lookup
- `ShipSystemsSavedData` power/ammo persistence
- `orbital_space` / `ship_interiors` actual dedicated-server registration
- clean dedicated server save/shutdown/restart on the same world
- same ShipId / owner / module slots / interior slot / power / ammo disk restore
- sensor cache rebuilt rather than persisted
- P0-A~G regression JUnit/build/JAR gate

최신 P0-G 검증:

- implementation/final CI commit: `557d273ecfa78c1ba9cc62956cd78f6eb7c55153`
- Actions run: `34188840459`
- alpha: `0.1.0-alpha.7`
- SHA-256: `76bc15395500382f0acbc68826c7e6b95533b5ce9863de40e837c9a2856ac708`

## 아직 실제 플레이/멀티에서 검증되지 않은 것

- 실제 ship exterior 조종감 / camera / interpolation
- 실제 Earth→orbit→Earth player flight
- 실제 exterior↔interior entry/exit
- 두 플레이어의 동일 interior 동시 체류
- 한 명이 외부 조종 중 다른 승무원이 내부에 남는 lifecycle
- actual multiplayer 2+ player session
- 실제 manual turret 조준/사격감
- 실제 AUTO_DEFENSE 전투/타격/피드백
- projectile visual / tracer / impact / sound
- production visual quality

위 항목은 dedicated-server lifecycle 성공만으로 완료라고 표현하지 않는다.

---

# 2. M0 — Build Bootstrap — VERIFIED

## 목표

저장소 표준에 맞는 실제 NeoForge 26.2 프로젝트를 만든다.

## 구현

- Gradle 9.2.1 wrapper
- Java 25 toolchain
- NeoForge 26.2.0.38-beta
- ModDevGradle 2.0.143
- mod metadata / namespace
- JUnit
- server/client run configs
- project-specific GitHub Actions
- production JAR verifier / SHA report
- `mod_version` 기반 version-aware artifact/report naming

## 현재 상태

`BUILD VERIFIED / DEDICATED SERVER BOOT VERIFIED / CLIENT SMOKE NOT RUN`

---

# 3. P0-A — Authoritative Ship Kernel — VERIFIED

## 구현

- `ShipId`
- `ShipState`
- `ModuleDefinition`
- `ModuleInstance`
- hardpoint/slot compatibility
- owner/crew/guest permission
- versioned persistence
- `ShipRepository`
- server-global `ShipSavedData`

## 검증

- valid/invalid install
- module removal
- ownership permission
- duplicate instance rejection
- serialization round trip
- schema validation
- dedicated-server disk save → restart → same `ShipId / owner / slots` restore

## 현재 상태

`SHIP KERNEL + REAL DISK RESTORE VERIFIED`

---

# 4. P0-B — Ship Exterior / Movement Backend — BUILD VERIFIED

## 구현됨

- temporary exterior object (`ArmorStand`, 기술 프록시)
- `ShipTransform`
- forward/right/up orientation
- throttle / yaw / pitch
- acceleration/deceleration
- server-authoritative transform
- server-issued control lease
- session UUID / monotonic input sequence / expiry
- client input-only payload boundary
- logout/dimension lease release
- central PowerGrid propulsion coupling

## 비목표

- 완전 자유 블록 물리
- 최종 ship model
- 최종 flight model
- 궤도역학 완성

## 현재 상태

`BACKEND BUILD VERIFIED / LIVE CONTROL FEEL NOT TESTED`

실플레이에서 확인할 것:

- 입력 지연
- 멀미 유발 회전
- camera
- 가속/감속
- power shortage feedback
- interpolation
- reconnect lease cleanup

---

# 5. P0-C — Earth → Orbital Space Transition — BUILD VERIFIED

## 구현됨

```text
Overworld craft ascent
→ altitude transition envelope
→ server transaction
→ orbital_space
→ same ShipState / ShipId
→ exterior replacement
→ control lease recovery
```

- Earth upward / orbit downward transition policy
- destination x/z/yaw/pitch 유지
- vertical velocity 제한
- target level absence rejection
- destination exterior failure rollback
- `earth_to_stars:orbital_space`
- server-global persistence integration

P0-G에서 `orbital_space` 자체가 dedicated server에서 실제 등록·저장·재부팅되는 것은 확인했다. 그러나 플레이어가 실제 함선을 몰아 Earth↔orbit를 왕복하는 경험은 아직 `NOT TESTED`다.

## 현재 상태

`TRANSITION BACKEND BUILD VERIFIED / ORBITAL DIMENSION SERVER LIFECYCLE VERIFIED / LIVE FLIGHT NOT TESTED`

후속 연출:

- sky darkening
- atmosphere thinning
- star reveal
- Earth horizon
- sound transition
- re-entry presentation

---

# 6. P0-D — Linked Ship Interior — SERVER LIFECYCLE VERIFIED

## 구현됨

- `InteriorRef(shipId, slot)`
- `InteriorSlotLayout`
- stable `ShipId → interior slot`
- reverse slot→ShipId lookup
- server-global `InteriorSavedData`
- 하나의 `earth_to_stars:ship_interiors` technical dimension
- per-ship 2048-block isolated cell
- persisted slot collision/corruption rejection
- `INTERIOR_ACCESS` permission-gated entry adapter
- interior→current exterior return adapter
- unavailable exterior / invalid link recovery
- login recovery path
- P0 technical room generation
- interior-linked player → same `ShipId` systems authority

## 설계 원칙

```text
ShipState(shipId)
   ↕
Exterior runtime(current layer/transform)
   ↕
InteriorRef(shipId, stable cell)
   ↕
ShipSystemsRuntime(shipId)
```

외부가 움직이거나 Earth↔orbit를 전환해도 내부 플레이어는 안정된 interior cell 좌표계에 남는다.

P0-G dedicated-server two-boot gate에서 `ship_interiors` 등록과 동일 `ShipId → slot` 디스크 복원은 실제 검증했다.

## 현재 상태

`LINKED INTERIOR DISK LIFECYCLE VERIFIED / LIVE PLAYER ENTRY & MULTIPLAYER INTERIOR NOT TESTED`

P0 기술 room의 smooth stone/barrier/lighting은 final interior 디자인이 아니다.

---

# 7. P0-E — Representative Turret — BUILD VERIFIED

대표 P0 무기 `autocannon_mk1`은 기술 프록시다.

## Modes

- `OFF`
- `MANUAL`
- `AUTO_DEFENSE`

## Manual flow

```text
WEAPON_CONTROL permission
→ MANUAL
→ exclusive server lease
→ session UUID + sequence validation
→ authoritative aim request
→ legal arc / shared power / shared ammo / cooldown validation
→ server logical shot
```

## Auto flow

```text
central ShipSensorGrid
→ interval contact acquisition
→ hostile filter
→ range / arc eligibility
→ target priority
→ AUTO_DEFENSE fire
→ same PowerGrid / AmmoPool / cooldown state
```

## 자동 검증

- exclusive lease: PASS
- replay/stale input rejection: PASS
- shared ammo/power consumption: PASS
- cooldown/rear arc rejection: PASS
- neutral auto-fire rejection: PASS
- central SensorGrid hostile selection: PASS
- mode switch lease cleanup: PASS
- server-authoritative damage boundary: 유지
- Minecraft 26.2 compile/JAR: PASS

## 현재 상태

`TURRET BACKEND + CENTRAL RESOURCE COUPLING VERIFIED / LIVE COMBAT & MULTIPLAYER NOT TESTED`

명령어 조작면과 논리 projectile은 production UX/비주얼이 아니다.

---

# 8. P0-F — Central Ship Systems — BUILD VERIFIED

## 구조

```text
ShipId
 └─ ShipSystemsRuntime
     ├─ ShipPowerGrid
     ├─ ShipAmmoPool
     └─ ShipSensorGrid
          ↑
   propulsion / sensors / turret(s)
```

## PowerGrid

- finite storage
- deterministic once-per-tick generation
- input-scaled propulsion consumption
- sensor/weapon consumption
- priority reserve
  - ESSENTIAL: 0%
  - PROPULSION: 10%
  - WEAPONS: 25%
  - UTILITY: 40%

## AmmoPool

- turret-local ammo 제거
- `autocannon_round` 중앙 탄약
- multiple weapon runtime → one truth
- failed fire → no partial power/ammo consumption

## SensorGrid

- one ship cache
- interval acquisition
- per-ShipId phase staggering
- stale contact expiry
- per-turret broad world scan 금지

## P0-G 이후 persistence 상태

P0-F 당시 미완료였던 current power/ammo restart persistence는 P0-G에서 닫았다.

- `ShipSystemsSnapshot`
- `ShipSystemsSavedData`
- checkpoint + server-stopping flush
- same-world two-boot restore
- power `37.5` / ammo `73` 실제 disk round trip 검증

센서 contact는 월드 유도 캐시이므로 의도적으로 저장하지 않는다.

## 현재 상태

`CENTRAL SYSTEMS BACKEND VERIFIED / POWER+AMMO DISK RESTORE VERIFIED / LIVE BROWNOUT UX & PERFORMANCE PROFILING NOT TESTED`

---

# 9. P0-G — Dedicated Lifecycle / Multiplayer Gate — SERVER PART VERIFIED

P0-G는 자동/서버에서 검증 가능한 부분과 실제 2인 플레이가 필요한 부분을 분리한다.

## Phase 1 — persistence contract — VERIFIED

- central PowerGrid current value save/restore
- AmmoPool type/count save/restore
- `ShipId` keyed system persistence
- persisted value validation against current capacity
- orphan systems state rejection
- runtime↔SavedData snapshot/restore
- volatile sensor contacts are not persisted

## Phase 2 — dedicated-server lifecycle — VERIFIED

GitHub Actions run `34188840459`이 같은 `run/world`에 서버를 두 번 실제 기동했다.

### Boot 1

- `orbital_space` load
- `ship_interiors` load
- fixed probe ShipId write
- owner/module slots write
- interior slot allocation
- power `37.5`, ammo `73` write
- clean server halt
- all dimensions saved

### Boot 2

- same world reload
- same custom dimensions load
- same ShipId/owner/module slots restore
- same interior slot `0` restore
- same power `37.5` restore
- same ammo `73` restore
- restored `ShipSystemsRuntime` initialization
- clean shutdown

검증 마커:

- `EARTH_TO_STARS_P0G_SEED_PASS`
- `EARTH_TO_STARS_P0G_VERIFY_PASS`

CI probe는 환경변수 `EARTH_TO_STARS_LIFECYCLE_PROBE`가 있을 때만 동작하며 일반 플레이어용 기능이 아니다.

## Phase 3 — live multiplayer — NOT TESTED

실제 멀티 환경이 가능한 경우 한 번의 의미 있는 세션으로 검사한다.

### Shared Ship

- P1 owner/pilot
- P2 crew/gunner
- P1 exterior control
- P2 interior 또는 turret control
- same ShipState / PowerGrid / AmmoPool 확인
- no duplicate projectile/damage
- exterior movement 중 interior link 유지

### Control conflict

- 두 플레이어가 같은 turret lease 요청
- 하나만 성공
- loser는 fire authority 없음

### Disconnect/reconnect

- pilot/gunner/interior logout
- lease cleanup
- reconnect orphan 없음

### Earth↔Orbit

- pilot exterior transition
- crew interior
- same ShipId
- interior crew stable
- shared resources duplicate 없음

실제 2인 환경을 사용하기 전까지 상태는 반드시:

`LIVE MULTIPLAYER NOT TESTED`

으로 남긴다.

---

# 10. P0-H — Nether/End Independence Validator — NEXT

## 목표

개발 중 실수로 Nether/End가 메인 우주 진행의 필수 게이트가 되는 것을 자동 탐지한다.

메인 progression graph에서 다음이 필수 ancestor가 되면 실패한다.

- Nether-only resource
- End-only resource
- Nether/End structure drop
- dimension-only advancement

허용:

- optional sidegrade
- shortcut
- specialist material
- late-game variant
- 선택형 위험/보상 루트

검증기는 향후 recipe/resource/progression 데이터가 늘어날 때 CI에서 자동으로 main-path independence를 확인할 수 있어야 한다.

---

# 11. P0 종료 조건

P0 완료 조건:

- 실제 26.2 project build
- authoritative ShipState + disk persistence
- B형 exterior movement backend
- Earth↔space transition backend
- stable linked interior + disk persistence
- representative manual/auto turret
- central power/ammo/sensor simulation
- central power/ammo disk persistence
- dedicated server custom-dimension lifecycle
- save/shutdown/restart/restore lifecycle
- Nether/End main-path independence validator
- multiplayer-authoritative 구조
- 실제 멀티 테스트 가능 시 2인 기본 세션

실제 2인 환경이 없다면 마지막 항목은 `NOT TESTED`로 남기고 P0의 자동/서버 검증 범위와 구분한다.

---

# 12. M1 — Earth/Orbit Gameplay Slice

P0가 닫히면 첫 실제 게임성을 만든다.

범위:

- Earth 초기 산업 최소 세트
- launch craft 제작
- fuel/oxygen
- 실제 atmosphere progression
- Earth Orbit environment
- 첫 salvage contact
- 첫 hostile drone/contact
- usable manual/auto autocannon
- orbital salvage reward
- Earth return

종료 경험:

> “내가 만든 작은 우주선으로 지구를 떠나 궤도에서 무언가를 회수하고 살아 돌아왔다.”

이 경험 자체가 재미있어야 Moon/Mars/소행성을 늘린다.

---

# 13. M2 — Moon Vertical Slice

- Moon local world
- landing/launch
- low gravity
- vacuum survival
- lunar resource
- 대표 discovery
- 작은 outpost
- meaningful ship upgrade

```text
Earth preparation
→ launch
→ orbit encounter
→ Moon
→ lunar exploration/resource
→ return
→ upgrade
```

---

# 14. M3 — Production Visual Gate

기능 프록시를 실제 제품 비주얼로 교체한다.

- ship reference board
- launch craft model
- cockpit/HUD mockup
- production interior reference/layout
- turret model/animation/VFX/sound
- Earth Orbit visuals
- Moon material/terrain reference

`03_UI_ART_REFERENCE_GATE.md`를 따른다.

---

# 15. M4 — Asteroid / Ship Growth

- Near-Earth Asteroids
- mining beam
- cargo trade-offs
- sensor signatures
- heavy weapon candidate
- significant ship frame expansion
- early automation

---

# 16. M5 — Mars / Expedition Ship

- Mars content pack
- longer survival loop
- thermal/environment expansion
- onboard refinery
- medium expedition frame

---

# 17. M6 — Belt / Multi-System Scale

- Main Belt
- larger fleet encounters
- drone mining
- multi-turret fire control
- mobile-base gameplay
- worst-case multiplayer/performance profiling

---

# 18. M7+ — Outer System / Deep Space

앞 단계의 gameplay, content pipeline, performance, multiplayer가 검증된 경우에만 확장한다. 새 천체 수로 규모를 과장하지 않는다.

---

# 19. 사용자 직접 테스트 전달 규칙

실제 JAR 테스트 단계에서는 항상 함께 제공한다.

- 테스트 JAR
- 필요한 `/give`
- 필요한 `/summon`
- 필요한 `/tp` 또는 기술검증 명령
- 테스트 순서
- 정상 결과
- 이상 증상 체크리스트

작은 기술 커밋마다 사용자 테스트를 요구하지 않는다. 관련 기능을 의미 있는 lifecycle/gameplay 단위로 묶어 검사한다.

---

# 20. 바로 다음 구현 단위

**P0-H Nether/End Independence Validator**

하나의 의미 있는 작업 묶음에 다음을 포함한다.

- canonical progression graph schema
- node source-dimension/source-kind metadata
- mandatory dependency edge와 optional edge 구분
- Earth→launch→orbit 메인 목표 노드 정의
- Nether/End-only node가 mandatory ancestor인지 탐색
- sidegrade/shortcut는 허용
- cycle/missing dependency 검증
- pure unit test
- CI validator gate
- 현재 기획의 Nether/End 비필수 계약을 machine-checkable하게 고정

P0-H 뒤에는 작은 기술 기능을 계속 늘리기보다 현재 이동/우주전환/interior/포탑/중앙자원을 **실제 플레이 가능한 Earth→Orbit 첫 테스트 덩어리**로 묶는다.
