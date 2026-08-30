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

    static int defaultTarget(List<ClientBattleState.Unit> units, String rule, String actorId) {
        if ("SELF".equals(rule)) {
            for (int i = 0; i < units.size(); i++) if (units.get(i).id().equals(actorId)) return i;
            return -1;
        }
        return needsSingleTarget(rule) ? BattleTargeting.firstValid(units, rule, actorId) : -1;
    }

    /** Returns null when confirmation is invalid; empty string is valid for self/all-target actions. */
    static String confirmedTarget(
            List<ClientBattleState.Unit> units,
            String rule,
            String actorId,
            int selectedTarget
    ) {
        if ("SELF".equals(rule) || "ALLY_ALL".equals(rule) || "ENEMY_ALL".equals(rule)) return "";
        if (!needsSingleTarget(rule) || selectedTarget < 0 || selectedTarget >= units.size()) return null;
        ClientBattleState.Unit unit = units.get(selectedTarget);
        return BattleTargeting.validTarget(rule, unit, actorId) ? unit.id() : null;
    }
}
