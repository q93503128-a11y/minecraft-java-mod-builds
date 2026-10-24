package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents direct attacks during the short reincarnation recovery stasis. */
@Mixin(Player.class)
abstract class PlayerActionLockMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$lockAttack(Entity target, CallbackInfo ci) {
        Object self = this;
        if (self instanceof ServerPlayer player && CombatAugmentService.isActionLocked(player)) {
            ci.cancel();
        }
    }
}
