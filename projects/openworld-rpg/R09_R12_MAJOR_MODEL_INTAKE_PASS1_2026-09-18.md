# Open-World RPG — R09–R12 Major Model Intake Pass 1 — 2026-09-18

> Scope: unresolved major player-facing actor/model gates in R09–R12.  
> Authority: `PROJECT.md`, `GAME_DESIGN.md`, `PRODUCTION_ASSET_BINDING_MATRIX.md`, and the matching regional content/package canon.  
> Result: **R09 and R11 gain bounded direct-review candidates; R10 remains current-Inferno accept/replace; R12 final guardian remains OPEN_MODEL_SELECTION.**

This pass does not redesign encounters. It narrows external model intake only.

---

## 1. Intake rules used

A candidate survives only when its body supports the already-authored encounter role.

Rejected shortcuts include:

- material/color match without useful attack anatomy;
- scaling an ordinary animal into a boss;
- humanoid armor simply because a region is desert/industrial;
- particle-only or floating-effect identity without readable physical tells;
- a generic golem for R12 when its body cannot express Network Link / Cascade / Partition / Reconfiguration states.

Marketplace availability is not production acceptance. Paid/local-only candidates remain outside the public repository unless their exact terms allow redistribution.

---

## 2. R09 optional major dryland hunt

Canon requires:

- Lv49 optional hunt;
- broad open terrain;
- committed movement / impact identity;
- no scaled common-animal placeholder;
- final attacks and trophy only after the accepted anatomy is known.

### Candidate A — Brimstone Behemoth

Source:
https://www.fab.com/listings/4ad4a7e7-afd5-4466-b4fc-ba64fd4a9576

Observed listing facts:

- armored infernal boar / bull-scale heavy charger silhouette;
- rigged and skinned;
- 30 baked animation clips;
- GLB + FBX delivery;
- about 10.4k triangles;
- specifically described around slow turning and brutal head-on charging.

Status:

```text
R09 direct-review prospect = YES
production acceptance = NO
license / acquisition = VERIFY
raw public-repo bytes = NO ASSUMPTION
```

Why it survives:

- its mass, tusks, plated back and charge-first body fit the locked committed-movement / impact role unusually well;
- the existing animation coverage is much stronger than a static heavy creature;
- it can support readable head-on danger, flank punish windows and recovery states without inventing magic anatomy.

Main risk:

- current infernal/brimstone art treatment may pull visually toward R10. If texture/material adaptation cannot make it read as Southstone dryland wildlife without destroying the source identity, reject it.

### Candidate B — Fantasy Creature: Combat Rhino

Source:
https://www.artstation.com/marketplace/p/mVvdV/fantasy-creature-combat-rhino

Observed listing facts:

- three creature variants;
- 33 animations;
- attack / defense / hit / dizzy / run / turn / walk / death coverage;
- low-poly real-time target;
- paid Standard / Extended commercial license options.

Status:

```text
R09 direct-review prospect = YES
production acceptance = NO
acquisition class = PAID / LOCAL INTAKE
```

Why it survives:

- stronger animation breadth than most marketplace rhinos;
- grounded heavy-impact identity fits Southstone better than overt elemental monsters;
- multiple variants provide a better chance of finding a non-generic silhouette.

Main risk:

- it must not become “just a large rhino.” Final acceptance requires a silhouette and encounter read sufficiently distinct from ordinary wildlife and from the Caravan Elephant role.

### Rejected / deprioritized in this pass

- static Oni Rhino Behemoth: strong silhouette but no rig/animation, so it creates too much production debt for this slot;
- ordinary realistic rhinoceros: useful animation reference but too close to scaled-common-animal presentation;
- bipedal Heavy Warrior Rhino / Reptilian: body grammar is too humanoid for the current broad-terrain heavy-beast slot;
- generic armored dinosaur/lizard candidates: kept only as references unless they outperform the two prospects above in direct 3D review.

### R09 next action

Directly inspect **Brimstone Behemoth vs Combat Rhino** in 3D/video before any further broad search.

If neither survives:

```text
search target =
heavy quadruped / siege-beast anatomy
+ explicit charge / turn / stagger / recovery animation
+ grounded dryland visual language
```

Do not search “desert monster” generically.

---

## 3. R10 final guardian / Inferno

Current canon already says Inferno is a candidate, not an automatic acceptance.

This pass found many lava/industrial/golem alternatives, but none justifies skipping the current dependency review.

Decision:

```text
R10 next step = CURRENT INFERNO DIRECT RUNTIME REVIEW
new broad model search = DEFER
```

Acceptance must inspect:

- current Fabric 26.2 model quality;
- animation coverage;
- melee uptime;
- visible attack range vs actual hitbox;
- weak-point/core readability;
- suitability for Coupled Forge / pressure / cascade presentation;
- camera behavior at Minecraft scale.

Only if current Inferno fails should a replacement search reopen.

Reason:

R10's authored story already supplies strong context and body-independent system mechanics. Replacing a potentially adequate dependency boss before viewing it would be churn.

---

## 4. R11 Breakwater Keep final boss

Canon requires an optional sea-fort final commander/guardian. Tidecross itself must remain traders/pilots/sailors rather than a cartoon pirate city, so the dungeon boss can use a corsair/fort-commander identity without turning the whole region into pirate parody.

### Preferred direct-review candidate — Pirate Captain Gameready with animations

Source:
https://www.cgtrader.com/3d-models/character/fantasy-character/pirate-captain-gameready-with-animations

Observed listing facts:

- low-poly realistic PBR pirate captain;
- sword + pistol;
- 22 included animations;
- idle / walk / run / strafe / hit / death / attack / combo coverage;
- modular clothing/accessories;
- multiple LODs;
- Unity / Unreal integrations;
- Royalty Free marketplace license;
- paid asset.

Status:

```text
R11 Sea-Fort direct-review prospect = STRONG
production acceptance = NO
acquisition class = PAID / LOCAL INTAKE
raw public-repo bytes = NO
```

Why it survives:

- complete humanoid combat animation coverage makes it much more production-useful than static pirate meshes;
- modular layers allow a restrained fortified-corsair silhouette rather than a theme-park pirate;
- sword/pistol anatomy naturally supports a boss that mixes melee pressure and projectile control, matching the existing deterministic ranged/control reward direction.

Required review before acceptance:

- remove/avoid presentation that becomes comic-pirate cliché;
- ensure pistol/projectile telegraphs remain readable in Minecraft;
- confirm melee reach and animation timing;
- check if animation quality remains acceptable after retarget/conversion;
- verify no animation or outfit piece causes camera/readability problems at Minecraft scale.

### Secondary references

Several rigged ArtStation pirate captains are visually useful, but the inspected listings include **no animations**. They remain reference or fallback rig sources, not preferred production candidates for this boss.

### R11 next action

Acquire/preview the animated CGTrader Pirate Captain only if the owner accepts a paid local-only candidate.

If rejected, continue searching specifically for:

```text
armored corsair / fort commander
+ humanoid retargetable skeleton
+ real melee + ranged animation coverage
+ grounded maritime military silhouette
```

Do not search generic “pirate NPC.”

---

## 5. R12 final systemic guardian

Canon requires the final body to support the already-authored finale mechanics:

- Network Link;
- Cascade Line;
- Partition Window;
- Reconfiguration;
- readable phase-state changes;
- final-boss-scale camera/hitbox truth.

### Screened candidates

This pass reviewed/search-screened:

- generic Arcane Guardian humanoids;
- animated armored knights/guardians;
- elemental/arcane golem packs;
- crystal golems;
- steampunk furnace-core golems;
- runic/arcane constructs.

Result:

```text
accepted direct-review candidate = NONE
R12 final systemic guardian = OPEN_MODEL_SELECTION
```

Reason:

Most candidates have only one static humanoid body topology. A glowing chest core, runes or “arcane” label is not enough. The finale needs a body whose state can visibly **open, separate, reconnect, expose sectors, change geometry or redistribute active components** so the network mechanics are legible without relying on arbitrary particles.

### Rejection rule strengthened

Do not accept:

- a knight with glowing runes;
- a generic crystal golem;
- a normal robot with a glowing chest;
- a steampunk golem whose only meaningful state is attack/not-attack;
- a humanoid magic creature where Partition/Reconfiguration would have to be represented only by VFX.

### Next search grammar

Search by function/anatomy instead of fantasy material:

```text
segmented guardian
multi-part construct
detachable / orbiting body components
rotating ring / articulated sector structure
opening core cage
reconfigurable boss body
mechanical-magical network avatar
```

A static base model is only acceptable if its source geometry is explicitly separable enough to author these states without rebuilding most of the creature.

---

## 6. R12 anomaly roster

No broad roster expansion was performed.

Rule retained:

- keep anomaly actors sparse;
- first inspect currently intended dependency candidates;
- only replace individual failed roles;
- do not populate R12 with many unrelated “weird” monsters just because the region is anomalous.

---

## 7. Pass-1 production delta

| Slot | Before | After this pass |
|---|---|---|
| R09 dryland major hunt | OPEN_MODEL_SELECTION | two bounded direct-review prospects: Brimstone Behemoth / Combat Rhino |
| R10 final guardian | Inferno candidate / OPEN_MODEL_SELECTION | no search churn; current Inferno direct runtime accept/reject is explicitly first |
| R11 Breakwater Keep boss | OPEN_MODEL_SELECTION | strong paid/local-only direct-review prospect: animated Pirate Captain |
| R12 systemic final guardian | OPEN_MODEL_SELECTION | remains open; generic arcane/golem candidates explicitly rejected by topology rule |
| R12 anomaly roster | OPEN_MODEL_SELECTION where dependency fails | unchanged; sparse replacement-only rule retained |

---

## 8. Next efficient action

Do not restart broad R09–R12 research.

Order:

1. R09: direct visual/animation review of Brimstone Behemoth and Combat Rhino;
2. R10: inspect current 26.2 Inferno dependency in the actual client/JAR environment;
3. R11: preview/acquire the animated Pirate Captain only if paid local-only intake is acceptable;
4. R12: continue only topology/function-based final-guardian search;
5. once a model is accepted, write the exact anatomy-supported attack/weak-point/signature-material sheet;
6. then update `PRODUCTION_ASSET_BINDING_MATRIX.md` from `OPEN_MODEL_SELECTION` to the appropriate validation state.

---

## 9. Verification state

```text
CURRENT CANON REVIEWED: YES
R09/R10/R11/R12 ROLE CONTRACTS REVIEWED: YES
EXTERNAL WEB CANDIDATES SCREENED: YES
MODEL BYTES ACQUIRED: NO
3D VIEWER REVIEW: NO
MINECRAFT CLIENT REVIEW: NO
LICENSE / PURCHASE TERMS FULLY ACCEPTED: NO
BUILD VERIFIED: NO
PLAYTESTED: NO
MULTIPLAYER TESTED: NO
```
