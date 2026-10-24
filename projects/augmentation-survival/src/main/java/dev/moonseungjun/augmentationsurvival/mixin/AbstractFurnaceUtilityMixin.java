package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.FuelValues;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies the nearby-holder fuel snapshot at the vanilla furnace burn-duration boundary. */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class AbstractFurnaceUtilityMixin {
    @ModifyReturnValue(
            method = "getBurnDuration(Lnet/minecraft/world/level/block/entity/FuelValues;Lnet/minecraft/world/item/ItemStack;)I",
            at = @At("RETURN")
    )
    private int augmentationSurvival$extendFuelDuration(
            int original,
            FuelValues fuelValues,
            ItemStack fuel
    ) {
        BlockEntity self = (BlockEntity) (Object) this;
        if (!(self.getLevel() instanceof ServerLevel level)) {
            return original;
        }
        return UtilityAugmentService.modifyFuelBurnDuration(level, self.getBlockPos(), original);
    }
}
