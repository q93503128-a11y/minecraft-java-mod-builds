package dev.moonseungjun.augmentationsurvival.threat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent per-entity threat metadata. */
public final class ThreatEntityAttachments {
    public static final AttachmentType<ThreatEntityState> ENTITY_STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            AugmentationSurvival.MOD_ID,
                            "threat_entity_state"
                    ),
                    builder -> builder
                            .initializer(ThreatEntityState::initial)
                            .persistent(ThreatEntityState.CODEC)
            );

    private ThreatEntityAttachments() {
    }

    public static void initialize() {
        // Static field initialization registers the attachment type.
    }
}
