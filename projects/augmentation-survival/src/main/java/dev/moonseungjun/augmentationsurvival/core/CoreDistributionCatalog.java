package dev.moonseungjun.augmentationsurvival.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.util.RandomSource;

/**
 * Validated runtime view of the canonical core-distribution contract.
 *
 * <p>This first implementation consumes the bundled contract at mod initialization. The catalog
 * API is intentionally independent from the resource source so a later data-pack reload listener
 * can replace the snapshot without changing reward code.</p>
 */
public final class CoreDistributionCatalog {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/core_distribution_contract.json";
    private static volatile Catalog current;

    private CoreDistributionCatalog() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded core distribution contract: {} templates, {} mob profiles, {} event profiles",
                current.distributionTemplates().size(),
                current.mobProfiles().size(),
                current.eventProfiles().size()
        );
    }

    public static Catalog current() {
        Catalog catalog = current;
        if (catalog == null) {
            throw new IllegalStateException("Core distribution catalog is not initialized");
        }
        return catalog;
    }

    public static Optional<AugmentTier> rollMobProfile(String profileId, RandomSource random) {
        MobProfile profile = current().mobProfiles().get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown mob core profile: " + profileId);
        }
        if (random.nextDouble() >= profile.coreRollChance()) {
            return Optional.empty();
        }
        return Optional.of(rollTier(profile.weights(), random));
    }

    public static List<AugmentTier> rollEventProfile(String profileId, RandomSource random) {
        EventProfile profile = current().eventProfiles().get(profileId);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown event core profile: " + profileId);
        }
        List<AugmentTier> result = new ArrayList<>(profile.guaranteedCoreCount());
        for (int index = 0; index < profile.guaranteedCoreCount(); index++) {
            result.add(rollTier(profile.weights(), random));
        }
        return List.copyOf(result);
    }

    private static AugmentTier rollTier(Map<AugmentTier, Double> weights, RandomSource random) {
        double roll = random.nextDouble();
        double cumulative = 0.0;
        AugmentTier last = null;
        for (AugmentTier tier : AugmentTier.values()) {
            Double weight = weights.get(tier);
            if (weight == null || weight <= 0.0) {
                continue;
            }
            last = tier;
            cumulative += weight;
            if (roll < cumulative) {
                return tier;
            }
        }
        if (last == null) {
            throw new IllegalStateException("Cannot roll an empty core tier distribution");
        }
        return last;
    }

    private static Catalog loadBundled() {
        ClassLoader loader = CoreDistributionCatalog.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled core distribution contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                return parse(root);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read core distribution contract", exception);
        }
    }

    private static Catalog parse(JsonObject root) {
        int schemaVersion = requireInt(root, "schema_version");
        if (schemaVersion != 2) {
            throw new IllegalStateException("Unsupported core distribution schema: " + schemaVersion);
        }
        if (!root.get("exact_weights_defined").getAsBoolean()) {
            throw new IllegalStateException("Core distribution contract must define exact weights");
        }

        boolean[] seenTier = new boolean[AugmentTier.values().length];
        for (JsonElement element : root.getAsJsonArray("tiers")) {
            JsonObject tierObject = element.getAsJsonObject();
            AugmentTier tier = AugmentTier.fromId(requireString(tierObject, "id"));
            if (seenTier[tier.ordinal()]) {
                throw new IllegalStateException("Duplicate tier in core contract: " + tier.id());
            }
            seenTier[tier.ordinal()] = true;
        }
        for (AugmentTier tier : AugmentTier.values()) {
            if (!seenTier[tier.ordinal()]) {
                throw new IllegalStateException("Missing tier in core contract: " + tier.id());
            }
        }

        Map<String, DistributionTemplate> templates = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("distribution_templates")) {
            JsonObject object = element.getAsJsonObject();
            DistributionTemplate template = new DistributionTemplate(
                    requireString(object, "id"),
                    requireProbability(object, "core_roll_chance"),
                    requireInt(object, "max_core_rolls"),
                    parseWeights(object.getAsJsonObject("weights"))
            );
            if (template.maxCoreRolls() <= 0) {
                throw new IllegalStateException("max_core_rolls must be positive: " + template.id());
            }
            putUnique(templates, template.id(), template, "distribution template");
        }

        Map<String, MobProfile> mobs = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("mob_profiles")) {
            JsonObject object = element.getAsJsonObject();
            MobProfile profile = new MobProfile(
                    requireString(object, "id"),
                    requireProbability(object, "core_roll_chance"),
                    parseWeights(object.getAsJsonObject("weights")),
                    object.get("require_player_attributed_kill").getAsBoolean()
            );
            putUnique(mobs, profile.id(), profile, "mob profile");
        }

        Map<String, EventProfile> events = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("event_profiles")) {
            JsonObject object = element.getAsJsonObject();
            EventProfile profile = new EventProfile(
                    requireString(object, "id"),
                    requireInt(object, "guaranteed_core_count"),
                    parseWeights(object.getAsJsonObject("weights")),
                    object.get("per_player_reward").getAsBoolean()
            );
            if (profile.guaranteedCoreCount() <= 0) {
                throw new IllegalStateException("guaranteed_core_count must be positive: " + profile.id());
            }
            putUnique(events, profile.id(), profile, "event profile");
        }

        return new Catalog(
                schemaVersion,
                Collections.unmodifiableMap(templates),
                Collections.unmodifiableMap(mobs),
                Collections.unmodifiableMap(events)
        );
    }

    private static Map<AugmentTier, Double> parseWeights(JsonObject object) {
        EnumMap<AugmentTier, Double> weights = new EnumMap<>(AugmentTier.class);
        double sum = 0.0;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            AugmentTier tier = AugmentTier.fromId(entry.getKey());
            double weight = entry.getValue().getAsDouble();
            if (!Double.isFinite(weight) || weight <= 0.0 || weight > 1.0) {
                throw new IllegalStateException("Invalid core tier weight for " + tier.id() + ": " + weight);
            }
            weights.put(tier, weight);
            sum += weight;
        }
        if (Math.abs(sum - 1.0) > 0.000001) {
            throw new IllegalStateException("Core tier weights must sum to 1.0, got " + sum);
        }
        return Collections.unmodifiableMap(weights);
    }

    private static double requireProbability(JsonObject object, String key) {
        double value = object.get(key).getAsDouble();
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalStateException(key + " must be between 0 and 1, got " + value);
        }
        return value;
    }

    private static int requireInt(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new IllegalStateException("Missing required integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static String requireString(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new IllegalStateException("Missing required string: " + key);
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Required string is blank: " + key);
        }
        return value;
    }

    private static <T> void putUnique(Map<String, T> map, String id, T value, String kind) {
        if (map.putIfAbsent(id, value) != null) {
            throw new IllegalStateException("Duplicate " + kind + " id: " + id);
        }
    }

    public record Catalog(
            int schemaVersion,
            Map<String, DistributionTemplate> distributionTemplates,
            Map<String, MobProfile> mobProfiles,
            Map<String, EventProfile> eventProfiles
    ) {
    }

    public record DistributionTemplate(
            String id,
            double coreRollChance,
            int maxCoreRolls,
            Map<AugmentTier, Double> weights
    ) {
    }

    public record MobProfile(
            String id,
            double coreRollChance,
            Map<AugmentTier, Double> weights,
            boolean requirePlayerAttributedKill
    ) {
    }

    public record EventProfile(
            String id,
            int guaranteedCoreCount,
            Map<AugmentTier, Double> weights,
            boolean perPlayerReward
    ) {
    }
}
