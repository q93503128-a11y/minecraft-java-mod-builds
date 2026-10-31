package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Reuses existing server-owned control state without duplicating restoration logic. */
@Mixin(SpecialActiveAugmentService.class)
public interface SpecialActiveAugmentServiceInvoker {
    @Invoker("applyStun")
    static void augmentationSurvival$applyStun(LivingEntity target, int ticks) {
        throw new AssertionError();
    }

    @Invoker("applyTimeStop")
    static void augmentationSurvival$applyTimeStop(LivingEntity target, int ticks) {
        throw new AssertionError();
    }

    @Invoker("freezeProjectile")
    static void augmentationSurvival$freezeProjectile(Projectile projectile, int ticks) {
        throw new AssertionError();
    }
}
