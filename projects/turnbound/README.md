# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 파티 턴제 RPG.

현재 버전: `0.1.0-alpha.6` — 전투 HUD 완전 재작성 + Minecraft 생존 체력/허기 제거 규칙 반영.

## 현재 구현 범위
- 서버 정본 SPD 누적 Turn Gauge 엔진
- 1000 기준 게이지 / 초과분 보존 / 자연 연속 행동
- 4인 파티 vs 5적 P0 전투 세션
- Basic/Active, 타겟 검증, 자기 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge shift / Revive / Guard redirect / Counter / 간단 상태효과
- P01 카이렌, P02 루메아, P03 브람, P04 엘리시아 P0 스킬셋
- 검병 2 / 궁수 / 방패병 / 주술사 적 구성
- 서버 권한 적 AI 및 AUTO 전투
- `1~5`, Tab/Shift+Tab, Enter, RMB 취소, A AUTO, X 배속, R 도주
- 드래그 orbit / 휠 zoom 전투 카메라
- 선택 타겟 3D 위치 마커
- alpha.4~5의 대형 좌우 패널/프레임 UI를 제거한 새 world-first HUD
- 하단 아군 4명 compact 상태바
- 상단 우측 적 최대 5명 compact 상태바
- 상단 중앙 턴 timeline
- 행동 가능 시에만 우측에 현재 캐릭터 스킬 표시
- 전투 중 플레이어 이동/필드 상호작용 잠금
- Minecraft 플레이어 본체는 생존 전투원이 아니라 이동/카메라/세션 shell로 취급
- 플레이어 바닐라 피해 무시, 허기 항상 충족, 하트/허기 HUD 숨김
- `/turnbound p0` 결정론적 서버 진단 시뮬레이션
- Java 25 JUnit 회귀 테스트 및 GitHub Actions 빌드

## P0 실행
- `/turnbound battle` : 플레이어블 P0 전투 시작
- `/turnbound leave` : 개발/테스트용 강제 세션 종료
- `/turnbound p0` : 자동 진단 전투 결과 출력

## 디자인 정본 델타
- `DESIGN_DELTA_ALPHA5.md` : R_PG 및 외부 사례 조사에서 확정한 3D 장면 우선 원칙
- `DESIGN_DELTA_ALPHA6.md` : alpha.4~5 UI 폐기, 새 HUD 구조, 플레이어 체력/허기 비생존 규칙

## 아직 임시인 부분
P0의 전투 참가자는 여전히 ArmorStand 기반 stand-in이다. 실제 영웅/적 모델, rig, 애니메이션, VFX/SFX는 HUD/카메라/전투 흐름이 검증된 다음 GeckoLib presentation 단계에서 교체한다.
