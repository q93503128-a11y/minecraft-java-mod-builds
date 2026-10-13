package dev.moonseungjun.openworldrpg.mount;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/** Persistent player mount progression. */
public final class TrailStagMountAttachments {
    public static final AttachmentType<TrailStagMountState> STATE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(OpenworldRpgMod.MOD_ID, "mount_state"),
            builder -> builder
                    .initializer(TrailStagMountState::initial)
                    .persistent(TrailStagMountState.CODEC)
                    .copyOnDeath()
    );

    private TrailStagMountAttachments() {
    }

    public static void initialize() {
    }
}
