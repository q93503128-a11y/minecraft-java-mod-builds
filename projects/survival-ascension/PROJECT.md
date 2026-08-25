# Survival Ascension

- Mod version: `0.55.0-alpha.1`
- Minecraft: `26.2`
- NeoForge: `26.2.0.38-beta`
- Java: `25`
- Network protocol: `8`
- Existing-world compatibility: no new SavedData ID or migration. Existing skill XP totals, `infrastructure_v1`, `field_depots_v1`, `outpost_v1`, `production_v1`, existing affix CustomData and older data stay unchanged. 0.54 adds standard mace-tag imprint plus real-smash outer impact rings; 0.53 adds standard shield-tag imprint plus successful-block guard waves with bounded player-persistent cooldown only; 0.52 adds the existing affix category `ranged` to standard bow/crossbow tags and stores bounded launch-time affix/precision snapshots only on the physical projectile; 0.51 armor, 0.50 regional 3/6/9 admission, 0.49 cart-local frontline manifest NBT, 0.48 exact-outpost local supply, optional integrations, physical freight railheads and item-data boundaries remain unchanged.

## Core direction
Progression enlarges physical player actions rather than mainly inflating percentages. Bigger actions create larger throughput; infrastructure, real storage, transport, bases, expeditions and behavior-driven enemies consume it again. Shift remains the precision/single-action safety override.

## 0.55 Native 26.2 Spear + Sulfur Integration / 26.2 스피어·유황동굴 통합
- `minecraft:sulfur_caves` is part of the existing Deep expedition integration tag. Sulfur/Cinnabar remain ordinary pickaxe-mineable terrain rather than valuable-ore vein/extract targets.
- `ItemTags.SPEARS` is a dedicated `SPEAR` imprint category and is intentionally absent from generated elite base loot.
- Vanilla spear Jab/Charge mechanics remain untouched. Survival adds only a Combat Lv30+ momentum-gated narrow drive line behind a real direct spear target.
- The drive line is zero-damage/zero-XP, hostile-only, Shift-suppressible, knockback-resistance-aware and capped at reach9.0 / targets8 / push1.10.
- Base physical scale is Lv30 3.5/1, Lv60 4.5/2, Lv90 5.5/3, Lv100 6.5/4, Field Mastery 7.5/5 reach/targets.
- Spear affixes: 관통 direct damage, 돌파 reach, 숙련 Combat XP, 대열 target count, 충압 push.
- No new SavedData, packet/protocol, custom spear/entity, force-load or passive simulation.

## 0.54 Mace Impact Ascension / 메이스 충격권 승천
- `c:tools/mace` is a standard-tag imprint category. Existing/external maces keep their original ItemStack/components and may use reforge/Mythic awakening/salvage.
- Mace is intentionally absent from `GEAR_CATEGORIES`, so elite random base loot cannot generate a Mace and bypass its normal acquisition path.
- Runtime specialization is gated by the actual `DamageTypeTags.IS_MACE_SMASH` damage type, not guessed fall distance.
- The vanilla Mace's 3.5-block knockback remains untouched. Survival only adds a hostile-only outer ring beyond 3.5 blocks: Lv30 4.5/3, Lv60 5.5/6, Lv90 6.5/10, Lv100 7.5/14, Field Mastery 9.0/20 radius/targets.
- The outer ring deals no damage and awards no Combat XP. It respects knockback resistance, excludes the primary target/player/allies, and replaces generic Survival cleave/shockwave handling for that smash hit.
- Shift suppresses only the Ascension outer ring; vanilla direct smash and vanilla 3.5-block knockback remain authoritative.
- Mace affixes: `충각` push, `진동` radius, `숙련` kill XP, `분쇄` target count, `격퇴` vertical lift. Caps: radius10.5, targets26, horizontal push1.30, lift0.28.
- No new SavedData ID, packet/protocol, custom item/entity, force-load or passive simulation.

## 0.53 Shield Ascension / 방패 승천
- NeoForge common `c:tools/shield` enters the existing imprint/reforge/Mythic-awakening/salvage loop and elite affix loot without optional-mod Java imports.
- A guard wave is eligible only when `LivingShieldBlockEvent` reports an actual successful block with positive blocked damage. Survival Ascension does not call `setBlocked(true)` or raise the blocked-damage amount.
- Combat mastery physical scale: Lv30 2.5 blocks/2 targets/0.45 push, Lv60 3.5/4/0.60, Lv90 4.5/6/0.75, Lv100 5.5/8/0.90, Lv100 Field Mastery 6.5/10/1.00.
- Shift while blocking is precision mode and suppresses the area wave without changing the underlying successful shield block.
- Shield affixes: `압력` horizontal push, `파동` radius, `대응` cooldown reduction, `진압` target count, `반동` vertical lift. Hard caps: radius8.0, targets14, horizontal push1.30, vertical lift0.28, minimum cooldown6 ticks.
- Guard waves only apply displacement to real hostile/boss targets through the existing combat-target compatibility predicate. They deal no damage and award no Combat mastery XP.
- Shield cooldown uses the player's existing persistent NBT only. No new SavedData ID, protocol bump, custom shield/entity, force-load, block-admission override or passive simulation.

## 0.52 Ranged Combat Ascension / 원거리 전투 승천
- NeoForge common bow/crossbow tags (`c:tools/bow`, `c:tools/crossbow`) now join Ascension Imprint, reforge, Mythic awakening, salvage and elite affix drops without optional-mod Java dependencies.
- Every player-fired tagged bow/crossbow projectile snapshots its ranged affix multipliers and Shift precision state at entity launch. Changing the held item after firing cannot change that shot's damage, Combat XP or burst modifiers.
- Combat Lv30/60/90/100 expands ranged impact bursts at the hit position: base 2.5/2, 3.5/4, 4.25/6, 5.0/8 radius/targets; Lv100 Field Mastery reaches base 6.0 blocks / 10 targets.
- Shift at launch is precision mode: direct-hit damage and snapshotted affixes remain, but the impact burst is disabled for that projectile.
- Ranged affixes reuse the five existing slots with authored roles: `강궁` direct damage (+8/+15/+25%), `산개` burst radius (+0.5/+1.0/+1.5), `숙련` Combat XP (+10/+25/+50%), `연쇄` extra targets (+1/+2/+4), `충격` burst fraction (+5/+10/+15 percentage points).
- Burst fraction is hard-capped at 65%. Persisted projectile bonuses are independently clamped to damage1.25x, XP1.50x, radius+1.5, targets+4 and fraction+0.15 so malformed NBT cannot create unbounded combat scale.
- Already-snapshotted projectiles are not re-snapshotted when re-entering the level, preserving launch-time ownership across chunk unload/reload.
- No custom projectile/entity, new SavedData, packet/protocol change, force-load, background simulation or optional-mod implementation import.

## 0.51 Armor Ascension / 방어구 승천 성장
- Standard humanoid armor tags (`HEAD_ARMOR`, `CHEST_ARMOR`, `LEG_ARMOR`, `FOOT_ARMOR`) now join the existing Ascension Imprint / reforge / Mythic-awakening path without optional-mod Java dependencies.
- Elite affix loot can now roll helmet, chestplate, leggings or boots on the same iron -> diamond -> netherite rarity ladder as existing weapon/tool drops.
- Armor reuses the existing five affix slots with worn-only roles: `수호` always reduces incoming damage, `불굴` adds reduction at <=50% health, `숙련` increases Combat mastery XP, `완강` adds reduction against hits of 8+ damage, and `보호` adds reduction when the damage source has no attacking entity.
- All armor-affix damage reduction is summed across equipped pieces and hard-capped at 35%. Armor `숙련` is summed separately and hard-capped at +32% Combat XP. Holding armor in the hand gives neither effect.
- Imprint keeps the source armor ItemStack and unrelated DataComponents; Survival Ascension still writes only its nested affix CustomData and display name.
- Existing imprint/reforge/awakening/salvage material costs and physical-logistics-first consumption are unchanged.
- No armor attribute rewriting, max-health inflation, new potion effect, SavedData, packet/protocol change, custom armor item, force-load or background simulation.

## 0.50 Regional Logistics Scale / 지역 물류망 확장
- Physical depot/outpost admission is no longer permanently capped at three: Industrial Works = 3, Civil Works = 6, Ascension Nexus = 9.
- The absolute persisted safety cap is9 in the existing `field_depots_v1` and `outpost_v1` lists; no v2 data ID or migration is introduced.
- `FieldDepotData.registrationLimit` derives the live admission limit from existing completed infrastructure. Data-layer `add`/`upgrade` overloads re-check the supplied limit, so callers cannot bypass the progression gate.
- `FieldDepotService` shows and enforces the current 3/6/9 limit before spending the depot's supply charge.
- `OutpostService` checks the current outpost limit before structure validation, material consumption or supply-charge consumption, preventing resources from being lost at a reached 3/6-stage limit.
- Production status and the in-game guide expose the same current-state rule: 산업3 → 토목6 → 승천 중추9.
- Capacity growth does not activate remote bases. Each depot remains a real loaded/interactable Barrel; each outpost still requires its existing physical structure and owner-nearby activation.
- No automatic routing, remote storage, passive upkeep, custom block/item/entity, packet/protocol change, force-load or background simulation.

## 0.49 Frontline Freight Manifest / 전선 보급 화물
- Existing normal physical freight remains the default behavior.
- Shift-selecting `물리 화물 수레` at a valid departure railhead loads one bounded frontline reserve: food176 + iron56 + fuel8 + logs32 + stone bricks128, exactly expedition1 + normal-defense1 + Bastion-defense1 local costs.
- The source is only that departure outpost's registered Barrel + linked warehouse Barrels. Admission is all-or-nothing; missing any part removes nothing.
- Selected material movement is deterministic and separate from general slot-order bulk loading, so dirt/cobblestone junk cannot consume the frontline reservation first.
- If the cart cannot accept the complete prepared bundle, already moved items are returned to the same source cluster and the cart is not accepted as a valid manifest.
- The manifest marker is only the physical Chest Minecart's persistent NBT. Destination unloading continues to respect real container capacity and leaves excess in the same cart.
- No route SavedData, virtual cargo account, generated supplies, auto-driving, teleport, cross-dimension freight or force-load.

## 0.48 Frontline Local Supply / 전선 현지 보급
- Physical freight now has a direct gameplay sink: frontline operations require real stock at the exact active departure outpost.
- Existing global field-supply charges remain unchanged and continue to represent completed four-line industrial production cycles. 0.48 does not replace them with a second virtual currency.
- Expedition operation launch requires supply1 plus local food32 + iron8 + fuel8.
- Normal outpost defense requires supply1 plus local food48 + iron16 + logs32.
- Bastion defense requires supply2 plus local food96 + iron32 + stone bricks128, in addition to the existing physical fortification validation.
- Food is pooled Wheat/Carrot/Potato/Beetroot. Fuel is Coal/Charcoal. Logs use `ItemTags.LOGS`. All are already freight-eligible bulk materials.
- Local material resolution is deliberately different from ordinary logistics-backed crafting: it resolves only the departure outpost's registered Barrel anchor and its persisted linked Barrels. It never reads a nearby different depot and never falls back to player inventory.
- Every local Barrel must be in an already-loaded chunk, remain a real Barrel Container and pass `level.mayInteract(player, pos)`.
- Readiness checks are non-mutating. The physical loadout is consumed only after the pre-existing encounter/operation system reports an actual successful start, so rejected starts do not burn local materials.
- Production Status exposes nearby active-outpost local counts for food/iron/fuel/logs/stone bricks. The production radial shows all three frontline loadouts.
- Players may hand-load a remote outpost, but the intended large-scale loop is production/harvest -> storage -> physical Chest Minecart freight -> destination outpost warehouse -> defense/expedition.
- No new SavedData, packet/protocol, virtual route/cargo balance, custom block/item/entity, teleport or force-load.

## 0.47 Major External Targets / 외부 강적 원정 연동
- Locked The Birth of Steve to audited 26.2 NeoForge `0.7.0+mc26.2+neoforge` (`gKOBlOap` / `xls8dTZv`, file `tbos-neoforge-26.2-0.7.0.jar`, SHA-1 `4d55c51685bff4247fa533c925f7641ce4880db3`).
- Binary audit identified `tbos:hour_cantor` and `tbos:phoenix_guardian` (The Last Curator) as the two TBS entities with the boss-event contract used for this integration. Minotaur is deliberately excluded.
- Added Survival-owned EntityType tag `survivalascension:expedition_major_targets`; external IDs live only in JSON as `required:false`. Java keeps no TBS registry ID or implementation-class dependency.
- Normal hostile kills retain the existing +1 regional action. A tagged major target adds exactly +3 `HOSTILES_KILLED` to the current regional directive and, when all existing validation still passes, the active same-region expedition operation.
- The +3 bypasses `ExpeditionIncidentSystem`, preventing one boss from collapsing an incident objective.
- Fractured Archive is a separate dimension `tbos:fractured_archive` whose internal biome is `minecraft:the_void`. It is not mapped to Deep/End/another of the nine regions, and active expedition operations still fail on dimension departure.
- When `currentRegion == null`, a major target creates no fake expedition progress. Combat mastery still values it with the major-target formula: max-health ×2.5, cap600 before the existing equipment XP multiplier. Ordinary targets remain ×1.5, cap200.
- No new SavedData, packet/protocol, custom block/item/entity, force-load or copied external source/assets.

## 0.46 Resonant Tool Preservation + Shovel Earthworks
- Locked Amethyst Resonance 26.2 NeoForge 1.0.0 binary audit found a concrete gap: Resonant Shovel correctly uses `minecraft:shovels`, but Survival Ascension 0.45 only accepted sword/pickaxe/axe/hoe.
- `ItemTags.SHOVELS` is now a first-class affix category. Scale/secondary shovel affixes expand bounded odd-sized surface earthworks rather than becoming dead rolls.
- Shovel-valid terrain uses Mining mastery speed and XP. It does not use valuable-ore vein traversal, Extract targeting or Bore tunneling.
- External item identity is preserved across imprint/reroll/awakening. Survival Ascension mutates only its nested CustomData and Custom Name; unrelated DataComponents remain on the stack.
- Binary evidence: Resonant Pickaxe class identity drives silent-mining/crystal behavior; Resonance Infusion uses `copyWithCount(1)` then adds its own persistent Resonant DataComponent.
- External ARR binary stays in the content pack only. No Amethyst Resonance implementation classes are linked or copied into the core mod.

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
`FieldDepotService.consumeMatching` resolves usable physical logistics Containers first and consumes them nearest-first before touching carried inventory. Vanilla crafting grids and Apex/Ascension Trial admissions remain separate carried-only rules. Frontline operation/defense local loadouts are the deliberate exact-outpost-only exception introduced in 0.48.

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
`field_depots_v1` keeps the existing anchor/link schema, max8 satellite `통`/anchor inside6, and an absolute9-anchor safety capacity. Live registration is progression-gated 3/6/9 by Industrial/Civil/Nexus. Real Container contents only, loaded-only, no virtual capacity.

## 0.36 Physical Commissioning retained
Civil Works, Industrial Works, Apex Tracking Post and Ascension Nexus use the same final-call real-world commissioning engine. It is one-time proof, not ongoing maintenance.

## 0.35 / 0.34 logistics retained
High-volume offload scans main inventory9..35. Stationary logistics-backed material sinks use nearest usable real `통` storage first, then player inventory. Apex/Trial admission stays player-carried.

## 0.33 / 0.32 expeditions retained
Active regional outpost -> supply1 -> exact local stock -> cross range -> two real validated objectives -> exact-origin return. One bounded complication per new sortie. No force-load.

## 0.31 Field Recovery retained
One prepaid ordinary-death return within96; authored encounter deaths remain excluded. No ordinary fast travel.

## 0.30 Physical Outposts retained
Owned registered `통` + Bed/Campfire/Crafting/Furnace-family within5. Owner-nearby activation64, logistics64, NATURAL-hostile safety24.

## Production retained
METALWORKS / TIMBERWORKS / PROVISIONS / PRECISION remain. One full cycle grants supply1, cap3. Dispatch remains a player-carried reward rather than freight/remote payment.

## Mastery / Field Mastery retained
Lv100 base: Mining11×11+vein192, Wood384, Harvest11×11, Construction49/11×11, combat6.5/16, air dash3.
After all nine regions: Quarry7×7×12, Wood448, Harvest13×13, Construction65/13×13 plus Causeway3×65, combat7.5/20, air dash4.
