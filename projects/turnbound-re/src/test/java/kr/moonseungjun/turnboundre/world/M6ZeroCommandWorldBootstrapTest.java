package kr.moonseungjun.turnboundre.world;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M6ZeroCommandWorldBootstrapTest {
    private static final FastTravelSavedData.AnchorLocation HUB =
            new FastTravelSavedData.AnchorLocation("minecraft:overworld", 0, 64, 0);
    private static final FastTravelSavedData.AnchorLocation REGION =
            new FastTravelSavedData.AnchorLocation("minecraft:overworld", 64, 64, 0);

    @Test
    void authoredWorldInstallsOnlyUntilBothCanonicalWaypointsExist() {
        assertTrue(AuthoredWorldBootstrapService.needsWorldInstall(Map.of()));
        assertTrue(AuthoredWorldBootstrapService.needsWorldInstall(Map.of(
                WorldFastTravelPrototype.HUB_LOCATOR, HUB)));
        assertTrue(AuthoredWorldBootstrapService.needsWorldInstall(Map.of(
                WorldFastTravelPrototype.REGION_LOCATOR, REGION)));
        assertFalse(AuthoredWorldBootstrapService.needsWorldInstall(Map.of(
                WorldFastTravelPrototype.HUB_LOCATOR, HUB,
                WorldFastTravelPrototype.REGION_LOCATOR, REGION)));
    }

    @Test
    void hubDiscoveryIsThePerPlayerNaturalStartMarker() {
        assertTrue(AuthoredWorldBootstrapService.needsInitialHubArrival(Set.of()));
        assertTrue(AuthoredWorldBootstrapService.needsInitialHubArrival(Set.of(
                WorldFastTravelPrototype.REGION_LOCATOR)));
        assertFalse(AuthoredWorldBootstrapService.needsInitialHubArrival(Set.of(
                WorldFastTravelPrototype.HUB_LOCATOR)));
        assertFalse(AuthoredWorldBootstrapService.needsInitialHubArrival(Set.of(
                WorldFastTravelPrototype.HUB_LOCATOR,
                WorldFastTravelPrototype.REGION_LOCATOR)));
    }

    @Test
    void trustedPackMarkerAcceptsOnlyThePinnedExternalWorldProfile() {
        assertTrue(DrehmalExternalWorldBinding.markerMatches(
                DrehmalExternalWorldBinding.PROFILE_ID));
        assertTrue(DrehmalExternalWorldBinding.markerMatches(
                "  " + DrehmalExternalWorldBinding.PROFILE_ID + "\r\n"));
        assertFalse(DrehmalExternalWorldBinding.markerMatches(null));
        assertFalse(DrehmalExternalWorldBinding.markerMatches(""));
        assertFalse(DrehmalExternalWorldBinding.markerMatches("turnbound_re:some_other_world"));
    }
}
