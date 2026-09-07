package kr.moonseungjun.turnboundre.progression;

/** Persisted per-character progression. originStar is copied from CharacterDefinition and never mutated. */
public record CharacterProgress(String characterId, int originStar, int currentStar, int level) {
    public CharacterProgress {
        if (characterId == null || characterId.isBlank()) throw new IllegalArgumentException("characterId must not be blank");
        if (originStar < 1 || originStar > 5) throw new IllegalArgumentException("originStar must be 1..5");
        if (currentStar < originStar || currentStar > 6) throw new IllegalArgumentException("currentStar must be originStar..6");
        if (level < 1 || level > ProgressionRules.levelCap(currentStar)) {
            throw new IllegalArgumentException("level outside currentStar cap");
        }
    }
}
