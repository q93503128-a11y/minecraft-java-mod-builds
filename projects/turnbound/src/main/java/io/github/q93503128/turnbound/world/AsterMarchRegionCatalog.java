package io.github.q93503128.turnbound.world;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Canonical v0.4 Aster March region bounds, authored seams, and major field/boss anchors. */
public final class AsterMarchRegionCatalog {
    public static final String FT_RADIA = "FT_RADIA";
    public static final String FT_MEADOW = "FT_MEADOW";
    public static final String FT_GLOAM = "FT_GLOAM";
    public static final String FT_AQUEDUCT = "FT_AQUEDUCT";
    public static final String FT_QUARRY = "FT_QUARRY";
    public static final String FT_RELAY = "FT_RELAY";

    public static final String B01 = "B01";
    public static final String B02 = "B02";
    public static final String B03 = "B03";
    public static final String B04 = "B04";
    public static final String B05 = "B05";

    public record Region(String id, String label, int minX, int maxX, int minZ, int maxZ, int minLevel, int maxLevel) {
        public boolean contains(double x, double z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }
    }

    /** X/Z route point used by world-wide guards that must also claim authored roads between region rectangles. */
    public record TransitPoint(double x, double z) {}

    public record TransitCorridor(String id, double radius, List<TransitPoint> points) {
        public TransitCorridor {
            points = List.copyOf(points);
            if (points.size() < 2) throw new IllegalArgumentException("Transit corridor needs at least two points");
            if (!(radius > 0.0)) throw new IllegalArgumentException("Transit corridor radius must be positive");
        }

        public boolean contains(double x, double z) {
            double radiusSq = radius * radius;
            for (int i = 0; i < points.size() - 1; i++) {
                if (distanceSqToSegment(x, z, points.get(i), points.get(i + 1)) <= radiusSq) return true;
            }
            return false;
        }
    }

    /**
     * Primitive canonical anchor. Keeping the static catalog free of Minecraft runtime objects lets pure
     * JUnit regressions validate coordinates without requiring transformed Minecraft classes on the test JVM.
     */
    public record Anchor(String id, String label, double x, double y, double z, float yaw) {
        public Vec3 position() { return new Vec3(x, y, z); }
    }

    /** Primitive mirror used by unit tests that intentionally do not depend on Minecraft runtime classes. */
    public record Point(double x, double y, double z, float yaw) {}

    public static final Region RADIA = new Region("radia", "라디아", -128, 128, -112, 128, 1, 60);
    public static final Region SOUTHGATE = new Region("southgate_meadow", "남문 초원", -80, 430, 120, 360, 1, 6);
    public static final Region GLOAMWOOD = new Region("gloamwood", "그늘숲", -220, 160, -500, -120, 5, 10);
    public static final Region AQUEDUCT = new Region("broken_aqueduct", "붕괴 수로", -500, -130, -170, 210, 8, 13);
    public static final Region QUARRY = new Region("ember_quarry", "잿불 채석장", -160, 210, 300, 500, 11, 16);
    public static final Region OLD_RELAY = new Region("old_relay_station", "구 중계소", 250, 500, -450, -170, 15, 20);

    private static final List<Region> REGIONS = List.of(RADIA, SOUTHGATE, GLOAMWOOD, AQUEDUCT, QUARRY, OLD_RELAY);

    // These mirror the authored AsterMarchWorldShell seam roads. Region rectangles intentionally do not overlap
    // across every gate, so global world guards must explicitly claim these corridors as first-class RPG space.
    private static final List<TransitCorridor> TRANSIT_CORRIDORS = List.of(
            new TransitCorridor("radia_gloam_seam", 6.0, List.of(
                    new TransitPoint(0, -108), new TransitPoint(0, -116), new TransitPoint(-3, -145))),
            new TransitCorridor("radia_aqueduct_seam", 6.0, List.of(
                    new TransitPoint(-124, 20), new TransitPoint(-132, 20), new TransitPoint(-150, 20))),
            new TransitCorridor("southgate_quarry_transit", 18.0, List.of(
                    new TransitPoint(190, 230), new TransitPoint(118, 266), new TransitPoint(42, 286),
                    new TransitPoint(-18, 294), new TransitPoint(-60, 300), new TransitPoint(-86, 307),
                    new TransitPoint(-110, 315))),
            new TransitCorridor("radia_relay_transit", 18.0, List.of(
                    new TransitPoint(124, -80), new TransitPoint(166, -104), new TransitPoint(202, -132),
                    new TransitPoint(232, -156), new TransitPoint(250, -170), new TransitPoint(270, -185)))
    );

    private static final List<Anchor> FAST_TRAVEL = List.of(
            new Anchor(FT_RADIA, "라디아 계전소", 0.0, 76.0, 20.0, 180.0F),
            new Anchor(FT_MEADOW, "남문 초원 계전소", 190.0, 67.0, 230.0, 90.0F),
            new Anchor(FT_GLOAM, "그늘숲 계전소", -40.0, 70.0, -300.0, 180.0F),
            new Anchor(FT_AQUEDUCT, "붕괴 수로 계전소", -320.0, 67.0, 20.0, -90.0F),
            new Anchor(FT_QUARRY, "잿불 채석장 계전소", 20.0, 70.0, 405.0, 0.0F),
            new Anchor(FT_RELAY, "구 중계소 계전소", 365.0, 68.0, -305.0, 90.0F)
    );
    private static final List<Anchor> BOSSES = List.of(
            new Anchor(B01, "들이받는 왕 그라울", 355.0, 68.0, 245.0, 90.0F),
            new Anchor(B02, "가시어미 베르나", -35.0, 72.0, -440.0, 180.0F),
            new Anchor(B03, "수문관리기 ORO-7", -430.0, 64.0, 35.0, -90.0F),
            new Anchor(B04, "재의 거상 콜바크", 65.0, 63.0, 455.0, 0.0F),
            new Anchor(B05, "균열감시자 세라크", 430.0, 66.0, -350.0, 90.0F)
    );

    private AsterMarchRegionCatalog() {}

    public static List<Region> regions() { return REGIONS; }
    public static List<TransitCorridor> transitCorridors() { return TRANSIT_CORRIDORS; }
    public static List<Anchor> fastTravelAnchors() { return FAST_TRAVEL; }
    public static List<Anchor> bossAnchors() { return BOSSES; }

    /** True for any authored chapter rectangle or authored seam road connecting those rectangles. */
    public static boolean containsAuthoredSpace(double x, double z) {
        for (Region region : REGIONS) {
            if (region.contains(x, z)) return true;
        }
        for (TransitCorridor corridor : TRANSIT_CORRIDORS) {
            if (corridor.contains(x, z)) return true;
        }
        return false;
    }

    public static Anchor fastTravel(String id) {
        return FAST_TRAVEL.stream().filter(anchor -> anchor.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown fast travel anchor " + id));
    }

    public static Anchor boss(String id) {
        return BOSSES.stream().filter(anchor -> anchor.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown boss anchor " + id));
    }

    public static Point fastTravelPoint(String id) {
        Anchor anchor = fastTravel(id);
        return new Point(anchor.x(), anchor.y(), anchor.z(), anchor.yaw());
    }

    public static Point bossPoint(String id) {
        Anchor anchor = boss(id);
        return new Point(anchor.x(), anchor.y(), anchor.z(), anchor.yaw());
    }

    private static double distanceSqToSegment(double x, double z, TransitPoint a, TransitPoint b) {
        double dx = b.x() - a.x();
        double dz = b.z() - a.z();
        double lengthSq = dx * dx + dz * dz;
        if (lengthSq <= 1.0e-9) {
            double ox = x - a.x();
            double oz = z - a.z();
            return ox * ox + oz * oz;
        }
        double t = ((x - a.x()) * dx + (z - a.z()) * dz) / lengthSq;
        t = Math.max(0.0, Math.min(1.0, t));
        double px = a.x() + dx * t;
        double pz = a.z() + dz * t;
        double ox = x - px;
        double oz = z - pz;
        return ox * ox + oz * oz;
    }
}
