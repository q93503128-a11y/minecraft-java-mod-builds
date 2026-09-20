package io.github.q93503128.turnbound.progression;

/**
 * TURNBOUND v1 character-growth state.
 *
 * <p>{@code currentStar} is retained only for save compatibility with v0.4. Formal roster rarity no longer
 * promotes repeatedly and gameplay always uses native rarity; Level 1..60 and one Awakening are the growth axes.</p>
 */
public final class CharacterGrowthRules {
    public record State(
            int currentStar,
            boolean awakened,
            boolean characterQuestComplete,
            boolean signatureTrialCleared) {
        public State {
            if (currentStar < 1 || currentStar > 6) throw new IllegalArgumentException("legacy currentStar must be 1..6");
            if (signatureTrialCleared && !characterQuestComplete) {
                throw new IllegalArgumentException("Signature Trial cannot precede character quest completion");
            }
        }

        /** Save-compatibility helper only; no production action promotes rarity in v1. */
        public State withStar(int star) {
            return new State(star, awakened, characterQuestComplete, signatureTrialCleared);
        }

        public State withCharacterQuestComplete() {
            return new State(currentStar, awakened, true, signatureTrialCleared);
        }

        public State withSignatureTrialCleared() {
            return new State(currentStar, awakened, true, true);
        }

        public State withAwakened() {
            return new State(currentStar, true, characterQuestComplete, signatureTrialCleared);
        }
    }

    private CharacterGrowthRules() {}

    public static State initial(String characterId) {
        return new State(GachaCatalog.nativeStars(characterId), false, false, false);
    }

    /** Legacy API retained for source compatibility; production has no repeat-rarity promotion action. */
    @Deprecated
    public static int promotionCost(int legacyCurrentStar) {
        throw new UnsupportedOperationException("Repeated rarity promotion is not a TURNBOUND v1 growth system");
    }

    /** Legacy API retained for stat callers; rarity promotion contributes no v1 stat multiplier. */
    @Deprecated
    public static double promotionMultiplier(int nativeStar, int legacyCurrentStar) {
        return 1.0;
    }

    /** Rarity no longer gates levels; every formal v1 character uses Level 1..60. */
    public static int levelCap(int ignoredLegacyCurrentStar) {
        return GrowthRulesV1.maxLevel();
    }
}
