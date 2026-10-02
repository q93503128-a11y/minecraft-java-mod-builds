package io.github.q93503128.turnbound.client;

import java.util.List;

/** Pure client-side rules for pending actions. Selecting a skill never executes it. */
final class BattleActionRules {
    private BattleActionRules() {}

    static boolean needsSingleTarget(String rule) {
        return "ALLY_SINGLE".equals(rule)
                || "ALLY_SINGLE_EXCEPT_SELF".equals(rule)
                || "ENEMY_SINGLE".equals(rule)
                || "DEAD_ALLY_SINGLE".equals(rule);
    }

    static boolean needsManualTarget(String rule) {
        return needsSingleTarget(rule) || "ENEMY_TWO".equals(rule);
    }

    static int requiredTargetCount(List<ClientBattleState.Unit> units, String rule, String actorId) {
        if ("ENEMY_TWO".equals(rule)) {
            return Math.min(2, (int)units.stream().filter(unit -> BattleTargeting.validTarget(rule, unit, actorId)).count());
        }
        return needsSingleTarget(rule) ? 1 : 0;
    }

    static int defaultTarget(List<ClientBattleState.Unit> units, String rule, String actorId) {
        if ("SELF".equals(rule)) {
            for (int i = 0; i < units.size(); i++) if (units.get(i).id().equals(actorId)) return i;
            return -1;
        }
        return needsSingleTarget(rule) ? BattleTargeting.firstValid(units, rule, actorId) : -1;
    }

    /** Returns null when confirmation is invalid; empty string is valid for self/all-target actions. */
    static String confirmedTargets(
            List<ClientBattleState.Unit> units,
            String rule,
            String actorId,
            int selectedTarget,
            int selectedTarget2
    ) {
        if ("SELF".equals(rule) || "ALLY_ALL".equals(rule) || "ENEMY_ALL".equals(rule)) return "";
        if ("ENEMY_TWO".equals(rule)) {
            int required = requiredTargetCount(units, rule, actorId);
            if (required < 1 || selectedTarget < 0 || selectedTarget >= units.size()) return null;
            ClientBattleState.Unit first = units.get(selectedTarget);
            if (!BattleTargeting.validTarget(rule, first, actorId)) return null;
            if (required == 1) return first.id();
            if (selectedTarget2 < 0 || selectedTarget2 >= units.size() || selectedTarget2 == selectedTarget) return null;
            ClientBattleState.Unit second = units.get(selectedTarget2);
            return BattleTargeting.validTarget(rule, second, actorId) ? first.id() + "," + second.id() : null;
        }
        if (!needsSingleTarget(rule) || selectedTarget < 0 || selectedTarget >= units.size()) return null;
        ClientBattleState.Unit unit = units.get(selectedTarget);
        return BattleTargeting.validTarget(rule, unit, actorId) ? unit.id() : null;
    }

    static String confirmedTarget(
            List<ClientBattleState.Unit> units,
            String rule,
            String actorId,
            int selectedTarget
    ) {
        return confirmedTargets(units, rule, actorId, selectedTarget, -1);
    }
}
