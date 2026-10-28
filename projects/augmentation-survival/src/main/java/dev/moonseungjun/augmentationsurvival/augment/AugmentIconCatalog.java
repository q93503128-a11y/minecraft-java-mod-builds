package dev.moonseungjun.augmentationsurvival.augment;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Stable one-icon-per-augment assignment shared by every player-facing augment surface. */
public final class AugmentIconCatalog {
    private static volatile Map<String, Item> canonical;

    private AugmentIconCatalog() { }

    public static Item iconFor(String id, AugmentTier tier) {
        Item item = canonicalIcons().get(id);
        return item != null ? item : AugmentCoreItems.item(tier);
    }

    public static Map<String, Item> canonicalIcons() {
        Map<String, Item> current = canonical;
        if (current != null) return current;
        synchronized (AugmentIconCatalog.class) {
            current = canonical;
            if (current == null) {
                current = build();
                canonical = current;
            }
        }
        return current;
    }

    private static Map<String, Item> build() {
        List<AugmentDefinition> definitions = AugmentCatalog.allDefinitions();
        LinkedHashMap<String, Item> assigned = new LinkedHashMap<>();
        Set<Item> used = new HashSet<>();

        // Keep the most interaction-heavy augments immediately recognizable.
        for (AugmentDefinition definition : definitions) {
            Item preferred = preferredIcon(path(definition.id()));
            if (preferred != null && used.add(preferred)) {
                assigned.put(definition.id(), preferred);
            }
        }

        List<Item> vanillaPool = new ArrayList<>();
        BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .filter(item -> "minecraft".equals(BuiltInRegistries.ITEM.getKey(item).getNamespace()))
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .forEach(vanillaPool::add);

        int cursor = 0;
        for (AugmentDefinition definition : definitions) {
            if (assigned.containsKey(definition.id())) continue;
            while (cursor < vanillaPool.size() && used.contains(vanillaPool.get(cursor))) cursor++;
            if (cursor >= vanillaPool.size()) {
                throw new IllegalStateException("Not enough distinct vanilla items for the canonical augment icon roster");
            }
            Item item = vanillaPool.get(cursor++);
            used.add(item);
            assigned.put(definition.id(), item);
        }

        if (assigned.size() != definitions.size() || used.size() != definitions.size()) {
            throw new IllegalStateException("Canonical augment icons must be one-to-one; augments="
                    + definitions.size() + ", assigned=" + assigned.size() + ", unique=" + used.size());
        }
        return Map.copyOf(assigned);
    }

    private static Item preferredIcon(String path) {
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

    private static String path(String id) {
        int separator = id.indexOf(':');
        return separator >= 0 ? id.substring(separator + 1) : id;
    }
}
