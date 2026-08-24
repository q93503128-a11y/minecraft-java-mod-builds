# Survival Ascension

- Mod version: `0.45.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: no new SavedData ID or migration. Existing skill XP totals, `infrastructure_v1`, `field_depots_v1`, `outpost_v1` and older data stay unchanged. Physical freight railheads remain world-block validated; external gear imprint stores only nested item CustomData on the item itself.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, real storage, transport, bases, expeditions and behavior-driven enemies consume it again. Shift remains the precision/single-action safety override.

## 0.45 External World Integration / 외부 월드 원정 통합
- 기존 9지역 원정이 정본이며 외부 바이옴용 별도 퀘스트/진행 시스템을 만들지 않는다.
- `ExpeditionRegion.matches`는 `survivalascension:expedition/<region>` 태그를 먼저 보고 바닐라 바이옴 fallback을 유지한다.
- BOP 항목은 데이터 전용 `required:false`; Deep에는 `glowing_grotto`와 `spider_nest`가 모두 들어간다.
- The Birth of Steve 적은 기존 `Enemy` 계약, 비-Enemy 보스는 NeoForge `BOSSES` 공용 태그로 전투 숙련과 상호 운용한다.
- optional-mod Java import, 새 SavedData/packet/entity, force-load, 외부 코드/리소스 복사는 추가하지 않는다.
- 실제 graphical client / external worldgen smoke는 별도 검증 전까지 미실행 상태다.

## 0.44 Content-pack Equipment Imprint / 외부 장비 승천 각인
### Purpose
The content pack now contains additional equipment sources. Those items should not sit beside Survival Ascension as a disconnected second progression system. `승천 각인` lets compatible external gear enter the same rarity/affix/mastery loop without directly linking optional mod classes.

### Eligibility
`AscensionAffixes.canImprint` accepts a non-affix, single-stack item when it belongs to one of Minecraft's standard item tags:
- `ItemTags.SWORDS` -> weapon;
- `ItemTags.PICKAXES` -> pickaxe;
- `ItemTags.AXES` -> axe;
- `ItemTags.HOES` -> hoe.

This deliberately avoids registry-ID lists and optional-mod Java imports. Any correctly tagged current or future content-pack item can participate; removing an optional mod does not stop Survival Ascension itself from loading.

### Player flow
`M -> 장비 -> 승천 각인` while holding the target item in the main hand.

The current World Ascension stage determines the initial affix rarity:
- Stage0 `각성` -> Elite I / 1 affix;
- Stage1 `전설` -> Ascended II / 2 affixes;
- Stage2 `종말` -> Mythic III / 3 affixes.

Costs remain real-material sinks through `FieldDepotService`, so nearby usable logistics `통` are consumed first and only the shortage comes from carried inventory.

Imprint costs:
- Stage0: Amethyst24 + Iron12;
- Stage1: Amethyst48 + Diamond4 + Gold16;
- Stage2: Amethyst96 + Diamond8 + Netherite Scrap2 + Echo8.

After imprint, existing systems apply automatically because they already read Survival Ascension affix data: reforge, salvage, Mythic awakening, mastery-XP multiplier, pickaxe area/vein bonuses, axe chain bonuses, hoe area bonuses and weapon damage/cleave bonuses.

### Item-data boundary
The source gear is not replaced with a vanilla item. All existing item components remain on the same stack. Survival Ascension only updates its nested `survivalascension_affix` CustomData and the display name. The first pre-affix hover name is stored as `base_name`, so rerolls and awakening do not recursively stack affix prefixes.

The built-in elite-drop generator still creates its own vanilla iron/diamond/netherite bases. Imprint is the bridge for externally supplied loot rather than a replacement for those drops.

### Deliberate exclusions
- no hard dependency on Biomes O' Plenty, The Birth of Steve, Amethyst Resonance or future content mods;
- no copying of external item classes, recipes, textures or models into Survival Ascension;
- no new packet schema/protocol bump; existing integer `EquipmentActionPayload` gains one action value;
- no armor imprint yet because current affix effects are authored for weapon/tool activity, not passive armor-stat stacking;
- no item duplication or generated base equipment.

## 0.43 Physical Freight Railheads / 물리 화물 하역장
### Purpose
0.42 proved that actual stock can move between real outpost warehouse clusters in a real vanilla Chest Minecart. Its weak point was the endpoint: a single arbitrary rail beside an active outpost was enough to trigger loading or unloading.

0.43 makes the endpoint itself physical. Freight now works only when the exact active outpost has a small real railhead around its registered `통(Barrel)` anchor. This gives Civil Works/rails a visible persistent job without introducing a background logistics simulator.

### Physical endpoint contract
Before any freight mutation, `FreightRailheadService` inspects only already-loaded blocks around the exact active outpost anchor.

Radius: `6` blocks around the outpost anchor.
Minimum visible yard:
- rail blocks using `BlockTags.RAILS`: `6+`;
- `Powered Rail`: `1+`;
- `Hopper`: `1+`;
- control: `Lever` or `Redstone Block`: `1+`.

The actual Chest Minecart rail must also be inside the same radius. To stop scattered checklist blocks from counting as a station, the active cart rail additionally needs:
- a Powered Rail within squared distance `9`;
- a Hopper within squared distance `9`;
- a Lever/Redstone Block within squared distance `16`.

Every scanned block must be in an already-loaded chunk and pass `level.mayInteract(player, pos)`.

### Runtime behavior
The railhead is not registered and has no SavedData flag. Every load/unload action checks the current physical blocks again. If the yard is broken, freight stops; rebuild it and freight works again.

The 0.42 freight semantics remain:
- Industrial Works + Civil Works complete;
- survival/non-spectator;
- active owned outpost within4;
- real Chest Minecart within4;
- cart standing on loaded rail;
- empty cart required for departure loading;
- source/destination inventory is only that exact outpost anchor + its persisted linked warehouse `통`;
- same-dimension, different owned active destination;
- only `FieldDepotService.isBulkMaterial` moves;
- partial unloading leaves remainder in the same physical cart;
- manifest remains only owner UUID + origin dimension/x/y/z on the cart entity.

### Deliberate exclusions
- no station SavedData or registration menu;
- no route SavedData;
- no automatic minecart driving or pathfinding;
- no generated freight reward or supply-charge fee;
- no item/cart/player teleport;
- no custom train/block/item/entity;
- no `getChunk`, region ticket or force-load;
- no cross-dimension freight;
- no passive maintenance timer.

## 0.42.1 Guide / Early Mastery / Logistics Priority
### Guide information architecture
`GuideScreen` is a gameplay reference, not a patch-note surface.

The body has a bounded viewport with scissor clipping, mouse-wheel scrolling and a visible scrollbar. The guide explains persistent world state directly: Stage0 `각성`, first Wither -> Stage1 `전설`, first Ender Dragon -> Stage2 `종말`.

### Early skill pacing
The base quadratic XP curve remains the late-game reference, with an opening discount that fades out by Lv60. Approximate cumulative thresholds remain Lv10 `453`, Lv30 `11,610`, Lv60 `107,010`, Lv90 `379,230`, Lv100 `521,270`.

### Mining XP material tiers
Mining XP remains material-tiered rather than flat valuable-ore XP. Key values: Copper7/8, Iron9/10, Gold12/13, Diamond18/20, Emerald20/22, Ancient Debris24, Obsidian16 / Crying Obsidian18.

### Physical logistics spend order
`FieldDepotService.consumeMatching` resolves usable physical logistics Containers first and consumes them nearest-first before touching carried inventory. Vanilla crafting grids and Apex/Ascension Trial admissions remain separate carried-only rules.

## 0.42 Physical Freight Relay / 물리 화물 중계
A real Chest Minecart physically moves the existing bulk whitelist between the exact warehouse clusters of two different active owned outposts in the same dimension. Actual source stacks shrink only by accepted cart capacity; actual cart stacks shrink only by accepted destination capacity. Manifest data remains only on the cart. No automatic driving, teleport, route SavedData, force-load or generated freight reward.

## 0.41 Civil Works Causeways retained
`CIVIL_WORKS` is Stage1: Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256. Final funding needs owned registered `통` within4 plus radius6 physical yard: Stone Bricks48, Scaffolding16, Iron Blocks4, Stonecutters2, Crafting Table1.

`CAUSEWAY("causeway", "도로/교량", 60)` uses the existing protected Construction queue. It lays the manually selected BlockItem as a flat 3-wide forward deck:17/33/49/65 length. Loaded-only, `mayInteract`, placement hooks, physical-logistics-first/player material, no terrain erasure or free supports.

## 0.40 Physical Siege Breachers retained
Only Bastion wave4 Ravager/Vindicator can damage qualifying fortification; normal three-wave defense remains non-destructive. Every break obeys loaded-only, mobGriefing, owner protection, entity-destroyability and NeoForge destroy event.

## 0.39 Physical Bastion Defense retained
Physical fortification uses radius6..12, Y-3..+4, four quadrants each12 unique x/z columns. Bastion remains supply2 /4 waves /6000 ticks and composition-driven rather than blanket-stat driven.

## 0.38 Defendable Physical Outposts retained
Normal defense remains supply1 /3 waves /4800 ticks with owner64, physical structure validation and breach radius6/limit200.

## 0.37 Physical Warehouse Clusters retained
`field_depots_v1` keeps max3 anchors/player, max8 satellite `통`/anchor inside6. Real Container contents only, loaded-only, no virtual capacity.

## 0.36 Physical Commissioning retained
Civil Works, Industrial Works, Apex Tracking Post and Ascension Nexus use the same final-call real-world commissioning engine. It is one-time proof, not ongoing maintenance.

## 0.35 / 0.34 logistics retained
High-volume offload scans main inventory9..35. Stationary logistics-backed material sinks use nearest usable real `통` storage first, then player inventory. Apex/Trial admission stays player-carried.

## 0.33 / 0.32 expeditions retained
Active regional outpost -> supply1 -> cross range -> two real validated objectives -> exact-origin return. One bounded complication per new sortie. No force-load.

## 0.31 Field Recovery retained
One prepaid ordinary-death return within96; authored encounter deaths remain excluded. No ordinary fast travel.

## 0.30 Physical Outposts retained
Owned registered `통` + Bed/Campfire/Crafting/Furnace-family within5. Owner-nearby activation64, logistics64, NATURAL-hostile safety24.

## Production retained
METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION remain. One full cycle grants supply1, cap3. Dispatch remains a player-carried reward rather than freight/remote payment.

## Mastery / Field Mastery retained
Lv100 base: Mining11×11+vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dash3.
After all nine regions: Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13 plus Causeway3×65, combat7.5/20, air dash4.
