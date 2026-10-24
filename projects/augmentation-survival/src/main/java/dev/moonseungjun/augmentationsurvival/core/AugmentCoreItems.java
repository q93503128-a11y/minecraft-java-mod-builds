package dev.moonseungjun.augmentationsurvival.core;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Static item registry for the six progression-core tiers. */
public final class AugmentCoreItems {
    private static final Map<AugmentTier, Item> BY_TIER = new EnumMap<>(AugmentTier.class);

    public static final Item COMMON_CORE = register(AugmentTier.COMMON);
    public static final Item UNCOMMON_CORE = register(AugmentTier.UNCOMMON);
    public static final Item RARE_CORE = register(AugmentTier.RARE);
    public static final Item EPIC_CORE = register(AugmentTier.EPIC);
    public static final Item LEGENDARY_CORE = register(AugmentTier.LEGENDARY);
    public static final Item MYTHIC_CORE = register(AugmentTier.MYTHIC);

    private AugmentCoreItems() {
    }

    private static Item register(AugmentTier tier) {
        String path = tier.id() + "_augment_core";
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, path)
        );
        Item item = register(key, Item::new, new Item.Properties().stacksTo(64));
        BY_TIER.put(tier, item);
        return item;
    }

    private static <T extends Item> T register(
            ResourceKey<Item> key,
            Function<Item.Properties, T> factory,
            Item.Properties properties
    ) {
        T item = factory.apply(properties.setId(key));
        Registry.register(BuiltInRegistries.ITEM, key, item);
        return item;
    }

    public static Item item(AugmentTier tier) {
        Item item = BY_TIER.get(tier);
        if (item == null) {
            throw new IllegalStateException("Core item requested before registry initialization: " + tier);
        }
        return item;
    }

    public static void initialize() {
        // Static field initialization performs registration.
    }
}
