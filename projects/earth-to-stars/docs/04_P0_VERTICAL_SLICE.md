# EARTH TO STARS — P0 & Vertical Slice Plan

이 문서는 “기획은 좋지만 Minecraft 26.2에서 핵심 기술이 안 된다”는 실패를 최대한 빨리 발견하기 위한 기술 검증 순서와 첫 플레이어블 수직 구간을 정의한다.

P0는 콘텐츠를 많이 만드는 단계가 아니다. 핵심 위험을 작은 실제 구현으로 닫는 단계다. 빌드 성공과 실제 플레이 검증을 구분하며, 반복적인 수동 테스트를 매 작은 커밋마다 요구하지 않는다.

---

# 1. 현재 상태

`M0 VERIFIED / P0-A SAVEDDATA ADAPTER BUILD VERIFIED / P0-B BACKEND BUILD VERIFIED / P0-C TRANSITION BACKEND BUILD VERIFIED / P0-D LINKED INTERIOR BACKEND BUILD VERIFIED / P0-E TURRET BACKEND BUILD VERIFIED / P0-F CENTRAL SYSTEMS BACKEND BUILD VERIFIED / LIVE INTEGRATION DEFERRED / P0-G LIFECYCLE GATE NEXT`

## 자동 검증된 기술축

- Minecraft 26.2 / NeoForge 26.2.0.38-beta / Java 25 build scaffold
- authoritative `ShipState`, module/permission/persistence codec
- server-authoritative movement/control lease backend
- Earth↔orbital-space transition-policy/runtime adapter
- server-global Ship SavedData adapter
- stable linked interior allocation/persistence backend
- representative autocannon manual/auto state machine
- exclusive turret control lease + replay rejection
- cooldown / legal firing arc rule
- server-side P0 logical projectile/damage authority boundary
- central `ShipPowerGrid` / `ShipAmmoPool` / `ShipSensorGrid`
- priority reserve / deterministic generation / shared ammo contention
- propulsion / sensor / weapon central power consumption
- interior-linked crew → same `ShipId` system authority lookup
- `orbital_space` / `ship_interiors` 기술 차원 production-JAR packaging
- P0-A/B/C/D/E/F 순수 JUnit 회귀

최신 P0-F 자동 검증:

- implementation/final CI commit: `8e65d142f2a4dc3edfd7ef30116ad0929d4bc622`
- Actions run: `34187867167`
- alpha: `0.1.0-alpha.6`
- SHA-256: `527f02b6c3a70337c25a8aeebda3c0d2059818fc9e49efc77ab23bba016a07e6`

## 아직 실게임 검증/구현되지 않은 것

- central power/ammo runtime quantity persistence across restart
- 실제 custom-dimension server boot
- save→disk→server restart→same ship/interior/systems restore
- 실제 ship exterior 조종감 / camera / interpolation / reconnect
- 실제 Earth→orbit→Earth flight
- 실제 exterior↔interior entry/exit
- 두 플레이어의 동일 interior 동시 체류
- 한 명이 외부 조종 중 다른 승무원이 내부에 남는 lifecycle
- actual multiplayer 2+ player session
- 실제 manual turret 조준/사격감
- 실제 AUTO_DEFENSE 전투/타격/피드백
- projectile visual / tracer / impact / sound
- production visual quality

위 항목은 자동 컴파일 성공만으로 완료라고 표현하지 않는다. P0-G에서 먼저 저장/서버 생명주기를 자동으로 닫고, 실제 멀티가 가능한 경우에만 여러 기술축을 한 번의 의미 있는 live gate로 묶는다.

---

# 2. M0 — Build Bootstrap

## 목표

저장소 표준에 맞는 실제 NeoForge 26.2 프로젝트를 만든다.

## 구현

- Gradle 9.2.1 wrapper
- Java 25 toolchain
- NeoForge 26.2.0.38-beta
- ModDevGradle 2.0.143
- mod metadata / minimal entrypoint / namespace
- JUnit
- server/client run configs
- project-specific GitHub Actions
- production JAR verifier / SHA report
- `mod_version`에서 artifact/report 버전을 자동 파생하는 CI

## 현재 상태

`BUILD VERIFIED`

실제 dedicated server/client smoke는 아직 별도 `NOT RUN`이다.

---

# 3. P0-A — Authoritative Ship Kernel

## 목표

Minecraft 렌더링 없이도 함선 게임의 서버 정본을 만든다.

## 구현

- `ShipId`
- `ShipState`
- `ModuleDefinition`
- `ModuleInstance`
- hardpoint/slot compatibility
- owner/crew/guest permission
- versioned persistence
- ShipRepository
- server-global Minecraft SavedData adapter

## 자동 테스트

- valid/invalid install
- module removal
- ownership permission
- duplicate instance rejection
- serialization round trip
- schema version validation
- P0-B/C/D/E/F에서 regression

## 현재 상태

`PURE KERNEL VERIFIED / SAVEDDATA ADAPTER BUILD VERIFIED / LIVE RESTART RESTORE NOT TESTED`

GameTest/dedicated-server create→save→restart→same shipId/modules restore는 P0-G integration gate에 묶는다.

---

# 4. P0-B — Ship Exterior / Movement Backend

## 목표

B형 함선을 실제 Minecraft에서 움직일 수 있는 최소 외부 표현을 만든다.

## 구현됨

- temporary exterior object (`ArmorStand`, 기술 프록시)
- `ShipTransform`
- forward/right/up orientation
- throttle
- yaw/pitch
- acceleration/deceleration
- server-authoritative transform
- server-issued control lease
- session UUID / monotonic input sequence / expiry
- client input-only payload boundary
- logout/dimension lease release
- P0-F central PowerGrid propulsion coupling

## 비목표

- 완전 자유 블록 물리
- 최종 ship model
- 최종 flight model
- 궤도역학 완성

## 현재 상태

`BACKEND BUILD VERIFIED / CENTRAL POWER COUPLING BUILD VERIFIED / LIVE CONTROL FEEL NOT TESTED`

실게임 검사에서 확인할 것:

- 입력 지연
- 멀미 유발 회전
- camera
- 가속/감속
- power shortage feedback
- interpolation
- reconnect lease cleanup

기술 프록시 외형을 final visual로 간주하지 않는다.

---

# 5. P0-C — Earth → Orbital Space Transition

## 목표

지상에서 올라가 우주 공간으로 이동하는 핵심 판타지가 기술적으로 성립하는지 검증한다.

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
- destination x/z/yaw/pitch 유지 정책
- vertical velocity 제한 정책
- target level absence rejection
- destination exterior failure rollback
- `earth_to_stars:orbital_space` technical dimension
- server-global Ship SavedData integration

## 현재 multiplayer boundary

현재 외부 transition은 **pilot-first proof**다. 외부 passenger seat의 다인 전환은 아직 구현/검증 완료가 아니다.

P0-D linked interior의 승무원은 외부 함선 좌표를 따라 매 tick 이동하지 않는다. 내부 플레이어는 안정된 interior cell에 남고 동일 `ShipId`가 가리키는 외부 함선의 layer/transform이 바뀐다. 이 구조가 다인 함선에서 interior 승무원 누락/desync를 줄이는 정본 방향이다.

## 현재 상태

`TRANSITION BACKEND BUILD VERIFIED / LIVE EARTH↔ORBIT NOT TESTED`

최종 연출 후속:

- sky darkening
- atmosphere thinning
- star reveal
- Earth horizon
- sound transition
- re-entry presentation

---

# 6. P0-D — Linked Ship Interior

## 목표

외부 함선이 움직여도 여러 플레이어가 안정적인 내부 공간을 사용할 수 있는 기반을 만든다.

## 구현됨

- `InteriorRef(shipId, slot)`
- `InteriorSlotLayout`
- stable `ShipId → interior slot`
- reverse slot→ShipId lookup
- server-global `InteriorSavedData`
- 하나의 `earth_to_stars:ship_interiors` technical dimension
- per-ship 2048-block isolated cell
- persistent slot collision/corruption rejection
- `INTERIOR_ACCESS` permission-gated exterior→interior entry adapter
- interior→current exterior dimension/transform return adapter
- exterior unavailable / invalid interior recovery path
- login recovery path
- P0 technical room generation
- allocation/layout/collision JUnit
- P0-F interior-linked player → same `ShipId` systems lookup boundary

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

내부 플레이어는 외부 함선 translation/rotation에 맞춰 좌표를 매 tick 변환하지 않는다. 외부가 Earth↔orbit으로 이동해도 내부 cell은 그대로 유지된다.

P0 기술 room의 smooth stone/barrier/lighting은 final interior 디자인이 아니다. Production interior는 `03_UI_ART_REFERENCE_GATE.md` 이후 별도 모델/재질/UI/연출 품질 게이트를 통과한다.

## 현재 상태

`LINKED INTERIOR BACKEND BUILD VERIFIED / SYSTEM LOOKUP BUILD VERIFIED / LIVE MULTIPLAYER INTERIOR NOT TESTED`

P0-G에서 확인:

- owner/crew/guest 실제 entry/exit
- 2인 이상 동시 체류
- exterior movement 중 interior 유지
- Earth↔orbit transition 중 interior crew 유지
- restart 후 same interior link
- same power/ammo truth after restart
- unavailable exterior에서 orphan 방지

---

# 7. P0-E — Representative Turret — BUILD VERIFIED

## 목표

한 무기 시스템에서 manual과 automatic control이 멀티 안전하게 공존하는지 검증한다.

## 대표 무기

P0 `autocannon_mk1` — 기술 프록시. 현재 이름/수치/조작면/논리 projectile을 production 콘텐츠로 고정하지 않는다.

선정 이유:

- projectile
- ammo
- rotation/arc
- target
- fire rate
- manual / auto
- multiplayer control lease

를 한 번에 검증할 수 있기 때문.

## 구현된 Control Modes

- `OFF`
- `MANUAL`
- `AUTO_DEFENSE`

`ASSISTED` / 공격적 자동 표적은 후속.

## Manual Flow

```text
WEAPON_CONTROL permission
→ MANUAL mode
→ exclusive server weapon lease
→ session UUID + sequence validation
→ authoritative player aim request
→ legal arc / shared power / shared ammo / cooldown validation
→ server logical shot
```

현재 `/earthtostars ship turret ...` 명령은 P0 조작면일 뿐이다. 최종 게임에서는 명령어가 아니라 실제 gunner station / key / camera / HUD로 교체한다.

## Auto Flow

```text
central ShipSensorGrid contacts
→ hostile filter
→ range / firing-arc eligibility
→ target priority
→ AUTO_DEFENSE fire
→ same authoritative PowerGrid / AmmoPool / cooldown state
```

Minecraft P0 adapter의 센서 검색은 P0-F에서 함선 중앙 `ShipSystemsManager`로 이동했다. 함선당 contact cache를 주기적으로 갱신하며 `ShipId` hash로 scan phase를 분산한다. **각 포탑이 매 tick 큰 반경 world scan을 독립 수행하지 않는다.**

현재 hostile 판정은 기술 검증용으로 Minecraft `Enemy` 계열만 사용한다. 장차 함선/세력/소유권/우호 관계를 포함한 target eligibility로 교체한다.

## Projectile Authority

현재 P0 projectile은 서버의 논리 moving point다.

- server position
- velocity
- lifetime
- collision envelope
- authoritative damage

을 가진다.

아직 없는 production 요소:

- 실제 projectile/tracer render
- muzzle flash
- turret model rotation
- firing animation
- impact VFX
- sound
- camera recoil/shake
- 실제 함선 대 함선 damage model

## 자동 Acceptance 결과

- 두 플레이어가 같은 manual lease를 동시에 보유하지 못함: `PASS` (JUnit)
- replay/stale sequence rejection: `PASS`
- shared ammo consumption: `PASS`
- shared power consumption: `PASS`
- cooldown rejection: `PASS`
- invalid rear arc rejection: `PASS`
- neutral contact auto-fire rejection: `PASS`
- central SensorGrid hostile selection: `PASS`
- manual→auto mode switch lease cleanup: `PASS`
- client가 hit/damage 결과를 authoritative하게 제출하지 않는 server boundary: 코드 구조 유지
- Minecraft 26.2 adapter compile: `PASS`
- production JAR verify: `PASS`

## 현재 한계

- power/ammo runtime quantity의 restart persistence는 아직 없다.
- command control surface는 production UX가 아니다.
- 실제 수동/자동 사격감과 실멀티 control conflict는 `NOT TESTED`다.

## 현재 상태

`TURRET BACKEND BUILD VERIFIED / CENTRAL RESOURCE COUPLING BUILD VERIFIED / LIVE COMBAT & MULTIPLAYER NOT TESTED`

첫 P0-E compile gate는 26.2 `getEntities` overload ambiguity로 실패했고, 기능 삭제 없이 source-entity 타입을 명시해 수정했다. 이후 전용 build가 성공했다. 성공 산출물의 workflow report가 옛 P0-D/alpha.4 라벨을 하드코딩한 것도 자체 검수에서 발견해, CI가 `gradle.properties`의 `mod_version`을 자동 읽게 고친 뒤 최종 run `34186350799`까지 다시 성공시켰다.

---

# 8. P0-F — Central Ship Systems — BUILD VERIFIED

## 목표

모듈/포탑 수가 커져도 계산 구조가 확장 가능하고, 함선 전체가 하나의 자원/센서 정본을 공유하는지 검증한다.

## 구현됨

```text
ShipId
 └─ ShipSystemsRuntime
     ├─ ShipPowerGrid
     ├─ ShipAmmoPool
     └─ ShipSensorGrid
          ↑
   propulsion / sensors / turret(s)
```

### PowerGrid

- finite storage
- deterministic once-per-tick generation
- monotonic server tick contract
- input-scaled propulsion consumption
- sensor-scan consumption
- weapon-shot consumption
- priority reserve
  - ESSENTIAL: 0% reserve floor
  - PROPULSION: 10%
  - WEAPONS: 25%
  - UTILITY: 40%

초기에는 플레이어가 priority를 미세 관리하게 만들지 않는다. 기본 priority가 대부분의 상황을 해결해야 한다. 수치는 P0 검증값이며 production balance 고정값이 아니다.

### AmmoPool / Logistics

- P0-E turret-local ammo 제거
- `autocannon_round` 함선 중앙 compatible ammo pool
- server-authoritative consumption
- 여러 weapon runtime 동시 fire에서도 하나의 truth 사용
- failed weapon transaction은 ammo/power를 부분 소비하지 않음
- 추후 compartment/zone 물류가 재미를 증명할 때만 확장

블록 파이프 하나마다 item entity/tick을 돌리는 구조를 기본으로 하지 않는다.

### SensorGrid

- P0-E contact cache를 central systems service로 이동
- interval acquisition
- cached contacts
- per-ShipId scan phase staggering
- stale contact expiry
- per-weapon eligibility
- multi-turret가 같은 scan 결과를 공유

### Interior crew boundary

함선 내부 플레이어는 외부 entity와의 물리 거리 대신 linked `ShipId`를 통해 중앙 시스템에 접근한다. 따라서 이후 조종석·기관실·포수석이 모두 같은 PowerGrid / AmmoPool / SensorGrid를 보게 할 수 있다.

## 자동 검증 결과

- P0-A~E 회귀: `PASS`
- priority reserve boundary: `PASS`
- generation monotonicity / same-tick double generation 방지: `PASS`
- two turret runtimes → one shared ammo pool: `PASS`
- failed fire → no partial ammo/power consumption: `PASS`
- propulsion draw scales with input / idle costs no propulsion power: `PASS`
- stale sensor contact expiry: `PASS`
- Minecraft central systems coordinator compile: `PASS`
- production JAR verify: `PASS`

최종 자동 검증:

- commit: `8e65d142f2a4dc3edfd7ef30116ad0929d4bc622`
- Actions run: `34187867167`
- version: `0.1.0-alpha.6`
- SHA-256: `527f02b6c3a70337c25a8aeebda3c0d2059818fc9e49efc77ab23bba016a07e6`

첫 P0-F run `34187734949`은 production compile은 성공했지만 reserve test의 기대값이 계약을 잘못 해석해 실패했다. production PowerGrid는 수정하지 않고 테스트를 올바른 reserve 경계에 맞춘 뒤 run `34187867167`에서 전체 성공했다.

## 아직 닫히지 않은 것

- central power/ammo current quantity persistence
- save/restart same systems state
- module-definition 실제 설치 상태에서 generation/capacity 파생
- production data-driven systems content
- 실제 부하 profiler 측정
- live brownout UX / feedback

## 현재 상태

`CENTRAL SYSTEMS BACKEND BUILD VERIFIED / RESTART PERSISTENCE & LIVE PERFORMANCE NOT TESTED`

---

# 9. P0-G — Lifecycle / Multiplayer Gate — NEXT

구조만 멀티 친화적이라고 끝내지 않는다. 단, 바로 사용자에게 2인 테스트를 요구하지 않는다. 먼저 자동/서버 생명주기 준비를 끝낸다.

## Phase 1 — persistence contract

- central PowerGrid stored value versioned save
- AmmoPool type/count versioned save
- `ShipId`와 시스템 저장 key 일치 검증
- corrupted/unknown schema를 조용히 초기화하지 않음
- runtime↔SavedData snapshot/restore

## Phase 2 — lifecycle automation / dedicated server

- create ship
- mutate modules/resources/interior assignment
- save to disk
- server restart/reload
- same ShipId/module/interior/power/ammo restore
- `orbital_space` / `ship_interiors` 실제 dedicated-server boot
- orphan/reconnect recovery

가능한 것은 GameTest/서버 자동검증으로 먼저 닫는다. 실제로 실행하지 않은 항목을 PASS라고 쓰지 않는다.

## Phase 3 — Shared Ship live session

실제 멀티 환경이 가능한 경우:

- P1 owner/pilot
- P2 crew/gunner
- P1 exterior control
- P2 interior 또는 turret control

검증:

- authoritative transform sync
- same ShipState observation
- one authoritative PowerGrid / AmmoPool
- no duplicate projectile/damage
- interior state remains linked while exterior moves

## Scenario B — Control Conflict

- 두 플레이어가 같은 turret control 요청

정상:
- 하나만 lease 획득
- 명확한 feedback
- loser가 fire authority를 얻지 못함

## Scenario C — Disconnect / Restart

- pilot disconnect
- gunner disconnect
- interior logout
- server save/restart

정상:
- leases cleaned
- ShipState persists
- interior assignment persists
- central power/ammo persists
- reconnect player not orphaned

## Scenario D — Earth↔Orbit

- pilot exterior transition
- crew interior

정상:
- same ShipId
- interior crew remains stable
- exterior layer/transform updates
- inventory/ammo/resources duplicate 없음

실제 검증 전 상태 표기:

`MULTIPLAYER ARCHITECTURE READY / NOT TESTED`

실제 테스트 후에만:

`MULTIPLAYER BASIC SESSION VERIFIED`

실제 2인 환경을 사용할 수 없으면 이 항목은 계속 `NOT TESTED`로 남긴다.

---

# 10. P0-H — Nether/End Independence Validator

## 목표

개발 중 실수로 Nether/End 필수 recipe가 들어가는 것을 자동 탐지한다.

메인 progression graph에서 다음이 필수 ancestor가 되면 실패:

- Nether-only resource
- End-only resource
- Nether/End structure drop
- dimension-only advancement

선택 sidegrade / shortcut graph는 허용한다.

---

# 11. P0 종료 조건

P0 완료는 다음을 의미한다.

- 실제 26.2 프로젝트 build
- authoritative ShipState + persistence boundary
- B형 exterior movement
- Earth↔space transition 최소 구현
- stable linked interior
- representative turret manual/auto
- central power/ammo/sensor simulation
- Nether/End main-path independence
- dedicated server / custom dimension lifecycle 검증
- save/restart lifecycle 검증
- multiplayer 구조 검증
- 실제 멀티 테스트 가능 시 2인 기본 세션 검증

실제 멀티 환경이 없다면 마지막 항목은 `NOT TESTED`로 남기며 성공했다고 꾸미지 않는다.

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

> “내가 만든 작은 우주선으로 지구를 떠나 궤도에서 뭔가를 회수하고 살아 돌아왔다.”

이 경험이 실제로 재미있어야 다음 천체를 늘린다.

---

# 13. M2 — Moon Vertical Slice

- Moon local world
- landing/launch
- low gravity
- vacuum survival
- lunar resource
- 대표 discovery
- 작은 outpost 기능
- meaningful ship upgrade

전체 첫 사이클:

```text
Earth preparation
→ launch
→ orbit encounter
→ Moon
→ lunar exploration/resource
→ return
→ upgrade
```

이 사이클이 재미없으면 Mars를 추가하지 않는다.

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
- first significant ship frame expansion
- early automation

---

# 16. M5 — Mars / Expedition Ship

- Mars content pack
- longer survival loop
- thermal/environment system expansion
- onboard refinery
- medium expedition frame

---

# 17. M6 — Belt / Multi-System Scale

- Main Belt
- larger fleet encounters
- drone mining
- multi-turret fire control
- mobile-base gameplay
- worst-case multiplayer/performance profiling 강화

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

단, 작은 기술 커밋마다 사용자 테스트를 요구하지 않는다. 관련 기능을 의미 있는 lifecycle 단위로 묶어 테스트한다.

---

# 20. 바로 다음 구현 단위

**P0-G Lifecycle / Multiplayer Gate — persistence + automated lifecycle first**

바로 사용자 멀티테스트를 요구하지 않는다. 다음 의미 있는 작업 묶음에 우선 포함한다.

- versioned central systems persistence
- PowerGrid stored / AmmoPool amounts save-restore
- corrupt/unknown systems schema rejection
- ship/interior/systems save-restart integration boundary
- dedicated server custom-dimension boot/smoke 준비 및 실행 가능한 자동검증
- lease/reconnect lifecycle 강화
- project build/JAR gate

그 다음 실제 2인 환경이 가능할 때만 pilot + gunner + interior + Earth↔orbit를 한 번의 live multiplayer gate로 묶는다.
