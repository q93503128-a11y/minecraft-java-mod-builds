package dev.moonseungjun.augmentationsurvival.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Speed-readable FOV for powered flight. Respects the player's vanilla FOV-effect scale. */
@Mixin(Camera.class)
abstract class FlightFovMixin {
    @ModifyReturnValue(method = "calculateFov", at = @At("RETURN"))
    private float augmentationSurvival$poweredFlightFov(float fov) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null
                || ClientAugmentProgressState.level(FlightAugmentService.FLIGHT_ID) <= 0
                || !client.player.isNoGravity()
                || client.player.getAbilities().flying) {
            return fov;
        }

        double speed = client.player.getDeltaMovement().length();
        float normalized = (float) Mth.clamp((speed - 0.42D) / (2.30D - 0.42D), 0.0D, 1.0D);
        if (normalized <= 0.0F) return fov;
        float eased = normalized * normalized * (3.0F - 2.0F * normalized);
        double effectScale = client.options.fovEffectScale().get();
        return fov + (float) (10.0D * effectScale * eased);
    }
}
