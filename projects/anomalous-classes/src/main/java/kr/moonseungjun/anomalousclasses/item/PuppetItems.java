package kr.moonseungjun.anomalousclasses.item;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import kr.moonseungjun.anomalousclasses.block.PuppetWorkshopBlock;
import kr.moonseungjun.anomalousclasses.block.PuppetBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PuppetItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, AnomalousClasses.MOD_ID);
    public static final DeferredHolder<Item, PuppetItem> PUPPET = ITEMS.register(
            "assembled_marionette", () -> new PuppetItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, PuppetPartItem> PART = ITEMS.register(
            "puppet_module", () -> new PuppetPartItem(new Item.Properties().stacksTo(1)));
    public static final DeferredHolder<Item, Item> WORKSHOP = ITEMS.register(
            "puppet_workbench", () -> new BlockItem(PuppetBlocks.WORKSHOP.get(),
                    new Item.Properties()));

    private PuppetItems() {}
}
