package kr.moonseungjun.arcanecircle.registry;

import kr.moonseungjun.arcanecircle.ArcaneCircle;
import kr.moonseungjun.arcanecircle.block.MagicCircleBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ArcaneCircle.MOD_ID);

    public static final DeferredBlock<MagicCircleBlock> MAGIC_CIRCLE = BLOCKS.registerBlock(
            "magic_circle",
            MagicCircleBlock::new,
            properties -> properties
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.2F)
                    .sound(SoundType.AMETHYST)
                    .lightLevel(state -> 8)
                    .noOcclusion()
    );

    private ModBlocks() {
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
    }
}
