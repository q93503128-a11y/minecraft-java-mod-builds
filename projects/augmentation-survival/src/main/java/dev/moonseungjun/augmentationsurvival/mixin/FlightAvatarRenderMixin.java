package dev.moonseungjun.augmentationsurvival.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.moonseungjun.augmentationsurvival.augment.movement.FlightAugmentService;
import dev.moonseungjun.augmentationsurvival.client.ClientAugmentProgressState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Uses vanilla's gliding body transform for a horizontal powered-flight silhouette, then adds a
 * restrained whole-body bank while the look direction turns across the current velocity vector.
 * This is presentation only: the powered-flight server runtime still owns velocity and collision.
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
        double speed = player.getDeltaMovement().length();
        if (speed < 0.30D) return;

        // setupRotations keys off the explicit gliding render-state boolean, not only Entity#pose.
        // Setting only FALL_FLYING left the powered-flight avatar visually upright in live play.
        state.pose = Pose.FALL_FLYING;
        state.isFallFlying = true;
        state.fallFlyingTimeInTicks = Math.max(state.fallFlyingTimeInTicks, speed >= 1.55D ? 24.0F : 14.0F);
        state.shouldApplyFlyingYRot = false;
    }

    @Inject(method = "setupRotations", at = @At("TAIL"))
    private void augmentationSurvival$poweredFlightBank(
            AvatarRenderState state,
            PoseStack poseStack,
            float bodyRot,
            float scale,
            CallbackInfo ci
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;
        if (!(client.level.getEntity(state.id) instanceof Player player) || !isPoweredFlight(player)) return;

        Vec3 velocity = player.getDeltaMovement();
        double horizontalSpeed = Math.hypot(velocity.x, velocity.z);
        if (horizontalSpeed < 0.30D) return;
        Vec3 look = player.getLookAngle();
        double lookHorizontal = Math.hypot(look.x, look.z);
        if (lookHorizontal < 1.0E-5D) return;

        double vx = velocity.x / horizontalSpeed;
        double vz = velocity.z / horizontalSpeed;
        double lx = look.x / lookHorizontal;
        double lz = look.z / lookHorizontal;
        double cross = lx * vz - lz * vx;
        float speedWeight = (float) Mth.clamp((horizontalSpeed - 0.30D) / 1.70D, 0.0D, 1.0D);
        float bankDegrees = (float) Mth.clamp(cross * 30.0D * speedWeight, -24.0D, 24.0D);
        poseStack.mulPose(Axis.ZP.rotationDegrees(bankDegrees));
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
