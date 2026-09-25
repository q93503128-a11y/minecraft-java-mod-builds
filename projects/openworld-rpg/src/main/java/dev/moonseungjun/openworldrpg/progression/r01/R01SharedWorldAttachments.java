package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent shared R01 world-loop state stored on the server Overworld. */
public final class R01SharedWorldAttachments {
    public static final AttachmentType<R01SharedWorldState> SHARED_R01 =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "shared_r01_world"
                    ),
                    builder -> builder
                            .initializer(R01SharedWorldState::initial)
                            .persistent(R01SharedWorldState.CODEC)
            );

    private R01SharedWorldAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
