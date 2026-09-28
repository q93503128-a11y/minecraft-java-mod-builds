# Open-World RPG — R01 Gathering Spatial Binding Pass 12

> Status: **47 AUTHORED AZARI GATHERING NODES BOUND TO RESOURCE IDENTITIES / PRODUCTION-ONLY GAMEPLAY GATE ADDED**
>
> Date: 2026-09-28

The extracted Azari R01 slice currently contains 47 authored gathering-node candidates:

```text
Healing Herb: 16
Hardwood: 16
Iron Ore: 11
Verdant Crystal: 4
Total: 47
```

Each node now resolves to its canonical project resource through one spatial registry.

Important boundary:

```text
candidate/client_verified node
→ planning/review only

production node
→ may enter live gathering authority
```

The current 47 nodes are still candidates, so none is exposed as live gameplay yet. This preserves the requirement that real-client placement/readability review happens before production promotion.

Multiplayer behavior is unchanged: gathering cooldown/availability is personal per player, so one player harvesting a production node does not consume that node for another player.

No user playtest is requested yet.
