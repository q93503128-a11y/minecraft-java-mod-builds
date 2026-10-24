package dev.moonseungjun.augmentationsurvival.augment.movement;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for movement augments that operate on water flow or controlled mounts. */
public final class MobilityUtilityTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/mobility_utility_contract.json";

    private static volatile Snapshot current;

    private MobilityUtilityTuning() {
    }

    public static void initialize() {
        AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":ride_current");
        AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":rider");
        current = loadBundled();
        AugmentationSurvival.LOGGER.info("Loaded mobility utility contract: water flow and mount runtime");
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Mobility utility tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        ClassLoader loader = MobilityUtilityTuning.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing mobility utility contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = root.get("schema_version").getAsInt();
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported mobility utility schema: " + schema);
                }
                return new Snapshot(
                        schema,
                        fractions(root.getAsJsonObject("ride_current"), "movement_bonus", 3),
                        fractions(root.getAsJsonObject("rider"), "movement_bonus", 3),
                        fractions(root.getAsJsonObject("rider"), "jump_bonus", 3)
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read mobility utility contract", exception);
        }
    }

    private static double[] fractions(JsonObject object, String key, int expected) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing mobility utility array: " + key);
        }
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() != expected) {
            throw new IllegalStateException("Wrong mobility utility array length: " + key);
        }
        double[] values = new double[expected];
        for (int index = 0; index < expected; index++) {
            double value = array.get(index).getAsDouble();
            if (!Double.isFinite(value) || value <= 0.0D || value > 1.0D) {
                throw new IllegalStateException("Mobility utility fraction must be in (0, 1]: " + key);
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
            double[] rideCurrentMovementBonus,
            double[] riderMovementBonus,
            double[] riderJumpBonus
    ) {
        public Snapshot {
            rideCurrentMovementBonus = rideCurrentMovementBonus.clone();
            riderMovementBonus = riderMovementBonus.clone();
            riderJumpBonus = riderJumpBonus.clone();
        }

        public double rideCurrentMovementBonus(int level) { return at(rideCurrentMovementBonus, level); }
        public double riderMovementBonus(int level) { return at(riderMovementBonus, level); }
        public double riderJumpBonus(int level) { return at(riderJumpBonus, level); }
    }
}
