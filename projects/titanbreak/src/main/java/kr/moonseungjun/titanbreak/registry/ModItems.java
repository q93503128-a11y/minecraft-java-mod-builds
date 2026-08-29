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

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Titanbreak.MOD_ID);

    public static final DeferredItem<Item> REFLEX_DRIVE_I = module("reflex_drive_i", Rarity.RARE);

    public static final DeferredItem<Item> SERVO_BUNDLE = material("servo_bundle");
    public static final DeferredItem<Item> SYNTHETIC_TENDON = material("synthetic_tendon");
    public static final DeferredItem<Item> HIGH_DENSITY_MUSCLE_FIBER = material("high_density_muscle_fiber");
    public static final DeferredItem<Item> HIGH_DENSITY_NEURAL_FIBER = material("high_density_neural_fiber");
    public static final DeferredItem<Item> COMPOSITE_ARMOR_PLATE = material("composite_armor_plate");
    public static final DeferredItem<Item> DENSE_BONE_LATTICE = material("dense_bone_lattice");
    public static final DeferredItem<Item> OPTIC_SENSOR_CLUSTER = material("optic_sensor_cluster");
    public static final DeferredItem<Item> CALCULATION_CORE = material("calculation_core");
    public static final DeferredItem<Item> RESONANT_NEURAL_GANGLION = material("resonant_neural_ganglion");
    public static final DeferredItem<Item> THERMAL_OPTIC_CLUSTER = material("thermal_optic_cluster");
    public static final DeferredItem<Item> TEMPORAL_NEURAL_BUNDLE = material("temporal_neural_bundle");
    public static final DeferredItem<Item> REACTION_TEMPORAL_MATRIX = material("reaction_temporal_matrix");
    public static final DeferredItem<Item> PREDICTIVE_OPTIC_CORE = material("predictive_optic_core");
    public static final DeferredItem<Item> PURSUER_REACTION_ORGAN = material("pursuer_reaction_organ");

    public static final DeferredItem<Item> TACTICAL_EYE = module("tactical_eye", Rarity.UNCOMMON);
    public static final DeferredItem<Item> THERMAL_EYE = module("thermal_eye", Rarity.UNCOMMON);
    public static final DeferredItem<Item> BALLISTIC_CORRECTION_EYE = module("ballistic_correction_eye", Rarity.UNCOMMON);
    public static final DeferredItem<Item> TARGET_ASSIST_COPROCESSOR = module("target_assist_coprocessor", Rarity.UNCOMMON);
    public static final DeferredItem<Item> REFLEX_ACCELERATOR_NODE = module("reflex_accelerator_node", Rarity.UNCOMMON);
    public static final DeferredItem<Item> THREAT_DETECTION_NODE = module("threat_detection_node", Rarity.UNCOMMON);
    public static final DeferredItem<Item> POWERED_SPINE = module("powered_spine", Rarity.UNCOMMON);
    public static final DeferredItem<Item> BIOALLOY_SKELETON = module("bioalloy_skeleton", Rarity.UNCOMMON);
    public static final DeferredItem<Item> SUBDERMAL_ARMOR_PLATE = module("subdermal_armor_plate", Rarity.UNCOMMON);
    public static final DeferredItem<Item> BLADE_ARM = module("blade_arm", Rarity.UNCOMMON);
    public static final DeferredItem<Item> WIRE_HOOK_ARM = module("wire_hook_arm", Rarity.UNCOMMON);
    public static final DeferredItem<Item> REINFORCED_TENDON_LEGS = module("reinforced_tendon_legs", Rarity.UNCOMMON);

    public static final DeferredItem<BlockItem> FABRICATOR_I = ITEMS.registerSimpleBlockItem(ModBlocks.FABRICATOR_I);
    public static final DeferredItem<BlockItem> SURGICAL_BAY = ITEMS.registerSimpleBlockItem(ModBlocks.SURGICAL_BAY);

    private ModItems() {}

    private static DeferredItem<Item> material(String id) {
        return ITEMS.registerItem(id, properties -> new Item(properties));
    }

    private static DeferredItem<Item> module(String id, Rarity rarity) {
        return ITEMS.registerItem(id, properties -> new Item(properties.stacksTo(1).rarity(rarity)));
    }

    public static Item byPath(String path) {
        return switch (path) {
            case "servo_bundle" -> SERVO_BUNDLE.get();
            case "synthetic_tendon" -> SYNTHETIC_TENDON.get();
            case "high_density_muscle_fiber" -> HIGH_DENSITY_MUSCLE_FIBER.get();
            case "high_density_neural_fiber" -> HIGH_DENSITY_NEURAL_FIBER.get();
            case "composite_armor_plate" -> COMPOSITE_ARMOR_PLATE.get();
            case "dense_bone_lattice" -> DENSE_BONE_LATTICE.get();
            case "optic_sensor_cluster" -> OPTIC_SENSOR_CLUSTER.get();
            case "calculation_core" -> CALCULATION_CORE.get();
            case "resonant_neural_ganglion" -> RESONANT_NEURAL_GANGLION.get();
            case "thermal_optic_cluster" -> THERMAL_OPTIC_CLUSTER.get();
            case "temporal_neural_bundle" -> TEMPORAL_NEURAL_BUNDLE.get();
            case "reaction_temporal_matrix" -> REACTION_TEMPORAL_MATRIX.get();
            case "predictive_optic_core" -> PREDICTIVE_OPTIC_CORE.get();
            case "pursuer_reaction_organ" -> PURSUER_REACTION_ORGAN.get();
            default -> null;
        };
    }

    public static Item augmentationByPath(String path) {
        return switch (path) {
            case "reflex_drive_i" -> REFLEX_DRIVE_I.get();
            case "tactical_eye" -> TACTICAL_EYE.get();
            case "thermal_eye" -> THERMAL_EYE.get();
            case "ballistic_correction_eye" -> BALLISTIC_CORRECTION_EYE.get();
            case "target_assist_coprocessor" -> TARGET_ASSIST_COPROCESSOR.get();
            case "reflex_accelerator_node" -> REFLEX_ACCELERATOR_NODE.get();
            case "threat_detection_node" -> THREAT_DETECTION_NODE.get();
            case "powered_spine" -> POWERED_SPINE.get();
            case "bioalloy_skeleton" -> BIOALLOY_SKELETON.get();
            case "subdermal_armor_plate" -> SUBDERMAL_ARMOR_PLATE.get();
            case "blade_arm" -> BLADE_ARM.get();
            case "wire_hook_arm" -> WIRE_HOOK_ARM.get();
            case "reinforced_tendon_legs" -> REINFORCED_TENDON_LEGS.get();
            default -> null;
        };
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
        bus.addListener(ModItems::addCreativeItems);
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(REFLEX_DRIVE_I.get());
            event.accept(TACTICAL_EYE.get());
            event.accept(THERMAL_EYE.get());
            event.accept(BALLISTIC_CORRECTION_EYE.get());
            event.accept(TARGET_ASSIST_COPROCESSOR.get());
            event.accept(REFLEX_ACCELERATOR_NODE.get());
            event.accept(THREAT_DETECTION_NODE.get());
            event.accept(POWERED_SPINE.get());
            event.accept(BIOALLOY_SKELETON.get());
            event.accept(SUBDERMAL_ARMOR_PLATE.get());
            event.accept(BLADE_ARM.get());
            event.accept(WIRE_HOOK_ARM.get());
            event.accept(REINFORCED_TENDON_LEGS.get());
        }
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(SERVO_BUNDLE.get());
            event.accept(SYNTHETIC_TENDON.get());
            event.accept(HIGH_DENSITY_MUSCLE_FIBER.get());
            event.accept(HIGH_DENSITY_NEURAL_FIBER.get());
            event.accept(COMPOSITE_ARMOR_PLATE.get());
            event.accept(DENSE_BONE_LATTICE.get());
            event.accept(OPTIC_SENSOR_CLUSTER.get());
            event.accept(CALCULATION_CORE.get());
            event.accept(RESONANT_NEURAL_GANGLION.get());
            event.accept(THERMAL_OPTIC_CLUSTER.get());
            event.accept(TEMPORAL_NEURAL_BUNDLE.get());
            event.accept(REACTION_TEMPORAL_MATRIX.get());
            event.accept(PREDICTIVE_OPTIC_CORE.get());
            event.accept(PURSUER_REACTION_ORGAN.get());
        }
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(FABRICATOR_I.get());
            event.accept(SURGICAL_BAY.get());
        }
    }
}
