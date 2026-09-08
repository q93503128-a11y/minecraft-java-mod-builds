# EARTH TO STARS

Minecraft Java / NeoForge 26.2 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **상태: P0 AUTOMATED TECHNICAL GATES COMPLETE / P0-G DEDICATED LIFECYCLE VERIFIED / P0-H NETHER-END INDEPENDENCE VERIFIED / LIVE MULTIPLAYER NOT TESTED / M1 EARTH-ORBIT GAMEPLAY SLICE NEXT**

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
- `docs/04_P0_VERTICAL_SLICE.md` — 기술 위험 제거와 첫 플레이어블 수직 구간
- `src/main/resources/data/earth_to_stars/progression/main_path.json` — 실제 CI가 검사하는 메인 진행 그래프 정본
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

### P0-H progression gate

최신 검증 기준 커밋: `8b3b4edda64505d476418e0b08fbe85baea6b0ba`

GitHub Actions `Build earth-to-stars` run `34189697283`:

- progression validator self-tests: PASS
- canonical main progression graph validation: PASS
- Nether-only required main route rejection: PASS
- End-only required main route rejection: PASS
- clean alternative route acceptance: PASS
- optional Nether/End side-route acceptance: PASS
- dependency cycle / unknown node rejection: PASS
- `clean test build`: PASS
- P0-A~G JUnit regression: PASS
- production JAR verify: PASS
- progression graph production JAR packaging: PASS
- JAR: `earth_to_stars-0.1.0-alpha.8.jar`
- SHA-256: `3af179b7cdb16236722507434a000f38dcc82fc59079aab584e1f79771f2e688`

P0-H는 단순히 `nether`/`end` 문자열이 존재하면 실패하는 검사가 아니다. `requires_any` 대체 경로를 가진 진행 그래프를 분석해서 **각 메인 마일스톤까지 Nether/End를 거치지 않는 실제 선행 경로가 하나 이상 존재하는지** 확인한다. Nether/End shortcut이나 sidegrade 자체는 허용되지만 그것만이 유일한 필수 경로가 되면 CI가 실패한다.

현재 정본 메인 마일스톤은 Earth Industry → Launch Craft → Earth Orbit → Orbital Salvage → Moon → Near-Earth Asteroids → Mars → Main Belt → Outer System → Deep Space까지 연결된다.

### P0-G lifecycle gate

P0-G의 실제 디스크 생명주기 검증은 run `34188840459`에서 이미 완료했다. alpha.8에서는 공용 검증 예산 원칙에 따라 이 비싼 dedicated server 2회 부팅을 다시 돌리지 않았다.

P0-G에서 실제 검증된 항목:

- dedicated server 1회차 boot
- `earth_to_stars:orbital_space` registration
- `earth_to_stars:ship_interiors` registration
- real SavedData write
- clean shutdown / all dimension save
- same world directory 2회차 boot
- same `ShipId / owner / module slots` restore
- same `ShipId → interior slot` restore
- central power `37.5` restore
- autocannon ammo `73` restore

센서 contact, pilot/turret lease, 논리 projectile, 임시 exterior entity ID는 restart 후 재구축해야 하는 휘발 상태이므로 persistence 대상이 아니다.

## 현재 기술 프록시

현재 projectile, ArmorStand exterior, 기술 interior room, command 조작면, 빈 orbital space는 P0 프록시다. 최종 모델·트레이서·총구화염·사운드·조종석 UI·카메라·함선 내부·지구/우주 비주얼로 간주하지 않는다.

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

자동 테스트나 dedicated-server lifecycle 성공을 위 실플레이 항목까지 검증한 것으로 간주하지 않는다.

## 다음 작업 — M1 Earth/Orbit Gameplay Slice

P0의 자동 기술 위험 제거는 완료했다. 이제 기술검증을 계속 옆으로 늘리지 않고 첫 실제 게임 루프 제작으로 넘어간다.

범위:

```text
Earth 생존/초기 산업
→ launch craft 제작
→ fuel / oxygen 준비
→ 직접 상승
→ atmosphere progression
→ Earth Orbit
→ 첫 salvage contact
→ 첫 hostile contact
→ manual/auto autocannon 사용
→ salvage 회수
→ Earth 귀환
→ 함선 개수조
```

다음 단계의 목표는 기능 목록이 아니라 다음 경험이다.

> **“내가 지구에서 준비한 작은 함선으로 직접 우주에 올라가, 궤도에서 처음으로 자원과 위험을 만나고 살아 돌아왔다.”**

이 덩어리가 실제로 플레이 가능해진 뒤에 사용자에게 한 번에 테스트를 요청한다. 작은 수정마다 테스트를 반복시키지 않는다.

최종 함선 모델·cockpit UI·내부 디자인·포탑 모델/VFX/사운드는 기술 프록시 단계에서 즉흥 제작하지 않고 `docs/03_UI_ART_REFERENCE_GATE.md`를 통과한 뒤 production 품질로 진행한다.
