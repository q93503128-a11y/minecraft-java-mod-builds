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

Primary production skin:
- **Foozle RPG UI Set 1 — CC0**
- ornate dark-fantasy panel/button/orb artwork를 management/map UI의 실제 화면 베이스로 사용한다.
- Minecraft 기본 버튼을 외부 테두리만 씌워 재현하는 방식은 금지한다.
- 캐릭터 profile은 Foozle ornament frame 안의 큰 live 3D bust를 사용한다. 28~36px짜리 잘린 작은 얼굴을 production profile로 쓰지 않는다.

Secondary/reference only:
- Kenney Fantasy UI Borders — CC0
- Kenney UI Pack/RPG Expansion — CC0
- Pretendard — OFL 1.1, runtime 검증 전

원칙:
- 같은 화면에서 서로 다른 pack의 장식을 섞지 않는다.
- primary skin은 Foozle로 고정한다.
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

## 5. 화면 이동 구조 — 편의성 우선

TURNBOUND의 메뉴 이동은 화면 수를 늘리는 방식이 아니라 **자주 하는 행동을 적은 단계로 끝내는 것**을 기준으로 설계한다.

### 참고하는 상용 게임의 해결 방식

- **Pokémon Scarlet / Violet**: 메인 메뉴를 열었을 때 현재 파티를 메뉴와 동시에 보여주고, 파티원을 별도 깊은 메뉴에 숨기지 않는다.
- **Honkai: Star Rail**: 한 캐릭터를 선택한 뒤 캐릭터 문맥을 유지한 채 상세/장비/성장 계열 화면을 옮긴다. 캐릭터를 바꿀 때 루트 메뉴까지 되돌아갈 필요가 없다.
- **OCTOPATH TRAVELER II**: 장비 선택 중 현재 수치와 변경 후 수치를 같은 화면에서 비교하게 하여 장착→뒤로가기→상태 확인 같은 왕복을 줄인다.

이 게임들의 화면을 복제하지 않는다. **파티가 항상 보이고, 현재 대상 문맥을 유지하며, 비교 정보를 같은 화면에 둔다는 UX 원리**만 TURNBOUND에 적용한다.

### 5.1 Root quick menu

필드에서 RPG 메뉴를 열면 첫 화면은 복잡한 대시보드가 아니라 다음 두 영역만 가진다.

- 좌측: 현재 파티 4명 portrait + HP/레벨/역할의 최소 정보
- 우측: 자주 쓰는 4개 관리 진입점
  - Party
  - Equipment
  - Quests
  - Summon
- Map은 필드의 `M` 직접 단축키가 정본이다. Root menu에 중복 버튼을 두지 않는다.

현재 파티 portrait를 선택하면 바로 해당 캐릭터 상세로 간다.
Characters라는 별도 최상위 메뉴를 하나 더 만들지 않는다. 전체 roster는 Party 화면에서 접근한다.

### 5.2 Character detail

캐릭터 상세는 최대 네 계층만 사용한다.

1. Overview — 역할, 핵심 스탯, signature mechanic, 현재 장비
2. Skills — Basic / Active / Passive의 정확한 수치
3. Equipment — 같은 캐릭터의 장비 교체/강화
4. Growth — Level / Awakening 및 필요한 비용

Profile/lore는 Overview의 보조 정보로 두고 별도 최상위 탭을 만들지 않는다.
Awakening도 별도 관리 메뉴로 분리하지 않는다.

캐릭터 상세/장비/성장 화면에서는 **다른 캐릭터로 바로 전환**할 수 있어야 한다. 캐릭터 하나를 확인할 때마다 Party 화면으로 되돌아가는 흐름은 금지한다.

### 5.3 Party

한 화면에서:
- 현재 4 slots
- 전체 보유 roster
- 선택 캐릭터의 역할/핵심 mechanic
- 현재 장비 요약

을 같이 본다.

편성은 drag/drop 또는 slot 선택 → 캐릭터 선택 두 방식 중 하나로 처리한다.
편성 변경은 되돌릴 수 있으므로 매 교체마다 확인창을 띄우지 않는다.

### 5.4 Equipment

장비 화면은 캐릭터와 슬롯을 동시에 유지한다.

- 캐릭터 전환을 위해 화면을 닫지 않는다.
- Weapon / Armor / Accessory / Signature를 한 화면에서 전환한다.
- item highlight 시 현재 수치 → 장착 후 수치 delta를 즉시 표시한다.
- 장착은 확인창 없이 적용 가능하다.
- Gold를 실제 소비하는 강화만 최종 확인을 사용한다.

캐릭터 → 장비 → 슬롯 → 아이템 → 뒤로 → 스탯 확인 → 다시 장비 같은 왕복 흐름은 실패로 본다.

### 5.5 Map / Quest

Map과 Quest는 서로 다른 데이터 복사본을 만들지 않는다.

- Map에서 활성 Quest를 선택하면 해당 objective/region을 즉시 focus한다.
- Quest 목록에서 지도에서 보기를 누르면 같은 Map 화면의 해당 위치로 이동한다.
- 지도에서 발견한 landmark / fast travel / danger marker는 Quest UI와 같은 world marker data를 읽는다.

Quest를 보기 위해 Map을 닫고 또 다른 3단계 메뉴를 거치는 구조를 만들지 않는다.

### 5.6 Back / context preservation

- Esc/Back: 항상 **정확히 한 단계 이전 화면**으로 간다.
- Root quick menu에서만 Back이 메뉴 전체를 닫는다.
- 이전 화면으로 돌아오면 선택 캐릭터, 선택 슬롯, 목록 scroll, filter, tab을 복원한다.
- 화면 전환 때마다 첫 캐릭터/첫 아이템으로 focus를 초기화하지 않는다.
- modal은 한 번에 하나만 띄운다.
- irreversible action 또는 실제 재화 소비가 아니면 확인창을 만들지 않는다.

### 5.7 Routine-task path budget

일상적으로 반복하는 행동은 아래 상한을 목표로 한다. 메뉴 열기 자체는 단계 수에서 제외한다.

| 행동 | 목표 최대 단계 |
|---|---:|
| 현재 파티 캐릭터 상세 보기 | 1 |
| 캐릭터 간 전환 | 1 |
| 파티원 한 명 교체 | 3 |
| 장비 슬롯 확인/교체 | 3 |
| 장비 강화 진입 | 3 |
| 활성 퀘스트 위치를 지도에서 보기 | 2 |
| 소환 화면 진입 | 1 |

이 상한을 넘기면 기능을 더 추가하기 전에 navigation 구조를 다시 검토한다.

### 5.8 Cross-link rule

관련 화면 사이에는 직접 연결을 둔다.

- Party → 선택 캐릭터 Detail
- Character Detail → 동일 캐릭터 Equipment
- Equipment → 동일 캐릭터 Growth/Detail 복귀
- Quest → 해당 Map focus
- Battle Result → 획득 장비/성장 대상 확인

다만 같은 기능을 여러 화면에서 서로 다른 코드로 구현하지 않는다. 모든 진입점은 같은 screen state/router를 사용한다.

### 5.9 금지되는 이동 구조

- Main → Characters → Manage → Equipment → Slot → Inventory처럼 routine action이 4~6단계 깊어지는 구조
- Back을 누르면 루트까지 한 번에 튕기는 구조
- 같은 캐릭터를 보는데 화면마다 다시 선택해야 하는 구조
- 정보 확인을 위해 여러 화면을 왕복해야 하는 구조
- 편의를 이유로 모든 기능을 Root menu와 별도 키 양쪽에 중복 노출하는 구조
- 개발자 관점의 시스템 모듈 구분을 그대로 플레이어 메뉴 구조로 노출하는 것

UI 파일/클래스 구조보다 **플레이어가 몇 번 눌러야 원하는 행동을 끝내는지**가 먼저다.

## 6. Battle HUD

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

## 7. Targeting

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

## 8. Character-specific HUD

화면을 새 게이지로 가득 채우지 않는다.

- P01 Focus: 현재 target 표식 + 0~3 작은 stack
- P02: 별도 resource 없음, Turn Order 변화 자체가 정보
- P03 Guard: portrait 주변 0~100 compact meter
- P04 Sanctuary: 해당 ally portrait에 mark
- P05 Shot: 0~2 ammo indicator + target Sightline
- P06 Records: 0~5 작은 glyph
- P07 Bond: 0~100 compact meter + partner HP
- P08 Fury: 0~100 compact meter

## 9. Party screen

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

## 10. Character detail

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

## 11. Equipment

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

## 12. Summon UI

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

## 13. Portrait system

`PortraitId = CharacterId`

상태:
- normal
- selected
- current actor
- target
- downed
- unavailable

색만으로 상태를 구분하지 않는다.

## 14. Minimap

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

## 15. World map

filter:
- quest
- travel
- shop/service
- dungeon
- boss
- character event

미발견 content 기본 숨김.

## 16. Dialogue

- portrait가 있으면 좌/우 작은 portrait
- speaker name
- 2~4줄 이내 본문
- 선택지는 충분한 hitbox
- 모든 대화를 full-screen black panel로 만들지 않음

## 17. Tutorial prompt

- 짧은 1문장
- 실제 해당 UI 근처
- 행동이 끝나면 사라짐
- 이미 완료한 설명 반복 금지
- 도움말에서 재확인 가능

## 18. Feedback

- click: 짧은 visual + SFX
- disabled: 이유 즉시 표시
- error: 짧은 문구
- success: icon/animation
- 큰 보상: 별도 reward presentation
- chat에 숫자만 연속 출력하지 않음

## 19. 개발자 문구 차단

Normal gameplay UI/chat에는:
- internal ID
- 개발 단계명
- debug/log
- raw exception
- 구현 용어
를 표시하지 않는다.

operator command도 가능하면 사람이 읽는 운영 문구로 변환.

## 20. 리소스 파일 경로

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

## 21. 금지

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

## 22. 검수

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
