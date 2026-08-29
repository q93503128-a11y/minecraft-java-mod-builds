# TURNBOUND Battle Presentation Delta — alpha.7

상태: v0.4 기획서 + `DESIGN_DELTA_ALPHA6.md`를 2026-08-29 실제 alpha.6 플레이 테스트 피드백으로 구체화한 개발 정본 델타.

## 1. alpha.6에서 확인된 문제

실제 테스트 화면에서 다음 문제가 확인되었다.

- 카메라가 플레이어의 필드 위치를 중심으로 돌아 전장 자체가 pivot이 아니었다.
- 카메라 기본 거리가 멀고 드래그 회전이 둔하고 딱딱했다.
- 나무/벽 옆에서 전투를 시작하면 현재 플레이어 위치에 전장을 그대로 만들어 카메라/전투원이 지형에 끼었다.
- 바닐라 hotbar와 crosshair가 전투 HUD와 겹쳤다.
- 적/아군 HUD는 작아졌지만 아직 전투 장면보다 메뉴처럼 느껴지는 부분이 남았다.
- 실제 3D 캐릭터를 클릭해 대상을 지정할 수 없고 HUD/Tab만 사용할 수 있었다.
- SELF/ALL 계열은 스킬 클릭 즉시 실행되어 다른 스킬과 입력 규칙이 일관되지 않았다.

## 2. 전장 생성

- 필드 조우/테스트 명령은 명령을 입력한 정확한 한 블록을 전장 중심으로 사용하지 않는다.
- 플레이어 전방의 선호 지점부터 주변 후보를 검색한다.
- 아군 4명, 적 5명, 카메라 후방 arc가 들어갈 공간의 충돌/물/높이 차를 검사한다.
- 가장 가까운 안전 후보를 battle center로 선택한다.
- 전투원 발 위치는 각 슬롯의 실제 지표면 높이에 맞춘다.
- P0에서는 전투 중 Minecraft player shell을 투명하게 하고 battle center에 고정한다.
- 전투 종료/도주 시 player shell을 전투 시작 전 정확한 필드 위치와 yaw/pitch로 되돌린다.

## 3. 전장 중심 카메라

- camera pivot은 전투 시작 전 player 위치가 아니라 선택된 battle center다.
- 기본 거리: 7.8 blocks.
- zoom clamp: 5.0~12.5 blocks.
- 기본 pitch: 28°.
- pitch clamp: 10°~58°.
- 드래그 감도는 alpha.6보다 크게 올리고 목표 yaw/pitch와 현재 yaw/pitch를 보간해 회전이 끊기지 않게 한다.
- NeoForge camera-angle hook을 사용하여 camera yaw/pitch를 player movement rotation과 분리한다.
- FOV는 P0 전투 중 70으로 고정해 3D target projection과 화면 가독성을 안정화한다.

## 4. 3D 캐릭터 직접 타겟 선택

- 단일 대상 스킬 선택 후 실제 전투원 모델 위치를 LMB로 클릭해 타겟을 고를 수 있어야 한다.
- 서버 snapshot은 각 combatant의 presentation world position을 client에 보낸다.
- client는 battle camera와 world position을 screen position으로 투영하여 mouse hit를 판정한다.
- 잘못된 side, downed 상태 등 TargetRule상 무효 대상은 click hit 후보에서 제외한다.
- 모델 클릭은 `선택`만 한다. 절대로 행동을 실행하지 않는다.
- 하단/우상단 상태바 클릭과 Tab/Shift+Tab은 접근성/보조 입력으로 유지한다.
- 선택 결과는 서버의 3D `▼` marker와 HUD accent가 같은 combatant id를 가리켜야 한다.

## 5. 모든 스킬의 명시적 확정

입력 규칙을 전 스킬에 통일한다.

1. 스킬 클릭/숫자키 → pending skill 선택
2. single target이면 캐릭터 클릭/Tab으로 target 선택
3. SELF/ALL이면 대상 단계 없이 pending 상태 유지
4. `사용 확정` 버튼 또는 Enter → ACT 전송
5. RMB → pending 선택 취소

따라서 SELF, ALLY_ALL, ENEMY_ALL도 첫 클릭으로 바로 사용하지 않는다.

## 6. HUD 정리

- 전투 중 vanilla hotbar/crosshair를 숨긴다.
- survival shell의 health/food/armor/air/xp/selected-item UI도 TURNBOUND HUD에서 사용하지 않는다.
- 아군은 화면 하단 얇은 이름+HP line 4개.
- 적은 우상단 얇은 이름+HP line 최대 5개.
- timeline은 상단 중앙의 작은 token strip.
- 스킬/확정은 현재 행동 가능한 아군이 있을 때만 우측에 등장.
- Auto/배속/도주는 우하단에서 스킬 panel과 겹치지 않는다.
- 단순 상태바에는 큰 불투명 카드 배경을 쓰지 않는다.
- 화면 중앙에는 상시 텍스트/패널을 두지 않는다.

## 7. 계속 유지되는 비생존 규칙

`DESIGN_DELTA_ALPHA6.md`의 player shell 규칙은 그대로 정본이다.

- Minecraft player 하트는 게임 HP가 아니다.
- player는 vanilla damage로 죽지 않는다.
- hunger/food survival loop는 없다.
- 실제 전투 HP는 party/enemy `CombatantState.hp`다.
- vanilla survival progression을 다시 추가하지 않는다.

## 8. 다음 단계

alpha.7 실플레이에서 camera/target/HUD 흐름을 검증한 뒤 실제 GeckoLib character model, outline/ground ring, animation, floating damage/heal number, VFX/SFX 순으로 presentation 품질을 올린다.
