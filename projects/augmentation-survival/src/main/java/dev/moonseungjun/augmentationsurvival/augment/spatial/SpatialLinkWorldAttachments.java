package dev.moonseungjun.augmentationsurvival.augment.spatial;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent Overworld attachment containing the server-owned spatial network. */
public final class SpatialLinkWorldAttachments {
    public static final AttachmentType<SpatialLinkWorldState> STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "spatial_link_world_state"),
            builder -> builder.initializer(SpatialLinkWorldState::initial).persistent(SpatialLinkWorldState.CODEC)
    );

    private SpatialLinkWorldAttachments() { }

    public static void initialize() {
        // Static initialization registers the attachment.
    }
}
