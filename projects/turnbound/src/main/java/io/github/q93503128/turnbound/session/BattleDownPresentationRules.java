package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantSide;

/** Pure rules for what remains in the 3D arena after a combatant becomes downed. */
final class BattleDownPresentationRules {
    private BattleDownPresentationRules() {}

    static boolean retiresVisual(CombatantSide side, boolean summon) {
        return summon;
    }

    static boolean usesRecoveryMarker(CombatantSide side, boolean summon) {
        return !summon;
    }

    static int removalTicks(boolean summon, boolean boss) {
        if (summon) return 12;
        return boss ? 28 : 20;
    }

    static int recoveryMarkerDelayTicks(String definitionId) {
        return switch (definitionId) {
            case "P01", "P03", "P06", "P08" -> 28;
            default -> 26;
        };
    }
}
