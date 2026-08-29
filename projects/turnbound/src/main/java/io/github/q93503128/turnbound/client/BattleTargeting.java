package io.github.q93503128.turnbound.client;

import java.util.List;

final class BattleTargeting {
    private BattleTargeting() {
    }

    static boolean validTarget(String rule, ClientBattleState.Unit unit, String actorId) {
        if (actorId == null || actorId.isBlank()) return false;
        boolean ally = "ALLY".equals(unit.side());
        return switch (rule) {
            case "ALLY_SINGLE" -> ally && !unit.downed();
            case "ENEMY_SINGLE" -> !ally && !unit.downed();
            case "DEAD_ALLY_SINGLE" -> ally && unit.downed();
            default -> false;
        };
    }

    static int firstValid(List<ClientBattleState.Unit> units, String rule, String actorId) {
        for (int i = 0; i < units.size(); i++) {
            if (validTarget(rule, units.get(i), actorId)) return i;
        }
        return -1;
    }

    static int cycle(
            List<ClientBattleState.Unit> units,
            String rule,
            String actorId,
            int current,
            int direction
    ) {
        if (units.isEmpty() || direction == 0) return -1;

        int cursor = current;
        if (cursor < 0 || cursor >= units.size()) {
            cursor = direction > 0 ? -1 : units.size();
        }

        for (int step = 1; step <= units.size(); step++) {
            int candidate = Math.floorMod(cursor + direction * step, units.size());
            if (validTarget(rule, units.get(candidate), actorId)) return candidate;
        }
        return -1;
    }
}
