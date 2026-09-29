package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectRangedProjectileContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CrossbowItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures the immutable project combat build for each committed Crossbow projectile. */
@Mixin(CrossbowItem.class)
public abstract class CrossbowShotAuthorityMixin {
    @Inject(method = "shootProjectile", at = @At("HEAD"))
    private void openworldRpg$captureCrossbowShot(
            LivingEntity shooter,
            Projectile projectile,
            int index,
            float power,
            float uncertainty,
            float angle,
            @Nullable LivingEntity targetOverride,
            CallbackInfo ci
    ) {
        ProjectRangedProjectileContext.recordCrossbowShot(shooter, projectile);
    }
}
