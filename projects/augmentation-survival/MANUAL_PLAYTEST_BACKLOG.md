# Augmentation Survival — Manual Playtest Backlog

This file is the durable reminder for the final in-game pass. Do not mark an item complete from compile/build/GameTest results alone.

## Must re-check because code/presentation changed after earlier playtest coverage

- [ ] `return_to_sender` — block an arrow while aiming elsewhere; confirm ownership/velocity transfer survives the next tick and the reflected arrow actually travels in the intended direction.
- [ ] `portable_riptide` — use Riptide while dry with a water bucket; confirm the vanilla launch occurs and exactly one water bucket becomes an empty bucket.
- [ ] `reroll` — reroll an offer once; confirm one charge is consumed, the card changes once, and the same click does not also select the card.
- [ ] `retrace` — inspect the augment card; confirm the summary/detail are visible Korean text, explain the last-2-second path rewind, 50/75/100% damage and 5-second internal cooldown, and never expose `ROOT`.
- [ ] `delayed_damage` + `last_stand` — take a hit large enough that a later reserved installment becomes lethal; confirm Last Stand leaves 1 HP and the immediately following reserved installment cannot damage that 1 HP during the 3-second protection window.
- [ ] `wall_slam` — melee a target into/near a wall; confirm bonus damage resolves once at wall contact and the wall impact point shows visible world-space particles plus an impact sound rather than only HUD feedback.
- [ ] `aerial_combo` — own Aerial Combo without Double Jump, stay airborne and hold jump; hit an enemy, consume the air jump, then hit another enemy before landing and confirm another air jump is recharged/consumed. Repeat several times to check the intended infinite aerial-combat loop.
- [ ] active HUD auto-equip — acquire a new active augment with at least one empty HUD slot; confirm it appears immediately in the first empty slot without overwriting manually assigned slots.
- [ ] active HUD respawn/rejoin — die and respawn, then disconnect/rejoin once; confirm active slots do not remain blank and the server-owned loadout reappears without opening another screen.
- [ ] `temporary_platform` — while falling with meaningful accumulated fall distance, activate the skill from the actual active HUD slot; confirm the 3x3 platform appears directly below, landing causes no fall damage, and occupied blocks are not overwritten.

## Hero-priority final feel pass

- [ ] `double_jump` — confirm 1.30x jump-power vertical impulse, +0.08 same-direction horizontal reboost, no direction rotation, and no forced forward movement when nearly stationary. If it feels weak, tune horizontal reboost before vertical height.
- [ ] `pull`
- [ ] `shield`
- [ ] `shield` + `delayed_damage`
- [ ] `shield_throw`
- [ ] `spell_refresh`
- [ ] `water_run`
- [ ] `shield_burst` — visible/mechanical radius should both read as 6 blocks.
- [ ] `life_wave` — visible/mechanical radius should both read as 6 blocks.
- [ ] `tracking_arrow` — 16-block acquisition, 45-degree cone, level turn limits 4/6/8 degrees per tick.
- [ ] general hostile combat feel — check zombie HP/time-to-kill under the current threat curve; report feel problems separately from code correctness.

## Verification rule

Automated tests/builds can close regressions, but this checklist stays unchecked until an actual Minecraft play session confirms the player-facing result. Record failures as `augment O/X - symptom` so the next repair pass is reproducible.
