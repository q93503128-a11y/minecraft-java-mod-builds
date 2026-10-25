package dev.moonseungjun.augmentationsurvival.device;

import dev.moonseungjun.augmentationsurvival.menu.AugmentDeviceMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Portable shell for the augmentation device.
 *
 * <p>The block registry is intentionally kept for existing-world compatibility, but the item no
 * longer places that block. Right-clicking in the air or on a block opens the server-authoritative
 * augmentation menu directly.</p>
 */
public final class PortableAugmentDeviceItem extends BlockItem {
    private final boolean advanced;

    public PortableAugmentDeviceItem(Block block, boolean advanced, Item.Properties properties) {
        super(block, properties);
        this.advanced = advanced;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        return open(level, player);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        return open(context.getLevel(), player);
    }

    private InteractionResult open(Level level, Player player) {
        if (!level.isClientSide()) {
            Component title = Component.translatable(
                    advanced
                            ? "container.augmentation_survival.advanced_augment_device"
                            : "container.augmentation_survival.augment_device"
            );
            player.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, menuPlayer) -> new AugmentDeviceMenu(
                            containerId,
                            inventory,
                            ContainerLevelAccess.NULL,
                            advanced
                    ),
                    title
            ));
        }
        return InteractionResult.SUCCESS;
    }
}
