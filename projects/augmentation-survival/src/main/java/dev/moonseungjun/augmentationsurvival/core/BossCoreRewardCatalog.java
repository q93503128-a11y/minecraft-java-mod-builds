package dev.moonseungjun.augmentationsurvival.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;

/** Validated runtime view of the canonical boss-core reward contract. */
public final class BossCoreRewardCatalog {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/boss_core_reward_contract.json";
    private static volatile Catalog current;

    private BossCoreRewardCatalog() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded boss core reward contract: {} reward bands, {} exact vanilla assignments",
                current.rewardBands().size(),
                current.exactAssignments().size()
        );
    }

    public static Catalog current() {
        Catalog catalog = current;
        if (catalog == null) {
            throw new IllegalStateException("Boss core reward catalog is not initialized");
        }
        return catalog;
    }

    public static Optional<Assignment> exactAssignment(Identifier bossId) {
        return Optional.ofNullable(current().exactAssignments().get(bossId.toString()));
    }

    public static List<AugmentTier> rollExactBossReward(
            Identifier bossId,
            boolean firstKill,
            RandomSource random
    ) {
        Assignment assignment = exactAssignment(bossId)
                .orElseThrow(() -> new IllegalArgumentException("No exact boss reward assignment: " + bossId));
        RewardBand band = current().rewardBands().get(assignment.rewardBand());
        if (band == null) {
            throw new IllegalStateException("Missing boss reward band: " + assignment.rewardBand());
        }

        Map<AugmentTier, Double> weights = !firstKill
                && assignment.repeatPolicy().equals("pinnacle_repeat_weights")
                && !band.repeatWeights().isEmpty()
                ? band.repeatWeights()
                : band.weights();

        int count = band.guaranteedCoreCount()
                + (firstKill ? band.firstKillBonusCoreCount() : 0);
        List<AugmentTier> result = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            result.add(rollTier(weights, random));
        }
        return List.copyOf(result);
    }

    private static AugmentTier rollTier(Map<AugmentTier, Double> weights, RandomSource random) {
        double roll = random.nextDouble();
        double cumulative = 0.0;
        AugmentTier last = null;
        for (AugmentTier tier : AugmentTier.values()) {
            Double weight = weights.get(tier);
            if (weight == null || weight <= 0.0) {
                continue;
            }
            last = tier;
            cumulative += weight;
            if (roll < cumulative) {
                return tier;
            }
        }
        if (last == null) {
            throw new IllegalStateException("Cannot roll empty boss reward weights");
        }
        return last;
    }

    private static Catalog loadBundled() {
        try (InputStream stream = BossCoreRewardCatalog.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled boss core reward contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read boss core reward contract", exception);
        }
    }

    private static Catalog parse(JsonObject root) {
        int schema = root.get("schema_version").getAsInt();
        if (schema != 2) {
            throw new IllegalStateException("Unsupported boss core reward schema: " + schema);
        }
        if (!root.get("exact_weights_defined").getAsBoolean()) {
            throw new IllegalStateException("Boss core reward contract must define exact weights");
        }

        Map<String, RewardBand> bands = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("reward_bands")) {
            JsonObject object = element.getAsJsonObject();
            String id = requiredString(object, "id");
            RewardBand band = new RewardBand(
                    id,
                    positiveInt(object, "guaranteed_core_count"),
                    parseWeights(object.getAsJsonObject("weights")),
                    positiveInt(object, "first_kill_bonus_core_count"),
                    object.has("repeat_weights")
                            ? parseWeights(object.getAsJsonObject("repeat_weights"))
                            : Map.of()
            );
            if (bands.putIfAbsent(id, band) != null) {
                throw new IllegalStateException("Duplicate boss reward band: " + id);
            }
        }

        Map<String, Assignment> assignments = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("vanilla_assignments")) {
            JsonObject object = element.getAsJsonObject();
            String source = requiredString(object, "source");
            String rewardBand = requiredString(object, "reward_band");
            if (!bands.containsKey(rewardBand)) {
                throw new IllegalStateException("Unknown boss reward band in assignment: " + rewardBand);
            }
            Assignment assignment = new Assignment(
                    rewardBand,
                    requiredString(object, "repeat_policy")
            );
            if (assignments.putIfAbsent(source, assignment) != null) {
                throw new IllegalStateException("Duplicate boss reward assignment: " + source);
            }
        }

        Map<String, String> externalDefaults = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("sources")) {
            JsonObject object = element.getAsJsonObject();
            String modId = requiredString(object, "mod_id");
            String band = requiredString(object, "default_reward_band");
            if (!bands.containsKey(band)) {
                throw new IllegalStateException("Unknown external default reward band: " + band);
            }
            if (externalDefaults.putIfAbsent(modId, band) != null) {
                throw new IllegalStateException("Duplicate external boss source: " + modId);
            }
        }

        return new Catalog(
                schema,
                Collections.unmodifiableMap(bands),
                Collections.unmodifiableMap(assignments),
                Collections.unmodifiableMap(externalDefaults)
        );
    }

    private static int positiveInt(JsonObject object, String key) {
        int value = object.get(key).getAsInt();
        if (value <= 0) {
            throw new IllegalStateException(key + " must be positive: " + value);
        }
        return value;
    }

    private static Map<AugmentTier, Double> parseWeights(JsonObject object) {
        EnumMap<AugmentTier, Double> weights = new EnumMap<>(AugmentTier.class);
        double sum = 0.0;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            AugmentTier tier = AugmentTier.fromId(entry.getKey());
            double weight = entry.getValue().getAsDouble();
            if (!Double.isFinite(weight) || weight <= 0.0 || weight > 1.0) {
                throw new IllegalStateException("Invalid boss reward weight " + entry.getKey() + ": " + weight);
            }
            weights.put(tier, weight);
            sum += weight;
        }
        if (Math.abs(sum - 1.0) > 0.000001) {
            throw new IllegalStateException("Boss reward weights must sum to 1.0, got " + sum);
        }
        return Collections.unmodifiableMap(weights);
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new IllegalStateException("Missing boss reward field: " + key);
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank boss reward field: " + key);
        }
        return value;
    }

    public record Catalog(
            int schemaVersion,
            Map<String, RewardBand> rewardBands,
            Map<String, Assignment> exactAssignments,
            Map<String, String> externalDefaultRewardBands
    ) {
    }

    public record RewardBand(
            String id,
            int guaranteedCoreCount,
            Map<AugmentTier, Double> weights,
            int firstKillBonusCoreCount,
            Map<AugmentTier, Double> repeatWeights
    ) {
    }

    public record Assignment(String rewardBand, String repeatPolicy) {
    }
}
