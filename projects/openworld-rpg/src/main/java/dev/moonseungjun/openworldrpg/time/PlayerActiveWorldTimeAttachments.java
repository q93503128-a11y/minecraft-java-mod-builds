package dev.moonseungjun.openworldrpg.time;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent personal active-world-time attachment. */
public final class PlayerActiveWorldTimeAttachments {
    public static final AttachmentType<PlayerActiveWorldTimeState> ACTIVE_WORLD_TIME =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "active_world_time"
                    ),
                    builder -> builder
                            .initializer(PlayerActiveWorldTimeState::initial)
                            .persistent(PlayerActiveWorldTimeState.CODEC)
                            .copyOnDeath()
            );

    private PlayerActiveWorldTimeAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
