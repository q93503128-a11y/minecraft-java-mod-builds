package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent shared Nature Spirit participation and personal pre-rolled reward plans. */
public final class R01NatureSpiritRewardAttachments {
    public static final AttachmentType<R01NatureSpiritRewardState> NATURE_SPIRIT_REWARDS =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_nature_spirit_rewards"
                    ),
                    builder -> builder
                            .initializer(R01NatureSpiritRewardState::initial)
                            .persistent(R01NatureSpiritRewardState.CODEC)
            );

    private R01NatureSpiritRewardAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
