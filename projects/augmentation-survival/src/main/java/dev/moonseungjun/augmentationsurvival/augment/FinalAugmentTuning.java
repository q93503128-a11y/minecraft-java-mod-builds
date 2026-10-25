package dev.moonseungjun.augmentationsurvival.augment;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven parameters for the final eight gameplay runtimes. */
public final class FinalAugmentTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/final_augment_contract.json";
    private static volatile Snapshot current;

    private FinalAugmentTuning() {
    }

    public static void initialize() {
        for (String path : new String[]{
                "dexterity", "potion_diffusion", "elastic_storage", "ballistic_barrier",
                "temporary_platform", "life_link", "shrink", "wall_slam"
        }) {
            AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
        }
        current = loadBundled();
        AugmentationSurvival.LOGGER.info("Loaded final eight augment runtime contract");
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Final augment tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = FinalAugmentTuning.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing final augment contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = positiveInt(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported final augment schema: " + schema);
                }
                JsonObject dexterity = object(root, "dexterity");
                JsonObject potion = object(root, "potion_diffusion");
                JsonObject elastic = object(root, "elastic_storage");
                JsonObject barrier = object(root, "ballistic_barrier");
                JsonObject platform = object(root, "temporary_platform");
                JsonObject link = object(root, "life_link");
                JsonObject shrink = object(root, "shrink");
                JsonObject wall = object(root, "wall_slam");
                return new Snapshot(
                        schema,
                        fraction(dexterity, "bonus_chance_per_level"),
                        positive(potion, "radius"),
                        numberArray(potion, "effect_fraction_by_level", 3, true),
                        positive(elastic, "minimum_fall_distance"),
                        positiveInt(elastic, "storage_ticks"),
                        numberArray(elastic, "return_fraction_by_level", 3, true),
                        positive(elastic, "vertical_velocity_per_fall_block"),
                        positive(elastic, "maximum_vertical_bonus"),
                        positive(barrier, "radius"),
                        intArray(barrier, "interval_ticks_by_level", 3),
                        intArray(barrier, "destroy_count_by_level", 3),
                        positiveInt(platform, "duration_ticks"),
                        positiveInt(platform, "cooldown_ticks"),
                        positiveInt(platform, "radius"),
                        positive(link, "radius"),
                        positiveInt(link, "max_targets"),
                        positiveInt(link, "duration_ticks"),
                        fraction(link, "shared_damage_fraction"),
                        positiveInt(shrink, "crouch_hold_ticks"),
                        fraction(shrink, "scale_multiplier"),
                        positiveInt(wall, "collision_window_ticks"),
                        numberArray(wall, "damage_fraction_by_level", 3, true),
                        positive(wall, "minimum_horizontal_speed_squared")
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read final augment contract", exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing final augment object: " + key);
        }
        return parent.getAsJsonObject(key);
    }

    private static int positiveInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing final augment integer: " + key);
        }
        int value = object.get(key).getAsInt();
        if (value <= 0) {
            throw new IllegalStateException("Final augment integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing final augment number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Final augment number must be finite and positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Final augment fraction must be <= 1: " + key);
        }
        return value;
    }

    private static int[] intArray(JsonObject object, String key, int expected) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null || array.size() != expected) {
            throw new IllegalStateException("Final augment int array has wrong length: " + key);
        }
        int[] values = new int[expected];
        for (int i = 0; i < expected; i++) {
            int value = array.get(i).getAsInt();
            if (value <= 0) {
                throw new IllegalStateException("Final augment int array value must be positive: " + key);
            }
            values[i] = value;
        }
        return values;
    }

    private static double[] numberArray(JsonObject object, String key, int expected, boolean fraction) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null || array.size() != expected) {
            throw new IllegalStateException("Final augment number array has wrong length: " + key);
        }
        double[] values = new double[expected];
        for (int i = 0; i < expected; i++) {
            double value = array.get(i).getAsDouble();
            if (!Double.isFinite(value) || value <= 0.0D || (fraction && value > 1.0D)) {
                throw new IllegalStateException("Invalid final augment array value: " + key);
            }
            values[i] = value;
        }
        return values;
    }

    private static int at(int[] values, int level) {
        return level <= 0 ? 0 : values[Math.min(level, values.length) - 1];
    }

    private static double at(double[] values, int level) {
        return level <= 0 ? 0.0D : values[Math.min(level, values.length) - 1];
    }

    public record Snapshot(
            int schemaVersion,
            double dexterityBonusChancePerLevel,
            double potionDiffusionRadius,
            double[] potionDiffusionFractionByLevel,
            double elasticMinimumFallDistance,
            int elasticStorageTicks,
            double[] elasticReturnFractionByLevel,
            double elasticVerticalVelocityPerFallBlock,
            double elasticMaximumVerticalBonus,
            double ballisticBarrierRadius,
            int[] ballisticBarrierIntervalTicksByLevel,
            int[] ballisticBarrierDestroyCountByLevel,
            int temporaryPlatformDurationTicks,
            int temporaryPlatformCooldownTicks,
            int temporaryPlatformRadius,
            double lifeLinkRadius,
            int lifeLinkMaxTargets,
            int lifeLinkDurationTicks,
            double lifeLinkSharedDamageFraction,
            int shrinkCrouchHoldTicks,
            double shrinkScaleMultiplier,
            int wallSlamCollisionWindowTicks,
            double[] wallSlamDamageFractionByLevel,
            double wallSlamMinimumHorizontalSpeedSquared
    ) {
        public Snapshot {
            potionDiffusionFractionByLevel = potionDiffusionFractionByLevel.clone();
            elasticReturnFractionByLevel = elasticReturnFractionByLevel.clone();
            ballisticBarrierIntervalTicksByLevel = ballisticBarrierIntervalTicksByLevel.clone();
            ballisticBarrierDestroyCountByLevel = ballisticBarrierDestroyCountByLevel.clone();
            wallSlamDamageFractionByLevel = wallSlamDamageFractionByLevel.clone();
        }

        public double potionDiffusionFraction(int level) { return at(potionDiffusionFractionByLevel, level); }
        public double elasticReturnFraction(int level) { return at(elasticReturnFractionByLevel, level); }
        public int ballisticBarrierIntervalTicks(int level) { return at(ballisticBarrierIntervalTicksByLevel, level); }
        public int ballisticBarrierDestroyCount(int level) { return at(ballisticBarrierDestroyCountByLevel, level); }
        public double wallSlamDamageFraction(int level) { return at(wallSlamDamageFractionByLevel, level); }
    }
}
