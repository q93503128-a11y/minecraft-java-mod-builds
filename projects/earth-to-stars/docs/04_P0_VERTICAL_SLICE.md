# EARTH TO STARS — P0 & Vertical Slice Plan

이 문서는 Minecraft 26.2에서 핵심 기술 위험을 먼저 제거하고, 그 뒤 첫 실제 게임 루프로 넘어가기 위한 개발 순서를 정의한다.

P0는 콘텐츠를 많이 만드는 단계가 아니다. 빌드 성공, dedicated-server 생명주기 검증, 실제 플레이, 실제 멀티를 서로 구분한다. 작은 커밋마다 사용자 테스트를 반복해서 요구하지 않는다.

---

# 1. 현재 상태

`P0 AUTOMATED TECHNICAL GATES COMPLETE / P0-G DEDICATED LIFECYCLE VERIFIED / P0-H NETHER-END INDEPENDENCE VERIFIED / LIVE ACCEPTANCE DEFERRED / LIVE MULTIPLAYER NOT TESTED / M1 EARTH-ORBIT GAMEPLAY SLICE NEXT`

최신 P0-H 자동 검증:

- implementation/CI commit: `8b3b4edda64505d476418e0b08fbe85baea6b0ba`
- Actions run: `34189697283`
- version: `0.1.0-alpha.8`
- JAR SHA-256: `3af179b7cdb16236722507434a000f38dcc82fc59079aab584e1f79771f2e688`

P0-G dedicated lifecycle 검증:

- commit: `557d273ecfa78c1ba9cc62956cd78f6eb7c55153`
- Actions run: `34188840459`
- dedicated server 2회 same-world boot: `PASS`
- ship/interior/power/ammo real disk restore: `PASS`

alpha.8에서는 persistence나 custom-dimension lifecycle을 건드리지 않았기 때문에 P0-G의 비싼 서버 2회 부팅을 다시 돌리지 않았다. 일반 push에서는 해당 단계가 `SKIPPED`되고 명시적 lifecycle 검증이 필요할 때만 `workflow_dispatch`로 실행한다.

---

# 2. P0에서 닫힌 기술축

## M0 — Build Bootstrap

- Minecraft 26.2
- NeoForge 26.2.0.38-beta
- Java 25
- Gradle 9.2.1
- ModDevGradle 2.0.143
- JUnit
- project-specific GitHub Actions
- production JAR verifier

상태: `BUILD VERIFIED`

## P0-A — Authoritative Ship Kernel

- stable `ShipId`
- `ShipState`
- owner / crew / guest permission
- module slot / hardpoint compatibility
- `ShipRepository`
- versioned `ShipStateCodec`
- server-global `ShipSavedData`
- save corruption/key mismatch rejection

상태: `AUTOMATED + REAL DISK RESTORE VERIFIED`

## P0-B — Ship Exterior / Movement Backend

- B형 함선용 authoritative transform
- throttle / yaw / pitch
- acceleration/deceleration
- control lease
- session UUID / monotonic sequence
- stale/replay input rejection
- logout/dimension lease cleanup
- central PowerGrid propulsion coupling
- temporary ArmorStand exterior proxy

상태: `BACKEND BUILD VERIFIED / LIVE CONTROL FEEL NOT TESTED`

ArmorStand는 final ship visual이 아니다.

## P0-C — Earth ↔ Orbital Space Transition

```text
Overworld ascent
→ server transition transaction
→ orbital_space
→ same ShipState / ShipId
→ exterior replacement
→ control lease recovery
```

- Earth upward / orbit downward transition policy
- target level absence rejection
- transition rollback
- `earth_to_stars:orbital_space`

상태: `BACKEND BUILD VERIFIED / LIVE FLIGHT NOT TESTED`

## P0-D — Linked Ship Interior

- `InteriorRef(shipId, slot)`
- one stable `ship_interiors` dimension
- 2048-block isolated per-ship cells
- persistent `ShipId → slot`
- slot collision rejection
- interior→current exterior return boundary
- orphan recovery path
- same ShipId system lookup from interior

상태: `AUTOMATED + REAL DISK RESTORE VERIFIED / LIVE MULTIPLAYER INTERIOR NOT TESTED`

내부 승무원은 외부 함선 translation/rotation을 매 tick 따라가지 않는다. 같은 ShipId로 연결된 안정된 내부 좌표계에 남는다.

## P0-E — Representative Turret

Control modes:

- `OFF`
- `MANUAL`
- `AUTO_DEFENSE`

검증된 구조:

- `WEAPON_CONTROL` permission
- exclusive weapon lease
- session/replay validation
- legal firing arc
- cooldown
- central ammo/power consumption
- shared SensorGrid target selection
- neutral target rejection
- server-authoritative logical projectile/damage

상태: `BACKEND BUILD VERIFIED / LIVE COMBAT FEEL NOT TESTED`

현재 command 조작면과 논리 projectile은 final UX/visual이 아니다.

## P0-F — Central Ship Systems

```text
ShipId
 └─ ShipSystemsRuntime
     ├─ ShipPowerGrid
     ├─ ShipAmmoPool
     └─ ShipSensorGrid
```

- one authoritative power state per ship
- one authoritative ammo pool per ship
- one shared SensorGrid per ship
- ESSENTIAL / PROPULSION / WEAPONS / UTILITY reserve
- multiple turrets share ammo
- failed shot consumes neither ammo nor power
- staggered sensor acquisition
- stale contact expiry

상태: `BACKEND VERIFIED`

P0 tuning 수치는 final balance가 아니다. 콘텐츠 단계에서 data-driven 값으로 승격한다.

## P0-G — Lifecycle / Persistence

실제 dedicated server에서 확인:

1. clean first boot
2. custom dimensions registration
3. deterministic ShipId / modules / interior / systems seed
4. real SavedData write
5. clean shutdown
6. same world second boot
7. same ShipId/modules restore
8. same interior slot restore
9. same central power/ammo restore

검증값:

- ShipId: `11111111-2222-3333-4444-555555555555`
- interior slot: `0`
- power: `37.5`
- autocannon ammo: `73`

센서 contact, pilot/turret lease, projectile, exterior entity ID는 restart 후 재구축하는 휘발 상태다.

상태: `DEDICATED LIFECYCLE VERIFIED / LIVE MULTIPLAYER NOT TESTED`

## P0-H — Nether / End Independence Validator

정본 그래프:

`src/main/resources/data/earth_to_stars/progression/main_path.json`

검증기:

`tools/validate_progression.py`

self-test:

`tools/test_progression_validator.py`

검사 방식:

- 각 메인 마일스톤의 전체 선행 그래프를 탐색한다.
- `requires_any`를 통해 여러 대체 경로를 표현한다.
- 적어도 하나의 완전한 경로가 Nether/End 없이 존재하면 통과한다.
- Nether/End route 자체는 optional shortcut/sidegrade로 존재 가능하다.
- Nether/End가 유일한 필수 경로가 되는 순간 실패한다.
- unknown dependency / cycle도 실패한다.

현재 메인 진행:

```text
Earth Industry
→ Launch Craft
→ Earth Orbit
→ Orbital Salvage
→ Moon
→ Near-Earth Asteroids
→ Mars
→ Main Belt
→ Outer System
→ Deep Space
```

그래프에는 의도적으로 다음 optional node도 포함한다.

- `nether_heat_shortcut`
- `nether_propellant_variant`
- `end_navigation_sidegrade`

즉 validator는 Nether/End의 존재 자체를 막는 것이 아니라 **강제 진행만 막는다.**

상태: `VERIFIED`

---

# 3. P0가 의미하는 것과 의미하지 않는 것

P0의 자동 기술 위험 제거는 완료했다.

완료된 의미:

- 현재 26.2 환경에서 핵심 서버 구조가 컴파일/빌드된다.
- 함선 상태, 이동, 우주 전환, 내부, 포탑, 중앙 시스템을 연결할 기반이 있다.
- 저장/재시작이 실제 dedicated server에서 성립한다.
- 메인 진행이 Nether/End 강제로 회귀하지 않도록 자동 방어가 있다.

완료되지 않은 의미:

- 조종이 재미있다.
- 대기권 연출이 좋다.
- 우주가 아름답다.
- 함선 모델/UI가 production 품질이다.
- 수동/자동 포탑 타격감이 좋다.
- 실제 두 플레이어가 동시에 문제없이 운용한다.

따라서 상태는 `P0 AUTOMATED TECHNICAL GATES COMPLETE`이며 `PLAYTESTED` 또는 `MULTIPLAYER TESTED`가 아니다.

---

# 4. 아직 실제 검증되지 않은 항목

- client smoke
- 실제 Earth→orbit→Earth player flight
- ship control feel / camera / interpolation
- exterior↔interior 실제 출입
- 2인 이상 같은 interior 동시 체류
- pilot가 외부를 조종하는 동안 crew가 내부에 남는 상황
- 실제 manual turret aiming
- AUTO_DEFENSE 실제 전투
- projectile/tracer/impact visual
- pilot + gunner control conflict/disconnect 실제 멀티
- live multiplayer session
- production ship/interior/turret rendering/audio

실행하지 않은 항목은 PASS로 표현하지 않는다.

---

# 5. M1 — Earth / Orbit Gameplay Slice — NEXT

이제 기술검증을 더 옆으로 늘리지 않고 첫 실제 게임 루프를 만든다.

## 목표 경험

> **내가 지구에서 준비한 작은 우주선으로 직접 우주에 올라가, 궤도에서 처음으로 자원과 위험을 만나고 살아 돌아왔다.**

## M1 전체 흐름

```text
Earth survival
→ 초기 산업
→ launch craft 제작
→ fuel / oxygen 준비
→ 발사
→ 고도 상승 / atmosphere 변화
→ orbital_space 진입
→ contact scan
→ 첫 salvage
→ 첫 hostile drone/contact
→ manual 또는 AUTO_DEFENSE 함포
→ cargo 확보
→ Earth 재진입
→ 귀환
→ 첫 함선 개수조
```

## M1-A — Earth Preparation

구현 목표:

- 기존 Minecraft 자원을 최대한 재사용
- 초기 generator / battery / metal processing
- 최소 electronics
- fuel processing
- oxygen/life support
- launch craft 제작

금지:

- 초기부터 기계 20종 추가
- 의미 없는 중간재 수십 종
- Nether/End 강제
- 새 광석 숫자로 규모 과장

## M1-B — Launch Craft

P0 ShipState를 실제 플레이어 제작 결과와 연결한다.

- small starter frame
- command/cockpit module
- propulsion module
- power module
- cargo
- one weapon hardpoint
- life-support requirement

함선은 숫자 티어가 아니라 **모듈 선택으로 역할이 달라지는 구조**를 유지한다.

## M1-C — Atmospheric Ascent

최종 목표는 menu teleport가 아니다.

플레이어는 실제로 상승하며 다음 변화를 느껴야 한다.

- sky darkening
- atmosphere thinning
- stars reveal
- engine/audio 변화
- oxygen pressure
- transition masking

P0 dimension transfer는 내부 구현일 뿐, 플레이어에게는 한 번의 연속된 비행처럼 보여야 한다.

## M1-D — Earth Orbit

첫 SF 플레이 공간.

초기 콘텐츠는 적은 수를 고품질로 만든다.

- one salvage wreck/contact
- one debris field
- one hostile drone/contact
- Earth visual anchor
- vacuum environment

첫 궤도를 의미 없는 빈 검은 공간으로 남기지 않는다.

## M1-E — Salvage / Combat

첫 보상:

- Salvaged Electronics
- Precision Components
- Orbital Alloy Scrap
- limited Research Data

역할:

- assisted targeting
- better servo/control
- first orbital-grade module
- life-support/range improvement

전투는 기존 P0 turret logic을 production 조작/feedback으로 승격한다.

## M1-F — Return / Upgrade

첫 원정이 지구에 영향을 줘야 한다.

```text
orbital salvage
→ Earth return
→ module upgrade
→ 다음 원정이 더 쉬워지거나 새로운 선택이 열림
```

한 번의 우주비행이 단순 구경으로 끝나지 않는다.

---

# 6. M1 품질 게이트

M1 완료 조건:

- 지구에서 실제 플레이로 launch craft 준비 가능
- Nether/End 없이 진행 가능
- 직접 조종하여 우주 진입 가능
- 궤도에서 salvage 또는 combat 발생
- 보상을 실제로 회수
- 지구 귀환 가능
- 보상이 함선 성장에 연결
- 실패/연료/산소 상태가 이해 가능
- P0 임시 command 의존을 player-facing flow에서 제거 또는 최소화
- 핵심 UI/모델/VFX/sound가 reference gate를 통과
- 실제 Minecraft client에서 조작감/가독성 검수
- 실제 멀티 환경을 사용할 수 있다면 pilot/crew 기본 세션 검수

실제 멀티 환경이 없다면 `MULTIPLAYER NOT TESTED`를 유지한다.

---

# 7. M2 이후

## M2 — Moon Vertical Slice

```text
Earth preparation
→ orbit
→ Moon
→ lunar exploration/resource
→ return
→ upgrade
```

- Moon local world
- low gravity
- vacuum
- lunar resource
- first outpost
- meaningful ship upgrade

M1이 재미없으면 Moon 콘텐츠 수만 늘리지 않는다.

## M3 — Production Visual Gate

- ship reference board
- production launch craft model
- cockpit/HUD
- interior
- turret model/animation/VFX/sound
- Earth Orbit visuals
- Moon material/terrain reference

`03_UI_ART_REFERENCE_GATE.md`를 따른다.

## M4+ — Scale Out

- Near-Earth Asteroids
- mining beam
- cargo trade-offs
- first frame expansion
- Mars expedition
- Main Belt automation
- multi-turret fire control
- outer system / deep space

앞 단계가 gameplay/visual/performance/multiplayer 기준을 통과한 경우에만 확장한다.

---

# 8. 사용자 직접 테스트 전달 규칙

실제 JAR 테스트 단계에서는 항상 같이 제공한다.

- 테스트 JAR
- 필요한 `/give`, `/summon`, `/tp` 또는 기술검증 명령
- 테스트 전제 조건
- 재현 순서
- 정상 결과
- 비정상 증상 체크리스트

단, 작은 기술 커밋마다 테스트를 요구하지 않는다. **M1의 Earth→Orbit→Return 루프가 의미 있는 플레이 덩어리가 된 뒤 한 번에 테스트한다.**

---

# 9. 바로 다음 구현 단위

**M1-A + M1-B — Earth Preparation + First Launch Craft**

다음 작업은 기술 proof가 아니라 실제 플레이어 진행을 만든다.

우선 연결할 것:

- Earth 초기 제작/산업의 최소 세트
- fuel / oxygen
- launch craft 제작 조건
- 기존 `ShipState`와 실제 제작 결과 연결
- starter module loadout
- Nether/End independence graph와 실제 콘텐츠 데이터의 일치

그 다음 M1-C에서 atmosphere/ascent를 production 경험으로 승격한다.
