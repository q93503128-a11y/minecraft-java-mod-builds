package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** First-person readability for Last Stand's otherwise easy-to-miss three-second invulnerability window. */
@Mixin(value = CombatAugmentService.class, remap = false)
abstract class LastStandReadabilityMixin {
    @Inject(method = "allowDeath", at = @At("RETURN"))
    private static void augmentationSurvival$announceLastStand(
            LivingEntity entity,
            DamageSource source,
            float amount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValueZ() && entity instanceof ServerPlayer player) {
            player.sendSystemMessage(Component.literal("한 번 버티기 · 3초 무적"), true);
        }
    }
}
