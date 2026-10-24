package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.survival.DeathPreservationAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents actual-death inventory drops for the keep-inventory augment. */
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
}
