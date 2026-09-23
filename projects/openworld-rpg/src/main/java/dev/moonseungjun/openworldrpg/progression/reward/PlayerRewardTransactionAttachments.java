package dev.moonseungjun.openworldrpg.progression.reward;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent coordinator state for interrupted one-time reward transactions. */
public final class PlayerRewardTransactionAttachments {
    public static final AttachmentType<PlayerRewardTransactionState> REWARD_TRANSACTIONS =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "reward_transactions"
                    ),
                    builder -> builder
                            .initializer(PlayerRewardTransactionState::initial)
                            .persistent(PlayerRewardTransactionState.CODEC)
                            .copyOnDeath()
            );

    private PlayerRewardTransactionAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
