package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectCombatRules;

/**
 * Server-owned transient state for the two R01 Earthloong Mythic unique powers.
 *
 * <p>Visual presentation is deliberately outside this state. It owns only the canon-locked
 * trigger, cooldown, barrier and numeric effect rules so reconnect/client presentation cannot
 * become damage or protection authority.</p>
 */
public final class R01EarthloongMythicEffectState {
    public static final double ROOTQUAKE_WEAPON_POWER_FRACTION = 0.40;
    public static final long ROOTQUAKE_COOLDOWN_TICKS = 160L;
    public static final double EARTHEN_REPRIEVE_MAX_HP_FRACTION = 0.08;
    public static final long EARTHEN_REPRIEVE_DURATION_TICKS = 80L;
    public static final long EARTHEN_REPRIEVE_COOLDOWN_TICKS = 200L;

    private long rootquakeReadyTick = Long.MIN_VALUE / 4;
    private long reprieveReadyTick = Long.MIN_VALUE / 4;
    private double barrierAmount;
    private long barrierUntilTick = Long.MIN_VALUE / 4;

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
        refreshBarrier(nowTick);
        if (nowTick < reprieveReadyTick) {
            return ReprieveTrigger.rejected(
                    barrierAmount,
                    barrierUntilTick,
                    reprieveReadyTick
            );
        }

        barrierAmount = maxHealth * EARTHEN_REPRIEVE_MAX_HP_FRACTION;
        barrierUntilTick = nowTick + EARTHEN_REPRIEVE_DURATION_TICKS;
        reprieveReadyTick = nowTick + EARTHEN_REPRIEVE_COOLDOWN_TICKS;
        return new ReprieveTrigger(
                true,
                barrierAmount,
                barrierUntilTick,
                reprieveReadyTick
        );
    }

    public BarrierApplication absorb(double incomingDamage, long nowTick) {
        requireFiniteNonNegative("incomingDamage", incomingDamage);
        refreshBarrier(nowTick);
        if (incomingDamage <= 0.0 || barrierAmount <= 0.0) {
            return new BarrierApplication(incomingDamage, 0.0, barrierAmount);
        }

        double absorbed = Math.min(barrierAmount, incomingDamage);
        barrierAmount -= absorbed;
        return new BarrierApplication(
                incomingDamage - absorbed,
                absorbed,
                barrierAmount
        );
    }

    public double barrierAmount(long nowTick) {
        refreshBarrier(nowTick);
        return barrierAmount;
    }

    public long rootquakeReadyTick() {
        return rootquakeReadyTick;
    }

    public long reprieveReadyTick() {
        return reprieveReadyTick;
    }

    private void refreshBarrier(long nowTick) {
        if (barrierAmount > 0.0 && nowTick >= barrierUntilTick) {
            barrierAmount = 0.0;
            barrierUntilTick = Long.MIN_VALUE / 4;
        }
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
        private static ReprieveTrigger rejected(
                double barrierAmount,
                long barrierUntilTick,
                long nextReadyTick
        ) {
            return new ReprieveTrigger(
                    false,
                    barrierAmount,
                    barrierUntilTick,
                    nextReadyTick
            );
        }
    }

    public record BarrierApplication(
            double remainingDamage,
            double absorbedDamage,
            double remainingBarrier
    ) {
    }
}
