# Open-World RPG — R01 Nessa Market Pass 18

> Status: **DETERMINISTIC PERSONAL 5-SLOT MARKET ROTATION IMPLEMENTED / EQUIPMENT MATERIALIZATION STILL SHARED-AFFIX-GENERATOR GATED**
>
> Date: 2026-09-28

## Implemented

Nessa Bell's exact R01 personal rotating stock is now server-owned:

- 5 slots per cycle;
- 10 active-world-minute refresh from the player's Alderford shrine epoch;
- world clock/sleep/offline time does not advance or reroll the merchant;
- deterministic seed uses world seed + player UUID + Nessa identity + cycle index;
- Slot 1: one of 7 weapons, equal weight;
- Slot 2: 50% weapon / 50% off-hand; weapon excludes Slot 1's exact base;
- Slots 3/4: two distinct armor equipment slots, all ten unordered pairs equal-weight, each with one of three armor families equal-weight;
- Slot 5: one of four exact R01 accessories, equal-weight;
- grade weights: Standard 50 / Refined 40 / Superior 10;
- at most one Superior in the five-slot cycle;
- later Superior rolls are normalized to Standard:Refined = 5:4;
- fixed Item Lv 2 / 4 / 6;
- exact category/grade prices;
- persistent per-slot affix seed reserved for the shared affix materializer;
- SOLD state persists for the cycle;
- a new cycle clears SOLD state and regenerates deterministically;
- stale old-cycle purchase requests are rejected;
- selling an item back will not restore the merchant slot;
- no paid/manual reroll exists.

The full generated five-item cycle is persisted on first resolution. A reconnect therefore does not regenerate a different cycle even if generation code later changes.

## Purchase boundary

A purchase request produces a stable transaction plan:

```text
player UUID + cycle + slot
→ exact stock item
→ exact price
→ exact affix seed
→ stable transaction id
```

The slot is marked SOLD only after a future shared equipment materializer reports that the exact Gold debit and item delivery have durably completed.

The project does not currently own the full ordinary-equipment affix generator required by the canon. Therefore this pass deliberately does **not** create zero-affix merchant gear or charge Gold for an incomplete item.

## World gate

Nessa's live market service still requires the production Alderford Market interaction binding from Pass 16. The current market structure/socket remain candidate, so player-facing shop access stays closed.

```text
NESSA STOCK GENERATION: IMPLEMENTED
NESSA PERSONAL CYCLE PERSISTENCE: IMPLEMENTED
NESSA SOLD/STALE-CYCLE AUTHORITY: IMPLEMENTED
NESSA PRICE/ITEM-LV RULES: IMPLEMENTED
PURCHASE TRANSACTION PLAN: IMPLEMENTED

SHARED ORDINARY AFFIX MATERIALIZER: NO
LIVE EQUIPMENT DELIVERY: NO
MARKET PRODUCTION SOCKET: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
