package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Titanbreak.MOD_ID);
    private static final Map<String, DeferredItem<Item>> MATERIALS = new LinkedHashMap<>();
    private static final Map<String, DeferredItem<Item>> AUGMENTATIONS = new LinkedHashMap<>();

    public static final DeferredItem<Item> COMPOSITE_ARMOR_PLATE = material("composite_armor_plate");
    public static final DeferredItem<Item> SERVO_BUNDLE = material("servo_bundle");
    public static final DeferredItem<Item> CALCULATION_CORE = material("calculation_core");
    public static final DeferredItem<Item> COOLING_CELL = material("cooling_cell");
    public static final DeferredItem<Item> SYNTHETIC_TENDON = material("synthetic_tendon");
    public static final DeferredItem<Item> SUTURE_POLYMER = material("suture_polymer");
    public static final DeferredItem<Item> HIGH_DENSITY_NEURAL_FIBER = material("high_density_neural_fiber");
    public static final DeferredItem<Item> RESONANT_NEURAL_GANGLION = material("resonant_neural_ganglion");
    public static final DeferredItem<Item> TEMPORAL_NEURAL_BUNDLE = material("temporal_neural_bundle");
    public static final DeferredItem<Item> OPTIC_SENSOR_CLUSTER = material("optic_sensor_cluster");
    public static final DeferredItem<Item> THERMAL_OPTIC_CLUSTER = material("thermal_optic_cluster");
    public static final DeferredItem<Item> PREDICTIVE_OPTIC_CORE = material("predictive_optic_core");
    public static final DeferredItem<Item> HIGH_DENSITY_MUSCLE_FIBER = material("high_density_muscle_fiber");
    public static final DeferredItem<Item> IMPACT_CORE = material("impact_core");
    public static final DeferredItem<Item> DENSE_BONE_LATTICE = material("dense_bone_lattice");
    public static final DeferredItem<Item> CAPACITOR_STACK = material("capacitor_stack");
    public static final DeferredItem<Item> HEAT_SINK = material("heat_sink");
    public static final DeferredItem<Item> RADIATION_CORE = material("radiation_core");
    public static final DeferredItem<Item> REGENERATIVE_TISSUE = material("regenerative_tissue");
    public static final DeferredItem<Item> CIRCULATION_CORE = material("circulation_core");
    public static final DeferredItem<Item> NANO_MEDIUM = material("nano_medium");
    public static final DeferredItem<Item> REACTION_TEMPORAL_MATRIX = material("reaction_temporal_matrix");
    public static final DeferredItem<Item> PHASE_COIL = material("phase_coil");
    public static final DeferredItem<Item> TEMPORAL_ORGAN = material("temporal_organ");
    public static final DeferredItem<Item> PURSUER_REACTION_ORGAN = material("pursuer_reaction_organ");
    public static final DeferredItem<Item> GRAVEMARCH_IMPACT_HEART = material("gravemarch_impact_heart");
    public static final DeferredItem<Item> BASTION_ARMOR_CORE = material("bastion_armor_core");
    public static final DeferredItem<Item> REGNANT_REGENERATION_CORE = material("regnant_regeneration_core");
    public static final DeferredItem<Item> WATCHER_PREDICTIVE_BRAIN = material("watcher_predictive_brain");
    public static final DeferredItem<Item> CHRONOPHAGE_TEMPORAL_ORGAN = material("chronophage_temporal_organ");
    public static final DeferredItem<Item> LEVIATHAN_STORM_ORGAN = material("leviathan_storm_organ");
    public static final DeferredItem<Item> ASH_RADIANT_HEART = material("ash_radiant_heart");
    public static final DeferredItem<Item> NULL_SUPPRESSION_CORE = material("null_suppression_core");
    public static final DeferredItem<Item> WORLDBREAKER_CORE = material("worldbreaker_core");

    public static final DeferredItem<Item> TACTICAL_EYE = module("tactical_eye", rarityForTier(0));
    public static final DeferredItem<Item> THERMAL_EYE = module("thermal_eye", rarityForTier(1));
    public static final DeferredItem<Item> STRUCTURAL_SECTION_EYE = module("structural_section_eye", rarityForTier(2));
    public static final DeferredItem<Item> MOTION_PREDICTION_EYE = module("motion_prediction_eye", rarityForTier(2));
    public static final DeferredItem<Item> WEAKPOINT_ANALYSIS_EYE = module("weakpoint_analysis_eye", rarityForTier(2));
    public static final DeferredItem<Item> BALLISTIC_CORRECTION_EYE = module("ballistic_correction_eye", rarityForTier(1));
    public static final DeferredItem<Item> MULTISPECTRUM_EYE = module("multispectrum_eye", rarityForTier(1));
    public static final DeferredItem<Item> ELECTROMAGNETIC_EYE = module("electromagnetic_eye", rarityForTier(3));
    public static final DeferredItem<Item> TARGET_ASSIST_COPROCESSOR = module("target_assist_coprocessor", rarityForTier(1));
    public static final DeferredItem<Item> PREDICTIVE_COMBAT_CORE = module("predictive_combat_core", rarityForTier(2));
    public static final DeferredItem<Item> REFLEX_ACCELERATOR_NODE = module("reflex_accelerator_node", rarityForTier(1));
    public static final DeferredItem<Item> THREAT_DETECTION_NODE = module("threat_detection_node", rarityForTier(1));
    public static final DeferredItem<Item> MOTOR_SYNC_CORE = module("motor_sync_core", rarityForTier(2));
    public static final DeferredItem<Item> COMBAT_AUTOPILOT = module("combat_autopilot", rarityForTier(4));
    public static final DeferredItem<Item> PAIN_SUPPRESSOR = module("pain_suppressor", rarityForTier(2));
    public static final DeferredItem<Item> REFLEX_DRIVE_I = module("reflex_drive_i", Rarity.RARE);
    public static final DeferredItem<Item> POWERED_SPINE = module("powered_spine", rarityForTier(1));
    public static final DeferredItem<Item> PHASE_STEP_SPINE = module("phase_step_spine", rarityForTier(4));
    public static final DeferredItem<Item> GYRO_STABILIZED_SPINE = module("gyro_stabilized_spine", rarityForTier(2));
    public static final DeferredItem<Item> KINETIC_RELAY_SPINE = module("kinetic_relay_spine", rarityForTier(3));
    public static final DeferredItem<Item> HIGH_SPEED_NEURAL_BUS = module("high_speed_neural_bus", rarityForTier(3));
    public static final DeferredItem<Item> ARTIFICIAL_HEART = module("artificial_heart", rarityForTier(0));
    public static final DeferredItem<Item> DUAL_HEART = module("dual_heart", rarityForTier(3));
    public static final DeferredItem<Item> ADRENALINE_PUMP = module("adrenaline_pump", rarityForTier(1));
    public static final DeferredItem<Item> HEMOSTATIC_PUMP = module("hemostatic_pump", rarityForTier(2));
    public static final DeferredItem<Item> OVERDRIVE_CIRCULATION = module("overdrive_circulation", rarityForTier(4));
    public static final DeferredItem<Item> BIOALLOY_SKELETON = module("bioalloy_skeleton", rarityForTier(1));
    public static final DeferredItem<Item> IMPACT_DISPERSAL_FRAME = module("impact_dispersal_frame", rarityForTier(2));
    public static final DeferredItem<Item> SUBDERMAL_ARMOR_PLATE = module("subdermal_armor_plate", rarityForTier(1));
    public static final DeferredItem<Item> REACTIVE_DERMIS = module("reactive_dermis", rarityForTier(3));
    public static final DeferredItem<Item> OPTICAL_CAMO_SKIN = module("optical_camo_skin", rarityForTier(3));
    public static final DeferredItem<Item> HEAT_SHUNT_MESH = module("heat_shunt_mesh", rarityForTier(2));
    public static final DeferredItem<Item> BLADE_ARM = module("blade_arm", rarityForTier(1));
    public static final DeferredItem<Item> HIGH_FREQUENCY_BLADE_ARM = module("high_frequency_blade_arm", rarityForTier(3));
    public static final DeferredItem<Item> POWER_ARM = module("power_arm", rarityForTier(1));
    public static final DeferredItem<Item> WIRE_HOOK_ARM = module("wire_hook_arm", rarityForTier(1));
    public static final DeferredItem<Item> RAIL_PROJECTOR_ARM = module("rail_projector_arm", rarityForTier(3));
    public static final DeferredItem<Item> PHOTON_EMITTER_ARM = module("photon_emitter_arm", rarityForTier(4));
    public static final DeferredItem<Item> SHOCK_PALM = module("shock_palm", rarityForTier(2));
    public static final DeferredItem<Item> SHIELD_PROJECTOR_ARM = module("shield_projector_arm", rarityForTier(3));
    public static final DeferredItem<Item> PRECISION_TOOL_ARM = module("precision_tool_arm", rarityForTier(0));
    public static final DeferredItem<Item> REINFORCED_TENDON_LEGS = module("reinforced_tendon_legs", rarityForTier(0));
    public static final DeferredItem<Item> JUMP_BOOSTER_LEGS = module("jump_booster_legs", rarityForTier(1));
    public static final DeferredItem<Item> PROPULSION_LEGS = module("propulsion_legs", rarityForTier(3));
    public static final DeferredItem<Item> WALL_RUN_SPURS = module("wall_run_spurs", rarityForTier(1));
    public static final DeferredItem<Item> IMPACT_ABSORBER_LEGS = module("impact_absorber_legs", rarityForTier(2));
    public static final DeferredItem<Item> NANO_REPAIR_ORGAN = module("nano_repair_organ", rarityForTier(3));
    public static final DeferredItem<Item> AUXILIARY_POWER_ORGAN = module("auxiliary_power_organ", rarityForTier(2));

    public static final DeferredItem<BlockItem> FABRICATOR_I = ITEMS.registerSimpleBlockItem(ModBlocks.FABRICATOR_I);
    public static final DeferredItem<BlockItem> FABRICATOR_II = ITEMS.registerSimpleBlockItem(ModBlocks.FABRICATOR_II);
    public static final DeferredItem<BlockItem> FABRICATOR_III = ITEMS.registerSimpleBlockItem(ModBlocks.FABRICATOR_III);
    public static final DeferredItem<BlockItem> SURGICAL_BAY = ITEMS.registerSimpleBlockItem(ModBlocks.SURGICAL_BAY);
    public static final DeferredItem<BlockItem> IMPLANT_VAULT = ITEMS.registerSimpleBlockItem(ModBlocks.IMPLANT_VAULT);

    private ModItems() {}

    private static DeferredItem<Item> material(String id) {
        DeferredItem<Item> item = ITEMS.registerItem(id, properties -> new Item(properties));
        MATERIALS.put(id, item);
        return item;
    }

    private static DeferredItem<Item> module(String id, Rarity rarity) {
        DeferredItem<Item> item = ITEMS.registerItem(id, properties -> new Item(properties.stacksTo(1).rarity(rarity)));
        AUGMENTATIONS.put(id, item);
        return item;
    }

    private static Rarity rarityForTier(int tier) {
        if (tier <= 0) return Rarity.COMMON;
        if (tier == 1) return Rarity.UNCOMMON;
        if (tier <= 3) return Rarity.RARE;
        return Rarity.EPIC;
    }

    public static Item byPath(String path) {
        DeferredItem<Item> holder = MATERIALS.get(path);
        return holder == null ? null : holder.get();
    }

    public static Item augmentationByPath(String path) {
        DeferredItem<Item> holder = AUGMENTATIONS.get(path);
        return holder == null ? null : holder.get();
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            for (DeferredItem<Item> item : AUGMENTATIONS.values()) event.accept(item.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            for (DeferredItem<Item> item : MATERIALS.values()) event.accept(item.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(FABRICATOR_I.get());
            event.accept(FABRICATOR_II.get());
            event.accept(FABRICATOR_III.get());
            event.accept(SURGICAL_BAY.get());
            event.accept(IMPLANT_VAULT.get());
        }
    }
}
