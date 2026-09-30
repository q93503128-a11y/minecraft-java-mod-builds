package dev.moonseungjun.openworldrpg.integration.actor;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public record ExternalActorWeakPointProfile(List<Zone> zones) {
    public ExternalActorWeakPointProfile {
        zones = List.copyOf(Objects.requireNonNull(zones, "zones"));
        for (Zone zone : zones) {
            Objects.requireNonNull(zone, "weak-point zone");
        }
    }

    public static ExternalActorWeakPointProfile none() {
        return new ExternalActorWeakPointProfile(List.of());
    }

    public static ExternalActorWeakPointProfile of(Zone... zones) {
        Objects.requireNonNull(zones, "zones");
        return new ExternalActorWeakPointProfile(List.of(zones));
    }

    public boolean isEmpty() {
        return zones.isEmpty();
    }

    public boolean contains(LivingEntity target, Vec3 hitPosition) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(hitPosition, "hitPosition");
        if (zones.isEmpty()) return false;
        double halfWidth = Math.max(1.0e-6, target.getBbWidth() * 0.5);
        double height = Math.max(1.0e-6, target.getBbHeight());
        Vec3 center = target.position();
        double dx = hitPosition.x - center.x;
        double dz = hitPosition.z - center.z;
        double yaw = Math.toRadians(target.getYRot());
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double localX = (dx * cos + dz * sin) / halfWidth;
        double localZ = (-dx * sin + dz * cos) / halfWidth;
        double normalizedY =
                (hitPosition.y - target.getBoundingBox().minY) / height;
        return containsNormalized(localX, normalizedY, localZ);
    }

    public boolean containsNormalized(double localX, double normalizedY, double localZ) {
        if (!Double.isFinite(localX)
                || !Double.isFinite(normalizedY)
                || !Double.isFinite(localZ)) {
            return false;
        }
        for (Zone zone : zones) {
            if (zone.contains(localX, normalizedY, localZ)) return true;
        }
        return false;
    }

    public record Zone(
            double minX, double maxX,
            double minY, double maxY,
            double minZ, double maxZ
    ) {
        public Zone {
            requireFinite("minX", minX);
            requireFinite("maxX", maxX);
            requireFinite("minY", minY);
            requireFinite("maxY", maxY);
            requireFinite("minZ", minZ);
            requireFinite("maxZ", maxZ);
            if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException(
                        "Weak-point zone minimums must be below maximums."
                );
            }
        }

        public boolean contains(double x, double y, double z) {
            return x >= minX && x <= maxX
                    && y >= minY && y <= maxY
                    && z >= minZ && z <= maxZ;
        }

        private static void requireFinite(String name, double value) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException(name + " must be finite.");
            }
        }
    }
}
