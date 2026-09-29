package io.github.q93503128.turnbound.progression;

/**
 * TURNBOUND v1 character-growth state.
 *
 * <p>Battle/quest XP owns the base level axis from Lv1..60. Duplicate acquisition owns a separate +0..+10 bonus
 * level axis, allowing an effective combat level of 70 without changing the base XP cap.</p>
 *
 * <p>{@code currentStar} is preserved because the historical native/current-star progression contract is still
 * under explicit canon review. Duplicate bonus levels are independent from rarity promotion and do not decide,
 * restore or remove any pending ★6/Awakening rule.</p>
 */
public final class CharacterGrowthRules {
    public record State(
            int currentStar,
            boolean awakened,
            boolean characterQuestComplete,
            boolean signatureTrialCleared) {
        public State {
            if (currentStar < 1 || currentStar > 6) throw new IllegalArgumentException("stored currentStar must be 1..6");
            if (signatureTrialCleared && !characterQuestComplete) {
                throw new IllegalArgumentException("Signature Trial cannot precede character quest completion");
            }
        }

        /** Stored-state helper retained while the promotion/★6 contract is unresolved. */
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

    /** Compatibility API; the current runtime exposes no promotion action while canon is unresolved. */
    @Deprecated
    public static int promotionCost(int legacyCurrentStar) {
        throw new UnsupportedOperationException("Repeated rarity promotion is not a TURNBOUND v1 growth system");
    }

    /** Compatibility API; current runtime stats do not apply a promotion multiplier while canon is unresolved. */
    @Deprecated
    public static double promotionMultiplier(int nativeStar, int legacyCurrentStar) {
        return 1.0;
    }

    /** Current v1 level runtime uses Level 1..60 independently from the unresolved star-growth contract. */
    public static int levelCap(int ignoredLegacyCurrentStar) {
        return GrowthRulesV1.maxLevel();
    }
}
