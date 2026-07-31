package kr.countrysidedays.gametest;

import kr.countrysidedays.CountrysideDays;
import kr.countrysidedays.gameplay.RuralGameplayHandler;
import kr.countrysidedays.gameplay.RuralNpcManager;
import kr.countrysidedays.registry.ModBlocks;
import kr.countrysidedays.world.CountrysideWorldData;
import kr.countrysidedays.world.StarterHomesteadGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
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

        int guestsBefore = data.customersServed();
        int coinsBefore = data.villageCoinsEarned();
        long nextDay = data.lastCustomerServiceDay() + 1L;
        helper.assertTrue(
                data.recordCustomerService(nextDay, RuralNpcManager.DAILY_REWARD_COINS),
                "a new countryside day should accept one customer service"
        );
        helper.assertFalse(
                data.recordCustomerService(nextDay, RuralNpcManager.DAILY_REWARD_COINS),
                "the same daily customer must not pay twice"
        );
        helper.assertTrue(data.customersServed() == guestsBefore + 1, "guest count should increase once");
        helper.assertTrue(
                data.villageCoinsEarned() == coinsBefore + RuralNpcManager.DAILY_REWARD_COINS,
                "coin earnings should match the daily reward"
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
        BlockPos relativeOrigin = new BlockPos(20, 4, 20);
        BlockPos absoluteOrigin = helper.absolutePos(relativeOrigin);
        StarterHomesteadGenerator.buildHomestead(helper.getLevel(), absoluteOrigin);
        RuralNpcManager.ensureForHomestead(helper.getLevel(), absoluteOrigin);
        RuralNpcManager.ensureForHomestead(helper.getLevel(), absoluteOrigin);

        helper.assertBlockPresent(ModBlocks.COUNTRY_KITCHEN_COUNTER.get(), new BlockPos(10, 5, 14));
        helper.assertBlockPresent(Blocks.FURNACE, new BlockPos(9, 5, 14));
        helper.assertBlockPresent(Blocks.FARMLAND, new BlockPos(24, 4, 13));
        helper.assertBlockPresent(Blocks.WATER, new BlockPos(27, 4, 27));
        helper.assertBlockPresent(Blocks.DEEPSLATE_TILES, new BlockPos(7, 9, 11));
        helper.assertBlockPresent(Blocks.PACKED_MUD, new BlockPos(13, 3, 22));

        AABB area = new AABB(
                absoluteOrigin.getX() - 14.0,
                absoluteOrigin.getY() - 2.0,
                absoluteOrigin.getZ() - 12.0,
                absoluteOrigin.getX() + 14.0,
                absoluteOrigin.getY() + 8.0,
                absoluteOrigin.getZ() + 12.0
        );
        List<Villager> villagers = helper.getLevel().getEntitiesOfClass(Villager.class, area);
        helper.assertTrue(villagers.size() == 2, "homestead should contain exactly two managed villagers");
        helper.assertTrue(
                villagers.stream().anyMatch(villager -> RuralNpcManager.RESIDENT_NAME.equals(villager.getName().getString())),
                "resident guide should spawn"
        );
        helper.assertTrue(
                villagers.stream().anyMatch(villager -> RuralNpcManager.CUSTOMER_NAME.equals(villager.getName().getString())),
                "daily customer should spawn"
        );

        helper.succeed();
    }
}
