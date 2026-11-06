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
        // A dominated illager/piglin/etc is no longer part of its former hostile faction.
        // Opposing controlled/uncontrolled enemies may attack each other; owner bonds stay intact.
        if (CompanionAugmentService.isDominated(self) != CompanionAugmentService.isDominated(other)
                && self instanceof net.minecraft.world.entity.monster.Enemy
                && other instanceof net.minecraft.world.entity.monster.Enemy) {
            cir.setReturnValue(false);
        } else if (CompanionAugmentService.areAllied(self, other)) {
            cir.setReturnValue(true);
        }
    }
}
