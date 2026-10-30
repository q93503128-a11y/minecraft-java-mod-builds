# Augmentation Survival — Manual Playtest Backlog

This file tracks only player-facing behavior that is still unresolved or has changed since the latest player-confirmed playtest. Do not reopen confirmed items unless related code changes again.

## Active retest after the current repair build

- [ ] `water_run` — with the augment owned, stand still, walk and sprint on a water surface without needing sprint input. Crouching must let the player sink. A fall below 5 blocks should still catch the surface, while a 5-block-or-higher dive must pass through it. Lava must remain unaffected.
- [ ] `return_to_sender` — block real attacks with a shield. Blocked damage accumulates; each 10 damage (5 hearts) of blocked damage emits one forward cone wave toward the entity that attacked the shield. The wave reaches 7 blocks, spans 45 degrees total, damages valid enemies in the cone, excludes allies/companions, and returns 60% / 80% / 100% of compressed damage at Lv.1 / 2 / 3. Leftover blocked damage carries into the next block.
- [ ] `last_stand` + `delayed_damage` — create real delayed-damage debt and let a later installment trigger Last Stand. Activation must leave 1 HP, immediately erase all remaining delayed installments, and prevent any new delayed reservation during the 3-second protection window. When the protection ends there must be no old delayed damage waiting to kill the player.
- [ ] `portable_riptide` — Survival, dry land, Riptide trident, water bucket in the offhand. Dry Riptide must launch repeatedly without consuming or degrading the water bucket. Confirm the normal visible Riptide body-spin animation also plays on dry-land launches.

## Confirmed in the 2026-10-30 player playtest

The player reported the remaining items from the previous manual backlog as working. Do not ask for another pass on those confirmed behaviors unless code affecting them changes again.

## Deferred change request

- `aerial_combo` — the player requested a later change so its air jump uses 1.5x jump strength. This is explicitly deferred and is not part of the current repair build or current retest list.

## Reporting rule

Automated tests/builds can validate code boundaries, but only the four active items above need another player-facing pass after this repair build. Report failures as `augment O/X - symptom` so the next repair is reproducible.
