package kr.moonseungjun.anomalousclasses.block;

import kr.moonseungjun.anomalousclasses.AnomalousClasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Altar cores are world landmarks; there is no ordinary survival crafting recipe. */
public final class PactBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, AnomalousClasses.MOD_ID);
    public static final DeferredHolder<Block, PactAltarBlock> ALTAR_CORE = BLOCKS.register(
            "pact_altar_core", () -> new PactAltarBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                            .strength(-1.0F, 3600000F).noLootTable()));
    private PactBlocks() {}
}
