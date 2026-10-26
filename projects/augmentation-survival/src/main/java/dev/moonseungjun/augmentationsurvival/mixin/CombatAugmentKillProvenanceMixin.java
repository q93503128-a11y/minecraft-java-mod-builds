package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps player kill rewards on the canonical ROOT direct-kill boundary. */
@Mixin(value = dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService.class, remap = false)
abstract class CombatAugmentKillProvenanceMixin {
    @Inject(method = "afterKill", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$guardRootKillEffects(
            ServerLevel level,
            Entity entity,
            LivingEntity killedEntity,
            DamageSource damageSource,
            CallbackInfo ci
    ) {
        if (entity instanceof ServerPlayer
                && !CombatProvenanceContext.resolve(damageSource).isRootDirect()) {
            CombatExpansionService.afterKill(level, entity, killedEntity, damageSource);
            ci.cancel();
        }
    }
}
