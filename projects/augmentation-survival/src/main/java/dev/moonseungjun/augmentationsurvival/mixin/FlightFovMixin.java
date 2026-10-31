package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Speed-readable powered-flight FOV with damping so server velocity updates do not shake the camera. */
@Mixin(Camera.class)
abstract class FlightFovMixin {
    private static float augmentationSurvival$smoothedFlightBonus;

    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float augmentationSurvival$poweredFlightFov(float fov) {
        Minecraft client = Minecraft.getInstance();
        boolean poweredFlight = client.player != null
                && ClientAugmentProgressState.level(FlightAugmentService.FLIGHT_ID) > 0
                && client.player.isNoGravity()
                && !client.player.getAbilities().flying;
        if (!poweredFlight) {
            augmentationSurvival$smoothedFlightBonus = 0.0F;
            return fov;
        }

        double speed = client.player.getDeltaMovement().length();
        float normalized = (float) Mth.clamp((speed - 0.42D) / (2.30D - 0.42D), 0.0D, 1.0D);
        float eased = normalized * normalized * (3.0F - 2.0F * normalized);
        float target = (float) (10.0D * client.options.fovEffectScale().get() * eased);

        // Raw player velocity is server authoritative and changes every flight tick. Feeding it
        // directly into FOV makes tiny correction packets visibly pulse the entire screen. Smooth
        // only the presentation value; movement/collision authority remains entirely server-side.
        augmentationSurvival$smoothedFlightBonus = Mth.lerp(
                0.12F,
                augmentationSurvival$smoothedFlightBonus,
                target
        );
        if (Math.abs(target - augmentationSurvival$smoothedFlightBonus) < 0.01F) {
            augmentationSurvival$smoothedFlightBonus = target;
        }
        return fov + augmentationSurvival$smoothedFlightBonus;
    }
}
