package dev.moonseungjun.augmentationsurvival.threat;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** World-global state is persisted on the server Overworld only. */
public final class WorldThreatAttachments {
    public static final AttachmentType<WorldThreatState> THREAT_STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            AugmentationSurvival.MOD_ID,
                            "world_threat"
                    ),
                    builder -> builder
                            .initializer(WorldThreatState::initial)
                            .persistent(WorldThreatState.CODEC)
            );

    private WorldThreatAttachments() {
    }

    public static void initialize() {
        // Static field initialization registers the attachment type.
    }
}
