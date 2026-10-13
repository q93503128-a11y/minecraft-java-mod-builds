package dev.moonseungjun.openworldrpg.mount;

/** Canon-locked Trail Stag values that do not depend on final model/collision acceptance. */
public final class TrailStagMountRules {
    public static final String TRAIL_STAG_ID = "openworld_rpg:trail_stag";

    public static final int SUMMON_CHANNEL_TICKS = 20;
    public static final int HOSTILE_COMBAT_QUIET_TICKS = 80;
    public static final int RESOLVE_BREAK_RESUMMON_COOLDOWN_TICKS = 240;

    public static final double RESOLVE_MAX_HP_MULTIPLIER = 1.00;
    public static final double CRUISE_BLOCKS_PER_SECOND = 6.4;
    public static final double ZERO_TO_NINETY_SECONDS = 1.0;
    public static final double NINETY_TO_STOP_SECONDS = 0.75;
    public static final double MAX_AUTHORED_STEP_UP_BLOCKS = 1.25;
    public static final double FULL_SPEED_YAW_DEGREES_PER_SECOND = 120.0;
    public static final double LOW_SPEED_YAW_DEGREES_PER_SECOND = 180.0;
    public static final double JUMP_APEX_BLOCKS = 1.65;
    public static final double JUMP_HORIZONTAL_VELOCITY_RETENTION = 0.85;
    public static final int JUMP_LANDING_RECOVERY_TICKS = 3;

    private TrailStagMountRules() {
    }

    public static double resolveMax(double riderMaxHp) {
        if (!Double.isFinite(riderMaxHp) || riderMaxHp <= 0.0) {
            throw new IllegalArgumentException("Rider max HP must be finite and positive.");
        }
        return riderMaxHp * RESOLVE_MAX_HP_MULTIPLIER;
    }
}
