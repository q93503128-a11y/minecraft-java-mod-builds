# Changelog

## 0.59.1-alpha.1
- Expanded integrated Biomes O' Plenty terrain from scenery into Survival-owned regional incidents; the field incident catalog is now roughly 37 variants.
- Expanded curated The Birth of Steve participation across expedition/interdiction/Apex pools through Survival-owned optional EntityType tags while preserving slot-replacement limits and avoiding story/boss-heavy actors.
- Added two bounded combat interdiction waves to long-form expedition operations; Forward Shift, Hot Extraction, Pursuit, Anomaly Surge, Hidden Ambush and Deep Front now materially change wave timing/composition.
- Added two additional archetype-specific Apex combat phases around 62% and 32% health for all nine regional Apex hunts.
- Expanded Amethyst Resonance into targeted Deep-operation and region-targeted Apex farming across pickaxe/axe/shovel/hoe/sword and the four armor slots, with Ascension II/III imprints on Apex rewards by world stage.
- Added Combat Lv90+ directional fracture-lane behavior, Mining Lv90+ quarry-network tunnel excavation, Woodcutting Lv90+ 2/3/4-tree forest chaining and Harvesting Lv90+ forward harvest lanes; Shift remains the precision/single-action override.
- Woodcutting remains bounded by the existing 256/384/448 total-log caps and tick-drained jobs; Harvesting retains tick-drained jobs and the 384 pending-target cap.
- Added CI staging for the exact seven locked Modrinth content JARs with size/SHA-1/SHA-512 verification before dedicated-server smoke. The integrated smoke now requires TBS expedition tags, Amethyst Resonance reward tags and a Biomes O' Plenty load signal before producing release artifacts.
- Network protocol remains 9. No new SavedData schema, force-load, unbounded scan or external implementation-class dependency was introduced.
- Content pack release: `0.59.1-alpha.1-content-preview.1`.

## 0.59.0-alpha.1
- Added Survival-owned optional EntityType tags `apex_escorts_tier_0`, `apex_escorts_tier_1` and `apex_escorts_tier_2` for curated content-pack participation in Apex hunts.
- Non-ocean Apex hunts may replace exactly one initial vanilla escort slot with one tagged external monster; total initial escort attempts are unchanged.
- Optional escort spawn failure falls back to the original vanilla escort in the same slot, preserving existing hunt admission and cleanup behavior.
- Successful content escorts glow and the Apex start message reports `이변 호위 1체 포함` so the variant is visible rather than hidden.
- Reused only the previously audited Armillary Scout / Blank Chronist / Gnomon Knight set. Minotaur and the Hour Cantor / Phoenix Guardian boss pair remain excluded from mixed escort allowlists.
- Added runtime `apex_escort_tier_0/1/2` census logging and retained dependency-free Java integration; no `tbos:*` registry ID or TBS implementation class exists in the bridge.
- Added no SavedData, packet/protocol, custom item/entity, force-load or passive simulation. Network protocol remains9.
- Content pack release: `0.59.0-alpha.1-content-preview.1`; all seven locked external project/version IDs remain unchanged.

## 0.58.0-alpha.1
- Added a 15% rare tier to bounded expedition field incidents: ~1.5x physical target scale, +15s, 2x mastery XP and stronger vanilla-material rewards.
- Added a once-per-second 48-block incident perimeter and same-level 112-block active-incident center clearance for multiplayer isolation.
- Added persistent, server-authoritative 5/9/17/33/49/65 LINE/CAUSEWAY length selection; Shift+click cycles in the construction radial while placement Shift stays precision-single.
- Added a one-time delayed `tbos:archivists_journal` compatibility cleanup using registry identity only; no TBS implementation dependency.
- Network protocol 9. External content dependencies remain locked to the same files.
- Content pack release: `0.58.0-alpha.1-content-preview.1`.

## 0.57.0-alpha.1
- Added loaded-chunk admission checks to Mining connected-vein discovery, planar area breaking and extract target re-read.
- Added loaded-chunk admission checks to Woodcutting connected-log/leaf discovery and queued fell-job execution.
- Fixed Woodland expedition accounting so the player's first valid log break counts once; removed the old secondary manual increment so queued logs are not double-counted.
- Fixed Arid construction expedition accounting so the player's first valid placement counts once while successful bulk follow-ups keep one credit each.
- Shift precision actions now progress the matching regional directive without starting bulk follow-up work.
- Added `TESTING.md` for the first manual gameplay smoke pass.
- Bumped content-preview lock to `0.57.0-alpha.1-content-preview.1`; all six external mod versions and network protocol8 remain unchanged.
- Added no SavedData, packet, custom item/entity, force-load or background simulation.

## 0.56.0-alpha.1
- Added firing-player UUID to the existing Survival ranged projectile launch snapshot.
- Added bounded online-owner fallback when a Survival-snapshotted projectile's `DamageSource` no longer exposes its ServerPlayer shooter.
- Routed fallback attribution through Combat damage/burst, Combat kill XP + major-target credit, Ascension elite-affix drops, Elite reactions/rank rewards, endgame-mutation reactions/rewards, and Warband leader rewards.
- Fixed armor `보호` environmental classification so a direct projectile with a missing attacking-entity reference is not treated as environmental damage.
- Kept live `DamageSource` ServerPlayer authority first; fallback is only for Survival-marked ranged projectiles and never queues offline rewards.
- Added no SavedData, packet/protocol, custom projectile/entity, force-load or background simulation. Network protocol remains8.
- Bumped content-preview lock to `0.56.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

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
- Bumped the content-preview lock to `0.54.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

## 0.53.0-alpha.1
- Added `Shield Ascension / 방패 승천`: NeoForge common `c:tools/shield` items now join imprint/reforge/Mythic awakening/salvage and elite affix drops.
- Added successful-block guard waves from Combat Lv30 upward: 2.5/2, 3.5/4, 4.5/6, 5.5/8, and Field Mastery 6.5/10 base radius/targets.
- Shift blocking suppresses the guard wave for precision defense while leaving vanilla successful blocking unchanged.
- Added shield affix roles `압력`/`파동`/`대응`/`진압`/`반동` for horizontal push/radius/cooldown/targets/vertical lift with hard caps 1.30/8.0/min6t/14/0.28.
- Guard waves deal zero damage and award zero Combat mastery XP; they never force a failed block to succeed or increase blocked damage.
- Preserved original shield ItemStack/components and network protocol8; added no new SavedData, packet, custom shield/entity, force-load or optional-mod implementation dependency.
- Bumped the one-import content-pack preview to `0.53.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

## 0.52.0-alpha.1
- Added `Ranged Combat Ascension / 원거리 전투 승천`: standard NeoForge bow/crossbow tags now join imprint/reforge/Mythic awakening/salvage and elite affix drops.
- Added launch-time projectile snapshots for ranged affix damage, Combat XP, burst radius/targets/fraction and Shift precision, preventing post-shot weapon-swap changes.
- Added mastery-scaled ranged impact bursts: Lv30 2.5/2, Lv60 3.5/4, Lv90 4.25/6, Lv100 5/8, Field Mastery 6/10 base radius/targets.
- Shift-fired ranged shots remain direct single-target precision shots with no impact burst.
- Added ranged affix roles: 강궁 direct damage, 산개 radius, 숙련 Combat XP, 연쇄 target bonus, 충격 burst fraction.
- Hard-capped burst fraction at 65% and clamped persisted projectile affix values to authored maxima; already-snapshotted projectiles are not re-snapshotted on level re-entry.
- Kept network protocol8 and added no custom projectile/entity, SavedData, force-load, background simulation or optional-mod Java dependency.
- Bumped the one-import content-pack preview to `0.52.0-alpha.1-content-preview.1` without changing the six audited external mod versions.

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
