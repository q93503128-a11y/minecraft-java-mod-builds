# EARTH TO STARS — M1 Earth / Orbit Gameplay Slice

이 문서는 P0 기술검증 이후 첫 실제 플레이 루프인 M1의 정본이다.

목표는 기능을 늘리는 것이 아니라 다음 한 사이클을 실제 Minecraft 서바이벌에서 성립시키는 것이다.

```text
Earth 생존/채집
→ 초기 우주 산업 재료 제작
→ 소형 개척선 조립
→ 추진제/산소 준비
→ 직접 상승
→ 지구 궤도 진입
→ 첫 salvage / hostile contact
→ 회수
→ 지구 귀환
→ 첫 함선 개수조
```

종료 경험:

> 지구에서 모은 평범한 Minecraft 자원을 우주비행 장비로 바꾸고, 내가 만든 작은 개척선으로 직접 궤도에 올라가 처음으로 우주의 자원과 위험을 만나고 살아 돌아온다.

---

# 1. M1 진행 상태

현재 버전: `0.1.0-alpha.11`

현재 상태:

`M1-D ORBITAL RECOVERY + FIRST CONTACT BACKEND VERIFIED / SHIPSTATE SCHEMA 1→2 LIFECYCLE VERIFIED / FULL EARTH→ORBIT→RETURN LIVE ACCEPTANCE NEXT / LIVE MULTIPLAYER NOT TESTED`

완료된 자동/서버 기술축:

- 실제 Minecraft item registry 7종
- Earth-only crafting chain
- actual recipe dependency closure validator
- Nether/End-independent launch recipe contract
- player-facing launch craft assembly item
- Earth-only deployment / 3×3×3 clearance / duplicate-owned-ship rejection
- authoritative starter `ShipState` creation + persistence
- starter craft canonical module loadout
- server-authoritative propellant / oxygen reserves
- propellant cell / oxygen cartridge 실제 함선 보급 상호작용 backend
- power + propellant 원자적 추진 transaction
- atmosphere-band별 추진제 소비
- active crew 기반 산소 소비
- life-support + fuel + oxygen orbit-readiness gate
- insufficient readiness 시 Earth→Orbit transition 차단
- orbital salvage encounter backend
- salvage 접근으로 실제 `autocannon_mk1` module 회수/설치
- 무장 모듈 없이는 turret runtime을 생성하지 않는 capability gate
- 회수 기관포 `AUTO_DEFENSE` 자동 기동 + 동일 runtime에서 manual 전환 가능
- 첫 unmanned interceptor 논리 적대체 / 이동 / 거리 유지 / 전력 공격
- 중앙 SensorGrid에 mission hostile contact 공유
- turret logical projectile → first-contact hostile 서버 판정
- first-contact 격파 시 `recovered_sensor_core` 보상
- 지구 귀환 후 `orbital_scanner_mk1` 설치
- scanner 설치 후 기본 센서 range `64 → 96`
- 첫 hostile clear 이후 같은 원정에서 보상 무한 생성 방지
- sensor core를 놓치고 귀환한 경우 다음 궤도 진입에서 first-contact를 다시 회수 가능하게 하는 anti-soft-lock reset
- technical cockpit tether로 pilot/exterior control-authority drift 방지
- ShipState schema 1→2 sensor-slot migration
- Propellant/Oxygen SavedData persistence 및 구 save 기본값 migration
- dedicated server save → shutdown → restart lifecycle 재검증
- production JAR packaging

아직 실제 플레이 검증되지 않은 축:

- crafting book/client recipe usability
- item rendering/temporary icons in client
- in-world launch package deployment
- propellant / oxygen 실제 보급 조작감
- actual pilot controls/camera feel
- 실제 atmosphere ascent feel
- readiness warning readability
- live Earth→orbit→salvage→combat→Earth return cycle
- salvage 접근 거리/발견감
- interceptor 전투 난이도/사격감
- recovered sensor core pickup/install UX
- scanner range 변화가 실제 플레이에서 체감되는지
- multiplayer pilot + gunner + linked interior crew session

---

# 2. Earth 자원 설계

M1은 새 광석/새 재화를 무작정 늘리지 않는다.

우주 진입의 첫 단계는 기존 Minecraft 자원이 새로운 의미를 얻는 구조를 우선한다.

주요 재사용 자원:

- Iron — 구조/압력 용기
- Copper — 전도/배관/제어계
- Redstone — 전력/제어
- Gold — 정밀 전자계통
- Amethyst — 초기 감지/항법 정밀부품의 Minecraft식 재해석
- Gunpowder — 초기 고체 추진제
- Paper — 추진제 결합/패키징 재료
- Water — 초기 산소 생산 원료
- Leather — 초기 생명유지 밀폐/패킹 재료

새로운 지구 광석은 현재 M1에 추가하지 않는다.

Bauxite/Aluminum 같은 신규 지구 자원은 실제 플레이에서 구조재 progression이 빈약하다는 증상이 확인될 때만 다시 검토한다.

---

# 3. M1 신규 아이템

## 3.1 경량 강화 프레임

ID: `earth_to_stars:reinforced_frame`

역할:
- 함체 구조재
- 기존 철/구리를 우주선 구조로 변환하는 첫 제작 단계

현재 제작:
- iron ingot ×4
- copper ingot ×4
- output ×2

## 3.2 항법제어장치

ID: `earth_to_stars:avionics_unit`

역할:
- 비행 제어
- 항법
- 센서/전자계통의 초기 핵심 부품

현재 제작:
- copper ingot ×1
- redstone ×2
- amethyst shard ×1
- gold ingot ×1

## 3.3 고체 추진제 셀

ID: `earth_to_stars:propellant_cell`

역할:
- 첫 발사 능력을 만드는 지구 기반 추진 재료
- alpha.10부터 실제 함선 propellant reserve에 연결

현재 제작 1회 output ×2:
- iron ingot ×1
- gunpowder ×2
- paper ×1
- copper ingot ×1

보급 효과:
- 셀 1개당 `+40` propellant
- starter capacity `240`

## 3.4 압축 산소 카트리지

ID: `earth_to_stars:oxygen_cartridge`

역할:
- 첫 진공 생존 자원
- alpha.10부터 실제 함선 oxygen reserve에 연결

현재 제작 1회 output ×2:
- copper ingot ×1
- iron ingot ×2
- water bucket ×1
- redstone ×1

보급 효과:
- 카트리지 1개당 `+40` oxygen
- starter capacity `240`

이 제작식은 초기 생존 단계의 압축된 abstraction이다. M1/M2에서 산업 설비가 생기면 같은 재료 흐름을 산소 생산/압축 기계로 승격할 수 있다.

## 3.5 생명유지장치

ID: `earth_to_stars:life_support_unit`

역할:
- starter craft의 life-support module 제작 재료
- 산소를 단순 소모품으로만 두지 않고 함선 시스템에 연결

현재 제작:
- copper ingot ×4
- redstone ×1
- oxygen cartridge ×2
- iron ingot ×1
- leather ×1

## 3.6 소형 개척선 조립 패키지

ID: `earth_to_stars:launch_craft_kit`

역할:
- Earth preparation의 최종 제작물
- 아이템을 실제 authoritative ShipState로 전환하는 player-facing construction object

현재 제작:
- reinforced frame ×4
- avionics unit ×1
- life support unit ×1
- propellant cell ×2

이 아이템은 완성된 함선을 인벤토리에 보관하는 개념이 아니라, 지상에서 함선을 조립하기 위한 패키지다.

## 3.7 회수 센서 코어

ID: `earth_to_stars:recovered_sensor_core`

역할:
- 첫 orbital hostile을 격파해서 얻는 귀환 보상
- 지구로 살아 돌아온 뒤 함선의 빈 sensor slot을 실제 `orbital_scanner_mk1`로 변환
- 첫 원정이 단순 자원 획득이 아니라 **새 탐색 능력 해금**으로 끝나게 하는 progression object

현재 규칙:
- Orbit first-contact hostile 격파 시 생성
- 지구에서 접근 가능한 함선에만 설치 가능
- scanner가 이미 있거나 slot이 막혀 있으면 소비하지 않음
- 성공 시에만 item 소비
- 현재 spyglass 기반 item model은 임시 proxy이며 production icon이 아님

---

# 4. Launch Craft 배치 계약

플레이어가 `launch_craft_kit`을 지면에 사용하면 서버가 다음을 검증한다.

1. 현재 차원이 `minecraft:overworld`인가.
2. 클릭 위치 위에 3×3×3 조립 공간이 비어 있는가.
3. 해당 플레이어에게 이미 등록된 함선이 없는가.
4. authoritative ShipState를 만들 수 있는가.
5. exterior proxy를 월드에 생성할 수 있는가.
6. ShipSavedData에 기록할 수 있는가.
7. central ShipSystemsRuntime을 만들고 저장할 수 있는가.
8. pilot control lease를 서버가 발급할 수 있는가.

성공 시에만 survival inventory에서 조립 패키지 1개를 소비한다.

실패 시 패키지는 보존된다.

중요:

- client가 ShipId나 성공 결과를 결정하지 않는다.
- existing ship을 새 패키지가 덮어쓰지 않는다.
- 현재 ArmorStand exterior는 기술 프록시이며 production ship model이 아니다.

---

# 5. Starter Craft canonical loadout

첫 소형 개척선은 빈 껍데기가 아니다.

슬롯:

```text
core
engine
power
cargo
life_support
turret
sensor
```

기본 설치 모듈:

- `command_core_mk1`
- `engine_mk1`
- `battery_mk1`
- `cargo_mk1`
- `life_support_mk1`

의도적으로 비워 두는 슬롯:

- `turret`
- `sensor`

초기 central resources:

- Power: `80 / 100`
- Propellant: `80 / 240`
- Oxygen: `80 / 240`

첫 함선에 자동포탑이나 고급 센서를 처음부터 지급하지 않는다.

Earth Orbit 첫 salvage/contact가 `turret → sensor` 순서로 실제 capability를 채우기 때문에 **첫 우주 원정이 함선의 플레이 방식을 두 단계로 바꾼다.**

---

# 6. M1-C Launch Readiness + Atmosphere

## 6.1 자원 authority

Propellant와 oxygen은 인벤토리 숫자가 아니라 `ShipId`에 연결된 `ShipSystemsRuntime`의 서버 정본이다.

```text
ShipId
 └─ ShipSystemsRuntime
     ├─ PowerGrid
     ├─ AmmoPool
     ├─ SensorGrid
     ├─ PropellantTank
     └─ OxygenTank
```

추진 중에는 Power와 Propellant를 하나의 authoritative transaction으로 검사한다.

- 둘 다 충분하면 둘 다 소비한다.
- 어느 한쪽이 부족하면 둘 다 소비하지 않고 추진 입력을 적용하지 않는다.
- client는 연료 성공/실패를 결정하지 않는다.

Oxygen은 연속 reserve라서 남은 양보다 소비량이 큰 마지막 tick에서도 정확히 0까지 drain된다.

## 6.2 보급

현재 M1 기술 UX:

- `propellant_cell`을 접근 가능한 함선 근처에서 사용 → 함선 PropellantTank에 최대 +40
- `oxygen_cartridge`를 접근 가능한 함선 근처에서 사용 → 함선 OxygenTank에 최대 +40
- 탱크가 가득 찼거나 접근 가능한 함선이 없으면 아이템을 소비하지 않는다.

이 방식은 M1 gameplay 연결용이다.

Production에서는 함체 연료 포트/내부 보급 패널/적절한 애니메이션과 사운드로 승격할 수 있으며, 현재 임시 use-on-block interaction을 최종 UX로 고정하지 않는다.

## 6.3 대기권 구간

현재 M1-C gameplay bands:

```text
Dense Atmosphere : Y < 256
Thin Atmosphere  : 256 ≤ Y < 384
Upper Atmosphere : 384 ≤ Y < 512
Earth Exit       : Y = 512
Orbit             : orbital_space
```

기존 P0의 Y=300 transition은 기술 proof였고 production 방향이 아니다.

alpha.10에서는 산/고지대가 사실상 우주 입구처럼 느껴지지 않게 transition boundary를 `Y=512`로 올리고, 그 사이를 실제 자원 소비 구간으로 만든다.

Re-entry destination은 Earth `Y=504`다.

정확한 고도/속도/소비율은 실제 조종감 검증 후 조정 가능한 gameplay tuning 값이다.

## 6.4 추진제 소비

활성 조종 입력의 최대 축 크기에 비례한다.

현재 rate:

- Dense: `0.020 / tick × activity`
- Thin: `0.040 / tick × activity`
- Upper: `0.070 / tick × activity`
- Orbit: `0.015 / tick × activity`
- Other: `0.025 / tick × activity`
- Idle: `0`

따라서 상층 대기권 돌파가 지상 기동보다 비싸며, 궤도 내 기동은 대기권 돌파보다 효율적이다.

## 6.5 산소 소비

산소는 실제 active crew 수에 비례한다.

현재 active crew 정의:

- 현재 pilot lease controller
- 같은 ShipId의 linked interior에 실제 접속 중인 플레이어
- UUID 기준 중복 제거

현재 rate / crew:

- Dense: `0`
- Thin: `0.0025 / tick`
- Upper: `0.010 / tick`
- Orbit: `0.015 / tick`
- Other: `0.005 / tick`

빈 함선은 산소를 소비하지 않는다.

이 구조는 멀티에서 플레이어 수에 따른 실제 생명유지 비용을 서버가 결정하게 하지만, 각 플레이어가 개별 산소 게이지 여러 개를 관리하도록 만들지는 않는다.

## 6.6 Orbit readiness

Earth→Orbit 진입에는 다음 세 조건이 모두 필요하다.

- `life_support_mk1` 설치
- Propellant ≥ `8`
- Oxygen ≥ `20`

조건을 만족하지 못한 상태로 Y=512 경계를 넘으려 하면:

- dimension transition을 거부한다.
- ship transform을 Y=511 이하로 유지한다.
- 상승 velocity를 제거한다.
- pilot에게 현재/필요 Propellant, Oxygen, Life Support 상태를 알린다.
- 경고는 최대 100 tick에 한 번으로 제한한다.

관리 노동을 늘리는 복잡한 발사 체크리스트 메뉴는 만들지 않는다.

## 6.7 persistence / migration

Propellant와 Oxygen은 Power/Ammo와 함께 `ShipSystemsSavedData`에 저장한다.

alpha.10 이전 저장에는 두 필드가 없으므로 codec default를 starter initial reserve인 `80 / 80`으로 둔다.

따라서 기존 P0/alpha.9 save가 새 필드 부재 때문에 로드 불가 상태가 되거나 fuel=0/oxygen=0으로 갑자기 고립되지 않는다.

alpha.10 CI run `34192830690`에서 real dedicated server:

```text
first boot
→ propellant 51.25 / oxygen 66.5 저장
→ 정상 server shutdown
→ 동일 world directory 재부팅
→ same ShipId systems restore
→ propellant 51.25 / oxygen 66.5 복원
```

을 검증했다.

---

# 7. Nether / End 독립성

P0-H의 canonical progression graph뿐 아니라 실제 M1 launch recipe closure도 별도로 검사한다.

`tools/validate_m1_launch.py`는 `launch_craft_kit`에서 시작해 모든 EARTH TO STARS 중간 제작물을 재귀적으로 따라간다.

현재 검사 목적:

- launch craft recipe 누락 방지
- 중간 mod item recipe 누락 방지
- Nether/End 전용 자원이 실제 launch crafting chain에 들어오는 회귀 방지

Nether/End sidegrade는 이후 추가 가능하지만 첫 우주 진입의 유일한 제작 경로가 되어서는 안 된다.

---

# 8. 현재 visual boundary

alpha.11의 아이템 모델, ArmorStand exterior/salvage/interceptor, 기술 interior, orbital space, cockpit tether, command 조작면은 **기술/게임플레이 연결용 placeholder**다.

현재 vanilla texture proxy나 임시 텍스트를 production art/UX로 유지하지 않는다.

Production 전환 시 `03_UI_ART_REFERENCE_GATE.md`와 `THIRD_PARTY_ASSETS.md`를 따른다.

대상:

- launch craft item/assembly representation
- actual launch craft exterior
- cockpit
- thruster/engine
- life-support module
- fuel/oxygen ports/containers
- recovered sensor core / module icons
- orbital salvage visual
- interceptor model/animation
- turret model/muzzle/tracer/hit VFX
- item icons
- atmosphere/re-entry VFX
- launch VFX/sound
- readiness feedback

외부 reference/asset 검토 없이 AI 즉흥 SF 디자인으로 확정하지 않는다.

---

# 9. M1-D — Orbital Salvage + First Contact + Return Reward

alpha.11에서 M1-D backend가 구현·자동 검증됐다.

## 9.1 첫 salvage

첫 궤도 진입 후 scanner가 아직 없고 turret도 비어 있으면 함선 기준 전방/측면에 첫 salvage target을 생성한다.

현재 구조:
- 함선별 encounter 하나
- server-side authoritative encounter state
- ArmorStand는 위치/이름을 보여주는 기술 proxy일 뿐 reward truth가 아님
- 함선이 salvage recovery radius에 들어오면 서버가 실제 module install 가능성을 검증
- 성공 시 `autocannon_mk1`을 기존 빈 `turret` hardpoint에 설치
- ShipSavedData 즉시 갱신

## 9.2 첫 무장 해금

Starter craft는 autocannon을 소유하지 않으므로 M1-D 이전 기술 명령도 실제 module이 없으면 turret runtime을 만들 수 없다.

Salvage로 `autocannon_mk1`을 회수한 뒤에만:
- turret runtime 생성
- central AmmoPool 사용
- central PowerGrid 사용
- central SensorGrid 사용
- `AUTO_DEFENSE` 즉시 기동
- 이후 동일 포탑을 `MANUAL`로 바꿀 수 있음

수동/자동 무장을 서로 다른 총으로 분리하지 않는다.

## 9.3 첫 hostile contact

Autocannon 회수 후 `orbital_scanner_mk1`이 아직 없으면 미확인 무인 요격기 encounter가 열린다.

현재 logical behavior:
- 서버가 position / health / movement를 소유
- 함선에 접근한 뒤 일정 standoff distance 유지
- 너무 가까우면 후퇴
- 적정 거리에서는 측면 이동
- 공격 interval과 grace period 존재
- 공격 성공 시 함선 중앙 PowerGrid를 직접 drain
- client는 hit/damage 성공을 결정하지 않음

현재 hostile visual은 ArmorStand proxy이며 production 적 모델이 아니다.

## 9.4 SensorGrid / turret 연결

첫 hostile은 vanilla `Enemy`가 아니므로 mission manager가 `SensorContact`를 중앙 SensorGrid에 주입한다.

센서 스캔은 여전히 함선당 중앙 interval scan이며 turret마다 broad entity scan을 반복하지 않는다.

Auto turret은 같은 SensorGrid의 hostile contact를 잡아 logical projectile을 발사한다.

Projectile hit은 서버가 mission hostile position/health와 충돌 판정을 수행한다.

## 9.5 첫 귀환 보상

Hostile 격파 시 `earth_to_stars:recovered_sensor_core`가 회수 보상으로 생성된다.

플레이어는 지구로 귀환한 뒤 이 core를 함선에 적용한다.

성공 시:
- 기존 빈 `sensor` slot에 `orbital_scanner_mk1` 설치
- ShipSavedData 즉시 갱신
- base sensor range `64`에 multiplier `1.5`
- 결과 sensor range `96`

즉 첫 원정의 성장:

```text
무장 없음
→ Orbit salvage
→ autocannon 획득
→ first contact 생존/격파
→ sensor core 회수
→ Earth return
→ orbital scanner 장착
→ 탐색 반경 증가
```

첫 원정이 단순히 자원 수치 하나를 올리는 것이 아니라 실제 행동 범위를 바꾼다.

## 9.6 anti-soft-lock / reward duplication

같은 원정에서 첫 hostile을 격파한 뒤에는 `HOSTILE_CLEARED` session state로 즉시 재생성을 막는다.

하지만 recovered sensor core를 놓치거나 잃어버렸는데 그 상태가 영구 progression lock이 되면 안 된다.

따라서 scanner가 아직 설치되지 않은 함선이 Orbit을 떠나 Earth로 돌아오면 해당 session clear를 해제한다. 다음 Orbit 진입에서는 first-contact를 다시 수행해 core를 회수할 수 있다.

Scanner가 설치된 뒤에는 M1-D encounter가 종료된다.

## 9.7 technical cockpit tether

P0 flight backend에서 함선 exterior만 이동하면 server input authority의 64-block range를 벗어나 pilot input이 끊기는 문제가 있다.

alpha.11은 production cockpit이 생기기 전까지 controlling player를 authoritative exterior 위치에 유지하는 technical cockpit tether를 사용한다.

이것은 최종 탑승/카메라 설계가 아니며 실제 client feel 검증 후 교체 대상이다.

## 9.8 ShipState schema 1 → 2

alpha.10 이하 starter craft에는 `sensor` slot 자체가 없다.

alpha.11은 ShipState schema를 `2`로 올리고 schema 1 decode 시:
- 기존 ShipId 유지
- owner/crew 유지
- 기존 slots/modules 유지
- `sensor` UTILITY size-1 slot만 추가

한다.

future unknown schema는 여전히 거부하며 reset하지 않는다.

---

# 10. Verification — alpha.11

검증 기준 source commit: `afe0d181667582877e911ef279a869c7e31d0c23`

GitHub Actions `Build earth-to-stars` run `34197931566`:

- P0-H progression validator: `PASS`
- M1 actual launch recipe closure: `PASS`
- M1 Nether/End launch independence: `PASS`
- starter empty turret/sensor slots JUnit: `PASS`
- recovered autocannon/scanner progression JUnit: `PASS`
- sensor range `64 → 96` JUnit: `PASS`
- ShipState schema 1→2 sensor-slot migration JUnit: `PASS`
- existing P0/M1 regression JUnit: `PASS`
- `clean test build`: `PASS`
- Minecraft 26.2 / NeoForge 26.2.0.38-beta compile: `PASS`
- production JAR verify: `PASS`
- orbital mission/recovered sensor resources packaged: `PASS`
- dedicated server first boot/save: `PASS`
- dedicated server clean shutdown: `PASS`
- same-world second boot/restore: `PASS`

JAR SHA-256:

`f818686c7dde57e7a33e27967069c7e2074ce6b97febd0b13721e1165ac37fb0`

Still NOT TESTED:

- live survival crafting/use flow
- actual atmosphere ascent feel
- actual readiness boundary/player feedback
- live Earth→Orbit transition
- salvage discovery/approach feel
- auto/manual turret actual game feel
- interceptor combat difficulty/visual readability
- recovered sensor core pickup/install UX
- actual Earth return
- full Earth→Orbit→salvage→combat→Earth→scanner upgrade acceptance
- client camera/interpolation
- multiplayer pilot + gunner + linked interior crew
- production visual/audio quality

Because alpha.11 changes ShipState schema, dedicated two-boot lifecycle was intentionally rerun once. After run `34197931566` passed, ordinary pushes returned to explicit/manual lifecycle-only execution.

---

# 11. M1 전체 종료 조건

M1은 아래 경험이 실제 Minecraft에서 한 사이클로 동작할 때 완료다.

```text
Earth 자원 준비
→ launch craft 제작
→ 실제 배치
→ fuel/oxygen readiness
→ 직접 상승
→ Earth Orbit
→ salvage 접근
→ autocannon 회수
→ first hostile contact
→ reward 회수
→ Earth 귀환
→ orbital scanner 장착
→ 탐색 능력 증가 체감
```

자동 build 성공만으로 M1 완료라고 표현하지 않는다.

다음 작업은 새 행성 기능 추가가 아니라 **이 전체 사이클의 live acceptance와 feel 교정**이다.
