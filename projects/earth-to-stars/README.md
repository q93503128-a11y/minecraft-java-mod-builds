# EARTH TO STARS

Minecraft Java / NeoForge 26.2 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **상태: M0 BUILD BOOTSTRAP VERIFIED / P0-A PURE SHIP KERNEL VERIFIED / P0-B NEXT**

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
- 바닐라 파티클과 임시 큐브 모델을 production 최종 비주얼로 남기지 않는다.

## 현재 검증 기준

구현 커밋: `d1c34db306680944c5696ecabd5f018943fef772`

GitHub Actions `Build earth-to-stars` run `34175374292`에서 한 번의 의미 있는 M0/P0-A 게이트를 통과했다.

- `clean test build`: PASS
- P0-A JUnit: PASS
- production JAR verify: PASS
- JAR: `earth_to_stars-0.1.0-alpha.1.jar`
- SHA-256: `baea44c12f5383781967c503c41363831312676643f1870fac82df9108c46848`

GameTest, dedicated server, client, live multiplayer는 이번 순수 커널 게이트에서 실행하지 않았다. 특히 실제 멀티는 `NOT TESTED`이며 성공했다고 간주하지 않는다.

## 다음 작업

다음 의미 있는 작업 단위는 **P0-B Ship Exterior / Movement Backend + 첫 Minecraft persistence integration**이다.

1. 최소 함선 exterior object/entity
2. server-authoritative transform
3. throttle / yaw / pitch / acceleration / deceleration
4. 조종 권한과 control lease
5. client interpolation 경계
6. ShipState를 Minecraft 저장 경계에 연결
7. create → save → reload → 동일 shipId/modules 복원 integration gate

이 묶음이 끝나기 전에는 최종 함선 모델이나 cockpit UI를 즉흥 제작하지 않는다. P0-B 외형은 기술 프록시에 한정하고, 최종 시각 작업은 `docs/03_UI_ART_REFERENCE_GATE.md`를 통과한 뒤 진행한다.