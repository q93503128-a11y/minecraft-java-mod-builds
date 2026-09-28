package io.github.q93503128.turnbound.world;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DrehmalOnboardingFlagsTest {
    @Test
    void flagsCanBeRemovedForOneTimeSaveMigrations() {
        UUID player=UUID.fromString("11111111-1111-1111-1111-111111111111");
        var entries=new LinkedHashSet<String>();
        assertTrue(DrehmalOnboardingFlags.add(entries,player,"ROUTE_DRABYEL_REACHED"));
        assertTrue(DrehmalOnboardingFlags.contains(entries,player,"ROUTE_DRABYEL_REACHED"));
        assertTrue(DrehmalOnboardingFlags.remove(entries,player,"ROUTE_DRABYEL_REACHED"));
        assertFalse(DrehmalOnboardingFlags.contains(entries,player,"ROUTE_DRABYEL_REACHED"));
        assertFalse(DrehmalOnboardingFlags.remove(entries,player,"ROUTE_DRABYEL_REACHED"));
    }
}
