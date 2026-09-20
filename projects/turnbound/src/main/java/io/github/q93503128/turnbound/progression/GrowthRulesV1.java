package io.github.q93503128.turnbound.progression;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tunable TURNBOUND v1 growth targets shared by level, equipment and Awakening runtime.
 * Values here are initial production targets from the v1 balance document and stay data-driven for later simulation.
 */
public final class GrowthRulesV1 {
    private static final JsonObject RAW = load();
    private static final int MAX_LEVEL = RAW.get("maxLevel").getAsInt();
    private static final double LEVEL_60_MULTIPLIER = RAW.get("level60Multiplier").getAsDouble();
    private static final double LEVEL_CURVE_EXPONENT = RAW.get("levelCurveExponent").getAsDouble();
    private static final int AWAKENING_GOLD_COST = RAW.get("awakeningGoldCost").getAsInt();
    private static final int MAX_ENHANCEMENT = RAW.get("maxEnhancement").getAsInt();
    private static final int[] ENHANCEMENT_WEIGHTS = weights();
    private static final Map<String, Integer> ENHANCEMENT_TOTALS = totals();

    private GrowthRulesV1() {}

    public static int maxLevel() { return MAX_LEVEL; }
    public static int maxEnhancement() { return MAX_ENHANCEMENT; }
    public static int awakeningGoldCost() { return AWAKENING_GOLD_COST; }

    public static double characterLevelMultiplier(int level) {
        int safe = Math.max(1, Math.min(MAX_LEVEL, level));
        if (safe == 1) return 1.0;
        double t = (safe - 1.0) / (MAX_LEVEL - 1.0);
        return 1.0 + (LEVEL_60_MULTIPLIER - 1.0) * Math.pow(t, LEVEL_CURVE_EXPONENT);
    }

    public static int enhancementCost(String tier, int fromLevel) {
        if (fromLevel < 0 || fromLevel >= MAX_ENHANCEMENT) {
            throw new IllegalArgumentException("Enhancement source level must be 0.." + (MAX_ENHANCEMENT - 1));
        }
        Integer total = ENHANCEMENT_TOTALS.get(tier);
        if (total == null) throw new IllegalArgumentException("Unknown equipment tier " + tier);
        return total * ENHANCEMENT_WEIGHTS[fromLevel] / 100;
    }

    /** Refunds only retired +11..+20 spend using the exact former v0.4 cost curve. */
    public static int legacyOverflowRefund(String tier, int legacyEnhancementLevel) {
        if (legacyEnhancementLevel <= MAX_ENHANCEMENT) return 0;
        int cappedLegacy = Math.min(20, legacyEnhancementLevel);
        int refund = 0;
        for (int fromLevel = MAX_ENHANCEMENT; fromLevel < cappedLegacy; fromLevel++) {
            refund = Math.addExact(refund, legacyEnhancementCost(tier, fromLevel));
        }
        return refund;
    }

    private static int legacyEnhancementCost(String tier, int fromLevel) {
        double factor = switch (tier) {
            case "T1" -> 1.0;
            case "T2" -> 1.5;
            case "T3" -> 2.2;
            case "T4" -> 3.2;
            case "SIGNATURE" -> 4.0;
            default -> throw new IllegalArgumentException("Unknown equipment tier " + tier);
        };
        return (int)Math.round(50.0 * factor * Math.pow(fromLevel + 1, 1.55));
    }

    private static int[] weights() {
        JsonArray raw = RAW.getAsJsonArray("enhancementWeights");
        if (raw.size() != MAX_ENHANCEMENT) throw new IllegalStateException("Enhancement weight count must match maxEnhancement");
        int[] values = new int[raw.size()];
        int total = 0;
        for (int i = 0; i < raw.size(); i++) {
            values[i] = raw.get(i).getAsInt();
            total += values[i];
        }
        if (total != 100) throw new IllegalStateException("Enhancement weights must sum to 100");
        return values;
    }

    private static Map<String, Integer> totals() {
        Map<String, Integer> out = new LinkedHashMap<>();
        for (var entry : RAW.getAsJsonObject("enhancementTotalGold").entrySet()) {
            out.put(entry.getKey(), entry.getValue().getAsInt());
        }
        return Map.copyOf(out);
    }

    private static JsonObject load() {
        try (InputStream stream = GrowthRulesV1.class.getResourceAsStream("/data/turnbound/growth/v1.json")) {
            if (stream == null) throw new IllegalStateException("Missing TURNBOUND v1 growth rules");
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.get("schemaVersion").getAsInt() != 1) throw new IllegalStateException("Invalid TURNBOUND v1 growth schema");
            return root;
        } catch (Exception ex) {
            if (ex instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("Failed loading TURNBOUND v1 growth rules", ex);
        }
    }
}
