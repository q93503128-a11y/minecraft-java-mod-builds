package kr.moonseungjun.titanbreak.augmentation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AugmentationCatalog {
    public enum Region {
        EYE, BRAIN, NERVES, SPINE, HEART, SKELETON, SKIN,
        LEFT_ARM, RIGHT_ARM, LEFT_LEG, RIGHT_LEG, AUX_ORGAN
    }

    public enum SlotKind { STANDARD, MAIN, AUXILIARY }

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

        public Region region() { return region; }
        public SlotKind kind() { return kind; }
        public String translationKey() { return "slot.titanbreak." + name().toLowerCase(); }
    }

    public record Placement(List<Slot> slots) {
        public Placement {
            slots = List.copyOf(slots);
            if (slots.isEmpty()) throw new IllegalArgumentException("augmentation placement cannot be empty");
        }
        public Slot anchor() { return slots.getFirst(); }
        public boolean contains(Slot slot) { return slots.contains(slot); }
    }

    public record Definition(String id, String itemId, int tier,
                             String nameKey, String effectKey, Map<String, Integer> recipe,
                             int fabricatorTier, int powerLoad, int heatLoad, int neuralLoad,
                             List<Placement> placements) {
        public Definition {
            recipe = Map.copyOf(recipe);
            placements = List.copyOf(placements);
            if (fabricatorTier < 1 || fabricatorTier > 3)
                throw new IllegalArgumentException("fabricator tier must be 1..3");
            if (placements.isEmpty())
                throw new IllegalArgumentException("augmentation must have at least one placement");
        }

        public Placement placementFor(Slot slot) {
            for (Placement placement : placements) if (placement.contains(slot)) return placement;
            return null;
        }
        public boolean canInstallAt(Slot slot) { return placementFor(slot) != null; }
        public boolean fabricatorOne() { return fabricatorTier == 1; }
        public int maxEnhancement() { return switch (fabricatorTier) { case 1 -> 3; case 2 -> 7; default -> 10; }; }

        public List<Slot> compatibleSlots() {
            List<Slot> result = new ArrayList<>();
            for (Placement placement : placements) {
                for (Slot slot : placement.slots()) if (!result.contains(slot)) result.add(slot);
            }
            return List.copyOf(result);
        }
    }

    private static Definition def(String id, String itemId, int tier, String translationStem,
                                  int fabricatorTier, int power, int heat, int neural,
                                  Map<String, Integer> recipe, List<Placement> placements) {
        String nameKey = id.equals("reflex_drive_i")
                ? "item.titanbreak.reflex_drive_i"
                : "augmentation.titanbreak." + translationStem;
        String effectKey = "augmentation.titanbreak." + translationStem + ".effect";
        return new Definition(id, itemId, tier, nameKey, effectKey, recipe,
                fabricatorTier, power, heat, neural, placements);
    }

    private static Map<String, Integer> recipe(Object... values) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put((String) values[i], (Integer) values[i + 1]);
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
            def("tactical_eye", "tactical_eye", 0, "tactical_eye", 1, 2, 0, 1,
                    recipe("optic_sensor_cluster", 1, "calculation_core", 1), singles(Slot.EYE_1, Slot.EYE_2)),
            def("thermal_eye", "thermal_eye", 1, "thermal_eye", 1, 3, 0, 1,
                    recipe("optic_sensor_cluster", 1, "thermal_optic_cluster", 1), singles(Slot.EYE_1, Slot.EYE_2)),
            def("structural_section_eye", "structural_section_eye", 2, "structural_section_eye", 2, 5, 1, 3,
                    recipe("thermal_optic_cluster", 2, "calculation_core", 2), singles(Slot.EYE_1, Slot.EYE_2)),
            def("motion_prediction_eye", "motion_prediction_eye", 2, "motion_prediction_eye", 2, 5, 1, 5,
                    recipe("predictive_optic_core", 1, "resonant_neural_ganglion", 2), singles(Slot.EYE_1, Slot.EYE_2)),
            def("weakpoint_analysis_eye", "weakpoint_analysis_eye", 2, "weakpoint_analysis_eye", 2, 4, 0, 4,
                    recipe("optic_sensor_cluster", 2, "predictive_optic_core", 1), singles(Slot.EYE_1, Slot.EYE_2)),
            def("ballistic_eye", "ballistic_correction_eye", 1, "ballistic_eye", 1, 3, 0, 2,
                    recipe("optic_sensor_cluster", 1, "calculation_core", 2), singles(Slot.EYE_1, Slot.EYE_2)),
            def("multispectrum_eye", "multispectrum_eye", 1, "multispectrum_eye", 1, 2, 0, 1,
                    recipe("thermal_optic_cluster", 1, "calculation_core", 1), singles(Slot.EYE_1, Slot.EYE_2)),
            def("electromagnetic_eye", "electromagnetic_eye", 3, "electromagnetic_eye", 2, 5, 1, 4,
                    recipe("thermal_optic_cluster", 2, "capacitor_stack", 1), singles(Slot.EYE_1, Slot.EYE_2)),
            def("target_assist", "target_assist_coprocessor", 1, "target_assist", 1, 3, 1, 4,
                    recipe("calculation_core", 2, "resonant_neural_ganglion", 1), singles(Slot.BRAIN_1, Slot.BRAIN_2)),
            def("predictive_combat_core", "predictive_combat_core", 2, "predictive_combat_core", 2, 6, 2, 7,
                    recipe("predictive_optic_core", 1, "temporal_neural_bundle", 1), singles(Slot.BRAIN_1, Slot.BRAIN_2)),
            def("reflex_accelerator", "reflex_accelerator_node", 1, "reflex_accelerator", 1, 4, 2, 6,
                    recipe("high_density_neural_fiber", 2, "resonant_neural_ganglion", 1), singles(Slot.NERVES_1, Slot.NERVES_2)),
            def("threat_detection", "threat_detection_node", 1, "threat_detection", 1, 2, 0, 2,
                    recipe("thermal_optic_cluster", 1, "resonant_neural_ganglion", 1), singles(Slot.BRAIN_1, Slot.BRAIN_2)),
            def("motor_sync_core", "motor_sync_core", 2, "motor_sync_core", 2, 4, 1, 5,
                    recipe("high_density_neural_fiber", 2, "servo_bundle", 1), singles(Slot.NERVES_1, Slot.NERVES_2)),
            def("combat_autopilot", "combat_autopilot", 4, "combat_autopilot", 3, 30, 8, 18,
                    recipe("watcher_predictive_brain", 1, "predictive_optic_core", 2), singles(Slot.BRAIN_1, Slot.BRAIN_2)),
            def("pain_suppressor", "pain_suppressor", 2, "pain_suppressor", 2, 2, 1, 5,
                    recipe("resonant_neural_ganglion", 2, "circulation_core", 1), singles(Slot.NERVES_1, Slot.NERVES_2)),
            def("reflex_drive_i", "reflex_drive_i", 2, "reflex_drive_i", 2, 18, 10, 14,
                    recipe("temporal_neural_bundle", 1, "reaction_temporal_matrix", 1), singles(Slot.SPINE_MAIN)),
            def("powered_spine", "powered_spine", 1, "powered_spine", 1, 5, 3, 2,
                    recipe("dense_bone_lattice", 2, "servo_bundle", 2), singles(Slot.SPINE_MAIN)),
            def("phase_step_spine", "phase_step_spine", 4, "phase_step_spine", 3, 24, 8, 15,
                    recipe("phase_coil", 2, "chronophage_temporal_organ", 1), singles(Slot.SPINE_MAIN)),
            def("gyro_stabilized_spine", "gyro_stabilized_spine", 2, "gyro_stabilized_spine", 2, 4, 1, 3,
                    recipe("servo_bundle", 2, "impact_core", 1), singles(Slot.SPINE_AUX)),
            def("kinetic_relay_spine", "kinetic_relay_spine", 3, "kinetic_relay_spine", 2, 3, 4, 3,
                    recipe("impact_core", 2, "capacitor_stack", 1), singles(Slot.SPINE_AUX)),
            def("high_speed_neural_bus", "high_speed_neural_bus", 3, "high_speed_neural_bus", 2, 5, 2, -12,
                    recipe("high_density_neural_fiber", 3, "calculation_core", 2), singles(Slot.SPINE_AUX)),
            def("artificial_heart", "artificial_heart", 0, "artificial_heart", 1, 1, 0, 0,
                    recipe("circulation_core", 1, "composite_armor_plate", 1), singles(Slot.HEART_1, Slot.HEART_2)),
            def("dual_heart", "dual_heart", 3, "dual_heart", 2, 4, 3, 2,
                    recipe("circulation_core", 2, "regnant_regeneration_core", 1), singles(Slot.HEART_1, Slot.HEART_2)),
            def("adrenaline_pump", "adrenaline_pump", 1, "adrenaline_pump", 1, 3, 5, 3,
                    recipe("circulation_core", 1, "high_density_muscle_fiber", 1), singles(Slot.HEART_1, Slot.HEART_2)),
            def("hemostatic_pump", "hemostatic_pump", 2, "hemostatic_pump", 2, 2, 1, 1,
                    recipe("regenerative_tissue", 2, "suture_polymer", 2), singles(Slot.HEART_1, Slot.HEART_2)),
            def("overdrive_circulation", "overdrive_circulation", 4, "overdrive_circulation", 3, 12, 20, 10,
                    recipe("gravemarch_impact_heart", 1, "circulation_core", 2), singles(Slot.HEART_1, Slot.HEART_2)),
            def("bioalloy_skeleton", "bioalloy_skeleton", 1, "bioalloy_skeleton", 1, 0, 0, 0,
                    recipe("dense_bone_lattice", 2, "composite_armor_plate", 3), singles(Slot.SKELETON_1, Slot.SKELETON_2)),
            def("impact_dispersal_frame", "impact_dispersal_frame", 2, "impact_dispersal_frame", 2, 0, 0, 0,
                    recipe("impact_core", 2, "dense_bone_lattice", 2), singles(Slot.SKELETON_1, Slot.SKELETON_2)),
            def("subdermal_armor", "subdermal_armor_plate", 1, "subdermal_armor", 1, 0, 0, 0,
                    recipe("composite_armor_plate", 4, "dense_bone_lattice", 1), singles(Slot.SKIN_1, Slot.SKIN_2)),
            def("reactive_dermis", "reactive_dermis", 3, "reactive_dermis", 2, 5, 4, 2,
                    recipe("composite_armor_plate", 3, "thermal_optic_cluster", 1), singles(Slot.SKIN_1, Slot.SKIN_2)),
            def("optical_camo_skin", "optical_camo_skin", 3, "optical_camo_skin", 2, 12, 5, 5,
                    recipe("thermal_optic_cluster", 2, "capacitor_stack", 1), singles(Slot.SKIN_1, Slot.SKIN_2)),
            def("heat_shunt_mesh", "heat_shunt_mesh", 2, "heat_shunt_mesh", 2, 2, -10, 0,
                    recipe("heat_sink", 2, "cooling_cell", 2), singles(Slot.SKIN_1, Slot.SKIN_2)),
            def("blade_arm", "blade_arm", 1, "blade_arm", 1, 2, 1, 2,
                    recipe("composite_armor_plate", 2, "high_density_muscle_fiber", 2), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("high_frequency_blade_arm", "high_frequency_blade_arm", 3, "high_frequency_blade_arm", 2, 10, 8, 5,
                    recipe("capacitor_stack", 1, "impact_core", 1, "gravemarch_impact_heart", 1), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("power_arm", "power_arm", 1, "power_arm", 1, 6, 4, 2,
                    recipe("high_density_muscle_fiber", 2, "impact_core", 1), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("wire_hook_arm", "wire_hook_arm", 1, "wire_hook_arm", 1, 4, 1, 3,
                    recipe("servo_bundle", 2, "synthetic_tendon", 2), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("rail_projector_arm", "rail_projector_arm", 3, "rail_projector_arm", 2, 16, 10, 5,
                    recipe("capacitor_stack", 2, "predictive_optic_core", 1), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("photon_emitter_arm", "photon_emitter_arm", 4, "photon_emitter_arm", 3, 35, 35, 10,
                    recipe("radiation_core", 2, "heat_sink", 2, "ash_radiant_heart", 1), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("shock_palm", "shock_palm", 2, "shock_palm", 2, 10, 7, 4,
                    recipe("capacitor_stack", 1, "impact_core", 1), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("shield_projector_arm", "shield_projector_arm", 3, "shield_projector_arm", 2, 14, 8, 5,
                    recipe("capacitor_stack", 2, "bastion_armor_core", 1), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("precision_tool_arm", "precision_tool_arm", 0, "precision_tool_arm", 1, 2, 1, 1,
                    recipe("servo_bundle", 1, "calculation_core", 1), singles(Slot.LEFT_ARM_MAIN, Slot.RIGHT_ARM_MAIN)),
            def("reinforced_legs", "reinforced_tendon_legs", 0, "reinforced_legs", 1, 0, 0, 0,
                    recipe("synthetic_tendon", 2, "high_density_muscle_fiber", 1), paired(Slot.LEFT_LEG_MAIN, Slot.RIGHT_LEG_MAIN)),
            def("jump_booster_legs", "jump_booster_legs", 1, "jump_booster_legs", 1, 5, 3, 2,
                    recipe("servo_bundle", 2, "capacitor_stack", 1), paired(Slot.LEFT_LEG_MAIN, Slot.RIGHT_LEG_MAIN)),
            def("propulsion_legs", "propulsion_legs", 3, "propulsion_legs", 2, 16, 12, 6,
                    recipe("capacitor_stack", 2, "servo_bundle", 3, "leviathan_storm_organ", 1), paired(Slot.LEFT_LEG_MAIN, Slot.RIGHT_LEG_MAIN)),
            def("wall_run_spurs", "wall_run_spurs", 1, "wall_run_spurs", 1, 3, 2, 3,
                    recipe("servo_bundle", 2, "synthetic_tendon", 2), paired(Slot.LEFT_LEG_MAIN, Slot.RIGHT_LEG_MAIN)),
            def("impact_absorber_legs", "impact_absorber_legs", 2, "impact_absorber_legs", 2, 2, 3, 1,
                    recipe("impact_core", 2, "dense_bone_lattice", 1), paired(Slot.LEFT_LEG_MAIN, Slot.RIGHT_LEG_MAIN)),
            def("nano_repair_organ", "nano_repair_organ", 3, "nano_repair_organ", 2, 8, 6, 2,
                    recipe("nano_medium", 2, "regnant_regeneration_core", 1), singles(Slot.AUX_ORGAN_1, Slot.AUX_ORGAN_2)),
            def("auxiliary_power_organ", "auxiliary_power_organ", 2, "auxiliary_power_organ", 2, 0, 2, 0,
                    recipe("capacitor_stack", 3, "cooling_cell", 1), singles(Slot.AUX_ORGAN_1, Slot.AUX_ORGAN_2))
    );

    private AugmentationCatalog() {}

    public static Definition byId(String id) {
        for (Definition definition : DEFINITIONS) if (definition.id().equals(id)) return definition;
        return null;
    }

    public static Definition byItemId(String id) {
        for (Definition definition : DEFINITIONS) if (definition.itemId().equals(id)) return definition;
        return null;
    }

    public static List<Definition> fabricatorDefinitions(int tier) {
        return DEFINITIONS.stream().filter(definition -> definition.fabricatorTier() <= tier).toList();
    }

    public static List<Definition> fabricatorOneDefinitions() {
        return fabricatorDefinitions(1);
    }
}
