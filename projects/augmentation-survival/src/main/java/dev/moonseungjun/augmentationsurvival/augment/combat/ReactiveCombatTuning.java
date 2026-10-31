package dev.moonseungjun.augmentationsurvival.augment.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for the reactive/status/protection combat batch. */
public final class ReactiveCombatTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/reactive_combat_contract.json";

    private static volatile Snapshot current;

    private ReactiveCombatTuning() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded reactive combat contract: shield compression/status/shield/path/invulnerability runtime"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Reactive combat tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        for (String path : new String[]{
                "return_to_sender", "status_reflect", "strong_mind",
                "shield_burst", "retrace", "invincibility"
        }) {
            AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
        }

        ClassLoader loader = ReactiveCombatTuning.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing reactive combat contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = integer(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported reactive combat schema: " + schema);
                }
                JsonObject reflected = object(root, "return_to_sender");
                JsonObject status = object(root, "status_reflect");
                JsonObject mind = object(root, "strong_mind");
                JsonObject burst = object(root, "shield_burst");
                JsonObject retrace = object(root, "retrace");
                JsonObject invulnerability = object(root, "invulnerability");
                return new Snapshot(
                        schema,
                        positiveInts(reflected, "block_count_threshold", 3),
                        fractions(reflected, "damage_fraction", 3),
                        positive(reflected, "wave_range"),
                        coneHalfAngle(reflected, "wave_half_angle_degrees"),
                        fraction(status, "duration_fraction"),
                        fractions(mind, "duration_reduction", 3),
                        positive(burst, "radius"),
                        positiveArray(burst, "damage_multiplier", 3),
                        positiveArray(burst, "damage_cap", 3),
                        positiveInt(retrace, "history_ticks"),
                        fractions(retrace, "damage_fraction", 3),
                        positiveInt(retrace, "cooldown_ticks"),
                        positiveInt(invulnerability, "interval_ticks"),
                        positiveInt(invulnerability, "duration_ticks")
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read reactive combat contract", exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing reactive combat object: " + key);
        }
        return parent.getAsJsonObject(key);
    }

    private static int integer(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing reactive combat integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static int positiveInt(JsonObject object, String key) {
        int value = integer(object, key);
        if (value <= 0) {
            throw new IllegalStateException("Reactive combat integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing reactive combat number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Reactive combat number must be positive: " + key);
        }
        return value;
    }

    private static double coneHalfAngle(JsonObject object, String key) {
        double value = positive(object, key);
        if (value >= 90.0D) {
            throw new IllegalStateException("Reactive combat cone half-angle must be < 90 degrees: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Reactive combat fraction must be <= 1: " + key);
        }
        return value;
    }

    private static JsonArray array(JsonObject object, String key, int expected) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing reactive combat array: " + key);
        }
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() != expected) {
            throw new IllegalStateException("Wrong reactive combat array length: " + key);
        }
        return array;
    }

    private static int[] positiveInts(JsonObject object, String key, int expected) {
        JsonArray array = array(object, key, expected);
        int[] values = new int[expected];
        for (int index = 0; index < expected; index++) {
            int value = array.get(index).getAsInt();
            if (value <= 0) {
                throw new IllegalStateException("Reactive combat array integer must be positive: " + key);
            }
            values[index] = value;
        }
        return values;
    }

    private static double[] positiveArray(JsonObject object, String key, int expected) {
        JsonArray array = array(object, key, expected);
        double[] values = new double[expected];
        for (int index = 0; index < expected; index++) {
            double value = array.get(index).getAsDouble();
            if (!Double.isFinite(value) || value <= 0.0D) {
                throw new IllegalStateException("Reactive combat array value must be positive: " + key);
            }
            values[index] = value;
        }
        return values;
    }

    private static double[] fractions(JsonObject object, String key, int expected) {
        double[] values = positiveArray(object, key, expected);
        for (double value : values) {
            if (value > 1.0D) {
                throw new IllegalStateException("Reactive combat array fraction must be <= 1: " + key);
            }
        }
        return values;
    }

    private static int at(int[] values, int level) {
        if (level <= 0) {
            return 0;
        }
        return values[Math.min(level, values.length) - 1];
    }

    private static double at(double[] values, int level) {
        if (level <= 0) {
            return 0.0D;
        }
        return values[Math.min(level, values.length) - 1];
    }

    public record Snapshot(
            int schemaVersion,
            int[] returnBlockCountThreshold,
            double[] returnDamageFraction,
            double returnWaveRange,
            double returnWaveHalfAngleDegrees,
            double statusReflectDurationFraction,
            double[] strongMindDurationReduction,
            double shieldBurstRadius,
            double[] shieldBurstDamageMultiplier,
            double[] shieldBurstDamageCap,
            int retraceHistoryTicks,
            double[] retraceDamageFraction,
            int retraceCooldownTicks,
            int invulnerabilityIntervalTicks,
            int invulnerabilityDurationTicks
    ) {
        public Snapshot {
            returnBlockCountThreshold = returnBlockCountThreshold.clone();
            returnDamageFraction = returnDamageFraction.clone();
            strongMindDurationReduction = strongMindDurationReduction.clone();
            shieldBurstDamageMultiplier = shieldBurstDamageMultiplier.clone();
            shieldBurstDamageCap = shieldBurstDamageCap.clone();
            retraceDamageFraction = retraceDamageFraction.clone();
        }

        public int returnBlockCountThreshold(int level) { return at(returnBlockCountThreshold, level); }
        public double returnDamageFraction(int level) { return at(returnDamageFraction, level); }
        /** Legacy accessor retained only so dead reflection source remains source-compatible. */
        public double returnSpeedScale(int level) { return returnDamageFraction(level); }
        public double strongMindDurationReduction(int level) { return at(strongMindDurationReduction, level); }
        public double shieldBurstDamageMultiplier(int level) { return at(shieldBurstDamageMultiplier, level); }
        public double shieldBurstDamageCap(int level) { return at(shieldBurstDamageCap, level); }
        public double retraceDamageFraction(int level) { return at(retraceDamageFraction, level); }
    }
}
