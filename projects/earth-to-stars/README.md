# EARTH TO STARS

Minecraft Java / NeoForge 26.2 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **상태: M0 VERIFIED / P0-A SAVEDDATA ADAPTER BUILD VERIFIED / P0-B BACKEND BUILD VERIFIED / P0-C TRANSITION BACKEND BUILD VERIFIED / P0-D LINKED INTERIOR BACKEND BUILD VERIFIED / P0-E TURRET BACKEND BUILD VERIFIED / P0-F NEXT**

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

최신 검증 기준 커밋: `85e4003223839dd3fe24e87e8fd8382a1931e693`

GitHub Actions `Build earth-to-stars` run `34186350799`:

- `clean test build`: PASS
- P0-A/P0-B/P0-C regression JUnit: PASS
- P0-D interior allocation/layout JUnit: PASS
- P0-E representative turret JUnit: PASS
- OFF / MANUAL / AUTO_DEFENSE state machine: PASS
- exclusive manual lease / replay rejection: PASS
- ammo / cooldown / firing-arc rules: PASS
- shared SensorGrid target selection: PASS
- Minecraft 26.2 turret adapter compile: PASS
- production JAR verify: PASS
- JAR: `earth_to_stars-0.1.0-alpha.5.jar`
- SHA-256: `53499a1cf6f14bd21cfefc8cdd095b2e2999c322c20c1be8c660d005efd48835`

P0-E는 하나의 대표 autocannon 상태를 `ShipId`에 연결하고, `OFF / MANUAL / AUTO_DEFENSE`를 같은 서버 정본에서 다룬다. 수동 모드는 단일 control lease와 session/sequence 검증을 사용하며, 자동 모드는 포탑별 world scan 대신 함선당 공유 `SensorGrid` contact cache를 사용한다. 현재 Minecraft adapter는 10 tick 간격의 staggered scan으로 P0 구조를 검증한다.

현재 projectile은 기술검증용 서버 논리 shot이며 실제 모델·트레이서·총구화염·사운드·카메라 반동이 없다. 자동 적 판정도 P0에서 `Enemy` 계열만 hostile로 취급하는 임시 정책이다. 이는 production 전투/세력 판정이 아니다.

## 아직 실게임 검증하지 않은 것

- 실제 save→disk→server restart→same ship/interior restore
- dedicated server custom-dimension boot/smoke
- 실제 Earth↔orbit 비행과 조종감/camera/interpolation
- 실제 exterior↔interior 출입과 다인 동시 체류
- 외부 조종 중 내부 승무원 유지
- 실제 수동 포탑 조준/사격감
- 실제 자동포탑 타격/피드백
- 실제 2인 pilot+gunner control conflict / disconnect lifecycle
- live multiplayer session
- production ship/interior/turret visual

자동 빌드 성공을 위 항목의 실플레이 완료로 간주하지 않는다.

## 다음 작업

다음 의미 있는 작업 단위는 **P0-F Central Ship Systems**다.

- 중앙 PowerGrid: generation / storage / demand / priority shortage
- 중앙 AmmoPool: compatible weapon server consumption
- 중앙 SensorGrid runtime을 P0-E 임시 adapter에서 함선 시스템으로 승격
- 여러 turret/module이 같은 계산 결과를 공유
- synthetic multi-module/multi-turret load로 구조적 비용 확인
- P0-E의 local ammo를 중앙 authoritative resource로 이동

P0-F까지 기술축을 묶은 뒤 P0-G에서 실제 Minecraft lifecycle/멀티 검증을 한 번의 큰 게이트로 진행한다. 반복적인 사용자 테스트는 요구하지 않는다.

최종 함선 모델·cockpit UI·내부 디자인·포탑 모델/VFX/사운드는 기술 프록시 단계에서 즉흥 제작하지 않고 `docs/03_UI_ART_REFERENCE_GATE.md`를 통과한 뒤 production 품질로 진행한다.
