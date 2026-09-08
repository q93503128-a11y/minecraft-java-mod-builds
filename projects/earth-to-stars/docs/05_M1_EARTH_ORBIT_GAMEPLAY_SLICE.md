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

현재 버전: `0.1.0-alpha.9`

현재 상태:

`M1-A/B EARTH PREPARATION + FIRST LAUNCH CRAFT BACKEND BUILD VERIFIED / LIVE CLIENT PLAY NOT TESTED / M1-C LAUNCH READINESS + ATMOSPHERE NEXT`

완료된 자동 기술축:

- 실제 Minecraft item registry 6종
- Earth-only crafting chain
- actual recipe dependency closure validator
- Nether/End-independent launch recipe contract
- player-facing launch craft assembly item
- Earth-only deployment check
- 3×3×3 deployment clearance check
- duplicate-owned-ship rejection
- successful deployment에서만 package 소비
- authoritative ShipState creation
- immediate ShipSavedData persistence boundary
- server-issued pilot control lease
- starter craft canonical module loadout
- production JAR packaging

아직 실제 플레이 검증되지 않은 축:

- crafting book/client recipe usability
- item rendering/temporary icons in client
- in-world launch package deployment
- actual pilot controls/camera feel
- actual oxygen/fuel consumption
- atmosphere transition presentation
- live Earth→orbit flight
- live salvage/contact loop
- multiplayer pilot + crew session

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
- 후속 M1-C에서 실제 launch readiness / fuel quantity와 연결

현재 제작 1회 output ×2:
- iron ingot ×1
- gunpowder ×2
- paper ×1
- copper ingot ×1

## 3.4 압축 산소 카트리지

ID: `earth_to_stars:oxygen_cartridge`

역할:
- 첫 진공 생존 자원
- 후속 M1-C에서 실제 oxygen reserve와 연결

현재 제작 1회 output ×2:
- copper ingot ×1
- iron ingot ×2
- water bucket ×1
- redstone ×1

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
- 아이템을 실제 authoritative ShipState로 전환하는 플레이어-facing construction object

현재 제작:
- reinforced frame ×4
- avionics unit ×1
- life support unit ×1
- propellant cell ×2

이 아이템은 완성된 함선을 인벤토리에 보관하는 개념이 아니라, 지상에서 함선을 조립하기 위한 패키지다.

---

# 4. Launch Craft 배치 계약

플레이어가 `launch_craft_kit`을 지면에 사용하면 서버가 다음을 검증한다.

1. 현재 차원이 `minecraft:overworld`인가.
2. 클릭 위치 위에 3×3×3 조립 공간이 비어 있는가.
3. 해당 플레이어에게 이미 등록된 함선이 없는가.
4. authoritative ShipState를 만들 수 있는가.
5. exterior proxy를 월드에 생성할 수 있는가.
6. ShipSavedData에 기록할 수 있는가.
7. pilot control lease를 서버가 발급할 수 있는가.

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
```

기본 설치 모듈:

- `command_core_mk1`
- `engine_mk1`
- `battery_mk1`
- `cargo_mk1`
- `life_support_mk1`

의도적으로 비워 두는 슬롯:

- `turret`

첫 함선에 자동포탑까지 모두 지급하지 않는다.

Earth Orbit의 첫 salvage/combat reward가 무기 hardpoint를 채우거나 강화하는 식으로, **첫 우주 원정이 실제 함선 능력을 바꾸는 성장**으로 연결되어야 한다.

---

# 6. Nether / End 독립성

P0-H의 canonical progression graph뿐 아니라 실제 M1 launch recipe closure도 별도로 검사한다.

`tools/validate_m1_launch.py`는 `launch_craft_kit`에서 시작해 모든 EARTH TO STARS 중간 제작물을 재귀적으로 따라간다.

현재 검사 목적:

- launch craft recipe 누락 방지
- 중간 mod item recipe 누락 방지
- Nether/End 전용 자원이 실제 launch crafting chain에 들어오는 회귀 방지

Nether/End sidegrade는 이후 추가 가능하지만 첫 우주 진입의 유일한 제작 경로가 되어서는 안 된다.

---

# 7. 현재 visual boundary

alpha.9의 아이템 모델은 **클라이언트 등록/가시성 기술용 placeholder**다.

현재 vanilla texture proxy를 production art로 유지하지 않는다.

Production 전환 시 `03_UI_ART_REFERENCE_GATE.md`와 `THIRD_PARTY_ASSETS.md`를 따른다.

대상:

- launch craft item/assembly representation
- actual launch craft exterior
- cockpit
- thruster/engine
- life-support module
- fuel/oxygen containers
- item icons
- launch VFX/sound

외부 reference/asset 검토 없이 AI 즉흥 SF 디자인으로 확정하지 않는다.

---

# 8. M1-C — 다음 구현 단위

다음 묶음은 **Launch Readiness + Atmosphere**다.

목표:

- propellant cell을 실제 ship fuel/launch reserve와 연결
- oxygen cartridge/life support를 실제 survival reserve와 연결
- launch craft가 준비 부족 상태에서 단순히 비행 가능한 문제 제거
- Earth atmosphere 구간 정의
- 고도 상승에 따라 산소/대기/추진 요구가 달라지는 최소 gameplay
- Earth→Orbit transition에 fuel/oxygen capability gate 연결
- 플레이어에게 관리 노동이 되지 않는 간단한 readiness feedback

하지 않는 것:

- 연료 종류 10개 추가
- 산소 압력 숫자 여러 개를 항상 직접 관리하게 하기
- 장비 메뉴/게이지를 먼저 대량 추가
- production HUD를 reference 없이 즉흥 제작

M1-C 이후 M1-D에서 첫 orbital salvage/contact와 Earth return reward를 연결한다.

---

# 9. M1 전체 종료 조건

M1은 아래 경험이 실제 Minecraft에서 한 사이클로 동작할 때 완료다.

```text
Earth 자원 준비
→ launch craft 제작
→ 실제 배치
→ fuel/oxygen readiness
→ 직접 상승
→ Earth Orbit
→ salvage/contact
→ reward 회수
→ Earth 귀환
→ 함선 upgrade 선택
```

자동 build 성공만으로 M1 완료라고 표현하지 않는다.
