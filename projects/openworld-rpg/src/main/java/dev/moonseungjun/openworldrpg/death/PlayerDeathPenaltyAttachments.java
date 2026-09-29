package dev.moonseungjun.openworldrpg.death;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent player death-penalty transaction attachment. */
public final class PlayerDeathPenaltyAttachments {
    public static final AttachmentType<PlayerDeathPenaltyState> DEATH_PENALTY =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "death_penalty"
                    ),
                    builder -> builder
                            .initializer(PlayerDeathPenaltyState::initial)
                            .persistent(PlayerDeathPenaltyState.CODEC)
                            .copyOnDeath()
            );

    private PlayerDeathPenaltyAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
