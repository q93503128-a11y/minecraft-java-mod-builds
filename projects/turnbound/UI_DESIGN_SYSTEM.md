# TURNBOUND UI / UX Design System v1

> TURNBOUND 대격변 production UI 정본.
> 목표는 Minecraft 메뉴를 꾸미는 것이 아니라 3D 파티 RPG의 정보 구조를 Minecraft 안에 구현하는 것이다.

## 1. 우선순위

1. 읽기 쉬움
2. 현재 행동을 빠르게 찾음
3. 3D 캐릭터/전장을 가리지 않음
4. 입력 상태가 명확함
5. 외부 고품질 asset/reference 활용
6. GUI Scale 대응
7. 화면 간 일관성

## 2. 외부 UI 자산

후보:
- Foozle RPG UI Set 1 — CC0
- Kenney Fantasy UI Borders — CC0
- Kenney UI Pack/RPG Expansion — CC0
- Pretendard — OFL 1.1, runtime 검증 전

원칙:
- 화면마다 다른 pack 혼합 금지
- primary skin 1개를 먼저 결정
- 필요한 조각만 import
- SOURCE/LICENSE 기록
- texture path를 Java 화면마다 하드코딩하지 않음

## 3. Typography

- 한국어 가독성 최우선
- 작은 크기에서 획이 뭉개지지 않음
- title/body/numeric hierarchy
- 긴 설명 상시 노출 금지
- 자동 말줄임표는 짧은 label에만
- 숫자는 정렬/비교가 쉬움

## 4. Layout token

기본 spacing:
- XS 4
- S 8
- M 12
- L 16
- XL 24

실제 외부 9-slice grid가 있으면 pack grid를 우선하고 전 화면에 일관 적용.

## 5. Battle HUD

### 상단 — Turn Order
- portrait token
- 다음 6~10 행동
- 현재 actor 강조
- 연속 행동 portrait 반복
- Gauge 변화 즉시 재정렬

### 하단 좌측 — Party
각 캐릭터:
- portrait
- HP
- 최소 상태 icon
- downed
- selected/current actor

상태 text를 여러 줄 쓰지 않는다.

### 하단 우측 — Actions
- Basic
- Active A
- Active B
- 필요 시 특별 action

각 버튼:
- icon
- name
- cooldown
- target hint

### 보조 strip
- AUTO
- 1x / 2x
- flee

전투 action보다 시각 우선순위 낮게.

## 6. Targeting

Primary:
- 실제 3D model click

Fallback:
- keyboard/tab
- HUD portrait/token

표현:
- world outline/marker
- HUD target marker
- same target ID

single target skill은 첫 대상을 자동 확정하지 않는다.

## 7. Character-specific HUD

화면을 새 게이지로 가득 채우지 않는다.

- P01 Focus: 현재 target 표식 + 0~3 작은 stack
- P02: 별도 resource 없음, Turn Order 변화 자체가 정보
- P03 Guard: portrait 주변 0~100 compact meter
- P04 Sanctuary: 해당 ally portrait에 mark
- P05 Shot: 0~2 ammo indicator + target Sightline
- P06 Records: 0~5 작은 glyph
- P07 Bond: 0~100 compact meter + partner HP
- P08 Fury: 0~100 compact meter

## 8. Party screen

목표:
- 4명 편성
- 교체
- 역할 확인
- 장비 확인

구조:
- 좌측: roster portrait grid/list
- 중앙: 선택 캐릭터 3D model 또는 큰 portrait
- 우측: 역할/레벨/핵심 mechanic/장비
- 하단 또는 tab: skill detail

같은 정보를 카드 3개에 반복하지 않는다.

## 9. Character detail

첫 화면에서 보여줄 것:
- 이름
- rarity
- role
- level
- HP/ATK/DEF/SPD
- signature mechanic 한 문장
- 장비

두 번째 계층:
- skill 정확한 수치
- passive
- awakening
- lore

## 10. Equipment

한 화면:
- Weapon
- Armor
- Accessory
- Signature

강화:
- 현재 stat
- 강화 후 stat
- Gold cost
- 확정 버튼

실패 확률/복잡한 재료 list 없음.

## 11. Summon UI

### 메타 화면
- 보유 Crystal
- 1회 / 10회
- pity progress
- 현재 unlock pool
- 확률 상세

### 3D reveal
- 월드와 분리된 안전한 presentation layer 또는 별도 staging
- 실제 character model
- rarity별 light/camera/SFX
- 고유 pose
- skip 가능

### 결과
10회 결과만 compact grid.
중복은 “중복”만 쓰지 않고 Star Essence 획득을 명확히 표시.

## 12. Portrait system

`PortraitId = CharacterId`

상태:
- normal
- selected
- current actor
- target
- downed
- unavailable

색만으로 상태를 구분하지 않는다.

## 13. Minimap

기본 위치는 실제 HUD 충돌 검토 후 결정하지만, 전투 HUD와 겹치지 않아야 한다.

표시:
- road/terrain
- player
- party member
- discovered landmark
- active objective
- discovered danger
- known service

적 개체를 레이더처럼 전부 표시하지 않는다.

## 14. World map

filter:
- quest
- travel
- shop/service
- dungeon
- boss
- character event

미발견 content 기본 숨김.

## 15. Dialogue

- portrait가 있으면 좌/우 작은 portrait
- speaker name
- 2~4줄 이내 본문
- 선택지는 충분한 hitbox
- 모든 대화를 full-screen black panel로 만들지 않음

## 16. Tutorial prompt

- 짧은 1문장
- 실제 해당 UI 근처
- 행동이 끝나면 사라짐
- 이미 완료한 설명 반복 금지
- 도움말에서 재확인 가능

## 17. Feedback

- click: 짧은 visual + SFX
- disabled: 이유 즉시 표시
- error: 짧은 문구
- success: icon/animation
- 큰 보상: 별도 reward presentation
- chat에 숫자만 연속 출력하지 않음

## 18. 개발자 문구 차단

Normal gameplay UI/chat에는:
- internal ID
- 개발 단계명
- debug/log
- raw exception
- 구현 용어
를 표시하지 않는다.

operator command도 가능하면 사람이 읽는 운영 문구로 변환.

## 19. 경로

```
assets/turnbound/ui/
  common/
  battle/
  party/
  character/
  equipment/
  map/
  summon/
  dialogue/
  icons/
  portraits/
  fonts/
```

깊은 화면별 중첩 폴더를 만들지 않는다.

## 20. 금지

- 검은 반투명 사각형 남발
- 모든 정보 카드화
- arbitrary neon/glow
- 무계획 gradient
- 화면마다 다른 padding
- 작은 글씨로 억지 해결
- 말줄임표 남발
- fixed 1920×1080 좌표
- Minecraft 기본 버튼 나열
- 외부 pack 무계획 혼합
- UI가 캐릭터보다 더 눈에 띄는 구성

## 21. 검수

compile이 UI 완료가 아니다.

실제 client에서:
- 16:9
- 16:10
- 4:3
- 여러 GUI Scale
을 확인한다.

검수:
- text clip
- overlap
- hitbox
- portrait quality
- tooltip
- target marker
- turn order update
- camera/HUD collision
- minimap density
- controller/keyboard/mouse 입력
