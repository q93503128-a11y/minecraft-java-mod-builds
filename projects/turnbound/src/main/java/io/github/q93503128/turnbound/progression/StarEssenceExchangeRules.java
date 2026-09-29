package io.github.q93503128.turnbound.progression;

import java.util.List;

/** Canonical permanent Star Essence exchange values and spoiler-safe choice eligibility. */
public final class StarEssenceExchangeRules {
    public static final int CRYSTAL_COST = 150;
    public static final int CRYSTAL_REWARD = 300;
    public static final int FOUR_STAR_CHOICE_COST = 450;
    public static final int FIVE_STAR_CHOICE_COST = 1_200;

    private StarEssenceExchangeRules() {}

    public static int choiceCost(int stars) {
        return switch (stars) {
            case 4 -> FOUR_STAR_CHOICE_COST;
            case 5 -> FIVE_STAR_CHOICE_COST;
            default -> throw new IllegalArgumentException("Essence character choice supports only ★4 or ★5");
        };
    }

    /**
     * Current production has no explicit per-character story-exposure flag.
     * Until that exists, owned characters are the only spoiler-safe selector pool.
     */
    public static List<String> eligibleOwnedChoices(PlayerProfile profile, int stars) {
        if (profile == null) return List.of();
        return GachaCatalog.standardPool(stars).stream()
                .filter(profile::owns)
                .toList();
    }
}
