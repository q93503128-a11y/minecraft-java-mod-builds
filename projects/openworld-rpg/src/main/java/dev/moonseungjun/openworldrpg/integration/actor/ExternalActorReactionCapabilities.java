package dev.moonseungjun.openworldrpg.integration.actor;

/**
 * Explicit presentation/physics acceptance for authored hostile reactions.
 *
 * <p>Normal-vs-elite cannot be guessed from donor identity. Forced movement stays disabled until an
 * actor binding explicitly proves the matching animation/body behavior.</p>
 */
public record ExternalActorReactionCapabilities(
        boolean pullToward,
        boolean knockdown,
        boolean launch,
        boolean hunterQuickstepPierceable
) {
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
                false
        );
    }

    public static ExternalActorReactionCapabilities normalBaseline() {
        return new ExternalActorReactionCapabilities(
                true,
                true,
                true,
                true
        );
    }
}
