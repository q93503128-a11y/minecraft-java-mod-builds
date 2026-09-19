package io.github.q93503128.turnbound.presentation;

import io.github.q93503128.turnbound.Turnbound;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Visual-only held weapon items used by production battle actors. */
public final class TurnboundVisualItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Turnbound.MOD_ID);

    public static final DeferredItem<Item> CV_B_IRON_CLEAVER = ITEMS.registerSimpleItem("cv_b_iron_cleaver");
    public static final DeferredItem<Item> CV_C_OAK_LONGBOW = ITEMS.registerSimpleItem("cv_c_oak_longbow");

    private TurnboundVisualItems() {}

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
