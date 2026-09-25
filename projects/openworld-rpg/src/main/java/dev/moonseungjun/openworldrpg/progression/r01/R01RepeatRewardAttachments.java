package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent bounded state for repeatable R01 reward transactions. */
public final class R01RepeatRewardAttachments {
    public static final AttachmentType<R01RepeatRewardState> REPEAT_REWARDS =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_repeat_rewards"
                    ),
                    builder -> builder
                            .initializer(R01RepeatRewardState::initial)
                            .persistent(R01RepeatRewardState.CODEC)
                            .copyOnDeath()
            );

    private R01RepeatRewardAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
