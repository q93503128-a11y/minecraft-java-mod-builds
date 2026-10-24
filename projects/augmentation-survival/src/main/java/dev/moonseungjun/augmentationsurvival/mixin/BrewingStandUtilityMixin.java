package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.component.BrewingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Applies the nearby-holder brewing-speed snapshot through the vanilla speed multiplier. */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandUtilityMixin {
    @ModifyReturnValue(
            method = "getSpeedMultiplier(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/component/BrewingFuel;)F",
            at = @At("RETURN")
    )
    private float augmentationSurvival$accelerateBrewing(
            float original,
            ServerLevel level,
            BrewingFuel brewingFuel
    ) {
        return UtilityAugmentService.modifyBrewingSpeedMultiplier(
                level,
                ((BlockEntity) (Object) this).getBlockPos(),
                original
        );
    }
}
