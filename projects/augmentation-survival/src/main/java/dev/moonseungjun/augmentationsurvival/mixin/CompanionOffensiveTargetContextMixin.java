package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.OffensiveTargetContext;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets scoped player-offense augments target neutral/non-hostile combatants while preserving ally safety. */
@Mixin(value = CompanionAugmentService.class, remap = false)
abstract class CompanionOffensiveTargetContextMixin {
    @Inject(method = "isHostileTarget", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$useScopedOffensiveTargeting(
            Entity source,
            LivingEntity candidate,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (OffensiveTargetContext.active()) {
            cir.setReturnValue(OffensiveTargetContext.isValid(source, candidate));
        }
    }
}
