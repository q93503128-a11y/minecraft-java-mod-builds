# 26 — M6 WORLD ASSET GATE — HUB_01 / REGION_01

이 문서는 `HUB_01 -> REGION_01` 기능 slice를 production authored 환경으로 승격하기 위한 **외부 reference / palette / scale / readability gate**다.

아직 실제 Minecraft screenshot 비교와 사용자 통합 playtest를 하지 않았으므로 이 문서는 **REFERENCE + PROTOTYPE GATE**이지 최종 visual PASS가 아니다.

---

## 1. 게임플레이 목적

첫 월드 slice는 다음 루프를 한 공간에서 자연스럽게 이해시키는 것이 목적이다.

`Hub 준비 -> 실제 채광/농사/낚시 -> 장비/준비물 선택 -> 보이는 Encounter -> 턴제 전투 -> 보상 -> Hub 귀환`

따라서 환경은 단순 배경이 아니라 다음을 직접 설명해야 한다.

- Hub에서 무엇을 준비할 수 있는지 멀리서도 읽힌다.
- 동쪽 주동선 하나를 잃지 않는다.
- 채광/농사/낚시는 짧은 분기로 갈라져 선택처럼 보인다.
- patrol은 첫 전투 landmark다.
- rift elite는 patrol보다 강한 다음 단계임을 외형만으로 구분한다.
- 플레이어가 UI 메뉴를 순회하지 않아도 실제 월드 이동으로 시스템 관계를 학습한다.

---

## 2. Reference set

### W-001 — Minecraft Legends: Well of Fate / village discovery

공식:
- https://www.minecraft.net/en-us/article/the-overworld-minecraft-legends
- https://www.minecraft.net/en-us/article/minecraft-legends-campaign-tips

관찰:
- Well of Fate는 멀리서도 읽히는 중심 구조이면서 성장 기능으로 돌아오게 만드는 home base다.
- 발견한 village는 fountain이라는 명확한 landmark를 통해 이동 거점으로 기능한다.
- resource가 월드의 서로 다른 환경에서 보이고, 탐험과 준비를 연결한다.

채택:
- HUB_01에 **하나의 강한 기능 landmark**를 둔다. 첫 slice에서는 forge hall이 그 역할을 한다.
- future fast travel도 별도 추상 메뉴가 아니라 발견 가능한 월드 landmark와 연결한다.
- Region resource site는 주동선에서 존재를 짐작할 수 있고 짧은 분기로 접근한다.

금지:
- Well of Fate의 실루엣/배치/색/기하를 복제하지 않는다.
- Minecraft Legends의 고유 구조물 asset을 가져오지 않는다.

사용 분류: **reference only / proprietary**.

### W-002 — Minecraft Dungeons Camp / Blacksmith

공식:
- https://www.minecraft.net/en-us/article/new-dungeons-dlc-and-more-september-8
- https://www.minecraft.net/en-us/article/creeping-winter-here

관찰:
- 월드에서 구한/구출한 기능이 camp에 실제 merchant/station으로 자리 잡아 귀환 이유가 된다.
- Blacksmith가 gear upgrade 기능을 물리적 camp 공간 안에 둔다.

채택:
- TURNBOUND 장비 제작/강화는 Character UI만으로 끝나는 기능이 아니라 **Hub forge 접근 상태**와 계속 연결한다.
- forge hall 안에서 Smithing Table / Furnace / Crafting / Anvil 계열의 의미를 한 덩어리로 읽게 한다.
- Hub는 기능을 늘릴 때 무작정 퍼지지 않고 실제 귀환 동기가 있는 station만 추가한다.

금지:
- Dungeons camp geometry/merchant 배치/장식 복제.
- Dungeons proprietary asset 사용.

사용 분류: **reference only / proprietary**.

### W-003 — MineColonies style packs: Medieval Oak / Medieval Spruce

공식 gallery:
- https://minecolonies.com/schematics/

공개 schematic repository:
- https://github.com/ldtteam/minecolonies-schematics
- repository license at review time: GPL-3.0.

관찰:
- 같은 settlement 안에서 건물별 기능은 달라도 반복되는 재료/지붕/기초 언어가 전체 장소를 하나로 묶는다.
- Medieval Oak / Medieval Spruce 같은 style pack은 건물 단위의 독립 장식보다 settlement 전체의 일관된 family를 우선한다.
- schematic gallery가 footprint와 style 비교에 유용하다.

채택:
- HUB_01은 `stone/tuff foundation + spruce timber/roof + restrained lantern/copper-like accent` 한 family를 반복한다.
- REGION_01은 같은 timber/stone 계열을 유지하되 자연 재료 비중을 높여 Hub와 연결감은 남기고 인공 밀도는 낮춘다.
- 구조물 높이보다 **반복 material family + 기능별 silhouette**로 장소 정체성을 만든다.

현재 사용:
- direct schematic import 없음.
- block-by-block 복제 없음.
- gallery와 공개 repository는 reference로만 사용.

직접 asset 사용을 나중에 검토할 경우 GPL 의무, 원본 author, Minecraft version 호환, 수정/배포 조건을 별도로 다시 확인하고 `THIRD_PARTY_ASSETS.md`를 갱신한다.

사용 분류: **reference only now / GPL-3.0 repository screened, no copied asset**.

### Screening exclusion — Terralith

조사 중 Terralith repository license가 해당 프로젝트의 code/textures/other products를 AI/generative system의 configure/test/debug/augment 용도로 사용하는 것을 금지한다고 명시하는 것을 확인했다.

따라서 이번 gate의 디자인 reference, palette source, code source, asset source에서 **Terralith를 제외**한다.

---

## 3. 선택한 장소 정체성

### HUB_01 — `Wayfarer Forge Court`

목표 감정:
- 안전한 귀환 지점.
- 다음 원정을 준비하는 작업 공간.
- 왕궁/대도시처럼 과장되지 않은 첫 RPG 거점.

핵심 silhouette:
- 낮고 넓은 forge hall.
- 한쪽으로 솟는 chimney.
- 정면에서 바로 보이는 open workshop.
- 동쪽 exit가 건물/벽에 가려지지 않음.

주요 palette:
- foundation: Stone Bricks / Polished Tuff / Tuff Bricks.
- structure: Spruce Log / Spruce Planks / Spruce Slab.
- workstation: Smithing Table / Blast Furnace / Furnace / Crafting Table / Anvil / Grindstone.
- lighting: Lantern.
- storage: Barrel / Chest.

금지:
- black translucent UI 같은 방식으로 월드도 임시 사각형 덩어리만 세우는 것.
- 건물마다 전혀 다른 목재/석재 family 사용.
- 첫 Hub부터 거대한 성/도시로 규모를 부풀리는 것.

### REGION_01 — `Riverward Foothill`

목표 감정:
- Hub 바깥 첫 생활권.
- 위험해지기 전 실제 자원을 확보하는 짧은 탐사 지역.
- main road를 기준으로 선택 가능한 생활 분기.

palette family:
- road: Coarse Dirt / Gravel / Mossy stone accent.
- quarry: Stone / Tuff / Andesite / Spruce support.
- farm: Grass / Farmland / Spruce Fence / Hay / Composter.
- river: Grass / Gravel / Sand / Spruce dock.
- patrol ruin: Mossy Cobblestone / Cracked & Mossy Stone Bricks.
- rift elite: Deepslate Tiles / Polished Deepslate / Obsidian / Crying Obsidian / Amethyst / Soul Campfire.

Rift palette는 일반 지역보다 어둡고 차갑지만, particle spam이나 거대한 Display Entity 없이도 threat tier를 읽게 하는 것이 1차 목표다.

---

## 4. Scale / sightline contract

### Hub
- footprint: 약 `23 x 19`.
- forge hall은 Hub 중앙에 위치.
- east gate는 `x ~= +10`.
- Smithing Table은 forge 중심부에 유지하여 기존 4-block workstation gate와 실제 동선이 맞는다.
- Hub 내부에서 east road 시작이 가려지지 않아야 한다.

### Main route
- width: 5 blocks (`center 3 + edge 2`).
- first slice 끝까지 대략 78 blocks.
- light/vertical cue cadence: 약 14 blocks.
- resource branch 때문에 main road가 끊기지 않는다.

### Resource branches
- mine center: `(42, -12)`.
- farm center: `(42, +12)`.
- river center: `(60, +15)`.
- main route와 각 site footprint 사이 최소 2 blocks의 visual breathing room.
- 너무 멀리 떨어져 별도 지역처럼 느껴지지 않도록 main route edge와 약 8 blocks 이내.

### Encounter escalation
- patrol center: `(52, 0)`.
- elite center: `(72, 0)`.
- patrol과 elite 사이 최소 16 blocks approach.
- resource branch를 본 뒤 patrol이 나오고, patrol 이후 별도 approach를 거쳐 elite가 나온다.

위 좌표는 `RegionDefinition` data가 아니라 **현재 authored prototype의 physical layout contract**다. gameplay identity는 계속 locator가 소유한다.

---

## 5. Resource yield intent

첫 production prototype은 기능만 확인하던 노출 광석 몇 개에서 벗어나 실제 선택이 가능한 최소 yield를 제공한다.

quarry exposed ore target:
- Coal Ore: 8.
- Copper Ore: 10.
- Iron Ore: 8.
- Gold Ore: 4.

의도:
- 한 번의 outcrop 방문으로 모든 장비를 Lv3까지 끝내지 않는다.
- 하지만 Iron/Copper/Gold 대표 Lv1 장비 중 하나 이상을 실제로 제작할 수 있는 선택지는 제공한다.
- Gold는 Golden Carrot preparation과 Golden Heart 장비가 같은 실물 자원을 놓고 경쟁하게 한다.

정확한 field yield / 강화 비용 / 채집 시간은 통합 playtest에서 측정해 조정한다.

---

## 6. Production-facing prototype implementation

새 구현:
- `ProductionWorldSlicePlan`
  - footprint, route width, landmark cadence, branch separation, encounter escalation을 pure contract로 검증.
- `ProductionWorldSlicePrototypeBuilder`
  - 기존 locator/data를 그대로 사용.
  - 기능 slice보다 실제 장소처럼 읽히는 Hub forge / road / quarry / farm / river / patrol ruin / rift landmark 배치.
  - 실제 ore/crop/water/workstation을 유지.
  - Encounter Interaction entity는 기존 server-authoritative anchor tag를 사용.
- operator command:
  - `/turnbound_re_world_slice prototype`

기존 `/turnbound_re_world_slice build`는 비교 가능한 **기능 harness**로 남긴다.

---

## 7. 현재 gate 판정

### 통과
- [x] gameplay 목적.
- [x] 이동/시야/전투/채집 동선 원칙.
- [x] 외부 게임/건축 reference set.
- [x] reference 사용 분류와 직접 복제 금지.
- [x] block palette 후보.
- [x] 규모/거리/landmark cadence 기준.
- [x] code-level authored prototype.
- [x] locator / server-authority 보존.

### 아직 미통과
- [ ] 실제 Minecraft screenshot side-by-side audit.
- [ ] GUI와 월드가 동시에 보일 때 시각 밀도 확인.
- [ ] 이동 시간 / 채광량 / crop 수확량 / fishing 체류 시간 측정.
- [ ] forge hall 내부에서 장비 UI 사용성 확인.
- [ ] patrol/rift landmark가 실제 거리에서 전투 위험도로 읽히는지 확인.
- [ ] chunk/entity/block update 성능 확인.
- [ ] 최종 structure/worldgen 배치 방식 결정.

따라서 상태는 **REFERENCE GATE PASS / PROTOTYPE IMPLEMENTED / PRODUCTION VISUAL PASS PENDING**이다.

사용자 방침에 따라 이 단계만 따로 수동 테스트를 요구하지 않는다. fast travel/discovery의 최소 hook과 production non-repeatable encounter까지 더 닫은 뒤 통합 screenshot/playtest에서 함께 검증한다.
