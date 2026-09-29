package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;

/**
 * Server-owned transient trigger/cooldown state for the two R01 Earthloong Mythic unique powers.
 *
 * <p>Earthen Reprieve's barrier amount is authored here, but the actual barrier layer is granted
 * through the shared project Barrier authority so it cannot bypass the global stacking cap.</p>
 */
public final class R01EarthloongMythicEffectState {
    public static final double ROOTQUAKE_WEAPON_POWER_FRACTION = 0.40;
    public static final long ROOTQUAKE_COOLDOWN_TICKS = 160L;
    public static final double EARTHEN_REPRIEVE_MAX_HP_FRACTION = 0.08;
    public static final int EARTHEN_REPRIEVE_DURATION_TICKS = 80;
    public static final long EARTHEN_REPRIEVE_COOLDOWN_TICKS = 200L;

    private long rootquakeReadyTick = Long.MIN_VALUE / 4;
    private long reprieveReadyTick = Long.MIN_VALUE / 4;

    public RootquakeTrigger tryRootquake(double weaponPower, long nowTick) {
        requireFiniteNonNegative("weaponPower", weaponPower);
        if (weaponPower <= 0.0 || nowTick < rootquakeReadyTick) {
            return RootquakeTrigger.rejected(rootquakeReadyTick);
        }

        double damage = ProjectCombatRules.roundFinal(
                weaponPower * ROOTQUAKE_WEAPON_POWER_FRACTION
        );
        rootquakeReadyTick = nowTick + ROOTQUAKE_COOLDOWN_TICKS;
        return new RootquakeTrigger(true, damage, rootquakeReadyTick);
    }

    public ReprieveTrigger tryEarthenReprieve(double maxHealth, long nowTick) {
        requireFinitePositive("maxHealth", maxHealth);
        if (nowTick < reprieveReadyTick) {
            return ReprieveTrigger.rejected(reprieveReadyTick);
        }

        double authoredBarrier = maxHealth
                * EARTHEN_REPRIEVE_MAX_HP_FRACTION;
        long barrierUntilTick = nowTick
                + EARTHEN_REPRIEVE_DURATION_TICKS;
        reprieveReadyTick = nowTick
                + EARTHEN_REPRIEVE_COOLDOWN_TICKS;
        return new ReprieveTrigger(
                true,
                authoredBarrier,
                barrierUntilTick,
                reprieveReadyTick
        );
    }

    public long rootquakeReadyTick() {
        return rootquakeReadyTick;
    }

    public long reprieveReadyTick() {
        return reprieveReadyTick;
    }

    private static void requireFiniteNonNegative(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative.");
        }
    }

    private static void requireFinitePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalArgumentException(name + " must be finite and positive.");
        }
    }

    public record RootquakeTrigger(
            boolean triggered,
            double damage,
            long nextReadyTick
    ) {
        private static RootquakeTrigger rejected(long nextReadyTick) {
            return new RootquakeTrigger(false, 0.0, nextReadyTick);
        }
    }

    public record ReprieveTrigger(
            boolean triggered,
            double barrierAmount,
            long barrierUntilTick,
            long nextReadyTick
    ) {
        private static ReprieveTrigger rejected(long nextReadyTick) {
            return new ReprieveTrigger(
                    false,
                    0.0,
                    Long.MIN_VALUE / 4,
                    nextReadyTick
            );
        }
    }
}
