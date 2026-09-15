package kr.moonseungjun.earthtostars;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class EarthToStarsContent {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EarthToStars.MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EarthToStars.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EarthToStars.MOD_ID);

    public static final RegistryObject<Block> STARTER_FLIGHT_CORE = BLOCKS.register(
            "starter_flight_core",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.COPPER_BLOCK).strength(4.0F, 8.0F))
    );

    public static final RegistryObject<Block> STARTER_CONTROL_NODE = BLOCKS.register(
            "starter_control_node",
            () -> new StarterControlNodeBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(3.0F, 6.0F))
    );

    public static final RegistryObject<Item> STARTER_FLIGHT_CORE_ITEM = ITEMS.register(
            "starter_flight_core",
            () -> new BlockItem(STARTER_FLIGHT_CORE.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> STARTER_CONTROL_NODE_ITEM = ITEMS.register(
            "starter_control_node",
            () -> new BlockItem(STARTER_CONTROL_NODE.get(), new Item.Properties())
    );

    public static final RegistryObject<Item> STARTER_CRAFT_DEPLOYER = ITEMS.register(
            "starter_craft_deployer",
            () -> new StarterCraftDeploymentItem(new Item.Properties().stacksTo(1))
    );

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.earth_to_stars"))
                    .icon(() -> new ItemStack(STARTER_CRAFT_DEPLOYER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(STARTER_CRAFT_DEPLOYER.get());
                        output.accept(STARTER_FLIGHT_CORE_ITEM.get());
                        output.accept(STARTER_CONTROL_NODE_ITEM.get());
                    })
                    .build()
    );

    private EarthToStarsContent() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
    }
}
