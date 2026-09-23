package dev.moonseungjun.openworldrpg.recovery;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent Recovery Belt attachment. */
public final class RecoveryBeltAttachments {
    public static final AttachmentType<RecoveryBeltState> RECOVERY_BELT =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "recovery_belt"
                    ),
                    builder -> builder
                            .initializer(RecoveryBeltState::empty)
                            .persistent(RecoveryBeltState.CODEC)
                            .copyOnDeath()
            );

    private RecoveryBeltAttachments() {
    }

    public static void initialize() {
        // Class initialization registers the attachment type.
    }
}
