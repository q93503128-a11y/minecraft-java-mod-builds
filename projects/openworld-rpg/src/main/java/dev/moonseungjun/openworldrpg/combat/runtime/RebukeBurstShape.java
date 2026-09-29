package dev.moonseungjun.openworldrpg.combat.runtime;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared server/client shape binding for Cleric Rebuke.
 *
 * <p>The 4.5 block reach is design canon. Width is an initial production-shape binding kept here so
 * server hit geometry and client presentation cannot drift independently before playtest tuning.</p>
 */
public final class RebukeBurstShape {
    public static final double RANGE_BLOCKS = 4.5;
    public static final double NEAR_HALF_WIDTH_BLOCKS = 0.35;
    public static final double FAR_HALF_WIDTH_BLOCKS = 1.90;
    public static final double BELOW_ORIGIN_BLOCKS = 0.70;
    public static final double ABOVE_ORIGIN_BLOCKS = 1.70;

    private RebukeBurstShape() {
    }

    public static boolean contains(
            LivingEntity caster,
            LivingEntity target
    ) {
        if (caster == target
                || caster.level() != target.level()
                || !target.isAlive()
                || !caster.hasLineOfSight(target)) {
            return false;
        }

        Vec3 origin = caster.position().add(
                0.0,
                caster.getBbHeight() * 0.45,
                0.0
        );
        Vec3 forward = Vec3.directionFromRotation(
                0.0F,
                caster.getYRot()
        );
        forward = new Vec3(
                forward.x,
                0.0,
                forward.z
        ).normalize();
        Vec3 right = new Vec3(
                forward.z,
                0.0,
                -forward.x
        );

        AABB box = target.getBoundingBox();
        Vec3 center = box.getCenter();
        Vec3 delta = center.subtract(origin);
        double horizontalRadius = Math.max(
                box.getXsize(),
                box.getZsize()
        ) * 0.5;

        double localForward = delta.x * forward.x
                + delta.z * forward.z;
        double localSide = delta.x * right.x
                + delta.z * right.z;

        if (localForward + horizontalRadius < 0.0
                || localForward - horizontalRadius > RANGE_BLOCKS) {
            return false;
        }

        double clampedForward = Math.max(
                0.0,
                Math.min(RANGE_BLOCKS, localForward)
        );
        double width = halfWidthAt(clampedForward);
        if (Math.abs(localSide) > width + horizontalRadius) {
            return false;
        }

        double minY = origin.y - BELOW_ORIGIN_BLOCKS;
        double maxY = origin.y + ABOVE_ORIGIN_BLOCKS;
        return box.maxY >= minY && box.minY <= maxY;
    }

    public static double halfWidthAt(double forwardBlocks) {
        double t = Math.max(
                0.0,
                Math.min(1.0, forwardBlocks / RANGE_BLOCKS)
        );
        return NEAR_HALF_WIDTH_BLOCKS
                + (FAR_HALF_WIDTH_BLOCKS
                        - NEAR_HALF_WIDTH_BLOCKS) * t;
    }
}
