package io.github.q93503128.turnbound.session;

import io.github.q93503128.turnbound.combat.CombatantState;

import java.util.ArrayList;
import java.util.List;

/** Pure local-space formation math. Two-row placement is visual only and carries no front/back mechanics. */
final class BattleFormationLayout {
    record Local(double x, double z) {}

    private static final double SOLO_SPACING = 2.0D;
    private static final double GROUP_STRIDE = 4.8D;
    private static final double BLOCK_HALF_WIDTH = 0.9D;
    private static final double BLOCK_ROW_SPACING = 1.8D;
    private static final double ALLY_CENTER_Z = 4.0D;

    private BattleFormationLayout() {}

    static Local ally(CombatantState unit, int fallbackIndex, int allyCount, int playerGroups) {
        if (playerGroups <= 1 || unit.presentationGroup() < 0 || unit.presentationSlot() < 0) {
            return new Local((fallbackIndex - (allyCount - 1) / 2.0D) * SOLO_SPACING, ALLY_CENTER_Z);
        }
        int group = unit.presentationGroup();
        int slot = unit.presentationSlot();
        double groupCenter = (group - (playerGroups - 1) / 2.0D) * GROUP_STRIDE;
        int col = slot % 2;
        int row = slot / 2;
        double x = groupCenter + (col == 0 ? -BLOCK_HALF_WIDTH : BLOCK_HALF_WIDTH);
        double z = ALLY_CENTER_Z + (row == 0 ? -BLOCK_ROW_SPACING / 2.0D : BLOCK_ROW_SPACING / 2.0D);
        return new Local(x, z);
    }

    static List<Local> sharedPartySlots(int playerGroups) {
        if (playerGroups <= 1) return List.of();
        List<Local> out = new ArrayList<>();
        for (int group = 0; group < playerGroups; group++) {
            double groupCenter = (group - (playerGroups - 1) / 2.0D) * GROUP_STRIDE;
            for (int slot = 0; slot < 4; slot++) {
                int col = slot % 2;
                int row = slot / 2;
                double x = groupCenter + (col == 0 ? -BLOCK_HALF_WIDTH : BLOCK_HALF_WIDTH);
                double z = ALLY_CENTER_Z + (row == 0 ? -BLOCK_ROW_SPACING / 2.0D : BLOCK_ROW_SPACING / 2.0D);
                out.add(new Local(x, z));
            }
        }
        return List.copyOf(out);
    }

    static Local enemy(int index, int enemyCount) {
        return new Local((index - (enemyCount - 1) / 2.0D) * SOLO_SPACING, -4.0D);
    }

    static int playerGroups(Iterable<CombatantState> units) {
        int groups = 0;
        for (CombatantState unit : units) {
            if (unit.presentationGroup() >= 0) groups = Math.max(groups, unit.presentationGroup() + 1);
        }
        return groups;
    }
}
