package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleStats;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.QuestCatalog;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.EquipmentRules;
import io.github.q93503128.turnbound.progression.GachaService;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.progression.QuestProgress;
import io.github.q93503128.turnbound.session.BattleResultSummary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Server-side campaign progression authority shared by combat, growth, equipment, quests, gacha and persistence. */
public final class CampaignProgressStore {
    private static final List<String> DEFAULT_PARTY = List.of("P01", "P03", "P04", "F03");

    public record Snapshot(
            PlayerProfile.Snapshot profile,
            Map<String, CharacterProgression.State> characters,
            Map<String, CharacterGrowthRules.State> growth,
            EquipmentInventory.Snapshot equipment,
            QuestProgress.Snapshot quests,
            List<String> activeParty,
            Set<String> clearedEncounters,
            Set<String> orphanedCharacterIds,
            Set<String> orphanedEquipmentIds) {
        public Snapshot(
                PlayerProfile.Snapshot profile,
                Map<String, CharacterProgression.State> characters,
                Map<String, CharacterGrowthRules.State> growth,
                EquipmentInventory.Snapshot equipment,
                QuestProgress.Snapshot quests,
                Set<String> clearedEncounters,
                Set<String> orphanedCharacterIds,
                Set<String> orphanedEquipmentIds) {
            this(profile, characters, growth, equipment, quests, defaultPartyFor(profile), clearedEncounters,
                    orphanedCharacterIds, orphanedEquipmentIds);
        }

        public Snapshot {
            if (profile == null || characters == null || growth == null || equipment == null || quests == null
                    || activeParty == null || clearedEncounters == null || orphanedCharacterIds == null || orphanedEquipmentIds == null) {
                throw new IllegalArgumentException("Incomplete campaign snapshot");
            }
            characters = Map.copyOf(characters);
            growth = Map.copyOf(growth);
            activeParty = validateParty(profile, activeParty);
            clearedEncounters = Set.copyOf(clearedEncounters);
            orphanedCharacterIds = Set.copyOf(orphanedCharacterIds);
            orphanedEquipmentIds = Set.copyOf(orphanedEquipmentIds);
        }
    }

    private static final Map<UUID, PlayerProgress> PLAYERS = new LinkedHashMap<>();
    private static final GachaService GACHA = new GachaService(RandomGenerator.getDefault());
    private static final List<CharacterSpec> STORY_PARTY = List.of(
            new CharacterSpec("P01", "카이렌"),
            new CharacterSpec("P03", "브람"),
            new CharacterSpec("P04", "엘리시아"),
            new CharacterSpec("F03", "변경 사냥꾼"));

    private CampaignProgressStore() {}

    public static BattleResultSummary previewVictory(UUID playerId, String encounterId) {
        if (playerId == null || encounterId == null) return BattleResultSummary.none();
        String canonicalId = canonicalEncounterId(encounterId);
        if (!V04Catalogs.hasEncounter(canonicalId)) return BattleResultSummary.none();
        PlayerProgress progress = player(playerId);
        V04Catalogs.Encounter encounter = V04Catalogs.encounter(canonicalId);
        boolean firstClear = !progress.clearedEncounters.contains(canonicalId);
        int xp = V04Catalogs.battleXp(encounter);
        int gold = V04Catalogs.battleGold(encounter);
        List<BattleResultSummary.PartyXp> party = progress.activeParty.stream().map(characterId -> {
            CharacterProgression.Gain gain = gain(progress, characterId, xp);
            return new BattleResultSummary.PartyXp(characterId, CanonicalData.definition(characterId).name(),
                    gain.before().level(), gain.before().xp(), gain.after().level(), gain.after().xp(), gain.xpToNextAfter());
        }).toList();
        return new BattleResultSummary(xp, gold, firstClear, party);
    }

    public static BattleResultSummary commit(UUID playerId, String encounterId, BattleOutcome outcome) {
        if (outcome != BattleOutcome.ALLY_VICTORY) return BattleResultSummary.none();
        String canonicalId = canonicalEncounterId(encounterId);
        if (!V04Catalogs.hasEncounter(canonicalId)) return BattleResultSummary.none();
        PlayerProgress progress = player(playerId);
        V04Catalogs.Encounter encounter = V04Catalogs.encounter(canonicalId);
        BattleResultSummary preview = previewVictory(playerId, canonicalId);
        boolean firstClear = progress.clearedEncounters.add(canonicalId);

        progress.profile.grant(PlayerProfile.Currency.GOLD, preview.gold());
        for (BattleResultSummary.PartyXp member : preview.party()) {
            progress.characters.put(member.characterId(), new CharacterProgression.State(member.levelAfter(), member.xpAfter()));
        }
        grantReserveXp(progress, preview.xp());

        if (firstClear && encounter.boss()) {
            String bossId = encounter.enemies().getFirst();
            progress.profile.grant(PlayerProfile.Currency.STAR_ESSENCE, V04Catalogs.bossFirstClearEssence(bossId));
            if ("B01".equals(bossId)) applyB01FirstClear(progress);
        }

        recordQuestEvent(progress, QuestProgress.Event.battleWin(canonicalId, Set.copyOf(encounter.enemies())));
        if (encounter.boss()) recordQuestEvent(progress, QuestProgress.Event.bossWin(encounter.enemies().getFirst()));
        progress.dirty = true;
        return new BattleResultSummary(preview.xp(), preview.gold(), firstClear, preview.party());
    }

    public static int gold(UUID playerId) { return Math.toIntExact(player(playerId).profile.currency(PlayerProfile.Currency.GOLD)); }
    public static long currency(UUID playerId, PlayerProfile.Currency currency) { return player(playerId).profile.currency(currency); }
    public static Set<String> ownedCharacters(UUID playerId) { return player(playerId).profile.ownedCharacters(); }
    public static boolean starterArchiveAvailable(UUID playerId) { return player(playerId).profile.starterArchiveAvailable(); }
    public static int fiveStarPity(UUID playerId) { return player(playerId).profile.fiveStarPity(); }
    public static CharacterProgression.State character(UUID playerId, String characterId) { return requireCharacter(player(playerId), characterId); }
    public static CharacterGrowthRules.State growth(UUID playerId, String characterId) { return requireGrowth(player(playerId), characterId); }
    public static EquipmentInventory.Snapshot equipment(UUID playerId) { return player(playerId).equipment.snapshot(); }
    public static QuestProgress.Snapshot quests(UUID playerId) { return player(playerId).quests.snapshot(); }
    public static List<String> activeParty(UUID playerId) { return List.copyOf(player(playerId).activeParty); }

    public static void setActiveParty(UUID playerId, List<String> characterIds) {
        PlayerProgress progress = player(playerId);
        List<String> validated = validateParty(progress.profile.snapshot(), characterIds);
        progress.activeParty.clear();
        progress.activeParty.addAll(validated);
        recordQuestEvent(progress, QuestProgress.Event.partyConfirm(Set.copyOf(validated)));
        progress.dirty = true;
    }

    public static Snapshot snapshot(UUID playerId) {
        PlayerProgress progress = player(playerId);
        Set<String> equipmentOrphans = new LinkedHashSet<>(progress.orphanedEquipmentIds);
        equipmentOrphans.addAll(progress.equipment.unknownItemIds());
        return new Snapshot(progress.profile.snapshot(), progress.characters, progress.growth, progress.equipment.snapshot(),
                progress.quests.snapshot(), progress.activeParty, progress.clearedEncounters,
                progress.orphanedCharacterIds, equipmentOrphans);
    }

    public static void restore(UUID playerId, Snapshot snapshot) {
        if (playerId == null || snapshot == null) throw new IllegalArgumentException("Missing campaign restore data");
        PlayerProgress restored = new PlayerProgress(false);
        restored.profile = PlayerProfile.restore(snapshot.profile());
        restored.characters.putAll(snapshot.characters());
        restored.growth.putAll(snapshot.growth());
        restored.equipment = EquipmentInventory.restore(snapshot.equipment());
        restored.quests = QuestProgress.restore(snapshot.quests());
        restored.activeParty.addAll(snapshot.activeParty());
        for (String id : snapshot.clearedEncounters()) restored.clearedEncounters.add(canonicalEncounterId(id));
        restored.orphanedCharacterIds.addAll(snapshot.orphanedCharacterIds());
        restored.orphanedEquipmentIds.addAll(snapshot.orphanedEquipmentIds());
        ensureStateForOwned(restored);
        if (restored.activeParty.isEmpty()) restored.activeParty.addAll(defaultPartyFor(restored.profile.snapshot()));
        restored.dirty = false;
        PLAYERS.put(playerId, restored);
    }

    public static GachaService.BatchResult summonStandard(UUID playerId, int count) {
        PlayerProgress progress = player(playerId);
        GachaService.BatchResult result = switch (count) {
            case 1 -> GACHA.summonStandardSingle(progress.profile);
            case 10 -> GACHA.summonStandardTen(progress.profile);
            default -> throw new IllegalArgumentException("Standard Archive supports only 1 or 10 pulls");
        };
        registerNewCharacters(progress, result);
        progress.dirty = true;
        return result;
    }

    public static GachaService.BatchResult summonStarter(UUID playerId) {
        PlayerProgress progress = player(playerId);
        GachaService.BatchResult result = GACHA.summonStarterTen(progress.profile);
        registerNewCharacters(progress, result);
        progress.dirty = true;
        return result;
    }

    public static void trackQuest(UUID playerId, String questId) {
        PlayerProgress progress = player(playerId);
        progress.quests.track(questId);
        progress.dirty = true;
    }

    public static QuestCatalog.Quest completeQuest(UUID playerId, String questId) {
        PlayerProgress progress = player(playerId);
        QuestCatalog.Quest quest = QuestCatalog.quest(questId);
        validateQuestPrerequisites(progress, quest);
        if (quest.kind() == QuestCatalog.Kind.MAIN && !progress.quests.satisfied(quest)) {
            throw new IllegalStateException("Main quest objective is not complete: " + questId);
        }
        completeQuestInternal(progress, quest);
        return quest;
    }

    public static void questInteract(UUID playerId, String targetId) {
        recordQuestEvent(player(playerId), QuestProgress.Event.interact(targetId));
    }

    public static void confirmParty(UUID playerId, Set<String> characterIds) {
        recordQuestEvent(player(playerId), QuestProgress.Event.partyConfirm(characterIds));
    }

    public static void inventoryFlag(UUID playerId, String flagId) {
        recordQuestEvent(player(playerId), QuestProgress.Event.inventoryFlag(flagId));
    }

    public static void recordKill(UUID playerId, String enemyId, int amount) {
        recordQuestEvent(player(playerId), QuestProgress.Event.kill(enemyId, amount));
    }

    public static void recordLoot(UUID playerId, String lootId, int amount) {
        recordQuestEvent(player(playerId), QuestProgress.Event.loot(lootId, amount));
    }

    public static CharacterGrowthRules.State promote(UUID playerId, String characterId) {
        PlayerProgress progress = player(playerId);
        CharacterGrowthRules.State state = requireGrowth(progress, characterId);
        if (state.currentStar() >= 6) throw new IllegalStateException("Character is already ★6");
        int targetStar = state.currentStar() + 1;
        if (targetStar == 6 && !b05Cleared(progress)) throw new IllegalStateException("★6 promotion unlocks after B05");
        int cost = CharacterGrowthRules.promotionCost(state.currentStar());
        if (!progress.profile.spend(PlayerProfile.Currency.STAR_ESSENCE, cost)) throw new IllegalStateException("Not enough Star Essence");
        CharacterGrowthRules.State promoted = state.withStar(targetStar);
        progress.growth.put(characterId, promoted);
        progress.dirty = true;
        return promoted;
    }

    public static void completeCharacterQuest(UUID playerId, String characterId) {
        String questId = "CQ_" + characterId;
        if (QuestCatalog.contains(questId)) completeQuest(playerId, questId);
        else throw new IllegalArgumentException("No character quest for " + characterId);
    }

    public static EquipmentInventory.Item completeSignatureTrial(UUID playerId, String characterId) {
        PlayerProgress progress = player(playerId);
        CharacterGrowthRules.State state = requireGrowth(progress, characterId);
        CharacterProgression.State level = requireCharacter(progress, characterId);
        if (!b05Cleared(progress)) throw new IllegalStateException("Signature Trial requires B05 clear");
        if (!state.characterQuestComplete()) throw new IllegalStateException("Character quest is not complete");
        if (state.currentStar() != 6 || level.level() != 60) throw new IllegalStateException("Signature Trial requires Lv60 / ★6");
        if (state.signatureTrialCleared()) throw new IllegalStateException("Signature Trial first-clear reward already claimed");
        String signatureId = V04Catalogs.signatureFor(characterId).id();
        EquipmentInventory.Item reward = progress.equipment.grantReward(signatureId);
        progress.profile.grant(PlayerProfile.Currency.AWAKENING_CORE, 1);
        progress.growth.put(characterId, state.withSignatureTrialCleared());
        progress.dirty = true;
        return reward;
    }

    public static CharacterGrowthRules.State awaken(UUID playerId, String characterId) {
        PlayerProgress progress = player(playerId);
        CharacterGrowthRules.State state = requireGrowth(progress, characterId);
        CharacterProgression.State level = requireCharacter(progress, characterId);
        if (state.awakened()) return state;
        if (state.currentStar() != 6 || level.level() != 60 || !state.signatureTrialCleared()) {
            throw new IllegalStateException("Awakening requires Lv60 / ★6 / Signature Trial clear");
        }
        if (!progress.profile.spend(PlayerProfile.Currency.AWAKENING_CORE, 1)) throw new IllegalStateException("No Awakening Core");
        CharacterGrowthRules.State awakened = state.withAwakened();
        progress.growth.put(characterId, awakened);
        progress.dirty = true;
        return awakened;
    }

    public static EquipmentInventory.Item grantEquipment(UUID playerId, String itemId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = progress.equipment.grantReward(itemId);
        progress.dirty = true;
        return item;
    }

    public static EquipmentInventory.Item claimEquipmentChoice(UUID playerId, String tier, String itemId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = progress.equipment.claimChoice(tier, itemId);
        progress.dirty = true;
        return item;
    }

    public static EquipmentInventory.Item enhanceEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = progress.equipment.enhance(instanceId, progress.profile);
        progress.dirty = true;
        return item;
    }

    public static void equip(UUID playerId, String characterId, String instanceId) {
        PlayerProgress progress = player(playerId);
        progress.equipment.equip(characterId, instanceId, requireGrowth(progress, characterId).currentStar());
        progress.dirty = true;
    }

    public static EquipmentInventory.Item buyEquipment(UUID playerId, String itemId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = EquipmentRules.buyNormal(progress.equipment, progress.profile, itemId, shopChapter(progress));
        progress.dirty = true;
        return item;
    }

    public static int sellEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        int gold = progress.equipment.sell(instanceId, progress.profile);
        progress.dirty = true;
        return gold;
    }

    public static EquipmentInventory.Item claimPendingEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = progress.equipment.claimPending(instanceId);
        progress.dirty = true;
        return item;
    }

    public static int sellPendingEquipment(UUID playerId, String instanceId) {
        PlayerProgress progress = player(playerId);
        int gold = progress.equipment.sellPending(instanceId, progress.profile);
        progress.dirty = true;
        return gold;
    }

    public static BattleStats finalStats(UUID playerId, String characterId) {
        PlayerProgress progress = player(playerId);
        CharacterProgression.State level = requireCharacter(progress, characterId);
        CharacterGrowthRules.State growth = requireGrowth(progress, characterId);
        BattleStats base = CanonicalData.definition(characterId, level.level(), growth.currentStar(), growth.awakened()).stats();
        EquipmentInventory.StatTotals gear = progress.equipment.statTotals(characterId);
        int hp = Math.max(1, (int)Math.floor(base.maxHp() * (1.0 + gear.value("HP_PCT"))));
        int atk = Math.max(0, (int)Math.floor(base.attack() * (1.0 + gear.value("ATK_PCT"))));
        int def = Math.max(0, (int)Math.floor(base.defense() * (1.0 + gear.value("DEF_PCT"))));
        int spd = Math.max(1, (int)Math.floor(base.speed() + gear.value("SPD_FLAT")));
        return new BattleStats(hp, atk, def, spd);
    }

    public static int combatPower(UUID playerId, String characterId) { return EquipmentRules.combatPower(finalStats(playerId, characterId)); }
    public static List<String> equipmentRules(UUID playerId, String characterId) { return player(playerId).equipment.fixedRules(characterId); }

    public static void ensureNewGame(UUID playerId) { player(playerId); }
    public static boolean hasRuntime(UUID playerId) { return playerId != null && PLAYERS.containsKey(playerId); }
    public static boolean isDirty(UUID playerId) { PlayerProgress progress = PLAYERS.get(playerId); return progress != null && progress.dirty; }
    public static void markClean(UUID playerId) { PlayerProgress progress = PLAYERS.get(playerId); if (progress != null) progress.dirty = false; }
    public static void markDirty(UUID playerId) { PlayerProgress progress = PLAYERS.get(playerId); if (progress != null) progress.dirty = true; }
    public static void removeRuntime(UUID playerId) { PLAYERS.remove(playerId); }
    static void resetForTests(UUID playerId) { PLAYERS.remove(playerId); }
    public static void clearRuntime() { PLAYERS.clear(); }

    public static String canonicalEncounterId(String encounterId) {
        return switch (encounterId) {
            case "southgate_enc_m01" -> "ENC_M01";
            case "southgate_enc_m02" -> "ENC_M02";
            case "southgate_enc_m03" -> "ENC_M03";
            case "southgate_enc_m04" -> "ENC_M04";
            case "southgate_enc_m05" -> "ENC_M05";
            case "southgate_b01_graul" -> "BATTLE_B01";
            default -> encounterId;
        };
    }

    private static void applyB01FirstClear(PlayerProgress progress) {
        progress.profile.grant(PlayerProfile.Currency.SUMMON_CRYSTAL, 3_000);
        PlayerProfile.Acquisition p08 = progress.profile.acquireCharacter("P08");
        if (p08.newlyOwned()) initializeCharacter(progress, "P08");
        progress.equipment.grantChoiceToken("T2", 1);
        progress.profile.unlockStarterArchive();
    }

    private static void recordQuestEvent(PlayerProgress progress, QuestProgress.Event event) {
        boolean changed = false;
        for (QuestCatalog.Quest quest : QuestCatalog.kind(QuestCatalog.Kind.MAIN)) {
            if (progress.quests.completed(quest.id()) || !prerequisitesMet(progress, quest)) continue;
            changed |= progress.quests.apply(quest, event);
            if (progress.quests.satisfied(quest)) {
                completeQuestInternal(progress, quest);
                changed = true;
            }
        }
        if (changed) progress.dirty = true;
    }

    private static void completeQuestInternal(PlayerProgress progress, QuestCatalog.Quest quest) {
        validateQuestPrerequisites(progress, quest);
        QuestCatalog.Reward reward = QuestCatalog.reward(quest);
        progress.profile.grant(PlayerProfile.Currency.SUMMON_CRYSTAL, reward.crystal());
        progress.profile.grant(PlayerProfile.Currency.GOLD, reward.gold());
        if (reward.xp() > 0) grantPartyAndReserveXp(progress, reward.xp());
        progress.quests.grantRewardToken(reward.rewardToken(), 1);
        progress.quests.complete(quest);
        if (quest.kind() == QuestCatalog.Kind.CHARACTER) {
            CharacterGrowthRules.State state = requireGrowth(progress, quest.owner());
            progress.growth.put(quest.owner(), state.withCharacterQuestComplete());
        }
        progress.dirty = true;
    }

    private static void validateQuestPrerequisites(PlayerProgress progress, QuestCatalog.Quest quest) {
        if (progress.quests.completed(quest.id())) throw new IllegalStateException("Quest already completed " + quest.id());
        for (String prerequisite : quest.prerequisites()) {
            if (!prerequisiteMet(progress, prerequisite)) throw new IllegalStateException("Quest prerequisite not met: " + prerequisite);
        }
        if (quest.kind() == QuestCatalog.Kind.CHARACTER) requireCharacter(progress, quest.owner());
    }

    private static boolean prerequisitesMet(PlayerProgress progress, QuestCatalog.Quest quest) {
        for (String prerequisite : quest.prerequisites()) if (!prerequisiteMet(progress, prerequisite)) return false;
        return quest.kind() != QuestCatalog.Kind.CHARACTER || progress.profile.owns(quest.owner());
    }

    private static boolean prerequisiteMet(PlayerProgress progress, String prerequisite) {
        if (prerequisite.startsWith("MQ_")) return progress.quests.completed(prerequisite);
        if (prerequisite.startsWith("CHAPTER_") && prerequisite.endsWith("_COMPLETE")) {
            String number = prerequisite.substring("CHAPTER_".length(), prerequisite.length() - "_COMPLETE".length());
            try { return QuestCatalog.chapterComplete(Integer.parseInt(number), progress.quests.completed()); }
            catch (NumberFormatException ignored) { return false; }
        }
        int levelMarker = prerequisite.indexOf("_LEVEL_");
        if (levelMarker > 0) {
            String characterId = prerequisite.substring(0, levelMarker);
            try {
                int level = Integer.parseInt(prerequisite.substring(levelMarker + "_LEVEL_".length()));
                return progress.profile.owns(characterId) && requireCharacter(progress, characterId).level() >= level;
            } catch (NumberFormatException ignored) { return false; }
        }
        return false;
    }

    private static void grantPartyAndReserveXp(PlayerProgress progress, int fullXp) {
        for (String characterId : progress.activeParty) {
            progress.characters.put(characterId, gain(progress, characterId, fullXp).after());
        }
        grantReserveXp(progress, fullXp);
    }

    private static void grantReserveXp(PlayerProgress progress, int fullXp) {
        int reserveXp = (int)Math.floor(fullXp * 0.20);
        if (reserveXp <= 0) return;
        Set<String> active = Set.copyOf(progress.activeParty);
        for (String characterId : progress.profile.ownedCharacters()) {
            if (!active.contains(characterId)) progress.characters.put(characterId, gain(progress, characterId, reserveXp).after());
        }
    }

    private static CharacterProgression.Gain gain(PlayerProgress progress, String characterId, int xp) {
        CharacterProgression.State before = requireCharacter(progress, characterId);
        int cap = CharacterGrowthRules.levelCap(requireGrowth(progress, characterId).currentStar());
        return CharacterProgression.gain(before, xp, cap);
    }

    private static void registerNewCharacters(PlayerProgress progress, GachaService.BatchResult result) {
        for (GachaService.PullResult pull : result.pulls()) if (pull.newlyOwned()) initializeCharacter(progress, pull.characterId());
    }

    private static void initializeCharacter(PlayerProgress progress, String characterId) {
        progress.characters.putIfAbsent(characterId, new CharacterProgression.State(1, 0));
        progress.growth.putIfAbsent(characterId, CharacterGrowthRules.initial(characterId));
    }

    private static void ensureStateForOwned(PlayerProgress progress) {
        for (String characterId : progress.profile.ownedCharacters()) initializeCharacter(progress, characterId);
        for (CharacterSpec spec : STORY_PARTY) initializeCharacter(progress, spec.id());
    }

    private static CharacterProgression.State requireCharacter(PlayerProgress progress, String characterId) {
        if (!progress.profile.owns(characterId)) throw new IllegalArgumentException("Character is not owned: " + characterId);
        CharacterProgression.State state = progress.characters.get(characterId);
        if (state == null) throw new IllegalStateException("Missing character progression " + characterId);
        return state;
    }

    private static CharacterGrowthRules.State requireGrowth(PlayerProgress progress, String characterId) {
        requireCharacter(progress, characterId);
        CharacterGrowthRules.State state = progress.growth.get(characterId);
        if (state == null) throw new IllegalStateException("Missing character growth " + characterId);
        return state;
    }

    private static boolean b05Cleared(PlayerProgress progress) {
        return progress.quests.completed("MQ_C05_03_reconnect") || progress.clearedEncounters.contains("BATTLE_B05");
    }

    private static int shopChapter(PlayerProgress progress) {
        int completedChapter = 0;
        for (int chapter = 1; chapter <= 5; chapter++) {
            if (QuestCatalog.chapterComplete(chapter, progress.quests.completed())) completedChapter = chapter;
        }
        return Math.max(1, completedChapter + 1);
    }

    private static PlayerProgress player(UUID playerId) {
        if (playerId == null) throw new IllegalArgumentException("Missing player id");
        return PLAYERS.computeIfAbsent(playerId, ignored -> new PlayerProgress());
    }

    private static List<String> defaultPartyFor(PlayerProfile.Snapshot profile) {
        ArrayList<String> out = new ArrayList<>();
        for (String id : DEFAULT_PARTY) if (profile.ownedCharacters().contains(id)) out.add(id);
        if (out.isEmpty()) {
            for (String id : profile.ownedCharacters()) { out.add(id); if (out.size() == 4) break; }
        }
        return List.copyOf(out);
    }

    private static List<String> validateParty(PlayerProfile.Snapshot profile, List<String> requested) {
        if (requested == null || requested.isEmpty() || requested.size() > 4) throw new IllegalArgumentException("Party requires 1-4 characters");
        LinkedHashSet<String> unique = new LinkedHashSet<>(requested);
        if (unique.size() != requested.size()) throw new IllegalArgumentException("Party cannot contain duplicate characters");
        for (String id : unique) if (!profile.ownedCharacters().contains(id)) throw new IllegalArgumentException("Party character is not owned: " + id);
        return List.copyOf(unique);
    }

    private record CharacterSpec(String id, String name) {}

    private static final class PlayerProgress {
        private final Map<String, CharacterProgression.State> characters = new LinkedHashMap<>();
        private final Map<String, CharacterGrowthRules.State> growth = new LinkedHashMap<>();
        private final List<String> activeParty = new ArrayList<>();
        private final Set<String> clearedEncounters = new LinkedHashSet<>();
        private final Set<String> orphanedCharacterIds = new LinkedHashSet<>();
        private final Set<String> orphanedEquipmentIds = new LinkedHashSet<>();
        private PlayerProfile profile = PlayerProfile.newGame();
        private EquipmentInventory equipment = EquipmentInventory.empty();
        private QuestProgress quests = QuestProgress.empty();
        private boolean dirty;

        private PlayerProgress() { this(true); }

        private PlayerProgress(boolean seedStoryParty) {
            if (seedStoryParty) {
                for (CharacterSpec spec : STORY_PARTY) {
                    profile.acquireCharacter(spec.id());
                    initializeCharacter(this, spec.id());
                    activeParty.add(spec.id());
                }
            }
            dirty = seedStoryParty;
        }
    }
}
