package io.github.q93503128.turnbound.combat;

import io.github.q93503128.turnbound.content.CanonicalData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Server-authoritative v0.4 battle interaction resolver. Numeric definitions remain data-driven. */
public final class BattleEngine {
    public static final long TURN_THRESHOLD = 1000L;
    public static final int MAX_REACTION_DEPTH = 3;
    public static final int MAX_REACTIONS_PER_ACTION = 3;
    private static final int NORMAL_GAUGE_REDUCTION_CAP = -500;
    private static final int BOSS_GAUGE_REDUCTION_CAP = -400;

    private final BattleState state;
    private final Deque<Reaction> reactions = new ArrayDeque<>();
    private final Set<String> emergencyHealingInProgress = new HashSet<>();
    private final Set<String> counteredThisAction = new HashSet<>();
    private final Set<String> lynetteTriggeredThisAction = new HashSet<>();
    private int reactionExecutionsThisAction;

    public BattleEngine(BattleState state) { this.state = Objects.requireNonNull(state); }
    public BattleState state() { return state; }

    public CombatantState nextReady() {
        if (state.outcome() != BattleOutcome.RUNNING) throw new IllegalStateException("Battle is over");
        if (state.currentActorId() != null) return state.combatant(state.currentActorId());
        List<CombatantState> living = state.combatants().stream().filter(c -> !c.downed()).toList();
        long pulses = living.stream().mapToLong(this::pulsesUntilReady).min().orElseThrow();
        if (pulses > 0) {
            for (CombatantState c : living) c.addGauge(pulses * c.speed());
            state.addLogicalPulse(pulses);
        }
        CombatantState ready = living.stream().filter(c -> c.gauge() >= TURN_THRESHOLD)
                .max(Comparator.comparingLong(CombatantState::gauge)
                        .thenComparingInt(CombatantState::speed)
                        .thenComparingInt(c -> -c.initiativeSeed()))
                .orElseThrow();
        state.setCurrentActorId(ready.instanceId());
        state.addEvent(new BattleEvent("TURN_READY", ready.instanceId(), ready.instanceId(),
                (int)Math.min(Integer.MAX_VALUE, ready.gauge()), "pulse=" + state.logicalPulse()));
        return ready;
    }

    private long pulsesUntilReady(CombatantState c) {
        if (c.gauge() >= TURN_THRESHOLD) return 0;
        long missing = TURN_THRESHOLD - c.gauge();
        return (missing + c.speed() - 1L) / c.speed();
    }

    public void useSkill(String actorId, String skillId, String... requestedTargetIds) {
        if (!actorId.equals(state.currentActorId())) throw new IllegalStateException("Not current actor: " + actorId);
        CombatantState actor = state.combatant(actorId);
        SkillDefinition skill = actor.definition().skill(skillId);
        if (actor.downed() || actor.cooldown(skillId) > 0) throw new IllegalStateException("Actor unavailable or skill on cooldown");
        if (actor.hasStatus("action_disable")) throw new IllegalStateException("Actor is action-disabled");
        if (actor.hasStatus("silence") && !skill.isBasic()) throw new IllegalStateException("Actor is silenced");

        List<CombatantState> targets = resolveTargets(actor, skill, requestedTargetIds);
        if (skill.hasRule("SELF_FORBIDDEN") && targets.contains(actor)) throw new IllegalArgumentException("Skill cannot target self");

        reactions.clear();
        reactionExecutionsThisAction = 0;
        counteredThisAction.clear();
        lynetteTriggeredThisAction.clear();

        // v0.4 commit boundary: once validation/target confirmation succeeds, Gauge and CD are committed
        // before Primary Effects, deaths, passives, reactions and phase transitions resolve.
        if (skill.cooldown() > 0) actor.setCooldown(skill.id(), skill.cooldown());
        actor.spendTurnGauge();
        state.addEvent(new BattleEvent("ACTION", actorId,
                String.join(",", targets.stream().map(CombatantState::instanceId).toList()), 0, skillId));

        boolean direct = skill.effects().stream().anyMatch(e -> e.type() == EffectType.DAMAGE);
        int focusBefore = actor.definition().id().equals("P01") ? actor.counter("focus") : 0;
        for (SkillEffect effect : skill.effects()) {
            applyEffect(actor, skill, targets, effect, focusBefore, direct);
            resolveReactions();
        }
        postRules(actor, skill, targets, direct, focusBefore);
        resolveReactions();

        tickCooldowns(actor, skill.id());
        resolveOwnerEndEffects(actor);
        afterRegularAction(actor, skill);
        actor.tickStatusesOnOwnTurn();
        triggerLumeaPassive(actor);
        processPendingMorwenReturns();
        refreshBossPackRules();
        state.setCurrentActorId(null);
        state.addEvent(new BattleEvent("TURN_END", actorId, actorId, 0, skillId));
    }

    private List<CombatantState> resolveTargets(CombatantState actor, SkillDefinition skill, String[] ids) {
        return switch (skill.targetRule()) {
            case SELF -> List.of(actor);
            case ALLY_ALL -> state.living(actor.side());
            case ENEMY_ALL -> state.living(actor.side().opposite());
            case ALLY_SINGLE -> List.of(single(ids, actor.side(), false, actor));
            case ENEMY_SINGLE -> List.of(single(ids, actor.side().opposite(), false, actor));
            case DEAD_ALLY_SINGLE -> List.of(single(ids, actor.side(), true, actor));
        };
    }

    private CombatantState single(String[] ids, CombatantSide side, boolean down, CombatantState actor) {
        if (ids.length != 1) throw new IllegalArgumentException("Exactly one target required");
        CombatantState target = state.combatant(ids[0]);
        if (target.side() != side || target.downed() != down) throw new IllegalArgumentException("Invalid target");
        StatusInstance taunt = actor.status("taunt");
        if (!down && side == actor.side().opposite() && taunt != null) {
            CombatantState taunter = state.find(taunt.sourceId());
            if (taunter != null && !taunter.downed() && taunter.side() == side && target != taunter) {
                throw new IllegalArgumentException("Taunt requires targeting " + taunter.instanceId());
            }
        }
        return target;
    }

    private void applyEffect(CombatantState actor, SkillDefinition skill, List<CombatantState> targets,
                             SkillEffect effect, int focusBefore, boolean direct) {
        switch (effect.type()) {
            case DAMAGE -> {
                if (targets.size() > 1) damageBatch(actor, skill, targets, effect.magnitude(), focusBefore, direct);
                else if (!targets.isEmpty()) damage(actor, targets.getFirst(),
                        effectivePotency(actor, skill, targets.getFirst(), effect.magnitude(), focusBefore), 0, direct, skill.id());
            }
            case HEAL -> {
                for (CombatantState target : targets) {
                    double potency = effect.magnitude();
                    if (actor.definition().id().equals("P04") && skill.id().equals("p04_resting_light") && target == actor) potency = 0.70;
                    int value = target.heal((int)Math.floor(actor.attack() * potency));
                    state.addEvent(new BattleEvent("HEAL", actor.instanceId(), target.instanceId(), value, skill.id()));
                }
            }
            case BARRIER_MAX_HP -> {
                for (CombatantState target : targets) {
                    int value = target.addBarrier((int)Math.floor(target.maxHp() * effect.magnitude()));
                    state.addEvent(new BattleEvent("BARRIER", actor.instanceId(), target.instanceId(), value, skill.id()));
                }
            }
            case GAUGE_ADD -> { for (CombatantState target : targets) applyGauge(actor, target, effect.flatValue(), skill.id()); }
            case SELF_GAUGE_ADD -> applyGauge(actor, actor, effect.flatValue(), skill.id());
            case GAUGE_AT_LEAST -> {
                for (CombatantState target : targets) {
                    long before = target.gauge();
                    target.setGauge(Math.max(target.gauge(), effect.flatValue()));
                    state.addEvent(new BattleEvent("GAUGE", actor.instanceId(), target.instanceId(), (int)(target.gauge() - before), skill.id()));
                }
            }
            case GUARD_REDIRECT -> {
                CombatantState target = targets.getFirst();
                if (target == actor) throw new IllegalArgumentException("Guard requires another ally");
                putTimedStatus(target, "guard_redirect", actor.instanceId(), effect.duration(), effect.magnitude(), actor);
                state.addEvent(new BattleEvent("STATUS", actor.instanceId(), target.instanceId(), effect.duration(), "guard_redirect"));
            }
            case DEFENSE_UP -> putModifier(actor, targets, "defense_multiplier", effect);
            case ATTACK_MOD -> putModifier(actor, targets, "attack_multiplier", effect);
            case DEFENSE_MOD -> putModifier(actor, targets, "defense_multiplier", effect);
            case SPEED_MOD -> putModifier(actor, targets, "speed_multiplier", effect);
            case DAMAGE_REDUCTION -> putModifier(actor, targets, "damage_reduction", effect);
            case DAMAGE_TAKEN_MOD -> putModifier(actor, targets, "damage_taken_multiplier", effect);
            case DOT_MAX_HP -> {
                for (CombatantState target : targets) {
                    putTimedStatus(target, "dot_max_hp", actor.instanceId(), effect.duration(), effect.magnitude(), actor);
                    state.addEvent(new BattleEvent("STATUS", actor.instanceId(), target.instanceId(), effect.duration(), "dot_max_hp"));
                }
            }
            case SELF_HP_COST -> {
                if (actor.definition().id().equals("P08") && skill.id().equals("p08_blood_charge") && actor.flag("p08_next_blood_free")) {
                    actor.clearFlag("p08_next_blood_free");
                    state.addEvent(new BattleEvent("HP_COST", actor.instanceId(), actor.instanceId(), 0, "P08_AWAKEN_FREE"));
                } else {
                    int cost = actor.spendCurrentHp(effect.magnitude());
                    state.addEvent(new BattleEvent("HP_COST", actor.instanceId(), actor.instanceId(), cost, skill.id()));
                }
            }
            case STATUS_MARK -> {
                for (CombatantState target : targets) {
                    String key = effect.key().isBlank() ? "mark" : effect.key();
                    if (target.definition().boss() && key.equalsIgnoreCase("ACTION_DISABLE")) {
                        applyGauge(actor, target, -350, "BOSS_CC_CONVERSION");
                    } else {
                        putTimedStatus(target, key.toLowerCase(), actor.instanceId(), effect.duration(), effect.magnitude(), actor);
                        state.addEvent(new BattleEvent("STATUS", actor.instanceId(), target.instanceId(), effect.duration(), key.toLowerCase()));
                    }
                }
            }
            case STATUS_CLEAR -> {
                for (CombatantState target : targets) {
                    if (effect.key().isBlank()) continue;
                    target.removeStatus(effect.key().toLowerCase(), actor.instanceId());
                    state.addEvent(new BattleEvent("STATUS_CLEAR", actor.instanceId(), target.instanceId(), 0, effect.key().toLowerCase()));
                }
            }
            case REVIVE -> {
                CombatantState target = targets.getFirst();
                int hp = target.revive(effect.magnitude());
                target.setCounter("p06_return_wait", 0);
                target.clearFlag("p06_return_wait_new");
                if (actor.definition().id().equals("P04") && actor.definition().hasRule("AWAKENED")) {
                    int gauge = actor.definition().intParam("awakenReviveGauge", 150);
                    target.addGauge(gauge);
                    target.putStatus(new StatusInstance("damage_reduction", actor.instanceId(), 1,
                            actor.definition().param("awakenReviveDr", 0.20)));
                }
                state.addEvent(new BattleEvent("REVIVE", actor.instanceId(), target.instanceId(), hp, skill.id()));
            }
            case NOOP -> { }
        }
    }

    private void putModifier(CombatantState actor, List<CombatantState> targets, String statusId, SkillEffect effect) {
        for (CombatantState target : targets) {
            putTimedStatus(target, statusId, actor.instanceId(), effect.duration(), effect.magnitude(), actor);
            state.addEvent(new BattleEvent("STATUS", actor.instanceId(), target.instanceId(), effect.duration(), statusId));
        }
    }

    private void putTimedStatus(CombatantState target, String id, String sourceId, int declaredTurns,
                                double magnitude, CombatantState currentActor) {
        int storedTurns = Math.max(1, declaredTurns + (target == currentActor ? 1 : 0));
        target.putStatus(new StatusInstance(id, sourceId, storedTurns, magnitude));
    }

    private void applyGauge(CombatantState source, CombatantState target, int requested, String detail) {
        int amount = requested;
        if (amount < 0) amount = Math.max(target.definition().boss() ? BOSS_GAUGE_REDUCTION_CAP : NORMAL_GAUGE_REDUCTION_CAP, amount);
        long before = target.gauge();
        target.addGauge(amount);
        state.addEvent(new BattleEvent("GAUGE", source.instanceId(), target.instanceId(), (int)(target.gauge() - before), detail));
    }

    private double effectivePotency(CombatantState actor, SkillDefinition skill, CombatantState target, double base, int focusBefore) {
        double potency = base;
        String id = actor.definition().id();
        if (id.equals("P01")) {
            potency *= 1.0 + actor.definition().param("focusDamagePer", 0.15) * focusBefore;
            if (skill.id().equals("p01_breaker_strike")) potency *= 1.0 + skill.param("focusBonusPer", 0.10) * focusBefore;
        } else if (id.equals("P05") && skill.id().equals("p05_piercing_shot")) {
            potency += skill.param("exposureBonus", 0.30) * exposure(target, actor);
        } else if (id.equals("P06")) {
            if (skill.id().equals("p06_echo")) potency += skill.param("memoryDamagePer", 0.05) * actor.counter("memory");
            if (skill.id().equals("p06_condolence") && actor.flag("p06_ally_death")) potency = skill.param("allyDeathPotency", 2.50);
            if (skill.id().equals("p06_funeral_order") && target.hp() / (double)target.maxHp() <= skill.param("threshold", 0.25)) potency = skill.param("executePotency", 2.10);
            if (actor.flag("p06_return_first_direct")) potency *= 1.20;
        } else if (id.equals("P07")) {
            if (skill.id().equals("p07_command") && livingP07Summon(actor) != null) potency = 0.0;
            if (skill.id().equals("p07_joint_attack") && livingP07Summon(actor) == null) potency = skill.param("soloPotency", 1.35);
            if (actor.flag("p07_next_direct_bonus")) potency *= 1.30;
        } else if (id.equals("P08") && skill.id().equals("p08_frenzy")) {
            double ratio = actor.hp() / (double)actor.maxHp();
            if (ratio <= actor.definition().param("lowThreshold", 0.30)) potency = actor.definition().param("lowBasic", 1.35);
            else if (ratio <= actor.definition().param("midThreshold", 0.50)) potency = actor.definition().param("midBasic", 1.15);
            else potency = 0.95;
        } else if (id.equals("E012") && target.gauge() < actor.gauge()) {
            potency *= 1.0 + actor.definition().param("bonus", 0.15);
        } else if (id.equals("EL02") && target.instanceId().equals(actor.ref("el02_last_target"))) {
            potency *= 1.0 + actor.definition().param("repeatBonus", 0.20);
        } else if (id.equals("B05") && target.status("serak_mark", actor.instanceId()) != null) {
            potency *= 1.30;
        }
        return potency;
    }

    private void damage(CombatantState actor, CombatantState target, double potency, int depth, boolean direct, String detail) {
        if (target.downed() || potency <= 0) return;
        int raw = calculateDamage(actor.attack(), target.defense(), potency);
        applySingleDamage(actor, target, raw, depth, direct, detail);
        consumeAttackFlags(actor, target, direct);
    }

    private void damageBatch(CombatantState actor, SkillDefinition skill, List<CombatantState> targets,
                             double basePotency, int focusBefore, boolean direct) {
        List<DamagePlan> plans = new ArrayList<>();
        for (CombatantState target : targets) {
            if (target.downed()) continue;
            double potency = effectivePotency(actor, skill, target, basePotency, focusBefore);
            int raw = calculateDamage(actor.attack(), target.defense(), potency);
            plans.add(new DamagePlan(target, adjustedIncoming(target, raw), potency));
        }
        List<DamageApplied> applied = new ArrayList<>();
        for (DamagePlan plan : plans) applied.add(applyNoTriggers(plan.target(), plan.amount()));
        for (int i = 0; i < applied.size(); i++) {
            DamageApplied result = applied.get(i);
            state.addEvent(new BattleEvent("DAMAGE", actor.instanceId(), result.target().instanceId(), result.hpLost(),
                    skill.id() + ":potency=" + plans.get(i).potency()));
        }
        for (DamageApplied result : applied) resolveApplied(actor, result, 0, direct, skill.id());
        for (CombatantState target : targets) consumeB05Mark(actor, target);
        if (direct && actor.flag("p06_return_first_direct")) actor.clearFlag("p06_return_first_direct");
        if (direct && actor.flag("p07_next_direct_bonus")) actor.clearFlag("p07_next_direct_bonus");
    }

    private void applySingleDamage(CombatantState actor, CombatantState target, int raw, int depth, boolean direct, String detail) {
        int adjusted = adjustedIncoming(target, raw);
        StatusInstance guard = target.status("guard_redirect");
        if (guard != null && direct) {
            CombatantState guardian = state.find(guard.sourceId());
            if (guardian != null && !guardian.downed()) {
                int redirected = (int)Math.floor(adjusted * guard.magnitude());
                adjusted -= redirected;
                DamageApplied redirectedResult = applyNoTriggers(guardian, redirected);
                resolveApplied(actor, redirectedResult, depth, direct, "redirect:" + target.instanceId());
                state.addEvent(new BattleEvent("DAMAGE_REDIRECT", actor.instanceId(), guardian.instanceId(), redirectedResult.hpLost(), target.instanceId()));
                if (guardian.definition().id().equals("P03") && guardian.definition().hasRule("AWAKENED") && redirectedResult.hpLost() > 0) {
                    int barrier = guardian.addBarrier((int)Math.floor(redirectedResult.hpLost() * guardian.definition().param("awakenRedirectBarrierRatio", 0.20)));
                    state.addEvent(new BattleEvent("BARRIER", guardian.instanceId(), guardian.instanceId(), barrier, "P03_AWAKEN_REDIRECT"));
                }
            }
        }
        DamageApplied result = applyNoTriggers(target, adjusted);
        state.addEvent(new BattleEvent("DAMAGE", actor.instanceId(), target.instanceId(), result.hpLost(), detail));
        resolveApplied(actor, result, depth, direct, detail);
    }

    private DamageApplied applyNoTriggers(CombatantState target, int amount) {
        int barrierBefore = target.barrier();
        boolean downBefore = target.downed();
        int hpLost = target.takeDamage(Math.max(0, amount));
        return new DamageApplied(target, hpLost, barrierBefore, barrierBefore > 0 && target.barrier() == 0,
                !downBefore && target.downed());
    }

    private void resolveApplied(CombatantState attacker, DamageApplied result, int depth, boolean direct, String detail) {
        CombatantState target = result.target();
        afterHit(attacker, target, result.hpLost(), result.barrierBefore(), result.barrierBroke(), depth, direct);
        if (result.newlyDowned()) onDown(attacker, target, detail);
        if (target.flag("p08_lethal_just_triggered")) {
            target.clearFlag("p08_lethal_just_triggered");
            state.addEvent(new BattleEvent("LETHAL_SURVIVE", target.instanceId(), target.instanceId(), 1, "P08_AWAKEN"));
        }
    }

    private void consumeAttackFlags(CombatantState actor, CombatantState target, boolean direct) {
        consumeB05Mark(actor, target);
        if (direct && actor.flag("p06_return_first_direct")) actor.clearFlag("p06_return_first_direct");
        if (direct && actor.flag("p07_next_direct_bonus")) actor.clearFlag("p07_next_direct_bonus");
    }

    private void consumeB05Mark(CombatantState actor, CombatantState target) {
        if (actor.definition().id().equals("B05") && target.status("serak_mark", actor.instanceId()) != null) {
            target.removeStatus("serak_mark", actor.instanceId());
        }
    }

    private int adjustedIncoming(CombatantState target, int raw) {
        return Math.max(1, (int)Math.floor(raw * (1.0 - target.damageReduction()) * (1.0 + target.damageTakenModifier())));
    }

    private void afterHit(CombatantState attacker, CombatantState target, int hpLost, int barrierBefore,
                          boolean barrierBroke, int depth, boolean direct) {
        triggerEnemyThresholdPassives(target);
        triggerBossPhases(target);
        if (barrierBroke && target.definition().id().equals("B03")) applyGauge(target, target, -180, "B03_BARRIER_BREAK");
        if (direct && barrierBefore > 0 && target.definition().id().equals("EL03") && !target.downed()
                && counteredThisAction.add("EL03:" + target.instanceId())) {
            reactions.addLast(new Reaction(target.instanceId(), attacker.instanceId(), target.definition().param("counterPotency", 0.45), "EL03_BARRIER_COUNTER", 1));
        }
        if (direct && hpLost > 0 && target.definition().id().equals("P03") && !target.downed()
                && counteredThisAction.add("P03:" + target.instanceId())) {
            reactions.addLast(new Reaction(target.instanceId(), attacker.instanceId(), target.definition().param("counterPotency", 0.65), "P03_COUNTER", 1));
            if (target.definition().hasRule("AWAKENED")) {
                int gauge = target.definition().intParam("awakenCounterGauge", 50);
                target.addGauge(gauge);
                state.addEvent(new BattleEvent("PASSIVE_GAUGE", target.instanceId(), target.instanceId(), gauge, "P03_COUNTER"));
            }
        }
        if (direct && attacker.side() == CombatantSide.ALLY && target.side() == CombatantSide.ENEMY) triggerLynetteFollowups(attacker, target);
        triggerElysia(target);
        refreshBossPackRules();
    }

    private void onDown(CombatantState attacker, CombatantState target, String detail) {
        state.addEvent(new BattleEvent("DOWN", attacker == null ? "" : attacker.instanceId(), target.instanceId(), 0, detail));
        recordMorwenMemory(target);
        for (CombatantState el01 : state.living(target.side())) {
            if (el01 != target && el01.definition().id().equals("EL01") && !target.definition().boss()) {
                int gauge = el01.definition().intParam("subordinateDeathGauge", 100);
                el01.addGauge(gauge);
                state.addEvent(new BattleEvent("PASSIVE_GAUGE", el01.instanceId(), el01.instanceId(), gauge, "EL01_SUBORDINATE_DEATH"));
            }
        }
        if (target.definition().id().equals("P06") && !target.flag("p06_return_used")) {
            target.setFlag("p06_return_used");
            target.setFlag("p06_return_wait_new");
            target.setCounter("p06_return_wait", target.definition().intParam("returnDelayActions", 2));
            state.addEvent(new BattleEvent("RETURN_WAIT", target.instanceId(), target.instanceId(), target.counter("p06_return_wait"), "P06_LAST_PAGE"));
        }
        if (target.definition().summon()) {
            CombatantState owner = p07Owner(target.side());
            if (owner != null) {
                int gauge = owner.definition().intParam("summonDeathGauge", 300);
                owner.addGauge(gauge);
                owner.setFlag("p07_next_direct_bonus");
                if (owner.definition().hasRule("AWAKENED") && !owner.flag("p07_awaken_resummon_used")) {
                    owner.setFlag("p07_awaken_resummon_used");
                    owner.setFlag("p07_awaken_resummon_pending");
                }
                state.addEvent(new BattleEvent("SUMMON_DOWN", target.instanceId(), owner.instanceId(), gauge, "P07_CONTRACT"));
            }
            state.removeCombatant(target.instanceId());
        }
        if (target.definition().id().equals("P07")) dismissP07Summon(target, false);
    }

    private void recordMorwenMemory(CombatantState deceased) {
        if (deceased.definition().summon() || deceased.flag("p06_memory_recorded")) return;
        deceased.setFlag("p06_memory_recorded");
        for (CombatantState morwen : state.combatants().stream().filter(c -> c.definition().id().equals("P06")).toList()) {
            morwen.incrementCounter("memory", 1, morwen.definition().intParam("memoryMax", 5));
            if (deceased.side() == morwen.side()) morwen.setFlag("p06_ally_death");
            state.addEvent(new BattleEvent("MEMORY", morwen.instanceId(), deceased.instanceId(), morwen.counter("memory"), "P06_MEMORY"));
        }
    }

    private void triggerLynetteFollowups(CombatantState attacker, CombatantState target) {
        for (CombatantState lynette : state.living(attacker.side())) {
            if (!lynette.definition().id().equals("P05") || lynette == attacker) continue;
            int exposed = exposure(target, lynette);
            if (exposed <= 0 || lynette.counter("p05_followups") >= lynette.definition().intParam("followUpLimit", 2)) continue;
            String guardKey = lynette.instanceId() + ":" + target.instanceId();
            if (!lynetteTriggeredThisAction.add(guardKey)) continue;
            double potency = lynette.definition().param("followUpPotency", 0.45);
            if (lynette.definition().hasRule("AWAKENED") && exposed >= 2) potency += lynette.definition().param("awakenExposure2Bonus", 0.15);
            if (target.status("hunting_target", lynette.instanceId()) != null) potency *= 1.0 + lynette.definition().param("huntFollowupBonus", 0.20);
            lynette.incrementCounter("p05_followups", 1, lynette.definition().intParam("followUpLimit", 2));
            if (lynette.definition().hasRule("AWAKENED") && lynette.counter("p05_followups") == 2) {
                int gauge = lynette.definition().intParam("awakenSecondGauge", 80);
                lynette.addGauge(gauge);
                state.addEvent(new BattleEvent("PASSIVE_GAUGE", lynette.instanceId(), lynette.instanceId(), gauge, "P05_SECOND_FOLLOW_UP"));
            }
            reactions.addLast(new Reaction(lynette.instanceId(), target.instanceId(), potency, "P05_FOLLOW_UP", 1));
        }
    }

    private int exposure(CombatantState target, CombatantState lynette) {
        StatusInstance exposed = target.status("exposed", lynette.instanceId());
        return exposed == null ? 0 : exposed.stacks();
    }

    private void resolveReactions() {
        while (!reactions.isEmpty() && reactionExecutionsThisAction < MAX_REACTIONS_PER_ACTION) {
            Reaction reaction = reactions.removeFirst();
            if (reaction.depth() > MAX_REACTION_DEPTH) continue;
            CombatantState source = state.find(reaction.sourceId());
            CombatantState target = state.find(reaction.targetId());
            if (source == null || target == null || source.downed() || target.downed()) continue;
            reactionExecutionsThisAction++;
            int raw = calculateDamage(source.attack(), target.defense(), reaction.potency());
            DamageApplied result = applyNoTriggers(target, adjustedIncoming(target, raw));
            state.addEvent(new BattleEvent("REACTION_DAMAGE", source.instanceId(), target.instanceId(), result.hpLost(), reaction.type()));
            resolveApplied(source, result, reaction.depth(), false, reaction.type());
        }
        if (reactionExecutionsThisAction >= MAX_REACTIONS_PER_ACTION) reactions.clear();
    }

    private void postRules(CombatantState actor, SkillDefinition skill, List<CombatantState> targets, boolean direct, int focusBefore) {
        String id = actor.definition().id();
        if (id.equals("E003") && skill.id().equals("e003_explode")) {
            actor.removeStatus("e003_armed");
            forceDown(actor, actor, "e003_self_explosion");
        }
        if (id.equals("P01")) postKyren(actor, skill, targets, direct, focusBefore);
        else if (id.equals("P02")) postLumea(actor, skill, targets);
        else if (id.equals("P05")) postLynette(actor, skill, targets);
        else if (id.equals("P06") && skill.id().equals("p06_funeral_order") && !targets.isEmpty() && targets.getFirst().downed()) {
            int gauge = skill.intParam("killGauge", 200); actor.addGauge(gauge);
            state.addEvent(new BattleEvent("GAUGE", actor.instanceId(), actor.instanceId(), gauge, "P06_EXECUTE_KILL"));
        } else if (id.equals("P07")) postMarion(actor, skill, targets);
        else if (id.equals("P08") && skill.id().equals("p08_blood_charge")
                && actor.hp() / (double)actor.maxHp() <= actor.definition().param("lowThreshold", 0.30)) {
            int gauge = actor.definition().intParam("bloodLowGauge", 180); actor.addGauge(gauge);
            state.addEvent(new BattleEvent("GAUGE", actor.instanceId(), actor.instanceId(), gauge, "P08_BLOOD_LOW"));
        } else if (id.equals("F01") && actor.definition().hasRule("AWAKENED") && skill.id().equals("f01_wood_sword") && !targets.isEmpty()) {
            reactions.addLast(new Reaction(actor.instanceId(), targets.getFirst().instanceId(), actor.definition().param("awakenExtraHit", 0.15), "F01_AWAKEN", 1));
        } else if (id.equals("F02") && actor.definition().hasRule("AWAKENED") && skill.id().equals("f02_first_aid") && !targets.isEmpty()) {
            CombatantState target = targets.getFirst(); int healed = target.heal((int)Math.floor(target.maxHp() * actor.definition().param("awakenMaxHpHeal", 0.03)));
            state.addEvent(new BattleEvent("HEAL", actor.instanceId(), target.instanceId(), healed, "F02_AWAKEN"));
        } else if (id.equals("F03") && actor.definition().hasRule("AWAKENED") && skill.id().equals("f03_focus_shot")) {
            int gauge = actor.definition().intParam("awakenFocusGauge", 80); actor.addGauge(gauge);
            state.addEvent(new BattleEvent("GAUGE", actor.instanceId(), actor.instanceId(), gauge, "F03_AWAKEN"));
        } else if (id.equals("F04") && actor.definition().hasRule("AWAKENED") && skill.id().equals("f04_endure")) {
            int barrier = actor.addBarrier((int)Math.floor(actor.maxHp() * actor.definition().param("awakenBarrier", 0.05)));
            state.addEvent(new BattleEvent("BARRIER", actor.instanceId(), actor.instanceId(), barrier, "F04_AWAKEN"));
        }
        postEnemyRules(actor, skill, targets);
        resolveReactions();
    }

    private void postKyren(CombatantState actor, SkillDefinition skill, List<CombatantState> targets, boolean direct, int focusBefore) {
        if (skill.id().equals("p01_duel_lock")) {
            CombatantState target = targets.getFirst();
            if (!target.instanceId().equals(actor.ref("focusTarget"))) { actor.setRef("focusTarget", target.instanceId()); actor.setCounter("focus", 0); }
            actor.incrementCounter("focus", 1, actor.definition().intParam("focusMax", 3));
            return;
        }
        if (!direct || targets.size() != 1) return;
        CombatantState target = targets.getFirst();
        if (target.instanceId().equals(actor.ref("focusTarget"))) actor.incrementCounter("focus", 1, actor.definition().intParam("focusMax", 3));
        else { actor.setRef("focusTarget", target.instanceId()); actor.setCounter("focus", actor.flag("p01_carry_focus") ? 1 : 0); actor.clearFlag("p01_carry_focus"); }
        if (actor.definition().hasRule("AWAKENED") && skill.id().equals("p01_chase_slash") && focusBefore >= 3 && !target.downed()) {
            reactions.addLast(new Reaction(actor.instanceId(), target.instanceId(), actor.definition().param("awakenBasicFollowup", 0.45), "P01_AWAKEN_FOLLOWUP", 1));
        }
        if (target.downed()) {
            if (actor.definition().hasRule("AWAKENED") && focusBefore >= 3) actor.setFlag("p01_carry_focus");
            actor.setRef("focusTarget", null); actor.setCounter("focus", 0);
        }
    }

    private void postLumea(CombatantState actor, SkillDefinition skill, List<CombatantState> targets) {
        if (!actor.definition().hasRule("AWAKENED") || (!skill.id().equals("p02_accelerate") && !skill.id().equals("p02_time_leap"))) return;
        for (CombatantState target : targets) {
            int turns = target == actor ? 2 : 1;
            target.putStatus(new StatusInstance("time_echo", actor.instanceId(), turns, actor.definition().intParam("awakenEchoGauge", 100)));
            state.addEvent(new BattleEvent("STATUS", actor.instanceId(), target.instanceId(), turns, "time_echo"));
        }
    }

    private void postLynette(CombatantState actor, SkillDefinition skill, List<CombatantState> targets) {
        if (targets.isEmpty()) return;
        CombatantState target = targets.getFirst();
        if (skill.id().equals("p05_suppressive_shot")) target.addStatusStack("exposed", actor.instanceId(), 999, 0.0, 1, actor.definition().intParam("exposureMax", 2));
        else if (skill.id().equals("p05_piercing_shot")) target.removeStatus("exposed", actor.instanceId());
        else if (skill.id().equals("p05_hunt_signal")) {
            target.removeStatus("exposed", actor.instanceId());
            int max = actor.definition().intParam("exposureMax", 2);
            target.addStatusStack("exposed", actor.instanceId(), 999, 0.0, max, max);
            target.putStatus(new StatusInstance("hunting_target", actor.instanceId(), 999, 0.0));
            actor.setRef("p05_hunt_target", target.instanceId());
            actor.setCounter("p05_hunt_actions", 2);
        }
    }

    private void postMarion(CombatantState actor, SkillDefinition skill, List<CombatantState> targets) {
        CombatantState summon = livingP07Summon(actor);
        if (skill.id().equals("p07_command")) {
            if (summon != null && !targets.isEmpty()) reactions.addLast(new Reaction(summon.instanceId(), targets.getFirst().instanceId(), skill.param("summonPotency", 0.70), "P07_COMMAND", 1));
            else {
                actor.incrementCounter("contract_prep", 1, actor.definition().intParam("prepMax", 2));
                if (actor.counter("contract_prep") >= actor.definition().intParam("prepMax", 2)) actor.setCooldown("p07_summon_toto", Math.max(0, actor.cooldown("p07_summon_toto") - 1));
            }
        } else if (skill.id().equals("p07_summon_toto")) {
            if (summon != null) throw new IllegalStateException("Contract beast already exists");
            actor.clearFlag("p07_awaken_resummon_pending");
            spawnP07Summon(actor, 1.0);
            actor.setCounter("contract_prep", 0);
        } else if (skill.id().equals("p07_joint_attack") && summon != null && !targets.isEmpty()) {
            reactions.addLast(new Reaction(summon.instanceId(), targets.getFirst().instanceId(), skill.param("summonPotency", 1.00), "P07_JOINT", 1));
        }
    }

    private void postEnemyRules(CombatantState actor, SkillDefinition skill, List<CombatantState> targets) {
        String id = actor.definition().id();
        if (id.equals("EL02") && !targets.isEmpty()) actor.setRef("el02_last_target", targets.getFirst().instanceId());
        if (id.equals("B01") && skill.id().equals("b01_charge")) actor.removeStatus("b01_charge_warning");
        if (id.equals("B02") && skill.id().equals("b02_summon")) spawnBossAdd(actor, "E007");
        if (id.equals("B03") && actor.flag("b03_phase3")) {
            if (skill.id().equals("b03_overclock")) actor.setCounter("b03_overclock_count", 0);
            else actor.incrementCounter("b03_overclock_count", 1, 2);
        }
        if (id.equals("B04")) {
            if (skill.id().equals("b04_warn")) actor.setFlag("b04_eruption_ready");
            if (skill.id().equals("b04_eruption")) { actor.clearFlag("b04_eruption_ready"); actor.removeStatus("b04_eruption_warning"); }
        }
        if (id.equals("B05")) {
            if (skill.id().equals("b05_order_collapse")) collapseNextAllies(actor, 2, -140);
            if (skill.id().equals("b05_warn")) actor.setFlag("b05_relay_ready");
            if (skill.id().equals("b05_relay_collapse")) { actor.clearFlag("b05_relay_ready"); actor.removeStatus("b05_collapse_warning"); }
        }
    }

    private void afterRegularAction(CombatantState actor, SkillDefinition skill) {
        if (actor.definition().id().equals("P05")) actor.setCounter("p05_followups", 0);
        if (actor.side() == CombatantSide.ALLY) {
            for (CombatantState lynette : state.living(CombatantSide.ALLY)) {
                if (!lynette.definition().id().equals("P05") || lynette.counter("p05_hunt_actions") <= 0) continue;
                if (lynette == actor && skill.id().equals("p05_hunt_signal")) continue;
                lynette.setCounter("p05_hunt_actions", lynette.counter("p05_hunt_actions") - 1);
                if (lynette.counter("p05_hunt_actions") <= 0) {
                    CombatantState hunted = state.find(lynette.ref("p05_hunt_target"));
                    if (hunted != null) hunted.removeStatus("hunting_target", lynette.instanceId());
                    lynette.setRef("p05_hunt_target", null);
                }
            }
        }
        if (actor.definition().id().equals("P07") && actor.flag("p07_awaken_resummon_pending") && livingP07Summon(actor) == null && !actor.downed()) {
            actor.clearFlag("p07_awaken_resummon_pending");
            spawnP07Summon(actor, actor.definition().param("awakenResummonHpRatio", 0.50));
        }
    }

    private void resolveOwnerEndEffects(CombatantState actor) {
        for (StatusInstance status : List.copyOf(actor.statusesView().values())) {
            if (status.id().equals("dot_max_hp")) {
                double ratio = actor.definition().boss() ? Math.min(0.02, status.magnitude()) : status.magnitude();
                CombatantState source = state.find(status.sourceId());
                int amount = Math.max(1, (int)Math.floor(actor.maxHp() * ratio));
                DamageApplied result = applyNoTriggers(actor, adjustedIncoming(actor, amount));
                state.addEvent(new BattleEvent("DOT", status.sourceId(), actor.instanceId(), result.hpLost(), "DOT_MAX_HP"));
                resolveApplied(source == null ? actor : source, result, 0, false, "DOT_MAX_HP");
                if (actor.downed()) break;
            } else if (status.id().equals("time_echo") && status.remainingOwnerTurns() <= 1) {
                int gauge = Math.max(0, (int)Math.round(status.magnitude())); actor.addGauge(gauge);
                actor.removeStatus("time_echo", status.sourceId());
                state.addEvent(new BattleEvent("PASSIVE_GAUGE", status.sourceId(), actor.instanceId(), gauge, "P02_TIME_ECHO"));
            }
        }
    }

    private void processPendingMorwenReturns() {
        for (CombatantState morwen : state.combatants().stream().filter(c -> c.definition().id().equals("P06") && c.downed()).toList()) {
            int wait = morwen.counter("p06_return_wait");
            if (wait <= 0) continue;
            // The action that caused P06's death does not count toward the two required other-unit actions.
            if (morwen.flag("p06_return_wait_new")) {
                morwen.clearFlag("p06_return_wait_new");
                continue;
            }
            morwen.setCounter("p06_return_wait", wait - 1);
            if (morwen.counter("p06_return_wait") > 0) continue;
            int hp = morwen.revive(morwen.definition().param("returnHp", 0.35));
            if (morwen.definition().hasRule("AWAKENED")) {
                morwen.incrementCounter("memory", morwen.definition().intParam("awakenReturnMemory", 2), morwen.definition().intParam("memoryMax", 5));
                morwen.addGauge(morwen.definition().intParam("awakenReturnGauge", 500));
                morwen.setFlag("p06_return_first_direct");
            }
            state.addEvent(new BattleEvent("SELF_REVIVE", morwen.instanceId(), morwen.instanceId(), hp, "P06_LAST_PAGE"));
        }
    }

    private void triggerLumeaPassive(CombatantState actor) {
        for (CombatantState lumea : state.living(actor.side())) {
            if (lumea != actor && lumea.definition().id().equals("P02") && actor.speed() < lumea.speed()) {
                int gauge = lumea.definition().intParam("slowAllyTurnGauge", 60); lumea.addGauge(gauge);
                state.addEvent(new BattleEvent("PASSIVE_GAUGE", lumea.instanceId(), lumea.instanceId(), gauge, "P02_WAIT_FOR_SLOW"));
            }
        }
    }

    private void triggerElysia(CombatantState hurt) {
        if (hurt.downed() || hurt.hp() * 100 > hurt.maxHp() * 30) return;
        for (CombatantState elysia : state.living(hurt.side())) {
            if (!elysia.definition().id().equals("P04")) continue;
            String flag = "p04_emergency:" + hurt.instanceId();
            if (elysia.flag(flag) || !emergencyHealingInProgress.add(hurt.instanceId())) continue;
            elysia.setFlag(flag);
            int healed = hurt.heal((int)Math.floor(elysia.attack() * elysia.definition().param("emergencyHeal", 0.80)));
            state.addEvent(new BattleEvent("REACTION_HEAL", elysia.instanceId(), hurt.instanceId(), healed, "P04_LAST_TOUCH"));
            emergencyHealingInProgress.remove(hurt.instanceId());
        }
    }

    private void triggerEnemyThresholdPassives(CombatantState target) {
        if (!target.downed() && target.definition().id().equals("E001") && !target.flag("e001_tenacity")
                && target.hp() / (double)target.maxHp() <= target.definition().param("tenacityThreshold", 0.30)) {
            target.setFlag("e001_tenacity");
            int value = target.addBarrier((int)Math.floor(target.maxHp() * target.definition().param("tenacityBarrier", 0.10)));
            state.addEvent(new BattleEvent("BARRIER", target.instanceId(), target.instanceId(), value, "E001_TENACITY"));
        }
    }

    private void triggerBossPhases(CombatantState boss) {
        if (boss.downed() || !boss.definition().boss()) return;
        double hp = boss.hp() / (double)boss.maxHp();
        switch (boss.definition().id()) {
            case "B01" -> {
                if (!boss.flag("b01_phase2") && hp <= boss.definition().param("phase2", 0.70)) {
                    boss.setFlag("b01_phase2"); spawnBossAdd(boss, "E001"); spawnBossAdd(boss, "E002");
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 2, "B01_PHASE2"));
                }
                if (!boss.flag("b01_phase3") && hp <= boss.definition().param("phase3", 0.35)) {
                    boss.setFlag("b01_phase3"); boss.putStatus(new StatusInstance("speed_multiplier", boss.instanceId(), 999, boss.definition().param("phase3Speed", 0.20)));
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 3, "B01_PHASE3"));
                }
            }
            case "B02" -> {
                if (!boss.flag("b02_phase2") && hp <= boss.definition().param("phase2", 0.65)) {
                    boss.setFlag("b02_phase2");
                    int barrier = boss.addBarrier((int)Math.floor(boss.maxHp() * boss.definition().param("phase2Barrier", 0.15)));
                    state.addEvent(new BattleEvent("BARRIER", boss.instanceId(), boss.instanceId(), barrier, "B02_PHASE2"));
                    spawnBossAdd(boss, "E008");
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 2, "B02_PHASE2"));
                }
                if (!boss.flag("b02_phase3") && hp <= boss.definition().param("phase3", 0.30)) {
                    boss.setFlag("b02_phase3");
                    boss.putStatus(new StatusInstance("speed_multiplier", boss.instanceId(), 999, boss.definition().param("phase3Speed", 0.15)));
                    boss.putStatus(new StatusInstance("healing_received_multiplier", boss.instanceId(), 999, boss.definition().param("phase3HealReceived", 0.25)));
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 3, "B02_PHASE3"));
                }
            }
            case "B03" -> {
                if (!boss.flag("b03_phase2") && hp <= boss.definition().param("phase2", 0.75)) {
                    boss.setFlag("b03_phase2"); spawnBossAdd(boss, "E009"); spawnBossAdd(boss, "E011");
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 2, "B03_PHASE2"));
                }
                if (!boss.flag("b03_phase3") && hp <= boss.definition().param("phase3", 0.40)) {
                    boss.setFlag("b03_phase3"); boss.putStatus(new StatusInstance("speed_multiplier", boss.instanceId(), 999, boss.definition().param("phase3Speed", 0.25)));
                    boss.setCounter("b03_overclock_count", 0);
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 3, "B03_PHASE3"));
                }
            }
            case "B04" -> {
                if (!boss.flag("b04_phase2") && hp <= boss.definition().param("phase2", 0.70)) {
                    boss.setFlag("b04_phase2"); spawnBossAdd(boss, "E014"); spawnBossAdd(boss, "E014");
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 2, "B04_PHASE2"));
                }
                if (!boss.flag("b04_phase3") && hp <= boss.definition().param("phase3", 0.35)) {
                    boss.setFlag("b04_phase3");
                    boss.putStatus(new StatusInstance("defense_multiplier", boss.instanceId(), 999, boss.definition().param("phase3Defense", -0.25)));
                    boss.putStatus(new StatusInstance("attack_multiplier", boss.instanceId(), 999, boss.definition().param("phase3Attack", 0.25)));
                    boss.putStatus(new StatusInstance("speed_multiplier", boss.instanceId(), 999, boss.definition().param("phase3Speed", 0.15)));
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 3, "B04_CORE_EXPOSED"));
                }
            }
            case "B05" -> {
                if (!boss.flag("b05_phase2") && hp <= boss.definition().param("phase2", 0.70)) {
                    boss.setFlag("b05_phase2"); spawnBossAdd(boss, "E009"); spawnBossAdd(boss, "E012");
                    boss.putStatus(new StatusInstance("damage_reduction", boss.instanceId(), 999, boss.definition().param("phase2Dr", 0.15)));
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 2, "B05_PHASE2"));
                }
                if (!boss.flag("b05_phase3") && hp <= boss.definition().param("phase3", 0.35)) {
                    boss.setFlag("b05_phase3"); removeBossAdds(boss); boss.removeStatus("damage_reduction", boss.instanceId());
                    boss.putStatus(new StatusInstance("speed_multiplier", boss.instanceId(), 999, boss.definition().param("phase3Speed", 0.25)));
                    boss.putStatus(new StatusInstance("attack_multiplier", boss.instanceId(), 999, boss.definition().param("phase3Attack", 0.20)));
                    state.addEvent(new BattleEvent("BOSS_PHASE", boss.instanceId(), boss.instanceId(), 3, "B05_LAST_WATCH"));
                }
            }
            default -> { }
        }
        refreshBossPackRules();
    }

    private void refreshBossPackRules() {
        for (CombatantState boss : state.combatants().stream().filter(c -> c.definition().boss() && !c.downed()).toList()) {
            boolean addAlive = state.living(CombatantSide.ENEMY).stream().anyMatch(unit -> unit != boss && !unit.definition().boss());
            if (boss.definition().id().equals("B01") && boss.flag("b01_phase2")) {
                if (addAlive) boss.putStatus(new StatusInstance("defense_multiplier", boss.instanceId(), 999, boss.definition().param("phase2Defense", 0.15)));
                else boss.removeStatus("defense_multiplier", boss.instanceId());
            }
            if (boss.definition().id().equals("B05") && boss.flag("b05_phase2") && !boss.flag("b05_phase3")) {
                if (addAlive) boss.putStatus(new StatusInstance("damage_reduction", boss.instanceId(), 999, boss.definition().param("phase2Dr", 0.15)));
                else boss.removeStatus("damage_reduction", boss.instanceId());
            }
        }
    }

    private void spawnBossAdd(CombatantState boss, String definitionId) {
        if (state.living(CombatantSide.ENEMY).size() >= 5) return;
        int serial = boss.counter("boss_add_serial") + 1; boss.setCounter("boss_add_serial", serial);
        CombatantDefinition definition = CanonicalData.definition(definitionId, boss.definition().intParam("level", 1), 0, false);
        CombatantState add = new CombatantState(boss.instanceId() + "_add_" + definitionId.toLowerCase() + "_" + serial,
                definition, CombatantSide.ENEMY, 4 + state.living(CombatantSide.ENEMY).size());
        state.addCombatant(add);
        state.addEvent(new BattleEvent("SPAWN", boss.instanceId(), add.instanceId(), 0, definitionId));
    }

    private void removeBossAdds(CombatantState boss) {
        for (CombatantState add : state.combatants().stream().filter(c -> c.side() == CombatantSide.ENEMY && c != boss && !c.definition().boss()).toList()) {
            state.removeCombatant(add.instanceId());
            state.addEvent(new BattleEvent("DESPAWN", boss.instanceId(), add.instanceId(), 0, "BOSS_PHASE"));
        }
    }

    private void collapseNextAllies(CombatantState source, int count, int gauge) {
        Set<String> selected = new HashSet<>();
        for (CombatantState unit : state.timelinePreview(12)) {
            if (unit.side() != CombatantSide.ALLY || unit.definition().summon() || !selected.add(unit.instanceId())) continue;
            applyGauge(source, unit, gauge, "B05_ORDER_COLLAPSE");
            if (selected.size() >= count) break;
        }
    }

    private CombatantState livingP07Summon(CombatantState owner) {
        return state.living(owner.side()).stream().filter(c -> c.definition().summon() && owner.instanceId().equals(c.ref("ownerId"))).findFirst().orElse(null);
    }

    private CombatantState p07Owner(CombatantSide side) {
        return state.living(side).stream().filter(c -> c.definition().id().equals("P07")).findFirst().orElse(null);
    }

    private void spawnP07Summon(CombatantState owner, double healthRatio) {
        if (livingP07Summon(owner) != null) return;
        int hp = Math.max(1, (int)Math.floor(owner.maxHp() * owner.definition().param("summonHpRatio", 0.45)));
        int atk = Math.max(1, (int)Math.floor(owner.attack() * owner.definition().param("summonAtkRatio", 0.70)));
        int def = Math.max(0, (int)Math.floor(owner.defense() * owner.definition().param("summonDefRatio", 0.80)));
        int spd = owner.definition().intParam("summonSpeed", 85);
        SkillDefinition wait = new SkillDefinition("p07_contract_wait", "계약 대기", TargetRule.SELF, 0,
                List.of(SkillEffect.noop("P07_SUMMON_INDEPENDENT_GAUGE")), "독립 Turn Gauge를 소비하고 명령을 기다립니다.");
        CombatantDefinition definition = new CombatantDefinition("P07_SUMMON", "계약수", new BattleStats(hp, atk, def, spd),
                wait.id(), List.of(wait), 0, List.of("SUMMON"), java.util.Map.of());
        CombatantState summon = new CombatantState("summon_" + owner.instanceId(), definition, owner.side(), 9);
        summon.setRef("ownerId", owner.instanceId());
        state.addCombatant(summon);
        double ratio = Math.max(0.01, Math.min(1.0, healthRatio));
        int desiredHp = Math.max(1, (int)Math.floor(summon.maxHp() * ratio));
        if (desiredHp < summon.maxHp()) summon.takeDamage(summon.maxHp() - desiredHp);
        state.addEvent(new BattleEvent("SPAWN", owner.instanceId(), summon.instanceId(), summon.hp(), "P07_CONTRACT"));
    }

    private void dismissP07Summon(CombatantState owner, boolean death) {
        CombatantState summon = livingP07Summon(owner);
        if (summon == null) return;
        state.removeCombatant(summon.instanceId());
        state.addEvent(new BattleEvent("DESPAWN", owner.instanceId(), summon.instanceId(), 0, death ? "SUMMON_DEATH" : "OWNER_DOWN"));
    }

    private void forceDown(CombatantState source, CombatantState target, String detail) {
        if (target.downed()) return;
        target.forceDown();
        onDown(source, target, detail);
    }

    private void tickCooldowns(CombatantState actor, String used) {
        for (String id : new ArrayList<>(actor.cooldownsView().keySet())) if (!id.equals(used)) actor.setCooldown(id, actor.cooldown(id) - 1);
    }

    public static int calculateDamage(int attack, int defense, double potency) {
        double reduction = defense / (double)(defense + 4L * Math.max(1, attack));
        reduction = Math.min(0.65, Math.max(0, reduction));
        return Math.max(1, (int)Math.floor(attack * potency * (1 - reduction)));
    }

    private record DamagePlan(CombatantState target, int amount, double potency) {}
    private record DamageApplied(CombatantState target, int hpLost, int barrierBefore, boolean barrierBroke, boolean newlyDowned) {}
    private record Reaction(String sourceId, String targetId, double potency, String type, int depth) {}
}
