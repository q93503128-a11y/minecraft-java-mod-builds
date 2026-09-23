package dev.moonseungjun.openworldrpg.economy;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent player Gold authority. */
public final class PlayerCurrencyAttachments {
    public static final AttachmentType<PlayerCurrencyState> CURRENCY =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "currency"
                    ),
                    builder -> builder
                            .initializer(PlayerCurrencyState::initial)
                            .persistent(PlayerCurrencyState.CODEC)
                            .copyOnDeath()
            );

    private PlayerCurrencyAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
