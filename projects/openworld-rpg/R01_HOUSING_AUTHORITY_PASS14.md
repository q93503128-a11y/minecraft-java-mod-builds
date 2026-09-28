# Open-World RPG — R01 Housing Authority Pass 14

> Status: **PROPERTY OWNERSHIP / ATOMIC PURCHASE+MOVE BACKEND IMPLEMENTED / PLAYER-FACING PROPERTY SHELLS STILL PRODUCTION GATED**
>
> Date: 2026-09-28

## Implemented

R01 now has server-owned housing transaction state for the exact Alderford roster:

- Gate Cottage — 2,400 Gold / 54 Home Storage slots;
- Paddock Cottage — 2,400 / 54;
- Riverside Cottage — 2,400 / 54;
- Quarry-Road Cottage — 2,400 / 54;
- Market House — 9,000 / 72.

The backend enforces:

- at most one current residence per player;
- one primary owner per physical property;
- shared-world reservation so two players cannot buy one vacancy at the same time;
- exact 80% old-house trade-in credit;
- one atomic Move / Trade Residence transaction instead of sell-first risk;
- idempotent Gold debit/credit receipts so reconnect/retry cannot charge or refund twice;
- durable transaction recovery after interruption;
- old ownership release only as part of the new ownership transfer;
- logical Home Storage capacity tied to the residence tier;
- safe downsizing: storage overflow enters a temporary Moving section instead of being deleted.

The normal Gold save codec remains backward compatible: older saves without the new debit ledger decode with an empty debit history.

## Production boundary

All five Alderford property structures remain candidate / visual-review-gated.

Therefore the live service refuses purchase/move for those properties until the shell composition and spatial binding are accepted as production.

This means the backend is ready without turning placeholder or unreviewed houses into player-facing content.

## Still open

- accepted exact house prefab/composition;
- interior/furnishing volumes and doorway clearances;
- furniture model/interaction binding;
- 750-Gold starter furnishing package delivery;
- furniture placement runtime;
- owner/trusted-decorator/guest interaction permissions;
- real two-client race/reconnect testing.

Current labels:

```text
HOUSING OWNERSHIP BACKEND: IMPLEMENTED
ATOMIC PURCHASE/MOVE BACKEND: IMPLEMENTED
80% TRADE-IN: IMPLEMENTED
HOME STORAGE RESIZE/MOVING OVERFLOW: IMPLEMENTED
MULTIPLAYER VACANCY RESERVATION: IMPLEMENTED

PROPERTY SHELL PRODUCTION BINDING: NO
FURNITURE RUNTIME: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
