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

/** Data-driven tuning for the advanced utility/combat bridge batch. */
public final class AdvancedAugmentTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/advanced_utility_contract.json";

    private static volatile Snapshot current;

    private AdvancedAugmentTuning() {
    }

    public static void initialize() {
        for (String path : new String[] {
                "armor_pierce",
                "hunt_record",
                "tree_climb",
                "pearl_mastery",
                "portable_riptide",
                "death_return"
        }) {
            AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
        }
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded advanced utility contract: armor/hunt/tree/pearl/riptide/death-return"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Advanced augment tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = AdvancedAugmentTuning.class
                .getClassLoader()
                .getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing advanced utility contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = integer(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported advanced utility schema: " + schema);
                }
                JsonObject armor = object(root, "armor_pierce");
                JsonObject hunt = object(root, "hunt_record");
                JsonObject tree = object(root, "tree_climb");
                JsonObject pearl = object(root, "pearl_mastery");
                return new Snapshot(
                        schema,
                        fractions(armor, "effectiveness_reduction", 3),
                        positiveInt(hunt, "max_stacks"),
                        positive(hunt, "max_health_per_stack"),
                        fraction(hunt, "root_damage_bonus_per_stack"),
                        positive(tree, "ladder_ascent_speed"),
                        fractions(tree, "speed_scale", 3),
                        fractions(pearl, "self_damage_reduction", 3),
                        fractions(pearl, "cooldown_reduction", 3)
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read advanced utility contract", exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing advanced utility object: " + key);
        }
        return parent.getAsJsonObject(key);
    }

    private static int integer(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing advanced utility integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static int positiveInt(JsonObject object, String key) {
        int value = integer(object, key);
        if (value <= 0) {
            throw new IllegalStateException("Advanced utility integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing advanced utility number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Advanced utility number must be positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Advanced utility fraction must be <= 1: " + key);
        }
        return value;
    }

    private static double[] fractions(JsonObject object, String key, int expected) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing advanced utility array: " + key);
        }
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() != expected) {
            throw new IllegalStateException("Wrong advanced utility array length: " + key);
        }
        double[] values = new double[expected];
        for (int index = 0; index < expected; index++) {
            double value = array.get(index).getAsDouble();
            if (!Double.isFinite(value) || value <= 0.0D || value > 1.0D) {
                throw new IllegalStateException("Invalid advanced utility fraction: " + key);
            }
            values[index] = value;
        }
        return values;
    }

    private static double at(double[] values, int level) {
        return level <= 0 ? 0.0D : values[Math.min(level, values.length) - 1];
    }

    public record Snapshot(
            int schemaVersion,
            double[] armorEffectivenessReduction,
            int huntRecordMaxStacks,
            double huntRecordMaxHealthPerStack,
            double huntRecordRootDamageBonusPerStack,
            double ladderAscentSpeed,
            double[] treeClimbSpeedScale,
            double[] pearlSelfDamageReduction,
            double[] pearlCooldownReduction
    ) {
        public Snapshot {
            armorEffectivenessReduction = armorEffectivenessReduction.clone();
            treeClimbSpeedScale = treeClimbSpeedScale.clone();
            pearlSelfDamageReduction = pearlSelfDamageReduction.clone();
            pearlCooldownReduction = pearlCooldownReduction.clone();
        }

        public double armorEffectivenessReduction(int level) {
            return at(armorEffectivenessReduction, level);
        }

        public double treeClimbSpeedScale(int level) {
            return at(treeClimbSpeedScale, level);
        }

        public double pearlSelfDamageReduction(int level) {
            return at(pearlSelfDamageReduction, level);
        }

        public double pearlCooldownReduction(int level) {
            return at(pearlCooldownReduction, level);
        }
    }
}
