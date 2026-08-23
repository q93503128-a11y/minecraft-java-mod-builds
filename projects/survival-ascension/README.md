# Survival Ascension

Minecraft Java 26.2 / NeoForge 26.2.0.38-beta / Java 25. Network protocol `8`.

Survival Ascension makes progression increase the physical scale of player actions, then makes infrastructure, logistics, expeditions and combat consume that larger output again.

## 0.40.0-alpha.1 — Physical Siege Breachers / 물리 공성 파괴자
0.39 made a player-built wall matter through collision and pathing. 0.40 lets the unique final bastion wave answer that infrastructure physically instead of merely receiving more health or damage.

### Bastion-only wall breaking
Only siege mobs tagged as wave `4` are eligible, which makes this behavior exclusive to the four-wave Bastion encounter. Normal three-wave Outpost Defense does not gain block destruction.

Eligible breakers:
- Ravager: one successful fortification break per `30` ticks at most;
- Vindicator: one successful fortification break per `60` ticks at most.

They search only a tiny local area around themselves while approaching the outpost and may destroy only the same blocks that qualify as physical fortification:
- vanilla `WALLS` tag blocks;
- Iron Bars;
- Nether Brick Fence;
- only while the block lies inside the outpost fortification annulus radius `6..12`.

The target must also be in front of the attacker toward the outpost anchor. This prevents the system from chewing arbitrary scenery behind the wave.

### Protection and world-safety contract
A break is allowed only if all of the following remain true:
- the owning player still has an active defense runtime;
- the attacker carries the matching siege-owner tag and bastion-only wave4 tag;
- the outpost is same-dimension, owner-within64 and still physically operational;
- the candidate position is already loaded;
- `EventHooks.canEntityGrief(level, mob)` allows mob griefing;
- the owning player's `level.mayInteract(owner, pos)` protection gate allows interaction;
- the block itself allows entity destruction;
- NeoForge `EventHooks.onEntityDestroyBlock` is not canceled;
- the target has no block entity.

Destroyed fortification uses ordinary `destroyBlock(..., true, mob)` drops, so the wall material falls into the world and can be picked up and rebuilt with the existing Construction system. Barrels, beds, crafting/furnace blocks and arbitrary terrain are never selected by this breacher service.

No new SavedData, packet, client coordinate, custom block, custom item, chunk ticket, `getChunk` or force-load is added.

### Why this exists
The final bastion wave now creates a real physical failure mode: a wall that stops pathing can be opened by heavy/sapper attackers, after which the existing radius6 breach-pressure objective becomes much harder to hold. Construction mastery therefore matters both before and during defense without adding a generic armor percentage or second auto-builder.

## 0.39.0-alpha.1 — Physical Bastion Defense / 물리 요새 방어
0.38 made the outpost itself worth defending. 0.39 makes the player's large construction scale matter inside that defense loop without adding a second auto-builder or a passive flat armor percentage.

Use `M -> Infrastructure -> 산업 가공소 -> 요새 방어전` while within4 blocks of an active owned outpost anchor.

### Physical fortification rule
The server scans the already-loaded world around the actual outpost anchor:
- horizontal annulus radius `6..12`;
- vertical allowance anchor Y `-3..+4`;
- accepted defensive material: blocks in the vanilla `WALLS` tag, Iron Bars, or Nether Brick Fence;
- the annulus is split into NE / NW / SE / SW quadrants;
- each quadrant requires at least `12` fortified x/z columns;
- total practical minimum is `48` columns;
- multiple wall blocks stacked in the same x/z column count only once.

This prevents one tall pile in a corner from satisfying the structure. The wall is not converted into virtual durability and the mod gives no passive damage reduction. Its real collision and mob path obstruction are the defense benefit.

The structure is not saved as a new claim. Removing the wall simply means the next validation fails.

### Bastion defense
A valid physical fortification ring admits a harder optional siege:
- field-supply cost `2`;
- `4` authored waves;
- total deadline `6000` ticks / 5 minutes;
- same 3-second regroup window;
- same owner radius64 and real Barrel + Bed + Campfire + Crafting Table + Furnace-family outpost requirement;
- same breach radius6 / breach limit200;
- fortification quadrants are revalidated between waves;
- failure does not refund the two supply charges.

Normal 0.38 defense remains unchanged at supply1 / 3 waves / 4 minutes.

### Difficulty without blanket HP inflation
Bastion waves increase pressure through more simultaneous ranged/melee/disruption/heavy roles and additional Ravagers at the final wave. No bastion-only `MAX_HEALTH`, `ATTACK_DAMAGE`, armor or generic damage-reduction multiplier is added.

### Rewards
Stage1 bastion completion: Combat mastery XP650 + Construction mastery XP250 + Diamond4 + Amethyst32 + Echo6 + vanilla XP220.

Stage2 bastion completion: Combat mastery XP900 + Construction mastery XP350 + Diamond6 + Echo10 + Dragon Breath4 + Netherite Scrap1 + vanilla XP320.

Nearby surviving allies inside48 receive vanilla XP70. These are encounter rewards, not permanent flat character multipliers.

### Runtime boundaries
- uses the existing Industrial Works action payload; no protocol bump;
- no new SavedData ID or fortification flag;
- no automatic construction or block replacement;
- no client coordinate trust;
- no chunk ticket / `getChunk` / force-load;
- normal 0.38 encounter overlap and Field Recovery exclusions remain shared by both defense modes.

## 0.38.0-alpha.1 — Defendable Physical Outposts / 전초 방어전
Active real outposts can host a three-wave defense. Siege mobs advance toward the real anchor; enemies inside6 generate breach pressure and200 fails the defense. The owner must remain within64 and keep the actual outpost structure operational. Stage scaling changes mob composition rather than adding a siege-only blanket HP multiplier.

## 0.37.0-alpha.1 — Physical Warehouse Clusters / 물리 창고군
Each registered depot Barrel remains an anchor and may explicitly link up to8 additional real Barrels inside radius6. `field_depots_v1` is retained with optional `warehouse_links`; unloaded satellites are skipped/preserved and loaded-invalid links prune individually.

## 0.36.0-alpha.1 — Physical Commissioning Sites / 물리 준공 현장
Industrial Works, Apex Tracking Post and Ascension Nexus require a real bounded commissioning site before a finalizable funding call can cross completion. Existing completed worlds remain compatible.

## Retained field loop
- 0.34 Integrated Logistics Backbone: stationary sinks consume inventory first, then nearest usable real logistics Barrels.
- 0.35 High-volume Field Offload: explicit main inventory slots9..35 -> nearest physical Barrel capacity; hotbar/equipment remain carried.
- 0.30 Physical Outposts: owned depot + Bed/Campfire/Crafting/Furnace; logistics64 and NATURAL-hostile safety24 while active.
- 0.31 Field Recovery: prepaid one-use ordinary-death return within96; authored encounter deaths stay excluded.
- 0.32/0.33 Expedition Operations: physical out-and-back regional sorties with one bounded complication per new sortie.

## Combat / endgame boundaries
Apex entry remains player-carried Echo8 + Amethyst32 + Gold32. Ascension Trial entry remains player-carried Echo32 + Amethyst64 + Dragon Breath8. Physical logistics never become remote encounter payment.

## Final action scale
After Lv100 + all nine expedition regions:
- Quarry tunnel 7×7×12
- Woodcutting 448 logs
- Harvest 13×13
- Combat shockwave 7.5 radius / 20 targets
- Construction line65 / plane13×13
- Mobility air dash4

Large work remains tick-budgeted and uses normal protection/material paths. Shift remains the precision override.

## External references
0.40 keeps the existing permissive-code/reference-only policy in `THIRD_PARTY_NOTICES.md`. The new breacher implementation is independent Survival Ascension code and does not bundle another mod's source, assets, AI goals or configuration data.
