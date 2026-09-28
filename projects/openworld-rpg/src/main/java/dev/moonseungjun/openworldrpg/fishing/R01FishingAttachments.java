package dev.moonseungjun.openworldrpg.fishing;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent personal R01 fishing attachment. */
public final class R01FishingAttachments {
    public static final AttachmentType<R01FishingState> FISHING =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_fishing"
                    ),
                    builder -> builder
                            .initializer(R01FishingState::initial)
                            .persistent(R01FishingState.CODEC)
                            .copyOnDeath()
            );

    private R01FishingAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
