package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Phase Shift disables attack and entity interaction while collision bypass is active. */
@Mixin(Player.class)
abstract class PhaseShiftPlayerActionMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$phaseAttackLock(Entity target, CallbackInfo ci) {
        Object self = this;
        if (self instanceof ServerPlayer player && AdvancedWorldAugmentService.isActionLockedByPhase(player)) {
            ci.cancel();
        }
    }

    @Inject(method = "interactOn", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$phaseEntityInteractLock(
            Entity target,
            InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        Object self = this;
        if (self instanceof ServerPlayer player && AdvancedWorldAugmentService.isActionLockedByPhase(player)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
