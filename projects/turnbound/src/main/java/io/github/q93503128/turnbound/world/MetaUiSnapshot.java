package io.github.q93503128.turnbound.world;

import java.util.List;

/** Immutable server-authored state for the RPG management menu. */
public record MetaUiSnapshot(
        long gold, long crystal, long starEssence, long awakeningCore, int partyCp, boolean riftUnlocked,
        int fiveStarPity, boolean starterArchiveAvailable,
        List<String> activeParty, List<List<String>> partyPresets,
        List<CharacterRow> characters, List<EquipmentRow> equipment,
        List<EndgameRow> endgame, List<ChallengeRow> challenges, List<RegionQuestRow> regionQuests,
        List<ArchiveRow> archiveHistory, List<ShopRow> shopItems, List<CodexRow> codex) {
    public MetaUiSnapshot {
        activeParty = List.copyOf(activeParty);
        partyPresets = partyPresets.stream().map(List::copyOf).toList();
        characters = List.copyOf(characters);
        equipment = List.copyOf(equipment);
        endgame = List.copyOf(endgame);
        challenges = List.copyOf(challenges);
        regionQuests = List.copyOf(regionQuests);
        archiveHistory = List.copyOf(archiveHistory);
        shopItems = List.copyOf(shopItems);
        codex = List.copyOf(codex);
    }

    public record CharacterRow(
            String id, String name, boolean owned, int nativeStar, int level, int star, boolean awakened,
            int cp, boolean active, String role, String primaryRole, String difficulty, boolean profileUnlocked,
            int hp, int attack, int defense, int speed) {}

    public record EquipmentRow(
            String instanceId, String itemId, String name, String tier, String slot, int enhancement,
            String equippedCharacterId, String mainType, double mainValue, String subType, double subValue,
            double mainAt20, double subAt20, int salePrice, boolean sellable) {}

    public record EndgameRow(String id, String kind, String label, boolean unlocked, boolean cleared, int level, boolean hardPattern) {}
    public record ChallengeRow(String id, int ordinal, String label, boolean completed, boolean autoEvaluable, String unresolvedReason) {}
    public record RegionQuestRow(String id, String region, boolean objectiveSpecified, boolean completed, String chestRule) {}
    public record ArchiveRow(String characterId, String name, int nativeStars, boolean newlyOwned, int essenceGranted, int pityAfter) {}
    public record ShopRow(String itemId, String name, String tier, String slot, int price, boolean unlocked) {}
    public record CodexRow(String category, String id, String name, boolean discovered, boolean detailUnlocked, String summary) {}
}
