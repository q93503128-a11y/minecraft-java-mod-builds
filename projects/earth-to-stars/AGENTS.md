# EARTH TO STARS — AGENTS

이 파일은 EARTH TO STARS를 수정하는 모든 작업자/에이전트의 프로젝트 전용 계약이다.

## 작업 전 HARD STOP

반드시 다음 순서로 읽는다.

1. 저장소 `/AGENTS.md`
2. 저장소 `/docs/BUILD_STANDARD.md`
3. 저장소 `/docs/QUALITY_STANDARD.md`
4. 저장소 `/docs/QUALITY_STANDARD_GAME_DESIGN.md`
5. `PROJECT.md`
6. `README.md`
7. `docs/00_MASTER_GAME_DESIGN.md`
8. 현재 작업 분야의 세부 문서
9. `THIRD_PARTY_ASSETS.md`
10. 실제 소스/리소스와 최신 플레이테스트 기록

## 절대 제품 규칙

- Overworld는 Earth다.
- Nether/End는 메인 progression 필수가 아니다.
- 함선은 B형 모듈식 Ship Object 구조다. 자유 블록 물리 함선을 기본 구조로 재설계하지 않는다.
- surface→orbit→space 이동은 플레이어 관점에서 연속 경험을 지향한다.
- ship/player/world의 중요 상태는 server-authoritative다.
- 싱글 전용 편의 구현 때문에 이후 멀티가 막히는 구조를 만들지 않는다.
- 자동화는 솔로 지원과 후반 성장 수단이다. 플레이어의 재미있는 직접 조작을 처음부터 제거하지 않는다.
- 자동/수동 포탑은 같은 weapon-system abstraction 안에서 다룬다.
- 중요한 전투/이동/채굴/생산 판정은 클라이언트 결과 통보를 신뢰하지 않는다.
- 대량 콘텐츠는 data-driven schema 위에 올린다.
- 행성마다 광물·재화만 늘리는 식의 quantity inflation을 금지한다.
- 새로운 자원은 새 행동, 새 선택, 새 위험 대응, 자동화, 이동 범위 또는 전략을 열어야 한다.
- Nether/End 자원에 메인 필수 recipe를 의존시키지 않는다.

## 멀티플레이 규칙

서버가 최종 결정한다.

- ship ownership / crew permission
- module install/remove
- power / fuel / ammo
- turret target eligibility and fire success
- projectile/hit/damage
- mining yields and resource transfer
- production output
- celestial travel completion
- progression unlocks
- ship damage/repair
- save data and migration

클라이언트는 입력, 렌더링, 애니메이션, UI, 사운드, VFX, 안전한 prediction을 담당한다.

실제 멀티 검증을 하지 않았다면 `MULTIPLAYER VERIFIED`라고 쓰지 않는다. 구조 준비만 됐으면 `MULTIPLAYER ARCHITECTURE READY / NOT TESTED`처럼 구분한다.

## 시각 품질 규칙

최종 UI/ship/module/weapon/planet/VFX/sound를 AI가 즉흥적으로 디자인하지 않는다.

특히 금지 기본값:

- 검은 반투명 사각형
- 임의의 네온 cyan 테두리
- 의미 없는 gradient/glow
- 모든 정보를 card로 분절
- 서로 다른 출처의 아이콘 무정리 혼합
- vanilla cube + particle만으로 대표 SF 장비를 최종 처리

핵심 비주얼은 `docs/03_UI_ART_REFERENCE_GATE.md`의 레퍼런스/자산 게이트를 통과해야 한다.

## 외부 해결책 활용

- 레퍼런스, 수정 가능한 베이스, 직접 사용 자산, 코드 라이브러리를 구분한다.
- 라이선스와 현재 버전 호환성을 확인한다.
- 외부 자산은 `THIRD_PARTY_ASSETS.md`에 기록한다.
- ARR/불명확 라이선스 프로젝트는 설계/화면 연구용이며 코드·자산을 복제하지 않는다.
- 26.2에 없는 라이브러리를 핵심 dependency로 먼저 고정하지 않는다.

## 성능 규칙

다음은 기본 금지 패턴이다.

- 각 turret가 매 tick 큰 반경 entity scan
- 각 module이 독립적인 power network 전체 탐색
- 실제 아이템 entity/pipe 이동을 초고빈도로 시뮬레이션해 ship logistics를 표현
- 모든 ship interior를 항상 강제 tick
- 과도한 client sync
- server에서 렌더용 세부 상태까지 지속 동기화

우선 구조:

```text
ShipSimulation
├ PowerGrid
├ AmmoNetwork
├ SensorGrid
├ WeaponController
├ Propulsion
├ DamageState
└ InteriorLink
```

성능 문제가 의심되면 spark/JFR로 측정한다.

## 완료 규칙

코드 작성이나 compile 성공만으로 완료가 아니다.

작업 성격에 따라:

- 요구 기능
- 기존 기능 보존
- 서버/클라이언트 경계
- 저장/재접속
- 멀티 구조
- 실제 플레이 흐름
- 비주얼/사운드/피드백
- edge case
- 성능
- 문서 정합
- 필요한 테스트

까지 확인한다.
