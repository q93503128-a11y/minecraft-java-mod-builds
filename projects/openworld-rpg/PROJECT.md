# Open-World RPG — Project Contract

> Working project slug: `openworld-rpg`  
> Player-facing title: **WORKING / CANDIDATE ONLY — not yet locked, and NOT a gameplay-source-bootstrap gate; `Anchorwake` is an evaluated candidate only**  
> Current phase: **M0 GAMEPLAY DEPENDENCY RUNTIME BOOT VERIFIED / R01 GAMEPLAY + CONTENT DESIGN CLOSED / PLAYER-FACING R01 IMPLEMENTATION STILL BLOCKED BY ASSET + AZARI SPATIAL BINDING; LATER-REGION FINAL-ENCOUNTER GATES REMAIN**

## 1. Repository / authority contract

This project follows, in order:

1. current GitHub `main`;
2. root `AGENTS.md`;
3. `docs/BUILD_STANDARD.md`;
4. `docs/QUALITY_STANDARD.md`;
5. this `PROJECT.md` for technical/project contracts;
6. `GAME_DESIGN.md` for gameplay/design master canon;
7. explicitly indexed later refinement/content documents;
8. historical audits and old chats only as evidence/history.

When a current canon decision changes, update or replace stale live wording. Git history is the archive. Do not preserve contradictory rules as if both were valid options.

`DESIGN_COMPLETENESS_AUDIT.md` is the design-quality audit that found several historical conflicts, but its blocker list may lag behind later closure work. **The current pre-code gate list in §7 of this file is authoritative when a later dedicated canon has already closed an older audit blocker.**

### 1.1 Git workflow

Routine project work is **direct `main` only**.

- do not create temporary branches;
- do not create feature branches;
- do not create PR branches merely to perform normal project work;
- do not force-push;
- update `main` only by normal fast-forward commits on top of the current remote `main`;
- never reset or rewrite unrelated projects in this shared repository;
- branch creation requires an explicit user request for that specific branch/workflow.

A tool offering `create_branch` is not permission to use it automatically.

---

## 2. Technical identity

- Minecraft Java: **26.2**
- Java: **25**
- Loader: **Fabric — locked for this existing project**
- Fabric Loader: **0.19.5**
- Fabric API: **0.160.0+26.2**
- Gradle: **9.5.1**
- Fabric Loom: **1.17.20**
- build plugin: `net.fabricmc.fabric-loom`
- mod id / namespace: `openworld_rpg`
- initial internal artifact version: `0.1.0-alpha.1`
- initial JAR basename: `openworld-rpg-0.1.0-alpha.1.jar`
- M0 core bootstrap source exists and remains available through the explicit `core` isolation profile.
- 10 foundation/safety JARs + 3 curated creature JARs are pinned to exact admitted versions/bytes and co-load in the `gameplay` profile. Historical Openworld verification checkpoint `36514543682` at code state `8d5233c3358a153132ec69e0d4366415c6ca7937` passed unit tests, clean build/JAR, core dedicated-server boot, gameplay dedicated-server boot, gameplay-client startup smoke and the R01 integration playtest-JAR build. The verified R01 authority layer now additionally includes persistent personal active-world time, semantic server-only world-action bridges for Lost Cargo / Meadow Viper contribution / broken road marker / valid R01 gathering / Roadside Trouble, persistent personal R01 gathering-node state with Field-tool validation, canon-locked yield/cooldown/timing metadata, mastery/discovery progression, full-amount Material Pouch delivery and reconnect-safe harvest receipts, server-owned Recovery Belt use timing with 0.72 s resolution / 0.95 s action / 6.0 s shared lockout, exact Healing Potion / Focus Draught / Cleansing Tonic effect resolution, pre-resolution poise-break cancellation, Focus 3.0 s Mana tail, and the corrected 10 s Cleansing buildup-resistance window, persistent shared Roadside Trouble cycle/lifecycle state, exact 4-minute first eligibility / 12-minute repeat delay / 120-second abandon / 2→4 threat scaling rules, reconnect-safe repeat rewards at 5% combat EXP + 4% Class XP + 20 Gold with the contribution-time class preserved, bounded repeat-reward receipts, bounded `Dust on the Quarry Road` class-attribution evidence so last-second class switching cannot steal the quest Class XP, and the reconnect-safe Earthloong first-clear reward transaction backend: a permanently committed 1-of-3 Superior Item Lv8 choice (Ironroot Longsword / Riverthorn Bow / Lumenwood Staff), deterministic 85th-percentile affix payloads, persistent equipment grade, Backpack → Personal Storage → Pending Reward Claim item delivery, plus two Earthloong Scales with a bounded lossless Material Pouch receipt and retained capacity-blocked claim. Earthloong canonical project-HP death now resolves the shared encounter, persists every server-accepted first-action contributor with the class active on that qualifying contribution, moves eligible players into reconnect-safe pending finalization, grants the first boss reward layer idempotently, and commits personal first-clear state only for a player who actually has an active personal quarry run. A helper without that personal quest state still receives only their eligible combat reward and cannot inherit story completion. At that historical checkpoint, final production promotion of the current Azari spatial candidates, Meadow Viper asset/entity binding, concrete support/heal/barrier/control/revive encounter participation, Lucifer reward-choice UI and external reward item visuals still remained open. R01 ordinary equipment base identities, generic category-driven affix eligibility and parameterized matching weapon-family power were bound and verified; Movement Speed was live through a server-owned Minecraft movement-attribute projection with the canonical +15% equipment cap. That audit hardening checkpoint also made pre-activation gathering independent from Dust quest credit, prevents stale post-completion Dust attribution, snapshots Bow/Crossbow combat builds at projectile launch, and persists Mana/Stamina, committed skill cooldowns, player poise, Shock/Conductive, authored negative statuses and Cleansing buildup-resistance state across reconnect; elapsed server time still advances poise recovery, Shock decay and status expiry rather than freezing them. At that historical checkpoint, Attack Speed, dodge/sprint Stamina-cost reduction, Healing Received, Healing Done and Ultimate charge gain were live runtime consumers and the static ordinary-affix catalog stood at 26/29; later §7 closure work supersedes that snapshot and records 29/29 runtime implementation. Healing Done uses the canonical server-owned skill-heal order `HealingReference × HealCoefficient × (1 + Healing Done) × (1 + target Healing Received)`. Cleric Mend is now the first production healing-skill caller bound through Spell Engine: its canonical 22 Mana / 8.0 s cooldown / 0.45 s cast / 12-block self-capable targeting contract uses a donor-neutral `CUSTOM/HELPFUL` impact, class-gates Cleric authority before project resource commit, and routes final HP exclusively through `ProjectHealingRuntime` with HealCoefficient 0.30. This closes the production caller/authority boundary; the later project-owned class-skill slot/input binding now exposes Mend through Cleric Active Skill 2 / G. Final HUD/icon and joined-player feel acceptance remain separate presentation work. The Earthloong healing bridge still records support participation only after another player actually recovers HP, but Mend deliberately does not auto-attribute that participation yet because current threat membership and candidate arena volumes are not a closed active-engagement contract; support attribution stays fail-closed rather than crediting stale/remote players. At that historical checkpoint Nessa materialization was still fail-closed on three `stored_only` identities; later §7 closure work supersedes that snapshot, closes the ordinary-affix runtime gate at 29/29 and records the current Nessa purchase/material-sale authority.
- Project-owned root-class skill access is bound through canonical five-slot class assignments: Mage Arc Bolt, Phase Step, Frost Ring and Flame Burst occupy all four ordinary Active Skill slots; Cleric Radiant Lance, Mend and Rebuke occupy Active Skills 1, 2 and 4, and Sanctuary occupies the Ultimate / Y slot; class changes/rejoin/respawn refresh the published slots, and the client exposes the canonical F/G/V/X/Y project actions while Spell Engine remains the casting/targeting/delivery backend. Spell Engine's donor hotbar input and donor spell-hotbar rendering are suppressed so they do not become a second control/progression surface. Build Openworld RPG run `36518589216` at code state `9e90acc01b1c95e9d00129b6b7da46a47bba0477` passes unit tests/build, core server, gameplay dependency server, gameplay client startup, both playtest-JAR builds and pack packaging. This is startup/build verification, not manual joined-player input/feel proof; final Lucifer skill HUD/icons, the still-unimplemented class slots, first-run control-preset conflict handling and full R01 playtest remain open.
- Cleric Grace root-mechanic runtime is now server-owned for its three canonical gain channels: damaging Cleric active hit (+1, 1.0 s ICD), effective heal restoring at least 6% recipient MaxHP (+1, per-recipient 2.0 s ICD), and hostile-consumed barrier of at least 6% recipient MaxHP (+1, per-recipient 2.0 s ICD). Grace caps at 3, remains alive while project combat activity continues, expires after 10 s outside combat, resets on an actual root-class switch, and a Grace-spender consumes all three pips. Mend is the first bound spender: ordinary Mend remains HealCoefficient 0.30; three-Grace Mend resolves at 0.40 and removes exactly one project-tagged `minor_dispellable` negative status. Full-health/no-op healing cannot generate a Grace pip. Cleansing Tonic retains its separate all-minor-dispellable cleanse behavior. Build Openworld RPG run `36519509967` at code state `1bd2c159bdf417ac2cf581635b3a9476b8dbdbf6` passes unit tests/build, core server, gameplay dependency server, gameplay client startup, both playtest-JAR builds and pack packaging. Grace HUD presentation, Cleric Active Skill 3 / ultimate, manual joined-player Grace/Mend feel and remaining presentation acceptance remain open.
- Project Barrier authority is now unified and server-owned. Skill barriers use the canonical `BarrierReference = BaseHP(casterLv) × AttributeDamageMultiplier(0.65 WIL + 0.35 END)` and `BarrierAmount = BarrierReference × BarrierCoefficient × (1 + applicable output bonus)`; the shared runtime enforces the default 6.0 s duration contract, same-source replace/refresh semantics and the 40% recipient-MaxHP cap across different sources. Hostile project damage consumes the shared barrier pool only after Defense/MR + dodge/guard resolution and before HP damage. Cleric-authored barrier layers accumulate actual hostile consumption and emit at most one Grace-qualification event after 6% recipient MaxHP has really been absorbed, still respecting the existing per-recipient Grace ICD. MaxHP decreases trim an over-cap live pool rather than allowing a temporary cap exploit. Earthscale Ward / Earthen Reprieve now grants its canonical 8% MaxHP, 4.0 s barrier through this same shared authority instead of owning a second uncapped barrier implementation; its 10.0 s trigger ICD remains item-owned. Build Openworld RPG run `36520872713` at code state `691b5dac91168c73fc721a2e3046af72b6ce2887` passes unit tests/build, core server, gameplay dependency server, gameplay client startup, both playtest-JAR builds and pack packaging. Barrier HUD/VFX, encounter-linked barrier threat/support attribution and the authored Cleric/Guardian skills that create additional barriers remain open.
- Cleric root combat now has a second production active and its alternating passive bound end-to-end. `Radiant Lance` occupies Cleric Active Skill 1 / F and owns the canonical 14 Mana, 4.0 s cooldown, 20-block range, 1.35 magic ActionCoefficient and 0.60 PoiseCoefficient. Grace/Doctrine are snapshotted exactly once at server-accepted cast time so projectile flight cannot retroactively change a cast. A three-Grace Lance keeps the 1.35 primary hit, chains to at most two additional project-owned hostiles within 4 blocks for 0.55 each, or if no chain target exists heals the lowest-health valid nearby player/self within 8 blocks at HealCoefficient 0.08. The primary damaging hit earns at most the normal damaging-active Grace event; the fallback heal independently follows the existing effective-heal threshold/ICD. `Balanced Doctrine` is server-owned as a single alternating 6.0 s prime: damaging Cleric active -> next healing/protection active +10% output; healing/protection active -> next damaging active +8% direct damage. Mend now consumes its accepted-cast Doctrine snapshot rather than reading combat state at impact. Radiant Lance presentation is no longer a particle-only placeholder: its projectile body uses the exact 73-vertex / 52-triangle KayKit Character Pack: Adventures `arrow.gltf` geometry from upstream commit `672074b73ba276876a19e8816ecdc5241817ab47`, with the original `rogue_texture.png` bytes and recorded CC0 license/source identities under `external-assets/kaykit-adventures/`. Only coordinate-axis remapping, runtime scale/lighting and a warm radiant tint are applied; the visible ~1.16-block body is paired with a 0.22 x 0.22 x 1.15 Spell Engine projectile OBB. Restrained particles are support/readability layers only. Build Openworld RPG run `36524067397` at code state `cbe9b6dab44db71c542507fc4403bbaae0d30288` passes unit tests/build, core server, gameplay dependency server, gameplay client startup, both playtest-JAR builds and pack packaging. This proves compile/startup/mixin loading, not an in-world screenshot or joined-player visual/feel pass; Radiant Lance manual playtest, final skill icon/HUD and final SFX audition remain open.
- Cleric `Rebuke` is now production-bound as Active Skill 4 / X: 24 Mana, 10.0 s cooldown, 4.5-block frontal holy burst, 1.80 magic ActionCoefficient and 1.60 PoiseCoefficient. A three-Grace cast keeps 1.80 damage, raises PoiseCoefficient to 2.20 and extends Rebuked by 1.0 s. Rebuked is a server-owned, non-stacking hostile state: normal/elite direct outgoing damage is multiplied by 0.85 for 80 ticks; miniboss/boss direct outgoing damage is multiplied by 0.92 for 60 ticks; empowered duration adds 20 ticks. Reapplication never adds another reduction; while active it preserves the stronger reduction and later expiry so a normal reapply cannot shorten an empowered state. The multiplier is applied at the shared project-owned hostile -> player incoming-damage boundary before player Defense/MR/active defense resolution, and changes only raw direct HP damage: guard pressure, dodge/guard flags, poise pressure and separate status mechanics are not weakened by Rebuked. External actor profiles now carry an explicit `NORMAL_ELITE` versus `MINIBOSS_BOSS` combat rank; R01 Earthloong is explicitly boss-ranked instead of inferring rank from donor identity. Rebuke target resolution is fully server-side at cast completion; Spell Engine supplies input/animation/VFX only and does not submit an authoritative target list. The canonical 4.5-block reach is paired with an initial playtest-tunable fan binding (0.35-block near half-width -> 1.90-block far half-width plus vertical overlap/line-of-sight); the same `RebukeBurstShape` constants drive server hit acceptance and client fan geometry so presentation and collision cannot drift independently. Presentation uses Spell Engine `one_handed_area_release_air_wave` plus an authored short-lived 3D fan textured with the exact Kenney Particle Pack `light_03.png` and `magic_03.png` bytes already shortlisted by Phase F; source/provenance is recorded under `external-assets/kenney-particle-pack/SOURCE.md`. Generic sparks remain a subordinate directional readability layer, not the attack body. The first CI run `36526114886` exposed one Java overload-inference compile error in deterministic target sorting; code state `4cb90c96358bdb77f7df151ab5232872830e4e6b` fixes only that method reference. Final Build Openworld RPG run `36526372157` passes unit tests/build, bootstrap JAR, core server, gameplay dependency server, gameplay client startup including the SpellModelEffect renderer mixin, both playtest-JAR builds and pack packaging. This is compile/startup verification, not an in-world screenshot or feel pass; Rebuke manual visual/range/width/impact-feel review, final skill icon/HUD and final SFX audition remain open.
- The shared Ultimate Gauge authority is now server-owned and reconnect-safe: charge is clamped to 0..100, successful Ultimate consumption resets it to 0 and starts the canonical 35.0 s lockout, charge may continue accumulating during lockout, class switching resets current charge, and a rest-reset API exists for future shrine/inn/camp interaction callers without pretending those world interactions are already bound. Stored charge begins decaying only after 45.0 s with no combat activity, then at 5 charge/s. Normal gain is subject to the global 12 charge/s authority cap. Combat-level anti-farm scaling uses the canonical Combat EXP encounter-level curve (120% / 110% / 100% / 75% / 40% / 10%). Equipment `Ultimate charge gain` is now a live percentage affix with its canonical +30% aggregate gear cap, raising the static ordinary-affix catalog to 26/29 implemented. Cleric production events currently publish the authored charge rules: a damaging active that successfully hits its eligible primary hostile gives +2; effective healing gives +2 per 5% recipient MaxHP restored, max +6 per cast/recipient; a Cleric barrier gives +2 at each actually consumed 5% recipient-MaxHP step, max +6 per barrier source/recipient; cleansing one meaningful negative status gives +3; overheal produces no healing charge. Barrier charge uses the actual project-owned hostile that consumed the barrier. Healing/cleanse support charge fails closed unless an admitted server-side encounter adapter proves a live engagement; R01 currently resolves that through Earthloong's live threat table, explicitly excluding verification fixtures, proximity-only players, formal party membership and stale reward eligibility. Ultimate Gauge/lockout state is persisted in combat-session schema v3 while older optional-field snapshots remain decodable. Build Openworld RPG run `36528913559` at code state `8baa6cf2a86d63203e43bd8e736c1a636a1b7041` passes unit tests/build, bootstrap JAR verification, core server, gameplay dependency server, gameplay client startup, both playtest-JAR builds and pack packaging. The gauge/resource authority remains shared across classes; Cleric Sanctuary now consumes this same authority rather than owning an alternate gauge. The Ultimate HUD is not implemented, the natural shrine/inn/camp rest caller is not implemented, and joined-player charge pacing is not playtested.
- Cleric `Consecrated Ground` is now source-bound to Active Skill 3 / V, completing the five-slot Cleric root loadout. The server owns the canonical 32 Mana / 14.0 s cooldown transaction, fixes the field at the accepted cast position for 5.0 s, and resolves five one-second pulses that preserve the locked total HealCoefficient 0.30 and enemy magic ActionCoefficient 1.35. Consecrated Ground is explicitly a healing/protection active for Balanced Doctrine: the accepted-cast +10% support snapshot affects healing and the empowered initial BarrierCoefficient 0.12, never the damage half. Three-Grace empowerment is consumed once at cast acceptance; the damage half can publish at most one damaging-active Grace/Ultimate event per cast while healing/barrier support continues through the shared per-recipient authority. The visible 5-block ward uses the existing admitted Kenney holy-support texture family through a dedicated 3D ground geometry tied to the same authoritative radius; final Minecraft visual/terrain/audio/feel acceptance remains pending. Build Openworld RPG run `36537843755` at code state `e5d5b4a782a577f35e429ce7425fd4b5785601ca` passes unit tests/build, core server, gameplay dependency server, gameplay client startup, both playtest-JAR builds and pack packaging. This is compile/startup verification, not an in-world Consecrated Ground playtest. The subsequent input/resource audit also restores the missing one-physical-press held-repeat gate and player-facing English/Korean name/description keys for Consecrated Ground.
- Warrior root combat is now source-bound across all five canonical slots: `Driving Slash` / F, `Iron Counter` / G, `Cyclone Cut` / V, `Breaker Slam` / X and `Earthshatter` / Y. Momentum is server-owned at 0..3 with the locked 0.80 s basic-cycle gain ICD, 7.0 s expiry, +5/+10/+15% poise-output tiers, full-Momentum spender consumption and once-per-cycle multi-target de-duplication. Warrior Ultimate gain now uses the shared authoritative gauge and global 12/s cap: basic cycle +2, active primary hit +3 once per cast, successful perfect guard/counter +5, eligible miniboss/boss poise break +10; Earthshatter itself does not publish the ordinary-active +3. Iron Counter's 18 Stamina is part of the same synchronized accepted-cast resource transaction as project Mana/cooldown authority. Breaker Slam/Earthshatter hyperarmor reduces incoming player-poise pressure by the locked x1.60/x2.00 effective-poise factors without changing HP mitigation. Five Spell Engine resources are donor-neutral and their project-authored 3D slash/sector presentation uses the exact pinned Kenney `slash_01.png` bytes only as support material; Driving/Breaker/Earthshatter range constants are shared with server hit geometry and line-of-sight remains authoritative. The next shared action/reaction binding now removes those code-level Warrior gaps without inventing unsupported enemy reactions: Combat Temper's locked 0.90 modifier is consumed by a common authored ordinary non-launch stagger path; guard break enters the same hard-reaction authority but is not shortened; Warrior action windows gate overlapping Better Combat basics, new guard starts and dodge admission; Cyclone owns the locked 0.75 movement multiplier and a 13-tick production envelope whose first legal quantized 72% dodge cancel is tick 10; its delayed second hit is clamped to a strictly earlier server tick so input order cannot erase the second hit; Driving Slash / Breaker Slam use their locked 70% / 82% cancel starts, while Iron Counter / Earthshatter remain committed through their authored stance/cast plus recovery windows. A hard reaction replaces the active action, so an interrupted Warrior cast cannot later release server damage and an interrupted Cyclone cannot emit its pending second hit. Empowered Cyclone's 0.6-block pull is now collision-resolved behind explicit external-actor reaction capability instead of inferring `normal` from the current `NORMAL_ELITE` rank; Earthloong remains non-displaceable. Current R01 data still does not author an exact ordinary player-stagger duration or a normal actor with accepted knockdown/launch animation, so those concrete caller/presentation bindings remain fail-closed. The canonical 2-tick dodge buffer exists at the shared server action layer, but the player-owned dodge input/network adapter is still not bound, so joined-player dodge buffering/feel is not claimed. Build Openworld RPG run `36566579937` at code state `fb63d70db9d7a1fbf23e8067e79dd24a499994ad` passes unit tests/build, bootstrap JAR verification, core-profile dedicated server smoke, gameplay dependency server smoke, gameplay client startup, both playtest-JAR builds, pack packaging and artifact upload. This verifies compile/startup/integration wiring, not joined-player dodge buffering, action-cancel feel, hostile knockdown/launch presentation or multiplayer. Build Openworld RPG run `36562682972` at code state `a5d50e7fe97e50b1cc6624fcd9fb27144ff5f782` passes unit tests/build, bootstrap JAR verification, core-profile dedicated server smoke, gameplay dependency server smoke, gameplay client startup, both playtest-JAR builds, pack packaging and artifact upload. This proves compile/startup/resource/mixin integration, not joined-player Warrior combat feel, hit-volume readability or multiplayer.
- Cleric root Ultimate `Sanctuary` is now source-bound to the canonical Ultimate / Y slot. Server preflight requires 100 Ultimate Gauge with no active 35.0 s lockout, and only the first accepted cast may spend the shared gauge; duplicate/re-entry callbacks cannot spend a second gauge. A successful cast creates a fixed ground ward at the accepted server position for 8.0 s with an exact 7.0-block horizontal radius. The initial `BarrierCoefficient 0.25` uses shared Barrier authority and ordinary 6.0 s barrier lifetime. Eight one-second server pulses partition the locked total `HealCoefficient 0.55` and total enemy magic `ActionCoefficient 2.00`, preserving those design totals rather than inventing a new per-tick budget. Effective Sanctuary healing publishes through the existing Grace and encounter-gated Ultimate support channels, while Sanctuary itself does not consume or prime Balanced Doctrine because that passive is defined for ordinary Cleric actives. Cleric barrier consumption keeps the shared actual-consumption Grace/Ultimate rules. Allies standing inside receive the locked 0.80 negative-status-duration multiplier only for newly applied authored negative statuses; existing statuses are not shortened retroactively, and the modifier is transient rather than persisted across disconnect. The initial production vertical envelope is -1.25 / +3.50 blocks around the cast origin and remains playtest-tunable. Presentation is not particle-only: Spell Engine owns release animation/effect transport, while a project-authored full-bright 3D ward uses the same 7.0-block radius constant as server targeting, with Kenney light/magic textures only as support layers. Healing Done is not reused as Barrier output; until a dedicated live barrier-output modifier is admitted, Sanctuary's initial barrier uses only its canonical coefficient. Build Openworld RPG run `36532672636` at code state `52884b22b1d59a06dc1c8c60209bd4a5895f6485` passes unit tests/build, bootstrap JAR verification, core server, gameplay dependency server, gameplay client startup including the Sanctuary model-effect renderer mixin, both playtest-JAR builds and pack packaging. This is compile/startup verification, not an in-world Sanctuary playtest; visual footprint/terrain readability, pulse feel, joined-player support behavior and multiplayer remain untested.
- Paid root-class switching now has a dedicated reconnect-safe server transaction boundary. The canonical Lv-scaled `round_to_10(min(2500, 50 + 15L + 0.4L²))` Gold cost is computed from the player's current combat Lv; the first root-class choice remains outside this path and stays free. A switch persists its source/target/cost before Gold debit, reuses one idempotent debit receipt across reconnect, then commits the target class through existing progression authority so class history/allocation is preserved while Grace/Doctrine/Ultimate state resets through the existing class-change hook. A stale pre-debit transaction is cancelled if another administrative path changed the source class; once Gold has been durably debited, reconnect completes the prepared target rather than losing paid currency. No settings-menu shortcut is exposed: the natural shrine/guild/class-service interaction and final UI remain world/presentation binding work. Build Openworld RPG run `36538652311` at code state `3874dba9facca23dfdd682709b56c0aa5c0971ad` passes unit tests/build, core server, gameplay dependency server, gameplay client startup, both playtest-JAR builds and pack packaging. This verifies the persistence/bootstrap path, not an in-world class-service/UI interaction.
- The automatic death penalty is now source-bound to the existing canon. Actual death respawn after the first Alderford shrine removes `min(current-Lv EXP, max(1, round(4% of EXP_to_next)))` when any current-Lv EXP exists; it never removes an earned Lv and does not add a Gold surcharge when the available current-Lv EXP is smaller than the target loss. With no removable current-Lv EXP, the server applies `round_to_10(min(1200, 30 + 6L + 0.12L²))` Gold and explicitly allows the balance to become negative; ordinary purchase/service debit still rejects insufficient Gold, so debt is not spending power and future income naturally pays it down. The pre-first-shrine opening remains penalty-free. A persistent pending death transaction records the exact before/after value before mutation and is reconciled on join; Gold fallback uses an idempotent debit receipt. Fabric `AFTER_RESPAWN` applies the penalty only when the old player is actually dead, not when a live player object is recreated. Build Openworld RPG run `36539562166` at code state `3a4fa5dc889be9a2514e849310ade9fefad1c58a` passes unit tests/build, bootstrap JAR verification, core-profile dedicated server smoke, gameplay dependency server smoke, gameplay client startup, both playtest-JAR builds, pack packaging and artifact upload. This verifies compile/startup/persistence wiring, not a joined-player death/economy feel playtest.
- Shared player Dodge/Roll is now wired end-to-end from the canonical remappable Q client input through a typed Fabric C2S payload into server-owned defense/action authority. The client sends only normalized directional intent and a monotonic sequence; the server alone owns 30 Stamina, the 12-tick Stamina regen delay, 6-tick i-frame, 9-tick action, 11-tick re-entry, action-cancel legality and collision-resolved movement. Directional dodge targets the locked 3.2-block level-ground distance over nine server steps; no-input uses an explicitly playtest-tunable 2.0-block backstep because canon specifies only a short backward evade. The existing canonical 2-tick dodge buffer is now consumed by the real request path without pre-spending Stamina, and a hard reaction/replacement action invalidates the pending buffer. Accepted dodge releases held guard and claims its own shared action window, preventing basic attack/project-skill/new-guard overlap. Server acceptance is then replicated to the dodging player and tracking clients for presentation. Player Animation Library drives an admitted MIT roll editable base from Kelvin285/Kevin Merrill, retimed from 0.375 s to the canonical 0.45 s and emitted as four directional variants; client input fails closed if the animation resource/API is unavailable rather than producing an invisible dash. The separate AcroWield 1.8.1-MC26.2 reference JAR was inspected at SHA-256 `6d0576405a380681b6e1d301a66fc4c58c4e3cce93779689ebf2c983590e2397` and contained dodge networking/code plus license material but no directly reusable dodge/roll animation resource under the inspected animation/resource names, so it remains reference-only and is not added as runtime dependency. Dedicated rooted/downed player states still do not exist; only the real shared hard-reaction mobility lock is claimed. Build Openworld RPG run `36648657612` at code state `717cddcc286e80c5bc7e2d26e8dc19274dfdcec4` passes unit tests/build, bootstrap JAR verification, core-profile dedicated server smoke, gameplay dependency server smoke, gameplay client startup, both playtest-JAR builds, pack packaging and artifact upload. This verifies the end-to-end C2S/server movement/i-frame/action-buffer/PAL startup integration, not joined-player camera/armor-clipping/terrain-shortening/latency feel or multiplayer.
- Hunter root-combat foundation is now server-owned without opening asset-gated visible skill slots. The first valid project-authorized Bow/Crossbow basic hit on a bound hostile marks or refreshes the current Quarry for the canonical 8.0 s; a new live target replaces the Quarry without inventing a Focus-clear rule that canon does not specify. Focus is clamped to 0..3, gains +1 on Quarry hits from >=7 blocks with the locked 0.75 s ICD, accepts an additional +1 only from an explicit authored weak-point fact with the locked 1.5 s ICD, loses exactly 1 on actual direct HP damage, and clears on Quarry expiry / the 8.0 s out-of-combat rule. A full three-Focus spender API is available for later Hunter actives. Ranged-shot context now snapshots launch position so range classification does not depend on the shooter's later movement or equipment state. The shared Ultimate authority publishes the canonical Hunter ranged-basic +2, >=7-block additional +1, explicit weak-point +3 with its independent 1.0 s ICD, and personally-caused ranged poise-break +6; the existing global 12/s cap still applies. At that code state, bound actors exposed no accepted anatomical weak-point contract, so the live Bow/Crossbow bridge deliberately passed weak-point=false instead of inferring anatomy from hitboxes. Hunter state resets on root-class change, death-respawn and disconnect. The 1.08 projectile-speed-vs-Quarry passive remains a separate passive/runtime binding rather than being faked through ordinary projectile code; Hunter's visible root loadout is now production-bound across Quickstep Volley, Pinning Shot, Fan of Arrows, Power Shot and Skyfall. Build Openworld RPG run `36651745440` at code state `89a480d16a3b8c49b5e43a369c53f94c3ab0df30` passes unit tests/build, bootstrap JAR verification, core-profile dedicated server smoke, gameplay dependency server smoke, gameplay client startup, both playtest-JAR builds, pack packaging and artifact upload. This verifies code/startup/authority wiring; Hunter joined-player feel and multiplayer remain untested.
- Hunter `Quickstep Volley` is now production-bound as Hunter Active Skill 1 / F. The server retains the locked 18 Mana / 7.0 s cooldown, Bow/Crossbow requirement, 3.0-block ordinary dash / 4.0-block three-Focus empowered dash, exactly 0 i-frame ticks, three physical projectiles at 0.55 ActionCoefficient each, 1.65 same-target coefficient cap and 0.70 whole-action PoiseCoefficient. The client sends only current forward/strafe key intent immediately before the cast; the server normalizes that intent against server-facing orientation and owns distance, collision and timing. Zero directional input produces no invented fallback displacement. Accepted movement is collision-resolved over six server steps inside a 10-tick action window; those six-step/10-tick bindings, the current 24-block Spell Engine delivery range, 2.0 projectile velocity and -4/0/+4-degree volley spread are implementation/playtest bindings rather than new gameplay-canon numbers. A full three Focus is consumed only after the server accepts the cast. Spell Engine supplies its pinned `spell_engine:archery_release` ranged-release animation and deterministic three-projectile transport; the projectile body reuses the already-admitted KayKit Character Pack: Adventures arrow mesh/texture with ordinary world lighting rather than holy/full-bright treatment. The project custom impact handler resolves each arrow as physical damage and one-third of the 0.70 whole-action Poise budget, caps a target at three Quickstep hits, refreshes/marks Quarry through Hunter authority, preserves long-range/weak-point Focus rules, publishes the canonical +3 Hunter-active Quarry Ultimate event at most once when the Quarry that existed at cast acceptance is hit, and preserves the existing +6 personally-caused ranged poise-break event. Empowered piercing is not a blanket Spell Engine `pierce=1`: the spell resource starts at zero pierce and a gated server mixin grants exactly one continuation per projectile only when the struck actor binding explicitly exposes `hunterQuickstepPierceable`; the normal baseline admits it while legacy/boss profiles including R01 Earthloong fail closed, so an empowered volley cannot silently pierce elites/bosses by donor inference. English/Korean player text is present and the canonical F slot is published through the existing project-owned five-slot Spell Engine bridge. Quaternius UAL2 Standard remains a useful CC0 motion pool but is not used for this skill: direct inspection of the pinned Spell Engine 26.2 `archery_release` asset showed a purpose-built ranged-weapon release pose, which preserves bow posture better than forcing a full-body slide/dash clip over the shot. Build Openworld RPG run `36662209318` at code state `5d0490b5906b898c3e2e7607f2b54f8cede384fa` passes unit tests/clean build, bootstrap JAR verification, core server, gameplay dependency server including the target-sensitive projectile mixin, gameplay client startup including the KayKit projectile renderer mixin, both playtest-JAR builds, Modrinth pack packaging and artifact upload. This is code/startup verification only: joined-player 3/4-block terrain feel, visible three-arrow spread, bow/crossbow pose, collision shortening, SFX, latency and empowered-normal-enemy pierce still require real Minecraft playtest; multiplayer remains untested.
- Hunter `Pinning Shot` is now production-bound as Hunter Active Skill 2 / G. It uses the locked 18 Mana / 8.0 s cooldown, Bow/Crossbow requirement, 1.55 physical ActionCoefficient and 1.00 PoiseCoefficient; a full three-Focus cast consumes Focus only after server acceptance and raises PoiseCoefficient to 1.50. The hit applies project-owned `Snared` instead of a donor potion effect: normal/elite targets receive x0.65 movement for 60 ticks, minibosses x0.80 for 50 ticks and bosses x0.88 for 40 ticks; empowerment adds 30 ticks to non-bosses and 10 ticks to bosses. Reapplication never stacks movement multipliers or shortens a stronger/longer active snare. To represent those canon values exactly, external hostile combat rank is now split into `NORMAL_ELITE / MINIBOSS / BOSS`; R01 Earthloong is explicitly `BOSS`, while Warrior/Rebuke boss-like checks continue to treat both miniboss and boss as their existing higher-rank bucket where their own canon does not distinguish the two. Snared projects through one stable transient Minecraft MOVEMENT_SPEED modifier and is removed on expiry/unload; targets without an admitted project combat profile or MOVEMENT_SPEED fail closed before Pinning damage is accepted. The single projectile uses pinned Spell Engine `archery_release`, the already-admitted KayKit arrow geometry/texture with a distinct neutral hunter tint, a current 24-block delivery range, 2.0 velocity and 0.24 x 0.24 x 1.15 hitbox; those range/velocity/hitbox/action-envelope values are implementation/playtest bindings, not newly invented class-canon numbers. The project custom impact handler owns damage, poise, Quarry/Focus refresh, long-range Focus, personal ranged poise-break publication and the canonical +3 active-hit-on-preexisting-Quarry Ultimate event. English/Korean text and the canonical G slot are published. The first Build Openworld RPG run `36663757089` failed at compile because `OpenworldRpgMod` was missing one import for the newly-ticked hostile-status runtime; code commit `d18dae843a3b0f5c79bfed838209a39852dfc870` fixes only that integration omission. Final run `36663889208` passes unit tests/clean build, bootstrap JAR verification, core server, gameplay dependency server, gameplay client startup including the Pinning projectile renderer mixin, both playtest-JAR builds, Modrinth pack packaging and artifact upload. This is code/startup verification only: joined-player arrow readability, actual normal/miniboss/boss slow feel, reapplication readability, SFX and multiplayer remain untested.
- Hunter `Fan of Arrows` is now production-bound as Hunter Active Skill 3 / V. The server owns the locked 26 Mana / 10.0 s cooldown, Bow/Crossbow requirement, five-projectile ordinary fan, seven-projectile three-Focus empowered fan, two-arrow per-target hit cap, 1.90 ordinary / 2.15 empowered maximum per-target ActionCoefficient and 1.00 whole-action PoiseCoefficient. A full three Focus is consumed only after the server accepts the cast. The per-target coefficient cap is enforced by distributing 0.95 ordinary / 1.075 empowered ActionCoefficient to each accepted arrow and rejecting a third hit on the same target; the 1.00 whole-action Poise budget is distributed across all visible projectiles as 0.20 per ordinary arrow or 1/7 per empowered arrow. The spell resource authors the ordinary five-shot sequence, while the pinned Spell Engine 26.2 `PROJECTILE_SHOOT` event exposes mutable launch properties before extra shots are scheduled; the project reflection adapter raises only an accepted empowered cast's `extra_launch_count` from 4 to 6 so the actual visible/projectile count becomes seven instead of spawning hidden or damage-less fake arrows. The ordered direction offsets are currently 0/-4/+4/-8/+8 degrees for the ordinary fan and add -12/+12 degrees when empowered, with one-tick launch spacing, 24-block delivery range, velocity 2.0 and a 0.22 x 0.22 x 1.15 projectile body. Those cone angles, spacing, range, velocity, hitbox and 10-tick action envelope are implementation/playtest bindings rather than new gameplay-canon numbers. Presentation deliberately reuses the already-admitted neutral KayKit Character Pack: Adventures arrow mesh/texture and pinned Spell Engine `spell_engine:archery_release`; Fan's identity comes from the visible multi-arrow cone rather than an unrelated temporary projectile skin. The custom impact path remains server-owned for physical damage, Poise, Quarry/Focus refresh, >=7-block Focus, personally-caused ranged poise break and the canonical +3 active-hit-on-the-Quarry Ultimate event, published at most once for the Quarry captured at cast acceptance. The resource has no pierce/homing, and the same-target cap remains two even when empowered, preventing point-blank shotgun abuse. English/Korean text and the canonical V slot are published. Build Openworld RPG run `36665723551` at code state `68a738de298ab3f313c47550b7ff2aa411a26e4f` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated server smoke, gameplay dependency server smoke including the new Spell Engine projectile-launch listener, gameplay client startup, both playtest-JAR builds, Modrinth pack packaging and artifact upload. This is code/startup verification only: joined-player five/seven-arrow readability, cone width, one-tick shot cadence, close-range two-hit cap feel, SFX and multiplayer remain untested.
- Hunter `Power Shot` is now production-bound as Hunter Active Skill 4 / X. It keeps the locked 34 Mana / 14.0 s cooldown, ranged-weapon requirement, fixed 0.70 s committed wind-up, 3.00 physical ActionCoefficient, 1.60 PoiseCoefficient and 1.40x shot-specific weak-point multiplier; a full three-Focus cast consumes Focus only after server acceptance and upgrades the shot to 3.35 ActionCoefficient / 1.55x weak-point multiplier. The 0.70 s wind-up is authored as Spell Engine `STANDARD`, not an early-release `CHARGE`, and reuses the pinned `spell_engine:archery_pull` ranged-charge animation followed by `spell_engine:archery_release`. The projectile is a real single physical Spell Engine projectile with no homing or pierce; current delivery range 24 blocks, velocity 3.20, 0.22 x 0.22 x 1.15 hitbox and 1.85 render scale are implementation/playtest bindings, not extra class-canon numbers. The visible body reuses the already-admitted KayKit Character Pack: Adventures arrow geometry/texture rather than introducing a temporary projectile asset. Power Shot also closes the previously missing authored weak-point seam without inventing anatomy: `ExternalActorWeakPointProfile` stores optional actor-local explicit volumes, and the server transforms the actual Spell Engine projectile collision point into that authored local frame before deciding whether the weak-point multiplier applies. Empty profiles always fail closed; R01 Earthloong deliberately remains without a weak-point profile, so its head/height/model is not guessed and Power Shot receives no weak-point bonus against it today. Upstream Spell Engine 26.2 at commit `d3cba71b726c6fcfb129969f7eeb679faa5ee8ed` was inspected to confirm that entity projectile impacts pass the adjusted `EntityHitResult.getLocation()` into `ImpactContext.position`. Archers 26.2 `Power Shot` at commit `8455d662c13b1236e0ddf42b0b802dc58960dd2a` was also inspected but not adopted because its next-arrow stash design conflicts with this project's authored committed direct shot; only the dependency-grade archery posture is reused. Damage, Poise, Quarry/Focus refresh, explicit weak-point Focus/+3 Ultimate publication, personally-caused ranged poise-break +6 and the canonical +3 active-hit-on-preexisting-Quarry event remain server-owned. English/Korean text and the canonical X slot are published. Build Openworld RPG run `36669480301` at code state `39929bae72b56750f5ec87a79c3e0f333ce9c0fb` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated server smoke, gameplay dependency server smoke, gameplay client startup including the Power projectile renderer mixin, both playtest-JAR builds, Modrinth pack packaging and artifact upload. This is code/startup verification only: joined-player 0.70 s draw feel, 3.20 projectile speed, enlarged-arrow readability, authored weak-point feedback/SFX and multiplayer remain untested.
- Hunter root Ultimate `Skyfall` is now production-bound to Y and consumes the shared 100-point Ultimate Gauge / 35.0 s lockout only after the server successfully resolves the first valid meteor placement. The authoritative storm is the locked 7.0-block radius / 4.5 s duration and is grouped into five server damage pulses rather than treating every visible arrow as an independent hidden hit; each pulse carries physical ActionCoefficient 1.10 and PoiseCoefficient 0.60, preserving the exact 5.50 / 3.00 per-target totals. Hostiles remaining in the exposed field receive the locked movement multiplier x0.75 for normal/elite or x0.90 for miniboss/boss. Skyfall slow owns a separate transient MOVEMENT_SPEED modifier, refreshes only while the target remains inside, and never stacks with the stronger Pinning Snare: an active Snare suppresses Skyfall's modifier and Skyfall resumes only if its own field is still active after Snare ends. Terrain authority checks both the selected ground column and each target's rain exposure; solid overhead blocks reject/clip the visible rain and prevent damage through roofs instead of showing arrows through dungeon ceilings. Presentation reuses the already-admitted KayKit arrow body and pinned Spell Engine 26.2 `archery_upwards_pull` / `archery_upwards_release` + `METEOR` transport. Archers 26.2 `rain_of_arrows` was used only as a behavior/presentation reference; donor projectile/sound assets are not copied. Fifteen real visual arrows are distributed by a deterministic golden-angle pattern within the same 7-block radius over the storm window, while current 0.5 s wind-up, 32-block target range, 12-block launch height, 1.5 projectile velocity, 6-tick visual spacing and 15-arrow density are implementation/playtest bindings rather than new gameplay-canon numbers. The spell's custom projectile impacts do not own damage; server pulses do, keeping visual density and combat authority decoupled. Build Openworld RPG run `36672148957` at code state `cdcc51bff539a5dc77a0b53c4a068fb15cdbaaa3` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated server smoke, gameplay dependency server smoke, gameplay client startup, both playtest-JAR builds, Modrinth pack packaging and artifact upload. The same code state also aligns the combat defaults to Q/F/G/V/X/Y and suppresses vanilla Drop Item / Swap Offhand click consumption only when those vanilla actions still share Q/F with the project bindings; it does not repeatedly rewrite saved user controls. Joined-player storm readability, indoor edge cases, slow feel, Q dodge ergonomics, Y reach, SFX and multiplayer remain untested.
- normal runtime now defaults to the `gameplay` profile; `core` remains an explicit isolation/test profile, so a production-shaped launch cannot silently omit the admitted gameplay stack.
- broad player-facing R01 world/UI implementation is still incomplete, but persistent server-owned save state now exists for combat Lv/EXP, per-root Class Rank/XP, Attributes, equipped combat loadout, personal R01 progression, Gold, four-slot Recovery Belt state, 36-slot Backpack/Personal Storage, Material Pouch/Vault, Key Items, Pending Reward Claim, and reconnect-safe pending/completed reward transactions
- class-progression authority now persists all ten Rank-10 specialization identities independently from root Class Rank. The first branch can be committed only at Rank 10+ for the active root and activates immediately; the sibling branch stays locked until Rank 20+ and its own specialization trial is completed. Branch switching uses the canonical 60% class-switch cost with the 150..1,500 Gold bounds, requires server-approved out-of-combat + class-facility context, debits Gold through an idempotent transaction, and reconciles an interrupted paid switch on reconnect instead of refunding while keeping state. Rank-20 technique, Rank-32 doctrine and Rank-44 ascendant completion are now separate persistent branch milestones; their order is enforced by state, each specialization owns exactly two canonical doctrines, and a completed Rank-32 branch can switch its active doctrine outside combat without rewriting the milestone. These milestone states do not yet imply that the authored branch skill/mechanic/ultimate effects themselves are runtime-bound.
- passive-progression authority is now persistent and data-driven. The canonical `CLASS_PROGRESSION.md` tables are represented by a bundled catalog of 115 root/branch nodes: 7 root nodes per class with 17 total root ranks and 8 nodes per specialization with 18 total branch ranks, including exact I/II/III/Capstone tier identities. Allocation enforces the 25 rank-point + first-five-of-eight Insight cap of 30, active-root/active-branch ownership, 5-root-point Branch-I gate, Rank-20 + 4-branch-point Branch-II gate, Rank-32 + 9-point Branch-III gate, and Rank-44 + 14-point Capstone gate. Forty authored Class Insight identities are persisted per root; the first five completed Insights affect the spendable-point budget and Insight completion uses the existing reconnect-safe reward transaction path for the canonical 8% current-Class-Rank XP reward. Full passive respec uses the canonical 20% class-switch-cost formula bounded to 50..500 Gold, requires server-approved shrine/trainer context and out-of-combat acceptance, refunds root + current active-branch allocations while preserving the inactive sibling allocation, and uses an idempotent persisted Gold transaction so reconnect cannot keep the new allocation while refunding the fee. Encounter-specific R01 Insight detectors remain separate work. All five root-class passive-tree consumer sets are now runtime-bound; specialization-branch consumers and Hidden Techniques remain separate. The state/API still does not auto-complete an Insight merely because its ID exists.
- Build Openworld RPG run `36681079981` at code state `1d4198aeb2485fc9528b4a19e0c0ffb7243b4901` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact `openworld-rpg-m0-1d4198aeb2485fc9528b4a19e0c0ffb7243b4901` was produced (SHA-256 `81a445b3421564679fcdc8955c468b8d678d143c87b414f24f225e18c8426fb7`). This is build/startup verification, not manual passive-tree/class-screen playtest or multiplayer proof.
- late-class Class-XP catch-up is now a shared project-owned runtime rule rather than a document-only formula. The expected-Class-Rank mapping and x1.00/x1.30/x1.75/x2.25 gap bands are deterministic, and percentage reward transactions persist the final resolved plan so reconnect or a later Rank change cannot alter an already-created reward. R01 Earthloong's first-boss reward is the first content-level path bound through the modifier -> catch-up -> persisted-plan sequence. Other reward sources remain gated on their own canonical content-level/anti-farm ownership rather than receiving a guessed modifier.
- Hunter and Warrior root passive trees now have concrete combat consumers for all 14 root nodes. Hunter binds Keen Eye, Light Step, Efficient Draw, Quarry Pressure, Focus Retention, Weakpoint Study and Trail Sense; the Bow/Crossbow authority bridge now accepts weak-point hits only from explicit authored actor-local weak-point zones and uses the canonical 1.25x default where such a zone actually exists. Current R01 Earthloong still exposes no accepted weak-point profile, so it remains fail-closed rather than receiving an inferred head zone. Warrior binds Steel Nerve, Tireless Combatant, Weapon Rhythm, Crushing Intent, Held Momentum, Counterforce and Battle Temper. Weapon Rhythm is applied to both the Minecraft attack-speed projection and the server cadence firewall; this prevents a faster animation from being rejected by unchanged project damage authority. Cleric now also binds Wellspring, Mercy, Sacred Guard, Resolute Faith, Lingering Grace, Balanced Service and Living Doctrine through the existing combat/resource/Grace authorities rather than a parallel class subsystem. Mage now binds Deep Well, Arcane Efficiency, Spell Edge and Quick Sigils through the shared Max-Mana, skill-Mana, Magic-Power and `spell_power:haste` authority paths, while Weave Memory, Triune Study and Resonant Mind are consumed by a server-owned Arcane Weave state machine. Guardian now also binds Bulwark, Enduring Guard, Shieldcraft, Protective Force, Resolve Keeper, Defiant Retort and Stand Together through the shared vitals/Stamina/guard/barrier/perfect-guard authorities, so all 35 root passive-tree nodes across the five classes have runtime consumers. Specialization branch passives, Hidden Techniques and encounter-specific Insight success detectors remain separate.
- Build Openworld RPG run `36706950621` at code state `b831cb2656b4037b4eb7d59d30ec1f0665f166f8` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact `openworld-rpg-m0-b831cb2656b4037b4eb7d59d30ec1f0665f166f8` was produced (SHA-256 `9d72443442e3e732e3aa4c8681c185cc32282276a95ca39b063b38f762c8b19d`). This verifies code/build/startup/runtime wiring only; manual passive feel, R01 end-to-end playtest and multiplayer remain untested.
- Cleric root passive authority is now runtime-bound for all seven canonical root nodes. Wellspring adds flat Max Mana while preserving current Mana percentage across allocation/respec changes; Mercy increases project-owned skill-healing output; Sacred Guard increases project-owned barrier amount; Resolute Faith scales only the equipment-derived Magic Resistance snapshot; Lingering Grace extends the existing Grace expiry clock by the authored amount. Balanced Service is integrated at the project spell transaction layer so a player who can afford only the discounted Mana cost is still accepted by PRE/POST authority, while the four-second discount is consumed only by the next matching opposite-role accepted skill. Living Doctrine restores 8 Mana only on an actual newly-added third Grace pip and uses its six-second server ICD. No new resource bar or duplicate healing/barrier authority was introduced.
- Build Openworld RPG run `36793554414` at code state `d5f2e6015a34976261d0a9bd1de9ca1edb4ff079` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact `openworld-rpg-m0-d5f2e6015a34976261d0a9bd1de9ca1edb4ff079` was produced (SHA-256 `9cbcabc5bae39fd17f60b946c3cd4c3d095a017f9caa446b69f10820ba96ed40`). This is technical/runtime verification only; manual Cleric feel, R01 end-to-end playtest and multiplayer remain untested.
- Mage root passive authority remains runtime-bound for all seven canonical root nodes, and all four ordinary Mage root actives are now production-bound: Arc Bolt / F, Phase Step / G, Frost Ring / V and Flame Burst / X. Arcane Memory remains live on the shared Arcane Weave state, and the Weave consumer list now explicitly includes Frost Ring and Flame Burst as well as Arc Bolt and Phase Step. Flame Burst is project-authoritative at 30 Mana, 12.0 s cooldown, 3.5-block ground-zone radius and 2.5 s duration. Under the pinned Spell Engine cloud lifecycle, the current 10-tick callback binding produces four project damage pulses before the 2.5 s cloud expires; a target present for all four receives exactly total direct ActionCoefficient 2.30 magic and PoiseCoefficient 1.00, while a Weave-empowered cast raises the direct total to 2.60. Burning is a separate server-owned timed magic/fire status: Flame Burst applies it once on that target's first successful field pulse, then eight 0.5 s ticks span the authored 4.0 s. Base Burning totals ActionCoefficient 0.40; Weave-empowered Burning totals 0.50; it cannot crit, is resolved through the project Magic Resistance path when applied, keeps the stronger existing resolved per-tick magnitude and refreshes duration instead of stacking. Triune Study scales only the incremental completed-Weave bonus, so the base 2.30 / 0.40 portions remain unchanged. Ground location and cloud presentation use Spell Engine `AIM -> CLOUD`, while project code revalidates the center/target footprint and owns damage, Poise, Burning and Weave math. The current 10-block ground-target range, immediate release and four-pulse cadence are implementation/playtest precision bindings rather than new locked class canon. Build Openworld RPG run `36808884541` at code state `019b520d368949bdac1a954198d4e79f7eaf1b89` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact `openworld-rpg-m0-019b520d368949bdac1a954198d4e79f7eaf1b89` was produced; normal JAR SHA-256 is `dcb674fa139b55547f1d81cb540faa643a9e04202aa63dcf02fd0a65afcf8115`. Manual Flame Burst targeting/readability, field pulse feel, Burning readability, the full four-active Mage rotation, latency and multiplayer remain untested. Mage root ultimate `Astral Convergence` and Mage Ultimate-Charge publishers remain the next root-kit gaps.
- Mage root combat is now production-bound across all five canonical root slots: Arc Bolt / F, Phase Step / G, Frost Ring / V, Flame Burst / X and Astral Convergence / Y. Astral Convergence uses the shared server-owned Ultimate Gauge, consumes a pre-existing Weave Ready at accepted activation without generating a new sigil, fixes its 6-block field at the caster's release position and resolves five one-second project-authority pulses across the authored 5.0 s duration. The five base pulses preserve total ActionCoefficient 5.80 magic and PoiseCoefficient 4.00; if Weave was consumed, only the final detonation receives the authored +15% direct / +25% poise bonus, with Triune Study scaling only that incremental Weave addition. Forced pull is fail-closed behind explicit actor reaction data: normal strength is at most 0.4 blocks/pulse, elite bindings can author the required 0.5 pull-strength multiplier, and miniboss/boss actors remain immovable unless their encounter binding explicitly enables pull. Mage Ultimate-Charge publishers are now live: damaging active primary +3, distinct additional target +0.5 up to +3/cast, completed 3-sigil Weave +6, and first meaningful eligible elite/boss control +2 with a server-owned 4.0 s caster/target ICD. Arc Bolt publishes its primary/fork events, Frost Ring publishes distinct-target and Chilled control events, Flame Burst de-duplicates its repeated field pulses per target/cast, and Phase Step's field publishes control only after the project slow is actually accepted. The actor binding schema now carries explicit pull-strength and meaningful-control-reward admission rather than guessing normal-vs-elite from donor identity. Build Openworld RPG run `36811586719` at code state `2899c0ec85d0658e5bba44014adfde31ed55538d` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact `openworld-rpg-m0-2899c0ec85d0658e5bba44014adfde31ed55538d` has archive SHA-256 `8f66ca65bacfc10d48cf2131250baeadcde1bb14b45b2a94e7fe7ed75dd4fd55`; normal JAR SHA-256 is `e0eeb875bbb837581af27619aaa6ff284c34c6c424183c446d7541e67e131500`. Astral visual/range/pulse feel, Ultimate-charge pacing, joined-player rotation, multiplayer and final skill HUD/icon/audio acceptance remain untested.
- Guardian root combat is now production-bound across all five canonical root slots in addition to its seven root passive-tree nodes. The shared server-owned guard state is now fed from a real validated Minecraft blocking setup rather than remaining an isolated state machine: a server-observed block opens the existing 4-tick perfect-guard/held-guard authority as soon as the shared action and guard-break gates permit it, release/dodge/guard break close the same state, and guard-state transitions republish combat build state so Stand Firm raises actual PlayerPoiseMax by 25% while guarding. Its authored ordinary non-launch knockback multiplier of 0.70 is exposed as a server seam, but current R01 hostile attacks do not publish an ordinary knockback amount, so no artificial R01 displacement was invented. Bulwark Rush / F spends 20 Stamina, uses the authored 8.0 s cooldown, moves up to 3.8 blocks collision-resolved, applies ActionCoefficient 1.45 physical / PoiseCoefficient 1.80 to the first substantial frontal target and grants 50% of the equipped guard setup's normal frontal absorption during its committed rush without becoming a perfect guard. Warding Strike / G spends 20 Mana on 7.0 s cooldown, resolves the authored 3.5-block arc at ActionCoefficient 1.60 / PoiseCoefficient 1.40 and applies a five-second server-owned Provoked state to admitted AI targets; R01 Earthloong target selection now consumes Provoked as x2 boss target weighting without mutating stored threat, while normal/elite bindings use x4 and PvP receives no mind-control behavior. Aegis Field / V spends 30 Mana on 15.0 s cooldown, consumes all available Resolve and applies the shared six-second barrier to nearby allies at BarrierCoefficient 0.18 + 0.04 per consumed pip. Counterwall / X spends 22 Stamina on 12.0 s cooldown, owns the canonical 15-tick stance, converts the first valid perfect-guardable hit into a true project perfect guard, releases its 4-block 1.70 Action / 2.20 Poise counter wave and grants exactly +2 Resolve total for that trigger. Unbroken Line / Ultimate spends the shared Ultimate Gauge and creates the authored moving eight-second / seven-block aura: initial BarrierCoefficient 0.22, ordinary admitted incoming damage x0.80, guard Stamina cost x0.80, Guardian effective player-poise pressure resistance x1.75, and engaged project AI inside the aura receives Provoked refresh every 40 ticks. Guardian Ultimate-Charge publishers are now live for qualifying guarded hits (+2 at final Stamina cost >=12), perfect guards (+6), hostile consumption of each 5% recipient-MaxHP step of a Guardian-owned barrier (+2, max +6 per source/recipient), and guarded/received hits from a currently Provoked target (+2, 2.0 s per-target ICD). Build Openworld RPG run `36816867019` at code state `39195b1d7a0e55f88f4837ec19cbceffa9f9118a` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact `openworld-rpg-m0-39195b1d7a0e55f88f4837ec19cbceffa9f9118a` has archive SHA-256 `20814287d2b5ab60abeadf5cc0dedd4caa05505513fe635c629aeceaaa3a839a`; normal JAR SHA-256 is `41b556c77df8a45f9f35f9c8e3faafed9263ec7daccde1ca55dc18fc2c9a1f71`. Guardian skill timing/geometry/presentation, press-to-perfect-guard feel, ultimate-charge pacing, final HUD/icons/audio, client world join and multiplayer remain manual gates and are not claimed tested.

The internal folder/mod/artifact identifiers remain stable production IDs. They are **not player-facing branding** and do not need to be renamed merely because the eventual display title differs.

The internal artifact/version strings above are production identifiers. They are **not permission to display `alpha`, the slug or other development terminology inside normal gameplay UI/content.**

Pinned dependency ownership/version boundaries are canonical in `M0_DEPENDENCY_AUDIT.md`.

Baseline stack includes Fabric API plus the selected player-animation, GeckoLib, armor/ranged/trinket/combat/spell and curated creature dependencies recorded there. **AzureLib is not a second baseline animation engine.** Essential may be used for hosting/social convenience only and never owns gameplay/save authority.

Do not silently upgrade/substitute pinned runtime dependencies during implementation because a newer version happens to exist. Re-evaluate only for a real blocker or deliberate migration.

---

## 3. Product identity

This project is a large authored open-world fantasy action RPG built on Minecraft, not vanilla-plus.

Minecraft supplies the block world, runtime, input base and hosting environment. Project-owned systems supply the player-facing RPG identity:

- The static ordinary-equipment affix catalog is now runtime-bound at **29/29**. `weak-point damage` is snapshotted with the server-authoritative Bow/Crossbow launch context and adds only after the target's authored weak-point volume actually accepts the impact; equipment swapping during flight cannot retroactively change it. `negative-status duration reduction` is projected from the equipped loadout into the server-owned negative-status application boundary, affects only newly applied authored statuses, and composes multiplicatively with transient protection such as Sanctuary without retroactively shortening an existing status. `potion/food effect strength` now has a live project-owned numeric-effect consumer: Healing Potion HP restoration and both immediate/tail Focus Draught Mana restoration scale by the equipped bonus, while Cleansing Tonic's qualitative cleanse and fixed authored buildup-resistance duration are deliberately unchanged. The food/Nourishment subsystem itself is still a separate R01 implementation task, so this binding does not claim meals are playable yet. With no static `stored_only` affix identities left, current R01/Nessa ordinary equipment can materialize without silently dropping an eligible affix; the Nessa Gold/inventory/SOLD transaction is now implemented and only its final player-facing market UI remains separate work. The first verification run `36820166557` correctly failed only because two legacy tests still asserted that weak-point damage must remain unsupported; those stale expectations were updated without weakening the production checks. Final Build Openworld RPG run `36820378036` at code state `284d92ee78fa0970d19f2688557d8c12747aea7c` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact `openworld-rpg-m0-284d92ee78fa0970d19f2688557d8c12747aea7c` has archive SHA-256 `2ab3762ef0905d0d659c927722c3b18e6e949c3df585529fcd02b83d49fff9ad`; normal JAR SHA-256 is `3f043ffac54b2aaecd8a6fd5d640e3792bb73d90e328c9069b4ed0a9078142e0`, M0 playtest JAR SHA-256 is `37e10de6d9e38b56fc6ed961b02d39116d91b1cf8d87f88e6bd695a503cb54ff`, R01 integration JAR SHA-256 is `eea3f9887bbd8fd0b7a460ff1aadd2406db482f848cc75522d8171d7ae8c39e8`, and the mrpack SHA-256 is `26b27c79e82d7a337b187d3b9d736d203030e8ae1c0d96daa4395ec3f944f114`. Client world join, integrated R01 playtest and multiplayer remain unrun.
- Nessa Bell's rotating R01 equipment market now has a complete server-owned purchase transaction rather than stopping at a materialization plan. The authoritative flow validates the current 10-active-minute personal cycle and unsold slot, reconstructs the exact deterministic materialized item, rejects insufficient Gold, and rejects a normal purchase when the ordinary Backpack cannot accept the full item before any Gold mutation. Accepted purchases then use the stable `openworld_rpg:nessa_purchase/<player>/<cycle>/<slot>` identity for idempotent Gold debit and Backpack delivery before the slot commits SOLD. Normal merchant purchases do not silently spill into Personal Storage. If a server interruption leaves an already-debited purchase unfinished, reconnect reconciliation runs before cycle refresh and resumes the same deterministic transaction; only that paid recovery path may use Backpack -> Personal Storage -> Pending Reward Claim so a purchased item cannot be lost. Repeated clicks/reconnects cannot duplicate Gold debit or equipment, selling the item back still cannot restore the SOLD slot, and stale old-cycle requests remain rejected. Build Openworld RPG run `36822811563` at code state `3258d2b79e9dec099a53e1079c070705e2469c32` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact archive SHA-256 is `cbf8b9e2e1b686a745c15db4f77091dfd4808058eab59926dfae94af18d74be1`; normal JAR SHA-256 is `35787323a9cf87acba92ce29ed6e345a375af1e0a6a45582a46ee31a9e759716`, M0 playtest JAR SHA-256 is `9d6e8b16997fcc9d17e208f956b0cfd9f048e84709dd28c7c7f4fe287e6a0f1d`, R01 integration JAR SHA-256 is `903e42f47bfc54f7cb7e413248b90937890059579a262fedb68c7c17c880096b`, and the mrpack SHA-256 is `fe9eafa1d51ec40a1cf2d1165645dbdb684e06751c4bf2612d019b7b8fbe0d4f`. This is build/startup/transaction verification; final Nessa market UI, client world-join purchase interaction, integrated R01 playtest and multiplayer remain untested.
- Lysa Fen and Brin Hale now share a reconnect-safe server-owned fixed-stock purchase backend for the exact R01 consumable catalogue: Healing Potion 30 Gold, Focus Draught 35, Cleansing Tonic 40, Herbed Louxia Roast 25, Trail Skewers 20 and Glow Broth 25. Unlimited stock does not mean reusable transaction IDs: each accepted purchase persists a monotonic per-player serial plus a pending transaction before Gold mutation, then uses that stable receipt for idempotent debit and item delivery. Normal purchases require enough Gold and legal Backpack capacity before debit and never silently spill into Personal Storage; only an already-paid interrupted transaction may recover through Backpack -> Personal Storage -> Pending Reward Claim so reconnect cannot destroy a purchase. Recovery items use the canonical stack cap 20 and meals use the global food cap 50. The R01 Nourishment backend is also live: a meal owns a 24-tick / 1.20 s shared project action, is rejected during the existing five-second active-combat boundary, consumes one real Backpack item only at completion, heals 15% MaxHP out of combat, and replaces the single persistent Nourishment slot for exactly 24,000 personal active-world ticks (20 active minutes), so offline time does not drain it and defeat/respawn preserves it. Herbed Louxia Roast publishes +6% MaxHP, Trail Skewers +10% Stamina recovery and Glow Broth +10% Mana recovery through the existing player vital/resource authorities. `Potion/Food Effect Strength` scales the meal's numeric heal and Nourishment magnitude, while generic Healing Received still applies to the actual meal heal. Expiry/replacement republishes the combat build so MaxHP/resource recovery never remain stale. This is backend authority only: the Greenwater Remedies/Copper Kettle world services still obey the Alderford production-socket gate, final Lucifer service UI and accepted food/eating presentation remain open, and the separate alchemy/cooking recipe transactions plus profession Mastery/Insight are not claimed implemented. Build Openworld RPG run `36824492009` at code state `d9e09671cab8d4e3078b16e19c704df77f5377ab` passes unit tests/clean build, bootstrap JAR verification, core-profile dedicated-server smoke, gameplay dependency server smoke, gameplay client startup, both verification-JAR builds, Modrinth pack packaging and artifact upload. Artifact archive SHA-256 is `f557656b24cb4bd5043418f9eba27c573809fe1c2da018273c3a955bb93b0322`; normal JAR SHA-256 is `ad1f058b65597eddd538b4684f55dd3d4aabcec75b8f4653d6596f8b709ffa35`, M0 playtest JAR SHA-256 is `c033795b28bc83382662dcf0538844ad3929e8b6dfe30bad1a799b75db369885`, R01 integration JAR SHA-256 is `40b43e2715a470e99700b44dc4fef5488213400350eceeb717ad7e9458ed7c7a`, and the mrpack SHA-256 is `f547cb91856848c41b448d1237ba046ffa8702f049c6f82dc13bb09bc933f7ff`. Client world-join meal/merchant interaction, integrated R01 playtest and multiplayer remain unrun.
- Lv/EXP progression;
- stats/classes/skills;
- dodge/guard/parry/poise combat;
- RPG equipment/inventory;
- quests/world state;
- settlements/services/economy;
- custom/external creature ecology;
- dungeons/field bosses/world bosses;
- gathering/fishing/camps/housing/mounts;
- party/co-op multiplayer;
- external-first UI/models/animation/VFX/audio;
- Anchor-network story and personal endings.

The design goal is not feature count. It is one cohesive game whose systems reinforce exploration, combat, progression and world consequence.

`BRANDING.md` owns title-candidate evaluation and the eventual player-facing title lock. `Anchorwake` is currently an evaluated candidate, **not final canon**. Title selection is intentionally lightweight and does **not** block gameplay source bootstrap. Exact logo/font/graphic bytes remain external-first visual assets and therefore stay inside the ordinary presentation/provenance gate.

---

## 4. Personal-use / public-repository boundary

The intended gameplay build is private-use, but this GitHub repository is public.

Therefore:

- non-redistributable/private-use third-party bytes stay outside the public repository;
- the repository may store provenance and local import/integration instructions;
- committed third-party code/assets must satisfy their actual redistribution terms;
- permissive/open code and assets may be reused directly when allowed and useful;
- license claims are package/source specific — never infer `all assets by creator X use one license` when the source record says otherwise;
- no DRM/paywall/access-control bypass and no paid-asset piracy;
- a future public release requires a fresh provenance audit.

`EXTERNAL_SOURCES.md`, `PRODUCTION_ASSET_BINDING_MATRIX.md` and the asset-intake manifests own detailed provenance/binding state.

---

## 5. Active canon map

Primary gameplay/system canon:

- `GAME_DESIGN.md`
- `BRANDING.md` — title-candidate evaluation and eventual player-facing title; branding is not a gameplay-source-bootstrap blocker
- `COMBAT_BALANCE.md`
- `CLASS_COMBAT_KITS.md`
- `CLASS_PROGRESSION.md`
- `STATUS_AND_R01_ENCOUNTERS.md`
- `LOOT_ECONOMY.md`
- `EQUIPMENT_BALANCE.md`
- `RECOVERY_PRODUCTION_APPEARANCE.md`
- `GATHERING_FISHING_CAMP_HOUSING.md`
- `FISHING_COLLECTION_HOUSING_MARKET.md`
- `MOUNTS.md`
- `QUEST_WORLD_STATE.md`
- `PARTY_MULTIPLAYER.md` — formal party UX, participation eligibility, non-split personal EXP/Class XP, personal loot/Gold, co-op scaling, friendly-fire baseline and multiplayer acceptance matrix
- `UI_DIRECTION.md`
- `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md` — world challenge presets, personal accessibility assists, final frequent-action input map, Essential-safe defaults, subtitles/captions, non-audio combat cues, camera/VFX comfort and dynamic audio/music state behavior
- `R11_AQUATIC_ACTION_MATRIX.md` — closes R11 frequent-action `AQUATIC_NATIVE / AQUATIC_ADAPTED / AQUATIC_DISABLED_WITH_FALLBACK` classification, accepted UAL swim/combat/cast/guard motion strategy, 3D targeting, fallback ownership and server-authority rules
- `M0_DEPENDENCY_AUDIT.md`
- `M0_INTEGRATION_ARCHITECTURE.md` — external-mod composition contract: adapter/data-overlay/tag/event boundaries, authority firewall, validation/sync rules and anti-fork/update-containment strategy
- `R01_TEST_HARNESS.md` — separation between the state-mutating M0 combat fixture and the non-mutating R01 integration-playtest artifact

World/story/regional canon:

- `WORLD_STORY_CANON.md`
- `MAIN_QUEST_SCENE_PACKAGE.md` — cross-region main-route requirements, recurring-character functions, evidence counting, rejoin points, sequence-break handling, personal finale choice and multiplayer story ownership
- `REGION_CROSS_AUDIT.md`
- `REGIONS.md` — current concise region index, not an archive of old candidates
- `R01_VERTICAL_SLICE.md` + `R01_CONTENT_BIBLE.md` + `R01_UI_PRODUCTION_SPEC.md` + `R01_PLAYER_TEXT_SPEC.md`
- `R02_IMPLEMENTATION_PACKAGE.md` + `R02_CONTENT_BIBLE.md`
- `R03_IMPLEMENTATION_PACKAGE.md` + `R03_CONTENT_BIBLE.md`
- `R04_IMPLEMENTATION_PACKAGE.md` + `R04_CONTENT_BIBLE.md`
- `R05_IMPLEMENTATION_PACKAGE.md` + `R05_CONTENT_BIBLE.md`
- `R06_IMPLEMENTATION_PACKAGE.md` + `R06_CONTENT_BIBLE.md`
- `R07_IMPLEMENTATION_PACKAGE.md` + `R07_CONTENT_BIBLE.md`
- `R08_IMPLEMENTATION_PACKAGE.md` + `R08_CONTENT_BIBLE.md`
- `R09_IMPLEMENTATION_PACKAGE.md` + `R09_CONTENT_BIBLE.md`
- `R10_IMPLEMENTATION_PACKAGE.md` + `R10_CONTENT_BIBLE.md`
- `R11_IMPLEMENTATION_PACKAGE.md` + `R11_CONTENT_BIBLE.md`
- `R12_IMPLEMENTATION_PACKAGE.md` + `R12_CONTENT_BIBLE.md`

Quality/intake:

- `DESIGN_COMPLETENESS_AUDIT.md`
- `EXTERNAL_SOURCES.md`
- `PRODUCTION_ASSET_BINDING_MATRIX.md` — cross-region binding state, true model-selection queue, dependency-validation queue and the no-temporary-player-facing-design production rule; explicit status corrections here supersede older broad source-status summaries where they directly conflict
- `R01_ASSET_INTAKE.md` and evidence snapshots where applicable;
- `R01_ASSET_PHASE_B_PASS5_ACQUISITION_EVIDENCE_2026-09-17.md` — creator-controlled direct ZIP locators, current Standard/Source boundary correction and honest binary/hash limitation.
- `R01_ASSET_PHASE_E_APPAREL_POTION_MOTION_EXACT_REVIEW_2026-09-18.md` — direct extracted glTF structure/geometry review for Wizard/Ranger/Knight and Potion_1..4 plus version-sensitive UAL Drink/Consume evidence; downstream snapshots are corroboration only.
- `R01_ASSET_PHASE_F_KENNEY_EXACT_SHORTLIST_2026-09-18.md` — exact ordinary Kenney VFX/SFX shortlist with direct candidate SHA-256, PNG dimensions and audio durations; audition/Minecraft composition and signature layers remain open.

The implementation package owns a region's traversal/ecology/encounter/dungeon/system contract. The later matching content bible closes settlement name, named cast, exact quests/scenes/rewards/reconnect state and story handoff. **R01 uses `R01_VERTICAL_SLICE.md` as its opening/system package and `R01_CONTENT_BIBLE.md` as the later full-region content-bible refinement.** If an old package contains a working placeholder superseded by its content bible, the later content bible wins for that explicitly refined point.

`MAIN_QUEST_SCENE_PACKAGE.md` owns only the cross-region main investigation and its rejoin/state rules. It does not overwrite local regional quests/rewards/aftermath already owned by the matching content bible.

`PARTY_MULTIPLAYER.md` is a subordinate refinement of `GAME_DESIGN.md` §23, `QUEST_WORLD_STATE.md`, `COMBAT_BALANCE.md` and `CLASS_PROGRESSION.md`. It does not replace their solo rules; it closes the missing co-op reward/party behavior details.

`R11_AQUATIC_ACTION_MATRIX.md` is the dedicated refinement/audit required by the older R11 package wording. It closes the aquatic action-compatibility design gate; runtime retarget/render/playtest proof remains validation work rather than a reason to invent a second underwater combat system.

`PRODUCTION_ASSET_BINDING_MATRIX.md` is the dedicated cross-region visual/source triage. A row marked `DEPENDENCY_VALIDATE` must not trigger another broad model search; a row marked `OPEN_MODEL_SELECTION` is a true visual pre-code gate. Its Threateningly Mobs Continued storefront-license conflict rule overrides stale `MIT` shorthand for raw-byte reuse decisions until exact upstream licensing is resolved.

### 5.1 Current canon-sync corrections

These are not new design options. They identify older live phrases that are already superseded by later canon and must be cleaned from their original documents during the final stale-text pass.

- **The final player-facing title is NOT locked, but it is NOT a gameplay-source-bootstrap blocker.** `Anchorwake` was researched and collision-screened but returned to candidate status after first-contact owner feedback showed that `Anchor` is not self-explanatory before the setting is learned. `BRANDING.md` owns lightweight later finalization.
- **Alderford is the final player-facing R01 starting-settlement name.** Older wording saying the starting-settlement name/lore will be decided later is stale.
- `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md` closes the global difficulty/assist, frequent-action input, subtitle/non-audio cue, camera/VFX comfort and audio/music **behavioral** contracts. Exact SFX/BGM files remain an asset-intake problem, not an open behavior-design problem.
- `R11_AQUATIC_ACTION_MATRIX.md` closes the former R11 aquatic action-compatibility design blocker. Runtime retarget/render/playtest proof remains validation work.
- **R03 Basalt Wyvern references in older class-progression prose are stale.** Basalt Wyvern belongs to R10. The live `CLASS_PROGRESSION.md` now uses current R03 collapsed-mine/lift-route/Griffin identities instead of resurrecting Basalt Wyvern or the obsolete `Rocky Roller` slot.
- `R01_ASSET_INTAKE.md` now includes the later R01 fish intake narrowing: old Quaternius clownfish/Sea-Life/tuna candidates are rejected for Heartland, and the current four-role direct-review queue is **Small Fish Common Minnow / CDmir Fish / Quaternius Armored Catfish / CDmir Esox**. Broad fish discovery is stopped unless one role fails direct review. None is yet binary-hashed/3D/Minecraft accepted; `R01 ASSET READY` remains `NO`.
- Phase E exact-file review now adds technical corroboration for the selected Wizard/Ranger/Knight apparel families (shared 65-joint humanoid skin, real BIN/texture payloads), confirms `Potion_1..4` are distinct geometry payloads, and corrects the UAL evidence boundary: the 2025-06-10 46-clip UAL1 Standard mirror has no `Drink`, while a newer pinned integration corroborates exact `Drink`/`Consume` names. Creator-current archive acquisition, project-local SHA-256, direct visual/retarget/Minecraft acceptance remain open, so `R01 ASSET_BINDING COMPLETE` and `R01 SOURCE READY` remain `NO`.
- Phase F Kenney binding now pins exact ordinary baseline sprite/audio candidates and direct candidate hashes/timing. This stops generic filename discovery for those baseline roles, but **does not** close Earthloong electrical signature presentation, final Burning/Frostbite shapes, audio audition, spatial mix, Low-VFX review or Minecraft composition. `R01 ASSET_BINDING COMPLETE` and `R01 SOURCE READY` therefore remain `NO`.
- Older `Threateningly Mobs Continued = MIT` shorthand is not sufficient for raw-byte reuse. Current storefront metadata conflicts; use dependency-only handling until the exact canonical upstream license is resolved as recorded in `PRODUCTION_ASSET_BINDING_MATRIX.md`.
- `M0_INTEGRATION_ARCHITECTURE.md` closes the former open question of how multiple large external mods are combined: dependencies stay isolated behind project adapters/data overlays/tags, while project state remains authoritative. KubeJS/FTB Quests/Cobblemon/Create/Mine & Slash are architecture references unless separately admitted as runtime dependencies; this does not reopen the Fabric loader choice or import a second RPG progression stack.

---

## 6. Meaning of design-closed

The project uses a strict definition:

> An implementer should be able to build the planned player-facing game from the canon without inventing game design while coding.

Before a subsystem is handed to implementation, the canon must close, as applicable:

- behavior and state transitions;
- formulas, costs, rewards, limits and defaults;
- failure/recovery/edge behavior;
- multiplayer/server authority;
- save/rejoin/late-join handling;
- UI hierarchy/interactions/states/scaling;
- quest objectives, branches, visible aftermath and reward ownership;
- named NPC roles and scene beats where narrative depends on them;
- encounter composition, telegraphs, phases and reward rules;
- POI/settlement/service roles;
- data fields needed for later tuning;
- external presentation direction and the explicit gate that closes any unresolved exact asset.

Do **not** leave `decide during coding`, player-affecting `TBD`, `add something later`, vague placeholder bosses/resources or hidden implementation choices.

If a hard technical constraint invalidates canon:

```text
stop affected implementation
→ revise canon
→ review the new rule
→ implement the revised canon
```

Code does not silently become design authority.

---

## 6.1 R01 design-closure status

R01 now has a dedicated closure split:

```text
R01 GAMEPLAY / CONTENT DESIGN CLOSED: YES
R01 IMPLEMENTATION-TIME GAMEPLAY CHOICES REMAIN: NO
R01 ASSET_BINDING COMPLETE: NO
R01 SPATIAL_BINDING COMPLETE: NO
R01 SOURCE READY: NO
R01 IMPLEMENTED: NO
R01 PLAYTESTED: NO
R01 MULTIPLAYER TESTED: NO
```

The current authoritative closure table is `R01_CONTENT_BIBLE.md` §26–§28.

This means **do not reopen R01 systems, rewards, quest text, merchant logic, fish mechanics, mount behavior, Camp/furniture rules, fast travel or UI flow during coding.** If real asset/spatial/play evidence invalidates a value, revise canon first.

---

## 7. Content-closed does not mean source-ready

R01–R12 now have concrete regional content authoring and the cross-region main quest/rejoin structure is closed in `MAIN_QUEST_SCENE_PACKAGE.md`, but the project is **not yet fully gameplay-source-ready**.

Older package headers using `implementation-ready` must be interpreted narrowly as `the described mechanics/content flow no longer needs invention`. They do not waive these current pre-code gates:

1. **exact external presentation binding and provenance** for unresolved models, outfits, items, structures, Anchor machinery, final logo/font/graphic, VFX, animations, SFX/BGM and local-only/dependency boundaries;
2. **actual Azari spatial closure** — local-only intake tooling exists and the creator-acquired R01 extracted world slice has now been parsed from real Anvil data in `AZARI_R01_SPATIAL_PASS2.md`. The selected Pass-2 surface anchors/areas/route plus the Pass-3 Quarry interior review volumes are bundled as validated data in `data/openworld_rpg/world/r01_spatial_candidates.json`; every entry is deliberately `candidate`, and production-only runtime accessors refuse to expose those coordinates until client review promotes them. `AZARI_R01_QUARRY_INTERIOR_PASS3.md` records the real subsurface scan and the five candidate 3D room/boss review volumes. A primary Alderford terrain candidate, Alderford→Quarry surface corridor and Quarry exterior/overlook/entrance cluster are selected for in-client review. The full creator-ZIP hash, actual Minecraft-client load, gameplay-FOV sightlines, measured travel times, final service/property/event/node positions and dungeon/boss interior coordinates remain required before `SPATIAL_BINDING` is complete;
3. **asset-gated final encounter sheets** — exact player-facing guardian/boss names, anatomy-supported attacks/weak points/signature materials only after their accepted models are known;
4. **final stale-document / hidden-choice audit** — remove obsolete alternatives and ensure no implementation-time gameplay decision remains hidden in older live text.

**Player-facing title selection is not one of these gates.** It may remain a lightweight working decision while gameplay source is prepared, provided internal slug/version/development strings are not exposed as finished player-facing branding. Final title/logo packaging is closed before the finished player-facing release/presentation pass.

The global accessibility/difficulty/assist, subtitles/non-audio cues, frequent-action input map and audio/music state-behavior design are closed in `ACCESSIBILITY_DIFFICULTY_INPUT_AUDIO.md`. Exact music/SFX bytes remain part of the external-asset gate in item 1.

The R11 aquatic action/animation compatibility design is closed in `R11_AQUATIC_ACTION_MATRIX.md`. Runtime animation retarget/render validation remains required before R11 can be called tested or play-ready, but it is no longer a design blocker.

The cross-region main-route region requirements, evidence counts, recurring-character scene functions, rejoin logic, sequence-break handling, finale handoff and personal ending commit behavior are closed in `MAIN_QUEST_SCENE_PACKAGE.md`. Exact scene locations/cameras/outfits/props/audio remain subject to gates 1–3 where applicable.

`PRODUCTION_ASSET_BINDING_MATRIX.md` narrows gate 1: already-selected dependency actors proceed to runtime acceptance, concrete external candidates proceed to acquisition/conversion review, and only `OPEN_MODEL_SELECTION` rows require new visual search.

The narrow M0/source-bootstrap and early R01 authority layer now includes persistent combat Lv/EXP, independent per-root Class Rank/XP, Attributes, persistent equipped-loadout authority, personal R01 progression state, server-owned Gold, four-slot Recovery Belt state, canonical opening-loadout projection (Heartland Arming Sword + Watch Buckler + one loaded Healing Potion + 150 Gold), first-root-class starter weapon projection with displaced starter preservation, server-owned 36-slot Backpack + 36-slot Alderford Personal Storage + Material Pouch/Vault + Key Items + Pending Reward Claim, Pouch-first settlement material sourcing, persistent personal active-world time, persistent personal R01 gathering-node cooldown/tool/mastery/discovery state with reconnect-safe lossless Material Pouch delivery, semantic world-action authority for the five `Dust on the Quarry Road` categories, bounded objective class-attribution evidence, `Dust` activation/completion/reward authority with `Roots Below Stone` handoff and reconnect repair, persistent shared `Roadside Trouble` lifecycle/timing/participation state, bounded reconnect-safe repeat rewards whose Class XP stays on the contribution-time class, reconnect-safe reward plans, and a reconnect-safe Earthloong first-clear transaction that commits exactly one Superior Item Lv8 choice before delivery, persists the choice/grade/affixes, safely falls back Backpack → Personal Storage → Pending Reward Claim, and preserves the two-scale material grant with a permanent bounded receipt. Canonical project HP reaching zero is now the physical Earthloong-clear authority trigger: first-action-qualified participants are persisted with their qualifying class, online players finalize immediately, brief disconnects finalize on join, and personal quarry first-clear commits only for an actually active personal quarry run. The generic server-owned skill-healing transaction and Earthloong effective-heal participation bridge now exist, but concrete Cleric skill binding plus barrier/control/revive participation callers remain separate connections. The final spatial/donor-entity adapters for Upper Gallery / Collapsed Hoist / Root-Breached, final Earthloong boss-gear presentation, Lucifer choice UI and final reward models/icons are still separate connections; the static ordinary-affix runtime gate is now closed at 29/29. Post-Quarry semantic progression is now wired: an eligible Earthloong first clear idempotently advances `EARTHLOONG_CLEARED -> POST_QUARRY_BRIEFING_PENDING`, persists the `Lines Beneath the Land — Return to Alderford` objective across reconnect, the later damaged relay interaction can backfill missed Quarry Relay Evidence without duplication, and completing or whole-scene-skipping the Wayfarers' Hall briefing commits the same result (`ACT1_LEADS_OPEN` + both Western Relay and Whitecrest Station leads). The three pre-boss Quarry rooms now have a persistent non-spatial server encounter controller: canonical room order is enforced, Upper Gallery uses the exact 2 + delayed 2..5 staggered activation for 4..7 total Cave Centipedes, Collapsed Hoist uses the exact 3..6 Cave Centipede count, Root-Breached owns exactly one Nature Spirit with the canonical 1.00/1.65/2.30/2.95 HP and 1.00/1.40/1.80/2.20 poise scaling, ordinary-room reset preserves previously cleared rooms, and first valid room-combat contribution captures class then commits exactly one reconnect-safe Quarry attribution unit only when that room actually clears. Authored semantic anchor IDs are fixed without inventing Azari coordinates. The first-clear Earthloong boss-loot RNG plan is now persisted exactly once: the canonical six normal base families are equal-weight, the normal roll carries a guaranteed Superior minimum floor without inventing an undocumented Superior-vs-Exalted split, and the exact 15% direct signature roll chooses Rootquake Maul / Earthscale Ward at equal baseline weight. Reconnect cannot reroll this plan. The formerly open Ironbound Guard armor-slot and guaranteed-Superior+ grade-resolution choices are now closed by the equipment/loot canon and persisted in the Earthloong loot plan. The shared ordinary-affix roller/catalog/materializer and R01 base-specific valid-affix pools now exist. Live ordinary-item materialization no longer has a static `stored_only` affix blocker; accepted player-facing equipment visuals remain a separate production gate, while Nessa's server-owned purchase/delivery/SOLD transaction is implemented and only final market UI/presentation remains separate. The Earthloong Mythic unique server-effect foundation is now connected independently of that gate: Rootquake listens to the personal elite/boss poise-break result and enforces the exact 4-block / 40%-WeaponPower / 8-second-ICD damage trigger without applying a second boss poise break, while Earthen Reprieve listens to successful perfect guard, creates an 8%-MaxHP barrier for 4 seconds, replaces/refreshes rather than stacks, and enforces the exact 10-second ICD. Final Mythic numeric affix materialization and accepted VFX/model presentation remain separate. Bounded personal Quarry-run class attribution is now persisted per run across six canonical combat/objective units; Lift Shortcut, Relay Evidence and Earthloong are bound to existing runtime paths, repeated actions cannot inflate ownership, and first-clear dungeon completion now grants the existing 100% current next-Lv EXP + 64% current Class Rank XP + 180 Gold through the reconnect-safe reward transaction using the attributed completion class. The first **actual Spell Engine spell resource** for `openworld_rpg:arc_bolt`, canonical Better Combat melee replacement for project-owned actors, authoritative Bow draw-power replacement for project-owned actors, project-owned player vitals/resources, and the canonical active-defense state machine. Arc Bolt keeps the locked 22-block range, project CUSTOM impact handler and donor-neutral cost; its current 1.0 s cast / 1.0 blocks-per-tick projectile timing remains playtest-tunable while damage, Mana, cooldown and Poise stay project-owned. The dedicated `-m0-playtest.jar` prepares a legal Lv8 Mage +7 INT / ItemLv8 Staff test build and exposes only marker-gated integration commands; the normal JAR remains free of that verification marker. A real joined-player playtest has confirmed the authored Earthloong spawn path, Arc Bolt cast/release, held-input behavior, project cooldown presentation and a successful Arc Bolt impact without the target disappearing or corrupting. CI run `36239966413` verifies the canonical Better Combat melee foundation, player MaxHP/VIT and END/Stamina runtime, canonical dodge/guard/perfect-guard/guard-break timing/math including perfect-guard-only charges, the persistent armor/shield defensive-equipment publisher, and the fail-closed outgoing HP-damage boundary for project-owned external actors. The project now also publishes defensive equipment independently of the offensive weapon build: armor archetype + slot + Item Lv produce canonical per-slot-rounded Defense/MR, shield family produces canonical GuardRating, and Defense/MR/Guard Strength affixes feed one server-owned defensive snapshot. No weapon-guard rating is invented because the current canon has not assigned one. A normalized server bridge now accepts only explicit project incoming-hit data and resolves it through the player's project Defense/MR/Stamina/guard/dodge state before applying HP damage. Project-owned external actors can no longer fall back to donor-origin HP damage against players. R01 Earthloong now shares one data-backed encounter source for action selection plus the canon-closed Phase-1 physical impact rules: Claw Sweep 10%/medium guard, Tail Scythe 20%/heavy guard, and Quarry Rush 24%/heavy perfect-guard-only. Those percentages are converted to fixed raw physical attack data from the canonical same-Lv benchmark rather than runtime target-MaxHP damage. R01 Earthloong Phase-1 physical start geometry is now data-backed: Claw Sweep uses the locked 0°..120° front/side arc within 3.5 blocks, Tail Scythe uses the locked 60°..180° side/rear arc within 4.5 blocks, and Quarry Rush requires 5.0..9.0 blocks plus a clear committed line. Claw Sweep and Quarry Rush also have runtime-verified dependency-only presentation-state bridges to the pinned donor SkillNumber states 1 and 2 respectively; Tail Scythe deliberately remains presentation-unbound because the pinned donor surface exposes no accepted dedicated tail-scythe animation. Full attack-state scheduling/movement/contact invocation, real client visual acceptance and player-facing dodge movement/animation/input remain separate connections; these are not replaced with temporary motion or donor combat authority. Better Combat's real joined-player network/presentation hit also still requires one manual acceptance pass before M0 gate 9 can be called closed.


The R01 recovery/economy backend now also includes server-authoritative settlement Alchemy/Cooking
craft transactions for the three canonical Lysa potions and three currently materializable Brin
meals. Multi-material costs are atomic and reconnect-idempotent, settlement resolution uses
Material Pouch before same-player Material Vault, complete Backpack output is preflighted before
normal mutation, and committed interruption recovery cannot duplicate Gold/material/output.
Persistent Smithing/Alchemy/Cooking Mastery Insight authority uses the locked 0/4/9/15/22 rank
thresholds; current recipe first-craft awards are one-time only. Grilled Catch remains intentionally
unbound until the fishing item/name/model asset gate closes, and Smithing award events remain a
separate implementation unit. Build Openworld RPG run `36827845622` is SUCCESS at
`4fcaecdb408182a3fd35530ffce62fd523c4d05f`, including clean tests/build, bootstrap JAR, core
server smoke, gameplay dependency server smoke, gameplay client startup, verification JARs,
mrpack packaging and artifact upload. This is backend/build evidence only: live crafting station
world interaction, final crafting UI/presentation, integrated R01 playtest and multiplayer are not
claimed.


The R01 personal-contract/economy backend now also binds `Riverbank Remedies` and Nessa Material
Pouch selling. Fresh contract Herbs are tracked from durable valid R01 harvest transactions,
protected inside the ordinary Pouch rather than converted into quest tokens, released on Abandon,
and reset through the existing contract-reaccept generation on Reaccept. That reservation is now
respected by Alchemy/Cooking preflight, Craft Max, atomic Pouch→Vault consumption, individual
material sales and bulk material sales. Lysa turn-in persists the reward class before mutation and
reconciles exact three-Herb consumption, 40% EXP + 30% Class XP + 60 Gold, Healing Potion x1,
Herbalism +10 and Contract completion without duplicate recovery. Nessa material selling uses the
canonical nine R01 unit values, requires explicit confirmation for Regalhart Antler/Earthloong
Scale, excludes those boss materials from bulk sell, and persists sale intent before material/Gold
mutation. Build Openworld RPG run `36830800234` is SUCCESS at
`76e3cab9a1c1251b66ddc2256305d61e4f842918`, including clean tests/build, core and gameplay
server smoke, gameplay client startup, verification JARs, mrpack packaging and artifact upload.
This remains backend/build evidence: final board/service/material-sale UI, physical Alderford
binding, integrated gameplay and multiplayer are not claimed. At that code state, exact
non-Earthloong donor actor registry bindings were still gated, so that pass correctly did not
fabricate Louxia/Tough-Hide creature drop hooks.

The pinned 26.2 creature surface has since been inspected directly from the dependency JARs.
Thirteen R01 registry targets are now exact and startup-verified. The Alex's Mobs set is
`alexsmobs:gazelle`, `alexsmobs:bison`, `alexsmobs:raccoon`, `alexsmobs:crow`,
`alexsmobs:grizzly_bear`, `alexsmobs:centipede_head`, `alexsmobs:centipede_body` and
`alexsmobs:centipede_tail`; the Threateningly set is `threateningly_mobs:louxia`,
`threateningly_mobs:steelboar`, `threateningly_mobs:nature_hamony`,
`threateningly_mobs:the_regalhart` and `threateningly_mobs:the_earthloong`.
Canonical project combat-stat authority is bound for Bison, Grizzly, Cave Centipede head,
Steelboar, Nature Spirit, Regalhart and Earthloong using the locked R01 stat table. Cave Centipede
is intentionally head-owned: the donor body/tail multipart entities are startup-verified but do not
receive duplicate project HP/poise states because their donor damage path forwards to the parent.
The project-facing Nature Spirit role intentionally maps to the pinned donor's legacy
`nature_hamony` registry spelling rather than inventing `nature_spirit`. This registry/stat
closure still does not make those actors production-spawn-ready: only Earthloong is admitted
through the authored production spawn path. Cave Centipede, Nature Spirit and the other bound
actors remain fail-closed until their authored attack/reward/presentation contracts are connected.
Gazelle/Raccoon/Crow remain ecology identities without invented hostile combat profiles, and Louxia
remains passive food ecology rather than a fabricated combat kit. Build Openworld RPG run
`36967514764` at code/workflow state `24de568d84f6c81d80cfe6651b37272d652bc0a4` verifies the
13-target startup gate, tests/build, core/gameplay server smoke, gameplay client startup,
verification JARs and pack packaging. The donor attack-surface follow-up at
`bf237ab9b0c88d7564f4574287364ad8026139ca` then inspected the exact pinned Cave Centipede
and Nature Spirit behavior surfaces without copying donor code/assets. Cave Centipede's multipart
damage authority is now concretely bridged at
`cb690604d2a03150e2c6d989c17222bc533b7034`: body/tail hit proxies walk the donor's public
`getParent()` chain and resolve to the single `alexsmobs:centipede_head` project HP/poise owner;
missing/broken parent chains fail closed instead of creating independent segment health. Project
melee, ranged and generic project-damage application now consume that same canonical owner.
Build Openworld RPG run `36969022278` is **SUCCESS** at that code state, including clean
tests/build, pinned creature inspection, core/gameplay dedicated-server smoke, gameplay client
startup, both verification JARs, mrpack packaging and artifact upload
(`openworld-rpg-m0-cb690604d2a03150e2c6d989c17222bc533b7034`, artifact
`11211146368`). This does **not** open Cave Centipede or Nature Spirit production spawning:
their authored attack/reward/presentation acceptance is still outstanding, and Earthloong remains
the only production-spawn-ready R01 external actor. Registry/stat + multipart damage closure is
therefore distinct from spawn/attack/loot/presentation closure.

The next creature-authority pass at code state `40dd83b8b0b0c871ad75783674238ea0f4033280`
closes the server-owned **authored action-contract layer** for Cave Centipede and Nature Spirit
without opening either production spawn. Cave Centipede now has data-validated Scuttle Bite /
Body Rake / physically-gated Ceiling Drop timing, weights, the exact 8.0 s Ceiling Drop cooldown,
damage/guard-pressure contracts, Poison buildup payloads and deterministic weighted selection;
the controller can only choose Ceiling Drop when an eventual physical binder reports a real
climbed-above-target position and otherwise repositions rather than teleporting. Nature Spirit now
has data-validated Rooted Swipe / Earthen Ram / Bloom Quake action rules plus the exact Living Shell
authority contract: 20% MaxHP hostile post-mitigation damage over 4.0 s or <=40% poise requests the
next legal Shell, Shell lasts 2.5 s, reuses after 12.0 s, applies 0.65 direct-damage taken and 1.25
poise-damage taken multipliers, ends immediately on poise break, and a natural end forces Bloom
Quake only when an eligible target is within 4.0 blocks. Bloom Quake cannot immediately select
itself again through normal selection. Build Openworld RPG run `36970730169` is **SUCCESS** for
this state: unit tests/build, pinned creature inspection, core/gameplay dedicated-server smoke,
gameplay client startup, both verification JARs, mrpack packaging and artifact upload all passed.
The produced artifact is
`openworld-rpg-m0-40dd83b8b0b0c871ad75783674238ea0f4033280` (artifact `11212086150`).
This is still **not** donor animation/contact binding, reward delivery, production spawn acceptance
or an in-world playtest. Cave Centipede and Nature Spirit therefore remain fail-closed for production
spawning until their real dependency presentation/contact surfaces and reward hooks are connected and
accepted.

The next donor-contact pass at code state `f9cb315406926b29f0b724b582efd2d1efb4fa94`
narrows that gate without falsely promoting either creature. The pinned Alex's Mobs Continued 2.1.13
Cave Centipede exposes a real donor melee contact and `alexsmobs:centipede_attack`; the project now
intercepts only an accepted authored Cave Centipede head's donor melee proposal, keeps donor damage
and vanilla instant Poison rejected, holds the actor through the canonical 7-tick Scuttle Bite tell,
revalidates the <=2.3-block target at the hit frame, applies only the project-owned 11% same-Lv
benchmark physical hit, and enforces the canonical 8-tick recovery. Donor natural spawns and
verification fixtures do not inherit this bridge merely because their registry/stat profile is bound.
Build Openworld RPG run `36972385046` is **SUCCESS**: unit tests/build, pinned creature inspection,
core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs, mrpack
packaging and artifact upload all passed; the gameplay server log explicitly armed the 7/8-tick bridge
with `alexsmobs:centipede_attack`. Artifact:
`openworld-rpg-m0-f9cb315406926b29f0b724b582efd2d1efb4fa94` (`11212397093`).

This remains a **technical contact binder, not presentation acceptance**. Cave Centipede Poison
buildup stays fail-closed because the project does not yet have the complete player Poison buildup/proc
runtime and the R01 canon does not specify Cave Centipede's fixed source-Lv Poison proc damage budget;
no value is invented. Body Rake / Ceiling Drop also remain unbound to accepted donor presentation.
A 2026-10-02 byte-level reinspection of the exact pinned Alex dependency confirms this is a real donor
surface limit, not merely a missing adapter: the head exposes ordinary donor melee contact plus
procedural multipart locomotion/segment following, but no dedicated Body Rake/Ceiling Drop animation
state. The older R01 encounter canon already fixes Alex's Mobs Continued as Cave Centipede's external
presentation, so model selection is **not reopened**. The next gate is direct client review of that
fixed multipart model's real locomotion/contact surface; only if an authored move cannot be shown
honestly does the existing canon permit revising the move to one the accepted model can show.
Dedicated verification artifacts now expose raw fixed-model preview commands for Cave Centipede and
Nature Spirit without adding the project `authored_spawn` tag or opening production spawn/reward
eligibility.

Build Openworld RPG run `37013373528` is **SUCCESS** for this correction/inspection harness at code
state `58b95f9fcb9960d64749aab45ced4f9e462788c6`: tests/build, pinned creature inspection,
core/gameplay server smoke, gameplay client startup, both verification JARs, mrpack packaging and
artifact upload all passed. Artifact:
`openworld-rpg-m0-58b95f9fcb9960d64749aab45ced4f9e462788c6`
(`11229185745`, SHA-256
`d2e785e2872c55c7dda2e41f82a3b838e5e3c3fc87c497cbd1b8df762eecc519`).
This does not claim joined-world visual acceptance. The raw fixed-model preview commands are
`/owr_preview_cave_centipede` and `/owr_preview_nature_spirit`.
The same pinned Threateningly Mobs Nature Spirit inspection exposes only
`naturehamony_spawn`, `naturehamony_idle` and `naturehamony_end` animation states, with no
attack-specific animation surface for Rooted Swipe / Earthen Ram / Bloom Quake. Per the external-first
acceptance rule, those attacks are therefore still presentation-blocked rather than implemented as
invisible AoE or generic particle substitutes. Earthloong remains the only production-spawn-ready
R01 external actor.

The following Nature Spirit reward-authority pass at code state
`199d82437a854603a7443ea9ad7d9a00fc10b09f` closes the server-owned **personal
elite reward plan** without opening production spawn. One accepted project damage/support contribution
fixes that player's reward class for the encounter instance; defeat pre-rolls and persists each eligible
player's personal result before delivery, so reconnect cannot reroll the canonical 30% equipment roll,
60% Healing Herb 1–2 roll or 35% Verdant Crystal roll. The five Nature Spirit equipment base families
are the canon-locked equal-weight pool (Initiate Staff / Initiate Wand / Apprentice Focus /
River Scholar Garb / Greenwater Pendant), River Scholar Garb resolves one of the five armor slots at
equal weight, and grade plus source-Lv ±2 Item-Lv variation use the global ordinary-loot rules. The
6% current next-Lv EXP + 5% current Class Rank XP + 20 Gold layer is routed through the existing
idempotent personal reward transaction and material delivery is reconnect-safe. A successful
equipment roll intentionally remains pending rather than fabricating an item while the ordinary
Exalted equipment sell/materialization value is not yet closed. Build Openworld RPG run
`36975176112` is **SUCCESS** at this code state: tests/build, pinned creature inspection,
core/gameplay dedicated-server smoke, gameplay client startup, both verification JARs, mrpack
packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-199d82437a854603a7443ea9ad7d9a00fc10b09f`
(`11213159221`). This is backend/runtime evidence only. Nature Spirit still lacks an accepted
production spawn binder and attack-specific player-facing model/animation presentation, so the reward
bridge remains dormant for real R01 production spawning until those gates are accepted.

The support-eligibility follow-up at code state
`efb63e028f5db5f0a802ff499b4457a693dfd668` closes the currently implementable
encounter-linked **heal + barrier contribution** path without broadening reward eligibility. A skill
heal can qualify only when the caller passes the actual Earthloong/Nature Spirit encounter actor,
the caster heals another player, and real missing HP is restored. A skill barrier can qualify only
through the explicit encounter-linked barrier entry point, when it gives another engaged player a
positive effective barrier; self-barriers, proximity, zero-effect applications and ordinary
non-encounter heal/barrier calls do not manufacture participation. Both encounter services perform
their own actor validation, and Nature Spirit still additionally requires the accepted authored-spawn
tag. Build Openworld RPG run `37003375371` is **SUCCESS** for this state: tests/build, pinned
creature inspection, core/gameplay dedicated-server smoke, gameplay client startup, both verification
JARs, mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-efb63e028f5db5f0a802ff499b4457a693dfd668`
(`11224598410`). Control/debuff/revive support callers are not fabricated here because their real
encounter-linked runtime paths are not yet present.

The next control-support pass is closed at code state
`6fe9862b489d154cb36c96464cd7d8ca5f2b0dd1` (bridge introduced at
`9565c9ff22070d52b9ff43980cd0033abb10c9aa`). It uses only concrete server-owned control that
already exists: Mage Phase Step's Weave field publishes one contribution for a target only when its
actual movement slow is accepted and is not suppressed by a stronger control, with same-field refresh
ticks deduplicated; Guardian `Provoked` publishes on the first accepted owner or an ownership change,
not on same-Guardian duration refreshes. The bridge routes only the exact Earthloong registry or the
exact legacy Nature Spirit registry `threateningly_mobs:nature_hamony`; Nature Spirit additionally
requires the accepted authored-spawn tag, so an invented `nature_spirit` ID or ordinary nearby mob
cannot create participation. This closes a real non-damaging control contribution path without
manufacturing a revive caller or granting party/proximity credit.

Build Openworld RPG run `37007134482` failed at `compileJava` only because the first bridge revision
called a non-existent Phase-field result accessor; code state
`6fe9862b489d154cb36c96464cd7d8ca5f2b0dd1` corrects that call to the existing
`suppressedByStrongerControl()` contract. Run `37007506464` is **SUCCESS** for the corrected state:
tests/build, pinned creature inspection, core/gameplay dedicated-server smoke, gameplay client startup,
both verification JARs, mrpack packaging and artifact upload all passed. Artifact:
`openworld-rpg-m0-6fe9862b489d154cb36c96464cd7d8ca5f2b0dd1`
(`11226493360`). This is automated runtime/build evidence, not an in-world Nature Spirit playtest;
production spawning remains closed by presentation/asset acceptance.

The following Living Shell authority pass is closed at code state
`ed91f8ff3af99db5a6d2372ed2bc6a175d2c8e17`. It moves the already-locked Nature Spirit
defensive rules out of design-only state and into the central server damage/poise pipeline: actual
post-mitigation canonical HP loss is retained for the rolling 80-tick trigger window; the next explicit
Nature Spirit decision may enter Living Shell at >=20% recent MaxHP damage or <=40% current poise;
while active direct damage taken is 0.65x and poise damage taken is 1.25x; project poise break ends the
shell immediately; only natural expiry preserves the controller's <=4.0-block forced Bloom Quake
handoff. The runtime remains exact-registry + `authored_spawn` gated, so donor natural spawns and raw
preview fixtures cannot accidentally gain production combat authority. Run `37016332713` is
**SUCCESS** across tests/build, pinned dependency inspection, core/gameplay server smoke, gameplay
client startup, both verification JARs, mrpack packaging and artifact upload. Artifact
`openworld-rpg-m0-ed91f8ff3af99db5a6d2372ed2bc6a175d2c8e17`
(`11230471423`, SHA-256
`a7c5d216631be9ef74eba8a245ec68c677b4d93a1dd28ff2c2991797b9ffda47`).
This closes Living Shell backend authority only; attack presentation and production spawn remain gated.

The next Nature Spirit attack-impact pass is closed at code state
`beed315775ae3589086d4a57ea49f22ef3cf817e`. A single project-owned impact authority now
accepts only the exact authored Nature Spirit and maps presentation-confirmed contacts to the
already-locked encounter contract: Rooted Swipe = 13% benchmark physical / medium guard pressure;
Earthen Ram = 22% / heavy guard pressure / player poise pressure 45; Bloom Quake = 25% /
unguardable / un-perfect-guardable / player poise pressure 45. It rejects Cave Centipede action ids,
donor natural spawns and raw preview fixtures. The binder does **not** invent frontal-arc geometry,
ram movement or quake-ring visuals; a later presentation layer must confirm the authored visible
contact and then call this authority. Run `37085239391` is **SUCCESS** across tests/build, pinned
dependency inspection, core/gameplay server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload. Artifact
`openworld-rpg-m0-beed315775ae3589086d4a57ea49f22ef3cf817e`
(`11260552298`, SHA-256
`fad3605f152aaf85654196d79362d08fe97eee851a5b0c67758acbe31f03fbee`).
This closes attack-result authority only; production spawn still waits on movement/telegraph/contact
presentation binding.

The Nature Spirit action-execution pass is additionally closed at code state
`c935da82645ed9b0d007ecb66ffd850b21ca5263`. A committed server action now owns the exact
timing window between decision and impact instead of allowing a future presentation callback to invoke
damage whenever it wants: Rooted Swipe impact = selection + 9 ticks; Earthen Ram = +15 ticks with
3.0-block committed-movement metadata and 17-tick recovery; Bloom Quake = +20 ticks with 4.0-block
area metadata and 18-tick recovery. A second decision is blocked while a committed action is busy,
pre-tell/late callbacks are rejected, and each target can be consumed only once for the committed
action counter while an area attack may still confirm multiple distinct players on its one impact
tick. The implementation does not invent the unresolved frontal-arc geometry, ram interpolation,
ground ring, VFX or donor animation. Run `37092771895` is **SUCCESS** across tests/build, pinned
dependency inspection, core/gameplay server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload. Artifact
`openworld-rpg-m0-c935da82645ed9b0d007ecb66ffd850b21ca5263`
(`11263037838`, SHA-256
`849d014d0a410dd618da52a012f8a933f3a24a07abd2cda46ac03b864f519572`).
Nature Spirit backend combat is now closed through action scheduling/impact/recovery; production spawn
still waits on the accepted visible movement/telegraph/contact binder.

Bloom Quake's canon-closed spatial authority is also bound at code state
`f920385919b7f7b752fa3e6ffea20b6cfc91e54b`: on the exact scheduled impact frame, eligible
encounter candidates are filtered against an exact 4.0-block horizontal radius, with the boundary
included and out-of-radius/dead/spectator/cross-level targets rejected. Candidate membership remains
owned by the encounter/presentation caller rather than inferred from raw proximity, preventing
accidental participation expansion. Accepted targets then pass through the existing per-action-counter
dedupe and Nature Spirit impact authority. Run `37093321135` is **SUCCESS** across tests/build,
pinned dependency inspection, core/gameplay server smoke, gameplay client startup, both verification
JARs, mrpack packaging and artifact upload. Artifact
`openworld-rpg-m0-f920385919b7f7b752fa3e6ffea20b6cfc91e54b`
(`11263683026`, SHA-256
`24de3a4a26d7dd6dbb10a954f2e9952d39457ca30dabfafaf6e0f451a8d566e3`).
The remaining Bloom Quake gate is player-facing presentation: its visible ring/VFX must match this
already-authoritative 4.0-block server area.

Steelboar selection is now bound at code state
`23c7fec95ed14c4f6ed6c80f774fa17ecac3b739`. A dedicated bundled controller owns the
exact R01 decision language instead of leaving donor AI to choose attacks: <=3.0-block melee uses
deterministic 60/40 Iron Tusk/Shoulder Hook weights with the 3.0 s Hook cooldown and two-repeat cap;
5.5–12.0 blocks with a clear committed line gives a ready 7.0 s-cooldown Iron Rush priority; the
3.0–5.5 gap repositions. Below 35% HP, a ready 14 s Furious Route deterministically replaces the next
legal Rush only at 6.0–12.0 blocks, records its >=0.60 s second pivot/tell and 1.40 s second-charge
recovery, and consumes the override cooldown at commitment even if a later second line cannot be
established. The data preserves one real canon gap rather than inventing it: Iron Rush explicitly has
`perfect_guardable: true`, heavy guard pressure and 1.40x perfect-guard poise reward, but the current
design text does not state ordinary `guardable`; that field remains unresolved/null and no Iron Rush
impact authority is fabricated yet. Run `37094461580` is **SUCCESS** across tests/build, pinned
dependency inspection, core/gameplay server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload. Artifact
`openworld-rpg-m0-23c7fec95ed14c4f6ed6c80f774fa17ecac3b739`
(`11262949211`, SHA-256
`f8645f5e15519b1b27ec594584a3efd5142afb61471e421eaad9d509c526d227`).
Steelboar production spawn/presentation and charge execution remain closed.

Steelboar's two fully specified melee impacts are now bound at code state
`7a4a06781ac9ceaecc21cff748a79464e5dbcf73`. Iron Tusk owns an exact 8-tick wind-up,
single impact frame and 7-tick recovery, resolving 13% same-Lv benchmark physical damage with medium
guard pressure and player-poise pressure 28. Shoulder Hook owns 12/impact/14 ticks and resolves 20%
physical with heavy guard pressure and player-poise pressure 45. The 28/45 poise values come from the
canonical shared player incoming stagger-pressure bands in `COMBAT_BALANCE.md`. Both attacks use the
central player defense/damage/poise runtimes, target-per-action-counter dedupe, exact impact-tick
validation and exact authored Steelboar gating. The runtime explicitly rejects Iron Rush/Furious Route
so their unresolved ordinary `guardable` contract cannot leak into damage execution. Run
`37095783113` is **SUCCESS** across tests/build, pinned dependency inspection, core/gameplay
server smoke, gameplay client startup, both verification JARs, mrpack packaging and artifact upload.
Artifact `openworld-rpg-m0-7a4a06781ac9ceaecc21cff748a79464e5dbcf73`
(`11264875569`, SHA-256
`22e67a3282418ecfa8af19fed35699a572da2b57ac778f068d43088c907a4eb8`).
Final melee arc presentation, Iron Rush/Furious Route execution and Steelboar production spawn remain
closed.

Regalhart selection/phase authority is now data-backed at code state
`1291553781db71966e14858ac84c8ac1d4ff4c4c`. The server controller owns rear-protection
priority, <=4.5-block Antler Sweep selection, exact Rear Kick/Crown Charge/Royal Bound cooldowns,
distance-band Crown Charge/Royal Bound weighting, deterministic RNG, signature-movement anti-repeat,
the one-time <=40% Sovereign transition and its full 30-tick action lock, Antler Sweep 3rd/2nd
follow-up counters and every-second Sovereign Crown Charge second-charge due marker. The controller
requires geometry legality from later binders instead of inventing rear cones, clear charge paths or
Royal Bound landing volumes. Two overlapped table endpoints—exactly 7.0 and 12.0 blocks—are explicitly
fail-closed until canon assigns them, rather than silently changing probability. The pass also removes
an invalid inferred Antler Sweep player-poise value and keeps the real impact gaps visible: Crown
Charge ordinary `guardable` and Rear Kick guard-pressure/player-poise are not currently stated.
Run `37096585105` is **SUCCESS** across tests/build, pinned dependency inspection, core/gameplay
server smoke, gameplay client startup, both verification JARs, mrpack packaging and artifact upload.
Artifact `openworld-rpg-m0-1291553781db71966e14858ac84c8ac1d4ff4c4c`
(`11264443479`, SHA-256
`90be1d6a68845b60a53c3de47d6868a91ea15b5f79f26a64153b33d469511890`).
Regalhart production spawn, weak-point geometry, Sovereign damage/movement application, attack
execution and final presentation remain closed.

The next Regalhart impact pass at code state
`2a182a9708f968e13751d0fe72927c2b19c27323` closes only the two attack results already fully
specified by canon. A non-combo Antler Sweep now owns its exact 9-tick tell / impact frame / 8-tick
recovery and resolves 11% same-Lv benchmark physical damage with medium guard pressure and normal
guard/perfect-guard handling; it does not invent any player-poise payload. If the deterministic
mirrored second sweep is due, execution fails closed until that second hit's exact timing is authored.
Royal Bound now has a server-owned confirmed-landing result—30% physical, unguardable,
un-perfect-guardable, exact 4.5-block horizontal radius—but no fake fixed landing time is derived from
the canon's >=1.10 s minimum leap tell. Crown Charge and Rear Kick remain impact-blocked because their
missing fields are real canon gaps. Run `37098727691` is **SUCCESS** across tests/build, pinned
dependency inspection, core/gameplay server smoke, gameplay client startup, both verification JARs,
mrpack packaging and artifact upload. Artifact
`openworld-rpg-m0-2a182a9708f968e13751d0fe72927c2b19c27323`
(`11265038798`, SHA-256
`74ee7d355b09b148f305a494905df29a6a2f9fc360348641a93740d740868576`).
This is backend impact closure only; accepted movement/animation geometry, Sovereign runtime modifiers,
weak-point geometry and production spawn are still separate gates.

The R01 world-binding runtime now also has a real, fail-closed promotion path. Candidate spatial data and gated Alderford compositions cannot leak live authority merely by flipping one entry: production access requires explicit source-level promotion plus the individual production binding. Once real client/asset review is complete, accepted runtime geometry can coexist with retained candidate review evidence instead of requiring review shells to be falsely relabeled as production. Build Openworld RPG run `36961511251` at code state `02c73c66f656bfee4aa8c77565bdeafe50c02c82` passes tests/build, R01 creature-surface inspection, core/gameplay server smoke, gameplay client startup, verification JARs and pack packaging. This closes the **promotion mechanism only**. No current Azari coordinate, Alderford composition/service/property, Quarry runtime volume/socket or Earthloong arena has been promoted; `R01 SPATIAL_BINDING COMPLETE` therefore remains `NO`.

This does **not** waive the remaining gates above. Broad player-facing R01 implementation still waits for exact asset/spatial closure. Narrow technical work that does not lock unresolved presentation may continue, but it must not create placeholders that later become de facto canon.

---

## 8. External-first admission contract

For important player-facing content, use this order:

```text
identify gameplay need
→ find/verify strong external model/design/code/reference
→ classify license / dependency / local-only / editable-base boundary
→ verify Minecraft-scale readability and animation coverage
→ lock final player-facing identity/name/role/stats
→ record binding
→ implement
```

Do not invent a long generic list first and hope matching art exists later.

This applies especially to:

- bosses/creatures/mounts;
- weapons/armor/outfits/accessories;
- ores/herbs/resource nodes;
- structures/workstations/important props;
- UI screens/icons/branding visuals;
- animation/VFX/audio.

### 8.1 No temporary player-facing design

The first production-facing implementation must already use the accepted external visual direction.

- UI is not first implemented as generic black panels/vanilla buttons and reskinned later;
- important actors are not first implemented as vanilla stand-ins and replaced later;
- signature attacks are not first presented as generic particle clouds and treated as acceptable until polish;
- settlements/dungeons are not first authored as temporary vanilla shells that quietly become permanent;
- placeholder icons/frames/models/VFX/SFX are not part of the player-facing production path.

If the exact external visual is unresolved, leave that visible slot gated and work on another closed unit. **Do not create temporary design debt merely to make a feature look implemented.**

External designs/assets may be adapted, recomposed, retargeted or integrated to fit the game's canon and technical constraints. This does not authorize improvised AI visual language that competes with the selected external art direction.

A missing exact source is a pre-code gate for that visible content, not permission for a vanilla/AI placeholder to become the final answer.

### 8.2 External-mod composition contract

External mods are treated as **components**, not as independent games that retain final authority inside Openworld RPG.

The canonical composition shape is:

```text
external runtime/content
→ project integration adapter / data overlay / semantic tags
→ project domain request/state
→ server-authoritative validation/transaction
→ project presentation sync
```

Project core code should not scatter donor implementation classes across combat, quests, loot, progression or world state.

For an external dependency, prefer in this order:

```text
documented public API
→ registry/resource ID
→ tag/datapack/data registry
→ published event/callback
→ supported config
→ narrow compatibility shim
→ donor-internal mixin/reflection only as a documented last resort
```

An accepted dependency actor/system gets explicit policy by dimension rather than a vague `use this mod` decision. The detailed policy vocabulary (`PASS_THROUGH / ADAPT / OVERRIDE / SUPPRESS / REFERENCE_ONLY`), overlay schema, integration-module contract, startup validation and server→client data-sync rules live in `M0_INTEGRATION_ARCHITECTURE.md`.

Invariants:

- external presentation/animation/AI primitives may be retained when they improve quality;
- project spawn/stat/damage/loot/progression/world-state rules override donor defaults where canon requires;
- no donor recipe/loot/worldgen/progression is accepted merely because the mod is installed;
- external actor changes should normally target registry IDs through project overlay data rather than editing/forking donor files;
- required integration failure is explicit during development, never silently replaced by a vanilla stand-in;
- optional integrations disable cleanly;
- one dependency update should normally require changes in its adapter/binding layer, not in unrelated project gameplay systems;
- the project does not install another complete RPG/class/economy/quest system merely to obtain one useful primitive.

The point of using many good mods is to reduce low-value reinvention while still shipping **one coherent game**.

---

## 9. Player-facing development-language prohibition

Production documents/code/logs may use internal identifiers.

Normal gameplay must never expose development/process language such as:

- `P0`, `P1`;
- `alpha`, `beta`;
- `prototype`, `temporary`, `placeholder`;
- `TODO`, `debug`, `developer`;
- internal milestone/test-stage names;
- asset-intake/license/hash notes;
- raw state/controller IDs.

This applies to UI, quests, dialogue, item descriptions, tutorials, loading text, system messages and localization.

A missing binding/localization is a content/build failure, not a player-facing warning.

---

## 10. Multiplayer / authority contract

Important state is server-authoritative, including:

- damage/hit acceptance;
- HP/resources/status/poise;
- item ownership and loot eligibility;
- Gold;
- EXP/Lv/Class XP;
- skill cost/success/cooldowns;
- class/progression;
- quests and evidence;
- world-state changes;
- mounts/major encounter controllers;
- party membership/leadership;
- save data and reward transactions.

Essential never replaces this authority model.

Personal/shared/encounter ownership follows `QUEST_WORLD_STATE.md`. Rewards must be idempotent. Support contribution must count where specified. Host ownership does not make the host the only legitimate story player.

### Co-op reward invariant

For a jointly defeated enemy or encounter:

```text
each eligible participating player receives their own normal personal EXP
+ their own normal Class XP for the qualifying active class
+ their own eligible personal loot/Gold roll
```

**EXP/Class XP are not divided by party size.** Each receiver uses their own Lv/Class Rank and the existing encounter-level anti-farm modifier. Last hit and party leader status have no reward ownership value.

Formal party membership alone never grants rewards; actual participation does. Detailed qualification/scaling/reconnect behavior lives in `PARTY_MULTIPLAYER.md`.

Do not label multiplayer successful until tested with real clients.

---

## 11. Performance / implementation cleanliness

Do not solve scale with hidden permanent tick cost.

Avoid:

- region-wide every-tick scans;
- huge permanent pathfinding populations;
- entity-heavy decoration where static/block composition works;
- full simulated ocean/aquifer/ecology networks;
- large VFX/Display Entity fields always running while unloaded/irrelevant.

Measure suspected hotspots with profiler/reproduction conditions.

After a replacement is verified at appropriate risk, remove dead/duplicate/prototype implementations and obsolete fallbacks. Keep compatibility/migration paths only when they have a live reason to exist.

---

## 12. Validation language

Use these states literally:

- `CODE REVIEWED`
- `TESTED`
- `BUILD VERIFIED`
- `JAR PRODUCED`
- `PLAYTESTED`
- `MULTIPLAYER TESTED`

A build does not prove combat feel, UI quality, traversal quality or multiplayer correctness.
