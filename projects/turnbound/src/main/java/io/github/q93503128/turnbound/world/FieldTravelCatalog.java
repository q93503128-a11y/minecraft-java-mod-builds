package io.github.q93503128.turnbound.world;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Player-facing v0.4 fast-travel registry. Development relay aliases are kept for save/test compatibility. */
public final class FieldTravelCatalog {
    public static final String FT_RADIA = AsterMarchRegionCatalog.FT_RADIA;
    public static final String FT_MEADOW = AsterMarchRegionCatalog.FT_MEADOW;
    public static final String FT_GLOAM = AsterMarchRegionCatalog.FT_GLOAM;
    public static final String FT_AQUEDUCT = AsterMarchRegionCatalog.FT_AQUEDUCT;
    public static final String FT_QUARRY = AsterMarchRegionCatalog.FT_QUARRY;
    public static final String FT_RELAY = AsterMarchRegionCatalog.FT_RELAY;

    /** @deprecated alpha.12 local relay id now maps to canonical FT_RADIA. */
    @Deprecated public static final String RELAY_A01 = FT_RADIA;
    /** @deprecated alpha.12 local relay id now maps to canonical FT_MEADOW. */
    @Deprecated public static final String RELAY_A02 = FT_MEADOW;

    /** Primitive static data; Vec3 is materialized only when the live Minecraft runtime asks for position(). */
    public record Destination(String id, String label, double x, double y, double z, float yaw) {
        public Vec3 position() { return new Vec3(x, y, z); }
    }

    private static final List<Destination> DESTINATIONS = List.of(
            fromAnchor(FT_RADIA),
            fromAnchor(FT_MEADOW),
            fromAnchor(FT_GLOAM),
            fromAnchor(FT_AQUEDUCT),
            fromAnchor(FT_QUARRY),
            fromAnchor(FT_RELAY)
    );

    private FieldTravelCatalog() {}

    public static List<Destination> destinations() { return DESTINATIONS; }

    public static boolean contains(String id) {
        return id != null && DESTINATIONS.stream().anyMatch(destination -> destination.id().equals(id));
    }

    public static Destination destination(String id) {
        return DESTINATIONS.stream().filter(destination -> destination.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown field travel destination " + id));
    }

    private static Destination fromAnchor(String id) {
        var anchor = AsterMarchRegionCatalog.fastTravel(id);
        return new Destination(anchor.id(), anchor.label(), anchor.x(), anchor.y(), anchor.z(), anchor.yaw());
    }
}
