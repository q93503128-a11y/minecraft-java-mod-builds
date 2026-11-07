package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Domination is persistent across dimensions: a player's recruited Piglin
 * or Piglin Brute must not silently turn into an unowned Zombified Piglin
 * after vanilla's 300 overworld ticks. This does not change natural immunity
 * or the original conversion timer once a companion is released.
 */
@Mixin(AbstractPiglin.class)
abstract class DominatedPiglinZombificationMixin {
    @Inject(method = "isImmuneToZombification", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$controlledPiglinKeepsOriginalForm(
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (CompanionAugmentService.isDominated((AbstractPiglin) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
