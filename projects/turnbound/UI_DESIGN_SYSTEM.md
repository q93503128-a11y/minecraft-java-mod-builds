# TURNBOUND UI / UX Design System v1

> 상태: 대격변 production 정본.
> 이전 “현재 UI를 조금 polish” 단계는 종료했다. TURNBOUND의 UI는 전면 재설계 대상이다.

## 1. 목표

TURNBOUND UI는 Minecraft 기본 메뉴를 장식한 화면이 아니라 **3D 턴제 RPG의 정보 구조**를 Minecraft 안에서 구현한다.

우선순위:
1. 읽기 쉬움
2. 현재 행동을 빠르게 찾음
3. 캐릭터/월드를 가리지 않음
4. 동일한 입력/상태 표현
5. 외부 검증 UI asset과 실제 게임 사례를 활용
6. GUI Scale/해상도 대응

## 2. 외부 자산 우선

AI가 즉흥적으로 패널을 그리는 것은 마지막 수단이다.

현재 production 후보:
- **Foozle RPG UI Set 1** — CC0. Dark/Fantasy RPG meta/menu 프레임 후보.
- **Kenney Fantasy UI Borders / RPG UI Pack** — CC0. 9-slice, button, utility frame 후보.
- 한국어 UI font 후보: **Pretendard** — SIL OFL 1.1.

한 화면에 서로 다른 pack style을 임의 혼합하지 않는다.

실제 Minecraft mockup에서:
- 글자 대비
- 9-slice 품질
- 16:9/16:10/4:3
- GUI Scale
를 비교한 뒤 primary skin 하나를 고른다.

자산을 실제 repository에 넣을 때 `EXTERNAL_ASSETS.md`에 source/license/modified/used-in을 기록한다.

## 3. Typography

Minecraft 기본 폰트를 production 본문 기본값으로 고정하지 않는다.

한국어 기준:
- body는 작은 GUI Scale에서도 획이 뭉개지지 않아야 함
- 제목과 본문의 weight 차이를 사용
- 긴 설명을 좁은 폭에 우겨넣지 않음
- 자동 말줄임표는 이름/짧은 라벨에만 제한
- 스킬 설명은 wrap + tooltip/detail panel
- 숫자는 정렬이 빨라야 함

최소 글자 크기는 실제 360p/720p급 logical viewport 테스트 후 결정한다.

## 4. Layout token

새 화면은 arbitrary absolute pixel을 늘리지 않는다.

기본 spacing scale:
- XS 4
- S 8
- M 12
- L 16
- XL 24

다만 외부 UI asset의 실제 9-slice/픽셀 grid가 다른 단위를 요구하면 asset grid를 우선하고 전 화면에 통일한다.

## 5. Portrait system

품질 좋은 hero portrait가 확보되면 가장 중요한 공용 component로 쓴다.

`PortraitId = CharacterId`

공용 사용:
- Party Slot
- Turn Order token
- Battle party HP
- Character list
- Result
- Gacha result

상태:
- normal
- current actor
- selected target
- downed
- disabled/not owned

색만으로 구분하지 않고 frame/shape/opacity/marker를 함께 사용한다.

## 6. Battle HUD

### 6.1 공간

중앙 전장은 비운다.

- Turn Order: 상단/측면의 얇은 rail
- Party: 하단 가장자리
- Skill actions: 우하단
- AUTO / speed / flee: skill보다 낮은 visual priority의 control strip
- Enemy HP/상태: 가능한 한 실제 3D 적과 공간적으로 연결

### 6.2 Turn Order rail

텍스트 이름 목록 대신 portrait token을 우선한다.

표현:
- 현재 actor
- 다음 예측 6~10 actions
- 연속 행동이면 같은 portrait가 반복될 수 있음
- Gauge push/delay로 순서가 바뀌면 즉시 재배치
- hover 시 SPD / 남은 Gauge 또는 Action Time 상세

예측은 UI 자체 계산이 아니라 server-authoritative snapshot과 공용 TurnScheduler simulation을 기반으로 한다.

### 6.3 Skill action

버튼에 상시 긴 설명을 넣지 않는다.

기본:
- icon
- skill name
- cooldown
- target hint

hover/focus:
- 정확한 수치
- 상태/조건
- 현재 대상에서 실제 예상 효과

말줄임표로 핵심 효과를 숨기지 않는다.

### 6.4 Targeting

- 실제 3D model click primary
- Tab/keyboard fallback
- world marker + HUD marker same target ID
- invalid/downed target는 선택 후보에서 제외
- single-target는 첫 대상을 몰래 자동 확정하지 않음

## 7. Party / Character UI

한 화면에서 가장 자주 하는 일:
- 4명 편성
- 캐릭터 교체
- 현재 역할/레벨/장비 확인

따라서:
- 좌측/하단 compact roster
- 중앙/우측 선택 캐릭터 detail
- portrait, 역할, 핵심 mechanic을 먼저
- 상세 lore/긴 스킬 설명은 별도 tab/tooltip
- 같은 캐릭터 정보를 카드 여러 장에 반복하지 않음

## 8. Minimap / World Map

목표는 “작은 Aster image”가 아니다.

Minimap:
- 주변 도로/지형
- 발견 landmark
- 목표 방향
- 알려진 facility/NPC
- 위험 marker

World Map:
- 발견한 지역만 명확히
- fast travel
- quest filter
- dungeon/boss 상태
- 중요 NPC/상점 filter

marker semantics는 한 renderer/data model을 공유한다.

## 9. Gacha / Summon UI

결과를 2D 카드 10장으로 바로 보여주는 화면은 최종 목표가 아니다.

3D reveal이 가능한 경우:
- 전용 camera scene 또는 안전한 presentation layer
- character 3D model
- rarity-specific lighting/VFX/SFX
- 고유 pose
- 짧은 nameplate
- 10-pull 최종 summary만 2D

SKIP:
- 첫 프레임부터 가능
- skip해도 이미 확정된 결과/보상은 동일

## 10. Feedback

- click: 시각 + 짧은 sound
- disabled: 이유를 즉시 알 수 있음
- error: ! + 짧은 문구
- success: icon/animation + 짧은 문구
- 중요한 자원 획득: 숫자만 chat에 찍지 않음
- 위험 skill: actor 근처 telegraph

과도한 screen shake/glow는 정보가 아니다.

## 11. 경로/코드 구조

자산 경로를 얕고 예측 가능하게 유지한다.

권장:
```
assets/turnbound/ui/
  common/
  battle/
  party/
  map/
  summon/
  icons/
  portraits/
  fonts/
```

외부 pack 원본은 production에서 쓰는 조각만 import하고, 필요하면 `third_party/` source note에서 출처를 기록한다. 화면별로 서로 다른 깊은 폴더 구조를 만들지 않는다.

UI code도:
- layout
- renderer
- state/view-model
- input
을 분리한다.

## 12. 금지

- 검은 반투명 사각형 남발
- 모든 정보 카드화
- 임의 neon/glow/gradient
- 화면마다 다른 padding
- 8px 이하로 축소해 해결
- 문자열을 계속 잘라 `...` 처리
- 색만으로 selected/disabled 표현
- 기본 Minecraft 버튼을 계획 없이 나열
- 장식이 캐릭터보다 큰 화면
- fixed 1920×1080 좌표 전제
- external pack 여러 개를 스타일 확인 없이 혼합

## 13. 실제 검수

UI 완료는 compile이 아니다.

지원 해상도/GUI Scale에서:
- 글씨 잘림
- 겹침
- 버튼 hitbox
- portrait 선명도
- skill tooltip 위치
- turn rail update
- 카메라와 HUD 충돌
- map marker 과밀
을 실제 client screenshot으로 확인한다.
