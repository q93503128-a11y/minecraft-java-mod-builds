# Augmentation Survival — Manual Playtest Backlog

This file tracks only player-facing behavior that is still unresolved or has changed since the latest player-confirmed playtest. Do not reopen confirmed items unless related code changes again.

## Active retest after the current repair build

- [ ] `water_run` / **수상 보행** — stand, walk and sprint across real water surfaces. The surface should feel like ground from above, including flowing-water height. Crouching must let the player sink. A fall below 5 blocks should land without fall damage; a 5-block-or-higher dive must pass through. While already underwater, swimming upward must cross the surface normally instead of trapping the player below it. Lava must remain unaffected.
- [ ] `portable_riptide` / **휴대 수류** — Survival, dry land, Riptide trident in the main hand, water bucket in the offhand. Repeated dry Riptide uses must keep the bucket intact and must show the same visible body-spin animation as ordinary wet Riptide.

## Next manual scope — Legendary tier

The next broader player pass will include Legendary augments. Their card/tooltip text is now hand-authored instead of relying on mechanical summary extraction. Test them naturally and report only behavior or wording that is actually wrong; previously confirmed lower-tier items do not need another pass.

## Confirmed / do not reopen

- `return_to_sender` — no new failure was reported after the shield-compression redesign pass.
- `last_stand` + `delayed_damage` — no new failure was reported after the delayed-debt clearing/protection repair.
- All items already recorded as working in the 2026-10-30 playtest remain closed unless code affecting them changes again.

## Deferred change request

- `aerial_combo` — later change requested: its air jump should use 1.5x jump strength. This remains deferred and is not part of the current repair build.

## Reporting rule

Automated tests/builds validate code boundaries, but only the two active repaired mechanics above require focused retesting. The Legendary pass is new coverage rather than a repeat of already confirmed items. Report failures as `augment O/X - symptom` so the next repair is reproducible.
