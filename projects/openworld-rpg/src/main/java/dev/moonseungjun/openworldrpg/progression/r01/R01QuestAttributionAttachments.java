package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent bounded class-attribution evidence for R01 objectives. */
public final class R01QuestAttributionAttachments {
    public static final AttachmentType<R01QuestAttributionState> QUEST_ATTRIBUTION =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_quest_attribution"
                    ),
                    builder -> builder
                            .initializer(R01QuestAttributionState::initial)
                            .persistent(R01QuestAttributionState.CODEC)
                            .copyOnDeath()
            );

    private R01QuestAttributionAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
