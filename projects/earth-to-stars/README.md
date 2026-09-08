# EARTH TO STARS

Minecraft Java / NeoForge 26.2 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **상태: P0 AUTOMATED TECHNICAL GATES COMPLETE / M1-A/B EARTH PREPARATION + FIRST LAUNCH CRAFT BACKEND BUILD VERIFIED / LIVE CLIENT PLAY NOT TESTED / LIVE MULTIPLAYER NOT TESTED / M1-C LAUNCH READINESS + ATMOSPHERE NEXT**

## 한 줄 설명

오버월드를 지구로 두고 바닐라 생존에서 시작해 산업·항공·궤도·달·소행성·행성·심우주로 실제 플레이 공간을 확장하면서, 하나의 모듈식 함선을 이동수단 → 집 → 공장 → 전함으로 성장시키는 대형 Minecraft 게임.

## 반드시 읽는 순서

1. 저장소 `/AGENTS.md`
2. 저장소 `/docs/BUILD_STANDARD.md`
3. 저장소 `/docs/QUALITY_STANDARD.md`
4. 저장소 `/docs/QUALITY_STANDARD_GAME_DESIGN.md`
5. 이 프로젝트 `PROJECT.md`
6. 이 프로젝트 `AGENTS.md`
7. `docs/00_MASTER_GAME_DESIGN.md`
8. `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md`
9. 작업 분야별 세부 문서
10. `THIRD_PARTY_ASSETS.md`
11. 실제 소스/리소스와 최신 플레이테스트 기록

기억이나 이전 대화가 현재 GitHub 문서와 충돌하면 GitHub `main`이 우선이다.

## 정본 문서

- `PROJECT.md` — 환경, 범위, 절대 제품 결정, 멀티 권한 계약, 현재 검증 기준
- `AGENTS.md` — 이 프로젝트 전용 작업 계약
- `docs/00_MASTER_GAME_DESIGN.md` — 게임 정체성, 핵심 루프, 함선 성장, 전투, 탐험, 경제, Nether/End 정책
- `docs/01_TECHNICAL_ARCHITECTURE.md` — B형 모듈식 함선, 서버 권한, interior instance, 네트워크, 저장, 성능 구조
- `docs/02_WORLD_PROGRESSION_CONTENT.md` — 지구→궤도→달→소행성→행성→심우주 진행과 자원 역할
- `docs/03_UI_ART_REFERENCE_GATE.md` — SF UI/모델/VFX/사운드의 외부 레퍼런스 기반 제작 게이트
- `docs/04_P0_VERTICAL_SLICE.md` — P0 기술 게이트와 첫 플레이어블 수직 구간
- `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md` — 실제 Earth preparation / first craft / orbit gameplay slice 정본
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

## 하지 않는 것

- 행성 선택 메뉴를 눌러 즉시 텔레포트하는 여행을 핵심 경험으로 만들지 않는다.
- 행성마다 색만 다른 광물 10개를 추가하지 않는다.
- Nether/End를 필수 진행 체크박스로 만들지 않는다.
- 완전 자유 블록 물리 함선을 처음부터 핵심 의존성으로 삼지 않는다.
- 자동 포탑이 각각 매 tick 전체 엔티티를 스캔하지 않는다.
- 싱글 구현 뒤 마지막에 멀티를 붙이지 않는다.
- 검은 반투명 패널 + 네온 테두리식 AI 즉흥 UI를 final로 쓰지 않는다.
- 임시 ArmorStand, vanilla texture proxy, 논리 projectile, command 조작면을 production visual/UX로 남기지 않는다.

---

# 현재 구현 — 0.1.0-alpha.9

## M1-A/B Earth Preparation + First Launch Craft

P0 기술검증 이후 실제 survival progression을 처음 연결했다.

신규 Earth 제작 아이템은 6개로 제한한다.

- `reinforced_frame` — 경량 강화 프레임
- `avionics_unit` — 항법제어장치
- `propellant_cell` — 고체 추진제 셀
- `oxygen_cartridge` — 압축 산소 카트리지
- `life_support_unit` — 생명유지장치
- `launch_craft_kit` — 소형 개척선 조립 패키지

첫 단계에서는 신규 광석을 추가하지 않고 Minecraft의 Iron / Copper / Redstone / Gold / Amethyst / Gunpowder / Paper / Water / Leather를 우주 진입 제작 루프에 다시 연결한다.

제작 흐름:

```text
vanilla Earth resources
→ reinforced frame / avionics / propellant / oxygen
→ life support
→ launch craft assembly package
→ 지상 배치
→ authoritative ShipState
```

`launch_craft_kit`은 Overworld 지면에서 사용한다.

서버가 확인하는 것:

- Earth인가
- 3×3×3 조립 공간이 확보됐는가
- 플레이어가 이미 함선을 소유하고 있지 않은가
- ShipState/exterior/persistence/control lease 생성이 가능한가

성공한 경우에만 survival item을 소비한다.

첫 함선의 canonical slots:

```text
core
engine
power
cargo
life_support
turret
```

기본 설치:

- `command_core_mk1`
- `engine_mk1`
- `battery_mk1`
- `cargo_mk1`
- `life_support_mk1`

`turret` hardpoint는 의도적으로 비어 있다. 첫 orbital salvage/combat가 실제 함선 능력을 바꾸는 성장으로 이어지도록 무장을 처음부터 공짜로 주지 않는다.

상세 제작/배치 계약은 `docs/05_M1_EARTH_ORBIT_GAMEPLAY_SLICE.md`를 따른다.

## 실제 recipe progression 보호

P0-H의 추상 progression graph뿐 아니라 alpha.9부터 실제 `launch_craft_kit` 제작식의 dependency closure도 `tools/validate_m1_launch.py`가 검사한다.

따라서 중간 제작물을 거쳐 Nether/End-only 재료가 첫 우주 진입의 필수 dependency로 들어오는 회귀를 자동으로 차단한다.

---

# 최신 검증 기준

검증 기준 커밋: `bc8e51ba30d2e3eec07dfd868b0f79dc9460e73f`

GitHub Actions `Build earth-to-stars` run `34191142069`:

- P0-H progression validator self-tests: PASS
- canonical main progression graph: PASS
- M1 launch recipe dependency closure: PASS
- M1 launch crafting Nether/End independence: PASS
- starter craft blueprint JUnit: PASS
- P0-A~G JUnit regression: PASS
- `clean test build`: PASS
- Minecraft 26.2 / NeoForge 26.2.0.38-beta compile: PASS
- production JAR verify: PASS
- M1 recipes/client item definitions packaged: PASS
- JAR: `earth_to_stars-0.1.0-alpha.9.jar`
- SHA-256: `c2f033c73de080c90cff7ed77ae0b3d2d07d6ec5d14aa22cc0f173766e223264`

P0-G의 실제 dedicated-server 두 번 부팅 / SavedData restore 검사는 run `34188840459`에서 이미 통과했고 alpha.9에서는 persistence/custom-dimension 구조를 바꾸지 않았기 때문에 의도적으로 다시 실행하지 않았다.

---

# 기술 프록시 경계

현재 다음은 final 품질이 아니다.

- ArmorStand ship exterior
- vanilla texture 기반 M1 item icon proxy
- 기술용 `ship_interiors` room
- 빈 `orbital_space`
- command 기반 일부 조작면
- 논리 projectile

이들은 기능 연결을 위한 proxy다. Production 함선/아이콘/cockpit/interior/turret/VFX/sound/space visual은 `docs/03_UI_ART_REFERENCE_GATE.md`를 통과한 뒤 제작한다.

---

# 아직 구현/실플레이 확인되지 않은 핵심

- propellant cell의 실제 fuel reserve 소비
- oxygen cartridge / life support의 실제 oxygen reserve 소비
- launch readiness gate
- atmosphere gameplay / 고도별 환경 변화
- 실제 Earth↔orbit 플레이 비행과 카메라/조종감
- 실제 crafting book/recipe usability
- 실제 launch package 월드 배치
- orbital salvage contact
- hostile orbital contact
- Earth return reward / first ship upgrade
- 실제 exterior↔interior 다인 체류
- 실제 manual/auto 포탑 사격감
- 실제 2인 pilot+gunner
- live multiplayer session
- production visual/audio

자동 build 성공을 이 항목의 실제 플레이 성공으로 취급하지 않는다.

---

# 다음 작업 — M1-C Launch Readiness + Atmosphere

다음 묶음에서는 현재 아이템을 실제 gameplay resource에 연결한다.

```text
propellant cell
→ authoritative launch/fuel reserve

oxygen cartridge + life_support_mk1
→ authoritative oxygen reserve

readiness
→ atmosphere ascent
→ Earth Orbit transition permission
```

관리 메뉴와 재화 종류를 늘리는 방식이 아니라, **출발 전에 준비했는가 / 우주에서 얼마나 버틸 수 있는가**라는 의미 있는 선택으로 만들고 초기에는 기본값과 명확한 피드백으로 관리 노동을 최소화한다.

M1-C 후에는 M1-D에서 첫 orbital salvage/contact → Earth return → 첫 함선 개수조까지 연결한다.

M1 전체 종료 경험은 다음이다.

> **“내가 지구에서 준비한 작은 개척선으로 직접 우주에 올라가, 궤도에서 처음으로 자원과 위험을 만나고 살아 돌아왔다.”**
