package dev.moonseungjun.augmentationsurvival.augment.storage;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent Overworld attachment containing all augmentation-owned storage segments. */
public final class StorageWorldAttachments {
    public static final AttachmentType<StorageWorldState> STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "storage_world_state"),
            builder -> builder.initializer(StorageWorldState::initial).persistent(StorageWorldState.CODEC)
    );

    private StorageWorldAttachments() {
    }

    public static void initialize() {
        // Static initialization registers the attachment.
    }
}
