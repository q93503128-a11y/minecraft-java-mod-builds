package io.github.q93503128.turnbound.world;

/** Pure scoring rules for source-aware first-route placement. */
final class DrehmalRoutePlacementRules {
    private DrehmalRoutePlacementRules() {}

    static double preferredRoadDistance(String siteKind) {
        return switch (siteKind == null ? "" : siteKind) {
            case "GUIDE_CANDIDATE", "NPC_ZONE" -> 3.5D;
            case "ENCOUNTER_ZONE", "PATROL_ZONE" -> 5.0D;
            case "ELITE_ZONE" -> 7.0D;
            default -> 0.0D;
        };
    }

    static boolean roadAware(String siteKind) {
        return preferredRoadDistance(siteKind) > 0.0D;
    }

    static boolean acceptableRoadDistance(String siteKind, double roadDistance) {
        if (!roadAware(siteKind)) return true;
        if (!Double.isFinite(roadDistance)) return false;
        return roadDistance >= 2.0D && roadDistance <= 18.0D;
    }

    static double score(String siteKind, double seedDistanceSq, double roadDistance) {
        double base = Math.max(0.0D, seedDistanceSq);
        double preferred = preferredRoadDistance(siteKind);
        if (preferred <= 0.0D || !Double.isFinite(roadDistance)) return base;
        double roadError = roadDistance - preferred;
        return base + roadError * roadError * 8.0D;
    }
    static double corridorDistance(DrehmalMapPlacementCatalog.Zone zone, double x, double z) {
        if (zone == null || zone.corridor().isEmpty()) return Double.POSITIVE_INFINITY;
        if (zone.corridor().size() == 1) {
            var point = zone.corridor().getFirst();
            return Math.sqrt(distanceSq(x, z, point.x() + 0.5D, point.z() + 0.5D));
        }
        double best = Double.POSITIVE_INFINITY;
        for (int i = 1; i < zone.corridor().size(); i++) {
            var a = zone.corridor().get(i - 1);
            var b = zone.corridor().get(i);
            best = Math.min(best, distanceToSegment(
                    x, z, a.x() + 0.5D, a.z() + 0.5D, b.x() + 0.5D, b.z() + 0.5D));
        }
        return best;
    }

    private static double distanceToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax;
        double vz = bz - az;
        double lengthSq = vx * vx + vz * vz;
        if (lengthSq <= 0.000001D) return Math.sqrt(distanceSq(px, pz, ax, az));
        double t = ((px - ax) * vx + (pz - az) * vz) / lengthSq;
        t = Math.max(0.0D, Math.min(1.0D, t));
        double qx = ax + t * vx;
        double qz = az + t * vz;
        return Math.sqrt(distanceSq(px, pz, qx, qz));
    }

    private static double distanceSq(double ax, double az, double bx, double bz) {
        double dx = ax - bx;
        double dz = az - bz;
        return dx * dx + dz * dz;
    }

}
