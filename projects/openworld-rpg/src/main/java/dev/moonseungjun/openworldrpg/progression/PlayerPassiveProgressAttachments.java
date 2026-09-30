package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class PlayerPassiveProgressAttachments {
    public static final AttachmentType<PlayerPassiveProgressState> PASSIVE_PROGRESS =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, "passive_progress"),
                    builder -> builder
                            .initializer(PlayerPassiveProgressState::initial)
                            .persistent(PlayerPassiveProgressState.CODEC)
                            .copyOnDeath()
            );

    private PlayerPassiveProgressAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
