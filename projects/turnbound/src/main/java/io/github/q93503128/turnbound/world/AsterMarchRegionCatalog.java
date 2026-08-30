package io.github.q93503128.turnbound.world;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Canonical v0.4 Aster March region bounds and major field/boss anchors. */
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
    private static final List<Anchor> FAST_TRAVEL = List.of(
            new Anchor(FT_RADIA, "라디아 계전소", 0.0, 66.0, 20.0, 180.0F),
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
    public static List<Anchor> fastTravelAnchors() { return FAST_TRAVEL; }
    public static List<Anchor> bossAnchors() { return BOSSES; }

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
}
