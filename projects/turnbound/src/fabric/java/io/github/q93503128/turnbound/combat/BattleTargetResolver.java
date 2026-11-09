package io.github.q93503128.turnbound.combat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Character-independent target selection rules (including taunt and downed targets).
 * The battle engine delegates here without deciding any authored character abilities.
 */
public final class BattleTargetResolver {
    private BattleTargetResolver() {}

    public static List<CombatantState> resolve(BattleState state, CombatantState actor,
                                              SkillDefinition skill, String... ids) {
        return switch (skill.targetRule()) {
            case SELF -> List.of(actor);
            case ALLY_ALL -> state.living(actor.side());
            case ENEMY_ALL -> state.living(actor.side().opposite());
            case ALLY_SINGLE -> List.of(single(state, ids, actor.side(), false, actor));
            case ENEMY_SINGLE -> List.of(single(state, ids, actor.side().opposite(), false, actor));
            case ENEMY_TWO -> enemyTwo(state, ids, actor);
            case DEAD_ALLY_SINGLE -> List.of(single(state, ids, actor.side(), true, actor));
        };
    }

    private static List<CombatantState> enemyTwo(BattleState state, String[] ids, CombatantState actor) {
        List<CombatantState> living = state.living(actor.side().opposite());
        int required = Math.min(2, living.size());
        if (required < 1 || ids.length != required) {
            throw new IllegalArgumentException("Enemy-two skill requires " + required + " distinct living target(s)");
        }
        List<CombatantState> targets = new ArrayList<>(required);
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank() || !seen.add(id)) {
                throw new IllegalArgumentException("Enemy-two targets must be distinct");
            }
            CombatantState target = state.combatant(id);
            if (target.side() != actor.side().opposite() || target.downed()) {
                throw new IllegalArgumentException("Invalid enemy-two target");
            }
            targets.add(target);
        }
        StatusInstance taunt = actor.status("taunt");
        if (taunt != null) {
            CombatantState taunter = state.find(taunt.sourceId());
            if (taunter != null && !taunter.downed() && taunter.side() == actor.side().opposite()
                    && !targets.contains(taunter)) {
                throw new IllegalArgumentException("Taunt requires including " + taunter.instanceId());
            }
        }
        return List.copyOf(targets);
    }

    private static CombatantState single(BattleState state, String[] ids, CombatantSide side,
                                         boolean down, CombatantState actor) {
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
}
