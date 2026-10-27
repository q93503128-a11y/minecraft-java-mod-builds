package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuses the existing server-owned stun control state without duplicating AI restoration logic. */
@Mixin(SpecialActiveAugmentService.class)
public interface SpecialActiveAugmentServiceInvoker {
    @Invoker("applyStun")
    static void augmentationSurvival$applyStun(LivingEntity target, int ticks) {
        throw new AssertionError();
    }
}
