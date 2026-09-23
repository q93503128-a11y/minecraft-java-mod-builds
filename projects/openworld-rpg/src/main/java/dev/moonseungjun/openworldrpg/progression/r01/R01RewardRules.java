package dev.moonseungjun.openworldrpg.progression.r01;

/** Exact progression/Gold portions of currently bindable R01 one-time rewards. */
public final class R01RewardRules {
    public static final RewardRule DUST_ON_QUARRY_ROAD =
            new RewardRule(0.70, 0.50, 90L);

    /** Earthloong first-eligible boss-kill layer. */
    public static final RewardRule EARTHLOONG_FIRST_BOSS_LAYER =
            new RewardRule(0.40, 0.20, 0L);

    /**
     * First quarry dungeon-completion layer after Earthloong.
     *
     * <p>This remains separate from the boss-kill layer so each canonical "current requirement"
     * percentage is evaluated at the point that layer is committed. Item/choice rewards are
     * separate until their item-authority path is bound.</p>
     */
    public static final RewardRule EARTHLOONG_FIRST_DUNGEON_COMPLETION =
            new RewardRule(1.00, 0.64, 180L);

    private R01RewardRules() {
    }

    public record RewardRule(
            double combatRequirementFraction,
            double classRequirementFraction,
            long gold
    ) {
        public RewardRule {
            if (!Double.isFinite(combatRequirementFraction)
                    || combatRequirementFraction < 0.0) {
                throw new IllegalArgumentException(
                        "combatRequirementFraction must be finite and non-negative."
                );
            }
            if (!Double.isFinite(classRequirementFraction)
                    || classRequirementFraction < 0.0) {
                throw new IllegalArgumentException(
                        "classRequirementFraction must be finite and non-negative."
                );
            }
            if (gold < 0L) {
                throw new IllegalArgumentException("gold cannot be negative.");
            }
        }
    }
}
