package kr.moonseungjun.anomalousclasses.block;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PuppetBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AnomalousClasses.MOD_ID);
    public static final DeferredHolder<Block, PuppetWorkshopBlock> WORKSHOP = BLOCKS.register(
            "puppet_workbench", () -> new PuppetWorkshopBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.WOOD)
                            .strength(2.5F).requiresCorrectToolForDrops()));

    private PuppetBlocks() {}
}
