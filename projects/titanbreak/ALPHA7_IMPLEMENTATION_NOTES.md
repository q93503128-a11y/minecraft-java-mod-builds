# alpha.7 implementation notes

## Why alpha.6 was not sufficient
alpha.6 returned the integrated server to 20 TPS, but it slowed nearby entities by cancelling a fraction of complete entity ticks. That removed the alpha.5 player attack-timing regression, yet it reused the same broad family of sparse-tick simulation that had already been risky for visual continuity: an affected entity only truly advanced on selected ticks.

alpha.7 removes complete entity-tick cancellation from Reflex Drive. The server stays at 20 TPS and affected entities still receive their ordinary entity tick every server tick.

## Continuous temporal path
The P0 field is now split by subsystem instead of treating an entity tick as one indivisible clock:

- `Entity#move` movement vectors are scaled by the local temporal factor on the authoritative server. A slowed mob therefore receives movement updates every server tick rather than alternating between full movement and a frozen tick.
- `Mob#serverAiStep` uses a fractional accumulator. AI/navigation/goal progression runs at the local temporal rate, while the entity's base physics/renderable position can continue every 20 TPS tick.
- projectiles no longer have their whole tick cancelled. Their velocity is scaled when crossing between time scales, so their collision path follows the slowed motion continuously. Projectiles owned by the active Reflex Drive user inherit the user's Temporal Rating and remain at 1x inside that user's field.
- the active player remains entirely on the ordinary 20 TPS axis; no movement/attack/mining compensation modifier is part of the active path.

The current no-TR field factor remains 0.40 for direct comparison with the alpha.5/alpha.6 playtests.

## P0 limitations still under test
This is a subsystem split, not the final temporal engine. Projectile gravity/drag, custom modded projectile classes, living hurt timers, every custom mob timer, block ticks, redstone, weather/time-of-day and dedicated-server presentation synchronization still need separate coverage before the content-bible temporal model is considered complete.

The purpose of alpha.7 is narrower and explicit: prove that ordinary nearby mob movement and combat pacing can be slowed without global tick-rate mutation and without cancelling complete entity ticks. If this movement/AI split is smooth in the graphical playtest, the remaining time-sensitive subsystems can be moved onto the same server-authoritative local-time model one by one.
