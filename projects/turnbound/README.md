# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 독립형 3D 파티 턴제 RPG.

현재 버전: `0.1.0-alpha.5` — 실제 R_PG 전투 레퍼런스와 v0.4 정본을 기준으로 P0 전투 HUD/카메라/도주 흐름을 전면 재구성.

## 현재 구현 범위
- 서버 정본 SPD 누적 Turn Gauge 엔진
- 1000 기준 게이지 / 초과분 보존 / 자연 연속 행동
- 4인 파티 vs 5적 P0 전투 세션
- Basic/Active, 타겟 검증, 자기 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge shift / Revive / Guard redirect / Counter / 간단 상태효과
- P01 카이렌, P02 루메아, P03 브람, P04 엘리시아 P0 스킬셋
- 검병 2 / 궁수 / 방패병 / 주술사 적 구성
- 서버 권한 적 AI 및 캐릭터별 AUTO 우선순위
- 1.0x / 2.0x 프레젠테이션 속도

## alpha.5 전투 Presentation
- alpha.4의 좌우 거대 아군/적 버튼 패널 폐기
- 화면 전체 암막 제거: 3D 전투 월드가 화면 대부분을 차지
- 하단 compact 아군 party strip
- 상단 우측 compact 적 상태 HUD
- 상단 좌측 최대 8슬롯 턴 타임라인
- 플레이어 행동 가능 시에만 우측 소형 action panel 노출
- 내부 `ally_*`, `TURN_READY`, pulse 등 디버그 문자열 화면 노출 제거
- 아머스탠드 상시 이름표 제거
- 선택 타겟과 연결되는 월드 공간 `▼` 마커
- 넓어진 4v5 formation과 역할별 임시 장비 실루엣
- Battle 진입 시 3인칭 후방 overview camera
- 빈 전투 장면 LMB drag orbit
- mouse wheel zoom 6~18 blocks
- 전투 종료 시 기존 camera type/yaw/pitch 복구

## 입력
- LMB: UI / 타겟 HUD 선택, 빈 장면 drag 시 orbit
- RMB: 스킬/타겟 선택 취소
- Wheel: zoom
- `1~5`: 현재 캐릭터 Basic/Active
- `Tab / Shift+Tab`: 유효 타겟 순환
- `Enter`: 현재 강조 타겟 확정(P0 보조 입력)
- `A`: AUTO
- `X`: 1.0x / 2.0x
- `R`: 일반 P0 전투 확정 도주 / 결과 후 복귀
- `Esc`: 전투 설정 overlay

## P0 실행
- `/turnbound battle`: 플레이어블 P0 전투 시작
- `/turnbound leave`: 개발/테스트용 강제 세션 종료 및 임시 전투 엔티티 정리
- `/turnbound p0`: 결정론적 자동 진단 전투

## 정본 문서

게임 전체 기획/수치/캐릭터 정본은 MasterDocs v0.4를 사용한다.
실제 alpha.4 테스트와 사용자 제공 R_PG 플레이 스크린샷에서 확인된 전투 Presentation 보정은 `DESIGN_DELTA_ALPHA5.md`가 전투 UI/카메라 범위에서 우선한다.

기존 공개 레퍼런스 조사 기록은 `REFERENCE_STUDY_ALPHA4.md`에 남기되, R_PG의 실제 UI 자산/텍스처/코드는 복제하지 않는다.

## 아직 임시인 부분

현재 3D 전투 참가자는 실제 영웅 모델이 아니라 ArmorStand 기반 stand-in이다. alpha.5는 이 임시 모델을 최종 디자인으로 다듬는 단계가 아니라, 실제 모델이 들어와도 그대로 유지할 수 있는 **공간 중심 HUD / 카메라 / 선택 흐름**을 먼저 검증하는 단계다.

다음 Presentation 단계에서는 GeckoLib 기반 실제 캐릭터 모델, 전용 애니메이션, VFX/SFX, 모델 outline/바닥 타겟 ring과 피해/회복 표기를 추가한다.

UI 자산 출처와 사용 조건은 `EXTERNAL_ASSETS.md`를 정본으로 한다.
