# Projects

이 폴더는 문승준의 Minecraft Java 모드를 프로젝트별로 분리하여 보관한다.

## 활성 프로젝트

- [`earth-to-stars`](./earth-to-stars/) — EARTH TO STARS, SF 우주 개척 + 모듈식 함선 성장 + 협동 함선 운용
  - 상태: `M0 VERIFIED / P0-A PURE KERNEL VERIFIED / P0-B BACKEND BUILD VERIFIED / P0-C NEXT`
  - 현재 버전: `0.1.0-alpha.2`
  - 정본 시작점: [`earth-to-stars/README.md`](./earth-to-stars/README.md)
  - 개발 순서: M0 Build Bootstrap → P0 Ship/Space/Multiplayer Technical Gate → M1 Earth/Orbit Gameplay Slice → M2 Moon Vertical Slice → M3 Production Visual Gate → M4 Asteroid/Ship Growth → M5 Mars/Expedition Ship → M6 Belt/Multi-System Scale → M7+ Outer System/Deep Space
  - 핵심 원칙: Overworld=Earth, Nether/End 비필수, B형 모듈식 함선, 서버 권한 멀티 구조, 수동/자동 포탑, 중앙 power/ammo/sensor simulation, 외부 reference 기반 SF 디자인

- [`riftfrontier`](./riftfrontier/) — Riftfrontier / 균열 개척기, 차원 탐사 액션 RPG + 개척·물류 + 산업·연구 + 세력 시뮬레이션
  - 상태: `M0 CANON LOCKED / BUILD BOOTSTRAP NEXT`
  - 정본 시작점: [`riftfrontier/README.md`](./riftfrontier/README.md)
  - 개발 순서: M0 Canon & Bootstrap → M1 Content Kernel → M2 Expedition Vertical Slice → M3 Combat/Boss Quality Gate → M4 Logistics/Industry → M5 Faction/Dynamic World → M6 Region 01 Production Complete → M7 Scale-Out
  - 핵심 원칙: 콘텐츠 개수보다 시스템 상호작용, Region Pack 밀도, schema/builder/validator 기반 생산, 외부 reference 기반 디자인

- [`turnbound-re`](./turnbound-re/) — TURNBOUND: RE, Minecraft 파티 턴제 RPG
  - 상태: `CORE IMPLEMENTATION READY / FINAL VISUALS GATED`
  - 정본 시작점: [`turnbound-re/README.md`](./turnbound-re/README.md)
  - 개발 순서: M0 Bootstrap → M1 Battle Core → M2 Minecraft/Network → M3 Representative Content → M4 Progression → M5 Visual Gate 이후 production UI
  - 구 `turnbound` 프로젝트는 폐기되었으며 설계 근거로 사용하지 않는다.

- [`village-guardians`](./village-guardians/) — Village Guardians, 마을지키기
  - 현재 버전: `0.1.0-alpha.1`
  - 상태: 통치 코어 소스 구현, 실제 JAR 빌드 및 게임 로딩 검증 전

## 공통 기준

- 새 모드는 `projects/<project-slug>/`에 추가한다.
- 각 프로젝트는 최소한 `PROJECT.md`, 실제 소스, 빌드 설정을 가진다. 기획 단계 프로젝트는 `PROJECT.md`와 구현 정본을 먼저 만들고 M0에서 소스/빌드 설정을 추가한다.
- 다른 프로젝트의 코드·리소스·워크플로를 복제하거나 덮어쓰지 않는다.
- 2026-07-31 이전 프로젝트 내용은 모두 폐기되었으며 복원 기준으로 사용하지 않는다.
- 새 Village Guardians도 기존 구현을 이어받지 않고 처음부터 작성했다.
