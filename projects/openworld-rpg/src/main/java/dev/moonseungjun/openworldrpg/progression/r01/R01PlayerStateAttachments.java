package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent personal R01 state attachment. */
public final class R01PlayerStateAttachments {
    public static final AttachmentType<R01PlayerState> R01_PLAYER_STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_player_state"
                    ),
                    builder -> builder
                            .initializer(R01PlayerState::initial)
                            .persistent(R01PlayerState.CODEC)
                            .copyOnDeath()
            );

    private R01PlayerStateAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
