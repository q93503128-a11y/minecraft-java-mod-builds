package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.survival.SurvivalAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Player.class)
public abstract class SurvivalPlayerMixin {
    @ModifyReturnValue(method = "getDestroySpeed", at = @At("RETURN"))
    private float augmentationSurvival$modifyDestroySpeed(float original, BlockState state) {
        if ((Object) this instanceof ServerPlayer player) {
            return SurvivalAugmentService.modifyBlockBreakSpeed(player, state, original);
        }
        return original;
    }

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float augmentationSurvival$modifyFoodExhaustion(float original) {
        if ((Object) this instanceof ServerPlayer player) {
            return SurvivalAugmentService.modifyFoodExhaustion(player, original);
        }
        return original;
    }
}
