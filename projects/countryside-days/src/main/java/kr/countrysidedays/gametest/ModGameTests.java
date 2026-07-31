package kr.countrysidedays.gametest;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.world.CountrysideWorldData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public final class ModGameTests {
    public static final DeferredRegister<Consumer<GameTestHelper>> TEST_FUNCTIONS = DeferredRegister.create(
            BuiltInRegistries.TEST_FUNCTION,
            CountrysideDays.MOD_ID
    );

    public static final DeferredHolder<Consumer<GameTestHelper>, Consumer<GameTestHelper>> WORLD_DATA_ROUND_TRIP =
            TEST_FUNCTIONS.register("world_data_round_trip", () -> ModGameTests::worldDataRoundTrip);

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
        helper.assertTrue(data.consumeHerbPreparation(testPos), "prepared herb should be consumable");
        helper.assertFalse(data.hasHerbPreparation(testPos), "consumed herb state should be removed");

        helper.succeed();
    }
}
