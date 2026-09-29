package dev.moonseungjun.openworldrpg.combat.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Server-owned runtime for the Cleric root mechanic.
 *
 * <p>Grace is intentionally a small combat mechanic rather than another persistent progression
 * currency. It tracks the three canonical gain channels and consumes all three pips on the next
 * Grace-spender. The caller supplies the project combat-activity tick so ordinary combat can keep
 * existing Grace alive without inventing a second combat detector.</p>
 */
public final class ClericGraceRuntimeState {
    public static final int MAX_PIPS = 3;
    public static final long OUT_OF_COMBAT_EXPIRY_TICKS = 200L;
    public static final long DAMAGE_GAIN_ICD_TICKS = 20L;
    public static final long SUPPORT_TARGET_ICD_TICKS = 40L;
    public static final double SUPPORT_THRESHOLD_MAX_HP_FRACTION = 0.06;

    private int pips;
    private long lastGraceActivityTick = Long.MIN_VALUE / 4;
    private long damagingActiveReadyTick = Long.MIN_VALUE / 4;
    private final Map<UUID, Long> healTargetReadyTick = new HashMap<>();
    private final Map<UUID, Long> barrierTargetReadyTick = new HashMap<>();

    public int pips(long nowTick, long lastCombatActivityTick) {
        refresh(nowTick, lastCombatActivityTick);
        return pips;
    }

    public GainResult recordDamagingActiveHit(
            long nowTick,
            long lastCombatActivityTick
    ) {
        validateTime(nowTick);
        refresh(nowTick, lastCombatActivityTick);
        if (nowTick < damagingActiveReadyTick) {
            return GainResult.icdBlocked(pips);
        }
        damagingActiveReadyTick = Math.addExact(
                nowTick,
                DAMAGE_GAIN_ICD_TICKS
        );
        return grantPip(nowTick);
    }

    public GainResult recordEffectiveHeal(
            UUID recipientId,
            double effectiveHealing,
            double recipientMaxHp,
            long nowTick,
            long lastCombatActivityTick
    ) {
        Objects.requireNonNull(recipientId, "recipientId");
        validateSupportAmount(
                effectiveHealing,
                recipientMaxHp,
                "effectiveHealing"
        );
        refresh(nowTick, lastCombatActivityTick);
        if (effectiveHealing + 1.0e-9
                < recipientMaxHp * SUPPORT_THRESHOLD_MAX_HP_FRACTION) {
            return GainResult.belowThreshold(pips);
        }
        long ready = healTargetReadyTick.getOrDefault(
                recipientId,
                Long.MIN_VALUE / 4
        );
        if (nowTick < ready) {
            return GainResult.icdBlocked(pips);
        }
        healTargetReadyTick.put(
                recipientId,
                Math.addExact(nowTick, SUPPORT_TARGET_ICD_TICKS)
        );
        return grantPip(nowTick);
    }

    public GainResult recordConsumedBarrier(
            UUID recipientId,
            double barrierConsumedByHostileDamage,
            double recipientMaxHp,
            long nowTick,
            long lastCombatActivityTick
    ) {
        Objects.requireNonNull(recipientId, "recipientId");
        validateSupportAmount(
                barrierConsumedByHostileDamage,
                recipientMaxHp,
                "barrierConsumedByHostileDamage"
        );
        refresh(nowTick, lastCombatActivityTick);
        if (barrierConsumedByHostileDamage + 1.0e-9
                < recipientMaxHp * SUPPORT_THRESHOLD_MAX_HP_FRACTION) {
            return GainResult.belowThreshold(pips);
        }
        long ready = barrierTargetReadyTick.getOrDefault(
                recipientId,
                Long.MIN_VALUE / 4
        );
        if (nowTick < ready) {
            return GainResult.icdBlocked(pips);
        }
        barrierTargetReadyTick.put(
                recipientId,
                Math.addExact(nowTick, SUPPORT_TARGET_ICD_TICKS)
        );
        return grantPip(nowTick);
    }

    /**
     * Consumes exactly a full three-pip Grace state for one authored Grace-spender.
     */
    public boolean consumeForSpender(
            long nowTick,
            long lastCombatActivityTick
    ) {
        refresh(nowTick, lastCombatActivityTick);
        if (pips < MAX_PIPS) {
            return false;
        }
        pips = 0;
        lastGraceActivityTick = nowTick;
        return true;
    }

    public void reset() {
        pips = 0;
        lastGraceActivityTick = Long.MIN_VALUE / 4;
        damagingActiveReadyTick = Long.MIN_VALUE / 4;
        healTargetReadyTick.clear();
        barrierTargetReadyTick.clear();
    }

    private GainResult grantPip(long nowTick) {
        boolean added = pips < MAX_PIPS;
        if (added) {
            pips++;
        }
        lastGraceActivityTick = nowTick;
        return new GainResult(true, added, false, pips);
    }

    private void refresh(
            long nowTick,
            long lastCombatActivityTick
    ) {
        validateTime(nowTick);
        long activity = Math.max(
                lastGraceActivityTick,
                lastCombatActivityTick
        );
        if (pips > 0
                && activity > Long.MIN_VALUE / 8
                && nowTick >= Math.addExact(
                        activity,
                        OUT_OF_COMBAT_EXPIRY_TICKS
                )) {
            pips = 0;
        }
        healTargetReadyTick.entrySet().removeIf(
                entry -> entry.getValue() <= nowTick
        );
        barrierTargetReadyTick.entrySet().removeIf(
                entry -> entry.getValue() <= nowTick
        );
    }

    private static void validateSupportAmount(
            double amount,
            double maxHp,
            String amountName
    ) {
        if (!Double.isFinite(amount) || amount < 0.0) {
            throw new IllegalArgumentException(
                    amountName + " must be finite and non-negative."
            );
        }
        if (!Double.isFinite(maxHp) || maxHp <= 0.0) {
            throw new IllegalArgumentException(
                    "recipientMaxHp must be finite and positive."
            );
        }
    }

    private static void validateTime(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Grace time must be non-negative."
            );
        }
    }

    public record GainResult(
            boolean thresholdQualified,
            boolean pipAdded,
            boolean icdBlocked,
            int currentPips
    ) {
        public GainResult {
            if (currentPips < 0 || currentPips > MAX_PIPS) {
                throw new IllegalArgumentException(
                        "Grace pips must stay inside [0, 3]."
                );
            }
            if (pipAdded && (!thresholdQualified || icdBlocked)) {
                throw new IllegalArgumentException(
                        "Added Grace pip requires a qualified non-ICD event."
                );
            }
        }

        public static GainResult belowThreshold(int pips) {
            return new GainResult(false, false, false, pips);
        }

        public static GainResult icdBlocked(int pips) {
            return new GainResult(true, false, true, pips);
        }
    }
}
