package kr.countrysidedays.gametest;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.gameplay.RuralGameplayHandler;
import kr.countrysidedays.gameplay.SharedRestaurantAccess;
import kr.countrysidedays.gameplay.VillageLifeManager;
import kr.countrysidedays.registry.ModBlocks;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
import kr.countrysidedays.world.SharedRestaurantBuilder;
import kr.countrysidedays.world.StarterHomesteadGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Consumer;

public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
            BuiltInRegistries.TEST_FUNCTION,
            CountrysideDays.MOD_ID
    );

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLD_DATA_ROUND_TRIP =
            TEST_FUNCTIONS.register("world_data_round_trip", () -> ModGameTests::worldDataRoundTrip);

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> HOMESTEAD_LAYOUT =
            TEST_FUNCTIONS.register("homestead_layout", () -> ModGameTests::homesteadLayout);

    private ModGameTests() {
    }

    public static void register(IEventBus modEventBus) {
        TEST_FUNCTIONS.register(modEventBus);
    }

    private static void worldDataRoundTrip(GameTestHelper helper) {
        BlockPos testPos = helper.absolutePos(new BlockPos(1, 1, 1));
        CountrysideWorldData data = CountrysideWorldData.get(helper.getLevel().getServer());

        data.removeKitchenState(testPos);
        helper.assertBlockPresent(Blocks.STONE, new BlockPos(0, 0, 0));
        helper.assertTrue(data.addHerbPreparation(testPos), "fresh counter should accept herb preparation");
        helper.assertTrue(data.hasHerbPreparation(testPos), "prepared herb state should be queryable");
        helper.assertTrue(data.removeKitchenState(testPos), "counter removal should clear temporary cooking state");
        helper.assertFalse(data.hasHerbPreparation(testPos), "removed counter must not retain herb preparation");

        UUID first = UUID.fromString("8be03a48-1c0d-4fe0-b4a5-201f95bdb600");
        UUID second = UUID.fromString("d0de18fd-c2ce-4392-a9e2-903dc6b8892d");
        BlockPos village = helper.absolutePos(new BlockPos(10, 6, 10));

        CountrysideWorldData.EstateAllocation firstAllocation = data.ensureEstate(first, "첫 주민", village);
        CountrysideWorldData.EstateAllocation secondAllocation = data.ensureEstate(second, "둘째 주민", village);
        helper.assertTrue(firstAllocation.estate().isOwner(first), "first UUID should own its estate");
        helper.assertTrue(secondAllocation.estate().isOwner(second), "second UUID should own its estate");
        helper.assertFalse(
                firstAllocation.estate().originPos().equals(secondAllocation.estate().originPos()),
                "multiplayer private homes, farms and ranches must never overlap"
        );
        helper.assertFalse(
                PlayerEstateLayout.contains(firstAllocation.estate().originPos(), secondAllocation.estate().originPos()),
                "the second private estate must remain outside the first protected boundary"
        );
        helper.assertTrue(
                PlayerEstateLayout.contains(
                        firstAllocation.estate().originPos(),
                        PlayerEstateLayout.ownerSign(firstAllocation.estate().originPos())
                ),
                "the ownership sign in front of the fence must remain protected"
        );

        helper.assertTrue(SharedRestaurantAccess.isOwner(data, first),
                "the first estate owner should own the world's shared restaurant");
        helper.assertTrue(SharedRestaurantAccess.isStaff(data, second),
                "later estate owners should be registered restaurant staff");
        helper.assertTrue(
                SharedRestaurantAccess.restaurantEstate(data).orElseThrow().ownerUuid().equals(first.toString()),
                "the shared restaurant should remain attached to the first estate for save compatibility"
        );

        helper.assertTrue(data.renameRestaurant(first, "느린 오후 식당"),
                "the restaurant owner should be able to rename the shared restaurant");
        helper.assertTrue(
                "느린 오후 식당".equals(data.estate(first).orElseThrow().restaurantName()),
                "the shared restaurant name should persist on its owner record"
        );
        helper.assertFalse(
                "느린 오후 식당".equals(data.estate(second).orElseThrow().restaurantName()),
                "a staff estate must not be converted into a second restaurant"
        );

        helper.assertTrue(SharedRestaurantAccess.toggleOpen(data, second).orElse(false),
                "registered staff should be able to open the shared restaurant");
        helper.assertTrue(data.estate(first).orElseThrow().restaurantOpen(),
                "staff opening should update the shared owner record");
        helper.assertFalse(data.estate(second).orElseThrow().restaurantOpen(),
                "staff must not create an independent restaurant state");

        long day = 112L;
        helper.assertTrue(SharedRestaurantAccess.recordCustomerService(data, day, 0, 5),
                "shared customer slot zero should pay once");
        helper.assertTrue(SharedRestaurantAccess.recordCustomerService(data, day, 1, 3),
                "shared customer slot one should be independent");
        helper.assertTrue(SharedRestaurantAccess.recordCustomerService(data, day, 2, 6),
                "shared customer slot two should be independent");
        helper.assertFalse(SharedRestaurantAccess.recordCustomerService(data, day, 1, 3),
                "the same shared customer must never pay twice in one day");
        helper.assertTrue(
                SharedRestaurantAccess.restaurantEstate(data).orElseThrow().customersServedToday(day) == 3,
                "the shared daily service mask should record all three guests"
        );

        long nextDay = 113L;
        helper.assertTrue(SharedRestaurantAccess.recordCustomerService(data, nextDay, 0, 5),
                "a new day should reset the shared customer mask");
        helper.assertTrue(SharedRestaurantAccess.recordCustomerService(data, nextDay, 1, 3),
                "five shared guests should advance restaurant progression");
        helper.assertTrue(
                SharedRestaurantAccess.restaurantEstate(data).orElseThrow().progressionStage() == 2,
                "five shared guests should advance to the ranch collection stage"
        );
        SharedRestaurantAccess.setOpen(data, false);
        helper.assertFalse(data.estate(first).orElseThrow().restaurantOpen(),
                "closing should update the single shared restaurant state");

        helper.assertTrue(
                data.recordRanchProduction(second, nextDay, 2, 1, 1),
                "each staff player should retain independent ranch production"
        );
        helper.assertFalse(
                data.recordRanchProduction(second, nextDay, 9, 9, 9),
                "the same private ranch production day must not duplicate goods"
        );
        CountrysideWorldData.RanchProducts claimed = data.claimRanchProducts(second);
        helper.assertTrue(
                claimed.eggs() == 2 && claimed.milk() == 1 && claimed.wool() == 1,
                "claimed ranch goods should match the staff player's private ranch stock"
        );
        helper.assertTrue(data.claimRanchProducts(second).total() == 0,
                "claiming the same private ranch stock twice must return nothing");

        helper.assertFalse(VillageLifeManager.isHoliday(5L), "sixth day should still be a workday");
        helper.assertTrue(VillageLifeManager.isHoliday(6L), "seventh day should be a village holiday");
        helper.assertTrue(VillageLifeManager.isHoliday(13L), "holiday cycle should repeat every seven days");
        int stablePrice = VillageLifeManager.dailyCoinPrice(4, 22L, 9);
        helper.assertTrue(stablePrice == VillageLifeManager.dailyCoinPrice(4, 22L, 9),
                "daily market price must be deterministic for one day and item");
        helper.assertTrue(stablePrice >= 1 && stablePrice <= 6,
                "daily market price must stay inside the safe fluctuation range");
        for (long marketDay = 0; marketDay < 70; marketDay++) {
            helper.assertTrue(VillageLifeManager.isProduceArbitrageSafe(marketDay, 6),
                    "buying and immediately reselling produce must never create infinite coins");
        }

        helper.assertTrue(
                RuralGameplayHandler.isForagePlant(Blocks.SHORT_GRASS.defaultBlockState()),
                "short grass should be a forage source"
        );
        helper.assertTrue(
                RuralGameplayHandler.isForagePlant(Blocks.FERN.defaultBlockState()),
                "fern should be a forage source"
        );
        helper.assertFalse(
                RuralGameplayHandler.isForagePlant(Blocks.STONE.defaultBlockState()),
                "stone must not be a forage source"
        );

        helper.succeed();
    }

    private static void homesteadLayout(GameTestHelper helper) {
        BlockPos relativeOrigin = new BlockPos(35, 6, 32);
        BlockPos absoluteOrigin = helper.absolutePos(relativeOrigin);
        StarterHomesteadGenerator.buildPlayerEstate(
                helper.getLevel(), absoluteOrigin, "테스트 주민", "테스트 식당"
        );
        VillageLifeManager.prepareNewEstate(helper.getLevel(), absoluteOrigin);
        SharedRestaurantBuilder.buildSharedRestaurant(
                helper.getLevel(), absoluteOrigin, "테스트 주민", "테스트 식당"
        );

        helper.assertBlockPresent(Blocks.CHEST, new BlockPos(10, 7, 15));
        helper.assertBlockPresent(Blocks.BED.pick(DyeColor.YELLOW), new BlockPos(10, 7, 21));
        helper.assertBlockPresent(Blocks.BED.pick(DyeColor.YELLOW), new BlockPos(10, 7, 22));
        helper.assertBlockProperty(new BlockPos(10, 7, 21), BedBlock.FACING, Direction.SOUTH);
        helper.assertBlockPresent(Blocks.GLASS, new BlockPos(10, 8, 13));

        helper.assertBlockPresent(Blocks.FARMLAND, new BlockPos(9, 6, 36));
        helper.assertBlockPresent(Blocks.AIR, new BlockPos(9, 7, 36));
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(17, 6, 42));
        helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(28, 6, 41));
        helper.assertBlockPresent(Blocks.BARREL, new BlockPos(26, 6, 50));

        helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(35, 6, 6));
        helper.assertBlockProperty(
                new BlockPos(35, 6, 6),
                BlockStateProperties.HORIZONTAL_FACING,
                Direction.NORTH
        );
        helper.assertBlockPresent(Blocks.OAK_SIGN, new BlockPos(38, 6, 5));

        helper.assertBlockPresent(ModBlocks.COUNTRY_KITCHEN_COUNTER.get(), new BlockPos(45, 7, 22));
        helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(52, 6, 9));
        helper.assertBlockProperty(
                new BlockPos(52, 6, 9),
                BlockStateProperties.HORIZONTAL_FACING,
                Direction.NORTH
        );
        helper.assertBlockProperty(
                new BlockPos(52, 6, 9),
                BlockStateProperties.OPEN,
                false
        );
        helper.assertBlockPresent(Blocks.SPRUCE_DOOR, new BlockPos(52, 7, 12));
        helper.assertBlockProperty(
                new BlockPos(52, 7, 12),
                BlockStateProperties.HORIZONTAL_FACING,
                Direction.NORTH
        );
        helper.assertBlockProperty(
                new BlockPos(52, 7, 12),
                BlockStateProperties.OPEN,
                false
        );
        helper.assertBlockPresent(Blocks.OAK_WALL_SIGN, new BlockPos(46, 8, 11));
        helper.assertBlockProperty(
                new BlockPos(46, 8, 11),
                BlockStateProperties.HORIZONTAL_FACING,
                Direction.NORTH
        );

        helper.assertBlockPresent(Blocks.OAK_STAIRS, new BlockPos(47, 7, 17));
        helper.assertBlockProperty(new BlockPos(47, 7, 17), StairBlock.FACING, Direction.SOUTH);
        helper.assertBlockPresent(Blocks.OAK_STAIRS, new BlockPos(47, 7, 19));
        helper.assertBlockProperty(new BlockPos(47, 7, 19), StairBlock.FACING, Direction.NORTH);
        helper.assertBlockPresent(Blocks.OAK_STAIRS, new BlockPos(52, 7, 17));
        helper.assertBlockProperty(new BlockPos(52, 7, 17), StairBlock.FACING, Direction.SOUTH);
        helper.assertBlockPresent(Blocks.OAK_STAIRS, new BlockPos(57, 7, 17));
        helper.assertBlockProperty(new BlockPos(57, 7, 17), StairBlock.FACING, Direction.SOUTH);

        SharedRestaurantBuilder.setOpen(helper.getLevel(), absoluteOrigin, true);
        helper.assertBlockProperty(
                new BlockPos(52, 6, 9),
                BlockStateProperties.OPEN,
                true
        );
        helper.assertBlockProperty(
                new BlockPos(52, 7, 12),
                BlockStateProperties.OPEN,
                true
        );
        SharedRestaurantBuilder.setOpen(helper.getLevel(), absoluteOrigin, false);

        helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(42, 6, 34));
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(44, 6, 55));
        helper.assertBlockPresent(Blocks.HAY_BLOCK, new BlockPos(58, 6, 54));
        helper.assertBlockPresent(Blocks.BARREL, new BlockPos(47, 6, 49));
        helper.assertBlockPresent(Blocks.BARREL, new BlockPos(59, 6, 49));
        helper.assertBlockPresent(Blocks.BRICKS, new BlockPos(45, 12, 37));
        helper.assertBlockPresent(Blocks.PACKED_MUD, new BlockPos(35, 5, 5));

        helper.succeed();
    }
}
