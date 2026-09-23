package dev.moonseungjun.openworldrpg.mixin;

import dev.moonseungjun.openworldrpg.combat.runtime.ProjectRangedProjectileContext;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.BowItem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Captures the final server BowItem launch power after ranged-weapon integrations have resolved
 * pull timing. No donor damage amount is accepted as project authority.
 */
@Mixin(BowItem.class)
public abstract class BowShotAuthorityMixin {
    @Inject(method = "shootProjectile", at = @At("HEAD"))
    private void openworldRpg$captureBowShotPower(
            LivingEntity shooter,
            Projectile projectile,
            int index,
            float power,
            float uncertainty,
            float angle,
            @Nullable LivingEntity targetOverride,
            CallbackInfo ci
    ) {
        ProjectRangedProjectileContext.recordBowShot(shooter, projectile, power);
    }
}
