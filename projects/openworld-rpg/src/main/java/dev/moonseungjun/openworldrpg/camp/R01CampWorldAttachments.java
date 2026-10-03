package dev.moonseungjun.openworldrpg.camp;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class R01CampWorldAttachments {
    public static final AttachmentType<R01CampWorldState> STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "r01_camp_world"
                    ),
                    builder -> builder
                            .initializer(R01CampWorldState::initial)
                            .persistent(R01CampWorldState.CODEC)
            );

    private R01CampWorldAttachments() {
    }

    public static void initialize() {
    }
}
