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

/** Data-driven values for the advanced active/cooldown batch. */
public final class AdvancedActiveTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/advanced_active_contract.json";

    private static volatile Snapshot current;

    private AdvancedActiveTuning() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded advanced active contract: rewind, time-acceleration, shield-throw, overtime and bargaining"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Advanced active tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = AdvancedActiveTuning.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing advanced active contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = requirePositiveInt(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported advanced active contract schema: " + schema);
                }
                for (String path : new String[]{
                        "rewind", "time_acceleration", "shield_throw", "overtime", "bargaining", "mantle"
                }) {
                    AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
                }

                JsonObject rewind = root.getAsJsonObject("rewind");
                JsonObject time = root.getAsJsonObject("time_acceleration");
                JsonObject shield = root.getAsJsonObject("shield_throw");
                JsonObject overtime = root.getAsJsonObject("overtime");
                JsonObject bargaining = root.getAsJsonObject("bargaining");
                return new Snapshot(
                        schema,
                        requirePositiveInt(rewind, "history_ticks"),
                        requirePositiveInt(rewind, "cooldown_ticks"),
                        requirePositiveInt(time, "focus_ticks"),
                        requirePositiveInt(time, "duration_ticks"),
                        requirePositiveInt(time, "cooldown_ticks"),
                        fraction(time, "movement_bonus"),
                        fraction(time, "attack_speed_bonus"),
                        fraction(time, "mining_bonus"),
                        positive(time, "item_use_speed_multiplier"),
                        positive(time, "projectile_speed_multiplier"),
                        positiveArray(shield, "range", 3),
                        intArray(shield, "targets", 3),
                        fractionArray(shield, "damage_fraction", 3),
                        intArray(shield, "cooldown_ticks", 3),
                        requirePositiveInt(overtime, "normal_extension_ticks"),
                        requirePositiveInt(overtime, "boss_extension_ticks"),
                        fraction(overtime, "maximum_extension_fraction"),
                        fractionArray(bargaining, "discount_fraction", 3)
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read advanced active contract", exception);
        }
    }

    private static int requirePositiveInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing advanced active integer: " + key);
        }
        int value = object.get(key).getAsInt();
        if (value <= 0) {
            throw new IllegalStateException("Advanced active integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing advanced active number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Advanced active number must be finite and positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Advanced active fraction must be <= 1: " + key);
        }
        return value;
    }

    private static int[] intArray(JsonObject object, String key, int size) {
        JsonArray array = object == null ? null : object.getAsJsonArray(key);
        if (array == null || array.size() != size) {
            throw new IllegalStateException("Advanced active integer array must have size " + size + ": " + key);
        }
        int[] values = new int[size];
        for (int index = 0; index < size; index++) {
            values[index] = array.get(index).getAsInt();
            if (values[index] <= 0) {
                throw new IllegalStateException("Advanced active integer array values must be positive: " + key);
            }
        }
        return values;
    }

    private static double[] positiveArray(JsonObject object, String key, int size) {
        JsonArray array = object == null ? null : object.getAsJsonArray(key);
        if (array == null || array.size() != size) {
            throw new IllegalStateException("Advanced active number array must have size " + size + ": " + key);
        }
        double[] values = new double[size];
        for (int index = 0; index < size; index++) {
            values[index] = array.get(index).getAsDouble();
            if (!Double.isFinite(values[index]) || values[index] <= 0.0D) {
                throw new IllegalStateException("Advanced active number array values must be positive: " + key);
            }
        }
        return values;
    }

    private static double[] fractionArray(JsonObject object, String key, int size) {
        double[] values = positiveArray(object, key, size);
        for (double value : values) {
            if (value > 1.0D) {
                throw new IllegalStateException("Advanced active fraction array values must be <= 1: " + key);
            }
        }
        return values;
    }

    public record Snapshot(
            int schemaVersion,
            int rewindHistoryTicks,
            int rewindCooldownTicks,
            int timeAccelerationFocusTicks,
            int timeAccelerationDurationTicks,
            int timeAccelerationCooldownTicks,
            double timeAccelerationMovementBonus,
            double timeAccelerationAttackSpeedBonus,
            double timeAccelerationMiningBonus,
            double timeAccelerationItemUseSpeedMultiplier,
            double timeAccelerationProjectileSpeedMultiplier,
            double[] shieldThrowRange,
            int[] shieldThrowTargets,
            double[] shieldThrowDamageFraction,
            int[] shieldThrowCooldownTicks,
            int overtimeNormalExtensionTicks,
            int overtimeBossExtensionTicks,
            double overtimeMaximumExtensionFraction,
            double[] bargainingDiscountFraction
    ) {
        public double shieldThrowRange(int level) {
            return shieldThrowRange[index(level)];
        }

        public int shieldThrowTargets(int level) {
            return shieldThrowTargets[index(level)];
        }

        public double shieldThrowDamageFraction(int level) {
            return shieldThrowDamageFraction[index(level)];
        }

        public int shieldThrowCooldownTicks(int level) {
            return shieldThrowCooldownTicks[index(level)];
        }

        public double bargainingDiscountFraction(int level) {
            return bargainingDiscountFraction[index(level)];
        }

        private static int index(int level) {
            return Math.max(0, Math.min(2, level - 1));
        }
    }
}
