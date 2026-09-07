package kr.moonseungjun.turnboundre.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Strict tuning validation kept separate from combat definition validation. */
public final class ProgressionDefinitionValidator {
    private static final Set<String> ORIGIN_STARS = Set.of("1", "2", "3", "4", "5");
    private static final Set<String> ALL_STARS = Set.of("1", "2", "3", "4", "5", "6");
    private static final Set<String> ASCENSION_TARGETS = Set.of("2", "3", "4", "5", "6");

    private ProgressionDefinitionValidator() {}

    public static List<String> validate(List<ProgressionDefinition> values) {
        List<String> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (values == null) return List.of("progressions must not be null");
        for (ProgressionDefinition value : values) {
            if (value == null) { errors.add("progression must not be null"); continue; }
            String id = value.id();
            if (!validId(id)) errors.add("invalid progression id: " + id);
            else if (!seen.add(id)) errors.add("duplicate progression id: " + id);
            if (value.partyCapacity() < 1 || value.partyCapacity() > 100) errors.add(id + ": partyCapacity must be 1..100");
            if (!value.unlockShardCostByOriginStar().keySet().equals(ORIGIN_STARS)) {
                errors.add(id + ": unlockShardCostByOriginStar must define exactly " + ORIGIN_STARS);
            }
            for (Map.Entry<String, Integer> entry : value.unlockShardCostByOriginStar().entrySet()) {
                if (entry.getValue() == null || entry.getValue() < 0) errors.add(id + ": invalid unlock shard cost for star " + entry.getKey());
            }
            if (!value.levelCostsByStar().keySet().equals(ALL_STARS)) {
                errors.add(id + ": levelCostsByStar must define exactly " + ALL_STARS);
            }
            for (Map.Entry<String, ProgressionDefinition.LevelCost> entry : value.levelCostsByStar().entrySet()) {
                ProgressionDefinition.LevelCost row = entry.getValue();
                if (row == null || row.coinBase() < 0 || row.coinPerLevel() < 0 || row.essenceBase() < 0 || row.essencePerLevel() < 0) {
                    errors.add(id + ": invalid level cost for star " + entry.getKey());
                }
            }
            if (!value.ascensionCostsByTargetStar().keySet().equals(ASCENSION_TARGETS)) {
                errors.add(id + ": ascensionCostsByTargetStar must define exactly " + ASCENSION_TARGETS);
            }
            long previousCoin = -1;
            long previousEssence = -1;
            int previousShards = -1;
            for (int star = 2; star <= 6; star++) {
                ProgressionDefinition.AscensionCost row = value.ascensionCostsByTargetStar().get(Integer.toString(star));
                if (row == null) continue;
                if (row.coin() < 0 || row.essence() < 0 || row.shards() < 0) {
                    errors.add(id + ": invalid ascension cost for target star " + star);
                    continue;
                }
                if (row.coin() < previousCoin || row.essence() < previousEssence || row.shards() < previousShards) {
                    errors.add(id + ": ascension costs must be monotonic by target star");
                }
                previousCoin = row.coin();
                previousEssence = row.essence();
                previousShards = row.shards();
            }
        }
        return List.copyOf(errors);
    }

    private static boolean validId(String value) {
        if (value == null) return false;
        int colon = value.indexOf(':');
        if (colon <= 0 || colon == value.length() - 1 || colon != value.lastIndexOf(':')) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == ':') continue;
            boolean ok = c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_' || c == '-' || c == '.' || c == '/';
            if (!ok) return false;
        }
        return true;
    }
}
