# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 캐릭터 수집형 파티 턴제 RPG.

현재 버전: `0.1.0-alpha.12` — Chapter 1 필드 UX / 보상 결과 / Relay 이동 / A02 연결.

## 현재 구현 범위
- 서버 정본 SPD 누적 Turn Gauge 엔진
- Gauge 1000 / 초과분 보존 / 자연 연속 행동
- 1~4 아군 / 최대 5적
- Basic/Active, 타겟 검증, 자기 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge shift / Revive / Guard redirect / Counter / 상태효과
- P01 카이렌, P02 루메아, P03 브람, P04 엘리시아
- 서버 권한 적 AI 및 AUTO
- 모든 스킬 `선택 → 필요 시 대상 → 사용 확정`
- 단일 대상 스킬은 실제 3D 전투원 클릭 또는 Tab으로 선택
- 스킬 hover 상세 설명
- 전투원 anchor 평균 중심 orbit/zoom 카메라
- Minecraft Player는 전투원이 아닌 이동·카메라·세션 shell
- 바닐라 플레이어 피해/허기/생존 HUD 제거

## Southgate Meadow A01 — Chapter 1
- Aster March 안의 첫 64×64 제작형 필드 셀
- X `-32..31`, Z `128..191`, 기준 Y `64`
- 주도로 / 관개 수로 / 다리 / 조우 공터 / Relay 잔해 / 남쪽 봉쇄선
- 가시적 조우 5개 `ENC_M01~M05`
- 정식 Southgate 적 E001~E005
- E003 갈고리 추적자 / E004 철갑 파수병
- PATROL → ALERT/추적 → ENGAGE
- 랜덤 인카운터 아님 / 회피 가능
- 조우별 최초 승리 보상 및 Chapter 1 진행도
- 5개 일반 조우 클리어 후 B01 그라울 출현
- B01 승리 시 Chapter 1 클리어

## alpha.12 필드 UX
- `남문 정찰관` 우클릭: 월드를 가리지 않는 Chapter 1 임무 패널
- 일반 조우/B01 클리어 상태, 현재 목표, 누적 XP/Gold 표시
- 전투 복귀 시 별도 승리/보상 결과 패널
- `남문 초원 계전석` 조사 시 Relay 활성화 및 이동 패널
- 활성화 전 목적지는 이동 불가
- Chapter 1 클리어 후 남쪽 봉쇄문 개방
- 신규 64×64 `South Road A02` 연결
- A02의 `남부 도로 거점 계전석`에 직접 도달해 조사하면 두 거점 Fast Travel 가능
- Field/Reward/Travel UI 상태는 서버 정본 payload로 동기화
- 블록 파괴/설치/바닐라 생존 아이템 진행 차단 유지

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

필드 패널:
- NPC/계전석 우클릭
- `Esc` 또는 RMB: 닫기
- Relay 패널의 활성 목적지 클릭: 이동

## 테스트 명령어
- `/turnbound field` : Southgate Meadow Chapter 1 정상 진입
- `/turnbound status` : Chapter 1 임무 패널 열기
- `/turnbound battle` : 필드 조우를 건너뛰는 4v5 전투 코어 진단
- `/turnbound leave` : 현재 전투 강제 종료
- `/turnbound p0` : 결정론적 자동 전투 진단

## 디자인 정본 델타
- `DESIGN_DELTA_ALPHA5.md` : 3D 장면 우선
- `DESIGN_DELTA_ALPHA6.md` : world-first HUD + 비생존 player shell
- `DESIGN_DELTA_ALPHA7.md` : 전장 카메라 + 3D 타겟 + 명시적 확정
- `DESIGN_DELTA_ALPHA8_FIELD_CELL.md` : A01 + 가시적 조우
- `DESIGN_DELTA_ALPHA9_BATTLE_UX.md` : 카메라 피벗/직접 클릭/action dock/hover tooltip
- `DESIGN_DELTA_ALPHA10_PRODUCTION_GATE.md` : P2→P3→P4 제작 순서
- `DESIGN_DELTA_ALPHA11_SOUTHGATE_CH1.md` : ENC_M01~M05 / E003/E004 / 보상 / B01
- `DESIGN_DELTA_ALPHA12_FIELD_UX_RELAY.md` : Quest/Reward UI / Relay / A02

## 다음 제작 방향
P2를 마무리한 뒤 P3로 이동한다.
- P2 계속: A02 실제 조우/NPC, 대화 흐름, 지역 전환 polish, 결과/퀘스트 UI 실플레이 조정
- P3: saveSchemaVersion 4, 레벨/성급/보유/가챠/장비/강화/파티 UI/CP
- P4: ★6 각성/전용 장비/보스/스토리/신규 지역/캐릭터/반복 콘텐츠

현재 ArmorStand는 시스템 검증용 presentation stand-in이며 최종 캐릭터 모델/애니메이션/VFX가 아니다.
