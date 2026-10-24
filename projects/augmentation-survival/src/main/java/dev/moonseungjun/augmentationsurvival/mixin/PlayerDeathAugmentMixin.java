package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.survival.DeathPreservationAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.utility.UtilityAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Actual-death inventory and XP preservation boundaries. */
@Mixin(Player.class)
public abstract class PlayerDeathAugmentMixin {
    @Inject(
            method = "dropEquipment(Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void augmentationSurvival$preserveInventory(ServerLevel level, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player
                && DeathPreservationAugmentService.shouldKeepInventory(player)) {
            ci.cancel();
        }
    }

    @ModifyReturnValue(
            method = "getBaseExperienceReward(Lnet/minecraft/server/level/ServerLevel;)I",
            at = @At("RETURN")
    )
    private int augmentationSurvival$reduceDroppedExperience(int original, ServerLevel level) {
        if ((Object) this instanceof ServerPlayer player) {
            return UtilityAugmentService.modifyDeathExperienceReward(player, original);
        }
        return original;
    }
}
