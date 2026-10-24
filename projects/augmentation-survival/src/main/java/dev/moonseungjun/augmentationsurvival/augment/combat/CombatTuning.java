package dev.moonseungjun.augmentationsurvival.augment.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Data-driven tuning for the shared combat, protection, healing and lethal runtime. */
public final class CombatTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/combat_modifier_contract.json";

    private static volatile Snapshot current;

    private CombatTuning() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded combat augment contract: shared combat/protection/heal/death baseline"
        );
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
                requireAugment("damage_adaptation");
                requireAugment("delayed_damage");
                requireAugment("overflow_healing");
                requireAugment("healing_share");
                requireAugment("shield");
                requireAugment("emergency_totem");
                requireAugment("reincarnation");

                JsonObject outgoing = root.getAsJsonObject("outgoing");
                JsonObject incoming = root.getAsJsonObject("incoming");
                JsonObject killStreak = root.getAsJsonObject("kill_streak");
                JsonObject urgentSteps = root.getAsJsonObject("urgent_steps");
                JsonObject healing = root.getAsJsonObject("healing");
                JsonObject lifesteal = root.getAsJsonObject("lifesteal");
                JsonObject lastStand = root.getAsJsonObject("last_stand");
                JsonObject adaptation = root.getAsJsonObject("damage_adaptation");
                JsonObject delayed = root.getAsJsonObject("delayed_damage");
                JsonObject overflow = root.getAsJsonObject("overflow_healing");
                JsonObject share = root.getAsJsonObject("healing_share");
                JsonObject shield = root.getAsJsonObject("shield");
                JsonObject reincarnation = root.getAsJsonObject("reincarnation");

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
                        requirePositiveInt(lastStand, "invulnerable_ticks"),
                        fraction(adaptation, "reduction_per_level"),
                        requirePositiveInt(adaptation, "duration_ticks"),
                        fraction(delayed, "level1_fraction"),
                        fraction(delayed, "per_additional_level"),
                        requirePositiveInt(delayed, "installment_count"),
                        requirePositiveInt(delayed, "interval_ticks"),
                        positive(overflow, "absorption_cap_per_level"),
                        fraction(share, "share_per_level"),
                        positive(share, "radius"),
                        positive(shield, "level1_amount"),
                        positive(shield, "per_additional_level"),
                        requirePositiveInt(shield, "duration_ticks"),
                        requirePositiveInt(reincarnation, "cooldown_ticks"),
                        requirePositiveInt(reincarnation, "stasis_ticks")
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
            int lastStandInvulnerableTicks,
            double damageAdaptationReductionPerLevel,
            int damageAdaptationDurationTicks,
            double delayedDamageLevel1Fraction,
            double delayedDamagePerAdditionalLevel,
            int delayedDamageInstallmentCount,
            int delayedDamageIntervalTicks,
            double overflowHealingAbsorptionCapPerLevel,
            double healingSharePerLevel,
            double healingShareRadius,
            double shieldLevel1Amount,
            double shieldPerAdditionalLevel,
            int shieldDurationTicks,
            int reincarnationCooldownTicks,
            int reincarnationStasisTicks
    ) {
        public double delayedDamageFraction(int level) {
            if (level <= 0) {
                return 0.0D;
            }
            return Math.min(
                    1.0D,
                    delayedDamageLevel1Fraction + delayedDamagePerAdditionalLevel * (level - 1)
            );
        }

        public double shieldAmount(int level) {
            if (level <= 0) {
                return 0.0D;
            }
            return shieldLevel1Amount + shieldPerAdditionalLevel * (level - 1);
        }
    }
}
