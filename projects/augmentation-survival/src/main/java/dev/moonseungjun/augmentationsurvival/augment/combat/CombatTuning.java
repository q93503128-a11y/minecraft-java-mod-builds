package dev.moonseungjun.augmentationsurvival.augment.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for the first shared combat runtime slice. */
public final class CombatTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/combat_modifier_contract.json";

    private static volatile Snapshot current;

    private CombatTuning() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info("Loaded combat augment contract: shared combat/heal/death baseline");
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Combat tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        ClassLoader loader = CombatTuning.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled combat augment contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schemaVersion = requireInt(root, "schema_version");
                if (schemaVersion != 1) {
                    throw new IllegalStateException("Unsupported combat augment contract schema: " + schemaVersion);
                }

                requireAugment("keen_edge");
                requireAugment("precise_aim");
                requireAugment("tough_skin");
                requireAugment("light_landing");
                requireAugment("strong_lungs");
                requireAugment("last_strength");
                requireAugment("kill_streak");
                requireAugment("urgent_steps");
                requireAugment("lifesteal");
                requireAugment("healing_amplification");
                requireAugment("last_stand");

                JsonObject outgoing = root.getAsJsonObject("outgoing");
                JsonObject incoming = root.getAsJsonObject("incoming");
                JsonObject killStreak = root.getAsJsonObject("kill_streak");
                JsonObject urgentSteps = root.getAsJsonObject("urgent_steps");
                JsonObject healing = root.getAsJsonObject("healing");
                JsonObject lifesteal = root.getAsJsonObject("lifesteal");
                JsonObject lastStand = root.getAsJsonObject("last_stand");

                return new Snapshot(
                        schemaVersion,
                        positive(outgoing, "keen_edge_per_level"),
                        positive(outgoing, "precise_aim_per_level"),
                        positive(outgoing, "last_strength_per_level"),
                        fraction(outgoing, "last_strength_health_threshold"),
                        positive(incoming, "tough_skin_per_level"),
                        positive(incoming, "light_landing_per_level"),
                        positive(incoming, "strong_lungs_per_level"),
                        positive(killStreak, "movement_per_level_per_stack"),
                        positive(killStreak, "attack_speed_per_level_per_stack"),
                        requirePositiveInt(killStreak, "max_stacks"),
                        requirePositiveInt(killStreak, "duration_ticks"),
                        positive(urgentSteps, "movement_per_level"),
                        requirePositiveInt(urgentSteps, "duration_ticks"),
                        positive(healing, "amplification_per_level"),
                        positive(lifesteal, "heal_per_level"),
                        requirePositiveInt(lastStand, "cooldown_ticks"),
                        requirePositiveInt(lastStand, "invulnerable_ticks")
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read combat augment contract", exception);
        }
    }

    private static void requireAugment(String path) {
        AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
    }

    private static int requireInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing combat contract integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static int requirePositiveInt(JsonObject object, String key) {
        int value = requireInt(object, key);
        if (value <= 0) {
            throw new IllegalStateException("Combat contract value must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing combat contract number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Combat contract value must be finite and positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Combat contract fraction must be <= 1: " + key);
        }
        return value;
    }

    public record Snapshot(
            int schemaVersion,
            double keenEdgePerLevel,
            double preciseAimPerLevel,
            double lastStrengthPerLevel,
            double lastStrengthHealthThreshold,
            double toughSkinPerLevel,
            double lightLandingPerLevel,
            double strongLungsPerLevel,
            double killStreakMovementPerLevelPerStack,
            double killStreakAttackSpeedPerLevelPerStack,
            int killStreakMaxStacks,
            int killStreakDurationTicks,
            double urgentStepsMovementPerLevel,
            int urgentStepsDurationTicks,
            double healingAmplificationPerLevel,
            double lifestealHealPerLevel,
            int lastStandCooldownTicks,
            int lastStandInvulnerableTicks
    ) {
    }
}
