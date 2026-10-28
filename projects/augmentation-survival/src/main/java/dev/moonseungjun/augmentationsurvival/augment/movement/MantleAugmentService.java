package dev.moonseungjun.augmentationsurvival.augment.movement;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.augment.AugmentCatalog;
import dev.moonseungjun.augmentationsurvival.augment.combat.CombatAugmentService;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerStateService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative one-block ledge mantle shared by the tick hook and deterministic tests. */
public final class MantleAugmentService {
    public static final String MANTLE_ID = AugmentationSurvival.MOD_ID + ":mantle";

    private MantleAugmentService() {
    }

    public static boolean tryMantle(ServerPlayer player) {
        if (player.onGround()
                || player.isInWater()
                || player.onClimbable()
                || player.isPassenger()
                || player.getAbilities().flying
                || player.isSpectator()
                || !player.horizontalCollision
                || player.getDeltaMovement().y > 0.20D
                || CombatAugmentService.isActionLocked(player)) {
            return false;
        }

        AugmentationPlayerState state = AugmentationPlayerStateService.state(player);
        if (state.level(MANTLE_ID) <= 0) {
            return false;
        }
        AugmentCatalog.require(MANTLE_ID);

        Vec3 look = player.getLookAngle();
        Vec3 horizontal = new Vec3(look.x, 0.0D, look.z);
        if (horizontal.lengthSqr() < 1.0E-6D) {
            return false;
        }
        horizontal = horizontal.normalize().scale(0.35D);
        AABB destination = player.getBoundingBox().move(horizontal.x, 1.0D, horizontal.z);
        if (!player.level().noCollision(player, destination)) {
            return false;
        }

        player.teleportTo(
                player.getX() + horizontal.x,
                player.getY() + 1.0D,
                player.getZ() + horizontal.z
        );
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, Math.max(0.0D, movement.y), movement.z);
        player.resetFallDistance();
        return true;
    }
}
