package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
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

/** Server-side campaign progression authority shared by combat rewards, gacha and persistence. */
public final class CampaignProgressStore {
    public record Snapshot(
            PlayerProfile.Snapshot profile,
            Map<String, CharacterProgression.State> characters,
            Set<String> clearedEncounters) {
        public Snapshot {
            if (profile == null || characters == null || clearedEncounters == null) {
                throw new IllegalArgumentException("Incomplete campaign snapshot");
            }
            characters = Map.copyOf(characters);
            clearedEncounters = Set.copyOf(clearedEncounters);
        }
    }

    private static final Map<UUID, PlayerProgress> PLAYERS = new LinkedHashMap<>();
    private static final GachaService GACHA = new GachaService(RandomGenerator.getDefault());
    private static final List<CharacterSpec> STORY_PARTY = List.of(
            new CharacterSpec("P01", "카이렌", 40),
            new CharacterSpec("P03", "브람", 40),
            new CharacterSpec("P04", "엘리시아", 40),
            new CharacterSpec("F03", "변경 사냥꾼", 20));

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
            CharacterProgression.Gain gain = CharacterProgression.gain(before, xp, spec.levelCap());
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
        if (SouthgateEncounterCatalog.B01_GRAUL.equals(encounterId)) applyB01FirstClear(progress);
        progress.dirty = true;
        return preview;
    }

    public static int gold(UUID playerId) {
        return Math.toIntExact(player(playerId).profile.currency(PlayerProfile.Currency.GOLD));
    }

    public static long currency(UUID playerId, PlayerProfile.Currency currency) {
        return player(playerId).profile.currency(currency);
    }

    public static Set<String> ownedCharacters(UUID playerId) {
        return player(playerId).profile.ownedCharacters();
    }

    public static boolean starterArchiveAvailable(UUID playerId) {
        return player(playerId).profile.starterArchiveAvailable();
    }

    public static int fiveStarPity(UUID playerId) {
        return player(playerId).profile.fiveStarPity();
    }

    public static Snapshot snapshot(UUID playerId) {
        PlayerProgress progress = player(playerId);
        return new Snapshot(progress.profile.snapshot(), progress.characters, progress.clearedEncounters);
    }

    public static void restore(UUID playerId, Snapshot snapshot) {
        if (playerId == null || snapshot == null) throw new IllegalArgumentException("Missing campaign restore data");
        PlayerProgress restored = new PlayerProgress(false);
        restored.profile = PlayerProfile.restore(snapshot.profile());
        restored.characters.putAll(snapshot.characters());
        restored.clearedEncounters.addAll(snapshot.clearedEncounters());
        ensureProgressionForOwned(restored);
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

    public static CharacterProgression.State character(UUID playerId, String characterId) {
        CharacterProgression.State state = player(playerId).characters.get(characterId);
        if (state == null) throw new IllegalArgumentException("Unknown campaign character " + characterId);
        return state;
    }

    public static void ensureNewGame(UUID playerId) {
        player(playerId);
    }

    public static boolean hasRuntime(UUID playerId) {
        return playerId != null && PLAYERS.containsKey(playerId);
    }

    public static boolean isDirty(UUID playerId) {
        PlayerProgress progress = PLAYERS.get(playerId);
        return progress != null && progress.dirty;
    }

    public static void markClean(UUID playerId) {
        PlayerProgress progress = PLAYERS.get(playerId);
        if (progress != null) progress.dirty = false;
    }

    public static void markDirty(UUID playerId) {
        PlayerProgress progress = PLAYERS.get(playerId);
        if (progress != null) progress.dirty = true;
    }

    public static void removeRuntime(UUID playerId) {
        PLAYERS.remove(playerId);
    }

    static void resetForTests(UUID playerId) { PLAYERS.remove(playerId); }
    public static void clearRuntime() { PLAYERS.clear(); }

    private static void applyB01FirstClear(PlayerProgress progress) {
        // Canonical v0.4 unlock package: boss reward 1,200 + tutorial 1,800 crystals, 60 essence and P08.
        progress.profile.grant(PlayerProfile.Currency.SUMMON_CRYSTAL, 3_000);
        progress.profile.grant(PlayerProfile.Currency.STAR_ESSENCE, 60);
        PlayerProfile.Acquisition p08 = progress.profile.acquireCharacter("P08");
        if (p08.newlyOwned()) progress.characters.putIfAbsent("P08", new CharacterProgression.State(1, 0));
        progress.profile.unlockStarterArchive();
    }

    private static void registerNewCharacters(PlayerProgress progress, GachaService.BatchResult result) {
        for (GachaService.PullResult pull : result.pulls()) {
            if (pull.newlyOwned()) progress.characters.putIfAbsent(pull.characterId(), new CharacterProgression.State(1, 0));
        }
    }

    private static void ensureProgressionForOwned(PlayerProgress progress) {
        for (String characterId : progress.profile.ownedCharacters()) {
            progress.characters.putIfAbsent(characterId, new CharacterProgression.State(1, 0));
        }
        for (CharacterSpec spec : STORY_PARTY) {
            progress.characters.putIfAbsent(spec.id(), new CharacterProgression.State(1, 0));
        }
    }

    private static PlayerProgress player(UUID playerId) {
        if (playerId == null) throw new IllegalArgumentException("Missing player id");
        return PLAYERS.computeIfAbsent(playerId, ignored -> new PlayerProgress());
    }

    private record CharacterSpec(String id, String name, int levelCap) {}

    private static final class PlayerProgress {
        private final Map<String, CharacterProgression.State> characters = new LinkedHashMap<>();
        private final Set<String> clearedEncounters = new LinkedHashSet<>();
        private PlayerProfile profile = PlayerProfile.newGame();
        private boolean dirty;

        private PlayerProgress() {
            this(true);
        }

        private PlayerProgress(boolean seedStoryParty) {
            for (CharacterSpec spec : STORY_PARTY) {
                characters.put(spec.id(), new CharacterProgression.State(1, 0));
                if (seedStoryParty) profile.acquireCharacter(spec.id());
            }
            dirty = seedStoryParty;
        }
    }
}
