package dev.moonseungjun.openworldrpg.recovery;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01NourishmentAttachments {
    public static final AttachmentType<R01NourishmentState> STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_nourishment"
                    ),
                    builder -> builder
                            .initializer(R01NourishmentState::initial)
                            .persistent(R01NourishmentState.CODEC)
                            .copyOnDeath()
            );

    private R01NourishmentAttachments() {
    }

    public static void initialize() {
    }
}
