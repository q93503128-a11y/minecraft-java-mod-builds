# EARTH TO STARS — P0 & Vertical Slice Plan

이 문서는 “기획은 좋지만 Minecraft 26.2에서 핵심 기술이 안 된다”는 실패를 최대한 빨리 발견하기 위한 기술 검증 순서와 첫 플레이어블 수직 구간을 정의한다.

P0는 콘텐츠를 많이 만드는 단계가 아니다. 핵심 위험을 작은 실제 구현으로 닫는 단계다.

---

# 1. 현재 상태

`M0 CANON LOCKED / BUILD BOOTSTRAP NEXT`

현재 확정된 것:

- 게임 정체성
- Overworld = Earth
- Nether/End optional progression
- B형 모듈식 함선
- server-authoritative multiplayer architecture
- manual/auto weapon abstraction
- linked interior instance 방향
- central power/ammo/sensor simulation
- 첫 vertical slice 범위

현재 아직 검증되지 않은 것:

- 26.2 실제 build scaffold
- 실제 space dimension/layer transition
- 실제 ship exterior movement
- 실제 multiplayer 2+ player session
- 실제 turret control
- 실제 interior link
- 실제 visual quality

이들을 완료했다고 표현하지 않는다.

---

# 2. M0 — Build Bootstrap

## 목표

저장소 표준에 맞는 실제 NeoForge 26.2 프로젝트를 만든다.

## 구현

- Gradle 9.2.1 wrapper
- Java 25 toolchain
- NeoForge 26.2.0.38-beta
- ModDevGradle 2.0.143 기준 재검증
- mod metadata
- minimal mod entrypoint
- resources namespace
- unit test setup
- datagen run config
- server/client run configs
- project-specific GitHub Actions workflow
- JAR verifier/report tooling baseline

## Acceptance

- dependency resolution 성공
- clean build 성공
- compiled `.class` 포함 JAR
- NeoForge metadata 포함
- assets/data namespace 존재
- unit test task 실행
- 가능한 server smoke
- 가능한 client smoke

실행하지 못한 항목은 `NOT RUN/BLOCKED`로 기록한다.

---

# 3. P0-A — Authoritative Ship Kernel

## 목표

Minecraft 렌더링 없이도 함선 게임의 서버 정본을 만들 수 있는지 검증한다.

## 구현

- `ShipId`
- `ShipState`
- `ModuleDefinition`
- `ModuleInstance`
- hardpoint/slot compatibility
- owner/crew/guest permission
- versioned persistence
- basic ShipRepository/manager

## 최소 모듈

- command core
- engine
- battery/generator
- cargo
- turret hardpoint

## 테스트

JUnit:
- valid/invalid install
- module removal
- ownership permission
- duplicate instance rejection
- serialization round trip
- schema version validation

GameTest/integration:
- server creates ship
- state persists
- reload restores same shipId/modules

## 실패 조건

- 중요한 상태가 client-only
- Minecraft entity가 없으면 domain test가 불가능할 정도로 강결합
- save reset으로 오류를 숨김

---

# 4. P0-B — Ship Exterior / Movement Backend

## 목표

B형 함선을 실제 Minecraft에서 움직일 수 있는 최소 외부 표현을 만든다.

## 구현

- simple exterior entity/object
- transform
- forward/right/up orientation
- throttle
- yaw/pitch control
- acceleration/deceleration
- mount/passenger or control-station link
- server-authoritative transform
- client interpolation

## 비목표

- 완전 자유 블록 물리
- 최종 ship model
- 최종 flight model
- 궤도역학 완성

## Acceptance

- 싱글에서 조종 가능
- dedicated server 구조에서 transform authority가 서버에 있음
- reconnect 후 잘못된 control lease가 남지 않음
- client가 임의 위치를 authoritative하게 확정할 수 없음

## 체감 검사

- 입력 지연
- 멀미 유발 회전
- 카메라
- 가속/감속

DEBUG_ONLY 외형을 final visual로 간주하지 않는다.

---

# 5. P0-C — Earth → Orbital Space Transition

## 목표

지상에서 올라가 우주 공간으로 이동하는 핵심 판타지가 실제로 성립하는지 검증한다.

## 최소 흐름

```text
Overworld test craft
→ altitude transition envelope
→ server transfer
→ orbital space
→ 같은 shipId / passengers 유지
→ control recovery
```

## 서버 검증

- 중복 Ship 생성 없음
- inventory/resource 복제 없음
- 승객 누락 없음
- velocity/rotation 정책 일관
- transition 중 disconnect 처리
- transition 실패 시 recovery

## 클라이언트 검수

첫 단계에서는 최종 연출이 아니어도 된다.

하지만 확인:
- 불필요한 메뉴 클릭 없음
- 갑작스러운 임의 teleport 느낌 최소화 가능성
- camera/control 회복
- 지구/우주 presentation backend 확장 가능성

## 최종 비전용 후속 항목

- sky darkening
- atmosphere thinning
- star reveal
- Earth horizon
- sound transition
- re-entry

P0에서는 기술 가능성을 먼저 닫는다.

---

# 6. P0-D — Linked Ship Interior

## 목표

외부 함선이 움직여도 여러 플레이어가 안정적인 내부 공간을 사용할 수 있는지 검증한다.

## 구현

- `InteriorRef(shipId)`
- test interior instance
- exterior → interior entry
- interior → exterior/seat return
- server permission check
- power/alarm state projection

## 테스트

- owner enters/leaves
- crew enters/leaves
- guest permission
- two players can coexist structurally
- ship moves while interior stays valid
- server restart restores link
- destroyed/unavailable ship does not orphan players permanently

## P0 비목표

- 최종 bridge
- 최종 windows
- 모든 module별 방
- 완전한 moving exterior view

---

# 7. P0-E — Representative Turret

## 목표

한 무기 시스템에서 manual과 automatic control이 멀티 안전하게 공존하는지 검증한다.

## 대표 무기

`Test Autocannon` — DEBUG_ONLY

선정 이유:
- projectile
- ammo
- rotation
- target
- fire rate
- manual/auto

를 한 번에 검증할 수 있음.

## Control Modes

- OFF
- MANUAL
- AUTO_DEFENSE

ASSISTED/AUTO_TARGET는 후속.

## Manual Flow

```text
player requests control
→ server grants lease
→ client aim input
→ server validates arc/ammo/cooldown
→ authoritative fire
```

## Auto Flow

```text
SensorGrid contact
→ threat filter
→ turret eligibility
→ server aim/fire
```

## 테스트

- two players cannot own same control lease
- disconnect releases lease
- no ammo = no shot
- insufficient power if power coupling active = no shot
- invalid arc = no shot
- friendly/unauthorized target policy
- auto does not fire at invalid contacts
- manual can override auto according to explicit state transition

## Performance

SensorGrid 한 번의 contact acquisition을 여러 weapon이 공유하는 구조를 검증한다.

---

# 8. P0-F — Central Ship Systems

## 목표

모듈이 많아질 때도 계산 구조가 확장 가능한지 검증한다.

## Power

- generation
- storage
- demand
- priority shortage

## Ammo

- central compatible pool
- server consumption

## Sensor

- cached contacts
- interval acquisition
- per-weapon eligibility

## 최소 부하 테스트

인공적으로 다수 module/turret definition을 붙여 계산 비용을 확인한다.

P0 수치는 최종 performance guarantee가 아니라 구조 검증이다.

---

# 9. P0-G — Multiplayer Session Gate

구조만 멀티 친화적이라고 끝내지 않는다.

실제 가능한 환경이 마련되면 최소 2인 검증을 한다.

## Scenario A — Shared Ship

- P1 owner
- P2 crew
- 동시에 탑승
- P1 조종
- P2 turret manual control

정상 결과:
- position authoritative sync
- P2 fire visible to P1
- ammo one authoritative count
- no duplicate projectile/damage

## Scenario B — Control Conflict

- 두 플레이어가 같은 turret control 요청

정상:
- 하나만 lease 획득
- 명확한 feedback

## Scenario C — Disconnect

- pilot disconnect
- gunner disconnect

정상:
- server state survives
- lease cleaned
- ship data remains

## Scenario D — Interior

- 한 명 exterior control
- 한 명 interior

정상:
- 두 상태가 같은 ShipState를 봄
- power/damage/cargo 불일치 없음

## 상태 표기

실제 이 검증 전:

`MULTIPLAYER ARCHITECTURE READY / NOT TESTED`

실제 테스트 후에만:

`MULTIPLAYER BASIC SESSION VERIFIED`

---

# 10. P0-H — Nether/End Independence Validator

## 목표

개발 중 실수로 Nether/End 필수 recipe가 들어가는 것을 자동 탐지한다.

## 검사

메인 progression graph에서:

- Nether-only resource
- End-only resource
- Nether/End structure drop
- dimension-only advancement

가 필수 ancestor로 존재하는지 확인한다.

선택 sidegrade graph는 허용한다.

---

# 11. P0 종료 조건

P0 완료는 다음을 의미한다.

- 실제 26.2 프로젝트 빌드
- authoritative ShipState 저장
- B형 exterior 이동
- space transition 최소 구현
- linked interior 최소 구현
- representative turret manual/auto
- central system simulation
- dedicated server boundary 검증
- Nether/End main-path independence
- multiplayer 구조 검증
- 실제 멀티 테스트 가능 시 2인 기본 세션 검증

실제 멀티 환경이 없다면 마지막 항목은 `NOT TESTED`로 남기되 구조적 blocker가 없는지 검사한다.

---

# 12. M1 — Earth/Orbit Gameplay Slice

P0가 닫히면 첫 실제 게임성을 만든다.

## 범위

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

## 종료 경험

플레이어가 처음으로:

> “내가 만든 작은 우주선으로 지구를 떠나 궤도에서 뭔가를 회수하고 살아 돌아왔다.”

를 경험해야 한다.

---

# 13. M2 — Moon Vertical Slice

## 범위

- Moon local world
- landing/launch
- low gravity
- vacuum survival
- lunar resource
- 대표 discovery
- 작은 outpost 기능
- meaningful ship upgrade

## 최종 첫 전체 사이클

```text
Earth preparation
→ launch
→ orbit encounter
→ Moon
→ lunar exploration/resource
→ return
→ upgrade
```

이 사이클이 실제 플레이에서 재미없으면 Mars를 추가하지 않는다.

---

# 14. M3 — Production Visual Gate

기능 프록시를 실제 제품 비주얼로 교체한다.

- ship reference board
- launch craft model
- cockpit/HUD mockup
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

앞 단계의 gameplay, content pipeline, performance, multiplayer가 검증된 경우에만 확장한다.

새 천체 수로 규모를 과장하지 않는다.

---

# 19. 사용자 직접 테스트 전달 규칙

실제 JAR 테스트 단계에서는 사용자에게 결과만 말하지 않는다.

항상 함께 제공할 것:

- 테스트 JAR
- 필요한 `/give`
- 필요한 `/summon`
- 필요한 `/tp` 또는 DEBUG_ONLY test command
- 테스트 순서
- 정상 결과
- 이상 증상 체크리스트

예를 들어 함선 테스트라면 한 명령으로 P0 craft/필요 자원을 받을 수 있는 DEBUG_ONLY 테스트 도구를 준비한다.

---

# 20. 바로 다음 구현 단위

문서 등록 이후 첫 코딩 묶음은:

**M0 Build Bootstrap + P0-A Authoritative Ship Kernel**

이다.

몇 줄씩 나눠 멈추지 않고 다음이 하나의 의미 있는 커밋/작업 묶음이 된다.

- build scaffold
- mod entrypoint
- module data contract
- ShipState
- permission baseline
- persistence baseline
- JUnit
- minimal CI

그 다음 P0-B movement로 넘어간다.
