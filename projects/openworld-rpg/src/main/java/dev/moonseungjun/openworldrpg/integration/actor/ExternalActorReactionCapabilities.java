package dev.moonseungjun.openworldrpg.integration.actor;

/**
 * Explicit presentation/physics acceptance for authored hostile reactions.
 *
 * <p>Normal-vs-elite cannot be guessed from donor identity. Forced movement stays disabled until an
 * actor binding explicitly proves the matching animation/body behavior. Pull strength is authored
 * explicitly so an elite can accept the same reaction at half strength without guessing from its
 * donor type.</p>
 */
public record ExternalActorReactionCapabilities(
        boolean pullToward,
        boolean knockdown,
        boolean launch,
        boolean hunterQuickstepPierceable,
        double pullStrengthMultiplier
) {
    public ExternalActorReactionCapabilities {
        if (!Double.isFinite(pullStrengthMultiplier)
                || pullStrengthMultiplier < 0.0
                || pullStrengthMultiplier > 1.0
                || (!pullToward
                        && pullStrengthMultiplier != 0.0)) {
            throw new IllegalArgumentException(
                    "Invalid authored pull-strength multiplier."
            );
        }
    }

    public ExternalActorReactionCapabilities(
            boolean pullToward,
            boolean knockdown,
            boolean launch,
            boolean hunterQuickstepPierceable
    ) {
        this(
                pullToward,
                knockdown,
                launch,
                hunterQuickstepPierceable,
                pullToward ? 1.0 : 0.0
        );
    }

    public ExternalActorReactionCapabilities(
            boolean pullToward,
            boolean knockdown,
            boolean launch
    ) {
        this(pullToward, knockdown, launch, false);
    }

    public static ExternalActorReactionCapabilities none() {
        return new ExternalActorReactionCapabilities(
                false,
                false,
                false,
                false,
                0.0
        );
    }

    public static ExternalActorReactionCapabilities normalBaseline() {
        return new ExternalActorReactionCapabilities(
                true,
                true,
                true,
                true,
                1.0
        );
    }
}
