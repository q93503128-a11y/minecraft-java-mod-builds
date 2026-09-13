# 08 — REFERENCE CATALOG

이 문서는 복제 목록이 아니라 **어떤 문제를 어떤 기존 사례에서 배웠는지** 기록한다.

상세 M5 UI 비교/정보계층/토큰/목업 정본은 `17_M5_UI_VISUAL_GATE.md`가 담당한다.

## R-001 Persona 5 Royal
- 공식: https://persona.atlus.com/p5r/
- 관찰: 적 약점 공략이 즉각적인 전투 템포 보상으로 이어지고, 현재 actor의 행동 선택이 강하게 구분된다.
- 채택: 현재 actor/action 선택을 명확하게 보여주는 정보 우선순위.
- TURNBOUND: RE 변환: WEAK가 Poise 피해와 EXPOSED 진입에 연결된다.
- 금지: Persona 고유 red/black composition, 만화 컷, command graphic을 복제하지 않는다.

## R-002 OCTOPATH TRAVELER II
- 공식: https://www.square-enix-games.com/games/octopath-traveler-ii
- 관찰: turn order, 약점, Break 계열 정보, party HP/SP/BP를 서로 다른 위치에 두면서도 한 전투 화면에서 읽을 수 있다.
- 채택: `준비 → 방어 붕괴 → 공격 창`의 리듬을 UI에서도 즉시 읽게 한다.
- TURNBOUND: RE 변환: Poise/EXPOSED + Energy를 사용한다.
- 금지: 원작 Shield/BP 아이콘과 레이아웃 복제.

## R-003 Clair Obscur: Expedition 33
- 공식: https://www.expedition33.com/overview
- 관찰: turn-based의 계획성과 3D 현장감을 같이 유지한다.
- 채택: UI가 battlefield를 대형 panel로 가리지 않는다.
- TURNBOUND: RE 변환: 초기에는 Enemy Intent로 적 차례 전 대응 결정을 만든다. QTE/parry는 핵심 UI 계약에 넣지 않는다.

## R-004 TurnBasedMinecraftMod
- 저장소: https://github.com/Stephen-Seo/TurnBasedMinecraftMod
- branch: `neoforge`
- license: MIT (실제 코드 사용 전 다시 확인)
- 관찰: Minecraft에서 battle/combatant, GUI, network 책임을 분리한 실제 구현 사례다.
- 채택: Minecraft world adapter와 battle state/network/presentation 분리.
- 현재 실제 코드 복사: 없음.

## R-005 Honkai: Star Rail
- UI catalog: https://interfaceingame.com/games/honkai-star-rail
- combat UI 설명: https://www.hoyolab.com/article/17984000
- 관찰: 좌측 세로 Action Order가 SPD 기반 전투 계획의 상시 기준점이다. 현재 action control은 별도 영역에 집중한다.
- 채택: TURNBOUND: RE turn queue를 작은 좌측 rail로 상시 노출.
- 금지: 원작 mobile/gacha portrait, 아이콘, 색, layout 복제.

## R-006 Metaphor: ReFantazio
- 공식: https://metaphor.atlus.com/
- 관찰: 강한 command hierarchy를 쓰면서도 전투 장면 자체를 계속 보여준다.
- 채택: command 영역은 한곳에 모으고 current selection만 강하게 강조.
- 금지: brush typography, red/white proprietary visual identity 복제.

## R-007 Slay the Spire
- UI catalog: https://interfaceingame.com/games/slay-the-spire/
- 관찰: Enemy Intent를 적 위에 아이콘/수치로 항상 보여줘 다음 판단을 짧게 만든다.
- 채택: TURNBOUND Intent는 적 상태의 P0 정보다. 긴 설명은 hover/inspect로 이동.
- 금지: card combat layout 자체를 가져오지 않는다.

## R-008 Darkest Dungeon II
- 공식: https://www.darkestdungeon.com/darkest-dungeon-2/
- 관찰: 4인 party 상태와 선택 action tray를 하단에 응축하고 선택 actor를 강하게 구분한다.
- 채택: party status와 command를 하단 edge에서 분리 배치.
- 금지: 고딕 frame/장식/token 복제.

## R-009 Pokémon Scarlet/Violet
- 공식: https://scarletviolet.pokemon.com/
- 관찰: 기본 command에서 move 선택으로 단계가 짧고, move row에서 effectiveness를 함께 읽을 수 있다.
- 채택: Basic/Skill/Guard/Burst → 필요한 경우 target 단계로 이어지는 짧은 흐름.
- 금지: Pokémon 고유 메뉴/아이콘/연출 복제.

## R-010 Into the Breach
- UI catalog: https://interfaceingame.com/games/into-the-breach
- 관찰: 장식보다 행동 결과/전술 예측을 우선하며 세부 정보는 inspect로 보낸다.
- 채택: TURNBOUND 전투 HUD도 결과 판단을 장식보다 우선한다.

## R-011 Cobblemon
- 공식: https://www.cobblemon.com/
- 관찰: Minecraft world 안에서 party/battle UI를 작은 overlay와 별도 summary screen으로 나눈다.
- 채택: `Minecraft-native tactical overlay`, world-first battle HUD, 별도 party/character screen.
- 현재 실제 코드/자산 복사: 없음.

## R-012 Cobblemon Extended Battle UI
- Modrinth: https://modrinth.com/mod/cobblemon-extended-battle-ui
- source: https://github.com/sveniik/CobblemonExtendedBattleUI
- license: MIT (2026-09-07 재확인)
- 관찰: battle info/log를 collapsible하게 만들고, 상대 정보도 이미 확실히 알려진 것만 표시한다. native texture 계열을 유지한다.
- 채택: progressive disclosure, revealed-information only, collapsible battle log.
- 현재 실제 코드/자산 복사: 없음.

## R-013 FTB Quests UI Overhaul / FTB 계열 UI
- Modrinth: https://modrinth.com/mod/ftb-quests-ui-overhaul
- 관찰: Minecraft pixel/vanilla 계열 visual language, hover state, 간결한 navigation, resource-pack 교체 가능성을 강조한다.
- 채택: nine-slice/pixel sprite 기반 component family, state 일관성.
- 현재 실제 코드/자산 복사: 없음.

## R-014 Questify
- Modrinth: https://modrinth.com/mod/questify
- 관찰: 큰 데이터 화면에서 chapter/filter/graph/detail을 역할별 영역으로 분리한다.
- 채택: 비전투 화면에서 roster/active party/detail을 한 화면에 역할별로 분리하는 원리.

## R-015 Pokémon held item
- 공식 가이드: https://diamondpearl.pokemon.com/en-gb/trainersguide/fundamentals/battling/
- 관찰: 한 캐릭터가 한 개의 held item을 선택하고, 캐릭터 자체 성장과 별도로 전투 성격을 조정한다.
- 채택: 첫 장비 Vertical Slice는 캐릭터당 장비 1슬롯만 사용해 선택을 읽기 쉽게 유지한다.
- TURNBOUND: RE 변환: 장비는 고정된 작은 HP/ATK/DEF/POISE 보정만 주고 캐릭터 kit/originStar 정체성을 대체하지 않는다.
- 금지: Pokémon 고유 아이템/수치/UI/아트를 복제하지 않는다.

## R-016 Minecraft Smithing Table
- 공식: https://www.minecraft.net/en-us/article/minecraft-snapshot-23w04a
- 관찰: 장비 변경/강화를 별도 추상 통화가 아니라 실제 장비와 Minecraft 소재가 만나는 물리 workstation으로 표현한다.
- 채택: TURNBOUND 장비 제작/강화도 Coin + 실제 Minecraft material을 서버에서 소비하는 물리적 Hub 흐름을 목표로 한다.
- 현재 단계: inventory transaction, server-authored Equipment UI, Smithing Table proximity gate까지 연결됨. production Hub forge 외형은 M6 World Asset Gate에서 감싼다.

## R-017 Minecraft Legends — Well of Fate / Village landmarks
- 공식: https://www.minecraft.net/en-us/article/the-overworld-minecraft-legends
- 공식 campaign tips: https://www.minecraft.net/en-us/article/minecraft-legends-campaign-tips
- 관찰: Well of Fate는 성장 기능이 모이는 강한 home landmark이고, 발견한 village의 fountain은 fast travel과 지역 상태를 공간적으로 표현한다.
- 채택: HUB_01은 한 개의 명확한 기능 landmark를 중심으로 읽히게 하고, future fast travel도 발견 가능한 월드 landmark와 연결한다. Region resource는 main route에서 짧은 분기로 읽히게 한다.
- 금지: Well of Fate geometry/color/asset 복제.
- 사용 분류: reference only / proprietary.

## R-018 Minecraft Dungeons — Camp / Blacksmith
- 공식: https://www.minecraft.net/en-us/article/new-dungeons-dlc-and-more-september-8
- 공식: https://www.minecraft.net/en-us/article/creeping-winter-here
- 관찰: 월드에서 확보한 기능이 camp의 merchant/station으로 자리 잡고, Blacksmith가 gear upgrade의 물리적 귀환 지점이 된다.
- 채택: TURNBOUND forge는 Character UI만의 버튼이 아니라 Hub의 실제 forge hall과 계속 결합한다. Hub station은 실제 귀환 동기가 있는 기능만 추가한다.
- 금지: Camp 배치/상점/asset 복제.
- 사용 분류: reference only / proprietary.

## R-019 MineColonies — Medieval Oak / Medieval Spruce style family
- gallery: https://minecolonies.com/schematics/
- schematic repository: https://github.com/ldtteam/minecolonies-schematics
- repository license: GPL-3.0 (2026-09-13 review; direct use 전 다시 확인)
- 관찰: 건물 기능이 달라도 반복 material/roof/foundation language를 유지해 settlement 전체를 한 장소로 읽게 한다.
- 채택: HUB_01은 stone/tuff foundation + spruce timber/roof family를 반복하고, REGION_01은 같은 family를 자연 재료 비중이 높은 형태로 낮춰 연결감을 유지한다.
- 현재 실제 schematic/asset 복사: 없음. reference only.
- 직접 사용 시 author/version/GPL 의무와 배포 조건을 다시 검토하고 `THIRD_PARTY_ASSETS.md` 기록이 필요하다.

## UI 구현 기술 reference
- NeoForge Screens: https://docs.neoforged.net/docs/rendering/screens/
- 채택: GUI-scale relative layout, `blitSprite`, `nine_slice`, scissor, tooltip/input.
- 첫 production pass는 Vanilla/NeoForge Screen을 사용한다. 현재 복잡도에서 대형 UI dependency를 추가하지 않는다.

## 참고 원칙
- 시스템 이름/수치/콘텐츠/화면을 통째로 복제하지 않는다.
- proprietary 게임은 **구조/정보계층/상태 피드백 원리**만 참고한다.
- 오픈소스 코드/자산을 실제 사용하면 라이선스와 파일 단위 출처를 다시 확인하고 `THIRD_PARTY_ASSETS.md`에 기록한다.
- '재미있어 보임'이 아니라 문제 → 관찰 → 채택 원리 → TURNBOUND: RE 변환을 기록한다.

## Visual Reference Gate status

### M5 UI — PASS for implementation start
- [x] Battle HUD reference set: 상용 게임 8개 이상 비교.
- [x] Character/party screen reference set: HSR/Cobblemon/Persona/FTB 계열 포함 비교.
- [x] Minecraft 구현 사례: Cobblemon, Extended Battle UI, TurnBasedMinecraftMod, FTB 계열.
- [x] Minecraft 26.2/NeoForge Screen feasibility.
- [x] Battle/Party information hierarchy.
- [x] UI design token contract.
- [x] Battle HUD structural mockup.
- [x] Party Formation structural mockup.
- [x] 상세 정본: `17_M5_UI_VISUAL_GATE.md`.

### M6 World architecture — REFERENCE GATE PASS / SCREENSHOT GATE PENDING
- [x] Hub home-landmark reference: Minecraft Legends.
- [x] Hub physical progression-station reference: Minecraft Dungeons Camp.
- [x] Minecraft settlement material-family reference: MineColonies styles.
- [x] HUB_01 / REGION_01 palette and scale contract: `26_M6_WORLD_ASSET_GATE.md`.
- [x] Production-facing authored prototype implementation.
- [ ] actual Minecraft screenshot side-by-side audit.
- [ ] movement/yield/readability/performance playtest.
- [ ] final structure/worldgen placement lock.

### 별도 후속 visual gate
- [ ] 실제 production sprite/icon asset 선정·검수.
- [ ] 구현 후 Minecraft screenshot side-by-side audit.
- [x] World architecture/reference set — M6 reference stage.
- [ ] Skill/VFX reference language — 해당 presentation 단계 전에 수행.
- [ ] 캐릭터 고유 외형 reference — roster production pass 전에 수행.
