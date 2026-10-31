package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Presents powered flight as one stable forward-facing prone silhouette. Rendering no longer
 * switches pose around a speed threshold or applies the old swim-like whole-body banking.
 */
@Mixin(AvatarRenderer.class)
abstract class FlightAvatarRenderMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void augmentationSurvival$poweredFlightSilhouette(
            Avatar avatar,
            AvatarRenderState state,
            float tickProgress,
            CallbackInfo ci
    ) {
        if (!(avatar instanceof Player player) || !isPoweredFlight(player)) return;

        // Vanilla's gliding transform gives the desired prone body alignment. Keep it active for
        // the entire powered-flight state instead of crossing in and out at low speeds. Disabling
        // flying-yaw correction leaves the body pointed with the avatar rather than drifting into
        // the old swim/bank presentation.
        state.pose = Pose.FALL_FLYING;
        state.isFallFlying = true;
        state.fallFlyingTimeInTicks = Math.max(state.fallFlyingTimeInTicks, 24.0F);
        state.shouldApplyFlyingYRot = false;
        state.flyingYRot = 0.0F;
    }

    private static boolean isPoweredFlight(Player player) {
        boolean localPlayer = player != null && player == Minecraft.getInstance().player;
        return player != null
                && (!localPlayer || ClientAugmentProgressState.level(FlightAugmentService.FLIGHT_ID) > 0)
                && !player.isSpectator()
                && player.isNoGravity()
                && !player.getAbilities().flying;
    }
}
