package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.ChallengeCatalog;
import io.github.q93503128.turnbound.content.QuestCatalog;
import io.github.q93503128.turnbound.content.RegionQuestCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.EquipmentRules;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Commands and snapshots for the v0.4 RPG management menu. All mutations are validated server-side. */
public final class MetaMenuService {
    private MetaMenuService() {}

    public static MetaUiSnapshot snapshot(ServerPlayer player) {
        var id = player.getUUID();
        var campaign = CampaignProgressStore.snapshot(id);
        List<String> party = CampaignProgressStore.activeParty(id);
        Set<String> clears = campaign.clearedEncounters();
        Set<String> challengeClears = ChallengeService.completed(id);
        int partyCp = party.stream().mapToInt(characterId -> CampaignProgressStore.combatPower(id, characterId)).sum();

        List<MetaUiSnapshot.CharacterRow> characters = campaign.profile().ownedCharacters().stream().sorted().map(characterId -> {
            var level = CampaignProgressStore.character(id, characterId);
            var growth = CampaignProgressStore.growth(id, characterId);
            return new MetaUiSnapshot.CharacterRow(characterId, CanonicalData.definition(characterId).name(), level.level(),
                    growth.currentStar(), growth.awakened(), CampaignProgressStore.combatPower(id, characterId), party.contains(characterId));
        }).toList();

        List<MetaUiSnapshot.EndgameRow> endgame = new ArrayList<>();
        int[] baseLevels = {6, 10, 13, 16, 20};
        for (int i = 1; i <= 5; i++) {
            String bossId = "B0" + i;
            String encounterId = EndgameEncounterCatalog.hardId(bossId);
            endgame.add(new MetaUiSnapshot.EndgameRow(encounterId, "HARD", CanonicalData.definition(bossId).name() + " Hard",
                    EndgameEncounterCatalog.unlocked(id, encounterId), clears.contains(encounterId), baseLevels[i - 1], true));
        }
        boolean riftUnlocked = clears.contains("BATTLE_B05");
        for (int floor = 1; floor <= 30; floor++) {
            V04Catalogs.RiftFloor spec = V04Catalogs.riftFloor(floor);
            String encounterId = EndgameEncounterCatalog.riftId(floor);
            endgame.add(new MetaUiSnapshot.EndgameRow(encounterId, "RIFT", "Rift F" + floor,
                    riftUnlocked, clears.contains(encounterId), spec.level(), spec.hardBossPattern()));
        }

        List<MetaUiSnapshot.ChallengeRow> challenges = ChallengeCatalog.all().stream()
                .map(c -> new MetaUiSnapshot.ChallengeRow(c.id(), c.ordinal(), c.label(), challengeClears.contains(c.id()),
                        c.autoEvaluable(), c.unresolvedReason())).toList();
        List<MetaUiSnapshot.RegionQuestRow> regionQuests = RegionQuestCatalog.all().stream()
                .map(q -> new MetaUiSnapshot.RegionQuestRow(q.id(), q.region(), q.objectiveSpecified(),
                        campaign.quests().completed().contains(q.id()), q.chestRule())).toList();

        List<MetaUiSnapshot.ArchiveRow> archive = new ArrayList<>();
        List<io.github.q93503128.turnbound.progression.PlayerProfile.SummonHistory> source = campaign.profile().summonHistory();
        for (int i = source.size() - 1; i >= 0; i--) {
            var row = source.get(i);
            archive.add(new MetaUiSnapshot.ArchiveRow(row.characterId(), CanonicalData.definition(row.characterId()).name(),
                    row.nativeStars(), row.newlyOwned(), row.starEssenceGranted(), row.pityAfter()));
        }

        int shopChapter = currentShopChapter(campaign.quests().completed());
        List<MetaUiSnapshot.ShopRow> shop = V04Catalogs.equipment().stream()
                .filter(item -> item.tier().equals("T1") || item.tier().equals("T2"))
                .sorted(Comparator.comparing(V04Catalogs.EquipmentSpec::tier)
                        .thenComparing(V04Catalogs.EquipmentSpec::slot)
                        .thenComparing(V04Catalogs.EquipmentSpec::id))
                .map(item -> {
                    int unlockChapter = item.tier().equals("T1") ? 1 : 2;
                    return new MetaUiSnapshot.ShopRow(item.id(), item.name(), item.tier(), item.slot(),
                            EquipmentRules.shopPrice(item.tier()), shopChapter >= unlockChapter);
                }).toList();

        return new MetaUiSnapshot(
                campaign.profile().gold(), campaign.profile().summonCrystal(), campaign.profile().starEssence(),
                campaign.profile().awakeningCore(), partyCp, riftUnlocked,
                campaign.profile().fiveStarPity(), CampaignProgressStore.starterArchiveAvailable(id),
                party, characters, endgame, challenges, regionQuests, archive, shop);
    }

    public static void command(ServerPlayer player, String raw) {
        if (raw == null || raw.isBlank()) return;
        String[] parts = raw.split("\\|", -1);
        switch (parts[0]) {
            case "OPEN", "SYNC" -> MetaNetwork.sync(player);
            case "PARTY" -> {
                if (BattleSessionManager.exists(player)) return;
                List<String> party = parts.length < 2 || parts[1].isBlank() ? List.of()
                        : Arrays.stream(parts[1].split(",")).filter(value -> !value.isBlank()).toList();
                try {
                    CampaignProgressStore.setActiveParty(player.getUUID(), party);
                    CampaignPersistence.saveIfDirty(player);
                } catch (RuntimeException ex) {
                    error(player, "파티 변경 실패", ex);
                }
                MetaNetwork.sync(player);
            }
            case "SUMMON1" -> mutate(player, "1회 소환 실패", () -> CampaignProgressStore.summonStandard(player.getUUID(), 1));
            case "SUMMON10" -> mutate(player, "10회 소환 실패", () -> CampaignProgressStore.summonStandard(player.getUUID(), 10));
            case "STARTER" -> mutate(player, "Starter Archive 실패", () -> CampaignProgressStore.summonStarter(player.getUUID()));
            case "BUY" -> {
                if (parts.length < 2) return;
                String itemId = parts[1];
                mutate(player, "구매 실패", () -> CampaignProgressStore.buyEquipment(player.getUUID(), itemId));
            }
            case "START" -> {
                if (parts.length < 2 || BattleSessionManager.exists(player)) return;
                String encounterId = parts[1];
                if (!EndgameEncounterCatalog.contains(encounterId)) return;
                if (!inRadia(player)) {
                    player.sendSystemMessage(Component.literal("TURNBOUND · Hard/Rift 입장은 라디아 허브에서만 가능합니다."));
                    MetaNetwork.sync(player);
                    return;
                }
                if (!EndgameEncounterCatalog.unlocked(player.getUUID(), encounterId)) {
                    player.sendSystemMessage(Component.literal("TURNBOUND · 아직 잠긴 콘텐츠입니다."));
                    MetaNetwork.sync(player);
                    return;
                }
                BattleSessionManager.startEncounter(player, encounterId);
            }
            default -> { }
        }
    }

    private static void mutate(ServerPlayer player, String label, Runnable action) {
        if (BattleSessionManager.exists(player)) return;
        try {
            action.run();
            CampaignPersistence.saveIfDirty(player);
        } catch (RuntimeException ex) {
            error(player, label, ex);
        }
        MetaNetwork.sync(player);
    }

    private static int currentShopChapter(Set<String> completed) {
        int completedChapter = 0;
        for (int chapter = 1; chapter <= 5; chapter++) if (QuestCatalog.chapterComplete(chapter, completed)) completedChapter = chapter;
        return Math.max(1, completedChapter + 1);
    }

    private static void error(ServerPlayer player, String label, RuntimeException ex) {
        player.sendSystemMessage(Component.literal("TURNBOUND · " + label + ": " + ex.getMessage()));
    }

    private static boolean inRadia(ServerPlayer player) {
        return player.getX() >= -128 && player.getX() <= 128 && player.getZ() >= -112 && player.getZ() <= 128;
    }
}
