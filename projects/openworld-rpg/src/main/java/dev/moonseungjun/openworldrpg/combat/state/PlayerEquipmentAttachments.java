package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class PlayerEquipmentAttachments {
    public static final AttachmentType<PlayerEquipmentLoadoutState> EQUIPPED_LOADOUT =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "equipped_loadout"
                    ),
                    builder -> builder
                            .initializer(PlayerEquipmentLoadoutState::empty)
                            .persistent(PlayerEquipmentLoadoutState.CODEC)
                            .copyOnDeath()
            );

    private PlayerEquipmentAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
