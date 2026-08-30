package io.github.q93503128.turnbound.world;

import java.util.List;

/** Immutable server-authored state for the RPG management menu. */
public record MetaUiSnapshot(
        long gold, long crystal, long starEssence, long awakeningCore, int partyCp, boolean riftUnlocked,
        List<String> activeParty, List<CharacterRow> characters, List<EndgameRow> endgame,
        List<ChallengeRow> challenges, List<RegionQuestRow> regionQuests) {
    public MetaUiSnapshot {
        activeParty = List.copyOf(activeParty);
        characters = List.copyOf(characters);
        endgame = List.copyOf(endgame);
        challenges = List.copyOf(challenges);
        regionQuests = List.copyOf(regionQuests);
    }
    public record CharacterRow(String id, String name, int level, int star, boolean awakened, int cp, boolean active) {}
    public record EndgameRow(String id, String kind, String label, boolean unlocked, boolean cleared, int level, boolean hardPattern) {}
    public record ChallengeRow(String id, int ordinal, String label, boolean completed, boolean autoEvaluable, String unresolvedReason) {}
    public record RegionQuestRow(String id, String region, boolean objectiveSpecified, boolean completed, String chestRule) {}
}
