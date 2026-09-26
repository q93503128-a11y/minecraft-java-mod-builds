package kr.moonseungjun.campfiresessions.registry;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CampfireSessions.MOD_ID);
    public static final DeferredItem<Item> ACOUSTIC_GUITAR = ITEMS.registerSimpleItem("acoustic_guitar", p -> p.stacksTo(1));
    public static final DeferredItem<BlockItem> WOODEN_CHAIR = ITEMS.registerSimpleBlockItem("wooden_chair", ModBlocks.WOODEN_CHAIR);
    private ModItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); }
}
