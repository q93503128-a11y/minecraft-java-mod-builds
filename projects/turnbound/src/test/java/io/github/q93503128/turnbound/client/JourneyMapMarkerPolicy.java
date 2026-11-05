package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;

/** Pure visibility policy shared by JourneyMap integration and regression tests. */
final class JourneyMapMarkerPolicy {
    private static final String NAVIGATION_PREFIX = "navigation:";

    private JourneyMapMarkerPolicy() {}

    static boolean isCurrentNavigation(FieldUiSnapshot.MapPoint point) {
        return point != null && point.active() && point.objective() && point.id().startsWith(NAVIGATION_PREFIX);
    }

    static boolean showOnMinimap(FieldUiSnapshot.MapPoint point) {
        return isCurrentNavigation(point);
    }
}
