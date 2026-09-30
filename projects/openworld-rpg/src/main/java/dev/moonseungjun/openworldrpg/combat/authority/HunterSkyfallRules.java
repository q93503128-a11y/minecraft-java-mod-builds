package dev.moonseungjun.openworldrpg.combat.authority;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import java.util.Objects;

public final class HunterSkyfallRules {
    public static final double RADIUS_BLOCKS = 7.0;
    public static final int DURATION_TICKS = 90;
    public static final int PULSE_COUNT = 5;
    public static final int FIRST_PULSE_DELAY_TICKS = 8;
    public static final int PULSE_INTERVAL_TICKS = 18;
    public static final double TOTAL_ACTION_COEFFICIENT = 5.50;
    public static final double TOTAL_POISE_COEFFICIENT = 3.00;
    public static final double ACTION_COEFFICIENT_PER_PULSE =
            TOTAL_ACTION_COEFFICIENT / PULSE_COUNT;
    public static final double POISE_COEFFICIENT_PER_PULSE =
            TOTAL_POISE_COEFFICIENT / PULSE_COUNT;

    public static final int CAST_TICKS = 10;
    public static final double TARGET_RANGE_BLOCKS = 32.0;
    public static final double VISUAL_LAUNCH_HEIGHT_BLOCKS = 12.0;
    public static final int VISUAL_PROJECTILE_COUNT = 15;
    public static final int VISUAL_EXTRA_LAUNCH_COUNT =
            VISUAL_PROJECTILE_COUNT - 1;
    public static final int VISUAL_LAUNCH_DELAY_TICKS = 6;
    public static final double VISUAL_PROJECTILE_VELOCITY = 1.5;

    private static final double GOLDEN_ANGLE_RADIANS =
            Math.PI * (3.0 - Math.sqrt(5.0));

    private HunterSkyfallRules() {
    }

    public static VisualOffset visualOffset(int sequenceIndex) {
        if (sequenceIndex < 0
                || sequenceIndex >= VISUAL_PROJECTILE_COUNT) {
            throw new IllegalArgumentException(
                    "Skyfall visual sequence index out of range."
            );
        }
        if (sequenceIndex == 0) {
            return new VisualOffset(0.0, 0.0);
        }
        double fraction = sequenceIndex
                / (double) (VISUAL_PROJECTILE_COUNT - 1);
        double radius = RADIUS_BLOCKS
                * 0.92
                * Math.sqrt(fraction);
        double angle = GOLDEN_ANGLE_RADIANS
                * sequenceIndex;
        return new VisualOffset(
                Math.cos(angle) * radius,
                Math.sin(angle) * radius
        );
    }

    public static double movementMultiplier(
            ExternalActorCombatProfile.CombatRank rank
    ) {
        Objects.requireNonNull(rank, "rank");
        return rank == ExternalActorCombatProfile.CombatRank.NORMAL_ELITE
                ? 0.75
                : 0.90;
    }

    public record VisualOffset(double x, double z) {
        public VisualOffset {
            if (!Double.isFinite(x) || !Double.isFinite(z)) {
                throw new IllegalArgumentException(
                        "Skyfall visual offset must be finite."
                );
            }
        }

        public double radius() {
            return Math.sqrt(x * x + z * z);
        }
    }
}
