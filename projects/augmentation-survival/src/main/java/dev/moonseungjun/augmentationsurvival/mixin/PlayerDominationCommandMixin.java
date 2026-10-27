package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the real player attack action as the authoritative domination command gesture. */
@Mixin(Player.class)
abstract class PlayerDominationCommandMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void augmentationSurvival$recordDominationCommand(Entity target, CallbackInfo ci) {
        Object self = this;
        if (self instanceof ServerPlayer player
                && !player.isSpectator()
                && target instanceof LivingEntity living
                && !living.isDeadOrDying()) {
            CompanionAugmentService.recordOwnerAttack(player, living);
        }
    }
}
