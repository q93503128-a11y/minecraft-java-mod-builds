# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 캐릭터 수집형 파티 턴제 RPG.

현재 버전: `0.1.0-alpha.9` — Southgate A01 가시적 조우 루프 + 전장 중심 카메라/직접 3D 타겟/스킬 action dock/hover 상세설명 보강.

## 현재 구현 범위
- 서버 정본 SPD 누적 Turn Gauge 엔진
- Gauge 1000 / 초과분 보존 / 자연 연속 행동
- 1~4 아군 / 최대 5적
- Basic/Active, 타겟 검증, 자기 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge shift / Revive / Guard redirect / Counter / 상태효과
- P01 카이렌, P02 루메아, P03 브람, P04 엘리시아
- 서버 권한 적 AI 및 AUTO
- 모든 스킬 `선택 → 필요 시 대상 → 사용 확정`
- 단일 대상 스킬은 자동 첫 타겟을 잡지 않고 실제 3D 전투원 클릭 또는 Tab으로 선택
- 3D 클릭 판정은 발~머리 body capsule 기반
- 스킬 버튼 hover 시 대상/쿨타임/남은 쿨타임/효과 상세 설명
- 전투원 anchor 평균을 기준으로 하는 orbit/zoom 카메라
- 실제 3인칭 카메라 위치 계산과 렌더 yaw/pitch 동기화
- 전투/필드에서 Minecraft Player는 생존 전투원이 아니라 이동·카메라·세션 shell
- 바닐라 플레이어 피해 무시, 허기 제거, 생존 HUD 제거

## Southgate Meadow A01
- Aster March 안의 첫 64×64 제작형 필드 셀
- X `-32..31`, Z `128..191`, 기준 Y `64`
- 주도로 / 관개 수로 / 다리 / 조우 공터 / Relay 잔해 / 숲·석재 경계
- 보이는 적 `E001 부패 보행자 + E002 뼈 사수 + E005 야전 치유사`
- PATROL → ALERT/추적 → ENGAGE
- 랜덤 인카운터 아님
- 회피 가능
- 승리 시 해당 필드 세션에서 제거, 도주/패배 시 유예 후 복귀
- 블록 파괴/설치/바닐라 생존 아이템 진행 차단

## 조작
전투:
- 마우스 스킬 클릭 또는 `1~5`
- 단일 대상: 실제 3D 캐릭터 클릭 또는 `Tab / Shift+Tab`
- `Enter`: 사용 확정
- `RMB`: 현재 선택 취소
- 빈 전장 `LMB Drag`: orbit
- Wheel: zoom
- `A`: AUTO
- `X`: 1× / 2×
- `R`: 일반 필드전 도주 / 전투 종료 후 복귀
- `Esc`: 전투 설정

스킬 상세 설명은 스킬 버튼에 마우스를 올리면 표시한다.

## 테스트 진입
- `/turnbound field` : Southgate Meadow A01 정상 플레이 진입
- `/turnbound battle` : 필드 조우를 건너뛰는 4v5 전투 코어 진단
- `/turnbound leave` : 현재 전투 강제 종료
- `/turnbound p0` : 결정론적 자동 전투 진단

## 디자인 정본 델타
- `DESIGN_DELTA_ALPHA5.md` : 3D 장면 우선
- `DESIGN_DELTA_ALPHA6.md` : world-first HUD + 비생존 player shell
- `DESIGN_DELTA_ALPHA7.md` : 전장 카메라 + 3D 타겟 + 명시적 확정
- `DESIGN_DELTA_ALPHA8_FIELD_CELL.md` : A01 + 가시적 조우
- `DESIGN_DELTA_ALPHA9_BATTLE_UX.md` : 카메라 피벗/직접 클릭/action dock/hover tooltip

## 다음 제작 방향
기획서 v0.4의 단계 순서를 유지한다.

- P2 완성: Southgate 확장, NPC, 퀘스트, 보상, 지역 이동, B01
- P3: 레벨/성급/보유/가챠/장비/강화/파티 UI/CP
- P4: ★6 각성/전용 장비/보스/스토리/신규 지역/캐릭터/반복 콘텐츠

현재 ArmorStand는 시스템 검증용 presentation stand-in이며 최종 캐릭터 모델/애니메이션/VFX가 아니다.
