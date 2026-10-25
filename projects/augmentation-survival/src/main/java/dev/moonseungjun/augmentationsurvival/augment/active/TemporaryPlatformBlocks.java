package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** System-created temporary platform block. It has no item form and cannot be harvested for resources. */
public final class TemporaryPlatformBlocks {
    private static final ResourceKey<Block> TEMPORARY_PLATFORM_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "temporary_platform")
    );

    public static final Block TEMPORARY_PLATFORM = Registry.register(
            BuiltInRegistries.BLOCK,
            TEMPORARY_PLATFORM_KEY,
            new Block(BlockBehaviour.Properties.of()
                    .setId(TEMPORARY_PLATFORM_KEY)
                    .noOcclusion()
                    .strength(-1.0F, 3_600_000.0F)
                    .lightLevel(state -> 8)
                    .sound(SoundType.AMETHYST))
    );

    private TemporaryPlatformBlocks() {
    }

    public static void initialize() {
        // Static initialization performs registration. The block is system-created and has no item form.
    }
}
