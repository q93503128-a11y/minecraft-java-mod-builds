package dev.moonseungjun.augmentationsurvival.augment.companion;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent per-entity ownership state for domination companions. */
public final class CompanionEntityAttachments {
    public static final AttachmentType<CompanionEntityState> ENTITY_STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "companion_entity_state"),
            builder -> builder.initializer(CompanionEntityState::initial).persistent(CompanionEntityState.CODEC)
    );

    private CompanionEntityAttachments() {
    }

    public static void initialize() {
        // Static initialization registers the attachment.
    }
}
