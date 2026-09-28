package dev.moonseungjun.openworldrpg.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules;
import dev.moonseungjun.openworldrpg.progression.r01.R01QuarryRoomEncounterRules.RoomId;
import dev.moonseungjun.openworldrpg.world.spatial.R01QuarrySpatialBindingData;
import dev.moonseungjun.openworldrpg.world.spatial.R01QuarrySpatialBindingLoader;
import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingLoader;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class R01QuarrySpatialBindingDataTest {
    @Test
    void bundledContractBindsAllAuthoredRoomSocketsButExposesNoCandidateGeometry() {
        var binding = R01QuarrySpatialBindingLoader.loadBundled();
        var spatial = R01SpatialBindingLoader.loadBundled();

        assertEquals(1, binding.schemaVersion());
        assertEquals(3, binding.rooms().size());
        assertFalse(binding.productionReady(spatial));

        for (RoomId room : RoomId.values()) {
            assertTrue(binding.room(room).isPresent());
            assertTrue(binding.productionRoom(room, spatial).isEmpty());
        }
        assertTrue(binding.productionRelay(spatial).isEmpty());
        assertTrue(binding.productionEarthloongArena(spatial).isEmpty());
    }

    @Test
    void upperGallerySocketContractMatchesBothCanonicalWaves() {
        var binding = R01QuarrySpatialBindingLoader.loadBundled();
        var upper = binding.room(RoomId.UPPER_GALLERY).orElseThrow();

        var expected = new HashSet<>(
                R01QuarryRoomEncounterRules
                        .initialPlan(RoomId.UPPER_GALLERY, 4)
                        .anchorIds()
        );
        expected.addAll(
                R01QuarryRoomEncounterRules
                        .upperGallerySecondWave(4)
                        .anchorIds()
        );

        assertEquals(expected, new HashSet<>(upper.spawnSocketIds()));
        assertEquals(
                R01QuarryRoomEncounterRules.UPPER_GALLERY_DEEPER_THRESHOLD,
                upper.secondWaveTriggerVolumeId()
        );
    }

    @Test
    void reviewShellsAreProvenanceOnlyAndFinalRuntimeIdsRemainUnbound() {
        var binding = R01QuarrySpatialBindingLoader.loadBundled();
        var spatial = R01SpatialBindingLoader.loadBundled();

        for (var room : binding.rooms()) {
            var review = spatial.volume(room.sourceReviewVolumeId())
                    .orElseThrow();
            assertEquals("candidate", review.status());
            assertFalse(
                    room.sourceReviewVolumeId().equals(room.runtimeVolumeId())
            );
            assertTrue(spatial.volume(room.runtimeVolumeId()).isEmpty());
        }

        assertEquals(
                "solid_carve_probe",
                spatial.volume(
                        binding.earthloongArena().sourceReviewVolumeId()
                ).orElseThrow().reviewMode()
        );
        assertTrue(
                spatial.volume(
                        binding.earthloongArena().runtimeVolumeId()
                ).isEmpty()
        );
        assertEquals(
                R01QuarrySpatialBindingData.EARTHLOONG_RUNTIME,
                binding.earthloongArena().runtimeVolumeId()
        );
    }

    @Test
    void noSemanticEncounterSocketCanLeakAsProductionBeforeAuthoredGeometryExists() {
        var binding = R01QuarrySpatialBindingLoader.loadBundled();
        var spatial = R01SpatialBindingLoader.loadBundled();

        for (var room : binding.rooms()) {
            for (String socket : room.spawnSocketIds()) {
                assertTrue(spatial.productionAnchor(socket).isEmpty());
            }
            if (room.secondWaveTriggerVolumeId() != null) {
                assertTrue(
                        spatial.productionVolume(
                                room.secondWaveTriggerVolumeId()
                        ).isEmpty()
                );
            }
        }
    }
}
