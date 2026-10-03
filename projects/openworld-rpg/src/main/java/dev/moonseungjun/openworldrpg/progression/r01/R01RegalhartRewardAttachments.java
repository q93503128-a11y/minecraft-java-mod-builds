package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent server-world Regalhart participation and personal reward plans. */
public final class R01RegalhartRewardAttachments {
    public static final AttachmentType<R01RegalhartRewardState> REGALHART_REWARDS =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_regalhart_rewards"
                    ),
                    builder -> builder
                            .initializer(R01RegalhartRewardState::initial)
                            .persistent(R01RegalhartRewardState.CODEC)
            );

    private R01RegalhartRewardAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
