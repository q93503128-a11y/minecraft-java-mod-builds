package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.combat.ReactiveCombatAugmentService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Retrace used to store the player's feet while collision checks compared against target body centers.
 * Sampling the moving body center makes the authoritative path and living hitboxes occupy the same
 * vertical frame of reference.
 */
@Mixin(value = ReactiveCombatAugmentService.class, remap = false)
abstract class RetracePathCenterMixin {
    @Redirect(
            method = "tickRuntime",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private static Vec3 augmentationSurvival$sampleBodyCenter(ServerPlayer player) {
        return player.getBoundingBox().getCenter();
    }
}
