package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.ChallengeCatalog;
import io.github.q93503128.turnbound.content.RegionQuestCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Commands and snapshots for party/endgame management. All mutations are validated server-side. */
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

        return new MetaUiSnapshot(
                campaign.profile().gold(), campaign.profile().summonCrystal(), campaign.profile().starEssence(),
                campaign.profile().awakeningCore(), partyCp, riftUnlocked, party, characters, endgame, challenges, regionQuests);
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
                    player.sendSystemMessage(Component.literal("TURNBOUND · 파티 변경 실패: " + ex.getMessage()));
                }
                MetaNetwork.sync(player);
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

    private static boolean inRadia(ServerPlayer player) {
        return player.getX() >= -128 && player.getX() <= 128 && player.getZ() >= -112 && player.getZ() <= 128;
    }
}
