package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Powered-flight body impacts affect ordinary mobs as well as hostiles, while keeping players and
 * allies safe. The flight service still owns damage, provenance, cooldown and collision geometry.
 */
@Mixin(value = FlightAugmentService.class, remap = false)
abstract class FlightCollisionTargetMixin {
    @Redirect(
            method = "damageCollisionEnemies",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/moonseungjun/augmentationsurvival/augment/companion/CompanionAugmentService;isHostileTarget(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/LivingEntity;)Z"
            )
    )
    private static boolean augmentationSurvival$allowNeutralMobImpact(Entity source, LivingEntity candidate) {
        return candidate != source
                && !candidate.isDeadOrDying()
                && !(candidate instanceof Player)
                && !CompanionAugmentService.areAllied(source, candidate)
                && (source == null || !source.isAlliedTo(candidate));
    }
}
