package dev.moonseungjun.openworldrpg.progression.r01;

/** Exact progression/Gold portions of currently bindable R01 one-time rewards. */
public final class R01RewardRules {
    public static final RewardRule DUST_ON_QUARRY_ROAD =
            new RewardRule(0.70, 0.50, 90L);

    /**
     * Earthloong first clear combines the boss layer (40% / 20%) and dungeon-completion layer
     * (100% / 64%). Item/choice rewards remain separate until their item-authority path is bound.
     */
    public static final RewardRule EARTHLOONG_FIRST_CLEAR_PROGRESSION =
            new RewardRule(1.40, 0.84, 180L);

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
