package dev.moonseungjun.openworldrpg.combat.runtime;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Shared server/client footprint for the Cleric Sanctuary ward.
 *
 * <p>The 7-block horizontal radius is design canon. The vertical envelope is an initial production
 * binding so a ground ward cannot affect unrelated floors while still tolerating ordinary terrain
 * steps and actor height. Presentation uses the same radius constant.</p>
 */
public final class SanctuaryZoneShape {
    public static final double RADIUS_BLOCKS = 7.0;
    public static final double BELOW_ORIGIN_BLOCKS = 1.25;
    public static final double ABOVE_ORIGIN_BLOCKS = 3.50;

    private SanctuaryZoneShape() {
    }

    public static AABB bounds(Vec3 origin) {
        return new AABB(
                origin.x - RADIUS_BLOCKS,
                origin.y - BELOW_ORIGIN_BLOCKS,
                origin.z - RADIUS_BLOCKS,
                origin.x + RADIUS_BLOCKS,
                origin.y + ABOVE_ORIGIN_BLOCKS,
                origin.z + RADIUS_BLOCKS
        );
    }

    public static boolean contains(Vec3 origin, LivingEntity target) {
        if (!target.isAlive()) {
            return false;
        }

        AABB box = target.getBoundingBox();
        if (box.maxY < origin.y - BELOW_ORIGIN_BLOCKS
                || box.minY > origin.y + ABOVE_ORIGIN_BLOCKS) {
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
        return dx * dx + dz * dz
                <= RADIUS_BLOCKS * RADIUS_BLOCKS;
    }
}
