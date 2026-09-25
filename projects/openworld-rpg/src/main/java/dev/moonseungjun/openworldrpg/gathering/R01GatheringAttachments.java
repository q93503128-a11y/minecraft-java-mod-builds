package dev.moonseungjun.openworldrpg.gathering;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent personal R01 gathering attachment. */
public final class R01GatheringAttachments {
    public static final AttachmentType<R01GatheringState> GATHERING =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_gathering"
                    ),
                    builder -> builder
                            .initializer(R01GatheringState::initial)
                            .persistent(R01GatheringState.CODEC)
                            .copyOnDeath()
            );

    private R01GatheringAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
