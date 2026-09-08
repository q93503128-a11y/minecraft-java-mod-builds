package kr.moonseungjun.earthtostars.ship;

import kr.moonseungjun.earthtostars.ship.domain.ShipId;
import kr.moonseungjun.earthtostars.ship.interior.InteriorAssignmentTable;
import kr.moonseungjun.earthtostars.ship.interior.InteriorRef;
import kr.moonseungjun.earthtostars.ship.interior.InteriorSlotLayout;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ShipInteriorTest {
    @Test
    void slotCoordinatesRoundTripAcrossGrid() {
        long[] slots = {0L, 1L, 8191L, 8192L, InteriorSlotLayout.MAX_SLOTS - 1L};
        for (long slot : slots) {
            var anchor = InteriorSlotLayout.anchor(slot);
            assertEquals(slot, InteriorSlotLayout.slotAt(anchor.x(), anchor.z()));
            assertTrue(InteriorSlotLayout.contains(slot, anchor.x(), anchor.z()));
        }
    }

    @Test
    void separateShipsReceiveStableSeparateInteriorCells() {
        ShipId first = new ShipId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        ShipId second = new ShipId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        InteriorAssignmentTable table = InteriorAssignmentTable.empty();

        InteriorRef firstRef = table.getOrAllocate(first);
        InteriorRef secondRef = table.getOrAllocate(second);

        assertNotEquals(firstRef.slot(), secondRef.slot());
        assertEquals(firstRef, table.getOrAllocate(first));
        assertEquals(first, table.findShip(firstRef.slot()).orElseThrow());
        assertEquals(second, table.findShip(secondRef.slot()).orElseThrow());
    }

    @Test
    void persistedSlotCollisionIsRejectedInsteadOfSilentlyRelinkingShips() {
        ShipId first = new ShipId(UUID.fromString("00000000-0000-0000-0000-000000000011"));
        ShipId second = new ShipId(UUID.fromString("00000000-0000-0000-0000-000000000012"));
        Map<ShipId, Long> corrupt = new LinkedHashMap<>();
        corrupt.put(first, 7L);
        corrupt.put(second, 7L);

        assertThrows(IllegalArgumentException.class, () -> InteriorAssignmentTable.restore(corrupt));
    }

    @Test
    void positionOutsideAllocationGridIsRejected() {
        double beyond = InteriorSlotLayout.CELL_SIZE * (InteriorSlotLayout.HALF_GRID + 2.0D);
        assertThrows(IllegalArgumentException.class, () -> InteriorSlotLayout.slotAt(beyond, beyond));
    }
}
