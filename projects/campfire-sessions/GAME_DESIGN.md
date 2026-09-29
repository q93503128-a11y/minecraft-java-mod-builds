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

The Acoustic Guitar is a normal lifestyle item sold through the village economy rather than a development/test item or mandatory starter tool.
Baseline direction:
- purchasable from the general store once ordinary shopping is available.
- reasonably affordable compared with luxury furniture.
- catalog-reorderable after acquisition.
- additional external guitar visual variants may appear later through premium stock, events or the musician, but should not create a stat ladder unless a meaningful music-system difference exists.

Music can appear in campfires, cafés, birthdays, plaza performances, seasonal events and the major festival.

### Animation and performance standard
Important residents and player-facing lifestyle actions should use real contextual animation wherever it materially improves the feeling of a living village.

Resident animation priorities include:
- expressive idle/talk reactions.
- walk/run.
- sit/stand.
- eat/drink.
- fish.
- catch bugs where appropriate.
- gardening/watering.
- shop/work actions.
- sleep/wake.
- clap/cheer.
- sing/dance/play music.
- festival-specific actions.

Player animation priorities include:
- guitar performance.
- fishing.
- bug net use.
- watering/gardening.
- sitting/eating/drinking.
- selected social/photo/festival actions.

Do not leave major visible actions as a character merely standing still while an item/effect performs the action.

### Player emotes
Provide a lightweight emote/reaction wheel using the selected player-animation stack.
Useful reactions include:
- wave/greeting.
- clap.
- happy/celebrate.
- sad/disappointed.
- laugh.
- surprised.
- sit/relax where appropriate.
- simple dance.

Residents may react contextually to nearby player emotes.
This is social expression, not a monetized emote catalogue or complex MMO communication system.

### Resident voice texture
Resident dialogue should not be visually silent if a suitable external sound direction can be sourced.
Use short non-verbal/fictional speech chirps or vocal textures rather than full spoken-language voice acting.

Direction:
- personality/species may influence pitch, cadence or timbre.
- keep dialogue text fully readable.
- avoid copying another game's exact speech synthesis/sound identity.
- external licensed/CC0 sound assets or a reusable sound system are preferred before authoring a large custom voice library.

### Catch presentation
Successful fish/bug/sea-life catches should have a brief readable presentation:
- short hold-up/show pose.
- species/item name.
- special cue for first discoveries and rare variants.
- no long unskippable interruption after every ordinary catch.

The catch pose should reuse the player animation framework rather than being a disconnected camera cutscene.

The current guitar implementation's arm-only pose is a prototype, not the final presentation.
Final guitar performance should position the instrument against the torso and use a full-body performance animation:
- fretting hand on the neck.
- strumming/picking hand on the strings.
- torso/head rhythm and believable stance.
- seated and standing variants where practical.
- BPM-aware subtle motion without exaggerated looping.
- synchronized third-person presentation for other multiplayer clients.

Prefer a proven external player-animation library compatible with Minecraft 26.2 NeoForge rather than accumulating fragile hand-written renderer hacks.

### Recurring musician
Add one original recurring animal musician/singer-guitarist as a fixed special character.
This character is inspired by the role of a recurring live performer in life-sim games, but must have an original identity, visual design, dialogue and presentation rather than copying an existing copyrighted character.

Direction:
- uses the selected external resident/special-character visual pipeline.
- appears on a predictable recurring schedule roughly once per 7-day season.
- spends part of the visit casually practicing/being present before the main performance window.
- performs bundled Campfire tracks in the plaza/café/campfire/performance area depending on context.
- may accept player song requests from the bundled set during the performance period.
- can appear in birthdays/special events where appropriate.
- becomes a major performer in the multi-day festival finale.
- public-server-style monetization or music progression is not the goal.

Local Custom Music remains a player-side/custom feature and should not automatically become the canonical NPC performance catalogue unless multiplayer availability/sync is explicitly solved.

### Lightweight multiplayer jam sessions
Players who own guitars may join the same bundled-track performance session.

Baseline:
- one player starts/hosts a bundled song session.
- nearby players with a compatible instrument can join.
- the track timeline is shared rather than each client independently drifting.
- participants use synchronized performance animations.
- the same song should not become several overlapping copies of the audio merely because several players joined.
- leaving the session does not stop it for everyone unless the remaining session has no valid host/source.

This is social performance, not a rhythm-score minigame.
Do not require note-by-note input or accuracy grading unless a later separate minigame explicitly justifies it.

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

### Island identity and naming
When the village/world is first established, the creating player chooses the island/village display name before normal arrival play begins.

The chosen name is village-wide state and can appear naturally in:
- resident-services signage.
- notice-board headers.
- mail and event notices.
- museum/history text.
- harbor/route presentation.
- festival presentation.
- selected dialogue.

Do not use the folder/save filename as the player-facing village identity.
If renaming is supported later, it should be a deliberate resident-services action rather than a casual per-session setting.

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

### Home access during expansion
Housing expansion should not lock the player out of their home/storage for an entire in-game construction period.

During an upgrade:
- exterior construction presentation may appear.
- the existing interior and storage remain accessible.
- the new room/space becomes available when construction completes.
- if an exterior doorway/footprint is temporarily obstructed, route entry through a safe temporary interaction/transition rather than making the home unusable.

Construction is progression presentation, not a punishment that suspends the normal life loop.

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

## 6. First-day arrival and onboarding

### First-time character creation
Before a player's first arrival presentation, show a compact game-like character setup flow.

Core steps:
display name
→ base appearance
→ hair/face/skin options supported by the selected external appearance system
→ starter outfit
→ birthday
→ confirm
→ arrival at the island.

The player-facing **display name** is separate from the Minecraft account/profile name.
Persist identity by player UUID; the display name is presentation data used by residents, mail, museum history, permissions and social UI.
Require village-local uniqueness for display names to avoid ambiguity in mail, permissions and multiplayer UI.
Later appearance changes happen through the wardrobe/mirror system; changing account name must not break player save ownership.

Do not expose a raw technical model/skin editor as the normal first-run UX.
Use curated presets/layers/assets in a Campfire-specific character creator while allowing a compatible external avatar/skin system to provide the underlying rendering/look application when it materially improves quality.

The first session should then begin with a short authored arrival/departure presentation rather than dropping the player into a menu-heavy tutorial.

Baseline flow:
first-time character creation
→ arrival at the island/pier
→ brief welcome/context
→ resident services introduction
→ receive/access the basic lifestyle tools needed to start
→ freely walk around the village
→ inspect homes/plots in-world
→ choose a home
→ continue normal island life.

Avoid a long mandatory tutorial quest chain.
Fishing, shops, museum, residents and other systems should be discovered primarily through actual play, world layout, dialogue and lightweight prompts.

### Late-joining players
A player joining an already-progressed multiplayer village should enter the **current shared village state**, not replay village-wide progression from zero.

Shared progress remains shared:
- museum/facility progression.
- public projects.
- unlocked village routes.
- resident roster and village history.
- current calendar/season/weather.

The new player starts their own personal progression where appropriate:
- personal encyclopedia.
- resident relationships.
- clothing/appearance ownership.
- personal photos/records.
- birthday/profile state.
- personal house/Household membership choice.

Give late joiners the basic lifestyle tools/onboarding information needed to participate quickly.
Do not force them to repeat already-completed facility construction or village milestones merely for parity.

### Resident arrival/recruitment
New residents should normally be encountered before they permanently occupy a house.
Preferred sources include:
- visitors/camp-style stays.
- other-island exploration encounters.
- pier/travel encounters.
- special visitor/event contexts.

The player can invite an encountered resident when housing rules allow.
Do not silently populate an empty house with a completely unknown resident without any encounter/presentation, except for narrowly justified world-initialization cases.

## 7. Housing and debt

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

The exact number and shape of housing stages are deliberately **not fixed before the external house/prefab set is acquired**.
Choose the final progression after inspecting actual usable buildings so each step is visually and spatially believable.

It is acceptable to combine Minecraft block-built houses/prefabs from multiple creators when:
- the actual downloadable world/schematic/structure file can be obtained directly.
- usage conditions are acceptable for this private project.
- material palette, scale and village style can be made coherent.
- the upgrade sequence does not look like an absurd unrelated-building swap.

The final progression does not need to copy Animal Crossing's exact room sequence.
Use as many stages as the acquired buildings support cleanly without padding the loan loop with meaningless upgrades.

House growth can unlock:
- larger interiors.
- more rooms.
- more storage.
- more exterior variants.
- more decoration capacity.

### House exterior customization
Player homes should not all remain visually identical once suitable external building variants are available.
Keep the authored prefab shell protected, but allow curated exterior choices such as:
- roof color/material variant.
- door color/style.
- wall/exterior palette.
- mailbox.
- house sign/name plate.
- selected porch/trim/decor variants.

Only expose variants that are actually supported by the acquired external house assets.
Do not let players freely dismantle the protected prefab exterior block-by-block in the core village.

## 8. Household cohabitation

Players may choose to live together as one Household.

Household-shared state:
- money.
- housing loan.
- house.
- home storage.

Collection encyclopedia progress is NOT shared by Household.
Each player maintains their own encyclopedia.

A valid collectible can be handed, mailed or otherwise transferred to another player regardless of Household membership.
Receiving/owning that specimen can register it to the recipient's personal encyclopedia, so players may deliberately help each other complete collections.

Separate households keep their shared housing/economy states separate.

Household join/leave operations are managed through resident services.
Do not build a complicated automatic divorce/split settlement system for this private friends-focused multiplayer project.
Players coordinate the split themselves and manually move transferable money, furniture and items before/after leaving.
The existing house and its loan remain attached to the Household/home until changed through the normal housing/move flow.

The design intention is that cohabitation feels like genuinely sharing one island home/economy, not merely sharing build permission.

### Personal rooms inside a shared Household
A shared Household may designate one or more interior rooms as personal rooms when the final house layout supports it.

Personal-room intent:
- give cohabiting players an area they can decorate independently.
- allow personal storage/privacy without splitting the Household economy.
- preserve shared kitchens/living rooms/common storage as genuinely shared space.

Do not require every room to have its own permission matrix.
A simple room designation plus owner/Household/guest access state is enough.

### Guest permissions for other households
A player/Household controls what friends from other homes may do inside its house and yard.

Do not build a public-server ACL administration system with dozens of technical permissions.
Use a small set of understandable guest capabilities that can be configured per invited player, with simple presets plus optional individual toggles.

Useful capability groups:
- enter the house/yard when guest access is enabled.
- use non-destructive furniture and social objects such as chairs, instruments and games.
- harvest/replant crops or collect ordinary yard produce.
- access specifically guest-enabled containers/storage.
- place decorations/furniture.
- move/remove decorations/furniture.

Safe defaults:
- ordinary visitors can enter and use non-destructive social furniture.
- harvesting, storage access and editing the house/yard require explicit permission.
- Household members retain full normal Household access.

Guest permissions never grant:
- Household money/loan authority.
- house ownership transfer.
- structural house upgrades.
- resident-services authority on behalf of the Household.
- access to protected private storage that was not explicitly shared.

The owner/Household can change or revoke a guest's permissions without needing that guest online.

Permission UI has two layers:

**Preset / broad permission level**
- Visit Only.
- Social Use.
- Shared Living.
- Decorator.
- Full Trust.

These presets apply a sensible bundle of permissions in one action.

**Granular overrides**
After choosing a preset, the owner can individually allow/deny specific capabilities such as:
- enter while owner is away.
- use chairs/instruments/social furniture.
- harvest crops.
- replant crops.
- collect ordinary yard resources.
- open selected storage.
- place furniture/decor.
- move furniture/decor.
- remove furniture/decor.
- use kitchen/crafting-style lifestyle stations.
- interact with pets/display containers if such systems are added.

The UI should clearly show when a granular override differs from the selected preset.
Changing the preset may offer to reset overrides or preserve them, but must never silently broaden access beyond what the owner expects.

## 9. Player-building restrictions and protection

### Vanilla-survival boundary
Campfire Sessions is a life-sim game built on the Minecraft engine, not a vanilla-survival progression pack.

Keep useful Minecraft foundations:
- character movement/camera.
- block-based world geometry.
- inventory/container fundamentals.
- server/client entity simulation.
- basic combat physics where useful.
- rendering/resource-pack/mod interoperability.

Replace, suppress or heavily constrain vanilla-survival loops that conflict with the authored island-life game.

On the main village/island and authored life-sim spaces:
- no normal hostile-mob night-spawn loop.
- no hunger/stamina survival pressure.
- no repeated tool-break durability loop for lifestyle tools.
- no unrestricted tunneling beneath protected/authored village spaces.
- no strip-mining as the main progression economy.
- no requirement to place vanilla torches everywhere merely to make authored areas usable.
- no arbitrary destruction of roads, facilities, houses, museum, harbor or authored terrain.
- no Nether/End progression dependency for the core island-life game.
- no vanilla fishing as the final fishing system.
- no vanilla villager economy/cast as the main social system.

Lighting should primarily come from authored environmental lighting and appropriate lifestyle/exploration equipment.
Portable lantern/flashlight-style tools may exist where exploration benefits from them, but ordinary play should not become torch-spam management.

Mining/exploration uses authored caves, renewable nodes/pockets and controlled resource areas.
Free block breaking may still exist in explicitly designated wilderness/resource areas when it improves Minecraft tactile play without damaging the authored world.

Combat can retain familiar Minecraft movement/attack foundations, but enemy placement, rewards, difficulty, safe zones and progression follow Campfire rules rather than vanilla survival progression.

Minecraft freedom is retained where it helps the life-sim and constrained where it can break the authored village.

### Crafting and recipe UX
Do not make memorizing vanilla 2×2/3×3 crafting recipes a central progression skill.

Core lifestyle crafting should use understandable themed stations/UI such as:
- kitchen/cooking.
- workbench/utility crafting.
- furniture/woodworking where needed.
- clothing/sewing/customization where supported.
- exploration-equipment preparation.

When a recipe is known, show required ingredients and the result directly.
Use external high-quality crafting/kitchen/UI systems where suitable before writing a parallel custom system.

Vanilla crafting may remain for a small number of low-level utility interactions if harmless, but the main game should not feel like a recipe-book survival mod.

### Building and landscaping freedom
In core village/home areas, prefer curated life-sim placement over unrestricted survival building.

Player interior/yard freedom should focus on:
- furniture.
- decoration.
- plants.
- fences.
- paths.
- approved landscaping objects.
- house customization options.

Do not encourage:
- dirt/cobblestone towers.
- arbitrary walling-off of roads.
- giant improvised block structures inside protected residential parcels.
- destructive terrain edits that erase the authored village.

Provide a dedicated landscaping layer/tool for approved yard changes such as:
- grass/dirt/sand surface variants.
- paths.
- small pond/water features where technically safe.
- modest terrain height/edge adjustments.
- garden beds and decorative ground treatment.

Large-scale cliff removal, river rerouting or unrestricted terraforming is not part of ordinary residential play.
Broader normal Minecraft block placement/breaking can remain in specifically designated wilderness/resource zones when it benefits tactile play.

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

Yard area may expand modestly with selected housing progression stages when the actual map/house spacing supports it.
Expansion must be capped by real anchor/parcel boundaries so one player's yard never grows into a neighbor's home, civic space, road or protected landscape.
The exact yard sizes are chosen only after the final map and housing anchors are inspected.

Player interior:
- free interior decoration within the managed shell.
- furniture, wall/floor finishes, lighting, wall decor.

Wilderness:
- ordinary gathering/mining rules as allowed for that area.

Players cannot destroy core buildings, roads, plazas, stores, museum structures, the pier, or other critical authored content.

## 10. Residents

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

### Character identity and data
Each approved resident appearance/model is bound to one fixed authored resident identity.
Do not procedurally reroll a model into a different named character between saves.

After the external resident model/rig set is selected, each resident entry binds:
- stable resident ID.
- one fixed appearance/model/variant.
- one fixed name.
- birthday.
- personality.
- individual traits.
- speech tendencies.
- hobbies.
- favorite colors.
- favorite food.
- furniture/style preferences.
- schedule.
- home interior theme.
- clothing preference.
- special dialogue/events.

Names and final identities should be assigned after the actual external resident visuals are selected so the character concept can fit the appearance rather than forcing art to match a prewritten name.

### Personality
Use six broad personality families as the baseline, combined with individual traits so same-personality residents do not become clones.

Each authored resident should combine:
- one broad personality family.
- two or more individual traits.
- one or two hobbies.
- likes/dislikes and style preferences.
- resident-specific dialogue/events where worthwhile.

The combination is an authoring tool for character variety, not a reason to procedurally randomize personalities every save.

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

### Schedule strictness
Resident scheduling should use broad authored time blocks and weighted activity choices rather than brittle minute-by-minute choreography.

Examples:
- morning.
- daytime.
- evening.
- night.

Within a time block, weather, hobby, season and current events can choose among valid destinations/activities.
Important work shifts for staffed facilities may be more fixed.

If pathing fails or a resident becomes badly desynchronized, recovery should prefer safe correction while the resident is not visibly being watched rather than obvious repeated teleporting in front of players.

### Dialogue repetition control
Resident dialogue quality depends on avoiding obvious repetition.
Use data-driven dialogue pools with context tags such as:
- personality and individual traits.
- relationship stage.
- season/weather/time.
- current location/activity.
- nearby residents.
- recent gifts/favors/events.
- recent player activity where useful.

Track a short recent-history window per resident/player pair and strongly suppress recently used lines.
Do not solve repetition by generating uncontrolled free-form text at runtime; authored/curated lines and combinations remain the source of truth.

## 11. Resident relationships and moving

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

Friendship does not decay merely because the real-world player was offline.
Absence reactions such as "long time no see" are based on elapsed **in-game world/calendar time since the last meaningful interaction**, not wall-clock time or login/logout duration.
If the player disconnects and returns before even one meaningful in-game day has passed, residents should not act as though a long absence occurred.
Offline/server-paused time does not advance these absence reactions.

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

Moving consideration should be relatively rare so residents have time to become familiar characters:
- as a baseline, the village should see roughly one voluntary move consideration every 1–2 in-game seasons rather than constant weekly churn.
- recently arrived residents are protected from immediately becoming move candidates.
- exact weighting can account for roster size and player count, but frequent turnover is not the goal.

Multiplayer rule:
for ordinary voluntary moving, if any player explicitly asks the resident to stay, the move is canceled.

Residents who leave are not erased.
They retain their identity and prior relationship history and may later:
- send letters.
- appear as visitors.
- be encountered again on travel/exploration.
- attend suitable festivals/special events.
- be invited back when housing allows.

A returning resident should recognize the player and prior relationship rather than behaving like a fresh clone.

## 12. Resident favors

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

## 13. Resident homes

Resident interiors should express character.

### Home access
Do not gate ordinary resident-home entry behind friendship levels.
A player may enter whenever that resident home's door is currently **unlocked**.
If the resident has locked the door for the current schedule/state, entry is blocked.
The lock state—not an arbitrary relationship threshold—is the primary access rule.

Examples:
- fishing hobby → aquariums/fishing decor.
- music hobby → instruments.
- plants → planters/greenery.

Use external interior/furniture designs.

### Pet direction
Do not add ordinary vanilla-style cats/dogs as a default household-pet system while the core resident cast is itself made of anthropomorphic animal characters.
That creates an awkward world-rule contrast.

Aquariums, terrariums and specimen displays already provide living home decoration.
Small non-anthropomorphic companion creatures may be reconsidered later only if a strong external model/behavior set fits the final world style.

Residents may display selected player gifts.
Do not allow uncontrolled AI furniture replacement that gradually ruins authored interiors.
Use approved slots/style constraints.

### Resident outfits and seasonal presentation
Where the selected resident model/rig supports it, residents can switch authored clothing/accessory variants for:
- rain/umbrellas/rainwear.
- winter/cold-weather clothing.
- birthdays.
- festivals/contests.
- café/work roles.
- special musician/performance states.

Do not require dozens of unique outfits per resident.
Use a manageable shared wardrobe system plus a smaller number of character-specific signature variants.

### Resident-to-resident physical interactions
Residents should sometimes physically acknowledge each other rather than only firing dialogue:
- face each other while talking.
- sit together.
- wave/greet.
- clap/cheer.
- brief disagreement/reconciliation gestures.
- participate together in hobbies/events.

These interactions should be schedule/context driven and lightweight enough not to create pathfinding deadlocks.

## 14. Economy and shop

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

Economy balancing should be benchmarked against proven cozy/life-sim economies such as Animal Crossing and Stardew Valley rather than invented in isolation.
Do not copy their raw prices directly; use them to establish useful ratios between:
- ordinary daily purchases.
- normal furniture.
- house upgrades.
- public projects.
- premium services.
- rare/high-end furniture.

Luxury/designer/high-end items should be meaningfully expensive and remain aspirational even after ordinary daily purchases become easy.

### Shop hours
The general store and comparable staffed shops use **07:00–23:00 in-game time** as the baseline staffed opening window.

When a shop is closed:
- no buying.
- no selling.
- no after-hours kiosk that bypasses the closure.
- no shop-based catalog ordering.

Special event/facility schedules may differ when clearly announced, but the ordinary general-store schedule is 07:00–23:00.
The 24-hour player-facing clock makes this schedule explicit rather than presenting it as a vague daypart.

### Other facility access / staffed hours
Facilities do not all need the same schedule.

Baseline:
- resident services: staffed 06:00–24:00.
- museum galleries: accessible 24 hours; curator/donation service normally 07:00–23:00.
- café: 07:00–24:00.
- clothing shop: 09:00–21:00.
- clinic: recovery/emergency access remains available at all times; staffed optional services normally 07:00–23:00.
- notice board: always accessible.
- harbor/pier: the physical area is always accessible; individual routes may have authored schedules only when that improves the route/event design.

When a staffed service is closed, do not silently replace it with a generic kiosk unless that facility specifically has a justified self-service function.
Exact special-event exceptions must be visible in the board/calendar schedule.

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
Ordinary furniture that has been acquired once can register in a catalog and later be reordered for money.
Catalog orders are delivered through the mailbox on the **next in-game day** rather than appearing instantly.

Basic items that may be needed immediately should remain available through ordinary shops/services instead of turning catalog delivery into friction.

Special event items, resident photos, unique trophies, etc. may be non-reorderable.

## 15. Tools

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

Acquisition should not be money-only for every tier.
Baseline structure:
- basic tools are readily purchased/obtained early.
- higher tiers generally combine money with a lightweight lifestyle-progress condition, suitable material, craft/service unlock or relevant resident/artisan service.
- requirements should connect to the activity without becoming a large RPG crafting tree.
- final requirements/names follow the selected external tool-art set and its believable progression.

Do not turn tool tiers into an RPG damage-stat ladder.

### Fishing bait
Basic fishing never requires consumable bait.

Optional bait can be used to make deliberate collection hunting more convenient, for example:
- attract a habitat/species group.
- improve the chance that a wanted size/rarity class approaches when the environmental conditions are already valid.
- reduce unwanted catches for a short period.
- support a small number of special fishing interactions.

Bait must not bypass core season/time/weather/location requirements for rare species unless a specific authored bait is explicitly designed to do so.
Do not make routine fishing depend on repeatedly farming bait.

## 16. Inventory and storage

Keep Minecraft inventory as a base rather than replacing everything with an incompatible custom inventory.

External inventory/tool-belt/backpack solutions may be used if they fit 26.2 and the final UX.

Current direction:
- tool-belt style convenience is desirable.
- portable extra storage is expected because specimen individuality can create many distinct stacks.
- prefer a proven external 26.2 NeoForge backpack/storage system over replacing the Minecraft inventory.
- sell useful portable storage through normal shops rather than treating it as debug/dev convenience.
- desirable specialized containers include a general backpack, fish container and insect container.
- if no strong external dedicated fish/insect container exists, use the selected external backpack system and add only the smallest Campfire-specific category/filter layer needed rather than building an entire parallel inventory framework.
- house storage is a major separate storage solution.

### Specimen stacking
Collected fish, bugs, sea creatures and other individualized specimens carry gameplay-relevant specimen data.

Stacking rule:
- specimens with the **exact same stack-affecting specimen signature** may stack normally.
- any difference in size, color, pattern, special variant or other collection-relevant trait makes them distinct inventory stacks.
- do not attach irrelevant per-item timestamps/IDs to the stack signature when that would prevent otherwise identical specimens from stacking.
- first-discovery date/history belongs in player save data rather than forcing every physical specimen item to be unique.

Storage/container systems must preserve these distinctions and must never silently merge non-identical specimens.

House storage:
- private to the player/household.
- grows with house progression.
- unified/searchable storage is preferable to forcing dozens of vanilla chests.
- shared for members of the same Household.

### Dropped items and lost-and-found
Do not use vanilla short despawn behavior for important lifestyle possessions.

Protected/recoverable categories include:
- furniture.
- clothing.
- photographs.
- unique/event items.
- important collectibles.
- rare specimen items.
- progression/key items.
- other items explicitly marked recoverable.

If such an item is left unattended in the protected village/home area long enough to require cleanup, move it into a **Lost & Found** service at resident services rather than deleting it.

Lost & Found may also receive:
- decorations displaced by building upgrades/construction.
- recoverable items moved out of invalid protected placements.
- selected items recovered from safe system cleanup.

Ordinary low-value materials/resources may still use normal cleanup/despawn rules after a reasonable grace period so the world cannot accumulate infinite dropped entities.
Lost & Found is data-backed and should not be exploitable as infinite deliberate storage; repeated junk abuse can be rejected/condensed while important items remain recoverable.

## 17. Calendar, time and sleep

### Time scale
Do not use strict real-world 1:1 time.

Current design direction:
about 48 real minutes per in-game day.

The player-facing clock still represents a normal 24-hour day with **hour:minute** time, preferably 24-hour notation such as:
- 06:37.
- 14:05.
- 23:48.

At exactly 48 real minutes per 24-hour in-game day:
- 1 in-game hour = 2 real minutes.
- 1 in-game minute = 2 real seconds.

Internally, schedule/event systems should use in-game clock time rather than exposing real-time compression math.
Player-facing schedules, shop hours, resident appointments and events can therefore display exact in-game start/end times such as 18:00–22:00.

This is provisional and can be tuned after playtesting; if the day duration changes, the in-game 24-hour clock representation remains the same.

### Offline/server time
When nobody is online, village time should pause.

### Sleep
One player cannot unilaterally skip the night while others are hunting night species.
Because this is private friends-focused multiplayer, do not over-engineer public-server governance or voting systems.
Night may only be skipped when the currently active players have mutually agreed through the normal sleep flow; no player gets a unilateral force-skip action.

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

Significant forecast events such as strong storms should also generate a clear notice-board/weather warning in advance when the forecast system knows about them.
This allows players to plan rare-species hunts, travel and outdoor activities without surprise punishment.

### Day rollover
Midnight/date change is a server-authoritative calendar event, not a forced global cutscene.

At the day boundary:
- date/weekday advance naturally.
- daily shop rotation becomes the new day's stock.
- ordinary beach finds/resource refresh timers may advance.
- mail/catalog deliveries scheduled for that day become available.
- resident/event schedules move to the new date.
- relevant weather/season transition state advances.

Do not:
- force all players to sleep.
- teleport everyone home.
- fade every client to black.
- interrupt an active fishing/combat/photo session solely because the clock reached 00:00.

Open transactional UIs should validate against the current server day when an action is committed so midnight cannot create stale-stock duplication or desync.

## 18. Seasons and weather

### Season length
Each season lasts **7 in-game days**.
At the current target of about 48 real minutes per in-game day, one season is about 5 hours 36 minutes of active world time and a full four-season year is about 22 hours 24 minutes.
Offline/server-paused time does not consume these days.

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
Weather should have several presentation/intensity states rather than only binary clear/rain:
- clear/fair.
- light rain or light snow.
- normal rain/snow.
- strong rain/storm where seasonally appropriate.

Stronger weather may affect:
- ambience, wind/wave/audio presentation.
- resident schedules.
- selected fish/bug/sea-creature conditions.
- visibility and travel mood.
- a small number of special collection opportunities.

Do not make storms destroy buildings, permanently damage the village, or turn the game into disaster survival.

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

## 19. Plants, farming and mushrooms

Expand flora significantly using external mods/assets.

### Farming interaction
Watering exists as a light lifestyle interaction, not a daily chore that consumes a large part of play.

Do not add a generic star/quality-grade ladder to normal crops/harvests.
Collection/economy variety should come from species, season, rarity, authored variants and relevant specimen traits rather than multiplying every ordinary crop into several quality tiers.
- base crops should not demand excessive watering frequency.
- rain satisfies normal watering needs.
- upgraded watering tools increase area/range.
- higher tool tiers also reduce watering frequency by applying longer-lasting moisture/hydration, not merely a wider click area.
- top-end routine crop care should require substantially fewer watering actions than early play.
- exact moisture duration is finalized after the crop/tool asset and system choices are known.
- farming remains useful but should not become an industrial automation-first economy.

### Seasonal crop expiry and player warning
Seasonal crops may stop growing and eventually wither after their valid season, but the game must not expect the player to memorize exact crop calendars to avoid a large accidental loss.

Use overlapping warning channels:
- crop visuals visibly change before withering rather than going from healthy to dead without warning.
- seed/item tooltip and shop information show the crop's valid season in simple terms.
- planting too late for the crop to reasonably mature before seasonal expiry produces a clear warning rather than silently accepting a doomed planting with no feedback.
- the calendar/season UI can indicate that sensitive crops are approaching the end of their growing window.
- residents/shopkeepers/tutorial text may provide lightweight seasonal reminders.
- when a crop is already mature, the warning presentation should make it obvious that it should be harvested soon.

The player does not need to know an exact hidden death tick.
The goal is that attentive ordinary play gives enough notice to harvest in time.

Perennial/woody plants such as suitable fruit trees may enter dormancy instead of dying when out of season.

Expand flora significantly using external mods/assets.

Desired systems:
- seasonal flowers.
- fruit trees.
- berries/crops.
- mushrooms.
- unusual/rare mushrooms.
- decorative plants.

A dedicated flower-breeding/genetics system is NOT required.
Flower variety can come from species, seasonal availability and authored color variants without adding a separate breeding-combination subsystem.

Farming is a lifestyle activity, not an industrial automation economy.

Weather and season should matter.
Rain can support crops.
Large automated farms should not become the dominant money exploit.

## 20. Cooking

Cooking quality should come from proven external mods/assets rather than a low-quality custom substitute.

### Recipe discovery
Recipes should come from several understandable lifestyle sources rather than one shop or opaque random drops:
- residents.
- message bottles/beach finds.
- café interactions.
- seasonal events/festivals.
- visiting NPCs.
- discovering/using notable ingredients.
- exploration discoveries.

Recipe acquisition itself is collection/progression content because hunger/stamina pressure is not the purpose of cooking.

### Personal recipe book
Learned recipes are permanently recorded in that player's recipe book/profile.

The recipe book shows:
- discovered recipe.
- required ingredients.
- station/context needed.
- useful category/filter information.
- undiscovered entries only as silhouettes/hints when that improves collection motivation without spoiling the answer.

Recipe sources remain lifestyle discoveries such as residents, message bottles, café interactions, events, visitors, ingredients and exploration.

When a reward source would grant a recipe the player already knows:
- prefer another unknown recipe from the same appropriate pool when available.
- otherwise convert the duplicate into a small sensible fallback such as ingredients, food or ordinary money.
- do not create a separate recipe-token currency merely to solve duplicates.

Players may intentionally share/trade recipe items only where that specific recipe source is designed to be transferable; learning remains personal progression.

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

## 21. Fishing

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

Record personal best sizes.

### Multiplayer catch contention
Do not add artificial ownership locks merely because one player is approaching or attempting to catch a free-moving creature.

Examples:
- a butterfly that one player is chasing or swinging a net at remains catchable by another player until someone actually catches it.
- players may compete for free-moving bugs/sea-life/world specimens.

Protect only interactions where the target has already been logically bound to a specific active action and another player taking it would be physically nonsensical or corrupt the interaction.
For example:
- a fish currently hooked on Player A's fishing line belongs to that active fishing interaction.
- another player's rod/net cannot steal that already-hooked fish out of the line state.

Prefer simple authoritative interaction-state rules over generalized reservation timers.

## 22. Bugs and life cycles

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

## 23. Sea activities

The ocean is active content, not just shoreline decoration.

Include:
- shoreline fishing.
- boats.
- offshore fishing.
- swimming.
- diving.
- sea-creature collection.

Diving can use visible shadows/movement to create pursuit gameplay.

Do not add a separate stamina bar just for diving.
Use Minecraft's existing air/breath concept as the base, tuned so early diving already gives enough time to explore without constant frustration.
Dedicated diving gear/upgrades may improve:
- breath duration.
- swimming/diving control.
- underwater movement speed.
- visibility/presentation where suitable.

Do not make basic underwater collecting require a long RPG equipment grind.

Use external swimming/diving presentation assets where possible.

## 24. Fossils

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

## 25. Museum

The museum is a major shared village facility and is **already a large, impressive building from the beginning**.
Do not make the core museum fantasy depend on repeatedly replacing a tiny museum with larger exterior tiers.

Museum progression comes primarily from watching initially empty/quiet exhibition spaces physically fill over time:
- empty tanks gain donated fish and sea life.
- insect spaces gain specimens.
- fossil halls assemble skeletons piece by piece.
- nature/art areas gain their actual exhibits.

The pleasure is seeing a substantial building gradually become complete, not merely unlocking a bigger shell.

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
For the first accepted donation of a specific museum entry, record the donor player identity as historical presentation data where appropriate.
This can appear subtly in exhibit/info text as a village-history detail.

First-donor credit:
- gives no ownership or governance privilege.
- does not block later players from collecting the same entry.
- does not alter the shared museum completion state.
- should survive resident/player absence as part of museum history.

Collection encyclopedia state is personal per player.
Players can still help each other by transferring valid specimens/items, which can register to the recipient's encyclopedia.

### Duplicate donations
Once the museum has accepted the required specimen/part for an exhibit entry, it does not need infinite duplicate donations.

Duplicates remain useful for:
- selling.
- gifting.
- home display.
- contests.
- photography.
- trading with other players.

Fossils are accepted per required species/part until that physical skeleton/exhibit is complete.
Do not turn duplicate donation into a hidden grind sink.

## 26. Personal displays

Players may display collected life in their homes:
- aquariums.
- terrariums.
- small sea-life tanks.
- flowers/mushroom specimens.

Use external display models and furniture.

## 27. Rare species and variants

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

Physical specimens retain these traits.
Two otherwise identical specimens may stack only when their stack-affecting specimen data is exactly identical; even a small meaningful size/appearance difference keeps them distinct.

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

## 28. Collection encyclopedia

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

Encyclopedia progress is personal, including in a shared Household.
Players may trade/gift valid specimens to help another player's personal encyclopedia.

Do **not** create a separate top-level encyclopedia page for every size/color/pattern variant.
The main encyclopedia has one species entry; opening that species shows a detailed record with a variant section for:
- discovered colors/patterns/forms.
- notable size records.
- other species-specific special variants.

Rare variants are desirable collection records but should not be required for ordinary 100% completion unless later explicitly decided.

## 29. Art and antiques

Art/antiques are retained as a smaller museum-side collection rather than a primary collection pillar.

A visiting vendor may sell:
- paintings.
- sculptures.
- antiques.
- unusual decor.

Real/fake identification can exist, but must have readable clues such as:
- visible model/art differences.
- descriptive details.
- resident/curator/vendor dialogue.
- provenance/context hints.

Do not force blind guessing.

Validated genuine pieces can be donated to the museum's art/antique area and may also remain desirable home decoration when obtained as duplicates or non-donation copies.

External artwork/models are required; do not create throwaway custom art.

## 30. Pier, routes and boats

Travel to other islands goes through the pier/harbor.

Flow:
select available route
→ pay fare
→ short boarding/departure presentation
→ transition/load
→ destination.

Multiplayer travel is player-controlled rather than server-wide:
- one player's departure never force-teleports every online player.
- players heading to the same destination may optionally form a temporary travel group at the pier and depart together.
- travel groups are independent from Household membership.
- each player/group pays and departs according to the route rules.

Return travel is also voluntary:
- players may return independently even if they originally departed together.
- one player cannot force-return companions from an exploration island.
- only explicitly authored special-instance content such as a boss encounter may impose a shared party-return rule, and that rule must be clear before entry.

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

Purchased boats are registered as persistent player/Household travel assets at the harbor rather than ordinary carry-around inventory items.
This avoids accidental loss/duplication and lets the harbor visibly present owned vessels.

Better boats can provide:
- improved appearance.
- improved travel presentation.
- access to farther routes.

Boat ownership must not give one Household control over another Household's travel.
Actual boat models must come from external assets.

## 31. Exploration and combat

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

### Exploration gear vs clothing
Do not make visible vanilla iron/diamond/netherite armor the normal end-state appearance of island residents/players.

Separate **visual lifestyle outfit** from **functional exploration protection** where practical.
Preferred direction:
- players keep curated clothing/appearance during ordinary village life.
- functional protection can use dedicated exploration slots, hidden-under-outfit logic, cosmetic override slots, or purpose-built visible accessories/gear.
- diving gear, cave lighting gear and dangerous-area protection should look like coherent lifestyle/adventure equipment rather than vanilla progression armor.
- external clothing/armor models and compatible cosmetic-slot systems are preferred over improvised recolored vanilla armor.

Do not create a large RPG defense-tier ladder just because armor exists.
Protection progression remains shallow and tied to access/convenience/safety in dangerous exploration content.

Combat rewards feed back into island life:
- rare furniture.
- artifacts.
- fossils.
- plants.
- collection pieces.
- special materials.

### Authored caves/mining
Important exploration caves/dungeons should not be permanently destroyed by unrestricted strip-mining.

For authored exploration cave areas:
- preserve core terrain, routes, landmarks and encounter spaces.
- use renewable ore/resource nodes or controlled replaceable mining pockets.
- regenerate ordinary mineable resources after suitable in-game intervals.
- do not overwrite player-owned yard/home spaces.
- keep mining useful without letting repeated excavation erase the authored level.

Ordinary unrestricted wilderness mining, if retained outside these authored areas, remains a separate map-design decision.

## 32. Clinic and death

### Clinic
Retain a small village clinic/medical facility as a real but lightweight service building.

Its role is limited to cozy-life support:
- one possible safe return/recovery point after death.
- basic treatment/recovery presentation.
- removal/treatment of selected temporary negative status effects where useful.
- a small amount of exploration-preparation utility if it fits the final external building/interior set.

Do not turn the clinic into an RPG healer/progression tree or mandatory combat hub.
Its exterior/interior should come from the same external-building-first workflow as other civic facilities.

### Death
Avoid vanilla full inventory-drop punishment.

On death:
- return to house/clinic/safe point.
- keep money.
- keep tools/equipment/clothing.
- keep unique progression items.
- keep rare/important collectible specimens and key fossil/quest items.
- only some ordinary temporary exploration loot/materials may be lost.

The exact ordinary-loot loss amount can be balanced later.
Death should create some exploration tension without deleting hours of cozy-life collection progress.

## 33. Natural resource recovery

Multiplayer must not permanently strip the island.

Natural zones can restore:
- trees.
- flowers.
- mushrooms.
- rock/gathering spots.
- beach shells.

Player yards should not be overwritten by automatic natural regeneration.

Outer/exploration islands use a hybrid persistence model:
- terrain, landmarks, authored secrets and important structures persist.
- ordinary natural resources and selected repeatable exploration content regenerate after appropriate in-game intervals.
- regeneration timing follows world/game time rather than real-world offline time where the server is paused.

This preserves a memorable sense of place without allowing a few visits to permanently exhaust shared exploration areas.

## 34. Mail and player gifting

Each player/household has a mailbox.

Mail may contain:
- resident letters.
- resident gifts.
- catalog orders.
- event notices.
- player-to-player letters.
- attached items.

Mailbox/mail history is stored as managed save data rather than represented by unlimited physical entities.
Give it a generous practical capacity and organization/archive behavior.

Rules:
- unread mail with an unclaimed attachment must not be silently auto-deleted.
- catalog deliveries and valuable resident/player gifts must remain recoverable until claimed.
- low-value system/event notices may be archived/condensed after a long in-game period rather than expanding the active inbox forever.
- multiplayer mail delivery must not depend on both players being online simultaneously.

### Offline personal-content handling
The shared village continues to advance while other players are online; an absent player does not freeze the server calendar.

Protect player-specific persistent content:
- personal catalog deliveries.
- important gifts/rewards.
- claimed-but-not-collected personal rewards.
- birthday make-up handling already defined elsewhere.
- personal mail with attachments.
- other explicit one-time personal entitlements.

Do not create universal catch-up copies of every missed shared seasonal event.
Ordinary fish/bugs/weather/events continue to follow the shared village calendar.
This preserves the feeling of one living shared world while preventing important personal items from silently disappearing because the player was offline.

Moved-away residents may occasionally send letters.

Players can also:
- hand items directly.
- leave gifts.
- send furniture.
- send some money.

Do not add a global auction house/economy.

## 35. Gifts

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

Gift display should remain curated:
- resident homes use authored display/furniture slots.
- gifts that strongly fit the resident's style/preferences may replace or rotate through suitable slots.
- residents do not endlessly stack every received object into the room.
- older/non-displayed gifts may remain remembered in data without physically cluttering the interior.
- player gifts should never silently destroy irreplaceable authored furniture or make the home unusable.

## 36. Clothing and appearance

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

Clothing and visible exploration gear should be **external-asset-first**:
- everyday outfits.
- hats/accessories.
- rainwear.
- winter clothing.
- festival outfits.
- café/work outfits.
- diving/exploration gear.
- light protective/adventure gear.

Prefer a system that can separate visible outfit from functional protection so players do not have to choose between stats and the intended life-sim look.
The actual adopted outfit/armor models must fit the final player-animation stack and not break guitar, sitting, fishing or photo poses.

## 37. Café

The café should be functional content rather than scenery.

The café exists from early village life in a modest form rather than being absent until late progression.
Progression can expand:
- menu variety.
- seating.
- resident/visitor activity.
- recipe discovery.
- performance/music space.
- special seasonal offerings.

Whether the exterior itself changes tiers depends on the acquired external prefab set; do not force a building swap if one strong café exterior with an expandable interior is visually better.

Possible interactions:
- coffee.
- tea.
- desserts.
- resident visits.
- visiting NPC encounters.
- conversation.
- recipe discovery.
- music.

Food/drink effects should be light lifestyle benefits rather than survival pressure or major combat buffs.
Possible effects include:
- short fishing/gathering convenience.
- mild movement/travel comfort.
- weather/cold comfort where appropriate.
- small social/relationship interactions when sharing food/drink with residents.
- event-specific presentation/effects.

Use the existing music foundation for café ambience/performance.

### Unlockable recordings and home music
The bundled music catalogue should connect performances to collection/decorating.

A player can unlock a reusable recording/album entry for bundled tracks through suitable music interactions, especially recurring musician performances/song requests.
Do not repeatedly award duplicate progression for hearing the same track.

Unlocked bundled tracks can be played through external-quality home music furniture such as:
- record players.
- radios.
- speakers/stereos.
- other coherent music furniture chosen from the final asset set.

Home playback should use the existing Campfire music/audio foundation rather than maintaining a second unrelated playback stack.
For multiplayer, bundled-track playback should synchronize the selected track/state cleanly for nearby players where practical.

Local Custom Music remains personal/custom content and is not automatically treated as a globally unlocked village recording.

## 38. Photography

Photography is retained as real in-game lifestyle content because current external 26.2-compatible systems can already create persistent in-world photographs.

Required gameplay capability:
camera/viewfinder
→ capture the actual rendered game scene
→ create/share a photograph item or equivalent persistent photo record
→ view it in-game
→ place/display it in frames
→ organize photos in an album/collection.

Prefer a proven external camera/photo mod over writing a custom renderer/storage pipeline.

Current strong candidates:
- henkelmax Camera Mod: Minecraft 26.2, NeoForge, client+server; real captured images, photo items, albums, resizable image frames, copying, photographer/date metadata, multiplayer support, images stored in the world save.
- Camerapture: Minecraft 26.2, NeoForge support; working camera, shareable pictures, wall display and albums.

Final dependency choice requires direct compatibility/playtest with the project and verification of license/packaging constraints.
Do not adopt both if one cleanly covers the feature.

Use cases:
- birthdays.
- festivals.
- rare species.
- multiplayer memories.
- resident photo requests.
- home decoration.
- photo albums.
- selected optional photography challenges.

Photography should remain optional lifestyle content, not a mandatory progression gate.

### Photo posing
Photography should integrate with player/resident animation:
- player pose/emote selection near a camera.
- wave/peace/cheer/sit-type social poses where supported.
- residents may turn toward the camera or use authored photo reactions.
- birthdays/festivals can offer simple group-photo positioning anchors.

Do not over-automate players into rigid cutscenes.
Photo posing should help compose memories while preserving normal multiplayer spontaneity.

## 39. Visitors and beach finds

Baseline visitor cadence is roughly **2–3 visiting-NPC appearances per 7-day season**, with room for special seasonal/event visits.

Visitor selection is semi-random rather than pure RNG:
- visitors absent for a long in-game period can gain selection weight.
- some visitors can be season-specific.
- event-linked visitors can be scheduled.
- specific discoveries may unlock additional visitor types.

This availability smoothing must not be reused as a hidden "play long enough and rare-species hints become easier" pity system.

Visiting NPC types may include:
- art dealer.
- rare plant seller.
- recurring special musician/performer.
- other traveling musician(s) if worthwhile.
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

Ordinary beach finds refresh modestly on an in-game-day cadence.
Do not cover the entire coastline with mandatory daily pickups or make a full beach sweep an optimal chore.
A small changing set of finds is enough to make shoreline walks worthwhile.

## 40. Treasure maps and secrets

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

Secret content mixes permanent discovery with renewable reasons to return:
- discovering a hidden location/landmark can be a permanent one-time record.
- ordinary resources, mushrooms, shells, ore pockets, rare seasonal spawns or selected treasure-like finds inside/near that location may regenerate or vary by season/weather.
- do not make every secret a one-time chest that becomes permanently irrelevant after the first visit.

Do not finalize exact locations until the actual map is inspected.

## 41. Village decoration and public projects

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

### Night lighting
Night is a real playable period, so the authored village should visibly transition rather than only becoming dark.
Where supported by the final external builds/assets:
- street lights turn on.
- house windows/interior lamps reflect occupancy/sleep state.
- shops and civic buildings change lighting around closing time.
- harbor/pier lights activate.
- event/festival lighting overrides or extends the normal night presentation.

Use external lighting/build assets where possible and keep updates event/time-driven rather than running expensive global scans every tick.

### Environmental soundscapes
Weather and location should also change sound presentation:
- rain/storm audio is strong outdoors.
- indoor spaces hear appropriately softened/muffled exterior rain.
- café, museum, homes and civic interiors can have subtle room ambience/reverb-like presentation.
- shoreline/harbor areas emphasize water/wind.
- night can have calmer natural ambience where seasonally appropriate.

Prefer external high-quality ambient sound assets/systems where compatible.
Do not stack so many ambient loops that the audio mix becomes muddy.

Public projects use a shared funding model.

The project pool should mix functional and decorative changes, for example:
- bridges/movement improvements.
- harbor/pier improvements.
- plaza/performance-space improvements.
- public garden.
- lookout/coastal walk.
- campfire/picnic area.
- museum surroundings.
- authored access improvements to selected natural areas.
- civic decoration/facility projects.

Only a limited subset should be mandatory progression.
Most public projects should remain elective village-development goals so the game does not become one long linear infrastructure checklist.

Single-player funds it alone.
Multiplayer players contribute voluntarily.
Do not give governance power based on who paid the most.

## 42. Furniture

Furniture should primarily come from external high-quality packs/mods.

Candidate categories include furniture mods such as Skniro's Furniture / BetterDeco if they remain technically suitable.

### Placement and rotation
Keep Minecraft's understandable grid/block placement as the baseline.
Where the chosen external furniture system already supports stable orientation/rotation or finer placement, reuse/adapt that behavior rather than rebuilding it from scratch.

Prefer:
- ordinary cardinal rotation at minimum.
- additional 8-direction/finer rotation only when the adopted external system handles collision, multiplayer sync, interaction anchors and save state reliably.
- furniture interaction hitboxes and sit/use points must rotate correctly with the model.

Do not add a free-transform editor merely because it looks flexible if it creates unstable placement or mismatched collision.

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

## 43. Resident contests

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

Baseline schedule:
- roughly one main fishing/bug-style contest per 7-day season.
- fishing and bug-focused contests rotate so neither activity monopolizes the calendar.
- announce the contest at least 1–2 in-game days ahead through the calendar/notice-board/resident dialogue.
- occasional special seasonal contests may exist, but ordinary weeks should not be saturated with competitions.

Contest rules can rotate between:
- largest catch.
- target species.
- catch count.
- weighted total score.

Rewards favor collectible items:
- participation keepsakes.
- event furniture.
- trophies.
- clothing.
- decorations.

Top placement should feel worthwhile, but a single missed contest should not permanently lock the player out of core progression.

## 44. Birthdays, schedule board and seasonal events

### Village notice board / schedule
The village has a physical notice board / community-board interaction point near the civic core.

When important unread/new board content exists, the physical board should have a subtle visual cue so players do not need to open it constantly.
Possible cues include a small ribbon, marker, lamp, pinned-note accent or other externally sourced/readable presentation.
Do not copy another game's exact mascot/board indicator asset.
The cue clears/changes after the relevant new notices have been viewed.

The content model should borrow the useful breadth of life-sim town boards rather than acting as an event list only.
Possible board content:
- scheduled events and contests.
- resident/player birthdays.
- weather warnings or unusual seasonal conditions.
- store/facility openings, closures, upgrades and special-sale notices.
- contest results/community achievements.
- resident/community flavor notes.
- player-written multiplayer notes.
- treasure-hunt/community activity notices when such content is active.

Do not plaster every full message onto the physical board surface.
The physical board's at-a-glance presentation shows only concise recent/important notices such as:
- an upcoming event exists.
- event/notice name or category.
- approximate date or urgency.
- a small visual marker/icon where useful.

Interacting with the board opens organized information.
Use lightweight categories when the final UI supports them cleanly, with a baseline such as:
- Schedule.
- Village News.
- Residents.
- Shops & Facilities.
- Player Notes.

If categorization makes the chosen external UI worse or overly menu-heavy, merge categories rather than preserving them dogmatically.

The dedicated **Schedule** view shows upcoming dates for a useful forward window.
Selecting a specific date/event reveals:
- exact start time.
- exact end time.
- location.
- event type.
- participation notes/rules when relevant.
- special closure or service changes, if any.

Player Notes remain separate from official system notices so friend messages cannot obscure important event information.
Because this is private friend multiplayer, do not build heavy public-server moderation tooling around player notes.

Events do not need to last all day.
Their true start/end times must be visible here so a player can plan around the 48-minute in-game day without memorizing hidden schedules.

The board/calendar UI should use the project's external-UI-first rule, and the physical board model/build should also prefer an external asset/reference if a fitting one can be obtained.

An MIT-licensed external bulletin-board implementation may be ported/adapted as a technical base if its actual source is legitimately obtainable and the port remains cleaner than a small native implementation.
A Fabric 1.21.1 implementation cannot simply be dropped into NeoForge 26.2; registry, networking, screen/container and rendering integration must be properly ported.
Preserve all required third-party copyright/license notices for reused code.

Residents have fixed authored birthdays on the 28-day four-season calendar.
Birthday overlap is explicitly allowed; multiple residents may share the same date.

### Player birthdays
A player's birthday is **not** derived from the date they first arrive on the island.
On first profile setup, each player chooses a birthday directly from the game's 28-day annual calendar.
This prevents players who join together from automatically sharing the same birthday.

Rules:
- multiple players may intentionally choose the same birthday.
- the birthday is stored in that player's profile.
- birthday logic follows in-game calendar/world time, not real-world elapsed time.
- if other players advance the world through a player's birthday while that player is offline, that player should not permanently lose the personal birthday content; their once-per-year personal celebration/gifts can be delivered on their next suitable login/active day.
- changing/correcting a birthday must not allow duplicate birthday rewards within the same in-game year.

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

Because the 7-day season and 48-minute day already provide meaningful planning time, do not add blanket catch-up/extension systems merely because an event can be missed.
Players are expected to consult the visible schedule and choose what to attend.
Important recurring seasonal content can return in future in-game years.

Final event list should use external event decoration sets rather than improvised visuals.

## 45. Major multi-day festival

The game has no hard ending, but it can have a major milestone festival.

Trigger rule:
the major festival is a completion-style milestone and requires **all required major progression pillars** to be completed, rather than a points system or "5 of 6" shortcut.

Required pillar categories include:
- the required museum progression.
- the required home expansion milestone.
- the required core-facility progression.
- the required sea-route progression.
- the required resident-relationship/community progression.
- the required public-project progression.

Exact numeric thresholds/content for each pillar can be finalized with the relevant systems, but every required pillar must be satisfied before the festival becomes available.
The festival is optional in the sense that normal island life does not end if the player ignores it.

Its finale may function like an ending-credits milestone for players who have effectively reached the game's major long-term goals, while still allowing continued play afterward.

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

Major festivals add activity on top of normal island life rather than shutting the game down for several in-game days.
Core services such as essential shopping, museum access, housing/resident services and ordinary home use should remain available.
NPCs may move to festival roles during certain time blocks, and shops may offer festival stock/presentation, but the player's ordinary life-sim loop remains usable.

The final day can have a large performance/finale using the Campfire Sessions music foundation.

After the festival, the game continues normally.

## 46. Competition, rarity and collectability philosophy

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

## 47. Multiplayer authority and save ownership

Important gameplay state is server-authoritative:
- money.
- loans.
- household membership.
- personal encyclopedia progress.
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

Logical save ownership is split by responsibility rather than stored as one monolithic blob:

VillageState:
- residents and move state.
- museum donations.
- facility/building progression.
- public projects and other village-wide progress.

HouseholdState:
- shared money.
- house/home identity.
- housing loan.
- shared home storage.

PlayerProfile:
- player UUID-backed identity.
- Campfire display name.
- birthday/profile setup state.
- personal appearance / selected base look.
- personal encyclopedia.
- learned recipe book.
- resident relationships.
- owned/worn clothing and personal appearance state.
- personal records/history that should follow that player.

WorldEcologyState:
- persistent ecological/resource-restoration state that must survive restart.

EventState:
- active/scheduled event and contest state.

Use stable IDs/UUIDs rather than display names for persisted identity.
Persist a schema version and support explicit save migration when the format changes.
Concrete class/file/codec layout remains an implementation detail as long as this ownership boundary is preserved.

Client responsibilities are presentation/input/UI, not final authority.

Do not claim multiplayer correctness until actual multiplayer testing occurs.

## 48. External UI requirement

Core UI must use selected external UI design assets.
Do not improvise the final visual language.

Screens likely include:
- first-run character creator/profile setup.
- calendar/weather.
- shop.
- loans/housing.
- encyclopedia.
- mail.
- resident interactions.
- catalog.
- wardrobe.
- museum information.
- Lost & Found.

Keep one coherent external design language rather than mixing unrelated asset styles.

## 49. Open decisions / unresolved work

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
- exact contest reward tables.
- exact public-project list.
- exact combat enemies/boss count.
- exact camera/player-model/inventory dependencies.
- exact resident name/identity/birthday assignments after the resident asset roster is selected.
- concrete save serialization/class layout.
- exact numeric thresholds/content inside each required festival progression pillar.

Continue planning in batches of roughly eight genuinely new decisions.
Do not recycle already-decided topics merely to fill the count.
