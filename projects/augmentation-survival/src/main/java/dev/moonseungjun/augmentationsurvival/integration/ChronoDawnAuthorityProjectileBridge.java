package dev.moonseungjun.augmentationsurvival.integration;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Runtime-only adapters to Chrono Dawn's original projectile entities (LGPL-3.0).
 * No external mod source or art is embedded; the original registered entity
 * supplies flight, spinning mesh, collision and sound when Chrono Dawn is loaded.
 *
 * Reference: ksoichiro/ChronoDawn,
 * common/26.2/src/main/java/com/chronodawn/entities/projectiles/GearProjectileEntity.java
 */
public final class ChronoDawnAuthorityProjectileBridge {
    private static final String GEAR = "com.chronodawn.entities.projectiles.GearProjectileEntity";
    private static final String TIME_BLAST = "com.chronodawn.entities.projectiles.TimeBlastEntity";
    private static boolean gearWarned;
    private static boolean timeBlastWarned;

    private ChronoDawnAuthorityProjectileBridge() { }

    public static boolean shootGear(ServerPlayer player, LivingEntity target) {
        if (!FabricLoader.getInstance().isModLoaded("chronodawn")) return false;
        if (target.isDeadOrDying() || target.level() != player.level()) return false;
        ServerLevel level = player.level();
        try {
            Class<?> projectileClass = Class.forName(GEAR);
            Constructor<?> constructor = projectileClass.getConstructor(Level.class, LivingEntity.class);
            Object spawned = constructor.newInstance(level, player);
            if (!(spawned instanceof Projectile projectile)) return false;
            projectile.setPos(player.getX(), player.getEyeY() - 0.25D, player.getZ());
            Vec3 offset = target.getBoundingBox().getCenter().subtract(projectile.position());
            double horizontal = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
            Method shoot = projectileClass.getMethod("shoot",
                    double.class, double.class, double.class, float.class, float.class);
            shoot.invoke(projectile, offset.x, offset.y + horizontal * 0.1D, offset.z, 1.5F, 1.0F);
            return level.addFreshEntity((Entity) projectile);
        } catch (ReflectiveOperationException | LinkageError ex) {
            if (!gearWarned) {
                gearWarned = true;
                AugmentationSurvival.LOGGER.warn(
                        "Chrono Dawn 26.2 Gear Projectile adapter unavailable; using vanilla-safe fallback", ex);
            }
            return false;
        }
    }

    /**
     * Both the Time Guardian and Temporal Phantom fire this exact Chrono Dawn entity.
     * Their original Warp Bolt is TimeBlastEntity, not a separately registered projectile.
     */
    public static boolean shootTimeBlast(ServerPlayer player, LivingEntity target) {
        if (!FabricLoader.getInstance().isModLoaded("chronodawn")) return false;
        if (target.isDeadOrDying() || target.level() != player.level()) return false;
        ServerLevel level = player.level();
        try {
            Class<?> projectileClass = Class.forName(TIME_BLAST);
            Constructor<?> constructor = projectileClass.getConstructor(Level.class, LivingEntity.class);
            Object spawned = constructor.newInstance(level, player);
            if (!(spawned instanceof Projectile projectile)) return false;
            Vec3 origin = player.getEyePosition();
            projectile.setPos(origin.x, origin.y, origin.z);
            Vec3 aim = target.getBoundingBox().getCenter().subtract(origin);
            if (aim.lengthSqr() <= 1.0E-6D) return false;
            projectile.setDeltaMovement(aim.normalize().scale(1.5D));
            return level.addFreshEntity((Entity) projectile);
        } catch (ReflectiveOperationException | LinkageError ex) {
            if (!timeBlastWarned) {
                timeBlastWarned = true;
                AugmentationSurvival.LOGGER.warn(
                        "Chrono Dawn 26.2 Time Blast adapter unavailable; using server-safe fallback", ex);
            }
            return false;
        }
    }
}
