# Augmentation Survival — Manual Playtest Backlog

This file is the durable reminder for the final in-game pass. Do not mark an item complete from compile/build/GameTest results alone.

## Rare regressions to re-check

- [ ] `reroll` — click the real reroll button; exactly one charge and one candidate change, with no click-through card selection.
- [ ] `return_to_sender` — block a real projectile with a shield while aiming elsewhere; the blocked original disappears and a newly owned projectile travels in the look direction without immediately diving into the ground. Repeat from several angles and confirm ownership/velocity survives the next tick.
- [ ] `portable_riptide` — Survival, dry land, Riptide trident, water bucket present. Confirm the vanilla Riptide launch occurs; one successful dry launch changes exactly one water bucket to an empty bucket; inventory placement does not snap the bucket back to the hotbar. Re-check normal water/rain behavior too.
- [ ] `retrace` presentation — card must explain the last-2-second path rewind, reverse-moving afterimage, path damage 50/75/100%, and 5-second internal cooldown. It must never expose `ROOT` or other internal terms.

## Hero regressions and interaction checks

- [ ] `delayed_damage` + `last_stand` — reserve a large hit so a later installment is lethal. Last Stand must leave 1 HP and the following reserved installment must not damage that 1 HP during the 3-second protection window. Delayed installments should cause no repeated knockback or camera shake, but successful HP loss must still produce an audible damage sound.
- [ ] `last_stand` alone — ordinary lethal damage leaves 1 HP, grants 3 seconds of ordinary-damage protection, and cannot retrigger during its 180-second cooldown.
- [ ] `wall_slam` — use a real direct melee attack and knock the target into or very near a wall. Bonus damage should resolve once with forgiving contact, with world-space particles and impact sound at the wall contact point; simply remaining near the wall must not cause repeated damage.
- [ ] `aerial_combo` — own Aerial Combo without Double Jump, stay airborne and hold jump, land a direct melee hit, consume the recharged air jump, then hit again before landing. Repeating hits, including the same target, should sustain the intended aerial-combat loop; allied/derived/companion/projectile hits must not recharge it.
- [ ] active HUD auto-equip — gain a new active augment with an empty HUD slot and confirm it appears immediately in the first empty slot without overwriting manually assigned slots.
- [ ] active HUD respawn/rejoin — die/respawn, then disconnect/rejoin once. The server-owned loadout must reappear automatically; opening K must not be required to repair blank slots.
- [ ] `temporary_platform` — activate from the real active HUD slot while falling with meaningful fall distance. A 3x3 platform appears directly below, occupied blocks are not overwritten, landing fall damage is zero, and the platform later expires. If practical, restart the server once to confirm it does not persist forever.
- [ ] `tracking_arrow` — test an ordinary shot with no ricochet. Tracking begins immediately, continues during normal flight and descending flight, reacquires within 16 blocks / 45°, and respects 4/6/8 degrees per tick at Lv.1/2/3. A neutral non-ally such as an iron golem may be targeted; the player's allies/companions must not be targeted.
- [ ] `water_run` — sprint into a water surface and continue running on it. Releasing sprint/ending the movement condition must return to normal swim/fall behavior; residual velocity alone must not sustain water running forever.
- [ ] `pull` — use the actual active slot. Ordinary animals/neutral entities should be pulled, player allies must not be pulled, bosses should move less than ordinary targets, and compatibility profiles with `allowPull=false` must remain fixed.
- [ ] `shield_throw` — use the actual active slot. The real shield leaves the hand, flies/hits/ricochets/returns, and the original ItemStack returns intact. Re-check death/respawn/disconnect and an inventory-full return path for duplication or loss.
- [ ] `enchant_refresh` (`마법 새로고침`) — use the actual enchantment menu refresh control. A successful refresh consumes exactly 5 XP points + 1 lapis and changes candidates; a failed refresh must not consume either cost.
- [ ] `shield` — confirm the augment shield absorbs damage according to its pool rules.
- [ ] `shield` + `delayed_damage` — damage absorbed by the augment shield must never reappear later as delayed damage.
- [ ] `shield_burst` — depletion or expiration should trigger exactly one burst. Mechanical and visible radius should both read as 6 blocks.
- [ ] `life_wave` — a real positive, non-natural primary heal triggers the wave at radius 6; shared healing must not recursively create another wave. Mechanical and visible radius should match.
- [ ] `double_jump` — confirm vertical impulse is actual jump power ×1.30 and horizontal reboost is +0.08 in the current travel direction. It must not rotate toward look direction or force forward motion while stationary/nearly stationary. If feel is weak, tune horizontal reboost before vertical height.
- [ ] general hostile combat feel — check ordinary zombie HP/time-to-kill under the current threat curve. Report “too slow / too tanky / flat” as feel feedback rather than assuming code correctness from the scaling table.

## Presentation audit

- [ ] Re-open cards/details for `reroll`, `return_to_sender`, `portable_riptide`, `retrace`, `delayed_damage`, `last_stand`, `wall_slam`, `aerial_combo`, `temporary_platform`, `tracking_arrow`, and `water_run`. Text must match the current live behavior and must not expose internal terms such as `ROOT`, `DERIVED`, `P0`, or `debug`.

## Verification rule

Automated tests/builds can close regressions, but this checklist stays unchecked until an actual Minecraft play session confirms the player-facing result. Record failures as `augment O/X - symptom` so the next repair pass is reproducible.
