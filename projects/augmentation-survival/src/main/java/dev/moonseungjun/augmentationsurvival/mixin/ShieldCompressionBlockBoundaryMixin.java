package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ShieldCompressionService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures the amount actually removed by the real 26.2 item-blocking path. */
@Mixin(LivingEntity.class)
abstract class ShieldCompressionBlockBoundaryMixin {
    @Inject(method = "applyItemBlocking", at = @At("RETURN"))
    private void augmentationSurvival$captureActuallyBlockedDamage(
            ServerLevel level,
            DamageSource source,
            float incomingDamage,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!((Object) this instanceof ServerPlayer player)) return;

        // This exact Fabric 26.2 runtime is regression-tested through a raised vanilla shield.
        // Its mapped boundary returns damage remaining after item blocking, so derive the removed
        // amount here. Do not replace this with assumptions from a different loader/version.
        float remaining = cir.getReturnValueF();
        float blocked = incomingDamage - remaining;
        if (blocked > 0.0F && Float.isFinite(blocked)) {
            ShieldCompressionService.onBlockedDamage(player, source, blocked);
        }
    }
}
