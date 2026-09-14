# Region 01 Extraction Relay Presentation Review

## Scope

This checkpoint improves world-space readability of the already-settled technical extraction relay. It does **not** change expedition authority, salvage requirements, rewards, pressure, save data, networking, or final Riftfrontier art direction.

## Reference basis

The temporary material composition deliberately reuses Minecraft's own visual grammar rather than inventing a new final art language:

- Minecraft official `Building Blocks: Amethyst Geode`: the vanilla geode exposes a smooth-basalt outer layer, calcite transition, then the bright amethyst interior. https://www.minecraft.net/de-de/article/amethyst-geode
- Minecraft official `Building Blocks: Ancient City`: Ancient City / Deep Dark presentation establishes dark deepslate as a high-contrast subterranean structural frame. https://www.minecraft.net/en-us/article/ancient-city

No external redistributable asset is added by this checkpoint. Every visible block is a Minecraft runtime block already provided by the target game.

## Implementation contract

- The relay remains the existing lodestone at the far edge of the technical Region 01 cell.
- Its approach floor is dressed only inside the already-owned technical cell: smooth basalt -> calcite -> deepslate tiles, with chiseled deepslate directly beneath the lodestone.
- Presentation code will replace only the technical floor materials it owns; arbitrary player blocks are not overwritten by the dressing helper.
- Readiness is derived from the current authoritative run's existing recovered `region_01_salvage` count. No readiness flag is persisted or sent by a new protocol.
- Below `3/3`, the two readiness markers are absent.
- At `3/3` or above, two vanilla end rods at the relay flanks become visible immediately after the successful salvage interaction.
- A successful extraction removes those ready markers before returning the player to the hub.
- The lodestone/platform/end rods remain a technical readability affordance, not approved final Region 01 portal/UI/VFX language.

## Human field procedure

1. Use a disposable test world and enter Region 01 through the normal hub lodestone.
2. Right-click the first salvage node. Confirm the far-edge extraction lodestone is materially distinct from the old uniform smooth-stone floor and that its short approach reads as basalt -> calcite -> dark deepslate.
3. At `1/3` and `2/3`, verify the two end-rod readiness markers are absent.
4. Recover the third salvage node. Without running a command or clicking the relay again, verify the two end rods appear at the relay flanks.
5. Kill or ignore remaining patrol threats; readiness markers must depend on salvage completion, not patrol count.
6. Right-click the relay and extract. Confirm normal hub return/reward/pressure behavior and that the ready markers are cleared.
7. Redeploy. The rebuilt technical cell must start clean and must not inherit stale ready markers from the previous run.

## Reject conditions

- extraction becomes possible before the existing authoritative gate permits it;
- decorative blocks are accepted as salvage;
- player-placed unrelated blocks outside the owned technical floor set are overwritten;
- readiness markers appear before `3/3`, fail to appear after the third successful recovery, or remain active after extraction;
- the platform obstructs movement or enemy navigation materially;
- presentation state survives independently of or disagrees with the authoritative active run;
- the temporary vanilla palette is mistaken for final Riftfrontier art approval.

## Verification vocabulary

Automated build/smoke success may establish `TESTED`, `BUILD VERIFIED`, and `JAR PRODUCED`. It cannot establish visual acceptance, `PLAYTESTED`, or `MULTIPLAYER TESTED`. Those remain NO until an actual human session records them.
