package dev.moonseungjun.openworldrpg.integration.verify;

import dev.moonseungjun.openworldrpg.world.spatial.R01SpatialBindingData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Deterministic, non-authoritative surface review order for the R01 integration playtest artifact.
 *
 * <p>The plan deliberately excludes Quarry interior review volumes. Those candidate volumes may be
 * solid carve probes and are not safe teleport destinations. Nothing in this class promotes spatial
 * data or exposes candidate coordinates to normal gameplay.</p>
 */
public final class R01SpatialReviewPlan {
    private static final List<String> CRITICAL_ANCHOR_IDS = List.of(
            "openworld_rpg:r01/alderford_center",
            "openworld_rpg:r01/alderford_gate_probe",
            "openworld_rpg:r01/broken_road_marker_probe",
            "openworld_rpg:r01/roadside_trouble_probe",
            "openworld_rpg:r01/lost_cargo_probe",
            "openworld_rpg:r01/quarry_waystone_probe",
            "openworld_rpg:r01/quarry_overlook_probe",
            "openworld_rpg:r01/quarry_lower_entrance_probe"
    );

    private R01SpatialReviewPlan() {
    }

    public static List<ReviewPoint> surfacePoints(
            R01SpatialBindingData data
    ) {
        Objects.requireNonNull(data, "data");

        Map<String, R01SpatialBindingData.Anchor> anchorsById =
                data.anchors().stream().collect(Collectors.toMap(
                        R01SpatialBindingData.Anchor::id,
                        Function.identity()
                ));
        ArrayList<ReviewPoint> result = new ArrayList<>();

        for (String id : CRITICAL_ANCHOR_IDS) {
            R01SpatialBindingData.Anchor anchor =
                    Objects.requireNonNull(
                            anchorsById.get(id),
                            "Missing critical R01 review anchor: " + id
                    );
            result.add(fromAnchor(anchor));
        }

        HashSet<String> critical = new HashSet<>(
                CRITICAL_ANCHOR_IDS
        );
        data.anchors().stream()
                .filter(anchor -> !critical.contains(anchor.id()))
                .sorted(Comparator.comparing(
                        R01SpatialBindingData.Anchor::id
                ))
                .map(R01SpatialReviewPlan::fromAnchor)
                .forEach(result::add);

        data.areas().stream()
                .sorted(Comparator.comparing(
                        R01SpatialBindingData.Area::id
                ))
                .map(area -> new ReviewPoint(
                        "area",
                        area.id(),
                        midpoint(area.minX(), area.maxX()),
                        null,
                        midpoint(area.minZ(), area.maxZ()),
                        area.role()
                ))
                .forEach(result::add);

        data.routes().stream()
                .sorted(Comparator.comparing(
                        R01SpatialBindingData.Route::id
                ))
                .forEach(route -> {
                    for (int index = 0;
                            index < route.points().size();
                            index++) {
                        R01SpatialBindingData.RoutePoint point =
                                route.points().get(index);
                        result.add(new ReviewPoint(
                                "route",
                                route.id() + "#" + (index + 1),
                                point.x(),
                                null,
                                point.z(),
                                "route waypoint "
                                        + (index + 1)
                                        + "/"
                                        + route.points().size()
                                        + " — "
                                        + route.role()
                        ));
                    }
                });

        return List.copyOf(result);
    }

    private static ReviewPoint fromAnchor(
            R01SpatialBindingData.Anchor anchor
    ) {
        return new ReviewPoint(
                "anchor",
                anchor.id(),
                anchor.x(),
                anchor.y(),
                anchor.z(),
                anchor.role()
        );
    }

    private static int midpoint(int min, int max) {
        return min + (max - min) / 2;
    }

    public record ReviewPoint(
            String kind,
            String id,
            int x,
            Integer y,
            int z,
            String role
    ) {
        public ReviewPoint {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(role, "role");
            if (kind.isBlank() || id.isBlank() || role.isBlank()) {
                throw new IllegalArgumentException(
                        "R01 spatial review point text cannot be blank."
                );
            }
        }
    }
}
