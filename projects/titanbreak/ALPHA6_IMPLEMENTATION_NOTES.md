# alpha.6 implementation notes

## Root cause fixed
alpha.5 slowed the entire integrated server to 8 TPS and then repaired selected player attributes by about 2.5x. That was enough for horizontal movement, mining speed and attack-strength recovery, but it could not restore player systems whose timing is driven directly by ticks. The visible melee hand swing is one of those systems, so `ATTACK_SPEED` compensation could not make the animation return to normal wall-clock duration. The same architecture could also distort jump/fall pacing, knockback integration, hurt timers, item cooldowns and other player timers.

alpha.6 removes Reflex Drive's global `TickRateManager` mutation. The drive user stays on the ordinary 20 TPS time axis and nearby entities are selectively throttled inside a 64-block field instead.

## P0 temporal field
- The current non-user relative rate is 0.40, preserving the approximate 8/20 world pacing used for the alpha.5 playtest while the architecture changes.
- Entity ticks are distributed evenly over time rather than running in a burst at the start of a fixed window.
- A drive user's projectile inherits the owner's Temporal Rating while inside the field, so arrows and similar projectiles are not accidentally slowed together with the target.
- Player attribute compensation and shortened item-use duration are no longer required and are removed from the active path.
- Heat, cooling and mentality costs keep the same approximate real-time rates as alpha.5 after returning the player to 20 ticks per second.

## Scope boundary
The content bible's final target is a range-local server-authoritative temporal field rather than a global world tick-rate change. This pass moves to that architecture for entity simulation. Range-local block ticks, redstone, weather/time-of-day handling and dedicated-server client field synchronization remain later P0 work. The final no-TR relative-rate target can be tuned down from the current 0.40 playtest value after movement, combat and projectile timing are verified.
