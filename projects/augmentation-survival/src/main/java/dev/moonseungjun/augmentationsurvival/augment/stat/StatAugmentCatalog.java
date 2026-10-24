package dev.moonseungjun.augmentationsurvival.augment.stat;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Data-driven definitions for unconditional player attribute augments.
 *
 * <p>Definitions sharing the same attribute and operation are grouped so positive percentage
 * augments are summed before one modifier is applied, matching the canonical stacking contract.</p>
 */
public final class StatAugmentCatalog {
    private static final String RESOURCE_PATH =
            "data/augmentation_survival/augmentation/stat_modifier_contract.json";

    private static volatile Snapshot current;

    private StatAugmentCatalog() {
    }

    public static void initialize() {
        current = loadBundled();
        AugmentationSurvival.LOGGER.info(
                "Loaded stat augment contract: {} definitions in {} modifier groups",
                current.definitions().size(),
                current.groups().size()
        );
    }

    public static Snapshot current() {
        Snapshot snapshot = current;
        if (snapshot == null) {
            throw new IllegalStateException("Stat augment catalog is not initialized");
        }
        return snapshot;
    }

    private static Snapshot loadBundled() {
        ClassLoader loader = StatAugmentCatalog.class.getClassLoader();
        try (InputStream stream = loader.getResourceAsStream(RESOURCE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled stat augment contract: " + RESOURCE_PATH);
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(reader).getAsJsonObject());
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read stat augment contract", exception);
        }
    }

    private static Snapshot parse(JsonObject root) {
        int schemaVersion = requireInt(root, "schema_version");
        if (schemaVersion != 1) {
            throw new IllegalStateException("Unsupported stat augment contract schema: " + schemaVersion);
        }

        List<StatModifierDefinition> definitions = new ArrayList<>();
        Map<GroupKey, List<StatModifierDefinition>> grouped = new LinkedHashMap<>();
        for (JsonElement element : root.getAsJsonArray("modifiers")) {
            JsonObject object = element.getAsJsonObject();
            String augmentPath = requireString(object, "augment_id");
            String augmentId = AugmentationSurvival.MOD_ID + ":" + augmentPath;
            AugmentCatalog.require(augmentId);

            AttributeTarget attribute = AttributeTarget.fromId(requireString(object, "attribute"));
            double amountPerLevel = requireDouble(object, "amount_per_level");
            if (!Double.isFinite(amountPerLevel) || amountPerLevel <= 0.0D) {
                throw new IllegalStateException(
                        "Stat augment amount_per_level must be finite and positive: " + augmentId
                );
            }
            Operation operation = Operation.fromId(requireString(object, "operation"));
            StatModifierDefinition definition = new StatModifierDefinition(
                    augmentId,
                    attribute,
                    amountPerLevel,
                    operation.minecraftOperation()
            );
            definitions.add(definition);
            grouped.computeIfAbsent(
                    new GroupKey(attribute, operation),
                    ignored -> new ArrayList<>()
            ).add(definition);
        }

        List<ModifierGroup> groups = new ArrayList<>();
        for (Map.Entry<GroupKey, List<StatModifierDefinition>> entry : grouped.entrySet()) {
            GroupKey key = entry.getKey();
            Identifier modifierId = Identifier.fromNamespaceAndPath(
                    AugmentationSurvival.MOD_ID,
                    "augment_stat/" + key.attribute().id() + "/" + key.operation().id()
            );
            groups.add(new ModifierGroup(
                    key.attribute(),
                    key.operation().minecraftOperation(),
                    modifierId,
                    List.copyOf(entry.getValue())
            ));
        }

        if (definitions.isEmpty()) {
            throw new IllegalStateException("Stat augment contract cannot be empty");
        }
        return new Snapshot(schemaVersion, List.copyOf(definitions), List.copyOf(groups));
    }

    private static int requireInt(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing required stat augment integer: " + key);
        }
        return object.get(key).getAsInt();
    }

    private static double requireDouble(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing required stat augment number: " + key);
        }
        return object.get(key).getAsDouble();
    }

    private static String requireString(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            throw new IllegalStateException("Missing required stat augment string: " + key);
        }
        String value = object.get(key).getAsString();
        if (value.isBlank()) {
            throw new IllegalStateException("Blank stat augment string: " + key);
        }
        return value;
    }

    public record Snapshot(
            int schemaVersion,
            List<StatModifierDefinition> definitions,
            List<ModifierGroup> groups
    ) {
        public Snapshot {
            definitions = List.copyOf(definitions);
            groups = List.copyOf(groups);
        }
    }

    public record StatModifierDefinition(
            String augmentId,
            AttributeTarget attribute,
            double amountPerLevel,
            AttributeModifier.Operation operation
    ) {
        public StatModifierDefinition {
            Objects.requireNonNull(augmentId, "augmentId");
            Objects.requireNonNull(attribute, "attribute");
            Objects.requireNonNull(operation, "operation");
        }
    }

    public record ModifierGroup(
            AttributeTarget attribute,
            AttributeModifier.Operation operation,
            Identifier modifierId,
            List<StatModifierDefinition> definitions
    ) {
        public ModifierGroup {
            Objects.requireNonNull(attribute, "attribute");
            Objects.requireNonNull(operation, "operation");
            Objects.requireNonNull(modifierId, "modifierId");
            definitions = List.copyOf(definitions);
        }
    }

    private record GroupKey(AttributeTarget attribute, Operation operation) {
    }

    public enum AttributeTarget {
        MAX_HEALTH("max_health", Attributes.MAX_HEALTH),
        ATTACK_SPEED("attack_speed", Attributes.ATTACK_SPEED),
        MOVEMENT_SPEED("movement_speed", Attributes.MOVEMENT_SPEED),
        KNOCKBACK_RESISTANCE("knockback_resistance", Attributes.KNOCKBACK_RESISTANCE),
        BLOCK_INTERACTION_RANGE("block_interaction_range", Attributes.BLOCK_INTERACTION_RANGE),
        SUBMERGED_MINING_SPEED("submerged_mining_speed", Attributes.SUBMERGED_MINING_SPEED),
        MOVEMENT_EFFICIENCY("movement_efficiency", Attributes.MOVEMENT_EFFICIENCY);

        private final String id;
        private final Holder<Attribute> attribute;

        AttributeTarget(String id, Holder<Attribute> attribute) {
            this.id = id;
            this.attribute = attribute;
        }

        public String id() {
            return id;
        }

        public Holder<Attribute> attribute() {
            return attribute;
        }

        private static AttributeTarget fromId(String id) {
            for (AttributeTarget value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            throw new IllegalStateException("Unknown stat augment attribute: " + id);
        }
    }

    private enum Operation {
        ADD_VALUE("add_value", AttributeModifier.Operation.ADD_VALUE),
        ADD_MULTIPLIED_BASE("add_multiplied_base", AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
        ADD_MULTIPLIED_TOTAL("add_multiplied_total", AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        private final String id;
        private final AttributeModifier.Operation minecraftOperation;

        Operation(String id, AttributeModifier.Operation minecraftOperation) {
            this.id = id;
            this.minecraftOperation = minecraftOperation;
        }

        private String id() {
            return id;
        }

        private AttributeModifier.Operation minecraftOperation() {
            return minecraftOperation;
        }

        private static Operation fromId(String id) {
            for (Operation value : values()) {
                if (value.id.equals(id)) {
                    return value;
                }
            }
            throw new IllegalStateException("Unknown stat augment operation: " + id);
        }
    }
}
