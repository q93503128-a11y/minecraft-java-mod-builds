package dev.moonseungjun.augmentationsurvival.threat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/** Validated runtime view of the save-wide hostile scaling contract. */
public final class EnemyScalingCatalog {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/enemy_scaling_contract.json";
    private static volatile Snapshot current;

    private EnemyScalingCatalog() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded enemy scaling contract: {} stages, {} exact profiles, {} namespace profiles",
                current.stages().size(),
                current.exactProfiles().size(),
                current.namespaceProfiles().size()
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Enemy scaling catalog is not initialized");
        }
        return snapshot;
    }

    public static Profile resolveProfile(LivingEntity entity) {
        Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (entityId != null) {
            Profile exact = current().exactProfiles().get(entityId.toString());
            if (exact != null) {
                return exact;
            }
            Profile namespace = current().namespaceProfiles().get(entityId.getNamespace());
            if (namespace != null) {
                return namespace;
            }
        }
        return entity instanceof Enemy ? current().defaultHostileProfile() : current().excludedProfile();
    }

    public static EffectiveScale scaleFor(int stage, Profile profile) {
        StageScale normal = current().stages().get(stage);
        if (normal == null) {
            throw new IllegalArgumentException("Unknown threat stage: " + stage);
        }
        GroupWeights group = current().groupWeights().get(profile.scaleGroup());
        if (group == null || profile.scaleGroup() == ScaleGroup.EXCLUDED) {
            return EffectiveScale.IDENTITY;
        }
        return new EffectiveScale(
                multiplyExtra(normal.health(), group.health() * profile.healthScaleWeight()),
                multiplyExtra(normal.damage(), group.damage() * profile.damageScaleWeight()),
                multiplyExtra(normal.speed(), group.speed() * profile.speedScaleWeight()),
                normal.armorAdd() * group.armor() * profile.armorScaleWeight(),
                normal.knockbackResistanceAdd()
                        * group.knockback()
                        * profile.knockbackScaleWeight()
        );
    }

    private static double multiplyExtra(double multiplier, double weight) {
        return 1.0D + (multiplier - 1.0D) * Math.max(0.0D, weight);
    }

    private static Snapshot loadBundled() {
        try (InputStream stream = EnemyScalingCatalog.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing enemy scaling contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read enemy scaling contract", exception);
        }
    }

    private static Snapshot parse(JsonObject root) {
        int schema = root.get("schema_version").getAsInt();
        if (schema != 2) {
            throw new IllegalStateException("Unsupported enemy scaling schema: " + schema);
        }
        if (!root.get("runtime_active").getAsBoolean()) {
            throw new IllegalStateException("Enemy scaling contract must be runtime-active");
        }

        Map<Integer, StageScale> stages = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("threat_stages")) {
            JsonObject object = element.getAsJsonObject();
            int stage = object.get("stage").getAsInt();
            JsonObject normal = object.getAsJsonObject("normal");
            StageScale scale = new StageScale(
                    normal.get("health").getAsDouble(),
                    normal.get("damage").getAsDouble(),
                    normal.get("speed").getAsDouble(),
                    normal.get("armor_add").getAsDouble(),
                    normal.get("knockback_resistance_add").getAsDouble()
            );
            if (stages.putIfAbsent(stage, scale) != null) {
                throw new IllegalStateException("Duplicate enemy scaling stage: " + stage);
            }
        }
        for (int stage = 0; stage <= ThreatCurve.maxStage(); stage++) {
            if (!stages.containsKey(stage)) {
                throw new IllegalStateException("Missing enemy scaling stage: " + stage);
            }
        }

        JsonObject scaleGroups = root.getAsJsonObject("scale_groups");
        Map<ScaleGroup, GroupWeights> groupWeights = new LinkedHashMap<>();
        double normalWeight = scaleGroups.getAsJsonObject("normal").get("extra_scale_weight").getAsDouble();
        double eliteWeight = scaleGroups.getAsJsonObject("elite").get("extra_scale_weight").getAsDouble();
        JsonObject boss = scaleGroups.getAsJsonObject("boss");
        groupWeights.put(ScaleGroup.NORMAL, GroupWeights.uniform(normalWeight));
        groupWeights.put(ScaleGroup.ELITE, GroupWeights.uniform(eliteWeight));
        groupWeights.put(ScaleGroup.BOSS, new GroupWeights(
                boss.get("health_extra_scale_weight").getAsDouble(),
                boss.get("damage_extra_scale_weight").getAsDouble(),
                boss.get("speed_scaling_enabled").getAsBoolean() ? 1.0D : 0.0D,
                0.0D,
                0.0D
        ));
        groupWeights.put(ScaleGroup.EXCLUDED, GroupWeights.uniform(0.0D));

        Map<String, Profile> exactProfiles = new LinkedHashMap<>();
        Map<String, Profile> namespaceProfiles = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("entity_profiles")) {
            JsonObject object = element.getAsJsonObject();
            String id = requiredString(object, "id");
            JsonObject selector = object.getAsJsonObject("selector");
            String selectorType = requiredString(selector, "type");
            String selectorValue = requiredString(selector, "value");
            Profile profile = new Profile(
                    id,
                    ScaleGroup.fromId(requiredString(object, "scale_group")),
                    optionalWeight(object, "health_scale_weight"),
                    optionalWeight(object, "damage_scale_weight"),
                    optionalWeight(object, "speed_scale_weight"),
                    optionalWeight(object, "armor_scale_weight"),
                    optionalWeight(object, "knockback_scale_weight")
            );
            Map<String, Profile> target;
            if (selectorType.equals("entity")) {
                Identifier parsed = Identifier.tryParse(selectorValue);
                if (parsed == null) {
                    throw new IllegalStateException("Invalid enemy profile entity id: " + selectorValue);
                }
                target = exactProfiles;
                selectorValue = parsed.toString();
            } else if (selectorType.equals("namespace")) {
                if (selectorValue.contains(":")) {
                    throw new IllegalStateException("Namespace selector must not contain ':' : " + selectorValue);
                }
                target = namespaceProfiles;
            } else {
                throw new IllegalStateException("Unsupported enemy profile selector type: " + selectorType);
            }
            if (target.putIfAbsent(selectorValue, profile) != null) {
                throw new IllegalStateException("Duplicate enemy profile selector: " + selectorValue);
            }
        }

        Profile defaultHostile = new Profile(
                "default_hostile",
                ScaleGroup.NORMAL,
                1.0D,
                1.0D,
                1.0D,
                1.0D,
                1.0D
        );
        Profile excluded = new Profile(
                "excluded",
                ScaleGroup.EXCLUDED,
                0.0D,
                0.0D,
                0.0D,
                0.0D,
                0.0D
        );
        return new Snapshot(
                Collections.unmodifiableMap(stages),
                Collections.unmodifiableMap(groupWeights),
                Collections.unmodifiableMap(exactProfiles),
                Collections.unmodifiableMap(namespaceProfiles),
                defaultHostile,
                excluded
        );
    }

    private static double optionalWeight(JsonObject object, String key) {
        double value = object.has(key) ? object.get(key).getAsDouble() : 1.0D;
        if (!Double.isFinite(value) || value < 0.0D) {
            throw new IllegalStateException("Invalid enemy scaling weight " + key + ": " + value);
        }
        return value;
    }

    private static String requiredString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing enemy scaling field: " + key);
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank enemy scaling field: " + key);
        }
        return value;
    }

    public enum ScaleGroup {
        NORMAL("normal"),
        ELITE("elite"),
        BOSS("boss"),
        EXCLUDED("excluded");

        private final String id;

        ScaleGroup(String id) {
            this.id = id;
        }

        public static ScaleGroup fromId(String id) {
            for (ScaleGroup value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            throw new IllegalStateException("Unknown enemy scale group: " + id);
        }
    }

    public record StageScale(
            double health,
            double damage,
            double speed,
            double armorAdd,
            double knockbackResistanceAdd
    ) {
    }

    public record GroupWeights(
            double health,
            double damage,
            double speed,
            double armor,
            double knockback
    ) {
        static GroupWeights uniform(double value) {
            return new GroupWeights(value, value, value, value, value);
        }
    }

    public record Profile(
            String id,
            ScaleGroup scaleGroup,
            double healthScaleWeight,
            double damageScaleWeight,
            double speedScaleWeight,
            double armorScaleWeight,
            double knockbackScaleWeight
    ) {
    }

    public record EffectiveScale(
            double healthMultiplier,
            double damageMultiplier,
            double speedMultiplier,
            double armorAdd,
            double knockbackResistanceAdd
    ) {
        static final EffectiveScale IDENTITY = new EffectiveScale(1.0D, 1.0D, 1.0D, 0.0D, 0.0D);
    }

    public record Snapshot(
            Map<Integer, StageScale> stages,
            Map<ScaleGroup, GroupWeights> groupWeights,
            Map<String, Profile> exactProfiles,
            Map<String, Profile> namespaceProfiles,
            Profile defaultHostileProfile,
            Profile excludedProfile
    ) {
    }
}
