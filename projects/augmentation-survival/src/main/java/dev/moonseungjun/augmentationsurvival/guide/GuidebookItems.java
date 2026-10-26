package dev.moonseungjun.augmentationsurvival.guide;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Player-facing recipe guide item. */
public final class GuidebookItems {
    private static final ResourceKey<Item> GUIDEBOOK_KEY = ResourceKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "guidebook")
    );

    public static final Item GUIDEBOOK = Registry.register(
            BuiltInRegistries.ITEM,
            GUIDEBOOK_KEY,
            new Item(new Item.Properties().stacksTo(1).setId(GUIDEBOOK_KEY))
    );

    private GuidebookItems() {
    }

    public static void initialize() {
        // Static initialization performs registration.
    }
}
