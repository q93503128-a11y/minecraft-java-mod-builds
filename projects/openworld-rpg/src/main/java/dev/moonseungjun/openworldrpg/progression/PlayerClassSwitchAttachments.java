package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent server-owned class-switch transaction attachment. */
public final class PlayerClassSwitchAttachments {
    public static final AttachmentType<PlayerClassSwitchState> CLASS_SWITCH =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "class_switch"
                    ),
                    builder -> builder
                            .initializer(PlayerClassSwitchState::initial)
                            .persistent(PlayerClassSwitchState.CODEC)
                            .copyOnDeath()
            );

    private PlayerClassSwitchAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
