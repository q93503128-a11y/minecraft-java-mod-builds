package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reflected harmful effects may be mitigated normally, but must never reflect recursively. */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
abstract class StatusReflectRecursionGuardMixin {
    private static final ThreadLocal<Boolean> AUGMENTATION_SURVIVAL_REFLECTING =
            ThreadLocal.withInitial(() -> Boolean.FALSE);

    @WrapOperation(
            method = "interceptIncomingEffect",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/moonseungjun/augmentationsurvival/augment/combat/ReactiveCombatAugmentService;reflectEffect(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;)V"
            )
    )
    private static void augmentationSurvival$preventReflectionLoop(
            ServerPlayer owner,
            LivingEntity target,
            MobEffectInstance incoming,
            Operation<Void> original
    ) {
        if (Boolean.TRUE.equals(AUGMENTATION_SURVIVAL_REFLECTING.get())) {
            return;
        }
        AUGMENTATION_SURVIVAL_REFLECTING.set(Boolean.TRUE);
        try {
            original.call(owner, target, incoming);
        } finally {
            AUGMENTATION_SURVIVAL_REFLECTING.remove();
        }
    }
}
