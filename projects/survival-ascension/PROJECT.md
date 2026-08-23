# Survival Ascension

- Mod version: `0.41.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: all existing SavedData IDs remain unchanged. 0.41 adds one new `InfrastructureProject` key inside the existing `infrastructure_v1` unbounded funding map; old saves simply have zero contribution for `civil_works` until funded. No migration or new SavedData ID is required.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, real storage, bases, expeditions and behavior-driven enemies must consume it again. Shift remains the precision/single-action safety override.

## 0.41 Civil Works Causeways / 토목 공사소·도로 교량 시공
### Purpose
0.38–0.40 gave physical construction a combat use. 0.41 returns to the other half of the game's identity: the huge material throughput from Mining/Woodcutting/Harvesting must be able to become persistent useful world infrastructure without adding a second blueprint/auto-builder game.

### Stage1 project
`InfrastructureProject.CIVIL_WORKS` requires World Ascension Stage1 and consumes:
- Stone Bricks2048
- Cobblestone1536
- Gravel1536
- Iron256
- Copper256

`InfrastructureData` remains `infrastructure_v1`; its map is keyed by project id + requirement index, so the new project is naturally absent/zero on old worlds.

Funding keeps the 0.34 inventory-first + nearest real logistics Barrel resolver. The project does not create a virtual stockpile.

### Physical civil yard commissioning
The finalizable Civil Works funding action reuses `InfrastructureSiteService` before any final-call project material is consumed.

Required anchor:
- an owned registered physical logistics Barrel within4.

Required loaded blocks inside radius6:
- Stone Bricks48
- Scaffolding16
- Iron Blocks4
- Stonecutters2
- Crafting Table1

The same loaded-only, `mayInteract`, real Barrel Container rules as 0.36 apply. This is one-time commissioning proof, not a permanent maintenance chore.

### ConstructionMode.CAUSEWAY
New construction mode:
`CAUSEWAY("causeway", "도로/교량", 60)`.

Server gate:
- Construction level >=60;
- `InfrastructureProject.CIVIL_WORKS` complete.

Client entry:
`M -> 건축 -> 도로/교량`.

The first player-placed ordinary BlockItem supplies the deck BlockState. Target geometry is always a flat three-wide strip extending forward from that first block in the player's dominant horizontal look direction.

Length follows the existing Construction line scale:
- Lv60:17
- Lv90:33
- Lv100:49
- Lv100 + Field Mastery:65

The initial row is widened around the manually placed center block, and each following row is three blocks wide. At Field Mastery the whole operation is at most194 queued targets after excluding the manually placed center, well under the existing512 pending cap.

### Existing queue / materials / protection
No second construction engine exists. `CAUSEWAY` enters the same `ConstructionProgression.BuildJob` queue.

Retained limits:
- global64 attempts/tick;
- local8 attempts/tick/player job rotation;
- max512 pending targets/player.

Every queued target now explicitly checks `level.hasChunkAt(target)` before touching block state. It then retains:
1. `level.mayInteract(player, target)`;
2. replaceability;
3. placed-state survival;
4. real BlockItem material availability through `FieldDepotService`;
5. NeoForge `EventHooks.onBlockPlace`;
6. actual `setBlockAndUpdate`;
7. consume one real item from player-first / physical logistics stock, with rollback if consumption loses a race.

Unloaded, protected, blocked or invalid targets are skipped. No chunk ticket, `getChunk`, force-load, terrain deletion or free support blocks are introduced.

### Precision / world consequence
Shift still prevents all bulk placement for that action. Causeway does not auto-sculpt terrain; it lays a same-height deck. This intentionally creates raised roads and bridges and makes terrain preparation a physical concern instead of silently editing the world.

## 0.40 Physical Siege Breachers retained
Only Bastion wave4 tagged Ravager/Vindicator can damage qualifying fortification. Ravager cooldown30, Vindicator60. Targeting remains local, annulus6..12 only, forward toward anchor, with `mobGriefing` + owner protection + block destroyability + NeoForge destroy-event checks. Normal three-wave defense is non-destructive.

## 0.39 Physical Bastion Defense retained
Physical fortification uses radius6..12, Y-3..+4 and NE/NW/SE/SW minimum12 unique x/z columns each. Bastion remains supply2 / four waves /6000 ticks, revalidating the wall between waves. No passive defense percentage.

## 0.38 Defendable Physical Outposts retained
Normal three-wave defense remains supply1 /4800 ticks with owner64, anchor-directed attackers and breach radius6/limit200.

## 0.37 Physical Warehouse Clusters retained
`field_depots_v1` keeps max3 anchors/player, max8 satellite Barrels/anchor inside6. Real Container contents, loaded-only, no automatic pickup routing or virtual capacity.

## 0.36 Physical Commissioning retained
Civil Works now joins Industrial Works / Apex Tracking Post / Ascension Nexus in the same one-time commissioning engine. Existing completed projects remain grandfathered.

## 0.35 / 0.34 logistics retained
High-volume offload scans main inventory slots9..35. Shared stationary material sinks use inventory first, then nearest usable real anchor/warehouse Barrel. Apex/Trial admission stays player-carried.

## 0.33 / 0.32 expedition operations retained
Active regional outpost -> supply1 -> cross range -> two real validated objectives -> exact-origin return. One complication per newly started sortie. No force-load.

## 0.31 Field Recovery retained
One prepaid ordinary-death return within96; Incident/Apex/Trial/Outpost/Bastion deaths remain excluded.

## 0.30 Physical Outposts retained
Owned registered Barrel + Bed/Campfire/Crafting/Furnace-family within5. Owner-nearby activation64, logistics64, NATURAL-hostile safety24.

## Production retained
METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION remain. One full four-line cycle grants supply1, cap3.

## Mastery / Field Mastery retained
Lv100 base: Mining11×11+vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dash3.
After all nine regions: Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13 plus Causeway3×65, combat7.5/20, air dash4.

## External-source policy
Building Gadgets 2 remains the permissively licensed Construction code/design reference already declared in `THIRD_PARTY_NOTICES.md`: material-backed bulk placement, protection hooks and tick-distributed work. 0.41's Civil Works project, physical yard, forward three-wide geometry and physical logistics integration are independent Survival Ascension code. No new external assets or dependency are bundled.
