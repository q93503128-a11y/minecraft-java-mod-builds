package dev.moonseungjun.augmentationsurvival.augment.utility;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for the dense utility/passive implementation batch. */
public final class UtilityAugmentTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/utility_runtime_contract.json";

    private static volatile Snapshot current;

    private UtilityAugmentTuning() {
    }

    public static void initialize() {
        for (String path : new String[] {
                "pickup_hand",
                "shield_grip",
                "fuel_saver",
                "fast_brewing",
                "water_run",
                "threat_suppression",
                "instant_mining",
                "experience_preservation",
                "focused_aim"
        }) {
            AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
        }
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded utility augment contract: pickup/shield/machines/water/toggles/xp/focused aim"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Utility augment tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        ClassLoader loader = UtilityAugmentTuning.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing utility augment contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = root.get("schema_version").getAsInt();
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported utility augment schema: " + schema);
                }
                JsonObject focused = root.getAsJsonObject("focused_aim");
                return new Snapshot(
                        schema,
                        positiveArray(root.getAsJsonObject("pickup_hand"), "radius", 3, false),
                        fractionArray(root.getAsJsonObject("shield_grip"), "disable_reduction", 3),
                        fractionArray(root.getAsJsonObject("fuel_saver"), "burn_time_bonus", 3),
                        fractionArray(root.getAsJsonObject("fast_brewing"), "time_reduction", 3),
                        positive(root, "machine_holder_radius"),
                        fractionArray(focused, "damage_bonus", 3),
                        positiveInt(focused, "full_draw_ticks"),
                        positiveInt(focused, "stationary_ticks"),
                        fractionArray(root.getAsJsonObject("experience_preservation"), "retain_fraction", 3),
                        positive(root.getAsJsonObject("threat_suppression"), "radius")
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read utility augment contract", exception);
        }
    }

    private static double[] fractionArray(JsonObject object, String key, int expected) {
        double[] values = positiveArray(object, key, expected, true);
        for (double value : values) {
            if (value > 1.0D) {
                throw new IllegalStateException("Utility fraction must be <= 1: " + key);
            }
        }
        return values;
    }

    private static double[] positiveArray(
            JsonObject object,
            String key,
            int expected,
            boolean allowOne
    ) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing utility array: " + key);
        }
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() != expected) {
            throw new IllegalStateException("Wrong utility array length: " + key);
        }
        double[] values = new double[expected];
        for (int index = 0; index < expected; index++) {
            double value = array.get(index).getAsDouble();
            if (!Double.isFinite(value) || value <= 0.0D || (!allowOne && value > 64.0D)) {
                throw new IllegalStateException("Invalid utility value in " + key + ": " + value);
            }
            values[index] = value;
        }
        return values;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing utility value: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Utility value must be positive: " + key);
        }
        return value;
    }

    private static int positiveInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing utility integer: " + key);
        }
        int value = object.get(key).getAsInt();
        if (value <= 0) {
            throw new IllegalStateException("Utility integer must be positive: " + key);
        }
        return value;
    }

    private static double at(double[] values, int level) {
        return level <= 0 ? 0.0D : values[Math.min(level, values.length) - 1];
    }

    public record Snapshot(
            int schemaVersion,
            double[] pickupRadius,
            double[] shieldDisableReduction,
            double[] fuelBurnTimeBonus,
            double[] brewingTimeReduction,
            double machineHolderRadius,
            double[] focusedAimDamageBonus,
            int focusedAimFullDrawTicks,
            int focusedAimStationaryTicks,
            double[] experienceRetainFraction,
            double threatSuppressionRadius
    ) {
        public Snapshot {
            pickupRadius = pickupRadius.clone();
            shieldDisableReduction = shieldDisableReduction.clone();
            fuelBurnTimeBonus = fuelBurnTimeBonus.clone();
            brewingTimeReduction = brewingTimeReduction.clone();
            focusedAimDamageBonus = focusedAimDamageBonus.clone();
            experienceRetainFraction = experienceRetainFraction.clone();
        }

        public double pickupRadius(int level) { return at(pickupRadius, level); }
        public double shieldDisableReduction(int level) { return at(shieldDisableReduction, level); }
        public double fuelBurnTimeBonus(int level) { return at(fuelBurnTimeBonus, level); }
        public double brewingTimeReduction(int level) { return at(brewingTimeReduction, level); }
        public double focusedAimDamageBonus(int level) { return at(focusedAimDamageBonus, level); }
        public double experienceRetainFraction(int level) { return at(experienceRetainFraction, level); }
    }
}
