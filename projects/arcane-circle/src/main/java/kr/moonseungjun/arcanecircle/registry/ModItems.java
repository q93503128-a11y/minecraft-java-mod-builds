package kr.moonseungjun.arcanecircle.registry;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArcaneCircle.MOD_ID);

    public static final DeferredItem<BlockItem> MAGIC_CIRCLE = ITEMS.registerSimpleBlockItem(
            "magic_circle",
            ModBlocks.MAGIC_CIRCLE
    );

    private ModItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
