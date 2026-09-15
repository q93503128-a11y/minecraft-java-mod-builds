# EARTH TO STARS

Minecraft Java `1.20.1` / Forge 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **현재 상태: `0.2.0-alpha.2` VS/Genesis REBOOT — 실제 Valkyrien Skies 블록 함선 starter craft + 외부 cockpit/propulsion stack 구현 / build·JAR·client resource smoke 검증 / live flight acceptance와 multiplayer는 아직 미검증**

## 한 줄 설명

오버월드를 지구로 두고 Minecraft 생존에서 시작해, 실제로 조종하는 함선으로 대기권을 벗어나 우주를 탐험하고 하나의 authored modular ship을 이동수단 → 집 → 공장 → 전함으로 성장시키는 대형 게임이다.

## 현재 기술 스택

```text
Minecraft 1.20.1
Forge 47.4.0
Java 17
Valkyrien Skies 2.4.10  ← 실제 moving-block ship / physics / collision
Genesis 0.7.3           ← VS 함선의 Earth↔space 기반
ZPS 2.5.1               ← Octo cockpit/controller + finite Power Cell
ZPL 1.5.0               ← ion thruster + gyroscope
EARTH TO STARS          ← authored craft, game rules, progression, authority
```

이 리부트는 품질 때문에 선택했다. 이전 Minecraft 26.2 구현은 ETS가 비행·충돌·카메라·우주 전환을 직접 흉내냈고 실제 플레이에서 조종감과 충돌, cockpit, 우주 이동 품질이 부족했다. 현재는 이미 검증된 외부 함선 생태계를 물리 기반으로 사용하고 ETS는 게임 자체의 규칙과 성장에 집중한다.

## 반드시 읽는 순서

1. 저장소 `/AGENTS.md`
2. 저장소 `/docs/BUILD_STANDARD.md`
3. 저장소 `/docs/QUALITY_STANDARD.md`
4. 저장소 `/docs/QUALITY_STANDARD_GAME_DESIGN.md`
5. 이 프로젝트 `PROJECT.md`
6. 이 프로젝트 `AGENTS.md`
7. `docs/00_MASTER_GAME_DESIGN.md`
8. `docs/06_VS_GENESIS_REBOOT.md`
9. 현재 작업 분야 문서
10. `THIRD_PARTY_ASSETS.md`
11. 실제 source/resource와 최신 playtest 기록

현재 GitHub `main`과 위 정본이 과거 대화나 예전 0.1.x 문서 세부 구현과 충돌하면 현재 `main`을 우선한다.

## 정본 문서

- `PROJECT.md` — 현재 환경, 제품 결정, 권한 구조, 구현/검증 상태, 다음 gate
- `AGENTS.md` — 프로젝트 전용 작업 계약
- `docs/00_MASTER_GAME_DESIGN.md` — 게임 정체성, 핵심 루프, 함선 성장, 탐험/전투/경제
- `docs/01_TECHNICAL_ARCHITECTURE.md` — authoritative modular ship, networking/save/performance 설계
- `docs/02_WORLD_PROGRESSION_CONTENT.md` — Earth→orbit→Moon→asteroid→planet→deep space 진행
- `docs/03_UI_ART_REFERENCE_GATE.md` — UI/모델/VFX/사운드 외부 reference/asset 품질 gate
- `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md` — 첫 Earth→orbit→return 게임 루프의 설계 의도. 단, 26.2 vehicle 구현 세부사항은 reboot보다 우선하지 않는다.
- `docs/06_VS_GENESIS_REBOOT.md` — 현재 물리/우주 stack 정본
- `THIRD_PARTY_ASSETS.md` — 외부 코드·자산·레퍼런스 출처와 사용 조건
- `CHANGELOG.md` — 실제 변경과 검증 기록

## 핵심 제품 규칙

- Overworld = Earth.
- Nether / End는 메인 진행 필수가 아니다.
- 실제 이동체는 Valkyrien Skies block ship이다. ETS가 물리엔진을 다시 만들지 않는다.
- 하지만 게임 구조는 **B형 authored modular ship**이다. VS를 쓴다고 unrestricted freeform ship-building 게임으로 바꾸지 않는다.
- Genesis가 baseline Earth↔space ship transition을 담당한다.
- 중요 상태는 server-authoritative다.
- manual / auto turret은 같은 authoritative weapon state를 공유한다.
- solo는 automation으로 가능하고 multiplayer 역할은 강제 노동이 아니다.
- Power / Ammo / Sensor / Propellant / Oxygen 등은 함선 수준의 중앙 simulation을 지향한다.
- 최종 ship/UI/weapon/space visual은 외부 reference/asset gate를 통과해야 한다.

---

# 현재 구현 — 0.2.0-alpha.2

Starter craft는 이제 보이는 모형이 아니라 실제 블록 구조를 조립한 뒤 VS `ShipAssembler`로 생성되는 물리 함선이다.

현재 craft에는 다음이 들어간다.

- ZPS Octo Controller 실제 좌석/입력
- ZPS finite Power Cell
- ZPL ion thruster / exhaust
- ZPL gyroscope
- ETS flight core
- ETS control node
- 배터리 → thruster Forge Energy bus

현재 입력 연결:

```text
W → forward
S → reverse
A / D → yaw
↑ / ↓ → ascend / descend
← / → → lateral strafe
```

이 입력은 코드/외부 API 수준으로 연결되었지만 **실제 플레이에서 방향·힘·카메라·seat feel까지 합격한 상태는 아니다.**

## 외부 시스템 사용 원칙

- VS가 collision / ship transform / physical movement를 담당한다.
- ZPL이 실제 thruster / gyro 힘을 담당한다.
- ZPS가 탑승 가능한 controller와 starter power hardware를 담당한다.
- Genesis가 우주 전환 기반을 담당한다.
- ETS는 이들을 얇게 연결하고 그 위에 progression, ownership, resource, combat, economy를 올린다.

외부 optional compatibility 경고를 없애기 위해 Create 같은 무관한 모드를 억지로 설치하거나 가짜 API를 만들지 않는다. 정상 runtime smoke에서는 Forge GameTest 자동 discovery를 분리하고 ETS가 실제로 사용하는 stack 자체를 검증한다.

---

# 현재 검증 상태

- reboot source-set isolation: **TESTED**
- clean build / production JAR structure: **BUILD VERIFIED**
- dedicated external stack boot: **TESTED**
- real VS starter craft server assembly: **TESTED**
- headless client resource/model reload gate: **TESTED**
- actual starter craft flight feel: **NOT PLAYTESTED**
- actual player movement/collision/camera on craft: **NOT PLAYTESTED**
- occupied craft Genesis Earth→space transition: **NOT TESTED**
- live multiplayer: **NOT TESTED**

Compile/build 성공을 실제 비행 성공으로 취급하지 않는다.

---

# 다음 실제 gate

새 Moon/asteroid 콘텐츠를 추가하기 전에 starter craft 한 대를 실제 Minecraft에서 끝까지 검증한다.

```text
ETS creative tab에서 starter craft 획득
→ 실제 VS ship으로 배치
→ ZPS cockpit 탑승
→ W/A/S/D + 방향키 조종
→ 이륙
→ 지형/엔티티와 실제 충돌 확인
→ 필요 시 좌석에서 내려 함선 위 이동
→ hull scale / seat / camera / 방향 / 추진력 검수
→ 대기권 상승
→ Genesis 경계를 함선째 통과
→ 우주에서도 같은 craft 계속 조종
```

이 gate가 플레이 감각까지 통과하면 기존 M1 설계의 survival crafting, fuel/oxygen, salvage, 첫 함포, first contact, sensor upgrade를 새 물리 stack 위에 다시 연결한다.

현재 0.1.x의 `ShipExteriorEntity`, 커스텀 26.2 비행, 임시 `orbital_space` 구현은 reboot의 현재 구현으로 간주하지 않는다.
