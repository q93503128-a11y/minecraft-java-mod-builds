package io.github.q93503128.turnbound.progression;

/** Pure v0.4 star-promotion and awakening milestone state. */
public final class CharacterGrowthRules {
    public record State(
            int currentStar,
            boolean awakened,
            boolean characterQuestComplete,
            boolean signatureTrialCleared) {
        public State {
            if (currentStar < 1 || currentStar > 6) throw new IllegalArgumentException("currentStar must be 1..6");
            if (awakened && currentStar != 6) throw new IllegalArgumentException("Awakened character must be ★6");
            if (signatureTrialCleared && !characterQuestComplete) {
                throw new IllegalArgumentException("Signature Trial cannot precede character quest completion");
            }
        }

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

    /** Star Essence cost for currentStar -> currentStar+1. */
    public static int promotionCost(int currentStar) {
        return switch (currentStar) {
            case 1 -> 20;
            case 2 -> 50;
            case 3 -> 120;
            case 4 -> 250;
            case 5 -> 500;
            default -> throw new IllegalArgumentException("★6 cannot be promoted further");
        };
    }

    public static int levelCap(int currentStar) {
        return switch (currentStar) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            case 4 -> 40;
            case 5 -> 50;
            case 6 -> 60;
            default -> throw new IllegalArgumentException("currentStar must be 1..6");
        };
    }

    public static double promotionMultiplier(int nativeStar, int currentStar) {
        if (nativeStar < 1 || nativeStar > 5 || currentStar < nativeStar || currentStar > 6) {
            throw new IllegalArgumentException("Invalid native/current star pair");
        }
        double value = 1.0;
        for (int star = nativeStar; star < currentStar; star++) {
            value *= switch (star) {
                case 1 -> 1.06;
                case 2 -> 1.07;
                case 3 -> 1.08;
                case 4 -> 1.10;
                case 5 -> 1.12;
                default -> 1.0;
            };
        }
        return value;
    }
}
