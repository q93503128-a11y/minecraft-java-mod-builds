package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.CombatExpansionService;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatProvenanceContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Preserves target-runtime cleanup while stopping derived kills before ROOT-only expansion effects. */
@Mixin(value = CombatExpansionService.class, remap = false)
abstract class CombatExpansionKillProvenanceMixin {
    @Inject(
            method = "afterKill",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/moonseungjun/augmentationsurvival/augment/combat/CombatExpansionService;removeTargetRuntime(Ljava/util/UUID;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private static void augmentationSurvival$guardRootKillEffects(
            ServerLevel level,
            Entity killer,
            LivingEntity killed,
            DamageSource source,
            CallbackInfo ci
    ) {
        if (!CombatProvenanceContext.resolve(source).isRootDirect()) {
            ci.cancel();
        }
    }
}
