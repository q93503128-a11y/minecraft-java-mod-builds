package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleStats;
import io.github.q93503128.turnbound.combat.EndgameEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.ChallengeCatalog;
import io.github.q93503128.turnbound.content.CharacterMenuCatalog;
import io.github.q93503128.turnbound.content.QuestCatalog;
import io.github.q93503128.turnbound.content.RegionQuestCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.EquipmentRules;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Commands and snapshots for the v0.4 RPG management menu. All mutations are validated server-side. */
public final class MetaMenuService {
    private MetaMenuService() {}

    public static MetaUiSnapshot snapshot(ServerPlayer player) {
        var id = player.getUUID();
        var campaign = CampaignProgressStore.snapshot(id);
        List<String> party = CampaignProgressStore.activeParty(id);
        Set<String> clears = campaign.clearedEncounters();
        Set<String> completedQuests = campaign.quests().completed();
        Set<String> challengeClears = ChallengeService.completed(id);
        int partyCp = party.stream().mapToInt(characterId -> CampaignProgressStore.combatPower(id, characterId)).sum();

        List<MetaUiSnapshot.CharacterRow> characters = CharacterMenuCatalog.all().stream().map(menu -> {
            boolean owned = campaign.profile().ownedCharacters().contains(menu.id());
            var base = CanonicalData.definition(menu.id());
            int level = 0;
            int star = base.nativeStars();
            boolean awakened = false;
            boolean active = false;
            boolean profileUnlocked = !menu.profileQuest();
            int cp = 0;
            BattleStats stats = base.stats();
            if (owned) {
                var progress = CampaignProgressStore.character(id, menu.id());
                var growth = CampaignProgressStore.growth(id, menu.id());
                level = progress.level();
                star = growth.currentStar();
                awakened = growth.awakened();
                active = party.contains(menu.id());
                profileUnlocked = !menu.profileQuest() || growth.characterQuestComplete();
                cp = CampaignProgressStore.combatPower(id, menu.id());
                stats = CampaignProgressStore.finalStats(id, menu.id());
            }
            return new MetaUiSnapshot.CharacterRow(menu.id(), base.name(), owned, base.nativeStars(), level, star,
                    awakened, cp, active, menu.role(), menu.primaryRole(), menu.difficulty(), profileUnlocked,
                    stats.maxHp(), stats.attack(), stats.defense(), stats.speed());
        }).toList();

        List<MetaUiSnapshot.EquipmentRow> equipment = equipmentRows(campaign.equipment());

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
                        completedQuests.contains(q.id()), q.chestRule())).toList();

        List<MetaUiSnapshot.ArchiveRow> archive = new ArrayList<>();
        List<PlayerProfile.SummonHistory> source = campaign.profile().summonHistory();
        for (int i = source.size() - 1; i >= 0; i--) {
            var row = source.get(i);
            archive.add(new MetaUiSnapshot.ArchiveRow(row.characterId(), CanonicalData.definition(row.characterId()).name(),
                    row.nativeStars(), row.newlyOwned(), row.starEssenceGranted(), row.pityAfter()));
        }

        int shopChapter = currentShopChapter(completedQuests);
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

        List<MetaUiSnapshot.CodexRow> codex = codexRows(campaign, characters, completedQuests.contains("MQ_C03_03_oro7"));

        return new MetaUiSnapshot(
                campaign.profile().gold(), campaign.profile().summonCrystal(), campaign.profile().starEssence(),
                campaign.profile().awakeningCore(), partyCp, riftUnlocked,
                campaign.profile().fiveStarPity(), CampaignProgressStore.starterArchiveAvailable(id),
                party, campaign.profile().partyPresets(), characters, equipment, endgame, challenges, regionQuests, archive, shop, codex);
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
                } catch (RuntimeException ex) { error(player, "파티 변경 실패", ex); }
                MetaNetwork.sync(player);
            }
            case "PRESET_SAVE" -> {
                if (parts.length < 2 || BattleSessionManager.exists(player)) return;
                mutate(player, "프리셋 저장 실패", () -> savePreset(player, Integer.parseInt(parts[1])));
            }
            case "PRESET_LOAD" -> {
                if (parts.length < 2 || BattleSessionManager.exists(player)) return;
                mutate(player, "프리셋 불러오기 실패", () -> loadPreset(player, Integer.parseInt(parts[1])));
            }
            case "SUMMON1" -> mutate(player, "1회 소환 실패", () -> CampaignProgressStore.summonStandard(player.getUUID(), 1));
            case "SUMMON10" -> mutate(player, "10회 소환 실패", () -> CampaignProgressStore.summonStandard(player.getUUID(), 10));
            case "STARTER" -> mutate(player, "Starter Archive 실패", () -> CampaignProgressStore.summonStarter(player.getUUID()));
            case "BUY" -> {
                if (parts.length < 2) return;
                mutate(player, "구매 실패", () -> CampaignProgressStore.buyEquipment(player.getUUID(), parts[1]));
            }
            case "ENHANCE" -> {
                if (parts.length < 2) return;
                mutate(player, "강화 실패", () -> CampaignProgressStore.enhanceEquipment(player.getUUID(), parts[1]));
            }
            case "EQUIP" -> {
                if (parts.length < 3) return;
                mutate(player, "장착 실패", () -> equip(player, parts[1], parts[2]));
            }
            case "PROMOTE" -> {
                if (parts.length < 2) return;
                mutate(player, "승급 실패", () -> CampaignProgressStore.promote(player.getUUID(), parts[1]));
            }
            case "AWAKEN" -> {
                if (parts.length < 2) return;
                mutate(player, "각성 실패", () -> CampaignProgressStore.awaken(player.getUUID(), parts[1]));
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

    private static List<MetaUiSnapshot.EquipmentRow> equipmentRows(EquipmentInventory.Snapshot inventory) {
        Map<String, String> owners = new LinkedHashMap<>();
        for (var entry : inventory.loadouts().entrySet()) {
            for (EquipmentInventory.Slot slot : EquipmentInventory.Slot.values()) {
                String instance = entry.getValue().get(slot);
                if (!instance.isBlank()) owners.put(instance, entry.getKey());
            }
        }
        return inventory.items().values().stream().sorted(Comparator.comparing(EquipmentInventory.Item::instanceId)).map(item -> {
            try {
                var spec = V04Catalogs.equipment(item.itemId());
                return new MetaUiSnapshot.EquipmentRow(item.instanceId(), item.itemId(), spec.name(), spec.tier(), spec.slot(),
                        item.enhancementLevel(), owners.getOrDefault(item.instanceId(), ""), spec.main().type(),
                        EquipmentInventory.scaledMain(spec.main().value(), item.enhancementLevel()), spec.sub().type(),
                        EquipmentInventory.scaledSub(spec.sub().value(), item.enhancementLevel()),
                        EquipmentInventory.scaledMain(spec.main().value(), 20), EquipmentInventory.scaledSub(spec.sub().value(), 20));
            } catch (RuntimeException ignored) {
                var spec = V04Catalogs.signature(item.itemId());
                return new MetaUiSnapshot.EquipmentRow(item.instanceId(), item.itemId(), spec.name(), "SIGNATURE", "SIGNATURE",
                        item.enhancementLevel(), owners.getOrDefault(item.instanceId(), ""), spec.main().type(),
                        EquipmentInventory.scaledMain(spec.main().value(), item.enhancementLevel()), spec.sub().type(),
                        EquipmentInventory.scaledSub(spec.sub().value(), item.enhancementLevel()),
                        EquipmentInventory.scaledMain(spec.main().value(), 20), EquipmentInventory.scaledSub(spec.sub().value(), 20));
            }
        }).toList();
    }

    private static List<MetaUiSnapshot.CodexRow> codexRows(CampaignProgressStore.Snapshot campaign,
                                                            List<MetaUiSnapshot.CharacterRow> characters,
                                                            boolean detailUnlocked) {
        List<MetaUiSnapshot.CodexRow> out = new ArrayList<>();
        for (var row : characters) {
            out.add(new MetaUiSnapshot.CodexRow("CHARACTERS", row.id(), row.name(), row.owned(), row.profileUnlocked(), row.role()));
        }
        List<String> ids = CanonicalData.ids().stream().sorted().toList();
        for (String id : ids) {
            String rank = CanonicalData.rank(id);
            if ("PLAYABLE".equals(rank)) continue;
            boolean boss = CanonicalData.definition(id).boss();
            boolean discovered = discoveredCombatant(campaign.clearedEncounters(), id);
            String category = boss ? "BOSSES" : "ENEMIES";
            String summary = boss ? "Boss" : "ELITE".equals(rank) ? "Elite" : "Enemy";
            out.add(new MetaUiSnapshot.CodexRow(category, id, CanonicalData.definition(id).name(), discovered,
                    discovered && detailUnlocked, summary));
        }
        Set<String> ownedEquipment = campaign.equipment().items().values().stream().map(EquipmentInventory.Item::itemId)
                .collect(java.util.stream.Collectors.toSet());
        for (var spec : V04Catalogs.equipment()) {
            out.add(new MetaUiSnapshot.CodexRow("EQUIPMENT", spec.id(), spec.name(), ownedEquipment.contains(spec.id()), true,
                    spec.tier() + " / " + spec.slot()));
        }
        for (String characterId : List.of("P01","P02","P03","P04","P05","P06","P07","P08")) {
            var spec = V04Catalogs.signatureFor(characterId);
            out.add(new MetaUiSnapshot.CodexRow("EQUIPMENT", spec.id(), spec.name(), ownedEquipment.contains(spec.id()), true,
                    "SIGNATURE / " + characterId));
        }
        return List.copyOf(out);
    }

    private static boolean discoveredCombatant(Set<String> clears, String combatantId) {
        for (String clear : clears) {
            if (!V04Catalogs.hasEncounter(clear)) continue;
            if (V04Catalogs.encounter(clear).enemies().contains(combatantId)) return true;
        }
        return false;
    }

    private static void equip(ServerPlayer player, String characterId, String instanceId) {
        var snapshot = CampaignProgressStore.snapshot(player.getUUID());
        EquipmentInventory.Item item = snapshot.equipment().items().get(instanceId);
        if (item == null) throw new IllegalArgumentException("Unknown equipment instance " + instanceId);
        String slot;
        try { slot = V04Catalogs.equipment(item.itemId()).slot(); }
        catch (RuntimeException ignored) { slot = "SIGNATURE"; }
        if ("ACCESSORY".equals(slot) && !snapshot.clearedEncounters().contains("BATTLE_B02")) {
            throw new IllegalStateException("Accessory slot unlocks after B02");
        }
        if ("SIGNATURE".equals(slot) && !snapshot.clearedEncounters().contains("BATTLE_B05")) {
            throw new IllegalStateException("Signature slot unlocks after B05");
        }
        CampaignProgressStore.equip(player.getUUID(), characterId, instanceId);
    }

    private static void savePreset(ServerPlayer player, int slotOneBased) {
        int index = slotOneBased - 1;
        var snapshot = CampaignProgressStore.snapshot(player.getUUID());
        PlayerProfile profile = PlayerProfile.restore(snapshot.profile());
        profile.savePartyPreset(index, snapshot.activeParty());
        CampaignProgressStore.restore(player.getUUID(), new CampaignProgressStore.Snapshot(
                profile.snapshot(), snapshot.characters(), snapshot.growth(), snapshot.equipment(), snapshot.quests(),
                snapshot.activeParty(), snapshot.clearedEncounters(), snapshot.orphanedCharacterIds(), snapshot.orphanedEquipmentIds()));
        CampaignProgressStore.markDirty(player.getUUID());
    }

    private static void loadPreset(ServerPlayer player, int slotOneBased) {
        int index = slotOneBased - 1;
        List<String> preset = CampaignProgressStore.snapshot(player.getUUID()).profile().partyPresets().get(index);
        if (preset.isEmpty()) throw new IllegalStateException("Preset " + slotOneBased + " is empty");
        CampaignProgressStore.setActiveParty(player.getUUID(), preset);
    }

    private static void mutate(ServerPlayer player, String label, Runnable action) {
        if (BattleSessionManager.exists(player)) return;
        try {
            action.run();
            CampaignPersistence.saveIfDirty(player);
        } catch (RuntimeException ex) { error(player, label, ex); }
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
