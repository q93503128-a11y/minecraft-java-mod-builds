package kr.moonseungjun.campfiresessions.registry;

import kr.moonseungjun.campfiresessions.CampfireSessions;
import kr.moonseungjun.campfiresessions.block.ChairBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CampfireSessions.MOD_ID);

    public static final DeferredBlock<ChairBlock> WOODEN_CHAIR = BLOCKS.registerBlock(
            "wooden_chair",
            ChairBlock::new,
            properties -> properties.mapColor(MapColor.WOOD).strength(1.5F).sound(SoundType.WOOD).noOcclusion()
    );

    private ModBlocks() {}

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}
