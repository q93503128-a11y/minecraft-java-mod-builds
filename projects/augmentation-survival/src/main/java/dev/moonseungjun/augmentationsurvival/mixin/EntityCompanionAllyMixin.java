package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends vanilla ally checks with permanent domination ownership. */
@Mixin(Entity.class)
abstract class EntityCompanionAllyMixin {
    @Inject(method = "isAlliedTo", at = @At("HEAD"), cancellable = true)
    private void augmentationSurvival$dominatedAlliance(
            Entity other,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Entity self = (Entity) (Object) this;
        if (CompanionAugmentService.areAllied(self, other)) {
            cir.setReturnValue(true);
        }
    }
}
