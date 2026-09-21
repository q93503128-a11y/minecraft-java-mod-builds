package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * Persistent Fabric Data Attachments owned by Openworld RPG.
 */
public final class PlayerProgressionAttachments {
    public static final AttachmentType<PlayerProgressionState> COMBAT_PROGRESSION =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "combat_progression"
                    ),
                    builder -> builder
                            .initializer(PlayerProgressionState::initial)
                            .persistent(PlayerProgressionState.CODEC)
                            .copyOnDeath()
            );

    private PlayerProgressionAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
