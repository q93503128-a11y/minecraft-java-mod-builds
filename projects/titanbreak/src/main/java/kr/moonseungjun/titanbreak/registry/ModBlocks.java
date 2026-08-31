package kr.moonseungjun.titanbreak.registry;

import kr.moonseungjun.titanbreak.Titanbreak;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Titanbreak.MOD_ID);

    public static final DeferredBlock<Block> FABRICATOR_I = BLOCKS.registerSimpleBlock(
            "fabricator_i", properties -> properties.strength(3.5F, 8.0F).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> FABRICATOR_II = BLOCKS.registerSimpleBlock(
            "fabricator_ii", properties -> properties.strength(4.2F, 10.0F).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> FABRICATOR_III = BLOCKS.registerSimpleBlock(
            "fabricator_iii", properties -> properties.strength(5.0F, 14.0F).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> SURGICAL_BAY = BLOCKS.registerSimpleBlock(
            "surgical_bay", properties -> properties.strength(4.0F, 10.0F).requiresCorrectToolForDrops());
    public static final DeferredBlock<Block> IMPLANT_VAULT = BLOCKS.registerSimpleBlock(
            "implant_vault", properties -> properties.strength(4.5F, 12.0F).requiresCorrectToolForDrops());

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
