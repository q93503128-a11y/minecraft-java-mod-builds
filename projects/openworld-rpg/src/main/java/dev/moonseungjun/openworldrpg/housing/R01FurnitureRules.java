package dev.moonseungjun.openworldrpg.housing;

import dev.moonseungjun.openworldrpg.inventory.ProjectInventoryItem;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Canonical R01 Household catalogue and starter-package semantics. */
public final class R01FurnitureRules {
    public static final long STARTER_PACKAGE_GOLD = 750L;

    public static final String ALDERFORD_BED =
            "openworld_rpg:furniture/alderford_bed";
    public static final String STORAGE_CABINET =
            "openworld_rpg:furniture/storage_cabinet";
    public static final String PLAIN_TABLE =
            "openworld_rpg:furniture/plain_table";
    public static final String ALDERFORD_CHAIR =
            "openworld_rpg:furniture/alderford_chair";
    public static final String IRON_LANTERN =
            "openworld_rpg:furniture/iron_lantern";
    public static final String WALL_SHELF =
            "openworld_rpg:furniture/wall_shelf";
    public static final String TROPHY_STAND =
            "openworld_rpg:furniture/trophy_stand";
    public static final String WARDROBE =
            "openworld_rpg:furniture/wardrobe";
    public static final String COOKING_HEARTH =
            "openworld_rpg:furniture/cooking_hearth";
    public static final String WOVEN_RUG =
            "openworld_rpg:furniture/woven_rug";
    public static final String WOODEN_BENCH =
            "openworld_rpg:furniture/wooden_bench";
    public static final String SIDE_TABLE =
            "openworld_rpg:furniture/side_table";

    public static final List<FurnitureDefinition> CATALOGUE = List.of(
            new FurnitureDefinition(
                    ALDERFORD_BED, 180L, Function.HOME_REST,
                    Set.of(SurfaceClass.FLOOR)
            ),
            new FurnitureDefinition(
                    STORAGE_CABINET, 140L, Function.HOME_STORAGE_ACCESS,
                    Set.of(SurfaceClass.FLOOR)
            ),
            new FurnitureDefinition(
                    PLAIN_TABLE, 90L, Function.PLACEMENT_SURFACE,
                    Set.of(SurfaceClass.FLOOR)
            ),
            new FurnitureDefinition(
                    ALDERFORD_CHAIR, 60L, Function.SITTABLE,
                    Set.of(SurfaceClass.FLOOR)
            ),
            new FurnitureDefinition(
                    IRON_LANTERN, 35L, Function.INTERIOR_LIGHT,
                    Set.of(
                            SurfaceClass.FLOOR,
                            SurfaceClass.TABLE,
                            SurfaceClass.SHELF
                    )
            ),
            new FurnitureDefinition(
                    WALL_SHELF, 60L, Function.DISPLAY_SURFACE,
                    Set.of(SurfaceClass.WALL)
            ),
            new FurnitureDefinition(
                    TROPHY_STAND, 90L, Function.TROPHY_DISPLAY,
                    Set.of(SurfaceClass.FLOOR)
            ),
            new FurnitureDefinition(
                    WARDROBE, 160L, Function.WARDROBE_ACCESS,
                    Set.of(SurfaceClass.FLOOR)
            ),
            new FurnitureDefinition(
                    COOKING_HEARTH, 220L, Function.HOME_COOKING,
                    Set.of(SurfaceClass.FLOOR)
            ),
            new FurnitureDefinition(
                    WOVEN_RUG, 70L, Function.DECOR,
                    Set.of(SurfaceClass.FLOOR_SURFACE)
            ),
            new FurnitureDefinition(
                    WOODEN_BENCH, 85L, Function.SITTABLE_DECOR,
                    Set.of(SurfaceClass.FLOOR)
            ),
            new FurnitureDefinition(
                    SIDE_TABLE, 50L, Function.PLACEMENT_SURFACE,
                    Set.of(SurfaceClass.FLOOR)
            )
    );

    private static final Map<String, FurnitureDefinition> BY_ID =
            CATALOGUE.stream().collect(
                    java.util.stream.Collectors.toUnmodifiableMap(
                            FurnitureDefinition::id,
                            value -> value
                    )
            );

    /**
     * Exact seven-line package from the R01 canon. The shelf/cabinet line resolves to the
     * 60-Gold Wall Shelf catalogue entry; the Storage Cabinet is already its own 140-Gold line.
     */
    public static final Map<String, Integer> STARTER_PACKAGE =
            starterPackage();

    private R01FurnitureRules() {
    }

    public static FurnitureDefinition definition(String furnitureId) {
        Objects.requireNonNull(furnitureId, "furnitureId");
        FurnitureDefinition value = BY_ID.get(furnitureId);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Unknown R01 furniture id: " + furnitureId
            );
        }
        return value;
    }

    public static long price(String furnitureId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Furniture quantity must be positive."
            );
        }
        return Math.multiplyExact(
                definition(furnitureId).priceGold(),
                quantity
        );
    }

    public static ProjectInventoryItem inventoryItem(
            String furnitureId,
            int quantity
    ) {
        definition(furnitureId);
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Furniture quantity must be positive."
            );
        }
        /*
         * Housing furniture has no generic resale value. Property resale is a separate shell
         * transaction and furniture survives a move.
         */
        return ProjectInventoryItem.ordinary(
                furnitureId,
                quantity,
                64,
                0L
        );
    }

    public static long starterPackageCatalogueValue() {
        long total = 0L;
        for (Map.Entry<String, Integer> entry : STARTER_PACKAGE.entrySet()) {
            total = Math.addExact(
                    total,
                    price(entry.getKey(), entry.getValue())
            );
        }
        return total;
    }

    private static Map<String, Integer> starterPackage() {
        LinkedHashMap<String, Integer> values = new LinkedHashMap<>();
        values.put(ALDERFORD_BED, 1);
        values.put(STORAGE_CABINET, 1);
        values.put(PLAIN_TABLE, 1);
        values.put(ALDERFORD_CHAIR, 2);
        values.put(IRON_LANTERN, 2);
        values.put(WALL_SHELF, 1);
        values.put(TROPHY_STAND, 1);
        return Map.copyOf(values);
    }

    public enum Function {
        HOME_REST,
        HOME_STORAGE_ACCESS,
        PLACEMENT_SURFACE,
        SITTABLE,
        INTERIOR_LIGHT,
        DISPLAY_SURFACE,
        TROPHY_DISPLAY,
        WARDROBE_ACCESS,
        HOME_COOKING,
        DECOR,
        SITTABLE_DECOR
    }

    public enum SurfaceClass {
        FLOOR,
        FLOOR_SURFACE,
        TABLE,
        SHELF,
        WALL
    }

    public record FurnitureDefinition(
            String id,
            long priceGold,
            Function function,
            Set<SurfaceClass> allowedSurfaces
    ) {
        public FurnitureDefinition {
            requireStableId(id);
            if (priceGold <= 0L) {
                throw new IllegalArgumentException(
                        "Furniture price must be positive."
                );
            }
            Objects.requireNonNull(function, "function");
            allowedSurfaces = Set.copyOf(
                    Objects.requireNonNull(
                            allowedSurfaces,
                            "allowedSurfaces"
                    )
            );
            if (allowedSurfaces.isEmpty()) {
                throw new IllegalArgumentException(
                        "Furniture requires an authored support surface."
                );
            }
        }
    }

    private static void requireStableId(String value) {
        if (value == null
                || value.isBlank()
                || value.indexOf(':') <= 0
                || value.indexOf(' ') >= 0) {
            throw new IllegalArgumentException(
                    "Expected stable namespaced furniture id."
            );
        }
    }
}
