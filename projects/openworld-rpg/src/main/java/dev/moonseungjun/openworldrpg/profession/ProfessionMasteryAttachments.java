package dev.moonseungjun.openworldrpg.profession;

import dev.moonseungjun.openworldrpg.OpenworldRpgMod;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

public final class ProfessionMasteryAttachments {
    public static final AttachmentType<ProfessionMasteryState> STATE =
            AttachmentRegistry.create(
                    Identifier.fromNamespaceAndPath(
                            OpenworldRpgMod.MOD_ID,
                            "profession_mastery"
                    ),
                    builder -> builder
                            .initializer(ProfessionMasteryState::initial)
                            .persistent(ProfessionMasteryState.CODEC)
                            .copyOnDeath()
            );

    private ProfessionMasteryAttachments() {
    }

    public static void initialize() {
    }
}
