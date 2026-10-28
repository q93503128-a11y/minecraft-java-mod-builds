package dev.moonseungjun.augmentationsurvival.client;

import dev.moonseungjun.augmentationsurvival.augment.AugmentIconCatalog;
import dev.moonseungjun.augmentationsurvival.core.AugmentTier;
import net.minecraft.world.item.ItemStack;

/** Renders the stable one-icon-per-augment assignment used across all augment UI surfaces. */
public final class AugmentIconClient {
    private AugmentIconClient() { }

    public static ItemStack stackFor(String id, AugmentTier tier) {
        return new ItemStack(AugmentIconCatalog.iconFor(id, tier));
    }
}
