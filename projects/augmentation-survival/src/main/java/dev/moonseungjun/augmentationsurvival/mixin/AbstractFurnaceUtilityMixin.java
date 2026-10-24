package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies the nearby-holder fuel snapshot at the vanilla furnace burn-duration boundary. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceUtilityMixin {
    @ModifyReturnValue(
            method = "getBurnDuration(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("RETURN")
    )
    private int augmentationSurvival$extendFuelDuration(
            int original,
            ServerLevel level,
            ItemStack fuel
    ) {
        return UtilityAugmentService.modifyFuelBurnDuration(
                level,
                ((BlockEntity) (Object) this).getBlockPos(),
                original
        );
    }
}
