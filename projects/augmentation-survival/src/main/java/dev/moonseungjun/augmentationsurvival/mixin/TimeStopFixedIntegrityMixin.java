package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.active.SpecialActiveTuning;
import dev.moonseungjun.augmentationsurvival.augment.active.TimeStopBoundaryService;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.integration.EntityCompatibilityProfileService;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the fallback scripted-damage integrity check on the fixed cast-time sphere. */
@Mixin(value = SpecialActiveAugmentService.class, remap = false)
abstract class TimeStopFixedIntegrityMixin {
    @Inject(method = "isHostileSourceInsideActiveTimeStopZone", at = @At("HEAD"), cancellable = true)
    private static void augmentationSurvival$useFixedField(
            Entity sourceEntity,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (sourceEntity == null || sourceEntity.isRemoved()
                || !(sourceEntity.level() instanceof ServerLevel level)) {
            cir.setReturnValue(false);
            return;
        }

        LivingEntity hostileOwner;
        if (sourceEntity instanceof Projectile projectile && projectile.getOwner() instanceof LivingEntity owner) {
            hostileOwner = owner;
        } else if (sourceEntity instanceof LivingEntity living) {
            hostileOwner = living;
        } else {
            cir.setReturnValue(false);
            return;
        }
        if (!EntityCompatibilityProfileService.allowTimeStop(hostileOwner)) {
            cir.setReturnValue(false);
            return;
        }

        MinecraftServer server = level.getServer();
        double radius = SpecialActiveTuning.current().timeStopRadius();
        double radiusSquared = radius * radius;
        for (TimeStopBoundaryService.ActiveField field : TimeStopBoundaryService.activeFields(level)) {
            ServerPlayer caster = server.getPlayerList().getPlayer(field.casterId());
            if (caster == null || caster.isDeadOrDying() || caster.level() != level) continue;
            if (field.center().distanceToSqr(sourceEntity.getBoundingBox().getCenter()) <= radiusSquared
                    && CompanionAugmentService.isHostileTarget(caster, hostileOwner)) {
                cir.setReturnValue(true);
                return;
            }
        }
        cir.setReturnValue(false);
    }
}
