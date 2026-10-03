package dev.moonseungjun.openworldrpg.world.spatial;

import com.google.gson.annotations.SerializedName;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.world.phys.Vec3;

/**
 * Dedicated accepted spatial binding for the R01 Regalhart field-boss controller.
 *
 * <p>The source coordinates come from the already-parsed Azari R01 Anvil slice. This binding is
 * intentionally separate from the broader candidate-only R01 spatial dataset so accepting Regalhart
 * terrain does not accidentally promote unrelated Alderford/Quarry candidates.</p>
 */
public record R01RegalhartSpatialBindingData(
        @SerializedName("schema_version") int schemaVersion,
        String id,
        @SerializedName("map_build") String mapBuild,
        @SerializedName("source_status") String sourceStatus,
        Bounds territory,
        @SerializedName("core_arena") Bounds coreArena,
        @SerializedName("start_anchors") List<StartAnchor> startAnchors
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String CANONICAL_ID =
            "openworld_rpg:r01/regalhart_spatial_binding";
    public static final String CANONICAL_MAP_BUILD = "AzariNEW4252026";
    public static final String ANVIL_RUNTIME_SOURCE_STATUS =
            "actual_r01_slice_anvil_runtime";

    public R01RegalhartSpatialBindingData {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(mapBuild, "mapBuild");
        Objects.requireNonNull(sourceStatus, "sourceStatus");
        Objects.requireNonNull(territory, "territory");
        Objects.requireNonNull(coreArena, "coreArena");
        startAnchors = List.copyOf(
                Objects.requireNonNull(startAnchors, "startAnchors")
        );
    }

    public StartAnchor startAnchor(String anchorId) {
        Objects.requireNonNull(anchorId, "anchorId");
        return startAnchors.stream()
                .filter(anchor -> anchor.id().equals(anchorId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown Regalhart start anchor: " + anchorId
                ));
    }

    public record Bounds(
            String id,
            @SerializedName("min_x") int minX,
            @SerializedName("max_x") int maxX,
            @SerializedName("min_z") int minZ,
            @SerializedName("max_z") int maxZ,
            String role
    ) {
        public Bounds {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(role, "role");
        }

        public boolean contains(double x, double z) {
            return x >= minX
                    && x <= maxX
                    && z >= minZ
                    && z <= maxZ;
        }

        public boolean contains(Bounds other) {
            Objects.requireNonNull(other, "other");
            return other.minX >= minX
                    && other.maxX <= maxX
                    && other.minZ >= minZ
                    && other.maxZ <= maxZ;
        }
    }

    public record StartAnchor(
            String id,
            int x,
            int y,
            int z,
            @SerializedName("surface_evidence") String surfaceEvidence,
            String role
    ) {
        public StartAnchor {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(surfaceEvidence, "surfaceEvidence");
            Objects.requireNonNull(role, "role");
        }

        /**
         * Spawn-center convention: the stored Y is the inspected support/surface block.
         */
        public Vec3 spawnCenter() {
            return new Vec3(x + 0.5, y + 1.0, z + 0.5);
        }
    }

    public static void validate(R01RegalhartSpatialBindingData data) {
        Objects.requireNonNull(data, "data");
        if (data.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unexpected Regalhart spatial schema version: "
                            + data.schemaVersion()
            );
        }
        if (!CANONICAL_ID.equals(data.id())) {
            throw new IllegalArgumentException(
                    "Unexpected Regalhart spatial id: " + data.id()
            );
        }
        if (!CANONICAL_MAP_BUILD.equals(data.mapBuild())) {
            throw new IllegalArgumentException(
                    "Unexpected Regalhart Azari map build: "
                            + data.mapBuild()
            );
        }
        if (!ANVIL_RUNTIME_SOURCE_STATUS.equals(data.sourceStatus())) {
            throw new IllegalArgumentException(
                    "Unexpected Regalhart spatial source status: "
                            + data.sourceStatus()
            );
        }

        validateBounds(data.territory(), "territory");
        validateBounds(data.coreArena(), "core arena");
        if (!data.territory().contains(data.coreArena())) {
            throw new IllegalArgumentException(
                    "Regalhart core arena must be contained by the territory."
            );
        }
        if (data.startAnchors().size() != 3) {
            throw new IllegalArgumentException(
                    "Regalhart requires exactly 3 authored start anchors."
            );
        }

        Set<String> ids = new HashSet<>();
        for (StartAnchor anchor : data.startAnchors()) {
            requireNamespacedId(anchor.id(), "start anchor");
            if (!ids.add(anchor.id())) {
                throw new IllegalArgumentException(
                        "Duplicate Regalhart start anchor id: " + anchor.id()
                );
            }
            if (!data.territory().contains(anchor.x(), anchor.z())) {
                throw new IllegalArgumentException(
                        "Regalhart start anchor outside territory: "
                                + anchor.id()
                );
            }
            if (anchor.y() < -64 || anchor.y() > 511) {
                throw new IllegalArgumentException(
                        "Regalhart start anchor Y outside world envelope: "
                                + anchor
                );
            }
            if (anchor.surfaceEvidence().isBlank()
                    || anchor.role().isBlank()) {
                throw new IllegalArgumentException(
                        "Regalhart start anchor evidence/role cannot be blank: "
                                + anchor.id()
                );
            }
        }

        if (!Set.of(
                "openworld_rpg:r01/regalhart/start_southwest",
                "openworld_rpg:r01/regalhart/start_center",
                "openworld_rpg:r01/regalhart/start_east_ridge"
        ).equals(ids)) {
            throw new IllegalArgumentException(
                    "Regalhart start-anchor identity set does not match canon."
            );
        }
    }

    private static void validateBounds(Bounds bounds, String subject) {
        requireNamespacedId(bounds.id(), subject);
        if (bounds.minX() > bounds.maxX()
                || bounds.minZ() > bounds.maxZ()) {
            throw new IllegalArgumentException(
                    "Inverted Regalhart " + subject + " bounds: " + bounds
            );
        }
        if (bounds.minX() < -4096
                || bounds.maxX() > 2559
                || bounds.minZ() < 0
                || bounds.maxZ() > 6655) {
            throw new IllegalArgumentException(
                    "Regalhart " + subject
                            + " outside extracted R01 Azari slice: " + bounds
            );
        }
        if (bounds.role().isBlank()) {
            throw new IllegalArgumentException(
                    "Regalhart " + subject + " role cannot be blank."
            );
        }
    }

    private static void requireNamespacedId(String id, String subject) {
        if (id == null
                || id.isBlank()
                || id.indexOf(':') <= 0
                || id.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Invalid Regalhart " + subject + " id: " + id
            );
        }
    }
}
