package kr.moonseungjun.livingkingdoms.item;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Materials that belong to Living Kingdoms fantasy wildlife rather than vanilla stand-ins. */
public final class FantasyItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LivingKingdoms.MOD_ID);

    public static final DeferredItem<Item> SILVER_ANTLER = ITEMS.registerSimpleItem("silver_antler");
    public static final DeferredItem<Item> ASH_FANG = ITEMS.registerSimpleItem("ash_fang");
    public static final DeferredItem<Item> RIVER_ESSENCE = ITEMS.registerSimpleItem("river_essence");

    private FantasyItems() {
    }

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }
}
