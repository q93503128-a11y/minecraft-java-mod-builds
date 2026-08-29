# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 파티 턴제 RPG.

현재 버전: `0.1.0-alpha.4` — 외부 턴제 RPG 사례/R_PG 조사 반영 + P0 입력·타겟팅·화면 생명주기 보강.

## 현재 구현 범위
- 서버 정본 SPD 누적 Turn Gauge 엔진
- 1000 기준 게이지 / 초과분 보존 / 자연 연속 행동
- 4인 파티 vs 5적 P0 전투 세션
- Basic/Active, 타겟 검증, 자기 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge shift / Revive / Guard redirect / Counter / 간단 상태효과
- P01 카이렌, P02 루메아, P03 브람, P04 엘리시아 P0 스킬셋
- 검병 2 / 궁수 / 방패병 / 주술사 적 구성
- 마우스 스킬/타겟 선택 + `1~5`, `Tab/Shift+Tab`, `Enter`, `Esc` 키보드 선택 흐름
- 서버 권한 적 AI 및 AUTO 전투
- `A` AUTO / `X` x1·x2 프레젠테이션 배속
- 상단 턴 타임라인, 현재 행동자, HP/보호막, 쿨타임, 선택 대상 표시
- Kenney CC0 RPG UI 원본 자산 사용
- GUI 배율이 높아 논리 화면이 작아져도 4v5 타겟/스킬 버튼이 화면 안에 유지되는 반응형 P0 배치
- 전투 중 플레이어 이동 및 일반 필드 상호작용 차단
- ESC로 라이브 전투 화면만 닫혀 서버 세션이 남는 상태 방지
- 승리/패배 후 `R`/복귀 버튼으로 명시적 복귀
- 방향키 시점 조절
- 임시 3D ArmorStand 전투 배치와 짧은 공격 접근/복귀 동작
- `/turnbound p0` 결정론적 서버 진단 시뮬레이션
- Java 25 JUnit 회귀 테스트 및 GitHub Actions 빌드

## P0 실행
- `/turnbound battle` : 플레이어블 P0 전투 시작
- `/turnbound leave` : 개발/테스트용 강제 세션 종료 및 임시 전투 엔티티 정리
- `/turnbound p0` : 자동 진단 전투 결과 출력

전투 화면은 마우스 또는 키보드로 조작할 수 있다. 단일 대상 스킬에서 Tab/Shift+Tab으로 유효 대상만 순환하고 Enter로 확정하며, Esc는 현재 선택만 취소한다. P0의 일반 전투 중에는 기본 도주 버튼을 노출하지 않는다.

## 외부 참고 조사

R_PG/R_PG X, TurnBasedMC, Soulbound: Turnbattle, Cobblemon, Craftics에서 실제로 확인한 내용과 TURNBOUND에 반영/보류한 경계는 `REFERENCE_STUDY_ALPHA4.md`에 기록한다. 타 게임의 비허가 UI/아이콘/텍스처/코드를 복제하지 않는다.

## 아직 임시인 부분

P0의 3D 전투 참가자는 실제 캐릭터 모델이 아니라 ArmorStand 기반 위치/동작 stand-in이다. 이 부분은 다음 캐릭터/프레젠테이션 단계에서 GeckoLib 기반 실제 모델·애니메이션·VFX로 교체한다. 현재 P0의 목적은 전투 규칙, 입력, 네트워크, 화면 흐름, 4v5 배치와 전투 템포를 실제 게임 안에서 검증하는 것이다.

UI 자산 출처와 사용 조건은 `EXTERNAL_ASSETS.md`를 정본으로 한다.
