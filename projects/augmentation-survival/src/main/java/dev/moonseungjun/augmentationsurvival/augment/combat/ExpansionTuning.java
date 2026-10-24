package dev.moonseungjun.augmentationsurvival.augment.combat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/** Data-driven tuning for the broad active/combat expansion batch. */
public final class ExpansionTuning {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/expansion_contract.json";

    private static volatile Snapshot current;

    private ExpansionTuning() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded expansion augment contract: active engine and broad combat batch"
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Expansion tuning is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        ClassLoader loader = ExpansionTuning.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled expansion contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                int schema = requireInt(root, "schema_version");
                if (schema != 1) {
                    throw new IllegalStateException("Unsupported expansion contract schema: " + schema);
                }

                for (String path : new String[]{
                        "cleanse", "pull", "swap", "shield", "berserk", "healing_wave",
                        "ability_haste", "double_charge", "replay", "combat_cycle",
                        "chain_explosion", "lightning", "echo_strike", "weakness_mark",
                        "speed_conversion", "heavy_strike", "life_wave", "brawl",
                        "glass_cannon", "boss_hunter"
                }) {
                    AugmentCatalog.require(AugmentationSurvival.MOD_ID + ":" + path);
                }

                JsonObject active = object(root, "active");
                JsonObject combat = object(root, "combat");
                JsonObject cleanse = object(active, "cleanse");
                JsonObject pull = object(active, "pull");
                JsonObject swap = object(active, "swap");
                JsonObject shield = object(active, "shield");
                JsonObject berserk = object(active, "berserk");
                JsonObject healingWave = object(active, "healing_wave");
                JsonObject abilityHaste = object(active, "ability_haste");
                JsonObject replay = object(active, "replay");

                JsonObject chainExplosion = object(combat, "chain_explosion");
                JsonObject lightning = object(combat, "lightning");
                JsonObject echoStrike = object(combat, "echo_strike");
                JsonObject weaknessMark = object(combat, "weakness_mark");
                JsonObject speedConversion = object(combat, "speed_conversion");
                JsonObject heavyStrike = object(combat, "heavy_strike");
                JsonObject lifeWave = object(combat, "life_wave");
                JsonObject brawl = object(combat, "brawl");
                JsonObject glassCannon = object(combat, "glass_cannon");
                JsonObject bossHunter = object(combat, "boss_hunter");

                return new Snapshot(
                        schema,
                        requirePositiveInt(cleanse, "cooldown_ticks"),
                        positiveArray(pull, "radius", 3),
                        positiveIntArray(pull, "cooldown_ticks", 3),
                        positive(pull, "pull_speed"),
                        fraction(pull, "boss_scalar"),
                        positive(swap, "range"),
                        requirePositiveInt(swap, "cooldown_ticks"),
                        positiveIntArray(shield, "cooldown_ticks", 3),
                        requirePositiveInt(berserk, "duration_ticks"),
                        fractionArray(berserk, "damage_bonus", 3),
                        fractionArray(berserk, "attack_speed_bonus", 3),
                        fraction(berserk, "incoming_damage_bonus"),
                        positiveIntArray(berserk, "cooldown_ticks", 3),
                        positive(healingWave, "radius"),
                        fractionArray(healingWave, "heal_fraction", 3),
                        positiveIntArray(healingWave, "cooldown_ticks", 3),
                        fraction(abilityHaste, "cooldown_reduction"),
                        requirePositiveInt(replay, "delay_ticks"),
                        fraction(replay, "efficiency"),
                        positiveArray(chainExplosion, "radius", 3),
                        fractionArray(chainExplosion, "max_health_fraction", 3),
                        positiveArray(chainExplosion, "damage_cap", 3),
                        positiveIntArray(lightning, "trigger_hits", 3),
                        positive(lightning, "radius"),
                        positiveIntArray(lightning, "target_count", 3),
                        fractionArray(lightning, "damage_fraction", 3),
                        positiveIntArray(echoStrike, "trigger_hits", 3),
                        fractionArray(echoStrike, "damage_fraction", 3),
                        requirePositiveInt(weaknessMark, "required_hits"),
                        requirePositiveInt(weaknessMark, "window_ticks"),
                        requirePositiveInt(weaknessMark, "duration_ticks"),
                        fractionArray(weaknessMark, "damage_bonus", 3),
                        positiveArray(speedConversion, "damage_per_movement_percent", 3),
                        fractionArray(speedConversion, "damage_cap", 3),
                        fraction(speedConversion, "sprint_bonus_exclusion"),
                        fractionArray(heavyStrike, "max_health_fraction", 3),
                        requirePositiveInt(heavyStrike, "cooldown_ticks"),
                        positive(lifeWave, "radius"),
                        positiveArray(lifeWave, "damage_multiplier", 3),
                        positiveArray(lifeWave, "damage_cap", 3),
                        positive(brawl, "radius"),
                        requirePositiveInt(brawl, "max_targets"),
                        fractionArray(brawl, "damage_per_enemy", 3),
                        fractionArray(brawl, "reduction_per_enemy", 3),
                        fractionArray(glassCannon, "health_multiplier", 3),
                        fractionArray(glassCannon, "damage_bonus", 3),
                        fractionArray(bossHunter, "damage_bonus", 3)
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read expansion augment contract", exception);
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        if (parent == null || !parent.has(key) || !parent.get(key).isJsonObject()) {
            throw new IllegalStateException("Missing expansion contract object: " + key);
        }
        return parent.getAsJsonObject(key);
    }

    private static int requireInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing expansion contract integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static int requirePositiveInt(JsonObject object, String key) {
        int value = requireInt(object, key);
        if (value <= 0) {
            throw new IllegalStateException("Expansion integer must be positive: " + key);
        }
        return value;
    }

    private static double positive(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing expansion contract number: " + key);
        }
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0D) {
            throw new IllegalStateException("Expansion number must be finite and positive: " + key);
        }
        return value;
    }

    private static double fraction(JsonObject object, String key) {
        double value = positive(object, key);
        if (value > 1.0D) {
            throw new IllegalStateException("Expansion fraction must be <= 1: " + key);
        }
        return value;
    }

    private static double[] positiveArray(JsonObject object, String key, int expected) {
        JsonArray array = array(object, key, expected);
        double[] values = new double[expected];
        for (int i = 0; i < expected; i++) {
            double value = array.get(i).getAsDouble();
            if (!Double.isFinite(value) || value <= 0.0D) {
                throw new IllegalStateException("Expansion array value must be positive: " + key);
            }
            values[i] = value;
        }
        return values;
    }

    private static double[] fractionArray(JsonObject object, String key, int expected) {
        double[] values = positiveArray(object, key, expected);
        for (double value : values) {
            if (value > 1.0D) {
                throw new IllegalStateException("Expansion array fraction must be <= 1: " + key);
            }
        }
        return values;
    }

    private static int[] positiveIntArray(JsonObject object, String key, int expected) {
        JsonArray array = array(object, key, expected);
        int[] values = new int[expected];
        for (int i = 0; i < expected; i++) {
            int value = array.get(i).getAsInt();
            if (value <= 0) {
                throw new IllegalStateException("Expansion integer array value must be positive: " + key);
            }
            values[i] = value;
        }
        return values;
    }

    private static JsonArray array(JsonObject object, String key, int expected) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            throw new IllegalStateException("Missing expansion contract array: " + key);
        }
        JsonArray array = object.getAsJsonArray(key);
        if (array.size() != expected) {
            throw new IllegalStateException(
                    "Expansion array has wrong length for " + key + ": " + array.size()
            );
        }
        for (JsonElement element : array) {
            if (!element.isJsonPrimitive()) {
                throw new IllegalStateException("Expansion array contains non-primitive: " + key);
            }
        }
        return array;
    }

    private static double at(double[] values, int level) {
        if (level <= 0) {
            return 0.0D;
        }
        return values[Math.min(level, values.length) - 1];
    }

    private static int at(int[] values, int level) {
        if (level <= 0) {
            return 0;
        }
        return values[Math.min(level, values.length) - 1];
    }

    public record Snapshot(
            int schemaVersion,
            int cleanseCooldownTicks,
            double[] pullRadius,
            int[] pullCooldownTicks,
            double pullSpeed,
            double pullBossScalar,
            double swapRange,
            int swapCooldownTicks,
            int[] shieldCooldownTicks,
            int berserkDurationTicks,
            double[] berserkDamageBonus,
            double[] berserkAttackSpeedBonus,
            double berserkIncomingDamageBonus,
            int[] berserkCooldownTicks,
            double healingWaveRadius,
            double[] healingWaveFraction,
            int[] healingWaveCooldownTicks,
            double abilityHasteReduction,
            int replayDelayTicks,
            double replayEfficiency,
            double[] chainExplosionRadius,
            double[] chainExplosionMaxHealthFraction,
            double[] chainExplosionDamageCap,
            int[] lightningTriggerHits,
            double lightningRadius,
            int[] lightningTargetCount,
            double[] lightningDamageFraction,
            int[] echoStrikeTriggerHits,
            double[] echoStrikeDamageFraction,
            int weaknessMarkRequiredHits,
            int weaknessMarkWindowTicks,
            int weaknessMarkDurationTicks,
            double[] weaknessMarkDamageBonus,
            double[] speedConversionDamagePerMovementPercent,
            double[] speedConversionDamageCap,
            double sprintBonusExclusion,
            double[] heavyStrikeMaxHealthFraction,
            int heavyStrikeCooldownTicks,
            double lifeWaveRadius,
            double[] lifeWaveDamageMultiplier,
            double[] lifeWaveDamageCap,
            double brawlRadius,
            int brawlMaxTargets,
            double[] brawlDamagePerEnemy,
            double[] brawlReductionPerEnemy,
            double[] glassCannonHealthMultiplier,
            double[] glassCannonDamageBonus,
            double[] bossHunterDamageBonus
    ) {
        public Snapshot {
            pullRadius = pullRadius.clone();
            pullCooldownTicks = pullCooldownTicks.clone();
            shieldCooldownTicks = shieldCooldownTicks.clone();
            berserkDamageBonus = berserkDamageBonus.clone();
            berserkAttackSpeedBonus = berserkAttackSpeedBonus.clone();
            berserkCooldownTicks = berserkCooldownTicks.clone();
            healingWaveFraction = healingWaveFraction.clone();
            healingWaveCooldownTicks = healingWaveCooldownTicks.clone();
            chainExplosionRadius = chainExplosionRadius.clone();
            chainExplosionMaxHealthFraction = chainExplosionMaxHealthFraction.clone();
            chainExplosionDamageCap = chainExplosionDamageCap.clone();
            lightningTriggerHits = lightningTriggerHits.clone();
            lightningTargetCount = lightningTargetCount.clone();
            lightningDamageFraction = lightningDamageFraction.clone();
            echoStrikeTriggerHits = echoStrikeTriggerHits.clone();
            echoStrikeDamageFraction = echoStrikeDamageFraction.clone();
            weaknessMarkDamageBonus = weaknessMarkDamageBonus.clone();
            speedConversionDamagePerMovementPercent = speedConversionDamagePerMovementPercent.clone();
            speedConversionDamageCap = speedConversionDamageCap.clone();
            heavyStrikeMaxHealthFraction = heavyStrikeMaxHealthFraction.clone();
            lifeWaveDamageMultiplier = lifeWaveDamageMultiplier.clone();
            lifeWaveDamageCap = lifeWaveDamageCap.clone();
            brawlDamagePerEnemy = brawlDamagePerEnemy.clone();
            brawlReductionPerEnemy = brawlReductionPerEnemy.clone();
            glassCannonHealthMultiplier = glassCannonHealthMultiplier.clone();
            glassCannonDamageBonus = glassCannonDamageBonus.clone();
            bossHunterDamageBonus = bossHunterDamageBonus.clone();
        }

        public double pullRadius(int level) { return at(pullRadius, level); }
        public int pullCooldownTicks(int level) { return at(pullCooldownTicks, level); }
        public int shieldCooldownTicks(int level) { return at(shieldCooldownTicks, level); }
        public double berserkDamageBonus(int level) { return at(berserkDamageBonus, level); }
        public double berserkAttackSpeedBonus(int level) { return at(berserkAttackSpeedBonus, level); }
        public int berserkCooldownTicks(int level) { return at(berserkCooldownTicks, level); }
        public double healingWaveFraction(int level) { return at(healingWaveFraction, level); }
        public int healingWaveCooldownTicks(int level) { return at(healingWaveCooldownTicks, level); }
        public double chainExplosionRadius(int level) { return at(chainExplosionRadius, level); }
        public double chainExplosionMaxHealthFraction(int level) { return at(chainExplosionMaxHealthFraction, level); }
        public double chainExplosionDamageCap(int level) { return at(chainExplosionDamageCap, level); }
        public int lightningTriggerHits(int level) { return at(lightningTriggerHits, level); }
        public int lightningTargetCount(int level) { return at(lightningTargetCount, level); }
        public double lightningDamageFraction(int level) { return at(lightningDamageFraction, level); }
        public int echoStrikeTriggerHits(int level) { return at(echoStrikeTriggerHits, level); }
        public double echoStrikeDamageFraction(int level) { return at(echoStrikeDamageFraction, level); }
        public double weaknessMarkDamageBonus(int level) { return at(weaknessMarkDamageBonus, level); }
        public double speedConversionDamagePerMovementPercent(int level) { return at(speedConversionDamagePerMovementPercent, level); }
        public double speedConversionDamageCap(int level) { return at(speedConversionDamageCap, level); }
        public double heavyStrikeMaxHealthFraction(int level) { return at(heavyStrikeMaxHealthFraction, level); }
        public double lifeWaveDamageMultiplier(int level) { return at(lifeWaveDamageMultiplier, level); }
        public double lifeWaveDamageCap(int level) { return at(lifeWaveDamageCap, level); }
        public double brawlDamagePerEnemy(int level) { return at(brawlDamagePerEnemy, level); }
        public double brawlReductionPerEnemy(int level) { return at(brawlReductionPerEnemy, level); }
        public double glassCannonHealthMultiplier(int level) { return at(glassCannonHealthMultiplier, level); }
        public double glassCannonDamageBonus(int level) { return at(glassCannonDamageBonus, level); }
        public double bossHunterDamageBonus(int level) { return at(bossHunterDamageBonus, level); }
    }
}
