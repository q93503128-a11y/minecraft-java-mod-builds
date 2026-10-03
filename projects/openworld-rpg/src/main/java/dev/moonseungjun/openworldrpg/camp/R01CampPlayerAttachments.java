package dev.moonseungjun.openworldrpg.camp;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01CampPlayerAttachments {
    public static final AttachmentType<R01CampPlayerState> STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_camp_player"
                    ),
                    builder -> builder
                            .initializer(R01CampPlayerState::initial)
                            .persistent(R01CampPlayerState.CODEC)
                            .copyOnDeath()
            );

    private R01CampPlayerAttachments() {
    }

    public static void initialize() {
    }
}
