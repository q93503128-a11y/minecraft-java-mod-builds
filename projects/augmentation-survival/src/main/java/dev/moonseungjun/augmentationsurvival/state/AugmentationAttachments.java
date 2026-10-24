package dev.moonseungjun.augmentationsurvival.state;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent server-authoritative attachment types for augmentation players. */
public final class AugmentationAttachments {
    public static final AttachmentType<AugmentationPlayerState> PLAYER_STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            AugmentationSurvival.MOD_ID,
                            "player_state"
                    ),
                    builder -> builder
                            .initializer(AugmentationPlayerState::initial)
                            .persistent(AugmentationPlayerState.CODEC)
                            .copyOnDeath()
            );

    private AugmentationAttachments() {
    }

    public static void initialize() {
        // Static field initialization registers the attachment type.
    }
}
