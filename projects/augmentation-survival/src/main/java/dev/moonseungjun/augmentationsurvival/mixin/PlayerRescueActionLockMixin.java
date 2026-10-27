package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.PlayerRescueAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Downed players cannot attack while waiting for a teammate rescue. */
@Mixin(Player.class)
abstract class PlayerRescueActionLockMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$lockDownedAttack(Entity target, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player && PlayerRescueAugmentService.isActionLocked(player)) {
            ci.cancel();
        }
    }
}
