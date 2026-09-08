# EARTH TO STARS — Technical Architecture

이 문서는 B형 모듈식 함선, 멀티플레이, 네트워크, 저장, 공간 전환, 성능 경계를 정의한다.

---

# 1. 기술 목표

EARTH TO STARS의 기술 구조는 다음을 동시에 만족해야 한다.

1. Minecraft 26.2 / NeoForge에서 유지 가능한 구조
2. 싱글과 멀티가 같은 authoritative runtime 사용
3. 함선이 커져도 tick 비용이 선형 폭증하지 않음
4. 외부 물리 라이브러리 하나에 프로젝트 전체가 인질 잡히지 않음
5. surface↔space 전환을 플레이어에게 연속적으로 보여줄 수 있음
6. 저장/재접속/서버 재시작에서 함선 상태가 일관됨
7. 콘텐츠 값과 규칙이 분리됨

---

# 2. 최상위 도메인

권장 feature 구조:

```text
ship/
  domain/
  runtime/
  data/
  persistence/
  networking/
  client/

space/
  travel/
  celestial/
  transition/
  world/

combat/
  weapons/
  targeting/
  projectiles/
  damage/

industry/
  power/
  production/
  logistics/

multiplayer/
  permission/
  crew/
```

Minecraft API adapter와 순수 도메인 로직을 가능한 한 분리한다.

---

# 3. ShipState

서버에는 함선마다 안정적인 `shipId`가 있다.

개념 모델:

```text
ShipState
├ shipId
├ owner / crew policy
├ shipClass / frame
├ transform
├ flightState
├ modules[]
├ hardpoints[]
├ powerState
├ fuelState
├ ammoState
├ sensorState
├ damageState
├ interiorRef
├ cargo
├ progressionTags
└ schemaVersion
```

클라이언트는 이 상태의 렌더/조작에 필요한 projection을 받는다.

---

# 4. ModuleDefinition vs ModuleInstance

콘텐츠 정의와 런타임 상태를 분리한다.

## ModuleDefinition

데이터 기반 불변 정책.

예:

```text
id
category
sizeClass
mass
powerUse
powerGeneration
heatProfile
capacity
hardpointCompatibility
visualKey
recipeKey
unlockRequirement
```

## ModuleInstance

실제 함선에 설치된 상태.

```text
instanceId
definitionId
slot/hardpoint
condition
runtimeState
customData
```

밸런스 값은 Java 곳곳에 하드코딩하지 않는다.

---

# 5. ShipSimulation

함선 모듈이 각자 독립적인 world tick machine이 되지 않게 한다.

```text
ShipSimulation
├ PowerGridSimulator
├ PropulsionSimulator
├ SensorGridSimulator
├ WeaponController
├ LogisticsSimulator
├ ThermalSimulator
├ DamageController
└ InteriorLinkController
```

주기별 계산을 분리한다.

예:

- flight critical: 매 tick 또는 필요한 주기
- power budget: 상태 변경 또는 낮은 주기
- sensor broad acquisition: 5~20 tick 간격, 상황별 조정
- LOS refinement: target 후보에만 수행
- production: recipe/event based
- UI detail sync: state delta 중심

실제 수치는 profiler로 정한다.

---

# 6. Multiplayer Authority

서버가 정본이다.

## Client → Server 입력 예

- throttle input
- steering input
- request weapon control
- aim direction
- request fire
- request target selection
- request module install/remove
- request interior/exterior transfer
- request docking

## Server 검증

예: `request fire`

```text
player is allowed?
→ weapon exists?
→ correct ship?
→ manual control ownership valid?
→ ammo/energy available?
→ cooldown ready?
→ heat allowed?
→ target/fire arc legal?
→ fire authoritative projectile/beam
→ consume resources
→ replicate result
```

클라이언트가 `damage=120`, `mined=4`, `travelComplete=true` 같은 결과를 제출하지 않는다.

---

# 7. Crew / Permission

초기 프리셋은 단순하게 유지한다.

## Owner
- 모든 권한

## Crew
- ship use
- cargo normal access
- weapon control
- module interaction
- interior access

## Guest
- interior access
- 제한적 seat/use

필요 시 기능별 override를 추가하지만, permission matrix 자체가 콘텐츠가 되지 않게 한다.

권한은 서버 저장 상태다.

---

# 8. Manual Turret Control

수동 조작 흐름:

```text
Player requests control
→ server validates permission + availability
→ server grants control token/session
→ client sends aim/input
→ server clamps legal yaw/pitch/arc
→ fire requests validated server-side
→ authoritative result replicated
```

동일 포탑을 동시에 두 사람이 조종하는 race condition을 막는다.

연결 끊김/사망/차원 전환 시 control lease를 회수한다.

---

# 9. Automatic Turret Control

자동 포탑은 각자 world scan을 하지 않는다.

```text
Ship SensorGrid
→ tracked contacts
→ threat classification
→ central target assignment
→ weapon-specific eligibility
→ fire decision
```

포탑별로 다른 요소만 계산한다.

- arc
- range
- turn speed
- ammo
- heat
- line of sight

Point Defense와 대형 주포는 같은 target priority를 쓰지 않는다.

---

# 10. SensorGrid

SensorGrid는 함선당 중앙 contact cache를 가진다.

Contact 후보 상태 예:

```text
UNKNOWN
DETECTED
TRACKED
IDENTIFIED
LOST
```

정확한 단계는 P0 이후 단순화할 수 있다.

센서의 역할:

- hostile entity/craft acquisition
- missile detection
- salvage/resource signature
- docking/navigation beacon

광범위 검사는 interval/cache/spatial query를 사용하고 per-tick full-world scan을 금지한다.

---

# 11. PowerGrid

모든 module이 독립적으로 발전기 목록을 찾지 않는다.

`PowerGridSimulator`가:

- generation
- storage
- demand
- priority
- shortage

를 계산한다.

전력 state change 시 dirty flag를 사용해 topology를 재계산할 수 있다.

Priority 기본값:

```text
CRITICAL: flight control / life support
HIGH: defensive systems
NORMAL: propulsion / sensors
LOW: industry / convenience
```

실제 게임 UX에서 너무 많은 priority 설정을 요구하지 않는다.

---

# 12. Ammo / Logistics

함선 내부 ammo/resource 이동은 논리 네트워크로 표현한다.

초기에는:

```text
Ship Cargo/Ammo Pool
→ compatible weapon consumption
```

으로 시작하고, 재미가 입증되면 zone/compartment 제한을 추가한다.

파이프 블록별 item simulation을 기본 요구사항으로 만들지 않는다.

---

# 13. Ship Exterior

B형 함선의 외부는 `ShipState`를 시각화하는 하나의 이동 객체/엔티티 계층으로 구현한다.

P0에서 요구하는 최소 기능:

- position
- rotation
- velocity
- acceleration input
- bounding/collision policy
- visible module attachment points
- weapon mount transforms
- multiplayer replication

최종 backend는 P0 결과에 따라 결정한다.

중요: 외부 물리 library를 사용하더라도 domain `ShipState`와 `ShipSimulation`이 library class에 직접 종속되지 않게 한다.

인터페이스 예:

```text
ShipMovementBackend
- spawnExterior
- applyControl
- getTransform
- teleport/transition
- destroyExterior
```

향후 backend 교체 가능성을 유지한다.

---

# 14. Ship Interior Instance

권장 구조:

```text
ShipState(shipId)
   ↕
InteriorInstance(shipId)
```

내부는 안정적인 Minecraft 공간에 존재하고, 외부 함선의 이동/회전과 직접 블록 좌표를 매 tick 변환하지 않는다.

## Interior가 동기화해야 할 것

- ship power state
- alarms
- damage state
- module rooms available
- cargo access
- control stations
- external camera/view projection
- docking/airlock state

## Interior가 반드시 외부와 1:1 물리 복제할 필요 없는 것

- 모든 외장 패널의 실제 블록 위치
- 외부 회전에 따른 내부 플레이어 위치 변환

이를 통해 multiplayer desync 위험을 줄인다.

---

# 15. Interior Lifecycle

항상 모든 interior chunk를 tick하지 않는다.

정책 후보:

- 플레이어가 있을 때 active
- production은 서버의 ShipSimulation으로 계산
- 비활성 interior는 visual/world tick 최소화
- 재접속 시 authoritative state를 재투영

실제 chunk ticket 정책은 P0/성능 측정 후 확정한다.

---

# 16. Earth ↔ Orbit Transition

플레이어 경험은 연속이어야 하지만 내부적으로 layer/dimension transition을 허용한다.

개념 흐름:

```text
Overworld ascent
→ transition envelope entered
→ server locks authoritative transfer
→ destination space layer prepared
→ ship/player state serialized in-memory transfer object
→ destination exterior created/moved
→ player control restored
→ client presentation hides abrupt loading where technically possible
```

전환 데이터:

- shipId
- passengers
- transform
- velocity
- fuel/power
- active control sessions
- target state reset policy

중복 생성/아이템 복제 방지를 위해 transfer는 서버에서 단일 transaction처럼 취급한다.

---

# 17. Celestial Coordinate Model

실제 천문학적 거리를 Minecraft 블록 1:1로 사용하지 않는다.

권장 계층:

```text
Surface local space
Orbital local space
Inter-body travel representation
Destination local space
```

각 레이어는 플레이 감각에 맞게 압축한다.

중요한 것은 숫자의 사실성이 아니라:

- 거리가 느껴짐
- 연료/항법 의미가 있음
- 이동이 지루하지 않음
- multiplayer 위치가 일관됨

이다.

---

# 18. Travel

여행은 장거리 빈 공간에서 W를 몇십 분 누르는 방식으로 만들지 않는다.

단계:

- local piloting
- departure burn / travel commitment
- compressed transit
- encounter/event 가능성
- destination approach
- local piloting

초반에는 단순화하고, 고급 drive가 생기면 여행 방식 자체가 개선되는 성장으로 연결한다.

---

# 19. Persistence

첫 플레이어블 alpha 이전부터 schema version을 둔다.

예:

```text
EarthToStarsWorldData
schemaVersion
ships
ownership
celestialProgress
stations/outposts
```

ShipState도 versioned serialization을 사용한다.

첫 플레이어블 이후:

- registry ID 변경 금지에 가깝게 취급
- save key 변경 시 migration
- 미래 schema 묵시적 수용 금지
- migration failure를 조용히 reset하지 않음

---

# 20. Disconnect / Restart

반드시 고려할 상황:

- 조종 중 disconnect
- turret control 중 disconnect
- interior에서 logout
- transition 도중 server stop
- ship owner offline
- crew만 접속
- projectile 존재 중 chunk unload

기본 원칙:

- control lease 회수
- authoritative ship state 보존
- 승무원 부재 때문에 함선 데이터 삭제 금지
- 불완전 transition은 명시적 recovery state로 처리

P0에서 핵심 상태 machine을 테스트한다.

---

# 21. Damage

초기 vertical slice는 지나치게 복잡한 voxel destruction을 피한다.

권장 단계:

### P0
- ship hull HP/integrity
- module damage

### 이후
- subsystem disabling
- localized hardpoint damage
- visible damage states

블록 하나씩 떨어져나가는 완전 구조 파괴는 핵심 재미 대비 비용을 검토한 뒤 결정한다.

---

# 22. Networking

원칙:

- authoritative snapshot + delta
- frequency 분리
- 렌더 전용 값은 client-side 계산 가능하면 동기화하지 않음
- large NBT spam 금지
- packet handler에서 world-wide search 금지

동기화 범주:

## High frequency
- controlled ship transform
- aiming state where required

## Medium
- power/fuel/combat state

## Low/event
- module config
- cargo changes
- progression
- permissions

실제 rate는 profile 후 결정한다.

---

# 23. Data-driven Content

데이터 후보:

- module definitions
- weapon profiles
- resource definitions
- celestial environment definitions
- progression unlocks
- recipes
- salvage tables
- encounter definitions

검증기에서 확인:

- unknown IDs
- invalid dependency
- impossible hardpoint
- negative/NaN values
- circular progression gates
- Nether/End mandatory dependency violation

---

# 24. Testing Strategy

## Pure domain
JUnit

- module compatibility
- power allocation
- target priority
- permission rules
- progression graph
- save migration

## Minecraft integration
GameTest

- module install/remove
- server-authoritative fire
- resource transfer
- space transition state
- save/reload

## Dedicated server

- mod load
- registry/data
- multiplayer packet safety
- reconnect flows where automatable

## Client/manual

- control feel
- transition visuals
- HUD readability
- interior/exterior consistency

실제 2+ player session이 실행되기 전까지 multiplayer는 `NOT TESTED`다.

---

# 25. Performance Worst Case

최종적으로 아래 같은 장면을 목표 부하 케이스로 삼는다.

- 여러 명이 탑승한 대형 함선
- 함선 3~4척 근접
- 각 함선 다수 turret
- 미사일/소형기/드론
- sensor contacts 다수
- production/cargo active

측정 항목:

- server MSPT
- entity count
- sensor query cost
- packet volume
- projectile cost
- interior chunk cost
- save serialization cost

추측으로 최적화 완료를 선언하지 않는다.

---

# 26. Dependency Policy

P0에서는 Minecraft/NeoForge 외 필수 dependency를 최소화한다.

외부 라이브러리 도입 전:

1. 26.2 실제 지원
2. 서버/클라이언트 양쪽 안정성
3. multiplayer
4. 유지보수
5. license
6. 성능
7. 교체 가능성

을 확인한다.

VS Genesis/Create Cosmonautics 등은 설계/코드 연구 가치가 크지만 현재 프로젝트의 26.2 핵심 runtime dependency로 자동 채택하지 않는다.

---

# 27. Architecture Definition of Done

기술 기반이 준비됐다는 의미는:

- ShipState가 server-authoritative
- module data/runtime 분리
- movement backend abstraction
- interior link abstraction
- power/ammo/sensor centralized simulation
- weapon manual/auto common abstraction
- persistence versioning
- network direction/permission contracts
- unit/integration tests

이 존재하는 상태다.

클래스 몇 개 만들었다고 architecture complete로 표시하지 않는다.
