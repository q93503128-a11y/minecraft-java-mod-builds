package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Semantic Minecraft-native icons for augments. Avoids random/procedural glyphs in player UI. */
public final class AugmentIconClient {
    private AugmentIconClient() { }

    public static ItemStack stackFor(String id, AugmentTier tier) {
        String path = id == null ? "" : id.substring(Math.max(0, id.indexOf(':') + 1));
        return switch (path) {
            case "cleanse" -> new ItemStack(Items.MILK_BUCKET);
            case "pull" -> new ItemStack(Items.LEAD);
            case "swap", "phase_shift", "return_home", "spatial_link" -> new ItemStack(Items.ENDER_PEARL);
            case "shield", "shield_throw", "threat_suppression" -> new ItemStack(Items.SHIELD);
            case "berserk", "divine_punishment" -> new ItemStack(Items.DIAMOND_SWORD);
            case "healing_wave", "revive", "death_return" -> new ItemStack(Items.GOLDEN_APPLE);
            case "temporary_platform" -> new ItemStack(Items.SCAFFOLDING);
            case "rewind", "time_acceleration", "time_stop", "area_acceleration", "candidate_reroll" -> new ItemStack(Items.CLOCK);
            case "clone", "domination" -> new ItemStack(Items.ARMOR_STAND);
            case "instant_mining" -> new ItemStack(Items.DIAMOND_PICKAXE);
            case "gigantification" -> new ItemStack(Items.HEAVY_CORE);
            default -> heuristic(path, tier);
        };
    }

    private static ItemStack heuristic(String path, AugmentTier tier) {
        if (contains(path, "mine", "mining", "ore", "stone", "dexterity", "harvest")) return new ItemStack(Items.DIAMOND_PICKAXE);
        if (contains(path, "fish", "water", "swim", "drown")) return new ItemStack(Items.FISHING_ROD);
        if (contains(path, "wood", "log", "tree")) return new ItemStack(Items.DIAMOND_AXE);
        if (contains(path, "farm", "crop", "seed", "harvest")) return new ItemStack(Items.GOLDEN_HOE);
        if (contains(path, "projectile", "arrow", "aim", "bow", "ricochet")) return new ItemStack(Items.BOW);
        if (contains(path, "health", "heal", "life", "survival", "absorption")) return new ItemStack(Items.GOLDEN_APPLE);
        if (contains(path, "armor", "protect", "guard", "damage_reduction", "shield")) return new ItemStack(Items.SHIELD);
        if (contains(path, "speed", "move", "step", "jump", "fall", "dash", "evade")) return new ItemStack(Items.FEATHER);
        if (contains(path, "loot", "abundance", "treasure", "drop", "storage", "pocket")) return new ItemStack(Items.CHEST);
        if (contains(path, "food", "hunger", "eat", "consume")) return new ItemStack(Items.COOKED_BEEF);
        if (contains(path, "enchant", "magic", "resonance")) return new ItemStack(Items.ENCHANTED_BOOK);
        if (contains(path, "tame", "companion", "pet", "revive")) return new ItemStack(Items.BONE);
        if (contains(path, "time", "cooldown", "haste")) return new ItemStack(Items.CLOCK);
        if (contains(path, "fire", "burn", "fuel")) return new ItemStack(Items.BLAZE_POWDER);
        if (contains(path, "experience", "xp")) return new ItemStack(Items.EXPERIENCE_BOTTLE);
        return new ItemStack(AugmentCoreItems.item(tier));
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
