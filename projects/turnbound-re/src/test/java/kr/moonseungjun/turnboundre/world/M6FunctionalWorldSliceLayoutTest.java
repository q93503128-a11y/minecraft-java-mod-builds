package kr.moonseungjun.turnboundre.world;

import kr.moonseungjun.turnboundre.fixtures.ProductionDefinitionFixture;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class M6FunctionalWorldSliceLayoutTest {
    @Test
    void productionSliceResolvesEveryPhysicalGameplayStopAgainstCurrentDefinitions() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        assertEquals(java.util.List.of(), FunctionalWorldSliceLayout.validate(
                registry, FunctionalWorldSliceLayout.DIMENSION));
    }

    @Test
    void sliceKeepsOneReadableRouteWithoutCollidingSiteOffsetsOrLocators() {
        Set<String> positions = new HashSet<>();
        Set<String> locators = new HashSet<>();
        for (FunctionalWorldSliceLayout.Site site : FunctionalWorldSliceLayout.SITES) {
            assertTrue(positions.add(site.offsetX() + "," + site.offsetZ()));
            if (site.authoredLocator()) assertTrue(locators.add(site.locator()));
        }

        assertEquals(FunctionalWorldSliceLayout.SiteKind.FORGE, FunctionalWorldSliceLayout.HUB_FORGE.kind());
        assertEquals(FunctionalWorldSliceLayout.SiteKind.MINING, FunctionalWorldSliceLayout.ORE_OUTCROP.kind());
        assertEquals(FunctionalWorldSliceLayout.SiteKind.FARMING, FunctionalWorldSliceLayout.RIVERSIDE_PLOT.kind());
        assertEquals(FunctionalWorldSliceLayout.SiteKind.FISHING, FunctionalWorldSliceLayout.RIVER_POOL.kind());
        assertTrue(FunctionalWorldSliceLayout.OVERWORLD_PATROL.offsetX() < FunctionalWorldSliceLayout.RIFT_ELITE.offsetX());
        assertTrue(FunctionalWorldSliceLayout.RIVER_POOL.offsetX() > FunctionalWorldSliceLayout.RIVERSIDE_PLOT.offsetX());
    }

    @Test
    void wrongDimensionIsRejectedBeforeAnyWorldMutationCanBegin() throws IOException {
        var registry = ProductionDefinitionFixture.load().registry();
        var errors = FunctionalWorldSliceLayout.validate(registry, "minecraft:the_nether");
        assertTrue(errors.stream().anyMatch(error -> error.contains("requires minecraft:overworld")));
    }
}
