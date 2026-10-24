package dev.moonseungjun.augmentationsurvival.augment.companion;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent save-wide state for domination occupancy. Stored on the Overworld level. */
public final class DominationWorldAttachments {
    public static final AttachmentType<DominationWorldState> STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "domination_world_state"),
            builder -> builder.initializer(DominationWorldState::initial).persistent(DominationWorldState.CODEC)
    );

    private DominationWorldAttachments() {
    }

    public static void initialize() {
        // Static initialization registers the attachment.
    }
}
