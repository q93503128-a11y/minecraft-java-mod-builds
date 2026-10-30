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

/** Captures the amount actually removed by the vanilla item-blocking calculation. */
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

        // 26.2 applyItemBlocking returns the amount actually blocked. The previous live-fix
        // treated it as remaining damage and subtracted it from incomingDamage, which turns a
        // fully blocked hit into zero stored compression and makes Return to Sender appear dead.
        float blocked = cir.getReturnValueF();
        if (blocked > 0.0F && Float.isFinite(blocked)) {
            ShieldCompressionService.onBlockedDamage(player, source, blocked);
        }
    }
}
