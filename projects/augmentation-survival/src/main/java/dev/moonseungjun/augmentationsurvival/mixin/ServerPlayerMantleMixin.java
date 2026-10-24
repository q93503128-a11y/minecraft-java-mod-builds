package dev.moonseungjun.augmentationsurvival.mixin;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** One-block ledge grab/mantle using server collision as the authority. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMantleMixin {
    private static final String MANTLE_ID = AugmentationSurvival.MOD_ID + ":mantle";

    @Inject(method = "tick", at = @At("TAIL"))
    private void augmentationSurvival$tryMantle(CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (player.onGround()
                || player.isInWater()
                || player.onClimbable()
                || player.isPassenger()
                || player.getAbilities().flying
                || player.isSpectator()
                || !player.horizontalCollision
                || player.getDeltaMovement().y > 0.20D
                || CombatAugmentService.isActionLocked(player)) {
            return;
        }
        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.level(MANTLE_ID) <= 0) {
            return;
        }
        AugmentCatalog.require(MANTLE_ID);

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return;
        }
        horizontal = horizontal.normalize().scale(0.35D);
        AABB destination = player.getBoundingBox().move(horizontal.x, 1.0D, horizontal.z);
        if (!player.level().noCollision(player, destination)) {
            return;
        }
        player.teleportTo(
                player.getX() + horizontal.x,
                player.getY() + 1.0D,
                player.getZ() + horizontal.z
        );
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.max(0.0D, movement.y), movement.z);
        player.resetFallDistance();
    }
}
