# Open-World RPG — R01 Multiplayer Foundation Pass 11

> Status: **CANONICAL CO-OP COMBAT RULES CENTRALIZED / DIRECT PvP DAMAGE FAIL-CLOSED / REAL MULTI-CLIENT TEST STILL REQUIRED**
>
> Date: 2026-09-28

## Result

The project already had personal R01 progression, personal rewards, reconnect-safe Earthloong participation, personal gathering state and authored Quarry co-op counts.

This pass closes two shared runtime gaps without changing design:

- one canonical boss HP/Poise scaling rule is shared by R01 and later content;
- direct player-origin HP damage is disabled by project authority instead of relying on the host/server PvP option.

Canonical values remain:

```text
formal party: 2–4 players
boss HP 1–4: 1.00 / 1.65 / 2.30 / 2.95
boss Poise 1–4: 1.00 / 1.40 / 1.80 / 2.20
>4 HP: +0.45 each
>4 Poise: +0.30 each
ordinary enemies: no generic co-op HP scaling
boss outgoing damage: no generic multiplayer increase
direct launch PvP damage: disabled
```

R01 Quarry Nature Spirit scaling now consumes the shared rule instead of duplicate local constants.

## Still not claimed

This does not mark multiplayer as tested.

Still requiring real multi-client play later:

- 2/3/4-player party UX;
- one-hit and support-only reward eligibility;
- Downed/revive;
- disconnect/rejoin during a boss;
- personal loot/quest/gathering behavior with two real clients;
- safe boss rescaling when engaged players enter/leave;
- Essential-hosted and ordinary server acceptance.

The final engaged-player detector for Earthloong remains gated on the accepted final arena geometry. A player who merely exists online or belongs to a party must never scale the boss.
