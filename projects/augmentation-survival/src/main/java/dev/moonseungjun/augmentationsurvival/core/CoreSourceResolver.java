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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.resources.Identifier;

/**
 * Runtime selector resolver for the canonical core-source profile contract.
 * Balance weights stay in {@link CoreDistributionCatalog}; this class resolves only source identity.
 */
public final class CoreSourceResolver {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/core_source_profiles.json";
    private static volatile Snapshot current;

    private CoreSourceResolver() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded core source profiles: {} exact hostiles, {} hostile namespace defaults, {} structure path rules",
                current.exactHostiles().size(),
                current.hostileNamespaceDefaults().size(),
                current.structurePathRules().size()
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Core source resolver is not initialized");
        }
        return snapshot;
    }

    public static HostileResolution resolveHostile(Identifier entityId, boolean serverClassifiedHostile) {
        Snapshot snapshot = current();
        String id = entityId.toString();

        String eventProfile = snapshot.eventRedirects().get(id);
        if (eventProfile != null) {
            return HostileResolution.event(eventProfile);
        }
        if (snapshot.bossRedirects().contains(id)) {
            return HostileResolution.suppressedBoss();
        }

        String exact = snapshot.exactHostiles().get(id);
        if (exact != null) {
            return HostileResolution.mob(exact);
        }

        String namespace = entityId.getNamespace();
        if (snapshot.hostileNamespaceExclusions().contains(namespace)) {
            return HostileResolution.none();
        }
        if (serverClassifiedHostile) {
            String namespaceProfile = snapshot.hostileNamespaceDefaults().get(namespace);
            if (namespaceProfile != null) {
                return HostileResolution.mob(namespaceProfile);
            }
            return HostileResolution.mob(snapshot.globalHostileFallbackProfile());
        }
        return HostileResolution.none();
    }

    public static StructureResolution resolveStructure(Identifier structureId) {
        Snapshot snapshot = current();
        String id = structureId.toString();
        StructureResolution exact = snapshot.exactStructures().get(id);
        if (exact != null) {
            return exact;
        }

        String namespace = structureId.getNamespace();
        if (snapshot.structureNamespaceExclusions().contains(namespace)) {
            return StructureResolution.excluded();
        }
        String path = structureId.getPath();
        for (StructurePathRule rule : snapshot.structurePathRules()) {
            if (!rule.namespace().equals(namespace)) {
                continue;
            }
            for (String prefix : rule.prefixes()) {
                if (path.startsWith(prefix)) {
                    return rule.excluded()
                            ? StructureResolution.excluded()
                            : new StructureResolution(rule.distributionProfile(), rule.coreRollMultiplier(), false);
                }
            }
        }

        StructureResolution namespaceDefault = snapshot.structureNamespaceDefaults().get(namespace);
        return namespaceDefault != null ? namespaceDefault : StructureResolution.none();
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = CoreSourceResolver.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled core source contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read core source contract", exception);
        }
    }

    private static Snapshot parse(JsonObject root) {
        int schema = root.get("schema_version").getAsInt();
        if (schema != 1) {
            throw new IllegalStateException("Unsupported core source schema: " + schema);
        }

        JsonObject structures = root.getAsJsonObject("structure_sources");
        Map<String, StructureResolution> exactStructures = new HashMap<>();
        for (JsonElement element : structures.getAsJsonArray("exact")) {
            JsonObject object = element.getAsJsonObject();
            String selector = requiredString(object, "selector");
            StructureResolution resolution = object.has("excluded") && object.get("excluded").getAsBoolean()
                    ? StructureResolution.excluded()
                    : structureResolution(object);
            putUnique(exactStructures, selector, resolution, "exact structure selector");
        }

        List<StructurePathRule> structurePathRules = new ArrayList<>();
        for (JsonElement element : structures.getAsJsonArray("path_prefix")) {
            JsonObject object = element.getAsJsonObject();
            List<String> prefixes = new ArrayList<>();
            for (JsonElement prefix : object.getAsJsonArray("prefixes")) {
                prefixes.add(prefix.getAsString());
            }
            boolean excluded = object.has("excluded") && object.get("excluded").getAsBoolean();
            String profile = excluded ? null : requiredDistributionProfile(object);
            double multiplier = excluded ? 0.0 : requiredPositiveMultiplier(object);
            structurePathRules.add(new StructurePathRule(
                    requiredString(object, "namespace"),
                    List.copyOf(prefixes),
                    profile,
                    multiplier,
                    excluded
            ));
        }

        Map<String, StructureResolution> structureNamespaceDefaults = new HashMap<>();
        for (JsonElement element : structures.getAsJsonArray("namespace_default")) {
            JsonObject object = element.getAsJsonObject();
            putUnique(
                    structureNamespaceDefaults,
                    requiredString(object, "namespace"),
                    structureResolution(object),
                    "structure namespace default"
            );
        }
        Set<String> structureNamespaceExclusions = namespaceSet(structures.getAsJsonArray("namespace_exclusions"));

        JsonObject hostiles = root.getAsJsonObject("hostile_sources");
        Map<String, String> exactHostiles = new HashMap<>();
        for (JsonElement element : hostiles.getAsJsonArray("exact")) {
            JsonObject object = element.getAsJsonObject();
            String profile = requiredMobProfile(object);
            putUnique(exactHostiles, requiredString(object, "selector"), profile, "exact hostile selector");
        }

        Map<String, String> eventRedirects = new HashMap<>();
        for (JsonElement element : hostiles.getAsJsonArray("event_redirects")) {
            JsonObject object = element.getAsJsonObject();
            String profile = requiredString(object, "event_profile");
            requireEventProfile(profile);
            putUnique(eventRedirects, requiredString(object, "selector"), profile, "event redirect");
        }

        Set<String> bossRedirects = new HashSet<>();
        for (JsonElement element : hostiles.getAsJsonArray("boss_redirects")) {
            String selector = requiredString(element.getAsJsonObject(), "selector");
            if (!bossRedirects.add(selector)) {
                throw new IllegalStateException("Duplicate boss redirect: " + selector);
            }
        }

        Map<String, String> hostileNamespaceDefaults = new HashMap<>();
        for (JsonElement element : hostiles.getAsJsonArray("namespace_default")) {
            JsonObject object = element.getAsJsonObject();
            putUnique(
                    hostileNamespaceDefaults,
                    requiredString(object, "namespace"),
                    requiredMobProfile(object),
                    "hostile namespace default"
            );
        }
        Set<String> hostileNamespaceExclusions = namespaceSet(hostiles.getAsJsonArray("namespace_exclusions"));
        String fallbackProfile = requiredString(hostiles.getAsJsonObject("global_fallback"), "mob_profile");
        requireMobProfile(fallbackProfile);

        return new Snapshot(
                Map.copyOf(exactStructures),
                List.copyOf(structurePathRules),
                Map.copyOf(structureNamespaceDefaults),
                Set.copyOf(structureNamespaceExclusions),
                Map.copyOf(exactHostiles),
                Map.copyOf(eventRedirects),
                Set.copyOf(bossRedirects),
                Map.copyOf(hostileNamespaceDefaults),
                Set.copyOf(hostileNamespaceExclusions),
                fallbackProfile
        );
    }

    private static StructureResolution structureResolution(JsonObject object) {
        return new StructureResolution(
                requiredDistributionProfile(object),
                requiredPositiveMultiplier(object),
                false
        );
    }

    private static String requiredDistributionProfile(JsonObject object) {
        String profile = requiredString(object, "distribution_profile");
        if (!CoreDistributionCatalog.current().distributionTemplates().containsKey(profile)) {
            throw new IllegalStateException("Unknown distribution profile referenced by source contract: " + profile);
        }
        return profile;
    }

    private static String requiredMobProfile(JsonObject object) {
        String profile = requiredString(object, "mob_profile");
        requireMobProfile(profile);
        return profile;
    }

    private static void requireMobProfile(String profile) {
        if (!CoreDistributionCatalog.current().mobProfiles().containsKey(profile)) {
            throw new IllegalStateException("Unknown mob profile referenced by source contract: " + profile);
        }
    }

    private static void requireEventProfile(String profile) {
        if (!CoreDistributionCatalog.current().eventProfiles().containsKey(profile)) {
            throw new IllegalStateException("Unknown event profile referenced by source contract: " + profile);
        }
    }

    private static double requiredPositiveMultiplier(JsonObject object) {
        double value = object.get("core_roll_multiplier").getAsDouble();
        if (!Double.isFinite(value) || value <= 0.0) {
            throw new IllegalStateException("core_roll_multiplier must be finite and positive: " + value);
        }
        return value;
    }

    private static Set<String> namespaceSet(Iterable<JsonElement> elements) {
        Set<String> result = new HashSet<>();
        for (JsonElement element : elements) {
            String namespace = requiredString(element.getAsJsonObject(), "namespace");
            if (!result.add(namespace)) {
                throw new IllegalStateException("Duplicate namespace exclusion: " + namespace);
            }
        }
        return result;
    }

    private static String requiredString(JsonObject object, String key) {
        if (!object.has(key)) {
            throw new IllegalStateException("Missing required source-profile field: " + key);
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank source-profile field: " + key);
        }
        return value;
    }

    private static <T> void putUnique(Map<String, T> target, String key, T value, String kind) {
        if (target.putIfAbsent(key, value) != null) {
            throw new IllegalStateException("Duplicate " + kind + ": " + key);
        }
    }

    public record Snapshot(
            Map<String, StructureResolution> exactStructures,
            List<StructurePathRule> structurePathRules,
            Map<String, StructureResolution> structureNamespaceDefaults,
            Set<String> structureNamespaceExclusions,
            Map<String, String> exactHostiles,
            Map<String, String> eventRedirects,
            Set<String> bossRedirects,
            Map<String, String> hostileNamespaceDefaults,
            Set<String> hostileNamespaceExclusions,
            String globalHostileFallbackProfile
    ) {
    }

    public record StructurePathRule(
            String namespace,
            List<String> prefixes,
            String distributionProfile,
            double coreRollMultiplier,
            boolean excluded
    ) {
    }

    public record StructureResolution(
            String distributionProfile,
            double coreRollMultiplier,
            boolean excludedSource
    ) {
        public static StructureResolution none() {
            return new StructureResolution(null, 0.0, false);
        }

        public static StructureResolution excluded() {
            return new StructureResolution(null, 0.0, true);
        }

        public boolean hasDistribution() {
            return distributionProfile != null;
        }
    }

    public record HostileResolution(
            Kind kind,
            String profileId
    ) {
        public enum Kind {
            NONE,
            MOB,
            EVENT,
            BOSS_SUPPRESSED
        }

        public static HostileResolution none() {
            return new HostileResolution(Kind.NONE, null);
        }

        public static HostileResolution mob(String profileId) {
            return new HostileResolution(Kind.MOB, profileId);
        }

        public static HostileResolution event(String profileId) {
            return new HostileResolution(Kind.EVENT, profileId);
        }

        public static HostileResolution suppressedBoss() {
            return new HostileResolution(Kind.BOSS_SUPPRESSED, null);
        }
    }
}
