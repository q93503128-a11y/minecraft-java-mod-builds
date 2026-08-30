package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.BattleStats;
import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import io.github.q93503128.turnbound.content.CanonicalData;
import io.github.q93503128.turnbound.content.V04Catalogs;
import io.github.q93503128.turnbound.progression.CharacterGrowthRules;
import io.github.q93503128.turnbound.progression.EquipmentInventory;
import io.github.q93503128.turnbound.progression.EquipmentRules;
import io.github.q93503128.turnbound.progression.GachaCatalog;
import io.github.q93503128.turnbound.progression.GachaService;
import io.github.q93503128.turnbound.progression.PlayerProfile;
import io.github.q93503128.turnbound.session.BattleResultSummary;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;

/** Server-side campaign progression authority shared by combat, growth, equipment, gacha and persistence. */
public final class CampaignProgressStore {
    public record Snapshot(
            PlayerProfile.Snapshot profile,
            Map<String, CharacterProgression.State> characters,
            Map<String, CharacterGrowthRules.State> growth,
            EquipmentInventory.Snapshot equipment,
            Set<String> clearedEncounters,
            Set<String> orphanedCharacterIds,
            Set<String> orphanedEquipmentIds) {
        public Snapshot {
            if (profile == null || characters == null || growth == null || equipment == null
                    || clearedEncounters == null || orphanedCharacterIds == null || orphanedEquipmentIds == null) {
                throw new IllegalArgumentException("Incomplete campaign snapshot");
            }
            characters = Map.copyOf(characters);
            growth = Map.copyOf(growth);
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
        if (playerId == null || encounterId == null || !SouthgateEncounterCatalog.contains(encounterId)) {
            return BattleResultSummary.none();
        }
        PlayerProgress progress = player(playerId);
        boolean firstClear = !progress.clearedEncounters.contains(encounterId);
        var encounter = SouthgateEncounterCatalog.spec(encounterId);
        int xp = firstClear ? encounter.rewardXp() : 0;
        int gold = firstClear ? encounter.rewardGold() : 0;
        List<BattleResultSummary.PartyXp> party = STORY_PARTY.stream().map(spec -> {
            CharacterProgression.State before = progress.characters.get(spec.id());
            int cap = CharacterGrowthRules.levelCap(progress.growth.get(spec.id()).currentStar());
            CharacterProgression.Gain gain = CharacterProgression.gain(before, xp, cap);
            return new BattleResultSummary.PartyXp(spec.id(), spec.name(),
                    gain.before().level(), gain.before().xp(), gain.after().level(), gain.after().xp(), gain.xpToNextAfter());
        }).toList();
        return new BattleResultSummary(xp, gold, firstClear, party);
    }

    public static BattleResultSummary commit(UUID playerId, String encounterId, BattleOutcome outcome) {
        if (outcome != BattleOutcome.ALLY_VICTORY) return BattleResultSummary.none();
        BattleResultSummary preview = previewVictory(playerId, encounterId);
        if (!preview.firstClear()) return preview;
        PlayerProgress progress = player(playerId);
        progress.clearedEncounters.add(encounterId);
        progress.profile.grant(PlayerProfile.Currency.GOLD, preview.gold());
        for (BattleResultSummary.PartyXp member : preview.party()) {
            progress.characters.put(member.characterId(), new CharacterProgression.State(member.levelAfter(), member.xpAfter()));
        }
        grantReserveXp(progress, preview.xp());
        if (SouthgateEncounterCatalog.B01_GRAUL.equals(encounterId)) applyB01FirstClear(progress);
        progress.dirty = true;
        return preview;
    }

    public static int gold(UUID playerId) { return Math.toIntExact(player(playerId).profile.currency(PlayerProfile.Currency.GOLD)); }
    public static long currency(UUID playerId, PlayerProfile.Currency currency) { return player(playerId).profile.currency(currency); }
    public static Set<String> ownedCharacters(UUID playerId) { return player(playerId).profile.ownedCharacters(); }
    public static boolean starterArchiveAvailable(UUID playerId) { return player(playerId).profile.starterArchiveAvailable(); }
    public static int fiveStarPity(UUID playerId) { return player(playerId).profile.fiveStarPity(); }
    public static CharacterProgression.State character(UUID playerId, String characterId) { return requireCharacter(player(playerId), characterId); }
    public static CharacterGrowthRules.State growth(UUID playerId, String characterId) { return requireGrowth(player(playerId), characterId); }
    public static EquipmentInventory.Snapshot equipment(UUID playerId) { return player(playerId).equipment.snapshot(); }

    public static Snapshot snapshot(UUID playerId) {
        PlayerProgress progress = player(playerId);
        Set<String> equipmentOrphans = new LinkedHashSet<>(progress.orphanedEquipmentIds);
        equipmentOrphans.addAll(progress.equipment.unknownItemIds());
        return new Snapshot(progress.profile.snapshot(), progress.characters, progress.growth, progress.equipment.snapshot(),
                progress.clearedEncounters, progress.orphanedCharacterIds, equipmentOrphans);
    }

    public static void restore(UUID playerId, Snapshot snapshot) {
        if (playerId == null || snapshot == null) throw new IllegalArgumentException("Missing campaign restore data");
        PlayerProgress restored = new PlayerProgress(false);
        restored.profile = PlayerProfile.restore(snapshot.profile());
        restored.characters.putAll(snapshot.characters());
        restored.growth.putAll(snapshot.growth());
        restored.equipment = EquipmentInventory.restore(snapshot.equipment());
        restored.clearedEncounters.addAll(snapshot.clearedEncounters());
        restored.orphanedCharacterIds.addAll(snapshot.orphanedCharacterIds());
        restored.orphanedEquipmentIds.addAll(snapshot.orphanedEquipmentIds());
        ensureStateForOwned(restored);
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
        PlayerProgress progress = player(playerId);
        CharacterGrowthRules.State state = requireGrowth(progress, characterId);
        progress.growth.put(characterId, state.withCharacterQuestComplete());
        progress.dirty = true;
    }

    /** Called only after the actual Signature Trial encounter is cleared. Rewards are first-clear only. */
    public static EquipmentInventory.Item completeSignatureTrial(UUID playerId, String characterId) {
        PlayerProgress progress = player(playerId);
        CharacterGrowthRules.State state = requireGrowth(progress, characterId);
        CharacterProgression.State level = requireCharacter(progress, characterId);
        if (!b05Cleared(progress)) throw new IllegalStateException("Signature Trial requires B05 clear");
        if (!state.characterQuestComplete()) throw new IllegalStateException("Character quest is not complete");
        if (state.currentStar() != 6 || level.level() != 60) throw new IllegalStateException("Signature Trial requires Lv60 / ★6");
        if (state.signatureTrialCleared()) throw new IllegalStateException("Signature Trial first-clear reward already claimed");
        String signatureId = V04Catalogs.signatureFor(characterId).id();
        EquipmentInventory.Item reward = progress.equipment.grant(signatureId);
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
        EquipmentInventory.Item item = progress.equipment.grant(itemId);
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
        CharacterGrowthRules.State growth = requireGrowth(progress, characterId);
        progress.equipment.equip(characterId, instanceId, growth.currentStar());
        progress.dirty = true;
    }

    public static EquipmentInventory.Item buyEquipment(UUID playerId, String itemId) {
        PlayerProgress progress = player(playerId);
        EquipmentInventory.Item item = EquipmentRules.buyNormal(progress.equipment, progress.profile, itemId, shopChapter(progress));
        progress.dirty = true;
        return item;
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

    private static void applyB01FirstClear(PlayerProgress progress) {
        progress.profile.grant(PlayerProfile.Currency.SUMMON_CRYSTAL, 3_000);
        progress.profile.grant(PlayerProfile.Currency.STAR_ESSENCE, 60);
        PlayerProfile.Acquisition p08 = progress.profile.acquireCharacter("P08");
        if (p08.newlyOwned()) initializeCharacter(progress, "P08");
        progress.equipment.grantChoiceToken("T2", 1);
        progress.profile.unlockStarterArchive();
    }

    private static void grantReserveXp(PlayerProgress progress, int fullXp) {
        int reserveXp = (int)Math.floor(fullXp * 0.20);
        if (reserveXp <= 0) return;
        Set<String> active = new LinkedHashSet<>();
        for (CharacterSpec spec : STORY_PARTY) active.add(spec.id());
        for (String characterId : progress.profile.ownedCharacters()) {
            if (active.contains(characterId)) continue;
            CharacterProgression.State before = requireCharacter(progress, characterId);
            int cap = CharacterGrowthRules.levelCap(requireGrowth(progress, characterId).currentStar());
            progress.characters.put(characterId, CharacterProgression.gain(before, reserveXp, cap).after());
        }
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
        return progress.clearedEncounters.contains("BATTLE_B05")
                || progress.clearedEncounters.contains("B05")
                || progress.clearedEncounters.contains("relay_b05_serak");
    }

    private static int shopChapter(PlayerProgress progress) {
        return progress.clearedEncounters.contains(SouthgateEncounterCatalog.B01_GRAUL) ? 2 : 1;
    }

    private static PlayerProgress player(UUID playerId) {
        if (playerId == null) throw new IllegalArgumentException("Missing player id");
        return PLAYERS.computeIfAbsent(playerId, ignored -> new PlayerProgress());
    }

    private record CharacterSpec(String id, String name) {}

    private static final class PlayerProgress {
        private final Map<String, CharacterProgression.State> characters = new LinkedHashMap<>();
        private final Map<String, CharacterGrowthRules.State> growth = new LinkedHashMap<>();
        private final Set<String> clearedEncounters = new LinkedHashSet<>();
        private final Set<String> orphanedCharacterIds = new LinkedHashSet<>();
        private final Set<String> orphanedEquipmentIds = new LinkedHashSet<>();
        private PlayerProfile profile = PlayerProfile.newGame();
        private EquipmentInventory equipment = EquipmentInventory.empty();
        private boolean dirty;

        private PlayerProgress() { this(true); }

        private PlayerProgress(boolean seedStoryParty) {
            if (seedStoryParty) {
                for (CharacterSpec spec : STORY_PARTY) {
                    profile.acquireCharacter(spec.id());
                    initializeCharacter(this, spec.id());
                }
            }
            dirty = seedStoryParty;
        }
    }
}
