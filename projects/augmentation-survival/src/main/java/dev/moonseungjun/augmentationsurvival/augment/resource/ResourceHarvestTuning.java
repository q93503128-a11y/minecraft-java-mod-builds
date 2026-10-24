package dev.moonseungjun.augmentationsurvival.augment.resource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for farming, block-drop and derived-harvest augments. */
public final class ResourceHarvestTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/resource_harvest_contract.json";

    private static volatile Snapshot current;

    private ResourceHarvestTuning() {
    }

    public static void initialize() {
        for (String path : new String[]{
                "bumper_crop", "seed_saver", "extra_piece", "loot_scoop",
                "replant", "sweeping_harvest", "tree_felling", "tunneling"
        }) {
            AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
        }
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded resource harvest contract: crop, block-drop and derived-harvest runtime"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Resource harvest tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = ResourceHarvestTuning.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled resource harvest contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = requireInt(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported resource harvest schema: " + schema);
                }
                JsonObject bumper = object(root, "bumper_crop");
                JsonObject seeds = object(root, "seed_saver");
                JsonObject extra = object(root, "extra_piece");
                JsonObject loot = object(root, "loot_scoop");
                JsonObject replant = object(root, "replant");
                JsonObject sweep = object(root, "sweeping_harvest");
                JsonObject tree = object(root, "tree_felling");
                JsonObject tunnel = object(root, "tunneling");
                return new Snapshot(
                        schema,
                        fraction(bumper, "bonus_chance_per_level"),
                        fraction(seeds, "bonus_chance_per_level"),
                        fraction(extra, "bonus_chance_per_level"),
                        fraction(loot, "bundle_copy_chance_per_level"),
                        positive(loot, "capture_radius"),
                        positiveInt(loot, "max_item_age_ticks"),
                        positiveInt(replant, "delay_ticks"),
                        positive(replant, "pickup_radius"),
                        intArray(sweep, "additional_crops_by_level", 3),
                        positiveInt(sweep, "search_radius"),
                        positiveInt(sweep, "vertical_radius"),
                        intArray(tree, "additional_logs_by_level", 3),
                        positiveInt(tree, "max_search_nodes"),
                        intArray(tunnel, "additional_blocks_by_level", 3),
                        fraction(tunnel, "durability_multiplier")
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read resource harvest contract", exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing resource harvest object: " + key);
        }
        return parent.getAsJsonObject(key);
    }

    private static int requireInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing resource harvest integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static int positiveInt(JsonObject object, String key) {
        int value = requireInt(object, key);
        if (value <= 0) {
            throw new IllegalStateException("Resource harvest integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing resource harvest number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Resource harvest number must be finite and positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Resource harvest fraction must be <= 1: " + key);
        }
        return value;
    }

    private static int[] intArray(JsonObject object, String key, int expected) {
        JsonArray array = object.getAsJsonArray(key);
        if (array == null || array.size() != expected) {
            throw new IllegalStateException("Resource harvest array has wrong length: " + key);
        }
        int[] values = new int[expected];
        for (int index = 0; index < expected; index++) {
            int value = array.get(index).getAsInt();
            if (value <= 0) {
                throw new IllegalStateException("Resource harvest array value must be positive: " + key);
            }
            values[index] = value;
        }
        return values;
    }

    private static int at(int[] values, int level) {
        if (level <= 0) {
            return 0;
        }
        return values[Math.min(level, values.length) - 1];
    }

    public record Snapshot(
            int schemaVersion,
            double bumperCropChancePerLevel,
            double seedSaverChancePerLevel,
            double extraPieceChancePerLevel,
            double lootScoopChancePerLevel,
            double lootCaptureRadius,
            int lootMaxItemAgeTicks,
            int replantDelayTicks,
            double replantPickupRadius,
            int[] sweepingHarvestAdditionalByLevel,
            int sweepingHarvestSearchRadius,
            int sweepingHarvestVerticalRadius,
            int[] treeFellingAdditionalByLevel,
            int treeFellingMaxSearchNodes,
            int[] tunnelingAdditionalByLevel,
            double tunnelingDurabilityMultiplier
    ) {
        public Snapshot {
            sweepingHarvestAdditionalByLevel = sweepingHarvestAdditionalByLevel.clone();
            treeFellingAdditionalByLevel = treeFellingAdditionalByLevel.clone();
            tunnelingAdditionalByLevel = tunnelingAdditionalByLevel.clone();
        }

        public int sweepingHarvestAdditional(int level) {
            return at(sweepingHarvestAdditionalByLevel, level);
        }

        public int treeFellingAdditional(int level) {
            return at(treeFellingAdditionalByLevel, level);
        }

        public int tunnelingAdditional(int level) {
            return at(tunnelingAdditionalByLevel, level);
        }
    }
}
