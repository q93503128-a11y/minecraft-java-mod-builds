package dev.moonseungjun.openworldrpg.combat.runtime;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared authoritative/client footprint for Cleric Consecrated Ground. */
public final class ConsecratedGroundZoneShape {
    public static final double RADIUS_BLOCKS = 5.0;
    public static final double BELOW_ORIGIN_BLOCKS = 1.25;
    public static final double ABOVE_ORIGIN_BLOCKS = 3.50;

    private ConsecratedGroundZoneShape() {
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
