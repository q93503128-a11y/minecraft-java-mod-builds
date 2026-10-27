package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.resource.AbundanceService;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Final block-drop list boundary after every block-specific getDrops implementation has completed. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateAbundanceMixin {
    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void augmentationSurvival$duplicateAbundanceDrops(
            LootParams.Builder params,
            CallbackInfoReturnable<List<ItemStack>> cir
    ) {
        List<ItemStack> original = cir.getReturnValue();
        List<ItemStack> augmented = AbundanceService.applyDirectBlockDrops(params, original);
        if (augmented != original) {
            cir.setReturnValue(augmented);
        }
    }
}
