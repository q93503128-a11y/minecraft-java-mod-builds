# EARTH TO STARS

Minecraft Java / NeoForge 26.2 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **상태: M0 VERIFIED / P0-A SAVEDDATA ADAPTER BUILD VERIFIED / P0-B BACKEND BUILD VERIFIED / P0-C TRANSITION BACKEND BUILD VERIFIED / P0-D LINKED INTERIOR BACKEND BUILD VERIFIED / P0-E TURRET BACKEND BUILD VERIFIED / P0-F CENTRAL SYSTEMS BACKEND BUILD VERIFIED / P0-G DEDICATED LIFECYCLE VERIFIED / LIVE MULTIPLAYER NOT TESTED / P0-H NEXT**

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
8. 작업 분야별 세부 문서
9. `THIRD_PARTY_ASSETS.md`
10. 실제 소스/리소스와 최신 플레이테스트 기록

기억이나 이전 대화가 현재 GitHub 문서와 충돌하면 GitHub `main`이 우선이다.

## 정본 문서

- `PROJECT.md` — 환경, 범위, 절대 제품 결정, 멀티 권한 계약, 현재 검증 기준
- `AGENTS.md` — 이 프로젝트 전용 작업 계약
- `docs/00_MASTER_GAME_DESIGN.md` — 게임 정체성, 핵심 루프, 함선 성장, 전투, 탐험, 경제, Nether/End 정책
- `docs/01_TECHNICAL_ARCHITECTURE.md` — B형 모듈식 함선, 서버 권한, interior instance, 네트워크, 저장, 성능 구조
- `docs/02_WORLD_PROGRESSION_CONTENT.md` — 지구→궤도→달→소행성→행성→심우주 진행과 자원 역할
- `docs/03_UI_ART_REFERENCE_GATE.md` — SF UI/모델/VFX/사운드의 외부 레퍼런스 기반 제작 게이트
- `docs/04_P0_VERTICAL_SLICE.md` — 구현 전 기술 위험 제거와 첫 플레이어블 수직 구간
- `THIRD_PARTY_ASSETS.md` — 외부 코드/자산/레퍼런스의 출처·라이선스 기록
- `CHANGELOG.md` — 실제 변경·검증 기록

## 프로젝트의 네 가지 핵심 기둥

```text
Minecraft 생존
+ 직접 이동하는 우주 탐사
+ 성장하는 모듈식 함선
+ 협동 가능한 함선 운용/전투
```

새 기능은 최소 하나의 기둥을 강화하고, 가능하면 둘 이상을 연결해야 한다.

## 하지 않는 것

- 행성 선택 메뉴를 누르면 즉시 텔레포트되는 우주 여행을 핵심 경험으로 만들지 않는다.
- 행성마다 색만 다른 광물 10개를 추가해 규모를 부풀리지 않는다.
- Nether/End를 필수 진행 체크박스로 만들지 않는다.
- 완전 자유 블록 물리 함선 엔진을 처음부터 직접 만들지 않는다.
- 자동 포탑이 각자 매 tick 전체 엔티티를 검색하는 구조를 만들지 않는다.
- 싱글 구현 후 마지막에 멀티를 붙이지 않는다.
- 최종 SF UI를 검은 반투명 패널 + 네온 테두리 + 의미 없는 글로우로 즉흥 제작하지 않는다.
- 바닐라 파티클과 임시 엔티티/명령 조작면을 production 최종 비주얼·UX로 남기지 않는다.

## 현재 검증 기준

최신 검증 기준 커밋: `557d273ecfa78c1ba9cc62956cd78f6eb7c55153`

GitHub Actions `Build earth-to-stars` run `34188840459`:

- `clean test build`: PASS
- production JAR verify: PASS
- P0-A~F regression JUnit: PASS
- P0-G systems snapshot restore JUnit: PASS
- dedicated server 1회차 실제 boot: PASS
- `earth_to_stars:orbital_space` dedicated-server registration: PASS
- `earth_to_stars:ship_interiors` dedicated-server registration: PASS
- seed world 정상 종료 및 모든 dimension save: PASS
- 같은 `run/world`로 dedicated server 2회차 boot: PASS
- `ShipId / owner / module slots` disk restore: PASS
- `ShipId → interior slot` disk restore: PASS
- central `PowerGrid / AmmoPool` disk restore: PASS
- restored `ShipSystemsRuntime` 초기화: PASS
- 센서는 저장하지 않고 재스캔하는 휘발 cache 정책: PASS
- JAR: `earth_to_stars-0.1.0-alpha.7.jar`
- SHA-256: `76bc15395500382f0acbc68826c7e6b95533b5ce9863de40e837c9a2856ac708`

P0-G lifecycle probe는 일반 플레이어에게 노출되는 명령/UI가 아니다. CI에서만 환경변수로 활성화되며, 첫 서버 부팅에서 고정 `ShipId`의 ship/interior/system state를 실제 SavedData에 기록하고 정상 종료한 뒤, 두 번째 서버 부팅이 같은 디스크 상태를 읽어 동일 값을 검증한다.

검증값:

- ship: `11111111-2222-3333-4444-555555555555`
- interior slot: `0`
- power: `37.5`
- autocannon ammo: `73`

전력/탄약은 이제 함선 단위 영속 정본이다. 센서 contact는 재시작 전 월드 엔티티 상태를 그대로 들고 있으면 유령 표적이 생길 수 있으므로 의도적으로 persistence 대상이 아니며 서버 기동 후 다시 획득한다.

현재 projectile, ArmorStand exterior, 기술 interior room, command 조작면은 여전히 P0 프록시다. 최종 모델·트레이서·총구화염·사운드·조종석 UI·카메라·함선 내부 비주얼로 간주하지 않는다.

## 아직 실게임 검증/구현하지 않은 것

- 실제 Earth↔orbit 비행과 조종감/camera/interpolation
- 실제 exterior↔interior 출입과 다인 동시 체류
- 외부 조종 중 내부 승무원 유지
- 실제 수동 포탑 조준/사격감
- 실제 자동포탑 타격/피드백
- 실제 2인 pilot+gunner control conflict / disconnect lifecycle
- live multiplayer session
- client smoke / production visual quality
- production ship/interior/turret visual

자동 dedicated-server lifecycle 성공을 위 실플레이 항목까지 검증한 것으로 간주하지 않는다.

## 다음 작업

다음 의미 있는 작업 단위는 **P0-H Nether/End Independence Validator**다.

메인 진행 그래프에서 Nether/End 전용 자원·구조물·advancement가 지구→우주 메인 루트의 필수 ancestor가 되는 순간 자동 실패시키는 검증기를 넣는다. Nether/End는 sidegrade, shortcut, specialist material, late-game variant로는 허용한다.

P0-H까지 닫은 뒤 실제 조종/전환/포탑/내부를 하나의 플레이 가능한 덩어리로 묶어 사용자 테스트를 요청한다. 작은 수정마다 테스트를 반복시키지 않는다.

최종 함선 모델·cockpit UI·내부 디자인·포탑 모델/VFX/사운드는 기술 프록시 단계에서 즉흥 제작하지 않고 `docs/03_UI_ART_REFERENCE_GATE.md`를 통과한 뒤 production 품질로 진행한다.
