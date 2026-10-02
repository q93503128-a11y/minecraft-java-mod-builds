package dev.moonseungjun.openworldrpg.world.spatial;

import com.google.gson.annotations.SerializedName;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Data-driven R01 Azari spatial candidates.
 *
 * <p>Candidate coordinates are deliberately distinct from production bindings. Runtime gameplay
 * may query only anchors/volumes whose status has been promoted to {@code production}; raw-world
 * candidates therefore cannot silently become live quest/spawn authority before the required
 * Minecraft-client sightline and travel review.</p>
 */
public record R01SpatialBindingData(
        @SerializedName("schema_version") int schemaVersion,
        String id,
        @SerializedName("map_build") String mapBuild,
        @SerializedName("source_status") String sourceStatus,
        List<Anchor> anchors,
        List<Area> areas,
        List<Volume> volumes,
        List<Route> routes
) {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final String CANONICAL_ID =
            "openworld_rpg:r01/azari_spatial_candidates";
    public static final String CANONICAL_MAP_BUILD = "AzariNEW4252026";
    public static final String CANDIDATE_SOURCE_STATUS =
            "actual_r01_slice_candidate";
    public static final String PRODUCTION_SOURCE_STATUS =
            "actual_r01_slice_production";

    private static final Set<String> ALLOWED_SOURCE_STATUSES = Set.of(
            CANDIDATE_SOURCE_STATUS,
            PRODUCTION_SOURCE_STATUS
    );

    public R01SpatialBindingData {
        anchors = List.copyOf(Objects.requireNonNull(anchors, "anchors"));
        areas = List.copyOf(Objects.requireNonNull(areas, "areas"));
        volumes = List.copyOf(Objects.requireNonNull(volumes, "volumes"));
        routes = List.copyOf(Objects.requireNonNull(routes, "routes"));
    }

    public Optional<Anchor> anchor(String anchorId) {
        Objects.requireNonNull(anchorId, "anchorId");
        return anchors.stream().filter(anchor -> anchor.id().equals(anchorId)).findFirst();
    }

    public Optional<Anchor> productionAnchor(String anchorId) {
        if (!productionSource()) {
            return Optional.empty();
        }
        return anchor(anchorId).filter(Anchor::production);
    }

    public Optional<Area> area(String areaId) {
        Objects.requireNonNull(areaId, "areaId");
        return areas.stream().filter(area -> area.id().equals(areaId)).findFirst();
    }

    public Optional<Area> productionArea(String areaId) {
        if (!productionSource()) {
            return Optional.empty();
        }
        return area(areaId).filter(Area::production);
    }

    public Optional<Volume> volume(String volumeId) {
        Objects.requireNonNull(volumeId, "volumeId");
        return volumes.stream().filter(volume -> volume.id().equals(volumeId)).findFirst();
    }

    public Optional<Volume> productionVolume(String volumeId) {
        if (!productionSource()) {
            return Optional.empty();
        }
        return volume(volumeId).filter(Volume::production);
    }

    public Optional<Route> route(String routeId) {
        Objects.requireNonNull(routeId, "routeId");
        return routes.stream().filter(route -> route.id().equals(routeId)).findFirst();
    }

    /**
     * Source-level production gate. Candidate/review evidence may remain in the same dataset after
     * promotion; only entries explicitly marked production are visible through production accessors.
     */
    public boolean productionReady() {
        return productionSource();
    }

    public boolean productionSource() {
        return PRODUCTION_SOURCE_STATUS.equals(sourceStatus);
    }

    public record Anchor(
            String id,
            String status,
            int x,
            Integer y,
            int z,
            String role
    ) {
        public Anchor {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(role, "role");
        }

        public boolean production() {
            return "production".equals(status);
        }
    }

    public record Area(
            String id,
            String status,
            @SerializedName("min_x") int minX,
            @SerializedName("max_x") int maxX,
            @SerializedName("min_z") int minZ,
            @SerializedName("max_z") int maxZ,
            String role
    ) {
        public Area {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(role, "role");
        }

        public boolean production() {
            return "production".equals(status);
        }
    }
    public record Volume(
            String id,
            String status,
            @SerializedName("min_x") int minX,
            @SerializedName("max_x") int maxX,
            @SerializedName("min_y") int minY,
            @SerializedName("max_y") int maxY,
            @SerializedName("min_z") int minZ,
            @SerializedName("max_z") int maxZ,
            @SerializedName("review_mode") String reviewMode,
            String role
    ) {
        public Volume {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            Objects.requireNonNull(reviewMode, "reviewMode");
            Objects.requireNonNull(role, "role");
        }

        public boolean production() {
            return "production".equals(status);
        }
    }

    public record Route(
            String id,
            String status,
            List<RoutePoint> points,
            String role
    ) {
        public Route {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(status, "status");
            points = List.copyOf(Objects.requireNonNull(points, "points"));
            Objects.requireNonNull(role, "role");
        }

        public boolean production() {
            return "production".equals(status);
        }

        public double horizontalLengthBlocks() {
            double sum = 0.0;
            for (int index = 1; index < points.size(); index++) {
                RoutePoint previous = points.get(index - 1);
                RoutePoint current = points.get(index);
                sum += Math.hypot(
                        current.x() - previous.x(),
                        current.z() - previous.z()
                );
            }
            return sum;
        }
    }

    public record RoutePoint(int x, int z) {
    }

    public static void validate(R01SpatialBindingData data) {
        Objects.requireNonNull(data, "data");
        if (data.schemaVersion() != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported R01 spatial schema: " + data.schemaVersion()
            );
        }
        if (!CANONICAL_ID.equals(data.id())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 spatial id: " + data.id()
            );
        }
        if (!CANONICAL_MAP_BUILD.equals(data.mapBuild())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 Azari map build: " + data.mapBuild()
            );
        }
        if (!ALLOWED_SOURCE_STATUSES.contains(data.sourceStatus())) {
            throw new IllegalArgumentException(
                    "Unexpected R01 spatial source status: " + data.sourceStatus()
            );
        }

        validateUniqueIds(
                data.anchors().stream().map(Anchor::id).toList(),
                "anchor"
        );
        validateUniqueIds(
                data.areas().stream().map(Area::id).toList(),
                "area"
        );
        validateUniqueIds(
                data.volumes().stream().map(Volume::id).toList(),
                "volume"
        );
        validateUniqueIds(
                data.routes().stream().map(Route::id).toList(),
                "route"
        );

        Set<String> requiredAnchors = Set.of(
                "openworld_rpg:r01/alderford_center",
                "openworld_rpg:r01/alderford_gate_probe",
                "openworld_rpg:r01/broken_road_marker_probe",
                "openworld_rpg:r01/roadside_trouble_probe",
                "openworld_rpg:r01/lost_cargo_probe",
                "openworld_rpg:r01/quarry_waystone_probe",
                "openworld_rpg:r01/quarry_overlook_probe",
                "openworld_rpg:r01/quarry_lower_entrance_probe"
        );
        Set<String> actualAnchors = new HashSet<>(
                data.anchors().stream().map(Anchor::id).toList()
        );
        if (!actualAnchors.containsAll(requiredAnchors)) {
            Set<String> missing = new HashSet<>(requiredAnchors);
            missing.removeAll(actualAnchors);
            throw new IllegalArgumentException(
                    "R01 spatial candidate set is incomplete; missing=" + missing
            );
        }
        Set<String> requiredVolumes = Set.of(
                "openworld_rpg:r01/quarry/upper_gallery_review",
                "openworld_rpg:r01/quarry/collapsed_hoist_review",
                "openworld_rpg:r01/quarry/root_breached_review",
                "openworld_rpg:r01/quarry/relay_gallery_review",
                "openworld_rpg:r01/quarry/earthloong_chamber_review"
        );
        Set<String> actualVolumes = new HashSet<>(
                data.volumes().stream().map(Volume::id).toList()
        );
        if (!actualVolumes.containsAll(requiredVolumes)) {
            Set<String> missing = new HashSet<>(requiredVolumes);
            missing.removeAll(actualVolumes);
            throw new IllegalArgumentException(
                    "R01 Quarry interior review-volume set is incomplete; missing=" + missing
            );
        }

        for (Anchor anchor : data.anchors()) {
            requireNamespacedId(anchor.id(), "anchor");
            requireReviewStatus(anchor.status(), "anchor " + anchor.id());
            requireInsideExtractedSlice(anchor.x(), anchor.z(), anchor.id());
            if (anchor.y() != null && (anchor.y() < -64 || anchor.y() > 511)) {
                throw new IllegalArgumentException(
                        "R01 spatial anchor y outside accepted world envelope: " + anchor
                );
            }
            if (anchor.production() && anchor.y() == null) {
                throw new IllegalArgumentException(
                        "Production R01 spatial anchor requires a verified y coordinate: "
                                + anchor.id()
                );
            }
            if (anchor.role().isBlank()) {
                throw new IllegalArgumentException(
                        "R01 spatial anchor role is blank: " + anchor.id()
                );
            }
        }

        for (Area area : data.areas()) {
            requireNamespacedId(area.id(), "area");
            requireReviewStatus(area.status(), "area " + area.id());
            requireInsideExtractedSlice(area.minX(), area.minZ(), area.id());
            requireInsideExtractedSlice(area.maxX(), area.maxZ(), area.id());
            if (area.minX() > area.maxX() || area.minZ() > area.maxZ()) {
                throw new IllegalArgumentException(
                        "R01 spatial area bounds are inverted: " + area
                );
            }
            if (area.role().isBlank()) {
                throw new IllegalArgumentException(
                        "R01 spatial area role is blank: " + area.id()
                );
            }
        }

        for (Volume volume : data.volumes()) {
            requireNamespacedId(volume.id(), "volume");
            requireReviewStatus(volume.status(), "volume " + volume.id());
            requireInsideExtractedSlice(volume.minX(), volume.minZ(), volume.id());
            requireInsideExtractedSlice(volume.maxX(), volume.maxZ(), volume.id());
            if (volume.minX() > volume.maxX()
                    || volume.minY() > volume.maxY()
                    || volume.minZ() > volume.maxZ()) {
                throw new IllegalArgumentException(
                        "R01 spatial volume bounds are inverted: " + volume
                );
            }
            if (volume.minY() < -64 || volume.maxY() > 511) {
                throw new IllegalArgumentException(
                        "R01 spatial volume y outside accepted world envelope: " + volume
                );
            }
            if (!Set.of(
                    "natural_seam",
                    "transition_probe",
                    "solid_carve_probe",
                    "runtime_authored"
            ).contains(volume.reviewMode())) {
                throw new IllegalArgumentException(
                        "Unknown R01 spatial volume review mode: " + volume.reviewMode()
                );
            }
            if (volume.production()
                    && !"runtime_authored".equals(volume.reviewMode())) {
                throw new IllegalArgumentException(
                        "Production R01 spatial volume must be authored runtime geometry: "
                                + volume.id()
                );
            }
            if (volume.role().isBlank()) {
                throw new IllegalArgumentException(
                        "R01 spatial volume role is blank: " + volume.id()
                );
            }
        }
        for (Route route : data.routes()) {
            requireNamespacedId(route.id(), "route");
            requireReviewStatus(route.status(), "route " + route.id());
            if (route.points().size() < 2) {
                throw new IllegalArgumentException(
                        "R01 spatial route requires at least two points: " + route.id()
                );
            }
            for (RoutePoint point : route.points()) {
                requireInsideExtractedSlice(point.x(), point.z(), route.id());
            }
            if (!Double.isFinite(route.horizontalLengthBlocks())
                    || route.horizontalLengthBlocks() <= 0.0) {
                throw new IllegalArgumentException(
                        "R01 spatial route length is invalid: " + route.id()
                );
            }
            if (route.role().isBlank()) {
                throw new IllegalArgumentException(
                        "R01 spatial route role is blank: " + route.id()
                );
            }
        }

        boolean anyProduction =
                data.anchors().stream().anyMatch(Anchor::production)
                        || data.areas().stream().anyMatch(Area::production)
                        || data.volumes().stream().anyMatch(Volume::production)
                        || data.routes().stream().anyMatch(Route::production);
        if (!data.productionSource() && anyProduction) {
            throw new IllegalArgumentException(
                    "Candidate R01 spatial source cannot expose production entries before "
                            + "source-level promotion."
            );
        }
        if (data.productionSource()) {
            if (data.anchors().stream().noneMatch(Anchor::production)
                    || data.areas().stream().noneMatch(Area::production)
                    || data.volumes().stream().noneMatch(Volume::production)
                    || data.routes().stream().noneMatch(Route::production)) {
                throw new IllegalArgumentException(
                        "Production R01 spatial source requires accepted production bindings "
                                + "for anchors, areas, authored runtime volumes and routes."
                );
            }
        }
    }

    private static void validateUniqueIds(List<String> ids, String kind) {
        Set<String> seen = new HashSet<>();
        for (String id : ids) {
            requireNamespacedId(id, kind);
            if (!seen.add(id)) {
                throw new IllegalArgumentException(
                        "Duplicate R01 spatial " + kind + " id: " + id
                );
            }
        }
    }

    private static void requireReviewStatus(String status, String subject) {
        if (!Set.of("candidate", "client_verified", "production").contains(status)) {
            throw new IllegalArgumentException(
                    "Unknown R01 spatial status for " + subject + ": " + status
            );
        }
    }

    private static void requireNamespacedId(String id, String kind) {
        if (id == null
                || id.isBlank()
                || id.indexOf(':') <= 0
                || id.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Invalid namespaced R01 spatial " + kind + " id: " + id
            );
        }
    }

    private static void requireInsideExtractedSlice(
            int x,
            int z,
            String subject
    ) {
        if (x < -4096 || x > 2559 || z < 0 || z > 6655) {
            throw new IllegalArgumentException(
                    "R01 spatial candidate outside extracted slice: "
                            + subject + " @ " + x + "," + z
            );
        }
    }
}
