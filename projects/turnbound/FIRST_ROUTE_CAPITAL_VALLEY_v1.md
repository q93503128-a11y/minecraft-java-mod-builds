# TURNBOUND First Route — Capital Valley / Drabyel v1

> 목적: TURNBOUND의 첫 30~60분을 Drehmal의 실제 초반 지형에 묶는 production planning 문서.
> 이 문서는 공식 Drehmal 위치 자료와 TURNBOUND 설계 판단을 구분한다.
> 원본 Drehmal 구조화 지도는 route/landmark/road 의미를 제공하고, 정확한 NPC·적·battle 위치는 bound Minecraft 26.2 월드에서 runtime이 자동 안전검사해 파생한다. 최종 client 검수는 좌표를 사람이 찍는 단계가 아니라 배치가 자연스러운지 확인하는 단계다.

## 0. Current opening override — 2026-09-29

Latest client-playtest decision supersedes both the original Primal-roadhead spawn and the temporary Explorer-camp spawn.

- Fresh TURNBOUND play now begins at the **north entrance of New Drabyel**, using the source-backed town-approach point around 502,1801 rather than the town center.
- The village is the narrative anchor from the first minute: the player arrives at its gate, speaks to the entrance guide and checks the current party.
- The first outdoor tutorial is a **short excursion**, not a journey to earn the town.
- After greeter dialogue + one E-menu party check, the objective points to CV_DRABYEL_ROAD outside the north gate.
- The approach encounter is moved close enough that its authored seed is under 100 m from the entrance seed, while remaining outside the hub safety ring.
- CV_DRABYEL_ROAD is a 2-enemy melee+ranged lesson (CV-B + CV-C). After victory the objective points back to New Drabyel and normal hub services continue.
- The older Explorer camp, Tower, Warning Cave, chapel and Primal roadhead remain real Capital Valley exploration content. They are optional/backward exploration and are never marked explored merely because the player started in New Drabyel.
- Travel distance is not a content goal. If the first combat cannot be seen/reached quickly in client playtest, move the beat within the same source-backed approach corridor rather than padding the route.

This override must stay consistent with ENCOUNTER_ENEMY_PLACEMENT_v1.md, UI_DESIGN_SYSTEM.md and PROJECT.md.

## 1. Source-backed geography

공식 Drehmal 자료에서 확인된 사실:

- 원본 맵의 실제 플레이 시작점은 **Stasis Facility** 내부다.
- 시설에서 지상으로 나오면 **Primal Caverns** 바로 옆 Capital Valley에 도달한다.
- 그 반대 방향의 기존 길은 버려진 예배당을 지나 **Capital Valley Tower**로 이어진다.
- 같은 길을 더 따라가면 **New Drabyel**로 이어지며, Drabyel은 대부분의 플레이어가 처음 발견하는 공식 마을로 설계되어 있다.
- Capital Valley Tower는 Stasis Facility와 Drabyel 사이의 길 바로 옆에 있다.
- 길 중간 남쪽에는 Explorer's Guide가 놓인 작은 캠프가 있다.
- Tower 남서쪽에는 여행자가 머무르지 말라고 경고하는 동굴이 실제로 존재한다.
- Drabyel은 작은 초기 거점으로 쓰기 좋은 농지/마구간/상인/대장장이 공간을 이미 가지고 있다.
- Av'Sal은 Drabyel에서 서쪽/남서쪽으로 이어지는 다음 대형 폐허 축으로 사용할 수 있다.

Official/source coordinates used only as survey anchors:

| Anchor | Approx. coordinate | Status |
|---|---:|---|
| Stasis Facility interior holo-door | 778,31,668 | source-backed |
| Primal Caverns | 855,65,553 | source-backed |
| Capital Valley Tower | 557,129,1176 | source-backed |
| Warning Cave | 454,73,1252 | source-backed |
| Explorer's Guide camp | 581,81,1501 | source-backed |
| New Drabyel | 502,67,1801 | source-backed |
| Av'Sal central island | -242,67,1532 | source-backed |

These are not automatically safe spawn/battle/NPC blocks.

## 2. TURNBOUND start-point decision

### Chosen topology

**Start at New Drabyel's source-backed north entrance, just outside/at the pedestrian gate rather than in the town center.**

Do not start:
- inside a random original Stasis pod,
- inside Drabyel town center,
- inside the Terminus,
- inside an authored dungeon,
- on an artificial flat platform.

### Why not the Stasis interior

The facility is a strong authored location, but its original puzzle and identity are tightly coupled to Drehmal's own story.

TURNBOUND should not force the player to inherit an unrelated original-map protagonist identity before its own party RPG loop starts.

The Stasis Facility remains:
- a nearby landmark,
- optional exploration/history space,
- possible later quest location,
not TURNBOUND's mandatory opening dungeon.

### Why not Drabyel town center

Starting inside the first hub removes:
- first world reveal,
- the feeling of following a road toward civilization,
- a clean place to teach movement and visible encounters,
- the contrast between wilderness and town.

Drabyel should feel like the first earned safe hub, not a menu room the player spawns inside.

### Why the New Drabyel gate works

It provides:
- an immediately understandable story anchor: a traveler arriving at the first settlement,
- the entrance guide as a natural first speaker instead of a tutorial kiosk,
- a safe place to check the party before combat,
- a short north-road excursion that teaches visible field enemies and turn combat without a long walk,
- a meaningful return-to-safety beat after the fight,
- backward exploration access to the camp/Tower/Warning Cave/Primal route without making that detour mandatory.

## 3. Exact spawn criteria

The exact block is **not yet frozen**.

During 26.2 world survey, choose a point satisfying all:

- existing road or shoulder, not a new platform
- 3×3 stable ground
- no water, leaves, fence, trapdoor, crop or moving block
- at least 3 blocks headroom
- no nearby cliff drop within accidental sprint range
- player camera can rotate 360° without entering terrain
- Primal Caverns or another strong landmark is visible
- the road toward Drabyel is readable within a few seconds
- the Stasis exit is not visually mistaken for the only path forward
- no original map trigger is activated merely by spawning
- party of up to 4 can be staged nearby
- multiplayer players have safe non-overlapping spawn offsets
- first tutorial prompt does not cover the landmark
- respawn does not place players inside an encounter aggro radius

Preferred spawn behavior:
- host starts at primary marker
- additional party members use nearby authored-safe offsets
- all players face roughly along the first road, but camera is never locked

## 4. First-route macro flow

```text
New Drabyel north gate arrival
→ entrance guide dialogue
→ E menu: current party check
→ short north-road patrol (CV_DRABYEL_ROAD)
→ return to New Drabyel
→ blacksmith / market / waystation / summon NPC onboarding
→ return to entrance guide 아렌
→ local main quest: inspect any 2 of 3 targets in the 64-110 block town ring
→ first north-road clear also unlocks repeatable Capital Valley regional contracts at 기록관 세린
→ optional record-keeper side hook: revisit the source-backed Explorer's Campsite
→ Capital Valley regional choice
→ Av'Sal remains locked until one additional meaningful regional milestone

Optional Capital Valley exploration:
New Drabyel → Explorer camp → Tower / Warning Cave → chapel / Primal roadhead
```

The route should teach the game without feeling like a tutorial corridor.

## 5. First 0–5 minutes — world reveal

Player state:
- core party available enough to demonstrate 4-person battle structure
- no giant menu dump
- no forced gacha
- no equipment-management wall

World:
- begin at the source-backed north entrance of New Drabyel, not the retired Explorer-camp start
- let the player read the gate, entrance guide and nearby town silhouette before any combat
- after guide dialogue and one party check, send the player only to the short north-road patrol
- objective phrasing should stay diegetic: respond to the gate patrol problem, not “Tutorial Step 1”

Tutorial:
- movement
- interact
- objective marker / minimap basics

NPC:
- one small non-service guide/event role can stand **off the road**, not blocking it
- use the existing location context: traveler, scout, courier, wounded patrol, etc.
- final role/name/model only after actual spawn-area screenshot review

No enemy should attack during the first few seconds.

## 6. First encounter — before the Tower

Placement:
- use a broad road shoulder, meadow edge, or opening after the abandoned-chapel beat
- do not fight inside the chapel
- enemy silhouette visible before aggro
- allow 1 obvious approach and 1 possible bypass

Purpose:
- Basic
- target select
- HP
- Turn Order
- one Active / cooldown after the player understands Basic

Composition:
- 2 simple enemies preferred
- no healer, summon, revive, counter chain or heavy Gauge control here

The encounter must be close enough to a valid 4v2 battle footprint; do not drag the party hundreds of blocks away from the road.

## 7. Capital Valley Tower beat

Official geography already makes the Tower a major landmark between spawn and Drabyel.

TURNBOUND use:
- strong visual midpoint
- map/discovery tutorial
- possible future fast-travel node

Do **not** automatically activate or rewrite Drehmal's original Terminus story network during TURNBOUND onboarding.

Production choice after survey:
1. TURNBOUND adds its own non-destructive discovery marker near the tower, or
2. the original tower interaction is safely wrapped/reused if technical and narrative conflicts are resolved.

The tower itself should remain visually intact.

## 8. Warning Cave — first optional danger POI

Source anchor:
- approximately 454,73,1252, southwest of Capital Valley Tower.

TURNBOUND role candidate:
- first optional Elite or high-risk side encounter
- player receives a visual/audio warning before entering
- reward higher than road combat
- not required for Drabyel progression

Why this location is strong:
- it is already authored as a suspicious/dangerous cave
- it creates the first real “continue to safety or investigate danger” choice
- it avoids inventing a random dungeon next to the tutorial road

Final decision requires:
- interior size check
- battle camera check
- escape route
- original content overlap
- spawn safety

If the cave is too cramped for the battle camera, use its exterior/approach as the encounter site and keep the cave as environmental storytelling.

## 9. Explorer's Guide camp — breathing beat

Source anchor:
- approximately 581,81,1501.

TURNBOUND role:
- non-combat rest/information beat
- first optional equipment comparison
- road/world-map hint
- NPC traveler or abandoned-camp event depending on actual scene

Do not turn it into a huge tutorial kiosk.

Possible functions:
- one loot/equipment decision
- short route hint
- optional heal/rest
- reveal that multiple regions/routes exist

## 10. Drabyel — first hub

Source-backed qualities:
- small official town
- common first settlement
- stables
- farmland
- Adventuring Merchant
- Runic Blacksmith
- several permanent market booths

TURNBOUND goal:
**compact first hub, not a menu plaza.**

### Entrance
- first guard/greeter should be near the actual entrance, not at a floating marker
- entering town should visibly lower combat pressure
- no hostile encounter within immediate hub safety radius

### Service zoning
Use existing spatial logic.

Candidate roles:
- stables → travel/road information
- market booths/merchant frontage → basic shop
- blacksmith area → equipment/upgrade
- central square/statue → narrative/party event
- farmhouse/farmland → ambient/side event
- one unobtrusive building/interior → summon access if scene quality supports it

Do not stack every service NPC on one line.

### Original NPC conflict rule
Before production binding:
- inspect every original merchant/NPC in the town
- decide whether it remains active, becomes ambience, is wrapped, or is suppressed only inside TURNBOUND-bound worlds
- never allow two overlapping shop systems to open from the same visual NPC
- never delete original NPCs simply to simplify code

## 11. Drabyel first-hub onboarding

The player should learn only what is useful now:

1. party screen
2. equipment
3. shop
4. map marker / travel node
5. next main-route choice

Summon is **not automatically unlocked on entering town**.

Preferred:
- unlock after one meaningful field milestone/Elite or a short Drabyel quest
- then introduce summon as a reward/party-expansion system

No 10-screen hub tutorial.

## 12. Post-Drabyel branch

Primary next major candidate:
**Av'Sal**, because it is a large ruined capital near Capital Valley and already contains hostile occupation in the source map.

Do not hard-lock all other exploration.

Desired structure:
- main objective strongly suggests one route
- at least one side route remains available
- dangerous regions can be signposted rather than invisible-walled

The first 60–120 minute arc should gradually move from safe Capital Valley travel into a clearly more dangerous ruin/dungeon context.

## 13. NPC placement sheet — first route

These are semantic roles, not final coordinates.

| Beat | Role | Placement rule |
|---|---|---|
| roadhead | guide/scout | shoulder with clear path visibility |
| chapel vicinity | traveler/event | outside structure, not doorway-blocking |
| Tower | discovery/travel hint | base area, preserve original structure |
| warning cave | warning/event NPC optional | outside danger radius |
| camp | traveler/rest/loot beat | use existing campsite composition |
| Drabyel entrance | guard/greeter | actual pedestrian entrance |
| Drabyel market | shop | existing commerce space |
| Drabyel blacksmith | equipment | existing smithing space |
| Drabyel center | story/party | landmark-adjacent, not service stack |

Every row becomes exact only after 26.2 screenshot/walkthrough verification.

## 14. Encounter placement sheet — first route

| Zone | Encounter | Required? | Design |
|---|---|---|---|
| start road | none | — | initial safety/readability |
| chapel→Tower opening | tutorial patrol | yes | 2 simple enemies, visible |
| Tower vicinity | none / optional roaming | no | landmark breathing room |
| warning cave | Elite candidate | no | high-risk/high-reward |
| camp→Drabyel | road event/patrol | yes or conditional | 2–3 enemies, one new mechanic |
| Drabyel safety ring | none | — | safe hub |

No exact spawn coordinates until battle footprints are checked.

## 15. First-route battle candidate rules

For each mandatory encounter mark at least two candidate footprints.

Minimum:
- party 4 + enemy 3 fit without clipping
- slope modest enough for readable formation
- no deep grass/tree trunk across center frame
- no cliff directly behind default camera
- no river/water across formation
- return point on same travel route
- no original NPC inside battle lock radius

If none exist, move the encounter beat, not the terrain.

## 16. First 30–60 minute target

Approximate experience target, not stopwatch requirement:

### 0–3
- New Drabyel gate arrival
- entrance guide dialogue
- current party check

### 3–10
- short north-road excursion
- first visible melee+ranged encounter
- Turn Order / target-priority understanding
- first meaningful reward

### 10–20
- return to New Drabyel
- blacksmith / market / stable
- optional summon introduction after the road milestone

### 20+
- next-route objective or optional Capital Valley exploration

The camp/Tower/Warning Cave/Primal corridor is optional regional exploration and must not be required to pad the opening runtime.

If real travel time differs, adjust beats around geography instead of moving landmarks arbitrarily.

## 17. Verification still required

Source research is sufficient to freeze **route topology and automatic search zones**, not visual playtest acceptance.

The player is **not** expected to author exact NPC/enemy coordinates. Runtime placement derives them from:
- pinned road/path data and semantic site seeds;
- actual 26.2 ground height/headroom/fluid/slope;
- existing block entities such as signs/chests/lecterns/caches;
- existing villagers/traders/item frames/armor stands;
- safe road-shoulder distance;
- 4-player battle-footprint checks.

Actual 26.2 client playtest still verifies:
- roadhead reveal and route readability;
- chapel/Tower/Warning Cave/camp sightlines;
- enemy silhouette before aggro;
- patrol/pathfinding feel;
- battle camera fallback views;
- original source-content coexistence;
- multiplayer density and return positions.

Static route JSON may remain `verifiedIn26_2=false`; a transient runtime placement is allowed only after the bound live world itself passes the automatic safety gates.


## 18. Road enemy binding

첫 route는 길을 비워두지 않는다.

- roadhead: safety
- chapel 이후: first visible common encounter
- Tower: breathing landmark
- warning cave: optional Elite
- camp: breathing/rest
- Drabyel 직전: melee+ranged road patrol
- Drabyel: safety

상세 enemy composition, Elite/Midboss/Boss hierarchy와 Av'Sal 배치는 `ENCOUNTER_ENEMY_PLACEMENT_v1.md`를 따른다.

Capital Valley Tower 주변은 source map의 no-hostile landmark 성격을 보존한다.

## 19. Drabyel → Av'Sal combat route

Drabyel 도착 이후 첫 본격 전투 축:

```text
Drabyel safety buffer
→ road patrol
→ Av'Sal occupation sign
→ first Mihkmari patrol
→ outer-ring encounters
→ Salvage Captain Elite
→ north-dock Midboss
→ central-island encounters
→ source named-warrior special encounter if safe
→ TURNBOUND regional boss candidate
```

전투 수는 실제 travel time과 지형 검수 후 줄일 수 있다.


## 20. Runtime pacing checkpoint

현재 코드에는 정확한 좌표와 분리된 첫 route 진행 상태가 들어가 있다.

- Tower 도달
- Explorer's Guide camp 도달
- New Drabyel 진입로 도달
- New Drabyel 도달

위 milestone은 player별 SavedData에 누적된다. 뒤 landmark를 먼저 확인하면 앞 milestone도 함께 완료 처리하므로, 재접속·후진 이동·관리자 위치 이동 이후 HUD가 이미 지난 Tower/camp로 되감기지 않는다.

현재 objective 흐름:

```text
첫 도로 조우
→ Capital Valley Tower
→ Warning Cave는 선택 / Explorer's Guide camp
→ New Drabyel 진입로
→ New Drabyel
```

중요:
- Warning Cave는 계속 optional이다.
- camp는 combat gate가 아니라 breathing/equipment-comparison beat다.
- `CV_DRABYEL_ROAD` 승리 후에는 물리적으로 뒤로 이동해도 main navigation이 hub 이전 단계로 되돌아가지 않는다.
- New Drabyel 도달 후 첫-route waypoint는 종료되고 hub onboarding으로 넘어간다.
- 이 checkpoint는 **route pacing/state 구현**이며 좌표 검증 완료를 의미하지 않는다.

다음 production gate는 실제 Minecraft 26.2 **통합 플레이 검수**다. 좌표를 하나씩 입력하는 작업은 요구하지 않는다. 자동 배치된 roadhead, Tower, Warning Cave, camp, Drabyel approach/entrance, field NPC와 service NPC의 동선·시야·battle camera를 한 번의 정상 플레이 흐름에서 확인한다.


## 21. Minecraft 26.2 diagnostics

외부 지도 좌표를 그대로 production block으로 고정하지 않는다.

다음 명령은 자동 배치가 실패했을 때 원인을 추적하는 **운영자 진단 도구**다. 정상 플레이/배치 작업에서는 사용할 필요가 없다.

- `/turnbound survey route`
  - 첫 route의 각 semantic site와 source-backed survey seed 좌표를 출력한다.
  - `PENDING / VERIFIED / PRODUCTION` 상태를 함께 보여준다.
- `/turnbound survey here`
  - 현재 X/Y/Z와 yaw를 기록한다.
  - 3×3 지면 안정성, 3-block headroom, fluid, 주변 급경사 위험을 검사한다.
  - 현재 지점이 solo battle arena와 최대 4-player shared formation에서 모두 열려 있는지 검사한다.
  - route JSON에 복붙 가능한 site `position`과 arena `candidate` JSON을 출력한다.

자동 검사가 PASS여도 다음은 사람이 직접 확인해야 한다.

- 실제 카메라 framing
- 전투 전 적 silhouette 가시성
- 원본 Drehmal NPC/trigger와 겹침 여부
- 길을 막지 않는지
- 도주/복귀 동선
- 나무/풀/건물에 의한 체감 시야 방해
- 멀티플레이에서 16 regular allies가 화면상 과밀하지 않은지

따라서 survey command는 **fallback diagnostics**다. 실제 first-route 배치는 구조화 지도 + live-world safety resolver가 담당하며 사용자가 NPC/조우별 좌표를 수동 수집하지 않는다.


## 22. Map-backed zone placement

첫 route는 단일 좌표 목록이 아니라 **연속 corridor + 역할 구역**으로 관리한다.

Reviewed reference:
- `zachaa/DrehmalMap`
- revision `72d82180cbe3f950f068cf2d8e8668c6b09d5c58`
- `data/paths.geojson`, `data/locations.json`, `data/towers.json`, `data/all_entity_data.json`

구역:
1. 프라이멀 길머리 — 안전한 world reveal, 전투 없음.
2. 옛 사원 → 탑 접근로 — 첫 common 조우.
3. 탑 / 경고 동굴 갈림길 — Tower breathing + optional Elite.
4. 탑 남쪽 → Explorer camp — 낮은 전투 밀도의 breathing corridor.
5. Explorer camp → Drabyel 북쪽 길 — 이동형 road patrol.
6. New Drabyel — hostile-free hub.

소스 지도는 구역, road corridor, landmark와 source-content 위치를 정한다. 실제 Minecraft 26.2 runtime이 ground, fluid, headroom,
local slope, 기존 sign/chest/lectern/cache, villager/trader/item-frame/armor-stand 충돌, road-shoulder 거리,
최대 4-player shared formation, battle camera corridor, safety-zone 침범을 검사해서 exact block을 고른다.

한 구역의 후보가 실패하면 가까운 **같은 구역** 안에서만 탐색한다. 다른 구역으로 멀리 튀어 encounter를 억지 생성하지 않는다.
두 개 이상의 co-op-safe arena를 확보하지 못하면 그 encounter는 해당 세션에서 fail-closed 한다.

따라서 배치 자동화는 지형을 무시한 랜덤 스폰이 아니라 **지도상 의미 있는 구역 선택 → 26.2 실지형 안전 검사** 순서다.


## 23. 첫 루트 빠른 이동

Capital Valley의 첫 traversal은 반드시 실제 도로 탐험으로 진행한다.
빠른 이동은 한 번 방문한 뒤의 왕복 피로만 제거한다.

초기 travel node:
- **프라이멀 길머리** — 첫 route 동쪽 시작부.
- **Capital Valley Tower** — 첫 route 중앙 landmark.
- **New Drabyel** — 첫 hub.

세 노드는 대략 500~700블록 단위로 떨어뜨린다.
Warning Cave / Explorer Camp / 일반 전투 지점은 빠른 이동 node가 아니다.

동작:
- node 반경에 직접 진입하면 개인 save에 발견 상태 기록
- 발견 전 사용 불가
- 월드맵에는 발견한 node만 빠른 이동 표식 활성
- 전투 중 또는 필드 적 추격 상태에서는 사용 불가
- 도착 시 원본 맵을 수정하지 않고 현재 26.2 지형에서 안전한 착지 블록을 서버가 선택
- Gold 비용 없음
- 실제 위치가 막혀 있으면 다른 지역으로 순간 이동시키지 않고 해당 node 주변에서만 안전 지점을 찾고, 실패하면 이동 취소

탈것은 이 시스템을 대체하지 않는다.
탈것은 길 자체를 즐기면서 빠르게 이동하는 수단이고, 빠른 이동은 이미 탐험한 장거리 왕복을 생략하는 수단이다.


## 24. 첫 루트 오픈월드 밀도

첫 route는 메인 목표 세 곳만 찍고 달리는 선형 복도가 아니다.
길을 직접 걷는 동안 **보이는 적 / NPC / 휴식 / 선택형 위험**이 번갈아 나타나야 한다.

현재 1차 production density 목표:
- 일반 필드 적 그룹 **6개**
- 선택 Elite **1개**
- 길 위 비서비스 NPC **3명**
- 안전/휴식 구역에서는 hostile 밀도를 의도적으로 낮춤

전투 그룹 역할:
1. 옛 사원 접근 전 야생 무리
2. 첫 도로 조우
3. Tower 남쪽 약탈자
4. Warning Cave 선택 Elite
5. Explorer Camp 북쪽 야생 무리
6. Drabyel 북부 정찰대
7. Drabyel 진입 이동 순찰대

NPC 역할:
- **길잡이 세라** — 시작부에서 길/회피 규칙을 세계 안의 말로 알려줌.
- **순찰대원 로엔** — Tower 부근에서 Warning Cave의 선택 위험을 경고.
- **탐험가 미라** — Explorer Camp에서 Drabyel 접근로의 순찰 정보를 제공.

배치 규칙:
- 도로 중앙에 일정 간격으로 몬스터를 세우지 않는다.
- 적 그룹은 road shoulder, 공터, 폐허/야생 경계처럼 실제 장소에 이유가 있는 위치를 우선한다.
- 일반 그룹은 우회할 공간을 남긴다.
- Tower / Explorer Camp / New Drabyel 안전 반경에는 적 순찰이 들어오지 않는다.
- 같은 구역에서 안전한 4인 shared arena를 찾지 못하면 다른 구역으로 밀어내지 않고 그 조우를 비활성 처리한다.
- NPC는 서비스 메뉴 버튼이 아니라 실제 3D actor로 존재하고, 가까워질 때만 이름/반응이 읽힌다.

목표는 전투 횟수를 채우는 것이 아니라 45~90초 이동마다 지형, 적, NPC, 갈림길, 전망 중 하나가 읽히는 리듬이다.


## Objective visibility / hidden requests / waystation

- New Drabyel service NPC runtime coordinates are authoritative map points; the world map and nearby minimap label them by name.
- Contextual onboarding navigates to the exact next service NPC instead of only telling the player to search the town.
- The travel facility is player-facing **역참**. It provides fast travel to personally discovered nodes; it does not currently grant a rideable mount.
- Quest NPC examples begin with 길잡이 세라 and 탐험가 미라. Their first conversation can reveal hidden requests that do not exist in the journal beforehand.
- Hidden requests are server-authoritative one-time quests once discovered and receive the same durable reward protection as other authored-world quests.
- The old three-objective tracking assumption is raised to five simultaneous tracked objectives. Authored-world active quests may also coexist on the map.
- Empty walking is not content. Return travel nodes and future shortcuts remove repeated long commuting while keeping the first exploration pass readable.


## New Drabyel local main ring — 2026-09-30

The immediate post-hub loop is deliberately compact.

Main-quest giver:
- entrance guide 문지기 아렌
- 북문 순찰 is a MAIN quest activated by the first greeter conversation
- after the patrol and physical facility visits, 아렌 gives 문 밖의 세 흔적

문 밖의 세 흔적 uses a 2-of-3 structure:
- 버려진 배송 상자 — object clue
- 정찰병 하엔 — NPC clue
- 찢긴 순찰 기록 — object clue

Placement contract:
- source seed distance from New Drabyel center: approximately 77 / 77 / 90 blocks
- every final position is re-resolved against live 26.2 collision/source-content clearance
- no terrain is written
- only two objectives are required, so the player is never forced to run all three legs

Target readability:
- active physical NPC/object targets use glowing outline while they are loaded, making them readable through walls
- M map still shows all unresolved objectives and the single navigation pointer chooses the nearest unresolved target
- physical facilities remain physical: global E-menu has no buy/sell/forge/summon/travel shortcut
- server-side MetaFacilityActionGate rejects economy actions unless the player is actually near the corresponding NPC facility
