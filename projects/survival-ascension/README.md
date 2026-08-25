# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `8`.

Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.

## 0.57.0-alpha.1 — Pre-Test Stabilization / 실플레이 직전 안정화
This release deliberately adds no new progression layer. It closes deterministic issues before the first broad gameplay pass.

Large Mining plane/vein/extract follow-up work now checks `hasChunkAt` before every newly discovered or re-read target. Woodcutting applies the same loaded-only boundary during connected-log/leaf discovery and when a queued fell job is drained after world state changes.

Expedition accounting is now symmetric with Mining/Harvesting: the player's first valid log break and first valid construction placement count once. Queued logs continue through the normal BreakBlock event and no longer receive a second manual increment, while successful bulk construction follow-ups retain their explicit one-per-placement credit. Shift precision therefore counts the real single action without opening bulk follow-up work.

A committed `TESTING.md` defines the first manual smoke order and pass/failure signals. No new SavedData, packet/protocol, item/entity, force-load, background simulation or external-mod version is introduced; network protocol remains `8`.

## 0.56.0-alpha.1 — Ranged Projectile Attribution Hardening / 원거리 발사자 귀속 안정화
Survival-recognized player-fired bow/crossbow projectiles now snapshot the firing player's UUID beside the existing affix/Shift launch state. The live `DamageSource` player remains authoritative when present; only when that source-player reference is missing does Survival resolve the still-online shooter from the same physical projectile.

That fallback now covers Combat damage scaling/impact burst, Combat kill XP and major-target expedition credit, Ascension elite-affix drops, Elite rank rewards/reactions, endgame-mutation rewards/reactions, and Warband leader rewards. An orphaned projectile is also no longer classified as environmental damage merely because its attacking-entity reference disappeared: environmental armor logic now requires both attacking entity and direct entity to be absent.

The fallback is deliberately bounded. It trusts only Survival-snapshotted ranged projectiles, resolves only a currently online player, queues no offline reward, and adds no new SavedData, packet/protocol, entity, force-load or background simulation. Network protocol remains `8` and the six locked external content-mod versions are unchanged.

## 0.55.0-alpha.1 — Native 26.2 Spear + Sulfur Integration / 26.2 스피어·유황동굴 통합
Minecraft 26.2의 `minecraft:sulfur_caves`를 기존 심층권 원정 태그에 편입했다. Sulfur/Cinnabar 계열은 바닐라 pickaxe-mineable 지형으로 기존 채굴 숙련 속도·XP·면적 작업을 받지만 `valuable_ores`로 승격하지 않아 광맥/추출 대상으로 오인하지 않는다.

Standard `minecraft:spears` equipment now joins Ascension Imprint/reforge/Mythic awakening/salvage while remaining absent from generated elite base loot. Vanilla Jab/Charge behavior remains authoritative. Combat Lv30+ adds only a zero-damage, zero-XP narrow drive line behind a direct spear target when the player or mount has real forward momentum; Shift suppresses this Ascension line for precision attacks. Base reach/targets are Lv30 3.5/1, Lv60 4.5/2, Lv90 5.5/3, Lv100 6.5/4, Field Mastery 7.5/5. Spear affixes are `관통` direct damage, `돌파` line reach, `숙련` kill XP, `대열` target count and `충압` push. The line is capped at reach9.0, targets8 and push1.10 and respects knockback resistance.

No new SavedData, packet/protocol, custom spear/entity, force-load or background simulation is introduced. Network protocol remains `8`.

## 0.54.0-alpha.1 — Mace Impact Ascension / 메이스 충격권 승천
Standard NeoForge `c:tools/mace` equipment can now enter Ascension Imprint, reforge, Mythic awakening and salvage without a Java dependency on the source mod. Maces are deliberately NOT added to the generated elite base-loot pool, so Survival Ascension does not bypass Trial Chamber or external-mod acquisition.

A real `minecraft:mace_smash` hit replaces the generic Survival melee cleave/shockwave branch with Mace-specific physical scaling. Vanilla owns its normal 3.5-block smash knockback unchanged. Survival Ascension only extends an outer hostile-only ring beyond 3.5 blocks: Combat Lv30 4.5/3, Lv60 5.5/6, Lv90 6.5/10, Lv100 7.5/14, and Lv100 + Field Mastery 9.0 blocks/20 targets. The outer ring deals zero damage and grants zero Combat mastery XP; it respects target knockback resistance and never expands into allies/passive mobs. Shift suppresses only the Ascension outer ring, leaving the vanilla mace smash intact.

Mace affixes are `충각` horizontal push, `진동` radius, `숙련` Combat XP on real kills, `분쇄` outer-ring target count and `격퇴` vertical lift. Runtime caps are radius10.5, targets26, horizontal push1.30 and lift0.28. No new SavedData, packet/protocol, custom mace/entity, force-load or background simulation is introduced.

## 0.53.0-alpha.1 — Shield Ascension / 방패 승천
Standard NeoForge `c:tools/shield` items now join Ascension Imprint, reforge, Mythic awakening, salvage and elite affix drops without optional-mod implementation dependencies. The original shield ItemStack and unrelated components remain intact.

Shield growth is active physical defense rather than another passive damage-reduction layer. When a real shield block succeeds, Combat Lv30/60/90/100 emits a zero-damage guard wave that pushes up to 2/4/6/8 nearby hostile targets within 2.5/3.5/4.5/5.5 blocks. Lv100 + Field Mastery reaches 6.5 blocks / 10 targets. Shift while blocking disables the wave and keeps the successful vanilla block as precision defense.

Shield affixes are `압력` knockback, `파동` radius, `대응` cooldown reduction, `진압` target count and `반동` vertical lift. Final runtime caps are radius8.0, targets14, horizontal push1.30, vertical lift0.28 and cooldown minimum6 ticks. The wave deals no damage and grants no Combat mastery XP; it never forces a failed shield check to succeed or increases the amount blocked. Cooldown is stored only in existing player persistent NBT, with no new SavedData, packet/protocol, custom shield/entity, force-load or background simulation.

## 0.52.0-alpha.1 — Ranged Combat Ascension / 원거리 전투 승천
Combat progression now gives bows and crossbows the same physical-scale growth principle as melee combat instead of leaving ranged play as only a numeric damage multiplier. NeoForge common `c:tools/bow` / `c:tools/crossbow` items can enter Ascension Imprint and elite affix drops can roll vanilla Bow/Crossbow bases.

A fired projectile snapshots its ranged affix values and Shift precision flag at launch. Swapping weapons after release cannot change the projectile's direct-damage affix, mastery-XP affix or impact-burst modifiers. Already-snapshotted projectiles keep that launch state when re-entering the level.

Ranged impact burst grows with Combat mastery: Lv30 2.5 blocks/2 targets, Lv60 3.5/4, Lv90 4.25/6, Lv100 5/8, and Lv100 + Field Mastery 6/10 before equipment bonuses. Shift at launch disables the burst for deliberate single-target precision. Ranged affixes are 강궁 damage, 산개 radius, 숙련 Combat XP, 연쇄 target count and 충격 burst fraction. The burst fraction is capped at 65%, and projectile-stored bonuses are clamped to their authored Mythic maxima.

This uses only the actual projectile's persistent NBT; it adds no custom projectile/entity, SavedData, packet/protocol bump, force-load, background combat simulation or optional-mod implementation dependency.

## 0.51.0-alpha.1 — Armor Ascension / 방어구 승천 성장
Armor is now part of the same ascension equipment loop as weapons and tools. Standard helmet/chestplate/leggings/boots tags can be used with `M -> 장비 -> 승천 각인`, then use the existing reforge, Mythic awakening and salvage path without replacing the original item or clearing unrelated components. Elite affix drops can also roll iron/diamond/netherite armor pieces.

Armor affixes only work while the piece is equipped: `수호` is general incoming-damage reduction, `불굴` strengthens defense below half health, `숙련` increases Combat mastery XP, `완강` helps against 8+ damage hits, and `보호` helps against damage with no attacking entity. The combined armor-affix damage reduction is capped at 35%, and armor mastery XP is capped at +32%, so four high-tier pieces cannot create an uncapped defensive multiplier.

This reuses the existing affix CustomData, material sinks and equipment action payload. No new SavedData, protocol bump, custom armor, attribute-rewrite system, force-load or optional-mod Java dependency is introduced.

## 0.50.0-alpha.1 — Regional Logistics Scale / 지역 물류망 확장
The physical logistics network can now grow with the world instead of remaining permanently capped at three anchors. Industrial Works supports 3 registered depots/outposts, completed Civil Works raises the live admission limit to 6, and completed Ascension Nexus raises it to 9 so all nine expedition fronts can remain established without repeated teardown and re-registration.

This is a capacity unlock, not remote logistics or automation. Every depot is still a real Barrel, every promoted outpost still needs its Bed/Campfire/Crafting/Furnace structure, and only loaded/interactable physical storage participates. Freight remains the same real Chest Minecart + railhead system and frontline launch costs still use the exact departure outpost warehouse.

Persistence stays on the existing `field_depots_v1` and `outpost_v1` SavedData IDs. Their absolute persisted safety cap is 9, while registration/promotion checks the player's current infrastructure before admitting a new position. The check occurs before outpost promotion materials or field-supply charges are consumed, so reaching a 3/6-stage limit cannot burn resources. Network protocol remains `8`; no new SavedData ID, packet, custom entity, force-load or background maintenance system is introduced.

## 0.49.0-alpha.1 — Frontline Freight Manifest / 전선 보급 화물
Physical freight gained an explicit frontline-loading mode without replacing ordinary bulk freight. At a valid departure railhead, normal selection keeps the existing general bulk load; Shift-selection on an empty Chest Minecart attempts one bounded frontline reserve manifest containing exactly expedition1 + normal-defense1 + Bastion-defense1 local loadout.

The manifest is food176 + iron56 + fuel8 + logs32 + stone bricks128, total 400 items. It is admitted all-or-nothing from the exact departure outpost Barrel cluster so slot-order junk cannot crowd out frontline materials. If the cart layout cannot accept the complete prepared manifest, moved stock is rolled back to the same physical source cluster. The frontline marker exists only on that physical cart NBT; there is no virtual cargo account, generated stock, auto-driving, teleport or force-load.

## 0.48.0-alpha.1 — Frontline Local Supply / 전선 현지 보급
Physical freight now feeds a concrete frontline sink instead of only rearranging bulk stock. Starting an expedition operation, normal outpost defense or Bastion defense still requires the existing global field-supply charge, but it also requires real material stock inside the exact departure outpost's registered Barrel + linked warehouse Barrels.

Local material costs:
- Expedition operation: food32 + iron8 + fuel8;
- normal outpost defense: food48 + iron16 + logs32;
- Bastion defense: food96 + iron32 + stone bricks128.

Food is the combined stock of wheat/carrot/potato/beetroot and fuel is coal/charcoal. These resources are already in the physical-freight bulk whitelist. The local-stock resolver never falls back to player inventory or a nearby different depot: players may hand-load the departure outpost or move supplies there with the real Chest Minecart freight loop.

The material precheck is non-mutating. Local stock is consumed only after the existing encounter/operation system reports that it actually started, so rejected starts do not burn the physical loadout. Existing supply-charge costs, expedition region/directive checks, fortification requirements, cooldowns, physical outpost rules and encounter behavior remain authoritative.

`M -> Infrastructure -> Industrial Works -> Production Status` now reports the nearby active outpost's food/iron/fuel/log/stone-brick stock, and the radial menu exposes the three local loadouts. No new SavedData, packet/protocol, custom item/block/entity, virtual cargo balance, teleport or force-load is introduced.

## 0.47.0-alpha.1 — Major External Targets / 외부 강적 원정 연동
The Birth of Steve is now locked to audited 26.2 NeoForge `0.7.0+mc26.2+neoforge` (`gKOBlOap` / `xls8dTZv`). Survival Ascension does not import TBS classes or copy its ARR content; the integration point is the Survival-owned optional EntityType tag `survivalascension:expedition_major_targets`.

The audited major-target set is `tbos:hour_cantor` and `tbos:phoenix_guardian` (The Last Curator), both declared `required:false`. A normal hostile kill still records the existing +1. A tagged major target adds bounded +3 `HOSTILES_KILLED` credit to the current regional directive and an already-active valid same-region expedition operation, but does not multiply incident counters.

Fractured Archive is a real separate TBS dimension (`tbos:fractured_archive`) using `minecraft:the_void` internally. It is deliberately not disguised as one of the nine expedition regions, and the existing rule that an active operation fails when leaving its origin dimension remains unchanged. Major targets killed outside the nine regions therefore create no fake regional progress; they still receive the stronger Combat mastery valuation (2.5× max-health basis, capped at 600 XP before the existing equipment XP multiplier).

The locked TBS file is `tbos-neoforge-26.2-0.7.0.jar`, SHA-1 `4d55c51685bff4247fa533c925f7641ce4880db3`. No new SavedData, packet/protocol, custom entity, force-load or hard optional-mod Java dependency is introduced.

## 0.46.0-alpha.1 — Resonant Tool Preservation + Shovel Earthworks / 공명 장비 보존·삽 토공
The locked Amethyst Resonance 26.2 NeoForge 1.0.0 binary was audited directly before changing integration. Its Resonant Sword/Pickaxe/Axe/Hoe are present in the corresponding vanilla item tags, while Resonant Shovel is in `minecraft:shovels`; 0.45 therefore had one real equipment gap because Survival Ascension did not yet accept shovels.

0.46 adds standard `ItemTags.SHOVELS` to Ascension Imprint and gives the shovel category real gameplay instead of dead affixes. Shovel-valid terrain (`BlockTags.MINEABLE_WITH_SHOVEL`) now uses Mining mastery speed/XP and bounded planar earthworks at the current Mining area scale. Shift stays precision. Shovels never inherit ore vein, Extract or Bore semantics.

The binary audit also verified why existing Resonant perks survive imprint/reforge/awakening: Survival Ascension keeps the same ItemStack/item identity and only writes its own nested CustomData + display-name layer. Amethyst Resonance's Resonant Pickaxe perks are tied to its actual item class, and Resonance Infusion copies the original armor stack then adds its own persistent `amethyst_resonance:resonant` DataComponent. Survival Ascension still does not imprint armor, and it never clears unrelated components.

The locked Amethyst Resonance file remains external ARR content: Modrinth project `8RyryQ7j`, version `no0B3Ssy`, SHA-1 `a3ac49a6202b7918d2ed22030df0b6e2906cdec8`. No source, class, texture, model, recipe or asset is copied into Survival Ascension.

## 0.45.0-alpha.1 — External World Integration / 외부 월드 원정 통합
Biomes O' Plenty 바이옴을 별도 원정 체계로 만들지 않고 기존 9지역 원정에 데이터 태그로 편입한다. `ExpeditionRegion`은 `survivalascension:expedition/<region>`을 바닐라 fallback보다 먼저 판정하며 BOP registry ID는 `required:false`라서 BOP가 없어도 코어가 로드된다.

0.44 호환 패치의 실제 누락이었던 `biomesoplenty:spider_nest`를 `glowing_grotto`와 함께 심층권에 추가했다. The Birth of Steve 핵심 적은 이미 Minecraft `Enemy` 계약을 따르므로 기존 전투 숙련에 들어오며, 비-Enemy 보스는 NeoForge 공용 `BOSSES` 태그 계약을 사용한다. 따라서 TBS 전용 Java 하드코딩은 추가하지 않는다.

0.44 외부 장비 승천 각인과 모든 물리 물류/전초/방어/원정 규칙은 그대로 유지한다. 그래픽 클라이언트·실제 외부 월드젠 스모크는 아직 실행하지 않았으므로 통과했다고 주장하지 않는다.

## 0.44.0-alpha.1 — External Gear Ascension Imprint / 외부 장비 승천 각인
The content pack now has additional equipment sources, so compatible external gear can be pulled into the same Survival Ascension rarity/affix/mastery loop instead of living beside it as disconnected loot.

Use `M -> 장비 -> 승천 각인` while holding an affix-free item in the main hand. The item must be single-stack equipment and use one of Minecraft's standard item tags: sword, pickaxe, axe or hoe. This intentionally avoids direct Java dependencies on optional content mods.

The current World Ascension stage decides the initial rarity:
- Stage0 `각성` -> 정예 I / 1 affix;
- Stage1 `전설` -> 승천 II / 2 affixes;
- Stage2 `종말` -> 신화 III / 3 affixes.

Physical-material imprint costs:
- Stage0: 자수정24 + 철12;
- Stage1: 자수정48 + 다이아4 + 금16;
- Stage2: 자수정96 + 다이아8 + 네더라이트 파편2 + 메아리8.

The same storage-first logistics resolver is used: nearby usable logistics `통` are consumed first and only the shortfall comes from player inventory.

Imprint does not replace an external item with a vanilla item. Existing item components stay on the same stack. Survival Ascension adds only its nested `survivalascension_affix` CustomData and display-name layer, while retaining a stored pre-affix base name so rerolls/awakening do not stack prefixes.

Once imprinted, the existing reforge/salvage/Mythic-awakening path and activity affixes work automatically: mastery XP, tool speed, mining area/vein, woodcutting chain, harvest area, weapon damage and cleave all read the same affix data.

This supports correctly tagged current and future mod gear without hardcoding `biomesoplenty`, `tbos`, `amethyst_resonance` or other optional namespaces. Armor is deliberately not imprinted yet because the current affix vocabulary is action/tool focused rather than passive armor-stat focused.

## 0.43.0-alpha.1 — Physical Freight Railheads / 물리 화물 하역장
0.42 made real Chest Minecarts carry actual outpost stock, but a single arbitrary rail beside an outpost was enough to use the system. 0.43 makes both endpoints into small real loading yards without turning freight into an automatic train network.

Use the existing `M -> Infrastructure -> 산업 가공소 -> 물리 화물 수레` action. At both departure and arrival, the exact active outpost `통(Barrel)` anchor must have a loaded radius6 railhead containing:
- at least6 blocks in `BlockTags.RAILS`;
- at least1 Powered Rail;
- at least1 Hopper;
- at least1 Lever or Redstone Block.

The Chest Minecart's actual rail must also be inside that same railhead. A Powered Rail and Hopper must be within squared distance9 of the cart rail, and a control block within squared distance16, so scattered checklist blocks do not count as a working platform.

The yard is not registered or saved. Every freight action reads the actual blocks with loaded-only and `mayInteract` checks. Break the yard and freight stops; rebuild it and freight immediately works again.

Everything important from 0.42 remains: Industrial Works + Civil Works, active owned outposts, same physical Chest Minecart, same dimension, different destination, exact source/destination `통` cluster, existing bulk whitelist, real Container capacity and partial unloading. There is still no generated freight reward or supply fee.

No auto-driving, pathfinding, virtual route, station SavedData, new packet, custom train/block/item/entity, teleport or force-load is added.

## 0.42.1-alpha.1 — Guide / Early Mastery / Logistics Priority
The in-game guide scrolls when its content is taller than the screen and explains current gameplay instead of patch history.

World progression:
- Stage0 `각성 단계` at world start;
- first Wither kill -> Stage1 `전설 단계`;
- first Ender Dragon kill -> Stage2 `종말 단계`.

Early mastery remains accelerated through Lv59 and converges to the late quadratic curve at Lv60. Approximate cumulative XP landmarks: Lv10 453, Lv30 11,610, Lv60 107,010, Lv90 379,230, Lv100 521,270.

Mining XP remains material-tiered rather than a flat valuable-ore value: Copper7/8, Iron9/10, Gold12/13, Diamond18/20, Emerald20/22, Ancient Debris24, Obsidian16 / Crying Obsidian18.

For Survival Ascension systems using shared physical logistics, consumption order is nearest usable registered/linked physical `통` first, then player inventory for the remaining amount. Vanilla crafting-table grids and Apex/Ascension Trial carried admissions remain separate.

## 0.42.0-alpha.1 — Physical Freight Relay / 물리 화물 중계
A real vanilla Chest Minecart physically carries the existing bulk-material whitelist from one owned active outpost warehouse cluster to another. Source stacks shrink only by accepted cart capacity and cart stacks shrink only by accepted destination capacity. Partial unload leaves remainder in the same cart.

The cart stores only owner UUID and origin outpost dimension/x/y/z on its own persistent entity NBT. No route SavedData, virtual cargo account, generated freight reward, auto-driving, teleport or force-load exists.

## 0.41.0-alpha.1 — Civil Works Causeways / 토목 공사소·도로 교량 시공
Stage1 `CIVIL_WORKS / 토목 공사소` consumes Stone Bricks2048 + Cobblestone1536 + Gravel1536 + Iron256 + Copper256 through nearby real logistics `통` first, then player inventory.

Its finalizable funding call requires an owned registered `통` within4 and a loaded radius6 commissioning yard containing Stone Bricks48, Scaffolding16, Iron Blocks4, Stonecutters2 and Crafting Table1.

After completion, Construction Lv60 gains `도로/교량`. The manually placed BlockItem becomes a flat three-wide deck extending forward: Lv60 3×17, Lv90 3×33, Lv100 3×49, Field Mastery 3×65. Loaded-only, real materials, protection hooks, no terrain erase or free supports.

## 0.40.0-alpha.1 — Physical Siege Breachers / 물리 공성 파괴자
Only Bastion wave4 Ravagers/Vindicators may physically break qualifying fortification. Every break requires loaded terrain, `mobGriefing`, owner `mayInteract`, entity-destroy permission and NeoForge destroy-event approval. Normal three-wave defense remains non-destructive.

## 0.39.0-alpha.1 — Physical Bastion Defense / 물리 요새 방어
A real outpost fortification ring uses radius6..12, Y-3..+4, vanilla WALLS/Iron Bars/Nether Brick Fence and at least12 unique fortified x/z columns in each quadrant. Bastion costs supply2, has4 waves/6000 ticks and revalidates the physical ring between waves.

## 0.38.0-alpha.1 — Defendable Physical Outposts / 전초 방어전
Active real outposts can host a supply1 three-wave defense. Attackers advance toward the actual anchor; enemies inside6 generate breach pressure and200 fails the defense. Owner must remain within64 and keep the physical outpost structure operational.

## 0.37.0-alpha.1 — Physical Warehouse Clusters / 물리 창고군
Each registered `통` remains an anchor and may explicitly link up to8 additional real `통` inside radius6. Real Container contents only; unloaded storage is not accessed.

## 0.36.0-alpha.1 — Physical Commissioning Sites / 물리 준공 현장
Major late projects require a bounded real-world commissioning site before a finalizable funding call can cross completion. Existing completed projects remain compatible.

## Retained field loop
- Integrated Logistics Backbone: stationary logistics-backed sinks consume nearest usable real logistics `통` first, then player inventory. Frontline operation/siege loadouts are the deliberate exception: exact departure-outpost storage only. Apex/Trial entry stays player-carried.
- High-volume Field Offload: main inventory slots9..35 -> nearest real `통` capacity; hotbar/equipment remain carried.
- Physical Outposts: owned depot + Bed/Campfire/Crafting/Furnace; logistics64 and NATURAL-hostile safety24 while active.
- Field Recovery: prepaid one-use ordinary-death return within96; authored encounter deaths stay excluded.
- Expedition Operations: physical out-and-back regional sorties with one bounded complication per sortie and exact-outpost local stock at launch.

## Combat / endgame boundaries
Apex entry remains player-carried Echo8 + Amethyst32 + Gold32. Ascension Trial entry remains player-carried Echo32 + Amethyst64 + Dragon Breath8. Physical logistics never become remote encounter payment.

## Final action scale
After Lv100 + all nine expedition regions:
- Quarry tunnel 7×7×12
- Woodcutting 448 logs
- Harvest 13×13
- Combat shockwave 7.5 radius / 20 targets
- Construction line65 / plane13×13 / road-bridge deck3×65
- Mobility air dash4

Large work remains tick-budgeted and uses normal protection/material paths. Shift remains the precision override.

## External references
Permissive-code and reference-only boundaries are documented in `THIRD_PARTY_NOTICES.md`. Content-pack mods remain external pack dependencies; Survival Ascension does not embed their assets into its own JAR.
