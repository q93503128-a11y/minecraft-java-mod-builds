package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.AbundanceService;
import dev.moonseungjun.augmentationsurvival.augment.resource.DexterityService;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exact final block-drop boundary for compatible gathering additions and Abundance duplication. */
@Mixin(Block.class)
abstract class BlockAbundanceMixin {
    @Inject(
            method = "popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At("HEAD")
    )
    private static void augmentationSurvival$augmentDirectBlockDrop(
            Level level,
            BlockPos pos,
            ItemStack stack,
            CallbackInfo ci
    ) {
        DexterityService.duplicateActualGatheringDrop(level, pos, stack);
        AbundanceService.duplicateDirectBlockDrop(level, pos, stack);
    }
}
