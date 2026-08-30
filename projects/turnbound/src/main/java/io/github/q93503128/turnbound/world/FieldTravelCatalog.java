package io.github.q93503128.turnbound.world;

import java.util.List;

public final class FieldTravelCatalog {
    public static final String RELAY_A01 = "relay_southgate_meadow";
    public static final String RELAY_A02 = "relay_southroad_foothold";

    public record Destination(String id, String label) {}

    private static final List<Destination> DESTINATIONS = List.of(
            new Destination(RELAY_A01, "남문 초원 계전석"),
            new Destination(RELAY_A02, "남부 도로 거점 계전석")
    );

    private FieldTravelCatalog() {}

    public static List<Destination> destinations() { return DESTINATIONS; }

    public static Destination destination(String id) {
        return DESTINATIONS.stream().filter(destination -> destination.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown field travel destination " + id));
    }
}
