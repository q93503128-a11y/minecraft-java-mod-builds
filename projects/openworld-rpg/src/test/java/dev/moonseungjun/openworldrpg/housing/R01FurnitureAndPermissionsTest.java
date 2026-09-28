package dev.moonseungjun.openworldrpg.housing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class R01FurnitureAndPermissionsTest {
    private static final String OWNER =
            UUID.fromString("11111111-1111-1111-1111-111111111111").toString();
    private static final String DECORATOR =
            UUID.fromString("22222222-2222-2222-2222-222222222222").toString();
    private static final String GUEST =
            UUID.fromString("33333333-3333-3333-3333-333333333333").toString();

    private static final String GATE =
            "openworld_rpg:property/alderford/gate_cottage";
    private static final String MARKET =
            "openworld_rpg:property/alderford/market_house";

    @Test
    void householdCatalogueAndStarterPackageMatchCanon() {
        assertEquals(12, R01FurnitureRules.CATALOGUE.size());
        assertEquals(180L, R01FurnitureRules.price(
                R01FurnitureRules.ALDERFORD_BED, 1
        ));
        assertEquals(140L, R01FurnitureRules.price(
                R01FurnitureRules.STORAGE_CABINET, 1
        ));
        assertEquals(90L, R01FurnitureRules.price(
                R01FurnitureRules.PLAIN_TABLE, 1
        ));
        assertEquals(120L, R01FurnitureRules.price(
                R01FurnitureRules.ALDERFORD_CHAIR, 2
        ));
        assertEquals(70L, R01FurnitureRules.price(
                R01FurnitureRules.IRON_LANTERN, 2
        ));
        assertEquals(60L, R01FurnitureRules.price(
                R01FurnitureRules.WALL_SHELF, 1
        ));
        assertEquals(90L, R01FurnitureRules.price(
                R01FurnitureRules.TROPHY_STAND, 1
        ));
        assertEquals(
                R01FurnitureRules.STARTER_PACKAGE_GOLD,
                R01FurnitureRules.starterPackageCatalogueValue()
        );
        assertEquals(
                Map.of(
                        R01FurnitureRules.ALDERFORD_BED, 1,
                        R01FurnitureRules.STORAGE_CABINET, 1,
                        R01FurnitureRules.PLAIN_TABLE, 1,
                        R01FurnitureRules.ALDERFORD_CHAIR, 2,
                        R01FurnitureRules.IRON_LANTERN, 2,
                        R01FurnitureRules.WALL_SHELF, 1,
                        R01FurnitureRules.TROPHY_STAND, 1
                ),
                R01FurnitureRules.STARTER_PACKAGE
        );
    }

    @Test
    void starterPackageFitsSmallCottageWithoutInventingExtraCapacity() {
        R01HomeStorageState storage = R01HomeStorageState.empty(54);
        for (var entry : R01FurnitureRules.STARTER_PACKAGE.entrySet()) {
            var inserted = storage.insert(
                    R01FurnitureRules.inventoryItem(
                            entry.getKey(),
                            entry.getValue()
                    )
            );
            assertTrue(inserted.remainder().isEmpty());
            storage = inserted.state();
        }

        assertTrue(storage.usedSlots() <= 54);
        assertEquals(54, storage.capacity());
    }

    @Test
    void placementUsesQuarterGridCardinalRotationAndAcceptedGeometry() {
        var request = new R01FurniturePlacementRules.PlacementRequest(
                R01FurnitureRules.ALDERFORD_BED,
                1.25,
                64.0,
                -2.50,
                90,
                R01FurnitureRules.SurfaceClass.FLOOR
        );
        var geometry = new R01FurniturePlacementRules.GeometryProbe(
                true,
                true,
                true,
                true,
                false,
                false,
                false,
                true
        );

        assertEquals(
                R01FurniturePlacementRules.PlacementResult.ACCEPTED,
                R01FurniturePlacementRules.validate(request, geometry)
        );
        assertEquals(
                0.05,
                R01FurniturePlacementRules.COLLISION_INFLATION_HORIZONTAL,
                0.000001
        );

        var offGrid = new R01FurniturePlacementRules.PlacementRequest(
                R01FurnitureRules.ALDERFORD_BED,
                1.20,
                64.0,
                -2.50,
                90,
                R01FurnitureRules.SurfaceClass.FLOOR
        );
        assertEquals(
                R01FurniturePlacementRules.PlacementResult.INVALID_HORIZONTAL_GRID,
                R01FurniturePlacementRules.validate(offGrid, geometry)
        );

        var badRotation = new R01FurniturePlacementRules.PlacementRequest(
                R01FurnitureRules.ALDERFORD_BED,
                1.25,
                64.0,
                -2.50,
                45,
                R01FurnitureRules.SurfaceClass.FLOOR
        );
        assertEquals(
                R01FurniturePlacementRules.PlacementResult.INVALID_ROTATION,
                R01FurniturePlacementRules.validate(badRotation, geometry)
        );
    }

    @Test
    void placementFailsClosedWithoutProductionShellOrAcceptedFurnitureVisual() {
        var request = new R01FurniturePlacementRules.PlacementRequest(
                R01FurnitureRules.WALL_SHELF,
                2.0,
                65.0,
                3.0,
                180,
                R01FurnitureRules.SurfaceClass.WALL
        );

        assertEquals(
                R01FurniturePlacementRules.PlacementResult.PROPERTY_NOT_PRODUCTION,
                R01FurniturePlacementRules.validate(
                        request,
                        new R01FurniturePlacementRules.GeometryProbe(
                                false, true, true, true,
                                false, false, false, true
                        )
                )
        );
        assertEquals(
                R01FurniturePlacementRules.PlacementResult.FURNITURE_VISUAL_NOT_ACCEPTED,
                R01FurniturePlacementRules.validate(
                        request,
                        new R01FurniturePlacementRules.GeometryProbe(
                                true, false, true, true,
                                false, false, false, true
                        )
                )
        );

        var wrongSurface = new R01FurniturePlacementRules.PlacementRequest(
                R01FurnitureRules.WALL_SHELF,
                2.0,
                65.0,
                3.0,
                180,
                R01FurnitureRules.SurfaceClass.FLOOR
        );
        assertEquals(
                R01FurniturePlacementRules.PlacementResult.INVALID_SURFACE_CLASS,
                R01FurniturePlacementRules.validate(
                        wrongSurface,
                        new R01FurniturePlacementRules.GeometryProbe(
                                true, true, true, true,
                                false, false, false, true
                        )
                )
        );
    }

    @Test
    void ownerDecoratorGuestPermissionsAndPrivateStorageStaySeparate() {
        R01HousingWorldState world = ownedGate();

        assertEquals(
                R01HousingWorldState.PropertyRole.OWNER,
                world.roleFor(GATE, OWNER)
        );
        assertEquals(
                R01HousingWorldState.PropertyRole.GUEST,
                world.roleFor(GATE, DECORATOR)
        );
        assertTrue(world.canFurnish(GATE, OWNER));
        assertFalse(world.canFurnish(GATE, DECORATOR));
        assertFalse(world.canAccessPrivateStorage(GATE, DECORATOR));
        assertTrue(world.canUseNonPrivateFurniture(GATE, GUEST));

        world = world.setTrustedDecorator(
                GATE,
                OWNER,
                DECORATOR,
                true
        );
        assertEquals(
                R01HousingWorldState.PropertyRole.TRUSTED_DECORATOR,
                world.roleFor(GATE, DECORATOR)
        );
        assertTrue(world.canFurnish(GATE, DECORATOR));
        assertFalse(world.canAccessPrivateStorage(GATE, DECORATOR));

        world = world.setPrivateStorageAccess(
                GATE,
                OWNER,
                DECORATOR,
                true
        );
        assertTrue(world.canAccessPrivateStorage(GATE, DECORATOR));
        assertFalse(world.canAccessPrivateStorage(GATE, GUEST));

        world = world.setTrustedDecorator(
                GATE,
                OWNER,
                DECORATOR,
                false
        );
        assertEquals(
                R01HousingWorldState.PropertyRole.GUEST,
                world.roleFor(GATE, DECORATOR)
        );
        assertFalse(world.canAccessPrivateStorage(GATE, DECORATOR));
    }

    @Test
    void storageGrantRequiresDecoratorAndOnlyOwnerCanChangePermissions() {
        R01HousingWorldState world = ownedGate();

        assertThrows(
                IllegalStateException.class,
                () -> world.setPrivateStorageAccess(
                        GATE,
                        OWNER,
                        DECORATOR,
                        true
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> world.setTrustedDecorator(
                        GATE,
                        GUEST,
                        DECORATOR,
                        true
                )
        );
    }

    @Test
    void moveClearsOldHousePermissionsAndNewHouseStartsClean() {
        R01HousingWorldState world = ownedGate()
                .setTrustedDecorator(GATE, OWNER, DECORATOR, true)
                .setPrivateStorageAccess(GATE, OWNER, DECORATOR, true);

        R01HousingPlayerState personal = new R01HousingPlayerState(
                1,
                Optional.of(GATE),
                Optional.of(R01HomeStorageState.empty(54)),
                1L,
                Optional.empty()
        ).prepareMove(
                OWNER,
                MARKET,
                72,
                9000,
                1920,
                7080
        );
        var pending = personal.pendingMove().orElseThrow();

        world = world.reserve(
                MARKET,
                OWNER,
                pending.transactionId()
        ).state();
        world = world.transferReserved(pending, OWNER);

        assertTrue(world.owner(GATE).isEmpty());
        assertEquals(OWNER, world.owner(MARKET).orElseThrow());
        assertTrue(world.trustedDecorators().isEmpty());
        assertTrue(world.privateStorageAccess().isEmpty());
        assertEquals(
                R01HousingWorldState.PropertyRole.GUEST,
                world.roleFor(MARKET, DECORATOR)
        );
    }

    @Test
    void permissionFieldsAreBackwardCompatibleWithOlderHousingSaveShape() {
        var legacy = JsonParser.parseString(
                """
                {
                  "housing_world_schema_version": 1,
                  "property_owners": {},
                  "reservations": {},
                  "completed_transfers": {}
                }
                """
        );
        var decoded = R01HousingWorldState.CODEC
                .parse(JsonOps.INSTANCE, legacy)
                .getOrThrow();

        assertTrue(decoded.trustedDecorators().isEmpty());
        assertTrue(decoded.privateStorageAccess().isEmpty());
    }

    private static R01HousingWorldState ownedGate() {
        R01HousingPlayerState personal =
                R01HousingPlayerState.initial().prepareMove(
                        OWNER,
                        GATE,
                        54,
                        2400,
                        0,
                        2400
                );
        var pending = personal.pendingMove().orElseThrow();

        return R01HousingWorldState.initial()
                .reserve(GATE, OWNER, pending.transactionId())
                .state()
                .transferReserved(pending, OWNER);
    }
}
