package dev.moonseungjun.openworldrpg.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.world.spatial.R01RegalhartSpatialAuthority;
import dev.moonseungjun.openworldrpg.world.spatial.R01RegalhartSpatialBindingLoader;
import java.util.HashSet;
import java.util.List;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

class R01RegalhartSpatialBindingTest {
    @Test
    void bundledBindingMatchesTheAnvilReviewedRootshadePocket() {
        var data = R01RegalhartSpatialBindingLoader.loadBundled();

        assertEquals(1, data.schemaVersion());
        assertEquals(
                "openworld_rpg:r01/regalhart_spatial_binding",
                data.id()
        );
        assertEquals("AzariNEW4252026", data.mapBuild());
        assertEquals(
                "actual_r01_slice_anvil_runtime",
                data.sourceStatus()
        );

        assertEquals(-2760, data.territory().minX());
        assertEquals(-2600, data.territory().maxX());
        assertEquals(4160, data.territory().minZ());
        assertEquals(4340, data.territory().maxZ());

        assertEquals(-2744, data.coreArena().minX());
        assertEquals(-2600, data.coreArena().maxX());
        assertEquals(4176, data.coreArena().minZ());
        assertEquals(4320, data.coreArena().maxZ());

        assertEquals(3, data.startAnchors().size());

        var southwest = data.startAnchor(
                "openworld_rpg:r01/regalhart/start_southwest"
        );
        assertEquals(-2696, southwest.x());
        assertEquals(65, southwest.y());
        assertEquals(4192, southwest.z());
        assertEquals("grass_block", southwest.surfaceEvidence());

        var center = data.startAnchor(
                "openworld_rpg:r01/regalhart/start_center"
        );
        assertEquals(-2680, center.x());
        assertEquals(68, center.y());
        assertEquals(4240, center.z());

        var east = data.startAnchor(
                "openworld_rpg:r01/regalhart/start_east_ridge"
        );
        assertEquals(-2600, east.x());
        assertEquals(76, east.y());
        assertEquals(4296, east.z());
    }

    @Test
    void allThreeStartAnchorsStayInsideTheAcceptedTerritory() {
        var data = R01RegalhartSpatialBindingLoader.loadBundled();

        for (var anchor : data.startAnchors()) {
            assertTrue(
                    data.territory().contains(anchor.x(), anchor.z())
            );
        }
        assertTrue(data.territory().contains(data.coreArena()));
    }

    @Test
    void worldAndCycleSeedChooseAStableAnchorWithoutReconnectReroll() {
        long worldSeed = 8_772_031_944L;
        var first = R01RegalhartSpatialAuthority.selectStartAnchor(
                worldSeed,
                4L
        );
        var second = R01RegalhartSpatialAuthority.selectStartAnchor(
                worldSeed,
                4L
        );
        assertEquals(first.id(), second.id());

        var seen = new HashSet<String>();
        for (long cycle = 0L; cycle < 96L; cycle++) {
            seen.add(
                    R01RegalhartSpatialAuthority.selectStartAnchor(
                            worldSeed,
                            cycle
                    ).id()
            );
        }
        assertEquals(3, seen.size());
    }

    @Test
    void materializationRejectsCameraAndTheExactTwentyFourBlockExclusion() {
        var anchor = R01RegalhartSpatialBindingLoader.loadBundled()
                .startAnchor(
                        "openworld_rpg:r01/regalhart/start_center"
                );
        Vec3 spawn = anchor.spawnCenter();

        assertFalse(
                R01RegalhartSpatialAuthority.materializationAllowed(
                        anchor,
                        List.of(),
                        true
                )
        );
        assertFalse(
                R01RegalhartSpatialAuthority.materializationAllowed(
                        anchor,
                        List.of(
                                spawn.add(23.999, 0.0, 0.0)
                        ),
                        false
                )
        );
        assertTrue(
                R01RegalhartSpatialAuthority.materializationAllowed(
                        anchor,
                        List.of(
                                spawn.add(24.0, 0.0, 0.0)
                        ),
                        false
                )
        );
    }

    @Test
    void rootshadeBoundsDriveTerritoryAndCorePresenceChecks() {
        assertTrue(
                R01RegalhartSpatialAuthority.insideTerritory(
                        -2680.0,
                        4240.0
                )
        );
        assertFalse(
                R01RegalhartSpatialAuthority.insideTerritory(
                        -2599.9,
                        4240.0
                )
        );
        assertTrue(
                R01RegalhartSpatialAuthority.insideCoreArena(
                        -2680.0,
                        4240.0
                )
        );
        assertFalse(
                R01RegalhartSpatialAuthority.insideCoreArena(
                        -2750.0,
                        4240.0
                )
        );
    }
}
