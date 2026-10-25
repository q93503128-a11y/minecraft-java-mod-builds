package dev.moonseungjun.augmentationsurvival.augment.spatial;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.network.SpatialLinkNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Physical anchor that represents a permanent Spatial Link gate in the world. */
public final class SpatialGateBlocks {
    private static final ResourceKey<Block> SPATIAL_GATE_KEY = ResourceKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "spatial_gate")
    );

    public static final Block SPATIAL_GATE = Registry.register(
            BuiltInRegistries.BLOCK,
            SPATIAL_GATE_KEY,
            new SpatialGateBlock(BlockBehaviour.Properties.of()
                    .setId(SPATIAL_GATE_KEY)
                    .noOcclusion()
                    .strength(-1.0F, 3_600_000.0F)
                    .lightLevel(state -> 10)
                    .sound(SoundType.AMETHYST))
    );

    private SpatialGateBlocks() { }

    public static void initialize() {
        // Static initialization performs registration. The anchor is system-created and has no item form.
    }

    private static final class SpatialGateBlock extends Block {
        private SpatialGateBlock(BlockBehaviour.Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(
                BlockState state,
                Level level,
                BlockPos pos,
                Player player,
                BlockHitResult hitResult
        ) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                if (SpatialLinkService.hasAccess(serverPlayer)) {
                    SpatialLinkNetworking.sendSnapshot(serverPlayer);
                    level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.55F, 1.55F);
                }
            }
            return InteractionResult.SUCCESS;
        }
    }
}
