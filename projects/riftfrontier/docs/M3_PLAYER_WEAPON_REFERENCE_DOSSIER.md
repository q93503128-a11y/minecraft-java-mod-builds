# M3 Player Weapon Reference Dossier

Status: **role-contract lock only**. This document approves exactly two player-combat family roles and one module-composition direction. It does **not** approve final weapon names, models, textures, animations, VFX, sound, damage, range, cooldown, hitboxes, stamina costs, cadence, or balance values.

Research date: 2026-09-10.

## Purpose

`M3_PLAYER_COMBAT_KERNEL.md` already provides `weapon_family` and `weapon_module` schema/validation. The missing gate is evidence for what the first two production families are supposed to *feel and behave like* before any production JSON or Minecraft execution is authored.

The first vertical slice needs contrast, not quantity. The two families therefore must disagree on commitment, reach, mobility, recovery and positioning pressure while still sharing `AttackPattern` as the only authoritative `telegraph -> ACTIVE -> recovery` clock.

## Bounded references

### 1. Monster Hunter — Great Sword: commitment has to be visible

Capcom's official Monster Hunter 4 Ultimate manual describes Great Sword as sacrificing mobility for large punishing attacks, with charge timing and blocking as explicit parts of the kit. The useful lesson is not the weapon model or exact charge mechanic. It is that a high-commitment family should make its risk visible *before* payoff and should have a clearly legible failure cost when the player commits at the wrong time.

Riftfrontier lesson:

- commitment is a gameplay property, not just a slower animation;
- a committed move needs an obvious anticipation state and meaningful recovery;
- reach/impact cannot erase positioning risk;
- defensive permission, if any, must be authored deliberately rather than assumed from weapon shape.

Source: https://game.capcom.com/manual/MH4U/en/page-101.html

### 2. Monster Hunter — Dual Blades: mobility must pay for itself with constraints

Capcom's official Monster Hunter Generations manual frames Dual Blades around continuous close-range offense, Demon Mode, a draining stamina constraint, a dedicated evade and a gauge that changes available actions. The useful lesson is that a mobile pressure family is not merely "the fast weapon". Mobility is coupled to short engagement distance, continued commitment to the target and a resource/state cost that prevents free safety.

Riftfrontier lesson:

- mobile pressure should require repeated proximity and repositioning;
- short reach is a real identity constraint, not a number to silently compensate away;
- mobility should alter how the player exits or re-enters commitment windows;
- a future resource layer may support the role, but this dossier does not invent one yet.

Source: https://game.capcom.com/manual/MH_Gen/en-UK/page-104.html

### 3. Darktide — attack chains and exits are part of weapon identity

Fatshark's official Arbites weapon notes explicitly distinguish light/heavy attacks, push-attacks, chain routes, block cancels and attacks that trade damage/cleave/control in different sequences. Their 2025 balance notes also treat chain timing, damage windows, hitboxes and range as separate tunable concerns. The useful lesson is architectural: a weapon family is a graph of authored actions and exits, not one DPS value attached to an item.

Riftfrontier lesson:

- move transition permissions belong to weapon semantics;
- recovery/cancel opportunities must be explicit and testable;
- utility/control actions should not be encoded as raw damage bonuses;
- hitbox/range/timing remain separate evidence gates and must not be guessed from role labels.

Sources:

- https://www.playdarktide.com/news/arbites-class-out-now
- https://www.playdarktide.com/news/june-update-early-balance-look

### 4. Hades II — a variant is worthwhile when it changes fighting style

Supergiant's official Unseen Update describes Hidden Aspects as giving every main weapon an all-new fighting style and separately calls out the Umbral Flames rework as a snappier keep-away style. The useful lesson for Riftfrontier modules is that a composition option should change decisions and transitions, not exist as a percentage-stat socket.

Riftfrontier lesson:

- module value is measured by changed behaviour, not stat inflation;
- the base family must remain recognizable after composition;
- one module line in the vertical slice is enough if it creates a real alternative decision pattern.

Source: https://www.supergiantgames.com/blog/hades2-unseen-update/

## Comparison matrix

| Axis | High-commitment reference lesson | Mobile-pressure reference lesson | Riftfrontier lock |
|---|---|---|---|
| Entry | wait for a readable opening | enter repeatedly from close range | families must expose different engagement decisions |
| Commitment | high and obvious | lower per action, accumulated through continued pressure | no universal cancel-anytime melee |
| Reach | enough to reward spacing | deliberately short | reach identity may not be normalized away |
| Mobility | restricted during payoff | active repositioning is core | movement permission is authored per move/transition |
| Recovery | meaningful punish window after commitment | shorter exits but repeated exposure | recovery remains in `AttackPattern` |
| Counterplay/failure | whiff or mistimed commitment creates vulnerability | lost proximity / bad reposition breaks pressure | both families need observable failure states |
| Composition | protect base identity | alter route/exit choices | module changes behaviour, not a hidden parallel clock |

## Locked production role contract A — Mobile Pressure

Working semantic label: `mobile_pressure`. This is a **role label, not a final item or family content ID**.

Required identity:

- close-range family whose success comes from staying near a target and repeatedly regaining favorable angle/position;
- lower commitment on ordinary entries than Contract B, but cannot convert that mobility into unconditional safety;
- deliberately shorter reach than Contract B;
- must have at least one authored attack transition that advances or repositions and at least one committed finisher/reward action whose recovery can be punished if used at the wrong time;
- pressure should break when the player mistimes proximity, loses the target line, or commits the finisher into a bad opening;
- no automatic homing/teleport-to-target behaviour is implied by this contract;
- no permanent invulnerability, free cancel-anytime rule or stat compensation for short reach is approved here.

Counterplay/failure contract:

- enemies can punish an overextended committed finisher;
- displacement, spacing and area denial can force the player to rebuild pressure;
- the player's answer is repositioning and timing, not simply outranging the encounter.

## Locked production role contract B — Reach Commitment

Working semantic label: `reach_commitment`. This is a **role label, not a final item or family content ID**.

Required identity:

- melee family that gains value from deliberate spacing and a clearer attack line than Contract A;
- longer practical engagement reach than Contract A, paired with visibly higher commitment on its primary payoff action;
- movement during the decisive anticipation/ACTIVE sequence must be more constrained than Contract A unless a later authored move explicitly spends another resource or trade-off to change that rule;
- must expose a readable anticipation and a real post-commitment recovery window;
- should reward choosing the correct lane/opening rather than chasing continuously;
- any defensive action must be separately authored and cannot be inferred from a large weapon silhouette;
- no damage, stagger, guard strength or exact reach value is approved here.

Counterplay/failure contract:

- enemies can punish a whiff, side-step the committed line, or pressure the player during recovery;
- the player's answer is spacing, target-line prediction and choosing a safe commitment window rather than rapid repeated correction.

## Locked module-composition line — Recovery Pivot technique

Socket semantic: `technique`.

Behaviour-change semantic: `recovery_pivot`.

This is the one approved first-slice module direction. It is not yet production JSON and has no numeric effect.

Contract:

- the module changes **what transition is available during an eligible authored recovery**, not the attack's authoritative hit timing;
- an eligible family/move may trade its normal immediate follow-up route for one bounded reposition transition after commitment;
- the pivot may not create a second telegraph/ACTIVE/recovery clock, skip an attack's required recovery, grant generic invulnerability, or silently shorten server-authoritative hit windows;
- compatibility is opt-in per `weapon_family`; a family that does not declare the `technique` socket cannot receive it;
- exact movement distance, timing, stamina/resource cost and invulnerability policy remain runtime/field-play design work and are **not approved** here.

Why this line is useful: it tests the existing composition kernel with an actual behaviour-routing choice. It can create a different post-commitment decision without turning the module system into `+X% damage` sockets.

## Production authoring gate after this dossier

The next implementation may author production data only if all of the following remain true:

1. exactly these two role contracts are used for the first vertical slice; do not add a third family to avoid resolving a design problem;
2. each real move is an `attack_pattern` and `AttackPattern` remains the only cadence source;
3. the two families differ structurally in reach/mobility/commitment/recovery, not merely in names or damage;
4. `Recovery Pivot` is expressed through the existing `weapon_module` semantics and compatible `technique` socket rather than a weapon-specific Java subclass;
5. no final damage/range/cooldown/hitbox/stamina/cadence values are invented before server-authoritative execution exists and field-play can measure them;
6. no final art, animation, VFX or sound is approved by this dossier;
7. actual hit volume/visual alignment remains an M3 presentation and Minecraft-screen gate.

## Explicit non-goals

- no third weapon family;
- no final weapon names;
- no copied Monster Hunter/Darktide/Hades moves, code, animation, UI, audio or assets;
- no third-party balance numbers;
- no promotion of existing fixture IDs/timings;
- no reuse of boss presentation plumbing as player weapon runtime;
- no client-authoritative damage or hit confirmation.

## Exact next engineering boundary

Author the smallest production `attack_pattern` set required to express the two locked contracts, then production `weapon_family` definitions with the shared `technique` socket and one `weapon_module` definition for `recovery_pivot`. Extend validation only if a real ambiguity appears; do not duplicate the already-complete schema kernel. After data validation, connect the authored definitions to a server-authoritative Minecraft player-combat execution boundary before assigning field-balance values.