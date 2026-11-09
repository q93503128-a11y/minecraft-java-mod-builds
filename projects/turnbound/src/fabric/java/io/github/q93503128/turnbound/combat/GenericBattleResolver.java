package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.List;

/**
 * Content-neutral, deterministic turn resolution. Only explicitly supported generic effects
 * are admitted. Unsupported effects are rejected BEFORE spending Gauge, CD or turn state.
 * This is not a world encounter controller and does not invent a playable roster.
 */
public final class GenericBattleResolver {
    private final BattleState state;

    public GenericBattleResolver(BattleState state) {
        if (state == null) throw new IllegalArgumentException("Battle state required");
        this.state = state;
    }

    public CombatantState beginTurn() {
        if (state.outcome() != BattleOutcome.RUNNING) {
            throw new IllegalStateException("Battle already resolved");
        }
        return TurnScheduler.nextReady(state);
    }

    /**
     * The caller must authenticate the acting character's owner and current session.
     * A received client packet is never authority by itself.
     */
    public void resolve(String actorId, String skillId, String... targetIds) {
        if (state.outcome() != BattleOutcome.RUNNING) throw new IllegalStateException("Battle finished");
        if (actorId == null || !actorId.equals(state.currentActorId())) {
            throw new IllegalArgumentException("Not this actor's turn");
        }
        CombatantState actor = state.combatant(actorId);
        if (actor.downed()) throw new IllegalArgumentException("Downed actor");
        SkillDefinition skill = actor.definition().skill(skillId);
        if (actor.cooldown(skillId) > 0) throw new IllegalArgumentException("Skill on cooldown");
        for (SkillEffect effect : skill.effects()) {
            if (effect == null || !supported(effect.type())) {
                throw new IllegalArgumentException("Effect requires authored resolver: " + effect);
            }
        }
        List<CombatantState> targets = BattleTargetResolver.resolve(state, actor, skill, targetIds);
        if (targets.isEmpty()) throw new IllegalArgumentException("No valid targets");

        // Commit only after validation; no mutation on rejected action.
        if (skill.cooldown() > 0) actor.setCooldown(skill.id(), skill.cooldown());
        actor.spendTurnGauge();
        state.addEvent(new BattleEvent("ACTION", actorId,
                String.join(",", targets.stream().map(CombatantState::instanceId).toList()), 0, skillId));
        for (SkillEffect effect : skill.effects()) apply(actor, targets, effect);
        for (String cd : new ArrayList<>(actor.cooldownsView().keySet())) {
            if (!cd.equals(skill.id())) actor.setCooldown(cd, actor.cooldown(cd) - 1);
        }
        actor.tickStatusesOnOwnTurn();
        state.setCurrentActorId(null);
        state.addEvent(new BattleEvent("TURN_END", actorId, actorId, 0, skillId));
    }

    private static boolean supported(EffectType type) {
        return switch (type) {
            case DAMAGE, HEAL, BARRIER_MAX_HP, GAUGE_ADD, SELF_GAUGE_ADD,
                    GAUGE_AT_LEAST, REVIVE, SELF_HP_COST, STATUS_MARK, STATUS_CLEAR -> true;
            default -> false;
        };
    }

    private void apply(CombatantState actor, List<CombatantState> targets, SkillEffect effect) {
        switch (effect.type()) {
            case DAMAGE -> {
                for (CombatantState target : targets) {
                    int raw = CombatMath.calculateDamage(actor.attack(), target.defense(), effect.magnitude());
                    int dealt = target.takeDamage(raw);
                    state.addEvent(new BattleEvent("DAMAGE", actor.instanceId(),
                            target.instanceId(), dealt, ""));
                }
            }
            case HEAL -> {
                for (CombatantState target : targets) {
                    int healed = target.heal(Math.max(0, (int)Math.floor(actor.attack() * effect.magnitude())));
                    state.addEvent(new BattleEvent("HEAL", actor.instanceId(),
                            target.instanceId(), healed, ""));
                }
            }
            case BARRIER_MAX_HP -> {
                for (CombatantState target : targets) {
                    int granted = target.addBarrier(Math.max(0, (int)Math.floor(target.maxHp() * effect.magnitude())));
                    state.addEvent(new BattleEvent("BARRIER", actor.instanceId(),
                            target.instanceId(), granted, ""));
                }
            }
            case GAUGE_ADD -> {
                for (CombatantState target : targets) target.addGauge(effect.flatValue());
            }
            case SELF_GAUGE_ADD -> actor.addGauge(effect.flatValue());
            case GAUGE_AT_LEAST -> {
                for (CombatantState target : targets) target.setGaugeAtLeast(effect.flatValue());
            }
            case REVIVE -> {
                for (CombatantState target : targets) {
                    int hp = target.revive(effect.magnitude());
                    state.addEvent(new BattleEvent("REVIVE", actor.instanceId(),
                            target.instanceId(), hp, ""));
                }
            }
            case SELF_HP_COST -> actor.spendCurrentHp(effect.magnitude());
            case STATUS_MARK -> {
                if (effect.key().isBlank() || effect.duration() <= 0)
                    throw new IllegalArgumentException("Malformed status mark");
                for (CombatantState target : targets)
                    target.putStatus(new StatusInstance(effect.key(), actor.instanceId(), effect.duration(), 1));
            }
            case STATUS_CLEAR -> {
                if (effect.key().isBlank()) throw new IllegalArgumentException("Malformed status clear");
                for (CombatantState target : targets) target.removeStatus(effect.key());
            }
            default -> throw new IllegalStateException("Effect admitted without implementation");
        }
    }
}
