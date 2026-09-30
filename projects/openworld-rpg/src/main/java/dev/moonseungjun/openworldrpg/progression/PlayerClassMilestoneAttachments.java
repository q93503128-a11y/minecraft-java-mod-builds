package dev.moonseungjun.openworldrpg.progression;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class PlayerClassMilestoneAttachments {
    public static final AttachmentType<PlayerClassMilestoneState> CLASS_MILESTONES =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "class_milestones"
                    ),
                    builder -> builder
                            .initializer(PlayerClassMilestoneState::initial)
                            .persistent(PlayerClassMilestoneState.CODEC)
                            .copyOnDeath()
            );

    private PlayerClassMilestoneAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
