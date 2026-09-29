package dev.moonseungjun.openworldrpg.combat.state;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent combat-resource/cooldown reconnect state. */
public final class PlayerCombatSessionAttachments {
    public static final AttachmentType<PlayerCombatSessionState> COMBAT_SESSION =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "combat_session"
                    ),
                    builder -> builder
                            .initializer(PlayerCombatSessionState::empty)
                            .persistent(PlayerCombatSessionState.CODEC)
                            .copyOnDeath()
            );

    private PlayerCombatSessionAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
