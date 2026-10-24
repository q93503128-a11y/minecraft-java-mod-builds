package dev.moonseungjun.augmentationsurvival.augment.projectile;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for shot-action and physical projectile flight-segment mechanics. */
public final class ProjectileTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/projectile_modifier_contract.json";

    private static volatile Snapshot current;

    private ProjectileTuning() {
    }

    public static void initialize() {
        requireAugment("arrow_saver");
        requireAugment("piercing_arrow");
        requireAugment("chain_arrow");
        requireAugment("scatter_shot");
        requireAugment("tracking_arrow");
        requireAugment("ricochet");
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded projectile augment contract: shared shot-action and flight-segment runtime"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Projectile tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        ClassLoader loader = ProjectileTuning.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled projectile contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read projectile augment contract", exception);
        }
    }

    private static Snapshot parse(JsonObject root) {
        int schemaVersion = requireInt(root, "schema_version");
        if (schemaVersion != 1) {
            throw new IllegalStateException("Unsupported projectile contract schema: " + schemaVersion);
        }
        JsonObject arrowSaver = requireObject(root, "arrow_saver");
        JsonObject piercing = requireObject(root, "piercing_arrow");
        JsonObject tracking = requireObject(root, "tracking_arrow");
        JsonObject ricochet = requireObject(root, "ricochet");
        JsonObject chain = requireObject(root, "chain_arrow");
        JsonObject scatter = requireObject(root, "scatter_shot");

        double[] piercingReduction = requireTriple(piercing, "followup_reduction_by_level", true);
        double[] trackingTurn = requireTriple(tracking, "turn_degrees_by_level", false);
        double[] ricochetDamage = requireTriple(ricochet, "damage_bonus_by_level", true);
        double[] scatterSpread = requireTriple(scatter, "max_spread_degrees_by_level", false);

        return new Snapshot(
                schemaVersion,
                requireFraction(arrowSaver, "chance_per_level"),
                piercingReduction,
                requirePositive(tracking, "range"),
                requirePositive(tracking, "cone_degrees"),
                trackingTurn,
                ricochetDamage,
                requirePositive(ricochet, "surface_epsilon"),
                requirePositive(chain, "range"),
                requireFraction(chain, "next_chain_multiplier"),
                requireFraction(scatter, "damage_multiplier"),
                scatterSpread
        );
    }

    private static double[] requireTriple(JsonObject object, String key, boolean allowZero) {
        if (!object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing projectile array: " + key);
        }
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() != 3) {
            throw new IllegalStateException("Projectile array must contain 3 values: " + key);
        }
        double[] result = new double[3];
        for (int index = 0; index < 3; index++) {
            double value = array.get(index).getAsDouble();
            if (!Double.isFinite(value) || (allowZero ? value < 0.0D : value <= 0.0D)) {
                throw new IllegalStateException("Invalid projectile array value: " + key);
            }
            result[index] = value;
        }
        return result;
    }

    private static JsonObject requireObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing projectile object: " + key);
        }
        return object.getAsJsonObject(key);
    }

    private static int requireInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing projectile integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static double requirePositive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing projectile number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Projectile number must be finite and positive: " + key);
        }
        return value;
    }

    private static double requireFraction(JsonObject object, String key) {
        double value = requirePositive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Projectile fraction must be <= 1: " + key);
        }
        return value;
    }

    private static void requireAugment(String path) {
        AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
    }

    public record Snapshot(
            int schemaVersion,
            double arrowSaverChancePerLevel,
            double[] piercingFollowupReductionByLevel,
            double trackingRange,
            double trackingConeDegrees,
            double[] trackingTurnDegreesByLevel,
            double[] ricochetDamageBonusByLevel,
            double ricochetSurfaceEpsilon,
            double chainRange,
            double chainNextMultiplier,
            double scatterDamageMultiplier,
            double[] scatterMaxSpreadDegreesByLevel
    ) {
        public double piercingFollowupReduction(int level) {
            return levelValue(piercingFollowupReductionByLevel, level);
        }

        public double trackingTurnDegrees(int level) {
            return levelValue(trackingTurnDegreesByLevel, level);
        }

        public double ricochetDamageBonus(int level) {
            return levelValue(ricochetDamageBonusByLevel, level);
        }

        public double scatterMaxSpreadDegrees(int level) {
            return levelValue(scatterMaxSpreadDegreesByLevel, level);
        }

        private static double levelValue(double[] values, int level) {
            if (level <= 0) {
                return 0.0D;
            }
            return values[Math.min(values.length, level) - 1];
        }
    }
}
