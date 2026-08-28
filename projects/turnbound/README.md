# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0 기반 3D 파티 턴제 RPG.

현재 버전: `0.1.0-alpha.1` — P0 전투 코어.

## 현재 구현 범위
- 서버 정본 SPD 누적 Turn Gauge 엔진
- 1000 기준 게이지 / 초과분 보존 / 자연 연속 행동
- 4인 파티와 최대 5적 전투 상태
- Basic/Active, 타겟 검증, 자기 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge shift / Revive
- P01 카이렌, P02 루메아, P03 브람, P04 엘리시아 P0 스킬셋
- P01 집중, P02 턴 지원, P03 보호/반격, P04 위기 자동회복
- 전투불능/부활/승패 판정
- `/turnbound p0` 서버 진단 시뮬레이션
- JUnit P0 회귀 테스트

최종 UI는 자체 즉흥 제작하지 않는다. UI Lib + 외부 CC0 RPG UI 자산을 사용한다. 자세한 출처는 `EXTERNAL_ASSETS.md`.
