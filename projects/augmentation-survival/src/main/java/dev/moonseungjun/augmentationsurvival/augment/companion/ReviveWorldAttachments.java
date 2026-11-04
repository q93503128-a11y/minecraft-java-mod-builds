package dev.moonseungjun.augmentationsurvival.augment.companion;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Save-wide persistent marker state, stored on the Overworld. */
public final class ReviveWorldAttachments {
    public static final AttachmentType<ReviveWorldState> STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(AugmentationSurvival.MOD_ID, "revive_world_state"),
            builder -> builder.initializer(ReviveWorldState::initial).persistent(ReviveWorldState.CODEC)
    );

    private ReviveWorldAttachments() { }

    public static void initialize() { }
}
