package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.AugmentDefinition;
import dev.moonseungjun.augmentationsurvival.core.AugmentCoreItems;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Stable, globally unique Minecraft-native icons for the canonical augment roster. */
public final class AugmentIconClient {
    private static final Set<String> FALLBACK_BLACKLIST = Set.of(
            "air", "barrier", "light", "structure_void", "debug_stick", "knowledge_book",
            "command_block", "chain_command_block", "repeating_command_block", "jigsaw", "structure_block"
    );
    private static volatile Map<String, Item> assignments;

    private AugmentIconClient() { }

    public static void initialize() {
        assignments();
    }

    public static ItemStack stackFor(String id, AugmentTier tier) {
        return new ItemStack(itemFor(id, tier));
    }

    public static Item itemFor(String id, AugmentTier tier) {
        Item mapped = assignments().get(id);
        return mapped != null ? mapped : AugmentCoreItems.item(tier);
    }

    private static Map<String, Item> assignments() {
        Map<String, Item> current = assignments;
        if (current != null) return current;
        synchronized (AugmentIconClient.class) {
            current = assignments;
            if (current == null) {
                current = buildAssignments();
                assignments = current;
            }
            return current;
        }
    }

    private static Map<String, Item> buildAssignments() {
        List<AugmentDefinition> definitions = AugmentCatalog.allDefinitions();
        Map<String, Item> result = new LinkedHashMap<>();
        Set<Item> used = new HashSet<>();

        for (AugmentDefinition definition : definitions) {
            Item item = explicit(path(definition.id()));
            if (item == null) continue;
            if (!used.add(item)) {
                throw new IllegalStateException("Duplicate explicit augment icon item: " + BuiltInRegistries.ITEM.getKey(item));
            }
            result.put(definition.id(), item);
        }

        for (AugmentDefinition definition : definitions) {
            if (result.containsKey(definition.id())) continue;
            Item preferred = choosePreferred(path(definition.id()), used);
            if (preferred != null) {
                used.add(preferred);
                result.put(definition.id(), preferred);
            }
        }

        List<Item> fallback = vanillaFallbackPool();
        int fallbackIndex = 0;
        for (AugmentDefinition definition : definitions) {
            if (result.containsKey(definition.id())) continue;
            while (fallbackIndex < fallback.size() && used.contains(fallback.get(fallbackIndex))) fallbackIndex++;
            if (fallbackIndex >= fallback.size()) {
                throw new IllegalStateException("Not enough distinct vanilla item icons for all augments");
            }
            Item item = fallback.get(fallbackIndex++);
            used.add(item);
            result.put(definition.id(), item);
        }

        if (result.size() != definitions.size() || used.size() != definitions.size()) {
            throw new IllegalStateException(
                    "Augment icon uniqueness contract failed: definitions=" + definitions.size()
                            + ", assignments=" + result.size() + ", uniqueItems=" + used.size()
            );
        }
        return Map.copyOf(result);
    }

    private static Item explicit(String path) {
        return switch (path) {
            case "cleanse" -> Items.MILK_BUCKET;
            case "pull" -> Items.LEAD;
            case "swap" -> Items.ENDER_PEARL;
            case "phase_shift" -> Items.CHORUS_FRUIT;
            case "return_home" -> Items.COMPASS;
            case "spatial_link" -> Items.ENDER_EYE;
            case "shield" -> Items.SHIELD;
            case "shield_throw" -> Items.TRIDENT;
            case "threat_suppression" -> Items.SOUL_TORCH;
            case "berserk" -> Items.DIAMOND_SWORD;
            case "divine_punishment" -> Items.NETHER_STAR;
            case "healing_wave" -> Items.GLISTERING_MELON_SLICE;
            case "revive" -> Items.TOTEM_OF_UNDYING;
            case "death_return" -> Items.RECOVERY_COMPASS;
            case "temporary_platform" -> Items.SCAFFOLDING;
            case "rewind" -> Items.CLOCK;
            case "time_acceleration" -> Items.SUGAR;
            case "time_stop" -> Items.ECHO_SHARD;
            case "area_acceleration" -> Items.BONE_MEAL;
            case "candidate_reroll" -> Items.AMETHYST_SHARD;
            case "clone" -> Items.ARMOR_STAND;
            case "domination" -> Items.NAME_TAG;
            case "instant_mining" -> Items.NETHERITE_PICKAXE;
            case "gigantification" -> Items.HEAVY_CORE;
            case "flight" -> Items.ELYTRA;
            case "authority_theft" -> Items.DRAGON_BREATH;
            default -> null;
        };
    }

    private static Item choosePreferred(String path, Set<Item> used) {
        if (contains(path, "mine", "mining", "ore", "stone", "dexterity", "harvest")) {
            Item item = firstUnused(used,
                    "diamond_pickaxe", "iron_pickaxe", "golden_pickaxe", "stone_pickaxe", "wooden_pickaxe",
                    "raw_iron", "raw_gold", "raw_copper", "coal", "redstone", "lapis_lazuli", "diamond", "emerald");
            if (item != null) return item;
        }
        if (contains(path, "fish", "water", "swim", "drown", "riptide")) {
            Item item = firstUnused(used,
                    "fishing_rod", "cod", "salmon", "tropical_fish", "pufferfish", "nautilus_shell",
                    "heart_of_the_sea", "prismarine_shard", "prismarine_crystals", "water_bucket");
            if (item != null) return item;
        }
        if (contains(path, "wood", "log", "tree")) {
            Item item = firstUnused(used,
                    "diamond_axe", "iron_axe", "golden_axe", "stone_axe", "wooden_axe", "oak_log", "spruce_log",
                    "birch_log", "jungle_log", "acacia_log", "dark_oak_log", "mangrove_log", "cherry_log");
            if (item != null) return item;
        }
        if (contains(path, "farm", "crop", "seed", "replant")) {
            Item item = firstUnused(used,
                    "diamond_hoe", "iron_hoe", "golden_hoe", "stone_hoe", "wooden_hoe", "wheat", "carrot",
                    "potato", "beetroot", "pumpkin", "melon_slice", "wheat_seeds", "beetroot_seeds");
            if (item != null) return item;
        }
        if (contains(path, "projectile", "arrow", "aim", "bow", "ricochet", "shot")) {
            Item item = firstUnused(used,
                    "bow", "crossbow", "arrow", "spectral_arrow", "target", "firework_rocket", "wind_charge");
            if (item != null) return item;
        }
        if (contains(path, "health", "heal", "life", "recovery", "survival", "absorption", "lifesteal")) {
            Item item = firstUnused(used,
                    "golden_apple", "enchanted_golden_apple", "honey_bottle", "potion", "rabbit_stew",
                    "mushroom_stew", "suspicious_stew", "cooked_beef", "cooked_chicken");
            if (item != null) return item;
        }
        if (contains(path, "armor", "protect", "guard", "damage", "brace", "skin")) {
            Item item = firstUnused(used,
                    "turtle_helmet", "chainmail_chestplate", "iron_chestplate", "golden_chestplate", "diamond_chestplate",
                    "netherite_chestplate", "iron_helmet", "diamond_helmet", "netherite_helmet");
            if (item != null) return item;
        }
        if (contains(path, "speed", "move", "step", "jump", "fall", "dash", "evade", "feet", "momentum", "mantle")) {
            Item item = firstUnused(used,
                    "feather", "rabbit_foot", "leather_boots", "chainmail_boots", "iron_boots", "golden_boots",
                    "diamond_boots", "netherite_boots", "slime_ball");
            if (item != null) return item;
        }
        if (contains(path, "loot", "abundance", "treasure", "drop", "storage", "pocket", "chest")) {
            Item item = firstUnused(used,
                    "chest", "barrel", "bundle", "ender_chest", "shulker_box", "spyglass", "map", "emerald_block");
            if (item != null) return item;
        }
        if (contains(path, "food", "hunger", "eat", "meal", "metabolism")) {
            Item item = firstUnused(used,
                    "bread", "baked_potato", "cooked_porkchop", "cooked_mutton", "cooked_rabbit", "pumpkin_pie",
                    "cookie", "cake", "dried_kelp");
            if (item != null) return item;
        }
        if (contains(path, "enchant", "magic", "resonance", "ability")) {
            Item item = firstUnused(used,
                    "enchanted_book", "book", "lapis_block", "amethyst_block", "end_crystal", "blaze_rod", "sculk_catalyst");
            if (item != null) return item;
        }
        if (contains(path, "tame", "companion", "pet", "rider", "mount")) {
            Item item = firstUnused(used,
                    "bone", "saddle", "carrot_on_a_stick", "warped_fungus_on_a_stick", "wolf_armor", "hay_block");
            if (item != null) return item;
        }
        if (contains(path, "time", "cooldown", "haste", "retrace", "delayed")) {
            Item item = firstUnused(used,
                    "redstone_torch", "repeater", "comparator", "daylight_detector", "sculk_sensor", "calibrated_sculk_sensor");
            if (item != null) return item;
        }
        if (contains(path, "fire", "burn", "fuel", "explosion", "lightning")) {
            Item item = firstUnused(used,
                    "blaze_powder", "fire_charge", "lava_bucket", "magma_cream", "tnt", "lightning_rod");
            if (item != null) return item;
        }
        if (contains(path, "experience", "xp")) {
            Item item = firstUnused(used, "experience_bottle", "sculk", "sculk_shrieker", "sculk_vein");
            if (item != null) return item;
        }
        return null;
    }

    private static Item firstUnused(Set<Item> used, String... paths) {
        for (String path : paths) {
            Item item = vanilla(path);
            if (item != null && item != Items.AIR && !used.contains(item)) return item;
        }
        return null;
    }

    private static List<Item> vanillaFallbackPool() {
        List<Identifier> ids = new ArrayList<>();
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (!"minecraft".equals(id.getNamespace()) || FALLBACK_BLACKLIST.contains(id.getPath())) continue;
            ids.add(id);
        }
        ids.sort(Comparator.comparing(Identifier::toString));
        List<Item> result = new ArrayList<>(ids.size());
        for (Identifier id : ids) {
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null && item != Items.AIR) result.add(item);
        }
        return List.copyOf(result);
    }

    private static Item vanilla(String path) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", path));
    }

    private static boolean contains(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }

    private static String path(String id) {
        if (id == null) return "";
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }
}
