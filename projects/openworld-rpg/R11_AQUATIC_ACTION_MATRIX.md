# Open-World RPG — R11 Aquatic Action / Animation Compatibility Matrix

> Status: **DESIGN CANON — R11 aquatic action compatibility closed before source bootstrap**  
> Parent regional canon: `R11_IMPLEMENTATION_PACKAGE.md`, `R11_CONTENT_BIBLE.md`  
> Combat/class canon: `COMBAT_BALANCE.md`, `CLASS_COMBAT_KITS.md`  
> Runtime/animation boundaries: `M0_DEPENDENCY_AUDIT.md`  
> Global input/comfort/audio canon: `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`  
> Rule: this file closes the former R11 `AQUATIC_NATIVE / AQUATIC_ADAPTED / AQUATIC_DISABLED_WITH_FALLBACK` pre-code gate. It does not waive the project-wide exact-asset/provenance gate for unrelated models, VFX, SFX, boss assets or the general two-player revive source.

R11 keeps the same character build, stats, equipment, class, learned skills, resources and reward rules underwater. It does **not** add a second aquatic skill bar, aquatic weapon tier or water-only progression tree.

The only production problem this document solves is presentation/mechanical honesty in three-dimensional water.

---

# 1. Source and motion basis

The accepted base motion family is the already-selected Quaternius Universal Animation Library stack used elsewhere in the project.

Verified reusable clip identities include:

```text
UAL1
- Swim_Idle_Loop
- Swim_Fwd_Loop
- Sword_Attack
- Sword_Attack_RM
- Spell_Simple_Enter
- Spell_Simple_Idle_Loop
- Spell_Simple_Shoot
- Spell_Simple_Exit
- Consume

UAL2 / extended UAL family used by the project
- Sword_Regular_A
- Sword_Regular_B
- Sword_Regular_C
- Sword_Block
- Sword_Dash_RM
- Idle_Shield_Loop
- Idle_Shield_Break
- Shield_OneShot
- Shield_Dash_RM
- 2H_Swing_A
- 2H_Swing_B
- 2H_Slam
- 2H_Thrust
- 2H_Block
- Bow_Aim_Loop
- Bow_Release
```

License/provenance authority remains `EXTERNAL_SOURCES.md` and the accepted Quaternius source records. UAL1/UAL2 are treated as CC0 motion sources; exact imported archive/hash recording still happens through the normal asset-intake pipeline.

## 1.1 Layering rule

Normal deep-water posture is not an upright land stance.

Canonical presentation stack:

```text
swim base
  = Swim_Idle_Loop or Swim_Fwd_Loop

combat/cast overlay
  = accepted weapon/cast upper-body clip
  + suppress land-only hips/legs/root translation
  + orient torso/arms toward 3D aim vector within safe pitch limits

real displacement / hit / resource result
  = server-authoritative project state
```

This means the project may reuse a strong sword, bow, shield or spell clip without showing planted feet or a standing run pose underwater.

No client animation/root motion owns real movement.

## 1.2 Body orientation

- deep-water body aims primarily along movement/target direction;
- pitch is clamped to avoid unreadable upside-down combat;
- camera roll is not required for normal swimming;
- melee and guard overlays preserve enough chest/shoulder visibility to read telegraphs in multiplayer;
- ascent/descent changes the swim base pitch, not the player's gameplay hitbox orientation independently of server state.

---

# 2. Tag meanings

## `AQUATIC_NATIVE`

The action already works in 3D water with only ordinary swim locomotion blending and 3D targeting.

## `AQUATIC_ADAPTED`

The learned action remains the same action with the same normal progression/resource/cooldown identity, but water changes one or more of:

- body animation layering;
- directional movement curve;
- projectile trajectory;
- ground plane → nearest valid solid surface / bounded 3D volume;
- visual decal geometry;
- lunge/impact direction.

## `AQUATIC_DISABLED_WITH_FALLBACK`

Used only when the land action's physical premise is dishonest in open water. The slot automatically exposes a direct aquatic expression of the **same learned skill**. No separate unlock, skill point, equipment or loadout choice exists.

Fallback keeps the parent skill's resource cost, cooldown, total damage/heal/barrier/poise budget and class-mechanic interactions unless this file explicitly says otherwise.

---

# 3. Universal movement / interaction matrix

| Action | Tag | Aquatic presentation / rule |
|---|---|---|
| idle in water | `AQUATIC_NATIVE` | `Swim_Idle_Loop` |
| forward/strafe swim | `AQUATIC_NATIVE` | `Swim_Fwd_Loop` rotated/blended to horizontal input; no vanilla standing locomotion |
| ascend / descend | `AQUATIC_ADAPTED` | same swim base with bounded body pitch and vertical velocity; no separate stamina tax |
| sprint-equivalent swim | `AQUATIC_ADAPTED` | faster `Swim_Fwd_Loop` timing/stride presentation; server owns speed |
| universal dodge | `AQUATIC_ADAPTED` | becomes **Swim Burst**: same normal dodge commitment, Stamina cost and i-frame philosophy; directional 3D displacement replaces ground roll |
| ordinary interaction | `AQUATIC_ADAPTED` | short reach/hand overlay on swim idle; no kneeling/standing foot plant |
| loot pickup | `AQUATIC_ADAPTED` | hand/reach flourish only where readable; auto-pickup rules unchanged |
| recovery consumable | `AQUATIC_ADAPTED` | sealed recovery vial/ampoule is brought to the Deep-Dive Harness mouth/neck area; no literal long open-cup drinking underwater; same resolution timing/lockout |
| downed state | `AQUATIC_ADAPTED` | stabilized low-motion float/downed pose; server down/revive timers unchanged |
| revive/help another player | `AQUATIC_ADAPTED` | two-player stabilizing grasp in place of land kneel; same revive ownership/channel rules. Exact general revive source remains part of the global asset gate, not an R11 rules question |
| mount/dismount Laviathan | `AQUATIC_NATIVE` | donor/project mount animation pipeline owns the final rider transition; server seat/controller ownership unchanged |

### Swim Burst canonical carry-over

Use the normal universal dodge target unless later playtest changes the global dodge itself:

```text
duration: ~0.45 s
movement target: ~3.2 blocks
invulnerability: ~0.30 s
Stamina: 30
```

In water the vector may include vertical input. Collision can shorten the displacement but never extend i-frames because the player hit a wall.

---

# 4. Basic weapon / defense matrix

| Weapon/action family | Tag | Motion binding / water rule |
|---|---|---|
| 1H sword / dagger basic chain | `AQUATIC_ADAPTED` | `Sword_Attack` or `Sword_Regular_A/B/C` as upper-body overlays over swim base; lower-body/root land motion suppressed |
| spear / thrust-capable melee | `AQUATIC_ADAPTED` | `2H_Thrust`-style forward thrust is preferred over broad standing swings where available |
| greatsword / hammer / heavy 2H | `AQUATIC_ADAPTED` | `2H_Swing_A/B`, `2H_Slam` retimed/trimmed into committed water sweeps/pressure strikes; no global damage penalty; visible drag comes from recovery and motion, not hidden damage nerf |
| sword/dagger parry | `AQUATIC_ADAPTED` | `Sword_Block` upper-body overlay; normal perfect-guard windows remain server-owned |
| shield guard | `AQUATIC_ADAPTED` | `Idle_Shield_Loop`; swimming slows visually while guarding but no invented stationary footing |
| shield bash | `AQUATIC_ADAPTED` | `Shield_OneShot`; short server-authoritative forward impulse may be used where the land action already moves |
| 2H guard | `AQUATIC_ADAPTED` | `2H_Block` upper-body overlay |
| bow aim / release | `AQUATIC_ADAPTED` | `Bow_Aim_Loop` + `Bow_Release` over stabilized swim base; full 3D aim; projectile range/velocity may use the authored aquatic profile but no hitscan conversion |
| crossbow / future black-powder family | `AQUATIC_ADAPTED` | keep equipped family and project damage rules; underwater projectile/visual profile must remain readable and server validated |
| staff / wand / tome cast | `AQUATIC_ADAPTED` | `Spell_Simple_Enter/Idle/Shoot/Exit` upper-body overlay; feet/ground stance suppressed; 3D target vector |
| unarmed | `AQUATIC_ADAPTED` | compact hook/jab upper-body actions only; no grounded step-in footwork |

## 4.1 Hitbox rule

The visible weapon arc/projectile path remains the readability target. Water never authorizes a hidden spherical melee hitbox simply because 3D movement is harder.

## 4.2 Guard rule

Guard/parry is not globally disabled underwater. If one equipment combination cannot present an honest guard, that **equipment presentation binding** is fixed or replaced; the project does not silently remove the universal defensive system for the whole region.

---

# 5. Warrior matrix

| Skill | Tag | Aquatic expression |
|---|---|---|
| Driving Slash | `AQUATIC_ADAPTED` | directional 3D short lunge + compact slash; same movement cap/coefficients |
| Iron Counter | `AQUATIC_ADAPTED` | `Sword_Block` / compatible guard overlay → visible counter strike |
| Cyclone Cut | `AQUATIC_ADAPTED` | two committed rotational sweeps around the swimmer's forward axis; hit volume still matches visible blade reach |
| Breaker Slam | `AQUATIC_ADAPTED` | becomes a downward/forward **pressure breaker** rather than pretending to hit nonexistent ground; same learned skill and budgets |
| Earthshatter | `AQUATIC_DISABLED_WITH_FALLBACK` | automatic aquatic form **Pressure Shatter**: 7-block frontal/forward pressure rupture in the aimed 3D direction; same Ultimate cost, total coefficient and poise budget; no fake ground crack in open water |
| Rending Advance | `AQUATIC_ADAPTED` | swim-lunge slash, same armor-fracture rule |
| Relentless Breaker | `AQUATIC_ADAPTED` | heavy pressure strike using adapted 2H/weapon motion |
| Warpath | `AQUATIC_ADAPTED` | normal self-buff; impact waves become short pressure rings centered on contacted target |
| Master's Riposte | `AQUATIC_ADAPTED` | same counter timing with water guard overlay |
| Measured Assault | `AQUATIC_ADAPTED` | three upper-body weapon contacts over stabilized swim base |
| Grandmaster's Sequence | `AQUATIC_ADAPTED` | four-hit committed sequence with bounded 3D target redirection; no 180° auto-track |

---

# 6. Hunter matrix

| Skill | Tag | Aquatic expression |
|---|---|---|
| Quickstep Volley | `AQUATIC_ADAPTED` | directional Swim Burst + three projectiles; no extra i-frames beyond the skill's canon |
| Pinning Shot | `AQUATIC_ADAPTED` | 3D aim projectile; same Snared rules |
| Fan of Arrows | `AQUATIC_ADAPTED` | readable 3D cone from aim vector; same per-target hit cap |
| Power Shot | `AQUATIC_ADAPTED` | 3D charged shot; weak-point rules unchanged |
| Skyfall | `AQUATIC_DISABLED_WITH_FALLBACK` | automatic aquatic form **Currentfall**: authored projectile pulses converge through the selected 3D volume instead of arrows falling from a nonexistent sky; same duration, per-target coefficient cap, poise and slow budget |
| Snare Trap | `AQUATIC_ADAPTED` | anchors to nearest valid solid reef/fort/temple surface within placement range; cannot float invisibly in empty water |
| Ricochet Shot | `AQUATIC_ADAPTED` | 3D target-to-target bounce, one hit per target |
| Wild Hunt | `AQUATIC_ADAPTED` | automatic pulses use water-readable energy/projectile trails; mark rules unchanged |
| Heartpiercer | `AQUATIC_ADAPTED` | stabilized 3D charge shot |
| Deadeye Reposition | `AQUATIC_ADAPTED` | 3D backward/side swim step, no i-frames |
| Final Shot | `AQUATIC_ADAPTED` | long 3D projectile shot; still a real miss if aim fails |

---

# 7. Cleric matrix

| Skill | Tag | Aquatic expression |
|---|---|---|
| Radiant Lance | `AQUATIC_NATIVE` | direct 3D magic projectile/chain behavior |
| Mend | `AQUATIC_NATIVE` | target/self heal with stabilized cast overlay |
| Consecrated Ground | `AQUATIC_ADAPTED` | same radius/effects represented as an anchored luminous ward volume or nearest-surface sigil; no fake horizontal floor required |
| Rebuke | `AQUATIC_ADAPTED` | 3D frontal holy burst aligned to aim/body |
| Sanctuary | `AQUATIC_ADAPTED` | 7-block bounded ward volume around the caster rather than a flat floor decal only |
| Greater Mend | `AQUATIC_NATIVE` | same heal/cleanse rules |
| Hallowed Ground | `AQUATIC_ADAPTED` | anchored ward volume; same mitigation/heal budgets |
| Miracle | `AQUATIC_NATIVE` | radial pulse/field; revive acceleration unchanged |
| Judgment Bolt | `AQUATIC_NATIVE` | 3D magic projectile |
| Sentence | `AQUATIC_NATIVE` | target detonation; Judged ownership unchanged |
| Final Judgment | `AQUATIC_ADAPTED` | bounded 3D judgment volume around target point/caster; no flat-ground dependency |

---

# 8. Mage matrix

| Skill | Tag | Aquatic expression |
|---|---|---|
| Arc Bolt | `AQUATIC_NATIVE` | 3D projectile/fork |
| Phase Step | `AQUATIC_ADAPTED` | up to normal distance in the full 3D aim/movement vector, server collision-safe; no wall bypass |
| Frost Ring | `AQUATIC_ADAPTED` | spherical/ring pressure-frost burst centered on caster; same radius/status budget |
| Flame Burst | `AQUATIC_ADAPTED` | magic heat/arcane ignition volume at target point; VFX is magical combustion, not ordinary campfire flames pretending water is air |
| Astral Convergence | `AQUATIC_ADAPTED` | 3D convergence volume; target pull uses authored displacement caps |
| Chain Lightning | `AQUATIC_NATIVE` | normal 3D chained spell |
| Glacial Wave | `AQUATIC_ADAPTED` | aimed 3D cone/pressure wave |
| Primal Tempest | `AQUATIC_ADAPTED` | bounded 3D elemental volume; element order unchanged |
| Binding Prism | `AQUATIC_ADAPTED` | 3D prism/field volume, same control rules |
| Arcane Orbit | `AQUATIC_NATIVE` | orbit and launch behavior independent of ground |
| Singularity | `AQUATIC_NATIVE` | 3D center/pull effect, existing boss displacement restrictions unchanged |

---

# 9. Guardian matrix

| Skill | Tag | Aquatic expression |
|---|---|---|
| Bulwark Rush | `AQUATIC_ADAPTED` | shield-led directional swim charge; same collision/guard ownership |
| Warding Strike | `AQUATIC_ADAPTED` | compact 3D melee arc; Provoked unchanged |
| Aegis Field | `AQUATIC_NATIVE` | barrier volume centered on caster/allies; no ground requirement |
| Counterwall | `AQUATIC_ADAPTED` | water guard stance + frontal counter pressure wave |
| Unbroken Line | `AQUATIC_NATIVE` | moving aura follows Guardian in 3D; radius remains server-owned |
| Bulwark Beacon | `AQUATIC_ADAPTED` | standard/ward anchors to nearest valid solid surface; cannot float in empty midwater unless final accepted prop explicitly supports it |
| Citadel Field | `AQUATIC_ADAPTED` | anchored bounded field/volume around the beacon/valid surface |
| Citadel | `AQUATIC_ADAPTED` | anchored 7-block defensive volume; placement must have valid world anchor |
| Interdict | `AQUATIC_ADAPTED` | same perfect-guard/counter logic with water guard overlay |
| Lockdown Strike | `AQUATIC_ADAPTED` | compact 3D strike, attack-recovery timing unchanged |
| No Passage | `AQUATIC_ADAPTED` | guard window buff unchanged; retaliation wave becomes frontal pressure arc matching the real 4-block region |

---

# 10. Environment-dependent rules

## 10.1 Surface anchoring

For adapted skills that normally create ground objects/decals:

```text
find valid solid surface inside authored placement tolerance
→ orient effect to surface normal
→ server validates placement
→ render VFX/prop from accepted binding
```

If no valid surface exists, the skill uses its documented bounded-volume form where this file allows one. It does not place an invisible floor beneath the player.

## 10.2 3D target volumes

A 3D adaptation does not automatically enlarge a skill.

Example:

- land 5-block-radius field → aquatic bounded sphere/cylinder whose effective reach is still approximately 5 blocks;
- frontal 4.5-block burst → aimed 4.5-block cone/sector, not full sphere;
- projectile skills remain projectile skills.

## 10.3 Surface transition

Crossing water surface must not duplicate casts, reset cooldowns, cancel costs after resolution or replay hits.

Server action ID remains one action across:

```text
land → water
water → surface
surface → deep-water state
```

Presentation may cross-fade from swim overlay to land animation only after authoritative movement state changes.

---

# 11. Multiplayer / authority

All existing multiplayer rules remain intact.

Server owns:

- aquatic/land action state;
- accepted skill ID and fallback variant ID;
- Mana/Stamina/Ultimate cost;
- cooldown;
- displacement;
- dodge i-frame window;
- hitbox/projectile spawn;
- damage/heal/barrier/status/poise result;
- revive/down state;
- mount seat/controller ownership.

Client owns/predicts only safe presentation.

A client claiming `I was underwater so use Pressure Shatter instead` is not authoritative; server derives the valid variant from world/action state.

---

# 12. Data contract

Every frequent action/skill receives these fields or equivalent data-owned values:

```text
aquatic_mode: NATIVE | ADAPTED | DISABLED_WITH_FALLBACK
aquatic_variant_id: optional
aquatic_animation_profile
aquatic_hitbox_profile
aquatic_movement_profile
aquatic_targeting_profile
aquatic_surface_anchor_rule
aquatic_vfx_binding
aquatic_sfx_binding
```

Fallback skills are not additional progression records. They are implementation variants owned by the parent learned skill.

Example:

```text
skill: warrior_earthshatter
aquatic_mode: DISABLED_WITH_FALLBACK
aquatic_variant_id: warrior_pressure_shatter
shares:
  cooldown
  ultimate_cost
  total_action_coefficient
  poise_budget
  class_progression_ownership
```

---

# 13. Production acceptance gate

The former R11 design-compatibility blocker is considered **closed** by this matrix.

Implementation still must prove the presentation with actual runtime assets before R11 is called play-ready:

1. `Swim_Idle_Loop` and `Swim_Fwd_Loop` retarget correctly to the final player rig;
2. sword/shield/2H/bow/spell upper-body overlays do not reintroduce standing legs or extreme torso twisting;
3. Swim Burst displacement and body motion agree visually;
4. 3D aim cannot shoot backward through the player's body or through walls;
5. land → water transitions do not duplicate hits/casts;
6. heavy weapon reach matches visible movement;
7. adapted ground fields have honest surface/volume visuals;
8. Earthshatter/Skyfall automatic aquatic fallbacks preserve the same progression/resource identity;
9. 1–4 player encounters remain readable with overlapping aquatic VFX;
10. Abyss Fang telegraphs remain visible against deep-water fog/lighting;
11. real multiplayer verification is still required before `MULTIPLAYER TESTED`.

Failure of a particular clip at runtime means **replace or retarget that clip**. It does not reopen the class progression or add a second underwater skill system.

---

# 14. Closure result

R11 now has a complete frequent-action classification and a concrete animation/adaptation strategy:

```text
locomotion: closed
dodge/defense: closed
basic melee/ranged/magic: closed
five root classes: closed
first specialization actions: closed
ground-dependent skill conversion: closed
fallback ownership: closed
server authority: closed
```

Remaining R11 work belongs to the broader project gates:

- exact imported asset/archive/hash/provenance records;
- exact boss/creature/harbor/Deep-Dive Harness visuals where still open;
- Azari coordinates and actual underwater sightline/travel-density validation;
- runtime retarget/render/playtest proof.

Those are not unresolved aquatic combat-design choices.