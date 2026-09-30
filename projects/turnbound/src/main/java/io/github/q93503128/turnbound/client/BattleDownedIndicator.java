package io.github.q93503128.turnbound.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Compact bone-white skull marker for combatants waiting to recover. */
final class BattleDownedIndicator {
    private static final int BONE = 0xFFE7E1D3;
    private static final int SHADOW = 0xFF24201C;

    private BattleDownedIndicator() {}

    static void drawSkeletonSkull(GuiGraphicsExtractor graphics, int centerX, int centerY, float scale) {
        if (graphics == null) return;
        float safe = Math.max(0.55F, Math.min(1.35F, scale));
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(safe, safe);

        graphics.fill(-5, -5, 6, 3, SHADOW);
        graphics.fill(-4, -6, 5, 4, BONE);
        graphics.fill(-3, 3, 4, 7, BONE);

        graphics.fill(-3, -2, -1, 1, SHADOW);
        graphics.fill(2, -2, 4, 1, SHADOW);
        graphics.fill(0, 1, 2, 3, SHADOW);
        graphics.fill(-2, 4, -1, 7, SHADOW);
        graphics.fill(1, 4, 2, 7, SHADOW);

        graphics.pose().popMatrix();
    }
}
