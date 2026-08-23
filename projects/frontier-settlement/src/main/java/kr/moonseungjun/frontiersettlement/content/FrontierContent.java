package kr.moonseungjun.frontiersettlement.content;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FrontierContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrontierSettlement.MOD_ID);

    public static final DeferredItem<PioneerMarkerItem> PIONEER_MARKER = ITEMS.registerItem(
            "pioneer_marker",
            PioneerMarkerItem::new,
            properties -> properties.stacksTo(1));

    private FrontierContent() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
