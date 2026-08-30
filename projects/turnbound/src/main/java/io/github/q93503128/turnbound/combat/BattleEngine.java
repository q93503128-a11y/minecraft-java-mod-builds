package io.github.q93503128.turnbound.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class BattleEngine {
    public static final long TURN_THRESHOLD = 1000L;
    public static final int MAX_REACTION_DEPTH = 3;
    private final BattleState state;
    private final Deque<Reaction> reactions = new ArrayDeque<>();
    private final Set<String> emergencyHealingInProgress = new HashSet<>();

    public BattleEngine(BattleState state) { this.state = Objects.requireNonNull(state); }
    public BattleState state() { return state; }

    public CombatantState nextReady() {
        if (state.outcome() != BattleOutcome.RUNNING) throw new IllegalStateException("Battle is over");
        if (state.currentActorId() != null) return state.combatant(state.currentActorId());
        List<CombatantState> living = state.combatants().stream().filter(c -> !c.downed()).toList();
        long pulses = living.stream().mapToLong(this::pulsesUntilReady).min().orElseThrow();
        if (pulses > 0) {
            for (var c : living) c.addGauge(pulses * c.speed());
            state.addLogicalPulse(pulses);
        }
        CombatantState ready = living.stream().filter(c -> c.gauge() >= TURN_THRESHOLD)
                .max(Comparator.comparingLong(CombatantState::gauge).thenComparingInt(CombatantState::speed).thenComparingInt(c -> -c.initiativeSeed()))
                .orElseThrow();
        state.setCurrentActorId(ready.instanceId());
        state.addEvent(new BattleEvent("TURN_READY", ready.instanceId(), ready.instanceId(), (int) Math.min(Integer.MAX_VALUE, ready.gauge()), "pulse=" + state.logicalPulse()));
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
        List<CombatantState> targets = resolveTargets(actor, skill, requestedTargetIds);
        if (skill.id().equals("p02_time_leap") && targets.getFirst() == actor) throw new IllegalArgumentException("P02 time leap cannot target self");

        state.addEvent(new BattleEvent("ACTION", actorId, String.join(",", targets.stream().map(CombatantState::instanceId).toList()), 0, skillId));
        boolean direct = skill.effects().stream().anyMatch(e -> e.type() == EffectType.DAMAGE);
        int focus = actor.definition().id().equals("P01") ? actor.counter("focus") : 0;
        for (var effect : skill.effects()) {
            applyEffect(actor, skill, targets, effect, focus, 0, direct);
            resolveReactions();
        }
        postRules(actor, skill, targets, direct);
        resolveReactions();
        if (skill.cooldown() > 0) actor.setCooldown(skill.id(), skill.cooldown());
        actor.spendTurnGauge();
        tickCooldowns(actor, skill.id());
        actor.tickStatusesOnOwnTurn();
        triggerLumeaPassive(actor);
        refreshBossPackDefense();
        state.setCurrentActorId(null);
        state.addEvent(new BattleEvent("TURN_END", actorId, actorId, 0, skillId));
    }

    private List<CombatantState> resolveTargets(CombatantState actor, SkillDefinition skill, String[] ids) {
        return switch (skill.targetRule()) {
            case SELF -> List.of(actor);
            case ALLY_ALL -> state.living(actor.side());
            case ENEMY_ALL -> state.living(actor.side().opposite());
            case ALLY_SINGLE -> List.of(single(ids, actor.side(), false));
            case ENEMY_SINGLE -> List.of(single(ids, actor.side().opposite(), false));
            case DEAD_ALLY_SINGLE -> List.of(single(ids, actor.side(), true));
        };
    }

    private CombatantState single(String[] ids, CombatantSide side, boolean down) {
        if (ids.length != 1) throw new IllegalArgumentException("Exactly one target required");
        CombatantState target = state.combatant(ids[0]);
        if (target.side() != side || target.downed() != down) throw new IllegalArgumentException("Invalid target");
        return target;
    }

    private void applyEffect(CombatantState actor, SkillDefinition skill, List<CombatantState> targets,
                             SkillEffect effect, int focus, int depth, boolean direct) {
        switch (effect.type()) {
            case DAMAGE -> {
                for (var target : targets) {
                    double potency = effect.magnitude();
                    if (actor.definition().id().equals("P01")) {
                        potency *= 1 + 0.15 * focus;
                        if (skill.id().equals("p01_shatter")) potency *= 1 + 0.10 * focus;
                    }
                    damage(actor, target, potency, depth, direct);
                }
            }
            case HEAL -> {
                for (var target : targets) {
                    double potency = effect.magnitude();
                    if (actor.definition().id().equals("P04") && skill.id().equals("p04_rest_light") && target == actor) potency = 0.70;
                    int value = target.heal((int) Math.floor(actor.attack() * potency));
                    state.addEvent(new BattleEvent("HEAL", actor.instanceId(), target.instanceId(), value, skill.id()));
                }
            }
            case BARRIER_MAX_HP -> {
                for (var target : targets) {
                    int value = target.addBarrier((int) Math.floor(target.maxHp() * effect.magnitude()));
                    state.addEvent(new BattleEvent("BARRIER", actor.instanceId(), target.instanceId(), value, skill.id()));
                }
            }
            case GAUGE_ADD -> {
                for (var target : targets) {
                    long before = target.gauge();
                    target.addGauge(effect.flatValue());
                    state.addEvent(new BattleEvent("GAUGE", actor.instanceId(), target.instanceId(), (int) (target.gauge() - before), skill.id()));
                }
            }
            case SELF_GAUGE_ADD -> {
                long before = actor.gauge();
                actor.addGauge(effect.flatValue());
                state.addEvent(new BattleEvent("GAUGE", actor.instanceId(), actor.instanceId(), (int) (actor.gauge() - before), skill.id()));
            }
            case GAUGE_AT_LEAST -> {
                for (var target : targets) {
                    long before = target.gauge();
                    target.setGauge(Math.max(target.gauge(), effect.flatValue()));
                    state.addEvent(new BattleEvent("GAUGE", actor.instanceId(), target.instanceId(), (int) (target.gauge() - before), skill.id()));
                }
            }
            case GUARD_REDIRECT -> {
                var target = targets.getFirst();
                if (target == actor) throw new IllegalArgumentException("Guard requires another ally");
                target.putStatus(new StatusInstance("guard_redirect", actor.instanceId(), effect.duration(), effect.magnitude()));
                state.addEvent(new BattleEvent("STATUS", actor.instanceId(), target.instanceId(), effect.duration(), "guard_redirect"));
            }
            case DEFENSE_UP -> {
                for (var target : targets) {
                    int storedTurns = effect.duration() + (target == actor ? 1 : 0);
                    target.putStatus(new StatusInstance("defense_multiplier", actor.instanceId(), storedTurns, effect.magnitude()));
                    state.addEvent(new BattleEvent("STATUS", actor.instanceId(), target.instanceId(), effect.duration(), "defense_multiplier"));
                }
            }
            case REVIVE -> {
                var target = targets.getFirst();
                int hp = target.revive(effect.magnitude());
                state.addEvent(new BattleEvent("REVIVE", actor.instanceId(), target.instanceId(), hp, skill.id()));
            }
        }
    }

    private void damage(CombatantState actor, CombatantState target, double potency, int depth, boolean direct) {
        if (target.downed()) return;
        int raw = calculateDamage(actor.attack(), target.defense(), potency);
        var guard = target.status("guard_redirect");
        if (guard != null && direct) {
            var guardian = state.combatant(guard.sourceId());
            if (!guardian.downed()) {
                int redirected = (int) Math.floor(raw * guard.magnitude());
                raw -= redirected;
                int dealt = guardian.takeDamage(redirected);
                state.addEvent(new BattleEvent("DAMAGE_REDIRECT", actor.instanceId(), guardian.instanceId(), dealt, target.instanceId()));
                afterDamage(actor, guardian, dealt, depth, direct);
            }
        }
        int dealt = target.takeDamage(raw);
        state.addEvent(new BattleEvent("DAMAGE", actor.instanceId(), target.instanceId(), dealt, "potency=" + potency));
        afterDamage(actor, target, dealt, depth, direct);
    }

    private void afterDamage(CombatantState attacker, CombatantState target, int dealt, int depth, boolean direct) {
        if (dealt <= 0) return;
        if (target.downed()) state.addEvent(new BattleEvent("DOWN", attacker.instanceId(), target.instanceId(), 0, "hp=0"));
        triggerEnemyThresholdPassives(target);
        triggerGraulThresholds(target);
        refreshBossPackDefense();
        if (direct && depth == 0 && target.definition().id().equals("P03") && !target.downed()) {
            reactions.addLast(new Reaction(target.instanceId(), attacker.instanceId(), 0.65, "P03_COUNTER", 1));
        }
        triggerElysia(target);
    }

    private void triggerEnemyThresholdPassives(CombatantState target) {
        if (target.downed()) return;
        if (target.definition().id().equals("E001") && !target.flag("e001_tenacity")
                && target.hp() * 100 <= target.maxHp() * 30) {
            target.setFlag("e001_tenacity");
            int value = target.addBarrier((int) Math.floor(target.maxHp() * 0.10));
            state.addEvent(new BattleEvent("BARRIER", target.instanceId(), target.instanceId(), value, "e001_tenacity"));
        }
    }

    private void triggerGraulThresholds(CombatantState target) {
        if (!target.definition().id().equals("B01") || target.downed()) return;
        if (!target.flag("b01_phase2") && target.hp() * 100 <= target.maxHp() * 70) {
            target.setFlag("b01_phase2");
            spawnBossAdd("b01_add_e001", PrototypeRoster.corruptedWalker(), 5);
            spawnBossAdd("b01_add_e002", PrototypeRoster.boneArcher(), 6);
            state.addEvent(new BattleEvent("BOSS_PHASE", target.instanceId(), target.instanceId(), 2, "E001+E002 summon"));
        }
        if (!target.flag("b01_phase3") && target.hp() * 100 <= target.maxHp() * 35) {
            target.setFlag("b01_phase3");
            target.putStatus(new StatusInstance("speed_multiplier", target.instanceId(), 999, 0.20));
            state.addEvent(new BattleEvent("BOSS_PHASE", target.instanceId(), target.instanceId(), 3, "SPD+20"));
        }
    }

    private void spawnBossAdd(String instanceId, CombatantDefinition definition, int seed) {
        if (state.combatants().stream().anyMatch(unit -> unit.instanceId().equals(instanceId))) return;
        state.addCombatant(new CombatantState(instanceId, definition, CombatantSide.ENEMY, seed));
        state.addEvent(new BattleEvent("SPAWN", "b01_graul", instanceId, 0, definition.id()));
    }

    private void refreshBossPackDefense() {
        CombatantState boss = state.combatants().stream().filter(unit -> unit.definition().id().equals("B01")).findFirst().orElse(null);
        if (boss == null || boss.downed() || !boss.flag("b01_phase2")) return;
        boolean addAlive = state.living(CombatantSide.ENEMY).stream().anyMatch(unit ->
                unit.instanceId().equals("b01_add_e001") || unit.instanceId().equals("b01_add_e002"));
        if (addAlive) boss.putStatus(new StatusInstance("defense_multiplier", boss.instanceId(), 999, 0.15));
        else boss.removeStatus("defense_multiplier");
    }

    private void triggerElysia(CombatantState hurt) {
        if (hurt.downed() || hurt.hp() * 100 > hurt.maxHp() * 30) return;
        for (var elysia : state.living(hurt.side())) {
            if (!elysia.definition().id().equals("P04")) continue;
            String flag = "p04_emergency:" + hurt.instanceId();
            if (elysia.flag(flag) || !emergencyHealingInProgress.add(hurt.instanceId())) continue;
            elysia.setFlag(flag);
            int healed = hurt.heal((int) Math.floor(elysia.attack() * 0.80));
            state.addEvent(new BattleEvent("REACTION_HEAL", elysia.instanceId(), hurt.instanceId(), healed, "P04_LAST_TOUCH"));
            emergencyHealingInProgress.remove(hurt.instanceId());
        }
    }

    private void resolveReactions() {
        while (!reactions.isEmpty()) {
            Reaction reaction = reactions.removeFirst();
            if (reaction.depth() > MAX_REACTION_DEPTH) continue;
            var source = state.combatant(reaction.sourceId());
            var target = state.combatant(reaction.targetId());
            if (source.downed() || target.downed()) continue;
            int dealt = target.takeDamage(calculateDamage(source.attack(), target.defense(), reaction.potency()));
            state.addEvent(new BattleEvent("REACTION_DAMAGE", source.instanceId(), target.instanceId(), dealt, reaction.type()));
            afterDamage(source, target, dealt, reaction.depth(), false);
        }
    }

    private void postRules(CombatantState actor, SkillDefinition skill, List<CombatantState> targets, boolean direct) {
        if (actor.definition().id().equals("E003")) {
            if (skill.id().equals("e003_arm")) {
                actor.putStatus(new StatusInstance("e003_armed", actor.instanceId(), 2, 1.0));
                state.addEvent(new BattleEvent("STATUS", actor.instanceId(), actor.instanceId(), 1, "e003_armed"));
            } else if (skill.id().equals("e003_explode")) {
                actor.removeStatus("e003_armed");
                int lost = actor.takeDamage(Integer.MAX_VALUE);
                state.addEvent(new BattleEvent("DOWN", actor.instanceId(), actor.instanceId(), lost, "e003_self_explosion"));
            }
        }
        if (actor.definition().id().equals("B01")) {
            if (skill.id().equals("b01_scratch")) {
                actor.putStatus(new StatusInstance("attack_multiplier", actor.instanceId(), 3, 0.15));
                state.addEvent(new BattleEvent("STATUS", actor.instanceId(), actor.instanceId(), 2, "attack_multiplier"));
            } else if (skill.id().equals("b01_warn")) {
                actor.setFlag("b01_charge_ready");
                actor.putStatus(new StatusInstance("b01_charge_warning", actor.instanceId(), 2, 1.0));
                state.addEvent(new BattleEvent("STATUS", actor.instanceId(), actor.instanceId(), 1, "b01_charge_warning"));
            } else if (skill.id().equals("b01_charge")) {
                actor.clearFlag("b01_charge_ready");
                actor.removeStatus("b01_charge_warning");
            }
        }
        if (!actor.definition().id().equals("P01")) return;
        if (skill.id().equals("p01_duel_lock")) {
            var target = targets.getFirst();
            if (!target.instanceId().equals(actor.ref("focusTarget"))) {
                actor.setRef("focusTarget", target.instanceId());
                actor.setCounter("focus", 0);
            }
            actor.incrementCounter("focus", 1, 3);
        } else if (direct && targets.size() == 1) {
            var target = targets.getFirst();
            String current = actor.ref("focusTarget");
            if (target.instanceId().equals(current)) actor.incrementCounter("focus", 1, 3);
            else {
                actor.setRef("focusTarget", target.instanceId());
                actor.setCounter("focus", 0);
            }
            if (target.downed()) {
                actor.setRef("focusTarget", null);
                actor.setCounter("focus", 0);
            }
        }
    }

    private void triggerLumeaPassive(CombatantState actor) {
        for (var lumea : state.living(actor.side())) {
            if (lumea != actor && lumea.definition().id().equals("P02") && actor.speed() < lumea.speed()) {
                lumea.addGauge(60);
                state.addEvent(new BattleEvent("PASSIVE_GAUGE", lumea.instanceId(), lumea.instanceId(), 60, "P02_WAIT_FOR_SLOW"));
            }
        }
    }

    private void tickCooldowns(CombatantState actor, String used) {
        for (String id : new ArrayList<>(actor.cooldownsView().keySet())) if (!id.equals(used)) actor.setCooldown(id, actor.cooldown(id) - 1);
    }

    public static int calculateDamage(int attack, int defense, double potency) {
        double reduction = defense / (double) (defense + 4L * Math.max(1, attack));
        reduction = Math.min(0.65, Math.max(0, reduction));
        return Math.max(1, (int) Math.floor(attack * potency * (1 - reduction)));
    }

    private record Reaction(String sourceId, String targetId, double potency, String type, int depth) {}
}
