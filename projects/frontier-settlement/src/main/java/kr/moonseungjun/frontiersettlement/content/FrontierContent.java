package kr.moonseungjun.frontiersettlement.content;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FrontierContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrontierSettlement.MOD_ID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(FrontierSettlement.MOD_ID);

    public static final DeferredItem<PioneerMarkerItem> PIONEER_MARKER = ITEMS.registerItem(
            "pioneer_marker",
            PioneerMarkerItem::new,
            properties -> properties.stacksTo(1));

    public static final DeferredHolder<EntityType<?>, EntityType<FrontierSoldierEntity>> FRONTIER_SOLDIER =
            ENTITIES.registerEntityType(
                    "frontier_soldier",
                    FrontierSoldierEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10));

    private FrontierContent() {}

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener(FrontierContent::onEntityAttributes);
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        // Keep the proven combat body exactly at Iron Golem attribute strength; Alpha.48 changes presentation,
        // collision silhouette and migration identity, not military slot count or recruitment combat economics.
        event.put(FRONTIER_SOLDIER.get(), IronGolem.createAttributes().build());
    }
}
