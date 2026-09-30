package dev.moonseungjun.openworldrpg.combat.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Pure server-side state machine for the Hunter root Quarry/Focus mechanic.
 *
 * <p>Weak-point inputs are accepted only as explicit authored facts from an actor/skill adapter.
 * This state never infers anatomy from entity size, hitbox position or donor metadata.</p>
 */
public final class HunterQuarryFocusRuntimeState {
    public static final int MAX_FOCUS = 3;
    public static final long QUARRY_DURATION_TICKS = 160L;
    public static final long OUT_OF_COMBAT_FOCUS_CLEAR_TICKS = 160L;
    public static final double LONG_RANGE_MIN_BLOCKS = 7.0;
    public static final long LONG_RANGE_FOCUS_ICD_TICKS = 15L;
    public static final long WEAK_POINT_FOCUS_ICD_TICKS = 30L;
    public static final long WEAK_POINT_ULTIMATE_ICD_TICKS = 20L;
    public static final long TRAIL_SENSE_TARGET_ICD_TICKS = 400L;

    private static final long UNSET_TICK = Long.MIN_VALUE / 4;
    private static final double RANGE_EPSILON = 1.0e-9;

    private UUID quarryId;
    private long quarryExpiresAtTick = UNSET_TICK;
    private int focus;
    private long longRangeFocusReadyAtTick = UNSET_TICK;
    private long weakPointFocusReadyAtTick = UNSET_TICK;
    private long weakPointUltimateReadyAtTick = UNSET_TICK;
    private long lastObservedTick = UNSET_TICK;
    private long outOfCombatFocusClearTicks =
            OUT_OF_COMBAT_FOCUS_CLEAR_TICKS;
    private final Map<UUID, Long> trailSenseReadyAtTick =
            new HashMap<>();

    public HitApplication recordRangedHit(
            UUID targetId,
            double shotDistanceBlocks,
            boolean weakPointHit,
            long nowTick,
            long lastCombatActivityTick
    ) {
        return recordRangedHit(
                targetId,
                shotDistanceBlocks,
                weakPointHit,
                nowTick,
                lastCombatActivityTick,
                0L,
                false
        );
    }

    public HitApplication recordRangedHit(
            UUID targetId,
            double shotDistanceBlocks,
            boolean weakPointHit,
            long nowTick,
            long lastCombatActivityTick,
            long focusExpiryBonusTicks,
            boolean trailSenseEnabled
    ) {
        Objects.requireNonNull(targetId, "targetId");
        if (focusExpiryBonusTicks < 0L) {
            throw new IllegalArgumentException(
                    "Focus expiry bonus cannot be negative."
            );
        }
        if (!Double.isFinite(shotDistanceBlocks)
                || shotDistanceBlocks < 0.0) {
            throw new IllegalArgumentException(
                    "Hunter shot distance must be finite and non-negative."
            );
        }
        refresh(nowTick, lastCombatActivityTick);

        boolean newlyMarked = quarryId == null;
        boolean replaced = quarryId != null
                && !quarryId.equals(targetId);
        quarryId = targetId;
        long retentionTicks = Math.addExact(
                QUARRY_DURATION_TICKS,
                focusExpiryBonusTicks
        );
        quarryExpiresAtTick = Math.addExact(
                nowTick,
                retentionTicks
        );
        outOfCombatFocusClearTicks = Math.addExact(
                OUT_OF_COMBAT_FOCUS_CLEAR_TICKS,
                focusExpiryBonusTicks
        );

        int focusBefore = focus;
        trailSenseReadyAtTick.entrySet().removeIf(
                entry -> entry.getValue() <= nowTick
        );
        boolean trailSenseFocusGranted = false;
        if (trailSenseEnabled
                && (newlyMarked || replaced)
                && nowTick >= trailSenseReadyAtTick.getOrDefault(
                        targetId,
                        UNSET_TICK
                )
                && focus < MAX_FOCUS) {
            focus++;
            trailSenseFocusGranted = true;
            trailSenseReadyAtTick.put(
                    targetId,
                    Math.addExact(
                            nowTick,
                            TRAIL_SENSE_TARGET_ICD_TICKS
                    )
            );
        }
        boolean longRangeEligible =
                shotDistanceBlocks + RANGE_EPSILON
                        >= LONG_RANGE_MIN_BLOCKS;
        boolean longRangeFocusGranted = false;
        if (longRangeEligible
                && nowTick >= longRangeFocusReadyAtTick
                && focus < MAX_FOCUS) {
            focus++;
            longRangeFocusGranted = true;
            longRangeFocusReadyAtTick = Math.addExact(
                    nowTick,
                    LONG_RANGE_FOCUS_ICD_TICKS
            );
        }

        boolean weakPointFocusGranted = false;
        if (weakPointHit
                && nowTick >= weakPointFocusReadyAtTick
                && focus < MAX_FOCUS) {
            focus++;
            weakPointFocusGranted = true;
            weakPointFocusReadyAtTick = Math.addExact(
                    nowTick,
                    WEAK_POINT_FOCUS_ICD_TICKS
            );
        }

        boolean weakPointUltimatePublicationClaimed = false;
        if (weakPointHit
                && nowTick >= weakPointUltimateReadyAtTick) {
            weakPointUltimatePublicationClaimed = true;
            weakPointUltimateReadyAtTick = Math.addExact(
                    nowTick,
                    WEAK_POINT_ULTIMATE_ICD_TICKS
            );
        }

        return new HitApplication(
                newlyMarked,
                replaced,
                trailSenseFocusGranted,
                longRangeEligible,
                longRangeFocusGranted,
                weakPointFocusGranted,
                weakPointUltimatePublicationClaimed,
                focusBefore,
                focus,
                quarryId,
                quarryExpiresAtTick
        );
    }

    public FocusLoss onDirectHpDamage(
            long nowTick,
            long lastCombatActivityTick
    ) {
        refresh(nowTick, lastCombatActivityTick);
        int before = focus;
        if (focus > 0) {
            focus--;
        }
        return new FocusLoss(before, focus);
    }

    public boolean consumeFocusSpenderIfFull(
            long nowTick,
            long lastCombatActivityTick
    ) {
        refresh(nowTick, lastCombatActivityTick);
        if (focus < MAX_FOCUS) {
            return false;
        }
        focus = 0;
        return true;
    }

    public Snapshot snapshot(
            long nowTick,
            long lastCombatActivityTick
    ) {
        refresh(nowTick, lastCombatActivityTick);
        return new Snapshot(
                Optional.ofNullable(quarryId),
                quarryExpiresAtTick,
                focus
        );
    }

    public boolean isCurrentQuarry(
            UUID targetId,
            long nowTick,
            long lastCombatActivityTick
    ) {
        Objects.requireNonNull(targetId, "targetId");
        return snapshot(nowTick, lastCombatActivityTick)
                .quarryId()
                .filter(targetId::equals)
                .isPresent();
    }

    public void reset() {
        quarryId = null;
        quarryExpiresAtTick = UNSET_TICK;
        focus = 0;
        longRangeFocusReadyAtTick = UNSET_TICK;
        weakPointFocusReadyAtTick = UNSET_TICK;
        weakPointUltimateReadyAtTick = UNSET_TICK;
        lastObservedTick = UNSET_TICK;
        outOfCombatFocusClearTicks =
                OUT_OF_COMBAT_FOCUS_CLEAR_TICKS;
        trailSenseReadyAtTick.clear();
    }

    private void refresh(
            long nowTick,
            long lastCombatActivityTick
    ) {
        if (nowTick < lastObservedTick) {
            throw new IllegalArgumentException(
                    "Hunter runtime time must be monotonic."
            );
        }
        lastObservedTick = nowTick;

        if (quarryId != null && nowTick >= quarryExpiresAtTick) {
            quarryId = null;
            quarryExpiresAtTick = UNSET_TICK;
            focus = 0;
        }

        if (focus > 0
                && lastCombatActivityTick > UNSET_TICK
                && nowTick >= Math.addExact(
                        lastCombatActivityTick,
                        outOfCombatFocusClearTicks
                )) {
            focus = 0;
        }
    }

    public record HitApplication(
            boolean newlyMarked,
            boolean replacedPreviousQuarry,
            boolean trailSenseFocusGranted,
            boolean longRangeEligible,
            boolean longRangeFocusGranted,
            boolean weakPointFocusGranted,
            boolean weakPointUltimatePublicationClaimed,
            int focusBefore,
            int focusAfter,
            UUID quarryId,
            long quarryExpiresAtTick
    ) {
        public HitApplication {
            Objects.requireNonNull(quarryId, "quarryId");
            validateFocus(focusBefore);
            validateFocus(focusAfter);
        }
    }

    public record FocusLoss(int focusBefore, int focusAfter) {
        public FocusLoss {
            validateFocus(focusBefore);
            validateFocus(focusAfter);
            if (focusAfter > focusBefore
                    || focusBefore - focusAfter > 1) {
                throw new IllegalArgumentException(
                        "Direct HP damage may remove at most one Focus."
                );
            }
        }
    }

    public record Snapshot(
            Optional<UUID> quarryId,
            long quarryExpiresAtTick,
            int focus
    ) {
        public Snapshot {
            Objects.requireNonNull(quarryId, "quarryId");
            validateFocus(focus);
        }
    }

    private static void validateFocus(int value) {
        if (value < 0 || value > MAX_FOCUS) {
            throw new IllegalArgumentException(
                    "Hunter Focus must stay inside [0, 3]."
            );
        }
    }
}
