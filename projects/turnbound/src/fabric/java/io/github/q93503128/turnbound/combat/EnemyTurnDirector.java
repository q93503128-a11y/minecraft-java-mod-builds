package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal content-neutral enemy decision policy until enemy kits are authored.
 * Only an enemy's declared basic skill is selected. It never changes any combat
 * state itself: the common validated resolver remains the single write path.
 */
public final class EnemyTurnDirector {
    private EnemyTurnDirector() {}

    public static boolean actOne(BattleState battle, GenericBattleResolver resolver) {
        if (battle.outcome() != BattleOutcome.RUNNING || battle.currentActorId() == null) {
            return false;
        }
        CombatantState actor = battle.combatant(battle.currentActorId());
        if (actor.side() != CombatantSide.ENEMY) return false;

        SkillDefinition skill = actor.definition().skill(actor.definition().basicSkillId());
        String[] ids = targetIds(battle, actor, skill.targetRule());
        resolver.resolve(actor.instanceId(), skill.id(), ids);
        return true;
    }

    private static String[] targetIds(BattleState state, CombatantState actor, TargetRule rule) {
        List<CombatantState> allies = state.living(actor.side());
        List<CombatantState> opponents = state.living(actor.side().opposite());
        return switch (rule) {
            case SELF, ALLY_ALL, ENEMY_ALL -> new String[0];
            case ALLY_SINGLE -> new String[]{require(aliesFirst(allies)).instanceId()};
            case ENEMY_SINGLE -> new String[]{require(preferredOpponent(state, actor, opponents)).instanceId()};
            case ENEMY_TWO -> {
                List<CombatantState> chosen = new ArrayList<>(2);
                CombatantState first = require(preferredOpponent(state, actor, opponents));
                chosen.add(first);
                for (CombatantState opponent : opponents) {
                    if (chosen.size() >= 2) break;
                    if (opponent != first) chosen.add(opponent);
                }
                yield chosen.stream().map(CombatantState::instanceId).toArray(String[]::new);
            }
            case DEAD_ALLY_SINGLE -> new String[]{
                    require(aliesFirst(state.downed(actor.side()))).instanceId()
            };
        };
    }

    private static CombatantState preferredOpponent(BattleState battle, CombatantState actor,
                                                     List<CombatantState> opponents) {
        StatusInstance taunt = actor.status("taunt");
        if (taunt != null) {
            CombatantState source = battle.find(taunt.sourceId());
            if (source != null && !source.downed() && opponents.contains(source)) return source;
        }
        return aliesFirst(opponents);
    }

    private static CombatantState aliesFirst(List<CombatantState> units) {
        return units.isEmpty() ? null : units.get(0);
    }

    private static CombatantState require(CombatantState unit) {
        if (unit == null) throw new IllegalStateException("Enemy basic action lacks a valid target");
        return unit;
    }
}
