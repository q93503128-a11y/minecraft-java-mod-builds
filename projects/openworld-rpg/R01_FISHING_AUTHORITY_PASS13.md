# Open-World RPG — R01 Fishing Authority Pass 13

> Status: **NON-VISUAL FISHING AUTHORITY IMPLEMENTED / 5+1+1 AZARI SPOTS BOUND / LIVE USE AND FISH MATERIALIZATION STILL ASSET+PRODUCTION GATED**
>
> Date: 2026-09-28

## Result

The closed R01 fishing rules are now represented by server-owned deterministic state:

- exactly 5 ordinary, 1 uncommon-pool and 1 rare Azari fishing spots;
- personal depletion and active-world-time respawn;
- deterministic per-player/per-spot/per-cycle catch candidates;
- reconnect/miss protection against rarity/size reroll abuse;
- Hook-time charge consumption;
- exact authored species weights;
- exact size distribution, Trophy threshold and sale-value math;
- exact bite-delay and Hook-window values;
- exact tension movement/progress/failure rules;
- Rank V second bounded size roll;
- successful catch resolution into a reconnect-safe semantic pending reward plan.

Personal cooldowns mean one player's catches do not consume another player's fishing spot.

## Production/asset boundary

All seven real Azari spot coordinates remain `candidate`.

Therefore:

```text
LIVE R01 FISHING SPOT INTERACTION: GATED
FISH PLAYER-FACING NAME/MODEL/ICON: GATED
FINAL ROD PRESENTATION/ANIMATION: GATED
FISH ITEM MATERIALIZATION: GATED
```

The backend intentionally stores the internal four content-slot identities only. It does not invent final species names or substitute vanilla fish/rod visuals.

A successful semantic catch becomes a persistent pending reward transaction. The later accepted fish asset/item binding will consume that plan atomically instead of rerolling the fish.

## Current verification boundary

This pass is testable without launching Minecraft for feel review. Real-client fishing presentation, Hook readability and tension feel remain part of the eventual integrated R01 playtest.

```text
R01 FISHING RULES: IMPLEMENTED
R01 FISHING PERSONAL STATE: IMPLEMENTED
R01 FISHING SPATIAL PRODUCTION GATE: IMPLEMENTED
R01 FISHING RECONNECT/REROLL PROTECTION: IMPLEMENTED

R01 FISH VISUAL BINDING: NO
R01 FISH ITEM DELIVERY: NO
R01 FISHING PLAYTESTED: NO
R01 MULTIPLAYER TESTED: NO
```
