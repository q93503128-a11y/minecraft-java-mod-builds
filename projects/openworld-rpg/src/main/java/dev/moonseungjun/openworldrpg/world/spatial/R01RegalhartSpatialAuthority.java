package dev.moonseungjun.openworldrpg.world.spatial;

import java.util.List;
import java.util.Objects;
import net.minecraft.world.phys.Vec3;

public final class R01RegalhartSpatialAuthority {
    public static final double MINIMUM_PLAYER_MATERIALIZATION_DISTANCE = 24.0;
    private static final double MINIMUM_PLAYER_DISTANCE_SQUARED = 24.0 * 24.0;

    private R01RegalhartSpatialAuthority() {
    }

    public static boolean insideTerritory(double x, double z) {
        return binding().territory().contains(x, z);
    }

    public static boolean insideCoreArena(double x, double z) {
        return binding().coreArena().contains(x, z);
    }

    public static R01RegalhartSpatialBindingData.StartAnchor selectStartAnchor(
            long worldSeed,
            long cycleIndex
    ) {
        if (cycleIndex < 0L) {
            throw new IllegalArgumentException(
                    "Regalhart cycle index must be non-negative."
            );
        }
        List<R01RegalhartSpatialBindingData.StartAnchor> anchors =
                binding().startAnchors();
        long value = worldSeed ^ (cycleIndex * 31L + 17L);
        int index = Math.floorMod(Long.hashCode(value), anchors.size());
        return anchors.get(index);
    }

    public static boolean materializationAllowed(
            R01RegalhartSpatialBindingData.StartAnchor anchor,
            Iterable<Vec3> activePlayerPositions,
            boolean directlyVisibleToAnyPlayerCamera
    ) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(activePlayerPositions, "activePlayerPositions");
        if (directlyVisibleToAnyPlayerCamera) {
            return false;
        }
        Vec3 spawnCenter = anchor.spawnCenter();
        for (Vec3 playerPosition : activePlayerPositions) {
            Objects.requireNonNull(playerPosition, "playerPosition");
            if (playerPosition.distanceToSqr(spawnCenter)
                    < MINIMUM_PLAYER_DISTANCE_SQUARED) {
                return false;
            }
        }
        return true;
    }

    private static R01RegalhartSpatialBindingData binding() {
        return R01RegalhartSpatialBindingRegistry.data();
    }
}
