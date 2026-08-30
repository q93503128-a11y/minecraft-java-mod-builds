package io.github.q93503128.turnbound.world;

import java.util.List;

/** Immutable server-authored state for the RPG management menu. */
public record MetaUiSnapshot(
        long gold, long crystal, long starEssence, long awakeningCore, int partyCp, boolean riftUnlocked,
        int fiveStarPity, boolean starterArchiveAvailable,
        List<String> activeParty, List<CharacterRow> characters, List<EndgameRow> endgame,
        List<ChallengeRow> challenges, List<RegionQuestRow> regionQuests,
        List<ArchiveRow> archiveHistory, List<ShopRow> shopItems) {
    public MetaUiSnapshot {
        activeParty = List.copyOf(activeParty);
        characters = List.copyOf(characters);
        endgame = List.copyOf(endgame);
        challenges = List.copyOf(challenges);
        regionQuests = List.copyOf(regionQuests);
        archiveHistory = List.copyOf(archiveHistory);
        shopItems = List.copyOf(shopItems);
    }
    public record CharacterRow(String id, String name, int level, int star, boolean awakened, int cp, boolean active) {}
    public record EndgameRow(String id, String kind, String label, boolean unlocked, boolean cleared, int level, boolean hardPattern) {}
    public record ChallengeRow(String id, int ordinal, String label, boolean completed, boolean autoEvaluable, String unresolvedReason) {}
    public record RegionQuestRow(String id, String region, boolean objectiveSpecified, boolean completed, String chestRule) {}
    public record ArchiveRow(String characterId, String name, int nativeStars, boolean newlyOwned, int essenceGranted, int pityAfter) {}
    public record ShopRow(String itemId, String name, String tier, String slot, int price, boolean unlocked) {}
}
