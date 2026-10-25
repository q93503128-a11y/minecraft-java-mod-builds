package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAugmentAccessor {
    @Accessor("enchantSlots")
    Container augmentationSurvival$getEnchantSlots();

    @Accessor("enchantmentSeed")
    DataSlot augmentationSurvival$getEnchantmentSeedData();
}
