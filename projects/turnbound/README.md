# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 파티 턴제 RPG.

현재 버전: `0.1.0-alpha.7` — 전장 중심 카메라/안전 아레나/3D 캐릭터 클릭 타겟/모든 스킬 확정 입력으로 P0 전투 UX 재구성.

## 현재 구현 범위
- 서버 정본 SPD 누적 Turn Gauge 엔진
- 1000 기준 게이지 / 초과분 보존 / 자연 연속 행동
- 4인 파티 vs 5적 P0 전투 세션
- Basic/Active, 타겟 검증, 자기 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge shift / Revive / Guard redirect / Counter / 간단 상태효과
- P01 카이렌, P02 루메아, P03 브람, P04 엘리시아 P0 스킬셋
- 검병 2 / 궁수 / 방패병 / 주술사 적 구성
- 서버 권한 적 AI 및 AUTO 전투
- `/turnbound battle` 시 주변 지형을 검사해 벽/나무/물에 끼지 않는 전투 중심점 탐색
- 전투 중 invisible player shell을 전장 중심 camera pivot으로 사용하고 종료 후 원래 필드 위치/시점 복구
- 부드러운 battlefield-centered orbit, 기본 거리 7.8, 휠 zoom 5.0~12.5
- 3D 전투 캐릭터 위치를 화면에 투영해 모델을 직접 클릭하는 타겟 선택
- HUD 상태바/Tab은 타겟 선택의 보조 입력
- `1~5` 스킬 선택, Enter/사용 확정, RMB 취소, A AUTO, X 배속, R 도주
- SELF/ALL 포함 모든 스킬은 첫 클릭으로 절대 실행하지 않고 반드시 별도 확정 단계 필요
- 하단 아군 4명 얇은 상태바 / 우상단 적 최대 5명 얇은 상태바 / 상단 중앙 턴 timeline
- 현재 아군 행동 차례에만 우측 contextual action UI 표시
- AUTO/배속/도주는 우하단 보조 제어
- 전투 중 바닐라 hotbar/crosshair 제거, survival health/food/armor/air/xp HUD 제거
- Minecraft 플레이어 본체는 생존 전투원이 아니라 이동/카메라/세션 shell
- 플레이어 바닐라 피해 무시, 허기 항상 충족
- `/turnbound p0` 결정론적 서버 진단 시뮬레이션
- Java 25 JUnit 회귀 테스트 및 GitHub Actions 빌드

## P0 실행
- `/turnbound battle` : 플레이어블 P0 전투 시작
- `/turnbound leave` : 개발/테스트용 강제 세션 종료
- `/turnbound p0` : 자동 진단 전투 결과 출력

## 디자인 정본 델타
- `DESIGN_DELTA_ALPHA5.md` : R_PG 및 외부 사례 조사에서 확정한 3D 장면 우선 원칙
- `DESIGN_DELTA_ALPHA6.md` : alpha.4~5 UI 폐기, 플레이어 체력/허기 비생존 규칙
- `DESIGN_DELTA_ALPHA7.md` : 실제 alpha.6 테스트 피드백 기반 카메라 pivot, 안전 아레나, 3D 직접 타겟, 명시적 행동 확정 규칙

## 아직 임시인 부분
P0의 전투 참가자는 여전히 ArmorStand 기반 stand-in이다. 실제 영웅/적 모델, rig, 애니메이션, VFX/SFX는 전투 UX가 검증된 다음 GeckoLib presentation 단계에서 교체한다.
