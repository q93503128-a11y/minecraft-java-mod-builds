package dev.moonseungjun.augmentationsurvival.device;

import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** Physical workstation shell for core draw/selection and, when advanced, 3:1 upgrades. */
public final class AugmentDeviceBlock extends Block {
    private final boolean advanced;

    public AugmentDeviceBlock(boolean advanced, BlockBehaviour.Properties properties) {
        super(properties);
        this.advanced = advanced;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        Component title = Component.translatable(
                advanced
                        ? "container.augmentation_survival.advanced_augment_device"
                        : "container.augmentation_survival.augment_device"
        );
        return new SimpleMenuProvider(
                (containerId, inventory, player) -> new AugmentDeviceMenu(
                        containerId,
                        inventory,
                        ContainerLevelAccess.create(level, pos),
                        advanced
                ),
                title
        );
    }
}
