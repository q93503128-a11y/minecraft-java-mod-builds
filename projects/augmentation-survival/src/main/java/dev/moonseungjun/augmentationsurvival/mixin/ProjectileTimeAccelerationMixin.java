package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.AdvancedActiveAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Accelerates only projectiles actually owned and launched by an accelerated player. */
@Mixin(Projectile.class)
public abstract class ProjectileTimeAccelerationMixin {
    @ModifyVariable(method = "shoot", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float augmentationSurvival$accelerateOwnedProjectile(float original) {
        Projectile self = (Projectile) (Object) this;
        if (self.getOwner() instanceof ServerPlayer player) {
            return AdvancedActiveAugmentService.modifyOwnedProjectileSpeed(player, original);
        }
        return original;
    }
}
