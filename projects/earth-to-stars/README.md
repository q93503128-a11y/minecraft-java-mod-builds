# EARTH TO STARS

Minecraft Java / NeoForge 26.2 기반의 SF 우주 개척·모듈식 함선 성장 프로젝트다.

> **상태: M0 VERIFIED / P0-A SAVEDDATA ADAPTER BUILD VERIFIED / P0-B BACKEND BUILD VERIFIED / P0-C TRANSITION BACKEND BUILD VERIFIED / P0-D NEXT**

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
- 바닐라 파티클과 임시 엔티티를 production 최종 비주얼로 남기지 않는다.

## 현재 검증 기준

최신 검증 구현 커밋: `a99f7b8470b09cfd509ec4f0aaf054c537db0f3f`

GitHub Actions `Build earth-to-stars` run `34181251912`:

- `clean test build`: PASS
- P0-A/P0-B regression JUnit: PASS
- P0-C transition-policy JUnit: PASS
- Minecraft 26.2 SavedData adapter compile: PASS
- orbital dimension data packaging: PASS
- transition runtime adapter compile: PASS
- production JAR verify: PASS
- JAR: `earth_to_stars-0.1.0-alpha.3.jar`
- SHA-256: `d49b228ad0fee44ceb395d56b040f2a796e55f452b9b410117ed6f947c3d1fd8`

P0-C에는 지구 상승 경계→궤도 레이어, 궤도 하강 경계→지구 귀환 정책과 서버 전환 transaction, 동일 `ShipState`/`ShipId` 유지 경계, lease 회수/재발급, target exterior 생성 실패 rollback, 서버 전역 SavedData 저장 어댑터, 저장된 소유 함선 restore 명령이 들어가 있다.

다만 **실제 디스크 재시작 복원, dedicated server datapack boot, 실제 지구↔우주 비행, client 조종감, 다인 승객 이동, 실멀티는 아직 테스트하지 않았다.** 자동 빌드 성공을 실플레이 완료로 간주하지 않는다.

## 다음 작업

다음 의미 있는 작업 단위는 **P0-D Linked Ship Interior**다.

- 외부 함선이 이동해도 안정적인 interior instance 유지
- `InteriorRef(shipId)` 서버 정본
- exterior ↔ interior 출입
- owner/crew/guest 접근 권한
- 같은 함선의 power/alarm/damage 상태 projection 경계
- 함선 전환 중 interior crew/passenger 처리 기반
- 서버 재시작 뒤 exterior/interior link 복원 구조

반복적인 사용자 테스트는 요구하지 않는다. P0-D까지 의미 있는 기술 묶음을 더 만든 뒤 P0-A/B/C/D의 실제 Minecraft lifecycle을 한 번의 큰 검증으로 확인한다.

최종 함선 모델·cockpit UI·우주 전환 연출은 기술 프록시 단계에서 즉흥 제작하지 않고 `docs/03_UI_ART_REFERENCE_GATE.md`를 통과한 뒤 production 품질로 진행한다.
