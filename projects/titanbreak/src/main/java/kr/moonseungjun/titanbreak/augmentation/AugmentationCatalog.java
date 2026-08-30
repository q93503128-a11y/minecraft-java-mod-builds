package kr.moonseungjun.titanbreak.augmentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AugmentationCatalog {
    public enum Region {
        EYE,
        BRAIN,
        NERVES,
        SPINE,
        HEART,
        SKELETON,
        SKIN,
        LEFT_ARM,
        RIGHT_ARM,
        LEFT_LEG,
        RIGHT_LEG,
        AUX_ORGAN
    }

    public enum SlotKind {
        STANDARD,
        MAIN,
        AUXILIARY
    }

    public enum Slot {
        EYE_1(Region.EYE, SlotKind.STANDARD),
        EYE_2(Region.EYE, SlotKind.STANDARD),
        BRAIN_1(Region.BRAIN, SlotKind.STANDARD),
        BRAIN_2(Region.BRAIN, SlotKind.STANDARD),
        NERVES_1(Region.NERVES, SlotKind.STANDARD),
        NERVES_2(Region.NERVES, SlotKind.STANDARD),
        SPINE_MAIN(Region.SPINE, SlotKind.MAIN),
        SPINE_AUX(Region.SPINE, SlotKind.AUXILIARY),
        HEART_1(Region.HEART, SlotKind.STANDARD),
        HEART_2(Region.HEART, SlotKind.STANDARD),
        SKELETON_1(Region.SKELETON, SlotKind.STANDARD),
        SKELETON_2(Region.SKELETON, SlotKind.STANDARD),
        SKIN_1(Region.SKIN, SlotKind.STANDARD),
        SKIN_2(Region.SKIN, SlotKind.STANDARD),
        LEFT_ARM_MAIN(Region.LEFT_ARM, SlotKind.MAIN),
        LEFT_ARM_AUX(Region.LEFT_ARM, SlotKind.AUXILIARY),
        RIGHT_ARM_MAIN(Region.RIGHT_ARM, SlotKind.MAIN),
        RIGHT_ARM_AUX(Region.RIGHT_ARM, SlotKind.AUXILIARY),
        LEFT_LEG_MAIN(Region.LEFT_LEG, SlotKind.MAIN),
        LEFT_LEG_AUX(Region.LEFT_LEG, SlotKind.AUXILIARY),
        RIGHT_LEG_MAIN(Region.RIGHT_LEG, SlotKind.MAIN),
        RIGHT_LEG_AUX(Region.RIGHT_LEG, SlotKind.AUXILIARY),
        AUX_ORGAN_1(Region.AUX_ORGAN, SlotKind.STANDARD),
        AUX_ORGAN_2(Region.AUX_ORGAN, SlotKind.STANDARD);

        private final Region region;
        private final SlotKind kind;

        Slot(Region region, SlotKind kind) {
            this.region = region;
            this.kind = kind;
        }

        public Region region() {
            return region;
        }

        public SlotKind kind() {
            return kind;
        }

        public String translationKey() {
            return "slot.titanbreak." + name().toLowerCase();
        }
    }

    public record Placement(List<Slot> slots) {
        public Placement {
            slots = List.copyOf(slots);
            if (slots.isEmpty()) throw new IllegalArgumentException("augmentation placement cannot be empty");
        }

        public Slot anchor() {
            return slots.getFirst();
        }

        public boolean contains(Slot slot) {
            return slots.contains(slot);
        }
    }

    public record Definition(String id, String itemId, int tier,
                             String nameKey, String effectKey, Map<String, Integer> recipe,
                             boolean fabricatorOne, List<Placement> placements) {
        public Definition {
            placements = List.copyOf(placements);
            if (placements.isEmpty()) throw new IllegalArgumentException("augmentation must have at least one placement");
        }

        public Placement placementFor(Slot slot) {
            for (Placement placement : placements) {
                if (placement.contains(slot)) return placement;
            }
            return null;
        }

        public boolean canInstallAt(Slot slot) {
            return placementFor(slot) != null;
        }

        public List<Slot> compatibleSlots() {
            List<Slot> result = new ArrayList<>();
            for (Placement placement : placements) {
                for (Slot slot : placement.slots()) {
                    if (!result.contains(slot)) result.add(slot);
                }
            }
            return List.copyOf(result);
        }
    }

    private static Map<String, Integer> recipe(Object... values) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) {
            result.put((String) values[i], (Integer) values[i + 1]);
        }
        return Map.copyOf(result);
    }

    private static List<Placement> singles(Slot... slots) {
        List<Placement> placements = new ArrayList<>();
        for (Slot slot : slots) placements.add(new Placement(List.of(slot)));
        return List.copyOf(placements);
    }

    private static List<Placement> paired(Slot... slots) {
        return List.of(new Placement(List.of(slots)));
    }

    public static final List<Definition> DEFINITIONS = List.of(
            new Definition("tactical_eye", "tactical_eye", 0,
                    "augmentation.titanbreak.tactical_eye", "augmentation.titanbreak.tactical_eye.effect",
                    recipe("optic_sensor_cluster", 1, "calculation_core", 1), true,
                    singles(Slot.EYE_1, Slot.EYE_2)),
            new Definition("thermal_eye", "thermal_eye", 1,
                    "augmentation.titanbreak.thermal_eye", "augmentation.titanbreak.thermal_eye.effect",
                    recipe("optic_sensor_cluster", 1, "thermal_optic_cluster", 1), true,
                    singles(Slot.EYE_1, Slot.EYE_2)),
            new Definition("ballistic_eye", "ballistic_correction_eye", 1,
                    "augmentation.titanbreak.ballistic_eye", "augmentation.titanbreak.ballistic_eye.effect",
                    recipe("optic_sensor_cluster", 1, "calculation_core", 2), true,
                    singles(Slot.EYE_1, Slot.EYE_2)),
            new Definition("target_assist", "target_assist_coprocessor", 1,
                    "augmentation.titanbreak.target_assist", "augmentation.titanbreak.target_assist.effect",
                    recipe("calculation_core", 2, "resonant_neural_ganglion", 1), true,
                    singles(Slot.BRAIN_1, Slot.BRAIN_2)),
            new Definition("reflex_accelerator", "reflex_accelerator_node", 1,
                    "augmentation.titanbreak.reflex_accelerator", "augmentation.titanbreak.reflex_accelerator.effect",
                    recipe("high_density_neural_fiber", 2, "resonant_neural_ganglion", 1), true,
                    singles(Slot.NERVES_1, Slot.NERVES_2)),
            new Definition("threat_detection", "threat_detection_node", 1,
                    "augmentation.titanbreak.threat_detection", "augmentation.titanbreak.threat_detection.effect",
                    recipe("thermal_optic_cluster", 1, "resonant_neural_ganglion", 1), true,
                    singles(Slot.BRAIN_1, Slot.BRAIN_2)),
            new Definition("powered_spine", "powered_spine", 1,
                    "augmentation.titanbreak.powered_spine", "augmentation.titanbreak.powered_spine.effect",
                    recipe("dense_bone_lattice", 2, "servo_bundle", 2), true,
                    singles(Slot.SPINE_MAIN)),
            new Definition("bioalloy_skeleton", "bioalloy_skeleton", 1,
                    "augmentation.titanbreak.bioalloy_skeleton", "augmentation.titanbreak.bioalloy_skeleton.effect",
                    recipe("dense_bone_lattice", 2, "composite_armor_plate", 3), true,
                    singles(Slot.SKELETON_1, Slot.SKELETON_2)),
            new Definition("subdermal_armor", "subdermal_armor_plate", 1,
                    "augmentation.titanbreak.subdermal_armor", "augmentation.titanbreak.subdermal_armor.effect",
                    recipe("composite_armor_plate", 4, "dense_bone_lattice", 1), true,
                    singles(Slot.SKIN_1, Slot.SKIN_2)),
            new Definition("blade_arm", "blade_arm", 1,
                    "augmentation.titanbreak.blade_arm", "augmentation.titanbreak.blade_arm.effect",
                    recipe("composite_armor_plate", 2, "high_density_muscle_fiber", 2), true,
                    singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            new Definition("wire_hook_arm", "wire_hook_arm", 1,
                    "augmentation.titanbreak.wire_hook_arm", "augmentation.titanbreak.wire_hook_arm.effect",
                    recipe("servo_bundle", 2, "synthetic_tendon", 2), true,
                    singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            new Definition("reinforced_legs", "reinforced_tendon_legs", 0,
                    "augmentation.titanbreak.reinforced_legs", "augmentation.titanbreak.reinforced_legs.effect",
                    recipe("synthetic_tendon", 2, "high_density_muscle_fiber", 1), true,
                    paired(Slot.LEFT_LEG_MAIN, Slot.RIGHT_LEG_MAIN)),
            new Definition("reflex_drive_i", "reflex_drive_i", 2,
                    "item.titanbreak.reflex_drive_i", "augmentation.titanbreak.reflex_drive_i.effect",
                    recipe("temporal_neural_bundle", 1, "reaction_temporal_matrix", 1), false,
                    singles(Slot.SPINE_MAIN))
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
