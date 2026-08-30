package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.combat.BattleOutcome;
import io.github.q93503128.turnbound.combat.SouthgateEncounterCatalog;
import io.github.q93503128.turnbound.session.BattleResultSummary;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side alpha.16 campaign progression authority for the current vertical slice.
 * The shape matches the future player Data Attachment payload so this runtime store can be replaced without
 * changing combat/result code when persistent progression lands.
 */
public final class CampaignProgressStore {
    private static final Map<UUID, PlayerProgress> PLAYERS = new LinkedHashMap<>();
    private static final List<CharacterSpec> PARTY = List.of(
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
        List<BattleResultSummary.PartyXp> party = PARTY.stream().map(spec -> {
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
        progress.gold += preview.gold();
        for (BattleResultSummary.PartyXp member : preview.party()) {
            progress.characters.put(member.characterId(), new CharacterProgression.State(member.levelAfter(), member.xpAfter()));
        }
        return preview;
    }

    public static int gold(UUID playerId) { return player(playerId).gold; }
    public static CharacterProgression.State character(UUID playerId, String characterId) {
        CharacterProgression.State state = player(playerId).characters.get(characterId);
        if (state == null) throw new IllegalArgumentException("Unknown campaign character " + characterId);
        return state;
    }

    static void resetForTests(UUID playerId) { PLAYERS.remove(playerId); }
    public static void clearRuntime() { PLAYERS.clear(); }

    private static PlayerProgress player(UUID playerId) {
        return PLAYERS.computeIfAbsent(playerId, ignored -> new PlayerProgress());
    }

    private record CharacterSpec(String id, String name, int levelCap) {}

    private static final class PlayerProgress {
        private final Map<String, CharacterProgression.State> characters = new LinkedHashMap<>();
        private final Set<String> clearedEncounters = new LinkedHashSet<>();
        private int gold;

        private PlayerProgress() {
            for (CharacterSpec spec : PARTY) characters.put(spec.id(), new CharacterProgression.State(1, 0));
        }
    }
}
