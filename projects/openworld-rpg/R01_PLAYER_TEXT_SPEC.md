
# Open-World RPG — R01 Player-Facing Text / Objective Canon

> Status: **DESIGN CANON — exact R01 board cards, HUD/journal objectives, named-NPC state dialogue, discovery text, ambient barks and post-clear text locked before implementation**
> Master gameplay canon: GAME_DESIGN.md
> Project contract: PROJECT.md
> Opening: R01_VERTICAL_SLICE.md
> Full-region content: R01_CONTENT_BIBLE.md
> UI flow: R01_UI_PRODUCTION_SPEC.md
> Quest/state: QUEST_WORLD_STATE.md
> Story: WORLD_STORY_CANON.md, MAIN_QUEST_SCENE_PACKAGE.md
> Rule: this file owns exact R01 player-facing wording where it explicitly defines it. It does not change quest conditions/rewards/state ownership. If a gameplay state in this file conflicts with a gameplay document, gameplay canon wins and this text file must be updated before implementation.

This file exists because player-facing wording is part of game quality. A source implementer must not invent objective wording, NPC fallback lines, completion text, map/discovery labels or service-state chatter while writing code.

Localization may translate these lines faithfully. Localization may not expose internal IDs, developer terminology or different gameplay instructions.

---

# 0. Text presentation rules

## 0.1 Tone

R01 language is:

- practical;
- grounded;
- concise;
- lightly warm;
- curious rather than prophecy-heavy;
- not sarcastic UI copy;
- not faux-medieval purple prose;
- not tutorial voice from a developer.

No one calls the player chosen, destined, reincarnated or uniquely important.

## 0.2 Objective grammar

HUD objectives:

- start with a direct verb;
- normally fit one line at common 1080p UI scale;
- never mention internal counters unless the counter represents a real player action;
- do not say “Quest Updated” as the objective itself.

Journal can use a short explanatory sentence beneath the HUD objective.

## 0.3 Counter grammar

Use:
- Herbs gathered: 2 / 3
- Signs inspected: 1 / 2
- Road evidence: 2 / 3

Do not use:
- Objective 2/3
- Stage 4
- r01_main_03

## 0.4 Suggested level

Use exactly:

> Suggested Lv 8

It is guidance, not a lock warning.

---

# 1. Alderford arrival

## 1.1 Location discovery

Title:

> Alderford

Subtitle:

> A road town at the Greenwater crossing.

One-time discovery toast only. No XP reward is attached merely because the opening path necessarily enters the hub.

## 1.2 Mara gate line

Use existing canonical line:

> “You picked a lively road to arrive on. If you're looking for work, the Wayfarers' Hall is ahead. Quarry carts have stopped coming back on time.”

After this line has played once, ordinary repeat interaction before first class selection:

> “Elian's inside the Hall. Pick a discipline when you're ready; the road can wait a minute.”

After first class selection but before Dust completion:

> “Take the quarry road at your own pace. I need a useful picture, not a heroic sprint.”

After Dust completion but before Earthloong:

> “The roots are the part I don't like. Find the lower workings and don't mistake old stone for safe stone.”

After Earthloong but before briefing:

> “Bring what you found inside. Ilyan and Daren both need to see it.”

After briefing:

> “West or uphill. Either trail teaches us something; neither needs your oath.”

---

# 2. First class selection — Elian Rook

## 2.1 First interaction

Existing line remains:

> “Choose the discipline you want to begin with. You can learn another path later; this is where you start.”

## 2.2 Closing without choosing

Repeat line next interaction:

> “No rush. Look through the disciplines again when you're ready.”

## 2.3 After first class commit

First repeat interaction:

> “Good. Learn what that path does under pressure before you worry about a second one.”

Later pre-quarry repeat:

> “Your first discipline isn't a prison. Build some experience before you start comparing every road at once.”

Post-Earthloong repeat:

> “You've had a real fight with that discipline now. If another path interests you, I can keep its progress separate.”

No line claims the player mastered a class after one dungeon.

---

# 3. Guild board — exact first-arrival cards

## 3.1 Dust on the Quarry Road

Category:

> Main

Card premise:

> Quarry carts are late, wildlife is spilling onto the road, and no one has one clean explanation. Find three useful pieces of the problem.

Before first class:

> Choose your first class with Elian Rook to begin.

Current objective after first class:

> Investigate the old quarry road.

Counter:

> Road evidence: 0 / 3

Reward preview:

> EXP • 90 Gold • Class XP

No exact percentage math is shown on the compact board card.

## 3.2 Riverbank Remedies

Category:

> Contract

Card premise:

> Greenwater Remedies needs fresh Healing Herbs from the river edge.

Objective preview:

> Gather 3 Healing Herbs and return them to Lysa Fen.

Reward preview:

> EXP • 60 Gold • Class XP • Healing Potion

## 3.3 Signs in the Meadow

Category:

> Contract

Card premise:

> Sera Wren wants the large tracks near Alder Meadow identified before someone turns them into a bad hunting story.

Objective preview:

> Inspect 2 signs of large-creature activity.

Reward preview:

> EXP • 70 Gold • Class XP

---

# 4. Dust on the Quarry Road — exact objective states

Quest title:

> Dust on the Quarry Road

## 4.1 Active

HUD:

> Investigate the old quarry road.

Counter:

> Road evidence: N / 3

Journal explanation:

> Cargo, wildlife, road damage, nearby resources and people caught on the route can all tell part of the story. Find any three distinct signs of what is happening.

## 4.2 Category credit toasts

Lost cargo:

> Lost cargo recovered.

Meadow Viper threat:

> Road threat cleared.

Broken road marker:

> Quarry-road marker inspected.

Field resource:

> Local resource gathered.

Roadside Trouble completion:

> Travelers helped on the quarry road.

Each toast appears only the first time that category credits this quest.

## 4.3 Completion

When 3/3 is committed, HUD changes to:

> Return to Mara Venn.

Journal explanation:

> You have enough evidence to show that the road problem is more than one isolated accident.

Mara completion line remains:

> “That's more than bad luck. Traffic and wildlife are both being pushed off their usual lines. The quarry crew marked roots in the lower workings before they pulled out.”

Completion toast:

> Main objective complete — Dust on the Quarry Road

Next-main toast:

> New main objective — Roots Below Stone

---

# 5. Riverbank Remedies — exact objective states

Quest title:

> Riverbank Remedies

Lysa accept line remains:

> “Three fresh river herbs will do. Don't strip a whole patch; take what you need and leave the bank alive.”

## 5.1 Gather

HUD:

> Gather fresh Healing Herbs.

Counter:

> Herbs gathered: N / 3

Journal explanation:

> Gather the herbs yourself from valid Greenwater-area plants so Lysa can judge what the riverbank is producing.

## 5.2 Return

After 3/3:

> Bring the herbs to Lysa Fen.

Journal:

> You have enough fresh Healing Herbs for Greenwater Remedies.

## 5.3 Completion

Lysa completion line remains:

> “Good. That's enough for the road and enough left for the next traveler. Keep a dose ready before you go underground.”

Completion toast:

> Contract complete — Riverbank Remedies

---

# 6. Signs in the Meadow — exact objective states

Quest title:

> Signs in the Meadow

Sera accept line remains:

> “Big tracks don't mean ‘go kill the biggest thing nearby.’ Learn what made them first. Check the damaged cart and the meadow edge.”

## 6.1 Active

HUD:

> Inspect signs in Alder Meadow.

Counter:

> Signs inspected: N / 2

Journal explanation:

> Check any two distinct signs of large-creature activity. You are identifying the territory, not hunting its owner.

Individual site text:

Damaged cart:

> The impact is broad and low. Something heavy hit this while moving fast.

Churned meadow:

> Deep hoof marks cut across older grazing tracks.

Broken fence/tree scoring:

> The scoring sits far higher than the boar marks near the road.

## 6.2 Completion

After 2/2:

> Return to Sera Wren.

Sera completion remains:

> “Steelboar made part of it. The deeper scoring didn't. If you follow the larger trail, do it because you chose to—not because a board told you to.”

Completion toast:

> Contract complete — Signs in the Meadow

---

# 7. A Stag at the Ford — exact objective states

Quest title:

> A Stag at the Ford

Toma hint line remains:

> “One of my stags tore loose by the ford. If you find it, don't chase it. Clear the danger, then let it come to you.”

## 7.1 Before discovery after explicit hint

HUD if pinned:

> Search Greenwater Ford for the loose stag.

Journal:

> Toma's stag broke loose near the ford. He warned against chasing it.

## 7.2 Ford discovered

HUD:

> Clear the danger around the frightened stag.

Journal:

> The stag is still here, but it will not calm while nearby threats are active.

## 7.3 Threat cleared

HUD:

> Approach the stag calmly.

Context interaction:

> Calm Trail Stag

## 7.4 Stag calmed

HUD:

> Mount the Trail Stag.

Context interaction:

> Mount

## 7.5 Mounted

HUD:

> Ride the Trail Stag back to Fordside Stables.

Journal:

> The stag is willing to follow your lead. Take the ford road back to Toma.

## 7.6 Registration

On entering the valid stable registration volume:

> Register Trail Stag

After server commit, Toma completion line remains:

> “There. It knows your hands now. Call it when the road is open enough to ride; don't ask it to fight your battles.”

Completion toast:

> Mount unlocked — Trail Stag

No objective ever says Escort the Stag.

---

# 8. Roots Below Stone — exact pre-dungeon objective states

Quest title:

> Roots Below Stone

Mara start line remains:

> “The quarry crew marked roots where there shouldn't be roots. Get eyes on the lower workings. If the entrance is open, don't assume miners opened it.”

## 8.1 Overlook

HUD:

> Reach the old quarry overlook.

Journal:

> The quarry crew reported impossible root growth in the lower workings.

## 8.2 Overlook discovered

HUD:

> Find a way down to the lower workings.

Journal:

> The lower entrance is visible from the quarry works. Reach it by any safe route.

## 8.3 Lower approach reached

HUD:

> Inspect the root-split lower entrance.

Context interaction:

> Inspect masonry

## 8.4 Lower entrance inspected

HUD:

> Enter the Old Alderford Quarry.

Secondary metadata:

> Suggested Lv 8

Journal:

> Roots have split through masonry older than the working quarry. The lower entrance is open.

This step commits the lower-entrance milestone reward and reveals the dungeon marker.

## 8.5 Dungeon entered

HUD:

> Reach the root-breached workings.

Journal:

> Follow the lower quarry route past the abandoned galleries.

---

# 9. Quarry dungeon — exact main-objective states

The dungeon does not add fake subquests. It advances the current Main objective.

## 9.1 Upper Mining Gallery

HUD:

> Push through the upper gallery.

After the room controller clears:

> Find a route through the collapsed hoist.

## 9.2 Collapsed Hoist Chamber

Before shortcut activation:

> Reach the hoist control.

Context:

> Operate lift mechanism

After shortcut activation:

> Continue into the root-breached workings.

Shortcut toast:

> Quarry lift opened.

## 9.3 Root-Breached Workings

On Nature Spirit engagement:

> Break through the root-breached workings.

After elite defeat:

> Follow the older stonework deeper.

## 9.4 Relay Gallery

HUD:

> Examine the damaged route plate.

Context:

> Examine route plate

Interaction text remains:

> “The stonework predates the quarry. Repeating route lines continue beyond Alderford—west through the forest and upward toward the mountains.”

After commit:

> Reach the chamber beyond the relay.

Evidence toast:

> Discovery recorded — Quarry Relay Evidence

The toast does not put an evidence item in the backpack.

## 9.5 Earthloong chamber

On arena threshold:

> Defeat Earthloong.

After defeat:

> Examine what the root breach exposed.

After evidence/fallback interaction:

> Return to Alderford.

Journal title after boss:

> Lines Beneath the Land

Sub-objective:

> Return to the Wayfarers' Hall.

---

# 10. Steel in the Grass — exact objective states

Quest title:

> Steel in the Grass

On discovery before acceptance:

> A plated boar is ranging closer to the road.

Sera offer line remains:

> “That plated boar has started pushing closer to the road. If you choose to hunt it, keep clear of the first charge. The armor matters less once it commits.”

Accepted HUD:

> Hunt the Steelboar near the meadow edge.

Journal:

> A plated Steelboar has pushed into the route between Alder Meadow and the deeper grove. It is optional, but leaving it alone will not block the quarry.

On qualifying defeat:

> Return to Sera Wren.

Sera completion line:

> “That's the one. The road will breathe easier for a while. Don't confuse that with making the meadow tame.”

Completion toast:

> Contract complete — Steel in the Grass

---

# 11. The Crowned Trail — exact clue text and hunt states

Journal title:

> The Crowned Trail

## 11.1 Individual clue interactions

Antler-height scoring:

> Deep scoring cuts the wood above Steelboar height.

Hoof furrows:

> Heavy hoof furrows cross the meadow without joining the bison trail.

Broken marker:

> The marker was split by a branching strike, not a cart wheel.

Each first clue toast:

> Crowned trail clue found.

No 1/3 journal entry is created after only one clue; the clue remains a Discovery note.

## 11.2 Two clues found

Journal entry is created.

HUD if pinned:

> Search the marked meadow-grove territory.

Journal:

> Two separate signs point to a huge antlered animal moving between the meadow and Rootshade Grove.

Map action:

> Show search area

No exact boss marker.

## 11.3 Regalhart found before clues

Discovery toast:

> Major threat discovered — Regalhart

Journal:

> You found the crowned hart without needing the trail signs. The remaining marks can still tell you where it has been ranging.

No clue requirement appears.

## 11.4 Regalhart encounter

Boss title:

> Regalhart

No subtitle calls it a quest boss.

## 11.5 First defeat

Toast sequence:

> Major threat defeated — Regalhart

Then normal reward toasts.

The Crowned Trail journal becomes:

> The crowned hart has been brought down. Signs of its territory remain across the Heartland.

It is marked complete for first-defeat discovery purposes but its physical clue interactions remain readable.

---

# 12. Roadside Trouble — exact event text

Event title:

> Roadside Trouble

Driver start bark remains:

> “Wheel's gone, and the grass started moving. Clear them out and give me a hand.”

Tracker:

> Clear the road threat  
> Brace the wheel  
> Return the cargo

Wheel interaction:

> Brace wheel

Cargo interaction:

> Return cargo

After all three:

> Roadside Trouble complete

Driver completion bark remains:

> “That's enough. We'll get moving before the road finds another problem for us.”

No failure text appears when the event simply resets after abandonment.

---

# 13. R01 substantial POI discovery text

## Greenwater Ford

Title:

> Greenwater Ford

Subtitle:

> A shallow crossing where Alderford's river road meets the meadow.

## Mosswheel Mill

Title:

> Mosswheel Mill

Subtitle:

> An abandoned local mill, long silent but still standing above the branch.

Loft cache interaction:

> Search mill cache

## Rootshade Grove

Title:

> Rootshade Grove

Subtitle:

> The forest edge grows denser here, and the tracks grow heavier.

## Quarry Surface Works

Title:

> Old Alderford Quarry

Subtitle:

> Surface works above abandoned lower galleries.

---

# 14. Minor landmark discovery text

## Bent Roadwatch

Subtitle:

> A weathered shelter from the old quarry-road watch.

## Shepherd's Overlook

Subtitle:

> A meadow rise with long sightlines over the Heartland roads.

## Twin-Willow Bend

Subtitle:

> A slow Greenwater bend framed by two old willows.

## Drover's Rest

Subtitle:

> A roadside shelter once used by traders and livestock hands.

## Quarrymen's Memorial

Subtitle:

> A small memorial to the people who worked the quarry before it closed.

Memorial inscription:

> “For those who cut a road through stone and came home by it.”

## Root-Split Cairn

Subtitle:

> An old local waystone now caught beneath newer roots.

No minor landmark text mentions Anchors.

---

# 15. Quarry Waystone text

Discovery title:

> Quarry Waystone

Before activation:

> Activate Waystone

After activation toast:

> Fast travel unlocked — Quarry Waystone

Repeat interaction:
- Travel
- Rest

No UI says Checkpoint Activated as a developer-style generic message when the place name fits.

---

# 16. Service NPC state dialogue

These are fallback/repeat lines, not mandatory conversations. Service screens open immediately after the line when the interaction context calls for the service.

## 16.1 Nessa Bell

First use remains:

> “Road's been odd, but the shelves aren't empty. If you need a replacement or want to sell what you won't use, start here.”

Pre-Dust repeat:

> “Early gear doesn't need to be precious. Keep what helps; sell what doesn't.”

After Dust:

> “Quarry-road trade is thin, so don't wait for one perfect shelf. Use what the road actually gives you.”

After Earthloong briefing:

> “Carts are moving again. Not normally, exactly—but moving is a start.”

When player owns a residence first time opening Household:

> “Got a house now? Then the useful things are under Household. Buy chairs because you want chairs, not because some ledger says you need six.”

## 16.2 Oren Quill

First use remains:

> “Materials stay under your name. Put them away here and the town services can still see what belongs to you.”

Repeat:

> “If your pack is full of ore and herbs, that's a storage problem, not an adventure.”

After Earthloong:

> “I've marked the quarry pieces under your account. No need to carry proof in your pockets.”

## 16.3 Brin Hale

First use remains:

> “Hot food is for the road, not another chore. Bring ingredients or buy a plate and get moving while it's still warm.”

Repeat pre-quarry:

> “Eat before the tunnel, not after you discover you should have.”

Post-Earthloong:

> “The quarry crew's talking again. That's usually how a town knows the worst part is over.”

If active Nourishment exists:

> “You've already got a meal working for you. A new one will replace it when you eat.”

## 16.4 Daren Holt

First use remains:

> “If it's in your Material Pouch, I can work from it. Bring better material and you'll see better options.”

Verdant Crystal first-known state:

> “That green crystal's worth using where the base is already sound. I can open the better R01 recipes from here.”

Player-facing localized version must not literally say R01. Final line is:

> “That green crystal's worth using where the base is already sound. I can make better work from it.”

Post-Earthloong before enough signature material:

> “Earthloong scale holds together better than quarry stone has any right to. Bring four and I can make something worth the trouble.”

Signature craft available:

> “Four scales. That's enough. Pick the piece you actually want to build around.”

After briefing:

> “Old machinery under a dead quarry is bad enough. Machinery tied to somewhere else is worse. Still—metal is metal. I can work with the part we understand.”

## 16.5 Lysa Fen

First use remains:

> “I can sell the basics. If you bring the plants and glow-organs yourself, I can make the same doses for less.”

Riverbank active before 3 herbs:

> “Fresh riverbank herbs, three. Don't bring me somebody else's bundle and call it fieldwork.”

After contract:

> “Now you know where the medicine starts. The bottle is the easy part.”

Post-Earthloong:

> “Lightning burns and poison bites need different answers. Keep the Belt loaded before you go chasing the next road.”

## 16.6 Toma Reed

Before Stag unlock remains:

> “The paddock's for trained stock. If you want one to answer your call, earn its trust first.”

After Dust, hint available:

> “One of my stags tore loose by the ford. If you find it, don't chase it. Clear the danger, then let it come to you.”

After Stag unlock:

> “It'll answer your call now. Give it room on tight roads and don't treat a living mount like a door key.”

Post-Earthloong:

> “Longer roads next, by the sound of it. Good time to have something with four steady legs.”

## 16.7 Elian Rook

Use §2 lines.

## 16.8 Sera Wren

Before Signs accepted:

> “Tracks are a map if you stop trying to turn every one into a hunt.”

Signs active:

> “Two good signs are enough. I want a pattern, not every broken twig in the meadow.”

After Signs:

> “You know the difference now: boar marks low, crowned-hart marks high.”

Steel in the Grass active:

> “Let the boar commit to the charge. Fighting its steering is easier than fighting its armor.”

After Steelboar:

> “The meadow won't stay quiet forever. It doesn't need to.”

After Earthloong briefing:

> “The western route disappears into forest cover. The high road advertises every bad decision from half a valley away. Pick the kind of problem you want first.”

## 16.9 Ilyan Voss

Pre-clear remains:

> “I'm here for old road records. Alderford keeps better ledgers than most places twice its size.”

If asked again pre-clear:

> “The interesting part of a road ledger is usually where the road stops making sense.”

After Earthloong before briefing:

> “That plate is worth seeing beside the older route books. Bring it to the Hall.”

After briefing:

> “One mark pointing west and one uphill. Two surviving comparisons are better than one confident guess.”

No line says that an ending choice is already correct.

## 16.10 Mara Venn

Use §1.2 lines plus existing quest dialogue.

## 16.11 Kest Arden

First-search line remains:

> “If you're following the crown marks, you're late by about an hour. Good news: it didn't stay where I found them.”

If Regalhart already defeated:

> “So you're the one who brought down the crowned hart. Saves me a long walk.”

If spoken to again in R01 after either first scene:

> “I'll take another road. If we meet twice, at least one of us is wandering properly.”

Kest offers no R01 service or mandatory quest.

---

# 17. Post-Earthloong named-NPC aftermath lines

These lines become the primary one-time post-briefing world-state comments. After playing once, NPCs use their normal service/repeat pool.

Mara:

> “West or uphill. Either trail teaches us something; neither needs your oath.”

Daren:

> “Whatever that network was built to do, I care what it does when one piece wakes another.”

Sera:

> “Forest route or high road. Both are readable if you look up from the marker.”

Ilyan:

> “Two surviving route lines are not a theory. They're enough reason to investigate.”

Elian:

> “New regions won't ask you to forget the class you started with. Bring the experience with you.”

Lysa:

> “Longer travel means you stop treating recovery like an emergency purchase.”

Toma:

> “The Stag will make the road shorter. It won't make the road safe.”

Brin:

> “Come back hungry instead of dead. I can help with one of those.”

Nessa:

> “Bring back something worth selling. Preferably not something still biting.”

Oren:

> “Whatever you find, keep the rare pieces named and the common pieces organized.”

No one tells the player to complete both R02 and R03.

---

# 18. Unnamed Alderford guard barks

Guards are not quest dispensers.

Use at most one bark per guard per 90 real seconds around the same player.

Pre-Dust pool:

> “Quarry road's open. That doesn't mean it's quiet.”

> “If a beast backs off, let it. Not every horn needs a hero.”

Dust/Roots active pool:

> “Watch the road bends. Vipers like the warm stone.”

> “Lower quarry hasn't had a proper shift in a while.”

Post-Earthloong pool:

> “Carts are moving again. We're still keeping the gate watch doubled.”

> “West road and high road both have traffic now. Different trouble, same boots.”

No guard gives new gameplay information that the journal does not contain.

---

# 19. Ambient Alderford townsfolk barks

Ambient townsfolk are optional atmosphere. They do not open a dialogue tree.

Maximum one bark from the ambient pool per 120 real seconds around the same player.

Pre-quarry:

> “Brin says the road dust gets into everything. Brin says that about rain too.”

> “The quarry used to wake us before the birds did.”

> “Greenwater's low enough to cross on foot this week.”

> “If Nessa says it's a bargain, ask who got bored of carrying it.”

After Earthloong:

> “Heard the quarry made thunder underground. I'd prefer ordinary mining.”

> “First cart came through this morning. Driver looked happier than the wheel.”

> “Forest folk came in asking about the western road again.”

> “If the Hall sends you uphill, pack something warm before you blame the mountain.”

Ambient barks never mention internal mechanics, classes by tier or the player's private quest state.

---

# 20. Housing/player-property text

Vacant property interaction:

> Inspect residence

Property unavailable because personally owned elsewhere and viewing for trade:

> View trade-up

Owned property sign:

> <Property Name> — Your Residence

Other player's personally unavailable shell in contexts where the game exposes personal occupancy state:

> Residence unavailable

Do not expose another player's account/name unless multiplayer property rules deliberately permit it.

Starter furnishing option:

> Add Starter Furnishing Package — 750 Gold

Package description:

> A bed, storage access cabinet, table, two chairs, lanterns, shelf and trophy stand. Furnishings are delivered to Home Storage for you to place.

---

# 21. Trail Stag summon / dismount feedback

Summon failure — insufficient legal space:

> Need more open ground.

Dismount failure:

> No room to dismount.

Combat-lock summon:

> You can't call your mount while under attack.

No-mount authored volume:

> Your mount can't be called here.

These are short feedback lines, not modal warnings.

---

# 22. Field Camp text

Recipe discovery toast:

> New utility recipe — Field Camp Kit

Crafted/unlocked toast:

> Utility unlocked — Field Camp

First deployment explanation remains:

> Field Camp  
> Rest • Reload Recovery Belt • Cook • Manage gear  
> Redeploying moves your camp.

Redeploy confirmation:

> Move your Field Camp here?

No resource cost is shown because redeployment is reusable.

---

# 23. Fishing R01 generic text before final species binding

Final fish species names remain ASSET_BINDING. Until those names are selected, internal slot IDs are never rendered.

Before exact species binding is closed, gameplay source for those catch result strings must not ship.

Allowed generic interaction text:

> Cast

> Hook

> Line tension

> Personal best

> Trophy

> New Fish Codex entry

Not allowed in player-facing builds:

- r01_river_common_a
- Fish A
- Placeholder Fish
- Common River Fish as a fake final species name

---

# 24. Main-story handoff text

Post-Earthloong return scene remains exactly the four canonical lines already in R01_VERTICAL_SLICE.md.

Final main entry:

> Lines Beneath the Land

Objective:

> Follow one of the old route lines beyond Alderford.

Journal summary:

> The quarry route plate points west toward an old forest relay and uphill toward Whitecrest station. Either investigation can show what the buried structure was connected to.

Known leads:

> Western Relay

Description:

> Follow the western road into the forest and river basin. Old route records suggest a relay survived there.

> Whitecrest Station

Description:

> Take the high road toward Whitecrest. The route plate points to an old mountain station linked to the quarry.

Neither lead is labeled primary, safer, correct or recommended.

---

# 25. System/result text used in R01

## Quest acceptance

> Contract accepted — <Title>

Main objectives that begin automatically use:

> New main objective — <Title>

## Quest completion

> Contract complete — <Title>

or

> Main objective complete — <Title>

according to category.

## Discovery

> Location discovered — <Name>

Major threat:

> Major threat discovered — <Name>

## Insufficient Gold

> Not enough Gold.

## Backpack / reward destination full

When an R01 important reward is automatically routed to Alderford Vault because the backpack lacks space:

> Backpack full. Reward sent to Alderford Vault.

When both backpack and the 36-slot Alderford Personal Storage lack legal room:

> Backpack and Vault full. Reward waiting to be claimed.

Pending-reward action:

> Claim reward

No-space pending state:

> Make room in your backpack or Alderford Vault.

For an ordinary world-ground item that is not auto-routed:

> Backpack full.

No player-facing string uses generic wording such as `pending pickup/storage-safe result`.

## Contract abandonment

Riverbank Remedies:

> Abandon Riverbank Remedies? Gather progress will reset.

Signs in the Meadow:

> Abandon Signs in the Meadow? Contract inspection progress will reset; discovered sites remain known.

Steel in the Grass:

> Abandon Steel in the Grass? A future reacceptance will require a new qualifying Steelboar defeat.

## Interaction in combat

> Not while in combat.

## Service unavailable due to world actor invalidation

> That service is unavailable right now.

Do not show networking/internal error text.

---

# 26. Text state ownership

At minimum, R01 text selection reads authoritative state for:

- first class selected;
- Dust stage/categories;
- Riverbank Remedies accepted/gathered/completed;
- Signs accepted/sites/completed;
- Stag discovery/calm/registration;
- Roots quarry stage;
- Steelboar discovered/accepted/completed;
- Crowned Trail clue/discovery/first defeat;
- Earthloong first clear;
- post-quarry briefing;
- R02/R03 lead availability;
- residence ownership;
- Camp unlock;
- service first-use flags;
- one-time NPC aftermath-comment flags.

Client presentation can choose timing only within the owning UI rules. It cannot choose a dialogue state inconsistent with server progress.

---

# 27. R01 player-text acceptance

R01 player-facing text is not implementation-ready unless:

1. every Main/Regional/Contract state has exact HUD wording;
2. every quest counter names the real action;
3. board cards have exact premise/objective/reward summaries;
4. first-use service lines have exact fallback states;
5. all eleven named R01 characters have at least one valid repeat state where they can remain interactable;
6. post-Earthloong Alderford has explicit human reaction text;
7. substantial/minor discoveries have player-facing names/descriptions where needed;
8. no optional content implies mandatory completion;
9. no dialogue tells the player which R02/R03 route is correct;
10. no player-facing internal IDs/development language can appear;
11. unresolved fish species names remain blocked by ASSET_BINDING instead of receiving fake temporary names;
12. actual Korean localization/layout is later checked in the real client before presentation completion.

Current verification:

~~~text
R01 PLAYER TEXT AUTHORED: YES
R01 ENGLISH CANON REVIEWED: YES
KOREAN LOCALIZATION FINAL: NO
IN-CLIENT TEXT LAYOUT REVIEWED: NO
PLAYTESTED: NO
~~~
