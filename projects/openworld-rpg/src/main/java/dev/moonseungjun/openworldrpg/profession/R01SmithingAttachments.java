package dev.moonseungjun.openworldrpg.profession;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01SmithingAttachments {
    public static final AttachmentType<R01SmithingState> STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_smithing"
                    ),
                    builder -> builder
                            .initializer(R01SmithingState::initial)
                            .persistent(R01SmithingState.CODEC)
                            .copyOnDeath()
            );

    private R01SmithingAttachments() {
    }

    public static void initialize() {
    }
}
