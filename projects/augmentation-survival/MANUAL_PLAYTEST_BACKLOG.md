# Augmentation Survival — Manual Playtest Backlog

This file tracks only player-facing behavior that is still unresolved or has changed since the latest player-confirmed playtest. Do not reopen confirmed items unless related code changes again.

## Active retest after Bundle E

- [ ] `return_to_sender` / **돌려보내기** — use a real damageable shield in Survival. Only attacks that actually consume shield durability should count. Expected threshold: Lv.1 / Lv.2 / Lv.3 = **5 / 4 / 3 blocks**. The shockwave remains 7 blocks / 45 degrees and returns **60% / 80% / 100%** of the accumulated blocked attack damage.
- [ ] `area_acceleration` / **영역 가속** — compare a crop patch inside the level-2 x7 field against a nearby outside-control patch. Growth should now feel continuously faster rather than arriving as rare bursty extra random ticks. Verify the left-side sustained timer/range presentation does not overlap vanilla upper-right status effects.
- [ ] `time_acceleration` / **시간 가속** — after the focus completes, verify the real sustained timer appears on the left and movement, attack/mining feel, item-use speed and owned projectile launch speed feel consistent with the same 60% acceleration contract.
- [ ] recurring invulnerability shield — confirm the translucent gold shell is vertically centered on the avatar after its center moved to `-0.82`, and that its persistent status presentation no longer competes with vanilla upper-right effects.
- [ ] `phase_shift` / **위상 이동** — entering solid terrain while the effect is active must not deal in-wall suffocation damage. Ordinary non-wall damage must still work.
- [ ] `rewind` after Phase Shift — record history while phasing through terrain, leave Phase Shift, then rewind. The player must resolve to nearby safe free space instead of being restored inside a solid block.
- [ ] `water_run` / **수상 보행** — stand, walk and sprint across real water surfaces. The surface should feel like ground from above, including flowing-water height. Crouching must let the player sink. A fall below 5 blocks should land without fall damage; a 5-block-or-higher dive must pass through. While already underwater, swimming upward must cross the surface normally instead of trapping the player below it. Lava must remain unaffected.
- [ ] `portable_riptide` / **휴대 수류** — Survival, dry land, Riptide trident in the main hand, water bucket in the offhand. Repeated dry Riptide uses must keep the bucket intact and must show the same visible body-spin animation as ordinary wet Riptide.

## Investigation closed without a mod change

- Spawn eggs or unrelated newly granted items entering free hotbar slots are not attributed to Auto Refill. Auto Refill only restores the selected empty slot using the exact prior item/component stack; vanilla `/give` / Creative insertion can independently prefer free hotbar slots.
- Monster-room chests currently use an 8% normal-core roll per chest. Four chests have only about a 28.4% chance of at least one core, so one normal core from four chests is not evidence of a loot bug.

## Confirmed / do not reopen without a new symptom

- `last_stand` + `delayed_damage` — no new failure was reported after the delayed-debt clearing/protection repair.
- Previously confirmed lower-tier items remain closed unless code affecting them changes again.

## Deferred change request

- `aerial_combo` — later change requested: its air jump should use 1.5x jump strength. This remains deferred and is not part of Bundle E.

## Verification boundary

Bundle E code checkpoint `0857a86cbabcfe2f55c22e3cef0bbdce3bc79171` passed **132/132 required GameTests**, clean build, dedicated-server boot smoke, client boot smoke, localization checks, verified MRPack generation and production-JAR verification in workflow #520. These checks do not replace the manual visual/feel retests above, and multiplayer behavior after Bundle E has not been manually tested.

Report failures as `augment O/X - symptom` with the simplest reproducible situation possible.
