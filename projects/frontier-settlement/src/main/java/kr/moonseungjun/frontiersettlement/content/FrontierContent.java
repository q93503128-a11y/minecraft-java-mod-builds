package kr.moonseungjun.frontiersettlement.content;

import kr.moonseungjun.frontiersettlement.FrontierSettlement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FrontierContent {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(FrontierSettlement.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(FrontierSettlement.MOD_ID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(FrontierSettlement.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, FrontierSettlement.MOD_ID);

    public static final DeferredBlock<SupplyDepotBlock> SUPPLY_DEPOT = BLOCKS.registerBlock(
            "supply_depot",
            SupplyDepotBlock::new,
            properties -> properties.strength(3.5F).sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK));

    public static final DeferredItem<BlockItem> SUPPLY_DEPOT_ITEM = ITEMS.registerSimpleBlockItem(SUPPLY_DEPOT);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SupplyDepotBlockEntity>> SUPPLY_DEPOT_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("supply_depot",
                    () -> new BlockEntityType<>(SupplyDepotBlockEntity::new, false, SUPPLY_DEPOT.get()));

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

    public static final DeferredHolder<EntityType<?>, EntityType<FrontierWorkerEntity>> FRONTIER_WORKER =
            ENTITIES.registerEntityType(
                    "frontier_worker",
                    FrontierWorkerEntity::new,
                    MobCategory.CREATURE,
                    builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10));

    private FrontierContent() {}

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(FrontierContent::onEntityAttributes);
        modBus.addListener(FrontierContent::onCreativeTabContents);
    }

    private static void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(FRONTIER_SOLDIER.get(), IronGolem.createAttributes().build());
        event.put(FRONTIER_WORKER.get(), FrontierWorkerEntity.createAttributes().build());
    }

    private static void onCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(SUPPLY_DEPOT_ITEM.get());
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(PIONEER_MARKER.get());
        }
    }
}
