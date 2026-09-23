# TURNBOUND First Route — Capital Valley / Drabyel v1

> 목적: TURNBOUND의 첫 30~60분을 Drehmal의 실제 초반 지형에 묶는 production planning 문서.
> 이 문서는 공식 Drehmal 위치 자료와 TURNBOUND 설계 판단을 구분한다.
> 정확한 NPC/적 좌표와 battle footprint는 Minecraft 26.2 migrated world에서 직접 확인한 뒤 확정한다.

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

**Start in the open world on the existing road immediately after the Stasis Facility surface exit, near the Primal Caverns side of the Capital Valley route.**

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

### Why the roadhead works

It provides:
- immediate outdoor sightline,
- visible terrain identity through Primal Caverns,
- one obvious authored road,
- a natural direction toward civilization,
- room for a non-invasive first NPC,
- room for a first visible encounter before the hub,
- Capital Valley Tower as a strong intermediate landmark,
- existing camp and optional cave as side beats,
- Drabyel as a clear destination.

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
TURNBOUND roadhead
→ Primal Caverns / surface reveal
→ first guide/event NPC
→ abandoned chapel beat
→ first visible encounter
→ Capital Valley Tower landmark
→ choice: warning cave detour or continue
→ Explorer's Guide camp / rest-information beat
→ second encounter or road event
→ Drabyel entrance reveal
→ first hub onboarding
→ route choice toward Av'Sal / another surveyed Capital Valley branch
```

The route should teach the game without feeling like a tutorial corridor.

## 5. First 0–5 minutes — world reveal

Player state:
- core party available enough to demonstrate 4-person battle structure
- no giant menu dump
- no forced gacha
- no equipment-management wall

World:
- begin at the safe roadhead
- let the player look around before any combat
- show Primal Caverns / surrounding Capital Valley terrain
- objective phrasing should be diegetic: reach the settlement down the road, not “Tutorial Step 1”

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

### 0–10
- world reveal
- interaction
- first combat

### 10–20
- Tower landmark
- Turn Order understanding
- first meaningful reward

### 20–35
- optional cave decision
- camp/rest/equipment

### 35–50
- second encounter/event
- Drabyel reveal

### 45–60
- hub services
- party/equipment management
- next-route objective
- optional summon-unlock setup

If real travel time differs, adjust beats around geography instead of moving landmarks arbitrarily.

## 17. Verification still required

Source research is sufficient to freeze **route topology**, not exact production coordinates.

Need actual 26.2 client survey for:
- exact roadhead spawn block
- chapel location and approach
- safe Tower-side battle footprint
- warning cave dimensions
- camp line-of-sight
- Drabyel pedestrian entrance
- service NPC exact spots
- original merchant/NPC behavior
- mob/pathfinding collision
- camera fallback views
- multiplayer spawn offsets

Until then exact positions remain `verifiedIn26_2=false`.


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

다음 production gate는 그대로 실제 Minecraft 26.2 client survey다. roadhead, Tower, Warning Cave, camp, Drabyel approach/entrance 및 service spot의 스크린샷·동선·battle camera 검증 없이는 `verifiedIn26_2`와 `productionEnabled`를 true로 올리지 않는다.
