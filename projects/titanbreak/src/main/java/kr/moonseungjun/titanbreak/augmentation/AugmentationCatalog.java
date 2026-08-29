package kr.moonseungjun.titanbreak.augmentation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AugmentationCatalog {
    public enum Slot {
        EYE,
        BRAIN,
        NERVES,
        SPINE,
        SKELETON,
        SKIN,
        LEFT_ARM,
        RIGHT_ARM,
        LEGS
    }

    public record Definition(String id, String itemId, Slot slot, int tier,
                             String nameKey, String effectKey, Map<String, Integer> recipe,
                             boolean fabricatorOne) {
        public boolean armModule() {
            return slot == Slot.LEFT_ARM || slot == Slot.RIGHT_ARM;
        }
    }

    private static Map<String, Integer> recipe(Object... values) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], (Integer) values[i + 1]);
        }
        return Map.copyOf(result);
    }

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition("tactical_eye", "tactical_eye", Slot.EYE, 0,
                    "augmentation.titanbreak.tactical_eye", "augmentation.titanbreak.tactical_eye.effect",
                    recipe("optic_sensor_cluster", 1, "calculation_core", 1), true),
            new Definition("thermal_eye", "thermal_eye", Slot.EYE, 1,
                    "augmentation.titanbreak.thermal_eye", "augmentation.titanbreak.thermal_eye.effect",
                    recipe("optic_sensor_cluster", 1, "thermal_optic_cluster", 1), true),
            new Definition("ballistic_eye", "ballistic_correction_eye", Slot.EYE, 1,
                    "augmentation.titanbreak.ballistic_eye", "augmentation.titanbreak.ballistic_eye.effect",
                    recipe("optic_sensor_cluster", 1, "calculation_core", 2), true),
            new Definition("target_assist", "target_assist_coprocessor", Slot.BRAIN, 1,
                    "augmentation.titanbreak.target_assist", "augmentation.titanbreak.target_assist.effect",
                    recipe("calculation_core", 2, "resonant_neural_ganglion", 1), true),
            new Definition("reflex_accelerator", "reflex_accelerator_node", Slot.NERVES, 1,
                    "augmentation.titanbreak.reflex_accelerator", "augmentation.titanbreak.reflex_accelerator.effect",
                    recipe("high_density_neural_fiber", 2, "resonant_neural_ganglion", 1), true),
            new Definition("threat_detection", "threat_detection_node", Slot.BRAIN, 1,
                    "augmentation.titanbreak.threat_detection", "augmentation.titanbreak.threat_detection.effect",
                    recipe("thermal_optic_cluster", 1, "resonant_neural_ganglion", 1), true),
            new Definition("powered_spine", "powered_spine", Slot.SPINE, 1,
                    "augmentation.titanbreak.powered_spine", "augmentation.titanbreak.powered_spine.effect",
                    recipe("dense_bone_lattice", 2, "servo_bundle", 2), true),
            new Definition("bioalloy_skeleton", "bioalloy_skeleton", Slot.SKELETON, 1,
                    "augmentation.titanbreak.bioalloy_skeleton", "augmentation.titanbreak.bioalloy_skeleton.effect",
                    recipe("dense_bone_lattice", 2, "composite_armor_plate", 3), true),
            new Definition("subdermal_armor", "subdermal_armor_plate", Slot.SKIN, 1,
                    "augmentation.titanbreak.subdermal_armor", "augmentation.titanbreak.subdermal_armor.effect",
                    recipe("composite_armor_plate", 4, "dense_bone_lattice", 1), true),
            new Definition("blade_arm", "blade_arm", Slot.RIGHT_ARM, 1,
                    "augmentation.titanbreak.blade_arm", "augmentation.titanbreak.blade_arm.effect",
                    recipe("composite_armor_plate", 2, "high_density_muscle_fiber", 2), true),
            new Definition("wire_hook_arm", "wire_hook_arm", Slot.RIGHT_ARM, 1,
                    "augmentation.titanbreak.wire_hook_arm", "augmentation.titanbreak.wire_hook_arm.effect",
                    recipe("servo_bundle", 2, "synthetic_tendon", 2), true),
            new Definition("reinforced_legs", "reinforced_tendon_legs", Slot.LEGS, 0,
                    "augmentation.titanbreak.reinforced_legs", "augmentation.titanbreak.reinforced_legs.effect",
                    recipe("synthetic_tendon", 2, "high_density_muscle_fiber", 1), true),
            new Definition("reflex_drive_i", "reflex_drive_i", Slot.SPINE, 2,
                    "item.titanbreak.reflex_drive_i", "augmentation.titanbreak.reflex_drive_i.effect",
                    recipe("temporal_neural_bundle", 1, "reaction_temporal_matrix", 1), false)
    );

    private AugmentationCatalog() {}

    public static Definition byId(String id) {
        for (Definition definition : DEFINITIONS) {
            if (definition.id().equals(id)) return definition;
        }
        return null;
    }

    public static Definition byItemId(String id) {
        for (Definition definition : DEFINITIONS) {
            if (definition.itemId().equals(id)) return definition;
        }
        return null;
    }

    public static List<Definition> fabricatorOneDefinitions() {
        return DEFINITIONS.stream().filter(Definition::fabricatorOne).toList();
    }
}
