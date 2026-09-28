package dev.moonseungjun.openworldrpg.housing;

import java.util.Objects;

/**
 * Exact non-visual R01 furniture placement contract.
 *
 * <p>The accepted external model adapter supplies its real collision/approach geometry. This class
 * refuses to invent placeholder boxes.</p>
 */
public final class R01FurniturePlacementRules {
    public static final double HORIZONTAL_GRID = 0.25;
    public static final double COLLISION_INFLATION_HORIZONTAL = 0.05;
    private static final double EPSILON = 1.0e-7;

    private R01FurniturePlacementRules() {
    }

    public static PlacementResult validate(
            PlacementRequest request,
            GeometryProbe geometry
    ) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(geometry, "geometry");

        R01FurnitureRules.FurnitureDefinition definition =
                R01FurnitureRules.definition(request.furnitureId());

        if (!geometry.propertyProduction()) {
            return PlacementResult.PROPERTY_NOT_PRODUCTION;
        }
        if (!geometry.visualBindingAccepted()) {
            return PlacementResult.FURNITURE_VISUAL_NOT_ACCEPTED;
        }
        if (!onQuarterGrid(request.x()) || !onQuarterGrid(request.z())) {
            return PlacementResult.INVALID_HORIZONTAL_GRID;
        }
        if (!validRotation(request.rotationDegrees())) {
            return PlacementResult.INVALID_ROTATION;
        }
        if (!definition.allowedSurfaces().contains(request.surfaceClass())) {
            return PlacementResult.INVALID_SURFACE_CLASS;
        }
        if (!geometry.supportSurfaceAccepted()) {
            return PlacementResult.INVALID_SUPPORT_SURFACE;
        }
        if (!geometry.insideOwnedFurnishingVolume()) {
            return PlacementResult.OUTSIDE_FURNISHING_VOLUME;
        }
        if (geometry.overlapsProtectedShell()) {
            return PlacementResult.PROTECTED_SHELL_COLLISION;
        }
        if (geometry.overlapsSolidFurniture()) {
            return PlacementResult.FURNITURE_COLLISION;
        }
        if (geometry.overlapsClearanceVolume()) {
            return PlacementResult.CRITICAL_CLEARANCE_BLOCKED;
        }
        if (functional(definition.function())
                && !geometry.interactionApproachPreserved()) {
            return PlacementResult.INTERACTION_APPROACH_BLOCKED;
        }
        return PlacementResult.ACCEPTED;
    }

    public static boolean onQuarterGrid(double coordinate) {
        if (!Double.isFinite(coordinate)) {
            return false;
        }
        double quarters = coordinate / HORIZONTAL_GRID;
        return Math.abs(quarters - Math.rint(quarters)) <= EPSILON;
    }

    public static boolean validRotation(int degrees) {
        return degrees == 0
                || degrees == 90
                || degrees == 180
                || degrees == 270;
    }

    private static boolean functional(R01FurnitureRules.Function function) {
        return switch (function) {
            case HOME_REST,
                    HOME_STORAGE_ACCESS,
                    PLACEMENT_SURFACE,
                    SITTABLE,
                    INTERIOR_LIGHT,
                    DISPLAY_SURFACE,
                    TROPHY_DISPLAY,
                    WARDROBE_ACCESS,
                    HOME_COOKING,
                    SITTABLE_DECOR -> true;
            case DECOR -> false;
        };
    }

    public record PlacementRequest(
            String furnitureId,
            double x,
            double y,
            double z,
            int rotationDegrees,
            R01FurnitureRules.SurfaceClass surfaceClass
    ) {
        public PlacementRequest {
            Objects.requireNonNull(furnitureId, "furnitureId");
            Objects.requireNonNull(surfaceClass, "surfaceClass");
            if (!Double.isFinite(x)
                    || !Double.isFinite(y)
                    || !Double.isFinite(z)) {
                throw new IllegalArgumentException(
                        "Furniture position must be finite."
                );
            }
        }
    }

    /**
     * Geometry facts are computed by the later production shell/model adapter.
     *
     * <p>Collision checks must use the accepted furniture placement box inflated horizontally by
     * {@link #COLLISION_INFLATION_HORIZONTAL}.</p>
     */
    public record GeometryProbe(
            boolean propertyProduction,
            boolean visualBindingAccepted,
            boolean supportSurfaceAccepted,
            boolean insideOwnedFurnishingVolume,
            boolean overlapsProtectedShell,
            boolean overlapsSolidFurniture,
            boolean overlapsClearanceVolume,
            boolean interactionApproachPreserved
    ) {
    }

    public enum PlacementResult {
        ACCEPTED,
        PROPERTY_NOT_PRODUCTION,
        FURNITURE_VISUAL_NOT_ACCEPTED,
        INVALID_HORIZONTAL_GRID,
        INVALID_ROTATION,
        INVALID_SURFACE_CLASS,
        INVALID_SUPPORT_SURFACE,
        OUTSIDE_FURNISHING_VOLUME,
        PROTECTED_SHELL_COLLISION,
        FURNITURE_COLLISION,
        CRITICAL_CLEARANCE_BLOCKED,
        INTERACTION_APPROACH_BLOCKED
    }
}
