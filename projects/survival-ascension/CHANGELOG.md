# Changelog

## 0.55.0-alpha.1
- Added Minecraft 26.2 `minecraft:sulfur_caves` to the existing Deep expedition integration tag.
- Kept Sulfur/Cinnabar as normal pickaxe-mineable terrain: Mining speed/XP/area applies, but they are not promoted into `valuable_ores` vein/extract semantics.
- Added dedicated `ItemTags.SPEARS` Ascension Imprint/reforge/Mythic-awakening/salvage support while preserving normal spear acquisition by excluding Spear from generated elite base loot.
- Preserved vanilla Jab/Charge authority; Combat Lv30+ adds only a real-forward-momentum narrow drive line behind the primary spear target.
- Drive line is hostile-only, 0피해/0XP (zero-damage/zero-XP), Shift-suppressible and knockback-resistance-aware; base reach/targets scale 3.5/1 -> 4.5/2 -> 5.5/3 -> 6.5/4 -> Field Mastery 7.5/5.
- Added Spear affixes `관통`/`돌파`/`숙련`/`대열`/`충압` with hard caps reach9.0, targets8 and push1.10.
- Kept network protocol8 and added no SavedData, packet, custom spear/entity, force-load or background simulation.
- Bumped content-preview lock to `0.55.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

## 0.54.0-alpha.1
- Added `Mace Impact Ascension / 메이스 충격권 승천` using NeoForge common `c:tools/mace` for imprint/reforge/Mythic-awakening/salvage compatibility.
- Deliberately kept Mace out of generated elite base loot so normal Trial Chamber/external acquisition is not bypassed.
- Real `DamageTypeTags.IS_MACE_SMASH` hits now replace generic Survival cleave/shockwave handling with a hostile-only outer impact ring beyond vanilla's unchanged 3.5-block Mace knockback.
- Outer-ring mastery scale: Lv30 4.5/3, Lv60 5.5/6, Lv90 6.5/10, Lv100 7.5/14, Field Mastery 9.0/20 radius/targets.
- Outer impact deals zero damage/zero Combat XP, respects knockback resistance and excludes allies/passive mobs. Shift disables only the Ascension outer ring.
- Added Mace affixes `충각`/`진동`/`숙련`/`분쇄`/`격퇴`; capped radius10.5, targets26, horizontal push1.30 and lift0.28.
- Kept network protocol8 and added no SavedData, packet, custom mace/entity, force-load or background simulation.
- Bumped content-preview lock to `0.54.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

## 0.53.0-alpha.1
- Added `Shield Ascension / 방패 승천`: NeoForge common `c:tools/shield` items now join imprint/reforge/Mythic awakening/salvage and elite affix drops.
- Added successful-block guard waves from Combat Lv30 upward: 2.5/2, 3.5/4, 4.5/6, 5.5/8, and Field Mastery 6.5/10 base radius/targets.
- Shift blocking suppresses the guard wave for precision defense while leaving vanilla successful blocking unchanged.
- Added shield affix roles `압력`/`파동`/`대응`/`진압`/`반동` for horizontal push/radius/cooldown/targets/vertical lift with hard caps 1.30/8.0/min6t/14/0.28.
- Guard waves deal zero damage and award zero Combat mastery XP; they never force a failed block to succeed or increase blocked damage.
- Preserved original shield ItemStack/components and network protocol8; added no new SavedData, packet, custom shield/entity, force-load or optional-mod implementation dependency.
- Bumped the one-import content-preview lock to `0.53.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

## 0.52.0-alpha.1
- Added `Ranged Combat Ascension / 원거리 전투 승천`: standard NeoForge bow/crossbow tags now join imprint/reforge/Mythic awakening/salvage and elite affix drops.
- Added launch-time projectile snapshots for ranged affix damage, Combat XP, burst radius/targets/fraction and Shift precision, preventing post-shot weapon-swap changes.
- Added mastery-scaled ranged impact bursts: Lv30 2.5/2, Lv60 3.5/4, Lv90 4.25/6, Lv100 5/8, Field Mastery 6/10 base radius/targets.
- Shift-fired ranged shots remain direct single-target precision shots with no impact burst.
- Added ranged affix roles: 강궁 direct damage, 산개 radius, 숙련 Combat XP, 연쇄 target bonus, 충격 burst fraction.
- Hard-capped burst fraction at 65% and clamped persisted projectile affix values to authored maxima; already-snapshotted projectiles are not re-snapshotted on level re-entry.
- Kept network protocol8 and added no custom projectile/entity, SavedData, force-load, background simulation or optional-mod Java dependency.
- Bumped the one-import content-preview lock to `0.52.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

## 0.51.0-alpha.1
- Added `Armor Ascension / 방어구 승천 성장`: standard head/chest/leg/foot armor tags now join the existing affix imprint, reforge, Mythic-awakening and salvage flow.
- Elite affix loot can now roll iron/diamond/netherite armor across all four equipment slots.
- Added worn-only armor affix roles: 수호 general reduction, 불굴 low-health reduction, 숙련 Combat XP, 완강 heavy-hit reduction, 보호 no-attacker/environmental reduction.
- Hard-capped total armor-affix incoming-damage reduction at 35% and armor mastery XP at +32% across all equipped pieces.
- Preserved original armor ItemStack identity and unrelated components; Survival Ascension still writes only its nested affix CustomData/display name.
- Kept existing physical-logistics-first imprint/reforge/awakening costs, network protocol 8, and all 0.49/0.50 freight/regional-logistics contracts.
- Added no new SavedData, custom armor item, attribute rewrite, potion-effect layer, force-load or optional-mod Java dependency.

## 0.50.0-alpha.1
- Added `Regional Logistics Scale / 지역 물류망 확장`: live registered depot/outpost admission grows 3 -> 6 -> 9 with Industrial Works -> Civil Works -> Ascension Nexus.
- Raised only the absolute persisted safety capacity of the existing `field_depots_v1` / `outpost_v1` lists to9; no new SavedData ID or migration is introduced.
- Added `FieldDepotData.registrationLimit` and data-layer bounded `add`/`upgrade` overloads so progression admission cannot be bypassed by another caller.
- Updated field-depot registration/status and outpost promotion/status to show and enforce the current infrastructure limit rather than displaying a permanent max9.
- Fixed a manual-audit bug found during 0.50 closure: outpost promotion now checks the 3/6/9 limit before structure/material/supply-charge mutation, so a reached stage limit cannot consume iron/gold/coal or field-supply charges.
- Kept each depot as a real loaded/interactable Barrel and each outpost as the same physical Bed/Campfire/Crafting/Furnace structure; expansion does not create remote activation or maintenance simulation.
- Kept network protocol8, physical Chest Minecart freight, exact-outpost frontline stock, no automatic routing, no virtual storage, no teleport and no force-load.
- Updated README / PROJECT / in-game guide / source audit / packaged-JAR verifier for the staged regional capacity contract.

## 0.49.0-alpha.1
- Added `Frontline Freight Manifest / 전선 보급 화물` as a Shift-select mode on the existing physical Chest Minecart freight action; normal selection retains ordinary bulk freight.
- Frontline manifest composition is exactly food176 + iron56 + fuel8 + logs32 + stone bricks128 = 400 items, matching one expedition + one normal outpost defense + one Bastion defense local loadout.
- Manifest admission is all-or-nothing from the exact departure outpost Barrel + linked warehouse cluster, so unrelated slot-order bulk material cannot crowd the reserve out.
- If cart slot layout prevents the complete prepared manifest, already moved stock is rolled back to the same physical source cluster and the load is rejected.
- The frontline marker is stored only on that actual Chest Minecart persistent NBT. Destination unloading still uses real capacity and may leave remainder in the same cart.
- Added no route SavedData, virtual cargo balance, generated supplies, auto-driving, cross-dimension freight, teleport or force-load.
- Bumped the content-preview lock to `0.49.0-alpha.1-content-preview.1` without changing the six audited external mod versions.
- Extended release source/content-pack/JAR verification while retaining all earlier physical freight, frontline supply and optional-mod compatibility checks.

## 0.48.0-alpha.1
- Added `Frontline Local Supply / 전선 현지 보급` so the physical freight network now feeds expedition and defense readiness rather than only relocating stock.
- Expedition operation launch keeps supply-charge1 and additionally requires food32 + iron8 + fuel8 in the exact departure outpost warehouse cluster.
- Normal outpost defense keeps supply-charge1 and additionally requires food48 + iron16 + logs32 in the exact departure outpost warehouse cluster.
- Bastion defense keeps supply-charge2, physical-fortification validation and additionally requires food96 + iron32 + stone bricks128 in the exact departure outpost warehouse cluster.
- Local food pools Wheat/Carrot/Potato/Beetroot, fuel pools Coal/Charcoal and logs use `ItemTags.LOGS`; every material is already freight-eligible.
- The local resolver reads only the exact outpost's registered Barrel anchor + persisted linked Barrels, and every Barrel must be loaded, interactable and still a real Container. It never falls back to player inventory or a different nearby depot.
- Frontline material checks are non-mutating and physical stock is consumed only after the existing encounter/operation system reports an actual successful start, preventing rejected launches from burning local stock.
- Added nearby active-outpost food/iron/fuel/log/stone-brick counts to Production Status and exposed all three loadouts in the production radial and in-game guide.
- Bumped the one-import content-pack preview to `0.48.0-alpha.1-content-preview.1` without changing the six audited external mod versions.
- Extended canonical source/content-pack audits and packaged-JAR verification for the local-supply runtime while retaining all previous 0.43-0.47 compatibility checks.
- Added no SavedData schema, packet/protocol, custom block/item/entity, virtual freight currency, teleport or force-load.

## 0.47.0-alpha.1
- Updated the locked The Birth of Steve pack entry to audited 26.2 NeoForge `0.7.0+mc26.2+neoforge` (`gKOBlOap` / `xls8dTZv`, file `tbos-neoforge-26.2-0.7.0.jar`, SHA-1 `4d55c51685bff4247fa533c925f7641ce4880db3`).
- Added Survival-owned optional EntityType tag `survivalascension:expedition_major_targets`; audited TBS entries are `tbos:hour_cantor` and `tbos:phoenix_guardian`, both `required:false`. Minotaur is excluded because the audited 0.7 binary did not expose the same boss-event contract.
- Tagged major targets keep the normal hostile-kill +1 and add bounded +3 `HOSTILES_KILLED` credit to a valid current regional directive and active same-region expedition operation.
- Major-target bonus does not enter `ExpeditionIncidentSystem`, preventing a single boss from multiplying incident progress.
- Fractured Archive remains its real separate `tbos:fractured_archive` dimension (`minecraft:the_void` biome internally). It is not mapped into the nine expedition regions, and existing operation dimension-leave failure behavior is retained.
- Major targets outside the nine expedition regions create no fake regional progress. Combat mastery uses max-health ×2.5 with a cap of600 XP; normal targets remain ×1.5 with cap200 before the existing equipment XP multiplier.
- Kept optional-mod implementation classes and registry IDs out of Java; external IDs live only in optional data JSON. Added no SavedData, packet/protocol, custom content, force-load or copied TBS source/assets.
- Updated the content-pack lock, compatibility matrix, source audits and packaged-JAR verifier coverage for the new integration.

## 0.46.0-alpha.1
- Audited the exact locked Amethyst Resonance 26.2 NeoForge 1.0.0 binary (`8RyryQ7j` / `no0B3Ssy`, SHA-1 `a3ac49a6202b7918d2ed22030df0b6e2906cdec8`).
- Confirmed its sword/pickaxe/axe/shovel/hoe use vanilla item tags and identified the actual 0.45 omission: standard shovels were not imprintable.
- Added `ItemTags.SHOVELS` and a real Shovel affix category, including vanilla-backed elite shovel generation without fragile `Category.values()[random.nextInt(4)]` indexing.
- Added shovel Mining mastery: shovel-mineable terrain receives Mining speed/XP and bounded planar earthworks; Shift stays precision and ore Vein/Extract/Bore remain pickaxe-only.
- Added functional Shovel Scale/Secondary affixes (`토공`/`개착`) with a hard area cap of13 so shovel rolls are not dead options.
- Verified Resonant Pickaxe item-class perks and Resonance Infusion DataComponent survive because imprint/reroll/awakening keep the original ItemStack identity/components and only update Survival Ascension nested CustomData + name.
- Updated equipment UI, guide, modpack lock version, compatibility docs, source audits and JAR verifier.
- No Amethyst Resonance code/assets are copied; no optional-mod Java dependency, packet, SavedData or force-load is added.

## 0.45.0-alpha.1
- BOP 원정 브리지를 정식 현재 버전에 포함하고 `spider_nest`를 Deep 원정에 추가했다.
- 9개 원정 태그의 BOP 항목이 모두 optional(`required:false`)인지, BOP ID가 지역 간 중복되지 않는지 source audit에 고정했다.
- packaged JAR이 9개 원정 태그와 Deep의 `glowing_grotto`/`spider_nest`를 실제 포함하는지 verifier에 추가했다.
- The Birth of Steve 핵심 적은 기존 `Enemy` 계약으로 이미 편입됨을 감사하여 전용 하드 의존을 추가하지 않았다.
- 0.44 외부 장비 각인과 기존 물리 물류/화물/토목/전초/방어/원정 체급을 회귀 유지했다.
- datagen, GameTest, dedicated server smoke, graphical client/external worldgen smoke는 실행하지 않았다.

## 0.44.0-alpha.1
- Added `Ascension Imprint / 승천 각인` so standard-tagged external swords, pickaxes, axes and hoes can enter Survival Ascension's existing affix/mastery progression instead of remaining disconnected content-pack gear.
- Imprint eligibility is dependency-free and uses Minecraft item tags (`ItemTags.SWORDS/PICKAXES/AXES/HOES`) rather than optional-mod implementation classes or registry hardcoding.
- Current World Ascension stage determines imprint rarity: Stage0 -> Elite I, Stage1 -> Ascended II, Stage2 -> Mythic III.
- Imprint costs are physical-logistics-backed and use the existing storage-first material resolver; no virtual currency or remote inventory system is added.
- Added `승천 각인` to the existing equipment radial and reused the existing integer equipment action payload, so protocol remains8.
- Imprinted external gear keeps all existing item components; Survival Ascension writes only its nested `survivalascension_affix` CustomData and display name. A stored base-name field prevents affix rerolls from stacking prefixes.
- Existing reforge, salvage, Mythic awakening, mastery-XP, mining area/vein, woodcutting chain, harvest area and weapon cleave/damage hooks automatically apply after imprint because they already read Survival Ascension affix data.
- Elite-generated affix gear remains vanilla-backed; imprint is the bridge for content-pack equipment rather than replacing external loot generation.
- Added no hard dependency on Biomes O' Plenty, The Birth of Steve, Amethyst Resonance or any future content mod; correctly tagged gear can join automatically.

## 0.43.0-alpha.1
- Added `Physical Freight Railheads / 물리 화물 하역장` so 0.42 freight can no longer load or unload from an arbitrary single rail beside an outpost.
- Every freight endpoint now validates the exact active owned outpost anchor and an already-loaded radius6 physical railhead before any cargo mutation.
- Minimum railhead: at least6 rail blocks, at least1 Powered Rail, at least1 Hopper and at least1 Lever or Redstone Block inside the outpost-anchor radius6.
- The actual Chest Minecart rail must also be inside that radius; a Powered Rail and Hopper must be within squared distance9 of the cart rail, and a Lever/Redstone Block within squared distance16.
- Railhead validation uses only real current-world blocks, `level.hasChunkAt` and `level.mayInteract`. It stores no completion flag, so breaking the yard disables freight immediately and rebuilding it restores freight immediately.
- Freight still requires Industrial Works + Civil Works, active owned outpost within4, real Chest Minecart within4, same-dimension different destination, exact outpost Barrel cluster, and the existing bulk whitelist.
- Retained partial unload, real Container capacity, component-equal merge-first insertion and the cart-local owner/origin manifest.
- Added no automatic cart movement, route/path simulation, freight reward, supply cost, SavedData, packet/protocol, custom block/item/entity, teleport, chunk ticket or force-load.
- Updated production status, radial UI, guide, source audit and JAR verification for the physical railhead contract.

## 0.42.1-alpha.1
- Reworked the in-game guide into a scrollable gameplay reference: body scissor clipping, mouse-wheel scrolling and a visible scrollbar replace the old hard bottom cutoff.
- Removed patch-note wording such as `0.42부터` from `GuideScreen`; release history remains in this file instead.
- Added explicit World Ascension guidance: world start Stage0 `각성`, first Wither kill Stage1 `전설`, first Ender Dragon kill Stage2 `종말`, plus current consequences/unlocks.
- Accelerated early mastery progression with a smooth discount that fades out by Lv60. Approximate cumulative XP: Lv10 1190->453, Lv30 17520->11610, Lv60 121890->107010, Lv90 394110->379230, Lv100 536150->521270.
- Existing saved total XP is preserved; because level is derived from total XP, old characters may resolve slightly upward under the new thresholds.
- Replaced flat `valuable_ores = 20 XP` mining valuation with material tiers while keeping the same tag for vein/extract eligibility.
- Key mining values: Copper7/8, Iron9/10, Gold12/13, Diamond18/20, Emerald20/22, Ancient Debris24, Obsidian16, Crying Obsidian18. This removes the old copper20 > obsidian6 inversion.
- Clarified player-facing `배럴` terminology as Minecraft's normal `통 (Barrel)` block in the guide, production radial and field-logistics messages.
- Reversed shared logistics-backed material consumption order: nearest usable physical logistics `통` Containers first, then player inventory only for the shortfall.
- The storage-first order automatically affects existing systems already using `FieldDepotService.consume*` such as Construction, Infrastructure and replant/material sinks; no virtual storage or new resolver is added.
- Vanilla crafting-table grids remain vanilla and do not remotely pull storage. Apex/Ascension Trial entry also remains carried-only.
- No new SavedData, packet/protocol, custom block/item/entity, force-load or third-party asset/dependency.

## 0.42.0-alpha.1
- Added `Physical Freight Relay / 물리 화물 중계` so actual logistics stock can move between physical outposts instead of roads/rails remaining decorative infrastructure.
- Added `FreightService` using vanilla Chest Minecarts, rails, real Barrel Containers and existing `FieldDepotData`/warehouse links; no new custom entity/block/item or dependency.
- Added `ProductionService.ACTION_FREIGHT = "physical_freight"` and `물리 화물 수레` to the existing Industrial Works radial; existing network protocol8 and InfrastructureActionPayload are reused.
- Freight requires both completed Industrial Works and Civil Works, an active owned outpost within4, a Chest Minecart within4 and already-loaded rail at/below the cart.
- Departure loading requires a completely empty cart and moves only the existing `FieldDepotService.isBulkMaterial` whitelist from that exact outpost's registered Barrel anchor + its linked warehouse Barrels.
- Loaded freight stores only owner UUID and origin outpost dimension/x/y/z on the Chest Minecart's persistent NBT; no global route/freight SavedData is added.
- The same physical cart must arrive at a different active owned outpost in the same dimension before unloading is allowed.
- Destination unloading inserts only into that destination's actual anchor + linked warehouse Barrels; partial unload is allowed and remainder stays in the cart.
- Freight insertion keeps component-equal merge-first behavior, `Container.canPlaceItem`, real stack/container limits and empty-slot fallback; source stacks shrink only by actually accepted quantities.
- Freight has no generated reward and no supply-charge cost. There is therefore no short-route reward exploit; the useful result is only physical relocation of existing stock.
- Added no cart spawn/auto-drive, cart/player teleport, abstract route registration, universal remote storage, `getChunk`, region ticket, force-load or cross-dimension transport.
- Create remains design-reference-only for the product lesson of physical stock movement. No Create train/contraption/package/Stock Link/routing source or assets are bundled.
- Retained 0.41 Civil Works causeways, 0.40 physical breachers, 0.39 Bastion defense, 0.38 Outpost defense, 0.37 warehouse clusters and older logistics/expedition/endgame contracts.

## 0.41.0-alpha.1
- Added `Civil Works Causeways / 토목 공사소·도로 교량 시공` to reconnect large resource throughput with persistent world engineering instead of adding another combat tier.
- Added Stage1 `CIVIL_WORKS`: Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256, funded through existing physical logistics.
- Added registered-Barrel Civil Works commissioning yard: radius6 Stone Bricks48 + Scaffolding16 + Iron Blocks4 + Stonecutters2 + Crafting Table1.
- Added `ConstructionMode.CAUSEWAY`: actual selected BlockItem as flat 3-wide forward deck at 17/33/49/65 length using the existing Construction queue.
- Added explicit `level.hasChunkAt(target)` bulk-placement boundary; no chunk tickets or force-load.

## 0.40.0-alpha.1
- Added Bastion-final-wave physical Ravager/Vindicator breachers targeting only qualifying fortification with full grief/protection hooks and ordinary block drops.

## 0.39.0-alpha.1
- Added `Physical Bastion Defense`: radius6..12 real fortification, four quadrants each12 unique fortified columns, supply2 /4 waves /6000 ticks.

## 0.38.0-alpha.1
- Added `Defendable Physical Outposts`: active outpost, supply1, three waves, anchor-directed mobs, breach radius6/limit200 and owner64 requirement.

## 0.37.0-alpha.1
- Added `Physical Warehouse Clusters`: each depot anchor may link max8 additional real Barrels inside radius6 in `field_depots_v1`.

## 0.36.0-alpha.1
- Added bounded real-world commissioning before finalizable late-project funding.

## 0.35.0-alpha.1
- Added explicit High-volume Field Offload from main inventory9..35 into nearest usable real Barrel stock.

## 0.34.0-alpha.1
- Added shared material resolver for industrial batches, unfinished infrastructure and equipment spending. 0.42.1 changes its consumption order to nearest usable physical logistics storage first, then player inventory. Apex/Trial entry stays player-carried.

## 0.33.0-alpha.1
- Added one bounded sortie complication to each new operation: DEEP_FRONT, FORWARD_SHIFT or HOT_EXTRACTION.

## 0.32.0-alpha.1
- Added nine repeatable physical out-and-back expedition operations staged from active regional outposts.

## 0.31.0-alpha.1
- Added prepaid one-use ordinary-death field recovery at active outposts; no ordinary fast travel.

## 0.30.0-alpha.1
- Added physical field outposts with Bed/Campfire/Crafting/Furnace structure, logistics64 and NATURAL-hostile safety24.

## 0.29.0-alpha.1
- Added real vanilla Barrel field depots, max3/player, one owner per physical position, no force-load.

## 0.28.0-alpha.1
- Added Stage1 Industrial Works, four production lines, bounded buffers and field-supply charges.

## 0.27.0-alpha.1
- Added Stage1 Apex Tracking Post and nine behavior-driven Apex Hunts.

## 0.26.0-alpha.1
- Added18 regional field incidents.

## 0.25.0-alpha.1
- Added two persistent directive options per expedition region.

## 0.24.0-alpha.1
- Reworked expedition discovery into persistent physical field objectives.

## 0.23.0-alpha.1
- Added nine expedition regions and Field Mastery progression.

## 0.22.0-alpha.1
- Added randomized Ascension Trial doctrines and4-affix Awakened Mythic progression.

## 0.21.0-alpha.1
- Added repeatable Stage2 four-wave Ascension Trial.

## 0.20.0-alpha.1
- Added final Lv100 Mastery VI across all six active skills.
