package dev.moonseungjun.augmentationsurvival.augment.active;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven values for standalone return-home and mythic active abilities. */
public final class SpecialActiveTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/special_active_contract.json";

    private static volatile Snapshot current;

    private SpecialActiveTuning() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded special active contract: return-home, time-stop and divine-punishment"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Special active tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = SpecialActiveTuning.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing special active contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = requirePositiveInt(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported special active contract schema: " + schema);
                }
                requireAugment("return_home");
                requireAugment("time_stop");
                requireAugment("divine_punishment");

                JsonObject returnHome = root.getAsJsonObject("return_home");
                JsonObject timeStop = root.getAsJsonObject("time_stop");
                JsonObject punishment = root.getAsJsonObject("divine_punishment");
                return new Snapshot(
                        schema,
                        requirePositiveInt(returnHome, "focus_ticks"),
                        positive(timeStop, "radius"),
                        requirePositiveInt(timeStop, "duration_ticks"),
                        requirePositiveInt(timeStop, "cooldown_ticks"),
                        positive(punishment, "radius"),
                        fraction(punishment, "normal_fraction_level_1"),
                        fraction(punishment, "normal_fraction_level_2"),
                        fraction(punishment, "boss_fraction_level_1"),
                        fraction(punishment, "boss_fraction_level_2"),
                        requirePositiveInt(punishment, "stun_ticks_level_2"),
                        requirePositiveInt(punishment, "cooldown_ticks")
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read special active contract", exception);
        }
    }

    private static void requireAugment(String path) {
        AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
    }

    private static int requirePositiveInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing special active integer: " + key);
        }
        int value = object.get(key).getAsInt();
        if (value <= 0) {
            throw new IllegalStateException("Special active integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing special active number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Special active number must be finite and positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Special active fraction must be <= 1: " + key);
        }
        return value;
    }

    public record Snapshot(
            int schemaVersion,
            int returnHomeFocusTicks,
            double timeStopRadius,
            int timeStopDurationTicks,
            int timeStopCooldownTicks,
            double divinePunishmentRadius,
            double divinePunishmentNormalFractionLevel1,
            double divinePunishmentNormalFractionLevel2,
            double divinePunishmentBossFractionLevel1,
            double divinePunishmentBossFractionLevel2,
            int divinePunishmentStunTicksLevel2,
            int divinePunishmentCooldownTicks
    ) {
        public double divinePunishmentFraction(int level, boolean boss) {
            if (boss) {
                return level >= 2 ? divinePunishmentBossFractionLevel2 : divinePunishmentBossFractionLevel1;
            }
            return level >= 2 ? divinePunishmentNormalFractionLevel2 : divinePunishmentNormalFractionLevel1;
        }
    }
}
