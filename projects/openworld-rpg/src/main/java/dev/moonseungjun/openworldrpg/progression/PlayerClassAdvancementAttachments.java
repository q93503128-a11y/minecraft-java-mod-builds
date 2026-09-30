package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class PlayerClassAdvancementAttachments {
    public static final AttachmentType<PlayerClassAdvancementState> CLASS_ADVANCEMENT =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "class_advancement"
                    ),
                    builder -> builder
                            .initializer(PlayerClassAdvancementState::initial)
                            .persistent(PlayerClassAdvancementState.CODEC)
                            .copyOnDeath()
            );

    private PlayerClassAdvancementAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
