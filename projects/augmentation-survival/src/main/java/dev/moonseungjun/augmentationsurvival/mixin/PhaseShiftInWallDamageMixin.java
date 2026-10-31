package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedWorldAugmentService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Phase Shift intentionally overlaps solid blocks, so only vanilla in-wall suffocation is invalid. */
@Mixin(ServerPlayer.class)
abstract class PhaseShiftInWallDamageMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$suppressPhaseInWallDamage(
            ServerLevel level,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (AdvancedWorldAugmentService.isPhaseShiftActive(player) && source.is(DamageTypes.IN_WALL)) {
            cir.setReturnValue(false);
        }
    }
}
