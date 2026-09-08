# EARTH TO STARS

Minecraft Java / NeoForge 26.2 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **상태: M1-D ORBITAL RECOVERY + FIRST CONTACT BACKEND VERIFIED / SHIPSTATE SCHEMA 1→2 LIFECYCLE VERIFIED / FULL EARTH→ORBIT→RETURN LIVE ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED**

## 한 줄 설명

오버월드를 지구로 두고 바닐라 생존에서 시작해 산업·궤도·달·소행성·행성·심우주로 실제 플레이 공간을 확장하면서, 하나의 B형 모듈식 함선을 이동수단 → 집 → 공장 → 전함으로 성장시키는 대형 Minecraft 게임.

## 반드시 읽는 순서

1. 저장소 `/AGENTS.md`
2. 저장소 `/docs/BUILD_STANDARD.md`
3. 저장소 `/docs/QUALITY_STANDARD.md`
4. 이 프로젝트 `PROJECT.md`
5. 이 프로젝트 `AGENTS.md`
6. `docs/00_MASTER_GAME_DESIGN.md`
7. `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md`
8. 작업 분야별 세부 문서
9. `THIRD_PARTY_ASSETS.md`
10. 실제 source/resource와 최신 playtest 기록

기억이나 이전 대화가 현재 GitHub 문서와 충돌하면 GitHub `main`이 우선이다.

## 정본 문서

- `PROJECT.md` — 환경, 제품 결정, 멀티 권한 계약, 현재 검증 기준
- `AGENTS.md` — 프로젝트 전용 작업 계약
- `docs/00_MASTER_GAME_DESIGN.md` — 게임 정체성, 핵심 루프, 함선 성장, 전투, 탐험, 경제, Nether/End 정책
- `docs/01_TECHNICAL_ARCHITECTURE.md` — B형 모듈식 함선, 서버 권한, linked interior, 네트워크, 저장, 성능 구조
- `docs/02_WORLD_PROGRESSION_CONTENT.md` — 지구→궤도→달→소행성→행성→심우주 진행과 자원 역할
- `docs/03_UI_ART_REFERENCE_GATE.md` — SF UI/모델/VFX/사운드의 외부 레퍼런스 기반 제작 게이트
- `docs/04_P0_VERTICAL_SLICE.md` — P0 기술 게이트
- `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md` — Earth preparation → first orbit → return 첫 게임 루프 정본
- `src/main/resources/data/earth_to_stars/progression/main_path.json` — CI가 검사하는 메인 진행 그래프
- `THIRD_PARTY_ASSETS.md` — 외부 코드/자산/레퍼런스 출처·라이선스
- `CHANGELOG.md` — 실제 변경·검증 기록

## 프로젝트 핵심 기둥

```text
Minecraft 생존
+ 직접 이동하는 우주 탐사
+ 성장하는 모듈식 함선
+ 협동 가능한 함선 운용/전투
```

새 기능은 최소 하나의 기둥을 강화하고 가능하면 둘 이상을 연결해야 한다.

## 잠긴 제품 결정

- Overworld = Earth.
- Nether / End는 메인 진행 필수가 아니다.
- 함선은 자유 moving-block contraption이 아니라 **B형 authoritative modular ship**이다.
- important state는 server-authoritative다.
- manual / auto turret은 별개 무기가 아니라 같은 weapon state를 공유한다.
- solo는 automation으로 가능하고 multiplayer 역할은 강제 노동이 아니다.
- Power / Ammo / Sensor / Propellant / Oxygen은 함선당 중앙 simulation을 사용한다.
- final ship/UI/weapon/space visual은 외부 reference gate를 통과해야 하며 ArmorStand/vanilla proxy를 production으로 남기지 않는다.

---

# 현재 구현 — 0.1.0-alpha.11

## M1-A/B — Earth Preparation + First Launch Craft

첫 지구 progression은 새 광석을 무작정 늘리지 않고 Iron / Copper / Redstone / Gold / Amethyst / Gunpowder / Paper / Water / Leather를 우주비행 재료로 다시 사용한다.

현재 survival chain:

```text
vanilla Earth resources
→ reinforced_frame / avionics_unit / propellant_cell / oxygen_cartridge
→ life_support_unit
→ launch_craft_kit
→ Earth 배치
→ authoritative ShipState
```

Player-facing item:

- `reinforced_frame`
- `avionics_unit`
- `propellant_cell`
- `oxygen_cartridge`
- `life_support_unit`
- `launch_craft_kit`
- `recovered_sensor_core` — M1-D 첫 귀환 upgrade reward

Starter craft 기본 설치:

- `command_core_mk1`
- `engine_mk1`
- `battery_mk1`
- `cargo_mk1`
- `life_support_mk1`

의도적으로 빈 슬롯:

- `turret`
- `sensor`

첫 우주 원정에서 이 두 빈 capability를 실제로 채운다.

## M1-C — Launch Readiness + Atmosphere

Central starter reserve:

- Power `80 / 100`
- Propellant `80 / 240`
- Oxygen `80 / 240`

보급:

- propellant cell → 최대 `+40` Propellant
- oxygen cartridge → 최대 `+40` Oxygen
- 성공했을 때만 item 소비

Power + Propellant는 authoritative propulsion transaction으로 같이 검사/소비한다.

현재 atmosphere bands:

```text
Dense : Y < 256
Thin  : 256 ≤ Y < 384
Upper : 384 ≤ Y < 512
Earth→Orbit : Y = 512
Earth re-entry : Y = 504
```

Earth→Orbit 진입은 `life_support_mk1`, Propellant ≥ 8, Oxygen ≥ 20을 요구한다. 부족하면 경계를 넘지 못하고 pilot에게 현재 준비 상태를 알린다.

## M1-D — Orbital Recovery + First Contact

첫 Earth Orbit의 진행은 다음처럼 연결된다.

```text
Orbit 진입
→ 첫 표류 잔해 발견
→ 함선으로 접근
→ autocannon_mk1 실제 hardpoint 설치
→ AUTO_DEFENSE 기동
→ first unmanned interceptor contact
→ 중앙 SensorGrid가 logical hostile 추적
→ turret projectile 서버 판정
→ hostile 격파
→ recovered_sensor_core 회수
→ Earth 귀환
→ orbital_scanner_mk1 설치
→ sensor range 64 → 96
```

### 첫 salvage

- 함선별 server-owned encounter 하나.
- 현재 visible salvage는 ArmorStand 기술 proxy다.
- 접근 radius에 들어오면 server가 실제 `turret` slot을 검사한다.
- 성공하면 `autocannon_mk1`을 ShipState에 설치하고 즉시 저장한다.
- 실제 autocannon module이 없으면 turret command/runtime도 무장을 가짜 생성하지 않는다.
- 첫 회수 함포는 `AUTO_DEFENSE`로 켜지며 이후 동일 runtime에서 MANUAL 전환 가능하다.

### first contact

- 미확인 무인 요격기의 position / health / movement / attack은 서버가 결정한다.
- 일정 거리를 유지하고 측면 이동하며, 가까워지면 함선 중앙 PowerGrid를 공격한다.
- logical hostile contact는 함선 중앙 SensorGrid에 합쳐진다.
- 각 turret가 별도로 월드를 broad scan하지 않는다.
- logical projectile hit도 server-side encounter health에 적용된다.

### 첫 Earth-return upgrade

- 첫 hostile 격파 시 `recovered_sensor_core` 획득.
- 지구에서 접근 가능한 함선에 사용해야 한다.
- 성공 시 `orbital_scanner_mk1`이 빈 `sensor` slot에 설치된다.
- 센서 반경은 기본 64에서 96으로 증가한다.
- 성공하지 않으면 core를 소비하지 않는다.

### anti-soft-lock

같은 Orbit session에서 첫 적을 잡은 뒤에는 즉시 다시 생성되지 않는다. 다만 core를 놓치거나 잃었는데 scanner도 아직 없다면 progression이 영구 막혀서는 안 된다. 그래서 함선이 Earth로 돌아오면 session-only clear를 해제하고 다음 Orbit 진입에서 first-contact를 다시 수행할 수 있다.

### technical cockpit tether

현재 production cockpit이 없기 때문에 pilot이 움직이는 exterior에서 멀어져 server control range 밖으로 나가지 않도록 controlling player를 authoritative exterior와 함께 이동시키는 기술용 tether를 사용한다. 이건 final 탑승/카메라 시스템이 아니다.

---

# 저장 호환성

Persisted:

- ShipId / owner / crew / slots / modules
- ShipId → interior assignment
- Power / Ammo / Propellant / Oxygen

Not persisted:

- SensorGrid contact cache
- pilot/turret control lease
- logical projectile
- temporary exterior entity id
- M1 encounter proxy entity

alpha.11에서 ShipState schema가 `1 → 2`로 올라갔다. schema 1 함선은 decode 시 기존 ShipId/owner/crew/slots/modules를 유지하면서 빠진 `sensor` UTILITY slot만 추가한다. 지원하지 않는 future schema는 reset하지 않고 거부한다.

---

# 실제 progression 보호

- `tools/validate_progression.py` — 전체 main progression에서 Nether/End 없는 경로 보장
- `tools/validate_m1_launch.py` — 실제 `launch_craft_kit` recipe dependency closure에서 Nether/End 강제 회귀 차단

즉 기획 그래프와 실제 제작식이 따로 놀지 않도록 둘 다 검사한다.

---

# 최신 검증 기준

검증 기준 source commit: `afe0d181667582877e911ef279a869c7e31d0c23`

GitHub Actions `Build earth-to-stars` run `34197931566`: **PASS**

- P0-H progression validator: PASS
- M1 recipe dependency closure: PASS
- M1 launch Nether/End independence: PASS
- starter empty turret/sensor slots JUnit: PASS
- recovered autocannon/scanner progression JUnit: PASS
- sensor range 64→96 JUnit: PASS
- ShipState schema 1→2 migration JUnit: PASS
- existing regression JUnit: PASS
- `clean test build`: PASS
- Minecraft 26.2 / NeoForge 26.2.0.38-beta compile: PASS
- production JAR verify: PASS
- orbital mission/recovered-sensor resources packaged: PASS
- dedicated server first boot/save/shutdown: PASS
- same-world second boot/restore: PASS
- JAR: `earth_to_stars-0.1.0-alpha.11.jar`
- SHA-256: `f818686c7dde57e7a33e27967069c7e2074ce6b97febd0b13721e1165ac37fb0`

ShipState schema 변경 위험 때문에 alpha.11에서는 dedicated two-boot lifecycle을 의도적으로 한 번 다시 실행했다. 성공 후 workflow는 다시 explicit `workflow_dispatch`에서만 비싼 lifecycle 검사를 수행한다.

---

# 기술 프록시 경계

현재 다음은 final 품질이 아니다.

- ArmorStand ship exterior
- ArmorStand orbital salvage/interceptor
- vanilla texture item proxy
- technical cockpit tether
- 기술용 `ship_interiors` room
- 현재 `orbital_space` presentation
- 임시 use-on-block 보급/upgrade UX
- command 기반 일부 조작면
- logical projectile visual

Production 함선/cockpit/interior/turret/hostile/salvage/UI/VFX/sound/space visual은 `docs/03_UI_ART_REFERENCE_GATE.md`를 통과한 뒤 제작한다.

---

# 다음 작업 — M1 Integrated Live Acceptance

새 행성 기능을 더 늘리기 전에 첫 폐쇄 루프를 실제 Minecraft에서 검증한다.

```text
Earth 자원 준비
→ launch craft 제작/배치
→ fuel/oxygen 보급
→ 직접 상승
→ Earth Orbit
→ salvage 접근
→ autocannon 회수
→ first interceptor
→ reward 회수
→ Earth 재진입
→ orbital scanner 설치
→ sensor range 증가
```

검수 항목은 단순 기능 존재 여부가 아니라 조종감, progression 막힘, 안내 가독성, salvage 발견감, 전투 시간, reward 회수, 귀환, 카메라/시각 문제다.

자동 build나 dedicated server lifecycle 성공을 실제 플레이 또는 multiplayer 성공으로 간주하지 않는다.

Live multiplayer는 여전히 `NOT TESTED`다.
