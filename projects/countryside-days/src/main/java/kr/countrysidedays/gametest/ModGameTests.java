package kr.countrysidedays.gametest;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.gameplay.RuralGameplayHandler;
import kr.countrysidedays.gameplay.RuralNpcManager;
import kr.countrysidedays.registry.ModBlocks;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.PlayerEstateLayout;
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
        helper.assertTrue(firstAllocation.estate().isOwner(first), "first UUID should own the first estate");
        helper.assertTrue(secondAllocation.estate().isOwner(second), "second UUID should own the second estate");
        helper.assertFalse(
                firstAllocation.estate().originPos().equals(secondAllocation.estate().originPos()),
                "multiplayer estates must never overlap at one origin"
        );
        helper.assertTrue(
                PlayerEstateLayout.contains(firstAllocation.estate().originPos(), firstAllocation.estate().originPos()),
                "estate origin should be inside its own protected boundary"
        );
        helper.assertFalse(
                PlayerEstateLayout.contains(firstAllocation.estate().originPos(), secondAllocation.estate().originPos()),
                "a second estate origin must be outside the first protected boundary"
        );

        helper.assertTrue(data.renameRestaurant(first, "느린 오후 식당"), "owner should rename only their restaurant");
        helper.assertTrue(
                "느린 오후 식당".equals(data.estate(first).orElseThrow().restaurantName()),
                "renamed restaurant should persist for its owner"
        );
        helper.assertFalse(
                "느린 오후 식당".equals(data.estate(second).orElseThrow().restaurantName()),
                "another player's restaurant name must remain independent"
        );

        long day = 12L;
        helper.assertTrue(
                data.recordCustomerService(first, day, RuralNpcManager.DAILY_REWARD_COINS),
                "first owner should serve their daily customer"
        );
        helper.assertFalse(
                data.recordCustomerService(first, day, RuralNpcManager.DAILY_REWARD_COINS),
                "the same owner's customer must not pay twice in one day"
        );
        helper.assertTrue(
                data.recordCustomerService(second, day, RuralNpcManager.DAILY_REWARD_COINS),
                "second owner should have an independent daily customer"
        );

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

        helper.assertBlockPresent(ModBlocks.COUNTRY_KITCHEN_COUNTER.get(), new BlockPos(45, 7, 18));
        helper.assertBlockPresent(Blocks.CHEST, new BlockPos(10, 7, 15));
        helper.assertBlockPresent(Blocks.BED.pick(DyeColor.YELLOW), new BlockPos(10, 7, 21));
        helper.assertBlockPresent(Blocks.BED.pick(DyeColor.YELLOW), new BlockPos(10, 7, 22));
        helper.assertBlockProperty(new BlockPos(10, 7, 21), BedBlock.FACING, Direction.SOUTH);
        helper.assertBlockPresent(Blocks.GLASS, new BlockPos(10, 8, 13));

        helper.assertBlockPresent(Blocks.FARMLAND, new BlockPos(9, 6, 36));
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(17, 6, 42));
        helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(28, 6, 41));

        helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(35, 6, 6));
        helper.assertBlockProperty(
                new BlockPos(35, 6, 6),
                BlockStateProperties.HORIZONTAL_FACING,
                Direction.NORTH
        );
        helper.assertBlockPresent(Blocks.OAK_SIGN, new BlockPos(38, 6, 5));
        helper.assertBlockPresent(Blocks.OAK_WALL_SIGN, new BlockPos(46, 8, 24));
        helper.assertBlockProperty(
                new BlockPos(46, 8, 24),
                BlockStateProperties.HORIZONTAL_FACING,
                Direction.SOUTH
        );

        helper.assertBlockPresent(Blocks.OAK_STAIRS, new BlockPos(49, 7, 20));
        helper.assertBlockProperty(new BlockPos(49, 7, 20), StairBlock.FACING, Direction.NORTH);

        helper.assertBlockPresent(Blocks.OAK_FENCE_GATE, new BlockPos(42, 6, 34));
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(44, 6, 55));
        helper.assertBlockPresent(Blocks.HAY_BLOCK, new BlockPos(58, 6, 54));
        helper.assertBlockPresent(Blocks.BRICKS, new BlockPos(45, 12, 37));
        helper.assertBlockPresent(Blocks.PACKED_MUD, new BlockPos(35, 5, 5));

        helper.succeed();
    }
}
