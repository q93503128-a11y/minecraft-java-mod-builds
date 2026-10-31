package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.moonseungjun.augmentationsurvival.augment.companion.CompanionAugmentService;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Powered-flight body impacts affect ordinary mobs as well as hostiles, while keeping players and
 * allies safe. The flight service still owns damage, provenance, cooldown and collision geometry.
 */
@Mixin(value = FlightAugmentService.class, remap = false)
abstract class FlightCollisionTargetMixin {
    @ModifyArg(
            method = "damageCollisionEnemies",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerLevel;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;",
                    remap = true
            ),
            index = 2
    )
    private static Predicate<LivingEntity> augmentationSurvival$allowNeutralMobImpact(
            Predicate<LivingEntity> original,
            @Local(argsOnly = true) ServerPlayer player
    ) {
        return candidate -> original.test(candidate)
                || (candidate != player
                && !candidate.isDeadOrDying()
                && !(candidate instanceof Player)
                && !CompanionAugmentService.areAllied(player, candidate)
                && !player.isAlliedTo(candidate));
    }
}
