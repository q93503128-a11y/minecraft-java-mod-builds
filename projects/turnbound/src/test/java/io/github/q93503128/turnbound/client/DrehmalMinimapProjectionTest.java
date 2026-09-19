package io.github.q93503128.turnbound.client;

import io.github.q93503128.turnbound.world.FieldUiSnapshot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DrehmalMinimapProjectionTest {
    @Test
    void projectsOnlyServerAuthoredNavigationInsideLocalRadius() {
        var navigation = new FieldUiSnapshot.Navigation("route:drabyel", "New Drabyel", 132.0, 164.0);
        var marker = DrehmalMinimapProjection.navigation(navigation, 100.0, 100.0, 80.0);
        assertNotNull(marker);
        assertEquals(32.0, marker.dx(), 0.001);
        assertEquals(64.0, marker.dz(), 0.001);
        assertEquals(72, marker.distance());
        assertEquals("New Drabyel", marker.label());
    }

    @Test
    void hidesDistantOrInactiveTargetsRatherThanInventingLocalMarkers() {
        var distant = new FieldUiSnapshot.Navigation("route:drabyel", "New Drabyel", 500.0, 500.0);
        assertNull(DrehmalMinimapProjection.navigation(distant, 100.0, 100.0, 80.0));
        assertNull(DrehmalMinimapProjection.navigation(FieldUiSnapshot.Navigation.none(), 100.0, 100.0, 80.0));
    }
}
