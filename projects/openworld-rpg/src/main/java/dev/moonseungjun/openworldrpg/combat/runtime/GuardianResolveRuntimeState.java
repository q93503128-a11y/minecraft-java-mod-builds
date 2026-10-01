package dev.moonseungjun.openworldrpg.combat.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class GuardianResolveRuntimeState {
    public static final int MAX_PIPS = 3;
    public static final long OUT_OF_COMBAT_EXPIRY_TICKS = 200L;
    public static final double GUARDED_HIT_STAMINA_THRESHOLD = 18.0;
    public static final long GUARDED_HIT_ICD_TICKS = 40L;
    public static final double BARRIER_THRESHOLD_MAX_HP_FRACTION = 0.08;
    public static final long BARRIER_TARGET_ICD_TICKS = 80L;

    private int pips;
    private long expiryBonusTicks;
    private long lastResolveActivityTick = Long.MIN_VALUE / 4;
    private long guardedHitReadyTick = Long.MIN_VALUE / 4;
    private long standTogetherReadyTick = Long.MIN_VALUE / 4;
    private final Map<UUID, Long> barrierTargetReadyTick = new HashMap<>();
    private final Map<UUID, Double> barrierAbsorptionSinceGain =
            new HashMap<>();

    public int pips(long nowTick, long lastCombatActivityTick) {
        refresh(nowTick, lastCombatActivityTick);
        return pips;
    }

    public void synchronizeExpiryBonusTicks(long bonusTicks) {
        if (bonusTicks < 0L) {
            throw new IllegalArgumentException(
                    "Resolve expiry bonus must be non-negative."
            );
        }
        Math.addExact(OUT_OF_COMBAT_EXPIRY_TICKS, bonusTicks);
        expiryBonusTicks = bonusTicks;
    }

    public GainResult recordPerfectGuard(
            long nowTick,
            long lastCombatActivityTick
    ) {
        refresh(nowTick, lastCombatActivityTick);
        return grantPip(nowTick);
    }

    public GainResult recordGuardedHit(
            double finalStaminaCost,
            long nowTick,
            long lastCombatActivityTick
    ) {
        if (!Double.isFinite(finalStaminaCost)
                || finalStaminaCost < 0.0) {
            throw new IllegalArgumentException(
                    "Guarded-hit Stamina cost must be finite and non-negative."
            );
        }
        refresh(nowTick, lastCombatActivityTick);
        if (finalStaminaCost + 1.0e-9
                < GUARDED_HIT_STAMINA_THRESHOLD) {
            return GainResult.belowThreshold(pips);
        }
        if (nowTick < guardedHitReadyTick) {
            return GainResult.icdBlocked(pips);
        }
        guardedHitReadyTick = Math.addExact(
                nowTick,
                GUARDED_HIT_ICD_TICKS
        );
        return grantPip(nowTick);
    }

    public GainResult recordBarrierAbsorption(
            UUID recipientId,
            double absorbedDamage,
            double recipientMaxHp,
            long nowTick,
            long lastCombatActivityTick
    ) {
        Objects.requireNonNull(recipientId, "recipientId");
        if (!Double.isFinite(absorbedDamage)
                || absorbedDamage < 0.0
                || !Double.isFinite(recipientMaxHp)
                || recipientMaxHp <= 0.0) {
            throw new IllegalArgumentException(
                    "Guardian barrier absorption inputs are invalid."
            );
        }
        refresh(nowTick, lastCombatActivityTick);
        double accumulated = barrierAbsorptionSinceGain
                .getOrDefault(recipientId, 0.0)
                + absorbedDamage;
        barrierAbsorptionSinceGain.put(recipientId, accumulated);
        double threshold = recipientMaxHp
                * BARRIER_THRESHOLD_MAX_HP_FRACTION;
        if (accumulated + 1.0e-9 < threshold) {
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
                Math.addExact(nowTick, BARRIER_TARGET_ICD_TICKS)
        );
        barrierAbsorptionSinceGain.remove(recipientId);
        return grantPip(nowTick);
    }

    public boolean tryClaimStandTogether(long nowTick) {
        validateTime(nowTick);
        if (nowTick < standTogetherReadyTick) {
            return false;
        }
        standTogetherReadyTick = Math.addExact(
                nowTick,
                GuardianRootPassiveEffects.STAND_TOGETHER_ICD_TICKS
        );
        return true;
    }

    public int consumeAll(
            long nowTick,
            long lastCombatActivityTick
    ) {
        refresh(nowTick, lastCombatActivityTick);
        int consumed = pips;
        pips = 0;
        lastResolveActivityTick = nowTick;
        return consumed;
    }

    public void reset() {
        pips = 0;
        expiryBonusTicks = 0L;
        lastResolveActivityTick = Long.MIN_VALUE / 4;
        guardedHitReadyTick = Long.MIN_VALUE / 4;
        standTogetherReadyTick = Long.MIN_VALUE / 4;
        barrierTargetReadyTick.clear();
        barrierAbsorptionSinceGain.clear();
    }

    private GainResult grantPip(long nowTick) {
        int before = pips;
        if (pips < MAX_PIPS) {
            pips++;
        }
        lastResolveActivityTick = nowTick;
        return new GainResult(
                true,
                pips != before,
                false,
                pips,
                before < MAX_PIPS && pips == MAX_PIPS
        );
    }

    private void refresh(
            long nowTick,
            long lastCombatActivityTick
    ) {
        validateTime(nowTick);
        long activity = Math.max(
                lastResolveActivityTick,
                lastCombatActivityTick
        );
        if (pips > 0
                && activity > Long.MIN_VALUE / 8
                && nowTick >= Math.addExact(
                        activity,
                        Math.addExact(
                                OUT_OF_COMBAT_EXPIRY_TICKS,
                                expiryBonusTicks
                        )
                )) {
            pips = 0;
        }
        barrierTargetReadyTick.entrySet().removeIf(
                entry -> entry.getValue() <= nowTick
        );
    }

    private static void validateTime(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Resolve time must be non-negative."
            );
        }
    }

    public record GainResult(
            boolean thresholdQualified,
            boolean pipAdded,
            boolean icdBlocked,
            int currentPips,
            boolean reachedMaxNow
    ) {
        public GainResult {
            if (currentPips < 0 || currentPips > MAX_PIPS) {
                throw new IllegalArgumentException(
                        "Resolve pips must stay inside [0, 3]."
                );
            }
            if (pipAdded && (!thresholdQualified || icdBlocked)) {
                throw new IllegalArgumentException(
                        "Added Resolve requires a qualified non-ICD event."
                );
            }
            if (reachedMaxNow
                    && (!pipAdded || currentPips != MAX_PIPS)) {
                throw new IllegalArgumentException(
                        "Resolve may reach max only on a newly added pip."
                );
            }
        }

        public static GainResult belowThreshold(int pips) {
            return new GainResult(false, false, false, pips, false);
        }

        public static GainResult icdBlocked(int pips) {
            return new GainResult(true, false, true, pips, false);
        }
    }
}
