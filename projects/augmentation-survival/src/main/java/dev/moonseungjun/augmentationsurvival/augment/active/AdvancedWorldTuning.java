package dev.moonseungjun.augmentationsurvival.augment.active;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven contract for the advanced world/control active batch. */
public final class AdvancedWorldTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/advanced_world_contract.json";
    private static volatile Snapshot current;

    private AdvancedWorldTuning() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded advanced world contract: phase-shift, domination and gigantification"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Advanced world tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = AdvancedWorldTuning.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing advanced world contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = positiveInt(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported advanced world contract schema: " + schema);
                }
                for (String path : new String[]{"phase_shift", "domination", "gigantification", "forbidden_magic"}) {
                    AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
                }
                JsonObject phase = root.getAsJsonObject("phase_shift");
                JsonObject domination = root.getAsJsonObject("domination");
                JsonObject giant = root.getAsJsonObject("gigantification");
                return new Snapshot(
                        schema,
                        positiveInt(phase, "duration_ticks"),
                        positiveInt(phase, "cooldown_ticks"),
                        positive(domination, "range"),
                        intArray(domination, "cap", 3),
                        positiveInt(giant, "duration_ticks"),
                        positiveInt(giant, "cooldown_ticks"),
                        positiveArray(giant, "size_multiplier", 2),
                        positiveArray(giant, "health_multiplier", 2),
                        positiveArray(giant, "reach_bonus", 2),
                        fractionArray(giant, "melee_damage_bonus", 2),
                        positive(giant, "knockback_multiplier"),
                        positive(giant, "fear_radius_level_2"),
                        positiveInt(giant, "fear_ticks_level_2")
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read advanced world contract", exception);
        }
    }

    private static int positiveInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing advanced world integer: " + key);
        }
        int value = object.get(key).getAsInt();
        if (value <= 0) {
            throw new IllegalStateException("Advanced world integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing advanced world number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Advanced world number must be finite and positive: " + key);
        }
        return value;
    }

    private static int[] intArray(JsonObject object, String key, int size) {
        JsonArray array = object == null ? null : object.getAsJsonArray(key);
        if (array == null || array.size() != size) {
            throw new IllegalStateException("Advanced world integer array must have size " + size + ": " + key);
        }
        int[] values = new int[size];
        for (int index = 0; index < size; index++) {
            values[index] = array.get(index).getAsInt();
            if (values[index] <= 0) {
                throw new IllegalStateException("Advanced world integer array values must be positive: " + key);
            }
        }
        return values;
    }

    private static double[] positiveArray(JsonObject object, String key, int size) {
        JsonArray array = object == null ? null : object.getAsJsonArray(key);
        if (array == null || array.size() != size) {
            throw new IllegalStateException("Advanced world number array must have size " + size + ": " + key);
        }
        double[] values = new double[size];
        for (int index = 0; index < size; index++) {
            values[index] = array.get(index).getAsDouble();
            if (!Double.isFinite(values[index]) || values[index] <= 0.0D) {
                throw new IllegalStateException("Advanced world array values must be finite and positive: " + key);
            }
        }
        return values;
    }

    private static double[] fractionArray(JsonObject object, String key, int size) {
        double[] values = positiveArray(object, key, size);
        for (double value : values) {
            if (value > 1.0D) {
                throw new IllegalStateException("Advanced world fraction array values must be <= 1: " + key);
            }
        }
        return values;
    }

    public record Snapshot(
            int schemaVersion,
            int phaseShiftDurationTicks,
            int phaseShiftCooldownTicks,
            double dominationRange,
            int[] dominationCap,
            int gigantificationDurationTicks,
            int gigantificationCooldownTicks,
            double[] gigantificationSizeMultiplier,
            double[] gigantificationHealthMultiplier,
            double[] gigantificationReachBonus,
            double[] gigantificationMeleeDamageBonus,
            double gigantificationKnockbackMultiplier,
            double gigantificationFearRadiusLevel2,
            int gigantificationFearTicksLevel2
    ) {
        public int dominationCap(int level) {
            return dominationCap[index(level, 3)];
        }

        public double gigantificationSizeMultiplier(int level) {
            return gigantificationSizeMultiplier[index(level, 2)];
        }

        public double gigantificationHealthMultiplier(int level) {
            return gigantificationHealthMultiplier[index(level, 2)];
        }

        public double gigantificationReachBonus(int level) {
            return gigantificationReachBonus[index(level, 2)];
        }

        public double gigantificationMeleeDamageBonus(int level) {
            return gigantificationMeleeDamageBonus[index(level, 2)];
        }

        private static int index(int level, int size) {
            return Math.max(0, Math.min(size - 1, level - 1));
        }
    }
}
