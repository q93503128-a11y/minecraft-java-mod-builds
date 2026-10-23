# Augmentation Survival — Augment Compatibility / Synergy Audit

Date: 2026-10-23
Status: DESIGN AUDIT — no runtime augment implementation yet
Canon reviewed: `AUGMENTS.md` at 124 confirmed augments

## 1. Audit result

- Confirmed roster exact/semantic duplicate requiring immediate deletion: **0**.
- Several augments intentionally share the same stat/event axis but have distinct triggers or roles. These are not duplicates.
- The main risk is not duplicate names/effects, but **event ordering and provenance** when several augments react to the same attack, heal, death, drop, projectile, cooldown, or companion action.
- Current source tree is still bootstrap-level and does not contain the 124 augment runtime implementations. Therefore this is a **design compatibility audit**, not a runtime/playtest result.
- `MULTIPLAYER TESTED: NO` remains unchanged.

## 2. Intentional overlap clusters — keep

These groups look similar at first glance but are mechanically distinct and may coexist.

### Attack speed / tempo
- `속공`: permanent base attack-speed growth.
- `연속 처치`: short kill-stack movement/attack-speed growth.
- `광폭화`: timed active burst with incoming-damage drawback.
- `시간 가속`: broad timed action-speed state.

Not duplicates. They require one shared attack-speed stacking rule.

### Healing / sustain
- `회복력`: natural regeneration frequency.
- `회복 증폭`: non-natural healing amount.
- `치유 파동`: active direct heal.
- `회복 나눔`: copies actual healing to allies.
- `넘치는 회복`: converts eligible overheal into absorption.

Not duplicates. They form a coherent healing build, but need an explicit heal pipeline.

### Death / survival
- `한 번 버티기`: lethal-hit prevention at 1 HP.
- `비상 토템`: allows inventory totem consumption.
- `재림`: automatic self revival after other prevention fails.
- `소지품 보존` / `경험 보존`: actual-death loss rules.
- `소생`: revives another dead ally/companion.
- `사망 지점 귀환`: post-respawn return to the last actual death point.

Not duplicates. They require a strict lethal/death ordering contract.

### Projectile / ranged multi-hit
- `관통 화살`: one projectile passes through extra targets.
- `연쇄 화살`: hit creates non-projectile chain damage to new targets.
- `분산 사격`: one shot creates extra physical projectiles.
- `벼락`: root direct-hit counter that creates chain lightning.
- `집중 조준`: conditional damage multiplier before the above effects.

Not duplicates. This is the largest finite-multiplication risk and needs provenance tags.

### Mining
- `광부의 손`: general mining speed.
- `야행성`: conditional night mining speed.
- `연속 채굴`: action-chain mining speed.
- `굴파기`: depth secondary mining.
- `나무 베기`: connected-log secondary mining.
- `순간 채굴`: mythic one-tick root mining.

Not duplicates. Block-category ownership and secondary-action inheritance must be explicit.

### Movement
`빠른 발`, `숨 고르기`, `야행성`, `햇빛 체질`, `다급한 발걸음`, `연속 처치`, `관성 전환`, `시간 가속` all touch movement speed under different conditions. This is intentional but needs one stat stacking formula.

### Teleport / traversal
`귀환`, `사망 지점 귀환`, `자리 바꾸기`, `위상 이동`, `공간 연결` all move entities but solve different problems. No semantic duplicate found.

### Status defense
`강인한 정신` reduces duration, `정화` actively removes effects, `상태 반사` sends hostile effects back, and `무적` rejects new removable harmful effects during its active window. Distinct roles.

### Projectile defense
`돌려보내기` is deliberate shield reflection. `탄도 장막` is passive periodic hostile-projectile destruction. Distinct roles, but projectile ownership/provenance must be preserved so reflected friendly projectiles are not destroyed.

## 3. Data integrity issue — kill-heal mismatch

The user stated that a kill-triggered heal already exists and rejected `사냥의 활력` as a duplicate.

Current canonical `AUGMENTS.md`, however, does **not** contain a confirmed kill-heal augment. It contains `흡혈` only under unresolved/held history (`적대몹 처치 시 체력 1 / 2 / 3 회복`). The available proposal history and prior conversation search did not reveal an explicit approval of that `흡혈` proposal.

Therefore there is a canon/history mismatch:
- User expectation: kill-heal already exists.
- Current GitHub canon: no confirmed kill-heal.

Do not silently invent the missing canon. Before implementation, either restore the intended earlier kill-heal or explicitly promote/redefine the held `흡혈` entry.

## 4. Required event provenance contract

Every combat/resource event should carry a source/provenance classification. Without this, cross-augment procs will accidentally recurse or trigger rewards from generated damage.

Recommended internal classes:

- `ROOT_MELEE`: player directly executed melee hit.
- `ROOT_PROJECTILE`: physical projectile directly launched by player.
- `DERIVED_PROJECTILE`: auxiliary projectile created by an augment.
- `DERIVED_CHAIN`: chain damage such as `연쇄 화살` / `벼락`.
- `DERIVED_AOE`: `연쇄 폭발` and similar generated area damage.
- `ECHO`: `메아리 타격` extra strike.
- `ACTIVE_SPECIAL`: `방패 던지기`, `천벌`, etc.
- `COMPANION`: `분신`, `지배` or another owned combat ally.
- `REFLECTED`: projectile reflected by `돌려보내기`.
- `SPECIAL_PERCENT_HP`: percentage-max-HP effects such as `천벌`.

Default principle from existing canon remains: cross-augment synergy is allowed, but same-effect recursion and reward/progression loops are blocked.

Important concrete results:
- `벼락`, `메아리 타격`, `약점 표식`, `전투 순환` should count **root direct hits/kills**, not their own generated damage.
- `분신` / `지배` kills must not become player-direct kills for `풍요`, `전투 순환` or progression/core rewards.
- `돌려보내기` changes projectile ownership, but launch-time augments such as `분산 사격` must not retroactively fire from the reflected projectile.
- `연쇄 폭발` may intentionally chain from deaths caused by a previous `연쇄 폭발`; same-entity duplicate death processing remains prohibited.

## 5. Projectile combination audit

The confirmed projectile build can become extremely large but remains finite if the graph is defined correctly.

Potential stack:
`분산 사격` → each physical projectile can `관통` → impacts may cause `연쇄 화살` → root direct impacts advance `벼락`.

Rules needed:
1. `분산 사격` may create auxiliary projectiles only once per shot action; an auxiliary projectile never creates more auxiliary projectiles.
2. `연쇄 화살` chain damage is not a new physical projectile and therefore cannot pierce or split.
3. `벼락` chain damage never advances the `벼락` counter.
4. Crossbow vanilla Multishot must be treated as one **shot action**. `분산 사격` adds its configured auxiliary count once to that action, not once per pre-existing Multishot projectile, unless explicitly redesigned later.
5. Existing Piercing enchantment and `관통 화살` should use one combined pierce budget rather than separate recursive passes.
6. `화살 아끼기` performs one ammo-save decision for the root shot; generated auxiliary projectiles never consume additional ammo.

Open balance/performance question: whether every pierced impact is allowed to start its own `연쇄 화살` sequence. Allowing it creates very high but finite fan-out. This is not a correctness bug, but it should be decided before implementation/playtest.

## 6. Healing pipeline audit

Recommended deterministic order:

1. A primary eligible heal is created.
2. The healed target's `회복 증폭` modifies the heal amount.
3. Actual missing HP is restored.
4. Eligible excess from that **primary** heal is converted by `넘치는 회복` up to its cap.
5. `회복 나눔` uses only the actual HP restored by the primary heal, not absorption generated from overheal.
6. Shared healing is tagged `SHARED_HEAL` and cannot trigger another `회복 나눔` or create `넘치는 회복` absorption.

This keeps `치유 파동 + 회복 증폭 + 넘치는 회복 + 회복 나눔` strongly synergistic without infinite healing recursion.

Group scaling note: if several players each own `회복 나눔`, one `치유 파동` can make each player's primary heal generate one non-recursive share wave. This can be very strong but remains finite and is consistent with the current synergy-first philosophy.

Undefined case to resolve: `물약 확산` describes duration sharing, so instant beneficial effects such as Instant Health have no defined duration fraction. It needs either an explicit exclusion or a separate instant-effect amount rule.

## 7. Incoming damage / lethal pipeline audit

The existing canon already requires `한 번 버티기` before totem use and `재림` after other death prevention. Recommended full state order:

1. Hard immunity/block state (`무적`, successful shield block/reflection where applicable).
2. Vanilla armor/toughness.
3. Protection enchantments and normal vanilla mitigation.
4. General augment damage reduction (`질긴 피부`).
5. Conditional/type augment reduction (`피해 적응`, etc.).
6. `피해 지연` removes its reserved percentage from the immediate final damage.
7. `보호막` absorbs damage before normal player HP layers.
8. Normal absorption/health loss.
9. If lethal: `한 번 버티기`.
10. If still lethal: hand/offhand Totem of Undying, then inventory totem through `비상 토템`.
11. If still lethal: `재림`.
12. Only after all above fail does an **actual death** occur.
13. Actual death then applies `소지품 보존`, `경험 보존`, death-point recording and possible ally `소생` marker creation.

Consequences:
- `재림` is not an actual death; it must not create a new death point, drops, XP loss, or `소생` marker.
- `사망 지점 귀환` updates only after actual death.
- Damage fully cancelled by `무적` must not trigger `피해 적응` / `다급한 발걸음` as a successful received-damage event.

Strong but finite synergy to keep unless explicitly changed: a scheduled `피해 지연` installment that arrives during `무적` can be blocked by the immunity window. This makes the two augments synergize rather than treating deferred damage as unavoidable true debt.

## 8. Status-effect pipeline audit

Recommended order for a removable hostile effect from an enemy:

1. Record original incoming effect/amplifier/duration and source.
2. If `무적` window rejects the effect, self application fails.
3. Otherwise `상태 반사` may create a separate reflected copy based on the **original incoming duration**, not the already shortened self duration.
4. `강인한 정신` reduces the duration applied to the player by 20% / 40% / 60%.
5. `정화` can later remove the remaining eligible effects.

Open choice: whether an effect attempt blocked by `무적` should still be reflected by `상태 반사`. Current wording most naturally means no reflection because no effect was actually applied.

## 9. Cooldown / charge pipeline audit

Recommended order:

1. Ability starts from its authored base cooldown.
2. `능력 가속` applies its 25% reduction when the recharge timer is created.
3. `이중 충전` stores up to two charges for eligible non-mythic cooldown actives.
4. `전투 순환` subtracts time from currently running eligible recharge timers after a direct kill.

Explicit exclusions already remain correct:
- no-cooldown abilities (`귀환`, `지배`) are unaffected;
- toggle-only abilities are unaffected;
- per-death resources (`사망 지점 귀환`) are unaffected;
- mythic actives are unaffected by `능력 가속` / `이중 충전`.

Open rule: whether two spent `이중 충전` charges recharge sequentially or each owns an independent timer. This should be fixed before HUD implementation because it materially changes power and display behavior.

## 10. Resource / mining pipeline audit

Recommended resource order:

`base vanilla/mod drop` → `Fortune/Looting` → compatible additive/duplicate augments (`한 조각 더`, `풍작`, `손재주`, `전리품 챙기기`, etc.) → final eligible stack → `풍요 ×2`.

This matches the already confirmed `풍요` rule.

Secondary mining/harvest:
- each automatically harvested/mined block may use compatible normal drop/Fortune/resource/durability rules;
- secondary blocks do **not** recursively start another area/chain selector;
- `연속 채굴` counts the player root mining action, not every automatic secondary block;
- `순간 채굴` changes break time of the directly targeted root block and does not bypass harvest/tool requirements.

Block-domain rule needed:
- `굴파기` should use explicit stone/soil/general-dig tags and exclude logs, ores and crops;
- `나무 베기` exclusively owns connected-log expansion.
This prevents the same log break from accidentally entering both chain algorithms.

`임시 발판 + 관성 전환` is a valid positive synergy: landing on the temporary platform may deal 0 fall damage while the 4+ block landing still qualifies for `관성 전환` movement speed.

## 11. Companion / ally compatibility

A unified ally classification is needed so support augments do not all implement their own definitions.

Recommended categories:
- allied player;
- tamed permanent combat companion;
- `지배` permanent dominated companion;
- temporary summon (`분신`).

Recommended default:
- `치유 파동`, `회복 나눔`, `물약 확산`, `약점 표식` ally benefits include tamed and dominated companions.
- `분신` may receive temporary heal/buff effects but never reward/progression attribution.
- `소생` may revive permanent tamed/dominated companions, but should not revive the temporary `분신`.
- once dominated, a mob is no longer a hostile target for `위험 감지`, `천벌`, `끌어당기기`, hostile projectile filters, etc.

External mod mobs may use custom AI ownership/target code. `지배` therefore needs per-namespace/entity compatibility exclusions or adapters instead of blindly rewriting every hostile mob AI.

## 12. Boss-control compatibility

The current roster intentionally allows strong boss interaction: `우두머리 사냥`, `끌어당기기`, `자리 바꾸기`, `시간 정지`, `천벌` stun/percent damage. This must remain server-authoritative but external scripted bosses need a unified compatibility profile.

Recommended data-driven boss control fields:
- `isBoss`
- `allowPull` / pull multiplier
- `allowPositionSwap`
- `allowTimeStop`
- `allowStun`
- `allowPercentHpDamage`
- `allowDominate` (default false)

Default design can remain permissive where already approved, with explicit exceptions for scripted entities whose phase logic would break.

`천벌` must apply its boss 10% / 25% value through the normal damage/hurt path, not directly `setHealth`, so invulnerability phases and health-threshold scripts can observe or reject the hit.

Recommended special-percent rule: `천벌` percentage damage should not be increased by generic attacker damage bonuses such as `날 선 감각`, `광폭화`, `우두머리 사냥` or `약점 표식`; otherwise the displayed 60/90 and 10/25 values stop being true. It should still respect the explicitly documented boss phase/damage-block rules.

Time interaction: while `시간 정지` freezes an entity, target-local stun/effect timers should also pause. Therefore a Lv.2 `천벌` stun applied during time stop should still have its 2 seconds available after time resumes rather than expiring invisibly during the freeze.

## 13. Better Combat compatibility

Official pack baseline pins Better Combat 3.2.2 for Minecraft 26.2, but current project docs already mark actual augment attack coexistence as **not playtested**.

Better Combat 26.2 weapon definitions own attack hitbox shape, range multiplier, angle, per-combo damage multiplier and attack timing/upswing. Attack animation playback is scaled to weapon attack cooldown. Therefore:

- `속공`, `연속 처치`, `광폭화`, `시간 가속` should modify one authoritative attack-speed/attack-cooldown path rather than creating independent animation speed hacks.
- `휩쓸기 강화` must not assume vanilla Sweeping Edge geometry. Under Better Combat it needs an adapter to the Better Combat target-selection/hitbox model or a post-selection expansion that preserves the weapon's attack shape.
- `벼락`, `메아리 타격`, `약점 표식` and direct-kill logic need a normalized server-side **root melee hit** signal that also fires for Better Combat attacks exactly once per target hit.
- Better Combat combo attacks may have their own damage multiplier. `방패 던지기` should use the canonical weapon snapshot contract rather than accidentally inheriting whatever combo frame happened to be active at cast time unless explicitly intended.

This integration requires gameplay testing; smoke boot success is not sufficient.

## 14. Farmer's Delight / modded crop compatibility

The official pack includes Farmer's Delight Refabricated. Vanilla-only crop hardcoding would leave several augments partially broken.

`풍작`, `씨앗 챙기기`, `다시 심기`, `싹쓸이 수확`, and `풍요` need crop/harvest adapters or tags that can represent:
- normal age-based crops;
- crops harvested by interaction rather than block destruction;
- multi-block or nonstandard crops;
- correct replant material.

Food-side augments (`빨리 먹기`, `든든한 한 끼`, `먹으면서 걷기`, `비축 식사`) should prefer normal item food/use components so mod foods work without per-item hardcoding.

## 15. Other external compatibility notes

- `광석 눈썰미` should use ore tags/compat sets rather than a vanilla ore ID list.
- `손재주` needs a data-driven whitelist/tag set for modded shear/harvest resources.
- `쌍둥이`, `길들이기 요령`, `기수` should use vanilla hooks where possible and explicit adapters for Naturalist/custom mobs that bypass vanilla breeding/taming/riding paths.
- `마법 공명` must not blindly overlevel every external enchantment. Use an allow/compat contract for enchantments whose effect actually scales safely above their normal max level.
- `소지품 보존` already correctly limits external accessory slots to explicit compatibility.

## 16. Highest-priority unresolved decisions before implementation

P0 design contracts:
1. Shared stat stacking formula for multiple `+X%` attack/move/mining/use-speed effects.
2. Root-vs-derived combat event provenance contract.
3. Projectile fan-out rule: whether every pierced impact may start its own `연쇄 화살` sequence.
4. `물약 확산` handling of instant beneficial potions.
5. `이중 충전` recharge model: sequential vs independent timers.
6. Unified companion classification, especially `지배` and `소생`.
7. Special percentage damage interaction with generic damage buffs (`천벌`).
8. Boss control compatibility profiles for `자리 바꾸기`, `시간 정지`, `천벌` and external scripted bosses.
9. Resolve the canon mismatch around the supposedly existing kill-heal augment.

## 17. Verification state

- CURRENT CANON REVIEWED: YES — 124 confirmed augments
- SEMANTIC DUPLICATE AUDIT: YES — no confirmed duplicate requiring removal found
- DESIGN INTERACTION AUDIT: YES
- HISTORICAL KILL-HEAL CONSISTENCY: MISMATCH FOUND
- RUNTIME AUGMENT IMPLEMENTATION REVIEWED: NOT APPLICABLE — not implemented yet
- BETTER COMBAT DESIGN COMPAT REVIEWED: YES
- BETTER COMBAT GAMEPLAY TESTED: NO
- EXTERNAL BOSS GAMEPLAY TESTED: NO
- MULTIPLAYER TESTED: NO
- BUILD/CI: NOT RUN — docs/audit only
