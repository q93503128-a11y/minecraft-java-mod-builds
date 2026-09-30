package dev.moonseungjun.openworldrpg.combat.runtime;

public final class ClericRootPassiveRuntimeState {
    public static final long BALANCED_SERVICE_WINDOW_TICKS = 80L;
    public static final long LIVING_DOCTRINE_ICD_TICKS = 120L;

    private int balancedServiceRank;
    private long supportDiscountUntilTick = Long.MIN_VALUE / 4;
    private long damageDiscountUntilTick = Long.MIN_VALUE / 4;
    private long livingDoctrineReadyTick = Long.MIN_VALUE / 4;

    public synchronized void synchronizeBalancedServiceRank(int rank) {
        if (rank < 0 || rank > 2) {
            throw new IllegalArgumentException(
                    "Balanced Service rank must be inside [0, 2]."
            );
        }
        balancedServiceRank = rank;
        if (rank == 0) {
            supportDiscountUntilTick = Long.MIN_VALUE / 4;
            damageDiscountUntilTick = Long.MIN_VALUE / 4;
        }
    }

    public synchronized void recordDamagingEligibleHit(long nowTick) {
        validateTick(nowTick);
        if (balancedServiceRank <= 0) {
            return;
        }
        supportDiscountUntilTick = Math.addExact(
                nowTick,
                BALANCED_SERVICE_WINDOW_TICKS
        );
    }

    public synchronized void recordEffectiveHealing(long nowTick) {
        validateTick(nowTick);
        if (balancedServiceRank <= 0) {
            return;
        }
        damageDiscountUntilTick = Math.addExact(
                nowTick,
                BALANCED_SERVICE_WINDOW_TICKS
        );
    }

    public synchronized double previewManaCostMultiplier(
            boolean damagingSkill,
            boolean healingOrBarrierSkill,
            long nowTick
    ) {
        validateTick(nowTick);
        if (balancedServiceRank <= 0) {
            return 1.0;
        }
        boolean eligible =
                (damagingSkill && nowTick < damageDiscountUntilTick)
                || (healingOrBarrierSkill
                        && nowTick < supportDiscountUntilTick);
        return eligible
                ? ClericRootPassiveEffects
                        .balancedServiceManaCostMultiplierForRank(
                                balancedServiceRank
                        )
                : 1.0;
    }

    public synchronized void consumeManaDiscount(
            boolean damagingSkill,
            boolean healingOrBarrierSkill,
            long nowTick
    ) {
        validateTick(nowTick);
        if (damagingSkill && nowTick < damageDiscountUntilTick) {
            damageDiscountUntilTick = Long.MIN_VALUE / 4;
        }
        if (healingOrBarrierSkill
                && nowTick < supportDiscountUntilTick) {
            supportDiscountUntilTick = Long.MIN_VALUE / 4;
        }
    }

    public synchronized boolean tryClaimLivingDoctrine(long nowTick) {
        validateTick(nowTick);
        if (nowTick < livingDoctrineReadyTick) {
            return false;
        }
        livingDoctrineReadyTick = Math.addExact(
                nowTick,
                LIVING_DOCTRINE_ICD_TICKS
        );
        return true;
    }

    public synchronized void reset() {
        balancedServiceRank = 0;
        supportDiscountUntilTick = Long.MIN_VALUE / 4;
        damageDiscountUntilTick = Long.MIN_VALUE / 4;
        livingDoctrineReadyTick = Long.MIN_VALUE / 4;
    }

    private static void validateTick(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Cleric passive runtime time must be non-negative."
            );
        }
    }
}
