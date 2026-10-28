package dev.moonseungjun.augmentationsurvival.augment.survival;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for the shared survival/resource runtime. */
public final class SurvivalTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/survival_modifier_contract.json";
    private static final int VANILLA_NIGHT_VISION_FLICKER_WINDOW_TICKS = 200;

    private static volatile Snapshot current;

    private SurvivalTuning() {
    }

    public static void initialize() {
        for (String path : new String[]{
                "logging_knack", "nocturnal", "continuous_mining", "efficient_metabolism",
                "webbed_feet", "long_breath", "cold_adaptation", "tool_care", "armor_care",
                "recovery", "sun_affinity", "night_vision", "auto_refill"
        }) {
            AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
        }
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded survival augment contract: shared resource, hunger, environment and utility runtime"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Survival tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        ClassLoader loader = SurvivalTuning.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled survival contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = requireInt(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported survival contract schema: " + schema);
                }
                JsonObject logging = object(root, "logging_knack");
                JsonObject nocturnal = object(root, "nocturnal");
                JsonObject mining = object(root, "continuous_mining");
                JsonObject metabolism = object(root, "efficient_metabolism");
                JsonObject webbed = object(root, "webbed_feet");
                JsonObject breath = object(root, "long_breath");
                JsonObject cold = object(root, "cold_adaptation");
                JsonObject tool = object(root, "tool_care");
                JsonObject armor = object(root, "armor_care");
                JsonObject recovery = object(root, "recovery");
                JsonObject sun = object(root, "sun_affinity");
                JsonObject nightVision = object(root, "night_vision");
                int nightVisionDuration = positiveInt(nightVision, "refresh_duration_ticks");
                int nightVisionThreshold = positiveInt(nightVision, "refresh_threshold_ticks");
                if (nightVisionThreshold <= VANILLA_NIGHT_VISION_FLICKER_WINDOW_TICKS) {
                    throw new IllegalStateException(
                            "Night vision refresh threshold must stay above vanilla flicker window: "
                                    + nightVisionThreshold
                    );
                }
                if (nightVisionDuration <= nightVisionThreshold) {
                    throw new IllegalStateException(
                            "Night vision refresh duration must exceed refresh threshold: "
                                    + nightVisionDuration + " <= " + nightVisionThreshold
                    );
                }

                return new Snapshot(
                        schema,
                        positive(logging, "block_break_speed_per_level"),
                        positive(nocturnal, "mining_speed_per_level"),
                        positiveInt(mining, "window_ticks"),
                        positiveInt(mining, "max_stacks"),
                        positive(mining, "block_break_speed_per_stack_per_level"),
                        fraction(metabolism, "exhaustion_reduction_per_level"),
                        positive(webbed, "swim_speed_per_level"),
                        positiveArray(breath, "oxygen_bonus_by_level", 3),
                        fractionArray(cold, "freeze_buildup_reduction_by_level", 3),
                        fraction(tool, "durability_avoid_per_level"),
                        fraction(armor, "durability_avoid_per_level"),
                        fraction(recovery, "natural_regen_interval_reduction_per_level"),
                        fraction(sun, "natural_regen_interval_reduction_per_level"),
                        nightVisionDuration,
                        nightVisionThreshold
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read survival augment contract", exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing survival object: " + key);
        }
        return parent.getAsJsonObject(key);
    }

    private static int requireInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing survival integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static int positiveInt(JsonObject object, String key) {
        int value = requireInt(object, key);
        if (value <= 0) {
            throw new IllegalStateException("Survival integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing survival number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Survival number must be finite and positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Survival fraction must be <= 1: " + key);
        }
        return value;
    }

    private static double[] positiveArray(JsonObject object, String key, int expected) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null || array.size() != expected) {
            throw new IllegalStateException("Survival array has wrong length: " + key);
        }
        double[] values = new double[expected];
        for (int i = 0; i < expected; i++) {
            double value = array.get(i).getAsDouble();
            if (!Double.isFinite(value) || value <= 0.0D) {
                throw new IllegalStateException("Survival array value must be positive: " + key);
            }
            values[i] = value;
        }
        return values;
    }

    private static double[] fractionArray(JsonObject object, String key, int expected) {
        double[] values = positiveArray(object, key, expected);
        for (double value : values) {
            if (value > 1.0D) {
                throw new IllegalStateException("Survival array fraction must be <= 1: " + key);
            }
        }
        return values;
    }

    private static double at(double[] values, int level) {
        if (level <= 0) {
            return 0.0D;
        }
        return values[Math.min(level, values.length) - 1];
    }

    public record Snapshot(
            int schemaVersion,
            double loggingBreakSpeedPerLevel,
            double nocturnalMiningSpeedPerLevel,
            int continuousMiningWindowTicks,
            int continuousMiningMaxStacks,
            double continuousMiningSpeedPerStackPerLevel,
            double metabolismExhaustionReductionPerLevel,
            double webbedFeetSwimSpeedPerLevel,
            double[] oxygenBonusByLevel,
            double[] freezeReductionByLevel,
            double toolCareAvoidPerLevel,
            double armorCareAvoidPerLevel,
            double recoveryIntervalReductionPerLevel,
            double sunAffinityRegenReductionPerLevel,
            int nightVisionRefreshDurationTicks,
            int nightVisionRefreshThresholdTicks
    ) {
        public Snapshot {
            oxygenBonusByLevel = oxygenBonusByLevel.clone();
            freezeReductionByLevel = freezeReductionByLevel.clone();
        }

        public double oxygenBonus(int level) { return at(oxygenBonusByLevel, level); }
        public double freezeReduction(int level) { return at(freezeReductionByLevel, level); }
    }
}
