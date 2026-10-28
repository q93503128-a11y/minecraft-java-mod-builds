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

    public static final AttachmentType<ShieldThrowEscrowState> SHIELD_THROW_ESCROW =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            AugmentationSurvival.MOD_ID,
                            "shield_throw_escrow"
                    ),
                    builder -> builder
                            .initializer(ShieldThrowEscrowState::empty)
                            .persistent(ShieldThrowEscrowState.CODEC)
                            .copyOnDeath()
            );

    private AugmentationAttachments() {
    }

    public static void initialize() {
        // Static field initialization registers the attachment types.
    }
}
