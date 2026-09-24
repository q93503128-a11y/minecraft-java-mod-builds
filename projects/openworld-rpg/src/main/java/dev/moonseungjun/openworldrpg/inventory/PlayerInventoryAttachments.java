package dev.moonseungjun.openworldrpg.inventory;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent project inventory/storage authority. */
public final class PlayerInventoryAttachments {
    public static final AttachmentType<PlayerInventoryState> INVENTORY =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "inventory"
                    ),
                    builder -> builder
                            .initializer(PlayerInventoryState::initial)
                            .persistent(PlayerInventoryState.CODEC)
                            .copyOnDeath()
            );

    private PlayerInventoryAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
