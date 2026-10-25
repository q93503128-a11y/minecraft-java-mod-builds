package dev.moonseungjun.augmentationsurvival.augment.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for food, fishing, breeding and abundance behavior. */
public final class ConsumptionAugmentTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/consumption_fishing_contract.json";

    private static volatile Snapshot current;

    private ConsumptionAugmentTuning() {
    }

    public static void initialize() {
        current = loadBundled();
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Consumption/fishing tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = ConsumptionAugmentTuning.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled consumption/fishing contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = requireInt(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported consumption/fishing schema: " + schema);
                }
                return new Snapshot(
                        schema,
                        doubleArray(object(root, "hearty_meal"), "saturation_bonus_by_level", 3),
                        doubleArray(object(root, "fishing_sense"), "wait_reduction_by_level", 3),
                        fraction(object(root, "fast_eating"), "reduction_per_level"),
                        doubleArray(object(root, "treasure_fishing"), "absolute_chance_bonus_by_level", 3),
                        doubleArray(object(root, "twins"), "extra_baby_chance_by_level", 3),
                        doubleArray(object(root, "eat_while_walking"), "penalty_ease_by_level", 3),
                        intArray(object(root, "reserve_meal"), "capacity_by_level", 3),
                        positive(object(root, "abundance"), "capture_radius"),
                        positiveInt(object(root, "abundance"), "max_item_age_ticks"),
                        positiveInt(object(root, "abundance"), "block_delay_ticks"),
                        positiveInt(object(root, "abundance"), "mob_delay_ticks"),
                        positiveInt(object(root, "abundance"), "fishing_delay_ticks")
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read consumption/fishing contract", exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing consumption/fishing object: " + key);
        }
        return parent.getAsJsonObject(key);
    }

    private static int requireInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing consumption/fishing integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static int positiveInt(JsonObject object, String key) {
        int value = requireInt(object, key);
        if (value <= 0) {
            throw new IllegalStateException("Consumption/fishing integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing consumption/fishing number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Consumption/fishing number must be finite and positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Consumption/fishing fraction must be <= 1: " + key);
        }
        return value;
    }

    private static double[] doubleArray(JsonObject object, String key, int expected) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null || array.size() != expected) {
            throw new IllegalStateException("Consumption/fishing array has wrong length: " + key);
        }
        double[] values = new double[expected];
        for (int index = 0; index < expected; index++) {
            double value = array.get(index).getAsDouble();
            if (!Double.isFinite(value) || value < 0.0D || value > 1.0D) {
                throw new IllegalStateException("Consumption/fishing fraction array invalid: " + key);
            }
            values[index] = value;
        }
        return values;
    }

    private static int[] intArray(JsonObject object, String key, int expected) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null || array.size() != expected) {
            throw new IllegalStateException("Consumption/fishing integer array has wrong length: " + key);
        }
        int[] values = new int[expected];
        for (int index = 0; index < expected; index++) {
            int value = array.get(index).getAsInt();
            if (value <= 0) {
                throw new IllegalStateException("Consumption/fishing integer array invalid: " + key);
            }
            values[index] = value;
        }
        return values;
    }

    private static double at(double[] values, int level) {
        return level <= 0 ? 0.0D : values[Math.min(level, values.length) - 1];
    }

    private static int at(int[] values, int level) {
        return level <= 0 ? 0 : values[Math.min(level, values.length) - 1];
    }

    public record Snapshot(
            int schemaVersion,
            double[] heartySaturationBonusByLevel,
            double[] fishingWaitReductionByLevel,
            double fastEatingReductionPerLevel,
            double[] treasureChanceBonusByLevel,
            double[] twinsChanceByLevel,
            double[] eatWhileWalkingEaseByLevel,
            int[] reserveCapacityByLevel,
            double abundanceCaptureRadius,
            int abundanceMaxItemAgeTicks,
            int abundanceBlockDelayTicks,
            int abundanceMobDelayTicks,
            int abundanceFishingDelayTicks
    ) {
        public Snapshot {
            heartySaturationBonusByLevel = heartySaturationBonusByLevel.clone();
            fishingWaitReductionByLevel = fishingWaitReductionByLevel.clone();
            treasureChanceBonusByLevel = treasureChanceBonusByLevel.clone();
            twinsChanceByLevel = twinsChanceByLevel.clone();
            eatWhileWalkingEaseByLevel = eatWhileWalkingEaseByLevel.clone();
            reserveCapacityByLevel = reserveCapacityByLevel.clone();
        }

        public double heartySaturationBonus(int level) {
            return at(heartySaturationBonusByLevel, level);
        }

        public double fishingWaitReduction(int level) {
            return at(fishingWaitReductionByLevel, level);
        }

        public double treasureChanceBonus(int level) {
            return at(treasureChanceBonusByLevel, level);
        }

        public double twinsChance(int level) {
            return at(twinsChanceByLevel, level);
        }

        public double eatWhileWalkingEase(int level) {
            return at(eatWhileWalkingEaseByLevel, level);
        }

        public int reserveCapacity(int level) {
            return at(reserveCapacityByLevel, level);
        }
    }
}
