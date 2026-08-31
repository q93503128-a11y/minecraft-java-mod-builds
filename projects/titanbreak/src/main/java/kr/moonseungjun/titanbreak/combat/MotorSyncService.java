package kr.moonseungjun.titanbreak.combat;

import kr.moonseungjun.titanbreak.augmentation.AugmentationCatalog;
import kr.moonseungjun.titanbreak.augmentation.AugmentationResourceService;
import kr.moonseungjun.titanbreak.player.TitanPlayerData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** High-speed steering stabilization for the Motor Sync Core. */
public final class MotorSyncService {
    private MotorSyncService() {}

    public static void tick(ServerPlayer player, TitanPlayerData.State state) {
        TitanPlayerData.AugmentInstance motor = state.firstInstalledInstance("motor_sync_core");
        if (motor == null) return;

        Vec3 motion = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        double speed = horizontal.length();
        boolean driveActive = ReflexDriveService.active(player.getUUID());
        if (speed < 0.26D || (!player.isSprinting() && !driveActive)) return;

        int enhancement = motor.enhancement();
        boolean airborneControl = !player.onGround() && enhancement >= 5;
        boolean wallControl = !player.onGround() && player.horizontalCollision && enhancement >= 7
                && state.hasInstalled("wall_run_spurs");
        boolean driveControl = driveActive && enhancement >= 10;
        if (!airborneControl && !wallControl && !driveControl) return;
        if (!AugmentationResourceService.trySpendContinuousPower(player, state, "motor_sync_core")) return;

        Vec3 look = player.getLookAngle();
        Vec3 desired = new Vec3(look.x, 0.0D, look.z);
        if (desired.lengthSqr() <= 1.0E-6D) return;
        desired = desired.normalize().scale(speed);

        double blend = driveControl ? 0.18D : wallControl ? 0.14D : 0.08D;
        double nx = motion.x + (desired.x - motion.x) * blend;
        double nz = motion.z + (desired.z - motion.z) * blend;
        double ny = motion.y;
        if (wallControl && ny < -0.055D) ny = -0.055D;
        player.setDeltaMovement(nx, ny, nz);
        player.hurtMarked = true;

        if (player.level() instanceof ServerLevel level) {
            TitanPlayerData data = TitanPlayerData.get(level.getServer());
            AugmentationCatalog.Definition definition = AugmentationCatalog.byId("motor_sync_core");
            if (definition != null && definition.heatLoad() > 0) {
                double rawHeat = definition.heatLoad() * 0.012D * state.heatLoadMultiplier("motor_sync_core");
                data.setHeat(player, state.heat() + AugmentationResourceService.normalizedHeatGain(state, rawHeat));
            }
            if (player.tickCount % 40 == 0) data.addMasteryXp(player, "motor_sync_core", 1);
        }
    }
}
