package dev.moonseungjun.augmentationsurvival.augment.movement;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for shared movement and aerial augmentation mechanics. */
public final class MovementTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/movement_modifier_contract.json";

    private static volatile Snapshot current;

    private MovementTuning() {
    }

    public static void initialize() {
        requireAugment("catch_breath");
        requireAugment("nocturnal");
        requireAugment("sun_affinity");
        requireAugment("webbed_feet");
        requireAugment("step_up");
        requireAugment("long_jump");
        requireAugment("momentum_shift");
        requireAugment("double_jump");
        requireAugment("aerial_combo");
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded movement augment contract: shared conditional movement and aerial runtime"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Movement tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        ClassLoader loader = MovementTuning.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled movement contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read movement augment contract", exception);
        }
    }

    private static Snapshot parse(JsonObject root) {
        int schemaVersion = requireInt(root, "schema_version");
        if (schemaVersion != 1) {
            throw new IllegalStateException("Unsupported movement contract schema: " + schemaVersion);
        }

        JsonObject catchBreath = requireObject(root, "catch_breath");
        JsonObject nocturnal = requireObject(root, "nocturnal");
        JsonObject sunAffinity = requireObject(root, "sun_affinity");
        JsonObject webbedFeet = requireObject(root, "webbed_feet");
        JsonObject stepUp = requireObject(root, "step_up");
        JsonObject longJump = requireObject(root, "long_jump");
        JsonObject momentumShift = requireObject(root, "momentum_shift");
        JsonObject doubleJump = requireObject(root, "double_jump");

        return new Snapshot(
                schemaVersion,
                requirePositive(catchBreath, "movement_per_level"),
                requirePositiveInt(catchBreath, "idle_ticks"),
                requirePositive(nocturnal, "movement_per_level"),
                requirePositive(sunAffinity, "movement_per_level"),
                requirePositive(webbedFeet, "movement_per_level"),
                requirePositive(stepUp, "additional_height"),
                requirePositive(longJump, "horizontal_bonus_per_level"),
                requirePositive(momentumShift, "minimum_fall_distance"),
                requirePositive(momentumShift, "movement_per_level"),
                requirePositiveInt(momentumShift, "duration_ticks"),
                requirePositive(doubleJump, "vertical_multiplier")
        );
    }

    private static void requireAugment(String path) {
        AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
    }

    private static JsonObject requireObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing movement object: " + key);
        }
        return object.getAsJsonObject(key);
    }

    private static int requireInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing movement integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static int requirePositiveInt(JsonObject object, String key) {
        int value = requireInt(object, key);
        if (value <= 0) {
            throw new IllegalStateException("Movement integer must be positive: " + key);
        }
        return value;
    }

    private static double requirePositive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing movement number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Movement number must be finite and positive: " + key);
        }
        return value;
    }

    public record Snapshot(
            int schemaVersion,
            double catchBreathMovementPerLevel,
            int catchBreathIdleTicks,
            double nocturnalMovementPerLevel,
            double sunAffinityMovementPerLevel,
            double webbedFeetMovementPerLevel,
            double stepUpAdditionalHeight,
            double longJumpHorizontalBonusPerLevel,
            double momentumMinimumFallDistance,
            double momentumMovementPerLevel,
            int momentumDurationTicks,
            double doubleJumpVerticalMultiplier
    ) {
    }
}
