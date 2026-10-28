package dev.moonseungjun.augmentationsurvival.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.ItemDisplay.class)
public interface ItemDisplayAugmentAccessor {
    @Invoker("setItemStack")
    void augmentationSurvival$setItemStack(ItemStack stack);

    @Invoker("setItemTransform")
    void augmentationSurvival$setItemTransform(ItemDisplayContext context);
}
