package io.github.q93503128.turnbound.combat;

/**
 * Content-independent combat arithmetic. This is the exact formula used by
 * the former battle resolver; balancing it is a separate design decision.
 */
public final class CombatMath {
    private CombatMath() {}

    public static int calculateDamage(int attack, int defense, double potency) {
        double reduction = defense / (double)(defense + 4L * Math.max(1, attack));
        reduction = Math.min(0.65, Math.max(0, reduction));
        return Math.max(1, (int)Math.floor(attack * potency * (1 - reduction)));
    }
}
