package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent Overworld attachment for platform cleanup across server restarts. */
public final class TemporaryPlatformWorldAttachments {
    public static final AttachmentType<TemporaryPlatformWorldState> STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "temporary_platform_world_state"),
            builder -> builder.initializer(TemporaryPlatformWorldState::initial).persistent(TemporaryPlatformWorldState.CODEC)
    );

    private TemporaryPlatformWorldAttachments() {
    }

    public static void initialize() {
        // Static initialization registers the attachment.
    }
}
