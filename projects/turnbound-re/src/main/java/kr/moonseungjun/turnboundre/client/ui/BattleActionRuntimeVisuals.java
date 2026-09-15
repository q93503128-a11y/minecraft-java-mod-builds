package kr.moonseungjun.turnboundre.client.ui;

import net.minecraft.world.item.ItemStack;

/**
 * Presentation-only action identity backed by Mojang runtime item models.
 * Unknown actions deliberately resolve to empty instead of inventing a TURNBOUND placeholder icon.
 */
final class BattleActionRuntimeVisuals {
    private BattleActionRuntimeVisuals() {}

    static ItemStack icon(String actionId) {
        String visualItem = visualItem(actionId);
        return visualItem.isBlank() ? ItemStack.EMPTY : RuntimeItemVisualResolver.stack(visualItem);
    }

    static String visualItem(String actionId) {
        if (actionId == null || actionId.isBlank()) return "";
        return switch (actionId) {
            case "turnbound_re:zombie_rotten_swing" -> "minecraft:rotten_flesh";
            case "turnbound_re:zombie_undead_grit" -> "minecraft:shield";
            case "turnbound_re:zombie_gravebreaker" -> "minecraft:iron_shovel";
            case "turnbound_re:zombie_relentless_horde" -> "minecraft:zombie_head";

            case "turnbound_re:skeleton_bone_arrow" -> "minecraft:arrow";
            case "turnbound_re:skeleton_pinning_shot" -> "minecraft:spectral_arrow";
            case "turnbound_re:skeleton_deadeye" -> "minecraft:crossbow";
            case "turnbound_re:skeleton_arrow_storm" -> "minecraft:bow";

            case "turnbound_re:spider_fang" -> "minecraft:spider_eye";
            case "turnbound_re:spider_venom_bite" -> "minecraft:fermented_spider_eye";
            case "turnbound_re:spider_binding_web" -> "minecraft:cobweb";
            case "turnbound_re:spider_brood_pounce" -> "minecraft:string";

            case "turnbound_re:creeper_fuse_bash" -> "minecraft:gunpowder";
            case "turnbound_re:creeper_volatile_charge" -> "minecraft:fire_charge";
            case "turnbound_re:creeper_blast_wave" -> "minecraft:tnt";
            case "turnbound_re:creeper_catastrophe" -> "minecraft:creeper_head";

            case "turnbound_re:blaze_ember_bolt" -> "minecraft:fire_charge";
            case "turnbound_re:blaze_searing_volley" -> "minecraft:blaze_powder";
            case "turnbound_re:blaze_heat_up" -> "minecraft:blaze_rod";
            case "turnbound_re:blaze_inferno_burst" -> "minecraft:magma_cream";

            case "turnbound_re:witch_splash_hex" -> "minecraft:splash_potion";
            case "turnbound_re:witch_weakening_brew" -> "minecraft:fermented_spider_eye";
            case "turnbound_re:witch_restorative_draught" -> "minecraft:potion";
            case "turnbound_re:witch_cauldron_overflow" -> "minecraft:cauldron";

            case "turnbound_re:enderman_rift_strike" -> "minecraft:ender_pearl";
            case "turnbound_re:enderman_phase_step" -> "minecraft:chorus_fruit";
            case "turnbound_re:enderman_void_rend" -> "minecraft:obsidian";
            case "turnbound_re:enderman_horizon_break" -> "minecraft:end_crystal";

            case "turnbound_re:iron_golem_iron_fist" -> "minecraft:iron_ingot";
            case "turnbound_re:iron_golem_guardian_plate" -> "minecraft:shield";
            case "turnbound_re:iron_golem_ground_slam" -> "minecraft:iron_block";
            case "turnbound_re:iron_golem_village_judgment" -> "minecraft:anvil";
            default -> "";
        };
    }
}
