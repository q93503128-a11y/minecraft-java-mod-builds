# MOBA Arena — M0 Donor Feature Checklist

This checklist validates the exact player-facing features that justify using Anime Assembly as the primary donor runtime.

Do not mark an item complete because the CurseForge description claims it exists. M0 requires an actual clean Forge 1.19.2 runtime observation.

## Profile identity

- [ ] Minecraft `1.19.2`
- [ ] Forge `43.3.13`
- [ ] Java `17`
- [ ] only the five pinned M0 donor JARs installed
- [ ] all five JAR SHA-256 values recorded by `tools/m0-preflight.cmd`
- [ ] no crash or missing-dependency error on client boot

## Character and combat donor layer

- [ ] `Ctrl + U` opens the donor character-selection interface
- [ ] forward/back character cycling works
- [ ] at least two distinct characters can be selected successfully
- [ ] selected character presentation/model/skin is visible correctly
- [ ] all four character ability inputs execute for a sampled character
- [ ] additional-skill selection/execution works
- [ ] ability cancel interaction works where applicable
- [ ] basic attacks and skill casts do not leave the player in a permanently broken animation/state
- [ ] death and return/recovery can be observed without corrupting character state

## Health-bar and presentation layer

- [ ] `Y` toggles entity health bars
- [ ] `Ctrl + Y` player/NPC-only mode behaves as described
- [ ] `Shift + Y` health-bar rendering toggle behaves as described
- [ ] health bars update when damage is dealt
- [ ] no persistent rendering corruption after switching characters

## Team layer

- [ ] Blue Wand assigns an entity/player to the blue team
- [ ] Red Wand assigns an entity/player to the red team
- [ ] team assignment is visible through stable Minecraft/Forge-observable state or another inspectable donor mechanism
- [ ] same-team/opposing-team combat behavior is recorded
- [ ] asymmetric human roster is not rejected merely because team sizes differ

The last item is a project requirement. If Anime Assembly itself hard-requires equal team sizes, record that as an adapter requirement rather than modifying the map or inventing player bots.

## Ready / match-start layer

- [ ] Info item behavior observed
- [ ] Start item behavior observed
- [ ] a player can mark themselves ready/start as described
- [ ] the donor waits for every non-spectator player before beginning
- [ ] start state can be detected through a stable event/state/signal suitable for `AnimeAssemblyBridge`, or the exact limitation is documented

## Shop / economy layer

- [ ] `M` opens the donor equipment shop
- [ ] item/equipment options render correctly
- [ ] displayed purchase cost is observed
- [ ] at least one purchase succeeds when requirements are met
- [ ] the actual currency/state changed by a purchase is identified
- [ ] that currency/state is server-visible or otherwise bridgeable
- [ ] closing/reopening the shop preserves authoritative purchase state

Do not implement LoM fallback shop code unless the last two bridgeability checks fail for a documented technical reason.

## MOBA minimap

- [ ] donor MOBA minimap is visible in the intended mode/map
- [ ] player position/team presentation updates
- [ ] minimap remains usable after death/respawn or character switching
- [ ] map-specific assumptions are recorded

## Primary local map

- [ ] Anime Assembly modified MOBA map obtained from the author's linked source
- [ ] archive/world SHA-256 recorded locally
- [ ] actual world folder name recorded
- [ ] map loads in the clean donor profile
- [ ] both team bases/spawns identified
- [ ] top/mid/bottom lane geometry identified without editing terrain
- [ ] existing shop/base/structure locations inspected
- [ ] map remains unmodified regardless of planned 1v1–5v5 roster size

## Multiplayer/dedicated-server viability

These are separate validation labels. Do not infer them from singleplayer/LAN success.

- [ ] second real client can join a Forge server/profile with the donor stack
- [ ] team assignment synchronizes between clients
- [ ] character selection/abilities are visible and authoritative across clients
- [ ] Ready/Start works with more than one human client
- [ ] shop transaction state synchronizes
- [ ] health bars/minimap do not expose obvious stale client state
- [ ] dedicated server can start with the donor dependencies, or a specific client-only blocker is documented

If only one local player is available during M0, leave these items unmarked and keep `MULTIPLAYER TESTED: NO`.

## M0 pass rule

M0 is accepted only when:

1. all required JARs are fingerprinted;
2. clean client boot succeeds;
3. character select, combat abilities, health bars, team assignment, Ready/Start, shop and minimap have all been demonstrated;
4. the primary local map loads;
5. Anime Assembly integration surfaces are inventoried from the actual JAR/runtime;
6. any remaining multiplayer-only checks are explicitly separated rather than guessed.

A failed checklist item does not authorize a custom replacement. First inspect the failure, dependency version, donor state, and available external alternatives.