package io.github.q93503128.turnbound.world;

import java.util.List;

/** Local P2 checkpoint registry inside the Southgate Meadow development slice. Major v0.4 FT anchors remain separate. */
public final class FieldTravelCatalog {
    public static final String RELAY_A01 = "relay_southgate_entry";
    public static final String RELAY_A02 = "relay_southgate_forward";

    public record Destination(String id, String label) {}

    private static final List<Destination> DESTINATIONS = List.of(
            new Destination(RELAY_A01, "남문 초원 입구 계전석"),
            new Destination(RELAY_A02, "남문 초원 전진 계전석")
    );

    private FieldTravelCatalog() {}
    public static List<Destination> destinations() { return DESTINATIONS; }
    public static Destination destination(String id) {
        return DESTINATIONS.stream().filter(destination -> destination.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown field travel destination " + id));
    }
}
