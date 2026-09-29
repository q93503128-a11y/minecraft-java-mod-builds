package dev.moonseungjun.openworldrpg.combat.runtime;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared server/client geometry constants for Warrior root skills. */
public final class WarriorSkillShape {
    public static final double DRIVING_SLASH_RANGE = 3.5;
    public static final double DRIVING_SLASH_HALF_ANGLE_DEGREES = 55.0;
    public static final double CYCLONE_RADIUS = 3.3;
    public static final double BREAKER_SLAM_RANGE = 4.2;
    public static final double BREAKER_SLAM_HALF_ANGLE_DEGREES = 58.0;
    public static final double EARTHSHATTER_RANGE = 7.0;
    public static final double EARTHSHATTER_HALF_ANGLE_DEGREES = 62.0;
    public static final double BELOW_BLOCKS = 1.25;
    public static final double ABOVE_BLOCKS = 3.50;

    private WarriorSkillShape() {
    }

    public static boolean frontalContains(
            LivingEntity caster,
            LivingEntity target,
            double range,
            double halfAngleDegrees
    ) {
        if (!target.isAlive() || target == caster) {
            return false;
        }
        AABB targetBox = target.getBoundingBox();
        double originY = caster.getY();
        if (targetBox.maxY < originY - BELOW_BLOCKS
                || targetBox.minY > originY + ABOVE_BLOCKS) {
            return false;
        }

        Vec3 origin = caster.position();
        Vec3 center = targetBox.getCenter();
        Vec3 toTarget = new Vec3(
                center.x - origin.x,
                0.0,
                center.z - origin.z
        );
        double distance = toTarget.length();
        if (distance > range + target.getBbWidth() * 0.5
                || distance <= 1.0e-9) {
            return distance <= range;
        }

        Vec3 look = caster.getLookAngle();
        Vec3 flatLook = new Vec3(look.x, 0.0, look.z);
        if (flatLook.lengthSqr() <= 1.0e-9) {
            return false;
        }
        double dot = flatLook.normalize()
                .dot(toTarget.normalize());
        double threshold = Math.cos(
                Math.toRadians(halfAngleDegrees)
        );
        return dot + 1.0e-9 >= threshold;
    }

    public static boolean radialContains(
            Vec3 origin,
            LivingEntity target,
            double radius
    ) {
        if (!target.isAlive()) {
            return false;
        }
        AABB box = target.getBoundingBox();
        if (box.maxY < origin.y - BELOW_BLOCKS
                || box.minY > origin.y + ABOVE_BLOCKS) {
            return false;
        }
        double closestX = Math.max(
                box.minX,
                Math.min(origin.x, box.maxX)
        );
        double closestZ = Math.max(
                box.minZ,
                Math.min(origin.z, box.maxZ)
        );
        double dx = closestX - origin.x;
        double dz = closestZ - origin.z;
        return dx * dx + dz * dz <= radius * radius;
    }
}
