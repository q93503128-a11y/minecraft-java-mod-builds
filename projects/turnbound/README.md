# TURNBOUND

Minecraft Java 26.2 / NeoForge 26.2.0.62 기반 3D 파티 턴제 RPG.

현재 버전: `0.1.0-alpha.8` — Aster March의 첫 제작형 필드 셀 + 가시적 적 조우 + 필드↔전투 루프.

## 현재 구현 범위
- 서버 정본 SPD 누적 Turn Gauge 엔진
- 1000 기준 게이지 / 초과분 보존 / 자연 연속 행동
- 4인 파티 vs 최대 5적 전투 세션
- Basic/Active, 타겟 검증, 자기 행동 기준 쿨타임
- Damage / Heal / Barrier / Gauge shift / Revive / Guard redirect / Counter / 간단 상태효과
- P01 카이렌, P02 루메아, P03 브람, P04 엘리시아 P0 스킬셋
- 서버 권한 적 AI 및 AUTO 전투
- 모든 스킬 `선택 → 대상 → 사용 확정` 입력
- 실제 3D 전투원 클릭 타겟 선택 + HUD/Tab 보조 선택
- 전장 중심 orbit/zoom 카메라 + 안전 전투 공간 탐색
- 전투 중 플레이어 이동/필드 상호작용 잠금
- Minecraft 플레이어 본체는 생존 전투원이 아니라 이동/카메라/세션 shell로 취급
- 플레이어 바닐라 피해 무시, 허기 항상 충족, 생존 HUD 제거

## alpha.8 — Southgate Meadow A01
- v0.4 월드 정본 `Aster March`의 `Southgate Meadow` 내부에 **64×64 한 셀만** 제작했다.
- 좌표: X `-32..31`, Z `128..191`, 기준 지표면 Y `64`.
- Radia 남문 계획 좌표 바로 바깥에서 시작해 남문 초원 방향(+Z)으로 이어진다.
- 굽은 주도로, 관개 수로/작은 다리, 전투 공터, Relay 석벽 잔해, 숲/석재 경계, 다음 셀용 무너진 길을 배치했다.
- 랜덤 인카운터 대신 필드에 실제로 보이는 `E001 부패 보행자 + E002 뼈 사수 + E005 야전 치유사` 순찰대가 존재한다.
- 순찰대는 PATROL → ALERT/추적 → 접촉 ENGAGE 순서로 전투에 진입한다.
- 필드에서 보인 3인과 전투에 등장하는 3인이 같은 EnemyDefinition/수치를 사용한다.
- E001 `끈질김`(최초 HP 30% 이하 Barrier 10%)과 E005 `전열 정비`(적군 전체 DEF +15%, 2행동)를 필드 전투 파이프라인에 연결했다.
- 승리하면 현재 필드 세션에서 해당 순찰대가 사라진다. 패배/도주 시에는 짧은 유예 후 순찰 상태로 복귀한다.
- 필드 셀에서는 블록 파괴/설치 및 바닐라 아이템 상호작용을 진행 수단으로 사용할 수 없다.

## 테스트 진입
- `/turnbound field` : Southgate Meadow A01을 구성하고 Radia 남문 쪽 입구로 이동. **alpha.8 정상 테스트 진입점.**
- 길을 따라가다 보이는 3인 적 파티에 접근하면 조우 전투가 시작된다. 멀리 돌아가면 회피할 수 있다.
- `/turnbound battle` : 필드 조우를 건너뛰는 4v5 전투 코어 진단용 직접 시작.
- `/turnbound leave` : 현재 전투 강제 종료.
- `/turnbound p0` : 자동 진단 전투 결과 출력.

## 디자인 정본 델타
- `DESIGN_DELTA_ALPHA5.md` : 3D 장면 우선 원칙
- `DESIGN_DELTA_ALPHA6.md` : world-first HUD + 비생존 player shell
- `DESIGN_DELTA_ALPHA7.md` : 전장 중심 카메라 + 3D 클릭 타겟 + 명시적 확정
- `DESIGN_DELTA_ALPHA8_FIELD_CELL.md` : Aster March/Southgate A01 + 가시적 조우 + 필드/전투 상태 연결

## 아직 임시인 부분
필드 적과 전투 참가자의 외형은 아직 ArmorStand stand-in이다. alpha.8은 **맵 밀도/탐험/조우/전투 연결**을 먼저 검증하는 단계다. 실제 E001/E002/E005 외형은 수치 위키의 모델/애니메이션 정본대로 GeckoLib presentation 단계에서 교체한다.
