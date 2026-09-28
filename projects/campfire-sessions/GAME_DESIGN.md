# Campfire Sessions — Master Game Design

Status: CANONICAL DESIGN DRAFT
Updated: 2026-09-28

This document is the canonical gameplay/design source for the current expansion of Campfire Sessions.
The original music system remains part of the project, but the game direction is now a cozy multiplayer island-life simulation built on Minecraft Java 26.2.

## 1. Product direction

### Genre
Cozy Multiplayer Island Life Sim.

### High-level fantasy
Players live together on a persistent island village populated by cute animal residents.
Daily play revolves around collecting, fishing, bug catching, cooking, decorating, socializing, paying off housing debt, expanding the village, visiting other islands, discovering rare species, and taking part in seasonal events.

Combat exists, but it is secondary and kept away from the peaceful main village.

### Core loop
Collect / fish / catch bugs / explore
→ sell / donate / gift / cook
→ repay housing debt / expand home
→ decorate / collect furniture and clothing
→ build resident relationships
→ progress museum, public projects and sea routes
→ discover new seasonal content, rare species and events
→ continue island life.

### Existing Campfire Sessions systems
The existing music work is preserved and becomes one lifestyle pillar:
- Acoustic Guitar.
- Wooden Chair.
- 15 bundled CC0 tracks.
- Local Custom Music.
- inertial music carousel.
- repeat / auto-next / outside-UI controls.
- BPM feedback.
- third-person guitar performance pose.

Music can appear in campfires, cafés, birthdays, plaza performances, seasonal events and the major festival.

## 2. Non-negotiable production rules

### Private-use project
This project is intended for personal/private play, not public distribution.
This allows a wider choice of lawful assets, but does not permit bypassing paid access, DRM, access controls, or other restrictions.

### External design first
Do not invent the final visual design from scratch when a suitable external asset exists.
For all player-facing visuals, search/select real external designs first:
- maps.
- buildings.
- houses.
- resident character models.
- furniture.
- kitchens.
- plants, flowers and mushrooms.
- tools.
- boats.
- clothing.
- UI frames/components/icons.
- festival props and staging.
- presentation objects and decorative VFX assets.

The implementation should adapt systems to strong external art rather than producing improvised AI-looking placeholder art.

### No player-facing development residue
Do not ship:
- debug labels.
- test items.
- prototype textures.
- TODO text.
- P0/P1 language.
- internal stage names.
- developer logs.
- temporary player-facing resources.

Testing should live in code/tests/development tooling, not as content inside the game.

### Code quality
Do not accumulate Manager2-style duplicate systems, temporary shims, or throwaway gameplay code.
Large systems should be feature-oriented and data-driven.
Important shared state is server-authoritative.

## 3. World and map

### World delivery
The user should not manually download a world ZIP, copy a save, or install schematics.

Target flow:
1. Install the mod/modpack.
2. Create/select the island-life world preset.
3. The game prepares the approved world template automatically.

The final island map must therefore be directly obtainable during development and legally usable for this private-use project.

### Map requirements
The final map must be inspected as an actual world file before roles are assigned.
Do not assign island roles from screenshots or description text alone.

Required inspection:
- actual island count.
- usable area per island.
- flatness.
- coastlines.
- rivers.
- cliffs.
- caves.
- distance between islands.
- room for player homes and fixed facilities.
- suitability for Minecraft 26.2 conversion.

Preference:
- archipelago shape.
- broad, relatively flat central/buildable areas.
- additional islands with enough flat land for selected facilities.
- ocean boundaries.
- terrain large enough for multiplayer without becoming tedious to traverse.

A previously discussed candidate was “4K Flat Islands Map for Creative” because of its flat terrain and ~4000×4000 scale, but it is NOT canonical until the actual world file is directly obtained and inspected.
If it cannot be directly obtained, replace it with another directly obtainable flat archipelago map.

### Island roles
Island roles are not fixed yet.
They will be assigned after the actual world is inspected.
Avoid turning every island into a single-resource gimmick.
One island may have a main theme but should still contain multiple useful activities.

## 4. Fixed buildings and interiors

### Fixed exterior locations
All major buildings have fixed world coordinates/anchors.
Players do not freely relocate or manually build:
- resident services / administration.
- general store.
- museum.
- café.
- clothing shop.
- clinic/medical building if retained.
- pier/harbor.
- other major civic facilities.

Upgrades stay on the same anchor and swap to an upgraded external prefab.

### Construction presentation
Buildings should not instantly appear the moment a condition is met.
Where appropriate:
requirement met
→ construction state/site
→ completion after an in-game period
→ upgraded prefab.

Construction-site art should also use external assets.

### Exterior vs interior size
Exterior footprint does not limit interior size.

A small exterior can lead through a door/fade transition into a much larger dedicated interior cell/space.
This is especially important for:
- museum.
- player homes.
- large shops.
- café.
- other public buildings.

The interior-space system must work in multiplayer and keep entry/exit anchors deterministic.

## 5. Initial village facilities

The village starts as an actual village, not an empty construction site.

Initially available:
- resident services / administration.
- small general store.
- pier/harbor.
- basic communal living space / plaza context.

### Resident services NPC
Handles:
- island administration.
- housing.
- loans.
- public projects.
- residence/move-related services.
- household registration.

### Store NPC
Separate from the administration NPC.
Handles buying/selling and store-specific progression.

### Store progression
The store exists from the beginning and may later grow:
small shop
→ standard shop
→ larger shop.

The same fixed anchor is used; exterior prefab is swapped.

## 6. Housing and debt

### Shared housing pool
Player homes and animal resident homes use the same physical housing pool.

Target starting village scale is around 10–12 houses, subject to the final map inspection.

Single-player:
- one player home.
- remaining homes mostly occupied by residents.

Multiplayer:
- more player homes.
- fewer resident homes.

The village should not reserve eight obviously empty player houses.

### Choosing a house
New players do not pick from a sterile menu first.
They walk around the village, inspect available houses/plots/signs, and choose the location they want.

### No vacancy
If a new player joins and no home is available, an NPC resident can move out and that house can become available to the player.
The resident should not instantly vanish:
move decision
→ packing state
→ departure presentation at the pier
→ vacant house
→ player move-in.

### Debt
Housing debt is a central long-term progression loop.

Rules:
- no interest.
- no late fees.
- no timer pressure.
- repay at resident services.
- paying off the current loan does not auto-upgrade the home.
- after repayment, the player/household chooses whether to request the next expansion.
- expansion uses a construction/upgrade flow.

Provisional progression concept:
tent / starter state
→ small house
→ larger main room
→ additional room(s)
→ larger layout
→ second floor
→ final large house.

Exact stage count and prices remain for economy balancing.

House growth can unlock:
- larger interiors.
- more rooms.
- more storage.
- more exterior variants.
- more decoration capacity.

## 7. Household cohabitation

Players may choose to live together as one Household.

Household-shared state:
- money.
- collection encyclopedia progress.
- housing loan.
- house.
- home storage.

Separate households keep those states separate.

Household join/leave operations are managed through resident services.

Detailed split/leave rules must be finalized alongside save-data design to avoid exploits or lost state.
The design intention is that cohabitation feels like genuinely sharing one island home/economy, not merely sharing build permission.

## 8. Player-building restrictions and protection

Minecraft freedom is retained where it helps the life-sim and constrained where it can break the authored village.

Area types:
- public/civic area.
- player yard.
- player interior.
- wilderness/natural areas.

Public/civic area:
- major structure blocks protected.
- arbitrary destruction/building restricted.
- road/plaza/building access kept intact.

Player yard:
- flowers.
- decorative furniture.
- fencing.
- landscaping items.
- approved decoration.

Player interior:
- free interior decoration within the managed shell.
- furniture, wall/floor finishes, lighting, wall decor.

Wilderness:
- ordinary gathering/mining rules as allowed for that area.

Players cannot destroy core buildings, roads, plazas, stores, museum structures, the pier, or other critical authored content.

## 9. Residents

### Visual direction
Do not use vanilla villagers as the core cast.
Residents should feel like cute, highly characterful animal villagers.

Possible species depend on available high-quality external model sets, but desired categories include:
- cat.
- dog.
- rabbit.
- bear.
- fox.
- deer.
- squirrel.
- duck.
- frog.
- penguin.

The actual roster follows available external character assets and a consistent rig/style.

### Roster scale
Provisional target:
- initial complete candidate roster: ~20.
- long-term roster: ~30–40.
- active village residents: roughly 10–12 depending on player occupancy.

### Character data
Each resident should have data-driven:
- personality.
- speech tendencies.
- hobbies.
- favorite colors.
- favorite food.
- furniture/style preferences.
- schedule.
- home interior theme.
- clothing preference.
- special dialogue/events.

### Personality
Use a manageable set of broad personality families plus individual traits so same-personality residents do not become clones.

### Resident life
Residents actually use the village.
Possible activities:
- fishing.
- tending flowers.
- shopping.
- museum visits.
- café visits.
- walking.
- beach time.
- campfire gatherings.
- music performances/audience.
- festivals and contests.

Schedules react to:
- time.
- weather.
- season.
- hobbies.
- current events.

## 10. Resident relationships and moving

### Player relationship
Do not expose raw 0–100 friendship as the main UI.
Relationships are experienced through behavior and dialogue.

Conceptual stages:
stranger
→ neighbor
→ friend
→ close friend.

Higher relationship can unlock:
- nicknames.
- special dialogue.
- home invitations.
- resident visiting the player.
- gifts.
- photos/keepsakes.
- personal favors.

### Resident-to-resident relationships
Residents also relate to each other.
Light states such as:
- friendly.
- neutral.
- slightly incompatible.

Residents may:
- chat.
- do activities together.
- have small disagreements.
- ask the player to deliver a gift/apology.

Avoid permanent toxic feud simulation.

### Voluntary moving
Moving is normal life-sim content, not only a multiplayer vacancy mechanism.

A resident can consider leaving.
Players can ask them to stay.

Multiplayer rule:
for ordinary voluntary moving, if any player explicitly asks the resident to stay, the move is canceled.

Residents who leave should not be treated as erased forever; later cameos/visits/letters are desirable.

## 11. Resident favors

Do not build a giant RPG quest log around everyday resident requests.

Typical favors:
- catch a fish.
- catch a bug.
- bring a flower.
- bring furniture.
- bring food.
- deliver an item to another resident.
- fish together.
- take a photo.
- join an activity.

Rewards may include:
- money.
- furniture.
- clothing.
- recipes.
- relationship improvement.
- rare-species hints.
- occasionally expensive furniture, rare furniture or other genuinely desirable items.

Resident favors should match personality/hobbies where possible.

## 12. Resident homes

Resident interiors should express character.

Examples:
- fishing hobby → aquariums/fishing decor.
- music hobby → instruments.
- plants → planters/greenery.

Use external interior/furniture designs.

Residents may display selected player gifts.
Do not allow uncontrolled AI furniture replacement that gradually ruins authored interiors.
Use approved slots/style constraints.

## 13. Economy and shop

### Currency
Prefer one primary money currency.
Avoid stacking coins/miles/tokens merely to create complexity.

Money sinks:
- housing debt.
- house expansion.
- furniture.
- clothing.
- public projects.
- shop items.
- services.
- boats/routes where relevant.

Income:
- fish.
- bugs.
- crops/fruit/flowers/mushrooms.
- sea creatures.
- duplicate fossils.
- resident favors.
- exploration finds.
- crafted/cooked items where balanced.

No single repetitive activity should dominate all income.

### Shop rotation
Use:
- seasonal persistent stock.
- daily rotating items.

Furniture display stock:
generally purchasable per player rather than letting one multiplayer user permanently deny others.

Seeds/basic consumables:
readily available.

Very special items may be village-wide limited.

### Daily premium buy list
The shop can feature around 2–4 premium-buy categories/items for the day.
This should encourage variety without randomly devaluing rare catches.

### Catalog
Ordinary furniture that has been acquired once can register in a catalog and later be reordered for money, with delivery through mail.

Special event items, resident photos, unique trophies, etc. may be non-reorderable.

## 14. Tools

Lifestyle tools have infinite durability.
Do not create a repeated break-and-remake annoyance loop.

Progression comes from tool tiers.

Do not lock the design to only silver/gold.
Target roughly 4–5 meaningful tiers depending on available external tool art.

Possible conceptual progression:
basic
→ improved
→ silver-like
→ gold-like
→ master/high-end.

Final names/art follow the chosen external asset set.

Tool upgrades should improve action quality, for example:
- fishing timing forgiveness.
- gathering convenience.
- action speed.
- access to certain rare interactions.
- collection usability.

Do not turn tool tiers into an RPG damage-stat ladder.

## 15. Inventory and storage

Keep Minecraft inventory as a base rather than replacing everything with an incompatible custom inventory.

External inventory/tool-belt/backpack solutions may be used if they fit 26.2 and the final UX.

Current direction:
- tool-belt style convenience is desirable.
- a backpack system should only be added if item volume proves it is actually needed.
- house storage is a major separate storage solution.

House storage:
- private to the player/household.
- grows with house progression.
- unified/searchable storage is preferable to forcing dozens of vanilla chests.
- shared for members of the same Household.

## 16. Calendar, time and sleep

### Time scale
Do not use strict real-world 1:1 time.

Current design direction:
about 48 real minutes per in-game day.

This is provisional and can be tuned after playtesting.

### Offline/server time
When nobody is online, village time should pause.

### Sleep
One player cannot unilaterally skip the night while others are hunting night species.
Night skip requires a multiplayer vote/majority threshold.

### Calendar UI
Provide a calendar/weather screen using external UI assets.

Display:
- date.
- weekday.
- time.
- season.
- current weather.
- next-day weather.
- resident birthdays.
- scheduled events/festivals.

## 17. Seasons and weather

Season is not a label; it changes the world.

Season affects:
- plant visuals/growth.
- fish availability/activity.
- insect availability/life stage.
- mushrooms.
- resident behavior/dialogue.
- shop stock.
- festivals.
- weather.
- environment presentation.

Do not allow nonsensical weather such as normal summer snow.

Examples:
spring:
- more rain.
- flowers/new growth.

summer:
- rain/thunder possible.
- no normal snow.
- active butterflies/dragonflies.

autumn:
- leaf color/leaf fall.
- mushroom activity.

winter:
- snow instead of rain where appropriate.
- snow cover/ice.
- many insects dormant.

### Gradual transition
Do not hard-swap the entire environment at midnight.
Use a 2–3 in-game day transitional period:
late autumn leaf fall
→ frost
→ early winter snow.

Spring reverses gradually:
snow melt
→ shoots
→ more flowers.

## 18. Plants, farming and mushrooms

Expand flora significantly using external mods/assets.

Desired systems:
- seasonal flowers.
- flower breeding/rare colors.
- fruit trees.
- berries/crops.
- mushrooms.
- unusual/rare mushrooms.
- decorative plants.

Farming is a lifestyle activity, not an industrial automation economy.

Weather and season should matter.
Rain can support crops.
Large automated farms should not become the dominant money exploit.

## 19. Cooking

Cooking quality should come from proven external mods/assets rather than a low-quality custom substitute.

Current leading external candidates:
- Croptopia.
- Cooking for Blockheads.

Farmer’s Delight-style options may be revisited if a sufficiently stable Minecraft 26.2 NeoForge implementation exists.

No hunger/stamina survival pressure is required for this game.

Food is for:
- cooking collection.
- gifts.
- café.
- festivals.
- decoration/presentation.
- light lifestyle buffs.
- selling when balanced.

## 20. Fishing

Do not leave fishing as vanilla bobber RNG.

Desired interaction:
fish shadow
→ cast
→ fish approaches
→ possible false bites
→ real bite
→ player input
→ short resistance/reel interaction
→ catch.

Typical interaction should remain short, around several seconds rather than becoming a long fishing-RPG minigame.

Fish data may include:
- species.
- season.
- time window.
- weather.
- location.
- size.
- rarity.

Record personal/household best sizes.

## 21. Bugs and life cycles

Bug catching should rely on movement/behavior rather than right-click collection.

Examples:
- butterflies move slowly.
- dragonflies evade quickly.
- cicadas rest on trees.
- beetles flee if approached badly.
- fireflies appear at night.
- snails appear in rain.

Where suitable, life stages can exist:
egg
→ larva/caterpillar
→ pupa
→ adult.

Life-cycle timing and activity should fit seasons.

## 22. Sea activities

The ocean is active content, not just shoreline decoration.

Include:
- shoreline fishing.
- boats.
- offshore fishing.
- swimming.
- diving.
- sea-creature collection.

Diving can use visible shadows/movement to create pursuit gameplay.
Use external swimming/diving presentation assets where possible.

## 23. Fossils

Fossils are a collection system with multipart skeleton completion.

Dig spots appear periodically.
Use a shovel to excavate.
Museum curator identifies fossils.

Large prehistoric animals can require multiple parts such as:
- skull.
- torso.
- pelvis.
- tail.
- other major sections as needed.

As parts are donated, the physical museum skeleton should visibly become more complete.
Small species may need 1–2 pieces; large dinosaurs can need roughly 4–6 pieces.

Dinosaur/fossil models should be external high-quality assets.

## 24. Museum

The museum is a major shared village facility.

Possible wings:
- fish.
- bugs.
- fossils.
- sea creatures.
- nature/rare finds.
- art/antiques if retained.

Displays are physical/3D:
- fish in tanks.
- insects in exhibit spaces.
- fossils as assembled skeletons.

Museum has a dedicated curator NPC.

Donation state is shared at village level.
Collection encyclopedia state follows player/Household rules.

## 25. Personal displays

Players may display collected life in their homes:
- aquariums.
- terrariums.
- small sea-life tanks.
- flowers/mushroom specimens.

Use external display models and furniture.

## 26. Rare species and variants

Two rarity layers exist.

### Rare species
Some species are inherently rare and may require combinations of:
- season.
- time.
- weather.
- island/location.
- habitat.
- player behavior.

### Rare variants
Some caught specimens can also have special variants:
- unusually large.
- special color.
- unusual shell/pattern.
- giant mushroom form.
- other species-appropriate variants.

Therefore an inherently rare species can also have a rare variant, creating extremely desirable collector targets.

Do NOT make rarity only a 0.001% blind drop chance.
Conditions should make rare targets intentionally huntable.

### Hints
Do not increase hint quality merely because the player has played for a long time.

Useful hints should exist from the beginning somewhere in the world and be discoverable:
- resident rumors.
- museum text.
- books.
- notice board.
- visiting NPCs.
- environmental clues.

The player finds/uses clues rather than waiting for pity help.

## 27. Collection encyclopedia

One unified lifestyle encyclopedia with sections such as:
- fish.
- bugs.
- sea creatures.
- fossils.
- plants.
- mushrooms.

Possible recorded facts:
- season.
- time.
- weather.
- location.
- largest size.
- first discovered date.

Household members share encyclopedia progress.

Rare variants are desirable collection records but should not be required for ordinary 100% completion unless later explicitly decided.

## 28. Art and antiques

A visiting vendor may sell:
- paintings.
- sculptures.
- antiques.
- unusual decor.

Real/fake identification can exist, but must have clues.
Do not force blind guessing.

External artwork/models are required; do not create throwaway custom art.

## 29. Pier, routes and boats

Travel to other islands goes through the pier/harbor.

Flow:
select available route
→ pay fare
→ short boarding/departure presentation
→ transition/load
→ destination.

Do not make players physically sail for several real minutes merely to hide loading.

Routes unlock through life-sim progress such as:
- housing.
- museum.
- public projects.
- key discoveries.

Avoid RPG level gates.

### Boats
Boats are purchased with money.
Do not build a complicated boat skill tree.

Better boats can provide:
- improved appearance.
- improved travel presentation.
- access to farther routes.

Actual boat models must come from external assets.

## 30. Exploration and combat

Main village is peaceful.
Hostile natural spawns should not ruin resident areas.

Combat exists in:
- selected exploration areas.
- caves.
- dangerous outer regions.

Normal exploration has light danger.
Rare/dangerous locations may contain:
- small dungeons.
- bosses.

Do not turn the project into a level-1-to-100 combat RPG.

Combat progression is limited:
- better weapons.
- selected special tools.
- food buffs.
- exploration equipment.

Combat rewards feed back into island life:
- rare furniture.
- artifacts.
- fossils.
- plants.
- collection pieces.
- special materials.

## 31. Death

Avoid vanilla full inventory-drop punishment.

On death:
- return to house/clinic/safe point.
- keep core tools and money.
- potentially lose only some temporary exploration loot.

Exact penalty remains to be balanced.

## 32. Natural resource recovery

Multiplayer must not permanently strip the island.

Natural zones can restore:
- trees.
- flowers.
- mushrooms.
- rock/gathering spots.
- beach shells.

Player yards should not be overwritten by automatic natural regeneration.

## 33. Mail and player gifting

Each player/household has a mailbox.

Mail may contain:
- resident letters.
- resident gifts.
- catalog orders.
- event notices.
- player-to-player letters.
- attached items.

Moved-away residents may occasionally send letters.

Players can also:
- hand items directly.
- leave gifts.
- send furniture.
- send some money.

Do not add a global auction house/economy.

## 34. Gifts

Residents have preferences such as:
- color.
- style.
- food.
- flowers.
- hobbies.

Gift reactions should reflect preferences.
Birthdays can increase gift significance.

Gift spam should not be the only path to maximum friendship.

Residents may wear gifted clothing or display approved furniture.

## 35. Clothing and appearance

Collectible clothing is meaningful lifestyle content.

Possible categories:
- tops.
- bottoms.
- hats.
- glasses/accessories.

Use external clothing/model assets.

A wardrobe manages owned clothing.
A mirror can open appearance/clothing functionality.

External 26.2-compatible player-model systems may be used if stable, but avoid exposing a raw editor if a curated game-like wardrobe provides better UX.

## 36. Café

The café should be functional content rather than scenery.

Possible interactions:
- coffee.
- tea.
- desserts.
- resident visits.
- visiting NPC encounters.
- conversation.
- recipe discovery.
- music.

Use the existing music foundation for café ambience/performance.

## 37. Photography

A 26.2-compatible external camera/photo system is preferred over writing a low-quality substitute.

Use cases:
- birthdays.
- festivals.
- rare species.
- multiplayer memories.
- home decoration/photo albums.

## 38. Visitors and beach finds

Visiting NPC types may include:
- art dealer.
- rare plant seller.
- traveling musician.
- fishing specialist.
- bug specialist.
- antique dealer.
- clothing designer.
- explorer.

Visitors can also be hint sources for rare content.

Beach finds can include:
- message bottles.
- drift items.
- recipes.
- treasure maps.
- rare furniture clues.

## 39. Treasure maps and secrets

Do not reduce treasure hunts to raw coordinate text.

Prefer:
- actual maps.
- silhouettes/landmarks.
- environmental clues.
- resident rumors.

Potential secret sites depend on the final map:
- sea caves.
- behind waterfalls.
- ruins.
- old wells.
- isolated groves.
- hidden coastal spaces.

Do not finalize them until the actual map is inspected.

## 40. Village decoration and public projects

There is NO village rating/star-score system.
Do not penalize creative freedom with a cleanliness/beauty score.

Players may decorate approved public areas with externally sourced props such as:
- benches.
- street lights.
- fountains.
- signs.
- small gardens.
- fences.
- sculptures.
- picnic areas.

Public projects use a shared funding model:
- bridge.
- civic decoration.
- facility project.
- other authored public works.

Single-player funds it alone.
Multiplayer players contribute voluntarily.
Do not give governance power based on who paid the most.

## 41. Furniture

Furniture should primarily come from external high-quality packs/mods.

Candidate categories include furniture mods such as Skniro's Furniture / BetterDeco if they remain technically suitable.

Functional furniture where appropriate:
- chairs → sit.
- beds → sleep.
- lighting → toggle.
- storage → storage.
- kitchens → cooking integration.
- mirrors → appearance/clothing.
- music devices → music.
- TVs → limited entertainment/presentation if worthwhile.

Do not force functionality onto every decorative object.

## 42. Resident contests

Fishing/bug contests and similar events are village events.

Important multiplayer rule:
**multiplayer contests do not become player-only tournaments.**

Participants:
- currently participating/online players.
- eligible NPC residents.

Single-player still competes against residents.
Multiplayer mixes players and residents in the same event.

NPCs have differing competence.
Do not pre-script a fake winner.

Contest goals may include:
- largest fish.
- target species.
- catch count.

Rewards favor collectible items:
- event furniture.
- trophies.
- clothing.
- decorations.

## 43. Birthdays and seasonal events

Residents have birthdays.
Birthdays can include:
- small parties.
- gifts.
- other resident attendance.
- relationship-specific dialogue.

Seasonal events can include major themed days such as:
- spring flower event.
- summer fireworks/fishing event.
- autumn harvest event.
- winter snow/festival event.

Final event list should use external event decoration sets rather than improvised visuals.

## 44. Major multi-day festival

The game has no hard ending, but it can have a major milestone festival.

Trigger concept:
sufficient progress across major village systems such as:
- museum.
- home expansion.
- core facilities.
- sea routes.
- resident relationships.

The festival is NOT a single cutscene.
It lasts multiple in-game days and is playable.

During the festival:
- plaza/environment decoration changes.
- special music.
- special shops.
- event food.
- resident outfits/behavior.
- minigames.
- visiting NPCs.
- limited collectibles.
- performances.

The final day can have a large performance/finale using the Campfire Sessions music foundation.

After the festival, the game continues normally.

## 45. Competition, rarity and collectability philosophy

The game should include genuinely hard-to-obtain content.
Collection desire comes from:
- rare species.
- rare variants.
- multipart fossils.
- limited event items.
- expensive furniture.
- rare visitor stock.
- contest trophies.
- special gifts.

Avoid pure opaque grind.
Difficult targets should usually be understandable through world clues and conditions.

## 46. Multiplayer authority

Important gameplay state is server-authoritative:
- money.
- loans.
- household membership.
- encyclopedia progress.
- donations.
- houses.
- residents.
- resident move state.
- shop purchase state.
- public projects.
- calendar.
- weather/season.
- event state.
- protected-world state.

Client responsibilities are presentation/input/UI, not final authority.

Do not claim multiplayer correctness until actual multiplayer testing occurs.

## 47. External UI requirement

Core UI must use selected external UI design assets.
Do not improvise the final visual language.

Screens likely include:
- calendar/weather.
- shop.
- loans/housing.
- encyclopedia.
- mail.
- resident interactions.
- catalog.
- wardrobe.
- museum information.

Keep one coherent external design language rather than mixing unrelated asset styles.

## 48. Open decisions / unresolved work

These are intentionally not yet canonical:
- final island map.
- exact island role assignment.
- exact house count after world inspection.
- exact resident asset pack and final species roster.
- exact building prefab set and anchors.
- exact furniture pack combination.
- exact tool art set and tier names/count.
- exact house loan prices.
- exact shop economy numbers.
- exact season length.
- exact contest schedule/reward tables.
- exact public-project list.
- exact combat enemies/boss count.
- exact camera/player-model/inventory dependencies.
- household split/leave edge cases.
- exact save schema.
- detailed festival unlock requirements.

Continue planning in batches of roughly eight genuinely new decisions.
Do not recycle already-decided topics merely to fill the count.
